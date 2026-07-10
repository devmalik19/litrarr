package devmalik19.litrarr.helper;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesHelper
{
	private static final double MIN_SIMILARITY = 0.80;
	private static final LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
	private static Logger logger = LoggerFactory.getLogger(FilesHelper.class);

	public static boolean isMatch(String term, String filename)
	{
		String s1 = term.toLowerCase().trim();
		String s2 = cleanFilename(filename).toLowerCase().trim();

		logger.debug("Matching {} - {}", s1, s2);

		int distance = levenshtein.apply(s1, s2);
		int maxLen = Math.max(s1.length(), s2.length());
		double similarity = (maxLen == 0) ? 1.0 : (1.0 - (double) distance / maxLen);

		logger.debug("Matching distance - {}, MaxLen - {}, similarity - {}", distance, maxLen, similarity);

		return similarity >= MIN_SIMILARITY;
	}

	public static String cleanFilename(String filename)
	{
		return filename.replaceAll("\\(.*?\\)|\\[.*?\\]|\\.\\w{3,4}$", "").trim();
	}
}
