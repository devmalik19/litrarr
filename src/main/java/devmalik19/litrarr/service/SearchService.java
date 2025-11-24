package devmalik19.litrarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.repository.SearchRepository;
import devmalik19.litrarr.service.plugins.PluginsService;
import devmalik19.litrarr.service.thirdparty.NetworkService;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SearchService
{
	static Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private NetworkService networkService;

	@Autowired
	private PluginsService pluginsService;

	@Autowired
	private SearchRepository searchRepository;

	@Autowired
	private ObjectMapper objectMapper;


    public Page<SearchResult> search(String searchTerm, Pageable pageable) throws Exception
    {
		int pageSize = pageable.getPageSize();
		int currentPage = pageable.getPageNumber();
		int startItem = currentPage * pageSize;

		List<SearchResult> searchResults = Arrays.asList(networkService.getSearchResults(searchTerm));
		searchResults = sortResults(pageable, searchResults);

		List<SearchResult> pageContent;
		if (searchResults.size() < startItem)
		{
			pageContent = Collections.emptyList();
		}
		else
		{
			int toIndex = Math.min(startItem + pageSize, searchResults.size());
			pageContent = searchResults.subList(startItem, toIndex);
		}

		return new PageImpl<>(pageContent, PageRequest.of(currentPage, pageSize), searchResults.size());
    }

	private static List<SearchResult> sortResults(Pageable pageable, List<SearchResult> searchResults)
	{
		Sort sort = pageable.getSort();
		if (sort.isSorted() && !searchResults.isEmpty())
		{
			Sort.Order order = sort.iterator().next();
			String property = order.getProperty();

			Comparator<SearchResult> comparator = null;
			switch (property)
			{
				case "protocol": comparator = Comparator.comparing(SearchResult::getProtocol, Comparator.nullsLast(String::compareTo)); break;
				case "indexer":  comparator = Comparator.comparing(SearchResult::getIndexer, Comparator.nullsLast(String::compareTo));  break;
				case "title":    comparator = Comparator.comparing(SearchResult::getTitle, Comparator.nullsLast(String::compareTo));    break;
				case "seeders":  comparator = Comparator.comparingInt(SearchResult::getSeeders);                                      break;
				case "leechers": comparator = Comparator.comparingInt(SearchResult::getLeechers);                                      break;
			}

			if (comparator != null)
			{
				if (order.isDescending())
				{
					comparator = comparator.reversed();
				}
				searchResults = searchResults.stream().sorted(comparator).collect(Collectors.toList());
			}
		}
		return searchResults;
	}

	public void download(DownloadRequest downloadRequest) throws Exception
	{
		networkService.download(downloadRequest);
	}

	public void search() throws Exception
	{
		logger.info("Starting search engine!");
		String value = Settings.store.get(Keys.PRIORITY);
		HashMap<String, Integer> priority = objectMapper.readValue(value, new TypeReference<HashMap<String, Integer>>() {});

		List<Entry<String, Integer>> sortedEntries = priority.entrySet().stream()
			.filter(entry -> entry.getValue() != 0)
			.sorted(Entry.comparingByValue()).toList();
		
		List<Search> searchList = searchRepository.findAll();
		searchList.forEach(search -> {
			
			logger.info("Searching for {} with priority order {}", search.getQuery(), sortedEntries);
			boolean isSuccess;
			for (Map.Entry<String, Integer> entry : sortedEntries)
			{
				try
				{
					if(NetworkService.services.contains(entry.getKey()))
						isSuccess = networkService.search(search.getQuery());
					else
						isSuccess = pluginsService.search(search.getQuery());

					if(isSuccess)
						break;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					logger.debug(e.getLocalizedMessage());
				}
			}
			logger.info("Search for {} finish", search.getQuery());
		});
		logger.info("Search engine finish!");
	}
}
