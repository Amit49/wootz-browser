package org.chromium.chrome.browser.browserservices;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;
import org.chromium.components.browser_ui.notifications.ForegroundServiceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
public class WootzAppBackgroundService extends Service {
    private static final String TAG = "WootzService";
    private static final String WOOTZ_JOBS_KEY = "Chrome.Wootzapp.Jobs";
    private static final String WOOTZ_RESULTS_KEY = "Chrome.Wootzapp.JobsResult";
    private static final long FETCH_INTERVAL_MS = 15000; // 15 seconds
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "wootz_service_channel";
    private Timer timer;
    private boolean isRunning;
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Create notification
        Notification notification = createNotification();

        // Handle different Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } 
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification);
        } 
        else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Start job processing
        startJobProcessing();

        return START_STICKY;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wootz Background Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background service for Wootz App");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wootzapp background service")
                .setContentText("Background service is running")
                .setSmallIcon(R.drawable.ic_chrome)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
    @Override
    public void onDestroy() {
        isRunning = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        ForegroundServiceUtils.getInstance().stopForeground(this, 
                NotificationManager.IMPORTANCE_NONE);
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void startJobProcessing() {
        Log.d(TAG, "Job processing started");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning) return;
                processJobs();
            }
        }, 0, FETCH_INTERVAL_MS);
    }
    private void processJobs() {
        Log.d(TAG, "Processing jobs...");
        SharedPreferences prefs = ContextUtils.getAppSharedPreferences();
        String jobsJson = prefs.getString(WOOTZ_JOBS_KEY, "[]");
        String resultsJson = prefs.getString(WOOTZ_RESULTS_KEY, "[]");
        try {
            JSONArray jobs = new JSONArray(jobsJson);
            JSONArray results = new JSONArray(resultsJson);
            // Limit results size (keep last 100 results)
            while (results.length() > 100) {
                results.remove(0);
            }
            
            // Process each URL in jobs list
            for (int i = 0; i < jobs.length(); i++) {
                String url = jobs.getString(i);
                Log.d(TAG, "Fetching URL: " + url);
                String response = fetchUrl(url);
                
                JSONObject result = new JSONObject();
                result.put("url", url);
                result.put("timestamp", System.currentTimeMillis());
                result.put("response", response);
                
                results.put(result);
            }
            // Save results
            prefs.edit()
                .putString(WOOTZ_RESULTS_KEY, results.toString())
                .apply();
            Log.d(TAG, "Results saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error processing jobs", e);
        }
    }
    private String fetchUrl(String urlString) {
        Log.d(TAG, "Fetching URL: " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            Log.d(TAG, "Fetch successful for URL: " + urlString);
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching URL: " + urlString, e);
            return "Error: " + e.getMessage();
        }
    }
}