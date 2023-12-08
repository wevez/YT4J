import com.google.gson.JsonObject;
import tech.tenamen.yt4j.YT4J;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

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
