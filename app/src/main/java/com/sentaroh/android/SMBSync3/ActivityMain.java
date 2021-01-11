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

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.sentaroh.android.SMBSync3.Log.LogManagementFragment;
import com.sentaroh.android.SMBSync3.Log.LogUtil;
import com.sentaroh.android.Utilities3.AppUncaughtExceptionHandler;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.Dialog.ProgressBarDialogFragment;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.SafStorage3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.SystemInfo;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.CustomTabLayout;
import com.sentaroh.android.Utilities3.Widget.CustomViewPager;
import com.sentaroh.android.Utilities3.Widget.CustomViewPagerAdapter;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;
import com.sentaroh.android.Utilities3.Zip.ZipUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.crypto.SecretKey;

import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.GlobalParameters.APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.*;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public class ActivityMain extends AppCompatActivity {

    final private static Logger log= LoggerFactory.getLogger(ActivityMain.class);

    private CustomTabLayout mMainTabLayout = null;
    private Context mContext = null;
    private ActivityMain mActivity = null;

    private GlobalParameters mGp = null;
    private TaskListUtils mTaskUtil = null;

    private CommonUtilities mUtil = null;
//    private CustomContextMenu ccMenu = null;

    private final static int START_INITIALYZING = 0;
    private final static int START_COMPLETED = 1;
    private final static int START_INPROGRESS = 2;
    private int appStartStaus = START_INITIALYZING;

    private boolean appStartWithRestored =false;

    private ServiceConnection mSvcConnection = null;
    private Handler mUiHandler = new Handler();

    private ActionBar mActionBar = null;

    private String mCurrentTab = null;

    private boolean enableMainUi = true;

    private String mTabNameTask="Task", mTabNameSchedule="Schedule", mTabNameHistory="History", mTabNameMessage="Message", mTabNameGroup="Group";

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        out.putString("currentTab", mCurrentTab);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        mCurrentTab = in.getString("currentTab");

        appStartWithRestored =true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(GlobalParameters.setNewLocale(base));
        GlobalParameters.setDisplayFontScale(base);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//        Log.v(APPLICATION_TAG, "onCreate entered");
        mActivity = ActivityMain.this;
        mContext = mActivity;

//        Intent splash=new Intent(mActivity, ActivitySplash.class);
//        startActivity(splash);

        mGp= GlobalWorkArea.getGlobalParameter(mActivity);
        GlobalParameters.setDisplayFontScale(mActivity);
        setTheme(mGp.applicationTheme);
        super.onCreate(savedInstanceState);
        mUtil = new CommonUtilities(mContext, "Main", mGp, getSupportFragmentManager());

        setContentView(R.layout.main_screen);
        mGp.themeColorList = ThemeUtil.getThemeColorList(mActivity);

        MyUncaughtExceptionHandler myUncaughtExceptionHandler = new MyUncaughtExceptionHandler();
        myUncaughtExceptionHandler.init(mContext, myUncaughtExceptionHandler);

        mGp.syncMessageListAdapter = new MessageListAdapter(mActivity, R.layout.message_list_item, mGp.syncMessageList, mGp);
        if (mGp.syncHistoryList == null) mGp.syncHistoryList = mUtil.loadHistoryList();
        mGp.syncHistoryListAdapter = new HistoryListAdapter(mActivity, R.layout.history_list_item, mGp.syncHistoryList);

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName(), " entered, appStartStaus="+appStartStaus);

        mTabNameTask=getString(R.string.msgs_tab_name_task);
        mTabNameSchedule=getString(R.string.msgs_tab_name_schedule);
        mTabNameHistory=getString(R.string.msgs_tab_name_history);
        mTabNameMessage=getString(R.string.msgs_tab_name_msg);
        mTabNameGroup=getString(R.string.msgs_tab_name_group);
        mCurrentTab = mTabNameTask;

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setHomeButtonEnabled(false);
        if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        mGp.syncTaskListAdapter = new TaskListAdapter(mActivity, R.layout.sync_task_item_view, mGp.syncTaskList, mGp);

        Thread th=new Thread(){
            @Override
            public void run() {
//                makeCacheDirectory();
                if (mGp.syncTaskList.size()==0) {
                    mUtil.addDebugMsg(1, "I", "Configuration load started");
                    mGp.loadConfigList(mContext);
                    mUtil.addDebugMsg(1, "I", "Configuration load ended");
                }
                mTaskUtil = new TaskListUtils(mUtil, mActivity, mGp, getSupportFragmentManager());
            }
        };
        th.setName("ActivityLoadConfig");
        th.start();

        createTabView();

        listSettingsOption();

        addAppShortCut();

    }

    private class MyUncaughtExceptionHandler extends AppUncaughtExceptionHandler {
        @Override
        public void appUniqueProcess(Throwable ex, String strace) {
            log.error("UncaughtException detected, error="+ex);
            log.error(strace);
            mUtil.flushLog();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, appStartStaus=" + appStartStaus);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, appStartStaus=" + appStartStaus);
        if (appStartStaus == START_INITIALYZING) {
            appStartStaus = START_INPROGRESS;
            if (Build.VERSION.SDK_INT>=30) {
                //ENable "ALL_FILE_ACCESS"
                if (!isAllFileAccessPermissionGranted()) {
                    NotifyEvent ntfy_all_file_access=new NotifyEvent(mContext);
                    ntfy_all_file_access.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            initApplication();
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    requestAllFileAccessPermission(ntfy_all_file_access);
                } else {
                    initApplication();
                }
            } else {
                if (isLegacyStorageAccessGranted()) initApplication();
                else {
                    checkPrivacyPolicyAgreement(new CallBackListener() {
                        @Override
                        public void onCallBack(Context context, boolean positive, Object[] objects) {
                            if (positive) {
                                requestLegacyStoragePermission(new CallBackListener(){
                                    @Override
                                    public void onCallBack(Context c, boolean positive, Object[] o) {
                                        initApplication();
                                    }
                                });
                            } else {
                                mUtil.showCommonDialogWarn(mContext, false,
                                    "プライバシーポリシーへの同意",
                                    "プライバシーポリシーに同意していただけないとアプリは起動できません",
                                    new CallBackListener(){
                                    @Override
                                    public void onCallBack(Context context, boolean positive, Object[] objects) {
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        } else if (appStartStaus == START_COMPLETED) {
            mGp.safMgr.refreshSafList();
            setActivityForeground(true);
            ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
            mGp.progressSpinSynctask.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
        }
    }

    private void initApplication() {
        openService(new CallBackListener(){
            @Override
            public void onCallBack(Context context, boolean positive, Object[] objects) {
                //Service起動終了
                ScheduleUtils.sendTimerRequest(mContext, SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET);
                checkStoredKey(new CallBackListener(){
                    @Override
                    public void onCallBack(Context context, boolean positive, Object[] objects) {
                        setActivityForeground(true);
                        ApplicationPassword.authentication(mGp, mActivity, getSupportFragmentManager(), mUtil, false, ApplicationPassword.APPLICATION_PASSWORD_RESOURCE_START_APPLICATION,
                            new CallBackListener() {
                            @Override
                            public void onCallBack(Context context, boolean positive, Object[] objects) {
                                if (positive) {
                                    mGp.waitConfigurationLock();
                                    appStartStaus = START_COMPLETED;

                                    setMessageContextButtonListener();
                                    setMessageViewListener();
                                    setMessageFilterListener();
                                    setMessageContextButtonNormalMode();

                                    setSyncTaskContextButtonHide();
                                    setSyncTaskContextButtonListener();
                                    setSyncTaskViewListener();
                                    setSyncTaskContextButtonNormalMode();
                                    mGp.syncTaskListAdapter.notifyDataSetChanged();

                                    setHistoryContextButtonListener();
                                    setHistoryViewListener();
                                    setHistoryContextButtonNormalMode();

                                    setScheduleContextButtonListener();
                                    setScheduleViewListener();
                                    setScheduleContextButtonNormalMode();

                                    setGroupContextButtonListener();
                                    setGroupListViewListener();
                                    setGroupContextButtonMode(mGp.syncGroupListAdapter);

                                    ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
                                    setScheduleTabMessage();
                                    setGroupTabMessage();

                                    reshowDialogWindow();

                                    showAddExternalStorageNotification();
                                    mMainScreenView.setVisibility(LinearLayout.VISIBLE);
                                    if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                                    else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
                                    mGp.syncMessageView.setSelection(mGp.syncMessageList.size()-1);
                                    if (mGp.syncThreadActive) {
                                        mMainTabLayout.setCurrentTabByName(mTabNameMessage);
                                    }
                                    if (!appStartWithRestored) {
                                        if (mGp.syncThreadActive) mMainTabLayout.setCurrentTabByName(mTabNameMessage);
                                    } else {
                                        if (mGp.activityIsFinished) {
                                            mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_smbsync_main_restart_by_killed));
                                        } else {
                                            mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_smbsync_main_restart_by_destroyed));
                                        }
                                        mMainTabLayout.setCurrentTabByName(mTabNameMessage);
                                    }
                                } else {
                                    //Application password確認でCancelが押下された
                                    finish();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void checkPrivacyPolicyAgreement(CallBackListener cbl) {
        cbl.onCallBack(mContext, true, null);

//        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.privacy_policy_agreement);
//
//        final LinearLayout ll_title=(LinearLayout) dialog.findViewById(R.id.privacy_policy_agreement_dlg_title_view);
//        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
//        final TextView tv_title=(TextView)dialog.findViewById(R.id.agree_privacy_policy_dlg_title);
//        tv_title.setTextColor(mGp.themeColorList.title_text_color);
//        final TextView tv_msg=(TextView)dialog.findViewById(R.id.privacy_policy_agreement_dlg_msg);
//        tv_msg.setVisibility(TextView.GONE);
//        final Button btn_ok=(Button)dialog.findViewById(R.id.privacy_policy_agreement_dlg_btn_ok);
//        final Button btn_cancel=(Button)dialog.findViewById(R.id.privacy_policy_agreement_dlg_btn_cancel);
//
//        final WebView web_view=(WebView)dialog.findViewById(R.id.agree_privacy_policy_dlg_webview);
//        final CheckedTextView ct_agree=(CheckedTextView) dialog.findViewById(R.id.privacy_policy_agreement_dlg_agree);
//
//        web_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_privacy_desc));
//        web_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//
//        ct_agree.setChecked(false);
//        ct_agree.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                boolean checked=!ct_agree.isChecked();
//                ct_agree.setChecked(checked);
//                if (checked) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
//                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
//            }
//        });
//
//        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
//        btn_ok.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cbl.onCallBack(mContext, true, null);
//                dialog.dismiss();
//            }
//        });
//
//        btn_cancel.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cbl.onCallBack(mContext, false, null);
//                dialog.dismiss();
//            }
//        });
//
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                cbl.onCallBack(mContext, false, null);
//                dialog.dismiss();
//            }
//        });
//
//        dialog.show();

    }

    private void checkStoredKey(CallBackListener cbl) {
        mUtil.addDebugMsg(1, "I", "checkStoredKey entered");
        String result="key not changed";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String key_value=prefs.getString(STORED_SECRET_KEY_VALIDATION_KEY, "");
        if (!key_value.equals("")) {
            try {
                SecretKey enc_key= KeyStoreUtils.getStoredKey(mActivity.getApplicationContext(), KeyStoreUtils.KEY_STORE_ALIAS);
                EncryptUtilV3.CipherParms cp_int = EncryptUtilV3.initCipherEnv(enc_key, KeyStoreUtils.KEY_STORE_ALIAS);
                String dec_str=CommonUtilities.decryptUserData(mContext, cp_int, key_value);
                if (dec_str==null) {
                    //Stored key was changed
                    if (!ApplicationPassword.getPasswordHashValue(prefs).equals("")) {
                        mUtil.showCommonDialogError(false,
                                mContext.getString(R.string.msgs_security_stored_sectret_key_change_title),
                                mContext.getString(R.string.msgs_security_stored_sectret_key_change_message),
                                null);
                        result="key chnaged";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mUtil.showCommonDialogError(false,
                        mContext.getString(R.string.msgs_security_stored_sectret_key_change_title),
                        mContext.getString(R.string.msgs_security_stored_sectret_key_change_error)+", "+e.toString(), null);
                result="key error";
            }
            mUtil.addDebugMsg(1, "I", "checkStoredKey exit, result=\""+result+"\"");
            cbl.onCallBack(mContext, true, null);
        } else {
            try {
                SecretKey enc_key= KeyStoreUtils.getStoredKey(mActivity.getApplicationContext(), KeyStoreUtils.KEY_STORE_ALIAS);
                EncryptUtilV3.CipherParms cp_int = EncryptUtilV3.initCipherEnv(enc_key, KeyStoreUtils.KEY_STORE_ALIAS);
                String enc_str=CommonUtilities.encryptUserData(mContext, cp_int, "enc_data");
                prefs.edit().putString(STORED_SECRET_KEY_VALIDATION_KEY, enc_str).commit();
                result="key saved";
            } catch (Exception e) {
                e.printStackTrace();
                mUtil.showCommonDialogError(false,
                        mContext.getString(R.string.msgs_security_stored_sectret_key_change_title),
                        mContext.getString(R.string.msgs_security_stored_sectret_key_save_error)+", "+e.toString(), null);
                result="key not save";
            }
            mUtil.addDebugMsg(1, "I", "checkStoredKey exit, result=\""+result+"\"");
            cbl.onCallBack(mContext, true, null);
        }
    }

    private void addAppShortCut() {
        ShortcutManager manager = (ShortcutManager) mContext.getSystemService(SHORTCUT_SERVICE);
        ArrayList<ShortcutInfo> sl=new ArrayList<ShortcutInfo>();
        manager.removeAllDynamicShortcuts();
        sl.add(createShortcutInfo(mActivity, APP_SHORTCUT_ID_VALUE_BUTTON1,
                mContext.getString(R.string.msgs_main_shortcut_button_label_button1), R.drawable.sync_button_1));
        sl.add(createShortcutInfo(mActivity, APP_SHORTCUT_ID_VALUE_BUTTON2,
                mContext.getString(R.string.msgs_main_shortcut_button_label_button2), R.drawable.sync_button_2));
        sl.add(createShortcutInfo(mActivity, APP_SHORTCUT_ID_VALUE_BUTTON3,
                mContext.getString(R.string.msgs_main_shortcut_button_label_button3), R.drawable.sync_button_3));
        manager.addDynamicShortcuts(sl);
    }

    private ShortcutInfo createShortcutInfo(Activity a, int shortcut_key_value, String label, int icon) {
        Intent intent = new Intent(Intent.ACTION_VIEW, null, a, ActivityShortcut.class);
        intent.putExtra(APP_SHORTCUT_ID_KEY, shortcut_key_value);

        ShortcutInfo info = new ShortcutInfo.Builder(mContext, "sync"+shortcut_key_value)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(mContext, icon))
                .setIntent(intent)
                .build();
        return info;
    }

    private void showAddExternalStorageNotification() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+
                " entered, isStoragePermissionRequired="+mGp.safMgr.isStoragePermissionRequired()+
                ", isSupressAddExternalStorageNotification="+mGp.isSupressAddExternalStorageNotification());
        if (Build.VERSION.SDK_INT>=30) return;
        if (mGp.safMgr.isStoragePermissionRequired() && !mGp.isSupressAddExternalStorageNotification()) {
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    boolean suppress=(boolean)objects[0];
                    if (suppress) {
                        mGp.setSupressAddExternalStorageNotification(mContext, true);
                    }
                    NotifyEvent ntfy_add=new NotifyEvent(mContext);
                    ntfy_add.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            mGp.syncTaskListAdapter.notifyDataSetChanged();
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    TaskEditor.requestLocalStoragePermission(mActivity, mGp, mUtil, ntfy_add);
                }

                @Override
                public void negativeResponse(Context context, Object[] objects) {
                    boolean suppress=(boolean)objects[0];
                    if (suppress) {
                        mGp.setSupressAddExternalStorageNotification(mContext, true);
                    }
                }
            });
            ArrayList<SafManager3.StorageVolumeInfo>svl=SafManager3.buildStoragePermissionRequiredList(mContext);
            String new_storage="";
            for(SafManager3.StorageVolumeInfo si:svl) {
                new_storage+=si.description+"("+si.uuid+")"+"\n";
            }
            TaskEditor.showDialogWithHideOption(mActivity, mGp, mUtil,
                    true, mContext.getString(R.string.msgs_common_dialog_ok),
                    true, mContext.getString(R.string.msgs_common_dialog_close),
                    mContext.getString(R.string.msgs_main_suppress_add_external_storage_notification_title),
                    mContext.getString(R.string.msgs_main_suppress_add_external_storage_notification_msg)+"\n-"+new_storage,
                    mContext.getString(R.string.msgs_main_suppress_add_external_storage_notification_suppress), ntfy);
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, appStartStaus=" + appStartStaus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, currentView=" + mCurrentTab +
                ", getChangingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
        setActivityForeground(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, isFinishing=" + isFinishing() +
                ", changingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
//        setActivityForeground(false);
        unsetCallbackListener();

        if (isFinishing()) {
            mGp.logCatActive=false;
        }
        mGp.appPasswordAuthValidated=false;
        mGp.activityIsFinished = isFinishing();
        closeService();
        LogUtil.flushLog(mContext);

        cleanupCacheFile();
    }

    @Override
    public void onConfigurationChanged(final android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mUtil != null) {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() , " Entered, New orientation=" + newConfig.orientation, ", New language=", newConfig.locale.getLanguage());
        }
        reloadScreen(false);
    }

    private void setActivityForeground(boolean fore_ground) {
        if (mSvcClient != null) {
            try {
                if (fore_ground) mSvcClient.aidlSetActivityInForeground();
                else mSvcClient.aidlSetActivityInBackground();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSystemInfo() {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.common_dialog);

        final LinearLayout ll_title=(LinearLayout) dialog.findViewById(R.id.common_dialog_title_view);
        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView tv_title=(TextView)dialog.findViewById(R.id.common_dialog_title);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        final TextView tv_msg_old=(TextView)dialog.findViewById(R.id.common_dialog_msg);
        tv_msg_old.setVisibility(TextView.GONE);
        final NonWordwrapTextView tv_msg=(NonWordwrapTextView)dialog.findViewById(R.id.common_dialog_custom_text_view);
        tv_msg.setVisibility(TextView.VISIBLE);
        tv_msg.setWordWrapEnabled(mGp.settingSyncMessageUseStandardTextView);
//        if (Build.VERSION.SDK_INT>=23) tv_msg.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
        final Button btn_copy=(Button)dialog.findViewById(R.id.common_dialog_btn_ok);
        final Button btn_close=(Button)dialog.findViewById(R.id.common_dialog_btn_cancel);
        final Button btn_send=(Button)dialog.findViewById(R.id.common_dialog_extra_button);
        btn_send.setText(mContext.getString(R.string.msgs_info_storage_send_btn_title));
        btn_send.setVisibility(Button.VISIBLE);

        tv_title.setText(mContext.getString(R.string.msgs_menu_list_storage_info));
        btn_close.setText(mContext.getString(R.string.msgs_common_dialog_close));
        btn_copy.setText(mContext.getString(R.string.msgs_info_storage_copy_clipboard));

        ArrayList<String> sil= CommonUtilities.listSystemInfo(mContext, mGp);
        String si_text="";
        for(String si_item:sil) si_text+=si_item+"\n";

        tv_msg.setText(si_text);

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        btn_copy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                android.content.ClipboardManager cm=(android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("SMBSync3 System Info", tv_msg.getOriginalText().toString()));
                CommonDialog.showPopupMessageAsUpAnchorViewShort(mActivity, btn_copy, mContext.getString(R.string.msgs_info_storage_copy_completed));
            }
        });

        btn_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btn_send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        String desc=(String)objects[0];
                        Intent intent=new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
//                intent.setType("text/plain");
//                intent.setType("application/zip");

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"gm.developer.fhoshino@gmail.com"});
//                intent.putExtra(Intent.EXTRA_CC, new String[]{"cc@example.com"});
//                intent.putExtra(Intent.EXTRA_BCC, new String[]{"bcc@example.com"});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "SMBSync3 System Info");
                        intent.putExtra(Intent.EXTRA_TEXT, desc+ "\n\n\n"+tv_msg.getText().toString());
//                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                        try {
                            mContext.startActivity(intent);
                        } catch(Exception e) {
                            String st= MiscUtil.getStackTraceString(e);
                            mUtil.showCommonDialogError(false, "Send system info error", e.getMessage()+"\n"+st, null);
                        }
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                getProblemDescription(ntfy);
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_close.performClick();
            }
        });

        dialog.show();
    }

    private void getProblemDescription(final NotifyEvent p_ntfy) {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView tv_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        tv_title.setText(mContext.getString(R.string.msgs_your_problem_title));

        final TextView tv_msg=(TextView)dialog.findViewById(R.id.single_item_input_msg);
        tv_msg.setVisibility(TextView.GONE);
        final TextView tv_desc=(TextView)dialog.findViewById(R.id.single_item_input_name);
        tv_desc.setText(mContext.getString(R.string.msgs_your_problem_msg));
        final EditText et_msg=(EditText)dialog.findViewById(R.id.single_item_input_dir);
        et_msg.setHint(mContext.getString(R.string.msgs_your_problem_hint));
        et_msg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        final Button btn_ok=(Button)dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.single_item_input_cancel_btn);

//        btn_cancel.setText(mContext.getString(R.string.msgs_common_dialog_close));

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        btn_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy_desc=new NotifyEvent(mContext);
                ntfy_desc.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        p_ntfy.notifyToListener(true, new Object[]{et_msg.getText().toString()});
                        dialog.dismiss();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                if (et_msg.getText().length()<=10) {
                    mUtil.showCommonDialog(false, "W", mContext.getString(R.string.msgs_your_problem_no_desc), "", null);
                } else {
                    ntfy_desc.notifyToListener(true, null);
                }
            }
        });

        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_cancel.performClick();
            }
        });

        dialog.show();
    }

    private void showBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            if (pm.isIgnoringBatteryOptimizations(packageName)) {
//                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Invoke battery optimization settings");
//            } else {
//                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Request ignore battery optimization");
//            }
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
            mUtil.addDebugMsg(1, "I", "Invoke battery optimization settings");
        }
    }

    class ViewSaveArea {
        public int current_tab_pos = 0;
        public int current_pager_pos = 0;
        public int task_list_view_pos_x = 0, task_list_view_pos_y = 0;
        public boolean prof_adapter_show_cb = false;
        public int msg_list_view_pos_x = 0, msg_list_view_pos_y = 0;
        public int hist_list_view_pos_x = 0, hist_list_view_pos_y = 0;
        public boolean sync_adapter_show_cb = false;

        public int prog_bar_view_visibility = ProgressBar.GONE,
                prog_spin_view_visibility = ProgressBar.GONE, confirm_view_visibility = ProgressBar.GONE;

        public String prog_task_name = "", prog_msg = "";

        public ArrayList<HistoryListAdapter.HistoryListItem> sync_hist_list = null;

        public String confirm_msg = "";
        public String progress_bar_msg = "";
        public int progress_bar_progress = 0, progress_bar_max = 0;

        public ButtonViewContent confirm_cancel = new ButtonViewContent();
        public ButtonViewContent confirm_yes = new ButtonViewContent();
        public ButtonViewContent confirm_yes_all = new ButtonViewContent();
        public ButtonViewContent confirm_no = new ButtonViewContent();
        public ButtonViewContent confirm_no_all = new ButtonViewContent();
        public ButtonViewContent prog_bar_cancel = new ButtonViewContent();
        public ButtonViewContent prog_bar_immed = new ButtonViewContent();
        public ButtonViewContent prog_spin_cancel = new ButtonViewContent();
    }

    class ButtonViewContent {
        public String button_text = "";
        public boolean button_visible = true, button_enabled = true, button_clickable = true;
    }

    private void saveButtonStatus(Button btn, ButtonViewContent sv) {
        sv.button_text = btn.getText().toString();
        sv.button_clickable = btn.isClickable();
        sv.button_enabled = btn.isEnabled();
        sv.button_visible = btn.isShown();
    }

    private void restoreButtonStatus(Button btn, ButtonViewContent sv, OnClickListener ocl) {
        btn.setText(sv.button_text);
        btn.setClickable(sv.button_clickable);
        CommonDialog.setViewEnabled(mActivity, btn, sv.button_enabled);
        btn.setOnClickListener(ocl);
    }

    private void reloadScreen(boolean force_reload) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Entered");
        ViewSaveArea vsa = null;
        vsa = saveViewContent();

        mGp.syncTaskView.setAdapter(null);
        mGp.syncHistoryView.setAdapter(null);

        setContentView(R.layout.main_screen);
        mActionBar = getSupportActionBar();

        mGp.syncHistoryView.setAdapter(null);

        mGp.syncTaskView.setAdapter(null);
        ArrayList<SyncTaskItem> pfl = mGp.syncTaskListAdapter.getArrayList();

        mGp.syncMessageView.setAdapter(null);

        mGp.syncGroupView.setAdapter(null);

        ArrayList<MessageListAdapter.MessageListItem> mfl=new ArrayList<MessageListAdapter.MessageListItem>(GlobalParameters.MESSAGE_LIST_INITIAL_VALUE);
        if (mGp.syncMessageListAdapter !=null) mfl=mGp.syncMessageListAdapter.getMessageList();

        boolean sync_schedule_adapter_select_mode=mGp.syncScheduleListAdapter.isSelectMode();

        boolean sync_group_adapter_select_mode=mGp.syncGroupListAdapter.isSelectMode();

        createTabView();

        GlobalParameters.setDisplayFontScale(mActivity);

        mGp.syncTaskListAdapter = new TaskListAdapter(mActivity, R.layout.sync_task_item_view, pfl, mGp);
        mGp.syncTaskListAdapter.setShowCheckBox(vsa.prof_adapter_show_cb);
        mGp.syncTaskListAdapter.notifyDataSetChanged();
        if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
        else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
        mGp.syncMessageListAdapter = new MessageListAdapter(mActivity, R.layout.message_list_item, mfl, mGp);
        mGp.syncHistoryListAdapter = new HistoryListAdapter(mActivity, R.layout.history_list_item, vsa.sync_hist_list);
        mGp.syncHistoryListAdapter.setShowCheckBox(vsa.sync_adapter_show_cb);
        mGp.syncHistoryListAdapter.notifyDataSetChanged();
        mGp.syncScheduleListAdapter.setSelectMode(sync_schedule_adapter_select_mode);
        mGp.syncGroupListAdapter.setSelectMode(sync_group_adapter_select_mode);

        restoreViewContent(vsa);

        setMessageContextButtonListener();
        setMessageViewListener();
        setMessageFilterListener();
        setMessageContextButtonNormalMode();

        setSyncTaskContextButtonListener();
        setSyncTaskViewListener();

        setHistoryContextButtonListener();

        setHistoryViewListener();

        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
        setScheduleContextButtonListener();
        setScheduleViewListener();

        setGroupContextButtonListener();
        setGroupListViewListener();
        setGroupContextButtonMode(mGp.syncGroupListAdapter);

        if (mCurrentTab.equals(mTabNameTask)) {
            if (mGp.syncTaskListAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();
        } else if (mCurrentTab.equals(mTabNameHistory)) {
            if (mGp.syncHistoryListAdapter.isShowCheckBox()) setHistoryContextButtonSelectMode();
            else setHistoryContextButtonNormalMode();
        }

        mMainScreenView.setVisibility(LinearLayout.VISIBLE);

        if (isUiEnabled()) setUiEnabled();
        else setUiDisabled();
        vsa = null;
    }

