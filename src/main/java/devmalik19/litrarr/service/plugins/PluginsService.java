package devmalik19.litrarr.service.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.FilesHelper;
import devmalik19.litrarr.repository.SearchRepository;
import devmalik19.litrarr.service.plugins.SlskdService.SearchResult;
import java.util.HashMap;
import java.util.List;

import java.util.Objects;
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


	@Autowired
	private SearchRepository searchRepository;


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

	public boolean search(Search search) throws Exception
	{
		boolean isSuccess = false;
		DownloadState downloadState;

		downloadState = search(search.getTitle());

		if(downloadState.isEmpty())
			downloadState = search(search.getAuthor()  + " " + search.getTitle());
		else
			isSuccess = true;

		if(downloadState.isEmpty())
			downloadState = search(search.getAuthor()  + " " + search.getTitle()  + " " +search.getYear());
		else
			isSuccess = true;

		if(!downloadState.isEmpty())
			isSuccess = true;

		if(isSuccess)
			search.setData(downloadState);
		search.setStatus(isSuccess ? SearchStatus.DOWNLOADING: SearchStatus.NOTFOUND);
		searchRepository.save(search);

		return isSuccess;
	}


	public DownloadState search(String query) throws Exception
	{
		logger.info("Searching plugins services");

		DownloadState downloadState = new DownloadState();
		List<SearchResult> results = slskdService.search(query);

		logger.info("{} results found from slskdService", results.size());

		for(SearchResult result: results)
		{
			logger.debug("Matching {} with search results {}", query, result.fullPath());
			if(FilesHelper.isMatch(query, result.fullPath()))
			{
				logger.info("Match successfully, adding to download {}", result.fullPath());
				slskdService.download(result.username(), result.fullPath(), result.fileSize());
				break;
			}
		}
		return downloadState;
	}

	public void checkDownloads(Search search)
	{
		if(Objects.equals(search.getData().getService(), PluginsService.SLSKD))
			slskdService.checkDownloads(search);
	}
}
