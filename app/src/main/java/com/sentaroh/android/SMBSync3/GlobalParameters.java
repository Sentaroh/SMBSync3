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


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.LocaleList;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.sentaroh.android.SMBSync3.Log.LogUtil;

import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThreadCtrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jcifs.util.LogStream;

import static android.content.Context.WINDOW_SERVICE;
import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.*;

public class GlobalParameters {
//    public Context appContext = null;
    private ReentrantReadWriteLock configurationLock =new ReentrantReadWriteLock();

    public Handler uiHandler = null;

    public boolean debuggable = false;

    public boolean activityIsFinished = true;
    public boolean logCatActive=false;

    public String profilePassword = "";

    private final static String SUPRESS_GRANT_LOCATION_PERMISSION_KEY ="settings_sync_supress_grant_location_permission";

    public ArrayBlockingQueue<SyncRequestItem> syncRequestQueue = new ArrayBlockingQueue<SyncRequestItem>(1000);

    public ThreadCtrl syncThreadConfirm = new ThreadCtrl();
    public ThreadCtrl syncThreadCtrl = new ThreadCtrl();

    public boolean activityRestartRequired = false;
    public boolean activityIsBackground = true;
    public boolean syncThreadEnabled = true;
    public boolean syncThreadActive = false;
    public boolean syncThreadConfirmWait = false;

    public ISvcCallback callbackStub = null;

//    public boolean themeIsLight = true;
    public String settingScreenTheme = SCREEN_THEME_STANDARD;
    public int applicationTheme = -1;
    public ThemeColorList themeColorList = null;

    //	Settings parameter
//    public String settingAppManagemsntDirectoryUuid ="primary";
    public String settingAppManagemsntDirectoryName = SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX+"/"+APPLICATION_TAG;
    public boolean settingExitClean = false;
    public boolean settingSyncMessageUseStandardTextView =false;

    public boolean settingWriteSyncResultLog = true;

//    public boolean settingErrorOption = false;
    public boolean settingWifiLockRequired = true;

    public String settingNoCompressFileType = DEFAULT_NOCOMPRESS_FILE_TYPE;

    public boolean settingFixDeviceOrientationToPortrait = false;
    public boolean settingForceDeviceTabletViewInLandscape = false;

    public boolean settingSupressAddExternalStorageNotification =false;
    public boolean isSupressAddExternalStorageNotification() {
        return settingSupressAddExternalStorageNotification;
    }
    public void setSupressAddExternalStorageNotification(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_add_external_storage_notification), suppress).commit();
        settingSupressAddExternalStorageNotification=suppress;
    }

    public boolean settingSupressStartSyncConfirmationMessage =false;
    public boolean isSupressStartSyncConfirmationMessage() {
        return settingSupressStartSyncConfirmationMessage;
    }
    public void setSupressStartSyncConfirmationMessage(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_start_sync_confirmation_message), suppress).commit();
        settingSupressStartSyncConfirmationMessage=suppress;
    }

    public boolean settingSupressStartGroupConfirmationMessage =false;
    public boolean isSupressStartGroupConfirmationMessage() { return settingSupressStartGroupConfirmationMessage;}
    public void setSupressStartGroupConfirmationMessage(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_start_group_confirmation_message), suppress).commit();
        settingSupressStartGroupConfirmationMessage=suppress;
    }

    public boolean settingSupressShortcut1ConfirmationMessage =false;
    public boolean isSupressShortcut1ConfirmationMessage() {
        return settingSupressShortcut1ConfirmationMessage;
    }
    public void setSupressShortcut1ConfirmationMessage(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_shortcut1_confirmation_message), suppress).commit();
        settingSupressShortcut1ConfirmationMessage =suppress;
    }

    public boolean settingSupressShortcut2ConfirmationMessage =false;
    public boolean isSupressShortcut2ConfirmationMessage() {
        return settingSupressShortcut2ConfirmationMessage;
    }
    public void setSupressShortcut2ConfirmationMessage(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_shortcut2_confirmation_message), suppress).commit();
        settingSupressShortcut2ConfirmationMessage =suppress;
    }

    public boolean settingSupressShortcut3ConfirmationMessage =false;
    public boolean isSupressShortcut3ConfirmationMessage() {
        return settingSupressShortcut3ConfirmationMessage;
    }
    public void setSupressShortcut3ConfirmationMessage(Context c, boolean suppress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putBoolean(c.getString(R.string.settings_suppress_shortcut3_confirmation_message), suppress).commit();
        settingSupressShortcut3ConfirmationMessage =suppress;
    }


    public String settingSecurityApplicationPasswordHashValue = "";
