import tech.tenamen.yt4j.YTFilter;

public class Main {
    public static void main(String[] args) {

        // Create an instance if CustomSC4J.
        HttpURLConnectionYT4J yt4J = new HttpURLConnectionYT4J();
        // Start searching with title "Seikin".
        yt4J.startSearch(result -> {
            // Start second search.
            yt4J.continueSearch(result2 -> {
                // Print all title and details in the search result.
                result2.forEach(r -> System.out.println(r.toString()));
            });
        }, "Seikin", YTFilter.VIDEO);
    }
}