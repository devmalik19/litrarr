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
	private final FileSystemService fileSystemService;

	@Value("${app.api-keys.google-books:}")
	private String apiKey;

	public GoogleBookService(HttpRequestService httpRequestService,
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
			if (StringUtils.hasText(library.getCreator()))
				query = library.getCreator() + " " + query;

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
		// Items are individual files; metadata enrichment can be added later
	}

	@Cacheable("GoogleBooksMetadata")
	public List<MetadataResult> search(String query)
	{
		List<MetadataResult> results = new ArrayList<>();

		String apiKey = getApiKey();

		try
		{
			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BASE_URL)
				.queryParam("q", query)
				.queryParam("maxResults", 10)
				.queryParam("printType", "books");

			if (StringUtils.hasText(apiKey))
				uriBuilder.queryParam("key", apiKey);

			URI uri = uriBuilder.build().toUri();

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

				JsonNode imageLinks = volumeInfo.path("imageLinks");
				if (!imageLinks.isMissingNode())
				{
					String thumbnail = imageLinks.path("thumbnail").asText(null);
					if (!StringUtils.hasText(thumbnail))
						thumbnail = imageLinks.path("smallThumbnail").asText(null);
					result.setImageUrl(thumbnail);
				}

				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("Google Books search failed for '{}': {}", query, e.getMessage());
		}

		return results;
	}

	private String getApiKey()
	{
		return StringUtils.hasText(apiKey) ? apiKey : null;
	}
}
