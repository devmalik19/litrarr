package devmalik19.litrarr.service.metadata;

import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.PaginationHelper;
import devmalik19.litrarr.repository.ItemRepository;
import devmalik19.litrarr.repository.LibraryRepository;
import devmalik19.litrarr.service.FileSystemService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetaDataService
{
	private static final Logger logger = LoggerFactory.getLogger(MetaDataService.class);

	private final ComicVineService comicVineService;
	private final GoogleBookService googleBookService;
	private final MyAnimeListService myAnimeListService;
	private final FileSystemService fileSystemService;
	private final LibraryRepository libraryRepository;
	private final ItemRepository itemRepository;

	public MetaDataService(ComicVineService comicVineService,
						   GoogleBookService googleBookService,
						   MyAnimeListService myAnimeListService,
						   FileSystemService fileSystemService,
						   LibraryRepository libraryRepository,
						   ItemRepository itemRepository)
	{
		this.comicVineService = comicVineService;
		this.googleBookService = googleBookService;
		this.myAnimeListService = myAnimeListService;
		this.fileSystemService = fileSystemService;
		this.libraryRepository = libraryRepository;
		this.itemRepository = itemRepository;
	}

	public void getMetaForLibrary(Library library, Path file)
	{
		Path imagePath = fileSystemService.findLibraryImage(file);
		if (imagePath != null && Files.exists(imagePath))
		{
			String name = String.valueOf(library.getId());
			String extension = StringUtils.getFilenameExtension(imagePath.toString());
			String imageFileName = name + "." + extension;
			fileSystemService.copyImageToCache(imagePath, "library", imageFileName);
			library.setImage(imageFileName);
		}
		else
		{
			switch (library.getCategory())
			{
				case BOOKS:
				case AUDIOBOOKS:
					googleBookService.getMetaForLibrary(library);
					break;

				case COMICS:
					comicVineService.getMetaForLibrary(library);
					saveComicIssues(library);
					break;

				case MANGA:
					myAnimeListService.getMetaForLibrary(library);
					saveMangaVolumes(library);
					break;
			}
		}
		libraryRepository.save(library);
	}

	/**
	 * Fetches and saves comic issues from ComicVine for the given library (comic series).
	 * Searches for the series name to get the volume ID, then fetches all issues.
	 */
	private void saveComicIssues(Library library)
	{
		try
		{
			// Skip if issues already exist for this library
			if (!itemRepository.findByLibrary(library).isEmpty())
				return;

			List<MetadataResult> results = comicVineService.search(library.getName());
			if (results.isEmpty())
				return;

			MetadataResult first = results.get(0);
			if (!StringUtils.hasText(first.getSourceId()))
				return;

			List<Item> issues = comicVineService.getIssuesForVolume(first.getSourceId(), library);
			if (!issues.isEmpty())
			{
				itemRepository.saveAll(issues);
				logger.info("Saved {} comic issues for library '{}'", issues.size(), library.getName());
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to save comic issues for library '{}': {}", library.getName(), e.getMessage());
		}
	}

	/**
	 * Fetches and saves manga volumes from MyAnimeList for the given library (manga series).
	 * Searches for the series name to get the MAL ID, then creates volume entries.
	 */
	private void saveMangaVolumes(Library library)
	{
		try
		{
			// Skip if volumes already exist for this library
			if (!itemRepository.findByLibrary(library).isEmpty())
				return;

			List<MetadataResult> results = myAnimeListService.search(library.getName());
			if (results.isEmpty())
				return;

			MetadataResult first = results.get(0);
			if (!StringUtils.hasText(first.getSourceId()))
				return;

			List<Item> volumes = myAnimeListService.getVolumesForManga(first.getSourceId(), library);
			if (!volumes.isEmpty())
			{
				itemRepository.saveAll(volumes);
				logger.info("Saved {} manga volumes for library '{}'", volumes.size(), library.getName());
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to save manga volumes for library '{}': {}", library.getName(), e.getMessage());
		}
	}

	public void getMetaForItem(Item item)
	{
	}

	/**
	 * Saves issues/volumes from a metadata search result when a user clicks download.
	 * Uses the sourceId from the MetadataResult to fetch issues from the appropriate provider.
	 * If a library is linked, issues are associated with it; otherwise they are saved without a parent.
	 */
	public void saveIssuesFromSearch(MetadataResult metadataResult)
	{
		try
		{
			String sourceId = metadataResult.getSourceId();
			if (!StringUtils.hasText(sourceId))
				return;

			Library parentLibrary = null;
			if (metadataResult.getLibrary() != null)
				parentLibrary = libraryRepository.findById(metadataResult.getLibrary()).orElse(null);

			// Skip if issues already exist for this library
			if (parentLibrary != null && !itemRepository.findByLibrary(parentLibrary).isEmpty())
				return;

			List<Item> items;
			switch (metadataResult.getCategory())
			{
				case COMICS:
					items = comicVineService.getIssuesForVolume(sourceId, parentLibrary);
					break;
				case MANGA:
					items = myAnimeListService.getVolumesForManga(sourceId, parentLibrary);
					break;
				default:
					return;
			}

			if (!items.isEmpty())
			{
				itemRepository.saveAll(items);
				logger.info("Saved {} issues from search for '{}'", items.size(), metadataResult.getTitle());
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to save issues from search for '{}': {}", metadataResult.getTitle(), e.getMessage());
		}
	}

	public Page<MetadataResult> search(String title, String author, String publisher, Pageable pageable)
	{
		List<String> parts = new ArrayList<>();
		if (StringUtils.hasText(author)) parts.add(author);
		if (StringUtils.hasText(publisher)) parts.add(publisher);
		parts.add(title);
		String query = String.join(" ", parts);

		List<MetadataResult> metadataResult = searchAll(query);
		return PaginationHelper.prepareResults(metadataResult, pageable);
	}

	private List<MetadataResult> searchAll(String query)
	{
		List<MetadataResult> results = new ArrayList<>();
		tryAddResults(results, () -> googleBookService.search(query));
		tryAddResults(results, () -> comicVineService.search(query));
		tryAddResults(results, () -> myAnimeListService.search(query));
		return results;
	}

	private void tryAddResults(List<MetadataResult> results, Supplier<List<MetadataResult>> supplier)
	{
		try
		{
			List<MetadataResult> providerResults = supplier.get();
			if (providerResults != null)
			{
				results.addAll(providerResults);
			}
		}
		catch (Exception e)
		{
			logger.error("Metadata provider search failed: {}", e.getMessage());
		}
	}
}
