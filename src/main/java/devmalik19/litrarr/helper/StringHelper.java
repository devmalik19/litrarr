package devmalik19.litrarr.helper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper
{
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

	public static String extractNumber(String text)
	{
		if (text == null)
			return null;
		Matcher matcher = NUMBER_PATTERN.matcher(text);
		if (matcher.find())
			return String.valueOf(Integer.parseInt(matcher.group()));
		return null;
	}

	public static LocalDate parseDate(String dateStr)
	{
		if (dateStr == null || dateStr.isBlank())
			return null;
		try
		{
			if (dateStr.length() == 10)
				return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
			else if (dateStr.length() == 4)
				return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
		}
		catch (DateTimeParseException | NumberFormatException e)
		{

		}
		return null;
	}

	public static String normalizeName(String name)
	{
		if (name == null)
			return "";
		return name.replaceAll("\\(.*?\\)|\\[.*?\\]", "")
			.toLowerCase()
			.replaceAll("[^\\p{L}\\p{N}]", "");
	}

	public static String buildQuery(String... parts)
	{
		StringBuilder sb = new StringBuilder();
		for (String part : parts)
		{
			if (part != null && !part.isBlank())
			{
				if (!sb.isEmpty()) sb.append(" ");
				sb.append(part);
			}
		}
		return sb.toString();
	}
}
