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
	private final HardcoverService hardcoverService;
	private final AudnexusService audnexusService;
	private final MyAnimeListService myAnimeListService;
	private final FileSystemService fileSystemService;
	private final LibraryRepository libraryRepository;
	private final ItemRepository itemRepository;

	public MetaDataService(ComicVineService comicVineService,
						   HardcoverService hardcoverService,
						   AudnexusService audnexusService,
						   MyAnimeListService myAnimeListService,
						   FileSystemService fileSystemService,
						   LibraryRepository libraryRepository,
						   ItemRepository itemRepository)
	{
		this.comicVineService = comicVineService;
		this.hardcoverService = hardcoverService;
		this.audnexusService = audnexusService;
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
libraryRepository.save(library);
}
}
	public void getMetaForItem(Item item)
	{
	}

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

		Map<Integer, String> imageUrls = new HashMap<>();
		for (Item issue : fetched)
			if (issue.getIssueNumber() != null && issue.getImageUrl() != null)
			imageUrls.putIfAbsent(issue.getIssueNumber(), issue.getImageUrl());
		List<Item> withImages = new ArrayList<>();
		for (Item item : itemRepository.findByLibrary(library))
		{
			if (StringUtils.hasText(item.getImage()))
				continue;
			String url = item.getImageUrl() != null ? item.getImageUrl() : imageUrls.get(item.getIssueNumber());
			if (!StringUtils.hasText(url))
				continue;
			String fileName = fileSystemService.downloadImageToCache(url, "library", "item-" + item.getId());
			if (fileName != null)
			{
				item.setImage(fileName);
				withImages.add(item);
			}
		}
		itemRepository.saveAll(withImages);
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

	private List<MetadataResult> searchAll(String query, Category category)
	{
		List<MetadataResult> results = new ArrayList<>();
		switch (category)
		{
			case BOOKS -> tryAddResults(results, () -> hardcoverService.search(query));
			case AUDIOBOOKS -> tryAddResults(results, () -> audnexusService.search(query));
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
