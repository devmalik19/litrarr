package devmalik19.litrarr.helper;

import devmalik19.litrarr.constants.Constants;
import java.util.HashMap;
import java.util.Map;

public class HttpHelper
{
	/**
	 * Returns standard JSON API headers (Accept + User-Agent).
	 */
	public static Map<String, String> jsonApiHeaders()
	{
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		headers.put("User-Agent", Constants.USER_AGENT);
		return headers;
	}

	/**
	 * Returns standard JSON API headers with additional custom entries.
	 * Pass key-value pairs as alternating strings: key1, value1, key2, value2, ...
	 */
	public static Map<String, String> jsonApiHeaders(String... extra)
	{
		Map<String, String> headers = jsonApiHeaders();
		for (int i = 0; i < extra.length - 1; i += 2)
		{
			headers.put(extra[i], extra[i + 1]);
		}
		return headers;
	}
}
