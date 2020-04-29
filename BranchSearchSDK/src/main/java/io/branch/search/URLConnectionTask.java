package io.branch.search;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http2.StreamResetException;

import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.AnalyticsWindowId;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.ApiPerformance;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.RequestId;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.RoundTripTime;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.StartTime;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.StatusCode;
import static io.branch.search.BranchDiscoveryRequest.KEY_REQUEST_ID;

/**
 * URLConnection Task.
 */
class URLConnectionTask extends AsyncTask<Void, Void, JSONObject> {

    private static final MediaType POST_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final long CONFIG_TIMEOUT_MILLIS = 6000;
    private static long startTimeMillis;
    private static int statusCode;

    @VisibleForTesting
    static OkHttpClient sClient = new OkHttpClient.Builder()
            .callTimeout(CONFIG_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build();

    @Override
    protected void onPreExecute() {
        startTimeMillis = System.currentTimeMillis();
    }

    /**
     * Creates a new task for a GET request.
     * @param url target url
     * @param callback callback
     * @return a new task
     */
    @NonNull
    static URLConnectionTask forGet(@NonNull String url,
                                    @Nullable IURLConnectionEvents callback) {
        return new URLConnectionTask(url, new Request.Builder().get(), callback);
    }

    /**
     * Creates a new task for a POST request.
     * @param url target url
     * @param params post params
     * @param callback callback
     * @return a new task
     */
    @NonNull
    static URLConnectionTask forPost(@NonNull String url,
                                     @NonNull JSONObject params,
                                     @Nullable IURLConnectionEvents callback) {
        Request.Builder builder = new Request.Builder()
                .post(RequestBody.create(POST_JSON, params.toString()));
        return new URLConnectionTask(url, builder, callback);

    }

    private final String mUrl;
    private final IURLConnectionEvents mCallback;
    private final Request.Builder mBuilder;
    private final Object mCallbackCalledLock = new Object();
    private boolean mCallbackCalled;
    @VisibleForTesting Call mCall;

    private URLConnectionTask(@NonNull String url,
                              @NonNull Request.Builder builder,
                              @Nullable IURLConnectionEvents callback) {
        mUrl = url;
        mBuilder = builder;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        synchronized (mCallbackCalledLock) {
            if (!mCallbackCalled) {
                if (mCallback != null) {
                    mCallback.onResult(jsonObject);
                }
                mCallbackCalled = true;
            }
        }
        if (jsonObject.has(KEY_REQUEST_ID)) {
            BranchAnalytics.trackObject(ApiPerformance.getKey(), getPerformanceJSON(jsonObject), true);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        synchronized (mCallbackCalledLock) {
            if (!mCallbackCalled) {
                // Ensure we call our callback with the appropriate code.
                if (mCallback != null) {
                    mCallback.onResult(new BranchSearchError(
                            BranchSearchError.ERR_CODE.REQUEST_CANCELED));
                }
                mCallbackCalled = true;
            }
        }
    }

    private JSONObject getPerformanceJSON(JSONObject jsonObject) {
        JSONObject result = new JSONObject();
        try {
            result.putOpt(RequestId.getKey(), jsonObject.optString(KEY_REQUEST_ID));
            result.putOpt(StatusCode.getKey(), statusCode);
            result.putOpt(StartTime.getKey(), startTimeMillis);
            result.putOpt(RoundTripTime.getKey(), System.currentTimeMillis() - startTimeMillis);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
        // If POST, we should have Content-Type: application/json in the request,
        // but this should be already done by OkHttp when creating the post body.
        mBuilder.addHeader("Accept", "application/json");
        mBuilder.addHeader(AnalyticsWindowId.getKey(), BranchAnalytics.getAnalyticsWindowId());
        // Do NOT add "Accept-Encoding"! Instead, rely on OkHttp adding that automatically,
        // which is done in their BridgeInterceptor. If we do add 'just to be sure', then
        // OkHttp will not automatically unzip the response, which would be an issue.
        // mBuilder.addHeader("Accept-Encoding", "gzip");
        mBuilder.url(mUrl);
        return executeRequest();
    }

    @NonNull
    private JSONObject executeRequest() {
        mCall = sClient.newCall(mBuilder.build());
        Response response = null;
        try {
            // Execute the call
            response = mCall.execute();
            long endTime = System.currentTimeMillis();

            // Check the response code
            // If >= 500, retry or return a server error..
            statusCode = response.code();
            if (statusCode >= 500) {
                return new BranchSearchError(BranchSearchError.ERR_CODE.INTERNAL_SERVER_ERR);
            }

            // This should never happen...?
            if (response.body() == null) {
                return new BranchSearchError(BranchSearchError.ERR_CODE.UNKNOWN_ERR);
            }

            // At this point we should have a valid server response
            String body = response.body().string();
            JSONObject result;
            try {
                result = new JSONObject(body);
            } catch (JSONException ignore) {
                return new BranchSearchError(BranchSearchError.ERR_CODE.INTERNAL_SERVER_ERR);
            }

            if (statusCode == 200) {
                // If statusCode == 200, the response body is also our response.
                return result;
            } else {
                // Try to parse an error.
                try {
                    if (result.has("error") && result.getJSONObject("error").has("message")) {
                        return new BranchSearchError(result.getJSONObject("error"));
                    } else if (result.has("code") && result.has("message")) {
                        return new BranchSearchError(result);
                    } else {
                        // Not 200, but does not fit our BranchSearchError scheme.
                        // Return a custom error if >= 400, otherwise return itself.
                        if (statusCode >= 400) {
                            return new BranchSearchError(BranchSearchError.ERR_CODE.convert(statusCode));
                        } else {
                            return result;
                        }
                    }
                } catch (JSONException e) {
                    // Not 200, but something when wrong when inspecting the result. Return itself.
                    return result;
                }
            }
        } catch (StreamResetException | SocketException | InterruptedIOException e) {
            // The meaning of exceptions in these catch blocks is not documented - at least,
            // it's not clear which exceptions are thrown by OkHttp. And even worse, their
            // meaning changes based on the retryOnConnectionFailure() value.
            // If retryOnConnectionFailure() is set to false, please replace InterruptedIOException
            // with SocketTimeoutException here.
            return new BranchSearchError(BranchSearchError.ERR_CODE.REQUEST_TIMED_OUT_ERR);
        } catch (UnknownHostException e) {
            return new BranchSearchError(BranchSearchError.ERR_CODE.BRANCH_NO_CONNECTIVITY_ERR);
        } catch (IOException e) {
            return new BranchSearchError(BranchSearchError.ERR_CODE.UNKNOWN_ERR);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ignore) {}
            }
        }
    }

    @WorkerThread
    void cancel() {
        // Cancel AsyncTask first, then the OkHttp call. If we do the opposite,
        // the executeRequest method can receive a quick IOException and return UNKNOWN_ERR.
        // By canceling the AsyncTask first, we should get the cancel callback first,
        // and correctly dispatch the REQUEST_CANCELED error.
        cancel(true);
        if (mCall != null)  mCall.cancel();
    }
}
