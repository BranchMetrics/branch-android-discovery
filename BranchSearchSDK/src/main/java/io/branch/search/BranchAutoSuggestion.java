package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.sdk.android.search.analytics.TrackedEntity;

import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.Autosuggest;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.Autosuggestion;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.RequestId;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.ResultId;

/**
 * Represents a single auto suggest result.
 */
public class BranchAutoSuggestion extends TrackedEntity implements Parcelable {
    private final String query;
    private final String requestId;
    private final String resultId;

    BranchAutoSuggestion(@NonNull String query, @NonNull String requestId, @NonNull String resultId) {
        this.query = query;
        this.requestId = requestId;
        this.resultId = resultId;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    @NonNull
    @Override
    public String toString() {
        return query;
    }

    @NonNull
    public String getRequestId() {
        return requestId;
    }

    @NonNull
    public String getResultId() {
        return resultId;
    }

    /**
     * Returns a search request that will search for this suggestion's query.
     * @return a new search request
     */
    @NonNull
    public BranchSearchRequest toSearchRequest() {
        return BranchSearchRequest.create(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
        dest.writeString(requestId);
        dest.writeString(resultId);
    }

    public final static Creator<BranchAutoSuggestion> CREATOR = new Creator<BranchAutoSuggestion>() {
        @Override
        public BranchAutoSuggestion createFromParcel(Parcel source) {
            //noinspection ConstantConditions
            String query = source.readString();
            String requestId = source.readString();
            String resultId = source.readString();
            return new BranchAutoSuggestion(query, requestId, resultId);
        }

        @Override
        public BranchAutoSuggestion[] newArray(int size) {
            return new BranchAutoSuggestion[size];
        }
    };

    @Override
    protected JSONObject getImpressionJson() {
        JSONObject impression = new JSONObject();
        try {
            impression.putOpt(Autosuggestion.getKey(), getQuery());
            impression.putOpt(RequestId.getKey(), getRequestId());
            impression.putOpt(ResultId.getKey(), getResultId());
        } catch (JSONException ignored) {}
        return impression;
    }

    @Override
    protected JSONObject getClickJson() {
        JSONObject click = new JSONObject();
        try {
            click.putOpt(Autosuggestion.getKey(), getQuery());
            click.putOpt(RequestId.getKey(), getRequestId());
            click.putOpt(ResultId.getKey(), getResultId());
        } catch (JSONException ignored) {}
        return click;
    }

    @Override
    protected String getAPI() {
        return Autosuggest.getKey();
    }
}
