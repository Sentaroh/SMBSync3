package com.sentaroh.android.SMBSync3;

/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceManager;

import com.sentaroh.android.Utilities3.Dialog.MessageDialogAppFragment;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.GlobalParameters.APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT;
import static com.sentaroh.android.SMBSync3.GlobalParameters.SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT;
import static com.sentaroh.android.SMBSync3.GlobalParameters.SMB_LM_COMPATIBILITY_DEFAULT;

public class ActivitySettings extends AppCompatActivity implements OnPreferenceStartFragmentCallback {
    static final private Logger log = LoggerFactory.getLogger(ActivitySettings.class);
    private static final String TITLE_TAG = "ActivitySettings";

    private static GlobalParameters mGp = null;
    private AppCompatActivity mActivity = null;
    private CommonUtilities mUtil = null;

    @Override
    protected void attachBaseContext(Context base) {
//        log.info("attachBaseContext entered");
        super.attachBaseContext(GlobalParameters.setLocaleAndMetrics(base));
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mActivity = ActivitySettings.this;
        mGp = GlobalWorkArea.getGlobalParameter(mActivity);
        mUtil = new CommonUtilities(mActivity, "ActivitySettings", mGp, null);
        //SharedPreferences shared_pref = CommonUtilities.getSharedPreference(ActivitySettings.this);

        // on Activity settings restart, apply new theme as it could have changed in settings
        setTheme(GlobalParameters.getScreenTemeValue(mActivity));
        //GlobalParameters.setDisplayFontScale(ActivitySettings.this);

        mGp.setDeviceOrientation(mActivity);

        super.onCreate(savedInstanceState);

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        setContentView(R.layout.settings_preference_main_layout);

        //Entering the settings menu from main activity
        // Create the fragment only when the activity is created for the first time, not after orientation changes or back from another setting subscreen
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_preference_frame_layout, new ActivitySettings.SettingsFragment())
                        .commit();
            mActivity.setTitle(R.string.settings_main_title);
        } else {
            //Returning to settings menu on configuration change (screen rotation...)
            mActivity.setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
            public void onBackStackChanged() {
                FragmentManager mFrag = getSupportFragmentManager();
                if (mFrag.getBackStackEntryCount() == 0) {
                    mActivity.setTitle(R.string.settings_main_title);
                }
            }
        });

        // Setup listeners to get results from a fragment
        // Deprecates fragment.setTargetFragment(caller, 0); in onPreferenceStartFragment(...)
/*
        // Test sample code
        getSupportFragmentManager().setFragmentResultListener("SettingsSyncFragmentRequestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                // We use a String here, but any type that can be put in a Bundle is supported
                boolean result = bundle.getBoolean("restart_activity");
                String result_msg = bundle.getString("restart_message");
                // Do something with the result
                mUtil.addDebugMsg(1, "I", "restart_activity="+result + " restart_message="+result_msg + " requestKey="+requestKey);
            }
        });
*/

        // Setup listener to reload SMB settings fragment: for some reason, PreferenceList is not refreshed after being modified by "Restore default values" button !!
        getSupportFragmentManager().setFragmentResultListener("SettingsSmbFragmentRequestFragmentReload", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                boolean reload = bundle.getBoolean("restart_smb_fragment");
                mUtil.addDebugMsg(1, "I", "SettingsSmbFragmentRequestFragmentReload: " + "restart_smb_fragment=" + reload + " requestKey=" + requestKey);

                // First ensure we have the proper fragment to restart and that it is active
                // Then, if result is reload==true:
                // - restore previous fragment (root preferences menu) from back stack (simulating Back button)
                // - replace current fragment (now root fragment) by a new instance of SettingsSmbFragment
                // - add previous fragment (root fragment) to the back stack
                // - properly set the title of new fragment (SettingsSmbFragment)
                final Fragment fragmentInFrame = getSupportFragmentManager().findFragmentById(R.id.settings_preference_frame_layout);
                if (fragmentInFrame instanceof SettingsSmbFragment) {
                    mUtil.addDebugMsg(3, "I", "setFragmentResultListener: found active fragment SettingsSmbFragment");
                    if (reload) {
                        if (getSupportFragmentManager().popBackStackImmediate()) {
                            getSupportFragmentManager().beginTransaction()
                            .replace(R.id.settings_preference_frame_layout, new SettingsSmbFragment())
                            .addToBackStack(null)
                            .commit();

                            mActivity.setTitle(R.string.settings_smb_title);
                        }
                    }
                } else {
                    mUtil.addDebugMsg(1, "E", "setFragmentResultListener Exception: Active fragment was not SettingsSmbFragment");
                }
            }
        });

        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setTitle(R.string.title); //Will fix the window title for all Settings sub-menus

            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Show home button with icon in action bar
            //actionBar.setIcon(R.drawable.ic_32_smbsync);
            //actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_TAG, mActivity.getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        return getSupportFragmentManager().popBackStackImmediate() || super.onSupportNavigateUp();
    }

    @Override
    public void onStart() {
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    public void onResume() {
        super.onResume();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // Return to main app when pressing Top action bar back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        // Instantiate the new Fragment
        if (pref.getFragment() == null) return false;

        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_preference_frame_layout, fragment)
                .addToBackStack(null)
                .commit();
        mActivity.setTitle(pref.getTitle());
        return true;
    }

    private static void playBackDefaultNotification(Context c, FragmentManager fm, int vol) {
        float volume = (float) vol / 100.0f;
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri != null) {
            final MediaPlayer player = MediaPlayer.create(c, uri);
            if (player != null) {
                player.setVolume(volume, volume);
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        int dur = player.getDuration();
                        player.start();
                        SystemClock.sleep(dur + 10);
                        player.stop();
                        player.reset();
                        player.release();
                    }
                };
                th.setPriority(Thread.MAX_PRIORITY);
                th.start();
            } else {
                MessageDialogAppFragment cd = MessageDialogAppFragment.newInstance(false, "E",
                    c.getString(R.string.settings_playback_ringtone_volume_disabled), "");
                cd.showDialog(fm, cd, null);
            }
        }
    }
