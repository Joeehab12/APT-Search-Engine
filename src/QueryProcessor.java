import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import org.bson.Document;

import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * <h1>QueryProcessor</h1>
 *
 * @author Mostafa Fateen
 * @version 1.0
 * @since 08/4/2017
 */
public class QueryProcessor {
    private MongoCollection<Document> collection;

    public QueryProcessor() {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("APT_Search_Engine");
        this.collection = database.getCollection("Inverted_Index");
    }

    List<String> QueryWord(String word) {
        List<String> Result = new LinkedList<>();

        SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
        word = word.toLowerCase();
        word = (String) stemmer.stem(word);

        collection.find(eq("Word", word)).forEach(
                (Block<? super Document>) document -> {
                    List<String> titleUrls = (List<String>) document.get("InTitle");
                    List<String> headUrls = (List<String>) document.get("InHeader");
                    List<String> parUrls = (List<String>) document.get("InParagraph");

                    Result.addAll(titleUrls);
                    Result.addAll(headUrls);
                    Result.addAll(parUrls);
                }
        );
        return Result;
    }
}
