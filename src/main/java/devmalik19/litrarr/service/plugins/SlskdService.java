package devmalik19.litrarr.service.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.FilesHelper;
import devmalik19.litrarr.helper.SettingsHelper;
import devmalik19.litrarr.service.HttpRequestService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SlskdService implements PluginHandler
{
	private static final Logger logger = LoggerFactory.getLogger(SlskdService.class);

	private static final String SERVICE_NAME = "slskd";
	private static final String PENDING_NAME = "slskd_pending";

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final SettingsHelper settingsHelper;

	public SlskdService(HttpRequestService httpRequestService,
						ObjectMapper objectMapper,
						SettingsHelper settingsHelper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.settingsHelper = settingsHelper;
	}

	@Override
	public String getServiceName()
	{
		return SERVICE_NAME;
	}

	@Override
	public String getPendingServiceName()
	{
		return PENDING_NAME;
	}

	@Override
	public String checkConnection(ConnectionSettings connectionSettings)
	{
		Map<String, String> headers = buildHeaders(connectionSettings);
		String response = httpRequestService.doGetRequest(
			String.format("%s/api/v0/session/enabled", connectionSettings.getUrl()), headers);
		if (!StringUtils.hasText(response))
		{
			throw new RuntimeException("Authentication failed: invalid API key");
		}
		return response;
	}

	@Override
	public ConnectionSettings getConnectionSettings()
	{
		return settingsHelper.getConnectionSettingsOrDefault(SERVICE_NAME);
	}

	@Override
	public boolean search(Search search) throws Exception
	{
		String query = search.getAuthor() + " " + search.getTitle();
		String searchId = submitSearch(query);

		if (searchId == null)
			return false;

		DownloadState downloadState = new DownloadState();
		downloadState.setService(PENDING_NAME);
		downloadState.setIdentifier(searchId);
		search.setData(downloadState);
		search.setStatus(SearchStatus.DOWNLOADING);

		logger.info("Slskd search submitted for '{}', will poll for results", query);
		return true;
	}

	@Override
	public boolean checkSearchAndDownload(Search search) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(SERVICE_NAME);
		if (connectionSettings == null) return false;

		DownloadState state = search.getData();
		String searchId = state.getIdentifier();
		String query = search.getAuthor() + " " + search.getTitle();

		Map<String, String> headers = buildHeaders(connectionSettings);

		String response = httpRequestService.doGetRequest(
			String.format("%s/api/v0/searches/%s", connectionSettings.getUrl(), searchId), headers);

		if (!StringUtils.hasText(response))
			return false;

		JsonNode jsonNode = objectMapper.readTree(response);
		if (!jsonNode.has("isComplete") || !jsonNode.get("isComplete").asBoolean())
		{
			logger.info("Slskd search {} still in progress", searchId);
			return false;
		}

		response = httpRequestService.doGetRequest(
			String.format("%s/api/v0/searches/%s/responses", connectionSettings.getUrl(), searchId), headers);

		jsonNode = objectMapper.readTree(response);
		for (JsonNode userResponse : jsonNode)
		{
			String username = userResponse.get("username").asText();
			JsonNode files = userResponse.get("files");
			for (JsonNode file : files)
			{
				String filename = file.get("filename").asText();
				long size = file.get("size").asLong();

				if (FilesHelper.isMatch(query, filename))
				{
					logger.info("Slskd match found, downloading: {}", filename);
					DownloadState downloadState = download(username, filename, size);
					if (!downloadState.isEmpty())
					{
						search.setData(downloadState);
						return true;
					}
				}
			}
		}

		logger.info("Slskd search {} complete but no matching file found for '{}'", searchId, query);
		return false;
	}

	@Override
	public void checkDownloads(Search search) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(SERVICE_NAME);
		if (connectionSettings == null) return;

		Map<String, String> headers = buildHeaders(connectionSettings);
		String response = httpRequestService.doGetRequest(
			String.format("%s/api/v0/transfers/downloads", connectionSettings.getUrl()), headers);

		DownloadState state = search.getData();
		JsonNode users = objectMapper.readTree(response);
		boolean found = false;
		boolean finished = false;

		for (JsonNode userNode : users)
		{
			JsonNode directories = userNode.get("directories");
			if (directories == null) continue;
			for (JsonNode dir : directories)
			{
				JsonNode files = dir.get("files");
				if (files == null) continue;
				for (JsonNode file : files)
				{
					if (file.get("filename").asText().equals(state.getIdentifier()))
					{
						found = true;
						String status = file.get("state").asText();
						if ("Completed".equalsIgnoreCase(status))
						{
							finished = true;
						}
						break;
					}
				}
			}
		}

		if (finished)
		{
			logger.info("Slskd download finished for: {}", state.getIdentifier());
			search.setStatus(SearchStatus.COMPLETED);
		}
		else if (!found)
		{
			logger.warn("Slskd download not found in queue. It may have been cleared or failed.");
		}
	}

	private String submitSearch(String query) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(SERVICE_NAME);
		if (connectionSettings == null) return null;

		Map<String, String> headers = buildHeaders(connectionSettings);
		String json = "{\"SearchText\": \"" + query + "\"}";
		String response = httpRequestService.doPostRequest(
			String.format("%s/api/v0/searches", connectionSettings.getUrl()), json, headers);

		JsonNode jsonNode = objectMapper.readTree(response);
		String searchId = jsonNode.get("id").asText();
		logger.info("Slskd search submitted with id: {} for query: {}", searchId, query);
		return searchId;
	}

	private DownloadState download(String username, String fullPath, long fileSize) throws Exception
	{
		DownloadState downloadState = new DownloadState();

		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(SERVICE_NAME);
		if (connectionSettings == null) return downloadState;

		Map<String, String> headers = buildHeaders(connectionSettings);
		try
		{
			String jsonBody = String.format("[{\"filename\": \"%s\", \"size\": %d}]",
				fullPath.replace("\\", "\\\\"),
				fileSize);

			String uri = String.format("%s/api/v0/transfers/downloads/%s", connectionSettings.getUrl(), username);
			String response = httpRequestService.doPostRequest(uri, jsonBody, headers);
			downloadState.setDownloadPath(connectionSettings.getCategory());
			downloadState.setService(SERVICE_NAME);
			downloadState.setIdentifier(fullPath);
			logger.info("Download enqueued for {}: {}", username, response);
		}
		catch (Exception e)
		{
			logger.error("Failed to enqueue download", e);
		}

		return downloadState;
	}

	private Map<String, String> buildHeaders(ConnectionSettings connectionSettings)
	{
		Map<String, String> headers = new HashMap<>();
		headers.put("X-Api-Key", connectionSettings.getApiKey());
		headers.put("Content-Type", "application/json");
		return headers;
	}
}
