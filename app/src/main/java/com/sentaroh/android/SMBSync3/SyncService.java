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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;

import com.sentaroh.android.SMBSync3.Log.LogUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafManager3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_TIMER_EXPIRED;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_SCHEDULE_NAME_KEY;

public class SyncService extends Service {
    final private static Logger log= LoggerFactory.getLogger(SyncService.class);
    private GlobalParameters mGp = null;

    private CommonUtilities mUtil = null;

    private WifiManager mWifiMgr = null;

    private Context mContext = null;

    private SleepReceiver mSleepReceiver = new SleepReceiver();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(new GlobalParameters().setNewLocale(base, true));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = SyncService.this;
        mGp= GlobalWorkArea.getGlobalParameter(mContext);

        mUtil = new CommonUtilities(mContext, "Service", mGp, null);

        mUtil.addDebugMsg(1, "I", "onCreate entered");

        Thread th=new Thread(){
            @Override
            public void run() {
                if (mGp.syncTaskList.size()==0) {
                    mUtil.addDebugMsg(1, "I", "Configuration load started");
                    mGp.loadConfigList(mContext);
                    mUtil.addDebugMsg(1, "I", "Configuration load ended");
                }
            }
        };
        th.setName("SvcLoadConfig");
        th.start();

        NotificationUtils.initNotification(mGp, mUtil, mContext);
        NotificationUtils.clearNotification(mGp, mUtil);
//        startForeground(R.string.app_name, mGp.notification);

        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_smbsync_main_start) +
                " API=" + Build.VERSION.SDK_INT +
                ", Version " + getApplVersionName());

        if (mGp.syncHistoryList == null)
            mGp.syncHistoryList = mUtil.loadHistoryList();

        mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        IntentFilter int_filter = new IntentFilter();
        int_filter = new IntentFilter();
        int_filter.addAction(Intent.ACTION_SCREEN_OFF);
        int_filter.addAction(Intent.ACTION_SCREEN_ON);
        int_filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mSleepReceiver, int_filter);
    }

    private String getApplVersionName() {
        String result = "Unknown";
        try {
            String packegeName = getPackageName();
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
            result = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            mUtil.addDebugMsg(1, "I", "SMBSync3 package can not be found");
        }
        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        WakeLock wl = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP , "SMBSync-Service-1");
        wl.acquire(1000);
        final String action = intent==null?"":(intent.getAction()!=null?intent.getAction():"");
        mUtil.addDebugMsg(1, "I", "onStartCommand entered, action=" + action);
        mGp.waitConfigurationLock();
        if (action.equals(SCHEDULE_INTENT_TIMER_EXPIRED)) {
            if (mGp.settingScheduleSyncEnabled) startSyncByScheduler(intent);
            else {
                mUtil.addDebugMsg(1,"I","Schedule sync request is ignored because scheduler is disabled");
            }
        } else if (action.equals(START_SYNC_INTENT) || action.equals(START_SYNC_AUTO_INTENT)) {
            Bundle bundle = intent.getExtras();
            if (bundle!=null && bundle.containsKey(START_SYNC_EXTRA_PARM_REQUESTOR)) {
                String req=bundle.getString(START_SYNC_EXTRA_PARM_REQUESTOR, "");
                if (req.equals(START_SYNC_EXTRA_PARM_REQUESTOR_SHORTCUT)) startSyncByShortcut(intent);
                else startSyncByAnotherAppl(intent);
            } else {
                startSyncByAnotherAppl(intent);
            }
        } else if (action.equals(QUERY_SYNC_TASK_INTENT)) {
            processQuerySyncTask(intent);
        } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ||
                action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) || action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)
            ) {
            String path = intent.getDataString();
            Thread th = new Thread() {
                @Override
                public void run() {
//                    SystemClock.sleep(3000);
                    ArrayList<SafManager3.StorageVolumeInfo> svlist=mGp.safMgr.getStorageVolumeInfo(mContext);
                    int prev_stor_info=mGp.safMgr.getLastStorageVolumeInfo().size();
                    int prev_saf_count=mGp.safMgr.getSafStorageList().size();
                    for(int i=0;i<30;i++) {
                        mGp.refreshMediaDir(mContext);
                        if (mGp.safMgr.getSafStorageList().size()!=prev_saf_count) {
                            try {
                                if (mGp.callbackStub!=null) {
                                    mGp.callbackStub.cbMediaStatusChanged(action);
                                    mUtil.addDebugMsg(1, "I", "Notify Media status change completed");
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            break;
                        } else {
                            SystemClock.sleep(500);
                        }
                    }
                    mUtil.addDebugMsg(1, "I", "Media status change ended");
                }
            };
            th.start();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setActivityForeground();
        return mSvcClientStub;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, qs=" + mGp.syncRequestQueue.size());
        if (isServiceToBeStopped()) stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        unregisterReceiver(mSleepReceiver);
//        unregisterReceiver(mUsbReceiver);
        stopForeground(true);
        if (mGp.notificationLastShowedMessage != null && !mGp.notificationLastShowedMessage.equals("")) {
            showSyncEndNotificationMessage();
            mGp.notificationLastShowedMessage = null;
        }
        mGp.releaseWakeLock(mUtil);
        mUtil.addLogMsgFromUI("I", "", mContext.getString(R.string.msgs_terminate_application));
        LogUtil.closeLog(mContext);
        NotificationUtils.setNotificationEnabled(mGp, true);
        CommonUtilities.saveMessageList(mContext, mGp);
        if (mGp.activityRestartRequired) {
            mGp.activityRestartRequired=false;
            mGp.clearParms(mContext);
            System.gc();
            Intent intent = new Intent(mContext, ActivityMain.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            if (mGp.settingExitClean) {
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                mGp.clearParms(mContext);
                System.gc();
            }
        }
    }

    private boolean isServiceToBeStopped() {
        boolean result = false;
        if (mGp.callbackStub == null) {
            synchronized (mGp.syncRequestQueue) {
                result = !(mGp.syncRequestQueue.size() > 0 || mGp.syncThreadActive);
            }
        }
        return result;
    }

    private ScheduleListAdapter.ScheduleListItem getScheduleInformation(ArrayList<ScheduleListAdapter.ScheduleListItem> sl, String name) {
        for (ScheduleListAdapter.ScheduleListItem si : sl) {
            if (si.scheduleName.equals(name))
                return si;
        }
        return null;
    }

    private void processQuerySyncTask(Intent in) {
        String reply_list="", sep="";
        String task_type=QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO;
        if (in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE)!=null) task_type=in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE);
        mUtil.addDebugMsg(1,"I","extra="+in.getExtras()+", str="+in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE));
        int reply_count=0;
        if (mGp.syncTaskList.size()>0) {
            for(int i=0;i<mGp.syncTaskList.size();i++) {
                SyncTaskItem sti=mGp.syncTaskList.get(i);
                if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST.toLowerCase())) {
                    if (sti.isSyncTestMode()) {
                        reply_list+=sep+sti.getSyncTaskName();
                        sep=NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO.toLowerCase())) {
                    if (sti.isSyncTaskAuto()) {
                        reply_list+=sep+sti.getSyncTaskName();
                        sep=NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL.toLowerCase())) {
                    if (!sti.isSyncTaskAuto()) {
                        reply_list+=sep+sti.getSyncTaskName();
                        sep=NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL.toLowerCase())) {
                    reply_list+=sep+sti.getSyncTaskName();
                    sep=NAME_LIST_SEPARATOR;
                    reply_count++;
                }
            }
        }
        Intent reply=new Intent(REPLY_SYNC_TASK_INTENT);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT, reply_count);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY, reply_list);
        mContext.sendBroadcast(reply);
    }

    private void startSyncByScheduler(Intent in) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler));

        if (in.getExtras().containsKey(SCHEDULE_SCHEDULE_NAME_KEY)) {
            String schedule_name_list = in.getStringExtra(SCHEDULE_SCHEDULE_NAME_KEY);

            mUtil.addDebugMsg(1,"I", "Schedule information, name=" + schedule_name_list);

            String[] schedule_list=schedule_name_list.split(NAME_LIST_SEPARATOR);

            startSyncBySchedulerByNameList(schedule_list);
        }
