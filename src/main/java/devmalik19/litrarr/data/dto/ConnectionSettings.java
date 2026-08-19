package devmalik19.litrarr.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionSettings
{
    private String name;
    private String url;
    private String apiKey;
    private String username;
    private String password;
	private String category;
}
