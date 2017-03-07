import java.util.*;
import java.io.File;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
/**
 * Crawler.java
 * Purpose: Fetches Urls in web pages starting from a URL seed.
 *
 * @author Abdallah Sobehy, Mostafa Fateen, Youssef Ehab
 * @version 1.0 5/3/2017
 */
public class Crawler {
	// A list of links to be visited
	private List<String> urlsToVisit = new LinkedList<String>();
	// The set of links that were visited, used to avoid visiting the same link twice.
	private Set<String> visitedUrls = new HashSet<String>();
	// Limit where crawler will stop crawling otherwise it will crawl infinitely
	private int maxPageLimit = 3;
	// User Agent to make web crawler conform to robot exclusion standard.
	private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64)"
            + " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

	/**
	 * Chooses which link to visit next
	 *
	 * @return next URL to visit
	 */
	public String nextUrl(){
		String next = urlsToVisit.remove(0); // checks the first link in the (links to visit) list
		while(visitedUrls.contains(next)){ // loops until it finds a link that it didn't visit before
			next = urlsToVisit.remove(0);
		}	
		//visitedUrls.add(next);  // then it marks this link as visited.
		return next;
	}

	/**
	 *  takes as input a link and downloads all the hyper-links found in that link.
	 *
	 * @param URL input link to investigate for links
	 * @param pageNum a number given to the input webpage
	 * @return
	 */
	public boolean crawl(String URL, int pageNum){
		try{
			// Jsoup acquires connection to given URL
			Connection con = Jsoup.connect(URL).userAgent(USER_AGENT);
			//gets the Document and parses it
			Document htmlDocument = con.get();
			if (con.response().contentType().contains("text/html")){ // check if document is HTML or not
				final File f = new File("page" + pageNum + ".html");
		        FileUtils.writeStringToFile(f, htmlDocument.outerHtml(), "UTF-8");
			}
			if(!con.response().contentType().contains("text/html")){
				System.out.println("Only HTML Documents are supported.. ");
				return false;
			}
			Elements URLs = htmlDocument.select("a[href]");  
			// get all hyper-links found on the given URL.
			int fileNum = 0;
			System.out.print(String.format("The page: %s contains %d links\n" ,URL,URLs.size()));
			for (Element link : URLs){
				urlsToVisit.add(link.attr("abs:href")); // Add each hyper-link to the links to visit list.
			System.out.println(String.format("Link %d: %s",fileNum,link.attr("abs:href")));
				fileNum++;
			}
			
			return true;
		}
		catch(Exception e){
			return true;
		}
	}
	/**
	 * starts with a link, crawls and gets included hyper-links and repeats the same process
	 * for each hyper-link until the maximum page limit is reached.
	 *
	 * @param Url URL seed to start fetching from
	 */
	public void searchLoop(String Url){
		String currentUrl = Url;	
		int fileNum = 0;
		while(visitedUrls.size() < maxPageLimit){	// loop to add given link to list of pages to be visited. find its hyper-links and mark it as visited and so on.
			urlsToVisit.add(currentUrl);
			crawl(currentUrl,fileNum);
			visitedUrls.add(currentUrl);
			currentUrl = nextUrl();
			fileNum++;
		}
	}
	public static void main(String[] args){
		Crawler c = new Crawler();
		c.searchLoop("https://www.wikipedia.org");
	}
}
