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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import static com.sentaroh.android.SMBSync3.Constants.*;

import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_TIMER_EXPIRED;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_SCHEDULE_NAME_KEY;
//import static com.sentaroh.android.SMBSync3.SyncWorker.WORKER_ACTION_KEY;

public class SyncReceiver extends BroadcastReceiver {
    final private static Logger log = LoggerFactory.getLogger(SyncReceiver.class);

    private Context mContext = null;

    private GlobalParameters mGp = null;

    private CommonUtilities mUtil = null;

    @Override
    final public void onReceive(Context c, Intent received_intent) {
        PowerManager.WakeLock wl =((PowerManager) c.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Receiver");
        try {wl.acquire(1000); } catch(Exception e) {};
        mContext = c;
        if (mGp == null) {
            mGp =GlobalWorkArea.getGlobalParameter(c);
        }
        if (mUtil == null) mUtil = new CommonUtilities(c, "Receiver", mGp, null);
        if (mUtil.getLogLevel()>0) mUtil.addDebugMsg(1, "I", "config load started");
        mGp.loadConfigList(c);
        if (mUtil.getLogLevel()>0) mUtil.addDebugMsg(1, "I", "config load ended");

        String action = received_intent.getAction();
        mUtil.addDebugMsg(1, "I", "Receiver received action=" + action);
        if (action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                    action.equals(Intent.ACTION_DATE_CHANGED) ||
                    action.equals(Intent.ACTION_TIMEZONE_CHANGED) ||
                    action.equals(Intent.ACTION_TIME_CHANGED) ||
                    action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                for (ScheduleListAdapter.ScheduleListItem si : mGp.syncScheduleList) si.scheduleLastExecTime = System.currentTimeMillis();
                TaskListImportExport.saveTaskListToAppDirectory(c, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                ScheduleUtils.setTimer(mContext, mGp, mUtil.getLogUtil());
            } else if (action.equals(SCHEDULE_INTENT_TIMER_EXPIRED)) {
                if (received_intent.getExtras().containsKey(SCHEDULE_SCHEDULE_NAME_KEY)) {
                    SyncWorker.startSyncWorkerByAction(mContext, mGp, mUtil, SCHEDULE_INTENT_TIMER_EXPIRED,
                            received_intent.getStringExtra(SCHEDULE_SCHEDULE_NAME_KEY), "");
                    String[] schedule_list=received_intent.getStringExtra(SCHEDULE_SCHEDULE_NAME_KEY).split(",");
                    for (String sched_name:schedule_list) {
                        if (ScheduleUtils.getScheduleItem(mGp.syncScheduleList, sched_name) != null) {
                            ScheduleUtils.getScheduleItem(mGp.syncScheduleList, sched_name).scheduleLastExecTime = System.currentTimeMillis();
                        }
                    }
                    TaskListImportExport.saveTaskListToAppDirectory(c, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                    ScheduleUtils.setTimer(mContext, mGp, mUtil.getLogUtil());
                }
            } else {
                mUtil.addDebugMsg(1, "I", "Receiver ignored action=" + action);
            }
        }
    }

}
