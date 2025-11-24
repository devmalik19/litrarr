package devmalik19.litrarr.service.thirdparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Settings;
import devmalik19.litrarr.data.dto.ConnectionSettings;
import devmalik19.litrarr.service.HttpRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SabnzbdService
{
    Logger logger = LoggerFactory.getLogger(SabnzbdService.class);

    @Autowired
    private HttpRequestService httpRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    public String checkConnection(ConnectionSettings connectionSettings)
    {
        return httpRequestService.doGetRequest(String.format("%s/api?mode=queue&output=json&apikey=%s",connectionSettings.getUrl(),connectionSettings.getApiKey()));
    }

    public void addNzb(String url) throws Exception
    {
		String value = Settings.store.get(NetworkService.SABNZBD);
		if(StringUtils.hasText(value))
		{
			ConnectionSettings connectionSettings = objectMapper.readValue(value, ConnectionSettings.class);
            String response = httpRequestService.doGetRequest(String.format("%s/api?mode=addurl&output=json&apikey=%s&cat=%s&name=%s",connectionSettings.getUrl(),connectionSettings.getApiKey(),
				connectionSettings.getCategory(), url));
			logger.info("Download enqueued for {} {}", url, response);
        }
    }
}
