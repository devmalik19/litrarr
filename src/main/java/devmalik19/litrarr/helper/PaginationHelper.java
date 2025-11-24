/**
 *  This class is to manage pagination and sorting.
 *  This code is related to UI presentation and there is no business logic,
 *  That's why I like to keep it in this separate file here.
 */

package devmalik19.litrarr.helper;

import devmalik19.litrarr.data.dto.MetadataResult;
import devmalik19.litrarr.data.dto.SearchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

public class PaginationHelper
{
	public static <T> String prepareResponse(Page<T> page, Pageable pageable, Model model)
	{
		model.addAttribute("searchResults", page.getContent());
		model.addAttribute("page", page);

		String sortField = "";
		String sortDirection = "";

		if (pageable.getSort().isSorted())
		{
			Sort.Order order = pageable.getSort().iterator().next();
			sortField = order.getProperty();
			sortDirection = order.getDirection().name();
		}

		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDirection", sortDirection);
		model.addAttribute("currentPage", page.getNumber() + 1);
		model.addAttribute("totalPages", page.getTotalPages());
		model.addAttribute("totalItems", page.getTotalElements());

		List<String> headers = new ArrayList<>();

		if (page.hasContent() && page.getContent().get(0) instanceof MetadataResult)
		{
			headers = List.of("title", "author", "year");
			model.addAttribute("endpoint", "home/search");
		}
		else if (page.hasContent() && page.getContent().get(0) instanceof SearchResult)
		{
			headers = List.of("protocol", "indexer", "title", "seeders", "leechers");
			model.addAttribute("isMetadataResult", true);
			model.addAttribute("endpoint", "search");
		}

		model.addAttribute("headers", headers);
		return "fragments/table :: data";
	}

	public static <T> Page<T>  prepareResults(List<T> results, Pageable pageable)
	{
		int pageSize = pageable.getPageSize();
		int currentPage = pageable.getPageNumber();
		int startItem = currentPage * pageSize;
		results = sortResults(pageable, results);

		List<T> pageContent;
		if (results.size() < startItem)
		{
			pageContent = Collections.emptyList();
		}
		else
		{
			int toIndex = Math.min(startItem + pageSize, results.size());
			pageContent = results.subList(startItem, toIndex);
		}

		return new PageImpl<>(pageContent, PageRequest.of(currentPage, pageSize), results.size());
	}


	private static  <T> List<T> sortResults(Pageable pageable, List<T> results)
	{
		Sort sort = pageable.getSort();
		if (sort.isSorted() && !results.isEmpty())
		{
			Sort.Order order = sort.iterator().next();
			String property = order.getProperty();

			T firstItem = results.get(0);
			Comparator<T> comparator = null;
			if (firstItem instanceof MetadataResult)
			{
				comparator = getMetadataComparator(property);
			}
			else if (firstItem instanceof SearchResult)
			{
				comparator = getSearchComparator(property);
			}

			if (comparator != null)
			{
				if (order.isDescending())
				{
					comparator = comparator.reversed();
				}
				results = results.stream().sorted(comparator).collect(Collectors.toList());
			}
		}
		return results;
	}

	private static <T> Comparator<T> getSearchComparator(String property)
	{
		Comparator<SearchResult> comparator = null;
		switch (property)
		{
			case "protocol":
				comparator = Comparator.comparing(SearchResult::getProtocol, Comparator.nullsLast(String::compareTo));
				break;
			case "indexer":
				comparator = Comparator.comparing(SearchResult::getIndexer, Comparator.nullsLast(String::compareTo));
				break;
			case "title":
				comparator = Comparator.comparing(SearchResult::getTitle, Comparator.nullsLast(String::compareTo));
				break;
			case "seeders":
				comparator = Comparator.comparingInt(SearchResult::getSeeders);
				break;
			case "leechers":
				comparator = Comparator.comparingInt(SearchResult::getLeechers);
				break;
		}
		return (Comparator<T>) comparator;
	}

	private static <T> Comparator<T> getMetadataComparator(String property)
	{
		Comparator<MetadataResult> comparator = null;
		switch (property)
		{
			case "title":
				comparator = Comparator.comparing(MetadataResult::getTitle, Comparator.nullsLast(String::compareTo));
				break;
			case "author":
				comparator = Comparator.comparing(MetadataResult::getAuthor, Comparator.nullsLast(String::compareTo));
			case "year":
				comparator = Comparator.comparing(MetadataResult::getYear, Comparator.nullsLast(String::compareTo));
				break;
		}
		return (Comparator<T>) comparator;
	}
}
