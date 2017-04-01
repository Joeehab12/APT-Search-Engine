import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.StringTokenizer;
import java.util.Vector;

public class Indexer {
	
	public static Vector<String> getTitleKeywords(Document link){	
		StringTokenizer st;
		String delimiters = "[ '\n\r.,_(){}-?!|&$\"+-*/\t]";
		String title = link.select("title").text();
		Vector<String> titleKeywords = new Vector<String>(0);
		st = new StringTokenizer(title,delimiters,false);
		
		while(st.hasMoreTokens()){
			String s = st.nextToken();
			SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
			s = (String) stemmer.stem(s);
			s.toLowerCase();
			titleKeywords.add(s);
		}
		return titleKeywords;
	}
	
	public static Vector<String> getHeaderKeywords(Document link){
		StringTokenizer st;
		Elements headers = link.select("h1,h2,h3,h4,h5,h6");
		String delimiters = "[ '\n\r.,_(){}-?!|&$\"+-*/\t]";
		Vector<String> headerKeywords = new Vector<String>(0);
		for (Element header : headers){
			String headerText = header.text();
			st = new StringTokenizer(headerText,delimiters,false);
			while(st.hasMoreTokens()){
				String s = st.nextToken();
				SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
				s = (String) stemmer.stem(s);
				s.toLowerCase();
				headerKeywords.add(s);
			}
		}
		return headerKeywords;
	}
	
	public static Vector<String> getParagraphKeywords(Document link){
		StringTokenizer st;
		String delimiters = "[ '\n\r.,_(){}-?!|&$\"+-*/\t]";
		Elements paragraphs = link.select("p");
		Vector<String> paragraphKeywords = new Vector<String>(0);
		for (Element paragraph : paragraphs){
			String paragraphText = paragraph.text();
			st = new StringTokenizer(paragraphText,delimiters,false);
			while(st.hasMoreTokens()){
				String s = st.nextToken();
				SnowballStemmer stemmer = new SnowballStemmer(ALGORITHM.ENGLISH);
				s = (String) stemmer.stem(s);
				s.toLowerCase();
				paragraphKeywords.add(s);
			}
		}
		return paragraphKeywords;
	}
}
