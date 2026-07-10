package devmalik19.litrarr.service.plugins;

import devmalik19.litrarr.helper.SearchHelper;
import devmalik19.litrarr.helper.SettingsHelper;
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
import org.springframework.stereotype.Service;

@Service
public class PluginsService
{
	private static final Logger logger = LoggerFactory.getLogger(PluginsService.class);

	public static final String SLSKD = "slskd";
	public static final List<String> services = List.of(SLSKD);

	private final SlskdService slskdService;
	private final SettingsHelper settingsHelper;
	private final SearchRepository searchRepository;

	public PluginsService(SlskdService slskdService,
						  SettingsHelper settingsHelper,
						  SearchRepository searchRepository)
	{
		this.slskdService = slskdService;
		this.settingsHelper = settingsHelper;
		this.searchRepository = searchRepository;
	}


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
		connectionSettingsList.put(SLSKD, settingsHelper.getConnectionSettingsOrDefault(SLSKD));
		return connectionSettingsList;
	}

	public boolean search(Search search) throws Exception
	{
		return SearchHelper.progressiveSearch(search, searchRepository, this::search);
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
				downloadState = slskdService.download(result.username(), result.fullPath(), result.fileSize());
				break;
			}
		}
		return downloadState;
	}

	public void checkDownloads(Search search) throws Exception
	{
		if(Objects.equals(search.getData().getService(), PluginsService.SLSKD))
			slskdService.checkDownloads(search);
	}
}
