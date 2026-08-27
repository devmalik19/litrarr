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
		String author = search.getAuthor();
		String title = search.getTitle();
		String year = search.getYear();

		DownloadState downloadState = executor.execute(StringHelper.buildQuery(author, title, year));

		if (downloadState.isEmpty())
			downloadState = executor.execute(StringHelper.buildQuery(author, title));

		if (downloadState.isEmpty())
			downloadState = executor.execute(StringHelper.buildQuery(title));

		boolean isSuccess = !downloadState.isEmpty();

		if (isSuccess)
			search.setData(downloadState);
		search.setStatus(isSuccess ? SearchStatus.DOWNLOADING : SearchStatus.NOTFOUND);
		searchRepository.save(search);

		return isSuccess;
	}
}
