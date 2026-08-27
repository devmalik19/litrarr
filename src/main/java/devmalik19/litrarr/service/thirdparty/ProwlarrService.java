package devmalik19.litrarr.service.thirdparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Constants;
import devmalik19.litrarr.data.dao.Index;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.data.dto.Tag;
import devmalik19.litrarr.helper.PriorityHelper;
import devmalik19.litrarr.helper.SettingsHelper;
import devmalik19.litrarr.repository.IndexRepository;
import devmalik19.litrarr.data.dto.ConnectionSettings;

import devmalik19.litrarr.service.HttpRequestService;
import java.net.URI;
import java.util.*;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProwlarrService
{
	private static final Logger logger = LoggerFactory.getLogger(ProwlarrService.class);

	private final HttpRequestService httpRequestService;
	private final IndexRepository indexRepository;
	private final ObjectMapper objectMapper;
	private final SettingsHelper settingsHelper;

	public ProwlarrService(HttpRequestService httpRequestService,
						   IndexRepository indexRepository,
						   ObjectMapper objectMapper,
						   SettingsHelper settingsHelper)
	{
		this.httpRequestService = httpRequestService;
		this.indexRepository = indexRepository;
		this.objectMapper = objectMapper;
		this.settingsHelper = settingsHelper;
	}

    public String checkConnection(ConnectionSettings connectionSettings)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", connectionSettings.getApiKey());
        return httpRequestService.doGetRequest(
            String.format("%s/api/v1/system/status", connectionSettings.getUrl()), headers);
    }

    public void sync() throws Exception
	{
		logger.info("Starting indexes sync");

		ConnectionSettings prowlarrSettings = settingsHelper.getConnectionSettings(NetworkService.PROWLARR);
		if (prowlarrSettings != null)
		{
			String url = String.format("%s/api/v1/indexer", prowlarrSettings.getUrl());

			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset= utf-8");
			headers.put("X-Api-Key", prowlarrSettings.getApiKey());

			String response = httpRequestService.doGetRequest(url, headers);
			if(StringUtils.hasText(response))
			{
				logger.debug("Response from prowlarr : {}", response);

				indexRepository.deleteAll();

				Map<Integer, String> prowlarrTags = tags(prowlarrSettings);
				Index[] indexes = objectMapper.readValue(response, Index[].class);
				String category = prowlarrSettings.getCategory();
				Arrays.stream(indexes)
						.filter(Index::isEnable)
						.filter(index -> {
							if (!StringUtils.hasText(category)) return true;

							if (index.getTags() == null || index.getTags().length == 0) return false;

							return Arrays.stream(index.getTags())
										.filter(prowlarrTags::containsKey)
										.anyMatch(indexTag-> prowlarrTags.get(indexTag).contains(category));

						})
						.forEach(indexRepository::save);
			}
		}
		logger.info("Indexes sync finish!");
    }

	@Cacheable("ProwlarrSearchResult")
    public SearchResult[] search(String searchTerm) throws Exception
	{
		HashMap<String, Integer> priority = PriorityHelper.getPriority();

		ConnectionSettings prowlarrSettings = settingsHelper.getConnectionSettings(NetworkService.PROWLARR);
		if (prowlarrSettings != null)
		{
			List<Index> indexes = indexRepository.findAll();

			List<String> indexerIds = indexes.stream()
					.filter(Index::isEnable)
					.filter(index -> priority.containsKey(index.getProtocol()) && priority.get(index.getProtocol())!=0)
					.map(index -> String.valueOf(index.getId()))
					.toList();

			String url = String.format("%s/api/v1/search/", prowlarrSettings.getUrl());
			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
			uriBuilder.queryParam("query", searchTerm);
			uriBuilder.queryParam("type", "search");
			for (String category : Constants.SEARCH_CATEGORY.split(","))
			{
				uriBuilder.queryParam("categories", category.trim());
			}
			uriBuilder.queryParam("indexerIds", indexerIds.toArray());
			uriBuilder.queryParam("limit", Constants.QUERY_LIMIT);
			uriBuilder.queryParam("offset", 0);

			URI uri = uriBuilder.build().toUri();

			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset= utf-8");
			headers.put("X-Api-Key", prowlarrSettings.getApiKey());

			String response = httpRequestService.doGetRequest(uri, headers);

			logger.debug("Search response {}", response);

			return objectMapper.readValue(response, SearchResult[].class);
		}

		return new SearchResult[]{};
    }

	public Map<Integer, String> tags(ConnectionSettings prowlarrSettings) throws Exception
	{
		logger.info("Fetching Tags");

		Map<Integer, String> tags = new HashMap<>();

		String url = String.format("%s/api/v1/tag", prowlarrSettings.getUrl());

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json; charset= utf-8");
		headers.put("X-Api-Key", prowlarrSettings.getApiKey());

		String response = httpRequestService.doGetRequest(url, headers);
		if(StringUtils.hasText(response))
		{
			logger.debug("Response from prowlarr : {}", response);
			Tag[] items = objectMapper.readValue(response, Tag[].class);
			tags = Arrays.stream(items)
					.collect(Collectors.toMap(
							Tag::getId,
							Tag::getLabel
					));
		}
		logger.info("Tags fetch!");
		return tags;
	}
}

