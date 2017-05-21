import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

/**
 * <h1>QueryProcessor</h1>
 *
 * @author Mostafa Fateen
 * @version 1.0
 * @ref http://www.java2novice.com/java-interview-programs/sort-a-map-by-value/
 * @since 08/4/2017
 */
public class QueryProcessor {
    private MongoCollection<Document> invertedIndexCollection,indexCollection;

    public QueryProcessor() {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("APT_Search_Engine");
        this.invertedIndexCollection = database.getCollection("Inverted_Index");
        this.indexCollection = database.getCollection("Crawl_Status");
    }

    void printList (List<String> l){
        for(String s : l){
            System.out.println(s);
        }
    }
    /**
     * returns list of urls containing the word
     * @param word the word used to look up urls
     * @param tag if tag = "title" only urls containing the word in the title headers are returned, similarly for "header" and paragraph
     *            any other value for tag means that all urls are returned in order of title - header - paragraph urls
     * @return
     */
    List<String> QueryWord(String word, String tag) {
        List<String> Result = new LinkedList<>();
        List<String> titleUrls = new LinkedList<>();
        List<String> headUrls = new LinkedList<>();
        List<String> parUrls = new LinkedList<>();

        SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
        word = word.toLowerCase();
        word = (String) stemmer.stem(word);
        //System.out.println("query Word -> " + word + " , tag : " +  tag);

        Document document = invertedIndexCollection.find(eq("Word", word)).first();

        //System.out.println("query 2.");

        titleUrls.addAll((List<String>) document.get("InTitle"));
        //System.out.println("title size before set : " + titleUrls.size());
        Set<String> urlSet = new HashSet<>();
        urlSet.addAll(titleUrls);
        titleUrls.clear();
        titleUrls.addAll(urlSet);
        //System.out.println("title size after set : " + titleUrls.size());
        titleUrls = sortUrls(titleUrls);
        //System.out.println("title size after sort : " + titleUrls.size());
        if(tag.equals("title"))
            return titleUrls;


        headUrls.addAll((List<String>) document.get("InHeader"));
        //System.out.println("headerurl size before set : " + headUrls.size());
        urlSet = new HashSet<>();
        urlSet.addAll(headUrls);
        headUrls.clear();
        headUrls.addAll(urlSet);
        //System.out.println("headerurl size before set : " + headUrls.size());
        //printList(headUrls);
        headUrls = sortUrls(headUrls);
        //System.out.println("headerurl size after sort : " + headUrls.size());
        //printList(headUrls);
        if(tag.equals("header"))
            return headUrls;


        parUrls.addAll((List<String>) document.get("InParagraph"));
        //System.out.println("par size before set : " + parUrls.size());
        urlSet = new HashSet<>();
        urlSet.addAll(parUrls);
        parUrls.clear();
        parUrls.addAll(urlSet);
        //System.out.println("par size after set : " + parUrls.size());
        parUrls = sortUrls(parUrls);
        //System.out.println("par size after sort : " + parUrls.size());
        if(tag.equals("paragraph"))
            return parUrls;

        Result.addAll(titleUrls);
        Result.addAll(headUrls);
        Result.addAll(parUrls);
        return Result;
    }

    /**
     * Filters urls that do not match the search phrase in order of words
     * @param urls urls to be filtered
     * @param phraseWords stemmed words of the phrase to be checked against
     * @param tag the html tag for which the url words are to be fetched
     */
    void quotePhraseFilter(List<String> urls , List<String> phraseWords, String tag ){
        //System.out.println("Filtering : " + tag);
        for(int i = 0; i < urls.size(); i++) {
            boolean urlAccepted = false;
            Document urlWordsDoc = indexCollection.find(eq("URL", urls.get(i))).first();
            List<String> urlWords = (List<String>) urlWordsDoc.get(tag);
            //System.out.println("url: "+ urls.get(i) + "Num words : " + urlWords.size() + " Tag: " + tag );
            String firstWord = phraseWords.get(0);
            for (int j = 0; j < urlWords.size(); j++) {
                if (firstWord.equals(urlWords.get(j))) {
                    // Loop on the phrase and check if the phrase exists in the url
                    for (int k = j + 1; k < urlWords.size() && k < j + phraseWords.size(); k++) {
                        if (!urlWords.get(k).equals(phraseWords.get(k - j))) {
                            break;
                        } else if (k == j + phraseWords.size() - 1) {
                            urlAccepted = true;
                        }
                    }
                }
                if (urlAccepted)
                    break;
            }
            if (!urlAccepted) {
                urls.remove(i);
                i--;
            }
        }
    }

