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

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.sentaroh.android.Utilities3.Dialog.MessageDialogAppFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.GlobalParameters.APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT;
import static com.sentaroh.android.SMBSync3.GlobalParameters.SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT;
import static com.sentaroh.android.SMBSync3.GlobalParameters.SMB_LM_COMPATIBILITY_DEFAULT;

public class ActivitySettings extends PreferenceActivity {
    static final private Logger log= LoggerFactory.getLogger(ActivitySettings.class);

    private static GlobalParameters mGp = null;

    private static String mCurrentScreenTheme=SCREEN_THEME_STANDARD;

    private CommonUtilities mUtil = null;

    @Override
    protected boolean isValidFragment(String fragmentName) {
        // 使用できる Fragment か確認する

        return true;
    }

    @Override
    protected void attachBaseContext(Context base) {
//        log.info("attachBaseContext entered");
        super.attachBaseContext(GlobalParameters.setNewLocale(base));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mGp= GlobalWorkArea.getGlobalParameter(ActivitySettings.this);
        SharedPreferences shared_pref = CommonUtilities.getSharedPreference(ActivitySettings.this);
        mCurrentScreenTheme=shared_pref.getString(getString(R.string.settings_screen_theme), SCREEN_THEME_STANDARD);
        if (mCurrentScreenTheme.equals(SCREEN_THEME_STANDARD)) setTheme(R.style.Main);
        else if (mCurrentScreenTheme.equals(SCREEN_THEME_LIGHT)) setTheme(R.style.MainLight);
        else if (mCurrentScreenTheme.equals(SCREEN_THEME_BLACK)) setTheme(R.style.MainBlack);
        GlobalParameters.setDisplayFontScale(ActivitySettings.this);
        super.onCreate(savedInstanceState);

        if (mUtil == null) mUtil = new CommonUtilities(this, "SettingsActivity", mGp, null);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        mCurrentScreenTheme=shared_pref.getString(getString(R.string.settings_screen_theme), SCREEN_THEME_STANDARD);
        mCurrentLangaue =GlobalParameters.getLanguageCode(ActivitySettings.this);
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

    private List<Header> mHeaderList=null;
    public void refreshHeader(String fs) {
        invalidateHeaders();
    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mHeaderList=target;
        loadHeadersFromResource(R.xml.settings_frag, target);
    }

    @Override
    public boolean onIsMultiPane() {
        mGp= GlobalWorkArea.getGlobalParameter(ActivitySettings.this);
        mUtil = new CommonUtilities(ActivitySettings.this, "SettingsActivity", mGp, null);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        return isTablet(this.getApplicationContext(), mUtil);

    }

    public static boolean isTablet(Context context, CommonUtilities cu) {
        float fs= GlobalParameters.getFontScaleFactorValue(context);
        int multiPaneDP=500;
        String lang_code=Locale.getDefault().getLanguage();
        if (lang_code.equals("fr")) multiPaneDP=540;
        else if (lang_code.equals("ru")) multiPaneDP=800;

        multiPaneDP=(int)(((float)multiPaneDP)*fs);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final float x_px = (float) Math.min(metrics.heightPixels, metrics.widthPixels);
        final float y_px = (float) Math.max(metrics.heightPixels, metrics.widthPixels);
        boolean portrait_mp = (x_px/metrics.density) >= multiPaneDP;
        boolean land_mp = (y_px/metrics.density) >= multiPaneDP;

        int orientation = context.getResources().getConfiguration().orientation;
        boolean sc_land_mp = (land_mp || mGp.settingForceDeviceTabletViewInLandscape) && orientation == Configuration.ORIENTATION_LANDSCAPE; //screen is in landscape orientation and either width size >= multiPaneDP or user forced MultiPanel view in landscape
        return portrait_mp||sc_land_mp; //use MultiPane display in portrait if width >= multiPaneDP or in landscape if largest screen side >= multiPaneDP
    }


    private Context getActivityContext() {
        return ActivitySettings.this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    final public void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    final public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    private static void playBackDefaultNotification(Context c, FragmentManager fm, int vol) {
        float volume = (float) vol / 100.0f;
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri != null) {
            final MediaPlayer player = MediaPlayer.create(c, uri);
            if (player != null) {
                player.setVolume(volume, volume);
                if (player != null) {
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
                }
            } else {
                MessageDialogAppFragment cd=MessageDialogAppFragment.newInstance(
                        false, "E", c.getString(R.string.settings_playback_ringtone_volume_disabled),"");
                cd.showDialog(fm, cd, null);
            }
        }
    }


    public static class SettingsSync extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettingValue(mUtil, shared_pref, key_string, getContext());
                    }
                };
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUtil = new CommonUtilities(getContext(), "SettingsSync", mGp, null);
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            addPreferencesFromResource(R.xml.settings_frag_sync);

            SharedPreferences shared_pref=CommonUtilities.getSharedPreference(getContext());

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_wifi_lock), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_sync_history_log), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_force_screen_on_while_sync), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_add_external_storage_notification), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_start_sync_confirmation_message), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_suppress_shortcut1_confirmation_message), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_no_compress_file_type), getContext());

        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key=findPreference(key_string);
            if (key_string.equals(c.getString(R.string.settings_force_screen_on_while_sync))) {
            } else if (key_string.equals(c.getString(R.string.settings_wifi_lock))) {
            } else if (key_string.equals(c.getString(R.string.settings_sync_history_log))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_add_external_storage_notification))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_start_sync_confirmation_message))) {
            } else if (key_string.equals(c.getString(R.string.settings_suppress_shortcut1_confirmation_message))) {
            } else if (key_string.equals(c.getString(R.string.settings_no_compress_file_type))) {
                if (shared_pref.getString(key_string, "").equals("")) {
                    shared_pref.edit().putString(key_string, DEFAULT_NOCOMPRESS_FILE_TYPE).commit();
                }
                pref_key.setSummary(shared_pref.getString(key_string, DEFAULT_NOCOMPRESS_FILE_TYPE));
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_sync_title);
        }

        @Override
        public void onStop() {
            super.onStop();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        }

    }

    public static class SettingsMisc extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettingValue(mUtil, shared_pref, key_string, getContext());
                    }
                };
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUtil = new CommonUtilities(getContext(), "SettingsMisc", mGp, null);
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            addPreferencesFromResource(R.xml.settings_frag_misc);

            SharedPreferences shared_pref=CommonUtilities.getSharedPreference(getContext());

