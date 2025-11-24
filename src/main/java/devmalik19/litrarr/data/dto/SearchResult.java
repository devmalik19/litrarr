package devmalik19.litrarr.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResult
{
    private String guid;
	private String protocol;
	private String indexer;
    private String title;
    private int seeders;
    private int leechers;
	private String downloadUrl;
}
