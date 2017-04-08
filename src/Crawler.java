import java.util.*;
import java.io.*;
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
public class Crawler implements Runnable{
	// A list of links to be visited
	private List<String> urlsToVisit = new LinkedList<String>();
	// The set of links that were visited, used to avoid visiting the same link twice.
	private static Set<String> visitedUrls = new HashSet<String>();
	// Limit where crawler will stop crawling otherwise it will crawl infinitely
	private int maxPageLimit;
	// Seed from which other links are extracted
	private String seed;
	// User Agent to make web crawler conform to robot exclusion standard.
	private static final String USER_AGENT =
			"Mozilla/5.0 (Windows NT 6.1; WOW64)"
					+ " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	public Crawler(String seed,int maxPages,Set<String> visitedUrls){
		this.seed = seed;
		this.maxPageLimit = maxPages;
		this.visitedUrls = visitedUrls;
	}

	/**
	 * run method for crawing threads
	 */
	public void run(){
		System.out.println("Thread started with seed: " + this.seed);
		searchLoop(this.seed);
	}
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
	 * Returns a list of children of the given seed
	 * @param seed the url to be searched for links (children)
	 * @param numChildren max number of returned links
	 * @return
	 */
	public static List<String> getChildren(String seed, int numChildren)
	{
		try {
			// Jsoup acquires connection to given URL
			Connection con = Jsoup.connect(seed).userAgent(USER_AGENT);
			//gets the Document and parses it
			Document htmlDocument = con.get();
			if (!con.response().contentType().contains("text/html")) {
				System.out.println("Only HTML Documents are supported.. ");
				return null;
			}
			Elements URLs = htmlDocument.select("a[href]");
			List<String> children = new LinkedList<String>();
			int count = 0;
			for (Element link : URLs) {
				String urlStr = link.attr("abs:href");
				// check that children are non-emty and fetchable
				if (!urlStr.equals("")&& isFetchable(urlStr)) {
					children.add(urlStr);
					count ++;
					if(count == numChildren)
						return children;
				}
			}
			return children;
		}
		catch(Exception e) {
			System.out.println("getChildren Exception: " + e.toString());
			return null;
		}
	}

	/**
	 * Checks if it possible to fetch the input url for other links
	 * @param url input url to be tested
	 * @return
	 */
	public static boolean isFetchable(String url)
	{
		try{
			// Jsoup acquires connection to given URL
			Connection con = Jsoup.connect(url).userAgent(USER_AGENT);
			Document htmlDocument = con.get();
			if (!con.response().contentType().contains("text/html")) {
				System.out.println("Only HTML Documents are supported.. ");
				return false;
			}
		}catch(Exception e)
		{
			System.out.println("isFetchable Exception: " + e.toString());
			return false;
		}
		return true;
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
			//System.out.print(String.format("The page: %s contains %d links\n" ,URL,URLs.size()));
			for (Element link : URLs){
				if(!link.attr("abs:href").equals("")){ // making sure it is not an empty string
					urlsToVisit.add(link.attr("abs:href"));
				}// Add each hyper-link to the links to visit list.
				fileNum++;
			}
			return true;
		}
		catch(Exception e){
			System.out.println("Crawl Exception: " + e.toString());
			return false;
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
			synchronized (visitedUrls) {
				if(visitedUrls.size() < maxPageLimit)
					visitedUrls.add(currentUrl);
			}
			currentUrl = nextUrl();
			fileNum++;
		}
	}
	public static void main(String[] args) throws Exception{
		BufferedReader consoleReader = new BufferedReader (new InputStreamReader(System.in));
		System.out.println("Enter the number of crawling threads: ");
		int numThreads = Integer.parseInt(consoleReader.readLine());
		String seed = "https://www.wikipedia.org";
		List<String> threadsSeeds = getChildren(seed,numThreads);
		// list to reference threads
		List<Thread> threads = new LinkedList<Thread>();
		// maximum number of threads upper limit is the number of children in the Seed
		for (int i=0 ; i < threadsSeeds.size() ; i++) {
			threads.add(new Thread(new Crawler(threadsSeeds.get(i), 10, visitedUrls)));
			threads.get(i).start();
		}
		for (int i=0 ; i < numThreads ; i++) {
			threads.get(i).join();
		}
		System.out.println("Fetched URLS: ");
		int count = 1 ;
		for(String url : visitedUrls) {
			System.out.println(count++ + "- " + url);
		}
	}
}