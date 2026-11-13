package devmalik19.litrarr.helper;

import devmalik19.litrarr.constants.Constants;
import java.util.HashMap;
import java.util.Map;

public class HttpHelper
{

	public static Map<String, String> jsonApiHeaders()
	{
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		headers.put("User-Agent", Constants.USER_AGENT);
		return headers;
	}

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