//    private int newSyncTaskListViewPos = -1;

    private ViewSaveArea saveViewContent() {
        ViewSaveArea vsa = new ViewSaveArea();
        vsa.current_tab_pos = mMainTabLayout.getSelectedTabPosition();
        vsa.current_pager_pos = mMainViewPager.getCurrentItem();

        vsa.task_list_view_pos_x = mGp.syncTaskView.getFirstVisiblePosition();
        if (mGp.syncTaskView.getChildAt(0) != null) vsa.task_list_view_pos_y = mGp.syncTaskView.getChildAt(0).getTop();
        vsa.prof_adapter_show_cb = mGp.syncTaskListAdapter.isShowCheckBox();
        vsa.msg_list_view_pos_x = mGp.syncMessageView.getFirstVisiblePosition();
        if (mGp.syncMessageView.getChildAt(0) != null) vsa.msg_list_view_pos_y = mGp.syncMessageView.getChildAt(0).getTop();
        vsa.hist_list_view_pos_x = mGp.syncHistoryView.getFirstVisiblePosition();
        if (mGp.syncHistoryView.getChildAt(0) != null) vsa.hist_list_view_pos_y = mGp.syncHistoryView.getChildAt(0).getTop();
        vsa.sync_adapter_show_cb = mGp.syncHistoryListAdapter.isShowCheckBox();

        vsa.prog_task_name = mGp.progressSpinSynctask.getText().toString();
        vsa.prog_msg = mGp.progressSpinMsg.getText().toString();
        vsa.confirm_view_visibility = mGp.confirmView.getVisibility();
        vsa.prog_spin_view_visibility = mGp.progressSpinView.getVisibility();

        saveButtonStatus(mGp.confirmCancel, vsa.confirm_cancel);
        saveButtonStatus(mGp.confirmYes, vsa.confirm_yes);
        saveButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all);
        saveButtonStatus(mGp.confirmNo, vsa.confirm_no);
        saveButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all);
        saveButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel);

        vsa.confirm_msg = mGp.confirmMsg.getText().toString();

        vsa.sync_hist_list = mGp.syncHistoryListAdapter.getSyncHistoryList();

        return vsa;
    }

    private void restoreViewContent(ViewSaveArea vsa) {
        mWhileRestoreViewProcess=true;
        mMainTabLayout.setCurrentTabByPosition(vsa.current_tab_pos);
        mMainViewPager.setCurrentItem(vsa.current_pager_pos);
        mWhileRestoreViewProcess=false;
        mGp.syncTaskView.setSelectionFromTop(vsa.task_list_view_pos_x, vsa.task_list_view_pos_y);
        mGp.syncMessageView.setSelectionFromTop(vsa.msg_list_view_pos_x, vsa.msg_list_view_pos_y);
        mGp.syncHistoryView.setSelectionFromTop(vsa.hist_list_view_pos_x, vsa.hist_list_view_pos_y);

        mGp.confirmMsg.setText(vsa.confirm_msg);

        restoreButtonStatus(mGp.confirmCancel, vsa.confirm_cancel, mGp.confirmCancelListener);
        restoreButtonStatus(mGp.confirmYes, vsa.confirm_yes, mGp.confirmYesListener);
        restoreButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all, mGp.confirmYesAllListener);
        restoreButtonStatus(mGp.confirmNo, vsa.confirm_no, mGp.confirmNoListener);
        restoreButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all, mGp.confirmNoAllListener);
        restoreButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel, mGp.progressSpinCancelListener);

        mGp.progressSpinSynctask.setText(vsa.prog_task_name);
        mGp.progressSpinMsg.setText(vsa.prog_msg);
        mGp.scheduleInfoView.setText(mGp.scheduleInfoText);
        mGp.scheduleErrorView.setText(mGp.scheduleErrorText);
        if (mGp.scheduleErrorText.equals("")) mGp.scheduleErrorView.setVisibility(TextView.GONE);
        else mGp.scheduleErrorView.setVisibility(TextView.VISIBLE);

        if (vsa.confirm_view_visibility != LinearLayout.GONE) {
            mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
            mGp.confirmView.bringToFront();
        } else {
            mGp.confirmView.setVisibility(LinearLayout.GONE);
        }

        if (vsa.prog_spin_view_visibility != LinearLayout.GONE) {
            mGp.progressSpinView.bringToFront();
            mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        } else mGp.progressSpinView.setVisibility(LinearLayout.GONE);

    }

    private LinearLayout mMainScreenView;
    private LinearLayout mSyncTaskView;
    private LinearLayout mGroupView;
    private LinearLayout mScheduleView;
    private LinearLayout mHistoryView;
    private LinearLayout mMessageView;

    private CustomViewPager mMainViewPager;
//    private CustomViewPagerAdapter mMainViewPagerAdapter;
    private boolean mWhileRestoreViewProcess=false;
    private void createTabView() {
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mMainScreenView = (LinearLayout) findViewById(R.id.main_screen_view);
        mGroupView = (LinearLayout) vi.inflate(R.layout.main_sync_group, null);
        mSyncTaskView = (LinearLayout) vi.inflate(R.layout.main_sync_task, null);
        mScheduleView = (LinearLayout) vi.inflate(R.layout.main_schedule, null);
        mHistoryView = (LinearLayout) vi.inflate(R.layout.main_history, null);
        mMessageView = (LinearLayout) vi.inflate(R.layout.main_message, null);

        createContextView();

        mGp.syncMessageView = (ListView) mMessageView.findViewById(R.id.main_message_list_view);
        mGp.syncMessageView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mGp.syncTaskView = (ListView) mSyncTaskView.findViewById(R.id.main_sync_task_view_list);
        mGp.syncTaskEmptyMessage=(TextView)mSyncTaskView.findViewById(R.id.main_sync_task_view_empty_message);
        mGp.syncTaskEmptyMessage.setTextColor(mGp.themeColorList.text_color_warning);
        mGp.syncScheduleView = (ListView) mScheduleView.findViewById(R.id.main_schedule_list_view);
        mGp.syncHistoryView = (ListView) mHistoryView.findViewById(R.id.main_history_list_view);

        mGp.syncScheduleListAdapter = new ScheduleListAdapter(mActivity, R.layout.schedule_sync_list_item, mGp.syncScheduleList);
        mGp.syncScheduleView.setAdapter(mGp.syncScheduleListAdapter);
        mGp.syncScheduleMessage =(TextView)mScheduleView.findViewById(R.id.main_schedule_list_message);
        mGp.syncScheduleMessage.setTextColor(mGp.themeColorList.text_color_warning);
        setScheduleTabMessage();

        mGp.syncGroupView = (ListView) mGroupView.findViewById(R.id.main_sync_group_view_list);
        mGp.syncGroupListAdapter = new GroupListAdapter(mActivity, R.layout.group_list_item, mGp.syncGroupList);
        mGp.syncGroupView.setAdapter(mGp.syncGroupListAdapter);
        mGp.syncGroupMessage =(TextView) mGroupView.findViewById(R.id.main_sync_group_view_empty_message);
        mGp.syncGroupMessage.setTextColor(mGp.themeColorList.text_color_warning);
        setGroupTabMessage();

        mGp.scheduleInfoView = (TextView) findViewById(R.id.main_schedule_view_info);
        mGp.scheduleErrorView = (TextView) findViewById(R.id.main_schedule_view_error);
        mGp.scheduleErrorView.setText(mGp.scheduleErrorText);
        mGp.scheduleErrorView.setTextColor(mGp.themeColorList.text_color_warning);
        if (mGp.scheduleErrorText.equals("")) mGp.scheduleErrorView.setVisibility(TextView.GONE);
        else mGp.scheduleErrorView.setVisibility(TextView.VISIBLE);

        mGp.mainDialogView = (LinearLayout) findViewById(R.id.main_dialog_view);
        mGp.mainDialogView.setBackgroundColor(mGp.themeColorList.text_background_color);
        mGp.mainDialogView.setVisibility(LinearLayout.VISIBLE);
        mGp.mainDialogView.bringToFront();

        mGp.confirmView = (LinearLayout) findViewById(R.id.main_dialog_confirm_view);
        mGp.confirmView.setVisibility(LinearLayout.GONE);
        mGp.confirmOverrideView=(LinearLayout) findViewById(R.id.main_dialog_confirm_override_view);
        mGp.confirmConflictView=(LinearLayout) findViewById(R.id.main_dialog_confirm_conflict_view);
        mGp.confirmConflictView.setVisibility(LinearLayout.GONE);
        mGp.confirmMsg = (TextView) findViewById(R.id.main_dialog_confirm_msg);
        mGp.confirmCancel = (Button) findViewById(R.id.main_dialog_confirm_sync_cancel);
        mGp.confirmYes = (Button) findViewById(R.id.copy_delete_confirm_yes);
        mGp.confirmNo = (Button) findViewById(R.id.copy_delete_confirm_no);
        mGp.confirmYesAll = (Button) findViewById(R.id.copy_delete_confirm_yesall);
        mGp.confirmNoAll = (Button) findViewById(R.id.copy_delete_confirm_noall);

        mGp.confirmDialogConflictFilePathA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_path);
        mGp.confirmDialogConflictFileLengthA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_length);
        mGp.confirmDialogConflictFileLastModA=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_a_last_mod);
        mGp.confirmDialogConflictFilePathB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_path);
        mGp.confirmDialogConflictFileLengthB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_length);
        mGp.confirmDialogConflictFileLastModB=(TextView) findViewById(R.id.main_dialog_confirm_conflict_pair_b_last_mod);
        mGp.confirmDialogConflictButtonSelectA=(Button) findViewById(R.id.main_dialog_confirm_conflict_select_pair_a_btn);
        mGp.confirmDialogConflictButtonSelectB=(Button) findViewById(R.id.main_dialog_confirm_conflict_select_pair_b_btn);
        mGp.confirmDialogConflictButtonSyncIgnoreFile=(Button) findViewById(R.id.main_dialog_confirm_conflict_ignore_file_btn);
        mGp.confirmDialogConflictButtonCancelSyncTask=(Button) findViewById(R.id.main_dialog_confirm_conflict_cancel_sync_task_btn);

        mGp.progressSpinView = (LinearLayout) findViewById(R.id.main_dialog_progress_spin_view);
        mGp.progressSpinView.setVisibility(LinearLayout.GONE);
        mGp.progressSpinSynctask = (TextView) findViewById(R.id.main_dialog_progress_spin_synctask);
        mGp.progressSpinMsg = (TextView) findViewById(R.id.main_dialog_progress_spin_syncmsg);
        mGp.progressSpinCancel = (Button) findViewById(R.id.main_dialog_progress_spin_btn_cancel);

        mMainTabLayout = (CustomTabLayout) findViewById(R.id.main_tab_layout);
        mMainTabLayout.addTab(mTabNameTask);
        mMainTabLayout.addTab(mTabNameSchedule);
        mMainTabLayout.addTab(mTabNameGroup);
        mMainTabLayout.addTab(mTabNameHistory);
        mMainTabLayout.addTab(mTabNameMessage);
        mMainTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mMainTabLayout.adjustTabWidth();

        View[] tab_view=new View[]{mSyncTaskView, mScheduleView, mGroupView, mHistoryView, mMessageView};
        CustomViewPagerAdapter mMainViewPagerAdapter = new CustomViewPagerAdapter(mActivity, tab_view);
        mMainViewPager = (CustomViewPager) findViewById(R.id.main_screen_pager);
        mMainViewPager.setAdapter(mMainViewPagerAdapter);
        mMainViewPager.setOffscreenPageLimit(tab_view.length);
        mMainViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                mMainTabLayout.setCurrentTabByPosition(position);
                if (isUiEnabled()) setUiEnabled();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mUtil.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });
        if (appStartStaus == START_INITIALYZING) {
            mMainTabLayout.setCurrentTabByName(mTabNameTask);
            mMainViewPager.setCurrentItem(0);
        }
        mMainTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabId=(String)tab.getTag();
                mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered. tab=" + tabId + ",v=" + mCurrentTab);

                mActionBar.setIcon(R.drawable.smbsync);
                mActionBar.setHomeButtonEnabled(false);
                mActionBar.setTitle(R.string.app_name);

                mMainViewPager.setCurrentItem(mMainTabLayout.getSelectedTabPosition());

                if (!mWhileRestoreViewProcess) {
                    if (mGp.syncTaskListAdapter.isShowCheckBox()) {
                        mGp.syncTaskListAdapter.setShowCheckBox(false);
                        mGp.syncTaskListAdapter.setAllItemChecked(false);
                        mGp.syncTaskListAdapter.notifyDataSetChanged();
                        setSyncTaskContextButtonNormalMode();
                    }

                    if (mGp.syncScheduleListAdapter.isSelectMode()) {
                        mGp.syncScheduleListAdapter.setSelectMode(false);
                        mGp.syncScheduleListAdapter.unselectAll();
                        mGp.syncScheduleListAdapter.notifyDataSetChanged();
                        setScheduleContextButtonNormalMode();
                    }

                    if (mGp.syncGroupListAdapter.isSelectMode()) {
                        mGp.syncGroupListAdapter.setSelectMode(false);
                        mGp.syncGroupListAdapter.unselectAll();
                        mGp.syncGroupListAdapter.notifyDataSetChanged();
                        setGroupContextButtonMode(mGp.syncGroupListAdapter);
                    }

                    if (mGp.syncHistoryListAdapter.isShowCheckBox()) {
                        mGp.syncHistoryListAdapter.setShowCheckBox(false);
                        mGp.syncHistoryListAdapter.setAllItemChecked(false);
                        mGp.syncHistoryListAdapter.notifyDataSetChanged();
                        setHistoryContextButtonNormalMode();
                    } else {
                        mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    }
                }

//                if (tabId.equals(mTabNameTask) && newSyncTaskListViewPos != -1) {
//                    mGp.syncTaskListView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            mGp.syncTaskListView.setSelection(newSyncTaskListViewPos);
//                            newSyncTaskListViewPos = -1;
//                        }
//                    });
//                } else if (tabId.equals(mTabNameMessage)) {
////                if (!mGp.freezeMessageViewScroll) {
////                    mGp.uiHandler.post(new Runnable() {
////                        @Override
////                        public void run() {
////                            if (mGp!=null && mGp.msgListView!=null && mGp.msgListAdapter!=null) {
////                                mGp.msgListView.setItemChecked(mGp.msgListAdapter.getCount() - 1, true);
////                                mGp.msgListView.setSelection(mGp.msgListAdapter.getCount() - 1);
////                            }
////                        }
////                    });
////                }
//                }
                mCurrentTab = tabId;
                refreshOptionMenu();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mGp.syncMessageView.setAdapter(mGp.syncMessageListAdapter);
        mGp.syncMessageView.setDrawingCacheEnabled(true);
        mGp.syncMessageView.setSelection(mGp.syncMessageListAdapter.getCount() - 1);

        mGp.syncTaskView.setAdapter(mGp.syncTaskListAdapter);
        mGp.syncTaskView.setDrawingCacheEnabled(true);

        mGp.syncHistoryView.setAdapter(mGp.syncHistoryListAdapter);
        mGp.syncHistoryListAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_top, menu);
        return true;//super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, isUiEnabled()="+isUiEnabled());
        boolean pm_bo = false;
//        if (Build.VERSION.SDK_INT >= 23) {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(true);
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            pm_bo = pm.isIgnoringBatteryOptimizations(packageName);
//            String bo_title = "";
//            if (pm_bo)
//                bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_disabled);
//            else bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_enabled);
//            menu.findItem(R.id.menu_top_show_battery_optimization).setTitle(bo_title);
//        } else {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(false);
//        }
//        LogCatUtil.prepareOptionMenu(mGp, mUtil, menu);

        menu.findItem(R.id.menu_top_request_all_file_access_permission).setVisible(false);
        //Enable "ALL_FILE_ACCESS"
//        if (CommonUtilities.isAllFileAccessAvailable()) {
//            menu.findItem(R.id.menu_top_request_all_file_access_permission).setVisible(true);
//        }

        if (mGp.settingScheduleSyncEnabled) menu.findItem(R.id.menu_top_scheduler).setIcon(R.drawable.ic_64_schedule);
        else menu.findItem(R.id.menu_top_scheduler).setIcon(R.drawable.ic_64_schedule_disabled);

        if (mGp.syncTaskListAdapter.isShowCheckBox()) menu.findItem(R.id.menu_top_sync).setTitle(R.string.msgs_menu_sync_selected);
        else menu.findItem(R.id.menu_top_sync).setTitle(R.string.msgs_menu_sync_all_auto);
        if (mCurrentTab.equals(mTabNameTask)) {
            menu.findItem(R.id.menu_top_sync).setVisible(true);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);
            menu.findItem(R.id.menu_top_execute_group).setVisible(false);
            menu.findItem(R.id.menu_top_show_hide_filter).setVisible(false);
        } else if (mCurrentTab.equals(mTabNameSchedule)) {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(true);
            menu.findItem(R.id.menu_top_execute_group).setVisible(false);
            menu.findItem(R.id.menu_top_show_hide_filter).setVisible(false);
        } else if (mCurrentTab.equals(mTabNameGroup)) {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);
            menu.findItem(R.id.menu_top_execute_group).setVisible(true);
            menu.findItem(R.id.menu_top_show_hide_filter).setVisible(false);
            if (mGp.syncGroupListAdapter.isSelectMode()) {
                menu.findItem(R.id.menu_top_execute_group).setTitle(R.string.msgs_menu_exec_group_selected);
                for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                    if (gli.isChecked) {
                        if (!gli.autoTaskOnly) {
                            String valid=GroupEditor.hasValidSyncTaskList(mContext, gli, mGp.syncTaskList);
                            if (!valid.equals("")) {
                                menu.findItem(R.id.menu_top_execute_group).setVisible(false);
                                break;
                            }
                        }
                    }
                }
            } else {
                if (mGp.syncGroupList.size()==0) menu.findItem(R.id.menu_top_execute_group).setVisible(false);
                menu.findItem(R.id.menu_top_execute_group).setTitle(R.string.msgs_menu_exec_group_all_enabled);
            }
        } else if (mCurrentTab.equals(mTabNameHistory)) {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);
            menu.findItem(R.id.menu_top_execute_group).setVisible(false);
            menu.findItem(R.id.menu_top_show_hide_filter).setVisible(false);
        } else if (mCurrentTab.equals(mTabNameMessage)) {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);
            menu.findItem(R.id.menu_top_execute_group).setVisible(false);
            menu.findItem(R.id.menu_top_show_hide_filter).setVisible(true);
        }

        if (isUiEnabled()) {
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_housekeep), true);
            if (mGp.syncThreadActive) menu.findItem(R.id.menu_top_housekeep).setVisible(false);
            else menu.findItem(R.id.menu_top_housekeep).setVisible(true);
            if (mCurrentTab.equals(mTabNameTask)) {
                if (mGp.syncTaskList.size()>0) menu.findItem(R.id.menu_top_sync).setVisible(true);
                else menu.findItem(R.id.menu_top_sync).setVisible(false);
            }
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_settings), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_export), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_import), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_log_management), true);

            if (mGp.safMgr.isStoragePermissionRequired()) menu.findItem(R.id.menu_top_select_storage).setVisible(true);
            else menu.findItem(R.id.menu_top_select_storage).setVisible(false);