//        mUtil.addLogMsg("I", mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler));
        if (isServiceToBeStopped()) stopSelf();
    }

    private void startSyncBySchedulerByNameList(String[] schedule_list) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        for(String schedule_name:schedule_list) {
            mUtil.addDebugMsg(1, "I", "Schedule start, name=" + schedule_name);
            ScheduleListAdapter.ScheduleListItem si = getScheduleInformation(mGp.syncScheduleList, schedule_name);
            if (si!=null) {
                if (si.syncAutoSyncTask) {
                    queueAutoSyncTask(SYNC_REQUEST_SCHEDULE, si);
                } else {
                    if (si.syncTaskList != null && si.syncTaskList.length() > 0) {
                        checkAndQueueSyncTaskExists(SYNC_REQUEST_SCHEDULE, si.syncTaskList,
                                mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_task_not_found),
                                mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_no_task_list));
                    } else {
                        mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_no_task_list));
                    }
                }
            } else {
                mUtil.addLogMsg("W","", "Specified schedule name was not found, name=",schedule_name);
            }
        }
        if (!mGp.syncThreadActive) {
            startSyncThread();
        }
    }

    private void checkAndQueueSyncTaskExists(String requestor, String task_list, String not_found_msg, String no_task_msg) {
        String[] sp = task_list.split(NAME_LIST_SEPARATOR);
        ArrayList<String> pl = new ArrayList<String>();
        for (int i = 0; i < sp.length; i++) {
            if (TaskListUtils.getSyncTaskByName(mGp.syncTaskList, sp[i]) != null) {
                pl.add(sp[i]);
            } else {
                mUtil.addLogMsg("W", "", not_found_msg + sp[i]);
                NotificationUtils.showOngoingMsg(mGp, mUtil, 0, not_found_msg + sp[i]);
            }
        }
        if (pl.size() > 0) {
            String[] nspl = new String[pl.size()];
            for (int i = 0; i < pl.size(); i++) nspl[i] = pl.get(i);
            queueSpecificSyncTask(nspl, requestor);
        } else {
            mUtil.addLogMsg("W", "", no_task_msg);
            NotificationUtils.showOngoingMsg(mGp, mUtil, 0, no_task_msg);
        }

    }

    private void startSyncByAnotherAppl(Intent in) {
        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_external));
        Bundle bundle = in.getExtras();
        if (bundle != null) {
            if (bundle.containsKey(START_SYNC_EXTRA_PARM_SYNC_PROFILE)) {
                if (!bundle.get(START_SYNC_EXTRA_PARM_SYNC_PROFILE).getClass().getSimpleName().equals("String")) {
                    NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                            mContext.getString(R.string.msgs_extra_data_sync_profile_type_error));
                    mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_extra_data_sync_profile_type_error));
                    return;
                }
                String t_sp = bundle.getString(START_SYNC_EXTRA_PARM_SYNC_PROFILE);
                checkAndQueueSyncTaskExists(SYNC_REQUEST_EXTERNAL,
                        t_sp, mContext.getString(R.string.msgs_svc_received_start_request_from_external_task_not_found),
                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_list));
            } else if (bundle.containsKey(START_SYNC_EXTRA_PARM_SYNC_GROUP)) {
                if (!bundle.get(START_SYNC_EXTRA_PARM_SYNC_GROUP).getClass().getSimpleName().equals("String")) {
                    NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                            mContext.getString(R.string.msgs_extra_data_sync_profile_type_error));
                    mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_extra_data_sync_profile_type_error));
                    return;
                }
                String t_sg = bundle.getString(START_SYNC_EXTRA_PARM_SYNC_GROUP);
                String t_sp="";
                for(GroupListAdapter.GroupListItem item:mGp.syncGroupList) {
                    if (item.groupName.equalsIgnoreCase(t_sg)) {
                        t_sp=item.taskList;
                    }
                }
                if (!t_sp.equals("")) {
                    checkAndQueueSyncTaskExists(SYNC_REQUEST_EXTERNAL, t_sp,
                            mContext.getString(R.string.msgs_svc_received_start_request_from_external_task_not_found),
                            mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_list));
                } else {
                    mUtil.addLogMsg("W", "",
                            mContext.getString(R.string.msgs_svc_received_start_request_from_external_group_not_found)+ t_sg);
                    NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                            mContext.getString(R.string.msgs_svc_received_start_request_from_external_group_not_found)+ t_sg);
                }
            } else {
                mUtil.addLogMsg("W","",
                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
                NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
            }
        } else {
            mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_external_auto_task));
            queueAutoSyncTask(SYNC_REQUEST_EXTERNAL);
            if (!mGp.syncThreadActive) {
                startSyncThread();
            }
        }
        if (isServiceToBeStopped()) stopSelf();
    }

    private void startSyncByShortcut(Intent in) {
        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_shortcut));
