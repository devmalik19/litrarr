package devmalik19.litrarr.service.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.SettingsHelper;
import devmalik19.litrarr.service.HttpRequestService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlskdService
{
	private static final Logger logger = LoggerFactory.getLogger(SlskdService.class);

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

	public String checkConnection(ConnectionSettings connectionSettings)
	{
		Map<String, String> headers = buildHeaders(connectionSettings);
		return httpRequestService.doGetRequest(
			String.format("%s/api/v0/session/enabled", connectionSettings.getUrl()), headers);
	}

	public void checkDownloads(Search search) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(PluginsService.SLSKD);
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

	public record SearchResult(String username, String fullPath, long fileSize) {}

	public List<SearchResult> search(String search) throws Exception
	{
		List<SearchResult> searchResultList = new ArrayList<>();

		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(PluginsService.SLSKD);
		if (connectionSettings == null) return searchResultList;

		Map<String, String> headers = buildHeaders(connectionSettings);
		String json = "{\"SearchText\": \"" + search + "\"}";
		String response = httpRequestService.doPostRequest(
			String.format("%s/api/v0/searches", connectionSettings.getUrl()), json, headers);
		JsonNode jsonNode = objectMapper.readTree(response);
		String searchId = jsonNode.get("id").asText();

		boolean isComplete = false;
		int maxAttempts = 10;
		int attempts = 0;
		do
		{
			TimeUnit.SECONDS.sleep(60);
			response = httpRequestService.doGetRequest(
				String.format("%s/api/v0/searches/%s", connectionSettings.getUrl(), searchId), headers);
			jsonNode = objectMapper.readTree(response);
			if (jsonNode.has("isComplete"))
				isComplete = jsonNode.get("isComplete").asBoolean();

			attempts++;
		}
		while (!isComplete && attempts < maxAttempts);

		if (isComplete)
		{
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
					searchResultList.add(new SearchResult(username, filename, size));
				}
			}
		}

		return searchResultList;
	}

	public DownloadState download(String username, String fullPath, long fileSize) throws Exception
	{
		DownloadState downloadState = new DownloadState();

		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(PluginsService.SLSKD);
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
			downloadState.setService(PluginsService.SLSKD);
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
