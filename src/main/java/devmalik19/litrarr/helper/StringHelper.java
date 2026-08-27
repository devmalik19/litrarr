package devmalik19.litrarr.helper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper
{
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

	/**
	 * Extracts the first numeric sequence from a string and returns it as a normalized integer string.
	 * Returns null if no number is found.
	 */
	public static String extractNumber(String text)
	{
		if (text == null)
			return null;

		Matcher matcher = NUMBER_PATTERN.matcher(text);
		if (matcher.find())
			return String.valueOf(Integer.parseInt(matcher.group()));
		return null;
	}

	/**
	 * Parses a date string in "YYYY-MM-DD" or "YYYY" format into a LocalDate.
	 * Returns null if the string is blank or unparseable.
	 */
	public static LocalDate parseDate(String dateStr)
	{
		if (dateStr == null || dateStr.isBlank())
			return null;

		try
		{
			if (dateStr.length() == 10) // YYYY-MM-DD
				return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
			else if (dateStr.length() == 4) // YYYY
				return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
		}
		catch (DateTimeParseException | NumberFormatException e)
		{
			// unparseable, return null
		}
		return null;
	}
}
