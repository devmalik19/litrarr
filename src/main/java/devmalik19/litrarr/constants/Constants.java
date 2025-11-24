package devmalik19.litrarr.constants;

import java.nio.file.PathMatcher;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Constants
{
	public static String CONFIG_PATH;
    public static String DOWNLOAD_PATH;
	public static String LIBRARY_PATH;
	public static String SEARCH_CATEGORY = "3000";
	public static String ENCRYPTION_KEY;
	public static int QUERY_LIMIT = 1000;
	public static List<PathMatcher> pathMatcherList;
	public static String[] IMAGE_TYPES = {".jpg", ".png", ".gif", ".webp"};
	public static String CACHE_PATH;
	public static String USER_AGENT = "Litrarr/1.0 ( dev.malik@outlook.com )";



	@Value("${ENCRYPTION_KEY:}")
	public void setEncryptionKey(String key)
	{
		if(StringUtils.hasText(key))
		{
			if (key.length() < 32)
				key += "#".repeat(32 - key.length());
			else
				key = key.substring(0, 32);
		}
		Constants.ENCRYPTION_KEY = key;
	}

	@Value("${app.path.config:/config}")
	public void setConfigPath(String path)
	{
		CONFIG_PATH = path;
		CACHE_PATH = CONFIG_PATH + "/cache";
	}

	@Value("${app.path.download:/downloads}")
	public void setDownloadPath(String path)
	{
		DOWNLOAD_PATH = path;
	}

	@Value("${app.path.library:/library}")
	public void setLibraryPath(String path)
	{
		LIBRARY_PATH = path;
	}
}
