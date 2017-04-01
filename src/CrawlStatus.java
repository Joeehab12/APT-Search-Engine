import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;


public class CrawlStatus {
	private static CrawlStatus  singleton = new CrawlStatus();

	private static List<String> urlsToVisit;
	private static Map<String, VisitedUrl> visitedUrls;
	private MongoCollection<Document> collection;

	private CrawlStatus() {
		urlsToVisit = new LinkedList<>();
		visitedUrls = new HashMap<>();

		MongoClient mongoClient = new MongoClient();
		MongoDatabase database = mongoClient.getDatabase("test");
		collection = database.getCollection("test");
	}

	public static CrawlStatus getInstance() {
		return singleton;
	}

	public void addVisitedUrl(String url) {
		VisitedUrl tmp = visitedUrls.get(url);

		if (tmp == null) {
			tmp = new VisitedUrl(url);
			visitedUrls.put(url, tmp);
		} else tmp.increment();

	}

	public void addUrlToVisit(String url) {
		urlsToVisit.add(url);
	}

	public void print() {
		for (Object o : visitedUrls.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			System.out.println(pair.getKey());
			System.out.println(pair.getValue());
		}
	}

	public void persistDB() {
		Document visited = new Document();

		String url;
		int frequency;
		for (Object o : visitedUrls.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			url = (String) pair.getKey();
			frequency = ((VisitedUrl) pair.getValue()).getFrequency();
			visited.append(url, frequency);
		}

		Document doc = new Document("name", "CrawlStatus")
				.append("Visited", visited)
				.append("ToVisit", urlsToVisit);

		collection.updateOne(eq("name", "CrawlStatus"), new Document("$set", doc), new UpdateOptions().upsert(true));
	}

	public void fetchDB () {
		Document doc = collection.find(eq("name", "CrawlStatus")).first();
		urlsToVisit = (List<String>) doc.get("ToVisit");


		Map<String,Integer> tmp = (Map<String, Integer>) doc.get("Visited");

		String url;
		int frequency;
		for (Object o : tmp.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			url = (String) pair.getKey();
			frequency = (int) pair.getValue();
			visitedUrls.put(url, new VisitedUrl(url, frequency));
		}
	}
}

class VisitedUrl {
	private static int nextID = 0;
	private String url;
	private int id;
	private int frequency;

	VisitedUrl(String url) {
		this.id = nextID ++;
		this.frequency = 1;
		this.url = url;
	}

	VisitedUrl(String url, int frequency) {
		this.frequency = frequency;
		this.url = url;
		this.id = nextID ++;
	}

	public void increment() {
		this.frequency++;
	}

	public int getFrequency() {
		return frequency;
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public String toString() {
		return "{\n\tID: " + id + "\n\tURL: " + url + "\n\tFrequency: " + frequency + "\n}\n";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof VisitedUrl)) return false;

		final VisitedUrl other = (VisitedUrl) obj;
		return this.url.equals(other.url);
	}
}
