import java.util.*;
import java.io.File;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.io.FileUtils;
public class Crawler {
	private List<String> urlstoVisit = new LinkedList<String>(); 
	// A list of links to be visited
	private Set<String> visitedUrls = new HashSet<String>(); 
	// The set of links that are visited
	// (It's a set to avoid visiting the same link twice.
	private int max_page_limit = 20;
	// Limit where crawler will stop crawling otherwise it will crawl infinitely
	// User Agent to make web crawler conform to robot exclusion standard.
	private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64)"
            + " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	// nextUrl:Function that works out which link to visit next
	public String nextUrl(){
		String next = urlstoVisit.remove(0); // checks the first link in the (links to visit) list
		while(visitedUrls.contains(next)){ // loops until it finds a link that it didn't visit before
			next = urlstoVisit.remove(0); 
		}	
		visitedUrls.add(next);  // then it marks this link as visited.
		return next;
	}
	// crawl:Function that takes as input a link and downloads all the hyper-links found in that link.
	public boolean crawl(String URL, int pagenum){
		try{
			Connection con = Jsoup.connect(URL).userAgent(USER_AGENT); 
			// Jsoup acquires connection to given URL
			Document htmlDocument = con.get();	
			//gets the Document and parses it 
			if (con.response().contentType().contains("text/html")){ // check if document is HTML or not
				final File f = new File("page" + pagenum + ".html");
		        FileUtils.writeStringToFile(f, htmlDocument.outerHtml(), "UTF-8");
			}
			if(!con.response().contentType().contains("text/html")){
				System.out.println("Only HTML Documents are supported.. ");
				return false;
			}
			Elements URLs = htmlDocument.select("a[href]");  
			// get all hyper-links found on the given URL.
			int file_num = 0;
			System.out.print(String.format("The page: %s contains %d links\n" ,URL,URLs.size()));
			for (Element link : URLs){
				urlstoVisit.add(link.attr("abs:href")); // Add each hyper-link to the links to visit list.
				System.out.println(String.format("Link %d: %s",file_num,link.attr("abs:href")));
				file_num++;
			}
			
			return true;
		}
		catch(Exception e){
			return true;
		}
	}
	//search_loop: Function that starts with a link, crawls and gets it's hyper-links and repeats the same process -
	// for each hyper-link until the maximum page limit is reached.
	public void search_loop(String Url){
		String currentUrl = Url;	
		int file_num = 0;
		while(visitedUrls.size() <= max_page_limit){	// loop to add given link to list of pages to be visited. find its hyper-links and mark it as visited and so on.
			urlstoVisit.add(currentUrl);
			crawl(currentUrl,file_num);
			visitedUrls.add(currentUrl);
			currentUrl = nextUrl();
			file_num++;
		}
	}
	public static void main(String[] args){
		Crawler c = new Crawler();
		c.search_loop("https://www.wikipedia.org");	
	}
}
