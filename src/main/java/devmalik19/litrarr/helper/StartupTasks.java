package devmalik19.litrarr.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Constants;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Setting;
import devmalik19.litrarr.repository.SettingsRepository;
import devmalik19.litrarr.service.LibraryService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
import jakarta.annotation.PostConstruct;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;
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

	public StartupTasks(NetworkService networkService,
		SettingsRepository settingsRepository,
		LibraryService libraryService)
	{
		this.networkService = networkService;
		this.settingsRepository = settingsRepository;
		this.libraryService = libraryService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void run() throws Exception
	{
		checkCache();
		setSkipPatterns();
		networkService.sync();
		libraryService.scan();
	}

	@PostConstruct
	public void loadSettings()
	{
		List<Setting> settingList = settingsRepository.findAll();
		settingList.forEach(setting -> Settings.store.put(setting.getKey(),setting.getValue()));
	}

	private void checkCache() throws Exception
	{
		Path cachePath = Paths.get(Constants.CONFIG_PATH+"/cache");
		Files.createDirectories(cachePath);
	}

	private void setSkipPatterns() throws Exception
	{
		List<String> userPatterns = new ArrayList<>();
		String patterns = Settings.store.get(Keys.PATTERNS);
		if(StringUtils.hasText(patterns))
			userPatterns = objectMapper.readValue(patterns, new TypeReference<List<String>>() {});

		List<String> systemPatterns  = List.of(
			"glob:**/.*",
			"glob:**/System Volume Information/**",
			"glob:**/$RECYCLE.BIN/**"
		);

		FileSystem fileSystem = FileSystems.getDefault();
		Constants.pathMatcherList =
			Stream.concat(
				systemPatterns.stream(),
				userPatterns.stream().map(p -> "glob:**/" + p + "{,/**}")
			)
				.distinct()
				.map(fileSystem::getPathMatcher)
			.toList();
	}
}
