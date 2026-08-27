package devmalik19.litrarr.service.thirdparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.SettingsHelper;
import devmalik19.litrarr.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SabnzbdService
{
	private static final Logger logger = LoggerFactory.getLogger(SabnzbdService.class);

	private final HttpRequestService httpRequestService;
	private final ObjectMapper objectMapper;
	private final SettingsHelper settingsHelper;

	public SabnzbdService(HttpRequestService httpRequestService,
						  ObjectMapper objectMapper,
						  SettingsHelper settingsHelper)
	{
		this.httpRequestService = httpRequestService;
		this.objectMapper = objectMapper;
		this.settingsHelper = settingsHelper;
	}

	public String checkConnection(ConnectionSettings connectionSettings)
	{
		String response = httpRequestService.doGetRequest(String.format("%s/api?mode=queue&output=json&apikey=%s", connectionSettings.getUrl(), connectionSettings.getApiKey()));
		if (response != null && response.contains("\"error\""))
		{
			throw new RuntimeException("API key incorrect");
		}
		return response;
	}

	public DownloadState addNzb(String url) throws Exception
	{
		DownloadState downloadState = new DownloadState();
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(NetworkService.SABNZBD);
		if (connectionSettings != null)
		{
			String response = httpRequestService.doGetRequest(String.format("%s/api?mode=addurl&output=json&apikey=%s&cat=%s&name=%s",
				connectionSettings.getUrl(), connectionSettings.getApiKey(), connectionSettings.getCategory(), url));

			if (response == null || response.isBlank())
			{
				logger.error("SABnzbd returned empty response when adding NZB: {}", url);
				return downloadState;
			}

			JsonNode root = objectMapper.readTree(response);
			boolean status = root.path("status").asBoolean(false);
			if (!status)
			{
				String error = root.path("error").asText("Unknown error");
				logger.error("SABnzbd rejected the NZB URL '{}': {}", url, error);
				return downloadState; // returns empty state → treated as not found
			}

			downloadState.setDownloadPath(connectionSettings.getCategory());
			downloadState.setService(NetworkService.SABNZBD);
			logger.info("Download enqueued for {} {}", url, response);
		}
		return downloadState;
	}

	public void checkDownloads(Search search) throws Exception
	{
		ConnectionSettings connectionSettings = settingsHelper.getConnectionSettings(NetworkService.SABNZBD);
		if (connectionSettings != null)
		{
			String response = httpRequestService.doGetRequest(String.format("%s/api?mode=history&output=json&apikey=%s",
				connectionSettings.getUrl(), connectionSettings.getApiKey()));

			if (response == null || response.isBlank())
			{
				logger.warn("Empty response from SABnzbd when checking downloads");
				return;
			}

			JsonNode root = objectMapper.readTree(response);
			JsonNode slots = root.path("history").path("slots");

			DownloadState downloadState = search.getData();
			boolean found = false;

			if (slots.isArray())
			{
				for (JsonNode slot : slots)
				{
					String nzbName = slot.path("nzb_name").asText();
					String status = slot.path("status").asText();

					if (nzbName.equalsIgnoreCase(downloadState.getIdentifier()))
					{
						found = true;
						if ("Completed".equalsIgnoreCase(status))
						{
							logger.info("Download for '{}' is COMPLETE.", nzbName);
							search.setStatus(SearchStatus.COMPLETED);
						}
						else if ("Failed".equalsIgnoreCase(status))
						{
							logger.warn("Download for '{}' FAILED.", nzbName);
							search.setStatus(SearchStatus.FAILED);
						}
						break;
					}
				}
			}

			if (!found)
			{
				// Also check the active queue before marking as failed
				String queueResponse = httpRequestService.doGetRequest(String.format("%s/api?mode=queue&output=json&apikey=%s",
					connectionSettings.getUrl(), connectionSettings.getApiKey()));

				if (queueResponse != null && !queueResponse.isBlank())
				{
					JsonNode queueRoot = objectMapper.readTree(queueResponse);
					JsonNode queueSlots = queueRoot.path("queue").path("slots");
					if (queueSlots.isArray())
					{
						for (JsonNode slot : queueSlots)
						{
							String filename = slot.path("filename").asText();
							if (filename.equalsIgnoreCase(downloadState.getIdentifier()))
							{
								found = true;
								break;
							}
						}
					}
				}

				if (!found)
				{
					logger.warn("NZB '{}' not found in SABnzbd history or queue. Marking as FAILED.", downloadState.getIdentifier());
					search.setStatus(SearchStatus.FAILED);
				}
			}

			logger.info("Download status check {}", response);
		}
	}
}
