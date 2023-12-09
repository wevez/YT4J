package tech.tenamen.yt4j.data;

import java.util.Arrays;

public class YTVideoDetail extends YTData {

    private final String DESCRIPTION, CATEGORY, PUBLISH_DATE, UPLOAD_DATE;
    private final String[] KEYWORDS, AVAILABLE_COUNTRIES;
    private final boolean FAMILY_SAFE;

    public YTVideoDetail(String DESCRIPTION, String CATEGORY, String PUBLISH_DATE, String UPLOAD_DATE, String[] KEYWORDS, String[] AVAILABLE_COUNTRIES, boolean FAMILY_SAFE) {
        this.DESCRIPTION = DESCRIPTION;
        this.CATEGORY = CATEGORY;
        this.PUBLISH_DATE = PUBLISH_DATE;
        this.UPLOAD_DATE = UPLOAD_DATE;
        this.KEYWORDS = KEYWORDS;
        this.AVAILABLE_COUNTRIES = AVAILABLE_COUNTRIES;
        this.FAMILY_SAFE = FAMILY_SAFE;
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    public String getCategory() {
        return CATEGORY;
    }

    public String getPublishDate() {
        return PUBLISH_DATE;
    }

    public String getUploadDate() {
        return UPLOAD_DATE;
    }

    public String[] getKeywords() {
        return KEYWORDS;
    }

    public String[] getAvailableCountries() {
        return AVAILABLE_COUNTRIES;
    }

    public boolean isFamilySafe() {
        return FAMILY_SAFE;
    }
    @Override
    public String toString() {
        return "YTVideoDetail{" +
                "DESCRIPTION='" + DESCRIPTION + '\'' +
                ", CATEGORY='" + CATEGORY + '\'' +
                ", PUBLISH_DATE='" + PUBLISH_DATE + '\'' +
                ", UPLOAD_DATE='" + UPLOAD_DATE + '\'' +
                ", KEYWORDS=" + Arrays.toString(KEYWORDS) +
                ", AVAILABLE_COUNTRIES=" + Arrays.toString(AVAILABLE_COUNTRIES) +
                ", FAMILY_SAFE=" + FAMILY_SAFE +
                '}';
    }

}
