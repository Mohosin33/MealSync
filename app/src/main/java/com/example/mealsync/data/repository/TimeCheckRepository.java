package com.example.mealsync.data.repository;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.example.mealsync.util.ClockCheckLogic;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimeCheckRepository implements AutoCloseable {

    private static final String TIME_API_URL =
            "https://timeapi.io/api/Time/current/zone?timeZone=UTC";
    private static final int TIMEOUT_MILLIS = 8_000;

    public interface Callback {
        void onSuccess(long networkTimeMillis, long phoneTimeMillis, long differenceMillis);
        void onFailure(Failure failure);
    }

    public enum Failure {
        OFFLINE,
        TIMEOUT,
        HTTP,
        BAD_JSON,
        OTHER
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void check(Callback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            long requestStartedAt = SystemClock.elapsedRealtime();
            long phoneTimeAtStart = System.currentTimeMillis();
            try {
                connection = (HttpURLConnection) new URL(TIME_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MILLIS);
                connection.setReadTimeout(TIMEOUT_MILLIS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setUseCaches(false);

                int statusCode = connection.getResponseCode();
                if (statusCode < HttpURLConnection.HTTP_OK
                        || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    callback.onFailure(Failure.HTTP);
                    return;
                }

                String responseBody = readFully(connection.getInputStream());
                long networkTimeMillis;
                try {
                    networkTimeMillis = ClockCheckLogic.parseNetworkTimeMillis(responseBody);
                } catch (JSONException | NumberFormatException error) {
                    callback.onFailure(Failure.BAD_JSON);
                    return;
                }

                long elapsedRequestMillis = SystemClock.elapsedRealtime() - requestStartedAt;
                long estimatedPhoneTime = phoneTimeAtStart + elapsedRequestMillis / 2L;
                long differenceMillis = ClockCheckLogic.differenceMillis(
                        networkTimeMillis, estimatedPhoneTime);
                callback.onSuccess(networkTimeMillis, estimatedPhoneTime, differenceMillis);
            } catch (SocketTimeoutException error) {
                callback.onFailure(Failure.TIMEOUT);
            } catch (UnknownHostException error) {
                callback.onFailure(Failure.OFFLINE);
            } catch (IOException error) {
                callback.onFailure(Failure.OTHER);
            } catch (RuntimeException error) {
                callback.onFailure(Failure.OTHER);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @NonNull
    private static String readFully(InputStream inputStream) throws IOException {
        try (InputStream input = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
