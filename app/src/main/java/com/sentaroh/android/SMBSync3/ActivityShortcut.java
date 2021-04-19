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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import androidx.fragment.app.FragmentActivity;

import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeUtil;

import static com.sentaroh.android.SMBSync3.Constants.APP_SHORTCUT_ID_KEY;
import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SHORTCUT;

public class ActivityShortcut extends FragmentActivity {

    private ActivityShortcut mActivity=null;

    private CommonUtilities mUtil = null;
    private GlobalParameters mGp = null;

    private int restartStatus = 0;
//    private boolean displayDialogRequired = false;

    private int mShotcutId=1;
    private String mShortcutName="";

    private String mSyncTaskList="";

    @Override
    final protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putBoolean("displayDialogRequired", displayDialogRequired);
    }

    @Override
    final protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
//        displayDialogRequired = savedState.getBoolean("displayDialogRequired", false);
        restartStatus = 2;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(GlobalParameters.setNewLocale(base));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_transrucent);

        mActivity= ActivityShortcut.this;
        mGp= GlobalWorkArea.getGlobalParameter(mActivity);
        GlobalParameters.setDisplayFontScale(mActivity);
        if (mGp.themeColorList == null) {
            mGp.themeColorList = ThemeUtil.getThemeColorList(mActivity);
        }

        mUtil = new CommonUtilities(mActivity, "Shortcuut"+mShotcutId, mGp, getSupportFragmentManager());

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus+", id="+mShotcutId);

        mGp.loadConfigList(mActivity, mUtil);

        mShotcutId=getIntent().getIntExtra(APP_SHORTCUT_ID_KEY, 0);
        if (mShotcutId==1) mShortcutName=mActivity.getString(R.string.msgs_group_name_for_shortcut1);
        else if (mShotcutId==2) mShortcutName=mActivity.getString(R.string.msgs_group_name_for_shortcut2);
        else if (mShotcutId==3) mShortcutName=mActivity.getString(R.string.msgs_group_name_for_shortcut3);

    }

    @Override
    public void onStart() {
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
    }

    @Override
    final public void onResume() {
        super.onResume();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
        if (restartStatus == 0 || restartStatus == 2) {
            restartStatus = 1;
            if (!mGp.syncThreadActive) {
                NotifyEvent ntfy_sync=new NotifyEvent(mActivity);
                ntfy_sync.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        if (objects!=null) {
                            boolean suppress=(boolean)objects[0];
                            if (suppress) {
                                if (mShotcutId==1) mGp.setSupressShortcut1ConfirmationMessage(mActivity, true);
                                else if (mShotcutId==2) mGp.setSupressShortcut2ConfirmationMessage(mActivity, true);
                                else if (mShotcutId==3) mGp.setSupressShortcut3ConfirmationMessage(mActivity, true);
                            }
                        }
                        startSync();
                        finish();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                        finish();
                    }
                });
                confirmStartSync(ntfy_sync);
            } else {
                NotifyEvent ntfy_error=new NotifyEvent(mActivity);
                ntfy_error.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        finish();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialogWarn(false,
                        mActivity.getString(R.string.msgs_main_shortcut_start_error_sync_already_started, mShortcutName), "", ntfy_error);
            }
        }
    }

    private Dialog mDialog=null;
    private void confirmStartSync(NotifyEvent ntfy) {
        boolean suppress=false;
        if (mShotcutId==1 && mGp.isSupressShortcut1ConfirmationMessage()) suppress=true;
        else if (mShotcutId==2 && mGp.isSupressShortcut2ConfirmationMessage()) suppress=true;
        else if (mShotcutId==3 && mGp.isSupressShortcut3ConfirmationMessage()) suppress=true;
        GroupListAdapter.GroupListItem group_item= getShortCutGroup();
        if (group_item==null) {
            String task_list=getAutoTaskList();
            if (task_list.equals("")) {
                putAutoTaskNotFoundMessage(mActivity.getString(R.string.msgs_main_shortcut_start_error_auto_task_does_not_exists, mShortcutName));
                return;
            }
            mSyncTaskList=task_list;
        } else if (group_item.autoTaskOnly) {
            String task_list=getAutoTaskList();
            if (task_list.equals("")) {
                putAutoTaskNotFoundMessage(mActivity.getString(R.string.msgs_main_shortcut_start_error_auto_task_does_not_exists, mShortcutName));
                return;
            }
            mSyncTaskList=task_list;
        } else {
            String error_task_name=isSpecificSyncTaskExists(group_item.taskList);
            if (!error_task_name.equals("")) {
                putTaskNotFountMessage(error_task_name);
                return;
            }
            String task_list=getSpecificSyncTaskList(group_item.taskList);
            mSyncTaskList=task_list;
        }
        if (suppress) {
            ntfy.notifyToListener(true, null);
        } else {
            if (group_item==null) {
                mDialog=TaskEditor.showDialogWithHideOption(mActivity, mGp, mUtil,
                        true, mActivity.getString(R.string.msgs_common_dialog_ok),
                        true, mActivity.getString(R.string.msgs_common_dialog_cancel),
                        mActivity.getString(R.string.msgs_main_shorcut_confirmation_message_title),
                        mActivity.getString(R.string.msgs_main_shortcut_not_assigned, mShortcutName) + "\n" + "-"+mSyncTaskList.replaceAll(",", "\n-"),
                        mActivity.getString(R.string.msgs_main_shorcut_confirmation_message_suppress), ntfy);
            } else {
                mDialog=TaskEditor.showDialogWithHideOption(mActivity, mGp, mUtil,
                        true, mActivity.getString(R.string.msgs_common_dialog_ok),
                        true, mActivity.getString(R.string.msgs_common_dialog_cancel),
                        mActivity.getString(R.string.msgs_main_shorcut_confirmation_message_title),
                        mActivity.getString(R.string.msgs_main_shorcut_confirmation_message_msg) + "\n" + "-"+mSyncTaskList.replaceAll(",", "\n-"),
                        mActivity.getString(R.string.msgs_main_shorcut_confirmation_message_suppress), ntfy);
            }
        }
    }

    private String isSpecificSyncTaskExists(String task_list) {
        String[]grp_task_array=task_list.split(",");
        String sep="";
        for(String item:grp_task_array) {
            if (!isTaskExists(item)) {
                return item;
            }
        }
        return "";
    }

    private String getSpecificSyncTaskList(String task_list) {
        String[]grp_task_array=task_list.split(",");
        String specific_task_list="";
        String sep="";
        for(String item:grp_task_array) {
            if (!isTaskExists(item)) {
                return "";
            } else {
                specific_task_list+=sep+item;
                sep=",";
            }
        }
        return task_list;
    }


    private void putAutoTaskNotFoundMessage(String msg_txt) {
        NotifyEvent ntfy_term=new NotifyEvent(mActivity);
        ntfy_term.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                finish();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mUtil.showCommonDialog(false, "E",
                mActivity.getString(R.string.msgs_main_shortcut_title),
                msg_txt, mActivity.getString(R.string.msgs_common_dialog_close),mActivity.getString(R.string.msgs_common_dialog_cancel),ntfy_term);
    }

    private void putTaskNotFountMessage(String not_found) {
        NotifyEvent ntfy_term=new NotifyEvent(mActivity);
        ntfy_term.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                finish();
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        mUtil.showCommonDialogError(false,
                mActivity.getString(R.string.msgs_main_shortcut_title),
                mActivity.getString(R.string.msgs_main_shortcut_start_error_task_not_found, mShortcutName, not_found), ntfy_term);

    }

    private boolean isTaskExists(String task_name) {
        String sep="";
        SyncTaskItem sti= TaskListUtils.getSyncTaskByName(mGp.syncTaskList, task_name);
        if (sti==null) {
            return false;
        }
        return true;
    }

    private void startSync() {
        String[] task_list_array=mSyncTaskList.split(NAME_LIST_SEPARATOR);
        SyncWorker.startSpecificSyncTask(mActivity, mGp, mUtil, SYNC_REQUEST_SHORTCUT, task_list_array);
    }

    private String getAutoTaskList() {
        String task_list="", sep="";
        for(SyncTaskItem sti:mGp.syncTaskList) {
            if (sti.isSyncTaskAuto() && !sti.isSyncTestMode()) {
                task_list+=sep+sti.getSyncTaskName();
                sep=",";
            }
        }
        return task_list;
    }

    private GroupListAdapter.GroupListItem getShortCutGroup() {
        for(GroupListAdapter.GroupListItem gi:mGp.syncGroupList) {
            if (mShotcutId==1) {
                if (gi.button == GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1) {
                    return gi;
                }
            } else if (mShotcutId==2) {
                if (gi.button == GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2) {
                    return gi;
                }
            } else if (mShotcutId==3) {
                if (gi.button == GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3) {
                    return gi;
                }
            }
        }
        return null;
    }

    private void terminateShortcut() {
        Handler hndl = new Handler();
        hndl.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 100);
    }

    @Override
    public void onPause() {
        super.onPause();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
        // Application process is follow
    }

    @Override
    public void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);
        // Application process is follow
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered restartStaus=" + restartStatus);

        if (mDialog!=null) mDialog.dismiss();

        mUtil.flushLog();
        CommonUtilities.saveMessageList(mActivity, mGp);
        System.gc();
//		android.os.Process.killProcess(android.os.Process.myPid());
    }

}