//            shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean), true).commit();
//            findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_exit_clean), getContext());
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {

            if (key_string.equals(c.getString(R.string.settings_exit_clean))) {
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_misc_title);
        }

        @Override
        public void onStop() {
            super.onStop();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        }

        ;
    }

    public static class SettingsSmb extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettingValue(mUtil, shared_pref, key_string, getContext());
                    }
                };
        private CommonUtilities mUtil = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUtil = new CommonUtilities(getContext(), "SettingsSmb", mGp, null);
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            addPreferencesFromResource(R.xml.settings_frag_smb);


            SharedPreferences shared_pref=CommonUtilities.getSharedPreference(getContext());

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_use_extended_security), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_lm_compatibility), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_client_response_timeout), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_smb_disable_plain_text_passwords), getContext());

            final Context c=getContext();
            Preference button = (Preference)getPreferenceManager().findPreference(getString(R.string.settings_smb_set_default_value_key));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    shared_pref.edit().putBoolean(c.getString(R.string.settings_smb_use_extended_security),true).commit();
                    shared_pref.edit().putBoolean(c.getString(R.string.settings_smb_disable_plain_text_passwords),false).commit();
                    shared_pref.edit().putString(c.getString(R.string.settings_smb_lm_compatibility),SMB_LM_COMPATIBILITY_DEFAULT).commit();
                    shared_pref.edit().putString(c.getString(R.string.settings_smb_client_response_timeout),SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT).commit();
                    return false;
                }
            });

        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key=findPreference(key_string);
            if (key_string.equals(c.getString(R.string.settings_smb_use_extended_security))) {
            } else if (key_string.equals(c.getString(R.string.settings_smb_disable_plain_text_passwords))) {
            } else if (key_string.equals(c.getString(R.string.settings_smb_lm_compatibility))) {
                String lmc=shared_pref.getString(c.getString(R.string.settings_smb_lm_compatibility),SMB_LM_COMPATIBILITY_DEFAULT);
                if (lmc.equals("3") || lmc.equals("4")) {
                    findPreference(c.getString(R.string.settings_smb_use_extended_security).toString()).setEnabled(false);
                } else {
                    findPreference(c.getString(R.string.settings_smb_use_extended_security).toString()).setEnabled(true);
                }
                pref_key.setSummary(lmc);
            } else if (key_string.equals(c.getString(R.string.settings_smb_client_response_timeout))) {
                pref_key.setSummary(shared_pref.getString(key_string, SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT)+" Millis");
            }
        }


        @Override
        public void onStart() {
            super.onStart();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_smb_title);
        }

        @Override
        public void onStop() {
            super.onStop();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        }

    }

    private static String mCurrentLangaue =null;

    public static class SettingsUi extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettingValue(mUtil, shared_pref, key_string, getContext());
                    }
                };
        private CommonUtilities mUtil = null;
        private int mInitVolume = 100;
        private String mCurrentFontScaleFactor=null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUtil = new CommonUtilities(getContext(), "SettingsUi", mGp, null);
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            addPreferencesFromResource(R.xml.settings_frag_ui);

            SharedPreferences shared_pref=CommonUtilities.getSharedPreference(getContext());

            mInitVolume = shared_pref.getInt(getString(R.string.settings_playback_ringtone_volume), 100);

            mCurrentFontScaleFactor=shared_pref.getString(getString(R.string.settings_display_font_scale_factor), GlobalParameters.FONT_SCALE_FACTOR_NORMAL);

            setCurrentValue(shared_pref);
        }

        private void setCurrentValue(SharedPreferences shared_pref) {
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_notification_message_when_sync_ended), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_playback_ringtone_when_sync_ended), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_playback_ringtone_volume), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_vibrate_when_sync_ended), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_screen_theme), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_device_orientation_portrait), getContext());

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_screen_theme_language), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_display_font_scale_factor), getContext());
            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_device_orientation_landscape_tablet), getContext());

        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key=findPreference(key_string);
            if (key_string.equals(c.getString(R.string.settings_playback_ringtone_when_sync_ended))) {
                Preference rv = findPreference(c.getString(R.string.settings_playback_ringtone_volume));
                String kv=shared_pref.getString(key_string, NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS);
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
                String kv=shared_pref.getString(key_string, NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS);
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
                String kv=shared_pref.getString(key_string, NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS);
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
                if (mInitVolume != vol) playBackDefaultNotification(c, getFragmentManager(), vol);
            } else if (key_string.equals(c.getString(R.string.settings_screen_theme))) {
                String tid=shared_pref.getString(key_string, SCREEN_THEME_STANDARD);
                String[] wl_label = c.getResources().getStringArray(R.array.settings_screen_theme_list_entries);
                String sum_msg = wl_label[Integer.parseInt(tid)];
                pref_key.setSummary(sum_msg);
                if (!mCurrentScreenTheme.equals(tid)) {
                    if (tid.equals(SCREEN_THEME_STANDARD)) getActivity().setTheme(R.style.Main);
                    else if (tid.equals(SCREEN_THEME_LIGHT)) getActivity().setTheme(R.style.MainLight);
                    else if (tid.equals(SCREEN_THEME_BLACK)) getActivity().setTheme(R.style.MainBlack);
                    mCurrentScreenTheme=tid;
                    getActivity().finish();
                    Intent intent = new Intent(getActivity(), ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(intent);
                }
            } else if (key_string.equals(c.getString(R.string.settings_screen_theme_language))) {
                String lang_value=shared_pref.getString(key_string, APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT);
                String[] lang_msgs = c.getResources().getStringArray(R.array.settings_screen_theme_language_list_entries);
                String sum_msg=lang_msgs[0];
                if (!lang_value.equals(APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT)) {
                    for(int i=0;i<lang_msgs.length;i++) {
                        sum_msg=lang_msgs[i];
                        if (sum_msg.contains("("+lang_value+")")) {
                            break;
                        }
                    }
                }
                pref_key.setSummary(sum_msg);
                if (!lang_value.equals(mCurrentLangaue)) {
                    getActivity().finish();//finish current preferences activity. Will trigger checkThemeLanguageChanged() to force restart app from main activity
                    GlobalParameters.setNewLocale(getActivity());
                    Intent intent = new Intent(getActivity(), ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(intent);
                }
            } else if (key_string.equals(c.getString(R.string.settings_display_font_scale_factor))) {
                String font_scale_id=shared_pref.getString(key_string, GlobalParameters.FONT_SCALE_FACTOR_NORMAL);
                String[] font_scale_label = c.getResources().getStringArray(R.array.settings_display_font_scale_factor_list_entries);
                String sum_msg = font_scale_label[Integer.parseInt(font_scale_id)];
                pref_key.setSummary(sum_msg);
                if (!mCurrentFontScaleFactor.equals(font_scale_id)) {
                    mCurrentFontScaleFactor=font_scale_id;
                    GlobalParameters.setDisplayFontScale(c, mCurrentFontScaleFactor);
                    getActivity().finish();
                    Intent intent = new Intent(getActivity(), ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(intent);
                }
            } else if (key_string.equals(c.getString(R.string.settings_device_orientation_landscape_tablet))) {
            } else if (key_string.equals(c.getString(R.string.settings_device_orientation_portrait))) {
                boolean portrait=shared_pref.getBoolean(key_string, false);
                Preference pref_key_tablet=findPreference(c.getString(R.string.settings_device_orientation_landscape_tablet));
                if (portrait) pref_key_tablet.setEnabled(false);
                else pref_key_tablet.setEnabled(true);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_ui_title);
        }

        @Override
        public void onStop() {
            super.onStop();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        }

    }

    public static class SettingsSecurity extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
                        checkSettingValue(mUtil, shared_pref, key_string, getContext());
                    }
                };
        private CommonUtilities mUtil = null;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 0) {
                if (resultCode == RESULT_OK) {
                    SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(getContext());
                    checkSettingValue(mUtil, shared_pref, getString(R.string.settings_security_application_password), getContext());
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mUtil = new CommonUtilities(getContext(), "SettingsSecurity", mGp, null);
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

            addPreferencesFromResource(R.xml.settings_frag_security);

            SharedPreferences shared_pref=CommonUtilities.getSharedPreference(getContext());

            checkSettingValue(mUtil, shared_pref, getString(R.string.settings_security_application_password), getContext());

            Preference button = (Preference)getPreferenceManager().findPreference(getString(R.string.settings_security_application_password));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent in=new Intent(getContext(), ActivityPasswordSettings.class);
                    startActivityForResult(in, 0);
                    return false;
                }
            });
        }

        private void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string, Context c) {
            Preference pref_key=findPreference(key_string);
            String hv= ApplicationPasswordUtils.getPasswordHashValue(shared_pref);

            if (key_string.equals(c.getString(R.string.settings_security_application_password))) {
                String contents_string="";
                if (hv.equals("")) {
                    contents_string=c.getString(R.string.settings_security_application_password_not_created);
                } else  {
                    contents_string="-"+c.getString(R.string.settings_security_application_password_created);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_use_auth_timeout), true))
                        contents_string+="\n-"+c.getString(R.string.settings_security_use_auth_timeout_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_app_startup), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_application_password_use_app_startup_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_edit_task), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_application_password_use_edit_task_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_application_password_use_export_task), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_application_password_use_export_task_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_init_smb_account_password), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_init_smb_account_password_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_init_zip_passowrd), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_init_zip_passowrd_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_hide_show_smb_passowrd), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_hide_show_smb_passowrd_title);
                    if (shared_pref.getBoolean(c.getString(R.string.settings_security_hide_show_zip_passowrd), false))
                        contents_string+="\n-"+c.getString(R.string.settings_security_hide_show_zip_passowrd_title);
                }
                pref_key.setSummary(contents_string);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listenerAfterHc);
            getActivity().setTitle(R.string.settings_ui_title);
        }

        @Override
        public void onStop() {
            super.onStop();
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
        }

    }

}