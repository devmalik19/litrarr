package devmalik19.litrarr.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Keys;
import devmalik19.litrarr.constants.Settings;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility for reading the priority settings from the in-memory store.
 * Eliminates duplicate priority-deserialization logic across services.
 */
public class PriorityHelper
{
	private static final Logger logger = LoggerFactory.getLogger(PriorityHelper.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Reads the priority map from Settings.store.
	 * Returns an empty map if parsing fails or no priority is configured.
	 */
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
