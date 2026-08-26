package devmalik19.litrarr.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.service.FileSystemService;
import devmalik19.litrarr.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import devmalik19.litrarr.constants.Constants;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches manga metadata from the official MyAnimeList API.
 * Requires a MAL Client ID to be configured, otherwise all requests are skipped.
 */
@Service
public class MyAnimeListService
{
	private static final Logger logger = LoggerFactory.getLogger(MyAnimeListService.class);
	private static final String MAL_BASE_URL = "https://api.myanimelist.net/v2";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final FileSystemService fileSystemService;

	@Value("${app.api-keys.mal-client-id:}")
	private String malClientId;

	public MyAnimeListService(HttpRequestService httpRequestService,
							  ObjectMapper objectMapper,
							  FileSystemService fileSystemService)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.fileSystemService = fileSystemService;
	}

	private boolean isEnabled()
	{
		return StringUtils.hasText(malClientId);
	}

	public void getMetaForLibrary(Library library)
	{
		if (!isEnabled())
			return;

		try
		{
			String query = library.getName();
			List<MetadataResult> results = search(query);
			if (!results.isEmpty())
			{
				MetadataResult first = results.get(0);
				if (StringUtils.hasText(first.getAuthor()))
					library.setCreator(first.getAuthor());

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
				.queryParam("fields", "id,title,alternative_titles,authors{first_name,last_name},start_date,main_picture")
				.build()
				.toUri();

			Map<String, String> headers = new HashMap<>();
			headers.put("X-MAL-CLIENT-ID", malClientId);
			headers.put("Accept", "application/json");
			headers.put("User-Agent", Constants.USER_AGENT);

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

				// Title: prefer English alternative, fall back to default
				String title = manga.path("alternative_titles").path("en").asText(null);
				if (!StringUtils.hasText(title))
					title = manga.path("title").asText(null);
				result.setTitle(title);

				// Author
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

				// Year from start_date (format: "YYYY-MM-DD" or "YYYY")
				String startDate = manga.path("start_date").asText(null);
				if (StringUtils.hasText(startDate) && startDate.length() >= 4)
					result.setYear(startDate.substring(0, 4));

				// Cover image
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
