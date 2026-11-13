package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.constants.Constants;
import devmalik19.litrarr.constants.FileTypes;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.helper.IssuePathHelper;
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
	private final ComicConverterService comicConverterService;

	public DownloadService(ItemRepository itemRepository, ComicConverterService comicConverterService)
	{
		this.itemRepository = itemRepository;
		this.comicConverterService = comicConverterService;
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
	 * Each issue file is moved to {series}/#{issue number, zero padded}/{issue title or #number}.{ext}
	 * and matched to an existing Item record if possible.
	 */
	private void processComicDownload(Search search, Path downloadDir) throws IOException
	{
		Library library = search.getLibrary();
		Path libraryPath = Paths.get(library.getPath());

		List<Path> downloadedFiles = findDownloadFiles(downloadDir, search.getData().getIdentifier());
		if (downloadedFiles.isEmpty())
		{
			logger.warn("No supported files found for download '{}' in {}", search.getData().getIdentifier(), downloadDir);
			return;
		}

		logger.info("Processing {} comic/manga files for '{}'", downloadedFiles.size(), search.getTitle());

		// Get existing metadata items for this library to match against
		List<Item> metadataItems = itemRepository.findByLibraryAndMissingTrue(library);

		for (Path downloaded : downloadedFiles)
		{
			// CBR files are converted to CBZ before they are moved into the library
			Path file = comicConverterService.toCbz(downloaded);
			String fileName = file.getFileName().toString();
			Integer issueNumber = IssuePathHelper.parseIssueNumber(StringHelper.extractNumber(fileName));

			if (issueNumber == null)
			{
				// No number found, move to library root
				Path target = libraryPath.resolve(fileName);
				Files.createDirectories(target.getParent());
				Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
				logger.info("Moved '{}' to library root (no issue number detected)", fileName);
				continue;
			}

			// Try to match to an existing metadata Item, its name is the issue title (or #number)
			Item matchedItem = findMatchingItem(metadataItems, issueNumber);
			String title = matchedItem != null ? matchedItem.getName() : null;

			// Move to {series}/#001/{title or #001}.{ext}
			String extension = StringUtils.getFilenameExtension(fileName);
			Path target = IssuePathHelper.issueFile(libraryPath, issueNumber, title, extension);
			Files.createDirectories(target.getParent());
			Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Moved '{}' to '{}'", fileName, target);

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
				newItem.setName(target.getFileName().toString());
				newItem.setIssueNumber(issueNumber);
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
		Path file = findFile(findDownloadFiles(downloadDir, search.getData().getIdentifier()), search.getTitle());
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
	 * Finds the supported files that belong to this download only.
	 * The identifier is the release name the download client knows (torrent name, NZB name or,
	 * for Soulseek, the remote file path). A torrent or NZB is saved in a folder with that name,
	 * a single file download is saved as a file with that name.
	 */
	private List<Path> findDownloadFiles(Path downloadDir, String identifier) throws IOException
	{
		if (!StringUtils.hasText(identifier))
			return List.of();

		// Soulseek identifiers are remote paths, only the file name is relevant
		String name = identifier.substring(Math.max(identifier.lastIndexOf('/'), identifier.lastIndexOf('\\')) + 1);
		String nameWithoutNzb = name.replaceAll("(?i)\\.nzb$", "");

		try (Stream<Path> files = Files.walk(downloadDir))
		{
			return files
				.filter(Files::isRegularFile)
				.filter(this::isSupportedFile)
				.filter(file -> belongsToDownload(downloadDir.relativize(file), name, nameWithoutNzb))
				.toList();
		}
	}

	private boolean belongsToDownload(Path relativePath, String name, String nameWithoutNzb)
	{
		for (Path part : relativePath)
		{
			String partName = part.toString();
			if (partName.equalsIgnoreCase(name) || partName.equalsIgnoreCase(nameWithoutNzb))
				return true;
		}
		// Single file download named after the release without its file extension
		String fileName = relativePath.getFileName().toString();
		String baseName = StringUtils.stripFilenameExtension(fileName);
		return baseName.equalsIgnoreCase(name) || baseName.equalsIgnoreCase(nameWithoutNzb);
	}

	/**
	 * Matches a downloaded file's issue number against metadata Items.
	 */
	private Item findMatchingItem(List<Item> metadataItems, Integer issueNumber)
	{
		for (Item item : metadataItems)
		{
			if (issueNumber.equals(item.getIssueNumber()))
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
	 * Picks the file matching the search title from the files of this download,
	 * or the only file if there is just one.
	 */
	private Path findFile(List<Path> downloadFiles, String title)
	{
		for (Path file : downloadFiles)
		{
			String nameWithoutExt = StringUtils.stripFilenameExtension(file.getFileName().toString());
			if (devmalik19.litrarr.helper.FilesHelper.isMatch(title, nameWithoutExt))
				return file;
		}
		return downloadFiles.size() == 1 ? downloadFiles.get(0) : null;
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
