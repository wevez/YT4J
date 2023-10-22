package tech.tenamen.yt4j;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tech.tenamen.yt4j.util.JSONUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class YT4J {

    private String countToken;
    private JsonObject context;
    private String apiToken;

    /** The global instance of GSon*/
    private static final Gson GSON = new Gson();

    /** The host URL of YouTube */
    private static final String HOST_URL = "https://www.youtube.com";

    /** Video tag for filtering search result */
    private static final String VIDEO_TAG = "EgIQAQ%3D%3D";

    /** User-agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36 OPR/91.0.4516.72 (Edition GX-CN)";

    private int searchOffset = 0;

    private final List<YTSearchResult> SEARCH_RESULT = new ArrayList<>();

    public final List<YTSearchResult> getSearchResult() {
        return this.SEARCH_RESULT;
    }

    private void parseAndAddContents(final JsonArray array) {

        // Parse count token from json.
        // The count token will be used to fetch next page data.
        final Optional<String> countTokenOptional = JSONUtil.streamOf(array)
                .filter(JSONUtil.hasFilter("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationEndpoint"))
                .map(JSONUtil.getObject("continuationCommand"))
                .map(JSONUtil.getString("token"))
                .findAny();

        countTokenOptional.ifPresent(s -> countToken = s);

        // Parse video info from json.
        JSONUtil.streamOf(array)
                .filter(JSONUtil.hasFilter("itemSectionRenderer"))
                .map(JSONUtil.getObject("itemSectionRenderer"))
                .map(JSONUtil.getArray("contents"))
                .forEach(contents -> JSONUtil.streamOf(contents)
                        .filter(JSONUtil.hasFilter("videoRenderer"))
                        .map(JSONUtil.getObject("videoRenderer"))
                        .filter(JSONUtil.hasFilter("videoId"))
                        .forEach(videoRenderer -> {

                            // Parse video info from video renderer.
                            final String videoId = videoRenderer.get("videoId").getAsString();
                            final String thumbnail = videoRenderer
                                    .getAsJsonObject("thumbnail")
                                    .getAsJsonArray("thumbnails")
                                    .get(0).getAsJsonObject()
                                    .get("url").getAsString();
                            final String title = videoRenderer
                                    .getAsJsonObject("title")
                                    .getAsJsonArray("runs")
                                    .get(0).getAsJsonObject()
                                    .get("text").getAsString();
                            String publisher = null;
                            if (
                                    videoRenderer.has("ownerText") &&
                                            videoRenderer.getAsJsonObject("ownerText").has("runs")
                            ) {
                                publisher = videoRenderer
                                        .getAsJsonObject("ownerText")
                                        .getAsJsonArray("runs")
                                        .get(0).getAsJsonObject()
                                        .get("text").getAsString();
                            }

                            // Create a snippet instance and add it to result list.
                            this.SEARCH_RESULT.add(
                                    new YTSearchResult(
                                            title,
                                            publisher,
                                            videoId,
                                            thumbnail
                                    )
                            );

                            this.searchOffset++;
                        })
                );

    }

    public void startSearch(final String KEYWORD) {
        this.searchOffset = 0;

        String encodedKeyword = null;
        try {
            encodedKeyword = URLEncoder.encode(KEYWORD, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        final String response = this.getHTTP(
                String.format(
                        "%s/results?search_query=%s&sp=%s",
                        HOST_URL,
                        encodedKeyword,
                        VIDEO_TAG
                ), USER_AGENT
        );

        final String[] ytInitData = response.split("var ytInitialData =");

        // No search result.
        if (ytInitData.length <= 1) {
            return;
        }

        final String s = ytInitData[1].split("</script>")[0];
        final String data = s.substring(0, s.length() - 1);

        final JsonObject initdata = GSON.fromJson(data, JsonObject.class);;

        // Parse API token which one is needed to search next page.
        this.apiToken = null;
        if (response.split("innertubeApiKey").length > 0) {
            this.apiToken = response
                    .split("innertubeApiKey")[1]
                    .trim()
                    .split(",")[0]
                    .split("\"")[2];
        }

        // Parse context which one is needed to search next page.
        this.context = null;
        if (response.split("INNERTUBE_CONTEXT").length > 0) {
            final String s2 = response
                    .split("INNERTUBE_CONTEXT")[1]
                    .trim();
            this.context = GSON.fromJson(s2.substring(2, s2.length() -2), JsonObject.class);
        }

        final JsonArray array = initdata
                .getAsJsonObject("contents")
                .getAsJsonObject("twoColumnSearchResultsRenderer")
                .getAsJsonObject("primaryContents")
                .getAsJsonObject("sectionListRenderer")
                .getAsJsonArray("contents");

        // This token is needed to search next page.
        this.countToken = null;

        this.parseAndAddContents(array);
    }

    public void continueSearch() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("context", this.context);
        jsonObject.addProperty("continuation", this.countToken);
        final String response = this.postHTTP(
                String.format(
                        "%s/youtubei/v1/search?key=",
                        HOST_URL,
                        this.apiToken
                ),
                USER_AGENT,
                jsonObject
        );

        final JsonObject item1 = GSON.fromJson(response, JsonObject.class)
                .getAsJsonArray("onResponseReceivedCommands")
                .get(0).getAsJsonObject()
                .getAsJsonObject("appendContinuationItemsAction");

        final JsonArray continuationItems = item1.getAsJsonArray("continuationItems");
        this.parseAndAddContents(continuationItems);
    }

    protected abstract String getHTTP(final String URL, final String USER_AGENT);
    protected abstract String postHTTP(final String URL, final String USER_AGENT, final JsonObject JSON);
}
