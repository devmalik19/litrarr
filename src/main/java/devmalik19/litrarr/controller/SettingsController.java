package devmalik19.litrarr.controller;

import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Category;
import devmalik19.litrarr.service.IndexService;
import devmalik19.litrarr.service.LibraryService;
import devmalik19.litrarr.service.SettingsService;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.service.plugins.PluginsService;
import devmalik19.litrarr.service.thirdparty.NetworkService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SettingsController
{
    @Autowired
    private SettingsService settingsService;

    @Autowired
	private IndexService indexService;

	@GetMapping("settings")
	public String general(Model model) throws Exception
	{
		List<String> services = settingsService.getServices();
		model.addAttribute("services", services);

		HashMap<Category, String> library = LibraryService.getLibrariesPath();

		model.addAttribute("comics", library.get(Category.COMICS));
		model.addAttribute("manga", library.get(Category.MANGA));
		model.addAttribute("books", library.get(Category.BOOKS));
		model.addAttribute("audiobooks", library.get(Category.AUDIOBOOKS));


		return "settings/general";
	}

    @GetMapping("settings/indexes")
    public String indexes(Model model) throws Exception
    {
        model.addAttribute("settings", settingsService.getConnectionsSettingsForIndexes());
        model.addAttribute("indexes", indexService.findAll());
        return "settings/indexes";
    }

    @GetMapping("settings/clients")
    public String clients(Model model) throws Exception
    {
        model.addAttribute("settings", settingsService.getConnectionsSettingsForClients());
        return "settings/clients";
    }

	@GetMapping("settings/services")
	public String services(Model model) throws Exception
	{
		model.addAttribute("settings", settingsService.getConnectionsSettingsForServices());
		return "settings/services";
	}

	@PostMapping("/settings/save/library")
	public String saveLibrary(
		@RequestParam("comics") String comics,
		@RequestParam("manga") String manga,
		@RequestParam("books") String books,
		@RequestParam("audiobooks") String audiobooks)
	{
		HashMap<Category, String> library = new HashMap<>();
		library.put(Category.COMICS, comics);
		library.put(Category.MANGA, manga);
		library.put(Category.BOOKS, books);
		library.put(Category.AUDIOBOOKS, audiobooks);
		settingsService.save(Keys.LIBRARY_PATHS, library);
		return "redirect:/settings";
	}

	@PostMapping("/settings/save/priority")
	public String savePriority(@RequestParam("enabled") List<String> enabled, @RequestParam("order") List<String> order)
	{
		HashMap<String, Integer> priority = new HashMap<>();
		int i=0;
		for (String o : order)
		{
			if (enabled.contains(o))
				priority.put(o, ++i);
			else
				priority.put(o, 0);
		}

		settingsService.save("priority", priority);
		return "redirect:/settings";
	}

	@PostMapping(value = "/settings/save/{key}", params = "redirect")
	public String save(
		@PathVariable String key,
		@RequestParam(value = "redirect") String redirect,
		@ModelAttribute ConnectionSettings connectionSettings)
	{
		settingsService.save(key, connectionSettings);
		return "redirect:/settings/"+redirect;
	}

	@GetMapping("/settings/indexes/{id}")
	@ResponseBody
	public void update(@PathVariable int id, @RequestParam(value = "status") String status)
	{
		settingsService.update(id,status);
	}

    @PostMapping("/settings/indexes/check/{key}")
    @ResponseBody
    public String checkNetworkConnection(@PathVariable String key, @ModelAttribute ConnectionSettings connectionSettings)
    {
        return settingsService.checkNetworkConnection(key, connectionSettings);
    }

	@PostMapping("/settings/services/check/{key}")
	@ResponseBody
	public String checkPluginConnection(@PathVariable String key, @ModelAttribute ConnectionSettings connectionSettings)
	{
		return settingsService.checkPluginConnection(key, connectionSettings);
	}

	@GetMapping("/settings/sync")
	public String sync(Model model) throws Exception
	{
		settingsService.sync();
		model.addAttribute("indexes", indexService.findAll());
		return   "settings/indexes :: indexes";
	}
}
