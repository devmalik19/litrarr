package devmalik19.litrarr.service.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.helper.FilesHelper;
import devmalik19.litrarr.service.plugins.SlskdService.SearchResult;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluginsService
{
	Logger logger = LoggerFactory.getLogger(PluginsService.class);

	public static final String SLSKD = "slskd";
	public static final List<String> services = List.of(SLSKD);

	@Autowired
	private SlskdService slskdService;

	@Autowired
	private ObjectMapper objectMapper;

	public String check(String key, ConnectionSettings connectionSettings)
	{
		return switch (key) {
			case SLSKD -> slskdService.checkConnection(connectionSettings);
			default -> null;
		};
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForServices()
	{
		HashMap<String, ConnectionSettings> connectionSettingsList = new HashMap<>();
		try
		{
			String value = Settings.store.get(SLSKD);
			if(value!=null)
				connectionSettingsList.put(SLSKD, objectMapper.readValue(value, ConnectionSettings.class));
			else
				connectionSettingsList.put(SLSKD, new ConnectionSettings());
		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage());
		}
		return connectionSettingsList;
	}

	public boolean search(String query) throws Exception
	{
		logger.info("Searching plugins services");

		boolean isSuccess = false;
		List<SearchResult> results = slskdService.search(query);

		logger.info("{} results found from slskdService", results.size());

		for(SearchResult result: results)
		{
			logger.debug("Matching {} with search results {}", query, result.fullPath());
			if(FilesHelper.isMatch(query, result.fullPath()))
			{
				logger.info("Match successfully, adding to download {}", result.fullPath());
				slskdService.download(result.username(), result.fullPath(), result.fileSize());
				isSuccess = true;
				break;
			}
		}
		return isSuccess;
	}
}
