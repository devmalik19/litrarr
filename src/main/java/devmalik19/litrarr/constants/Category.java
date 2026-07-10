package devmalik19.litrarr.constants;

import org.springframework.util.StringUtils;

public enum Category
{
	COMICS,
	BOOKS,
	AUDIOBOOKS,
	MANGA;

	public FolderType getRootFolderType()
	{
		return switch (this) {
			case BOOKS, AUDIOBOOKS -> FolderType.AUTHOR;
			case COMICS, MANGA -> FolderType.TITLE;
		};
	}

	public static boolean isMatch(String type)
	{
		if (!StringUtils.hasText(type))
			return false;

		try
		{
			Category.valueOf(type.toUpperCase());
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