//    public boolean settingSecurityApplicationPassword = false;
    public boolean settingSecurityApplicationPasswordUseAppStartup = false;
    public boolean settingSecurityApplicationPasswordUseEditTask = false;
    public boolean settingSecurityApplicationPasswordUseExport = false;
    public boolean settingSecurityReinitSmbAccountPasswordValue = false;
    public boolean settingSecurityReinitZipPasswordValue = false;
    public boolean settingSecurityHideShowPasswordButton = false;

    public boolean appPasswordAuthValidated=false;
    public long appPasswordAuthLastTime=0L;

    public boolean settingPreventSyncStartDelay = true;
//    public boolean settingScreenOnIfScreenOnAtStartOfSync = false;

    public boolean settingExportedTaskEncryptRequired = true;

    public String settingNotificationMessageWhenSyncEnded = NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS;
    public String settingNotificationVibrateWhenSyncEnded = NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
    public String settingNotificationSoundWhenSyncEnded = NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS;

    public int settingNotificationVolume = 100;

    public float displayFontSizeMedium =15.0f;//Small=12, Normal=15, Large=18
    public float displayFontSizeSmall=displayFontSizeMedium/1.2f;
    public float displayFontSizeLarge=displayFontSizeMedium*1.2f;

    public boolean settingScheduleSyncEnabled=true;

    public NotificationManager notificationManager = null;
    public boolean notificationEnabled = true;
    public int notificationSmallIcon = R.drawable.ic_48_smbsync_wait;
    public Notification notification = null;
    public NotificationCompat.Builder notificationBuilder = null;
    public NotificationCompat.BigTextStyle notificationBigTextStyle = null;
    public Intent notificationIntent = null;
    public PendingIntent notificationPendingIntent = null;
    public String notificationLastShowedMessage = null, notificationLastShowedTitle = "";
    public long notificationLastShowedWhen = 0;
    public String notificationAppName = "";
    //	public boolean notiifcationEnabled=false;
    public long notificationNextShowedTime = 0;
