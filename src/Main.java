import tech.tenamen.yt4j.YTFilter;
import tech.tenamen.yt4j.data.YTVideo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // Create an instance if CustomSC4J.
        HttpURLConnectionYT4J yt4J = new HttpURLConnectionYT4J();
        // Start searching with title "Seikin".
        yt4J.startSearch(result -> {
            yt4J.downloadVideo((YTVideo) result.get(0), new File(""));
            // Start second search.
            //yt4J.continueSearch(result2 -> {

                // Print all title and details in the search result.
                //result2.forEach(r -> System.out.println(r.toString()));
            //});
        }, "blue roar", YTFilter.VIDEO);
    }
}