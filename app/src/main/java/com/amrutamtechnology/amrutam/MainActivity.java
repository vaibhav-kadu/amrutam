package com.amrutamtechnology.amrutam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ALL_PERMISSIONS = 100;
    private DownloadManager downloadManager;
    private long downloadReference;

    NetworkChangeListener networkChangeListener = new NetworkChangeListener();
    @SuppressLint("StaticFieldLeak")
    private static WebView webView;
    private ValueCallback<Uri[]> mUploadMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    public boolean isRefreshing = false;
    private static boolean isFileChooserOpen = false;
    static View popup, popup_error; // Assuming popup and popup_error are views from your layout
    private DatabaseReference databaseReference;
    private String defaultUrl = "https://amrutamtechnology.com/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("URLS").child("notesproject");

        webView = findViewById(R.id.wv);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        popup = findViewById(R.id.popup); // Adjust based on your layout
        popup_error = findViewById(R.id.popup_error); // Adjust based on your layout
        popup_error.setVisibility(View.GONE);

        // Set up WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setGeolocationEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // Enable offline capabilities

        // Load the URL from Firebase
        loadUrlFromFirebase();

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadMessage = filePathCallback;
                openFileChooser();
                isFileChooserOpen = true;
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefreshLayout.setEnabled(scrollY == 0);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (NetworkChangeListener.Common.isConnectedToInternet(MainActivity.this)) {
                webView.reload();
            }
            swipeRefreshLayout.setRefreshing(false);
            isRefreshing = false;
        });

        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light);

        // Initialize DownloadManager
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        requestPermissions();

        // Subscribe to FCM topic
        FirebaseMessaging.getInstance().subscribeToTopic("notification")
                .addOnCompleteListener(task -> {
                    String msg = "Subscribed to notification topic";
                    if (!task.isSuccessful()) {
                        msg = "Subscription failed";
                    }
                    Log.d("FCM", msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUrlFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                      url="https://recaptcha.cesindia.org/";
                if (url != null && !url.isEmpty()) {
                    popup_error.setVisibility(View.GONE);
                    webView.loadUrl(url);
                } else {
                    //webView.loadUrl(defaultUrl);
                    popup_error.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to read URL from Firebase", error.toException());
                //webView.loadUrl(defaultUrl);
                popup_error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VIDEO",
                    "android.permission.READ_MEDIA_AUDIO"
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6 to Android 12
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        } else { // Below Android 6
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        }
    }

    // Method to handle file download
    private void downloadFile(String url, String title, String description) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(title);
        request.setDescription(description);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        String fileName = URLUtil.guessFileName(url, null, null);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        downloadReference = downloadManager.enqueue(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allPermissionsGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register the BroadcastReceiver for download completion
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(networkChangeListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            registerReceiver(networkChangeListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onStop() {
        // Unregister the BroadcastReceiver for download completion
        unregisterReceiver(downloadReceiver);

        unregisterReceiver(networkChangeListener);
        super.onStop();
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadReference) {
                Toast.makeText(MainActivity.this, "Download completed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                Uri result = data.getData();
                if (result != null && mUploadMessage != null) {
                    Uri[] resultArray = new Uri[]{result};
                    mUploadMessage.onReceiveValue(resultArray);
                    mUploadMessage = null;
                }
            } else {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Choose File"), 1);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public static class NetworkChangeListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Common.isConnectedToInternet(context)) {
                if (!isFileChooserOpen) {
                    webView.reload();
                }
                popup.setVisibility(View.GONE);
            } else {
                popup.setVisibility(View.VISIBLE);
            }
        }

        public static class Common {
            public static boolean isConnectedToInternet(Context context) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                }
                return false;
            }
        }
    }
}
