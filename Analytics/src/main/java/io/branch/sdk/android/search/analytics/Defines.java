package io.branch.sdk.android.search.analytics;

public class Defines {

    /**
     * json keys
     */
    public static final String AnalyticsWindowId = "analytics_window_id";
    public static final String Hints = "hints";
    public static final String Autosuggest = "autosuggest";
    public static final String Search = "search";
    public static final String Clicks = "clicks";
    public static final String Impressions = "impressions";
    public static final String ApiPerformance = "api_performance";
    public static final String StartTime = "start_time";
    public static final String RoundTripTime = "round_trip_time";
    public static final String Failure = "failures";
    public static final String Source = "source";
    public static final String Message = "message";
    public static final String DeviceInfo = "device_info";
    public static final String ConfigInfo = "config_info";
    public static final String EmptySessions = "empty_sessions";
    public static final String RequestId = "request_id";
    public static final String ResultId = "result_id";
    public static final String EntityId = "entity_id";
    public static final String Timestamp = "timestamp";
    public static final String Hint = "hint";
    public static final String Autosuggestion = "autosuggestion";
    public static final String Rank = "rank";
    public static final String Handler = "handler";
    public static final String VirtualRequest = "virtual_request";
    public static final String Area = "area";
    public static final String StatusCode = "status_code";
    public static final String PreviousAnalyticsWindowId = "prev_analytics_window_id";
    public static final String Url = "url";
    public static final String APIType = "api_type";

    // Clicked link handler types
    // todo remove these once link handling v2 rolls out, use BranchLinkHandler.getClass().getSimpleName().toLowerCase() instead.
    //  Expected values after link handling v2: ViewIntent, CustomIntent, LaunchIntent, Shortcut, DeepView.
    public static final String Shortcut = "shortcut";
    public static final String ViewIntent = "viewIntent";
    public static final String Deepview = "deepview";
    public static final String DeepviewCTA = "deepview_cta";

    /**
     * Other strings
     */
    static final String LOGTAG = "BranchAnalytics";
    static final String BNC_ANALYTICS_PREFS_NAME = "BNC_ANALYTICS_PREFS_NAME";
}
