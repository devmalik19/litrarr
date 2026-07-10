package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Constants;
import devmalik19.litrarr.constants.FileTypes;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DownloadService
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

	/**
	 * Processes a completed download: finds the file in the download directory
	 * and moves it to the target library folder.
	 */
	public void process(Search search)
	{
		try
		{
			DownloadState downloadState = search.getData();
			if (downloadState == null || downloadState.getDownloadPath() == null)
			{
				logger.warn("No download state/path for search id={}, skipping post-download.", search.getId());
				return;
			}

			Path downloadDir = resolveDownloadPath(downloadState.getDownloadPath());
			if (downloadDir == null || !Files.exists(downloadDir))
			{
				logger.warn("Download directory does not exist: {}", downloadDir);
				return;
			}

			Path file = findFile(downloadDir, search.getTitle());
			if (file == null)
			{
				logger.warn("Could not locate file for '{}' in {}", search.getTitle(), downloadDir);
				return;
			}

			logger.info("Found downloaded file: {}", file);

			// Move file to library folder
			Path targetPath = resolveTargetPath(search, file);
			if (targetPath != null)
			{
				Files.createDirectories(targetPath.getParent());
				Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
				logger.info("Moved '{}' to '{}'", file.getFileName(), targetPath);
			}
		}
		catch (Exception e)
		{
			logger.error("Post-download processing failed for search id={}: {}", search.getId(), e.getMessage(), e);
		}
	}

	/**
	 * Resolves the download path. The downloadPath from DownloadState is the category name.
	 * Files are expected under DOWNLOAD_PATH/category/ or directly in DOWNLOAD_PATH.
	 */
	private Path resolveDownloadPath(String category)
	{
		Path basePath = Paths.get(Constants.DOWNLOAD_PATH);
		if (StringUtils.hasText(category))
		{
			Path categoryPath = basePath.resolve(category);
			if (Files.exists(categoryPath))
				return categoryPath;
		}
		return basePath;
	}

	/**
	 * Searches for a file matching the search title in the download directory.
	 */
	private Path findFile(Path downloadDir, String title) throws IOException
	{
		try (Stream<Path> files = Files.walk(downloadDir))
		{
			List<Path> matchingFiles = files
				.filter(Files::isRegularFile)
				.filter(this::isSupportedFile)
				.toList();

			// Try matching by filename similarity
			for (Path file : matchingFiles)
			{
				String fileName = file.getFileName().toString();
				String nameWithoutExt = fileName.replaceAll("\\.\\w{3,5}$", "");
				if (devmalik19.litrarr.helper.FilesHelper.isMatch(title, nameWithoutExt))
				{
					return file;
				}
			}

			// If only one file exists in the directory, it's likely the right one
			if (matchingFiles.size() == 1)
			{
				return matchingFiles.get(0);
			}
		}
		return null;
	}

	private boolean isSupportedFile(Path path)
	{
		String extension = StringUtils.getFilenameExtension(path.toString());
		return FileTypes.isMatch(extension);
	}

	/**
	 * Resolves the target path in the library where the file should be moved.
	 */
	private Path resolveTargetPath(Search search, Path file)
	{
		if (search.getLibrary() == null || search.getLibrary().getPath() == null)
		{
			logger.warn("No library assigned for search id={}, cannot move file.", search.getId());
			return null;
		}

		Path libraryPath = Paths.get(search.getLibrary().getPath());
		return libraryPath.resolve(file.getFileName());
	}
}
