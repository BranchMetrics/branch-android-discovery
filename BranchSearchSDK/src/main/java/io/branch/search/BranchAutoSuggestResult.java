package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.branch.search.BranchDiscoveryRequest.KEY_REQUEST_ID;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.ResultId;

/**
 * Represents results of an auto suggest query started from
 * {@link BranchSearch#autoSuggest(BranchAutoSuggestRequest, IBranchAutoSuggestEvents)}.
 */
public class BranchAutoSuggestResult implements Parcelable {
    private static final String KEY_RESULTS = "results";
    private static final String KEY_SUGGESTION = "suggestion";

    private final List<BranchAutoSuggestion> suggestions;
    private final String requestId;

    private BranchAutoSuggestResult(@NonNull List<BranchAutoSuggestion> suggestions, @NonNull String requestId) {
        this.suggestions = suggestions;
        this.requestId = requestId;
    }

    @NonNull
    public List<BranchAutoSuggestion> getSuggestions() {
        return suggestions;
    }

    @NonNull
    static BranchAutoSuggestResult createFromJson(@NonNull JSONObject jsonObject) {
        List<BranchAutoSuggestion> suggestions = new ArrayList<>();
        String requestId = jsonObject.optString(KEY_REQUEST_ID);
        try {
            JSONArray jsonArray = jsonObject.optJSONArray(KEY_RESULTS);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    suggestions.add(new BranchAutoSuggestion(jsonArray.getString(i), requestId, UUID.randomUUID().toString()));
                    // todo when v2 linking is rolled out, replace above line with below
//                    JSONObject autoSuggestion = jsonArray.getJSONObject(i);
//                    suggestions.add(new BranchAutoSuggestion(
//                            autoSuggestion.getString(KEY_SUGGESTION),
//                            requestId,
//                            autoSuggestion.getString(ResultId.getKey())));
                }
            }
        } catch (JSONException ignore) { }
        return new BranchAutoSuggestResult(suggestions, requestId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(suggestions);
        dest.writeString(requestId);
    }

    public final static Creator<BranchAutoSuggestResult> CREATOR = new Creator<BranchAutoSuggestResult>() {
        @Override
        public BranchAutoSuggestResult createFromParcel(Parcel source) {
            List<BranchAutoSuggestion> suggestions = new ArrayList<>();
            source.readTypedList(suggestions, BranchAutoSuggestion.CREATOR);
            String requestId = source.readString();
            return new BranchAutoSuggestResult(suggestions, requestId);
        }

        @Override
        public BranchAutoSuggestResult[] newArray(int size) {
            return new BranchAutoSuggestResult[size];
        }
    };
}
