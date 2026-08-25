package devmalik19.litrarr.helper;

import java.util.Comparator;

/**
 * Provides a natural-order comparator for strings containing numeric segments.
 * Sorts "1, 2, 10, 20" instead of the lexicographic "1, 10, 2, 20".
 */
public class SortingHelper
{
	private static final Comparator<String> COMPARATOR = SortingHelper::compare;

	public static Comparator<String> naturalOrder()
	{
		return COMPARATOR;
	}

	private static int compare(String a, String b)
	{
		int i = 0, j = 0;
		while (i < a.length() && j < b.length())
		{
			char ca = a.charAt(i);
			char cb = b.charAt(j);

			if (Character.isDigit(ca) && Character.isDigit(cb))
			{
				int numA = 0, numB = 0;
				while (i < a.length() && Character.isDigit(a.charAt(i)))
					numA = numA * 10 + (a.charAt(i++) - '0');
				while (j < b.length() && Character.isDigit(b.charAt(j)))
					numB = numB * 10 + (b.charAt(j++) - '0');
				if (numA != numB) return Integer.compare(numA, numB);
			}
			else
			{
				int cmp = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
				if (cmp != 0) return cmp;
				i++;
				j++;
			}
		}
		return Integer.compare(a.length() - i, b.length() - j);
	}
}
