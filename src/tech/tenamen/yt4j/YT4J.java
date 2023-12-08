package tech.tenamen.yt4j;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tech.tenamen.yt4j.data.YTData;
import tech.tenamen.yt4j.data.YTPublisher;
import tech.tenamen.yt4j.data.YTShorts;
import tech.tenamen.yt4j.data.YTVideo;
import tech.tenamen.yt4j.util.JSONUtil;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class YT4J {

    /** Need to continue to search */
    private String countToken;
    private JsonObject context;
    private String apiToken;

    /** The global instance of GSon*/
    private static final Gson GSON = new Gson();

    /** The host URL of YouTube */
    private static final String HOST_URL = "https://www.youtube.com";

    /** User-agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36 OPR/91.0.4516.72 (Edition GX-CN)";

    private YTVideo parseVideo(final JsonObject videoRenderer) {
        final JsonObject owner = videoRenderer
                .getAsJsonObject("ownerText")
                .getAsJsonArray("runs")
                .get(0)
                .getAsJsonObject();

        return new YTVideo(
                videoRenderer
                        .getAsJsonObject("title")
                        .getAsJsonArray("runs")
                        .get(0)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString(), // video title
                new YTPublisher(
                        owner
                                .get("text")
                                .getAsString(),
                        owner
                                .getAsJsonObject("navigationEndpoint")
                                .getAsJsonObject("commandMetadata")
                                .getAsJsonObject("webCommandMetadata")
                                .get("url")
                                .getAsString()
                                .substring(1),
                        videoRenderer
                                .getAsJsonObject("channelThumbnailSupportedRenderers")
                                .getAsJsonObject("channelThumbnailWithLinkRenderer")
                                .getAsJsonObject("thumbnail")
                                .getAsJsonArray("thumbnails")
                                .get(0)
                                .getAsJsonObject()
                                .get("url")
                                .getAsString()
                ), // publisher
                videoRenderer.get("videoId").getAsString(), // video id
                videoRenderer
                        .getAsJsonObject("thumbnail")
                        .getAsJsonArray("thumbnails")
                        .get(0)
                        .getAsJsonObject()
                        .get("url").getAsString(), // thumbnail url
                this.parseLengthSec(
                        videoRenderer
                        .getAsJsonObject("lengthText")
                        .get("simpleText")
                        .getAsString()
                ), // length
                parseViewCount(
                        videoRenderer
                                .getAsJsonObject("viewCountText")
                                .get("simpleText")
                                .getAsString()
                ) // view count
        );
    }

    private List<YTShorts> parseShorts(JsonObject reelShelfRenderer) {
        final List<YTShorts> SHORTS_LIST = new ArrayList<>();

        JSONUtil.streamOf(reelShelfRenderer.getAsJsonArray("items"))
                .map(item -> item.getAsJsonObject("reelItemRenderer"))
                .map(reelItemRenderer -> new YTShorts(
                        reelItemRenderer
                                .get("videoId")
                                .getAsString(), // video id
                        reelItemRenderer
                                .getAsJsonObject("headline")
                                .get("simpleText")
                                .getAsString(), // title
                        reelItemRenderer
                                .getAsJsonObject("thumbnail")
                                .getAsJsonArray("thumbnails")
                                .get(0)
                                .getAsJsonObject()
                                .get("url")
                                .getAsString() // thumbnail url
                ))
                .forEach(SHORTS_LIST::add);
        return SHORTS_LIST;
    }

    private int parseLengthSec(final String PLAIN_TEXT) {
        final String[] SP = PLAIN_TEXT.split(":");
        int lengthSec = 0;
        switch (SP.length) {
            case 1:
                lengthSec += Integer.parseInt(PLAIN_TEXT);
                break;
            case 2:
                lengthSec += Integer.parseInt(SP[0]) * 60;
                lengthSec += Integer.parseInt(SP[1].replaceAll(":", ""));
                break;
        }
        return lengthSec;
    }

    private int parseViewCount(final String PLAIN_TEXT) {
        return Integer.parseInt(PLAIN_TEXT.split(" ")[0].replace(",", ""));
    }

    /**
     * Parse YouTube videos from JsonArray
     *
     * @param array JsonArray to be parse
     * @return List of YouTube videos
     */
    private List<YTData> parseContents(final JsonArray array) {
        try {
            FileWriter file = new FileWriter("C:\\Users\\ryo\\Desktop\\ytoutput.txt");
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            pw.println(array);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final List<YTData> CONTENTS = new ArrayList<>();

        // Parse count token from json.
        // The count token will be used to fetch next page data.
        JSONUtil.streamOf(array)
                .filter(JSONUtil.hasFilter("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationEndpoint"))
                .map(JSONUtil.getObject("continuationCommand"))
                .map(JSONUtil.getString("token"))
                .findAny().ifPresent(s -> countToken = s);

        // Parse video info from json.
        JSONUtil.streamOf(array)
                .filter(JSONUtil.hasFilter("itemSectionRenderer"))
                .map(JSONUtil.getObject("itemSectionRenderer"))
                .map(JSONUtil.getArray("contents"))
                .forEach(contents -> JSONUtil.streamOf(contents)
                        .forEach(renderer -> {
                            if (renderer.has("videoRenderer")) {
                                CONTENTS.add(this.parseVideo(renderer.getAsJsonObject("videoRenderer")));
                            } else if (renderer.has("reelShelfRenderer")) {
                                CONTENTS.addAll(this.parseShorts(renderer.getAsJsonObject("reelShelfRenderer")));
                            }
                        })
                );

        return CONTENTS;
    }

    /**
     * Start search with given title from the first
     *
     * @param ON_SUCCESS process executed when searching is success
     * @param KEYWORD search title
     */
    public void startSearch(final Consumer<List<YTData>> ON_SUCCESS, final String KEYWORD, final YTFilter FILTER) {
        String encodedKeyword = null;
        try {
            encodedKeyword = URLEncoder.encode(KEYWORD, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        this.getHTTP(
                String.format(
                        "%s/results?search_query=%s&sp=%s",
                        HOST_URL,
                        encodedKeyword,
                        FILTER.TAG
                ),
                USER_AGENT,
                response -> {
                    final String[] ytInitData = response.split("var ytInitialData =");

                    // No search result.
                    if (ytInitData.length <= 1) {
                        ON_SUCCESS.accept(new ArrayList<>());
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

                    ON_SUCCESS.accept(this.parseContents(array));
                }
        );
    }

    /**
     * Continue search with current context
     *
     * @param ON_SUCCESS process executed when searching is success
     */
    public void continueSearch(final Consumer<List<YTData>> ON_SUCCESS) {
        if (this.context == null) {
            throw new RuntimeException("Search context is null!(forget calling YT4J#startSearch()?");
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("context", this.context);
        jsonObject.addProperty("continuation", this.countToken);
        this.postHTTP(
                String.format(
                        "%s/youtubei/v1/search?key=%s",
                        HOST_URL,
                        this.apiToken
                ),
                USER_AGENT,
                jsonObject,
                response -> {
                    final JsonObject item1 = GSON.fromJson(response, JsonObject.class)
                            .getAsJsonArray("onResponseReceivedCommands")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("appendContinuationItemsAction");

                    final JsonArray continuationItems = item1.getAsJsonArray("continuationItems");

                    ON_SUCCESS.accept(this.parseContents(continuationItems));
                }
        );
    }

    protected abstract void getHTTP(final String URL, final String USER_AGENT, final Consumer<String> RESPONSE_HANDLER);
    protected abstract void postHTTP(final String URL, final String USER_AGENT, final JsonObject JSON, final Consumer<String> RESPONSE_HANDLER);
}
