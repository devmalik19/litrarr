package devmalik19.litrarr.service.plugins;

import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.repository.SearchRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PluginsService
{
	private static final Logger logger = LoggerFactory.getLogger(PluginsService.class);

	private final Map<String, PluginHandler> handlersByService;
	private final Map<String, PluginHandler> handlersByPending;
	private final List<PluginHandler> handlers;
	private final SearchRepository searchRepository;

	public PluginsService(List<PluginHandler> handlers, SearchRepository searchRepository)
	{
		this.handlers = handlers;
		this.searchRepository = searchRepository;

		this.handlersByService = handlers.stream()
			.collect(Collectors.toMap(PluginHandler::getServiceName, Function.identity()));

		this.handlersByPending = handlers.stream()
			.collect(Collectors.toMap(PluginHandler::getPendingServiceName, Function.identity()));
	}

	/** All registered plugin service names (for priority configuration) */
	public List<String> getServiceNames()
	{
		return handlers.stream().map(PluginHandler::getServiceName).toList();
	}

	public String check(String key, ConnectionSettings connectionSettings)
	{
		PluginHandler handler = handlersByService.get(key);
		return handler != null ? handler.checkConnection(connectionSettings) : null;
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForServices()
	{
		HashMap<String, ConnectionSettings> settings = new HashMap<>();
		for (PluginHandler handler : handlers)
		{
			settings.put(handler.getServiceName(), handler.getConnectionSettings());
		}
		return settings;
	}

	/**
	 * Tries each registered plugin in order until one successfully submits a search.
	 */
	public boolean search(Search search) throws Exception
	{
		for (PluginHandler handler : handlers)
		{
			if (handler.search(search))
			{
				searchRepository.save(search);
				return true;
			}
		}
		return false;
	}

	/**
	 * Routes download checks to the correct plugin based on the service name in DownloadState.
	 */
	public void checkDownloads(Search search) throws Exception
	{
		if (search.getData() == null) return;

		String service = search.getData().getService();

		PluginHandler handler = handlersByPending.get(service);
		if (handler != null)
		{
			handler.checkSearchAndDownload(search);
			return;
		}

		handler = handlersByService.get(service);
		if (handler != null)
		{
			handler.checkDownloads(search);
		}
	}
}
