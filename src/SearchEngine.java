import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * <h1>SearchEngine</h1>
 *
 * @author Mostafa Fateen
 * @version 1.0
 * @since 01/4/2017
 */
public class SearchEngine {
    public static void main(String[] args) throws Exception{
        CrawlStatus crawlStatus = CrawlStatus.getInstance();
        crawlStatus.setMaxPageLimit(10);
        crawlStatus.fetchDB();
        BufferedReader consoleReader = new BufferedReader (new InputStreamReader(System.in));
        System.out.println("Enter the number of crawling threads: ");
        int numThreads = Integer.parseInt(consoleReader.readLine());

        // list to reference threads
        List<Thread> threads = new LinkedList<>();
        // maximum number of threads upper limit is the number of children in the Seed
        for (int i=0 ; i < numThreads ; i++) {
            threads.add(new Thread(new Crawler( )));
            threads.get(i).start();
        }
        for (int i=0 ; i < numThreads ; i++) {
            threads.get(i).join();
        }

        crawlStatus.persistDB();
    }
}
