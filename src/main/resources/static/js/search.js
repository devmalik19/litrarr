let currentPage = 0;
let pageSize = 100;
let currentSortField = '';
let currentSortDirection = '';
let lastQuery = {};

async function metadataSearch(searchButton)
{
	const originalText = searchButton.textContent;
 	searchButton.disabled = true;
    searchButton.textContent = '...';

	const titleInput = document.getElementById("title");
	if (!titleInput.value.trim())
	{
		titleInput.focus();
		searchButton.textContent = originalText;
		searchButton.disabled = false;
		return;
	}

	lastQuery = {
		title: titleInput.value.trim(),
		author: document.getElementById("author").value,
		publisher: document.getElementById("publisher").value,
		category: document.getElementById("category").value
	};
	currentPage = 0;
	await loadResults("home/search");
    searchButton.textContent = originalText;
	searchButton.disabled = false;
}

// Loads one page of results for the last search, keeping its filters, page and sorting
async function loadResults(endpoint)
{
	const sortParam = currentSortField ? `${currentSortField},${currentSortDirection}` : '';
	const params = new URLSearchParams({
		...lastQuery,
		page: currentPage,
		size: pageSize,
		sort: sortParam
	});
    const response = await doGetRequest(endpoint + "?" + params.toString());
    document.getElementById("searchResults").innerHTML = response;
}

async function triggerSearch(searchButton, id)
{
    const originalText = searchButton.textContent;
    searchButton.disabled = true;
    searchButton.textContent = '...';

    const response = await doGetRequest("trigger/"+id);

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

async function interactiveSearch(searchButton, id)
{
	const originalText = searchButton.textContent;
 	searchButton.disabled = true;
    searchButton.textContent = '...';

	lastQuery = { id: id };
	currentPage = 0;
	await loadResults("interactive");
    searchButton.textContent = originalText;
	searchButton.disabled = false;
}

async function fetchPage(pageNumber)
{
    currentPage = pageNumber;
    fetchResults();
}

async function sortResults(header)
{
	field = header.getAttribute('data-header');
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
    fetchResults();
}

async function fetchResults()
{
	const endpoint = document.getElementById("endpoint").getAttribute('data-endpoint');
	await loadResults(endpoint);
}

async function download(button)
{
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '...';

    const downloadUrl = button.getAttribute('data-url');
    const protocol = button.getAttribute('data-protocol');
    const body = {
            url: downloadUrl,
            protocol: protocol
    };
    const response = await doPostRequestWithBody("search/download", body);

    if (response.ok)
    {
        button.textContent = '✅';
    }
    else
    {
        button.textContent = '❌';
    }
}

async function addToLibrary(button)
{
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '...';

    const payload = button.getAttribute('data-payload');
    const data = JSON.parse(payload);
    const response = await doPostRequestWithBody("library/add", data);

    if (response.ok)
    {
        button.textContent = '✅';
    }
    else
    {
        button.textContent = '❌';
    }
}

async function deleteSearch(id)
{
	if (confirm("Are you sure you want to delete this entry?"))
	{
		window.location.href = `delete?id=${id}`;
	}
}

document.addEventListener('DOMContentLoaded', function() {
	const inputs = document.querySelectorAll('#title, #author, #publisher');
	inputs.forEach(function(input) {
		input.addEventListener('keydown', function(event) {
			if (event.key === 'Enter') {
				event.preventDefault();
				document.getElementById('metadataSearchBtn').click();
			}
		});
	});
});
