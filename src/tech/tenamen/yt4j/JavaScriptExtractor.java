package tech.tenamen.yt4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaScriptExtractor {

    public static String extractManipulations(String body, String caller) {
        String functionName = YT4J.clip(caller, "a=a.split(\"\");", ".");
        if (functionName.isEmpty()) {
            return "";
        }

        String functionStart = "var " + functionName + "={";
        int ndx = body.indexOf(functionStart);
        if (ndx == -1) {
            return "";
        }

        String subBody = body.substring(ndx + functionStart.length() - 1);

        String cutAfterSubBody = cutAfterJs(subBody);
        if (cutAfterSubBody == null) {
            cutAfterSubBody = "null";
        }

        String returnFormattedString = "var " + functionName + "=" + cutAfterSubBody;

        return returnFormattedString;
    }

    public static void extractDecipher(String body, List<Map<String, String>> functions) {
        String functionName = YT4J.clip(body, "a.set(\"alr\",\"yes\");c&&(c=", "(decodeURIC");

        if (!functionName.isEmpty()) {
            String functionStart = functionName + "=function(a)";
            int ndx = body.indexOf(functionStart);

            if (ndx != -1) {
                String subBody = body.substring(ndx + functionStart.length());

                String cutAfterSubBody = cutAfterJs(subBody);
                if (cutAfterSubBody == null) {
                    cutAfterSubBody = "{}";
                }

                String functionBody = "var " + functionStart + cutAfterSubBody + ";";

                functionBody = extractManipulations(body, functionBody) + functionBody;

                functionBody = functionBody.replaceAll("\\n", "");

                Map<String, String> functionMap = new HashMap<>();
                functionMap.put("name", functionName);
                functionMap.put("body", functionBody);
                functions.add(functionMap);
            }
        }
    }

    public static void extractNcode(String body, List<Map<String, String>> functions) {
        String functionBody;
        String functionNames = extractFunctionNames(body);

        if (!functionNames.isEmpty()) {
            Pattern functionStartPattern = Pattern.compile(functionNames + "=function\\(a\\)");
            Matcher startMatcher = functionStartPattern.matcher(body);

            if (startMatcher.find()) {
                String subBody = body.substring(startMatcher.end());

                String cutAfterSubBody = cutAfterJs(subBody);
                if (cutAfterSubBody == null) {
                    cutAfterSubBody = "{}";
                }

                functionBody = "var " + functionNames + cutAfterSubBody + ";";
                functionBody = functionBody.replaceAll("\\n", "");

                Map<String, String> functionMap = new HashMap<>();
                functionMap.put("name", functionNames);
                functionMap.put("body", functionBody);
                functions.add(functionMap);
            }
        }
    }

    public static String extractFunctionNames(String body) {
        String functionNames = YT4J.clip(body, "&&(b=a.get(\"n\"))&&(b=\"#", "(b)");

        String leftName = "var " + (functionNames.split("\\[")[0] != null ? functionNames.split("\\[")[0] : "") + "=[";

        if (functionNames.contains("[")) {
            functionNames = YT4J.clip(body, leftName, "]");
        }

        return functionNames;
    }

    public static String cutAfterJs(String mixedJson) {
        char open;
        char close;
        switch (mixedJson.charAt(0)) {
            case '[':
                open = '[';
                close = ']';
                break;
            case '{':
                open = '{';
                close = '}';
                break;
            default:
                return null;
        }

        EscapeSequence isEscapedObject = null;

        boolean isEscaped = false;
        int counter = 0;

        char[] mixedJsonCharArray = mixedJson.toCharArray();
        for (int i = 0; i < mixedJsonCharArray.length; i++) {
            char value = mixedJsonCharArray[i];

            if (!isEscaped && isEscapedObject != null && value == isEscapedObject.end) {
                isEscapedObject = null;
                continue;
            } else if (!isEscaped && isEscapedObject == null) {
                for (EscapeSequence escaped : ESCAPING_SEQUENCES) {
                    if (value != escaped.start) {
                        continue;
                    }

                    if (escaped.startPrefix == null) {
                        isEscapedObject = escaped;
                        break;
                    }
                }

                if (isEscapedObject != null) {
                    continue;
                }
            }

            isEscaped = value == '\\' && !isEscaped;

            if (isEscapedObject != null) {
                continue;
            }

            if (value == open) {
                counter++;
            } else if (value == close) {
                counter--;
            }

            if (counter == 0) {
                return mixedJson.substring(0, i + 1);
            }
        }

        return null;
    }

    public static final class EscapeSequence {
        public char start;
        public char end;
        public Pattern startPrefix;

        public EscapeSequence(char start, char end, Pattern startPrefix) {
            this.start = start;
            this.end = end;
            this.startPrefix = startPrefix;
        }
    }

    public static final List<EscapeSequence> ESCAPING_SEQUENCES = Arrays.asList(
            new EscapeSequence('"', '"', null),
            new EscapeSequence('\'', '\'', null),
            new EscapeSequence('`', '`', null)
    );
}