//        queueAutoSyncTask(SYNC_REQUEST_SHORTCUT);
        Bundle bundle = in.getExtras();
        if (bundle.containsKey(START_SYNC_EXTRA_PARM_SYNC_PROFILE)) {
            if (bundle.get(START_SYNC_EXTRA_PARM_SYNC_PROFILE).getClass().getSimpleName().equals("String")) {
                String t_sp = bundle.getString(START_SYNC_EXTRA_PARM_SYNC_PROFILE);
                String[] sp = t_sp.split(NAME_LIST_SEPARATOR);
                queueSpecificSyncTask(sp, SYNC_REQUEST_SHORTCUT);
                if (!mGp.syncThreadActive) {
                    startSyncThread();
                }
            } else {
                mUtil.addLogMsg("W","",
                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
                NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
            }
        } else {
            mUtil.addLogMsg("W","",
                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
            NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
        }

        if (isServiceToBeStopped()) stopSelf();
    }

    final private ISvcClient.Stub mSvcClientStub = new ISvcClient.Stub() {
        @Override
        public void setCallBack(ISvcCallback callback)
                throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            mGp.callbackStub = callback;
        }

        @Override
        public void removeCallBack(ISvcCallback callback)
                throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
            mGp.callbackStub = null;
        }

        @Override
        public void aidlConfirmReply(int confirmed)
                throws RemoteException {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, confirmed=" + confirmed);
            synchronized (mGp.syncThreadConfirm) {
                mGp.syncThreadConfirm.setExtraDataInt(confirmed);
                mGp.syncThreadConfirm.notify();
            }
        }

        @Override
        public void aidlStopService() throws RemoteException {
            stopSelf();
        }

        @Override
        public void aidlStartSpecificSyncTask(String[] job_name) throws RemoteException {
//            Thread.dumpStack();
            queueSpecificSyncTask(job_name, SYNC_REQUEST_ACTIVITY);
        }

        @Override
        public void aidlStartAutoSyncTask() throws RemoteException {
            queueAutoSyncTask(SYNC_REQUEST_ACTIVITY);
            if (!mGp.syncThreadActive) {
                startSyncThread();
            }
        }

        @Override
        public void aidlStartSchedule(String[] schedule_name_array) throws RemoteException {
            startSyncBySchedulerByNameList(schedule_name_array);
        }

