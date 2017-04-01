import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import static com.mongodb.client.model.Updates.*;


import java.util.*;

import static com.mongodb.client.model.Filters.eq;


public class CrawlStatus {
	private static CrawlStatus  singleton = new CrawlStatus();

	private static List<String> urlsToVisit;
	private static Map<String, VisitedUrl> visitedUrls;
	private static Map<String, Keyword> keywords;
	private MongoCollection<Document> collection;
	MongoDatabase database;
	private int maxPageLimit;

	private CrawlStatus() {
		urlsToVisit = new LinkedList<>();
		visitedUrls = new HashMap<>();
		keywords = new HashMap<>();

		MongoClient mongoClient = new MongoClient();
		database = mongoClient.getDatabase("APT_Search_Engine");
	}

	public static CrawlStatus getInstance() {
		return singleton;
	}

	public int getMaxPageLimit() {
		return maxPageLimit;
	}

	public void setMaxPageLimit(int maxPageLimit) {
		this.maxPageLimit = maxPageLimit;
	}

	synchronized public void addVisitedUrl(String url) {
		VisitedUrl tmpUrl = visitedUrls.get(url);

		if (tmpUrl == null) {
			visitedUrls.put(url, new VisitedUrl(url));
		} else tmpUrl.increment();
	}

	synchronized public void addKeyword(String word, String url, int position) {
		Keyword tmpKeyword = keywords.get(word);

		if (tmpKeyword == null) {
			keywords.put(word, new Keyword(word, url, position));
		} else tmpKeyword.addReference(url, position);
	}

	public void addVisitedUrl (Set<String> visitedUrls){
		for (String visitedUrl : visitedUrls) {
			addVisitedUrl(visitedUrl);
		}
	}

	synchronized public void addUrlToVisit(String url) {
		VisitedUrl tmp = visitedUrls.get(url);
		if (tmp == null)
			urlsToVisit.add(url);
		else
			tmp.increment();
	}

	synchronized public String getNextUrlToVisit() {
		return urlsToVisit.remove(0);
	}

	public void print() {
		for (Object o : visitedUrls.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			System.out.println(pair.getKey());
			System.out.println(pair.getValue());
		}
	}

	public void persistDB() {
		List<Document> visitedDocs = new ArrayList<>();
		List<Document> keywordDocs = new ArrayList<>();
		Document toVisitDoc = new Document("URLs", urlsToVisit);
		collection = database.getCollection("Crawl_Status");
		String url;
		int frequency;
		long id;
		VisitedUrl visitedUrlObj;
		for (Object o : visitedUrls.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			url = (String) pair.getKey();
			visitedUrlObj = ((VisitedUrl) pair.getValue());
			frequency = visitedUrlObj.getFrequency();
			id = visitedUrlObj.getId();
			if (!visitedUrlObj.isPersisted()) {
				visitedDocs.add(new Document("_id", id).append("name", "Visited").append("URL", url).append("Frequency", frequency));
				visitedUrlObj.setPersisted();
			} else {
				collection.updateOne(eq("_id", id), combine(set("Frequency", frequency)));
			}
		}

		collection.updateOne(eq("name", "ToVisit"), new Document("$set", toVisitDoc), new UpdateOptions().upsert(true));
		if (!visitedDocs.isEmpty())
			collection.insertMany(visitedDocs);

		String word;
		Keyword keywordObj;
		collection = database.getCollection("Inverted_Index");
		for (Object o : keywords.entrySet()) {
			Map.Entry pair = (Map.Entry) o;
			word = (String) pair.getKey();
			keywordObj = ((Keyword) pair.getValue());

			if (!keywordObj.isPersisted()) {
				visitedDocs.add(new Document("Word", word)
						.append("InTitle", keywordObj.getInUrlTitle())
						.append("InHeader", keywordObj.getInUrlHeader())
						.append("InParagraph", keywordObj.getInUrlParagraph()));
				keywordObj.setPersisted();
			} else {
				collection.updateOne(eq("Word", word),
						combine(
								set("InTitle", keywordObj.getInUrlTitle()),
								set("InHeader", keywordObj.getInUrlHeader()),
								set("InParagraph", keywordObj.getInUrlParagraph())
						));
			}
		}


		if (!keywordDocs.isEmpty())
			collection.insertMany(keywordDocs);
	}

	public void fetchDB () {
		try {
			collection = database.getCollection("Crawl_Status");
			Document doc = collection.find(eq("name", "ToVisit")).first();
			List<String> tmpUrlsToVisit = (List<String>) doc.get("URLs");

			for (String tmpUrl : tmpUrlsToVisit) {
				addUrlToVisit(tmpUrl);
			}


			collection.find(eq("name", "Visited")).forEach(
					(Block<? super Document>) document -> {
						final String url = document.getString("URL");
						VisitedUrl tmpUrl = visitedUrls.get(url);
						if (tmpUrl == null)
							visitedUrls.put(url, new VisitedUrl(url, (long) document.get("_id"), (int) document.get("Frequency"), true));
						else {
							tmpUrl.increment((int) document.get("Frequency"));
							tmpUrl.setPersisted();
						}
					}
			);
		} catch (Exception e) {
			System.err.println("Error in fetching from database");
		}
	}
}