//            if (mGp.debuggable) menu.findItem(R.id.menu_top_select_storage).setVisible(true);
//            else menu.findItem(R.id.menu_top_select_storage).setVisible(false);

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_about), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_show_battery_optimization), true);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_list_storage), true);

        } else {
            menu.findItem(R.id.menu_top_sync).setVisible(false);

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_export), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_import), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_settings), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_log_management), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_housekeep), false);

            menu.findItem(R.id.menu_top_select_storage).setVisible(false);

            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_about), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_show_battery_optimization), false);
            setMenuItemEnabled(menu, menu.findItem(R.id.menu_top_list_storage), false);
            menu.findItem(R.id.menu_top_scheduler).setVisible(false);

        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setMenuItemEnabled(Menu menu, MenuItem menu_item, boolean enabled) {
        CommonDialog.setMenuItemEnabled(mActivity, menu, menu_item, enabled);
    }


    private boolean mScheduleEditorAvailable = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                processHomeButtonPress();
                return true;
            case R.id.menu_top_sync:
                confirmStartSync();
                return true;
            case R.id.menu_top_execute_group:
                confirmGroupExecute();
                return true;
            case R.id.menu_top_show_hide_filter:
                showHideFilterView();
                return true;
            case R.id.menu_top_export:
                exportSyncTaskAndParms();
                return true;
            case R.id.menu_top_import:
                importSyncTaskAndParms();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_log_management:
                invokeLogManagement();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_scheduler:
                toggleScheduleEnabled();
                return true;
            case R.id.menu_top_about:
                aboutApp();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_settings:
                invokeSettingsActivity();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_kill:
                killTerminateApplication();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_housekeep:
                new HouseKeep(mActivity, mGp, mUtil);
                return true;
            case R.id.menu_top_show_battery_optimization:
                showBatteryOptimization();
                return true;
            case R.id.menu_top_list_storage:
                showSystemInfo();
                return true;
            case R.id.menu_top_select_storage:
                invokeStorageRequestor();
                return true;
            case R.id.menu_top_request_all_file_access_permission:
                //ENable "ALL_FILE_ACCESS"
                requestAllFileAccessPermission(null);
                return true;
        }
        if (isUiEnabled()) {
        }
        return false;
    }

    private void confirmStartSync() {
        if (isUiEnabled()) {
            final ArrayList<SyncTaskItem>sync_task_list=new ArrayList<SyncTaskItem>();
            if (mGp.syncTaskListAdapter.isShowCheckBox()) {
                String sep="";
                String task_list="";
                for(SyncTaskItem sti:mGp.syncTaskList) {
                    if (!sti.isSyncTaskError() && sti.isChecked()) {
                        task_list+="-"+sti.getSyncTaskName()+"\n";
                        sync_task_list.add(sti);
                        sep=",";
                    }
                }
                if (sync_task_list.size()>0) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            startSyncTask(sync_task_list);
                            TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);
                            setSyncTaskContextButtonNormalMode();
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                        }
                    });
                    mUtil.showCommonDialogWarn(true, mContext.getString(R.string.msgs_main_sync_button_confirmation_message_title),
                            mContext.getString(R.string.msgs_main_sync_button_confirmation_message_msg)+"\n"+task_list, ntfy);
                } else {
                    mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_main_sync_button_confirmation_message_title),
                            mContext.getString(R.string.msgs_main_sync_select_task_no_auto_task), null);
                }
            } else {
                GroupListAdapter.GroupListItem group_item=null;
                for(GroupListAdapter.GroupListItem gi:mGp.syncGroupList) {
                    if (gi.button== GroupListAdapter.GroupListItem.BUTTON_SYNC_BUTTON) {
                        group_item=gi;
                        break;
                    }
                }

                String task_list="";
                if (group_item==null || group_item.autoTaskOnly) {
                    for(SyncTaskItem sti:mGp.syncTaskList) {
                        if (sti.isSyncTaskAuto() && !sti.isSyncTestMode()) {
                            task_list+="-"+sti.getSyncTaskName()+"\n";
                            sync_task_list.add(sti);
                        }
                    }
                    if (task_list.equals("")) {
                        mUtil.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_sync_button_confirmation_message_title),
                                mContext.getString(R.string.msgs_main_sync_button_start_error_auto_task_does_not_exists)+"\n"+task_list, null);
                        return;
                    }
                } else {
                    String[]grp_task_array=group_item.taskList.split(NAME_LIST_SEPARATOR);
                    for(String item:grp_task_array) {
                        SyncTaskItem sti= TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item);
                        if (sti==null) {
                            mUtil.showCommonDialog(false, "W",
                                    mContext.getString(R.string.msgs_main_sync_button_confirmation_message_title),
                                    mContext.getString(R.string.msgs_main_sync_button_start_error_task_not_found,item), null);
                            return;
                        } else {
                            task_list+=sti.getSyncTaskName()+"\n";
                            sync_task_list.add(sti);
                        }
                    }
                }

                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        if (objects!=null) {
                            boolean suppress=(boolean)objects[0];
                            mGp.setSupressStartSyncConfirmationMessage(mContext, suppress);
                        }
                        startSyncTask(sync_task_list);
                        TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);
                        setSyncTaskContextButtonNormalMode();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
//                        if (objects!=null) {
//                            boolean suppress=(boolean)objects[0];
//                            mGp.setSupressStartSyncConfirmationMessage(mContext, suppress);
//                        }
                    }
                });

                if (mGp.isSupressStartSyncConfirmationMessage()) {
                    ntfy.notifyToListener(true, null);
                } else {
                    TaskEditor.showDialogWithHideOption(mActivity, mGp, mUtil,
                            true, mContext.getString(R.string.msgs_common_dialog_ok),
                            true, mContext.getString(R.string.msgs_common_dialog_cancel),
                            mContext.getString(R.string.msgs_main_sync_button_confirmation_message_title),
                            mContext.getString(R.string.msgs_main_sync_button_confirmation_message_msg)+"\n"+task_list,
                            mContext.getString(R.string.msgs_main_sync_button_confirmation_message_suppress), ntfy);
                }
            }
        }
    }

    private void confirmGroupExecute() {
        if (isUiEnabled()) {
            String e_msg="";
            ArrayList<String>exec_task_name_list=new ArrayList<String>();
            ArrayList<String>duplicate_task_name_list=new ArrayList<String>();
            if (mGp.syncGroupListAdapter.isSelectMode()) {
                for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                    if (gli.isChecked) {
                        if (gli.enabled) {
                            e_msg=GroupEditor.buildGroupExecuteSyncTaskList(mContext, mGp, mUtil, gli, exec_task_name_list, duplicate_task_name_list);
                            if (!e_msg.equals("")) break;
                        }
                    }
                }
            } else {
                for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                    if (gli.enabled) {
                        e_msg=GroupEditor.buildGroupExecuteSyncTaskList(mContext, mGp, mUtil, gli, exec_task_name_list, duplicate_task_name_list);
                        if (!e_msg.equals("")) break;
                    }
                }
            }
            if (!e_msg.equals("")) mUtil.showCommonDialogError(false, mContext.getString(R.string.msgs_group_start_confirmation_message_title), e_msg, null);
            else if (exec_task_name_list.size()==0) {
                mUtil.showCommonDialogError(false, mContext.getString(R.string.msgs_group_start_confirmation_message_title),
                        mContext.getString(R.string.msgs_group_start_confirmation_message_no_sync_task), null);
            } else {
                String w_msg="";
                if (duplicate_task_name_list.size()>0) {
                    w_msg=mContext.getString(R.string.msgs_group_start_confirmation_message_duplicate_msg)+"\n";
                    String w_msg_list="";
                    for(String item:duplicate_task_name_list) {
                        String[] item_array=item.split(NAME_LIST_SEPARATOR);
                        w_msg_list+=mContext.getString(R.string.msgs_group_start_confirmation_message_duplicate_item, item_array[0], item_array[1])+"\n";
                    }
                    w_msg+=w_msg_list;

                    w_msg+="\n"+mContext.getString(R.string.msgs_group_start_confirmation_message_start_msg)+"\n";
                    w_msg_list="";
                    for(String item:exec_task_name_list) {
                        w_msg_list+=String.format("- %1$s", item)+"\n";
                    }
                    w_msg+=w_msg_list;
                } else {
                    String w_msg_list="", sep="";
                    w_msg+=mContext.getString(R.string.msgs_group_start_confirmation_message_start_msg)+"\n";
                    for(String item:exec_task_name_list) {
                        w_msg_list+=sep+String.format("- %1$s", item);
                        sep="\n";
                    }
                    w_msg+=w_msg_list;
                }

                NotifyEvent ntfy_confirm_exec=new NotifyEvent(mContext);
                ntfy_confirm_exec.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        ArrayList<SyncTaskItem>exec_task_item_list=new ArrayList<SyncTaskItem>();
                        for(String item:exec_task_name_list) {
                            exec_task_item_list.add(TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item));
                        }
                        startSyncTask(exec_task_item_list);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });


                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        if (objects!=null) {
                            boolean suppress=(boolean)objects[0];
                            mGp.setSupressStartGroupConfirmationMessage(mContext, suppress);
                        }
                        ArrayList<SyncTaskItem>exec_task_item_list=new ArrayList<SyncTaskItem>();
                        for(String item:exec_task_name_list) {
                            exec_task_item_list.add(TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item));
                        }
                        startSyncTask(exec_task_item_list);
                        setSyncTaskContextButtonNormalMode();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
//                        if (objects!=null) {
//                            boolean suppress=(boolean)objects[0];
//                            mGp.setSupressStartSyncConfirmationMessage(mContext, suppress);
//                        }
                    }
                });

                if (mGp.isSupressStartGroupConfirmationMessage()) {
                    ntfy.notifyToListener(true, null);
                } else {
                    TaskEditor.showDialogWithHideOption(mActivity, mGp, mUtil,
                            true, mContext.getString(R.string.msgs_common_dialog_ok),
                            true, mContext.getString(R.string.msgs_common_dialog_cancel),
                            mContext.getString(R.string.msgs_group_start_confirmation_message_title),
                            w_msg,
                            mContext.getString(R.string.msgs_group_start_confirmation_message_suppress), ntfy);
                }
            }
        }
    }

    private void showHideFilterView() {
        LinearLayout ll_filter_view=(LinearLayout)mMessageView.findViewById(R.id.main_message_filter_view);
        if (ll_filter_view.getVisibility()==LinearLayout.GONE) ll_filter_view.setVisibility(LinearLayout.VISIBLE);
        else ll_filter_view.setVisibility(LinearLayout.GONE);
    }

    private void invokeStorageRequestor() {
        NotifyEvent ntfy_esp=new NotifyEvent(mContext);
        ntfy_esp.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                invalidateOptionsMenu();
                mGp.syncTaskListAdapter.notifyDataSetChanged();
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        TaskEditor.requestLocalStoragePermission(mActivity, mGp, mUtil, ntfy_esp);
    }

    private void toggleScheduleEnabled() {
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                mGp.setScheduleEnabled(mContext, !mGp.settingScheduleSyncEnabled);
                invalidateOptionsMenu();
                setScheduleTabMessage();
                ScheduleUtils.sendTimerRequest(mContext, SCHEDULE_INTENT_SET_TIMER);
                ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        String msg="";
        if (mGp.settingScheduleSyncEnabled) msg=mContext.getString(R.string.msgs_schedule_list_edit_confirm_scheduler_to_disabled);
        else msg=mContext.getString(R.string.msgs_schedule_list_edit_confirm_scheduler_to_enabled);
        mUtil.showCommonDialogWarn(true,msg,"",ntfy);
    }

    private void setContextButtonNormalMode() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskListAdapter.setShowCheckBox(false);
        mGp.syncTaskListAdapter.setAllItemChecked(false);
        mGp.syncTaskListAdapter.notifyDataSetChanged();
        setSyncTaskContextButtonNormalMode();

        mGp.syncHistoryListAdapter.setShowCheckBox(false);
        mGp.syncHistoryListAdapter.setAllItemChecked(false);
        mGp.syncHistoryListAdapter.notifyDataSetChanged();
        setHistoryContextButtonNormalMode();

        refreshOptionMenu();
    }

    private void processHomeButtonPress() {
        if (mCurrentTab.equals(mTabNameTask)) {
            if (mGp.syncTaskListAdapter.isShowCheckBox()) {
                mGp.syncTaskListAdapter.setShowCheckBox(false);
                mGp.syncTaskListAdapter.notifyDataSetChanged();

                setSyncTaskContextButtonNormalMode();
            }
        } else if (mCurrentTab.equals(mTabNameMessage)) {
        } else if (mCurrentTab.equals(mTabNameHistory)) {
            if (mGp.syncHistoryListAdapter.isShowCheckBox()) {
                mGp.syncHistoryListAdapter.setShowCheckBox(false);
                mGp.syncHistoryListAdapter.notifyDataSetChanged();
                setHistoryItemUnselectAll();
                setHistoryContextButtonNormalMode();
            }
        } else if (mCurrentTab.equals(mTabNameSchedule)) {
            if (mGp.syncScheduleListAdapter.isSelectMode()) {
                mGp.syncScheduleListAdapter.setSelectMode(false);
                mGp.syncScheduleListAdapter.notifyDataSetChanged();
                setScheduleContextButtonNormalMode();
            }
        } else if (mCurrentTab.equals(mTabNameGroup)) {
            if (mGp.syncGroupListAdapter.isSelectMode()) {
                mGp.syncGroupListAdapter.setSelectMode(false);
                mGp.syncGroupListAdapter.notifyDataSetChanged();
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
            }
        }
    }

    private void invokeLogManagement() {
        LogUtil.flushLog(mContext);
        LogManagementFragment lfm = LogManagementFragment.newInstance(mContext, false, getString(R.string.msgs_log_management_title));
        lfm.showDialog(mActivity, getSupportFragmentManager(), lfm, null);
    }

    private void exportSyncTaskAndParms() {
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                TaskListImportExport eitl=new TaskListImportExport(mActivity, mGp, mUtil);
                eitl.exportSyncTaskListDlg();
                setContextButtonNormalMode();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        ApplicationPassword.authentication(mGp, mActivity, getSupportFragmentManager(), mUtil, false, ntfy, ApplicationPassword.APPLICATION_PASSWORD_RESOURCE_EXPORT_TASK_LIST);
    }

    private void importSyncTaskAndParms() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
                reloadSettingParms();
                ScheduleUtils.sendTimerRequest(mContext, SCHEDULE_INTENT_SET_TIMER);
                ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
                setSyncTaskContextButtonNormalMode();
                mGp.syncTaskListAdapter.setShowCheckBox(false);

                mGp.syncScheduleListAdapter.notifyDataSetChanged();
                setScheduleTabMessage();

                setGroupContextButtonMode(mGp.syncGroupListAdapter);
                mGp.syncGroupListAdapter.notifyDataSetChanged();
                setGroupTabMessage();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        TaskListImportExport eitl=new TaskListImportExport(mActivity, mGp, mUtil);
        eitl.importSyncTaskListDlg(ntfy);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mUtil.addDebugMsg(2, "I", "main onKeyDown enterd, kc=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isUiEnabled()) {
                    if (mMainTabLayout.getSelectedTabName().equals(mTabNameTask)) {//
                        if (mGp.syncTaskListAdapter.isShowCheckBox()) {
                            //リセット選択モード
                            mGp.syncTaskListAdapter.setShowCheckBox(false);
                            mGp.syncTaskListAdapter.notifyDataSetChanged();
                            setSyncTaskContextButtonNormalMode();
                            return true;
                        }
                    } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameSchedule)) {
                        if (mGp.syncScheduleListAdapter.isSelectMode()) {
                            //リセット選択モード
                            mGp.syncScheduleListAdapter.setSelectMode(false);
                            mGp.syncScheduleListAdapter.notifyDataSetChanged();
                            setScheduleContextButtonNormalMode();
                            return true;
                        }
                    } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameMessage)) {
                        //NOP
                    } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameHistory)) {
                        if (mGp.syncHistoryListAdapter.isShowCheckBox()) {
                            //リセット選択モード
                            mGp.syncHistoryListAdapter.setShowCheckBox(false);
                            mGp.syncHistoryListAdapter.notifyDataSetChanged();
                            setHistoryItemUnselectAll();
                            setHistoryContextButtonNormalMode();
                            return true;
                        }
                    } else if (mMainTabLayout.getSelectedTabName().equals(mTabNameGroup)) {
                        if (mGp.syncGroupListAdapter.isSelectMode()) {
                            //リセット選択モード
                            mGp.syncGroupListAdapter.setSelectMode(false);
                            mGp.syncGroupListAdapter.notifyDataSetChanged();
                            setGroupContextButtonMode(mGp.syncGroupListAdapter);
                            return true;
                        }
                    }
                    finish();
                } else {
                    //処理中のためホームスクリーンを表示する
                    Intent in = new Intent();
                    in.setAction(Intent.ACTION_MAIN);
                    in.addCategory(Intent.CATEGORY_HOME);
                    startActivity(in);
                }
                return true;
            // break;
            default:
                return super.onKeyDown(keyCode, event);
            // break;
        }
    }

    private void aboutApp() {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.about_dialog);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        title.setText(getString(R.string.msgs_dlg_title_about) + " (Ver" + SystemInfo.getApplVersionName(mContext) + ")");

        // get our tabHost from the xml
        final CustomTabLayout tab_layout = (CustomTabLayout) dialog.findViewById(R.id.tab_layout);
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_func_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_privacy_btn));
        tab_layout.addTab(mContext.getString(R.string.msgs_about_dlg_change_btn));

        tab_layout.adjustTabWidth();

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int zf=(int)((float)100* GlobalParameters.getFontScaleFactorValue(mActivity));

        LinearLayout ll_func = (LinearLayout) vi.inflate(R.layout.about_dialog_func, null);
        final WebView func_view = (WebView) ll_func.findViewById(R.id.about_dialog_function_view);
        func_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_func_desc));
        func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        func_view.getSettings().setTextZoom(zf);

        LinearLayout ll_privacy = (LinearLayout) vi.inflate(R.layout.about_dialog_privacy, null);
        final WebView privacy_view = (WebView) ll_privacy.findViewById(R.id.about_dialog_privacy_view);
        privacy_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_privacy_desc));
        privacy_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        privacy_view.getSettings().setTextZoom(zf);

        LinearLayout ll_change = (LinearLayout) vi.inflate(R.layout.about_dialog_change, null);
        final WebView change_view = (WebView) ll_change.findViewById(R.id.about_dialog_change_view);
        change_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_change_desc));
        change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        change_view.getSettings().setTextZoom(zf);

        final CustomViewPagerAdapter adapter = new CustomViewPagerAdapter(mActivity,
                new WebView[]{func_view, privacy_view, change_view});
        final CustomViewPager viewPager = (CustomViewPager) dialog.findViewById(R.id.about_view_pager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
//                mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                tab_layout.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//		    	util.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });

        tab_layout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabSelected entered, state="+tab);
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabUnselected entered, state="+tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
//                mUtil.addDebugMsg(2,"I","onTabReselected entered, state="+tab);
            }

        });

        final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        // OKボタンの指定
        btnOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnOk.performClick();
            }
        });

        dialog.show();
    }

    private void killTerminateApplication() {

        mUtil.showCommonDialog(mContext, true, "W", mContext.getString(R.string.msgs_smnsync_main_kill_application), "", new CallBackListener() {
            @Override
            public void onCallBack(Context c, boolean positive, Object[] objects) {
                if (positive) {
                    LogUtil.flushLog(mContext);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
        });
    }

    private void invokeSettingsActivity() {
        mUtil.addDebugMsg(1, "I", "Invoke Setting activity.");
        ActivityResultLauncher<Intent> activity_laucher =
                mActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                mUtil.addDebugMsg(1, "I", "Return from Setting activity.");
                reloadSettingParms();
            }
        });
        mPrevLanguageSetting=GlobalParameters.getLanguageCode(mContext);
        Intent intent = new Intent(mContext, ActivitySettings.class);
        activity_laucher.launch(intent);
    }

    private String mPrevLanguageSetting=APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT;
    private void reloadSettingParms() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        String p_theme = mGp.settingScreenTheme;

        mGp.loadSettingsParms(mContext);

        String new_lc=GlobalParameters.getLanguageCode(mContext);
        final boolean theme_lang_changed=!new_lc.equals(mPrevLanguageSetting);

        if (!p_theme.equals(mGp.settingScreenTheme) || theme_lang_changed) {
            mUtil.showCommonDialogWarn(mContext, true,
                    mUtil.getStringWithLangCode(mActivity, new_lc, R.string.msgs_smbsync_main_settings_restart_title),
                    mUtil.getStringWithLangCode(mActivity, new_lc, R.string.msgs_smbsync_ui_settings_language_changed_restart),
                    mUtil.getStringWithLangCode(mActivity, new_lc, R.string.msgs_smbsync_ui_settings_language_changed_restart_immediate),
                    mUtil.getStringWithLangCode(mActivity, new_lc, R.string.msgs_smbsync_ui_settings_language_changed_restart_later),
                    new CallBackListener() {
                @Override
                public void onCallBack(Context c, boolean positive, Object[] objects) {
                    if (positive) {
                        mGp.activityRestartRequired=true;
                        mUtil.flushLog();
                        mGp.settingExitClean=false;
                        finish();
                    }
                }
            });
        }

        mGp.setDisplayFontScale(mActivity);
        reloadScreen(false);

        if (mGp.settingFixDeviceOrientationToPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        checkJcifsOptionChanged();

    }

    private void listSettingsOption() {
        mUtil.addDebugMsg(1, "I", "Option: " +
//                ", settingErrorOption=" + mGp.settingErrorOption +

                ", settingWifiLockRequired=" + mGp.settingWifiLockRequired +
                ", settingNoCompressFileType=" + mGp.settingNoCompressFileType +
                ", settingNotificationMessageWhenSyncEnded="+mGp.settingNotificationMessageWhenSyncEnded +
                ", settingNotificationVibrateWhenSyncEnded=" + mGp.settingNotificationVibrateWhenSyncEnded +
                ", settingNotificationSoundWhenSyncEnded=" + mGp.settingNotificationSoundWhenSyncEnded +
                ", settingNotificationVolume="+mGp.settingNotificationVolume +
                ", settingPreventSyncStartDelay="+mGp.settingPreventSyncStartDelay +

                ", settingFixDeviceOrientationToPortrait=" + mGp.settingFixDeviceOrientationToPortrait +
                ", settingForceDeviceTabletViewInLandscape=" + mGp.settingForceDeviceTabletViewInLandscape +
                ", settingApplicationLanguage=" + GlobalParameters.getLanguageCode(mContext) +

                ", settingExportedTaskEncryptRequired=" + mGp.settingExportedTaskEncryptRequired +
                ", settingScreenTheme=" + mGp.applicationTheme+//.settingScreenTheme +

                ", settingExitClean=" + mGp.settingExitClean +
                "");
    }

    private boolean isLegacyStorageAccessGranted() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return true;
        return false;
    }

    private boolean isPrimaryStorageAccessGranted() {
        if (mGp.safMgr.isUuidRegistered(SAF_FILE_PRIMARY_UUID)) return true;
        return false;
    }

    private void requestAllFileAccessPermission(NotifyEvent p_ntfy) {
//        Enable "ALL_FILE_ACCESS"
        NotifyEvent ntfy_all_file_access=new NotifyEvent(mContext);
        ntfy_all_file_access.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ActivityResultLauncher<Intent> mStartForActivityResult =
                        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (isAllFileAccessPermissionGranted()) {
                            if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
                        } else {
                            NotifyEvent ntfy_denied=new NotifyEvent(mContext);
                            ntfy_denied.setListener(new NotifyEvent.NotifyEventListener() {
                                @Override
                                public void positiveResponse(Context context, Object[] objects) {
                                    finish();
                                }
                                @Override
                                public void negativeResponse(Context context, Object[] objects) {}
                            });
                            mUtil.showCommonDialogWarn(false,
                                    mContext.getString(R.string.msgs_storage_permission_all_file_access_title),
                                    mContext.getString(R.string.msgs_storage_permission_all_file_access_denied_message), ntfy_denied);
                        }
                    }
                });
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                mStartForActivityResult.launch(intent);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
                NotifyEvent ntfy_denied=new NotifyEvent(mContext);
                ntfy_denied.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        finish();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialogWarn(false,
                        mContext.getString(R.string.msgs_storage_permission_all_file_access_title),
                        mContext.getString(R.string.msgs_storage_permission_all_file_access_denied_message), ntfy_denied);
            }
        });
        showDialogWithImageView(mContext.getString(R.string.msgs_storage_permission_all_file_access_title),
                mContext.getString(R.string.msgs_storage_permission_all_file_access_request_message),
                mContext.getString(R.string.msgs_storage_permission_all_file_access_image),
                mContext.getString(R.string.msgs_storage_permission_all_file_access_button_text),
                mContext.getString(R.string.msgs_common_dialog_cancel), ntfy_all_file_access);
    }

    private void showDialogWithImageView(final  String title_text, final String msg_text, String image_file_name,
                                         String button_label_ok, String button_label_cancel, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.dialog_with_image_view_dlg);

        TextView dlg_title=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_title);
        TextView dlg_msg=(TextView)dialog.findViewById(R.id.dialog_with_image_view_dlg_msg);
        ImageView dlg_image=(ImageView)dialog.findViewById(R.id.dialog_with_image_view_dlg_image);

        Button dlg_ok=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_ok);
        Button dlg_cancel=(Button)dialog.findViewById(R.id.dialog_with_image_view_dlg_btn_cancel);

        dlg_title.setText(title_text);
        dlg_msg.setText(msg_text);

        try {
            InputStream is = mContext.getResources().getAssets().open(image_file_name);
            BufferedInputStream bis = new BufferedInputStream(is, 1024*1024);
            Bitmap bm = BitmapFactory.decodeStream(bis);
            dlg_image.setImageBitmap(bm);
            is.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        dlg_ok.setText(button_label_ok);
        dlg_cancel.setText(button_label_cancel);

        dlg_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(true, null);
                dialog.dismiss();
            }
        });

        dlg_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(false, null);
                dialog.dismiss();;
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dlg_cancel.performClick();
            }
        });

        dialog.show();
    }

    private boolean isAllFileAccessPermissionGranted() {
        return Environment.isExternalStorageManager();
    }

