package devmalik19.litrarr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.SearchStatus;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.MetadataResult;
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
import org.springframework.scheduling.annotation.Async;
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

	private static List<Entry<String, Integer>> sortedServices;

	public void setPriorityOrder() throws Exception
	{
		String value = Settings.store.get(Keys.PRIORITY);
		HashMap<String, Integer> priority = objectMapper.readValue(value, new TypeReference<HashMap<String, Integer>>() {});

		sortedServices = priority.entrySet().stream()
			.filter(entry -> entry.getValue() != 0)
			.sorted(Entry.comparingByValue()).toList();
	}

	public List<Search> searchHistory()
	{
		return searchRepository.findAll();
	}

	public void addToDownloadClients(DownloadRequest downloadRequest) throws Exception
	{
		networkService.addToDownloadClients(downloadRequest);
	}

	@Async
	public void addSearch(MetadataResult metadataResult)
	{
		Search search = new Search();
		search.setTitle(metadataResult.getTitle());
		search = searchRepository.save(search);
		searchEntry(search);
	}

	public Page<SearchResult> interactiveSearch(Integer id, Pageable pageable) throws Exception
	{
		Search search = getSearchById(id);
		List<SearchResult> searchResults = Arrays.asList(networkService.getSearchResults(search.getAuthor()  + " " + search.getTitle()));
		return PaginationHelper.prepareResults(searchResults, pageable);
	}

	public void triggerSearch() throws Exception
	{
		logger.info("Starting search engine!");
		List<Search> searchList = searchRepository.findByStatus(SearchStatus.NEW);
		searchList.forEach(this::searchEntry);
		logger.info("Search engine finish!");
	}

	public void searchEntry(Search search)
	{
		logger.info("Searching for {} with priority order {}", search.getTitle(), sortedServices);
		search.setStatus(SearchStatus.SEARCHING);
		searchRepository.save(search);

		boolean isSuccess;
		for (Entry<String, Integer> entry : sortedServices)
		{
			try
			{
				if(NetworkService.services.contains(entry.getKey()))
					isSuccess = networkService.search(search);
				else
					isSuccess = pluginsService.search(search);

				if(isSuccess)
					break;
			}
			catch (Exception e)
			{
				logger.debug(e.getLocalizedMessage());
			}
		}
		logger.info("Search for {} finish", search.getTitle());
	}

	public void reset()
	{
		searchRepository.update(SearchStatus.NOTFOUND, SearchStatus.NEW);
	}

	public void delete(int id)
	{
		Optional<Search> opt = searchRepository.findById(id);
		opt.ifPresent(search -> searchRepository.delete(search));
	}

	public Search getSearchById(Integer id)
	{
		Optional<Search> opt = searchRepository.findById(id);
		return opt.get();
	}


	public void checkDownloads()
	{
		List<Search> searchList = searchRepository.findByStatus(SearchStatus.DOWNLOADING);
		searchList.forEach(search -> {
			try
			{
				networkService.checkDownloads(search);
				pluginsService.checkDownloads(search);
			}
			catch (Exception e)
			{
				logger.debug(e.getLocalizedMessage());
			}
		});
	}
}
