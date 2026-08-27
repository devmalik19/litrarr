package devmalik19.litrarr.service.thirdparty;

import devmalik19.litrarr.constants.Protocol;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.helper.FilesHelper;
import devmalik19.litrarr.helper.PriorityHelper;
import devmalik19.litrarr.helper.SearchHelper;
import devmalik19.litrarr.helper.SettingsHelper;

import devmalik19.litrarr.repository.SearchRepository;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NetworkService
{
	private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

	public static final String PROWLARR = "prowlarr";
	public static final String QBITTORRENT = "qbt";
	public static final String SABNZBD = "sabnzbd";

	public static final List<String> services = List.of(Protocol.TORRENT.name(), Protocol.USENET.name());

	private final ProwlarrService prowlarrService;
	private final QbittorrentService qbittorrentService;
	private final SabnzbdService sabnzbdService;
	private final SettingsHelper settingsHelper;
	private final SearchRepository searchRepository;

	public NetworkService(ProwlarrService prowlarrService,
						  QbittorrentService qbittorrentService,
						  SabnzbdService sabnzbdService,
						  SettingsHelper settingsHelper,
						  SearchRepository searchRepository)
	{
		this.prowlarrService = prowlarrService;
		this.qbittorrentService = qbittorrentService;
		this.sabnzbdService = sabnzbdService;
		this.settingsHelper = settingsHelper;
		this.searchRepository = searchRepository;
	}



	public String check(String key, ConnectionSettings connectionSettings)
	{
		return switch (key) {
			case PROWLARR -> prowlarrService.checkConnection(connectionSettings);
			case QBITTORRENT -> qbittorrentService.checkConnection(connectionSettings);
			case SABNZBD -> sabnzbdService.checkConnection(connectionSettings);
			default -> null;
		};
	}

	public void sync() throws Exception
	{
		prowlarrService.sync();
	}

	public SearchResult[] getSearchResults(String searchTerm) throws Exception
	{
		return prowlarrService.search(searchTerm);
	}

	public void addToDownloadClients(DownloadRequest downloadRequest) throws Exception
	{
		if(Protocol.isTorrent(downloadRequest.getProtocol()))
			qbittorrentService.addTorrent(downloadRequest.getUrl());
		else
			sabnzbdService.addNzb(downloadRequest.getUrl());
	}

	public boolean search(Search search, Set<String> blocklist) throws Exception
	{
		return SearchHelper.progressiveSearch(search, searchRepository, query -> search(query, blocklist));
	}

	public boolean search(Search search) throws Exception
	{
		return search(search, Collections.emptySet());
	}

	public DownloadState search(String query, Set<String> blocklist) throws Exception
	{
		logger.info("Searching network services");

		DownloadState downloadState = new DownloadState();
		SearchResult[] results = prowlarrService.search(query);

		logger.info("{} results found from prowlarr for {}", results.length, query);

		Comparator<SearchResult> comparator = Comparator
			.<SearchResult>comparingInt(r -> r.getSeeders() + r.getLeechers()).reversed();

		HashMap<String, Integer> priority = PriorityHelper.getPriority();
		if(priority.getOrDefault(Protocol.TORRENT.name(), 0)<priority.getOrDefault(Protocol.USENET.name(), 0))
			results = Arrays.stream(results).sorted(Comparator.comparing(SearchResult::getProtocol).thenComparing(comparator)).toArray(SearchResult[]::new);
		else
			results = Arrays.stream(results).sorted(Comparator.comparing(SearchResult::getProtocol).reversed().thenComparing(comparator)).toArray(SearchResult[]::new);

		for(SearchResult result: results)
		{
			if (blocklist.contains(result.getTitle()))
			{
				logger.debug("Skipping blocklisted result: {}", result.getTitle());
				continue;
			}

			logger.debug("Matching {} with search results {}", query, result.getTitle());
			if(FilesHelper.isMatch(query, result.getTitle()))
			{
				logger.info("Match successfully, adding to download {}", result.getTitle());
				if(Protocol.isTorrent(result.getProtocol()))
					downloadState = qbittorrentService.addTorrent(result.getGuid());
				else
					downloadState = sabnzbdService.addNzb(result.getGuid());
				downloadState.setIdentifier(result.getTitle());
				break;
			}
		}
		return downloadState;
	}

	public DownloadState search(String query) throws Exception
	{
		return search(query, Collections.emptySet());
	}

	public ConnectionSettings getConnectionsSettingsForIndexes()
	{
		return settingsHelper.getConnectionSettingsOrDefault(PROWLARR);
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForClients()
	{
		HashMap<String, ConnectionSettings> connectionSettingsList = new HashMap<>();
		connectionSettingsList.put(QBITTORRENT, settingsHelper.getConnectionSettingsOrDefault(QBITTORRENT));
		connectionSettingsList.put(SABNZBD, settingsHelper.getConnectionSettingsOrDefault(SABNZBD));
		return connectionSettingsList;
	}

	public void checkDownloads(Search search) throws Exception
	{
		if(Objects.equals(search.getData().getService(), NetworkService.QBITTORRENT))
			qbittorrentService.checkDownloads(search);
		if(Objects.equals(search.getData().getService(), NetworkService.SABNZBD))
			sabnzbdService.checkDownloads(search);
	}
}