//    private void checkInternalStoragePermission(final NotifyEvent p_ntfy) {
//        ArrayList<SafStorage3>ssl=mGp.safMgr.getSafStorageList();
//        boolean internal_permitted=isPrimaryStorageAccessGranted();
//        if (!internal_permitted) {
//            NotifyEvent ntfy_request=new NotifyEvent(mContext);
//            ntfy_request.setListener(new NotifyEvent.NotifyEventListener() {
//                @Override
//                public void positiveResponse(Context context, Object[] objects) {
//                    final NotifyEvent ntfy_response=new NotifyEvent(mContext);
//                    ntfy_response.setListener(new NotifyEvent.NotifyEventListener() {
//                          @Override
//                          public void positiveResponse(Context context, Object[] objects) {
//                              int requestCode=(Integer)objects[0];
//                              int resultCode=(Integer)objects[1];
//                              Intent data=(Intent)objects[2];
//
//                              if (resultCode == Activity.RESULT_OK) {
//                                  if (data==null || data.getDataString()==null) {
//                                      mUtil.showCommonDialog(false, "W", "Storage Grant write permission failed because null intent data was returned.", "", null);
//                                      mUtil.addLogMsg("E", "", "Storage Grant write permission failed because null intent data was returned.", "");
//                                      return;
//                                  }
//                                  mUtil.addDebugMsg(1, "I", "Intent=" + data.getData().toString());
//                                  if (!mGp.safMgr.isRootTreeUri(data.getData())) {
//                                      mUtil.addDebugMsg(1, "I", "Selected UUID="+ SafManager3.getUuidFromUri(data.getData().toString()));
//                                      String em=mGp.safMgr.getLastErrorMessage();
//                                      if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
//
//                                      NotifyEvent ntfy_retry = new NotifyEvent(mContext);
//                                      ntfy_retry.setListener(new NotifyEvent.NotifyEventListener() {
//                                          @Override
//                                          public void positiveResponse(Context c, Object[] o) {
//                                              requestStoragePermissionByUuid(SAF_FILE_PRIMARY_UUID, ntfy_response);
//                                          }
//
//                                          @Override
//                                          public void negativeResponse(Context c, Object[] o) {
//                                              NotifyEvent ntfy_term = new NotifyEvent(mContext);
//                                              ntfy_term.setListener(new NotifyEvent.NotifyEventListener() {
//                                                  @Override
//                                                  public void positiveResponse(Context c, Object[] o) {
//                                                      isTaskTermination = true;
//                                                      finish();
//                                                  }
//
//                                                  @Override
//                                                  public void negativeResponse(Context c, Object[] o) {}
//                                              });
//                                              mUtil.showCommonDialog(false, "W",
//                                                      mContext.getString(R.string.msgs_main_permission_internal_storage_title),
//                                                      mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), ntfy_term);
//                                          }
//                                      });
//                                      mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_external_storage_select_retry_select_msg),
//                                              data.getData().getPath(), ntfy_retry);
//                                  } else {
//                                      mUtil.addDebugMsg(1, "I", "Selected UUID="+SafManager3.getUuidFromUri(data.getData().toString()));
//                                      String em=mGp.safMgr.getLastErrorMessage();
//                                      if (em.length()>0) mUtil.addDebugMsg(1, "I", "SafMessage="+em);
//                                      boolean rc=mGp.safMgr.addUuid(data.getData());
//                                      if (!rc) {
//                                          String saf_msg=mGp.safMgr.getLastErrorMessage();
//                                          mUtil.showCommonDialog(false, "W", "Primary UUID registration failed.", saf_msg, null);
//                                          mUtil.addLogMsg("E", "", "Primary UUID registration failed.\n", saf_msg);
//                                      }
//                                      mGp.syncTaskListAdapter.notifyDataSetChanged();
//                                      p_ntfy.notifyToListener(true, null);
//                                  }
//                              } else {
//                                  NotifyEvent ntfy_term = new NotifyEvent(mContext);
//                                  ntfy_term.setListener(new NotifyEvent.NotifyEventListener() {
//                                      @Override
//                                      public void positiveResponse(Context c, Object[] o) {
//                                          isTaskTermination = true;
//                                          finish();
//                                      }
//
//                                      @Override
//                                      public void negativeResponse(Context c, Object[] o) {}
//                                  });
//                                  mUtil.showCommonDialog(false, "W",
//                                          mContext.getString(R.string.msgs_main_permission_internal_storage_title),
//                                          mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), ntfy_term);
//
//                              }
//                          }
//
//                          @Override
//                          public void negativeResponse(Context context, Object[] objects) {
//
//                          }
//                    });
//                    requestStoragePermissionByUuid(SAF_FILE_PRIMARY_UUID, ntfy_response);
//                }
//
//                @Override
//                public void negativeResponse(Context context, Object[] objects) {
//                    NotifyEvent ntfy_term = new NotifyEvent(mContext);
//                    ntfy_term.setListener(new NotifyEvent.NotifyEventListener() {
//                        @Override
//                        public void positiveResponse(Context c, Object[] o) {
//                            isTaskTermination = true;
//                            finish();
//                        }
//
//                        @Override
//                        public void negativeResponse(Context c, Object[] o) {}
//                    });
//                    mUtil.showCommonDialogWarn(false,
//                            mContext.getString(R.string.msgs_main_permission_internal_storage_title),
//                            mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), ntfy_term);
//                }
//            });
//            mUtil.showCommonDialogWarn(true,
//                    mContext.getString(R.string.msgs_main_permission_internal_storage_title),
//                    mContext.getString(R.string.msgs_main_permission_internal_storage_request_msg),
//                    ntfy_request);
//        } else {
//            p_ntfy.notifyToListener(true, null);
//        }
//    }

    private void requestLegacyStoragePermission(final CallBackListener cbl) {
        ArrayList<SafStorage3>ssl=mGp.safMgr.getSafStorageList();
        mUtil.showCommonDialogWarn(mContext, true,
                mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                mContext.getString(R.string.msgs_main_permission_internal_storage_request_msg), new CallBackListener() {
            @Override
            public void onCallBack(Context c, boolean positive, Object[] objects) {
                if (positive) {
                    ActivityResultLauncher<String> request_permission =mActivity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            mGp.syncHistoryList.addAll(mUtil.loadHistoryList());
                            mGp.syncMessageList.addAll(CommonUtilities.loadMessageList(mContext, mGp));
                            cbl.onCallBack(mContext, true, null);
                        } else {
                            mUtil.showCommonDialog(mContext, false, "W",
                                    mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                                    mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), new CallBackListener() {
                                @Override
                                public void onCallBack(Context c, boolean positive, Object[] objects) {
                                    if (positive) finish();
                                }
                            });
                        }
                    });
                    request_permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    mUtil.showCommonDialogWarn(mContext, false,
                            mContext.getString(R.string.msgs_main_permission_internal_storage_title),
                            mContext.getString(R.string.msgs_main_permission_internal_storage_denied_msg), new CallBackListener() {
                        @Override
                        public void onCallBack(Context c, boolean positive, Object[] objects) {
                            mUiHandler.post(new Runnable(){
                                @Override
                                public void run() {
                                    if (positive) finish();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //ActivityResultLauncherを使用する事
        //コードを追加しない事
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //ActivityResultLauncherを使用する事
        //コードを追加しない事
    }

    private void setHistoryViewListener() {
        mGp.syncHistoryView.setEnabled(true);
        mGp.syncHistoryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mGp.syncHistoryView.setEnabled(false);
                HistoryListAdapter.HistoryListItem item = mGp.syncHistoryListAdapter.getItem(position);
                if (mGp.syncHistoryListAdapter.isShowCheckBox()) {
                    item.isChecked = !item.isChecked;
                    setHistoryContextButtonSelectMode();
                    mGp.syncHistoryView.setEnabled(true);
                } else {
                    if (item.sync_result_file_path!=null && !item.sync_result_file_path.equals("")) {
                        SafFile3 lf=new SafFile3(mContext, item.sync_result_file_path);
                        if (lf.exists()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri=FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(item.sync_result_file_path));
                            intent.setDataAndType(uri, "text/plain");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                mActivity.startActivity(intent);
                            } catch(ActivityNotFoundException e) {
                                mUtil.showCommonDialog(false, "E",mContext.getString(R.string.msgs_main_sync_history_result_activity_not_found_for_log_display), "", null);
                            }
                        }
                    }
                    mUiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mGp.syncHistoryView.setEnabled(true);
                        }
                    }, 1000);
                }
                mGp.syncHistoryListAdapter.notifyDataSetChanged();
            }
        });

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setHistoryContextButtonSelectMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        mGp.syncHistoryListAdapter.setNotifyCheckBoxEventHandler(ntfy);

        mGp.syncHistoryView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                if (mGp.syncHistoryListAdapter.isEmptyAdapter()) return true;
                if (!isUiEnabled()) return true;

                if (!mGp.syncHistoryListAdapter.getItem(pos).isChecked) {
                    if (mGp.syncHistoryListAdapter.isAnyItemSelected()) {
                        int down_sel_pos = -1, up_sel_pos = -1;
                        int tot_cnt = mGp.syncHistoryListAdapter.getCount();
                        if (pos + 1 <= tot_cnt) {
                            for (int i = pos + 1; i < tot_cnt; i++) {
                                if (mGp.syncHistoryListAdapter.getItem(i).isChecked) {
                                    up_sel_pos = i;
                                    break;
                                }
                            }
                        }
                        if (pos > 0) {
                            for (int i = pos; i >= 0; i--) {
                                if (mGp.syncHistoryListAdapter.getItem(i).isChecked) {
                                    down_sel_pos = i;
                                    break;
                                }
                            }
                        }
                        if (up_sel_pos != -1 && down_sel_pos == -1) {
                            for (int i = pos; i < up_sel_pos; i++)
                                mGp.syncHistoryListAdapter.getItem(i).isChecked = true;
                        } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                mGp.syncHistoryListAdapter.getItem(i).isChecked = true;
                        } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i <= pos; i++)
                                mGp.syncHistoryListAdapter.getItem(i).isChecked = true;
                        }
                        mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    } else {
                        mGp.syncHistoryListAdapter.setShowCheckBox(true);
                        mGp.syncHistoryListAdapter.getItem(pos).isChecked = true;
                        mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    }
                    setHistoryContextButtonSelectMode();
                }
                return true;
            }
        });

    }

    private void sendHistoryFile() {
        final SafFile3 sf=mGp.safMgr.getRootSafFile(SAF_FILE_PRIMARY_UUID);
        final String zip_file_name = sf.getAppDirectoryCache() + "/history.zip";
        final File lf = new File(zip_file_name);

        int no_of_files = 0;
        for (int i = 0; i < mGp.syncHistoryListAdapter.getCount(); i++) {
            if (mGp.syncHistoryListAdapter.getItem(i).isChecked && !mGp.syncHistoryListAdapter.getItem(i).sync_result_file_path.equals("")) {
                no_of_files++;
            }
        }

        if (no_of_files == 0) {
            MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "E", mContext.getString(R.string.msgs_main_sync_history_result_log_not_found), "");
            mdf.showDialog(getSupportFragmentManager(), mdf, null);
            return;
        }

        final String[] file_name = new String[no_of_files];
        int files_pos = 0;
        for (int i = 0; i < mGp.syncHistoryListAdapter.getCount(); i++) {
            if (mGp.syncHistoryListAdapter.getItem(i).isChecked && !mGp.syncHistoryListAdapter.getItem(i).sync_result_file_path.equals("")) {
                file_name[files_pos] = mGp.syncHistoryListAdapter.getItem(i).sync_result_file_path;
                files_pos++;
            }
        }

        final ThreadCtrl tc = new ThreadCtrl();
        final ProgressBarDialogFragment pbdf = ProgressBarDialogFragment.newInstance(
                mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_creating), "",
                mContext.getString(R.string.msgs_common_dialog_cancel),
                mContext.getString(R.string.msgs_common_dialog_cancel));
        pbdf.showDialog(mContext, getSupportFragmentManager(), pbdf, true, new CallBackListener(){
            @Override
            public void onCallBack(Context c, boolean positive, Object[] objects) {
                if (!positive) tc.setDisabled();
            }
        });
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    ZipUtil.createZipFile(mContext, tc, pbdf, zip_file_name, sf.getAppDirectoryCache(), file_name);
                    if (tc.isEnabled()) {
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("application/zip");
                        Uri uri=FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", lf);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        try {
                            mContext.startActivity(intent);
                        } catch(Exception e) {
                            String st= MiscUtil.getStackTraceString(e);
                            mUtil.showCommonDialog(false, "E", "History send error", e.getMessage()+"\n"+st, null);
                        }
                    } else {
                        lf.delete();
                        MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "W", mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_cancelled), "");
                        mdf.showDialog(getSupportFragmentManager(), mdf, null);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                pbdf.dismiss();
            }
        };
        th.start();
    }

    private void saveGroupList() {
        mGp.syncGroupListAdapter.sort();
        Thread th=new Thread() {
            @Override
            public void run() {
                String config_data= TaskListImportExport.saveTaskListToAppDirectory(mContext, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                if (config_data!=null) TaskListImportExport.saveTaskListToAutosave(mActivity, mContext, mGp.settingAppManagemsntDirectoryName, config_data);
            }
        };
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
    }

    private void setGroupContextButtonListener() {
        mContextGroupButtonMoveToUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUiEnabled()) {
                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                    for (int i = 0; i < mGp.syncGroupListAdapter.getCount(); i++) {
                        GroupListAdapter.GroupListItem item = mGp.syncGroupListAdapter.getItem(i);
                        if (item.isChecked) {
                            int c_pos = item.position;
                            if (c_pos > 0) {
                                for (int j = 0; j < mGp.syncGroupListAdapter.getCount(); j++) {
                                    if (mGp.syncGroupListAdapter.getItem(j).position == (c_pos - 1)) {
                                        mGp.syncGroupListAdapter.getItem(j).position=c_pos;
                                    }
                                }
                                item.position=c_pos - 1;
                                mGp.syncGroupListAdapter.sort();
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                                saveGroupList();

                                if (item.position == 0) {
                                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.INVISIBLE);
                                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                                }
                                if (item.position == (mGp.syncGroupListAdapter.getCount() - 1)) {
                                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.INVISIBLE);
                                }
                            }
                            break;
                        }
                    }
                }

            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonMoveToUp, mContext.getString(R.string.msgs_group_cont_label_up));

        mContextGroupButtonMoveToDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUiEnabled()) {
                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                    for (int i = 0; i < mGp.syncGroupListAdapter.getCount(); i++) {
                        GroupListAdapter.GroupListItem item = mGp.syncGroupListAdapter.getItem(i);
                        if (item.isChecked) {
                            int c_pos = item.position;
                            if (item.position < (mGp.syncGroupListAdapter.getCount() - 1)) {
                                for (int j = 0; j < mGp.syncGroupListAdapter.getCount(); j++) {
                                    if (mGp.syncGroupListAdapter.getItem(j).position == (c_pos + 1)) {
                                        mGp.syncGroupListAdapter.getItem(j).position=c_pos;
                                    }
                                }
                                item.position=c_pos + 1;
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                                saveGroupList();

                                if (item.position == 0) {
                                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.INVISIBLE);
                                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                                }
                                if (item.position == (mGp.syncGroupListAdapter.getCount() - 1)) {
                                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.INVISIBLE);
                                }
                            }
                            break;
                        }
                    }
                }

            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonMoveToDown, mContext.getString(R.string.msgs_group_cont_label_down));

        mContextGroupButtonToEnabled.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String list="", sep="";
                for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                    if (gli.isChecked) {
                        list+=sep+gli.groupName;
                        sep="\n";
                    }
                }
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                            if (gli.isChecked) {
                                gli.enabled=true;
                            }
                        }
                        mGp.syncGroupListAdapter.setSelectMode(false);
                        mGp.syncGroupListAdapter.notifyDataSetChanged();
                        setGroupContextButtonMode(mGp.syncGroupListAdapter);
                        saveGroupList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialogWarn(true, mContext.getString(R.string.msgs_group_edit_enable_sync_group), list, ntfy);
            }
        });
//        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonToEnabled, mContext.getString(R.string.msgs_group_cont_label_add));

        mContextGroupButtonToDisabled.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String list="", sep="";
                for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                    if (gli.isChecked) {
                        list+=sep+gli.groupName;
                        sep="\n";
                    }
                }
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for(GroupListAdapter.GroupListItem gli:mGp.syncGroupList) {
                            if (gli.isChecked) {
                                gli.enabled=false;
                            }
                        }
                        mGp.syncGroupListAdapter.setSelectMode(false);
                        mGp.syncGroupListAdapter.notifyDataSetChanged();
                        setGroupContextButtonMode(mGp.syncGroupListAdapter);
                        saveGroupList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialogWarn(true, mContext.getString(R.string.msgs_group_edit_disable_sync_group), list, ntfy);
            }
        });
//        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonToDisabled, mContext.getString(R.string.msgs_group_cont_label_add));

        mContextGroupButtonAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        GroupListAdapter.GroupListItem si = (GroupListAdapter.GroupListItem) objects[0];
                        mGp.syncGroupListAdapter.add(si);
                        mGp.syncGroupListAdapter.sort();
                        mGp.syncGroupListAdapter.notifyDataSetChanged();
                        setGroupContextButtonMode(mGp.syncGroupListAdapter);
                        saveGroupList();
                        setGroupTabMessage();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                GroupEditor ge=new GroupEditor(mUtil, mActivity, mGp, false, mGp.syncGroupList, new GroupListAdapter.GroupListItem(), ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonAdd, mContext.getString(R.string.msgs_group_cont_label_add));

        mContextGroupButtonCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                for(GroupListAdapter.GroupListItem item:mGp.syncGroupList) {
                    if (item.isChecked) {
                        NotifyEvent ntfy = new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                GroupListAdapter.GroupListItem si = (GroupListAdapter.GroupListItem) objects[0];
                                item.isChecked=false;
                                mGp.syncGroupListAdapter.add(si);
                                mGp.syncGroupListAdapter.sort();
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                                setGroupContextButtonMode(mGp.syncGroupListAdapter);
                                saveGroupList();
                                setGroupTabMessage();
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        GroupListAdapter.GroupListItem gi=item.clone();
                        gi.isChecked=false;
                        GroupEditor ge=new GroupEditor(mUtil, mActivity, mGp, false, mGp.syncGroupList, gi, ntfy);
                        break;
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonCopy, mContext.getString(R.string.msgs_group_cont_label_copy));

        mContextGroupButtonRename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmGroupRename();
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonRename, mContext.getString(R.string.msgs_group_cont_label_rename));

        mContextGroupButtonDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmGroupDelete();
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonDelete, mContext.getString(R.string.msgs_group_cont_label_delete));

        mContextGroupButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.syncGroupListAdapter.setSelectMode(true);
                mGp.syncGroupListAdapter.selectAll();
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonSelectAll, mContext.getString(R.string.msgs_group_cont_label_select_all));

        mContextGroupButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                mGp.syncTabGroupAdapter.setSelectMode(false);
                mGp.syncGroupListAdapter.unselectAll();
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonUnselectAll, mContext.getString(R.string.msgs_group_cont_label_unselect_all));
    }

    private void setGroupContextButtonMode(GroupListAdapter adapter) {
        boolean selected = false;
        int sel_cnt = 0;
        boolean enabled = false, disabled = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).isChecked) {
                selected = true;
                sel_cnt++;
                if (adapter.getItem(i).enabled) enabled = true;
                else disabled = true;
            }
        }

        setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.INVISIBLE);

        setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.INVISIBLE);

        setContextButtonVisibility(mContextGroupButtonToEnabledView,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextGroupButtonToDisabledView,ImageButton.INVISIBLE);
        if (adapter.isSelectMode()) {
            if (sel_cnt == 0) {
                setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.INVISIBLE);
            } else if (sel_cnt == 1) {
                setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.VISIBLE);
                int sel_pos=0;
                for(int i=0;i<mGp.syncGroupList.size();i++) {
                    if (mGp.syncGroupList.get(i).isChecked) {
                        sel_pos=i;
                        if (mGp.syncGroupList.get(i).enabled) {
                            setContextButtonVisibility(mContextGroupButtonToDisabledView,ImageButton.VISIBLE);
                        } else {
                            setContextButtonVisibility(mContextGroupButtonToEnabledView,ImageButton.VISIBLE);
                        }
                        break;
                    }
                }
                setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                if (sel_pos == 0) {
                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.INVISIBLE);
                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.VISIBLE);
                }
                if (sel_pos == (mGp.syncGroupList.size() - 1)) {
                    setContextButtonVisibility(mContextGroupButtonMoveToUpView,ImageButton.VISIBLE);
                    setContextButtonVisibility(mContextGroupButtonMoveToDownView,ImageButton.INVISIBLE);
                }
            } else if (sel_cnt >= 1) {
                setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.VISIBLE);

                boolean show_enable=false, show_disable=false;
                for(int i=0;i<mGp.syncGroupList.size();i++) {
                    if (mGp.syncGroupList.get(i).isChecked) {
                        if (mGp.syncGroupList.get(i).enabled) {
                            show_disable=true;
                        } else {
                            show_enable=true;
                        }
                    }
                }
                if (show_disable) setContextButtonVisibility(mContextGroupButtonToDisabledView,ImageButton.VISIBLE);
                if (show_enable) setContextButtonVisibility(mContextGroupButtonToEnabledView,ImageButton.VISIBLE);

            }
            int tot_cnt = adapter.getCount();
            setActionBarSelectMode(sel_cnt, tot_cnt);
        } else {
            if (adapter.getCount()>0) {
                setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.VISIBLE);
            } else {
                setContextButtonVisibility(mContextGroupButtonAddView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextGroupButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonDeleteView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonUnselectAllView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextGroupButtonSelectAllView,LinearLayout.INVISIBLE);
            }
            setActionBarNormalMode();
        }
        refreshOptionMenu();
    }

    private void setGroupListViewListener() {
        NotifyEvent ntfy_sync=new NotifyEvent(mContext);
        ntfy_sync.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] o) {
                if (isUiEnabled()) {
                    GroupListAdapter.GroupListItem gi=(GroupListAdapter.GroupListItem)o[0];
                    if (!gi.autoTaskOnly) {
                        String[]task_array=gi.taskList.split(NAME_LIST_SEPARATOR);
                        ArrayList<SyncTaskItem>task_list=new ArrayList<SyncTaskItem>();
                        for(String item:task_array) {
                            SyncTaskItem sti= TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item);
                            if (sti!=null) task_list.add(sti);
                            else {
                                mUtil.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_group_can_not_sync_specified_task_does_not_exists, item), "", null);
                                return;
                            }
                        }
                        startSyncTask(task_list);
                    } else {
                        syncAutoSyncTask();
                    }
                }
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) { }
        });
        mGp.syncGroupListAdapter.setSyncNotify(ntfy_sync);

        NotifyEvent ntfy_sw = new NotifyEvent(mContext);
        ntfy_sw.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                int pos=(int)objects[0];
                saveGroupList();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncGroupListAdapter.setSwNotify(ntfy_sw);

        NotifyEvent ntfy_cb = new NotifyEvent(mContext);
        ntfy_cb.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
                refreshOptionMenu();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncGroupListAdapter.setCbNotify(ntfy_cb);

        mGp.syncGroupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isUiEnabled()) {
                    if (mGp.syncGroupListAdapter.isSelectMode()) {
                        if (mGp.syncGroupListAdapter.getItem(i).isChecked) {
                            mGp.syncGroupListAdapter.getItem(i).isChecked = false;
                        } else {
                            mGp.syncGroupListAdapter.getItem(i).isChecked = true;
                        }
                        mGp.syncGroupListAdapter.notifyDataSetChanged();
                        setGroupContextButtonMode(mGp.syncGroupListAdapter);
                    } else {
                        NotifyEvent ntfy = new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                saveGroupList();
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        GroupEditor sm = new GroupEditor(mUtil, mActivity, mGp, true, mGp.syncGroupList, mGp.syncGroupList.get(i), ntfy);
                    }
                }
            }
        });
        mGp.syncGroupView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isUiEnabled()) {
                    mGp.syncGroupListAdapter.setSelectMode(true);
                    mGp.syncGroupListAdapter.getItem(i).isChecked = !mGp.syncGroupListAdapter.getItem(i).isChecked;
                    mGp.syncGroupListAdapter.notifyDataSetChanged();
                    setGroupContextButtonMode(mGp.syncGroupListAdapter);
                }
                return true;
            }
        });
    }

    private void confirmGroupRename() {
        for(GroupListAdapter.GroupListItem item:mGp.syncGroupList) {
            if (item.isChecked) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        NotifyEvent ntfy_rename=new NotifyEvent(mContext);
                        ntfy_rename.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                saveGroupList();
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        renameGroup(item, ntfy_rename);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                ntfy.notifyToListener(true, null);
                break;
            }
        }
    }

    private void renameGroup(final GroupListAdapter.GroupListItem si, final NotifyEvent p_ntfy) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        dlg_msg.setTextColor(Color.YELLOW);
        dlg_msg.setVisibility(TextView.VISIBLE);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etInput = (EditText) dialog.findViewById(R.id.single_item_input_dir);

        title.setText(mContext.getString(R.string.msgs_group_cont_label_rename));

        dlg_cmp.setVisibility(TextView.GONE);
        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);
        etInput.setText(si.groupName);
