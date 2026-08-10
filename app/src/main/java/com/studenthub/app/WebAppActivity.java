package com.studenthub.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class WebAppActivity extends Activity {

    private WebView w;
    private ValueCallback<Uri[]> cb;

    private static final int FILE_PICKER = 42;
    private static final int NOTIFICATION_PERMISSION = 1001;
    private static final String CHANNEL_ID = "student_hub_app";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        w = new WebView(this);
        setContentView(w);

        hideNavigationBar();

        WebSettings s = w.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);

        s.setMediaPlaybackRequiresUserGesture(false);

        WebViewAssetLoader loader =
                new WebViewAssetLoader.Builder()
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader.AssetsPathHandler(this)
                        )
                        .build();

        w.setWebViewClient(new WebViewClientCompat() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                return loader.shouldInterceptRequest(
                        request.getUrl()
                );
            }
        });

        w.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
            ) {

                cb = callback;

                try {

                    Intent intent = params.createIntent();

                    intent.putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            true
                    );

                    startActivityForResult(
                            intent,
                            FILE_PICKER
                    );

                    return true;

                } catch (Exception e) {

                    cb = null;
                    return false;
                }
            }
        });

        String page =
                getIntent().getStringExtra("page");

        if (page == null || page.trim().isEmpty()) {

            page =
                    "https://student-hub-five-ashy.vercel.app";
        }

        w.loadUrl(page);

        requestNotificationPermission();

        createNotificationChannel();
    }

    private void hideNavigationBar() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            WindowInsetsController controller =
                    getWindow().getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    );
        }
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION
                );
            }
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Student Hub",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Student Hub app notifications"
            );

            channel.enableVibration(true);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void sendAppNotification(
            String title,
            String message
    ) {

        if (Build.VERSION.SDK_INT >= 33) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        Intent intent =
                new Intent(
                        this,
                        WebAppActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            manager.notify(
                    (int) System.currentTimeMillis(),
                    notification.build()
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != FILE_PICKER) {
            return;
        }

        if (cb == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == RESULT_OK && data != null) {

            if (data.getClipData() != null) {

                int count =
                        data.getClipData().getItemCount();

                results = new Uri[count];

                for (int i = 0; i < count; i++) {

                    results[i] =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                }

            } else if (data.getData() != null) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }
        }

        cb.onReceiveValue(results);
        cb = null;
    }

    @Override
    protected void onResume() {

        super.onResume();

        hideNavigationBar();
    }

    @Override
    public void onWindowFocusChanged(
            boolean hasFocus
    ) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideNavigationBar();
        }
    }

    @Override
    public void onBackPressed() {

        if (w != null && w.canGoBack()) {

            w.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (w != null) {

            w.stopLoading();
            w.destroy();
            w = null;
        }

        super.onDestroy();
    }
}
