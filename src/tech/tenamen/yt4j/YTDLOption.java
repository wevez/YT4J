package tech.tenamen.yt4j;

public class YTDLOption {

    private final YTDLType TYPE;
    private final YTDLQuality QUALITY;

    public YTDLOption(YTDLType TYPE, YTDLQuality QUALITY) {
        this.TYPE = TYPE;
        this.QUALITY = QUALITY;
    }

    public YTDLQuality getQuality() {
        return QUALITY;
    }

    public YTDLType getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "YTDownloadType{" +
                "TYPE=" + TYPE +
                ", QUALITY=" + QUALITY +
                '}';
    }

    public enum YTDLType {

        VIDEO_MP4,
        VIDEO_WEBM,
        AUDIO_WEBM,
        AUDIO_MP4;
    }

    public enum YTDLQuality {

        HIGHEST,
        LOWEST;
    }
}
