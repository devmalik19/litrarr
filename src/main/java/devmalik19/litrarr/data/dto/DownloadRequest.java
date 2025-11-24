package devmalik19.litrarr.data.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DownloadRequest
{
    @NotNull(message = "URL cannot be null")
    private String url;
    @NotNull(message = "URL cannot be null")
    @Pattern(regexp = "^(torrent|usenet)$", message = "Protocol must be 'torrent' or 'usenet'")
    private String protocol;
}