//        @Override
//        public void aidlCancelSyncTask() throws RemoteException {
//            cancelSyncTask();
//        }

        @Override
        public void aidlReloadTaskList() throws RemoteException {

        }

        @Override
        public void aidlSetActivityInBackground() throws RemoteException {
            setActivityBackground();
        }

        @Override
        public void aidlSetActivityInForeground() throws RemoteException {
            setActivityForeground();
        }

    };

    private void setActivityForeground() {
        mGp.activityIsBackground = false;
        NotificationUtils.setNotificationEnabled(mGp, false);
        stopForeground(true);
        NotificationUtils.clearNotification(mGp, mUtil);
    }

    private void setActivityBackground() {
        mGp.activityIsBackground = true;
        NotificationUtils.setNotificationEnabled(mGp, true);
        if (mGp.syncThreadActive) startForeground(R.string.app_name, mGp.notification);
    }

    private void cancelSyncTask() {
        synchronized (mGp.syncThreadCtrl) {
            mGp.syncThreadCtrl.setDisabled();
            mGp.syncThreadCtrl.notify();
        }
    }

    private SyncTaskItem getSyncTask(String job_name) {
        for (SyncTaskItem sji : mGp.syncTaskList) {
            if (sji.getSyncTaskName().equals(job_name)) {
                return sji;
            }
        }
        return null;
    }

    private boolean isSyncTaskAlreadyScheduled(ArrayBlockingQueue<SyncRequestItem> srq, String task_name) {
        boolean result=false;
        for(SyncRequestItem sri:srq) {
            for(SyncTaskItem sti:sri.sync_task_list) {
                if (sti.getSyncTaskName().equals(task_name)) {
                    result=true;
                    break;
                }
            }
            if (result) break;
        }
        return result;
    }

    private void queueSpecificSyncTask(String job_name[], String req_id, ScheduleListAdapter.ScheduleListItem si) {
        SyncRequestItem sri = new SyncRequestItem();
        sri.schedule_name=si.scheduleName;
        sri.wifi_off_after_sync_ended = si.syncWifiOffAfterEnd;
        sri.wifi_on_before_sync_start = si.syncWifiOnBeforeStart;
        sri.start_delay_time_after_wifi_on = si.syncDelayAfterWifiOn;
        sri.overrideSyncOptionCharge=si.syncOverrideOptionCharge;
        sri.requestor = req_id;
        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(mContext, req_id);
        if (job_name != null && job_name.length > 0) {
            for (int i = 0; i < job_name.length; i++) {
                if (getSyncTask(job_name[i]) != null) {
                    if (!getSyncTask(job_name[i]).isSyncFolderStatusError()) {
                        if (isSyncTaskAlreadyScheduled(mGp.syncRequestQueue, job_name[i])) {
                            if (si.scheduleName.equals("")) {
                                mUtil.addLogMsg("W", "", String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued),
                                        job_name[i], sri.requestor_display));
                            } else {
                                mUtil.addLogMsg("W", "", String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
                                        sri.schedule_name, job_name[i], sri.requestor_display));
                            }
                        } else {
                            sri.sync_task_list.add(getSyncTask(job_name[i]).clone());
                            if (si.scheduleName.equals("")) {
                                mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted),
                                        job_name[i], sri.requestor_display));
                            } else {
                                mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_schedule),
                                        sri.schedule_name, job_name[i], sri.requestor_display));
                            }
                        }
                    } else {
                        mUtil.addLogMsg("W", "",
                                String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_task_is_error),
                                        job_name[i], sri.requestor_display));
                    }
                } else {
                    mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_main_sync_selected_task_not_found, job_name[i]));
                }
            }
            if (sri.sync_task_list.size() > 0) {
                mGp.syncRequestQueue.add(sri);
            } else {
//                mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_specified_sync_task_not_scheduled));
            }
        } else {
//            mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_specified_sync_task_not_scheduled));
        }
    }

    private void queueSpecificSyncTask(String job_name[], String req_id) {
        ScheduleListAdapter.ScheduleListItem si=new ScheduleListAdapter.ScheduleListItem();
        si.scheduleName="";
        si.syncWifiOnBeforeStart=false;
        si.syncDelayAfterWifiOn=0;
        si.syncWifiOffAfterEnd=false;
        si.syncOverrideOptionCharge= ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
        queueSpecificSyncTask(job_name, req_id, si);
        if (!mGp.syncThreadActive) {
            startSyncThread();
        }
    }

    private void queueAutoSyncTask(String req_id, ScheduleListAdapter.ScheduleListItem si) {
        int cnt = 0;
        SyncRequestItem sri = new SyncRequestItem();
        sri.requestor = req_id;
        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(mContext, req_id);
        sri.schedule_name=si.scheduleName;
        sri.wifi_off_after_sync_ended = si.syncWifiOffAfterEnd;
        sri.wifi_on_before_sync_start = si.syncWifiOnBeforeStart;
        sri.start_delay_time_after_wifi_on = si.syncDelayAfterWifiOn;
        sri.overrideSyncOptionCharge=si.syncOverrideOptionCharge;
        synchronized (mGp.syncRequestQueue) {
            for (SyncTaskItem sji : mGp.syncTaskList) {
                if (sji.isSyncTaskAuto() && !sji.isSyncTestMode()) {
                    String[] job_name=new String[]{sji.getSyncTaskName()};
                    if (!sji.isSyncFolderStatusError()) {
                        if (isSyncTaskAlreadyScheduled(mGp.syncRequestQueue, job_name[0])) {
                            mUtil.addLogMsg("W","",
                                    String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
                                            sri.schedule_name, job_name[0], sri.requestor));
                        } else {
                            cnt++;
                            if (si.scheduleName.equals("")) {
                                mUtil.addLogMsg("I","",
                                        String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted),
                                                job_name[0], sri.requestor));
                            } else {
                                mUtil.addLogMsg("I","",
                                        String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_schedule),
                                                sri.schedule_name, job_name[0], sri.requestor));
                            }
                            sri.sync_task_list.add(sji.clone());
                        }
                    } else {
                        mUtil.addLogMsg("W","",
                                String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_task_is_error),
                                        job_name[0], sri.requestor));
                    }
                }
            }
            if (cnt == 0) {
                mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_auto_sync_task_not_found));
                NotificationUtils.showOngoingMsg(mGp, mUtil, System.currentTimeMillis(),
                        mContext.getString(R.string.msgs_auto_sync_task_not_found));
            } else {
                mGp.syncRequestQueue.add(sri);
            }
        }
    }

    private void queueAutoSyncTask(String req_id) {
        ScheduleListAdapter.ScheduleListItem si=new ScheduleListAdapter.ScheduleListItem();
        si.scheduleName="";
        si.syncWifiOnBeforeStart=false;
        si.syncDelayAfterWifiOn=0;
        si.syncWifiOffAfterEnd=false;
        si.syncOverrideOptionCharge= ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
        queueAutoSyncTask(req_id, si);
    }

    private void startSyncThread() {
        if (!mGp.syncThreadEnabled) {
            mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_svc_can_not_start_sync_task_disabled));
            return;
        }
        if (NotificationUtils.isNotificationEnabled(mGp))
            startForeground(R.string.app_name, mGp.notification);
        if (mGp.syncRequestQueue.size() > 0) {
            mGp.acquireWakeLock(mContext, mUtil);
            NotifyEvent ntfy = new NotifyEvent(this);
            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                @Override
                public void positiveResponse(Context c, Object[] o) {
                    mSyncThreadResult = (int) o[0];
                    mGp.releaseWakeLock(mUtil);
                    hideDialogWindow();
                    synchronized (mGp.syncRequestQueue) {
                        if (mGp.syncRequestQueue.size() > 0) {
                            showSyncEndNotificationMessage();
                            startSyncThread();
                        } else {
                            if (mGp.callbackStub == null) {
                                stopSelf();
                            } else {
                                stopForeground(true);
                                showSyncEndNotificationMessage();
                                mGp.notificationLastShowedMessage = "";
                            }
                        }
                    }
                }

                @Override
                public void negativeResponse(Context c, Object[] o) {
                    mSyncThreadResult = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    mGp.releaseWakeLock(mUtil);
                    hideDialogWindow();
                    synchronized (mGp.syncRequestQueue) {
                        mGp.syncRequestQueue.clear();
                        if (mGp.callbackStub == null) {
                            stopSelf();
                        } else {
                            stopForeground(true);
                            showSyncEndNotificationMessage();
                            mGp.notificationLastShowedMessage = "";
                        }
                    }
                }
            });

            showDialogWindow();

            Thread tm = new SyncThread(mContext, mGp, ntfy);
            tm.setName("SyncThread");
            tm.setPriority(Thread.MIN_PRIORITY);
            tm.start();
        } else {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " task has not started, queued task does not exist");
            stopForeground(true);
        }
    }

    private int mSyncThreadResult = 0;

    private void showSyncEndNotificationMessage() {
        boolean sound=false, vibration=false;
        if (mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_SUCCESS)) sound=true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_SUCCESS)) vibration=true;
        } else if (mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS)) sound=true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS)) vibration=true;
        } else if (mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ERROR)) sound=true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ERROR)) vibration=true;
        }
        boolean is_notice_message_showed=false;
        if (mGp.activityIsBackground) {
            if (mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS || mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                if (mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS) ||
                        mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_SUCCESS)) {
                    NotificationUtils.showNoticeMsg(mContext, mGp, mUtil, mGp.notificationLastShowedMessage, sound, vibration);
                    is_notice_message_showed=true;
                }
            } else if (mSyncThreadResult == SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
                if (mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS) ||
                        mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ERROR)) {
                    NotificationUtils.showNoticeMsg(mContext, mGp, mUtil, mGp.notificationLastShowedMessage, sound, vibration);
                    is_notice_message_showed=true;
                }
            }
        }
