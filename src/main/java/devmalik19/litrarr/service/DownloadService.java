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
	private final AudiobookConverterService audiobookConverterService;

	public DownloadService(ItemRepository itemRepository,
						 ComicConverterService comicConverterService,
						 AudiobookConverterService audiobookConverterService)
	{
		this.itemRepository = itemRepository;
		this.comicConverterService = comicConverterService;
		this.audiobookConverterService = audiobookConverterService;
	}

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

			if (isComicOrManga(search) && search.getLibrary() != null)
			{
				processComicDownload(search, downloadDir);
			}
			else if (search.getCategory() == Category.AUDIOBOOKS && search.getLibrary() != null && search.getLibrary().getPath() != null)
			{
				processAudiobookDownload(search, downloadDir);
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

		List<Item> metadataItems = itemRepository.findByLibraryAndMissingTrue(library);

		for (Path downloaded : downloadedFiles)
		{

			Path file = comicConverterService.toCbz(downloaded);
			String fileName = file.getFileName().toString();
			Integer issueNumber = IssuePathHelper.parseIssueNumber(StringHelper.extractNumber(fileName));

			if (issueNumber == null)
			{

				Path target = libraryPath.resolve(fileName);
				Files.createDirectories(target.getParent());
				Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
				logger.info("Moved '{}' to library root (no issue number detected)", fileName);
				continue;
			}

			Item matchedItem = findMatchingItem(metadataItems, issueNumber);
			String title = matchedItem != null ? matchedItem.getName() : null;

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

		cleanEmptyDirectories(downloadDir);
	}

	private void processAudiobookDownload(Search search, Path downloadDir) throws IOException
	{
		Library library = search.getLibrary();
		List<Path> downloaded = AudiobookConverterService.naturalOrder(findDownloadFiles(downloadDir, search.getData().getIdentifier()));
		if (downloaded.size() < 2)
		{
			processSingleFileDownload(search, downloadDir);
			return;
		}

		Path libraryPath = Paths.get(library.getPath());
		Files.createDirectories(libraryPath);

		boolean allConvertible = downloaded.stream().allMatch(AudiobookConverterService::isConvertible);
		if (allConvertible && audiobookConverterService.isEnabledForDownloads())
		{
			Path target = libraryPath.resolve(library.getName() + ".m4b");
			if (audiobookConverterService.mergeToM4b(downloaded, target, library.getName()))
			{
				for (Path file : downloaded)
					Files.deleteIfExists(file);
				cleanEmptyDirectories(downloadDir);
				logger.info("Merged {} audio files into '{}'", downloaded.size(), target);
				return;
			}
		}

		for (Path file : downloaded)
		{
			Files.move(file, libraryPath.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			logger.info("Moved '{}' to '{}'", file.getFileName(), libraryPath);
		}
		cleanEmptyDirectories(downloadDir);
	}

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

	private List<Path> findDownloadFiles(Path downloadDir, String identifier) throws IOException
	{
		if (!StringUtils.hasText(identifier))
			return List.of();

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

		String fileName = relativePath.getFileName().toString();
		String baseName = StringUtils.stripFilenameExtension(fileName);
		return baseName.equalsIgnoreCase(name) || baseName.equalsIgnoreCase(nameWithoutNzb);
	}

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

	private void cleanEmptyDirectories(Path dir)
	{
		try (Stream<Path> paths = Files.walk(dir))
		{
			paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
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
