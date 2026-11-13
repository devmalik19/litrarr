package devmalik19.litrarr.service.plugins;

import devmalik19.litrarr.data.dao.Search;
import devmalik19.litrarr.data.dto.ConnectionSettings;

import java.util.Set;

public interface PluginHandler
{

	String getServiceName();

	String getPendingServiceName();

	String checkConnection(ConnectionSettings connectionSettings);

	ConnectionSettings getConnectionSettings();

	boolean search(Search search, Set<String> blocklist) throws Exception;

	boolean checkSearchAndDownload(Search search, Set<String> blocklist) throws Exception;

	void checkDownloads(Search search) throws Exception;
}
