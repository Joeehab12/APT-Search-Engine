import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.util.*;

/**
 * Indexer.java
 * Purpose: Prepares html data to be stored in the database
 *
 * @author Abdallah Sobehy, Mostafa Fateen, Youssef Ehab
 * @version 1.0 5/3/2017
 */
public class Indexer {

	// Set of words which are not used in the index
	public static final Set<String> stopWordList = new HashSet<>(Arrays.asList("a", "about", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "computer", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herse", "him", "himse", "his", "how", "however", "hundred", "i", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itse", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myse", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves"));
	/**
	 * retrieves words in a given tag of an html document
	 * @param htmlDocument html Document to be scanned
	 * @param tag tag to be fetched
	 * @return list of words in the given tag
	 */
	public static Vector<String> getKeywords(Document htmlDocument, String tag) {
		StringTokenizer stringTokenizer;
		String delimiters = "[ '\n\r.:,_(){}-?!|&$\"+-*/\t]";
		String title = htmlDocument.select(tag).text();
		Vector<String> keyWords = new Vector<>(0);
		stringTokenizer = new StringTokenizer(title, delimiters, false);
		while (stringTokenizer.hasMoreTokens()) {
			String word = stringTokenizer.nextToken();
			SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
			word = word.toLowerCase();
			if (isAccepted(word))
				keyWords.add((String) stemmer.stem(word));
		}
		return keyWords;
	}

	/**
	 * Decides if the given string is accepted to be used in creaitng the index
	 */
	static boolean isAccepted(String word)
	{
		return !(stopWordList.contains(word) || word.length() == 1);
	}
}
