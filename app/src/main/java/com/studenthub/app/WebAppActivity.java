package com.studenthub.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.app.ActivityCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class WebAppActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_PICKER =
            5001;

    private static final int
            NOTIFICATION_PERMISSION =
            5002;

    private static final String WEBSITE_URL =
            "https://student-hub-five-ashy.vercel.app";

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        configureWebView();

        hideNavigationBar();

        requestNotificationPermission();

        getFCMToken();

        webView.loadUrl(
                WEBSITE_URL
        );
    }

    private void configureWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setDatabaseEnabled(true);

        settings.setAllowContentAccess(true);

        settings.setAllowFileAccess(false);

        settings.setMediaPlaybackRequiresUserGesture(
                false
        );

        settings.setBuiltInZoomControls(false);

        settings.setDisplayZoomControls(false);

        /*
         * Website → Android bridge
         */
        webView.addJavascriptInterface(
                new AndroidNotificationBridge(),
                "StudentHubAndroid"
        );

        webView.setWebViewClient(
                new WebViewClient()
        );

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean
                    onShowFileChooser(
                            WebView webView,
                            ValueCallback<Uri[]> callback,
                            FileChooserParams params
                    ) {

                        fileCallback =
                                callback;

                        try {

                            Intent intent =
                                    params.createIntent();

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

                            fileCallback = null;

                            return false;
                        }
                    }
                }
        );
    }

    /*
     * Website se Android notification
     * settings control karne ke liye bridge.
     */
    public class AndroidNotificationBridge {

        @JavascriptInterface
        public void setAppEnabled(
                boolean value
        ) {

            NotificationSettings
                    .setAppEnabled(
                            WebAppActivity.this,
                            value
                    );
        }

        @JavascriptInterface
        public void setGlobalEnabled(
                boolean value
        ) {

            NotificationSettings
                    .setGlobalEnabled(
                            WebAppActivity.this,
                            value
                    );
        }

        @JavascriptInterface
        public void setAnnouncementEnabled(
                boolean value
        ) {

            NotificationSettings
                    .setAnnouncementEnabled(
                            WebAppActivity.this,
                            value
                    );
        }

        @JavascriptInterface
        public void setWebAppEnabled(
                boolean value
        ) {

            NotificationSettings
                    .setWebAppEnabled(
                            WebAppActivity.this,
                            value
                    );
        }

        @JavascriptInterface
        public void setMessageMode(
                String mode
        ) {

            if (!"all".equals(mode)
                    &&
                    !"mentions".equals(mode)
                    &&
                    !"off".equals(mode)) {

                return;
            }

            NotificationSettings
                    .setMessageMode(
                            WebAppActivity.this,
                            mode
                    );
        }

        @JavascriptInterface
        public void setClassEnabled(
                String className,
                boolean value
        ) {

            NotificationSettings
                    .setClassEnabled(
                            WebAppActivity.this,
                            className,
                            value
                    );
        }

        @JavascriptInterface
        public boolean isAppEnabled() {

            return NotificationSettings
                    .isAppEnabled(
                            WebAppActivity.this
                    );
        }

        @JavascriptInterface
        public boolean isGlobalEnabled() {

            return NotificationSettings
                    .isGlobalEnabled(
                            WebAppActivity.this
                    );
        }

        @JavascriptInterface
        public boolean isAnnouncementEnabled() {

            return NotificationSettings
                    .isAnnouncementEnabled(
                            WebAppActivity.this
                    );
        }

        @JavascriptInterface
        public boolean isWebAppEnabled() {

            return NotificationSettings
                    .isWebAppEnabled(
                            WebAppActivity.this
                    );
        }

        @JavascriptInterface
        public String getMessageMode() {

            return NotificationSettings
                    .getMessageMode(
                            WebAppActivity.this
                    );
        }

        @JavascriptInterface
        public boolean isClassEnabled(
                String className
        ) {

            return NotificationSettings
                    .isClassEnabled(
                            WebAppActivity.this,
                            className
                    );
        }
    }

    private void getFCMToken() {

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnSuccessListener(
                        token -> {

                            /*
                             * Token ko backend ke
                             * authenticated user ke
                             * account se link karo.
                             */
                        }
                );
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission
                                    .POST_NOTIFICATIONS
                    )
                    != PackageManager
                            .PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission
                                        .POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION
                );
            }
        }
    }

    private void hideNavigationBar() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {

            WindowInsetsController controller =
                    getWindow()
                            .getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type
                                .navigationBars()
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
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    );
        }
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

        super.onWindowFocusChanged(
                hasFocus
        );

        if (hasFocus) {
            hideNavigationBar();
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

        if (fileCallback == null) {
            return;
        }

        Uri[] results = null;

        if (
                resultCode == RESULT_OK
                &&
                data != null
        ) {

            if (
                    data.getClipData()
                    != null
            ) {

                int count =
                        data.getClipData()
                                .getItemCount();

                results =
                        new Uri[count];

                for (
                        int i = 0;
                        i < count;
                        i++
                ) {

                    results[i] =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                }

            } else if (
                    data.getData() != null
            ) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }
        }

        fileCallback.onReceiveValue(
                results
        );

        fileCallback = null;
    }

    @Override
    public void onBackPressed() {

        if (
                webView != null
                &&
                webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}
