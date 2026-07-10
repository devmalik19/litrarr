package devmalik19.litrarr.service.metadata;

import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.PaginationHelper;
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

	public MetaDataService(ComicVineService comicVineService,
						   GoogleBookService googleBookService,
						   MyAnimeListService myAnimeListService,
						   FileSystemService fileSystemService,
						   LibraryRepository libraryRepository)
	{
		this.comicVineService = comicVineService;
		this.googleBookService = googleBookService;
		this.myAnimeListService = myAnimeListService;
		this.fileSystemService = fileSystemService;
		this.libraryRepository = libraryRepository;
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
					break;

				case MANGA:
					myAnimeListService.getMetaForLibrary(library);
					break;
			}
		}
		libraryRepository.save(library);
	}

	public void getMetaForItem(Item item)
	{
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
