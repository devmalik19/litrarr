package devmalik19.litrarr.helper;

import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadState;
import devmalik19.litrarr.repository.SearchRepository;

public class SearchHelper {

	@FunctionalInterface
	public interface QueryExecutor {
		DownloadState execute(String query) throws Exception;
	}

	public static boolean progressiveSearch(Search search, SearchRepository searchRepository, QueryExecutor executor) throws Exception {
		DownloadState downloadState = executor.execute(search.getAuthor() + " " + search.getTitle() + " " + search.getYear());

		if (downloadState.isEmpty())
			downloadState = executor.execute(search.getAuthor() + " " + search.getTitle());

		if (downloadState.isEmpty())
			downloadState = executor.execute(search.getTitle());

		boolean isSuccess = !downloadState.isEmpty();

		if (isSuccess)
			search.setData(downloadState);
		search.setStatus(isSuccess ? SearchStatus.DOWNLOADING : SearchStatus.NOTFOUND);
		searchRepository.save(search);

		return isSuccess;
	}
}