//        dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_duplicate_name));
//        dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_warning));
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
//                if (arg0.toString().startsWith(GROUP_SYSTEM_PREFIX)) {
//                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
//                    dlg_msg.setText(mContext.getString(R.string.msgs_group_list_edit_dlg_error_group_name_not_allowed_asterisk_in_first_char));
//                    return;
//                }
                if (!arg0.toString().equalsIgnoreCase(si.groupName)) {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    dlg_msg.setText("");
                } else {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_duplicate_name));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        });

        //OK button
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy_conf=new NotifyEvent(mContext);
                ntfy_conf.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        dialog.dismiss();
                        String new_name = etInput.getText().toString();

                        si.groupName = new_name;
                        si.isChecked=false;
                        saveGroupList();
                        if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_schedule_confirm_msg_rename_warning), "", ntfy_conf);
            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

    }

    private void confirmGroupDelete() {
        String del_name="", sep="";
        final ArrayList<GroupListAdapter.GroupListItem>del_list=new ArrayList<GroupListAdapter.GroupListItem>();
        for(GroupListAdapter.GroupListItem item:mGp.syncGroupList) {
            if (item.isChecked) {
                del_name+=sep+item.groupName;
                sep=", ";
                del_list.add(item);
            }
        }
        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                for(GroupListAdapter.GroupListItem item:del_list) {
                    mGp.syncGroupList.remove(item);
                }
                mGp.syncGroupListAdapter.unselectAll();
                mGp.syncGroupListAdapter.setSelectMode(false);
                saveGroupList();
                mGp.syncGroupListAdapter.notifyDataSetChanged();
                setGroupTabMessage();
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_group_confirm_msg_delete), del_name, ntfy);

    }

    private void setGroupTabMessage() {
        if (mGp.syncGroupListAdapter.getCount() == 0) {
            mGp.syncGroupMessage.setVisibility(TextView.VISIBLE);
            mGp.syncGroupMessage.setText(mContext.getString(R.string.msgs_group_list_edit_no_group));
            mGp.syncGroupView.setVisibility(ListView.GONE);
        } else {
            mGp.syncGroupMessage.setVisibility(TextView.GONE);
            mGp.syncGroupView.setVisibility(ListView.VISIBLE);
        }
        mGp.syncGroupListAdapter.notifyDataSetChanged();
    }

    private void setScheduleTabMessage() {
        if (mGp.syncScheduleListAdapter.getCount() == 0) {
            mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
            mGp.syncScheduleMessage.setText(mContext.getString(R.string.msgs_schedule_list_edit_no_schedule));
        } else {
            if (mGp.settingScheduleSyncEnabled) {
                if (mGp.syncScheduleListAdapter.getCount()!=0) {
                    mGp.syncScheduleMessage.setVisibility(TextView.GONE);
                }
            } else {
                mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
                mGp.syncScheduleMessage.setText(mContext.getString(R.string.msgs_schedule_list_edit_scheduler_disabled));
            }
        }
        mGp.syncScheduleListAdapter.notifyDataSetChanged();
    }

    private void setScheduleContextButtonListener() {
        mContextScheduleButtonAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        ScheduleListAdapter.ScheduleListItem si = (ScheduleListAdapter.ScheduleListItem) objects[0];
                        mGp.syncScheduleListAdapter.add(si);
                        mGp.syncScheduleListAdapter.sort();
                        mGp.syncScheduleListAdapter.notifyDataSetChanged();
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                        saveScheduleList();
                        setScheduleTabMessage();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                ScheduleEditor sm = new ScheduleEditor(mUtil, mActivity, mContext, mGp, false, mGp.syncScheduleList, new ScheduleListAdapter.ScheduleListItem(), ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonAdd, mContext.getString(R.string.msgs_schedule_cont_label_add));

        mContextScheduleButtonDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleListAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleListAdapter.getItem(i).isChecked) {
                                mGp.syncScheduleListAdapter.remove(mGp.syncScheduleListAdapter.getItem(i));
                            }
                        }
                        if (mGp.syncScheduleListAdapter.getCount() == 0) {
                            mGp.syncScheduleListAdapter.setSelectMode(false);
                            mGp.syncScheduleMessage.setVisibility(TextView.VISIBLE);
                        }
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                        mGp.syncScheduleListAdapter.notifyDataSetChanged();
                        saveScheduleList();
                        setScheduleTabMessage();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                String del_list = "", sep="";
                for (int i = 0; i < mGp.syncScheduleListAdapter.getCount(); i++) {
                    if (mGp.syncScheduleListAdapter.getItem(i).isChecked) {
                        del_list +=sep+"-"+mGp.syncScheduleListAdapter.getItem(i).scheduleName;
                        sep="\n";
                    }
                }
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_schedule_confirm_msg_delete), del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonDelete, mContext.getString(R.string.msgs_schedule_cont_label_delete));

        mContextScheduleButtonActivate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleListAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleListAdapter.getItem(i).isChecked && !mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled) {
                                mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled = true;
                                mGp.syncScheduleListAdapter.getItem(i).scheduleLastExecTime = System.currentTimeMillis();
                            }
                        }
                        mGp.syncScheduleListAdapter.setSelectMode(false);
                        mGp.syncScheduleListAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                        saveScheduleList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "", sep="";
                for (int i = 0; i<mGp.syncScheduleListAdapter.getCount(); i++) {
                    if (mGp.syncScheduleListAdapter.getItem(i).isChecked && !mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled) {
                        del_list +=sep+"-"+mGp.syncScheduleListAdapter.getItem(i).scheduleName;
                        sep="\n";
                    }
                }
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_schedule_confirm_msg_enable), del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonActivate, mContext.getString(R.string.msgs_schedule_cont_label_activate));

        mContextScheduleButtonInactivate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        for (int i = mGp.syncScheduleListAdapter.getCount() - 1; i >= 0; i--) {
                            if (mGp.syncScheduleListAdapter.getItem(i).isChecked && mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled) {
                                mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled = false;
                            }
                        }
                        mGp.syncScheduleListAdapter.setSelectMode(false);
                        mGp.syncScheduleListAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                        saveScheduleList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                String del_list = "", sep="";
                for (int i = 0; i<mGp.syncScheduleListAdapter.getCount(); i++) {
                    if (mGp.syncScheduleListAdapter.getItem(i).isChecked && mGp.syncScheduleListAdapter.getItem(i).scheduleEnabled) {
                        del_list +=sep+"-"+mGp.syncScheduleListAdapter.getItem(i).scheduleName;
                        sep="\n";
                    }
                }
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_schedule_confirm_msg_disable), del_list, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonInactivate, mContext.getString(R.string.msgs_schedule_cont_label_inactivate));

        mContextScheduleButtonRename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        mGp.syncScheduleListAdapter.setSelectMode(false);
                        mGp.syncScheduleListAdapter.sort();
                        mGp.syncScheduleListAdapter.unselectAll();
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                        saveScheduleList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                ScheduleListAdapter.ScheduleListItem si = null;
                for (int i = mGp.syncScheduleListAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncScheduleListAdapter.getItem(i).isChecked) {
                        si = mGp.syncScheduleListAdapter.getItem(i);
                        break;
                    }
                }
                if (si==null) {
                    mUtil.addLogMsg("E","", "renameSchedule error, schedule item can not be found.");
                    mUtil.showCommonDialog(false, "E", "renameSchedule error, schedule item can not be found.", "", null);
                } else {
                    renameSchedule(si, ntfy);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonRename, mContext.getString(R.string.msgs_schedule_cont_label_rename));

        mContextScheduleButtonCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        ScheduleListAdapter.ScheduleListItem si = (ScheduleListAdapter.ScheduleListItem) objects[0];
                        mGp.syncScheduleListAdapter.setSelectMode(false);
                        mGp.syncScheduleListAdapter.add(si);
                        mGp.syncScheduleListAdapter.unselectAll();
                        mGp.syncScheduleListAdapter.sort();
                        saveScheduleList();
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                ScheduleListAdapter.ScheduleListItem si = null;
                for (int i = mGp.syncScheduleListAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncScheduleListAdapter.getItem(i).isChecked) {
                        si = mGp.syncScheduleListAdapter.getItem(i);
                        ScheduleListAdapter.ScheduleListItem new_si = si.clone();
                        ScheduleEditor sm = new ScheduleEditor(mUtil, mActivity, mContext, mGp, false, mGp.syncScheduleList, new_si, ntfy);
                        break;
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonCopy, mContext.getString(R.string.msgs_schedule_cont_label_copy));

        mContextScheduleButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mGp.syncScheduleListAdapter.setSelectMode(true);
                mGp.syncScheduleListAdapter.selectAll();
                setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonSelectAll, mContext.getString(R.string.msgs_schedule_cont_label_select_all));

        mContextScheduleButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                mGp.syncTabScheduleAdapter.setSelectMode(false);
                mGp.syncScheduleListAdapter.unselectAll();
                setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextScheduleButtonUnselectAll, mContext.getString(R.string.msgs_schedule_cont_label_unselect_all));
    }

    private void renameSchedule(final ScheduleListAdapter.ScheduleListItem si, final NotifyEvent p_ntfy) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);

//        Drawable db = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.dialog_box_outline, null);
//        ll_dlg_view.setBackground(db);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        dlg_msg.setVisibility(TextView.VISIBLE);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etInput = (EditText) dialog.findViewById(R.id.single_item_input_dir);

        title.setText(mContext.getString(R.string.msgs_schedule_rename_schedule));

        dlg_cmp.setVisibility(TextView.GONE);
        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);
        etInput.setText(si.scheduleName);
        dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_duplicate_name));
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (!arg0.toString().equalsIgnoreCase(si.scheduleName)) {
                    String e_msg=ScheduleEditor.hasScheduleNameUnusableCharacter(mContext, arg0.toString());
                    if (e_msg.equals("")) {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_warning));
                    } else {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        dlg_msg.setText(e_msg);
                    }
                } else {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    dlg_msg.setText(mContext.getString(R.string.msgs_schedule_confirm_msg_rename_duplicate_name));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        });

        //OK button
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                String new_name = etInput.getText().toString();

                si.scheduleName = new_name;

                p_ntfy.notifyToListener(true, null);
            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

    }

    private String checkExecuteScheduleConditions(ScheduleListAdapter.ScheduleListItem sched_item) {
        String e_msg="";
        if (sched_item.syncOverrideOptionCharge.equals(ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_ENABLED)) {
            if (!CommonUtilities.isCharging(mContext, mUtil)) {
                e_msg=mContext.getString(R.string.msgs_mirror_sync_cancelled_battery_option_not_satisfied);
            }
        }
        return e_msg;
    }

    private void setScheduleViewListener() {
        NotifyEvent ntfy_cb = new NotifyEvent(mContext);
        ntfy_cb.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncScheduleListAdapter.setCbNotify(ntfy_cb);

        NotifyEvent ntfy_sw = new NotifyEvent(mContext);
        ntfy_sw.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                int pos=(int)objects[0];
                if (mGp.syncScheduleListAdapter.getItem(pos).scheduleEnabled) {
                    mGp.syncScheduleListAdapter.getItem(pos).scheduleLastExecTime = System.currentTimeMillis();
                }
                saveScheduleList();
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncScheduleListAdapter.setSwNotify(ntfy_sw);

        NotifyEvent ntfy_sync = new NotifyEvent(mContext);
        ntfy_sync.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ScheduleListAdapter.ScheduleListItem sched_item=(ScheduleListAdapter.ScheduleListItem)objects[0];
                String e_msg= checkExecuteScheduleConditions(sched_item);
                if (e_msg.equals("")) {
                    try {
                        mSvcClient.aidlStartSchedule(new String[]{sched_item.scheduleName});
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_schedule_sync_task_start_error), e_msg, null);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mGp.syncScheduleListAdapter.setSyncNotify(ntfy_sync);

        mGp.syncScheduleView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isUiEnabled()) {
                    if (mGp.syncScheduleListAdapter.isSelectMode()) {
                        if (mGp.syncScheduleListAdapter.getItem(i).isChecked) {
                            mGp.syncScheduleListAdapter.getItem(i).isChecked = false;
                        } else {
                            mGp.syncScheduleListAdapter.getItem(i).isChecked = true;
                        }
                        mGp.syncScheduleListAdapter.notifyDataSetChanged();
                        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                    } else {
                        NotifyEvent ntfy = new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                saveScheduleList();
                                mGp.syncScheduleListAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {
                            }
                        });
                        ScheduleEditor sm = new ScheduleEditor(mUtil, mActivity, mContext, mGp, true, mGp.syncScheduleList, mGp.syncScheduleList.get(i), ntfy);
                    }
                }
            }
        });

        mGp.syncScheduleView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isUiEnabled()) {
                    mGp.syncScheduleListAdapter.setSelectMode(true);
                    mGp.syncScheduleListAdapter.getItem(i).isChecked = !mGp.syncScheduleListAdapter.getItem(i).isChecked;
                    mGp.syncScheduleListAdapter.notifyDataSetChanged();
                    setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
                }
                return true;
            }
        });

    }

    private void saveScheduleList() {
        mGp.syncScheduleListAdapter.sort();
        Thread th=new Thread() {
            @Override
            public void run() {
                String config_data= TaskListImportExport.saveTaskListToAppDirectory(mContext, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                if (config_data!=null) TaskListImportExport.saveTaskListToAutosave(mActivity, mContext, mGp.settingAppManagemsntDirectoryName, config_data);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ScheduleUtils.sendTimerRequest(mContext, SCHEDULE_INTENT_SET_TIMER);
                        ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
                    }
                });
            }
        };
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
    }

    private void setScheduleContextButtonNormalMode() {
        setScheduleContextButtonMode(mGp.syncScheduleListAdapter);
    }

    private void setScheduleContextButtonMode(ScheduleListAdapter adapter) {
        boolean selected = false;
        int sel_cnt = 0;
        boolean enabled = false, disabled = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).isChecked) {
                selected = true;
                sel_cnt++;
                if (adapter.getItem(i).scheduleEnabled) enabled = true;
                else disabled = true;
            }
        }

        setContextButtonVisibility(mContextScheduleButtonAddView,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextScheduleButtonActivateView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextScheduleButtonInactivateView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextScheduleButtonCopyView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextScheduleButtonRenameView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextScheduleButtonDeleteView,LinearLayout.INVISIBLE);
        setContextButtonVisibility(mContextScheduleButtonSelectAllView,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextScheduleButtonUnselectAllView,LinearLayout.INVISIBLE);

        if (adapter.isSelectMode()) {
            if (sel_cnt == 0) {
                setContextButtonVisibility(mContextScheduleButtonAddView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonActivateView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonInactivateView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonDeleteView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonUnselectAllView,LinearLayout.INVISIBLE);
            } else if (sel_cnt == 1) {
                setContextButtonVisibility(mContextScheduleButtonAddView,LinearLayout.INVISIBLE);
                if (disabled) setContextButtonVisibility(mContextScheduleButtonActivateView,LinearLayout.VISIBLE);
                if (enabled) setContextButtonVisibility(mContextScheduleButtonInactivateView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonCopyView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonRenameView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonDeleteView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonUnselectAllView,LinearLayout.VISIBLE);
            } else if (sel_cnt >= 2) {
                setContextButtonVisibility(mContextScheduleButtonAddView,LinearLayout.INVISIBLE);
                if (disabled) setContextButtonVisibility(mContextScheduleButtonActivateView,LinearLayout.VISIBLE);
                if (enabled) setContextButtonVisibility(mContextScheduleButtonInactivateView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonCopyView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonRenameView,LinearLayout.INVISIBLE);
                setContextButtonVisibility(mContextScheduleButtonDeleteView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonSelectAllView,LinearLayout.VISIBLE);
                setContextButtonVisibility(mContextScheduleButtonUnselectAllView,LinearLayout.VISIBLE);
            }
            int tot_cnt = mGp.syncScheduleListAdapter.getCount();
            setActionBarSelectMode(sel_cnt, tot_cnt);
        } else {
            setContextButtonVisibility(mContextScheduleButtonAddView,LinearLayout.VISIBLE);
            setContextButtonVisibility(mContextScheduleButtonActivateView,LinearLayout.INVISIBLE);
            setContextButtonVisibility(mContextScheduleButtonInactivateView,LinearLayout.INVISIBLE);
            setContextButtonVisibility(mContextScheduleButtonCopyView,LinearLayout.INVISIBLE);
            setContextButtonVisibility(mContextScheduleButtonRenameView,LinearLayout.INVISIBLE);
            setContextButtonVisibility(mContextScheduleButtonDeleteView,LinearLayout.INVISIBLE);
            setContextButtonVisibility(mContextScheduleButtonUnselectAllView,LinearLayout.INVISIBLE);
            if (adapter.getCount() == 0) {
                setContextButtonVisibility(mContextScheduleButtonSelectAllView,LinearLayout.INVISIBLE);
            }
            setActionBarNormalMode();
        }

    }

    private boolean canListViewScrollDown(ListView lv) {
        boolean result=false;
        if (lv.getLastVisiblePosition()<(lv.getCount()-1)) result=true;
        return result;
    }

    private boolean canListViewScrollUp(ListView lv) {
        boolean result=false;
        if (lv.getFirstVisiblePosition()>0) result=true;
        return result;
    }

    private void setHistoryScrollButtonVisibility() {
        Handler hndl=new Handler();
        hndl.post(new Runnable(){
            @Override
            public void run() {
                if (canListViewScrollDown(mGp.syncHistoryView)) {
                    mContextHistoryButtonScrollDown.setVisibility(LinearLayout.VISIBLE);
                    mContextHistoryButtonPageDown.setVisibility(LinearLayout.VISIBLE);
//                    mContextHistoryButtonMoveBottom.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextHistoryButtonScrollDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextHistoryButtonPageDown.setVisibility(LinearLayout.INVISIBLE);
//                    mContextHistoryButtonMoveBottom.setVisibility(LinearLayout.INVISIBLE);
                }
                if (canListViewScrollUp(mGp.syncHistoryView)) {
                    mContextHistoryButtonScrollUp.setVisibility(LinearLayout.VISIBLE);
                    mContextHistoryButtonPageUp.setVisibility(LinearLayout.VISIBLE);
//                    mContextHistoryButtonMoveTop.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextHistoryButtonScrollUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextHistoryButtonPageUp.setVisibility(LinearLayout.INVISIBLE);
//                    mContextHistoryButtonMoveTop.setVisibility(LinearLayout.INVISIBLE);
                }
            }
        });
    }

    private final static int HISTORY_SCROLL_AMOUNT=1;
    private void setHistoryContextButtonListener() {
        setHistoryScrollButtonVisibility();
        mGp.syncHistoryView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                setHistoryScrollButtonVisibility();
            }
        });

        mContextHistoryButtonScrollUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncHistoryView.getFirstVisiblePosition() - HISTORY_SCROLL_AMOUNT;
                if (sel > mGp.syncHistoryListAdapter.getCount() - 1) sel = mGp.syncHistoryListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncHistoryListAdapter.notifyDataSetChanged();
                mGp.syncHistoryView.setSelection(sel);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonScrollDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncHistoryView.getFirstVisiblePosition() + HISTORY_SCROLL_AMOUNT;
                if (sel > mGp.syncHistoryListAdapter.getCount() - 1) sel = mGp.syncHistoryListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncHistoryListAdapter.notifyDataSetChanged();
                mGp.syncHistoryView.setSelection(sel);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonPageUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int lv_height = mGp.syncHistoryView.getHeight();
                int first_item_y_top =  mGp.syncHistoryView.getChildAt(0).getTop();
                int first_item_y_bottom =  mGp.syncHistoryView.getChildAt(0).getBottom();
                int first_item_height = first_item_y_bottom - first_item_y_top;
                int y_offset = 0;
                if (first_item_y_top < 0) {
                    // part of first item is hidden on top
                    y_offset = first_item_height;
                    if (y_offset > lv_height) {
                        //item is more than one page: position to the bottom, the current top exact last visible position, minus 3 text lines
                        TextView listTextView = (TextView) mGp.syncHistoryView.getChildAt(0).findViewById(R.id.history_list_view_date);
                        int text_context_size = 0;
                        if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                        y_offset = first_item_height - first_item_y_bottom + text_context_size;
                    }
                }
                mGp.syncHistoryView.setItemChecked(mGp.syncHistoryView.getFirstVisiblePosition(), true);//needed on app start to set touch focus
                mGp.syncHistoryView.setSelectionFromTop(mGp.syncHistoryView.getFirstVisiblePosition(), lv_height - y_offset);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonPageDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, DEFAULT_LONG_PRESS_REPEAT_INTERVAL, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int last_item_pos = mGp.syncHistoryView.getLastVisiblePosition() - mGp.syncHistoryView.getFirstVisiblePosition();
                int lv_height = mGp.syncHistoryView.getHeight();
                int last_item_y_top =  mGp.syncHistoryView.getChildAt(last_item_pos).getTop();
                int last_item_y_bottom =  mGp.syncHistoryView.getChildAt(last_item_pos).getBottom();
                int last_item_height = last_item_y_bottom - last_item_y_top;
                int y_offset = 0;

                if (last_item_height > lv_height) {
                    //item is more than one page: position to the top, the current bottom exat last visible position, minus 3 text lines
                    TextView listTextView = (TextView) mGp.syncHistoryView.getChildAt(last_item_pos).findViewById(R.id.history_list_view_date);
                    int text_context_size = 0;
                    if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                    y_offset = -(lv_height - last_item_y_top - text_context_size);
                }

                mGp.syncHistoryView.setItemChecked(mGp.syncHistoryView.getLastVisiblePosition(), true);
                mGp.syncHistoryView.setSelectionFromTop(mGp.syncHistoryView.getLastVisiblePosition(), y_offset);
                setHistoryScrollButtonVisibility();
            }
        }));

        mContextHistoryButtonSendTo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonSendTo, false);
                if (isUiEnabled()) {
                    sendHistoryFile();
                    mGp.syncHistoryListAdapter.setAllItemChecked(false);
                    mGp.syncHistoryListAdapter.setShowCheckBox(false);
                    mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonSendTo, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonSendTo, mContext.getString(R.string.msgs_hist_cont_label_share));

