package tech.tenamen.yt4j;

public class HTMLPLayer {

    private static String OK(final String a, final int b) {
        final char[] retVal = a.toCharArray();
        final char c = retVal[0];
        retVal[0] = retVal[b % a.length()];
        retVal[b % a.length()] = c;
        return new String(retVal);
    }
}
