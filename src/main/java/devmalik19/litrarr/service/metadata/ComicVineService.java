package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.SettingsHelper;
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
public class ComicVineService
{
	private static final Logger logger = LoggerFactory.getLogger(ComicVineService.class);
	private static final String BASE_URL = "https://comicvine.gamespot.com/api";
	public static final String SETTINGS_KEY = "comic_vine";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final SettingsHelper settingsHelper;

	public ComicVineService(HttpRequestService httpRequestService,
							ObjectMapper objectMapper,
							SettingsHelper settingsHelper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.settingsHelper = settingsHelper;
	}

	public void getMetaForLibrary(Library library)
	{
		try
		{
			String query = library.getName();
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
		// Item-level metadata enrichment can be added later
	}

	@Cacheable("ComicVineMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();

		String apiKey = getApiKey();
		if (!StringUtils.hasText(apiKey))
		{
			logger.debug("ComicVine API key not configured, skipping search");
			return results;
		}

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/search")
				.queryParam("api_key", apiKey)
				.queryParam("format", "json")
				.queryParam("resources", "volume")
				.queryParam("query", query)
				.queryParam("limit", 10)
				.queryParam("field_list", "name,start_year,publisher,people")
				.build()
				.toUri();

			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("User-Agent", "Litrarr/1.0");

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return results;

			JsonNode root = objectMapper.readTree(response);
			int statusCode = root.path("status_code").asInt(-1);
			if (statusCode != 1)
			{
				logger.warn("ComicVine API returned status_code: {}", statusCode);
				return results;
			}

			JsonNode resultsNode = root.path("results");
			if (!resultsNode.isArray())
				return results;

			for (JsonNode item : resultsNode)
			{
				MetadataResult result = new MetadataResult();
				result.setTitle(item.path("name").asText(null));

				String startYear = item.path("start_year").asText(null);
				if (StringUtils.hasText(startYear))
					result.setYear(startYear);

				JsonNode publisher = item.path("publisher");
				if (publisher != null && !publisher.isMissingNode() && !publisher.isNull())
				{
					String publisherName = publisher.path("name").asText(null);
					if (StringUtils.hasText(publisherName))
						result.setAuthor(publisherName);
				}

				// Try to get a person (writer) from the people credits
				JsonNode people = item.path("people");
				if (people != null && people.isArray() && !people.isEmpty())
				{
					for (JsonNode person : people)
					{
						String role = person.path("role").asText("");
						if (role.toLowerCase().contains("writer"))
						{
							result.setAuthor(person.path("name").asText(null));
							break;
						}
					}
				}

				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("ComicVine search failed for '{}': {}", query, e.getMessage());
		}

		return results;
	}

	private String getApiKey()
	{
		var settings = settingsHelper.getConnectionSettings(SETTINGS_KEY);
		if (settings != null && StringUtils.hasText(settings.getApiKey()))
			return settings.getApiKey();
		return null;
	}
}
