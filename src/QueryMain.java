import java.util.List;

/**
 * Created by abdallah-sobehy on 5/20/17.
 */
public class QueryMain {
    public static void main(String[] args){
        String phrase = "\"theoretical and practical aspects\"";
        QueryProcessor qp = new QueryProcessor();
        List<String> urls = qp.QueryPhrase(phrase);

        for(int i = 0 ; i < urls.size() ; i++){
            System.out.println(i + " - " + urls.get(i));
        }
    }
}
