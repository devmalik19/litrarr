package devmalik19.litrarr.service.plugins;

import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;

/**
 * Interface for plugin-based download services.
 * Implement this to add a new plugin — it will auto-register with PluginsService.
 */
public interface PluginHandler
{
	/** Unique service name stored in DownloadState (e.g. "slskd") */
	String getServiceName();

	/** Service name used while search is pending (e.g. "slskd_pending") */
	String getPendingServiceName();

	/** Test connectivity to the service */
	String checkConnection(ConnectionSettings connectionSettings);

	/** Get the current connection settings for this plugin */
	ConnectionSettings getConnectionSettings();

	/** Submit a search — returns true if successfully submitted and search state updated */
	boolean search(Search search) throws Exception;

	/** Poll for search results and enqueue download if ready. Returns true if download enqueued. */
	boolean checkSearchAndDownload(Search search) throws Exception;

	/** Check actual download transfer progress */
	void checkDownloads(Search search) throws Exception;
}
