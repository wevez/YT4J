import tech.tenamen.yt4j.YTDLOption;
import tech.tenamen.yt4j.YTDLQuality;
import tech.tenamen.yt4j.YTDLType;
import tech.tenamen.yt4j.YTFilter;
import tech.tenamen.yt4j.data.YTVideo;

public class Main {

    public static void main(String[] args) {
        // Create an instance if CustomSC4J.
        HttpURLConnectionYT4J yt4J = new HttpURLConnectionYT4J();

        // Start searching with title "blue roar".
        yt4J.startSearch(result -> {

            // Start second search.
            yt4J.continueSearch(result2 -> {

                // Print all title and details in the search result.
                result2.forEach(r -> System.out.println(r.toString()));

                // Print the download URL of the lowest quality music of the first video in the search result
                yt4J.getDownloadURL(
                        System.out::println,
                        (YTVideo) result.get(0),
                        new YTDLOption(YTDLType.AUDIO_MP4, YTDLQuality.LOWEST)
                );
            });
        }, "blue roar", YTFilter.VIDEO);
    }
}