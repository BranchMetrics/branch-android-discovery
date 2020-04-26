package io.branch.search;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http2.StreamResetException;

/**
 * URLConnection Task.
 */
class URLConnectionTask extends AsyncTask<Void, Void, JSONObject> {

    private static final MediaType POST_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final long CONFIG_TIMEOUT_MILLIS = 6000;

    @VisibleForTesting
    static OkHttpClient sClient = new OkHttpClient.Builder()
            .callTimeout(CONFIG_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static long sLastPostRTT = -1; // Last post request round trip time
    private static long sLastGetRTT = -1; // Last get request round trip time

    /**
     * Creates a new task for a GET request.
     * @param url target url
     * @param callback callback
     * @return a new task
     */
    @NonNull
    static URLConnectionTask forGet(@NonNull String url,
                                    @Nullable IURLConnectionEvents callback) {
        if (sLastGetRTT >= 0) {
            url = Uri.parse(url)
                    .buildUpon()
                    .appendQueryParameter("lr_rtt", String.valueOf(sLastGetRTT))
                    .build()
                    .toString();
            sLastGetRTT = -1;
        }
        return new URLConnectionTask(url, new Request.Builder().get(), callback, false);
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
        if (sLastPostRTT >= 0) {
            try {
                params.putOpt("lr_rtt", sLastPostRTT);
            } catch (JSONException ignore) {}
            sLastPostRTT = -1;
        }
        Request.Builder builder = new Request.Builder()
                .post(RequestBody.create(POST_JSON, params.toString()));
        return new URLConnectionTask(url, builder, callback, true);

    }

    private final String mUrl;
    private final IURLConnectionEvents mCallback;
    private final Request.Builder mBuilder;
    private final Object mCallbackCalledLock = new Object();
    private final boolean mIsPost;
    private boolean mCallbackCalled;
    @VisibleForTesting Call mCall;

    private URLConnectionTask(@NonNull String url,
                              @NonNull Request.Builder builder,
                              @Nullable IURLConnectionEvents callback,
                              boolean isPost) {
        mUrl = url;
        mBuilder = builder;
        mCallback = callback;
        mIsPost = isPost;
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


    @Override
    protected JSONObject doInBackground(Void... voids) {
        // If POST, we should have Content-Type: application/json in the request,
        // but this should be already done by OkHttp when creating the post body.
        mBuilder.addHeader("Accept", "application/json");
        // Do NOT add "Accept-Encoding"! Instead, rely on OkHttp adding that automatically,
        // which is done in their BridgeInterceptor. If we do add 'just to be sure', then
        // OkHttp will not automatically unzip the response, which would be an issue.
        // mBuilder.addHeader("Accept-Encoding", "gzip");
        mBuilder.url(mUrl);
        return executeRequest();
    }

    @NonNull
    private JSONObject executeRequest() {
        long startTime = System.currentTimeMillis();
        mCall = sClient.newCall(mBuilder.build());
        Response response = null;
        try {
            // Execute the call and save the RTT
            response = mCall.execute();
            long endTime = System.currentTimeMillis();
            if (mIsPost) {
                sLastPostRTT = endTime - startTime;
            } else {
                sLastGetRTT = endTime - startTime;
            }

            // Check the response code
            // If >= 500, retry or return a server error..
            int code = response.code();
            if (code >= 500) {
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

            if (code == 200) {
                // If code == 200, the response body is also our response.
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
                        if (code >= 400) {
                            return new BranchSearchError(BranchSearchError.ERR_CODE.convert(code));
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
