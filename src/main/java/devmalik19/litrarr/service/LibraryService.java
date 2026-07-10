package devmalik19.litrarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.*;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.LibraryFilter;
import devmalik19.litrarr.repository.ItemRepository;
import devmalik19.litrarr.repository.LibraryFilterRepository;
import devmalik19.litrarr.repository.LibraryRepository;
import devmalik19.litrarr.service.metadata.MetaDataService;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LibraryService
{
	Logger logger = LoggerFactory.getLogger(LibraryService.class);

	@Autowired
	private FileSystemService fileSystemService;

	@Autowired
	private MetaDataService metaDataService;

	@Autowired
	private LibraryRepository libraryRepository;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private LibraryFilterRepository libraryFilterRepository;

	public void dbCleanUp() throws Exception
	{
		logger.info("Database cleanup started!");
		List<Library> libraryList = libraryRepository.findAll();
		List<Library> toDelete = new ArrayList<>();
		for(Library library:libraryList)
		{
			if(!Files.exists(Path.of(library.getPath())))
				toDelete.add(library);
		}
		if (!toDelete.isEmpty())
		{
			libraryRepository.deleteAllInBatch(toDelete);
			logger.info("Deleted {} missing records.", toDelete.size());
		}
		logger.info("Database cleanup complete!");
	}

	@SneakyThrows
	public void scan()
	{
		HashMap<Category, String> library = getLibrariesPath();
		library.forEach(this::scan);
	}

	private void scan(Category category, String libraryPath)
	{
		String root = Constants.LIBRARY_PATH + libraryPath;
		Map<Path, Library> savedDirectories = new HashMap<>();
		List<Path> filesList = fileSystemService.scanRoot(root);
		Path rootPath = Paths.get(root).toAbsolutePath().normalize();
		filesList.sort(Comparator.comparingInt(Path::getNameCount));
		List<PathMatcher> dbFilters = dbFilters();

		logger.info("Starting root scan with list of files/directories {}", filesList);

		filesList.forEach(file->{

			boolean isIgnored = Constants.pathMatcherList.stream().anyMatch(matcher -> matcher.matches(file));
			boolean isDbIgnored = dbFilters.stream().anyMatch(matcher -> matcher.matches(file));
			if (isIgnored || isDbIgnored)
			{
				logger.info("Skipping for file {}", file);
				return;
			}

			Path path = file.toAbsolutePath().normalize();
			Path parentPath = path.getParent();

			if(parentPath!=null && rootPath.equals(parentPath))
				parentPath=null;

			if(Files.isDirectory(file))
			{
				logger.info("Scanning directory {}", path);
				int depth = rootPath.relativize(path).getNameCount();

				Library library = libraryRepository.findByPath(path.toString()).orElse(new Library());
				library.setName(path.getFileName().toString());
				library.setPath(path.toString());
				library.setCategory(category);
				if (parentPath != null && savedDirectories.containsKey(parentPath))
				{
					library.setLibrary(savedDirectories.get(parentPath));
				}

				switch (category)
				{
					case BOOKS :
					case AUDIOBOOKS:
						if (depth == 1)
							library.setType(FolderType.AUTHOR);
						else if (depth == 2)
							library.setType(FolderType.BOOK);
					break;
					case COMICS:
					case MANGA:
						if (depth == 1)
							library.setType(FolderType.TITLE);
						else if (depth == 2)
							library.setType(FolderType.ISSUE_NUMBER);

					break;
				}

				library = libraryRepository.save(library);
				metaDataService.getMetaForLibrary(library, file);
				savedDirectories.put(path, library);
			}
			else
			{
				logger.info("Scanning files {} in directory {}", path, parentPath);
				String extension = StringUtils.getFilenameExtension(path.toString());
				if(!FileTypes.isMatch(extension))
					return;

				Item item = itemRepository.findByPath(path.toString()).orElse(new Item());
				item.setName(path.getFileName().toString());
				item.setPath(path.toString());

				if (parentPath != null && savedDirectories.containsKey(parentPath))
				{
					item.setLibrary(savedDirectories.get(parentPath));
				}

				item = itemRepository.save(item);
				metaDataService.getMetaForItem(item);
			}
		});
		logger.info("Root scan finish");
	}

	private List<PathMatcher> dbFilters()
	{
		List<LibraryFilter> dbFilters = libraryFilterRepository.findAll();
		FileSystem fileSystem = FileSystems.getDefault();
		return dbFilters.stream()
			.map(filter -> {
				return fileSystem.getPathMatcher("glob:" + filter + "{,/**}");
			})
			.toList();
	}

	public  List<Library> getLibrary(FolderType type, Category category)
	{
		return libraryRepository.findByTypeAndCategory(type, category);
	}

	public Library findById(Integer id)
	{
		return libraryRepository.findById(id).orElse(new Library());
	}

	public static HashMap<Category, String> getLibrariesPath() throws Exception
	{
		HashMap<Category, String> library = new HashMap<>();
		String paths = Settings.store.get(Keys.LIBRARY_PATHS);
		if(StringUtils.hasText(paths))
		{
			ObjectMapper objectMapper = new ObjectMapper();
			library = objectMapper.readValue(paths, new TypeReference<HashMap<Category, String>>(){});
		}
		return library;
	}
}