//    public Bitmap notificationLargeIcon = null;

    public static final int MESSAGE_LIST_INITIAL_VALUE=5500;
    public ArrayList<MessageListAdapter.MessageListItem> syncMessageList = null; //new ArrayList<MessageListItem>();
    public boolean syncMessageListChanged =false;
    public boolean freezeMessageViewScroll = false;
    public MessageListAdapter syncMessageListAdapter = null;
    public ListView syncMessageView = null;

    public static final int HISTORY_LIST_INITIAL_VALUE=510;
    public ArrayList<HistoryListAdapter.HistoryListItem> syncHistoryList = null;
    public HistoryListAdapter syncHistoryListAdapter = null;
    public ListView syncHistoryView = null;

    public ArrayList<ScheduleListAdapter.ScheduleListItem> syncScheduleList = new ArrayList<ScheduleListAdapter.ScheduleListItem>();
    public ScheduleListAdapter syncScheduleListAdapter = null;
    public ListView syncScheduleView = null;
    public TextView syncScheduleMessage =null;

    public ArrayList<GroupListAdapter.GroupListItem> syncGroupList = new ArrayList<GroupListAdapter.GroupListItem>();
    public GroupListAdapter syncGroupListAdapter = null;
    public ListView syncGroupView = null;
    public TextView syncGroupMessage =null;

    public ArrayList<SyncTaskItem> syncTaskList = new ArrayList<SyncTaskItem>();
    public TaskListAdapter syncTaskListAdapter = null;
    public ListView syncTaskView = null;
    public TextView syncTaskEmptyMessage=null;

    public TextView scheduleInfoView = null;
    public String scheduleInfoText = "";
    public TextView scheduleErrorView = null;
    public String scheduleErrorText = "";

    public boolean dialogWindowShowed = false;
    public String progressSpinSyncprofText = "", progressSpinMsgText = "";

    public boolean confirmDialogShowed = false;
    public String confirmDialogFilePathPairA = "";
    public long confirmDialogFileLengthPairA = 0L, confirmDialogFileLastModPairA = 0L;
    public String confirmDialogFilePathPairB = "";
    public long confirmDialogFileLengthPairB = 0L, confirmDialogFileLastModPairB = 0L;
    public String confirmDialogMethod = "";
    public String confirmDialogMessage = "";

    public LinearLayout mainDialogView = null;
    public LinearLayout confirmView = null;
    public LinearLayout confirmOverrideView = null;
    public LinearLayout confirmConflictView = null;
    public TextView confirmMsg = null;
    public Button confirmCancel = null;
    public View.OnClickListener confirmCancelListener = null;
    public Button confirmYes = null;
    public View.OnClickListener confirmYesListener = null;
    public Button confirmNo = null;
    public View.OnClickListener confirmNoListener = null;
    public Button confirmYesAll = null;
    public View.OnClickListener confirmYesAllListener = null;
    public Button confirmNoAll = null;
    public View.OnClickListener confirmNoAllListener = null;

    public TextView confirmDialogConflictFilePathA=null;
    public TextView confirmDialogConflictFileLengthA=null;
    public TextView confirmDialogConflictFileLastModA=null;
    public TextView confirmDialogConflictFilePathB=null;
    public TextView confirmDialogConflictFileLengthB=null;
    public TextView confirmDialogConflictFileLastModB=null;

    public Button confirmDialogConflictButtonSelectA=null;
    public View.OnClickListener confirmDialogConflictButtonSelectAListener = null;
    public Button confirmDialogConflictButtonSelectB=null;
    public View.OnClickListener confirmDialogConflictButtonSelectBListener = null;
    public Button confirmDialogConflictButtonSyncIgnoreFile=null;
    public View.OnClickListener confirmDialogConflictButtonSyncIgnoreFileListener = null;
    public Button confirmDialogConflictButtonCancelSyncTask=null;
    public View.OnClickListener confirmDialogConflictButtonCancelSyncTaskListener = null;

    public LinearLayout progressSpinView = null;
    public TextView progressSpinSynctask = null;
    public TextView progressSpinMsg = null;
    public Button progressSpinCancel = null;
    public View.OnClickListener progressSpinCancelListener = null;

    public SafManager3 safMgr = null;

    private static LogStream logStream=null;//JCIFS logStream
    private static Logger log = LoggerFactory.getLogger(GlobalParameters.class);

    public GlobalParameters() {
    }

