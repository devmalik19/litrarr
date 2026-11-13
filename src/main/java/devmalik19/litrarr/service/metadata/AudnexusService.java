package devmalik19.litrarr.service.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.HttpHelper;
import devmalik19.litrarr.service.HttpRequestService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AudnexusService
{
	private static final Logger logger = LoggerFactory.getLogger(AudnexusService.class);
	private static final String AUDIBLE_SEARCH_URL = "https://api.audible.com/1.0/catalog/products";
	private static final String AUDNEXUS_BOOK_URL = "https://api.audnex.us/books/";
	private static final int MAX_RESULTS = 5;
	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	public AudnexusService(HttpRequestService httpRequestService,
						   ObjectMapper objectMapper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
	}
	@Cacheable("AudnexusMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();
		try
		{
			URI uri = UriComponentsBuilder.fromUriString(AUDIBLE_SEARCH_URL)
				.queryParam("keywords", query)
				.queryParam("num_results", MAX_RESULTS)
				.queryParam("products_sort_by", "Relevance")
				.queryParam("response_groups", "contributors,media,product_attrs")
				.build()
				.toUri();
			String response = httpRequestService.doGetRequest(uri, HttpHelper.jsonApiHeaders());
			if (!StringUtils.hasText(response))
				return results;
			JsonNode products = objectMapper.readTree(response).path("products");
			if (!products.isArray())
				return results;
			for (JsonNode product : products)
			{
				String asin = product.path("asin").asText(null);
				if (!StringUtils.hasText(asin))
					continue;

				MetadataResult result = fromAudnexus(asin);
				results.add(result != null ? result : fromCatalog(product, asin));
			}
		}
		catch (Exception e)
		{
			logger.error("Audnexus search failed for '{}': {}", query, e.getMessage());
		}
		return results;
	}
	private MetadataResult fromAudnexus(String asin)
	{
		try
		{
			String response = httpRequestService.doGetRequest(AUDNEXUS_BOOK_URL + asin, HttpHelper.jsonApiHeaders());
			if (!StringUtils.hasText(response))
				return null;
			JsonNode book = objectMapper.readTree(response);
			if (!StringUtils.hasText(book.path("title").asText(null)))
				return null;
			MetadataResult result = new MetadataResult();
			result.setSourceId(asin);
			result.setTitle(book.path("title").asText(null));
			JsonNode authors = book.path("authors");
			if (authors.isArray() && !authors.isEmpty())
				result.setAuthor(authors.get(0).path("name").asText(null));
			result.setPublisher(book.path("publisherName").asText(null));
			setYear(result, book.path("releaseDate").asText(""));
			result.setImageUrl(book.path("image").asText(null));
			return result;
		}
		catch (Exception e)
		{
			logger.debug("Audnexus lookup failed for {}: {}", asin, e.getMessage());
			return null;
		}
	}
	private MetadataResult fromCatalog(JsonNode product, String asin)
	{
		MetadataResult result = new MetadataResult();
		result.setSourceId(asin);
		result.setTitle(product.path("title").asText(null));
		JsonNode authors = product.path("authors");
		if (authors.isArray() && !authors.isEmpty())
			result.setAuthor(authors.get(0).path("name").asText(null));
		result.setPublisher(product.path("publisher_name").asText(null));
		setYear(result, product.path("release_date").asText(""));
		JsonNode images = product.path("product_images");
		result.setImageUrl(images.path("500").asText(null));
		return result;
	}
	private void setYear(MetadataResult result, String date)
	{
		if (date.length() >= 4)
			result.setYear(date.substring(0, 4));
	}
}