/*
    public static boolean isTablet(Context context, CommonUtilities cu) {
        float fs = GlobalParameters.getFontScaleFactorValue(context);
        int multiPaneDP = 500;
        // String lang_code=Locale.getDefault().getLanguage();
        // if (lang_code.equals("fr")) multiPaneDP=540;
        // else if (lang_code.equals("ru")) multiPaneDP=800;

        multiPaneDP=(int)(((float)multiPaneDP)*fs);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final float x_px = (float) Math.min(metrics.heightPixels, metrics.widthPixels);
        final float y_px = (float) Math.max(metrics.heightPixels, metrics.widthPixels);
        boolean portrait_mp = (x_px/metrics.density) >= multiPaneDP;
        boolean land_mp = (y_px/metrics.density) >= multiPaneDP;

        int orientation = context.getResources().getConfiguration().orientation;
        boolean sc_land_mp = (land_mp || mGp.settingForceDeviceTabletViewInLandscape) && orientation == Configuration.ORIENTATION_LANDSCAPE;
            //screen is in landscape orientation and either width size >= multiPaneDP or user forced MultiPanel view in landscape
        return portrait_mp || sc_land_mp;
            //use MultiPane display in portrait if width >= multiPaneDP or in landscape if largest screen side >= multiPaneDP
    }
*/
    public static final class SettingsFragment extends PreferenceFragmentCompat {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsFragment", mGp, null);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag, rootKey);
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }
    }

    public static final class SettingsSyncFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsSyncFragment", mGp, null);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag_sync, rootKey);

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Register listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            shared_pref.registerOnSharedPreferenceChangeListener(this);

            // Check all currrent preferences values and do any custom actions (enable/disable a setting...) on preferences screen creations
            setCurrentValues(shared_pref);
        }

        /*
         * Listener when the preferences change
         * @param sharedPreferences that changed
         * @param key that changed
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
            checkSettingValue(mUtil, shared_pref, key_string, mContext);
/*
            Preference preference = findPreference(key_string);
            if (preference != null) {
                if (!(preference instanceof CheckBoxPreference)){
                    String value = shared_pref.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
*/
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Unregister listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            shared_pref.unregisterOnSharedPreferenceChangeListener(this);
        }
