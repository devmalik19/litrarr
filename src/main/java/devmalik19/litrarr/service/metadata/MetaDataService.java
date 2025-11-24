package devmalik19.litrarr.service.metadata;

import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.repository.LibraryRepository;
import devmalik19.litrarr.service.FileSystemService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetaDataService
{
	@Autowired
	private SampleService sampleService;

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
		sampleService.getMetaForLibrary(library);
		libraryRepository.save(library);
    }

	public void getMetaForItem(Item item)
	{
		sampleService.getMetaForItem(item);
	}
}
