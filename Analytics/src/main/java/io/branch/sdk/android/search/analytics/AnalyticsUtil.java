package io.branch.sdk.android.search.analytics;

import android.os.AsyncTask;

import java.net.URL;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import static io.branch.sdk.android.search.analytics.BranchAnalytics.Logd;

public class AnalyticsUtil {
    private static final String analyticsUploadUrl = "https://9b3a54d7.ngrok.io/upload";

    static void makeUpload(final String veryLongString) {

        if (BranchAnalytics.loggingEnabled) {
            // todo fix this so it doesn't print across multiple lines
            int maxLogSize = 1000;
            for(int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
                int start = i * maxLogSize;
                int end = (i+1) * maxLogSize;
                end = Math.min(end, veryLongString.length());
                Logd(veryLongString.substring(start, end));
            }
        }

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                byte[] data = veryLongString.getBytes();
                HttpsURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpsURLConnection) new URL(analyticsUploadUrl).openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setChunkedStreamingMode(0);// 0 = default size chunks
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty("Content-Encoding", "gzip");
                    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(urlConnection.getOutputStream());
                    gzipOutputStream.write(data);
                    gzipOutputStream.close();
                    if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                        Logd("analytics upload was successful");
                    } else {
                        Logd("analytics upload was not successful, status: " + urlConnection.getResponseCode());
                        // todo add retry logic?
                    }
                } catch (Exception e) {
                    Logd("exception when uploading: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return null;
            }
        }.execute(veryLongString);
    }
}
