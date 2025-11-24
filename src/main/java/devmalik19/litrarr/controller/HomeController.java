package devmalik19.litrarr.controller;

import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.helper.PaginationHelper;
import devmalik19.litrarr.service.metadata.MetaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController
{
	@Autowired
	private MetaDataService metaDataService;

    @GetMapping("/")
    public String home()
    {
        return "home";
    }

	@GetMapping("/home/search")
	public String search(
		@RequestParam(value = "title") String title,
		@RequestParam(value = "author", required = false) String author,
		@RequestParam(value = "publisher", required = false) String publisher,
		Pageable pageable,
		Model model
	) throws Exception
	{
		Page<MetadataResult> searchResultsPage = metaDataService.search(title, author, publisher,  pageable);
		return PaginationHelper.prepareResponse(searchResultsPage, pageable, model);
	}
}