//        mContextHistoryButtonMoveTop.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setContextButtonEnabled(mContextHistoryButtonMoveTop, false);
//                mGp.syncHistoryAdapter.notifyDataSetChanged();
//                mGp.syncHistoryListView.setSelection(0);
//                setContextButtonEnabled(mContextHistoryButtonMoveTop, true);
//            }
//        });
//        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonMoveTop, mContext.getString(R.string.msgs_hist_cont_label_move_top));
//
//        mContextHistoryButtonMoveBottom.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setContextButtonEnabled(mContextHistoryButtonMoveBottom, false);
//                mGp.syncHistoryAdapter.notifyDataSetChanged();
//                mGp.syncHistoryListView.setSelection(mGp.syncHistoryAdapter.getCount() - 1);
//                setContextButtonEnabled(mContextHistoryButtonMoveBottom, true);
//            }
//        });
//        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonMoveBottom, mContext.getString(R.string.msgs_hist_cont_label_move_bottom));

        mContextHistoryButtonDeleteHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    confirmDeleteHistory();
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonDeleteHistory, mContext.getString(R.string.msgs_hist_cont_label_delete));

        mContextHistoryButtonHistiryCopyClipboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, false);
                if (isUiEnabled()) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    StringBuilder out = new StringBuilder(256);
                    for (int i = 0; i < mGp.syncHistoryListAdapter.getCount(); i++) {
                        if (mGp.syncHistoryListAdapter.getItem(i).isChecked) {
                            HistoryListAdapter.HistoryListItem hli = mGp.syncHistoryListAdapter.getItem(i);
                            out.append(hli.sync_date).append(" ");
                            out.append(hli.sync_time).append(" ");
                            out.append(hli.sync_task).append("\n");
                            if (hli.sync_status == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_SUCCESS) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_success)).append("\n");
                            } else if (hli.sync_status == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_error)).append("\n");
                            } else if (hli.sync_status == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_CANCEL) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_cancel)).append("\n");
                            }
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_copied))
                                    .append(Integer.toString(hli.sync_result_no_of_copied)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_deleted))
                                    .append(Integer.toString(hli.sync_result_no_of_deleted)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_ignored))
                                    .append(Integer.toString(hli.sync_result_no_of_ignored)).append(" ");
                            out.append("\n").append(hli.sync_error_text);
                        }
                    }
                    if (out.length() > 0) cm.setText(out);
                    CommonDialog.showPopupMessageAsUpAnchorViewShort(mActivity, mContextHistiryViewHistoryCopyClipboard,
                            mContext.getString(R.string.msgs_main_sync_history_copy_completed));
                    mGp.syncHistoryListAdapter.setAllItemChecked(false);
                    mGp.syncHistoryListAdapter.setShowCheckBox(false);
                    mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonHistiryCopyClipboard, mContext.getString(R.string.msgs_hist_cont_label_copy));

        mContextHistoryButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistoryButtonSelectAll, false);
                    setHistoryItemSelectAll();
                    mGp.syncHistoryListAdapter.setShowCheckBox(true);
                    setHistoryContextButtonSelectMode();
                    setContextButtonEnabled(mContextHistoryButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistoryButtonSelectAll, mContext.getString(R.string.msgs_hist_cont_label_select_all));

        mContextHistiryButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, false);
                    setHistoryItemUnselectAll();
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextHistiryButtonUnselectAll, mContext.getString(R.string.msgs_hist_cont_label_unselect_all));
    }

    private void setHistoryContextButtonSelectMode() {
        int sel_cnt = mGp.syncHistoryListAdapter.getItemSelectedCount();
        int tot_cnt = mGp.syncHistoryListAdapter.getCount();
        setActionBarSelectMode(sel_cnt, tot_cnt);

//        setContextButtonVisibility(mContextHistiryViewMoveTop,ImageButton.VISIBLE);
//        setContextButtonVisibility(mContextHistiryViewMoveBottom,ImageButton.VISIBLE);

        if (sel_cnt > 0) {
            setContextButtonVisibility(mContextHistiryViewShare,ImageButton.VISIBLE);
            setContextButtonVisibility(mContextHistiryViewDeleteHistory,ImageButton.VISIBLE);
            setContextButtonVisibility(mContextHistiryViewHistoryCopyClipboard,ImageButton.VISIBLE);
            setContextButtonVisibility(mContextHistiryViewUnselectAll,ImageButton.VISIBLE);
        } else {
            setContextButtonVisibility(mContextHistiryViewShare,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewDeleteHistory,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewHistoryCopyClipboard,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewUnselectAll,ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) setContextButtonVisibility(mContextHistiryViewSelectAll,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextHistiryViewSelectAll,ImageButton.INVISIBLE);

    }

    private void setHistoryContextButtonNormalMode() {
        setActionBarNormalMode();

        if (!mGp.syncHistoryListAdapter.isEmptyAdapter()) {
            setContextButtonVisibility(mContextHistiryViewShare,ImageButton.INVISIBLE);
//            setContextButtonVisibility(mContextHistiryViewMoveTop,ImageButton.INVISIBLE);
//            setContextButtonVisibility(mContextHistiryViewMoveBottom,ImageButton.VISIBLE);
            setContextButtonVisibility(mContextHistiryViewDeleteHistory,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewHistoryCopyClipboard,ImageButton.INVISIBLE);
            if (isUiEnabled()) setContextButtonVisibility(mContextHistiryViewSelectAll,ImageButton.VISIBLE);
            else setContextButtonVisibility(mContextHistiryViewSelectAll,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewUnselectAll,ImageButton.INVISIBLE);
        } else {
            setContextButtonVisibility(mContextHistiryViewShare,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewShare,ImageButton.INVISIBLE);
//            setContextButtonVisibility(mContextHistiryViewMoveTop,ImageButton.INVISIBLE);
//            setContextButtonVisibility(mContextHistiryViewMoveBottom,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewDeleteHistory,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewHistoryCopyClipboard,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewSelectAll,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextHistiryViewUnselectAll,ImageButton.INVISIBLE);
        }
    }

    private void setHistoryItemUnselectAll() {
        mGp.syncHistoryListAdapter.setAllItemChecked(false);
        mGp.syncHistoryListAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    private void setHistoryItemSelectAll() {
        mGp.syncHistoryListAdapter.setAllItemChecked(true);
        mGp.syncHistoryListAdapter.setShowCheckBox(true);
        mGp.syncHistoryListAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    private void confirmDeleteHistory() {
        String conf_list = "";
        boolean del_all_history = false;
        int del_cnt = 0;
        String sep = "";
        for (int i = 0; i < mGp.syncHistoryListAdapter.getCount(); i++) {
            if (mGp.syncHistoryListAdapter.getItem(i).isChecked) {
                del_cnt++;
                conf_list += sep + mGp.syncHistoryListAdapter.getItem(i).sync_date + " " +
                        mGp.syncHistoryListAdapter.getItem(i).sync_time + " " +
                        mGp.syncHistoryListAdapter.getItem(i).sync_task + " ";
                sep = "\n";
            }
        }
        if (del_cnt == mGp.syncHistoryListAdapter.getCount()) del_all_history = true;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                for (int i = mGp.syncHistoryListAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncHistoryListAdapter.getItem(i).isChecked) {
                        String result_fp = mGp.syncHistoryListAdapter.getItem(i).sync_result_file_path;
                        if (!result_fp.equals("")) {
                            SafFile3 lf = new SafFile3(mContext, result_fp);
                            if (lf.exists()) {
                                lf.delete();
                                mUtil.addDebugMsg(1, "I", "Sync history log file deleted, fp=" + result_fp);
                            }
                        }
                        mUtil.addDebugMsg(1, "I", "Sync history item deleted, item=" + mGp.syncHistoryListAdapter.getItem(i).sync_task);
                        mGp.syncHistoryListAdapter.remove(mGp.syncHistoryListAdapter.getItem(i));
                    }
                }
                mUtil.saveHistoryList(mGp.syncHistoryList);
                mGp.syncHistoryListAdapter.setShowCheckBox(false);
                mGp.syncHistoryListAdapter.notifyDataSetChanged();
                setHistoryContextButtonNormalMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });

        if (del_all_history) {
            mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_all_history), "", ntfy);
        } else {
            mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_selected_history), conf_list, ntfy);
        }
    }

    private void setSyncTaskViewListener() {
        mGp.syncTaskView.setEnabled(true);
        mGp.syncTaskView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isUiEnabled()) {
                    mGp.syncTaskView.setEnabled(false);
                    SyncTaskItem item = mGp.syncTaskListAdapter.getItem(position);
                    if (!mGp.syncTaskListAdapter.isShowCheckBox()) {
                        editSyncTask(item.getSyncTaskName(), item.isSyncTaskAuto(), position);
                        mUiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mGp.syncTaskView.setEnabled(true);
                            }
                        }, 1000);
                    } else {
                        item.setChecked(!item.isChecked());
                        setSyncTaskContextButtonSelectMode();
                        mGp.syncTaskView.setEnabled(true);
                        mGp.syncTaskListAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        NotifyEvent ntfy_cb = new NotifyEvent(mContext);
        ntfy_cb.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (!mGp.syncTaskListAdapter.isShowCheckBox()) {
                    mGp.syncTaskListAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonNormalMode();
                } else {
                    setSyncTaskContextButtonSelectMode();
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        mGp.syncTaskListAdapter.setNotifyCheckBoxEventHandler(ntfy_cb);

        NotifyEvent ntfy_sync = new NotifyEvent(mContext);
        ntfy_sync.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (isUiEnabled()) {
                    SyncTaskItem sti=(SyncTaskItem)o[0];
                    syncSpecificSyncTask(sti);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        mGp.syncTaskListAdapter.setNotifySyncButtonEventHandler(ntfy_sync);

        mGp.syncTaskView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> list_view, final View item_view,
                                           int pos, long arg3) {
                if (mGp.syncTaskListAdapter.isEmptyAdapter()) return true;
                if (!isUiEnabled()) return true;

                if (!mGp.syncTaskListAdapter.getItem(pos).isChecked()) {
                    if (TaskListUtils.isSyncTaskSelected(mGp.syncTaskListAdapter)) {

                        int down_sel_pos = -1, up_sel_pos = -1;
                        int tot_cnt = mGp.syncTaskListAdapter.getCount();
                        if (pos + 1 <= tot_cnt) {
                            for (int i = pos + 1; i < tot_cnt; i++) {
                                if (mGp.syncTaskListAdapter.getItem(i).isChecked()) {
                                    up_sel_pos = i;
                                    break;
                                }
                            }
                        }
                        if (pos > 0) {
                            for (int i = pos; i >= 0; i--) {
                                if (mGp.syncTaskListAdapter.getItem(i).isChecked()) {
                                    down_sel_pos = i;
                                    break;
                                }
                            }
                        }
                        if (up_sel_pos != -1 && down_sel_pos == -1) {
                            for (int i = pos; i < up_sel_pos; i++)
                                mGp.syncTaskListAdapter.getItem(i).setChecked(true);
                        } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                mGp.syncTaskListAdapter.getItem(i).setChecked(true);
                        } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                            for (int i = down_sel_pos + 1; i <= pos; i++)
                                mGp.syncTaskListAdapter.getItem(i).setChecked(true);
                        }
                        mGp.syncTaskListAdapter.notifyDataSetChanged();
                    } else {
                        mGp.syncTaskListAdapter.setShowCheckBox(true);
                        mGp.syncTaskListAdapter.getItem(pos).setChecked(true);
                        mGp.syncTaskListAdapter.notifyDataSetChanged();
                    }
                    setSyncTaskContextButtonSelectMode();
                }
                return true;
            }
        });

    }

    private LinearLayout mContextSyncTaskContextView=null;
    private ImageButton mContextSyncTaskButtonToAuto = null;
    private ImageButton mContextSyncTaskButtonToManual = null;
    private ImageButton mContextSyncTaskButtonAddSync = null;
    private ImageButton mContextSyncTaskButtonCopySyncTask = null;
    private ImageButton mContextSyncTaskButtonDeleteSyncTask = null;
    private ImageButton mContextSyncTaskButtonRenameSyncTask = null;
    private ImageButton mContextSyncTaskButtonMoveToUp = null;
    private ImageButton mContextSyncTaskButtonMoveToDown = null;
    private ImageButton mContextSyncTaskButtonSelectAll = null;
    private ImageButton mContextSyncTaskButtonUnselectAll = null;

    private LinearLayout mContextSyncTaskViewToAuto = null;
    private LinearLayout mContextSyncTaskViewToManual = null;
    private LinearLayout mContextSyncTaskViewAddSync = null;
    private LinearLayout mContextSyncTaskViewCopySyncTask = null;
    private LinearLayout mContextSyncTaskViewDeleteSyncTask = null;
    private LinearLayout mContextSyncTaskViewRenameSyncTask = null;
    private LinearLayout mContextSyncTaskViewMoveToUp = null;
    private LinearLayout mContextSyncTaskViewMoveToDown = null;
    private LinearLayout mContextSyncTaskViewSelectAll = null;
    private LinearLayout mContextSyncTaskViewUnselectAll = null;

    private ImageButton mContextHistoryButtonSendTo = null;
//    private ImageButton mContextHistoryButtonMoveTop = null;
//    private ImageButton mContextHistoryButtonMoveBottom = null;
    private ImageButton mContextHistoryButtonScrollDown = null;
    private ImageButton mContextHistoryButtonScrollUp = null;
    private ImageButton mContextHistoryButtonPageDown = null;
    private ImageButton mContextHistoryButtonPageUp = null;
    private ImageButton mContextHistoryButtonDeleteHistory = null;
    private ImageButton mContextHistoryButtonHistiryCopyClipboard = null;
    private ImageButton mContextHistoryButtonSelectAll = null;
    private ImageButton mContextHistiryButtonUnselectAll = null;

    private LinearLayout mContextHistiryViewShare = null;
//    private LinearLayout mContextHistiryViewMoveTop = null;
//    private LinearLayout mContextHistiryViewMoveBottom = null;
    private LinearLayout mContextHistiryViewScrollDown = null;
    private LinearLayout mContextHistiryViewScrollUp = null;
    private LinearLayout mContextHistiryViewPageDown = null;
    private LinearLayout mContextHistiryViewPageUp = null;
    private LinearLayout mContextHistiryViewDeleteHistory = null;
    private LinearLayout mContextHistiryViewHistoryCopyClipboard = null;
    private LinearLayout mContextHistiryViewSelectAll = null;
    private LinearLayout mContextHistiryViewUnselectAll = null;

    private ImageButton mContextScheduleButtonAdd = null;
    private ImageButton mContextScheduleButtonActivate = null;
    private ImageButton mContextScheduleButtonInactivate = null;
    private ImageButton mContextScheduleButtonCopy = null;
    private ImageButton mContextScheduleButtonRename = null;
    private ImageButton mContextScheduleButtonDelete = null;
    private ImageButton mContextScheduleButtonSelectAll = null;
    private ImageButton mContextScheduleButtonUnselectAll = null;

    private LinearLayout mContextScheduleView = null;

    private LinearLayout mContextScheduleButtonAddView = null;
    private LinearLayout mContextScheduleButtonActivateView = null;
    private LinearLayout mContextScheduleButtonInactivateView = null;
    private LinearLayout mContextScheduleButtonCopyView = null;
    private LinearLayout mContextScheduleButtonRenameView = null;
    private LinearLayout mContextScheduleButtonDeleteView = null;
    private LinearLayout mContextScheduleButtonSelectAllView = null;
    private LinearLayout mContextScheduleButtonUnselectAllView = null;

    private ImageButton mContextGroupButtonAdd = null;
//    private ImageButton mContextGroupButtonActivate = null;
//    private ImageButton mContextGroupButtonInactivate = null;
    private ImageButton mContextGroupButtonCopy = null;
    private ImageButton mContextGroupButtonRename = null;
    private ImageButton mContextGroupButtonDelete = null;
    private ImageButton mContextGroupButtonSelectAll = null;
    private ImageButton mContextGroupButtonUnselectAll = null;
    private ImageButton mContextGroupButtonMoveToUp = null;
    private ImageButton mContextGroupButtonMoveToDown = null;
    private ImageButton mContextGroupButtonToEnabled = null;
    private ImageButton mContextGroupButtonToDisabled = null;

    private LinearLayout mContextGroupView = null;

    private LinearLayout mContextGroupButtonAddView = null;
//    private LinearLayout mContextGroupButtonActivateView = null;
//    private LinearLayout mContextGroupButtonInactivateView = null;
    private LinearLayout mContextGroupButtonCopyView = null;
    private LinearLayout mContextGroupButtonRenameView = null;
    private LinearLayout mContextGroupButtonDeleteView = null;
    private LinearLayout mContextGroupButtonSelectAllView = null;
    private LinearLayout mContextGroupButtonUnselectAllView = null;
    private LinearLayout mContextGroupButtonMoveToUpView = null;
    private LinearLayout mContextGroupButtonMoveToDownView = null;
    private LinearLayout mContextGroupButtonToEnabledView = null;
    private LinearLayout mContextGroupButtonToDisabledView = null;

    private ImageButton mContextMessageButtonMoveTop = null;
    private ImageButton mContextMessageButtonPinned = null;
    private ImageButton mContextMessageButtonMoveBottom = null;
    private ImageButton mContextMessageButtonScrollDown = null;
    private ImageButton mContextMessageButtonScrollUp = null;
    private ImageButton mContextMessageButtonPageDown = null;
    private ImageButton mContextMessageButtonPageUp = null;
    private ImageButton mContextMessageButtonClear = null;

    private LinearLayout mContextMessageViewMoveTop = null;
    private LinearLayout mContextMessageViewPinned = null;
    private LinearLayout mContextMessageViewMoveBottom = null;
    private LinearLayout mContextMessageViewScrollDown = null;
    private LinearLayout mContextMessageViewScrollUp = null;
    private LinearLayout mContextMessageViewPageDown = null;
    private LinearLayout mContextMessageViewPageUp = null;
    private LinearLayout mContextMessageViewClear = null;

    private void releaseImageBtnRes(ImageButton ib) {
        ib.setImageDrawable(null);
        ib.setBackgroundDrawable(null);
        ib.setImageBitmap(null);
    }

    private void createContextView() {
        mContextSyncTaskButtonToAuto = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_auto_task);
        if (mGp.themeColorList.theme_is_light) mContextSyncTaskButtonToAuto.setImageResource(R.drawable.smbsync_auto_task_black);
        else mContextSyncTaskButtonToAuto.setImageResource(R.drawable.smbsync_auto_task);
        mContextSyncTaskContextView=(LinearLayout) mSyncTaskView.findViewById(R.id.context_view_task);
        mContextSyncTaskButtonToManual = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_inactivate);
        mContextSyncTaskButtonAddSync = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_add_sync);
        mContextSyncTaskButtonCopySyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_copy);
        mContextSyncTaskButtonDeleteSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_delete);
        mContextSyncTaskButtonRenameSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_rename);
        mContextSyncTaskButtonMoveToUp = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_up_arrow);
        mContextSyncTaskButtonMoveToDown = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_down_arrow);
        mContextSyncTaskButtonSelectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_select_all);
        mContextSyncTaskButtonUnselectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_unselect_all);

        mContextSyncTaskViewToAuto = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_auto_task_view);
        mContextSyncTaskViewToManual = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_inactivate_view);
        mContextSyncTaskViewAddSync = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_add_sync_view);
        mContextSyncTaskViewCopySyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_copy_view);
        mContextSyncTaskViewDeleteSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_delete_view);
        mContextSyncTaskViewRenameSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_rename_view);
        mContextSyncTaskViewMoveToUp = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_up_arrow_view);
        mContextSyncTaskViewMoveToDown = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_down_arrow_view);

        mContextSyncTaskViewSelectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_select_all_view);
        mContextSyncTaskViewUnselectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_unselect_all_view);

        mContextScheduleView=(LinearLayout)mScheduleView.findViewById(R.id.context_view_schedule);

        mContextScheduleButtonAddView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_add_view);
        mContextScheduleButtonActivateView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_activate_view);
        mContextScheduleButtonInactivateView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_inactivate_view);
        mContextScheduleButtonCopyView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_copy_view);
        mContextScheduleButtonRenameView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_rename_view);
        mContextScheduleButtonDeleteView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_delete_view);
        mContextScheduleButtonSelectAllView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_select_all_view);
        mContextScheduleButtonUnselectAllView = (LinearLayout) mScheduleView.findViewById(R.id.context_button_unselect_all_view);

        mContextScheduleButtonAdd = (ImageButton) mScheduleView.findViewById(R.id.context_button_add);
        mContextScheduleButtonActivate = (ImageButton) mScheduleView.findViewById(R.id.context_button_activate);
        mContextScheduleButtonInactivate = (ImageButton) mScheduleView.findViewById(R.id.context_button_inactivate);
        mContextScheduleButtonCopy = (ImageButton) mScheduleView.findViewById(R.id.context_button_copy);
        mContextScheduleButtonRename = (ImageButton) mScheduleView.findViewById(R.id.context_button_rename);
        mContextScheduleButtonDelete = (ImageButton) mScheduleView.findViewById(R.id.context_button_delete);
        mContextScheduleButtonSelectAll = (ImageButton) mScheduleView.findViewById(R.id.context_button_select_all);
        mContextScheduleButtonUnselectAll = (ImageButton) mScheduleView.findViewById(R.id.context_button_unselect_all);

        mContextGroupView=(LinearLayout)mGroupView.findViewById(R.id.context_view_group);

        mContextGroupButtonAddView = (LinearLayout) mGroupView.findViewById(R.id.context_button_add_view);
        mContextGroupButtonCopyView = (LinearLayout) mGroupView.findViewById(R.id.context_button_copy_view);
        mContextGroupButtonRenameView = (LinearLayout) mGroupView.findViewById(R.id.context_button_rename_view);
        mContextGroupButtonDeleteView = (LinearLayout) mGroupView.findViewById(R.id.context_button_delete_view);
        mContextGroupButtonSelectAllView = (LinearLayout) mGroupView.findViewById(R.id.context_button_select_all_view);
        mContextGroupButtonUnselectAllView = (LinearLayout) mGroupView.findViewById(R.id.context_button_unselect_all_view);
        mContextGroupButtonMoveToUpView = (LinearLayout) mGroupView.findViewById(R.id.context_button_up_arrow_view);
        mContextGroupButtonMoveToDownView = (LinearLayout) mGroupView.findViewById(R.id.context_button_down_arrow_view);
        mContextGroupButtonToEnabledView = (LinearLayout) mGroupView.findViewById(R.id.context_button_activate_view);
        mContextGroupButtonToDisabledView = (LinearLayout) mGroupView.findViewById(R.id.context_button_inactivate_view);

        mContextGroupButtonAdd = (ImageButton) mGroupView.findViewById(R.id.context_button_add);
        mContextGroupButtonCopy = (ImageButton) mGroupView.findViewById(R.id.context_button_copy);
        mContextGroupButtonRename = (ImageButton) mGroupView.findViewById(R.id.context_button_rename);
        mContextGroupButtonDelete = (ImageButton) mGroupView.findViewById(R.id.context_button_delete);
        mContextGroupButtonSelectAll = (ImageButton) mGroupView.findViewById(R.id.context_button_select_all);
        mContextGroupButtonUnselectAll = (ImageButton) mGroupView.findViewById(R.id.context_button_unselect_all);
        mContextGroupButtonMoveToUp = (ImageButton) mGroupView.findViewById(R.id.context_button_up_arrow);
        mContextGroupButtonMoveToDown = (ImageButton) mGroupView.findViewById(R.id.context_button_down_arrow);
        mContextGroupButtonToEnabled = (ImageButton) mGroupView.findViewById(R.id.context_button_activate);
        mContextGroupButtonToDisabled = (ImageButton) mGroupView.findViewById(R.id.context_button_inactivate);

        mContextHistoryButtonSendTo = (ImageButton) mHistoryView.findViewById(R.id.context_button_share);
        if (mGp.themeColorList.theme_is_light) mContextHistoryButtonSendTo.setImageResource(R.drawable.context_button_share_dark);
