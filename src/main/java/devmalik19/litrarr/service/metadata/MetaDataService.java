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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetaDataService
{
	@Autowired
	private ComicVineService comicVineService;

	@Autowired
	private GoogleBookService googleBookService;

	@Autowired
	private MyAnimeListService myAnimeListService;

	@Autowired
	private FileSystemService fileSystemService;

	@Autowired
	private LibraryRepository libraryRepository;

	public void getMetaForLibrary(Library library, Path file)
	{
		Path imagePath = fileSystemService.findLibraryImage(file);
		if (imagePath!=null && Files.exists(imagePath))
		{
			String name = String.valueOf(library.getId());
			String extension = StringUtils.getFilenameExtension(imagePath.toString());
			String imageFileName = name+"."+extension;
			fileSystemService.copyImageToCache(imagePath, "library", imageFileName);
			library.setImage(imageFileName);
		}
		else
		{
			switch (library.getCategory())
			{
				case BOOKS :
				case AUDIOBOOKS:
					googleBookService.getMetaForLibrary(library);
				break;

				case COMICS:
					comicVineService.getMetaForLibrary(library);
				break;

				case MANGA:
					myAnimeListService.getMetaForLibrary(library);
			}
		}
		libraryRepository.save(library);
    }

	public void getMetaForItem(Item item)
	{

	}

	public Page<MetadataResult> search(String title, String author, String publisher, Pageable pageable)
	{
		List<MetadataResult> metadataResult;
		boolean hasAuthor = StringUtils.hasText(author);
		boolean hasPublisher = StringUtils.hasText(publisher);

		if (hasAuthor && hasPublisher)
			metadataResult = searchSongByTitleAuthorPublisher(title, author, publisher);
		else if (hasAuthor)
			metadataResult = searchSongByTitleAuthor(title, author);
		else if (hasPublisher)
			metadataResult = searchByTitlePublisher(title, publisher);
		else
			metadataResult = searchByTitle(title);

		return PaginationHelper.prepareResults(metadataResult, pageable);
	}

	private List<MetadataResult> searchByTitlePublisher(String title, String publisher)
	{
		return new ArrayList<>();
	}

	private List<MetadataResult> searchSongByTitleAuthor(String title, String author)
	{
		return new ArrayList<>();
	}

	private List<MetadataResult> searchSongByTitleAuthorPublisher(String title, String author, String publisher)
	{
		return new ArrayList<>();
	}

	private List<MetadataResult> searchByTitle(String search)
	{
		return new ArrayList<>();
	}

}
