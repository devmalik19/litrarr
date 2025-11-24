document.addEventListener('DOMContentLoaded', function ()
{
    const toggleButton = document.getElementById('sidebar-toggle');
	toggleButton.addEventListener('click', function ()
	{
		const opened = toggleButton.getAttribute('aria-expanded') === 'true';
		if (opened)
			closeSidebar();
		else
			openSidebar();
	});

    const sidebar = document.getElementById('sidebar');
	sidebar.addEventListener('click', function (e)
	{
		const link = e.target.closest('a');
		if (!link)
			return;
		if (window.matchMedia('(max-width: 768px)').matches)
		{
		 	closeSidebar();
		}
	});

    function openSidebar()
    {
    	toggleButton.setAttribute('aria-expanded', 'true');
    	sidebar.classList.remove('toggle');
    }

    function closeSidebar()
    {
    	toggleButton.setAttribute('aria-expanded', 'false');
    	sidebar.classList.add('toggle');
    }

    const headings = document.querySelectorAll('.sidebar-heading');
    const submenus = document.querySelectorAll('.sidebar-submenu');
    headings.forEach(heading =>
    {
        heading.addEventListener('click', function()
        {
            const submenu = this.nextElementSibling;
            if (submenu && submenu.classList.contains('sidebar-submenu')) {
                submenus.forEach(otherSubmenu => {
                    if (otherSubmenu !== submenu && otherSubmenu.classList.contains('sidebar-active')) {
                        otherSubmenu.classList.remove('sidebar-active');
                    }
                });
                submenu.classList.toggle('sidebar-active');
            }
        });
    });
});

function toggleDialogWindow(element)
{
	var id = element.getAttribute('data-id');
    const dialog = document.getElementById('dialog_'+id);
	dialog.classList.toggle('hide');
}

async function sync(event, url)
{
    event.stopPropagation();
    const element = document.getElementById("syncButton");
    const originalText = element.textContent;
    element.disabled = true;
    element.textContent = '...';

    const response = await doGetRequest(url);
    element.innerHTML = '✅';
    element.disabled = false;
    document.getElementById("indexes").innerHTML = response;
}

async function update(event, id, currentStatus)
{
    event.stopPropagation();
    const newStatus = !currentStatus;
    const response = await doGetRequest("/settings/indexes/"+id+"?status="+newStatus);
}

async function check(url, element)
{
	const formId = element.getAttribute('data-id');
    const testButton = document.getElementById("testButton_"+formId);
    const originalText = testButton.textContent;
    testButton.disabled = true;
    testButton.textContent = '...';

    const response = await doPostRequest(url, formId);

    if (response.ok)
    {
        testButton.textContent = '✅';
    }
    else
    {
        testButton.textContent = '❌';
    }

    testButton.disabled = false;
}

async function doGetRequest(url)
{
	const response = await fetch(url, {
		method: "GET"
	});
	return await response.text();
}

async function doPostRequest(url, formId)
{
	const formElement = document.getElementById(formId);
	const formData = new FormData(formElement);
	const response = await fetch(url, {
		method: "POST",
		body: formData,
	});
	return await response;
}

async function doPostRequestWithBody(url, body)
{
	const response = await fetch(url, {
		method: "POST",
		headers: {'Content-Type': 'application/json'},
		body: JSON.stringify(body),
	});
	return await response;
}