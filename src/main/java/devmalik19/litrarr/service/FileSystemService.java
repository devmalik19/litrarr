package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

	/**
	 * Downloads an image from a URL and saves it to the cache directory.
	 * Returns the filename if successful, null otherwise.
	 */
	public String downloadImageToCache(String imageUrl, String location, String baseName)
	{
		if (!StringUtils.hasText(imageUrl))
			return null;

		try
		{
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(imageUrl))
				.GET()
				.build();

			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200)
			{
				logger.error("Failed to download image from '{}': HTTP {}", imageUrl, response.statusCode());
				return null;
			}

			String extension = guessImageExtension(imageUrl, response);
			String fileName = baseName + extension;

			Path targetDirectory = Path.of(Constants.CACHE_PATH).resolve(location);
			Files.createDirectories(targetDirectory);
			Path targetFile = targetDirectory.resolve(fileName);

			try (InputStream inputStream = response.body())
			{
				Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}

			return fileName;
		}
		catch (Exception e)
		{
			logger.error("Failed to download image from '{}': {}", imageUrl, e.getMessage());
			return null;
		}
	}

	private String guessImageExtension(String url, HttpResponse<?> response)
	{
		// Try content-type header first
		String contentType = response.headers().firstValue("content-type").orElse("");
		if (contentType.contains("png")) return ".png";
		if (contentType.contains("gif")) return ".gif";
		if (contentType.contains("webp")) return ".webp";
		if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";

		// Fall back to URL extension
		String path = url.toLowerCase();
		if (path.contains(".png")) return ".png";
		if (path.contains(".gif")) return ".gif";
		if (path.contains(".webp")) return ".webp";

		return ".jpg";
	}
}
