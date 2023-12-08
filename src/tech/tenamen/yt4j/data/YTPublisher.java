package tech.tenamen.yt4j.data;

public class YTPublisher extends YTData {

    private final String NAME;
    private final String TAG;
    private final String IMAGE_URL;

    public YTPublisher(final String NAME, final String TAG, final String IMAGE_URL) {
        this.NAME = NAME;
        this.TAG = TAG;
        this.IMAGE_URL = IMAGE_URL;
    }

    public final String getName() {
        return this.NAME;
    }

    public final String getTag() {
        return this.TAG;
    }

    public final String IMAGE_URL() {
        return this.IMAGE_URL;
    }

    @Override
    public String toString() {
        return String.format(
                "name: %s, tag: %s, image URL: %s",
                this.NAME,
                this.TAG,
                this.IMAGE_URL
        );
    }
}
