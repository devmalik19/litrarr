package devmalik19.litrarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.*;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.LibraryFilter;
import devmalik19.litrarr.helper.SortingHelper;
import devmalik19.litrarr.helper.StringHelper;
import devmalik19.litrarr.repository.ItemRepository;
import devmalik19.litrarr.repository.LibraryFilterRepository;
import devmalik19.litrarr.repository.LibraryRepository;
import devmalik19.litrarr.service.metadata.MetaDataService;
import jakarta.persistence.EntityNotFoundException;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LibraryService
{
	private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);

	private final FileSystemService fileSystemService;
	private final MetaDataService metaDataService;
	private final LibraryRepository libraryRepository;
	private final ItemRepository itemRepository;
	private final LibraryFilterRepository libraryFilterRepository;

	public LibraryService(FileSystemService fileSystemService,
						  MetaDataService metaDataService,
						  LibraryRepository libraryRepository,
						  ItemRepository itemRepository,
						  LibraryFilterRepository libraryFilterRepository)
	{
		this.fileSystemService = fileSystemService;
		this.metaDataService = metaDataService;
		this.libraryRepository = libraryRepository;
		this.itemRepository = itemRepository;
		this.libraryFilterRepository = libraryFilterRepository;
	}

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
				if (!library.isMetadataFetched())
				{
					try
					{
						metaDataService.getMetaForLibrary(library, file);
						library.setMetadataFetched(true);
						libraryRepository.save(library);
					}
					catch (Exception e)
					{
						logger.error("Metadata fetch failed for {}: {}", library.getPath(), e.getMessage());
					}
				}
				savedDirectories.put(path, library);
			}
			else
			{
				logger.info("Scanning files {} in directory {}", path, parentPath);
				String extension = StringUtils.getFilenameExtension(path.toString());
				if(!FileTypes.isMatch(extension))
					return;

				Library parentLibrary = (parentPath != null && savedDirectories.containsKey(parentPath))
					? savedDirectories.get(parentPath) : null;

				// For COMICS/MANGA, try to match against existing metadata-created items
				Item item = itemRepository.findByPath(path.toString()).orElse(null);
				if (item == null && parentLibrary != null
					&& (category == Category.COMICS || category == Category.MANGA))
				{
					item = findMatchingMetadataItem(parentLibrary, path);
				}
				if (item == null)
					item = new Item();

				item.setName(path.getFileName().toString());
				item.setPath(path.toString());
				item.setMissing(false);

				if (parentLibrary != null)
					item.setLibrary(parentLibrary);

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
				return fileSystem.getPathMatcher("glob:" + filter.getPath() + "{,/**}");
			})
			.toList();
	}

	/**
	 * Tries to find an existing metadata-created Item (missing, no path) in the given library
	 * that matches the scanned file by comparing issue/volume numbers in the filename.
	 */
	private Item findMatchingMetadataItem(Library library, Path filePath)
	{
		String fileName = filePath.getFileName().toString().toLowerCase();
		List<Item> metadataItems = itemRepository.findByLibraryAndMissingTrue(library);

		for (Item metadataItem : metadataItems)
		{
			if (metadataItem.getPath() != null)
				continue;

			String itemName = metadataItem.getName().toLowerCase();
			String itemNumber = StringHelper.extractNumber(itemName);
			String fileNumber = StringHelper.extractNumber(fileName);

			if (itemNumber != null && itemNumber.equals(fileNumber))
				return metadataItem;
		}
		return null;
	}

	public  List<Library> getLibrary(FolderType type, Category category, String sort, String search)
	{
		List<Library> library = libraryRepository.findByTypeAndCategory(type, category);
		if (StringUtils.hasText(search))
		{
			library = library.stream()
				.filter(lib -> lib.getName().toLowerCase().contains(search.toLowerCase()))
				.toList();
		}
		library = new ArrayList<>(library);
		sortLibrary(library, sort);
		return library;
	}

	private void sortLibrary(List<Library> library, String sort)
	{
		switch (sort)
		{
			case "name" -> library.sort(Comparator.comparing(Library::getName, SortingHelper.naturalOrder()));
			case "directory" -> library.sort(Comparator.comparing(
				lib -> Path.of(lib.getPath()).getParent().getFileName().toString(),
				SortingHelper.naturalOrder()
			));
		}
	}

	public List<Library> getSortedChildren(Library library, String sort, String search)
	{
		List<Library> children = new ArrayList<>(library.getLibraryList());
		if (StringUtils.hasText(search))
		{
			children = new ArrayList<>(children.stream()
				.filter(lib -> lib.getName().toLowerCase().contains(search.toLowerCase()))
				.toList());
		}
		sortLibrary(children, sort);
		return children;
	}

	public List<Item> getFilteredItems(Library library, String search)
	{
		List<Item> items = library.getItemList();
		if (StringUtils.hasText(search))
		{
			items = items.stream()
				.filter(item -> item.getName().toLowerCase().contains(search.toLowerCase()))
				.toList();
		}
		return items;
	}

	public Library findById(Integer id)
	{
		return libraryRepository.findById(id).orElse(new Library());
	}

	public void refreshMetadata(Integer id)
	{
		Library library = libraryRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Library not found: " + id));
		library.setMetadataFetched(false);
		libraryRepository.save(library);
		try
		{
			metaDataService.getMetaForLibrary(library, Path.of(library.getPath()));
			library.setMetadataFetched(true);
			libraryRepository.save(library);
		}
		catch (Exception e)
		{
			logger.error("Metadata refresh failed for {}: {}", library.getPath(), e.getMessage());
			throw e;
		}
	}

	@Transactional
	public int resetMetadataFlagsByCategory(Category category)
	{
		return libraryRepository.resetMetadataFlagsByCategory(category);
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
