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

async function searchMissing(button)
{
const originalText = button.textContent;
button.disabled = true;
button.textContent = '...';
const response = await fetch(button.getAttribute('data-url'), { method: 'POST' });
button.title = await response.text();
button.textContent = response.ok ? '✅' : (response.status === 409 ? 'ℹ️' : '❌');
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
