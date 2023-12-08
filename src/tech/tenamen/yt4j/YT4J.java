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
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class YT4J {

    public static boolean useJsCache = true;

    /** Need to continue to search */
    private String countToken;
    private JsonObject context;
    private String apiToken;

    /** The global instance of GSon*/
    private static final Gson GSON = new Gson();

    /** The host URL of YouTube */
    private static final String HOST_URL = "https://www.youtube.com";

    /** User-Agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Safari/537.36";

    private static final EscapeSequence[] ESCAPING_SEQUENCES = {
            new EscapeSequence("\"", "\"", null),
            new EscapeSequence("'", "'", null),
            new EscapeSequence("`", "`", null),
            new EscapeSequence("/", "/", Pattern.compile("(?m)(^|[\\[{:;,/])\\s?$"))
    };

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
                ), // length
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
     * Example: "114,514" -> 114514
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

    /**
     * Download video and save it to give file
     *
     * @param VIDEO video to download
     * @param FILE file where video date is downloaded
     */
    public void downloadVideo(final YTVideo VIDEO, final File FILE) {
        this.getHTTP(
                "https://www.youtube.com/watch?v=" + VIDEO.getVideoId(),
                USER_AGENT,
                response -> {
                    this.getHTTP(
                            String.format("%s/s/player/dee96cfa/player_ias.vflset/en_US/base.js", HOST_URL),
                            USER_AGENT,
                            jsResponse -> {
                                //System.out.println(jsResponse);
                                final Map<String, String> functions = extractFunctions(jsResponse);
                                /*
                                final JsonObject playerJson = GSON.fromJson(
                                        clip(response, "var ytInitialPlayerResponse =", ";var meta"),
                                        JsonObject.class
                                );
                                final JsonObject initJson = GSON.fromJson(
                                        clip(response, "var ytInitialData =", ";</script>"),
                                        JsonObject.class
                                );
                                printDbg(response);
                                */
                            }
                    );
                }
        );
    }

    private Map<String, String> extractFunctions(final String response) {
        final Map<String, String> functions = new HashMap<>();
        extractDecipher(response, functions);
        extractNode(response, functions);
        return functions;
    }

    private String extractManipulation(final String body, final String caller) {
        String function_name = clip(caller, "a=a.split(\"\");", ".");
        if (function_name.isEmpty()) {
            System.out.println("function_name is empty in extractManipulation");
            return "";
        }

        String function_start = String.format("var %s={", function_name);
        int ndx = body.indexOf(function_start);

        if (ndx == -1) {
            System.out.println("body.indexOf(function_start) not found in extractManipulation");
            return "";
        }

        String sub_body = body.substring(ndx + function_start.length() - 1);

        String cut_after_sub_body = cutAfterJS(sub_body);

        return String.format("var %s=%s", function_name, cut_after_sub_body);
    }

    private void extractDecipher(final String body, final Map<String, String> functions) {
        final String function_name = clip(body, "a.set(\"alr\",\"yes\");c&&(c=", "(decodeURIC");
        // System.out.println("decipher function name: " + function_name);

        if (!function_name.isEmpty()) {
            String function_start = String.format("%s=function(a)", function_name);
            int ndx = body.indexOf(function_start);

            if (ndx != -1) {
                String sub_body = body.substring(ndx + function_start.length());

                String cut_after_sub_body = cutAfterJS(sub_body);

                String function_body = String.format("var %s=%s", function_start, cut_after_sub_body);

                function_body = String.format(
                        "%s;%s;",
                        extractManipulation(body, function_body), // assuming you have the extractManipulations method
                        function_body
                );

                function_body = function_body.replace("\n", "");

                functions.put(function_name, function_body);
            } else {
                System.out.println("body.indexOf(function_start) not found in extractDecipher");
            }
        }
    }

    private void extractNode(final String body, final Map<String, String> functions) {
        String function_name = clip(body, "&&(b=a.get(\"n\"))&&(b=", "(b)");

        String left_name = String.format("var %s=[", function_name.split("\\[")[0]);

        if (function_name.contains("[")) {
            function_name = clip(body, left_name, "]");
        }

        // System.out.println("ncode function name: " + function_name);

        if (!function_name.isEmpty()) {
            final String function_start = String.format("%s=function(a)", function_name);
            final int ndx = body.indexOf(function_start);

            if (ndx != -1) {
                String sub_body = body.substring(ndx + function_start.length());

                String cut_after_sub_body = cutAfterJS(sub_body);

                String function_body = String.format("var %s;%s;", function_start, cut_after_sub_body);

                function_body = function_body.replace("\n", "");

                functions.put(function_name, function_body);
            } else {
                System.out.println("body.indexOf(function_start) not found in extractNode");
            }
        }
    }

    private String cutAfterJS(final String mixedJson) {
        String open, close;

        switch (mixedJson.substring(0, 1)) {
            case "[":
                open = "[";
                close = "]";
                break;
            case "{":
                open = "{";
                close = "}";
                break;
            default:
                System.out.println("open close not defined in cutAfterJS");
                return null;
        }

        EscapeSequence isEscapedObject = null;
        boolean isEscaped = false;
        int counter = 0;

        List<String> mixedJsonUnicode = new ArrayList<>();
        for (int i = 0; i < mixedJson.length(); ) {
            int codepoint = mixedJson.codePointAt(i);
            int charCount = Character.charCount(codepoint);
            String value = mixedJson.substring(i, i + charCount);
            mixedJsonUnicode.add(value);

            if (!isEscaped && isEscapedObject != null && value.equals(isEscapedObject.getEnd())) {
                isEscapedObject = null;
                i += charCount;
                continue;
            } else if (!isEscaped && isEscapedObject == null) {
                for (EscapeSequence escaped : ESCAPING_SEQUENCES) {
                    if (!value.equals(escaped.getStart())) {
                        continue;
                    }

                    int substringStartNumber = Math.max(0, i - 10);

                    if (escaped.getStartPrefix() == null ||
                            (escaped.getStartPrefix() != null &&
                                    escaped.getStartPrefix().matcher(mixedJson.substring(substringStartNumber, i)).matches())) {
                        isEscapedObject = escaped;
                        break;
                    }
                }

                if (isEscapedObject != null) {
                    i += charCount;
                    continue;
                }
            }

            isEscaped = value.equals("\\") && !isEscaped;

            if (isEscapedObject != null) {
                i += charCount;
                continue;
            }

            if (value.equals(open)) {
                counter++;
            } else if (value.equals(close)) {
                counter--;
            }

            if (counter == 0) {
                return mixedJson.substring(0, i + charCount);
            }

            i += charCount;
        }

        return null;
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

    // For debugging
    private static void printDbg(final String data) {
        try {
            FileWriter file = new FileWriter("C:\\Users\\ryo\\Desktop\\ytoutput.txt");
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            pw.println(data);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
