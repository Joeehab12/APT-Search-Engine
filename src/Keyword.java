import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * <h1>Keyword</h1>
 *
 * @author Mostafa Fateen
 * @version 1.0
 * @since 01/4/2017
 */

public class Keyword {
    private String word;
    private boolean persisted = false;
    private List<String> inUrlTitle = new LinkedList<>(),
            inUrlHeader =  new LinkedList<>(),
            inUrlParagraph = new LinkedList<>();

    public Keyword(String word, String url, int position) {
        this.word = word;
        addReference(url, position);
    }

    public void addReference(String url, int position) {
        if (position >= 5) {
            inUrlTitle.add(url);
            position -= 5;
        }
        if (position >= 3) {
            inUrlHeader.add(url);
            position -= 3;
        }
        if (position == 1)
            inUrlParagraph.add(url);
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted() {
        this.persisted = true;
    }

    public List<String> getInUrlTitle() {
        return inUrlTitle;
    }

    public List<String> getInUrlHeader() {
        return inUrlHeader;
    }

    public List<String> getInUrlParagraph() {
        return inUrlParagraph;
    }

    public void persistWord() {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("APT_Search_Engine");
        MongoCollection<Document> collection = database.getCollection("InvertedIndex");

        Document keyword = new Document("Word", this.word)
                .append("InTitle", inUrlTitle)
                .append("InHeader", inUrlHeader)
                .append("InParagraph", inUrlParagraph);

        collection.updateOne(eq("Word", this.word), new Document("$set", keyword), new UpdateOptions().upsert(true));
    }

}
