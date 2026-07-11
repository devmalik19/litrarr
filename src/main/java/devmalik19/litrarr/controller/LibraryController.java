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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

	@PostMapping("/metadata/{id}")
	public String refreshMetadata(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes)
	{
		try
		{
			libraryService.refreshMetadata(id);
			redirectAttributes.addFlashAttribute("message", "Metadata refreshed successfully.");
		}
		catch (Exception e)
		{
			redirectAttributes.addFlashAttribute("error", "Metadata refresh failed: " + e.getMessage());
		}
		return "redirect:/library/view/" + id;
	}

	@PostMapping("/metadata/all")
	public String refreshAllMetadata(RedirectAttributes redirectAttributes)
	{
		int count = libraryService.resetAllMetadataFlags();
		redirectAttributes.addFlashAttribute("message", count + " library entries reset. Metadata will be re-fetched on next scan.");
		return "redirect:/library";
	}
}
