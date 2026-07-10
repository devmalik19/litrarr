package devmalik19.litrarr.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SettingsHelper {
    private static final Logger logger = LoggerFactory.getLogger(SettingsHelper.class);
    private final ObjectMapper objectMapper;

    public SettingsHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves and deserializes ConnectionSettings for the given service key.
     * Returns null only if the key is not found in the store.
     * For all existing keys (including empty or null values), deserialization is attempted.
     */
    public ConnectionSettings getConnectionSettings(String key) {
        if (!Settings.store.containsKey(key))
            return null;
        String value = Settings.store.get(key);
        try {
            return objectMapper.readValue(value, ConnectionSettings.class);
        } catch (Exception e) {
            logger.error("Failed to parse ConnectionSettings for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves ConnectionSettings for the given key, returning a new empty instance if
     * the key is missing or deserialization fails.
     */
    public ConnectionSettings getConnectionSettingsOrDefault(String key) {
        ConnectionSettings settings = getConnectionSettings(key);
        return settings != null ? settings : new ConnectionSettings();
    }
}
