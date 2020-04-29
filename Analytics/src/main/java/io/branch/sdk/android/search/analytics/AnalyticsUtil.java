package io.branch.sdk.android.search.analytics;

import android.util.Log;

import java.net.URL;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import static io.branch.sdk.android.search.analytics.BranchAnalytics.LOGTAG;

public class AnalyticsUtil {
    private static final String analyticsUploadUrl = "https://fakeUrl.fakeUrl";


    static void makeUpload(String veryLongString) {

//        int maxLogSize = 1000;
//        for(int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
//            int start = i * maxLogSize;
//            int end = (i+1) * maxLogSize;
//            end = end > veryLongString.length() ? veryLongString.length() : end;
//            Log.i(LOGTAG, veryLongString.substring(start, end));
//        }

        byte[] data = veryLongString.getBytes();
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) new URL(analyticsUploadUrl).openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setFixedLengthStreamingMode(data.length);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(urlConnection.getOutputStream());
            gzipOutputStream.write(data);
            gzipOutputStream.flush();
        } catch (Exception e) {
            Log.i(LOGTAG, "exception uploading = " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
