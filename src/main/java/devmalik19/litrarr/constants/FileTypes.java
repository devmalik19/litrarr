package devmalik19.litrarr.constants;

import org.springframework.util.StringUtils;

public enum FileTypes
{
	CBZ,
	CBR,
	EPUB,
	PDF,
	MOBI,
	AZW3,
	MP3,
	M4B,
	FLAC,
	OGG;

	public static boolean isMatch(String extension)
	{
		if (!StringUtils.hasText(extension))
			return false;

		try
		{
			FileTypes.valueOf(extension.toUpperCase());
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
