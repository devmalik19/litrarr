package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.HttpHelper;
import devmalik19.litrarr.helper.IssuePathHelper;
import devmalik19.litrarr.helper.StringHelper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MyAnimeListService
{
	private static final Logger logger = LoggerFactory.getLogger(MyAnimeListService.class);
	private static final String MAL_BASE_URL = "https://api.myanimelist.net/v2";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;

	@Value("${app.api-keys.mal-client-id:}")
	private String malClientId;

	public MyAnimeListService(HttpRequestService httpRequestService,
							  ObjectMapper objectMapper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
	}

	private boolean isEnabled()
	{
		return StringUtils.hasText(malClientId);
	}

	public void getMetaForItem(Item item)
	{

	}

	public List<Item> getVolumesForManga(String mangaId, Library parentLibrary)
	{
		List<Item> volumes = new ArrayList<>();

		if (!isEnabled() || !StringUtils.hasText(mangaId))
			return volumes;

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(MAL_BASE_URL + "/manga/" + mangaId)
				.queryParam("fields", "num_volumes,start_date")
				.build()
				.toUri();

			Map<String, String> headers = HttpHelper.jsonApiHeaders("X-MAL-CLIENT-ID", malClientId);

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return volumes;

			JsonNode root = objectMapper.readTree(response);
			int numVolumes = root.path("num_volumes").asInt(0);

			if (numVolumes <= 0)
			{
				logger.debug("MAL manga {} has no volume count, skipping issue creation", mangaId);
				return volumes;
			}

			LocalDate startDate = StringHelper.parseDate(root.path("start_date").asText(null));

			for (int i = 1; i <= numVolumes; i++)
			{
				Item item = new Item();
				item.setName("Volume " + i);
				item.setIssueNumber(i);
				item.setGuid(mangaId + "-vol-" + i);
				item.setType(Category.MANGA);
				item.setLibrary(parentLibrary);
				item.setMissing(true);

				if (i == 1 && startDate != null)
					item.setReleaseOn(startDate);

				volumes.add(item);
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to fetch volumes for MAL manga {}: {}", mangaId, e.getMessage());
		}

		return volumes;
	}

	@Cacheable("MyAnimeListMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();

		if (!isEnabled())
		{
			logger.debug("MyAnimeList API key not configured, skipping search");
			return results;
		}

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(MAL_BASE_URL + "/manga")
				.queryParam("q", query)
				.queryParam("limit", 10)
				.queryParam("fields", "id,title,alternative_titles,authors{first_name,last_name},start_date,num_volumes,main_picture")
				.build()
				.toUri();

			Map<String, String> headers = HttpHelper.jsonApiHeaders("X-MAL-CLIENT-ID", malClientId);

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return results;

			JsonNode root = objectMapper.readTree(response);
			JsonNode data = root.path("data");
			if (!data.isArray())
				return results;

			for (JsonNode node : data)
			{
				JsonNode manga = node.path("node");
				MetadataResult result = new MetadataResult();
				result.setCategory(Category.MANGA);

				String malId = manga.path("id").asText(null);
				if (StringUtils.hasText(malId))
					result.setSourceId(malId);

				String title = manga.path("alternative_titles").path("en").asText(null);
				if (!StringUtils.hasText(title))
					title = manga.path("title").asText(null);
				result.setTitle(title);

				JsonNode authors = manga.path("authors");
				if (authors.isArray() && !authors.isEmpty())
				{
					JsonNode firstAuthor = authors.get(0).path("node");
					String firstName = firstAuthor.path("first_name").asText("");
					String lastName = firstAuthor.path("last_name").asText("");
					String authorName = (firstName + " " + lastName).trim();
					if (StringUtils.hasText(authorName))
						result.setAuthor(authorName);
				}

				String startDate = manga.path("start_date").asText(null);
				if (StringUtils.hasText(startDate) && startDate.length() >= 4)
					result.setYear(startDate.substring(0, 4));

				JsonNode mainPicture = manga.path("main_picture");
				if (!mainPicture.isMissingNode())
				{
					String imageUrl = mainPicture.path("large").asText(null);
					if (!StringUtils.hasText(imageUrl))
						imageUrl = mainPicture.path("medium").asText(null);
					result.setImageUrl(imageUrl);
				}

				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("MyAnimeList search failed for '{}': {}", query, e.getMessage());
		}

		return results;
	}
}
