package devmalik19.litrarr.controller;

import devmalik19.litrarr.constants.LibraryTypes;
import devmalik19.litrarr.data.dao.Library;
import devmalik19.litrarr.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LibraryController
{
	@Autowired
	private LibraryService libraryService;

	@GetMapping("/library/comics")
	public String comics(Model model)
	{
		model.addAttribute("title", "Comics");
		model.addAttribute("library", libraryService.getLibrary(LibraryTypes.BOOKS));
		return "library";
	}

	@GetMapping("/library/manga")
	public String manga(Model model)
	{
		model.addAttribute("title", "Manga");
		model.addAttribute("library", libraryService.getLibrary(LibraryTypes.MANGA));
		return "library";
	}

	@GetMapping("/library/books")
	public String books(Model model)
	{
		model.addAttribute("title", "Books");
		model.addAttribute("library", libraryService.getLibrary(LibraryTypes.BOOKS));
		return "library";
	}

	@GetMapping("/library/audiobooks")
	public String audiobooks(Model model)
	{
		model.addAttribute("title", "Audiobooks");
		model.addAttribute("library", libraryService.getLibrary(LibraryTypes.AUDIOBOOKS));
		return "library";
	}

	@GetMapping("/library/{id}")
	public String items(@PathVariable("id") String id, Model model)
	{
		Library library = libraryService.findById(id);

		model.addAttribute("library", library);
		model.addAttribute("libraries", library.getLibraryList());
		model.addAttribute("items", library.getItemList());

		return "items";
	}
}
