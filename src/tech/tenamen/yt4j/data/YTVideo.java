package tech.tenamen.yt4j.data;

public class YTVideo extends YTData {

    private final String TITLE;
    private final String VIDEO_ID;
    private final YTPublisher PUBLISHER;
    private final String THUMBNAIL_URL;
    private final int LENGTH_SEC;
    private final int VIEW_COUNT;

    public YTVideo(
            final String TITLE,
            final YTPublisher PUBLISHER,
            final String VIDEO_ID,
            final String THUMBNAIL_URL,
            final int LENGTH_SEC,
            final int VIEW_COUNT
    ) {
        this.TITLE = TITLE;
        this.VIDEO_ID = VIDEO_ID;
        this.PUBLISHER = PUBLISHER;
        this.THUMBNAIL_URL = THUMBNAIL_URL;
        this.LENGTH_SEC = LENGTH_SEC;
        this.VIEW_COUNT = VIEW_COUNT;
    }

    public final String getTitle() {
        return this.TITLE;
    }

    public final String getVideoId() {
        return this.VIDEO_ID;
    }

    public final YTPublisher getPublisher() {
        return this.PUBLISHER;
    }

    public final String getThumbnailURL() {
        return this.THUMBNAIL_URL;
    }

    public final int getLengthSec() {
        return this.LENGTH_SEC;
    }

    public final int getViewCount() {
        return this.VIEW_COUNT;
    }

    @Override
    public String toString() {
        return String.format(
                "title: %s, videoId: %s, publisher: %s, thumbnail URL: %s view count: %d, length sec: %d",
                this.TITLE,
                this.VIDEO_ID,
                this.PUBLISHER.toString(),
                this.THUMBNAIL_URL,
                this.VIEW_COUNT,
                this.LENGTH_SEC
        );
    }
}
