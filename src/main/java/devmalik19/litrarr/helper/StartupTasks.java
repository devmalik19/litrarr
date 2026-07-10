package devmalik19.litrarr.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Setting;
import devmalik19.litrarr.repository.SettingsRepository;
import devmalik19.litrarr.service.FileSystemService;
import devmalik19.litrarr.service.LibraryService;
import devmalik19.litrarr.service.SearchService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.util.StringUtils;

@Component
public class StartupTasks
{
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final NetworkService networkService;
	private final SettingsRepository settingsRepository;
	private final LibraryService libraryService;
	private final FileSystemService fileSystemService;
	private final SearchService searchService;

	public StartupTasks(NetworkService networkService,
		SettingsRepository settingsRepository,
		LibraryService libraryService,
		FileSystemService fileSystemService,
		SearchService searchService)
	{
		this.networkService = networkService;
		this.settingsRepository = settingsRepository;
		this.libraryService = libraryService;
		this.searchService = searchService;
		this.fileSystemService = fileSystemService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void run() throws Exception
	{
		fileSystemService.checkCacheDirectory();
		setSkipPatterns();
		searchService.setPriorityOrder();
		networkService.sync();
		libraryService.dbCleanUp();
		libraryService.scan();
	}

	@PostConstruct
	public void loadSettings()
	{
		List<Setting> settingList = settingsRepository.findAll();
		settingList.forEach(setting -> Settings.store.put(setting.getKey(),setting.getValue()));
	}

	private void setSkipPatterns() throws Exception
	{
		List<String> userPatterns = new ArrayList<>();
		String patterns = Settings.store.get(Keys.PATTERNS);
		if(StringUtils.hasText(patterns))
			userPatterns = objectMapper.readValue(patterns, new TypeReference<List<String>>() {});
		fileSystemService.setSkipPatterns(userPatterns);
	}
}
