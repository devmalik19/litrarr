package devmalik19.litrarr.helper;

import devmalik19.litrarr.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class ScheduledTasks
{
	static Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

	@Autowired
	private SearchService searchService;

	@Scheduled(cron = "0 0 6 * * *")
	public void search() throws Exception
	{
		logger.info("Starting scheduled search!");
		searchService.reset();
		searchService.triggerSearch();
		logger.info("Scheduled search ended!");
	}
}
