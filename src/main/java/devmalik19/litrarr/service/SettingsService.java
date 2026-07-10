package devmalik19.litrarr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Index;
import devmalik19.litrarr.data.dao.Setting;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.helper.PriorityHelper;
import devmalik19.litrarr.repository.IndexRepository;
import devmalik19.litrarr.repository.SettingsRepository;
import devmalik19.litrarr.service.plugins.PluginsService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SettingsService
{
	private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

	private final SettingsRepository settingsRepository;
	private final IndexRepository indexRepository;
	private final ObjectMapper objectMapper;
	private final NetworkService networkService;
	private final PluginsService pluginsService;

	public SettingsService(SettingsRepository settingsRepository,
						   IndexRepository indexRepository,
						   ObjectMapper objectMapper,
						   NetworkService networkService,
						   PluginsService pluginsService)
	{
		this.settingsRepository = settingsRepository;
		this.indexRepository = indexRepository;
		this.objectMapper = objectMapper;
		this.networkService = networkService;
		this.pluginsService = pluginsService;
	}

	public <T> void save(String key, T value)
	{
		Setting setting = new Setting();
		setting.setKey(key);

		try
		{
			setting.setValue(objectMapper.writeValueAsString(value));
		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage());
		}
		settingsRepository.save(setting);
		List<Setting> settingList = settingsRepository.findAll();
		settingList.forEach(item -> Settings.store.put(item.getKey(), item.getValue()));
	}

	public <T> T get(String key, Class<T> type) throws Exception
	{
		Optional<Setting> settings = settingsRepository.findById(key);
		if (settings.isPresent())
		{
			try
			{
				return objectMapper.readValue(settings.get().getValue(), type);
			}
			catch (Exception e)
			{
				logger.error(e.getLocalizedMessage());
			}
		}
		return type.getConstructor().newInstance();
	}

	public void update(int id, String status)
	{
		Optional<Index> optionalIndex = indexRepository.findById(id);
		optionalIndex.ifPresent(index -> {
			index.setEnable(Boolean.parseBoolean(status));
			indexRepository.save(index);
			logger.info("Index {} updated to enable={}", id, status);
		});
	}

	public void reloadSettings()
	{
		List<Setting> settingList = settingsRepository.findAll();
		settingList.forEach(item -> Settings.store.put(item.getKey(), item.getValue()));
	}

	public void sync() throws Exception
	{
		networkService.sync();
	}

	public String checkNetworkConnection(String key, ConnectionSettings connectionSettings)
	{
		return networkService.check(key, connectionSettings);
	}

	public String checkPluginConnection(String key, ConnectionSettings connectionSettings)
	{
		return pluginsService.check(key, connectionSettings);
	}

	public ConnectionSettings getConnectionsSettingsForIndexes()
	{
		return networkService.getConnectionsSettingsForIndexes();
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForClients()
	{
		return networkService.getConnectionsSettingsForClients();
	}

	public HashMap<String, ConnectionSettings> getConnectionsSettingsForServices()
	{
		return pluginsService.getConnectionsSettingsForServices();
	}

	public List<String> getServices()
	{
		HashMap<String, Integer> priority = PriorityHelper.getPriority();

		return priority.entrySet().stream()
			.filter(e -> e.getValue() != 0)
			.sorted(Entry.comparingByValue())
			.map(Entry::getKey).toList();
	}

	public List<String> getDisabledServices()
	{
		HashMap<String, Integer> priority = PriorityHelper.getPriority();

		return priority.entrySet().stream()
			.filter(e -> e.getValue() == 0)
			.map(Entry::getKey).toList();
	}
}
