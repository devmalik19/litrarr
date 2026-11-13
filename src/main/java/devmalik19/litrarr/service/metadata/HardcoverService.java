package devmalik19.litrarr.service.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.HttpHelper;
import devmalik19.litrarr.service.HttpRequestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HardcoverService
{
	private static final Logger logger = LoggerFactory.getLogger(HardcoverService.class);
	private static final String BASE_URL = "https://api.hardcover.app/v1/graphql";
	private static final String SEARCH_QUERY =
		"query($q: String!) { search(query: $q, query_type: \"Book\", per_page: 10, page: 1) { results } }";
	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	@Value("${app.api-keys.hardcover:}")
	private String apiKey;
	public HardcoverService(HttpRequestService httpRequestService,
							ObjectMapper objectMapper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
	}
	@Cacheable("HardcoverMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();
		if (!isEnabled())
		{
			logger.debug("Hardcover API key not configured, skipping search");
			return results;
		}
		try
		{
			ObjectNode body = objectMapper.createObjectNode();
			body.put("query", SEARCH_QUERY);
			body.putObject("variables").put("q", query);
			Map<String, String> headers = HttpHelper.jsonApiHeaders(
				"Content-Type", "application/json",
				"Authorization", bearerToken());
			String response = httpRequestService.doPostRequest(BASE_URL, objectMapper.writeValueAsString(body), headers);
			if (!StringUtils.hasText(response))
				return results;

			JsonNode searchResults = objectMapper.readTree(response).path("data").path("search").path("results");
			if (searchResults.isTextual())
				searchResults = objectMapper.readTree(searchResults.asText());
			JsonNode hits = searchResults.path("hits");
			if (!hits.isArray())
				return results;
			for (JsonNode hit : hits)
			{
				JsonNode document = hit.path("document");
				MetadataResult result = new MetadataResult();
				result.setTitle(document.path("title").asText(null));
				result.setSourceId(document.path("id").asText(null));
				JsonNode authors = document.path("author_names");
				if (authors.isArray() && !authors.isEmpty())
					result.setAuthor(authors.get(0).asText(null));
				int year = document.path("release_year").asInt(0);
				if (year > 0)
					result.setYear(String.valueOf(year));
				String imageUrl = document.path("image").path("url").asText(null);
				if (StringUtils.hasText(imageUrl))
					result.setImageUrl(imageUrl);
				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("Hardcover search failed for '{}': {}", query, e.getMessage());
		}
		return results;
	}

	private String bearerToken()
	{
		String token = apiKey.trim();
		return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token : "Bearer " + token;
	}
	private boolean isEnabled()
	{
		return StringUtils.hasText(apiKey);
	}
}
