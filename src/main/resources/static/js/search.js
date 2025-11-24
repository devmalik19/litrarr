let currentPage = 0;
let pageSize = 100;
let currentSortField = '';
let currentSortDirection = '';
let currentSearchTerm = '';

async function search(searchButton, url)
{
    const originalText = searchButton.textContent;
    searchButton.disabled = true;
    searchButton.textContent = '...';

    const response = await doPostRequestWithBody(url, "{}");

    if (response.ok)
    {
        searchButton.textContent = '✅';
    }
    else
    {
        searchButton.textContent = '❌';
    }

    searchButton.disabled = false;
}

async function manualSearch(searchButton, uri)
{
	const originalText = searchButton.textContent;
 	searchButton.disabled = true;
    searchButton.textContent = '...';
	const searchInput = document.getElementById("search");

	const sortParam = currentSortField ? `${currentSortField},${currentSortDirection}` : '';
	const params = new URLSearchParams({
		search: searchInput.value,
		page: currentPage,
		size: pageSize,
		sort: sortParam
	});

	url = uri + "?" + params.toString();
    const response = await doGetRequest(url);
    document.getElementById("searchResults").innerHTML = response;
    searchButton.textContent = originalText;
	searchButton.disabled = false;
}

async function fetchPage(pageNumber)
{
    currentPage = pageNumber;
    fetchResults('/search/result');
}

async function sortResults(field)
{
	if (currentSortField === field)
	{
        currentSortDirection = (currentSortDirection === 'asc') ? 'desc' : 'asc';
    }
    else
    {
        currentSortField = field;
        currentSortDirection = 'asc';
    }

    currentPage = 0;
    fetchResults('/search/result');
}

async function fetchResults(uri)
{
	const searchInput = document.getElementById("search");

	const sortParam = currentSortField ? `${currentSortField},${currentSortDirection}` : '';
	const params = new URLSearchParams({
		search: searchInput.value,
		page: currentPage,
		size: pageSize,
		sort: sortParam
	});

	url = uri + "?" + params.toString();
    const response = await doGetRequest(url);
    document.getElementById("searchResults").innerHTML = response;
}

async function download(button, uri)
{
    const downloadUrl = button.getAttribute('data-url');
    const protocol = button.getAttribute('data-protocol');
    const body = {
            url: downloadUrl,
            protocol: protocol
    };
    const response = await doPostRequestWithBody(uri, body);
}