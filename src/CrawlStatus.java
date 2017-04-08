import java.util.LinkedList;
import java.util.List;

public class CrawlStatus {
	//TODO make singleton
	private static List<String> urlsToVisit;
	private static List<VisitedUrl> visitedUrls;
	
	public CrawlStatus() {
		urlsToVisit = new LinkedList<String>();
		visitedUrls = new LinkedList<VisitedUrl>();
	}
	
	public void addVisitedUrl(String url) {
		VisitedUrl tmp = new VisitedUrl(url);
	}
}

class VisitedUrl {
	private static int nextID = 0;
	String url;
	int id;
	int frequency;
	
	VisitedUrl(String url) {
		this.id = nextID ++;
		this.frequency = 1;
		this.url = url;
	}
	
	public void increment() {
		this.frequency++;
	}
	
}
