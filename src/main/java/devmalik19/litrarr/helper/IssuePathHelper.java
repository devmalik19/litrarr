package devmalik19.litrarr.helper;

import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Builds and parses the comics/manga library layout:
 * <pre>
 * &lt;series&gt;/#&lt;zero padded issue number&gt;/&lt;issue title, or #&lt;issue number&gt; if there is no title&gt;.&lt;ext&gt;
 * </pre>
 */
public class IssuePathHelper
{
	private static final int PAD_WIDTH = 3;
	private static final int MAX_NAME_LENGTH = 150;
	private static final Pattern INTEGER = Pattern.compile("\\d+");
	private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

	private IssuePathHelper()
	{
	}

	/** Returns the zero padded label used for the issue folder, for example {@code #001}. */
	public static String issueLabel(int issueNumber)
	{
		return "#" + String.format("%0" + PAD_WIDTH + "d", issueNumber);
	}

	/**
	 * Returns the file name for an issue: the title if there is one, otherwise the issue label.
	 * Characters that are not allowed in file names are removed from the title.
	 */
	public static String fileName(String title, int issueNumber, String extension)
	{
		String name = sanitize(title);
		if (name.isEmpty())
			name = issueLabel(issueNumber);
		return StringUtils.hasText(extension) ? name + "." + extension : name;
	}

	/** Returns the full path for an issue file inside the series folder. */
	public static Path issueFile(Path seriesPath, int issueNumber, String title, String extension)
	{
		return seriesPath.resolve(issueLabel(issueNumber)).resolve(fileName(title, issueNumber, extension));
	}

	/**
	 * Parses a whole number such as {@code 5} or {@code 005}.
	 * Returns null for anything else (null, empty, decimals like {@code 10.5}, text).
	 */
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
		// Windows does not allow names that end with a dot or space
		return cleaned.replaceAll("[. ]+$", "");
	}
}
