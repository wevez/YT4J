package tech.tenamen.yt4j;

public class YTSearchResult {

    private final String TITLE;
    private final String VIDEO_ID;
    private final String PUBLISHER;
    private final String THUMBNAIL_URL;

    public YTSearchResult(final String TITLE, final String PUBLISHER, final String VIDEO_ID, final String THUMBNAIL_URL) {
        this.TITLE = TITLE;
        this.VIDEO_ID = VIDEO_ID;
        this.PUBLISHER = PUBLISHER;
        this.THUMBNAIL_URL = THUMBNAIL_URL;
    }

    public final String getTitle() { return this.TITLE; }

    public final String getVideoId() { return this.VIDEO_ID; }

    public final String getPublisher() { return this.PUBLISHER; }

    public final String getThumbnailURL() { return this.THUMBNAIL_URL; }
}
