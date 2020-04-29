package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.sdk.android.search.analytics.TrackedEntity;

import static io.branch.sdk.android.search.analytics.Defines.Hint;
import static io.branch.sdk.android.search.analytics.Defines.Hints;
import static io.branch.sdk.android.search.analytics.Defines.RequestId;
import static io.branch.sdk.android.search.analytics.Defines.ResultId;

/**
 * Represents a single query hint result.
 */
public class BranchQueryHint extends TrackedEntity implements Parcelable {
    private final String query;
    private final String requestId;
    private final String resultId;

    BranchQueryHint(@NonNull String query, @NonNull String requestId, @NonNull String resultId) {
        this.query = query;
        this.requestId = requestId;
        this.resultId = resultId;
    }

    @NonNull
    public String getQuery() {
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

    @NonNull
    @Override
    public String toString() {
        return query;
    }

    /**
     * Returns a search request that will search for this hint's query.
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

    public final static Creator<BranchQueryHint> CREATOR = new Creator<BranchQueryHint>() {
        @Override
        public BranchQueryHint createFromParcel(Parcel source) {
            //noinspection ConstantConditions
            String hint = source.readString();
            String requestId = source.readString();
            String resultId = source.readString();
            return new BranchQueryHint(hint, requestId, resultId);
        }

        @Override
        public BranchQueryHint[] newArray(int size) {
            return new BranchQueryHint[size];
        }
    };

    @Override
    protected JSONObject getImpressionJson() {
        JSONObject impression = new JSONObject();
        try {
            impression.putOpt(Hint, getQuery());
            impression.putOpt(RequestId, getRequestId());
            impression.putOpt(ResultId, getResultId());
        } catch (JSONException ignored) {}
        return impression;
    }

    @Override
    protected JSONObject getClickJson() {
        JSONObject click = new JSONObject();
        try {
            click.putOpt(Hint, getQuery());
            click.putOpt(RequestId, getRequestId());
            click.putOpt(ResultId, getResultId());
        } catch (JSONException ignored) {}
        return click;
    }

    @Override
    protected String getAPI() {
        return Hints;
    }
}
