import com.google.gson.JsonObject;
import tech.tenamen.yt4j.YT4J;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CustomYT4J extends YT4J {

    @Override
    protected String getHTTP(String URL, String USER_AGENT) {
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
            return response.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String postHTTP(String URL, String USER_AGENT, JsonObject JSON) {
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

            return response.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
