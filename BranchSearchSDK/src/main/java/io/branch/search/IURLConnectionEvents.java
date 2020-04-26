package io.branch.search;

import android.support.annotation.NonNull;

import org.json.JSONObject;

/**
 * URLConnection Event Interface.
 */
public interface IURLConnectionEvents {
    void onResult(@NonNull JSONObject response);
}