/*
        // Test code to send results from current fragment to listening fragment
        private void setResultCallback(boolean restart) {
            Bundle result = new Bundle();
            result.putBoolean("restart_activity", restart);
            result.putString("restart_message", restart ? "restart requiered":"no restart requiered");
            //requireActivity().getSupportFragmentManager().setFragmentResult("BasicPreferencesFragmentRequestKey", result);
            //((AppCompatActivity)mActivity).getSupportFragmentManager().setFragmentResult("BasicPreferencesFragmentRequestKey", result);
            mActivity.getSupportFragmentManager().setFragmentResult("SettingsSyncFragmentRequestKey", result);
        }
*/
        private void setCurrentValues(SharedPreferences shared_pref) {
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_wifi_lock), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_sync_history_log), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_force_screen_on_while_sync), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_add_external_storage_notification), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_start_sync_confirmation_message), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_shortcut1_confirmation_message), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_no_compress_file_type), mContext);
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key = findPreference(key_string);
            if (key_string.equals(c.getString(R.string.settings_force_screen_on_while_sync))) {
            } else if (key_string.equals(c.getString(R.string.settings_wifi_lock))) {
            } else if (key_string.equals(c.getString(R.string.settings_sync_history_log))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_add_external_storage_notification))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_start_sync_confirmation_message))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_shortcut1_confirmation_message))) {
            } else if (key_string.equals(c.getString(R.string.settings_no_compress_file_type))) {
                if (shared_pref.getString(key_string, "").equals("")) {
                    shared_pref.edit()
                        .putString(key_string, DEFAULT_NOCOMPRESS_FILE_TYPE)
                        .commit();
                }
                Objects.requireNonNull(pref_key).setSummary(shared_pref.getString(key_string, DEFAULT_NOCOMPRESS_FILE_TYPE));
            }
        }
    }

    public static class SettingsUiFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;

        private int mInitVolume = 100;
        private String mCurrentLangaueSetting = null;
        private String mCurrentThemeSetting = null;
        private String mCurrentFontScaleSetting = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsUiFragment", mGp, null);

            // Get current language, theme and font scale settings from Saved preferences (use global public method since we have one)
            mCurrentLangaueSetting = GlobalParameters.getLanguageCode(mActivity);
            mCurrentThemeSetting = GlobalParameters.getScreenTemeSetting(mActivity);
            mCurrentFontScaleSetting = GlobalParameters.getFontScaleFactorSetting(mActivity);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag_ui, rootKey);

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Register listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            shared_pref.registerOnSharedPreferenceChangeListener(this);

            mInitVolume = shared_pref.getInt(getString(R.string.settings_playback_ringtone_volume), 100);

            // Check all currrent preferences values and do any custom actions (enable/disable a setting...) on preferences screen creations
            setCurrentValues(shared_pref);
        }

        /*
         * Listener when the preferences change
         * @param sharedPreferences that changed
         * @param key that changed
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
            checkSettingValue(mUtil, shared_pref, key_string, mContext);
/*
            Preference preference = findPreference(key_string);
            if (preference != null) {
                if (!(preference instanceof CheckBoxPreference)){
                    String value = shared_pref.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
*/
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Unregister listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            shared_pref.unregisterOnSharedPreferenceChangeListener(this);
        }

        private void setCurrentValues(SharedPreferences shared_pref) {
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_notification_message_when_sync_ended), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_playback_ringtone_when_sync_ended), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_playback_ringtone_volume), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_vibrate_when_sync_ended), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_screen_theme), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_device_orientation_portrait), mContext);

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_screen_theme_language), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_display_font_scale_factor), mContext);
            //checkSettingValue(mUtil, shared_pref, getString(R.string.settings_device_orientation_landscape_tablet), mContext);
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key = findPreference(key_string);
            Objects.requireNonNull(pref_key);
            if (key_string.equals(c.getString(R.string.settings_playback_ringtone_when_sync_ended))) {
                Preference rv = findPreference(c.getString(R.string.settings_playback_ringtone_volume));
                String kv = shared_pref.getString(key_string, NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS);
                Objects.requireNonNull(rv);
                if (kv.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS)) {
                    pref_key.setSummary(c.getString(R.string.settings_playback_ringtone_when_sync_ended_summary_always));
                    rv.setEnabled(true);
                } else if (kv.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ERROR)) {
                    pref_key.setSummary(c.getString(R.string.settings_playback_ringtone_when_sync_ended_summary_error));
                    rv.setEnabled(true);
                } else if (kv.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_SUCCESS)) {
                    pref_key.setSummary(c.getString(R.string.settings_playback_ringtone_when_sync_ended_summary_success));
                    rv.setEnabled(true);
                } else if (kv.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_NO)) {
                    pref_key.setSummary(c.getString(R.string.settings_playback_ringtone_when_sync_ended_summary_no));
                    rv.setEnabled(false);
                }
            } else if (key_string.equals(c.getString(R.string.settings_vibrate_when_sync_ended))) {
                String kv = shared_pref.getString(key_string, NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS);
                if (kv.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS)) {
                    pref_key.setSummary(c.getString(R.string.settings_vibrate_when_sync_ended_summary_always));
                } else if (kv.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ERROR)) {
                    pref_key.setSummary(c.getString(R.string.settings_vibrate_when_sync_ended_summary_error));
                } else if (kv.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_SUCCESS)) {
                    pref_key.setSummary(c.getString(R.string.settings_vibrate_when_sync_ended_summary_success));
                } else if (kv.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_NO)) {
                    pref_key.setSummary(c.getString(R.string.settings_vibrate_when_sync_ended_summary_no));
                }
            } else if (key_string.equals(c.getString(R.string.settings_notification_message_when_sync_ended))) {
                String kv = shared_pref.getString(key_string, NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS);
                if (kv.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS)) {
                    pref_key.setSummary(c.getString(R.string.settings_notification_message_when_sync_ended_summary_always));
                } else if (kv.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ERROR)) {
                    pref_key.setSummary(c.getString(R.string.settings_notification_message_when_sync_ended_summary_error));
                } else if (kv.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_SUCCESS)) {
                    pref_key.setSummary(c.getString(R.string.settings_notification_message_when_sync_ended_summary_success));
                } else if (kv.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_NO)) {
                    pref_key.setSummary(c.getString(R.string.settings_notification_message_when_sync_ended_summary_no));
                }
            } else if (key_string.equals(c.getString(R.string.settings_playback_ringtone_volume))) {
                int vol = shared_pref.getInt(key_string, 100);
                pref_key.setSummary(
                        String.format(c.getString(R.string.settings_playback_ringtone_volume_summary), vol));
                if (mInitVolume != vol) playBackDefaultNotification(c, mActivity.getSupportFragmentManager(), vol);
            } else if (key_string.equals(c.getString(R.string.settings_screen_theme))) {
                String tid = shared_pref.getString(key_string, SCREEN_THEME_STANDARD);
                String[] wl_label = c.getResources().getStringArray(R.array.settings_screen_theme_list_entries);
                String sum_msg = wl_label[Integer.parseInt(tid)];
                pref_key.setSummary(sum_msg);
                if (!mCurrentThemeSetting.equals(tid)) {
                    // Theme language changed, restart ActivitySettings for the changes to take effect. They will be applied by onCreate() of ActivitySettings
                    // App restart will be triggered on exiting Settings
                    //mCurrentThemeSetting = tid; //not needed because we restart activity immeadiately
                    mActivity.finish();
                    Intent intent = new Intent(mActivity, ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    mActivity.startActivity(intent);
                }
            } else if (key_string.equals(c.getString(R.string.settings_screen_theme_language))) {
                String lang_value = shared_pref.getString(key_string, APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT);
                String[] lang_msgs = c.getResources().getStringArray(R.array.settings_screen_theme_language_list_entries);
                String sum_msg = lang_msgs[0];
                if (!lang_value.equals(APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT)) {
                    for (String lang_msg : lang_msgs) {
                        sum_msg = lang_msg;
                        if (sum_msg.contains("(" + lang_value + ")")) {
                            break;
                        }
                    }
                }
                pref_key.setSummary(sum_msg);
                if (!lang_value.equals(mCurrentLangaueSetting)) {
                    // Setting changed, restart ActivitySettings to apply new language. Full app restart will be triggered on return to MainActivity
                    // Language is applied to settings activity from attachBaseContext()
                    //mCurrentLangaueSetting = lang_value; //not needed because we restart activity immeadiately
                    mActivity.finish();
                    Intent intent = new Intent(mActivity, ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    mActivity.startActivity(intent);
                }
            } else if (key_string.equals(c.getString(R.string.settings_display_font_scale_factor))) {
                String font_scale_id = shared_pref.getString(key_string, GlobalParameters.FONT_SCALE_FACTOR_NORMAL);
                String[] font_scale_label = c.getResources().getStringArray(R.array.settings_display_font_scale_factor_list_entries);
                String sum_msg = font_scale_label[Integer.parseInt(font_scale_id)];
                pref_key.setSummary(sum_msg);
                if (!mCurrentFontScaleSetting.equals(font_scale_id)) {
                    // Setting changed, restart ActivitySettings to apply new language. Full app restart will be triggered on return to MainActivity
                    // Fon scale is applied to settings activity from attachBaseContext()
                    //mCurrentFontScaleSetting = font_scale_id; //not needed because we restart activity immeadiately
                    mActivity.finish();
                    Intent intent = new Intent(mActivity, ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    mActivity.startActivity(intent);
                }
            } //else if (key_string.equals(c.getString(R.string.settings_device_orientation_landscape_tablet))) { }
              else if (key_string.equals(c.getString(R.string.settings_device_orientation_portrait))) {
                boolean isPortrait = shared_pref.getBoolean(key_string, false);
                mGp.settingFixDeviceOrientationToPortrait = isPortrait;
                mGp.setDeviceOrientation(mActivity);

                // Disable "Tablet view in landscape mode" (dual pane prefs) menu when option to fix orientation to portrait is checked
                //Preference pref_key_tablet = findPreference(c.getString(R.string.settings_device_orientation_landscape_tablet));
                //Objects.requireNonNull(pref_key_tablet).setEnabled(!isPortrait);
            }
        }
    }

    public static class SettingsSmbFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsSmbFragment", mGp, null);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag_smb, rootKey);

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Register listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            shared_pref.registerOnSharedPreferenceChangeListener(this);

            // Register listener to override "Restore default values" button onClick action
            Preference button = findPreference(getString(R.string.settings_smb_set_default_value_key));
            Objects.requireNonNull(button).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    shared_pref.edit().putBoolean(mContext.getString(R.string.settings_smb_use_extended_security), true).commit();
                    shared_pref.edit().putBoolean(mContext.getString(R.string.settings_smb_disable_plain_text_passwords), false).commit();
                    shared_pref.edit().putString(mContext.getString(R.string.settings_smb_lm_compatibility), SMB_LM_COMPATIBILITY_DEFAULT).commit();
                    shared_pref.edit().putString(mContext.getString(R.string.settings_smb_client_response_timeout), SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT).commit();

                    // Force restart of the fragment: for some reason, PreferenceList is not refreshed !!
                    setResultCallback(true);

                    return false;
                }
            });

            // Check all currrent preferences values and do any custom actions (enable/disable a setting...) on preferences screen creations
            setCurrentValues(shared_pref);
        }

        /*
         * Listener when the preferences change
         * @param sharedPreferences that changed
         * @param key that changed
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
            checkSettingValue(mUtil, shared_pref, key_string, mContext);
/*
            Preference preference = findPreference(key_string);
            if (preference != null) {
                if (!(preference instanceof CheckBoxPreference)){
                    String value = shared_pref.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
*/
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Unregister listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            shared_pref.unregisterOnSharedPreferenceChangeListener(this);
        }

        // Send results from current fragment to listening fragment
        private void setResultCallback(boolean reload) {
            Bundle result = new Bundle();
            result.putBoolean("restart_smb_fragment", reload);

            //requireActivity().getSupportFragmentManager().setFragmentResult("SettingsSmbFragmentRequestFragmentReload", result);
            //((AppCompatActivity)mActivity).getSupportFragmentManager().setFragmentResult("SettingsSmbFragmentRequestFragmentReload", result);
            mActivity.getSupportFragmentManager().setFragmentResult("SettingsSmbFragmentRequestFragmentReload", result);
        }

        private void setCurrentValues(SharedPreferences shared_pref) {
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_use_extended_security), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_lm_compatibility), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_client_response_timeout), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_disable_plain_text_passwords), mContext);
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key = findPreference(key_string);
            Objects.requireNonNull(pref_key);
            if (key_string.equals(c.getString(R.string.settings_smb_use_extended_security))) {
            } else if (key_string.equals(c.getString(R.string.settings_smb_disable_plain_text_passwords))) {
            } else if (key_string.equals(c.getString(R.string.settings_smb_lm_compatibility))) {
                String lmc = shared_pref.getString(c.getString(R.string.settings_smb_lm_compatibility), SMB_LM_COMPATIBILITY_DEFAULT);
                Preference es = findPreference(c.getString(R.string.settings_smb_use_extended_security));
                Objects.requireNonNull(es);
                if (lmc.equals("3") || lmc.equals("4")) {
                    es.setEnabled(false);
                } else {
                    es.setEnabled(true);
                }
                pref_key.setSummary(lmc);
            } else if (key_string.equals(c.getString(R.string.settings_smb_client_response_timeout))) {
                pref_key.setSummary(shared_pref.getString(key_string, SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT) +  " Millis");
            }
        }
    }

    public static class SettingsSecurityFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;
        private String mCurrentAppSettingsDirectory = null;

        ActivityResultLauncher<Intent> activityResultLaunchActivityPasswordSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    int resultCode = result.getResultCode();
                    //Intent data = result.getData();
                    if (resultCode == Activity.RESULT_OK) {
                        SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                        checkSettingValue(mUtil, shared_pref, getString(R.string.settings_security_application_password), mContext);
                    }
                }
            }
        );

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsSecurityFragment", mGp, null);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag_security, rootKey);

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Register listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            shared_pref.registerOnSharedPreferenceChangeListener(this);

            // Register listener to start password activity on preference click
            Preference button = findPreference(getString(R.string.settings_security_application_password));
            Objects.requireNonNull(button).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    Intent in = new Intent(mContext, ActivityPasswordSettings.class);
                    activityResultLaunchActivityPasswordSettings.launch(in);
                    return false;
                }
            });

            // Get current app settings directory preference value
            mCurrentAppSettingsDirectory = GlobalParameters.getAppManagementDirSetting(mContext);

            // Check all currrent preferences values and do any custom actions (enable/disable a setting...) on preferences screen creations
            setCurrentValues(shared_pref);
        }

        /*
         * Listener when the preferences change
         * @param sharedPreferences that changed
         * @param key that changed
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
            checkSettingValue(mUtil, shared_pref, key_string, mContext);
/*
            Preference preference = findPreference(key_string);
            if (preference != null) {
                if (!(preference instanceof CheckBoxPreference)){
                    String value = shared_pref.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
*/
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Unregister listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            shared_pref.unregisterOnSharedPreferenceChangeListener(this);
        }

        private void setCurrentValues(SharedPreferences shared_pref) {
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_security_application_password), mContext);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_security_app_settings_directory), mContext);
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key = findPreference(key_string);
            String hv = ApplicationPasswordUtils.getPasswordHashValue(shared_pref);

            // pref_key can be null if key_string equals settings_application_password_hash_value
            // this is the case when We create, remove or change app password.
            // the preference is edited in ApplicationPasswordUtils.class when we set the password
            // This triggers onSharedPreferenceChanged() for the key settings_application_password_hash_value
            // Since settings_application_password_hash_value is not present in our Security Preferences xml resource, it will return null
            // One other alternative would be to add it as a hidden preference in xml
