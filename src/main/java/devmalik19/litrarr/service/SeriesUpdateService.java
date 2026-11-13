package devmalik19.litrarr.service;

import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.data.dao.Item;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.repository.LibraryRepository;
import devmalik19.litrarr.service.metadata.MetaDataService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeriesUpdateService
{
	private static final Logger logger = LoggerFactory.getLogger(SeriesUpdateService.class);

	private static final int CHECK_INTERVAL_DAYS = 7;

	private static final long PROVIDER_DELAY_MS = 1000;

	private final LibraryRepository libraryRepository;
	private final MetaDataService metaDataService;
	private final SearchService searchService;

	public SeriesUpdateService(LibraryRepository libraryRepository,
							   MetaDataService metaDataService,
							   SearchService searchService)
	{
		this.libraryRepository = libraryRepository;
		this.metaDataService = metaDataService;
		this.searchService = searchService;
	}

	public void checkAllSeries()
	{
		logger.info("Checking ongoing series for new issues");
		List<Library> series = new ArrayList<>();
		series.addAll(libraryRepository.findByTypeAndCategory(FolderType.TITLE, Category.COMICS));
		series.addAll(libraryRepository.findByTypeAndCategory(FolderType.TITLE, Category.MANGA));

		LocalDate today = LocalDate.now();
		int checked = 0;
		for (Library library : series)
		{
			try
			{
				if (!isDue(library, today))
					continue;
				if (checked++ > 0)
					Thread.sleep(PROVIDER_DELAY_MS);
				checkSeries(library, today);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return;
			}
			catch (Exception e)
			{
				logger.error("New issue check failed for '{}': {}", library.getName(), e.getMessage());
			}
		}
		logger.info("Series check finished, {} series checked", checked);
	}

	private boolean isDue(Library library, LocalDate today)
	{
		if (library.isComplete() || !StringUtils.hasText(library.getGuid()))
			return false;

		LocalDate lastChecked = library.getLastChecked();
		return lastChecked == null || !lastChecked.plusDays(CHECK_INTERVAL_DAYS).isAfter(today);
	}

	private void checkSeries(Library library, LocalDate today)
	{
		List<Item> fetched = metaDataService.fetchIssues(library);
		if (fetched.isEmpty())
		{

			logger.debug("No issues returned for '{}'", library.getName());
			return;
		}

		List<Item> newItems = metaDataService.syncIssues(library, fetched);

		library.setLastChecked(today);
		libraryRepository.save(library);

		if (newItems.isEmpty())
			return;

		logger.info("Found {} new issues for '{}'", newItems.size(), library.getName());

		Search search = searchService.prepareMissingSearch(library.getId());
		if (search != null)
			searchService.runSearch(search);
	}
}
