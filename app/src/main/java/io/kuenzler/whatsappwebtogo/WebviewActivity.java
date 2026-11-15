package io.kuenzler.whatsappwebtogo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class WebviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Microsoft Edge kwenye Windows 10/11 x64 (Master Desktop User-Agent)
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.2535.92";

    private static final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO;
    private static final String[] VIDEO_PERMISSION = {CAMERA_PERMISSION, AUDIO_PERMISSION};

    private static final String WHATSAPP_HOMEPAGE_URL = "https://www.whatsapp.com/";
    private static final String WHATSAPP_WEB_BASE_URL = "web.whatsapp.com";
    private static final String WORLD_ICON = "\uD83C\uDF10";
    private static final String WHATSAPP_WEB_URL = "https://" + WHATSAPP_WEB_BASE_URL
            + "/" + WORLD_ICON + "/"
            + Locale.getDefault().getLanguage();

    private static final int FILECHOOSER_RESULTCODE        = 200;
    private static final int CAMERA_PERMISSION_RESULTCODE  = 201;
    private static final int AUDIO_PERMISSION_RESULTCODE   = 202;
    private static final int VIDEO_PERMISSION_RESULTCODE   = 203;
    private static final int STORAGE_PERMISSION_RESULTCODE = 204;

    public static final String DEBUG_TAG = "WAWEBTOGO";

    private final Activity activity = this;
    private SharedPreferences mSharedPrefs;
    private WebView mWebView;
    private ViewGroup mMainView;
    private long mLastBackClick = 0;

    boolean mKeyboardEnabled = false;
    boolean mDarkMode;

    private ValueCallback<Uri[]> mUploadMessage;
    private PermissionRequest mCurrentPermissionRequest;
    private String mCurrentDownloadRequest = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mSharedPrefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
        mDarkMode = mSharedPrefs.getBoolean("darkMode", false);
        mMainView = findViewById(R.id.layout);

        mWebView = findViewById(R.id.webview);

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                mCurrentDownloadRequest = url;
                if(checkPermission(STORAGE_PERMISSION)) {
                    mWebView.loadUrl(BlobDownloader.getBase64StringFromBlobUrl(url));
                    triggerDownload();
                } else {
                    requestPermission(STORAGE_PERMISSION);
                }
            }
        });
        mWebView.addJavascriptInterface(new BlobDownloader(getApplicationContext()), BlobDownloader.JsInstance);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setAppCacheEnabled(false);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);

        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);

        mWebView.getSettings().setSaveFormData(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.getSettings().setBlockNetworkLoads(false);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setNeedInitialFocus(false);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                Toast.makeText(getApplicationContext(), "OnCreateWindow", Toast.LENGTH_LONG).show();
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (request.getResources()[0].equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_DENIED
                            && ContextCompat.checkSelfPermission(activity, AUDIO_PERMISSION) == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(activity, VIDEO_PERMISSION, VIDEO_PERMISSION_RESULTCODE);
                        mCurrentPermissionRequest = request;
                    } else if (ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(activity, new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_RESULTCODE);
                        mCurrentPermissionRequest = request;
                    } else if (ContextCompat.checkSelfPermission(activity, AUDIO_PERMISSION) == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(activity, new String[]{AUDIO_PERMISSION}, AUDIO_PERMISSION_RESULTCODE);
                        mCurrentPermissionRequest = request;
                    } else {
                        request.grant(request.getResources());
                    }
                } else if (request.getResources()[0].equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(activity, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        ActivityCompat.requestPermissions(activity, new String[]{AUDIO_PERMISSION}, AUDIO_PERMISSION_RESULTCODE);
                        mCurrentPermissionRequest = request;
                    }
                } else {
                    try {
                        request.grant(request.getResources());
                    } catch (RuntimeException e) {
                        Log.d(DEBUG_TAG, "Granting permissions failed", e);
                    }
                }
            }

            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(DEBUG_TAG, "WebView console message: " + cm.message());
                return super.onConsoleMessage(cm);
            }

            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                mUploadMessage = filePathCallback;
                Intent chooserIntent = fileChooserParams.createIntent();
                WebviewActivity.this.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {

            // =========================================================================
            // MASTER HARDWARE-LEVEL ANTI-FINGERPRINTING & WINDOWS SPOOFING ENGINE (35 in 1)
            // =========================================================================
                        private void applyMasterHardwareSpoof(WebView view) {
                view.loadUrl("javascript:(function() {" +
                        "   try {" +
                        "       // 1-10: Navigator & Platform Core Signatures" +
                        "       Object.defineProperty(navigator, 'platform', { get: function() { return 'Win32'; } });" +
                        "       Object.defineProperty(navigator, 'vendor', { get: function() { return 'Google Inc.'; } });" +
                        "       Object.defineProperty(navigator, 'maxTouchPoints', { get: function() { return 0; } });" +
                        "       Object.defineProperty(navigator, 'hardwareConcurrency', { get: function() { return 16; } });" +
                        "       Object.defineProperty(navigator, 'deviceMemory', { get: function() { return 16; } });" +
                        "       Object.defineProperty(navigator, 'languages', { get: function() { return ['en-US', 'en']; } });" +
                        "       Object.defineProperty(navigator, 'language', { get: function() { return 'en-US'; } });" +
                        "       Object.defineProperty(navigator, 'pdfViewerEnabled', { get: function() { return true; } });" +
                        "       Object.defineProperty(navigator, 'cookieEnabled', { get: function() { return true; } });" +
                        "       Object.defineProperty(navigator, 'doNotTrack', { get: function() { return null; } });" +
                        "" +
                        "       // 11-20: UserAgentData & Client Hints (Windows 11 Edge Signature)" +
                        "       if (navigator.userAgentData) {" +
                        "           Object.defineProperty(navigator, 'userAgentData', { get: function() { " +
                        "               return {" +
                        "                   brands: [{brand: 'Not/A)Brand', version: '8'}, {brand: 'Microsoft Edge', version: '125'}, {brand: 'Chromium', version: '125'}]," +
                        "                   mobile: false," +
                        "                   platform: 'Windows'," +
                        "                   getHighEntropyValues: function(hints) { return Promise.resolve({platform: 'Windows', platformVersion: '15.0.0', architecture: 'x86', model: '', uaFullVersion: '125.0.2535.92'}); }" +
                        "               }; " +
                        "           } });" +
                        "       }" +
                        "" +
                        "       // 21-35: Screen, Display & Window Geometry Matrix" +
                        "       Object.defineProperty(screen, 'width', { get: function() { return 1920; } });" +
                        "       Object.defineProperty(screen, 'height', { get: function() { return 1080; } });" +
                        "       Object.defineProperty(screen, 'availWidth', { get: function() { return 1920; } });" +
                        "       Object.defineProperty(screen, 'availHeight', { get: function() { return 1040; } });" +
                        "       Object.defineProperty(screen, 'colorDepth', { get: function() { return 24; } });" +
                        "       Object.defineProperty(screen, 'pixelDepth', { get: function() { return 24; } });" +
                        "       Object.defineProperty(window, 'innerWidth', { get: function() { return 1920; } });" +
                        "       Object.defineProperty(window, 'innerHeight', { get: function() { return 955; } });" +
                        "       Object.defineProperty(window, 'outerWidth', { get: function() { return 1920; } });" +
                        "       Object.defineProperty(window, 'outerHeight', { get: function() { return 1040; } });" +
                        "       Object.defineProperty(window, 'screenX', { get: function() { return 0; } });" +
                        "       Object.defineProperty(window, 'screenY', { get: function() { return 0; } });" +
                        "       Object.defineProperty(window, 'devicePixelRatio', { get: function() { return 1; } });" +
                        "" +
                        "       // 36-55: Hardware GPU & WebGL / WebGL2 Spoofing (Intel UHD & ANGLE)" +
                        "       const hookWebGL = function(contextType) {" +
                        "           const proto = window[contextType] && window[contextType].prototype;" +
                        "           if (!proto) return;" +
                        "           const getParamOrig = proto.getParameter;" +
                        "           proto.getParameter = function(parameter) {" +
                        "               if (parameter === 37445) { return 'Intel Inc.'; }" +
                        "               if (parameter === 37446) { return 'ANGLE (Intel, Intel(R) UHD Graphics Direct3D11 vs_5_0 ps_5_0)'; }" +
                        "               if (parameter === 37924) { return 'Google Inc. (Intel)'; }" +
                        "               if (parameter === 37925) { return 'ANGLE (Intel, Intel(R) UHD Graphics Direct3D11 vs_5_0 ps_5_0, D3D11)'; }" +
                        "               return getParamOrig.apply(this, arguments);" +
                        "           };" +
                        "           const getExtOrig = proto.getExtension;" +
                        "           proto.getExtension = function(name) {" +
                        "               if (name === 'WEBGL_debug_renderer_info') {" +
                        "                   return { UNMASKED_VENDOR_WEBGL: 37445, UNMASKED_RENDERER_WEBGL: 37446 };" +
                        "               }" +
                        "               return getExtOrig.apply(this, arguments);" +
                        "           };" +
                        "       };" +
                        "       hookWebGL('WebGLRenderingContext');" +
                        "       hookWebGL('WebGL2RenderingContext');" +
                        "" +
                        "       // 56-70: Complete Eradication of Touch / Mobile Events & Pointers" +
                        "       delete window.ontouchstart;" +
                        "       delete window.ontouchmove;" +
                        "       delete window.ontouchend;" +
                        "       delete window.ontouchcancel;" +
                        "       if (window.TouchEvent) { window.TouchEvent = undefined; }" +
                        "       if (window.Touch) { window.Touch = undefined; }" +
                        "       if (window.TouchList) { window.TouchList = undefined; }" +
                        "" +
                        "       const origMatchMedia = window.matchMedia;" +
                        "       window.matchMedia = function(query) {" +
                        "           if (query.includes('pointer: fine') || query.includes('hover: hover')) {" +
                        "               return { matches: true, media: query, onchange: null, addListener: function(){}, removeListener: function(){}, addEventListener: function(){}, removeEventListener: function(){}, dispatchEvent: function() { return false; } };" +
                        "           }" +
                        "           if (query.includes('pointer: coarse') || query.includes('hover: none')) {" +
                        "               return { matches: false, media: query, onchange: null, addListener: function(){}, removeListener: function(){}, addEventListener: function(){}, removeEventListener: function(){}, dispatchEvent: function() { return false; } };" +
                        "           }" +
                        "           return origMatchMedia.call(window, query);" +
                        "       };" +
                        "" +
                        "       // 71-85: Desktop Plugins, MimeTypes & PDF Engine Signatures" +
                        "       Object.defineProperty(navigator, 'plugins', {" +
                        "           get: function() {" +
                        "               return {" +
                        "                   length: 3," +
                        "                   0: { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format', length: 1 }," +
                        "                   1: { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '', length: 1 }," +
                        "                   2: { name: 'Native Client', filename: 'internal-nacl-plugin', description: '', length: 2 }," +
                        "                   item: function(index) { return this[index]; }," +
                        "                   namedItem: function(name) { return this[0]; }" +
                        "               };" +
                        "           }" +
                        "       });" +
                        "" +
                        "       Object.defineProperty(navigator, 'mimeTypes', {" +
                        "           get: function() {" +
                        "               return {" +
                        "                   length: 2," +
                        "                   0: { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format' }," +
                        "                   1: { type: 'text/pdf', suffixes: 'pdf', description: 'Portable Document Format' }," +
                        "                   item: function(index) { return this[index]; }," +
                        "                   namedItem: function(name) { return this[0]; }" +
                        "               };" +
                        "           }" +
                        "       });" +
                        "" +
                        "       // 86-100: Advanced Anti-Fraud, AudioContext & Battery API Masking" +
                        "       if (window.AudioContext || window.webkitAudioContext) {" +
                        "           const AudioCtx = window.AudioContext || window.webkitAudioContext;" +
                        "           const origCreateBuffer = AudioCtx.prototype.createBuffer;" +
                        "           AudioCtx.prototype.createBuffer = function(channels, length, sampleRate) {" +
                        "               return origCreateBuffer.call(this, channels, length, 44100);" +
                        "           };" +
                        "       }" +
                        "" +
                        "       if (navigator.getBattery) {" +
                        "           navigator.getBattery = function() {" +
                        "               return Promise.resolve({ charging: true, chargingTime: 0, dischargingTime: Infinity, level: 1, onchargingchange: null, onlevelchange: null, addEventListener: function(){}, removeEventListener: function(){} });" +
                        "           };" +
                        "       }" +
                        "" +
                        "       // Hide Selenium/Automation flags" +
                        "       delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;" +
                        "       delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;" +
                        "       delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;" +
                        "       if (navigator.webdriver) { Object.defineProperty(navigator, 'webdriver', { get: function() { return false; } }); }" +
                        "" +
                        "   } catch(e) {}" +
                        "})();");
            }


            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                applyMasterHardwareSpoof(view);
                setContentSize(view);
                if (mDarkMode) {
                    addDarkMode(view);
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                applyMasterHardwareSpoof(view);
                setContentSize(view);
                if (mDarkMode) {
                    addDarkMode(view);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.scrollTo(0, 0);
                applyMasterHardwareSpoof(view);
                setContentSize(view);
                if (mDarkMode) {
                    addDarkMode(view);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                Log.d(DEBUG_TAG, url.toString());

                if (url.toString().equals(WHATSAPP_HOMEPAGE_URL)){
                    showToast(R.string.reload_required);
                    loadWhatsapp();
                    return true;
                } else if (url.getHost().equals(WHATSAPP_WEB_BASE_URL)) {
                    return super.shouldOverrideUrlLoading(view, request);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(intent);
                    return true;
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String msg = String.format("Error: %s - %s", error.getErrorCode(), error.getDescription());
                Log.d(DEBUG_TAG, msg);
            }

            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                Log.d(DEBUG_TAG, "Unhandled key event: " + event.toString());
            }
        });

        if (savedInstanceState == null) {
            loadWhatsapp();
        } else {
            Log.d(DEBUG_TAG, "savedInstanceState is present");
        }

        mWebView.getSettings().setUserAgentString(USER_AGENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();

        mKeyboardEnabled = mSharedPrefs.getBoolean("keyboardEnabled", true);
        setAppbarEnabled(mSharedPrefs.getBoolean("appbarEnabled", true));
        setKeyboardEnabled(mKeyboardEnabled);

        showIntroInfo();
        showVersionInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.toggle_keyboard) {
            toggleKeyboard();
        } else if (id == R.id.scroll_left) {
            showToast(R.string.appbar_button_scroll_left);
            runOnUiThread(() -> mWebView.scrollTo(0, 0));
        } else if (id == R.id.scroll_right) {
            showToast(R.string.appbar_title_scroll_right);
            runOnUiThread(() -> mWebView.scrollTo(2000, 0));
        }
        return true;
    }

    private boolean checkPermission(String permission) {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, permission);
    }

    private void requestPermission(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, STORAGE_PERMISSION_RESULTCODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case VIDEO_PERMISSION_RESULTCODE:
                if (permissions.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        mCurrentPermissionRequest.grant(mCurrentPermissionRequest.getResources());
                    } catch (RuntimeException e) {
                        Log.e(DEBUG_TAG, "Granting permissions failed", e);
                    }
                } else {
                    showSnackbar(R.string.permission_denied_video);
                    mCurrentPermissionRequest.deny();
                }
                break;
            case CAMERA_PERMISSION_RESULTCODE:
            case AUDIO_PERMISSION_RESULTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        mCurrentPermissionRequest.grant(mCurrentPermissionRequest.getResources());
                    } catch (RuntimeException e) {
                        Log.e(DEBUG_TAG, "Granting permissions failed", e);
                    }
                } else {
                    showSnackbar(R.string.permission_denied_cam +
                            (requestCode == CAMERA_PERMISSION_RESULTCODE ? R.string.camera : R.string.microphone));
                    mCurrentPermissionRequest.deny();
                }
                break;
            case STORAGE_PERMISSION_RESULTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    triggerDownload();
                } else {
                    showToast(R.string.permission_denied_download);
                    mCurrentDownloadRequest = null;
                }
                break;
            default:
                Log.d(DEBUG_TAG, "Got permission result with unknown request code " +
                        requestCode + " - " + Arrays.asList(permissions).toString());
        }
        mCurrentPermissionRequest = null;
    }

    private void triggerDownload(){
        if(null != mCurrentDownloadRequest) {
            mWebView.loadUrl(BlobDownloader.getBase64StringFromBlobUrl(mCurrentDownloadRequest));
        }
        mCurrentDownloadRequest = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILECHOOSER_RESULTCODE:
                if (resultCode == RESULT_CANCELED || data.getData() == null) {
                    mUploadMessage.onReceiveValue(null);
                } else {
                    Uri result = data.getData();
                    Uri[] results = new Uri[1];
                    results[0] = result;
                    mUploadMessage.onReceiveValue(results);
                }
                break;
            default:
                Log.d(DEBUG_TAG, "Got activity result with unknown request code " +
                        requestCode + " - " + data.toString());
        }
    }

    private void showPopupDialog(String message) {
        final SpannableString msg = new SpannableString(message);
        Linkify.addLinks(msg, Linkify.WEB_URLS|Linkify.EMAIL_ADDRESSES);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, null);
        AlertDialog alert = builder.create();
        alert.show();
        ((TextView) alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showPopupDialog(int resId) {
       showPopupDialog(getString(resId));
    }

    private void showToast(int resId) {
        this.runOnUiThread(() -> Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show());
    }

    private void showSnackbar(int resId) {
        this.runOnUiThread(() -> {
            final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getString(resId), 900);
            snackbar.setAction(getString(R.string.dismiss), (View view) -> snackbar.dismiss());
            snackbar.setActionTextColor(Color.parseColor("#075E54"));
            snackbar.show();
        });
    }

    private void toggleKeyboard() {
        setKeyboardEnabled(!mKeyboardEnabled);
    }

    private void setKeyboardEnabled(final boolean enable) {
        mKeyboardEnabled = enable;
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (enable && mMainView.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
            mMainView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            showSnackbar(R.string.unblock_keyboard);
        } else if (!enable) {
            mMainView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mWebView.getRootView().requestFocus();
            showSnackbar(R.string.block_keyboard);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
        mSharedPrefs.edit().putBoolean("keyboardEnabled", enable).apply();
    }

    private void setAppbarEnabled(boolean enable) {
        ActionBar actionBar= getSupportActionBar();
        if (actionBar != null) {
            if (enable) {
                actionBar.show();
            } else {
                actionBar.hide();
            }
            mSharedPrefs.edit().putBoolean("appbarEnabled", enable).apply();
        }
    }

    private void toggleDarkMode() {
        boolean currentState = mSharedPrefs.getBoolean("darkMode", false);
        mSharedPrefs.edit().putBoolean("darkMode", !currentState).apply();
        Log.d(DEBUG_TAG, "Dark Mode Enabled: " + !currentState);
        recreate();
    }

    private void showVersionInfo() {
        int lastShownVersionCode = 0;
        int currentVersionCode = 0;
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(DEBUG_TAG, "Error checking versioncode", e);
            return;
        }
        lastShownVersionCode = mSharedPrefs.getInt("lastShownVersionCode", 0);
        if (lastShownVersionCode == 0) {
            mSharedPrefs.edit().putInt("lastShownVersionCode", currentVersionCode).apply();
            return;
        }
        if (lastShownVersionCode < currentVersionCode) {
            showPopupDialog(R.string.versionInfoText);
        } else {
            return;
        }
        mSharedPrefs.edit().putInt("lastShownVersionCode", currentVersionCode).apply();
    }

    private void showIntroInfo() {
        if (!mSharedPrefs.getBoolean("introShown", false)) {
            showPopupDialog(R.string.introInfoText);
        } else {
            return;
        }
        mSharedPrefs.edit().putBoolean("introShown", true).apply();
    }

    private void showAbout() {
        showPopupDialog(R.string.aboutText);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (System.currentTimeMillis() - mLastBackClick < 1100) {
            finishAffinity();
        } else {
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
            showToast(R.string.navigation_drawer_close_confirm);
            mLastBackClick = System.currentTimeMillis();
        }
    }

    private void loadWhatsapp() {
        mWebView.getSettings().setUserAgentString(USER_AGENT);
        mWebView.loadUrl(WHATSAPP_WEB_URL);
    }

    public void addDarkMode(final WebView mWebView) {
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setStatusBarColor(Color.BLACK);

        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(mWebView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(mWebView.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
            }
        } else {
            mWebView.loadUrl("javascript:(" +
                    "function(){ " +
                    "try {  document.body.classList.add('dark') } catch(err) { }" +
                    "})()");
        }
    }

    public void setContentSize(final WebView mWebView){
        if (getResources().getBoolean(R.bool.isTablet)) {
            return;
        }

        mWebView.loadUrl("javascript:(function(){" +
                "  try { " +
                "	var css = '.two > div:nth-child(4){flex: 1 0 100vmin;}.two{overflow:visible}'," +
                "    	head = document.head || document.getElementsByTagName('head')[0]," +
                "    	style = document.createElement('style');" +
                "	head.appendChild(style);" +
                "	style.type = 'text/css';" +
                "	if (style.styleSheet){" +
                "  		style.styleSheet.cssText = css;" +
                "	} else {" +
                "  		style.appendChild(document.createTextNode(css));" +
                "	}" +
                "} catch(err) { }" +
                "})()");
    }

    public void logout(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes_button, (dialog, which) -> {
                    showSnackbar(R.string.logging_out);
                    mWebView.loadUrl("javascript:localStorage.clear()");
                    WebStorage.getInstance().deleteAllData();
                    loadWhatsapp();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no_button, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.appbar_hide) {
            if (getSupportActionBar().isShowing()) {
                showSnackbar(R.string.appbar_hiding);
                setAppbarEnabled(false);
            } else {
                setAppbarEnabled(true);
            }
        } else if (id == R.id.nav_logout) {
           logout();
        } else if (id == R.id.nav_new) {
            //showToast("nav_new");
        } else if (id == R.id.nav_switch) {
            //showToast("nav_switch");
        } else if (id == R.id.nav_settings) {
            //showToast("nav_settings");
        } else if (id == R.id.nav_about) {
            showAbout();
        } else if (id == R.id.nav_reload) {
            showSnackbar(R.string.reloading);
            loadWhatsapp();
        } else if (id == R.id.nav_dark_mode) {
            toggleDarkMode();
        } else if (id == R.id.nav_keyboard) {
            toggleKeyboard();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
