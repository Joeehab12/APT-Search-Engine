import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;


public class CrawlStatus {
    private static CrawlStatus singleton = new CrawlStatus();

    private static List<String> urlsToVisit;
    private static Map<String, VisitedUrl> visitedUrls;
    private static Map<String, Keyword> keywords;
    private MongoCollection<Document> collection;
    private MongoClient mongoClient;
    MongoDatabase database;
    private int maxPageLimit;

    private CrawlStatus() {
        urlsToVisit = new LinkedList<>();
        visitedUrls = new HashMap<>();
        keywords = new HashMap<>();


        MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
        builder.maxConnectionIdleTime(1000000000);
        MongoClientOptions opts = builder.build();

        mongoClient = new MongoClient("localhost", opts);
        database = mongoClient.getDatabase("APT_Search_Engine");
    }

    public static CrawlStatus getInstance() {
        return singleton;
    }

    public void setMaxPageLimit(int maxPageLimit) {
        this.maxPageLimit = maxPageLimit;
    }

    synchronized public void addVisitedUrl(String url, List<String> titleKeywords, List<String>headerKeywords, List<String>paragraphKeywords) {
        VisitedUrl tmpUrl = visitedUrls.get(url);
        if (tmpUrl == null) {
            visitedUrls.put(url, new VisitedUrl(url, titleKeywords, headerKeywords, paragraphKeywords));
            addKeyword(url, titleKeywords, headerKeywords, paragraphKeywords);
            maxPageLimit--;
            if(maxPageLimit%50 == 0)
                this.persistDB();
            System.out.println("Remaining number of pages: " + maxPageLimit);
        } else tmpUrl.increment();
    }

    synchronized public void addKeyword(String url, List<String> titleKeywords, List<String>headerKeywords, List<String>paragraphKeywords) {
        for (String titleKeyword : titleKeywords) {
            Keyword tmpKeyword = keywords.get(titleKeyword);

            if (tmpKeyword == null) {
                keywords.put(titleKeyword, new Keyword(titleKeyword, url, Keyword.Position.TITLE));
            } else tmpKeyword.addReference(url, Keyword.Position.TITLE);
        }

        for (String headerKeyword : headerKeywords) {
            Keyword tmpKeyword = keywords.get(headerKeyword);

            if (tmpKeyword == null) {
                keywords.put(headerKeyword, new Keyword(headerKeyword, url, Keyword.Position.HEADER));
            } else tmpKeyword.addReference(url, Keyword.Position.HEADER);
        }

        for (String paragraphKeyword : paragraphKeywords) {
            Keyword tmpKeyword = keywords.get(paragraphKeyword);

            if (tmpKeyword == null) {
                keywords.put(paragraphKeyword, new Keyword(paragraphKeyword, url, Keyword.Position.PARAGRAPH));
            } else tmpKeyword.addReference(url, Keyword.Position.PARAGRAPH);
        }


    }

    public boolean stopCrawling() {
        if (maxPageLimit <= 0)
            return true;
        return false;
    }

    synchronized public void addUrlToVisit(String url) {
        VisitedUrl tmp = visitedUrls.get(url);
        if (tmp == null)
            urlsToVisit.add(url);
        else
            tmp.increment();
}

    synchronized public String getNextUrlToVisit() {
        String url = urlsToVisit.remove(0);
        VisitedUrl tmp = visitedUrls.get(url);
        try {
            while (tmp != null) {
                tmp.increment();
                url = urlsToVisit.remove(0);
                tmp = visitedUrls.get(url);
            }
        } catch (Exception e) {
            return "";
        }
        return url;
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
            if (!visitedUrlObj.isPersisted()) {
                visitedDocs.add(new Document("name", "Visited").append("URL", url).append("Frequency", frequency).append("InTitle", visitedUrlObj.getTitleKeywords())
                        .append("InHeader", visitedUrlObj.getHeaderKeywords())
                        .append("InParagraph", visitedUrlObj.getParagraphKeywords()));
                visitedUrlObj.setPersisted();
                visitedUrlObj.persistURL();
            } else {
                collection.updateOne(eq("URL", url), combine(set("Frequency", frequency)));
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
                keywordDocs.add(new Document("Word", word)
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

    public void fetchDB() {
        collection = database.getCollection("Crawl_Status");
        Document doc = collection.find(eq("name", "ToVisit")).first();
        List<String> tmpUrlsToVisit;
        try {
            tmpUrlsToVisit = (List<String>) doc.get("URLs");
        } catch (Exception e) {
            tmpUrlsToVisit = Crawler.getChildren("https://en.wikipedia.org");
        }

        for (String tmpUrl : tmpUrlsToVisit) {
            addUrlToVisit(tmpUrl);
        }


        collection.find(eq("name", "Visited")).forEach(
                (Block<? super Document>) document -> {
                    final String url = document.getString("URL");
                    VisitedUrl tmpUrl = visitedUrls.get(url);
                    if (tmpUrl == null) {
                        visitedUrls.put(url, new VisitedUrl(url, (int) document.get("Frequency"), true));
                        maxPageLimit--;
                    } else {
                        tmpUrl.increment((int) document.get("Frequency"));
                        tmpUrl.setPersisted();
                    }
                }
        );
    }
}
