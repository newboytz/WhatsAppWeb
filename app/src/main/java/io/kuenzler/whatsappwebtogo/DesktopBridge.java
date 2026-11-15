package io.kuenzler.whatsappwebtogo;

import android.content.Context;
import android.webkit.WebView;

public class DesktopBridge {

    public static final String JsInstance = "DesktopEngineBridge";
    private Context mContext;

    public DesktopBridge(Context context) {
        mContext = context;
    }

    // Njia ya kulazimisha kivinjari kijitambue kama Edge kwenye Windows kupitia JS
    public static void injectDesktopEnvironment(WebView webView) {
        String spoofScript = "javascript:(" +
                "function() {" +
                "   try {" +
                "       Object.defineProperty(navigator, 'platform', { get: function() { return 'Win32'; } });" +
                "       Object.defineProperty(navigator, 'vendor', { get: function() { return 'Google Inc.'; } });" +
                "       Object.defineProperty(navigator, 'userAgentData', { get: function() { " +
                "           return { brands: [{brand: 'Not/A)Brand', version: '8'}, {brand: 'Microsoft Edge', version: '125'}, {brand: 'Chromium', version: '125'}], mobile: false, platform: 'Windows' }; " +
                "       } });" +
                "   } catch(e) {}" +
                "})();";
        webView.loadUrl(spoofScript);
    }
}
