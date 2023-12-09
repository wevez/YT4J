# YT4J
YouTube Download and Search API implemented with Java.
## Features
- Download vide or audio
- Search videos and shorts
- Get video detail
## TODO
Currentry working on rewriting the decipher function written in JavaScript in Java
## How to use
1. Create a class that inherits from YT4J and implement the getHTTP and postHTTP functions.
### In case of using [HttpURLConnection](https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html)
```java
public class HttpURLConnectionYT4J extends YT4J {

    @Override
    protected void getHTTP(String URL, String USER_AGENT, Consumer<String> RESPONSE_HANDLER) {
        try {
            final URL requestUrl = new URL(URL);

            final HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestMethod("GET");

            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            final StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            RESPONSE_HANDLER.accept(response.toString());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void postHTTP(String URL, String USER_AGENT, JsonObject JSON, Consumer<String> RESPONSE_HANDLER) {
        try {
            final URL url = new URL(URL);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            final OutputStream outputStream = connection.getOutputStream();
            outputStream.flush();
            outputStream.write(JSON.toString().getBytes());
            outputStream.close();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            final StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            RESPONSE_HANDLER.accept(response.toString());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```
### In case of using [Volley](https://github.com/google/volley)
```java
// TODO
```
2. Create an instance of your custom SC4J and enjoy your scraping!
```java
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
```
## This project contains following libraries.
- [gson](https://github.com/google/gson) For parsing json data.
