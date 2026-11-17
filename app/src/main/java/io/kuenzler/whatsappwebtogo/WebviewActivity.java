package io.kuenzler.whatsappwebtogo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.util.Locale;
import java.util.Objects;

public class WebviewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String WHATSAPP_WEB_BASE_URL = "web.whatsapp.com";
    private static final String WORLD_ICON = "\uD83C\uDF10";
    private static final String WHATSAPP_WEB_URL = "https://" + WHATSAPP_WEB_BASE_URL
            + "/" + WORLD_ICON + "/"
            + Locale.getDefault().getLanguage();

    public static final String DEBUG_TAG = "WAWEBTOGO";

    private final Activity activity = this;
    private SharedPreferences mSharedPrefs;

    private GeckoView mGeckoView;
    private static GeckoRuntime sRuntime;
    private GeckoSession mSession;
    private ViewGroup mMainView;

    private long mLastBackClick = 0;
    boolean mKeyboardEnabled = false;
    boolean mDarkMode;

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

        // GeckoView Setup (Badala ya WebView ya zamani)
        mGeckoView = findViewById(R.id.webview); // Inatumia ID ile ile ya kwenye layout XML yako

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
            // Kusoma extension yetu ya spoofer kutoka kwenye assets/spoofer/[span_2](start_span)[span_2](end_span)[span_3](start_span)[span_3](end_span)
            sRuntime.getWebExtensionController()
                    .ensureBuiltIn("resource://android/assets/spoofer/", "spoofer@whatsapp.local")
                    .accept(
                        extension -> Log.i(DEBUG_TAG, "Spoofer Extension imewashwa kikamilifu!"),
                        e -> Log.e(DEBUG_TAG, "Hitilafu kwenye Spoofer Extension", e)
                    );
        }

        mSession = new GeckoSession();

        // Kulazimisha GeckoView itumie Desktop Mode (Firefox Windows) kuzuia mabango ya simu[span_4](start_span)[span_4](end_span)[span_5](start_span)[span_5](end_span)
        mSession.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
        mSession.getSettings().setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP);
        
        mSession.open(sRuntime);
        mGeckoView.setSession(mSession);

        if (savedInstanceState == null) {
            loadWhatsapp();
        }

        applyThemeSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mKeyboardEnabled = mSharedPrefs.getBoolean("keyboardEnabled", true);
        setAppbarEnabled(mSharedPrefs.getBoolean("appbarEnabled", true));
        setKeyboardEnabled(mKeyboardEnabled);

        showIntroInfo();
        showVersionInfo();
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
        } else if (id == R.id.scroll_right) {
            showToast(R.string.appbar_title_scroll_right);
        }
        return true;
    }

    private void loadWhatsapp() {
        if (mSession != null) {
            mSession.loadUri(WHATSAPP_WEB_URL);
        }
    }

    private void applyThemeSettings() {
        if (mDarkMode) {
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(Color.BLACK);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            }
        }
    }

    public void logout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes_button, (dialog, which) -> {
                    showSnackbar(R.string.logging_out);
                    if (mSession != null) {
                        // Kufuta session na kuanza upya
                        mSession.loadUri("about:blank");
                        loadWhatsapp();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no_button, (dialog, which) -> dialog.dismiss())
                .show();
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
            showSnackbar(R.string.block_keyboard);
            if (getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
        mSharedPrefs.edit().putBoolean("keyboardEnabled", enable).apply();
    }

    private void setAppbarEnabled(boolean enable) {
        ActionBar actionBar = getSupportActionBar();
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
        recreate();
    }

    private void showPopupDialog(String message) {
        final SpannableString msg = new SpannableString(message);
        Linkify.addLinks(msg, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

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

    private void showVersionInfo() {
        int currentVersionCode = 0;
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        int lastShownVersionCode = mSharedPrefs.getInt("lastShownVersionCode", 0);
        if (lastShownVersionCode == 0 || lastShownVersionCode < currentVersionCode) {
            mSharedPrefs.edit().putInt("lastShownVersionCode", currentVersionCode).apply();
            if (lastShownVersionCode != 0) {
                showPopupDialog(R.string.versionInfoText);
            }
        }
    }

    private void showIntroInfo() {
        if (!mSharedPrefs.getBoolean("introShown", false)) {
            mSharedPrefs.edit().putBoolean("introShown", true).apply();
            showPopupDialog(R.string.introInfoText);
        }
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
            showToast(R.string.navigation_drawer_close_confirm);
            mLastBackClick = System.currentTimeMillis();
        }
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
