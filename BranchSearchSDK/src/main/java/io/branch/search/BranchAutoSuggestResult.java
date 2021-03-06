package io.branch.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents results of an auto suggest query started from
 * {@link BranchSearch#autoSuggest(BranchAutoSuggestRequest, IBranchAutoSuggestEvents)}.
 */
public class BranchAutoSuggestResult implements Parcelable {
    private static final String KEY_RESULTS = "results";

    private final List<BranchAutoSuggestion> suggestions;

    private BranchAutoSuggestResult(@NonNull List<BranchAutoSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @NonNull
    public List<BranchAutoSuggestion> getSuggestions() {
        return suggestions;
    }

    @NonNull
    static BranchAutoSuggestResult createFromJson(@NonNull JSONObject jsonObject) {
        List<BranchAutoSuggestion> suggestions = new ArrayList<>();
        try {
            JSONArray jsonArray = jsonObject.optJSONArray(KEY_RESULTS);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    suggestions.add(new BranchAutoSuggestion(jsonArray.getString(i)));
                }
            }
        } catch (JSONException ignore) { }
        return new BranchAutoSuggestResult(suggestions);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(suggestions);
    }

    public final static Creator<BranchAutoSuggestResult> CREATOR = new Creator<BranchAutoSuggestResult>() {
        @Override
        public BranchAutoSuggestResult createFromParcel(Parcel source) {
            List<BranchAutoSuggestion> suggestions = new ArrayList<>();
            source.readTypedList(suggestions, BranchAutoSuggestion.CREATOR);
            return new BranchAutoSuggestResult(suggestions);
        }

        @Override
        public BranchAutoSuggestResult[] newArray(int size) {
            return new BranchAutoSuggestResult[size];
        }
    };
}
