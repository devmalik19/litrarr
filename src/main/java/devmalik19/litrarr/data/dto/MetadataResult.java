package devmalik19.litrarr.data.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataResult
{
	private String title;
	private String author;
	private String year;
}
