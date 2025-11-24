package devmalik19.litrarr.controller;

import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.helper.PaginationHelper;
import devmalik19.litrarr.service.SearchService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
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

    @Autowired
    private NetworkService networkService;

	@GetMapping("/search")
    public String result(
		@RequestParam(value = "search") String search,
		Pageable pageable,
		Model model) throws Exception
	{

		Page<SearchResult> searchResultsPage = searchService.search(search, pageable);
		return PaginationHelper.prepareResponse(searchResultsPage, pageable, model);
	}

    @PostMapping("/search/download")
    @ResponseBody
    public ResponseEntity<String> download(@RequestBody @Valid DownloadRequest downloadRequest) throws Exception
    {
		networkService.download(downloadRequest);
        return ResponseEntity.ok("");
    }

	@GetMapping("/search/trigger")
	@ResponseBody
	public ResponseEntity<String> trigger() throws Exception
	{
		searchService.search();
		return ResponseEntity.ok("");
	}
}
