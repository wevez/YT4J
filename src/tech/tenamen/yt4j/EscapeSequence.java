package tech.tenamen.yt4j;

import java.util.regex.Pattern;

class EscapeSequence {

    private final String START, END;
    private final Pattern START_PREFIX;

    EscapeSequence(final String START, final String END, final Pattern START_PREFIX) {
        this.START = START;
        this.END = END;
        this.START_PREFIX = START_PREFIX;
    }

    public final String getStart() {
        return this.START;
    }

    public final String getEnd() {
        return this.END;
    }

    public final Pattern getStartPrefix() {
        return this.START_PREFIX;
    }

    @Override
    public String toString() {
        return String.format(
                "start: %s, end: %s, startPrefix: %s",
                this.START,
                this.END,
                this.START_PREFIX.toString()
        );
    }

    public EscapeSequence copy() {
        return new EscapeSequence(
                this.START,
                this.END,
                this.START_PREFIX
        );
    }
}
