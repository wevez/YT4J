package tech.tenamen.yt4j.data;

public class YTShorts extends YTData {

    private final String VIDEO_ID;
    private final String TITLE;
    private final String THUMBNAIL_URL;

    public YTShorts(final String VIDEO_ID, final String TITLE, final String THUMBNAIL_URL) {
        this.VIDEO_ID = VIDEO_ID;
        this.TITLE = TITLE;
        this.THUMBNAIL_URL = THUMBNAIL_URL;
    }

    public final String getVideoId() {
        return this.VIDEO_ID;
    }

    public final String getTitle() {
        return this.TITLE;
    }

    public final String getThumbnailURL() {
        return this.THUMBNAIL_URL;
    }

    @Override
    public String toString() {
        return String.format(
                "title: %s, videoId: %s, thumbnail URL: %s",
                this.TITLE,
                this.VIDEO_ID,
                this.THUMBNAIL_URL
        );
    }
}
