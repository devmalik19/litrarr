package devmalik19.litrarr.lifecycle;

import devmalik19.litrarr.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks
{
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

	private final SearchService searchService;

	public ScheduledTasks(SearchService searchService)
	{
		this.searchService = searchService;
	}

	@Scheduled(cron = "0 0 6 * * *")
	public void scheduledSearch() throws Exception
	{
		logger.info("Starting scheduled search!");
		searchService.reset();
		searchService.triggerSearch();
		logger.info("Scheduled search ended!");
	}

	@Scheduled(cron = "0 */10 * * * *")
	public void checkDownloads()
	{
		logger.info("Starting download check!");
		searchService.checkDownloads();
		logger.info("Download check ended!");
	}
}
