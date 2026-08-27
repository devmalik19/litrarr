package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.constants.Constants;
import devmalik19.litrarr.constants.FileTypes;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.StringHelper;
import devmalik19.litrarr.repository.ItemRepository;
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

	private final ItemRepository itemRepository;

	public DownloadService(ItemRepository itemRepository)
	{
		this.itemRepository = itemRepository;
	}

	/**
	 * Processes a completed download: finds the file(s) in the download directory
	 * and moves them to the target library folder.
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

			// For comics/manga with a linked library, use multi-file import
			if (isComicOrManga(search) && search.getLibrary() != null)
			{
				processComicDownload(search, downloadDir);
			}
			else
			{
				processSingleFileDownload(search, downloadDir);
			}
		}
		catch (Exception e)
		{
			logger.error("Post-download processing failed for search id={}: {}", search.getId(), e.getMessage(), e);
		}
	}

	/**
	 * Handles multi-file comic/manga downloads.
	 * Each issue file is moved to library.path/{issueNumber}/filename
	 * and matched to an existing Item record if possible.
	 */
	private void processComicDownload(Search search, Path downloadDir) throws IOException
	{
		Library library = search.getLibrary();
		Path libraryPath = Paths.get(library.getPath());

		List<Path> downloadedFiles = findAllSupportedFiles(downloadDir, search.getTitle());
		if (downloadedFiles.isEmpty())
		{
			logger.warn("No supported files found for comic '{}' in {}", search.getTitle(), downloadDir);
			return;
		}

		logger.info("Processing {} comic/manga files for '{}'", downloadedFiles.size(), search.getTitle());

		// Get existing metadata items for this library to match against
		List<Item> metadataItems = itemRepository.findByLibraryAndMissingTrue(library);

		for (Path file : downloadedFiles)
		{
			String fileName = file.getFileName().toString();
			String issueNumber = StringHelper.extractNumber(fileName);

			if (issueNumber == null)
			{
				// No number found, move to library root
				Path target = libraryPath.resolve(fileName);
				Files.createDirectories(target.getParent());
				Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
				logger.info("Moved '{}' to library root (no issue number detected)", fileName);
				continue;
			}

			// Move to library.path/{issueNumber}/filename
			Path issueDir = libraryPath.resolve(issueNumber);
			Files.createDirectories(issueDir);
			Path target = issueDir.resolve(fileName);
			Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Moved '{}' to '{}'", fileName, target);

			// Try to match to an existing metadata Item
			Item matchedItem = findMatchingItem(metadataItems, issueNumber);
			if (matchedItem != null)
			{
				matchedItem.setPath(target.toString());
				matchedItem.setMissing(false);
				itemRepository.save(matchedItem);
				metadataItems.remove(matchedItem);
				logger.debug("Matched issue {} to Item id={}", issueNumber, matchedItem.getId());
			}
			else
			{
				// No metadata item exists for this issue — create a new one
				Item newItem = new Item();
				newItem.setName(fileName);
				newItem.setPath(target.toString());
				newItem.setType(library.getCategory());
				newItem.setLibrary(library);
				newItem.setMissing(false);
				itemRepository.save(newItem);
				logger.debug("Created new Item for unmatched issue {}", issueNumber);
			}
		}

		// Clean up empty download subdirectories
		cleanEmptyDirectories(downloadDir);
	}

	/**
	 * Handles single-file downloads (books, audiobooks, or unlinked searches).
	 */
	private void processSingleFileDownload(Search search, Path downloadDir) throws IOException
	{
		Path file = findFile(downloadDir, search.getTitle());
		if (file == null)
		{
			logger.warn("Could not locate file for '{}' in {}", search.getTitle(), downloadDir);
			return;
		}

		logger.info("Found downloaded file: {}", file);

		Path targetPath = resolveTargetPath(search, file);
		if (targetPath != null)
		{
			Files.createDirectories(targetPath.getParent());
			Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Moved '{}' to '{}'", file.getFileName(), targetPath);
		}
	}

	/**
	 * Finds all supported files in the download directory that could belong to this download.
	 * Looks inside subdirectories as well (torrents often create a subfolder).
	 */
	private List<Path> findAllSupportedFiles(Path downloadDir, String title) throws IOException
	{
		try (Stream<Path> files = Files.walk(downloadDir))
		{
			List<Path> allFiles = files
				.filter(Files::isRegularFile)
				.filter(this::isSupportedFile)
				.toList();

			// If there's a subfolder matching the title, prefer files from it
			List<Path> matchingFiles = allFiles.stream()
				.filter(f -> {
					String parent = f.getParent().getFileName().toString();
					return devmalik19.litrarr.helper.FilesHelper.isMatch(title, parent);
				})
				.toList();

			if (!matchingFiles.isEmpty())
				return matchingFiles;

			// Otherwise return all supported files
			return allFiles;
		}
	}

	/**
	 * Matches a downloaded file's issue number against metadata Items.
	 */
	private Item findMatchingItem(List<Item> metadataItems, String issueNumber)
	{
		for (Item item : metadataItems)
		{
			String itemNumber = StringHelper.extractNumber(item.getName());
			if (itemNumber != null && itemNumber.equals(issueNumber))
				return item;
		}
		return null;
	}

	private boolean isComicOrManga(Search search)
	{
		return search.getCategory() == Category.COMICS || search.getCategory() == Category.MANGA;
	}

	/**
	 * Resolves the download path. The downloadPath from DownloadState is the category name.
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
	 * Searches for a single file matching the search title in the download directory.
	 */
	private Path findFile(Path downloadDir, String title) throws IOException
	{
		try (Stream<Path> files = Files.walk(downloadDir))
		{
			List<Path> matchingFiles = files
				.filter(Files::isRegularFile)
				.filter(this::isSupportedFile)
				.toList();

			for (Path file : matchingFiles)
			{
				String fileName = file.getFileName().toString();
				String nameWithoutExt = fileName.replaceAll("\\.\\w{3,5}$", "");
				if (devmalik19.litrarr.helper.FilesHelper.isMatch(title, nameWithoutExt))
				{
					return file;
				}
			}

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

	/**
	 * Removes empty directories left behind after moving files.
	 */
	private void cleanEmptyDirectories(Path dir)
	{
		try (Stream<Path> paths = Files.walk(dir))
		{
			paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()) // deepest first
				.filter(Files::isDirectory)
				.filter(p -> !p.equals(dir))
				.forEach(p -> {
					try
					{
						String[] contents = p.toFile().list();
						if (contents != null && contents.length == 0)
							Files.delete(p);
					}
					catch (IOException e)
					{
						logger.debug("Could not delete directory {}: {}", p, e.getMessage());
					}
				});
		}
		catch (IOException e)
		{
			logger.debug("Error cleaning empty directories in {}: {}", dir, e.getMessage());
		}
	}
}
