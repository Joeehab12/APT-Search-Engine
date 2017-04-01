import java.util.*;
import java.io.*;
import crawlercommons.robots.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.commons.io.FileUtils;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import crawlercommons.robots.SimpleRobotRules.RobotRulesMode;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Crawler.java
 * Purpose: Fetches Urls in web pages starting from a URL seed.
 *
 * @author Abdallah Sobehy, Mostafa Fateen, Youssef Ehab
 * @version 1.0 5/3/2017
 * @ref http://stackoverflow.com/questions/19332982/parsing-robot-txt-using-java-and-identify-whether-an-url-is-allowed
 * @ref https://code.google.com/archive/p/crawler-commons/source/default/source
 */




public class Crawler implements Runnable{
	// A list of links to be visited
	private List<String> urlsToVisit = new LinkedList<String>();

	private static Vector<Map<String,Vector<String>>> paragraphsMap = new Vector<Map<String,Vector<String>>>();
	
	private static Vector<Map<String,Vector<String>>> headersMap = new Vector<Map<String,Vector<String>>>();

	private static Vector<Map<String,Vector<String>>> titlesMap = new Vector<Map<String,Vector<String>>>();

	private static Vector<Map<String,Vector<String>>> urlsMap = new Vector<Map<String,Vector<String>>>();

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
				if (!urlStr.equals("")&& isFetchable(urlStr) && !children.contains(urlStr)) {
					children.add(urlStr);
					System.out.println("Child: " + urlStr);
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
	 * Returns true if the site allows crawler to access it to
	 * adhere to robot's exclusion standard
	 */
	private static boolean isRobotAllowed(String USER_AGENT,String url ) throws Exception
	{
		try {
			URL urlObj = new URL(url);
			String hostId = urlObj.getProtocol() + "://" + urlObj.getHost()
					+ (urlObj.getPort() > -1 ? ":" + urlObj.getPort() : "");
			Map<String, BaseRobotRules> robotsTxtRules = new HashMap<String, BaseRobotRules>();
			BaseRobotRules rules = robotsTxtRules.get(hostId);
			if (rules == null) {
				CloseableHttpClient httpclient = HttpClients.createDefault();
				HttpGet httpget = new HttpGet(hostId + "/robots.txt");
				HttpContext context = new BasicHttpContext();
				HttpResponse response = httpclient.execute(httpget, context);
				if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
					rules = new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
					// consume entity to deallocate connection
					EntityUtils.consumeQuietly(response.getEntity());
				} else {
					BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
					SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
					rules = robotParser.parseContent(hostId, IOUtils.toByteArray(entity.getContent()),
							"text/plain", USER_AGENT);
				}
				robotsTxtRules.put(hostId, rules);
			}
			return rules.isAllowed(url);
		}catch(Exception e)
		{
			System.out.println("isRobotAllowed Exception: " + e.toString());
			return false;
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
			// Check for webpage allowance for robots
			if(!isRobotAllowed(USER_AGENT,URL))
			{
				System.out.println("The url : " + URL + "is not allowed to be fetched by robots");
				return false;
			}
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
			String childUrl;
			for (Element link : URLs){
				childUrl = link.attr("abs:href");
				if(!childUrl.equals("")){ // making sure it is not an empty string
					urlsToVisit.add(link.attr("abs:href"));
				}// Add each hyper-link to the links to visit list.
				
				fileNum++;
			}
			Vector<String> titleKeywords = Indexer.getTitleKeywords(htmlDocument);
			Vector<String> paragraphKeywords = Indexer.getParagraphKeywords(htmlDocument);
			Vector<String> headerKeywords = Indexer.getHeaderKeywords(htmlDocument);
			
			
			Map<String,Vector<String>> titleMap = new HashMap<String, Vector<String>>(0);
			titleMap.put(URL, titleKeywords);
			titlesMap.add(titleMap);
			
			Map<String,Vector<String>> paragraphMap = new HashMap<String, Vector<String>>(0);
			paragraphMap.put(URL, paragraphKeywords);
			paragraphsMap.add(paragraphMap);
			
			Map<String,Vector<String>> headerMap = new HashMap<String, Vector<String>>(0);
			headerMap.put(URL, headerKeywords);
			headersMap.add(headerMap);

			return true;
		}
		catch(Exception e){
			System.out.println("Crawl Exception: " + e.toString());
			return false;
		}
	}
	
	/*public void getUrlsContainingWord(String word){
		Map<String,Vector<String>> mp;
		Set<String> set;
		for (int i = 0;i<urlsMap.size();i++){
			mp = urlsMap.get(i);
			set = mp.keySet();
		}
	}
	*/

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
		String seed = "https://sethgodin.typepad.com/";
		int maxPages = 1;
		List<String> threadsSeeds = getChildren(seed,numThreads);
		// list to reference threads
		List<Thread> threads = new LinkedList<Thread>();
		// maximum number of threads upper limit is the number of children in the Seed
		for (int i=0 ; i < threadsSeeds.size() ; i++) {
			threads.add(new Thread(new Crawler(threadsSeeds.get(i), maxPages, visitedUrls)));
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