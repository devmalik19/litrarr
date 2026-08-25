package devmalik19.litrarr.service.thirdparty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.SettingsHelper;
import devmalik19.litrarr.service.HttpRequestService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class QbittorrentService
{
	private static final Logger logger = LoggerFactory.getLogger(QbittorrentService.class);

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final SettingsHelper settingsHelper;

	private ResponseEntity<String> authResponse;

	public QbittorrentService(HttpRequestService httpRequestService,
							  ObjectMapper objectMapper,
							  SettingsHelper settingsHelper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.settingsHelper = settingsHelper;
	}

	public String checkConnection(ConnectionSettings connectionSettings)
	{
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		String body = String.format("username=%s&password=%s", connectionSettings.getUsername(), connectionSettings.getPassword());
		authResponse = httpRequestService.doPostRequestRaw(String.format("%s/api/v2/auth/login", connectionSettings.getUrl()), body, headers);
		String responseBody = authResponse.getBody();
		if (!"Ok.".equalsIgnoreCase(responseBody))
		{
			throw new RuntimeException("Authentication failed: invalid username or password");
		}
		return responseBody;
	}

	public DownloadState addTorrent(String url) throws Exception
	{
		DownloadState downloadState = new DownloadState();
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(NetworkService.QBITTORRENT);
		if (connectionSettings != null)
		{
			Map<String, String> headers = getAuthenticatedHeaders(connectionSettings);
			headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
			String body = String.format("urls=%s&category=%s", url, connectionSettings.getCategory());
			String response = httpRequestService.doPostRequest(String.format("%s/api/v2/torrents/add", connectionSettings.getUrl()), body, headers);
			downloadState.setDownloadPath(connectionSettings.getCategory());
			downloadState.setService(NetworkService.QBITTORRENT);
			logger.info("Download enqueued for {} {}", body, response);
		}
		return downloadState;
	}

	public void checkDownloads(Search search) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(NetworkService.QBITTORRENT);
		if (connectionSettings != null)
		{
			Map<String, String> headers = getAuthenticatedHeaders(connectionSettings);
			String url = String.format("%s/api/v2/torrents/info?category=%s",
				connectionSettings.getUrl(), connectionSettings.getCategory());

			String responseJson = httpRequestService.doGetRequest(url, headers);
			List<Map<String, Object>> torrents = objectMapper.readValue(responseJson, new TypeReference<List<Map<String, Object>>>() {});

			DownloadState downloadState = search.getData();
			for (Map<String, Object> torrent : torrents)
			{
				String name = (String) torrent.get("name");
				double progress = Double.parseDouble(torrent.get("progress").toString());
				String state = (String) torrent.get("state");

				if (name != null && name.equalsIgnoreCase(downloadState.getIdentifier()))
				{
					if (progress >= 1.0)
					{
						logger.info("Torrent '{}' is COMPLETE.", name);
						search.setStatus(SearchStatus.COMPLETED);
					}
					else if ("error".equalsIgnoreCase(state))
					{
						logger.warn("Torrent '{}' FAILED.", name);
						search.setStatus(SearchStatus.FAILED);
					}
					break;
				}
			}
		}
	}

	private Map<String, String> getAuthenticatedHeaders(ConnectionSettings connectionSettings)
	{
		if (authResponse == null)
			checkConnection(connectionSettings);

		HttpHeaders responseHeaders = authResponse.getHeaders();
		String cookies = responseHeaders.get("set-cookie").toString();
		String sid = cookies.replaceAll(".*SID=(.+?);.*", "$1");

		Map<String, String> headers = new HashMap<>();
		headers.put("Cookie", "SID=" + sid);
		return headers;
	}
}
