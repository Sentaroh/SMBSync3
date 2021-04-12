package com.sentaroh.android.SMBSync3;

/*
The MIT License (MIT)
Copyright (c) 2021 Sentaroh

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

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import static com.sentaroh.android.SMBSync3.Constants.APPLICATION_TAG;
import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ERROR;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_SUCCESS;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ERROR;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_SOUND_WHEN_SYNC_ENDED_SUCCESS;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ERROR;
import static com.sentaroh.android.SMBSync3.Constants.NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_SUCCESS;
import static com.sentaroh.android.SMBSync3.Constants.START_SYNC_AUTO_INTENT;
import static com.sentaroh.android.SMBSync3.Constants.START_SYNC_INTENT;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_EXTERNAL;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SCHEDULE;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_TIMER_EXPIRED;

public class SyncWorker extends Worker {
    private static final Logger log= LoggerFactory.getLogger(SyncWorker.class);
    private GlobalParameters mGp=null;
    private boolean mWorkerStopped=false;
    private CommonUtilities mUtil = null;


    final static public String WORKER_TAG="SyncWorker";
//    final static public String WORKER_ACTION_KEY="worker_action_key";
//    final static public String WORKER_SYNC_REUEST_ITEM_KEY="worker_sync_request_key";

    public Context mContext=null;

    public SyncWorker (@NonNull Context c, @NonNull WorkerParameters workerParams) {
        super(c, workerParams);
        mContext=c;
        if (!GlobalWorkArea.isGlobalParameterCreated()) {
            mGp=GlobalWorkArea.getGlobalParameter(mContext);
            mUtil = new CommonUtilities(mContext, "SyncWorker", mGp, null);
            NotificationUtils.initNotification(mGp, mUtil, mContext);
            mUtil.addDebugMsg(1, "I", "Configuration load started");
            mGp.loadConfigList(mContext);
            mUtil.addDebugMsg(1, "I", "Configuration load ended");
        } else {
            mGp=GlobalWorkArea.getGlobalParameter(mContext);
            if (mGp.notificationManager==null) NotificationUtils.initNotification(mGp, mUtil, mContext);
            mUtil = new CommonUtilities(mContext, "SyncWorker", mGp, null);
        }

        mUtil.addDebugMsg(1, "I", "Init SyncWorker entered");

        NotificationUtils.clearAllNotification(mGp, mUtil);

        mUtil.addDebugMsg(1, "I", "SyncWorker started" +" API=" + Build.VERSION.SDK_INT +", Version " + SystemInfo.getApplVersionNameCode(mContext));

        if (mGp.syncHistoryList == null)
            mGp.syncHistoryList = mUtil.loadHistoryList();

    }

    @Override
    public void onStopped() {
        mUtil.addDebugMsg(1, "I", "onStopped entered");
        mWorkerStopped=true;
        mGp.syncThreadCtrl.setDisabled();
    }

    @Override
    public Result doWork() {
//        mContext=getApplicationContext();
        mGp.setSyncWorkerActive(true);

        mUtil.addDebugMsg(1, "I", "doWork entered");

        NotificationUtils.setNotificationIcon(mGp, mUtil, R.drawable.ic_48_smbsync_run_anim, R.drawable.ic_48_smbsync_run);
        ForegroundInfo fg=new ForegroundInfo(mGp.notificationOngoingMessageID, mGp.notification);
        setForegroundAsync(fg);

        SyncWorker.listWorkerEnqueuedItem(mContext, mGp, mUtil, WorkManager.getInstance(mContext), WORKER_TAG);

        if (Looper.myLooper()==null) Looper.prepare();

//        final Data input_data=getInputData();

        if (!mGp.syncThreadActive) {
            if (mGp.syncRequestQueue.size()>0) {
                NotifyEvent ntfy_thread=new NotifyEvent(mContext);
                ntfy_thread.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        int result_code = (int) o[0];
                        mGp.releaseWakeLock(mUtil);
                        hideDialogWindow();
                        synchronized (mGp.syncRequestQueue) {
                            if (mGp.syncRequestQueue.size() > 0) {
                                showSyncEndNotificationMessage(result_code);
                                mUtil.addDebugMsg(1, "I", "Start SyncThread for new sync reuest detected.");
                                startSyncThread(ntfy_thread);
                            } else {
                                showSyncEndNotificationMessage(result_code);
                                mGp.notificationLastShowedMessage = "";
                                synchronized(WORKER_TAG) {WORKER_TAG.notify();}
                            }
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mGp.releaseWakeLock(mUtil);
                        hideDialogWindow();
                        synchronized (mGp.syncRequestQueue) {
                            mGp.syncRequestQueue.clear();
                            showSyncEndNotificationMessage(SyncTaskItem.SYNC_RESULT_STATUS_ERROR);
                            mGp.notificationLastShowedMessage = "";
                            synchronized(WORKER_TAG) {WORKER_TAG.notify();}
                        }
                    }
                });

                startSyncThread(ntfy_thread);

                //Wait until SyncThread ended
                synchronized(WORKER_TAG) {try {WORKER_TAG.wait();} catch (Exception e) {e.printStackTrace();}}

            } else {
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()," Queued task does not exist");
            }
        } else {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()," SyncThread already active");
        }

        return terminateWorker();

    }

    private Result terminateWorker() {
        mUtil.addDebugMsg(1, "I", "SyncWorker ended");

        mGp.setSyncWorkerActive(false);

        mUtil.flushLog();
        CommonUtilities.saveMessageList(mContext, mGp);

        NotificationUtils.clearOngoingNotification(mGp, mUtil);
//        Thread th=new Thread() {
//            @Override
//            public void run() {
//                SystemClock.sleep(1000);
////                WorkManager.getInstance(mContext).cancelAllWorkByTag(WORKER_TAG);
////                WorkManager.getInstance(mContext).pruneWork();
//                NotificationUtils.clearOngoingNotification(mGp, mUtil);
//            }
//        };
//        th.start();

        mGp.notificationLastShowedMessage="";
        mGp.notificationLastShowedTitle="";
        if (mGp.progressSpinSynctask!=null) mGp.progressSpinSynctask.setText(APPLICATION_TAG);
        if (mGp.progressSpinMsg!=null) mGp.progressSpinMsg.setText("");


        return Result.success();
    }

    private void startSyncThread(NotifyEvent p_ntfy) {
        mGp.acquireWakeLock(mContext, mUtil);

        showDialogWindow();

        Thread tm = new SyncThread(mContext, mGp, p_ntfy);
        tm.setName("SyncThread");
        tm.setPriority(Thread.MIN_PRIORITY);
        tm.start();
    }

    private void showDialogWindow() {
        mGp.dialogWindowShowed = true;
        if (mGp.callbackShowDialogWindow != null) mGp.callbackShowDialogWindow.onCallBack(mContext, true, null);
    }

    private void hideDialogWindow() {
        mGp.dialogWindowShowed = false;
        if (mGp.callbackHideDialogWindow != null) mGp.callbackHideDialogWindow.onCallBack(mContext, false, null);
    }


    private void showSyncEndNotificationMessage(int result_code) {
        boolean sound = false, vibration = false;
        NotificationUtils.clearOngoingNotification(mGp, mUtil);
        if (result_code == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_SUCCESS))
                sound = true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_SUCCESS))
                vibration = true;
        } else if (result_code == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS))
                sound = true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS))
                vibration = true;
        } else if (result_code == SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
            if (mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationSoundWhenSyncEnded.equals(NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ERROR))
                sound = true;
            if (mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
                    mGp.settingNotificationVibrateWhenSyncEnded.equals(NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ERROR))
                vibration = true;
        }

        if (!mGp.activityIsForeground) {
            //When show notice message
            if (result_code == SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
                if (mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS) ||
                        mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ERROR)) {
                    NotificationUtils.showNoticeMsg(mContext, mGp, mUtil, mGp.notificationLastShowedMessage);
                }
            } else {
                if (mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS) ||
                        mGp.settingNotificationMessageWhenSyncEnded.equals(NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_SUCCESS)) {
                    NotificationUtils.showNoticeMsg(mContext, mGp, mUtil, mGp.notificationLastShowedMessage);
                }
            }
        }
        if (sound) playBackDefaultNotification(mContext, mGp, mUtil, mGp.settingNotificationVolume);
        if (vibration) vibrateDefaultPattern(mContext, mGp, mUtil);
    }

    static private void playBackDefaultNotification(Context c, GlobalParameters gp, CommonUtilities cu, int volume) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri != null) {
            final MediaPlayer player = MediaPlayer.create(c, uri);
            if (player != null) {
                float vol = (float) volume / 100.0f;
                player.setVolume(vol, vol);
                if (player != null) {
                    final Thread th = new Thread() {
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
                cu.addDebugMsg(1, "I", "Default playback is can not initialized.");
            }
        } else {
            cu.addDebugMsg(1, "I", "Default ringtone not found.");
        }
    }

    static private void vibrateDefaultPattern(Context c, GlobalParameters gp, CommonUtilities cu) {
        Thread th = new Thread() {
            @Override
            public void run() {
                Vibrator vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(new long[]{0, 300, 200, 300}, -1);
            }
        };
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
    }

//    private ScheduleListAdapter.ScheduleListItem getScheduleInformation(ArrayList<ScheduleListAdapter.ScheduleListItem> sl, String name) {
//        for (ScheduleListAdapter.ScheduleListItem si : sl) {
//            if (si.scheduleName.equals(name))
//                return si;
//        }
//        return null;
//    }

    private SyncTaskItem getSyncTask(String job_name) {
        for (SyncTaskItem sji : mGp.syncTaskList) {
            if (sji.getSyncTaskName().equals(job_name)) {
                return sji;
            }
        }
        return null;
    }

    static public boolean startSpecificSyncTask(Context c, GlobalParameters gp, CommonUtilities lu, String requestor, String task_name) {
        boolean task_queued=false;
        String[] task_name_array=new String[]{task_name};
        task_queued=startSpecificSyncTask(c, gp, lu, requestor, task_name_array);
        return task_queued;
    }

    static public boolean startSpecificSyncTask(Context c, GlobalParameters gp, CommonUtilities lu, String requestor, String[] task_name_array) {
        boolean task_queued=false;
        SyncRequestItem sri=new SyncRequestItem();
        sri.requestor=requestor;
        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(c, sri.requestor);
        lu.addDebugMsg(1, "I", "startSpecificSyncTask schedule task="+ stringArrayToString(task_name_array));
        buildSyncTaskListFromList(c, gp, lu, sri, task_name_array);
        if (sri.sync_task_list.size()>0) {
            task_queued=true;
            beginSyncWorker(c, gp, lu, sri);
        }
        return task_queued;
    }

    static private String stringArrayToString(String[] array) {
        String out="", sep="";
        if (array!=null) {
            for(String item:array) {
                out+=sep+item;
                sep=",";
            }
        }
        return "["+out+"]";
    }

    static public boolean startSpecificSchedule(Context c, GlobalParameters gp, CommonUtilities lu, String requestor, String sched_name) {
        boolean task_queued=false;
        SyncRequestItem sri=new SyncRequestItem();
        sri.requestor=requestor;
        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(c, sri.requestor);
        lu.addDebugMsg(1, "I", "startSpecificSchedule schedule="+sched_name);
        String task_list="";
        ScheduleListAdapter.ScheduleListItem sli=ScheduleUtils.getScheduleItem(gp.syncScheduleList, sched_name);
        if (sli==null) {
            lu.addDebugMsg(1, "I", "startSpecificSchedule schedule not found, schedule="+sched_name);
        } else {
            task_list=sli.syncTaskList;
            if (task_list.equals("")) buildAutoSyncTaskList(c, gp, lu, sri);
            else buildSyncTaskListFromList(c, gp, lu, sri, task_list);
        }
        if (sri.sync_task_list.size()>0) {
            task_queued=true;
            beginSyncWorker(c, gp, lu, sri);
        }
        return task_queued;
    }

    static public void startSyncWorkerByAction(Context c, GlobalParameters gp, CommonUtilities lu,
                                               String action, String schedule_item_name, String task_name_list) {
        SyncRequestItem sri=new SyncRequestItem();
        lu.addDebugMsg(1, "I", "startSyncWorkerByAction action="+action+", schedule="+schedule_item_name+", task="+task_name_list);
        if (action.equals(SCHEDULE_INTENT_TIMER_EXPIRED)) {
            ScheduleListAdapter.ScheduleListItem sched_item=ScheduleUtils.getScheduleItem(gp.syncScheduleList, schedule_item_name);
            sri.schedule_name=schedule_item_name;
            sri.requestor=SYNC_REQUEST_SCHEDULE;
            sri.schedule_name = sched_item.scheduleName;
            sri.wifi_off_after_sync_ended = sched_item.syncWifiOffAfterEnd;
            sri.wifi_on_before_sync_start = sched_item.syncWifiOnBeforeStart;
            sri.start_delay_time_after_wifi_on = sched_item.syncDelayAfterWifiOn;
            sri.overrideSyncOptionCharge = sched_item.syncOverrideOptionCharge;
            sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(c, sri.requestor);
            if (sched_item!=null) {
                if (sched_item.syncTaskList.equals("")) {
                    lu.addDebugMsg(1, "I", "startSyncWorkerByAction schedule auto sync");
                    buildAutoSyncTaskList(c, gp, lu, sri);
                } else {
                    lu.addDebugMsg(1, "I", "startSyncWorkerByAction schedule task list="+sched_item.syncTaskList);
                    buildSyncTaskListFromList(c, gp, lu, sri, sched_item.syncTaskList);
                }
                beginSyncWorker(c, gp, lu, sri);
            } else {

            }
        } else if (action.equals(START_SYNC_INTENT)) {
            sri.requestor=SYNC_REQUEST_EXTERNAL;
            sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(c, sri.requestor);
            lu.addDebugMsg(1, "I", "startSyncWorkerByAction schedule task list="+task_name_list);
            buildSyncTaskListFromList(c, gp, lu, sri, task_name_list);
            beginSyncWorker(c, gp, lu, sri);
        } else if (action.equals(START_SYNC_AUTO_INTENT)) {
            sri.requestor=SYNC_REQUEST_EXTERNAL;
            sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(c, sri.requestor);
            lu.addDebugMsg(1, "I", "startSyncWorkerByAction schedule auto sync");
            buildAutoSyncTaskList(c, gp, lu, sri);
            beginSyncWorker(c, gp, lu, sri);
        } else {
            lu.addDebugMsg(1, "I", "startSyncWorkerByAction Unsupported action, action="+action);
        }

    }

    static public void beginSyncWorker(Context c, GlobalParameters gp, CommonUtilities lu, SyncRequestItem sri) {
        if (sri.sync_task_list.size()>0) {
            gp.syncRequestQueue.add(sri);
            if (gp.isSyncWorkerActive()) {
                lu.addDebugMsg(1, "I", "SyncWorker is already started.");
            } else {
                WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG);
                WorkManager.getInstance().pruneWork();
                OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
//                        .setInputData(new Data.Builder().putString(WORKER_ACTION_KEY, SCHEDULE_INTENT_TIMER_EXPIRED).build())
                        .addTag(WORKER_TAG)
                        .build();
                WorkManager.getInstance().enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.KEEP, req);
            }
        }
    }

    static private void buildAutoSyncTaskList(Context c, GlobalParameters gp, CommonUtilities lu, SyncRequestItem sri) {
        for(SyncTaskItem item:gp.syncTaskList) {
            if (item.isSyncTaskAuto() && !item.isSyncTestMode()) {
                if (!isSyncTaskAlreadyScheduled(gp.syncRequestQueue, item.getSyncTaskName())) sri.sync_task_list.add(item);
                else {
                    if (sri.schedule_name.equals("")) {
                        lu.addLogMsg("W", "", String.format(c.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued),
                                item.getSyncTaskName(), sri.requestor_display));
                    } else {
                        lu.addLogMsg("W", "", String.format(c.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
                                sri.schedule_name, item.getSyncTaskName(), sri.requestor_display));
                    }
                }
            }
        }
    }

    static private void buildSyncTaskListFromList(Context c, GlobalParameters gp, CommonUtilities lu, SyncRequestItem sri, String task_list) {
        String[] task_list_array=task_list.split(NAME_LIST_SEPARATOR);
        buildSyncTaskListFromList(c, gp, lu, sri, task_list_array);
    }

    static private void buildSyncTaskListFromList(Context c, GlobalParameters gp, CommonUtilities lu, SyncRequestItem sri, String[] task_list_array) {
        for(String tn:task_list_array) {
            SyncTaskItem sti=getSyncTask(gp.syncTaskList, tn);
            if (sti!=null) {
                if (!isSyncTaskAlreadyScheduled(gp.syncRequestQueue, sti.getSyncTaskName())) sri.sync_task_list.add(sti);
                else {
                    if (sri.schedule_name.equals("")) {
                        lu.addLogMsg("W", "", String.format(c.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued),
                                sti.getSyncTaskName(), sri.requestor_display));
                    } else {
                        lu.addLogMsg("W", "", String.format(c.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
                                sri.schedule_name, sti.getSyncTaskName(), sri.requestor_display));
                    }
                }
            } else {
                lu.addLogMsg("W", "", c.getString(R.string.msgs_main_sync_selected_task_not_found, tn));
            }
        }
    }

    static public boolean isWorkerEnqueued(Context c, GlobalParameters gp, CommonUtilities lu, WorkManager workManager, String tag) {
//        lu.addDebugMsg(1, "I", "gp.syncWorkerIsActive2="+gp.isSyncWorkerActive());
//        if (gp.isSyncWorkerActive()) return true;
//        else return false;
        ListenableFuture<List<WorkInfo>> future =  workManager.getWorkInfosByTag(tag);
        try {
            for (WorkInfo workInfo : future.get()){
                lu.addDebugMsg(1, "I", "state="+workInfo.getState());
                if (//workInfo.getState() == WorkInfo.State.BLOCKED ||
                        workInfo.getState() == WorkInfo.State.ENQUEUED ||
                                workInfo.getState() == WorkInfo.State.RUNNING ) {
                    return true;
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    static public void listWorkerEnqueuedItem(Context c, GlobalParameters gp, CommonUtilities lu, WorkManager workManager, String tag) {
        ListenableFuture<List<WorkInfo>> future =  workManager.getWorkInfosByTag(tag);
        try {
            int count=0;
            for (WorkInfo workInfo : future.get()){
                count++;
                lu.addDebugMsg(1, "I", "Worker ID="+workInfo.getId()+", tag="+workInfo.getTags()+", state="+workInfo.getState());
            }
            if (count==0) lu.addDebugMsg(1, "I", "Worker work does not exists.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static private SyncTaskItem getSyncTask(ArrayList<SyncTaskItem> list, String job_name) {
        for (SyncTaskItem sji : list) {
            if (sji.getSyncTaskName().equals(job_name)) {
                return sji;
            }
        }
        return null;
    }

    static private boolean isSyncTaskAlreadyScheduled(ArrayBlockingQueue<SyncRequestItem> srq, String task_name) {
        boolean result = false;
        for (SyncRequestItem sri : srq) {
            for (SyncTaskItem sti : sri.sync_task_list) {
                if (sti.getSyncTaskName().equals(task_name)) {
                    result = true;
                    break;
                }
            }
            if (result) break;
        }
        return result;
    }

//    private boolean startSyncByAnotherAppl(Data in) {
//        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_external));
//        boolean task_queued=false;
//        if (in.getString(START_SYNC_EXTRA_PARM_SYNC_TASK)!=null) {
//            String t_sp = in.getString(START_SYNC_EXTRA_PARM_SYNC_TASK);
//            task_queued=checkAndQueueSyncTaskExists(SYNC_REQUEST_EXTERNAL,
//                    t_sp, mContext.getString(R.string.msgs_svc_received_start_request_from_external_task_not_found),
//                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_list));
//        } else if (in.getString(START_SYNC_EXTRA_PARM_SYNC_GROUP)!=null) {
//            String t_sg = in.getString(START_SYNC_EXTRA_PARM_SYNC_GROUP);
//            String t_sp = "";
//            for (GroupListAdapter.GroupListItem item : mGp.syncGroupList) {
//                if (item.groupName.equalsIgnoreCase(t_sg)) {
//                    t_sp = item.taskList;
//                }
//            }
//            if (!t_sp.equals("")) {
//                task_queued=checkAndQueueSyncTaskExists(SYNC_REQUEST_EXTERNAL, t_sp,
//                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_task_not_found),
//                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_list));
//            } else {
//                mUtil.addLogMsg("W", "",
//                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_group_not_found) + t_sg);
//                NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
//                        mContext.getString(R.string.msgs_svc_received_start_request_from_external_group_not_found) + t_sg);
//            }
//        } else {
//            mUtil.addLogMsg("W", "",
//                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
//            NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
//                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
//        }
//        return task_queued;
//    }
//
//    private boolean startSyncByShortcut(Data in) {
//        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_shortcut));
//        boolean task_queued=false;
//        if (in.getString(START_SYNC_EXTRA_PARM_SYNC_TASK)!=null) {
//            String t_sp = in.getString(START_SYNC_EXTRA_PARM_SYNC_TASK);
//            String[] sp = t_sp.split(NAME_LIST_SEPARATOR);
//            task_queued=queueSpecificSyncTask(sp, SYNC_REQUEST_SHORTCUT);
//        } else {
//            mUtil.addLogMsg("W", "",
//                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
//            NotificationUtils.showOngoingMsg(mGp, mUtil, 0,
//                    mContext.getString(R.string.msgs_svc_received_start_request_from_external_no_task_specified));
//        }
//        return task_queued;
//    }
//
//    private boolean startSyncByScheduler(Data in) {
//        boolean task_queued=false;
//        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//
//        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler));
//
//        if (in.getString(SCHEDULE_SCHEDULE_NAME_KEY)!=null) {
//            String schedule_name_list = in.getString(SCHEDULE_SCHEDULE_NAME_KEY);
//
//            mUtil.addDebugMsg(1, "I", "Schedule information, name=" + schedule_name_list);
//
//            String[] schedule_list = schedule_name_list.split(NAME_LIST_SEPARATOR);
//
//            task_queued=startSyncBySchedulerByNameList(schedule_list);
//        }
//        return task_queued;
//    }
//
//    private boolean startSyncBySchedulerByNameList(String[] schedule_list) {
//        boolean task_queued=false;
//        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//
//        for (String schedule_name : schedule_list) {
//            mUtil.addDebugMsg(1, "I", "Schedule start, name=" + schedule_name);
//            ScheduleListAdapter.ScheduleListItem si = getScheduleInformation(mGp.syncScheduleList, schedule_name);
//            if (si != null) {
//                if (si.syncAutoSyncTask) {
//                    task_queued=queueAutoSyncTask(SYNC_REQUEST_SCHEDULE, si);
//                } else {
//                    if (si.syncTaskList != null && si.syncTaskList.length() > 0) {
//                        task_queued=checkAndQueueSyncTaskExists(SYNC_REQUEST_SCHEDULE, si.syncTaskList,
//                                mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_task_not_found),
//                                mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_no_task_list));
//                    } else {
//                        mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_svc_received_start_request_from_scheduler_no_task_list));
//                    }
//                }
//            } else {
//                mUtil.addLogMsg("W", "", "Specified schedule name was not found, name=", schedule_name);
//            }
//        }
//        return task_queued;
////        if (task_queued && !mGp.syncThreadActive) {
////            startSyncThread();
////        }
//    }
//
//    private boolean checkAndQueueSyncTaskExists(String requestor, String task_list, String not_found_msg, String no_task_msg) {
//        boolean task_queued=false;
//        String[] sp = task_list.split(NAME_LIST_SEPARATOR);
//        ArrayList<String> pl = new ArrayList<String>();
//        for (int i = 0; i < sp.length; i++) {
//            if (TaskListUtils.getSyncTaskByName(mGp.syncTaskList, sp[i]) != null) {
//                pl.add(sp[i]);
//            } else {
//                mUtil.addLogMsg("W", "", not_found_msg + sp[i]);
//                NotificationUtils.showOngoingMsg(mGp, mUtil, 0, not_found_msg + sp[i]);
//                SyncThread.sendEndNotificationIntent(mContext, mUtil, requestor, sp[i], 9);
//            }
//        }
//        if (pl.size() > 0) {
//            String[] nspl = new String[pl.size()];
//            for (int i = 0; i < pl.size(); i++) nspl[i] = pl.get(i);
//            task_queued=queueSpecificSyncTask(nspl, requestor);
//        } else {
//            mUtil.addLogMsg("W", "", no_task_msg);
//            NotificationUtils.showOngoingMsg(mGp, mUtil, 0, no_task_msg);
//        }
//        return task_queued;
//    }
//
//    private boolean queueSpecificSyncTask(String job_name[], String req_id, ScheduleListAdapter.ScheduleListItem si) {
//        boolean task_queued=false;
//        SyncRequestItem sri = new SyncRequestItem();
//        sri.schedule_name = si.scheduleName;
//        sri.wifi_off_after_sync_ended = si.syncWifiOffAfterEnd;
//        sri.wifi_on_before_sync_start = si.syncWifiOnBeforeStart;
//        sri.start_delay_time_after_wifi_on = si.syncDelayAfterWifiOn;
//        sri.overrideSyncOptionCharge = si.syncOverrideOptionCharge;
//        sri.requestor = req_id;
//        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(mContext, req_id);
//        if (job_name != null && job_name.length > 0) {
//            for (int i = 0; i < job_name.length; i++) {
//                if (getSyncTask(job_name[i]) != null) {
//                    if (!getSyncTask(job_name[i]).isSyncFolderStatusError()) {
//                        if (isSyncTaskAlreadyScheduled(mGp.syncRequestQueue, job_name[i])) {
//                            if (si.scheduleName.equals("")) {
//                                mUtil.addLogMsg("W", "", String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued),
//                                        job_name[i], sri.requestor_display));
//                            } else {
//                                mUtil.addLogMsg("W", "", String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
//                                        sri.schedule_name, job_name[i], sri.requestor_display));
//                            }
//                        } else {
//                            sri.sync_task_list.add(getSyncTask(job_name[i]).clone());
//                            task_queued=true;
//                            if (si.scheduleName.equals("")) {
//                                mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted),
//                                        job_name[i], sri.requestor_display));
//                            } else {
//                                mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_schedule),
//                                        sri.schedule_name, job_name[i], sri.requestor_display));
//                            }
//                        }
//                    } else {
//                        mUtil.addLogMsg("W", "",
//                                String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_task_is_error),
//                                        job_name[i], sri.requestor_display));
//                    }
//                } else {
//                    mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_main_sync_selected_task_not_found, job_name[i]));
//                }
//            }
//            if (sri.sync_task_list.size() > 0) {
//                mGp.syncRequestQueue.add(sri);
//            } else {
////                mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_specified_sync_task_not_scheduled));
//                mUtil.addDebugMsg(1, "W", "queueSpecificSyncTask sync request not created.");
//            }
//        } else {
////            mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_specified_sync_task_not_scheduled));
//            mUtil.addDebugMsg(1, "W", "queueSpecificSyncTask invalid task name list received, name="+job_name);
//        }
//        return task_queued;
//    }
//
//    private boolean queueSpecificSyncTask(String job_name[], String req_id) {
//        boolean task_queued=false;
//        ScheduleListAdapter.ScheduleListItem si = new ScheduleListAdapter.ScheduleListItem();
//        si.scheduleName = "";
//        si.syncWifiOnBeforeStart = false;
//        si.syncDelayAfterWifiOn = 0;
//        si.syncWifiOffAfterEnd = false;
//        si.syncOverrideOptionCharge = ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
//        task_queued=queueSpecificSyncTask(job_name, req_id, si);
//        return task_queued;
//    }
//
//    private boolean queueAutoSyncTask(String req_id) {
//        boolean task_queued=false;
//        ScheduleListAdapter.ScheduleListItem si = new ScheduleListAdapter.ScheduleListItem();
//        si.scheduleName = "";
//        si.syncWifiOnBeforeStart = false;
//        si.syncDelayAfterWifiOn = 0;
//        si.syncWifiOffAfterEnd = false;
//        si.syncOverrideOptionCharge = ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
//        task_queued=queueAutoSyncTask(req_id, si);
//        return task_queued;
//    }
//
//    private boolean queueAutoSyncTask(String req_id, ScheduleListAdapter.ScheduleListItem si) {
//        int cnt = 0;
//        boolean task_queued=false;
//        SyncRequestItem sri = new SyncRequestItem();
//        sri.requestor = req_id;
//        sri.requestor_display = HistoryListAdapter.HistoryListItem.getSyncStartRequestorDisplayName(mContext, req_id);
//        sri.schedule_name = si.scheduleName;
//        sri.wifi_off_after_sync_ended = si.syncWifiOffAfterEnd;
//        sri.wifi_on_before_sync_start = si.syncWifiOnBeforeStart;
//        sri.start_delay_time_after_wifi_on = si.syncDelayAfterWifiOn;
//        sri.overrideSyncOptionCharge = si.syncOverrideOptionCharge;
//        synchronized (mGp.syncRequestQueue) {
//            for (SyncTaskItem sji : mGp.syncTaskList) {
//                if (sji.isSyncTaskAuto() && !sji.isSyncTestMode()) {
//                    String[] job_name = new String[]{sji.getSyncTaskName()};
//                    if (!sji.isSyncFolderStatusError()) {
//                        if (isSyncTaskAlreadyScheduled(mGp.syncRequestQueue, job_name[0])) {
//                            mUtil.addLogMsg("W", "",
//                                    String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_already_task_queued_schedule),
//                                            sri.schedule_name, job_name[0], sri.requestor));
//                        } else {
//                            cnt++;
//                            if (si.scheduleName.equals("")) {
//                                mUtil.addLogMsg("I", "",
//                                        String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted),
//                                                job_name[0], sri.requestor));
//                            } else {
//                                mUtil.addLogMsg("I", "",
//                                        String.format(mContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_schedule),
//                                                sri.schedule_name, job_name[0], sri.requestor));
//                            }
//                            sri.sync_task_list.add(sji.clone());
//                            task_queued=true;
//                        }
//                    } else {
//                        mUtil.addLogMsg("W", "",
//                                String.format(mContext.getString(R.string.msgs_svc_received_start_request_ignored_task_is_error),
//                                        job_name[0], sri.requestor));
//                    }
//                }
//            }
//            if (cnt == 0) {
//                mUtil.addLogMsg("E", "", mContext.getString(R.string.msgs_auto_sync_task_not_found));
//                NotificationUtils.showOngoingMsg(mGp, mUtil, System.currentTimeMillis(),
//                        mContext.getString(R.string.msgs_auto_sync_task_not_found));
//            } else {
//                mGp.syncRequestQueue.add(sri);
//            }
//        }
//        return task_queued;
//    }

}