//        mContextHistoryButtonMoveTop = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_top);
//        mContextHistoryButtonMoveBottom = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_bottom);
        mContextHistoryButtonScrollDown = (ImageButton) mHistoryView.findViewById(R.id.context_button_scroll_down);
        mContextHistoryButtonScrollUp = (ImageButton) mHistoryView.findViewById(R.id.context_button_scroll_up);
        mContextHistoryButtonPageDown = (ImageButton) mHistoryView.findViewById(R.id.context_button_page_down);
        mContextHistoryButtonPageUp = (ImageButton) mHistoryView.findViewById(R.id.context_button_page_up);
        mContextHistoryButtonDeleteHistory = (ImageButton) mHistoryView.findViewById(R.id.context_button_delete);
        mContextHistoryButtonHistiryCopyClipboard = (ImageButton) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard);
        mContextHistoryButtonSelectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_select_all);
        mContextHistiryButtonUnselectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_unselect_all);

        mContextHistiryViewShare = (LinearLayout) mHistoryView.findViewById(R.id.context_button_share_view);
//        mContextHistiryViewMoveTop = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_top_view);
//        mContextHistiryViewMoveBottom = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextHistiryViewScrollDown = (LinearLayout) mHistoryView.findViewById(R.id.context_button_scroll_down_view);
        mContextHistiryViewScrollUp = (LinearLayout) mHistoryView.findViewById(R.id.context_button_scroll_up_view);
        mContextHistiryViewPageDown = (LinearLayout) mHistoryView.findViewById(R.id.context_button_page_down_view);
        mContextHistiryViewPageUp = (LinearLayout) mHistoryView.findViewById(R.id.context_button_page_up_view);
        mContextHistiryViewDeleteHistory = (LinearLayout) mHistoryView.findViewById(R.id.context_button_delete_view);
        mContextHistiryViewHistoryCopyClipboard = (LinearLayout) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard_view);
        mContextHistiryViewSelectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_select_all_view);
        mContextHistiryViewUnselectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_unselect_all_view);

        mContextMessageButtonPinned = (ImageButton) mMessageView.findViewById(R.id.context_button_pinned);
        mContextMessageButtonMoveTop = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_top);
        mContextMessageButtonMoveBottom = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_bottom);
        mContextMessageButtonScrollDown = (ImageButton) mMessageView.findViewById(R.id.context_button_scroll_down);
        mContextMessageButtonScrollUp = (ImageButton) mMessageView.findViewById(R.id.context_button_scroll_up);
        mContextMessageButtonPageDown = (ImageButton) mMessageView.findViewById(R.id.context_button_page_down);
        mContextMessageButtonPageUp = (ImageButton) mMessageView.findViewById(R.id.context_button_page_up);
        mContextMessageButtonClear = (ImageButton) mMessageView.findViewById(R.id.context_button_delete);

        mContextMessageViewPinned = (LinearLayout) mMessageView.findViewById(R.id.context_button_pinned_view);
        mContextMessageViewMoveTop = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_top_view);
        mContextMessageViewMoveBottom = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextMessageViewScrollDown = (LinearLayout) mMessageView.findViewById(R.id.context_button_scroll_down_view);
        mContextMessageViewScrollUp = (LinearLayout) mMessageView.findViewById(R.id.context_button_scroll_up_view);
        mContextMessageViewPageDown = (LinearLayout) mMessageView.findViewById(R.id.context_button_page_down_view);
        mContextMessageViewPageUp = (LinearLayout) mMessageView.findViewById(R.id.context_button_page_up_view);
        mContextMessageViewClear = (LinearLayout) mMessageView.findViewById(R.id.context_button_delete_view);
    }

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
        if (enabled) {
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            }, 1000);
        } else {
            btn.setEnabled(false);
        }
    }

    private void setSyncTaskContextButtonListener() {
        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (mGp.syncTaskListAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                else setSyncTaskContextButtonNormalMode();
                ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);
                if (mGp.syncTaskList.size()==0) mGp.syncTaskEmptyMessage.setVisibility(TextView.VISIBLE);
                else mGp.syncTaskEmptyMessage.setVisibility(TextView.GONE);
                mGp.syncScheduleListAdapter.notifyDataSetChanged();
                mGp.syncGroupListAdapter.notifyDataSetChanged();
                setGroupContextButtonMode(mGp.syncGroupListAdapter);
                setGroupTabMessage();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });

        mContextSyncTaskButtonToAuto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmToAuto(mGp.syncTaskListAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonToAuto, mContext.getString(R.string.msgs_task_cont_label_auto));

        mContextSyncTaskButtonToManual.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmToManual(mGp.syncTaskListAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonToManual, mContext.getString(R.string.msgs_task_cont_label_manual));

        mContextSyncTaskButtonAddSync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, false);
                    SyncTaskItem pfli = new SyncTaskItem();
                    pfli.setSyncTaskAuto(true);
                    if (mTaskEditor ==null) mTaskEditor = TaskEditor.newInstance();
                    mTaskEditor.showDialog(getSupportFragmentManager(), mTaskEditor, TaskEditor.TASK_EDIT_METHOD_ADD, pfli, mTaskUtil, mUtil, mGp, ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonAddSync, mContext.getString(R.string.msgs_task_cont_label_add_sync));

        mContextSyncTaskButtonCopySyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, false);
                    for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
                        final SyncTaskItem item = mGp.syncTaskListAdapter.getItem(i);
                        if (item.isChecked()) {
                            NotifyEvent ntfy_check = new NotifyEvent(mContext);
                            ntfy_check.setListener(new NotifyEvent.NotifyEventListener() {
                                @Override
                                public void positiveResponse(Context c, Object[] o) {
                                    SyncTaskItem npfli = item.clone();
                                    npfli.setLastSyncResult(0);
                                    npfli.setLastSyncTime("");
                                    if (mTaskEditor ==null) mTaskEditor = TaskEditor.newInstance();
                                    mTaskEditor.showDialog(getSupportFragmentManager(), mTaskEditor, TaskEditor.TASK_EDIT_METHOD_COPY, npfli, mTaskUtil, mUtil, mGp, ntfy);
                                }
                                @Override
                                public void negativeResponse(Context c, Object[] o) {}
                            });
                            ApplicationPassword.authentication(mGp, mActivity, mActivity.getSupportFragmentManager(),
                                    mUtil, false, ntfy_check, ApplicationPassword.APPLICATION_PASSWORD_RESOURCE_EDIT_SYNC_TASK);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonCopySyncTask, mContext.getString(R.string.msgs_task_cont_label_copy));

        mContextSyncTaskButtonDeleteSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, false);
                    mTaskUtil.deleteSyncTask(ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonDeleteSyncTask, mContext.getString(R.string.msgs_task_cont_label_delete));

        mContextSyncTaskButtonRenameSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, false);
                    for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskListAdapter.getItem(i);
                        if (item.isChecked()) {
                            mTaskUtil.renameSyncTask(item, ntfy);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonMoveToUp, mContext.getString(R.string.msgs_task_cont_label_up));

        mContextSyncTaskButtonMoveToUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskListAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (c_pos > 0) {
                                for (int j = 0; j < mGp.syncTaskListAdapter.getCount(); j++) {
                                    if (mGp.syncTaskListAdapter.getItem(j).getSyncTaskPosition() == (c_pos - 1)) {
                                        mGp.syncTaskListAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos - 1);
                                mGp.syncGroupListAdapter.sort();
                                mGp.syncGroupListAdapter.notifyDataSetChanged();
                                saveGroupList();

                                if (item.getSyncTaskPosition() == 0) {
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskListAdapter.getCount() - 1)) {
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.VISIBLE);
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextGroupButtonMoveToDown, mContext.getString(R.string.msgs_task_cont_label_down));

        mContextSyncTaskButtonMoveToDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskListAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (item.getSyncTaskPosition() < (mGp.syncTaskListAdapter.getCount() - 1)) {
                                for (int j = 0; j < mGp.syncTaskListAdapter.getCount(); j++) {
                                    if (mGp.syncTaskListAdapter.getItem(j).getSyncTaskPosition() == (c_pos + 1)) {
                                        mGp.syncTaskListAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos + 1);
                                mGp.syncTaskListAdapter.sort();
                                TaskListImportExport.saveTaskListToAppDirectory(mContext, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                                mGp.syncTaskListAdapter.notifyDataSetChanged();

                                if (item.getSyncTaskPosition() == 0) {
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskListAdapter.getCount() - 1)) {
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.VISIBLE);
                                    setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
                                }

                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonMoveToDown, mContext.getString(R.string.msgs_task_cont_label_down));

        mContextSyncTaskButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, false);
                    for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
                        mGp.syncTaskListAdapter.getItem(i).setChecked(true);
                    }
                    mGp.syncTaskListAdapter.notifyDataSetChanged();
                    mGp.syncTaskListAdapter.setShowCheckBox(true);
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonSelectAll, mContext.getString(R.string.msgs_task_cont_label_select_all));

        mContextSyncTaskButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, false);
                    TaskListUtils.setAllSyncTaskToUnchecked(false, mGp.syncTaskListAdapter);
                    mGp.syncTaskListAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextSyncTaskButtonUnselectAll, mContext.getString(R.string.msgs_task_cont_label_unselect_all));
    }

    private void confirmToAuto(TaskListAdapter pa, final NotifyEvent p_ntfy) {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTaskUtil.setSyncTaskToAuto(mGp);
                TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);
                p_ntfy.notifyToListener(true, null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && !pa.getItem(i).isSyncTaskAuto()) {
                msg += sep+"-" + pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }
        mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_task_cont_to_auto_task), msg, ntfy);
    }

    private void confirmToManual(TaskListAdapter pa, final NotifyEvent p_ntfy) {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTaskUtil.setSyncTaskToManual();
                TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);
                p_ntfy.notifyToListener(true, null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && pa.getItem(i).isSyncTaskAuto()) {
                msg += sep+"-" + pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }
        mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_task_cont_to_manual_task), msg, ntfy);
    }

    private void setSyncTaskContextButtonSelectMode() {
        int sel_cnt = TaskListUtils.getSyncTaskSelectedItemCount(mGp.syncTaskListAdapter);
        int tot_cnt = mGp.syncTaskListAdapter.getCount();
        refreshOptionMenu();
        setActionBarSelectMode(sel_cnt, tot_cnt);

        boolean any_selected = TaskListUtils.isSyncTaskSelected(mGp.syncTaskListAdapter);

        boolean act_prof_selected = false, inact_prof_selected = false;
        boolean selected_task_can_sync=false;
        if (any_selected) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskListAdapter.getItem(i).isChecked()) {
                    selected_task_can_sync=true;
                    if (mGp.syncTaskListAdapter.getItem(i).isSyncTaskAuto()) act_prof_selected = true;
                    else inact_prof_selected = true;
                    if (act_prof_selected && inact_prof_selected) break;
                }
            }
        }

        if (inact_prof_selected) {
            if (any_selected) setContextButtonVisibility(mContextSyncTaskViewToAuto,ImageButton.VISIBLE);
            else setContextButtonVisibility(mContextSyncTaskViewToAuto,ImageButton.INVISIBLE);
        } else setContextButtonVisibility(mContextSyncTaskViewToAuto,ImageButton.INVISIBLE);

        if (act_prof_selected) {
            if (any_selected) setContextButtonVisibility(mContextSyncTaskViewToManual,ImageButton.VISIBLE);
            else setContextButtonVisibility(mContextSyncTaskViewToManual,ImageButton.INVISIBLE);
        } else setContextButtonVisibility(mContextSyncTaskViewToManual,ImageButton.INVISIBLE);

        setContextButtonVisibility(mContextSyncTaskViewAddSync,ImageButton.INVISIBLE);

        if (sel_cnt == 1) setContextButtonVisibility(mContextSyncTaskViewCopySyncTask,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewCopySyncTask,ImageButton.INVISIBLE);

        if (any_selected) setContextButtonVisibility(mContextSyncTaskViewDeleteSyncTask,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewDeleteSyncTask,ImageButton.INVISIBLE);

        if (sel_cnt == 1) setContextButtonVisibility(mContextSyncTaskViewRenameSyncTask,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewRenameSyncTask,ImageButton.INVISIBLE);

        if (sel_cnt == 1) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskListAdapter.getItem(i).isChecked()) {
                    if (i == 0) setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
                    else setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.VISIBLE);
                    if (i == (tot_cnt - 1)) setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
                    else setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.VISIBLE);
                    break;
                }
            }
        } else {
            setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
            setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.INVISIBLE);

        if (any_selected) setContextButtonVisibility(mContextSyncTaskViewUnselectAll,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewUnselectAll,ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setSyncTaskContextButtonHide() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskListAdapter.setAllItemChecked(false);
        mGp.syncTaskListAdapter.setShowCheckBox(false);
        mGp.syncTaskListAdapter.notifyDataSetChanged();

        setContextButtonVisibility(mContextSyncTaskViewToAuto, ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewToManual,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewAddSync,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewCopySyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewDeleteSyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewRenameSyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewUnselectAll,ImageButton.INVISIBLE);

    }

    private void setContextButtonVisibility(View v, int visiblity) {
        v.setVisibility(visiblity);
    }

    private void setActionBarSelectMode(int sel_cnt, int tot_cnt) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String sel_txt = "" + sel_cnt + "/" + tot_cnt;
        actionBar.setTitle(sel_txt);
    }

    private void setActionBarNormalMode() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void setSyncTaskContextButtonNormalMode() {
        setActionBarNormalMode();

        mGp.syncTaskListAdapter.setAllItemChecked(false);
        mGp.syncTaskListAdapter.setShowCheckBox(false);
        mGp.syncTaskListAdapter.notifyDataSetChanged();

        setContextButtonVisibility(mContextSyncTaskViewToAuto,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewToManual,ImageButton.INVISIBLE);
        if (isUiEnabled()) setContextButtonVisibility(mContextSyncTaskViewAddSync,ImageButton.VISIBLE);
        else setContextButtonVisibility(mContextSyncTaskViewAddSync,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewCopySyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewDeleteSyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewRenameSyncTask,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewMoveToUp,ImageButton.INVISIBLE);
        setContextButtonVisibility(mContextSyncTaskViewMoveToDown,ImageButton.INVISIBLE);
        if (isUiEnabled()) {
            if (!mGp.syncTaskListAdapter.isEmptyAdapter()) setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.VISIBLE);
            else setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.INVISIBLE);
        } else {
            setContextButtonVisibility(mContextSyncTaskViewSelectAll,ImageButton.INVISIBLE);
        }
        setContextButtonVisibility(mContextSyncTaskViewUnselectAll,ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setMessageScrollButtonVisibility() {
        Handler hndl=new Handler();
        hndl.post(new Runnable(){
            @Override
            public void run() {
                if (canListViewScrollDown(mGp.syncMessageView)) {
                    mContextMessageButtonScrollDown.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonPageDown.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonMoveBottom.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextMessageButtonScrollDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonPageDown.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonMoveBottom.setVisibility(LinearLayout.INVISIBLE);
                }
                if (canListViewScrollUp(mGp.syncMessageView)) {
                    mContextMessageButtonScrollUp.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonPageUp.setVisibility(LinearLayout.VISIBLE);
                    mContextMessageButtonMoveTop.setVisibility(LinearLayout.VISIBLE);
                } else {
                    mContextMessageButtonScrollUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonPageUp.setVisibility(LinearLayout.INVISIBLE);
                    mContextMessageButtonMoveTop.setVisibility(LinearLayout.INVISIBLE);
                }
            }
        });
    }

    private long timeLastMessagesTouchEvent = System.currentTimeMillis();
    private final static int MESSAGE_SCROLL_AMOUNT=1;
    private void setMessageContextButtonListener() {
        setMessageScrollButtonVisibility();
        mGp.syncMessageView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                setMessageScrollButtonVisibility();
            }
        });

        mContextMessageButtonScrollUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncMessageView.getFirstVisiblePosition() - MESSAGE_SCROLL_AMOUNT;

                if (sel > mGp.syncMessageListAdapter.getCount() - 1) sel = mGp.syncMessageListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;
                mGp.syncMessageListAdapter.notifyDataSetChanged();
                mGp.syncMessageView.setSelection(sel);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonScrollDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int sel = mGp.syncMessageView.getFirstVisiblePosition() + MESSAGE_SCROLL_AMOUNT;

                if (sel > mGp.syncMessageListAdapter.getCount() - 1) sel = mGp.syncMessageListAdapter.getCount() - 1;
                if (sel < 0) sel = 0;

                mGp.syncMessageListAdapter.notifyDataSetChanged();
                mGp.syncMessageView.setSelection(sel);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPageUp.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int lv_height = mGp.syncMessageView.getHeight();
                int first_item_y_top =  mGp.syncMessageView.getChildAt(0).getTop();
                int first_item_y_bottom =  mGp.syncMessageView.getChildAt(0).getBottom();
                int first_item_height = first_item_y_bottom - first_item_y_top;
                int y_offset = 0;
                if (first_item_y_top < 0) {
                    // part of first item is hidden on top
                    y_offset = first_item_height;
                    if (y_offset > lv_height) {
                        //item is more than one page: position to the bottom, the current top exact last visible position, minus 3 text lines
                        TextView listTextView = (TextView) mGp.syncMessageView.getChildAt(0).findViewById(R.id.message_list_item_date);
                        int text_context_size = 0;
                        if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                        y_offset = first_item_height - first_item_y_bottom + text_context_size;
                    }
                }

                //mUtil.addDebugMsg(2, "I", "lv_height="+lv_height + " first_item_height="+first_item_height + " first_item_y_top="+first_item_y_top + " first_item_y_bottom="+first_item_y_bottom);
                int lp=mGp.syncMessageView.getLastVisiblePosition();
                int fp=mGp.syncMessageView.getFirstVisiblePosition();
                mGp.syncMessageListAdapter.refresh();
                mGp.syncMessageView.setSelectionFromTop(mGp.syncMessageView.getFirstVisiblePosition(), lv_height - y_offset);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPageDown.setOnTouchListener(new RepeatListener(ANDROID_LONG_PRESS_TIMEOUT, 50, false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                int last_item_pos = mGp.syncMessageView.getLastVisiblePosition() - mGp.syncMessageView.getFirstVisiblePosition();
                int lv_height = mGp.syncMessageView.getHeight();
                int last_item_y_top =  mGp.syncMessageView.getChildAt(last_item_pos).getTop();
                int last_item_y_bottom =  mGp.syncMessageView.getChildAt(last_item_pos).getBottom();
                int last_item_height = last_item_y_bottom - last_item_y_top;
                int y_offset = 0;

                if (last_item_height > lv_height) {
                    //item is more than one page: position to the top, the current bottom exat last visible position, minus 3 text lines
                    TextView listTextView = (TextView) mGp.syncMessageView.getChildAt(last_item_pos).findViewById(R.id.message_list_item_date);
                    int text_context_size = 0;
                    if (listTextView != null) text_context_size = (int)(listTextView.getTextSize() * 3);
                    y_offset = -(lv_height - last_item_y_top - text_context_size);
                }

                //mUtil.addDebugMsg(2, "I", "y_offset="+y_offset + " last_item_height="+last_item_height + " last_item_y_top="+last_item_y_top);
                mGp.syncMessageListAdapter.refresh();
                mGp.syncMessageView.setSelectionFromTop(mGp.syncMessageView.getLastVisiblePosition(), y_offset);
                setMessageScrollButtonVisibility();
            }
        }));

        mContextMessageButtonPinned.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonPinned, false);
                mGp.freezeMessageViewScroll = !mGp.freezeMessageViewScroll;
                if (mGp.freezeMessageViewScroll) {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
                    CommonDialog.showPopupMessageAsUpAnchorViewShort(mActivity, mContextMessageButtonPinned, mContext.getString(R.string.msgs_log_activate_pinned));
                    ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_active));
                } else {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
                    mGp.syncMessageListAdapter.notifyDataSetChanged();
                    mGp.syncMessageView.setSelection(mGp.syncMessageView.getCount() - 1);
                    CommonDialog.showPopupMessageAsUpAnchorViewShort(mActivity, mContextMessageButtonPinned, mContext.getString(R.string.msgs_log_inactivate_pinned));
                    ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));
                }
                setContextButtonEnabled(mContextMessageButtonPinned, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonPinned, mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));

        mContextMessageButtonMoveTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveTop, false);
                mGp.syncMessageListAdapter.notifyDataSetChanged();
                mGp.syncMessageView.setSelection(0);
                setContextButtonEnabled(mContextMessageButtonMoveTop, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonMoveTop, mContext.getString(R.string.msgs_msg_cont_label_move_top));

        mContextMessageButtonMoveBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveBottom, false);
                mGp.syncMessageListAdapter.notifyDataSetChanged();
                mGp.syncMessageView.setSelection(mGp.syncMessageListAdapter.getCount() - 1);
                setContextButtonEnabled(mContextMessageButtonMoveBottom, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonMoveBottom, mContext.getString(R.string.msgs_msg_cont_label_move_bottom));

        mContextMessageButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mGp.syncMessageView.setSelection(0);
                        if (mGp.syncMessageListAdapter !=null) mGp.syncMessageListAdapter.clear();
                        CommonUtilities.saveMessageList(mContext, mGp);
                        mUtil.addLogMsg("W", "", getString(R.string.msgs_log_msg_cleared));
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mUtil.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_log_confirm_clear_all_msg), "", ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, mContextMessageButtonClear, mContext.getString(R.string.msgs_msg_cont_label_clear));
    }

    private void setMessageViewListener() {
//        final EditText et_find_string=(EditText)mMessageView.findViewById(R.id.main_message_filter_string_value);
//        mGp.syncMessageView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
//                et_find_string.setText(mGp.syncMessageAdapter.getItem(i).getDate()+" "+mGp.syncMessageAdapter.getItem(i).getTime()+
//                        " "+mGp.syncMessageAdapter.getItem(i).getMessage());
//                return true;//Do not propergate onClickListener()
//            }
//        });
    }

    private void setMessageFilterListener() {
        LinearLayout ll_filter_category=(LinearLayout) mMessageView.findViewById(R.id.main_message_filter_category_view);
        LinearLayout ll_filter_string=(LinearLayout) mMessageView.findViewById(R.id.main_message_filter_string_view);
        if (mGp.themeColorList.theme_is_light) {
            ll_filter_category.setBackgroundColor(Color.LTGRAY);
            ll_filter_string.setBackgroundColor(Color.LTGRAY);
        }

        final CheckBox cb_filter_info=(CheckBox)mMessageView.findViewById(R.id.main_message_filter_category_info);
        final CheckBox cb_filter_warn=(CheckBox)mMessageView.findViewById(R.id.main_message_filter_category_warn);
        final CheckBox cb_filter_error=(CheckBox)mMessageView.findViewById(R.id.main_message_filter_category_error);

        final EditText et_find_string=(EditText)mMessageView.findViewById(R.id.main_message_filter_string_value);
        final Button btn_find=(Button)mMessageView.findViewById(R.id.main_message_filter_string_find);
        final Button btn_reset=(Button)mMessageView.findViewById(R.id.main_message_filter_string_reset);
        final CheckBox cb_filter_case_sensitive=(CheckBox) mMessageView.findViewById(R.id.main_message_filter_string_case_insensitive);

        cb_filter_info.setTextColor(mGp.themeColorList.text_color_primary);
        cb_filter_warn.setTextColor(mGp.themeColorList.text_color_warning);
        cb_filter_error.setTextColor(mGp.themeColorList.text_color_error);
        cb_filter_info.setChecked(true);
        cb_filter_warn.setChecked(true);
        cb_filter_error.setChecked(true);

        cb_filter_info.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.syncMessageListAdapter.setFilterInfo(cb_filter_info.isChecked());
            }
        });
        cb_filter_warn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.syncMessageListAdapter.setFilterWarn(cb_filter_warn.isChecked());
            }
        });
        cb_filter_error.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.syncMessageListAdapter.setFilterError(cb_filter_error.isChecked());
            }
        });

        cb_filter_case_sensitive.setChecked(false);
        cb_filter_case_sensitive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (et_find_string.getText().length()>0)
                    mGp.syncMessageListAdapter.setFilterString(et_find_string.getText().toString(), cb_filter_case_sensitive.isChecked());
            }
        });

        CommonDialog.setViewEnabled(mActivity, btn_find, false);
        CommonDialog.setViewEnabled(mActivity, btn_reset, false);
        et_find_string.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length()>0) {
                    CommonDialog.setViewEnabled(mActivity, btn_find, true);
                    CommonDialog.setViewEnabled(mActivity, btn_reset, true);
                } else {
                    CommonDialog.setViewEnabled(mActivity, btn_find, false);
                }
            }
        });

        btn_reset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonDialog.setViewEnabled(mActivity, btn_reset, false);
                et_find_string.setText("");
                mGp.syncMessageListAdapter.setFilterString("");
            }
        });

        btn_find.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mGp.syncMessageListAdapter.setFilterString(et_find_string.getText().toString(), cb_filter_case_sensitive.isChecked());
