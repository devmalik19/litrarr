package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.HttpHelper;
import devmalik19.litrarr.service.FileSystemService;
import devmalik19.litrarr.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ComicVineService
{
	private static final Logger logger = LoggerFactory.getLogger(ComicVineService.class);
	private static final String BASE_URL = "https://comicvine.gamespot.com/api";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final FileSystemService fileSystemService;

	@Value("${app.api-keys.comic-vine:}")
	private String apiKey;

	public ComicVineService(HttpRequestService httpRequestService,
							ObjectMapper objectMapper,
							FileSystemService fileSystemService)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.fileSystemService = fileSystemService;
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
					library.setAuthor(first.getAuthor());

				if (StringUtils.hasText(first.getImageUrl()))
				{
					String fileName = fileSystemService.downloadImageToCache(
						first.getImageUrl(), "library", String.valueOf(library.getId()));
					if (fileName != null)
						library.setImage(fileName);
				}
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

	/**
	 * Fetches all issues for a given ComicVine volume and returns them as Item entities
	 * linked to the provided parent library.
	 */
	public List<Item> getIssuesForVolume(String volumeId, Library parentLibrary)
	{
		List<Item> issues = new ArrayList<>();

		String apiKey = getApiKey();
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(volumeId))
			return issues;

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/volume/4050-" + volumeId)
				.queryParam("api_key", apiKey)
				.queryParam("format", "json")
				.queryParam("field_list", "issues")
				.build()
				.toUri();

			Map<String, String> headers = HttpHelper.jsonApiHeaders();

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return issues;

			JsonNode root = objectMapper.readTree(response);
			int statusCode = root.path("status_code").asInt(-1);
			if (statusCode != 1)
			{
				logger.warn("ComicVine API returned status_code {} when fetching issues for volume {}", statusCode, volumeId);
				return issues;
			}

			JsonNode issuesNode = root.path("results").path("issues");
			if (!issuesNode.isArray())
				return issues;

			for (JsonNode issueNode : issuesNode)
			{
				Item item = new Item();
				item.setName(issueNode.path("name").asText("Issue #" + issueNode.path("issue_number").asText("?")));
				item.setGuid(issueNode.path("id").asText(null));
				item.setType(Category.COMICS);
				item.setLibrary(parentLibrary);
				item.setMissing(true);

				issues.add(item);
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to fetch issues for ComicVine volume {}: {}", volumeId, e.getMessage());
		}

		return issues;
	}

	/**
	 * Fetches detailed issue information (including cover_date) for a single issue by its ID.
	 * Returns the release date if available, null otherwise.
	 */
	public LocalDate getIssueCoverDate(String issueId)
	{
		String apiKey = getApiKey();
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(issueId))
			return null;

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/issue/4000-" + issueId)
				.queryParam("api_key", apiKey)
				.queryParam("format", "json")
				.queryParam("field_list", "cover_date")
				.build()
				.toUri();

			Map<String, String> headers = HttpHelper.jsonApiHeaders();

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return null;

			JsonNode root = objectMapper.readTree(response);
			if (root.path("status_code").asInt(-1) != 1)
				return null;

			String coverDate = root.path("results").path("cover_date").asText(null);
			if (StringUtils.hasText(coverDate))
				return LocalDate.parse(coverDate, DateTimeFormatter.ISO_LOCAL_DATE);
		}
		catch (DateTimeParseException e)
		{
			logger.debug("Could not parse cover_date for issue {}: {}", issueId, e.getMessage());
		}
		catch (Exception e)
		{
			logger.error("Failed to fetch cover_date for issue {}: {}", issueId, e.getMessage());
		}

		return null;
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
				.queryParam("field_list", "id,name,start_year,publisher,people,image")
				.build()
				.toUri();

			Map<String, String> headers = HttpHelper.jsonApiHeaders();

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
				result.setCategory(Category.COMICS);
				result.setTitle(item.path("name").asText(null));

				// Capture the volume ID as sourceId for fetching issues later
				String id = item.path("id").asText(null);
				if (StringUtils.hasText(id))
					result.setSourceId(id);

				String startYear = item.path("start_year").asText(null);
				if (StringUtils.hasText(startYear))
					result.setYear(startYear);

				JsonNode publisher = item.path("publisher");
				if (publisher != null && !publisher.isMissingNode() && !publisher.isNull())
				{
					String publisherName = publisher.path("name").asText(null);
					if (StringUtils.hasText(publisherName))
						result.setPublisher(publisherName);
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

				JsonNode image = item.path("image");
				if (!image.isMissingNode() && !image.isNull())
				{
					String imageUrl = image.path("medium_url").asText(null);
					if (!StringUtils.hasText(imageUrl))
						imageUrl = image.path("small_url").asText(null);
					result.setImageUrl(imageUrl);
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
		return StringUtils.hasText(apiKey) ? apiKey : null;
	}
}
