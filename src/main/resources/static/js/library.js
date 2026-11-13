function setViewMode(mode)
{
	localStorage.setItem('viewMode', mode);
	document.body.dataset.view = mode;
	document.querySelectorAll('.view-button').forEach(function(b) {
		b.classList.toggle('active', b.getAttribute('data-view') === mode);
	});
}
async function toggleMonitor(checkbox)
{
	const wanted = checkbox.checked;
	checkbox.disabled = true;
	const response = await fetch(checkbox.getAttribute('data-url') + '?monitored=' + wanted, { method: 'POST' });
	if (!response.ok)
		checkbox.checked = !wanted;
	checkbox.disabled = false;
	const row = checkbox.closest('tr');
	if (row)
		row.classList.toggle('row-unmonitored', !checkbox.checked);
}
document.addEventListener('DOMContentLoaded', function() {
	setViewMode(localStorage.getItem('viewMode') === 'list' ? 'list' : 'grid');
	const searchInput = document.getElementById('library-search');
	if (!searchInput) return;
	const entries = document.querySelectorAll('.poster-grid .poster, .list-view tbody tr');
	searchInput.addEventListener('input', function() {
		const query = this.value.toLowerCase().trim();
		entries.forEach(function(entry) {
			const titleElement = entry.querySelector('.poster-title, .row-title');
			const title = titleElement ? titleElement.textContent.toLowerCase() : '';
			entry.style.display = title.includes(query) ? '' : 'none';
		});
	});
});
async function refreshItemMetadata(event, button)
{
	event.preventDefault();
	const id = button.getAttribute('data-id');
	const originalText = button.textContent;
	button.disabled = true;
	button.textContent = '...';
	const response = await fetch('/library/metadata/' + id, { method: 'POST' });
	if (response.ok)
	{
		button.textContent = '✅';
	}
	else
	{
		button.textContent = '❌';
	}
}
async function searchMissing(button)
{
	const originalText = button.textContent;
	button.disabled = true;
	button.textContent = '...';
	const response = await fetch(button.getAttribute('data-url'), { method: 'POST' });
	button.title = await response.text();
	button.textContent = response.ok ? '✅' : (response.status === 409 ? 'ℹ' : '❌');
	setTimeout(() => {
		button.textContent = originalText;
		button.disabled = false;
	}, 3000);
}
async function toggleComplete(button)
{
	button.disabled = true;
	button.textContent = '...';
	const response = await fetch(button.getAttribute('data-url') + '?complete=' + button.getAttribute('data-complete'), { method: 'POST' });
	if (response.ok)
		window.location.reload();
	else
		button.textContent = '❌';
}
