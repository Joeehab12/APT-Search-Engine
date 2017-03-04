
import java.util.*;
import java.util.concurrent.*;
import javax.swing.plaf.SliderUI;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawler {
	String seed = "http://eng.cu.edu.eg";
	private int max_pages = 20;
	private List<String> urlsToVisit = new LinkedList<String>();
	private Set<String> VisitedUrls = new HashSet<String>();
	private List<String> crawledLinks = new LinkedList<String>();
	private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64)"
            + " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	private Document htmlDocument;
	private List<Document> htmlDocuments = new LinkedList<Document>();
	String nextUrl(){
		String next;
		do{
			next = urlsToVisit.remove(0);
		}while(VisitedUrls.contains(next));
		this.VisitedUrls.add(next);
		return next;
	}
	
	void search(String url,String keyword){
		while(this.VisitedUrls.size() < max_pages){
			String currentUrl;
			if (this.urlsToVisit.isEmpty()){
				currentUrl = url;
				this.VisitedUrls.add(currentUrl);
			}
			else{
				currentUrl = this.nextUrl();
			}
			this.crawl(currentUrl);
			
			boolean success = this.searchForKeyword(keyword);
			if (success){
				System.out.println(String.format("**Success** Word %s found at %s", keyword, currentUrl));
			}
			this.urlsToVisit.addAll(this.getLinks());
			  System.out.println(String.format("**Done** Visited %s web page(s)", this.VisitedUrls.size()));
		}
	}
	public boolean searchForKeyword(String keyword){
		System.out.println("Searching for the word " + keyword + "...");
		String htmlbody = this.htmlDocument.body().text();
		return htmlbody.toLowerCase().contains(keyword.toLowerCase());	
	}
	public boolean crawl(String url){
		try{
		Connection con = Jsoup.connect(url).userAgent(USER_AGENT);
		Document htmlDoc = con.get();
		htmlDocuments.add(htmlDoc);
		this.htmlDocument = htmlDoc;
		if (con.response().statusCode() == 200){
			System.out.println("Visiting Recieved Webpage *** at: " + url);
		}
		if(!con.response().contentType().contains("text/html")){
			System.out.println("Failed*** only HTML Pages are allowed");
			return false;
		}
		Elements linksOnPage = htmlDocument.select("a[href]");
        System.out.println("Found (" + linksOnPage.size() + ") links");
        for(Element link : linksOnPage)
        {
            this.crawledLinks.add(link.absUrl("href"));
        }
        return true;
		}
		catch (Exception e){
			return false;
		}
	}
	 public List<String> getLinks()
	    {
	        return this.crawledLinks;
	    }
	 
	 public void PrintDocs(){
		 for(int i = 0;i< htmlDocuments.size();i++){
			 System.out.println(htmlDocuments.get(i).body().text());
		 }
	 }
	 public static void main(String args[]){
		 WebCrawler crawler = new WebCrawler();
		 crawler.search(crawler.seed, "engineering");
		 crawler.PrintDocs();
		 System.out.println("success");
	 }
}
