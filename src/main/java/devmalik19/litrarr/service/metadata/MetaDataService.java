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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Picks up a cover image placed in the folder (for example folder.jpg).
 * Provider metadata is never guessed here, it is linked when a result is added to the library.
 */
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
libraryRepository.save(library);
}
}
	public void getMetaForItem(Item item)
	{
	}

	/**
	 * Fetches the current issues/volumes of a comic/manga series from its provider,
	 * using the provider ID stored in {@link Library#getGuid()}.
	 * Returns an empty list when the provider is not configured, fails or has no data.
	 */
	public List<Item> fetchIssues(Library library)
	{
		if (!StringUtils.hasText(library.getGuid()))
			return List.of();
		return switch (library.getCategory())
		{
			case COMICS -> comicVineService.getIssuesForVolume(library.getGuid(), library);
			case MANGA -> myAnimeListService.getVolumesForManga(library.getGuid(), library);
			default -> List.of();
		};
	}

/**
 * Merges the issues/volumes returned by the provider into the items of a series.
 * Items that already exist (from scanned files or an earlier fetch) are matched by issue number
 * and only get the provider ID and release date filled in. Everything else is saved as a missing item.
 *
 * @return the newly created items
 */
public List<Item> syncIssues(Library library, List<Item> fetched)
{
Map<Integer, Item> byNumber = new HashMap<>();
Map<String, Item> byGuid = new HashMap<>();
for (Item item : itemRepository.findByLibrary(library))
{
if (item.getIssueNumber() != null)
byNumber.putIfAbsent(item.getIssueNumber(), item);
if (item.getGuid() != null)
byGuid.putIfAbsent(item.getGuid(), item);
}

List<Item> newItems = new ArrayList<>();
List<Item> updated = new ArrayList<>();
for (Item issue : fetched)
{
Item existing = issue.getIssueNumber() != null
? byNumber.get(issue.getIssueNumber())
: byGuid.get(issue.getGuid());
if (existing == null)
{
newItems.add(issue);
continue;
}

boolean changed = false;
if (existing.getGuid() == null && issue.getGuid() != null)
{
existing.setGuid(issue.getGuid());
changed = true;
}
if (existing.getReleaseOn() == null && issue.getReleaseOn() != null)
{
existing.setReleaseOn(issue.getReleaseOn());
changed = true;
}
if (existing.getType() == null)
{
existing.setType(issue.getType());
changed = true;
}
if (changed)
updated.add(existing);
}

itemRepository.saveAll(updated);
itemRepository.saveAll(newItems);
return newItems;
}
	public Page<MetadataResult> search(String title, String author, String publisher, Category category, Pageable pageable)
	{
		List<String> parts = new ArrayList<>();
		if (StringUtils.hasText(author)) parts.add(author);
		if (StringUtils.hasText(publisher)) parts.add(publisher);
		parts.add(title);
		String query = String.join(" ", parts);

		List<MetadataResult> metadataResult = searchAll(query, category);
		return PaginationHelper.prepareResults(metadataResult, pageable);
	}

	/**
	 * Searches the provider matching the category.
	 * Books and audiobooks both use Google Books; results are tagged with the requested category.
	 */
	private List<MetadataResult> searchAll(String query, Category category)
	{
		List<MetadataResult> results = new ArrayList<>();
		switch (category)
		{
			case BOOKS, AUDIOBOOKS ->
			{
				tryAddResults(results, () -> googleBookService.search(query));
				results.forEach(r -> r.setCategory(category));
			}
			case COMICS -> tryAddResults(results, () -> comicVineService.search(query));
			case MANGA -> tryAddResults(results, () -> myAnimeListService.search(query));
		}
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
