package io.branch.sdk.android.search.analytics;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Defines {

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
    public static final String BranchKey = "branch_key";
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

    // Clicked link handler types
    // todo remove these once link handling v2 rolls out, use BranchLinkHandler.getClass().getSimpleName() instead. Expected values after link handling v2: ViewIntent, CustomIntent, LaunchIntent, Shortcut, DeepView.
    public static final String Shortcut = "Shortcut";
    public static final String ViewIntent = "ViewIntent";
    public static final String Deepview = "Deepview";

    /** Predefined json keys for analytics module payload. Other keys are accepted but these are expected.
     * */
    @SuppressWarnings("WeakerAccess")
    @StringDef({
            AnalyticsWindowId, Hints, Autosuggest, Search, Clicks, Impressions, ApiPerformance,
            StartTime, RoundTripTime, Failure, Source, Message, BranchKey, DeviceInfo, ConfigInfo,
            EmptySessions, RequestId, ResultId, EntityId, Timestamp, Hint, Autosuggestion, Rank,
            Handler, VirtualRequest, Area, StatusCode
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnalyticsJsonKey {}
}
