package devmalik19.litrarr.data.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devmalik19.litrarr.constants.Category;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataResult
{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String title;
	private String author;
	private String publisher;
	private String year;
	private String imageUrl;
	private Integer library;

	/** The external API identifier (e.g. ComicVine volume ID, MAL manga ID) */
	private String sourceId;

	/** The category this result belongs to (COMICS, MANGA, BOOKS, AUDIOBOOKS) */
	private Category category;

	@Override
	public String toString()
	{
		try
		{
			return OBJECT_MAPPER.writeValueAsString(this);
		}
		catch (JsonProcessingException e)
		{
			return "{}";
		}
	}
}