/*
            <Preference
                android:defaultValue=""
                android:key="settings_application_password_hash_value"
                android:title="Password Hash"
                app:isPreferenceVisible="false" />
*/
            //Objects.requireNonNull(pref_key);
            if (pref_key == null) {
                mUtil.addDebugMsg(1, "I", "checkSettingValue: pref_key was null, key_string=" + key_string);
            }

            if (key_string.equals(c.getString(R.string.settings_security_application_password))) {
                String contents_string;
                if (hv.equals("")) {
                    contents_string=c.getString(R.string.settings_security_application_password_not_created);
                } else  {
                    contents_string = "-" + c.getString(R.string.settings_security_application_password_created);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_use_auth_timeout), true))
                        contents_string += "\n-" + c.getString(R.string.settings_security_use_auth_timeout_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_app_startup), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_application_password_use_app_startup_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_edit_task), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_application_password_use_edit_task_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_export_task), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_application_password_use_export_task_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_init_smb_account_password), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_init_smb_account_password_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_init_zip_passowrd), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_init_zip_passowrd_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_hide_show_smb_passowrd), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_hide_show_smb_passowrd_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_hide_show_zip_passowrd), false))
                        contents_string += "\n-" + c.getString(R.string.settings_security_hide_show_zip_passowrd_title);
                }
                Objects.requireNonNull(pref_key).setSummary(contents_string);
            } else if (key_string.equals(c.getString(R.string.settings_security_app_settings_directory))) {
                String kv = shared_pref.getString(key_string, APP_SETTINGS_DIRECTORY_ROOT);
                String summary = "";
                if (kv.equals(APP_SETTINGS_DIRECTORY_ROOT)) {
                    // /data/data/APPLICATION_ID/files
                    try {
                        summary = c.getFilesDir().getCanonicalPath();
                    } catch (IOException e) {
                        e.printStackTrace();
                        summary = "/data/data/"+APPLICATION_ID+"/files";
                    }
                    summary = String.format(c.getString(R.string.settings_security_app_settings_directory_summary_root), summary);
                } else if (kv.equals(APP_SETTINGS_DIRECTORY_APP_SPECIFIC_INTERNAL)) {
                    // /storage/emulated/0/Android/data/APPLICATION_ID/files
                    summary = SafManager3.getAppSpecificDirectory(c, SafFile3.SAF_FILE_PRIMARY_UUID);
                    if (summary == null) {
                        summary = SafManager3.getPrimaryStoragePath() + "/Android/data/" + APPLICATION_ID + "/files";
                        mUtil.addDebugMsg(1, "I", "checkSettingValue: SafManager3.getAppSpecificDirectory returns null on primary uid");
                    }
                    summary = String.format(c.getString(R.string.settings_security_app_settings_directory_summary_app_specific), summary);
                } else if (kv.equals(APP_SETTINGS_DIRECTORY_STORAGE)) {
                    // /storage/emulated/0/app_name
                    summary = String.format(c.getString(R.string.settings_security_app_settings_directory_summary_storage), mGp.externalStoragePrefix+"/"+APPLICATION_TAG);
                }
                Objects.requireNonNull(pref_key).setSummary(summary);

                if (!mCurrentAppSettingsDirectory.equals(kv)) {
                    //mCurrentAppSettingsDirectory = kv; //not needed because we restart activity immeadiately on Settings exit
                    // If the app dir setting is changed, an app restart is needed to properly display further messages and history
                    // App restart is triggered in reloadSettingParms() because it is useless here
                    // To properly preserve eventual log messages, we could setup a FragmentResultListener to restart app when returning to main Settings screen/fragment
                    //mActivity.finish();
                    //Intent intent = new Intent(mActivity, ActivitySettings.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    //mActivity.startActivity(intent);
                }
            }
        }
    }

    public static class SettingsMiscFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private AppCompatActivity mActivity = null;
        private Context mContext = null;
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mActivity = (ActivitySettings)getActivity();
            mContext = mActivity;
            mUtil = new CommonUtilities(mContext, "SettingsMiscFragment", mGp, null);

            super.onCreate(savedInstanceState);//calls onCreatePreferences()

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.settings_frag_misc, rootKey);

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Register listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            shared_pref.registerOnSharedPreferenceChangeListener(this);

            // Register listener to start activity with system app permissions
            Preference button = findPreference(getString(R.string.settings_app_permissions));
            Objects.requireNonNull(button).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                    intent.setData(uri);
                    mActivity.startActivity(intent);

                    return false;
                }
            });

            // Check all currrent preferences values and do any custom actions (enable/disable a setting...) on preferences screen creations
            //setCurrentValues(shared_pref);

            shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean), false).commit();
            Preference pref_key = findPreference(getString(R.string.settings_exit_clean));
            if (pref_key != null) pref_key.setEnabled(false);

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_exit_clean), mContext);
        }

        /*
         * Listener when the preferences change
         * @param sharedPreferences that changed
         * @param key that changed
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
            checkSettingValue(mUtil, shared_pref, key_string, mContext);
/*
            Preference preference = findPreference(key_string);
            if (preference != null) {
                if (!(preference instanceof CheckBoxPreference)){
                    String value = shared_pref.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
*/
        }

        // Tied to Activity.onStart of containing app, called when fragment gets visible to user
        @Override
        public void onStart() {
            super.onStart();

            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        }

        // Tied to parent activity state, called when fragment is no longer visible
        @Override
        public void onStop() {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            super.onStop();
        }

        // Cleaning up, called after onStop when view is detached
        @Override
        public void onDestroyView() {
            super.onDestroyView();

            //SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences shared_pref = CommonUtilities.getSharedPreference(mContext);

            // Unregister listener to call onSharedPreferenceChanged() when a preference changes
            //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            shared_pref.unregisterOnSharedPreferenceChangeListener(this);
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            if (key_string.equals(c.getString(R.string.settings_exit_clean))) {
            }
        }
    }
}