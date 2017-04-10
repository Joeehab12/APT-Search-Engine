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
    static public int numThreads;
    public static void main(String[] args) throws Exception{
        BufferedReader consoleReader = new BufferedReader (new InputStreamReader(System.in));
        System.out.println("Enter the number of crawling threads: ");
        numThreads = Integer.parseInt(consoleReader.readLine());

        long startTime = System.currentTimeMillis();

        CrawlStatus crawlStatus = CrawlStatus.getInstance();
        crawlStatus.setMaxPageLimit(10);

        crawlStatus.fetchDB();

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
        long endTime = System.currentTimeMillis();
        System.out.println("Elapsed time: "+(endTime - startTime) + " ms");
    }
}
