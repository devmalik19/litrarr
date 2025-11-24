package devmalik19.litrarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.helper.PaginationHelper;
import devmalik19.litrarr.repository.SearchRepository;
import devmalik19.litrarr.service.plugins.PluginsService;
import devmalik19.litrarr.service.thirdparty.NetworkService;

import java.util.*;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
		List<SearchResult> searchResults = Arrays.asList(networkService.getSearchResults(searchTerm));
		return PaginationHelper.prepareResults(searchResults, pageable);
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
