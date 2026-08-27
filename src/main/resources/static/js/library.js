document.addEventListener('DOMContentLoaded', function() {
	const searchInput = document.getElementById('library-search');
	if (!searchInput) return;

	const posters = document.querySelectorAll('.poster-grid .poster');

	searchInput.addEventListener('input', function() {
		const query = this.value.toLowerCase().trim();
		posters.forEach(function(poster) {
			const title = poster.querySelector('.poster-title').textContent.toLowerCase();
			poster.style.display = title.includes(query) ? '' : 'none';
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
