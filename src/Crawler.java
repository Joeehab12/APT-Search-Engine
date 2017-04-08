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
	private Vector<String> titleKeywords, paragraphKeywords, headerKeywords;

	private CrawlStatus crawlStatus = CrawlStatus.getInstance( );

	// User Agent to make web crawler conform to robot exclusion standard.
	private static final String USER_AGENT =
			"Mozilla/5.0 (Windows NT 6.1; WOW64)"
					+ " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

	public Crawler(){}


	/**
	 * run method for crawing threads
	 */
	public void run(){
		System.out.println("Thread started ");
		searchLoop();
	}
	/**
	 * Chooses which link to visit next
	 *
	 * @return next URL to visit
	 */
	public String nextUrl(){
		return crawlStatus.getNextUrlToVisit();
	}

	/**
	 * Returns a list of children of the given seed
	 * @param seed the url to be searched for links (children)
	 * @return
	 */
	public static List<String> getChildren(String seed)
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
					if (count >= SearchEngine.numThreads)
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
	 * @return
	 */
	public boolean crawl(String URL){
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
			if(!con.response().contentType().contains("text/html")){
				System.out.println("Only HTML Documents are supported.. ");
				return false;
			}
			Elements URLs = htmlDocument.select("a[href]");

			// get all hyper-links found on the given URL.
			//System.out.print(String.format("The page: %s contains %d links\n" ,URL,URLs.size()));
			String childUrl;
			for (Element link : URLs){
				childUrl = link.attr("abs:href");
				if(!childUrl.equals("")){ // making sure it is not an empty string
					crawlStatus.addUrlToVisit(childUrl);
				}// Add each hyper-link to the links to visit list.
			}

			titleKeywords = Indexer.getKeywords(htmlDocument,"title");
			paragraphKeywords = Indexer.getKeywords(htmlDocument,"p");
			headerKeywords = Indexer.getKeywords(htmlDocument,"h1,h2,h3,h4,h5,h6");
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
	 */
	public void searchLoop(){
		String currentUrl;
		while(!crawlStatus.stopCrawling()){	// loop to add given link to list of pages to be visited. find its hyper-links and mark it as visited and so on.
			currentUrl = crawlStatus.getNextUrlToVisit();
			if (crawl(currentUrl)) {
				crawlStatus.addVisitedUrl(currentUrl, titleKeywords, headerKeywords, paragraphKeywords);
			}
		}
	}
}