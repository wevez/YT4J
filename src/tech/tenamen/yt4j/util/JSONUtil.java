package tech.tenamen.yt4j.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JSONUtil {

    /**
     * Creates a stream of JSONObject from the given JSONArray.
     *
     * @param array the JSONArray to create the stream from
     * @return a stream of JSONObject from the JSONArray
     */
    public static Stream<JsonObject> streamOf(final JsonArray array) {
        return IntStream.range(0, array.size()).mapToObj(i -> array.get(i).getAsJsonObject());
    }

    /**
     * Constructs a function that retrieves a JSONObject with the specified name from a given JSONObject.
     *
     * @param name the name of the JSONObject to retrieve
     * @return a function that retrieves the specified JSONObject from another JSONObject
     */
    public static Function<JsonObject, JsonObject> getObject(final String name) {
        return i -> i.getAsJsonObject(name);
    }

    /**
     * Constructs a predicate that checks if a given JSONObject contains a specific field with the specified name.
     *
     * @param name the name of the field to check for existence in the JSONObject
     * @return a predicate that evaluates to true if the JSONObject contains the specified field, false otherwise
     */
    public static Predicate<JsonObject> hasFilter(final String name) {
        return o -> o.has(name);
    }

    /**
     * Constructs a function that retrieves a String value associated with the specified name from a given JSONObject.
     *
     * @param name name the name of the String value to retrieve
     * @return a function that retrieves the specified String value from the JSONObject
     */
    public static Function<JsonObject, String> getString(final String name) {
        return o -> o.get(name).getAsString();
    }

    /**
     * Constructs a function that retrieves a JSONArray associated with the specified name from a given JSONObject.
     *
     * @param name name the name of the JSONArray to retrieve
     * @return a function that retrieves the specified JSONArray from the JSONObject
     */
    public static Function<JsonObject, JsonArray> getArray(final String name) {
        return o -> o.getAsJsonArray(name);
    }
}