//    private Semaphore configSemaphore =new Semaphore(1);
    public void acquireConfigurationLock() {
        configurationLock.writeLock().lock();
//        try {
//            configSemaphore.acquire();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public void releaseConfigurationLock() {
        configurationLock.writeLock().unlock();
//        configSemaphore.release();
    }

    public void waitConfigurationLock() {
        acquireConfigurationLock();
        releaseConfigurationLock();
    }

    synchronized public void initGlobalParamter(Context c) {
        uiHandler = new Handler();

        debuggable = isDebuggable(c);

        mDimWakeLock = ((PowerManager) c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SMBSync3-thread-dim");
        forceDimScreenWakelock = ((PowerManager) c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SMBSync3-thread-force-dim");
        mPartialWakeLock = ((PowerManager) c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SMBSync3-thread-partial");
        WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SMBSync3-thread");

        initLogger(c);

        initStorageStatus(c);

        initSettingsParms(c);
        loadSettingsParms(c);

        if (syncMessageList == null) syncMessageList =CommonUtilities.loadMessageList(c, this);

        initJcifsOption(c);

    }

    public boolean configListLoaded=false;
    public void loadConfigList(Context c) {
        acquireConfigurationLock();
        if (!configListLoaded) {
            configListLoaded=true;
            ArrayList<SyncTaskItem>stl=new ArrayList<SyncTaskItem>();
            ArrayList<ScheduleListAdapter.ScheduleListItem>sl=new ArrayList<ScheduleListAdapter.ScheduleListItem>();
            ArrayList<GroupListAdapter.GroupListItem>gl=new ArrayList<GroupListAdapter.GroupListItem>();
            TaskListImportExport.loadTaskListFromAppDirectory(c, stl, sl, null, gl);
            syncTaskList.addAll(stl);
            syncScheduleList.addAll(sl);
            syncGroupList.addAll(gl);
        }
        releaseConfigurationLock();
    }

    private void initLogger(Context c) {
        final LogUtil jcifs_ng_lu = new LogUtil(c, "SLF4J");
        final LogUtil jcifs_old_lu = new LogUtil(c, "JCIFS-V1");

        PrintStream smb1_ps= null;
        OutputStream smb1_os=new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                byte[] buff = ByteBuffer.allocate(4).putInt(b).array();
            }
            @Override
            public void write(byte[] buff) throws IOException {
                if (buff.length==1 && buff[0]!=0x0a) {
                } else {
                    String msg=new String(buff,"UTF-8");
                    if (!msg.equals("\n") && msg.replaceAll(" ","").length()>0) jcifs_old_lu.addDebugMsg(0,"I",msg);
                }
            }
            @Override
            public void write(byte[] buff, int buff_offset, int buff_length) throws IOException {
                if (buff_length==1 && buff[buff_offset]!=0x0a) {
                } else {
                    String msg=new String(buff,buff_offset,buff_length, "UTF-8");
                    if (!msg.equals("\n") && msg.replaceAll(" ","").length()>0) jcifs_old_lu.addDebugMsg(0,"I",msg);
                }
            }
        };
        smb1_ps=new PrintStream(smb1_os);
        LogStream.setInstance(smb1_ps);
        logStream= LogStream.getInstance();//Initial create JCIFS logStream object

        Slf4jLogWriter jcifs_ng_lw=new Slf4jLogWriter(jcifs_ng_lu);
        log.setWriter(jcifs_ng_lw);
    }

    public void clearParms(Context c) {
//        synchronized (msgList) {
//            msgList = new ArrayList<MessageListItem>();
//            msgListAdapter = null;
//        }
    }

    @SuppressLint("NewApi")
    public void initStorageStatus(Context c) {

        refreshMediaDir(c);
    }

    public void refreshMediaDir(Context c) {
        if (safMgr == null) {
            Thread th=new Thread(){
                @Override
                public void run() {
                    safMgr = new SafManager3(c);
                }
            };
            th.start();
        } else {
            safMgr.refreshSafList();
        }
    }

    public void initSettingsParms(Context c) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor pe = prefs.edit();

        if (!prefs.contains(c.getString(R.string.settings_exit_clean))) {
            pe.putBoolean(c.getString(R.string.settings_exit_clean), false);
            pe.putString(c.getString(R.string.settings_smb_lm_compatibility), SMB_LM_COMPATIBILITY_DEFAULT);
            pe.putBoolean(c.getString(R.string.settings_smb_use_extended_security), true);
            pe.putString(c.getString(R.string.settings_smb_client_response_timeout), SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT);
            pe.putBoolean(c.getString(R.string.settings_smb_disable_plain_text_passwords), false);
        }

        if (!prefs.contains(c.getString(R.string.settings_screen_theme)))
            pe.putString(c.getString(R.string.settings_screen_theme), SCREEN_THEME_STANDARD);

        if (!prefs.contains(c.getString(R.string.settings_notification_message_when_sync_ended)))
            pe.putString(c.getString(R.string.settings_notification_message_when_sync_ended),NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS);
        if (!prefs.contains(c.getString(R.string.settings_vibrate_when_sync_ended)))
            pe.putString(c.getString(R.string.settings_vibrate_when_sync_ended),NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS);
        if (!prefs.contains(c.getString(R.string.settings_playback_ringtone_when_sync_ended)))
            pe.putString(c.getString(R.string.settings_playback_ringtone_when_sync_ended),NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS);
        if (!prefs.contains(c.getString(R.string.settings_playback_ringtone_volume)))
            pe.putInt(c.getString(R.string.settings_playback_ringtone_volume), 100);

        if (!prefs.contains(c.getString(R.string.settings_sync_history_log)))
            pe.putBoolean(c.getString(R.string.settings_sync_history_log), true);

        if (!prefs.contains(SUPRESS_GRANT_LOCATION_PERMISSION_KEY))
            pe.putBoolean(SUPRESS_GRANT_LOCATION_PERMISSION_KEY, false);

        if (!prefs.contains(c.getString(R.string.settings_security_application_password)))
            pe.putBoolean(c.getString(R.string.settings_security_application_password), false);

        if (!prefs.contains(c.getString(R.string.settings_security_application_password_use_app_startup)))
            pe.putBoolean(c.getString(R.string.settings_security_application_password_use_app_startup), false);

        if (!prefs.contains(c.getString(R.string.settings_security_application_password_use_edit_task)))
            pe.putBoolean(c.getString(R.string.settings_security_application_password_use_edit_task), false);

        if (!prefs.contains(c.getString(R.string.settings_security_application_password_use_export_task)))
            pe.putBoolean(c.getString(R.string.settings_security_application_password_use_export_task), false);

        if (!prefs.contains(c.getString(R.string.settings_security_init_smb_account_password)))
            pe.putBoolean(c.getString(R.string.settings_security_init_smb_account_password), false);

        if (!prefs.contains(c.getString(R.string.settings_security_init_zip_passowrd)))
            pe.putBoolean(c.getString(R.string.settings_security_init_zip_passowrd), false);

        if (!prefs.contains(c.getString(R.string.settings_security_hide_show_passowrd)))
            pe.putBoolean(c.getString(R.string.settings_security_hide_show_passowrd), false);

        if (!prefs.contains(c.getString(R.string.settings_wifi_lock)))
            pe.putBoolean(c.getString(R.string.settings_wifi_lock), true);

        if (!prefs.contains(c.getString(R.string.settings_force_screen_on_while_sync)))
            pe.putBoolean(c.getString(R.string.settings_force_screen_on_while_sync), true);

        if (!prefs.contains(c.getString(R.string.settings_screen_theme_language)))
            pe.putString(c.getString(R.string.settings_screen_theme_language), APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT);

        if (!prefs.contains(c.getString(R.string.settings_display_font_scale_factor)))
            pe.putString(c.getString(R.string.settings_display_font_scale_factor), FONT_SCALE_FACTOR_NORMAL);

        pe.commit();

    }

    public void loadSettingsParms(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        settingExitClean=prefs.getBoolean(c.getString(R.string.settings_exit_clean), false);

//        settingErrorOption = prefs.getBoolean(c.getString(R.string.settings_error_option), false);
        settingWifiLockRequired = prefs.getBoolean(c.getString(R.string.settings_wifi_lock), true);

        if (prefs.getString(c.getString(R.string.settings_no_compress_file_type), "").equals("")) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(c.getString(R.string.settings_no_compress_file_type), DEFAULT_NOCOMPRESS_FILE_TYPE);
            ed.commit();
        }
        settingNoCompressFileType = prefs.getString(c.getString(R.string.settings_no_compress_file_type), DEFAULT_NOCOMPRESS_FILE_TYPE);

        settingNotificationMessageWhenSyncEnded = prefs.getString(c.getString(R.string.settings_notification_message_when_sync_ended), "1");
        settingNotificationSoundWhenSyncEnded = prefs.getString(c.getString(R.string.settings_playback_ringtone_when_sync_ended), "0");
        settingNotificationVibrateWhenSyncEnded = prefs.getString(c.getString(R.string.settings_vibrate_when_sync_ended), "0");
        settingExportedTaskEncryptRequired = prefs.getBoolean(c.getString(R.string.settings_exported_profile_encryption), true);

        settingScreenTheme =prefs.getString(c.getString(R.string.settings_screen_theme), SCREEN_THEME_STANDARD);
        if (prefs.contains("settings_use_light_theme")) {
            boolean themeIsLight = prefs.getBoolean("settings_use_light_theme", false);
            if (themeIsLight) {
                prefs.edit().remove("settings_use_light_theme").commit();
                settingScreenTheme= SCREEN_THEME_LIGHT;
                prefs.edit().putString(c.getString(R.string.settings_screen_theme), SCREEN_THEME_LIGHT).commit();
            }
        }
        if (settingScreenTheme.equals(SCREEN_THEME_LIGHT)) applicationTheme = R.style.MainLight;
        else if (settingScreenTheme.equals(SCREEN_THEME_BLACK)) applicationTheme = R.style.MainBlack;
        else applicationTheme = R.style.Main;

