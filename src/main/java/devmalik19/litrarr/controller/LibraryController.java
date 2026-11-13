package devmalik19.litrarr.controller;

import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.service.LibraryService;
import devmalik19.litrarr.service.SearchService;
import org.springframework.http.HttpStatus;
import devmalik19.litrarr.data.dto.MetadataResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/library")
public class LibraryController
{
	private final LibraryService libraryService;
	private final SearchService searchService;

	public LibraryController(LibraryService libraryService, SearchService searchService)
	{
		this.libraryService = libraryService;
		this.searchService = searchService;
	}

	@GetMapping("/view/{id}")
	public String items(@PathVariable("id") Integer id,
						@RequestParam(value = "sort", defaultValue = "name") String sort,
						@RequestParam(value = "search", defaultValue = "") String search,
						Model model)
	{
		Library library = libraryService.findById(id);

		model.addAttribute("library", library);
		model.addAttribute("libraries", libraryService.getSortedChildren(library, sort, search));
		model.addAttribute("items", libraryService.getFilteredItems(library, search));
		model.addAttribute("sort", sort);
		model.addAttribute("search", search);

		return "items";
	}

	@GetMapping("/{category}")
	public String library(@PathVariable("category") String categoryStr,
						  @RequestParam(value = "sort", defaultValue = "name") String sort,
						  @RequestParam(value = "search", defaultValue = "") String search,
						  Model model)
	{
		Category category = Category.valueOf(categoryStr.toUpperCase());
		FolderType type = category.getRootFolderType();
		List<Library> library = libraryService.getLibrary(type, category, sort, search);
		model.addAttribute("title", StringUtils.capitalize(categoryStr.toLowerCase()));
		model.addAttribute("category", categoryStr.toLowerCase());
		model.addAttribute("library", library);
		model.addAttribute("sort", sort);
		model.addAttribute("search", search);
		return "library";
	}

	@PostMapping("/add")
	@ResponseBody
	public ResponseEntity<String> addToLibrary(@RequestBody @Valid MetadataResult metadataResult)
	{
		try
		{
			libraryService.addToLibrary(metadataResult);
			return ResponseEntity.ok("");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@PostMapping("/complete/{id}")
	@ResponseBody
	public ResponseEntity<String> setComplete(@PathVariable("id") Integer id,
											  @RequestParam("complete") boolean complete)
	{
		try
		{
			libraryService.setComplete(id, complete);
			return ResponseEntity.ok("");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@PostMapping("/monitor/{id}")
	@ResponseBody
	public ResponseEntity<String> setMonitored(@PathVariable("id") Integer id,
											   @RequestParam("monitored") boolean monitored)
	{
		try
		{
			libraryService.setMonitored(id, monitored);
			return ResponseEntity.ok("");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}
	@PostMapping("/item/monitor/{id}")
	@ResponseBody
	public ResponseEntity<String> setItemMonitored(@PathVariable("id") Integer id,
												   @RequestParam("monitored") boolean monitored)
	{
		try
		{
			libraryService.setItemMonitored(id, monitored);
			return ResponseEntity.ok("");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}
	@PostMapping("/search-missing/{id}")
	@ResponseBody
	public ResponseEntity<String> searchMissing(@PathVariable("id") Integer id)
	{
		try
		{
			Search search = searchService.prepareMissingSearch(id);
			if (search == null)
				return ResponseEntity.status(HttpStatus.CONFLICT).body("Nothing missing, or a search is already running");
			searchService.runSearch(search);
			return ResponseEntity.ok("Searching for missing files");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@PostMapping("/metadata/{id}")
	@ResponseBody
	public ResponseEntity<String> refreshMetadata(@PathVariable("id") Integer id)
	{
		try
		{
			libraryService.refreshMetadata(id);
			return ResponseEntity.ok("");
		}
		catch (Exception e)
		{
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@PostMapping("/metadata/category/{category}")
	public String refreshCategoryMetadata(@PathVariable("category") String categoryStr, RedirectAttributes redirectAttributes)
	{
		Category category = Category.valueOf(categoryStr.toUpperCase());
		int count = libraryService.resetMetadataFlagsByCategory(category);
		redirectAttributes.addFlashAttribute("message", count + " library entries reset. Metadata will be re-fetched on next scan.");
		return "redirect:/library/" + categoryStr;
	}
}
