package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Constants;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileSystemService
{
	Logger logger = LoggerFactory.getLogger(FileSystemService.class);

	public List<Path> scanRoot(String path)
    {
		Path start = Paths.get(path);
		try (Stream<Path> stream = Files.walk(start))
		{
			return stream.skip(1).collect(Collectors.toList());
		}
		catch (NoSuchFileException e)
		{
			logger.error("This path does not exists ! Skipping - {}", path);
			return Collections.emptyList();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
			return Collections.emptyList();
		}
    }

	public Path findLibraryImage(Path file)
	{
		Path imagePath = null;
		for (String ext : Constants.IMAGE_TYPES)
		{
			Path p = file.resolve("folder" + ext);
			if (Files.exists(p))
			{
				imagePath = p;
				break;
			}
		}
		return imagePath;
	}

	public void copyImageToCache(Path file, String location, String fileName)
	{
		try
		{
			Path targetDirectory = Path.of(Constants.CACHE_PATH).resolve(location);
			Files.createDirectories(targetDirectory);
			Path targetFile = targetDirectory.resolve(fileName);
			Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			logger.info(e.getLocalizedMessage());
		}
	}

	public void checkCacheDirectory() throws Exception
	{
		Path cachePath = Paths.get(Constants.CONFIG_PATH+"/cache");
		Files.createDirectories(cachePath);
	}

	public void setSkipPatterns(List<String> userPatterns) throws Exception
	{
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