//        if (mGp.callbackStub != null && sound) playBackDefaultNotification();
//        if (mGp.callbackStub != null && vibration) vibrateDefaultPattern();
        if (!is_notice_message_showed) {
            Intent na=new Intent(mContext, ActivityNotification.class);
            na.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            na.putExtra("SOUND", sound);
            na.putExtra("SOUND_VOLUME", mGp.settingNotificationVolume);
            na.putExtra("VIBRATE", vibration);
            startActivity(na);
        }
    }

    private void showDialogWindow() {
        mGp.dialogWindowShowed = true;
        try {
            if (mGp.callbackStub != null) mGp.callbackStub.cbThreadStarted();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void hideDialogWindow() {
        mGp.dialogWindowShowed = false;
        try {
            if (mGp.callbackStub != null) mGp.callbackStub.cbThreadEnded();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    final private class SleepReceiver extends BroadcastReceiver {
        @SuppressLint({"Wakelock", "NewApi"})
        @Override
        final public void onReceive(Context c, Intent in) {
            String action = in.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
//                if (mGp.settingScreenOnIfScreenOnAtStartOfSync && mGp.syncThreadActive && !mGp.syncThreadConfirmWait) {
                if (Build.VERSION.SDK_INT<=28) {
                    if (mGp.syncThreadActive && !mGp.syncThreadConfirmWait) {
                        if (mGp.forceDimScreenWakelock.isHeld()) mGp.forceDimScreenWakelock.release();
                        mGp.forceDimScreenWakelock.acquire();
                        mUtil.addDebugMsg(1, "I", "Sleep receiver, ForceDim wake lock acquired");
                    }
                }
            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                if (mGp.settingPreventSyncStartDelay && mGp.syncThreadActive && !mGp.syncThreadConfirmWait) {
                    if (!mGp.mDimWakeLock.isHeld()) {
                        mGp.mDimWakeLock.acquire();
                        mUtil.addDebugMsg(1, "I", "Sleep receiver, Dim wake lock acquired");
                    }
                }
            }
        }
    }

}

