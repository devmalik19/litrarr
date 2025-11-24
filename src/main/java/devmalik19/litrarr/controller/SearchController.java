package devmalik19.litrarr.controller;

import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.helper.PaginationHelper;
import devmalik19.litrarr.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SearchController
{

    @Autowired
    private SearchService searchService;

	@GetMapping("/search")
	public String history(Pageable pageable, Model model) throws Exception
	{
		model.addAttribute("history", searchService.searchHistory());
		return "history";
	}

    @PostMapping("/search/download")
    @ResponseBody
    public ResponseEntity<String> download(@RequestBody @Valid DownloadRequest downloadRequest) throws Exception
    {
		searchService.addToDownloadClients(downloadRequest);
        return ResponseEntity.ok("");
    }

	@PostMapping("/search/add")
	@ResponseBody
	public ResponseEntity<String> add(@RequestBody @Valid MetadataResult metadataResult) throws Exception
	{
		searchService.addSearch(metadataResult);
		return ResponseEntity.ok("");
	}

	@GetMapping("/search/delete")
	public String delete(@RequestParam(value = "id") int id)
	{
		searchService.delete(id);
		return "redirect:/search";
	}

	@GetMapping("/search/interactive")
	public String interactive(@RequestParam(value = "search") String search, Pageable pageable, Model model) throws Exception
	{
		Page<SearchResult> searchResultsPage = searchService.interactiveSearch(search, pageable);
		return PaginationHelper.prepareResponse(searchResultsPage, pageable, model);
	}

	@GetMapping("/search/trigger")
	@ResponseBody
	public ResponseEntity<String> trigger() throws Exception
	{
		searchService.triggerSearch();
		return ResponseEntity.ok("");
	}
}