//        loadLanguagePreference(c);
        setDisplayFontScale(c);

        settingForceDeviceTabletViewInLandscape = prefs.getBoolean(c.getString(R.string.settings_device_orientation_landscape_tablet), false);

        settingFixDeviceOrientationToPortrait = prefs.getBoolean(c.getString(R.string.settings_device_orientation_portrait), false);

        settingNotificationVolume = prefs.getInt(c.getString(R.string.settings_playback_ringtone_volume), 100);

        settingWriteSyncResultLog = prefs.getBoolean(c.getString(R.string.settings_sync_history_log), true);

        settingPreventSyncStartDelay =prefs.getBoolean(c.getString(R.string.settings_force_screen_on_while_sync), true);

        settingSyncMessageUseStandardTextView =prefs.getBoolean(c.getString(R.string.settings_sync_message_use_standard_text_view), false);

        settingSecurityApplicationPasswordHashValue = ApplicationPassword.getPasswordHashValue(prefs);
        settingSecurityApplicationPasswordUseAppStartup = prefs.getBoolean(c.getString(R.string.settings_security_application_password_use_app_startup), false);
        settingSecurityApplicationPasswordUseEditTask = prefs.getBoolean(c.getString(R.string.settings_security_application_password_use_edit_task), false);
        settingSecurityApplicationPasswordUseExport = prefs.getBoolean(c.getString(R.string.settings_security_application_password_use_export_task), false);
        settingSecurityReinitSmbAccountPasswordValue = prefs.getBoolean(c.getString(R.string.settings_security_init_smb_account_password), false);

        settingSecurityReinitZipPasswordValue = prefs.getBoolean(c.getString(R.string.settings_security_init_zip_passowrd), false);
        settingSecurityHideShowPasswordButton = prefs.getBoolean(c.getString(R.string.settings_security_hide_show_passowrd), false);

        settingScheduleSyncEnabled=prefs.getBoolean(SCHEDULE_ENABLED_KEY, true);

        settingSupressAddExternalStorageNotification=
            prefs.getBoolean(c.getString(R.string.settings_suppress_add_external_storage_notification), false);
        settingSupressStartSyncConfirmationMessage=
                prefs.getBoolean(c.getString(R.string.settings_suppress_start_sync_confirmation_message), false);
        settingSupressStartGroupConfirmationMessage=
                prefs.getBoolean(c.getString(R.string.settings_suppress_start_group_confirmation_message), false);
        settingSupressShortcut1ConfirmationMessage =
                prefs.getBoolean(c.getString(R.string.settings_suppress_shortcut1_confirmation_message), false);
        settingSupressShortcut2ConfirmationMessage =
                prefs.getBoolean(c.getString(R.string.settings_suppress_shortcut2_confirmation_message), false);
        settingSupressShortcut3ConfirmationMessage =
                prefs.getBoolean(c.getString(R.string.settings_suppress_shortcut3_confirmation_message), false);
    }

    public void setScheduleEnabled(Context c, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        settingScheduleSyncEnabled=enabled;
        prefs.edit().putBoolean(SCHEDULE_ENABLED_KEY, enabled).commit();
    }

    public static final String FONT_SCALE_FACTOR_SMALL="0";
    public static final float FONT_SCALE_FACTOR_SMALL_VALUE=0.8f;
    public static final String FONT_SCALE_FACTOR_NORMAL="1";
    public static final float FONT_SCALE_FACTOR_NORMAL_VALUE=1.0f;
    public static final String FONT_SCALE_FACTOR_LARGE="2";
    public static final float FONT_SCALE_FACTOR_LARGE_VALUE=1.2f;
    public static final String FONT_SCALE_FACTOR_LARGEST="3";
    public static final float FONT_SCALE_FACTOR_LARGEST_VALUE=1.6f;
    static public String getFontScaleFactor(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String fs=prefs.getString(c.getString(R.string.settings_display_font_scale_factor), FONT_SCALE_FACTOR_NORMAL);
        return fs;
    }

    static public float getFontScaleFactorValue(Context c) {
        String fs=getFontScaleFactor(c);
        float fs_value=getFontScaleFactorValue(c, fs);
        return fs_value;
    }

    static public float getFontScaleFactorValue(Context c, String fs) {
        float fs_value=1.0f;
        if (fs.equals(GlobalParameters.FONT_SCALE_FACTOR_SMALL)) {
            fs_value=FONT_SCALE_FACTOR_SMALL_VALUE;
        } else if (fs.equals(GlobalParameters.FONT_SCALE_FACTOR_NORMAL)) {
            fs_value=FONT_SCALE_FACTOR_NORMAL_VALUE;
        } else if (fs.equals(GlobalParameters.FONT_SCALE_FACTOR_LARGE)) {
            fs_value=FONT_SCALE_FACTOR_LARGE_VALUE;
        } else if (fs.equals(GlobalParameters.FONT_SCALE_FACTOR_LARGEST)) {
            fs_value=FONT_SCALE_FACTOR_LARGEST_VALUE;
        }
        return fs_value;
    }

    static public void setDisplayFontScale(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String fs=prefs.getString(c.getString(R.string.settings_display_font_scale_factor), FONT_SCALE_FACTOR_NORMAL);
        setDisplayFontScale(c, fs);
    }

    static public void setDisplayFontScale(Context c, String fs) {
        float fs_value=getFontScaleFactorValue(c, fs);
        setDisplayFontScale(c, fs_value);
    }

    static public void setDisplayFontScale(Context c, float scale) {
        Configuration configuration=c.getResources().getConfiguration();
        configuration.fontScale = scale;
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) c.getSystemService(WINDOW_SERVICE);

        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        c.getResources().updateConfiguration(configuration, metrics);

    }

    static public Context setNewLocale(Context c) {
        String lc= getLanguageCode(c);
        return updateLanguageResources(c, lc);
    }

    public static final String APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT = "system";

    static public String getLanguageCode(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String lc=prefs.getString(c.getString(R.string.settings_screen_theme_language), APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT);
        return lc;
    }

    // wrap language layout in the base context for all activities
    static private Context updateLanguageResources(Context context, String language) {
        //if language is set to system default (defined as "0"), do not apply non existing language code "0" and return current context without wrapped language
        if (!language.equals(APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT)) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Resources res = context.getResources();
            android.content.res.Configuration config = new android.content.res.Configuration(res.getConfiguration());

            setLocaleForApi24(config, locale);
            context = context.createConfigurationContext(config);
        }
        return context;
    }

    static private void setLocaleForApi24(android.content.res.Configuration config, Locale target) {
        Set<Locale> set = new LinkedHashSet<>();
        // bring the target locale to the front of the list
        set.add(target);

        LocaleList all = LocaleList.getDefault();
        for (int i = 0; i < all.size(); i++) {
            // append other locales supported by the user
            set.add(all.get(i));
        }

        Locale[] locales = set.toArray(new Locale[0]);
        config.setLocales(new LocaleList(locales));
    }

