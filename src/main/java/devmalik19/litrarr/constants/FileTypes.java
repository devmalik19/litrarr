package devmalik19.litrarr.constants;

import org.springframework.util.StringUtils;

public enum FileTypes
{
	CBZ,
	EPUB;

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
