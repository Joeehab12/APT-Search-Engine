import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.Vector;

import static com.mongodb.client.model.Filters.eq;

/**
 * <h1>VisitedUrl</h1>
 *
 * @author Mostafa Fateen
 * @version 1.0
 * @since 01/4/2017
 */
public class VisitedUrl {
    private static long nextID = 0;
    private String url;
    private int frequency;

    private long id;
    private boolean persisted = false;

    VisitedUrl(String url) {
        this.frequency = 1;
        this.url = url;
        id = nextID ++;
    }

    VisitedUrl(String url, long id, int frequency, boolean persisted) {
        this.frequency = frequency;
        this.url = url;
        this.persisted = persisted;
        this.id = id;
    }

    public void increment() { this.frequency++; }

    public void increment(int frequency) { this.frequency += frequency; }

    public int getFrequency() { return frequency; }

    public long getId() { return id; }

    public boolean isPersisted() { return persisted; }

    public void setPersisted() { this.persisted = true; }

    private void persistURL(Vector<String> paragraphs, Vector<String> headers, Vector<String> title){
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("APT_Search_Engine");
        MongoCollection<Document> collection = database.getCollection("Index");

        Document page = new Document("URL", this.url).append("Title", title).append("Headers", headers).append("Paragraphs", paragraphs);

        collection.updateOne(eq("URL", this.url), new Document("$set", page), new UpdateOptions().upsert(true));
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
