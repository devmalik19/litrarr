package devmalik19.litrarr.service.thirdparty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Protocol;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.helper.FilesHelper;
import devmalik19.litrarr.helper.ServiceProvider;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NetworkService implements ServiceProvider
{
	Logger logger = LoggerFactory.getLogger(NetworkService.class);

	public static final String PROWLARR = "prowlarr";
	public static final String QBITTORRENT = "qbt";
	public static final String SABNZBD = "sabnzbd";

	public static final List<String> services = List.of(Protocol.TORRENT.name(), Protocol.USENET.name());

	@Autowired
	private ProwlarrService prowlarrService;

	@Autowired
	private QbittorrentService qbittorrentService;

	@Autowired
	private SabnzbdService sabnzbdService;

	@Autowired
	private ObjectMapper objectMapper;


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

	@Override

	public boolean search(String query) throws Exception
	{
		logger.info("Searching network services");

		boolean isSuccess = false;
		SearchResult[] results = prowlarrService.search(query);

		logger.info("{} results found from prowlarr for {}", results.length, query);

		Comparator<SearchResult> comparator = Comparator
			.comparingInt(SearchResult::getSeeders).reversed()
			.thenComparingInt(SearchResult::getLeechers).reversed();

		String value = Settings.store.get(Keys.PRIORITY);
		HashMap<String, Integer> priority = objectMapper.readValue(value, new TypeReference<HashMap<String, Integer>>() {});
		if(priority.get(Protocol.TORRENT.name())<priority.get(Protocol.USENET.name()))
			results = Arrays.stream(results).sorted(Comparator.comparing(SearchResult::getProtocol).thenComparing(comparator)).toArray(SearchResult[]::new);
		else
			results = Arrays.stream(results).sorted(Comparator.comparing(SearchResult::getProtocol).reversed().thenComparing(comparator)).toArray(SearchResult[]::new);

		for(SearchResult result: results)
		{
			logger.debug("Matching {} with search results {}", query, result.getTitle());
			if(FilesHelper.isMatch(query, result.getTitle()))
			{
				logger.info("Match successfully, adding to download {}", result.getTitle());
				if(Protocol.isTorrent(result.getProtocol()))
					qbittorrentService.addTorrent(result.getGuid());
				else
					sabnzbdService.addNzb(result.getGuid());
				isSuccess = true;
				break;
			}
		}
		return isSuccess;
	}

	public ConnectionSettings getConnectionsSettingsForIndexes()
	{
		String value = Settings.store.get(PROWLARR);
		try
		{
			return objectMapper.readValue(value, ConnectionSettings.class);
		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage());
		}
		return new ConnectionSettings();
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForClients()
	{
		HashMap<String, ConnectionSettings> connectionSettingsList = new HashMap<>();
		try
		{
			String value = Settings.store.get(QBITTORRENT);
			if(value!=null)
				connectionSettingsList.put(QBITTORRENT, objectMapper.readValue(value, ConnectionSettings.class));
			else
				connectionSettingsList.put(QBITTORRENT, new ConnectionSettings());

			value = Settings.store.get(SABNZBD);
			if(value!=null)
				connectionSettingsList.put(SABNZBD, objectMapper.readValue(value, ConnectionSettings.class));
			else
				connectionSettingsList.put(SABNZBD,  new ConnectionSettings());

		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage());
		}
		return connectionSettingsList;
	}
}