//                CommonDialog.setViewEnabled(mActivity, btn_find, false);
            }
        });
    }

    private void setMessageContextButtonNormalMode() {
        setContextButtonVisibility(mContextMessageViewPinned,LinearLayout.VISIBLE);
        if (mGp.freezeMessageViewScroll) {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
        } else {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
        }
        setContextButtonVisibility(mContextMessageViewMoveTop,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextMessageViewMoveBottom,LinearLayout.VISIBLE);
        setContextButtonVisibility(mContextMessageViewClear,LinearLayout.VISIBLE);
    }

    private TaskEditor mTaskEditor =null;
    private void editSyncTask(final String prof_name, final boolean prof_act, final int prof_num) {
        NotifyEvent ntfy_check = new NotifyEvent(mContext);
        ntfy_check.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                SyncTaskItem item = mGp.syncTaskListAdapter.getItem(prof_num);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mGp.syncTaskListAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}
                });
                if (mTaskEditor ==null) mTaskEditor = TaskEditor.newInstance();
                mTaskEditor.showDialog(getSupportFragmentManager(), mTaskEditor, TaskEditor.TASK_EDIT_METHOD_EDIT, item, mTaskUtil, mUtil, mGp, ntfy);
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        ApplicationPassword.authentication(mGp, mActivity, getSupportFragmentManager(), mUtil, false, ntfy_check, ApplicationPassword.APPLICATION_PASSWORD_RESOURCE_EDIT_SYNC_TASK);
     }

    private void syncSpecificSyncTask(SyncTaskItem sti) {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        t_list.add(sti);
        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_selected_tasks));
        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_task_name_list) + " " + sti.getSyncTaskName());
        Toast.makeText(mContext, mContext.getString(R.string.msgs_main_sync_selected_tasks),Toast.LENGTH_SHORT).show();
        startSyncTask(t_list);
    }

    private void syncSelectedSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        SyncTaskItem item;
        String sync_list_tmp = "";
        String sep = "";
        boolean test_sync_task_found = false;
        for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
            item = mGp.syncTaskListAdapter.getItem(i);
            if (item.isChecked()  && !item.isSyncFolderStatusError()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = ",";
                if (item.isSyncTestMode()) test_sync_task_found = true;
            }
        }
        final String sync_list = sync_list_tmp;

        NotifyEvent ntfy_test_mode = new NotifyEvent(mContext);
        ntfy_test_mode.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (t_list.isEmpty()) {
                    mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_main_sync_select_task_no_auto_task));
                    mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_main_sync_select_task_no_auto_task), "", null);
                } else {
                    mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_selected_tasks));
                    mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_task_name_list) + " " + sync_list);
                    Toast.makeText(mContext,
                            mContext.getString(R.string.msgs_main_sync_selected_tasks), Toast.LENGTH_SHORT).show();
                    startSyncTask(t_list);
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        if (test_sync_task_found) {
            mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_sync_test_mode_warnning), "", ntfy_test_mode);
        } else {
            ntfy_test_mode.notifyToListener(true, null);
        }
    }

    private void syncAutoSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        String sync_list_tmp = "", sep = "";
        for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
            SyncTaskItem item = mGp.syncTaskListAdapter.getItem(i);
            if (item.isSyncTaskAuto() && !item.isSyncTestMode() && !item.isSyncFolderStatusError()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = ",";
            }
        }

        if (t_list.isEmpty()) {
            mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_auto_sync_task_not_found));
            mUtil.showCommonDialog(false, "E", mContext.getString(R.string.msgs_auto_sync_task_not_found), "", null);
        } else {
            mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_all_auto_tasks));
            mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_main_sync_task_name_list) + sync_list_tmp);
//			tabHost.setCurrentTabByTag(TAB_TAG_MSG);
            Toast.makeText(mContext, mContext.getString(R.string.msgs_main_sync_all_auto_tasks), Toast.LENGTH_SHORT).show();
            startSyncTask(t_list);
        }

    }

    public void setUiEnabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = true;

        if (!mGp.syncTaskListAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryListAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        setContextButtonVisibility(mContextScheduleView,LinearLayout.VISIBLE);
//        mContextSyncTaskContextView,LinearLayout.VISIBLE);

        refreshOptionMenu();
    }

    public void setUiDisabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = false;

        if (!mGp.syncTaskListAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryListAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        setContextButtonVisibility(mContextScheduleView,LinearLayout.GONE);

        refreshOptionMenu();
    }

    private boolean isUiEnabled() {
        return enableMainUi;
    }

    final public void refreshOptionMenu() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        supportInvalidateOptionsMenu();
    }

    private void startSyncTask(ArrayList<SyncTaskItem> alp) {
        String[] task_name = new String[alp.size()];
        for (int i = 0; i < alp.size(); i++) task_name[i] = alp.get(i).getSyncTaskName();
        try {
            mSvcClient.aidlStartSpecificSyncTask(task_name);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void syncThreadStarted() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSynctask.setVisibility(TextView.VISIBLE);
        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_sync_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        SyncThread.cancelTask(mGp.syncThreadCtrl);
                        mGp.progressSpinCancel.setEnabled(false);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        SyncThread.unlockTask(mGp.syncThreadCtrl);
                    }
                });
                SyncThread.lockTask(mGp.syncThreadCtrl);
                mUtil.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_cancel_confirm), "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        ScheduleUtils.setScheduleInfo(mContext, mGp, mUtil);

        LogUtil.flushLog(mContext);
    }

    private void syncThreadEnded() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        LogUtil.flushLog(mContext);

        mGp.progressSpinCancelListener = null;
        mGp.progressSpinCancel.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        mGp.syncHistoryListAdapter.notifyDataSetChanged();

        setUiEnabled();
    }

    private ISvcCallback mSvcCallbackStub = new ISvcCallback.Stub() {
        @Override
        public void cbThreadStarted() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadStarted();
                }
            });
        }

        @Override
        public void cbThreadEnded() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadEnded();
                }
            });
        }

        @Override
        public void cbShowConfirmDialog(final String method, final String msg,
                                        final String pair_a_path, final long pair_a_length, final long pair_a_last_mod,
                                        final String pair_b_path, final long pair_b_length, final long pair_b_last_mod) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showConfirmDialog(method, msg, pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
                }
            });
        }

        @Override
        public void cbHideConfirmDialog() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideConfirmDialog();
                }
            });
        }

        @Override
        public void cbWifiStatusChanged(String status, String ssid) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshOptionMenu();
                    if (mGp.syncTaskListAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                    else setSyncTaskContextButtonNormalMode();
                }
            });
        }

        @Override
        public void cbMediaStatusChanged(String action) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, count="+mGp.safMgr.getSafStorageList().size());
                    refreshOptionMenu();
                    mGp.syncTaskListAdapter.notifyDataSetChanged();
                    if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        ArrayList<SafStorage3> dup_list=SafManager3.getDuplicateUuid(mContext);
                        if (dup_list.size()>0) {
                            String dup_info="";
                            for(SafStorage3 item:dup_list) dup_info+="UUID="+item.uuid+" , Description="+item.description+"\n";
                            mUtil.showCommonDialog(false, "W",
                                    mContext.getString(R.string.msgs_main_external_storage_uuid_duplicated), dup_info, null);
                        }
                    }
                }
            });
        }

    };

    private ISvcClient mSvcClient = null;

    private void openService(final CallBackListener cbl) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mSvcConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
                mSvcClient = ISvcClient.Stub.asInterface(service);
                setCallbackListener();
                cbl.onCallBack(mContext, true, null);
            }

            public void onServiceDisconnected(ComponentName name) {
                mSvcConnection = null;
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            }
        };

        Intent intmsg = new Intent(mContext, SyncService.class);
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
    }

    private void closeService() {

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        if (mSvcConnection != null) {
            mSvcClient = null;
            try {
                unbindService(mSvcConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSvcConnection = null;
        }
    }

    final private void setCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        try {
            mSvcClient.setCallBack(mSvcCallbackStub);
        } catch (RemoteException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "E", "setCallbackListener error :" + e.toString());
        }
    }

    final private void unsetCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mSvcClient != null) {
            try {
                mSvcClient.removeCallBack(mSvcCallbackStub);
            } catch (RemoteException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "E", "unsetCallbackListener error :" + e.toString());
            }
        }
    }

    private void reshowDialogWindow() {
        if (mGp.dialogWindowShowed) {
            syncThreadStarted();
            mGp.progressSpinSynctask.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
            if (mGp.confirmDialogShowed)
                showConfirmDialog(mGp.confirmDialogMethod, mGp.confirmDialogMessage,
                        mGp.confirmDialogFilePathPairA, mGp.confirmDialogFileLengthPairA, mGp.confirmDialogFileLastModPairA,
                        mGp.confirmDialogFilePathPairB, mGp.confirmDialogFileLengthPairB, mGp.confirmDialogFileLastModPairB);
        }
    }

    private void hideConfirmDialog() {
        mGp.confirmView.setVisibility(LinearLayout.GONE);
    }

    private void showConfirmDialog(final String method, final String msg,
                                   final String pair_a_path, final long pair_a_length, final long pair_a_last_mod,
                                   final String pair_b_path, final long pair_b_length, final long pair_b_last_mod) {
        if (method.equals(CONFIRM_REQUEST_CONFLICT_FILE)) {
            TwoWaySyncFile.showConfirmDialogConflict(mGp, mUtil, mSvcClient,
                    method, msg, pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
        } else {
            showConfirmDialogOverride(method, msg, pair_a_path, pair_b_path);
        }
    }

    private void showConfirmDialogOverride(String method, String msg, String from_path, String to_path) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        final int prog_view=mGp.progressSpinView.getVisibility();
        mGp.progressSpinView.setVisibility(ProgressBar.GONE);
        mGp.confirmOverrideView.setVisibility(LinearLayout.VISIBLE);
        mGp.confirmConflictView.setVisibility(LinearLayout.GONE);
        mGp.confirmDialogShowed = true;
        mGp.confirmDialogFilePathPairA = from_path;
        mGp.confirmDialogFilePathPairA = to_path;
        mGp.confirmDialogMethod = method;
        mGp.confirmDialogMessage = msg;

        mGp.mainDialogView.bringToFront();
        mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
        mGp.confirmView.setBackgroundColor(mGp.themeColorList.text_background_color);
        mGp.confirmView.bringToFront();
        String msg_text = "";
        if (method.equals(CONFIRM_REQUEST_COPY)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_copy_confirm), to_path);
        } else if (method.equals(CONFIRM_REQUEST_DELETE_FILE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_file_confirm), to_path);
        } else if (method.equals(CONFIRM_REQUEST_DELETE_DIR)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_dir_confirm), to_path);
        } else if (method.equals(CONFIRM_REQUEST_MOVE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_move_confirm), from_path, to_path);
        } else if (method.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_zip_item_file_confirm), to_path);
        } else if (method.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_zip_item_dir_confirm), to_path);
        } else if (method.equals(CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE)) {
            long fd=(new SafFile3(mContext, from_path)).lastModified();
            String date_time= StringUtil.convDateTimeTo_YearMonthDayHourMinSec(fd);
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_confirm), date_time, from_path);
        }
        mGp.confirmMsg.setText(msg_text);

        // Yesボタンの指定
        mGp.confirmYesListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, CONFIRM_RESP_YES);
            }
        };
        mGp.confirmYes.setOnClickListener(mGp.confirmYesListener);
        // YesAllボタンの指定
        mGp.confirmYesAllListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, CONFIRM_RESP_YESALL);
            }
        };
        mGp.confirmYesAll.setOnClickListener(mGp.confirmYesAllListener);
        // Noボタンの指定
        mGp.confirmNoListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, CONFIRM_RESP_NO);
            }
        };
        mGp.confirmNo.setOnClickListener(mGp.confirmNoListener);
        // NoAllボタンの指定
        mGp.confirmNoAllListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, CONFIRM_RESP_NOALL);
            }
        };
        mGp.confirmNoAll.setOnClickListener(mGp.confirmNoAllListener);
        // Task cancelボタンの指定
        mGp.confirmCancelListener = new OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                mGp.progressSpinView.setVisibility(prog_view);
                sendConfirmResponse(mGp, mSvcClient, CONFIRM_RESP_CANCEL);
            }
        };
        mGp.confirmCancel.setOnClickListener(mGp.confirmCancelListener);
    }

    static public void sendConfirmResponse(GlobalParameters gp, ISvcClient sc, int response) {
        gp.confirmDialogShowed = false;
        try {
            sc.aidlConfirmReply(response);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        gp.confirmYesListener = null;
        gp.confirmYesAllListener = null;
        gp.confirmNoListener = null;
        gp.confirmNoAllListener = null;
        gp.confirmCancelListener = null;
        gp.confirmCancel.setOnClickListener(null);
        gp.confirmYes.setOnClickListener(null);
        gp.confirmYesAll.setOnClickListener(null);
        gp.confirmNo.setOnClickListener(null);
        gp.confirmNoAll.setOnClickListener(null);

        gp.confirmDialogConflictButtonSelectAListener = null;
        gp.confirmDialogConflictButtonSelectBListener = null;
        gp.confirmDialogConflictButtonSyncIgnoreFileListener = null;
        gp.confirmDialogConflictButtonCancelSyncTaskListener = null;

    }

    final private boolean checkJcifsOptionChanged() {
        boolean changed = false;

        String prevSmbLmCompatibility = mGp.settingsSmbLmCompatibility,
                prevSmbUseExtendedSecurity = mGp.settingsSmbUseExtendedSecurity;
        String p_response_timeout=mGp.settingsSmbClientResponseTimeout;
        String p_disable_plain_text_passwords=mGp.settingsSmbDisablePlainTextPasswords;

        mGp.initJcifsOption(mContext);

        if (!mGp.settingsSmbLmCompatibility.equals(prevSmbLmCompatibility)) changed = true;
        else if (!mGp.settingsSmbUseExtendedSecurity.equals(prevSmbUseExtendedSecurity)) changed = true;
        else if (!mGp.settingsSmbClientResponseTimeout.equals(p_response_timeout)) changed = true;
        else if (!mGp.settingsSmbDisablePlainTextPasswords.equals(p_disable_plain_text_passwords)) changed = true;

        if (changed) {
            listSettingsOption();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    mUtil.flushLog();
                    mGp.settingExitClean=true;
                    finish();
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {
                    mGp.settingExitClean=true;
                }
            });
            mUtil.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_smbsync_main_settings_restart_title),
                    mContext.getString(R.string.msgs_smbsync_main_settings_jcifs_changed_restart), ntfy);
        }

        return changed;
    }

    private String printStackTraceElement(StackTraceElement[] ste) {
        String st_msg = "";
        for (int i = 0; i < ste.length; i++) {
            st_msg += "\n at " + ste[i].getClassName() + "." +
                    ste[i].getMethodName() + "(" + ste[i].getFileName() +
                    ":" + ste[i].getLineNumber() + ")";
        }
        return st_msg;
    }

    private void makeCacheDirectory() {
//        String packageName = mContext.getPackageName();
        long b_time=System.currentTimeMillis();
        File[] stg_array=mContext.getExternalFilesDirs(null);
        for(File item:stg_array) {
            if (item!=null) {
                File cd=new File(item.getParentFile()+"/cache");
                if (!cd.exists()) {
                    boolean rc=cd.mkdirs();
//                mUtil.addDebugMsg(1, "I", "makeChachDirectory directory create result="+rc);
                }
            }
        }
        mUtil.addDebugMsg(1, "I", "makeCacheDirectory elapsed="+(System.currentTimeMillis()-b_time));
    }

    private void cleanupCacheFile() {
        long b_time=System.currentTimeMillis();
        File[] fl=mContext.getExternalCacheDirs();
        if (fl!=null && fl.length>0) {
            for(File cf:fl) {
                if (cf!=null) {
                    File[] child_list=cf.listFiles();
                    if (child_list!=null) {
                        for(File ch_item:child_list) {
                            if (ch_item!=null) {
                                if (!deleteCacheFile(ch_item)) break;
                            }
                        }
                    }
                }
            }
        } else {
            mUtil.addDebugMsg(1, "E", "cleanupCacheFile cache directory not found.");
        }
        mUtil.addDebugMsg(1, "I", "cleanupCacheFile elapsed="+(System.currentTimeMillis()-b_time));
    }

    private boolean deleteCacheFile(File del_item) {
        boolean result=true;
        if (del_item.isDirectory()) {
            File[] child_list=del_item.listFiles();
            for(File child_item:child_list) {
                if (child_item!=null) {
                    if (!deleteCacheFile(child_item)) {
                        result=false;
                        break;
                    }
                }
            }
            if (result) {
                result=del_item.delete();
                if (mUtil.getLogLevel()>=2 && result) mUtil.addDebugMsg(2, "I", "cache directory deleted, dir="+ del_item.getPath());
            }
        } else {
            result=del_item.delete();
            if (mUtil.getLogLevel()>=2 && result) mUtil.addDebugMsg(2, "I", "cache file deleted, file="+ del_item.getPath());
        }
        return result;
    }

    /*
     * A class, that can be used as a TouchListener on any view (e.g. a Button).
     * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
     * click is fired immediately, next one after the initialDelay, and subsequent
     * ones after the repeatDelay.
     *
     * Android default onLongClick can be returned by int ViewConfiguration.getLongPressTimeout() : 500 msec default
     * Interval is scheduled after the onClick completes, so it has to run fast.
     * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
     * achieve this.
     */
    final static private int ANDROID_LONG_PRESS_TIMEOUT = 500;
    final static private int DEFAULT_LONG_PRESS_REPEAT_INTERVAL = 100;
    private class RepeatListener implements View.OnTouchListener {

        private Handler handler = new Handler();

        private final int mLongPressTimeout;
        private final int mRepeatInterval;
        private final boolean mConsumeEvent;
        private final OnClickListener mClickListener;
        private View mTouchedView;
//      private Rect mRect; // Variable to hold the bounds of the view rectangle

        private Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
//                mUtil.addDebugMsg(1, "I", "runnable enterd, enabled="+mTouchedView.isEnabled());
                if (mTouchedView.isEnabled()) {
                    handler.postDelayed(this, mRepeatInterval);
                    mClickListener.onClick(mTouchedView);
                } else {
                    //view was disabled by the clickListener: remove the callback
                    //mUtil.addDebugMsg(2, "I", "runnable cancelled by View Removed");
                    handler.removeCallbacks(handlerRunnable);
                    mTouchedView.setPressed(false);
                    mTouchedView = null;
                }
                //mUtil.addDebugMsg(2, "I", "runnable running");
            }
        };

        /**
         * @param initialDelay The interval after first click event
         * @param repeatDelay The interval after second and subsequent click
         *        events (100 msec recommended)
         * @param clickListener The OnClickListener, that will be called
         *        periodically
         * @param consumeEvent: return value after touch event used
         *        set to false to be able to use the event by other methods directly by caller listener or to pass to parent view if needed
         * [@param] speedIncrementDelay: Optional, not implemented here
        delay after which the speed is even more incremented
         *        by default we will increment the speed by a 3x factor
        set to 0 to disable
         */
        public RepeatListener(int initialDelay, int repeatDelay, boolean consumeEvent, OnClickListener clickListener) {
            if (clickListener == null)
                throw new IllegalArgumentException("null runnable");
            if (initialDelay < 0 || repeatDelay < 0)
                throw new IllegalArgumentException("negative interval");

            mLongPressTimeout = initialDelay;
            mRepeatInterval = repeatDelay;
            mClickListener = clickListener;
            mConsumeEvent = consumeEvent;
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by ACTION_DOWN");
                    handler.removeCallbacks(handlerRunnable);
                    handler.postDelayed(handlerRunnable, mLongPressTimeout);
                    mTouchedView = view;
                    mTouchedView.setPressed(true);
                    mClickListener.onClick(view);
                    return mConsumeEvent;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_CANCEL:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by ACTION_CANCEL");
                case MotionEvent.ACTION_UP:
//                    mUtil.addDebugMsg(1, "I", "runnable cancelled by Finger UP");
                    handler.removeCallbacks(handlerRunnable);
                    mTouchedView.setPressed(false);
                    mTouchedView = null;
                    return mConsumeEvent;
            }
            return false;
        }
    }

}
