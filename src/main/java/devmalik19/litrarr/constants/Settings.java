package devmalik19.litrarr.constants;

import java.util.concurrent.ConcurrentHashMap;

public interface Settings
{
	ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
}
