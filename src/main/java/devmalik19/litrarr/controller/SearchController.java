package devmalik19.litrarr.controller;

import devmalik19.litrarr.data.dto.DownloadRequest;
import devmalik19.litrarr.data.dto.SearchResult;
import devmalik19.litrarr.service.SearchService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public String home()
    {
        return "search";
    }

	@PostMapping("/search")
	public void search() throws Exception
	{
		searchService.search();
	}

    @GetMapping("/search/result")
    public String result(
		@RequestParam(value = "search") String search,
		Pageable pageable,
		Model model) throws Exception
	{

		Page<SearchResult> searchResultsPage = searchService.search(search, pageable);
        model.addAttribute("searchResults", searchResultsPage.getContent());
		model.addAttribute("page", searchResultsPage);

		if (pageable.getSort().isSorted())
		{
			Sort.Order order = pageable.getSort().iterator().next();
			model.addAttribute("sortField", order.getProperty());
			model.addAttribute("sortDirection", order.getDirection().name());
		}
		else
		{
			model.addAttribute("sortField", "");
			model.addAttribute("sortDirection", "");
		}

		model.addAttribute("currentPage", searchResultsPage.getNumber() + 1);
		model.addAttribute("totalPages", searchResultsPage.getTotalPages());
		model.addAttribute("totalItems", searchResultsPage.getTotalElements());
        return "fragments/table :: searchResults";
    }

    @PostMapping("/search/result/download")
    @ResponseBody
    public ResponseEntity<String> download(@RequestBody @Valid DownloadRequest downloadRequest) throws Exception
    {
		networkService.download(downloadRequest);
        return ResponseEntity.ok("");
    }
}
