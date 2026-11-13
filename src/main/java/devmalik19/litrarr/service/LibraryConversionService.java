package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.repository.ItemRepository;
import devmalik19.litrarr.repository.LibraryRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LibraryConversionService
{
	private static final Logger logger = LoggerFactory.getLogger(LibraryConversionService.class);

	private final ItemRepository itemRepository;
	private final LibraryRepository libraryRepository;
	private final ComicConverterService comicConverterService;
	private final AudiobookConverterService audiobookConverterService;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public LibraryConversionService(ItemRepository itemRepository,
									LibraryRepository libraryRepository,
									ComicConverterService comicConverterService,
									AudiobookConverterService audiobookConverterService)
	{
		this.itemRepository = itemRepository;
		this.libraryRepository = libraryRepository;
		this.comicConverterService = comicConverterService;
		this.audiobookConverterService = audiobookConverterService;
	}

	public boolean isRunning()
	{
		return running.get();
	}

	public boolean tryStart()
	{
		return running.compareAndSet(false, true);
	}

	@Async
	public void convertLibrary(Integer libraryId)
	{
		try
		{
			Library library = libraryRepository.findById(libraryId).orElseThrow();
			logger.info("Converting files in '{}'", library.getName());
			int comics = 0;
			int audiobooks = 0;
			if (library.getCategory() == Category.COMICS || library.getCategory() == Category.MANGA)
				comics = convertComics(itemRepository.findByLibrary(library));
			else if (library.getCategory() == Category.AUDIOBOOKS)
				audiobooks = convertAudiobooks(library);
			logger.info("Conversion of '{}' finished: {} comic files and {} audiobooks converted", library.getName(), comics, audiobooks);
		}
		catch (Exception e)
		{
			logger.error("Converting files failed: {}", e.getMessage(), e);
		}
		finally
		{
			running.set(false);
		}
	}

	private int convertComics(List<Item> items)
	{
		int converted = 0;
		for (Item item : items)
		{
			String path = item.getPath();
			if (!StringUtils.hasText(path) || !path.toLowerCase().endsWith(".cbr"))
				continue;

			Path file = Path.of(path);
			if (!Files.exists(file))
				continue;

			Path result = comicConverterService.toCbz(file);
			if (result.equals(file))
				continue;

			String oldName = file.getFileName().toString();
			if (oldName.equals(item.getName()))
				item.setName(result.getFileName().toString());
			item.setPath(result.toString());
			itemRepository.save(item);
			converted++;
		}
		return converted;
	}

	private int convertAudiobooks(Library library)
	{
		if (!audiobookConverterService.isAvailable())
		{
			logger.info("ffmpeg not found, skipping audiobook conversion");
			return 0;
		}

		List<Library> books = library.getType() == FolderType.BOOK
			? List.of(library)
			: libraryRepository.findByTypeAndCategory(FolderType.BOOK, Category.AUDIOBOOKS).stream()
			.filter(book -> book.getLibrary() != null && library.getId().equals(book.getLibrary().getId()))
			.toList();

		int converted = 0;
		for (Library book : books)
		{
			try
			{
				if (convertAudiobook(book))
					converted++;
			}
			catch (Exception e)
			{
				logger.warn("Could not convert audiobook '{}': {}", book.getName(), e.getMessage());
			}
		}
		return converted;
	}
	private boolean convertAudiobook(Library book) throws IOException
	{
		List<Item> sources = itemRepository.findByLibrary(book).stream()
			.filter(item -> StringUtils.hasText(item.getPath()))
			.filter(item -> AudiobookConverterService.isConvertible(Path.of(item.getPath())))
			.filter(item -> Files.exists(Path.of(item.getPath())))
			.toList();
		if (sources.size() < 2)
			return false;

		Path target = Path.of(book.getPath()).resolve(book.getName() + ".m4b");
		if (Files.exists(target))
		{
			logger.warn("Not converting '{}': {} already exists", book.getName(), target.getFileName());
			return false;
		}

		List<Path> files = sources.stream().map(item -> Path.of(item.getPath())).toList();
		if (!audiobookConverterService.mergeToM4b(files, target, book.getName()))
			return false;

		for (Path file : files)
			Files.deleteIfExists(file);
		itemRepository.deleteAll(sources);

		Item merged = new Item();
		merged.setName(target.getFileName().toString());
		merged.setPath(target.toString());
		merged.setType(Category.AUDIOBOOKS);
		merged.setLibrary(book);
		merged.setMissing(false);
		itemRepository.save(merged);
		logger.info("Converted audiobook '{}' ({} files) to M4B", book.getName(), files.size());
		return true;
	}
}