    List<String> orderByValue(HashMap<String, Integer> map){
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        List<String> result = new LinkedList<>();
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(set);
        Collections.sort( list, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );
        for(Map.Entry<String, Integer> entry:list){
            //System.out.println(entry.getKey()+" ==== "+entry.getValue());
            result.add(entry.getKey());
        }
        return result;
    }

    List<String> sortUrls(List<String> urls){

        //System.out.println("Sorting !!");
        HashMap<String, Integer> freqMap = new HashMap<>();
        int freq;
        for(String url : urls) {
            Document urlDoc = indexCollection.find(eq("URL", url)).first();
            freq = (int) urlDoc.get("Frequency");
            freqMap.put(url, freq);
        }
        return orderByValue(freqMap);
    }

    /**
     * fetches urls containing the input phrase
     * @param phrase search phrase for which the urls are to be fetched
     * @return a list of urls containing the phrase
     */
    List<String> QueryPhrase(String phrase) {
        SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
        phrase = phrase.toLowerCase();
        boolean quotedPhrase = phrase.charAt(0) == '"' && phrase.charAt(phrase.length() - 1) == '"';
        String delimiters = "[ '\n\r.:,_(){}?!|&$\"+-/*/\t]";
        Vector<String> phraseWords = new Vector<String>(Arrays.asList(phrase.split(delimiters)));

        // Stem and keep only accepted words
        for (int i = 0 ; i < phraseWords.size() ; i++){
            if(!Indexer.isAccepted(phraseWords.get(i))) {
                phraseWords.remove(i);
                i--;
            }
            else
                phraseWords.set(i,(String) stemmer.stem(phraseWords.get(i)) );
        }
        // check if the string is empty
        if(phraseWords.size() == 0 ) {
            return null;
        }
        else if (phraseWords.size() == 1){
            List<String> urls =  QueryWord(phraseWords.get(0),"allTags");
            // remove duplicates
            Set<String> urlSet = new HashSet<>();
            urlSet.addAll(urls);
            urls.clear();
            urls.addAll(urlSet);
            return urls;
        }
        else if(quotedPhrase){
            String firstWord = phraseWords.get(0);
            List<String> firstWordTitleUrls = QueryWord(firstWord,"title");
            List<String> firstWordHeadUrls = QueryWord(firstWord,"header");
            List<String> firstWordParUrls =  QueryWord(firstWord,"paragraph");


            quotePhraseFilter(firstWordTitleUrls, phraseWords, "InTitle");
            quotePhraseFilter(firstWordHeadUrls, phraseWords, "InHeader");
            quotePhraseFilter(firstWordParUrls, phraseWords, "InParagraph");

            firstWordTitleUrls.addAll(firstWordHeadUrls);
            firstWordTitleUrls.addAll(firstWordParUrls);

            // remove duplicates
            Set<String> urlSet = new HashSet<>();
            urlSet.addAll(firstWordTitleUrls);
            firstWordTitleUrls.clear();
            firstWordTitleUrls.addAll(urlSet);
            return firstWordTitleUrls;

        }
        else{ // no quotations
            List<String> globalTitleUrls = new LinkedList<>() ;
            List<String> globalHeadUrls = new LinkedList<>() ;
            List<String> globalParUrls =  new LinkedList<>();
            List<String> Result = new LinkedList<>();
            List<String> searchedWords = new LinkedList<>();

            for(int i = 0; i < phraseWords.size(); i++) {

                if(!searchedWords.contains(phraseWords.get(i))) {
                    globalTitleUrls.addAll(QueryWord(phraseWords.get(i), "title"));
                    globalHeadUrls.addAll(QueryWord(phraseWords.get(i), "header"));
                    globalParUrls.addAll(QueryWord(phraseWords.get(i), "paragraph"));
                    searchedWords.add(phraseWords.get(i));
                }
            }
            Result.addAll(globalTitleUrls);
            Result.addAll(globalHeadUrls);
            Result.addAll(globalParUrls);

            // remove duplicates
            Set<String> urlSet = new HashSet<>();
            urlSet.addAll(Result);
            Result.clear();
            Result.addAll(urlSet);
            return Result;
        }

    }
}
