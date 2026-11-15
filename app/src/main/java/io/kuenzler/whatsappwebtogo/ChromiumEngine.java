package io.kuenzler.whatsappwebtogo;

import android.content.Context;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.util.Locale;

public class ChromiumEngine {

    // Microsoft Edge kwenye Windows (Chromium-based) User-Agent kamili ya kisasa
    private static final String EDGE_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.2535.92";

    public static String getDesktopUserAgent() {
        return EDGE_DESKTOP_UA;
    }

    public static void applyEdgeDesktopEngine(WebView webView, Context context) {
        WebSettings settings = webView.getSettings();

        // 1. Zima kabisa mfumo wa kuonekana kama Mobile/Touch device
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // 2. Lazimisha Desktop View na Layout Algorithm ya PC
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 3. Weka User-Agent ya Microsoft Edge ya Windows
        settings.setUserAgentString(EDGE_DESKTOP_UA);

        // 4. Zuia Networking/Cache kuleta bug za Mobile Web
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setBlockNetworkLoads(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 5. Zuia Content Access ya simu isitambuliwe na mitandao ya nje
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
    }
}
