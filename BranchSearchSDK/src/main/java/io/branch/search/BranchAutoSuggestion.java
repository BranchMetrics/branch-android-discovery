package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.sdk.android.search.analytics.TrackedEntity;

import static io.branch.sdk.android.search.analytics.Defines.Autosuggest;
import static io.branch.sdk.android.search.analytics.Defines.Autosuggestion;
import static io.branch.sdk.android.search.analytics.Defines.RequestId;
import static io.branch.sdk.android.search.analytics.Defines.ResultId;

/**
 * Represents a single auto suggest result.
 */
public class BranchAutoSuggestion extends TrackedEntity implements Parcelable {
    private final String query;
    private final String requestId;
    private final Integer resultId;

    BranchAutoSuggestion(@NonNull String query, @NonNull String requestId, @NonNull Integer resultId) {
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
    public Integer getResultId() {
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
        dest.writeInt(resultId);
    }

    public final static Creator<BranchAutoSuggestion> CREATOR = new Creator<BranchAutoSuggestion>() {
        @Override
        public BranchAutoSuggestion createFromParcel(Parcel source) {
            //noinspection ConstantConditions
            String query = source.readString();
            String requestId = source.readString();
            Integer resultId = source.readInt();
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
            impression.putOpt(Autosuggestion, getQuery());
            impression.putOpt(RequestId, getRequestId());
            impression.putOpt(ResultId, getResultId());
        } catch (JSONException ignored) {}
        return impression;
    }

    @Override
    protected JSONObject getClickJson() {
        JSONObject click = new JSONObject();
        try {
            click.putOpt(Autosuggestion, getQuery());
            click.putOpt(RequestId, getRequestId());
            click.putOpt(ResultId, getResultId());
        } catch (JSONException ignored) {}
        return click;
    }

    @Override
    protected String getAPI() {
        return Autosuggest;
    }
}
