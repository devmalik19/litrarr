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
	public static String COMICS_PATHS;
	public static String EBOOKS_PATH;
	public static String MANGA_PATH;
	public static String AUDIOBOOKS_PATH;
	public static String SEARCH_CATEGORY = "3000";
	public static String ENCRYPTION_KEY;
	public static int QUERY_LIMIT = 1000;
	public static List<PathMatcher> pathMatcherList;
	public static String[] IMAGE_TYPES = {".jpg", ".png", ".gif", ".webp"};
	public static String CACHE_PATH;


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
		COMICS_PATHS = LIBRARY_PATH + "/comics";
		MANGA_PATH = LIBRARY_PATH + "/manga";
		EBOOKS_PATH = LIBRARY_PATH + "/ebooks";
		AUDIOBOOKS_PATH = LIBRARY_PATH + "/audiobooks";
	}
}
