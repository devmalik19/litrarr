package devmalik19.litrarr.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityHelper
{
	private static final Logger logger = LoggerFactory.getLogger(PriorityHelper.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static HashMap<String, Integer> getPriority()
	{
		try
		{
			String value = Settings.store.get(Keys.PRIORITY);
			if (value != null)
			{
				return objectMapper.readValue(value, new TypeReference<HashMap<String, Integer>>() {});
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to parse priority settings: {}", e.getMessage());
		}
		return new HashMap<>();
	}
}
