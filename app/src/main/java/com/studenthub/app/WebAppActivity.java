package com.studenthub.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class WebAppActivity extends Activity {

    private WebView w;
    private ValueCallback<Uri[]> cb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        w = new WebView(this);
        setContentView(w);

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
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
                return loader.shouldInterceptRequest(request.getUrl());
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
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                    startActivityForResult(intent, 42);

                    return true;
                } catch (Exception e) {
                    cb = null;
                    return false;
                }
            }
        });

        String page = getIntent().getStringExtra("page");

        if (page != null && !page.isEmpty()) {
            w.loadUrl(page);
        } else {
            w.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 42 || cb == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == RESULT_OK && data != null) {

            if (data.getClipData() != null) {

                int count = data.getClipData().getItemCount();
                results = new Uri[count];

                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData()
                            .getItemAt(i)
                            .getUri();
                }

            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }

        cb.onReceiveValue(results);
        cb = null;
    }

    @Override
    protected void onDestroy() {
        if (w != null) {
            w.destroy();
            w = null;
        }

        super.onDestroy();
    }
}
