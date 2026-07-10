package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleBookService
{
	private static final Logger logger = LoggerFactory.getLogger(GoogleBookService.class);
	private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;

	public GoogleBookService(HttpRequestService httpRequestService, ObjectMapper objectMapper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
	}

	public void getMetaForLibrary(Library library)
	{
		try
		{
			String query = library.getName();
			if (StringUtils.hasText(library.getCreator()))
				query = library.getCreator() + " " + query;

			List<MetadataResult> results = search(query);
			if (!results.isEmpty())
			{
				MetadataResult first = results.get(0);
				if (StringUtils.hasText(first.getAuthor()))
					library.setCreator(first.getAuthor());
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to fetch metadata for library '{}': {}", library.getName(), e.getMessage());
		}
	}

	public void getMetaForItem(Item item)
	{
		// Items are individual files; metadata enrichment can be added later
	}

	@Cacheable("GoogleBooksMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
				.queryParam("q", query)
				.queryParam("maxResults", 10)
				.queryParam("printType", "books")
				.build()
				.toUri();

			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return results;

			JsonNode root = objectMapper.readTree(response);
			int totalItems = root.path("totalItems").asInt(0);
			if (totalItems == 0)
				return results;

			JsonNode items = root.path("items");
			if (!items.isArray())
				return results;

			for (JsonNode item : items)
			{
				JsonNode volumeInfo = item.path("volumeInfo");
				MetadataResult result = new MetadataResult();

				result.setTitle(volumeInfo.path("title").asText(null));

				JsonNode authors = volumeInfo.path("authors");
				if (authors.isArray() && !authors.isEmpty())
					result.setAuthor(authors.get(0).asText(null));

				String publishedDate = volumeInfo.path("publishedDate").asText("");
				if (publishedDate.length() >= 4)
					result.setYear(publishedDate.substring(0, 4));

				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("Google Books search failed for '{}': {}", query, e.getMessage());
		}

		return results;
	}
}
