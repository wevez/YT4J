package tech.tenamen.yt4j;

public class DecipherJS {

    private static void OK(final char[] retVal, final int b) {
        final char c = retVal[0];
        retVal[0] = retVal[b % retVal.length];
        retVal[b % retVal.length] = c;
    }

    private static char[] LU(final char[] a, final int b) {
        final char[] retVal = new char[a.length - b];
        System.arraycopy(a, b, retVal, 0, retVal.length);
        return retVal;
    }

    private static char[] s2(final char[] a) {
        final char[] retVal = new char[a.length];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = a[a.length - 1 - i];
        }
        return retVal;
    }

    static String HLa(final String b) {
        char[] a = b.toCharArray();
        a = s2(a);
        OK(a, 62);
        a = LU(a, 3);
        a = s2(a);
        OK(a, 11);
        OK(a, 31);
        return new String(a);
    }
}
