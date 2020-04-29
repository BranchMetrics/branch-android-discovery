package io.branch.search;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.branch.search.BranchDiscoveryRequest.KEY_REQUEST_ID;
import static io.branch.search.BranchDiscoveryRequest.KEY_RESULT_ID;

/**
 * Application Result.
 * Contains enough information to open the app or fall back to the play store.
 */
// todo decide if we are going to track BranchAppResult at all (it does not result_id and backend does not understand it as search result)
public class BranchAppResult implements Parcelable {
    private static final String LINK_ENTITY_ID_KEY = "entity_id";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_APP_STORE_ID = "app_store_id";
    private static final String KEY_APP_ICON_URL = "app_icon_url";
    private static final String KEY_APP_SCORE = "score";
    private static final String KEY_APP_DEEP_LINKS = "deep_links";
    private static final String KEY_RANKING_HINT = "ranking_hint";
    private static final String KEY_NOT_INSTALLED_MAX_RESULTS = "not_installed_max_results";
    private static final String KEY_DEEPVIEW_EXTRA_TEXT = "deepview_extra_text";

    private final String app_store_id;
    private final String app_name;
    private final String app_icon_url;
    private String ranking_hint;
    private final float score;
    private final List<BranchLinkResult> deep_links;
    private String request_id;

    BranchAppResult(String appStoreID, String appName, String appIconUrl,
                    @NonNull String rankingHint,
                    float score,
                    @NonNull List<BranchLinkResult> deep_links,
                    @NonNull String requestId) {
        this.app_store_id = appStoreID;
        this.app_name = appName;
        this.app_icon_url = appIconUrl;
        this.ranking_hint = rankingHint;
        this.deep_links = deep_links;
        this.score = score;
        this.request_id = requestId;
    }

    /**
     * @return the App Name.
     */
    public String getAppName() {
        return this.app_name;
    }

    /**
     * @return the Package Name.
     */
    public String getPackageName() {
        return this.app_store_id;
    }

    /**
     * @return the App Icon Url.
     */
    public String getAppIconUrl() {
        return this.app_icon_url;
    }

    /**
     * @return the Ranking Hint.
     */
    @NonNull
    public String getRankingHint() {
        return this.ranking_hint;
    }

    /**
     * Returns true if this link represents an ad.
     * @return true if ad, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean isAd() {
        return ranking_hint.toLowerCase().startsWith("featured");
    }

    /**
     * @return the App Score.
     */
    public float getScore() {
        return this.score;
    }

    /**
     * @return a list of Deep Links.
     */
    @NonNull
    public List<BranchLinkResult> getDeepLinks() { return this.deep_links; }

    /**
     * @return the request ID.
     */
    public String getRequestId() {
        return this.request_id;
    }

    //---- Parcelable implementation -------//
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.app_store_id);
        dest.writeString(this.app_name);
        dest.writeString(this.app_icon_url);
        dest.writeString(this.ranking_hint);
        dest.writeFloat(this.score);
        dest.writeTypedList(this.deep_links);
        dest.writeString(this.request_id);
    }

    private BranchAppResult(Parcel in) {
        this.app_store_id = in.readString();
        this.app_name = in.readString();
        this.app_icon_url = in.readString();
        this.ranking_hint = in.readString();
        this.score = in.readFloat();
        this.deep_links = in.createTypedArrayList(BranchLinkResult.CREATOR);
        this.request_id = in.readString();
    }

    public static final Creator<BranchAppResult> CREATOR = new Creator<BranchAppResult>() {
        @Override
        public BranchAppResult createFromParcel(Parcel source) {
            return new BranchAppResult(source);
        }

        @Override
        public BranchAppResult[] newArray(int size) {
            return new BranchAppResult[size];
        }
    };

    /***
     * Opens the app to the home screen if installed or fallback to play store if opted.
     * @param context Application context
     * @param fallbackToPlayStore If set, opens the app in play store if not installed
     * @return BranchSearchError Return  {@link BranchSearchError} in case of an error else null
     */
    public BranchSearchError openApp(Context context, boolean fallbackToPlayStore) {
        return Util.openApp(context,fallbackToPlayStore, app_store_id)? null : new BranchSearchError(BranchSearchError.ERR_CODE.ROUTING_ERR_UNABLE_TO_OPEN_APP);
    }

    /**
     * Triggers the deep link that will open the app or website and search for the query that the user entered.
     * 1. Try to open the app with deep linking, if app is installed on the device
     * 2. Opens the browser with the web fallback link
     * 3. Opens the play store if none of the above succeeded and fallbackToPlayStore is set to true
     *
     * @param context             Application context
     * @param fallbackToPlayStore If set to {@code true} fallbacks to the Google play if the app is not installed and there is no valid web url.
     * @return BranchSearchError {@link BranchSearchError} object to pass any error with complete action. Null if succeeded.
     * @deprecated the search deep link, if available, will be among other link results for this app
     */
    @Deprecated
    public BranchSearchError openSearchDeepLink(Context context, boolean fallbackToPlayStore) {
        Util.reportUseOfDeprecatedMethod(this.getClass().toString(), "openSearchDeepLink");
        return openApp(context, fallbackToPlayStore);
    }

    /**
     * This method will inform whether the search deep link is available, or if the app will just
     * open to the home page when "openSearchDeepLink" is called.
     *
     * @return boolean  True if the search deep link is available. False if not.
     * @deprecated the search deep link, if available, will be among other link results for this app
     */
    @Deprecated
    public boolean isSearchDeepLinkAvailable() {
        return false;
    }

    @SuppressWarnings("unused")
    @Nullable
    static BranchAppResult createFromJson(@NonNull JSONObject json) {

        // Parse fields and check if installed
        String name = Util.optString(json, KEY_APP_NAME);
        String packageName = Util.optString(json, KEY_APP_STORE_ID);
        String iconUrl = Util.optString(json, KEY_APP_ICON_URL);
        float score = (float) json.optDouble(KEY_APP_SCORE, 0.0);
        String rankingHint = Util.optString(json, KEY_RANKING_HINT);
        String deepviewExtraText = Util.optString(json, KEY_DEEPVIEW_EXTRA_TEXT);
        String requestId = Util.optString(json, KEY_REQUEST_ID);
        Context appContext = BranchSearch.getInstance().getApplicationContext();
        boolean isInstalled = Util.isAppInstalled(appContext, packageName);

        // Parse deep links
        JSONArray linksJson = json.optJSONArray(KEY_APP_DEEP_LINKS);
        List<BranchLinkResult> links = new ArrayList<>();
        if (linksJson != null) {
            for (int j = 0; j < linksJson.length(); j++) {
                JSONObject linkResult = linksJson.optJSONObject(j);
                if (linkResult == null) continue;
                BranchLinkResult link = BranchLinkResult.createFromJson(
                        linkResult,
                        name,
                        packageName,
                        iconUrl,
                        deepviewExtraText,
                        isInstalled,
                        requestId,
                        linkResult.optString(KEY_RESULT_ID));
                if (link != null) {
                    links.add(link);
                }
            }
        }

        // Apply the max results constraint
        // If nothing remains, this app should disappear
        if (!isInstalled) {
            int maxResults = json.optInt(KEY_NOT_INSTALLED_MAX_RESULTS, Integer.MAX_VALUE);
            int max = Math.min(maxResults, links.size());
            links = links.subList(0, max);
        }
        if (links.isEmpty()) {
            return null;
        } else {
            return new BranchAppResult(packageName, name, iconUrl, rankingHint, score, links, requestId);
        }
    }
}

