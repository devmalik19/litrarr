package devmalik19.litrarr.helper;

import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public class IssuePathHelper
{
	private static final int PAD_WIDTH = 3;
	private static final int MAX_NAME_LENGTH = 150;
	private static final Pattern INTEGER = Pattern.compile("\\d+");
	private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

	private IssuePathHelper()
	{
	}

	public static String issueLabel(int issueNumber)
	{
		return "#" + String.format("%0" + PAD_WIDTH + "d", issueNumber);
	}

	public static String fileName(String title, int issueNumber, String extension)
	{
		String name = sanitize(title);
		if (name.isEmpty())
			name = issueLabel(issueNumber);
		return StringUtils.hasText(extension) ? name + "." + extension : name;
	}

	public static Path issueFile(Path seriesPath, int issueNumber, String title, String extension)
	{
		return seriesPath.resolve(issueLabel(issueNumber)).resolve(fileName(title, issueNumber, extension));
	}

	public static Integer parseIssueNumber(String value)
	{
		if (value == null)
			return null;
		String trimmed = value.trim();
		if (!INTEGER.matcher(trimmed).matches())
			return null;
		try
		{
			return Integer.valueOf(trimmed);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static String sanitize(String value)
	{
		if (value == null)
			return "";
		String cleaned = ILLEGAL_CHARACTERS.matcher(value).replaceAll("")
			.replaceAll("\\s+", " ")
			.trim();
		if (cleaned.length() > MAX_NAME_LENGTH)
			cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();

		return cleaned.replaceAll("[. ]+$", "");
	}
}
