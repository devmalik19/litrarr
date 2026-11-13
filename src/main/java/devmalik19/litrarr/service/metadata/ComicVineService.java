package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.HttpHelper;
import devmalik19.litrarr.helper.IssuePathHelper;
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
	private static final int ISSUES_PAGE_SIZE = 100;

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;

	@Value("${app.api-keys.comic-vine:}")
	private String apiKey;

	public ComicVineService(HttpRequestService httpRequestService,
							ObjectMapper objectMapper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
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
			// The issues endpoint returns at most 100 issues per call, so page through them
			int offset = 0;
			int total = Integer.MAX_VALUE;
			while (offset < total)
			{
				JsonNode root = fetchIssuesPage(apiKey, volumeId, offset);
				if (root == null)
					return issues;

				JsonNode issuesNode = root.path("results");
				if (!issuesNode.isArray() || issuesNode.isEmpty())
					break;

				total = root.path("number_of_total_results").asInt(0);
				offset += issuesNode.size();

				for (JsonNode issueNode : issuesNode)
					issues.add(toItem(issueNode, parentLibrary));
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to fetch issues for ComicVine volume {}: {}", volumeId, e.getMessage());
		}

		return issues;
	}

	/** Returns one page of issues, or null when the API did not return a usable response. */
	private JsonNode fetchIssuesPage(String apiKey, String volumeId, int offset) throws Exception
	{
		URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/issues/")
			.queryParam("api_key", apiKey)
			.queryParam("format", "json")
			.queryParam("filter", "volume:" + volumeId)
			.queryParam("field_list", "id,name,issue_number,cover_date")
			.queryParam("limit", ISSUES_PAGE_SIZE)
			.queryParam("offset", offset)
			.build()
			.toUri();

		String response = httpRequestService.doGetRequest(uri, HttpHelper.jsonApiHeaders());
		if (!StringUtils.hasText(response))
			return null;

		JsonNode root = objectMapper.readTree(response);
		int statusCode = root.path("status_code").asInt(-1);
		if (statusCode != 1)
		{
			logger.warn("ComicVine API returned status_code {} when fetching issues for volume {}", statusCode, volumeId);
			return null;
		}
		return root;
	}

	private Item toItem(JsonNode issueNode, Library parentLibrary)
	{
		String rawNumber = issueNode.path("issue_number").asText("");
		JsonNode nameNode = issueNode.path("name");
		String title = nameNode.isTextual() ? nameNode.asText().trim() : "";

		Item item = new Item();
		item.setName(!title.isEmpty() ? title : "Issue #" + (rawNumber.isEmpty() ? "?" : rawNumber));
		item.setIssueNumber(IssuePathHelper.parseIssueNumber(rawNumber));
		item.setGuid(issueNode.path("id").asText(null));
		item.setReleaseOn(parseCoverDate(issueNode.path("cover_date").asText(null)));
		item.setType(Category.COMICS);
		item.setLibrary(parentLibrary);
		item.setMissing(true);
		return item;
	}

	/** ComicVine uses 00 for an unknown day or month, for example 2015-03-00. */
	private LocalDate parseCoverDate(String coverDate)
	{
		if (!StringUtils.hasText(coverDate))
			return null;
		try
		{
			return LocalDate.parse(coverDate.replace("-00", "-01"), DateTimeFormatter.ISO_LOCAL_DATE);
		}
		catch (DateTimeParseException e)
		{
			logger.debug("Could not parse cover_date '{}': {}", coverDate, e.getMessage());
			return null;
		}
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