//    static public void loadLanguagePreference(Context c) {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
//        settingApplicationLanguage = prefs.getString(c.getString(R.string.settings_screen_theme_language), APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT);
//    }

    public boolean isScreenThemeIsLight() {
        boolean result=false;
        if (settingScreenTheme.equals(SCREEN_THEME_LIGHT)) result = true;
        return result;
    }

    final static public String SMB_LM_COMPATIBILITY_DEFAULT="3";
    final static public String SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT="30000";
    final static public String SMB_USE_EXTENDED_SECURITY_DEFAULT="true";
    final static public String SMB_USE_DISABLE_PLAIN_TEXT_PASSWORD_DEFAULT="false";

    public String settingsSmbLmCompatibility = SMB_LM_COMPATIBILITY_DEFAULT,
            settingsSmbUseExtendedSecurity = SMB_USE_EXTENDED_SECURITY_DEFAULT,
            settingsSmbClientResponseTimeout = SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT,
            settingsSmbDisablePlainTextPasswords=SMB_USE_DISABLE_PLAIN_TEXT_PASSWORD_DEFAULT;

    final public void initJcifsOption(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        settingsSmbLmCompatibility = prefs.getString(c.getString(R.string.settings_smb_lm_compatibility), SMB_LM_COMPATIBILITY_DEFAULT);
        boolean ues = prefs.getBoolean(c.getString(R.string.settings_smb_use_extended_security), true);
        boolean dpp=prefs.getBoolean(c.getString(R.string.settings_smb_disable_plain_text_passwords),false);
        settingsSmbClientResponseTimeout = prefs.getString(c.getString(R.string.settings_smb_client_response_timeout), SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT);

        if (settingsSmbLmCompatibility.equals("3") || settingsSmbLmCompatibility.equals("4")) {
            if (!ues) {
                ues = true;
//                prefs.edit().putBoolean(c.getString(R.string.settings_smb_use_extended_security), true).commit();
            }
        }

        settingsSmbUseExtendedSecurity = ues ? "true" : "false";
        settingsSmbDisablePlainTextPasswords=dpp ? "true" : "false";

        System.setProperty(JCIFS_OPTION_CLIENT_ATTR_EXPIRATION_PERIOD, "0");
        System.setProperty(JCIFS_OPTION_NETBIOS_RETRY_TIMEOUT, "3000");
//        System.setProperty("jcifs.smb.client.listSize", "1000");
        System.setProperty(JCIFS_OPTION_SMB_LM_COMPATIBILITY, settingsSmbLmCompatibility);
        System.setProperty(JCIFS_OPTION_CLIENT_USE_EXTENDED_SECUEITY, settingsSmbUseExtendedSecurity);
        System.setProperty(JCIFS_OPTION_CLIENT_RESPONSE_TIMEOUT, settingsSmbClientResponseTimeout);
        System.setProperty(JCIFS_OPTION_CLIENT_DISABLE_PLAIN_TEXT_PASSWORDS,settingsSmbDisablePlainTextPasswords);

//        System.setProperty("jcifs.smb.client.snd_buf_size","61440");
//        System.setProperty("jcifs.smb.client.tcpNoDelay","true");
//        System.setProperty("jcifs.smb.maxBuffers","100");
    }

    private boolean isDebuggable(Context c) {
        boolean result = false;
        PackageManager manager = c.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(c.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            result = false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            result = true;
//        Log.v("","debuggable="+result);
        return result;
    }

    public PowerManager.WakeLock mDimWakeLock = null;
    public PowerManager.WakeLock forceDimScreenWakelock = null;

    public PowerManager.WakeLock mPartialWakeLock = null;
    public WifiManager.WifiLock mWifiLock = null;

    public void releaseWakeLock(CommonUtilities util) {
        if (forceDimScreenWakelock.isHeld()) {
            forceDimScreenWakelock.release();
            util.addDebugMsg(1, "I", "ForceDim wakelock released");
        }

        if (mDimWakeLock.isHeld()) {
            mDimWakeLock.release();
            util.addDebugMsg(1, "I", "Dim wakelock released");
        }
        if (mPartialWakeLock.isHeld()) {
            mPartialWakeLock.release();
            util.addDebugMsg(1, "I", "Partial wakelock released");
        }
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
            util.addDebugMsg(1, "I", "Wifilock released");
        }
    }

    public void acquireWakeLock(Context c, CommonUtilities util) {
        if (settingWifiLockRequired) {
            if (!mWifiLock.isHeld()) {
                mWifiLock.acquire();
                util.addDebugMsg(1, "I", "Wifilock acquired");
            }
        }
        isScreenOn(c,util);
        if ((settingPreventSyncStartDelay)) {// && isScreenOn(c, util))) {// && !activityIsBackground) {
            if (!mDimWakeLock.isHeld()) {
                mDimWakeLock.acquire();
                util.addDebugMsg(1, "I", "Dim wakelock acquired");
            }
        } else {
            if (!mPartialWakeLock.isHeld()) {
                mPartialWakeLock.acquire();
                util.addDebugMsg(1, "I", "Partial wakelock acquired");
            }
        }
    }

    @SuppressLint("NewApi")
    static public boolean isScreenOn(Context context, CommonUtilities util) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        util.addDebugMsg(1, "I", "isDeviceIdleMode()=" + pm.isDeviceIdleMode() +
                ", isPowerSaveMode()=" + pm.isPowerSaveMode() + ", isInteractive()=" + pm.isInteractive());
        return pm.isInteractive();
    }

    class Slf4jLogWriter extends LoggerWriter {
        private LogUtil mLu =null;
        public Slf4jLogWriter(LogUtil lu) {
            mLu =lu;
        }
        @Override
        public void write(String msg) {
            mLu.addDebugMsg(1,"I", msg);
        }
    }

}
