package devmalik19.litrarr.helper;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesHelper
{
	private static final double MIN_SIMILARITY = 0.80;
	private static final LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
	private static final Logger logger = LoggerFactory.getLogger(FilesHelper.class);

	public static boolean isMatch(String term, String filename)
	{
		String s1 = term.toLowerCase().trim();
		String s2 = cleanFilename(filename).toLowerCase().trim();

		logger.debug("Matching {} - {}", s1, s2);

		// Compare against the shorter length to avoid penalizing long torrent/usenet filenames
		// that contain extra metadata (resolution, codec, release group, etc.)
		int distance = levenshtein.apply(s1, s2);
		int minLen = Math.min(s1.length(), s2.length());
		double similarity = (minLen == 0) ? 0.0 : (1.0 - (double) distance / Math.max(s1.length(), s2.length()));

		// Also check if all words from the search term appear in the filename
		String[] terms = s1.split("\\s+");
		long matchedTerms = java.util.Arrays.stream(terms)
			.filter(t -> !t.isEmpty())
			.filter(s2::contains)
			.count();
		double termMatchRatio = terms.length == 0 ? 0.0 : (double) matchedTerms / terms.length;

		logger.debug("Matching distance - {}, similarity - {}, termMatchRatio - {}", distance, similarity, termMatchRatio);

		return similarity >= MIN_SIMILARITY || termMatchRatio >= 1.0;
	}

	public static String cleanFilename(String filename)
	{
		return filename.replaceAll("\\(.*?\\)|\\[.*?\\]|\\.\\w{3,4}$", "").trim();
	}
}
