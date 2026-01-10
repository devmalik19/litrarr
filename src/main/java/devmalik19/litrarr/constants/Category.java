package devmalik19.litrarr.constants;

import org.springframework.util.StringUtils;

public enum Category
{
	COMICS,
	BOOKS,
	AUDIOBOOKS,
	MANGA;

	public static boolean isMatch(String type)
	{
		if (!StringUtils.hasText(type))
			return false;

		try
		{
			FileTypes.valueOf(type.toUpperCase());
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
