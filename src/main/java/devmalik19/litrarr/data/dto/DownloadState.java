package devmalik19.litrarr.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadState
{
	private String identifier;
	private String service;
	private String downloadPath;

	public boolean isEmpty()
	{
		return Stream.of(identifier, service, downloadPath).allMatch(Objects::isNull);
	}
}
