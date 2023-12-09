package tech.tenamen.yt4j;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tech.tenamen.yt4j.data.*;
import tech.tenamen.yt4j.util.JSONUtil;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
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

    /** Define thr global User-Agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Safari/537.36";

    private static final String DECIPHER_SCRIPT = "var vP={OK:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c},LU:function(a,b){a.splice(0,b)},s2:function(a){a.reverse()}};var HLa=function(a){a=a.split(\"\");vP.s2(a,33);vP.OK(a,62);vP.LU(a,3);vP.s2(a,29);vP.OK(a,11);vP.OK(a,31);return a.join(\"\")};";

    /**
     * Parse YouTube video data from given JsonObject
     *
     * @param videoRenderer JsonObject contains YouTube video data
     * @return YouTube video data object
     */
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
                ), // video length
                parseViewCount(
                        videoRenderer
                                .getAsJsonObject("viewCountText")
                                .get("simpleText")
                                .getAsString()
                ) // view count
        );
    }

    /**
     * Parse YouTube shorts videos data from give JsonObject
     *
     * @param reelShelfRenderer JsonObject contains data of a YouTube shorts videos
     * @return list of shorts video data
     */
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

    /**
     * Parse string of video length to seconds integer of one.
     * Example: 1:42 -> 60 + 42 = 102(sec)
     *
     * @param PLAIN_TEXT string of video length
     * @return seconds integer of video length
     */
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
            case 3:
                lengthSec += Integer.parseInt(SP[0]) * 60 * 60;
                lengthSec += Integer.parseInt(SP[1]) * 60;
                lengthSec += Integer.parseInt(SP[2]);
                break;
            default:
                // video which is longer than one day!?!?
                break;
        }
        return lengthSec;
    }

    /**
     * Converts string of view count to integer of one
     * Example: "114,514 views" -> 114514
     *
     * @param PLAIN_TEXT string of view count
     * @return integer of view count
     */
    private int parseViewCount(final String PLAIN_TEXT) {
        return Integer.parseInt(PLAIN_TEXT.split(" ")[0].replace(",", ""));
    }

    /**
     * Parse YouTube videos from JsonArray
     *
     * @param array JsonArray to be parsed
     * @return List of YouTube videos
     */
    private List<YTData> parseContents(final JsonArray array) {

        final List<YTData> CONTENTS = new ArrayList<>();

        // Parse count token from json.
        // The count token will be used to fetch next page data.
        JSONUtil.streamOf(array)
                .filter(JSONUtil.hasFilter("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationItemRenderer"))
                .map(JSONUtil.getObject("continuationEndpoint"))
                .map(JSONUtil.getObject("continuationCommand"))
                .map(JSONUtil.getString("token"))
                .findAny()
                .ifPresent(s -> countToken = s);

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
    public void startSearch(final Consumer<List<YTData>> ON_SUCCESS, final String KEYWORD, final YTSearchFilterOption FILTER) {
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
                            .get(0)
                            .getAsJsonObject()
                            .getAsJsonObject("appendContinuationItemsAction");

                    final JsonArray continuationItems = item1.getAsJsonArray("continuationItems");

                    ON_SUCCESS.accept(this.parseContents(continuationItems));
                }
        );
    }

    /**
     * Download video and save it to give file
     *
     * @param ON_SUCCESS process executed when searching is success
     * @param VIDEO video to download
     * @param OPTION video download type
     */
    public void getDownloadURL(final Consumer<String> ON_SUCCESS, final YTVideo VIDEO, final YTDLOption OPTION) {
        this.getDownloadURL(ON_SUCCESS, VIDEO.getVideoId(), OPTION);
    }

    /**
     * Download video and save it to give file
     *
     * @param ON_SUCCESS process executed when searching is success
     * @param VIDEO_ID videoId to download
     * @param OPTION video download type
     */
    public void getDownloadURL(final Consumer<String> ON_SUCCESS, final String VIDEO_ID, final YTDLOption OPTION) {
        this.getHTTP(
                String.format("%s/watch?v=%s", HOST_URL, VIDEO_ID),
                USER_AGENT,
                response -> {
                    final JsonObject playerJson = GSON.fromJson(
                            clip(response, "var ytInitialPlayerResponse =", ";var meta"),
                            JsonObject.class
                    );
                    final Optional<JsonObject> bestFormatOption = JSONUtil.streamOf(
                            playerJson
                                    .getAsJsonObject("streamingData")
                                    .getAsJsonArray("adaptiveFormats")
                    ).filter(format -> {
                        final String miniType = format.get("mimeType").getAsString();
                        switch (OPTION.getType()) {
                            case VIDEO_MP4:
                                return miniType.startsWith("video/mp4;");
                            case VIDEO_WEBM:
                                return miniType.startsWith("video/webm;");
                            case AUDIO_MP4:
                                return miniType.startsWith("audio/mp4;");
                            case AUDIO_WEBM:
                                return miniType.startsWith("audio/webm;");
                        }
                        System.out.printf("Unknown miniType: %s\n", OPTION.getType());
                        return false;
                    }).min(Comparator.comparingInt(format -> {
                        switch (OPTION.getType()) {
                            case VIDEO_MP4:
                            case VIDEO_WEBM:
                                return Integer.parseInt(
                                        format
                                                .get("qualityLabel")
                                                .getAsString()
                                                .replaceAll("p", "")
                                );
                            case AUDIO_MP4:
                            case AUDIO_WEBM:
                                return format
                                        .get("averageBitrate")
                                        .getAsInt();
                        }
                        System.out.printf("Unknown miniType: %s\n", OPTION.getType());
                        return 0;
                    }));
                    if (!bestFormatOption.isPresent()) {
                        System.out.printf("Format adapted to %s not found\n", OPTION.toString());
                        return;
                    }
                    ON_SUCCESS.accept(this.getDownloadURL(bestFormatOption.get()));
                    /*
                    final JsonObject initJson = GSON.fromJson(
                            clip(response, "var ytInitialData =", ";</script>"),
                            JsonObject.class
                    );
                    */
                }
        );
    }

    // TODO
    public void getVideoDetail(final Consumer<YTVideoDetail> ON_SUCCESS, final YTVideo VIDEO) {
        this.getHTTP(
                String.format("%s/watch?v=%s", HOST_URL, VIDEO.getVideoId()),
                USER_AGENT,
                response -> {
                    final JsonObject playerJson = GSON.fromJson(
                            clip(response, "var ytInitialPlayerResponse =", ";var meta"),
                            JsonObject.class
                    );
                    final JsonObject videoDetails = playerJson.getAsJsonObject("videoDetails");
                    final JsonObject playerMicroformatRenderer = playerJson
                            .getAsJsonObject("microformat")
                            .getAsJsonObject("playerMicroformatRenderer");
                    ON_SUCCESS.accept(new YTVideoDetail(
                            playerMicroformatRenderer
                                    .getAsJsonObject("description")
                                    .get("simpleText")
                                    .getAsString(), // description
                            playerMicroformatRenderer
                                    .get("category")
                                    .getAsString(), // category
                            playerMicroformatRenderer
                                    .get("publishDate")
                                    .getAsString(), // publish date
                            playerMicroformatRenderer
                                    .get("uploadDate")
                                    .getAsString(), // upload date
                            JSONUtil.streamOfStr(videoDetails.getAsJsonArray("keywords"))
                                    .toArray(String[]::new), // keywords
                            JSONUtil.streamOfStr(playerMicroformatRenderer.getAsJsonArray("availableCountries"))
                                    .toArray(String[]::new), // available countries
                            playerMicroformatRenderer.get("isFamilySafe").getAsBoolean() // family safe
                    ));
                    /*
                    final JsonObject initJson = GSON.fromJson(
                            clip(response, "var ytInitialData =", ";</script>"),
                            JsonObject.class
                    );
                    System.out.println(initJson);
                    */
                }
        );
    }

    private String getDownloadURL(final JsonObject format) {
        if (format.has("url")) {
            return format.get("url").getAsString();
        }
        final String decodedURL = URLDecoder.decode(format.get("signatureCipher").getAsString());
        String sig = null;
        sig = DecipherJS.HLa(clip(decodedURL, "s=", "&sp=sig&url="));
        return String.format(
                "%s&sig=%s",
                decodedURL.substring(decodedURL.indexOf("&sp=sig&url=") + "&sp=sig&url=".length()),
                sig
        );
    }

    /**
     * A method that extracts a substring between a specified start string and end string from a specified string.
     *
     * @param target Target string
     * @param first start string
     * @param last end string
     * @return Substring between start string and end string
     */
    private static String clip(final String target, final String first, final String last) {
        final int startIndex = target.indexOf(first) + first.length();
        return target.substring(startIndex, target.indexOf(last, startIndex));
    }

    protected abstract void getHTTP(final String URL, final String USER_AGENT, final Consumer<String> RESPONSE_HANDLER);
    protected abstract void postHTTP(final String URL, final String USER_AGENT, final JsonObject JSON, final Consumer<String> RESPONSE_HANDLER);
}
