package devmalik19.litrarr.controller;

import devmalik19.litrarr.constants.FolderType;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.service.LibraryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/library")
public class LibraryController
{
	private final LibraryService libraryService;

	public LibraryController(LibraryService libraryService)
	{
		this.libraryService = libraryService;
	}

	@GetMapping("/view/{id}")
	public String items(@PathVariable("id") Integer id, Model model)
	{
		Library library = libraryService.findById(id);

		model.addAttribute("library", library);
		model.addAttribute("libraries", library.getLibraryList());
		model.addAttribute("items", library.getItemList());

		return "items";
	}

	@GetMapping("/{category}")
	public String library(@PathVariable("category") String categoryStr, Model model)
	{
		Category category = Category.valueOf(categoryStr.toUpperCase());
		FolderType type = category.getRootFolderType();
		model.addAttribute("title", StringUtils.capitalize(categoryStr.toLowerCase()));
		model.addAttribute("library", libraryService.getLibrary(type, category));
		return "library";
	}
}
