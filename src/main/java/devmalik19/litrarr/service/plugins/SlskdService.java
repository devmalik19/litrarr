package devmalik19.litrarr.service.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.service.HttpRequestService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlskdService
{
    Logger logger = LoggerFactory.getLogger(SlskdService.class);

    @Autowired
    private HttpRequestService httpRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    public String checkConnection(ConnectionSettings connectionSettings)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", connectionSettings.getApiKey());
        return httpRequestService.doGetRequest(String.format("%s/api/v0/session/enabled",connectionSettings.getUrl()));
    }

    public void checkDownloads(Search search) {
    }

    public record SearchResult(String username, String fullPath, long fileSize){}
    public List<SearchResult> search(String search) throws Exception
    {
		List<SearchResult> searchResultList = new ArrayList<>();

		String value = Settings.store.get(PluginsService.SLSKD);
		if (value!=null)
		{
            ConnectionSettings connectionSettings = objectMapper.readValue(value, ConnectionSettings.class);
            if(connectionSettings != null)
            {
				Map<String, String> headers = new HashMap<>();
				headers.put("X-Api-Key", connectionSettings.getApiKey());
				headers.put("Content-Type", "application/json");
				String json = "{\"SearchText\": \""+search+"\"}";
               	String response = httpRequestService.doPostRequest(String.format("%s/api/v0/searches", connectionSettings.getUrl()), json, headers);
				JsonNode jsonNode = objectMapper.readTree(response);
				String searchId = jsonNode.get("id").asText();

				boolean isComplete = false;
				int maxAttempts = 10;
				int attempts = 0;
				do
				{
					TimeUnit.SECONDS.sleep(60);
					response = httpRequestService.doGetRequest(
						String.format("%s/api/v0/searches/%s", connectionSettings.getUrl(),
							searchId), headers);
					jsonNode = objectMapper.readTree(response);
					if (jsonNode.has("isComplete"))
						isComplete = jsonNode.get("isComplete").asBoolean();

					attempts++;
				}
				while(!isComplete && attempts < maxAttempts);

				if(isComplete)
				{
					response = httpRequestService.doGetRequest(String.format("%s/api/v0/searches/%s/responses", connectionSettings.getUrl(), searchId), headers);
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
            }
        }
        return searchResultList;
    }

	public void download(String username, String fullPath, long fileSize)
		throws Exception
	{
		String value = Settings.store.get(PluginsService.SLSKD);
		if (value!=null)
		{
			ConnectionSettings connectionSettings = objectMapper.readValue(value, ConnectionSettings.class);
			if (connectionSettings != null)
			{
				Map<String, String> headers = new HashMap<>();
				headers.put("X-Api-Key", connectionSettings.getApiKey());
				headers.put("Content-Type", "application/json");
				try
				{
					String jsonBody = String.format("[{\"filename\": \"%s\", \"size\": %d}]",
						fullPath.replace("\\", "\\\\"), // Escape backslashes for JSON
						fileSize);

					String uri = String.format("%s/api/v0/transfers/downloads/%s", connectionSettings.getUrl(), username);
					String response = httpRequestService.doPostRequest(uri, jsonBody, headers);
					logger.info("Download enqueued for {}: {}", username, response);
				}
				catch (Exception e)
				{
					logger.error("Failed to enqueue download", e);
				}
			}
		}
	}
}
