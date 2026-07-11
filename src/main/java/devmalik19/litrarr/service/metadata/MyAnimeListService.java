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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches manga metadata from the Jikan API (public MyAnimeList proxy).
 * No API key required — Jikan is rate-limited to ~3 requests/second.
 */
@Service
public class MyAnimeListService
{
	private static final Logger logger = LoggerFactory.getLogger(MyAnimeListService.class);
	private static final String BASE_URL = "https://api.jikan.moe/v4";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final FileSystemService fileSystemService;

	public MyAnimeListService(HttpRequestService httpRequestService,
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

		try
		{
			URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/manga")
				.queryParam("q", query)
				.queryParam("limit", 10)
				.queryParam("order_by", "scored_by")
				.queryParam("sort", "desc")
				.build()
				.toUri();

			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");

			String response = httpRequestService.doGetRequest(uri, headers);
			if (!StringUtils.hasText(response))
				return results;

			JsonNode root = objectMapper.readTree(response);
			JsonNode data = root.path("data");
			if (!data.isArray())
				return results;

			for (JsonNode item : data)
			{
				MetadataResult result = new MetadataResult();

				// Prefer English title, fall back to default title
				String title = item.path("title_english").asText(null);
				if (!StringUtils.hasText(title))
					title = item.path("title").asText(null);
				result.setTitle(title);

				// Extract the first author from the list
				JsonNode authors = item.path("authors");
				if (authors.isArray() && !authors.isEmpty())
				{
					String authorName = authors.get(0).path("name").asText(null);
					if (StringUtils.hasText(authorName))
						result.setAuthor(authorName);
				}

				// Extract year from published.from (ISO date string)
				JsonNode published = item.path("published");
				String from = published.path("from").asText(null);
				if (StringUtils.hasText(from) && from.length() >= 4)
					result.setYear(from.substring(0, 4));

				// Extract cover image
				JsonNode images = item.path("images").path("jpg");
				if (!images.isMissingNode())
				{
					String imageUrl = images.path("large_image_url").asText(null);
					if (!StringUtils.hasText(imageUrl))
						imageUrl = images.path("image_url").asText(null);
					result.setImageUrl(imageUrl);
				}

				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("MyAnimeList (Jikan) search failed for '{}': {}", query, e.getMessage());
		}

		return results;
	}
}
