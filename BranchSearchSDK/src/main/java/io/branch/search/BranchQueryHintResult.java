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
import static io.branch.search.BranchDiscoveryRequest.KEY_RESULT_ID;

/**
 * Represents results of a query hint query started from
 * {@link BranchSearch#queryHint(BranchQueryHintRequest, IBranchQueryHintEvents)}.
 */
public class BranchQueryHintResult implements Parcelable {
    private static final String KEY_RESULTS = "results";
    private static final String KEY_SUGGESTION = "suggestion";

    private final List<BranchQueryHint> hints;
    private String requestId;

    private BranchQueryHintResult(@NonNull List<BranchQueryHint> hints, String requestId) {
        this.hints = hints;
        this.requestId = requestId;
    }

    @NonNull
    public List<BranchQueryHint> getHints() {
        return hints;
    }

    @NonNull
    String getRequestId() {
        return requestId;
    }

    @NonNull
    static BranchQueryHintResult createFromJson(@NonNull JSONObject jsonObject) {
        List<BranchQueryHint> hints = new ArrayList<>();
        String requestId = jsonObject.optString(KEY_REQUEST_ID);
        try {
            JSONArray jsonArray = jsonObject.optJSONArray(KEY_RESULTS);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    hints.add(new BranchQueryHint(jsonArray.getString(i), requestId, UUID.randomUUID().toString()));
//                    JSONObject hintResult = jsonArray.getJSONObject(i);
//                    hints.add(new BranchQueryHint(
//                            hintResult.getString(KEY_SUGGESTION),
//                            requestId,
//                            hintResult.getString(KEY_RESULT_ID)));
                }
            }
        } catch (JSONException ignore) { }
        return new BranchQueryHintResult(hints, requestId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(hints);
        dest.writeString(requestId);
    }

    public final static Creator<BranchQueryHintResult> CREATOR = new Creator<BranchQueryHintResult>() {
        @Override
        public BranchQueryHintResult createFromParcel(Parcel source) {
            List<BranchQueryHint> hints = new ArrayList<>();
            source.readTypedList(hints, BranchQueryHint.CREATOR);
            String requestId = source.readString();
            return new BranchQueryHintResult(hints, requestId);
        }

        @Override
        public BranchQueryHintResult[] newArray(int size) {
            return new BranchQueryHintResult[size];
        }
    };
}
