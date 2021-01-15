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
import android.hardware.usb.UsbManager;
import android.os.PowerManager;

import com.sentaroh.android.SMBSync3.Log.LogUtil;
import com.sentaroh.android.Utilities3.MiscUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sentaroh.android.SMBSync3.Constants.*;

import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_SET_TIMER;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_TIMER_EXPIRED;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_SCHEDULE_NAME_KEY;

public class SyncReceiver extends BroadcastReceiver {
    private static Logger slf4jLog = LoggerFactory.getLogger(SyncReceiver.class);

    private static Context mContext = null;

    private static GlobalParameters mGp = null;

    private static LogUtil mLog = null;

    @Override
    final public void onReceive(Context c, Intent received_intent) {
        PowerManager.WakeLock wl =((PowerManager) c.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Receiver");
        try {wl.acquire(1000); } catch(Exception e) {};
        mContext = c;
        if (mLog == null) mLog = new LogUtil(c, "Receiver");
        if (mGp == null) {
            mGp =GlobalWorkArea.getGlobalParameter(c);
            if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "config load started");
            mGp.loadConfigList(c);
            if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "config load ended");
        }

        String action = received_intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                    action.equals(Intent.ACTION_DATE_CHANGED) ||
                    action.equals(Intent.ACTION_TIMEZONE_CHANGED) ||
                    action.equals(Intent.ACTION_TIME_CHANGED) ||
                    action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                for (ScheduleListAdapter.ScheduleListItem si : mGp.syncScheduleList) si.scheduleLastExecTime = System.currentTimeMillis();
                TaskListImportExport.saveTaskListToAppDirectory(c, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                ScheduleUtils.setTimer(mContext, mGp, mLog);
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED) ||
//                    action.equals(Intent.ACTION_MEDIA_EJECT) ||
//                    action.equals(Intent.ACTION_MEDIA_REMOVED) ||
                    action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) ||
                    action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)
                ) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                Intent in = new Intent(mContext, SyncService.class);
                in.setAction(action);
                in.setData(received_intent.getData());
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    e.printStackTrace();
                    mLog.addDebugMsg(1,"E", "startService filed, action="+action+", error=" + e.getMessage());
                    mLog.addDebugMsg(1,"E", MiscUtil.getStackTraceString(e));
                }
            } else if (action.equals(SCHEDULE_INTENT_SET_TIMER)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                ScheduleUtils.setTimer(mContext, mGp, mLog);
            } else if (action.equals(SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                ScheduleUtils.setTimerIfNotSet(mContext, mGp, mLog);
            } else if (action.equals(SCHEDULE_INTENT_TIMER_EXPIRED)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                if (received_intent.getExtras().containsKey(SCHEDULE_SCHEDULE_NAME_KEY)) {
                    Intent send_intent = new Intent(mContext, SyncService.class);
                    send_intent.setAction(SCHEDULE_INTENT_TIMER_EXPIRED);
                    send_intent.putExtra(SCHEDULE_SCHEDULE_NAME_KEY, received_intent.getStringExtra(SCHEDULE_SCHEDULE_NAME_KEY));
                    try {
                        mContext.startService(send_intent);
                    } catch(Exception e) {
                        mLog.addDebugMsg(1,"E", "startService filed, action="+action+", error=" + e.getMessage());
                        mLog.addDebugMsg(1,"E", MiscUtil.getStackTraceString(e));
                    }
                    String[] schedule_list=received_intent.getStringExtra(SCHEDULE_SCHEDULE_NAME_KEY).split(",");
                    for (String sched_name:schedule_list) {
                        if (ScheduleUtils.getScheduleItem(mGp.syncScheduleList, sched_name) != null) {
                            ScheduleUtils.getScheduleItem(mGp.syncScheduleList, sched_name).scheduleLastExecTime = System.currentTimeMillis();
                        }
                    }
                    TaskListImportExport.saveTaskListToAppDirectory(c, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                    ScheduleUtils.setTimer(mContext, mGp, mLog);
                }
            } else if (action.equals(START_SYNC_INTENT)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                Intent in = new Intent(mContext, SyncService.class);
                in.setAction(START_SYNC_INTENT);
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    mLog.addDebugMsg(1,"E", "Start intent error=" + e.getMessage());
                    mLog.addDebugMsg(1,"E", MiscUtil.getStackTraceString(e));
                }
            } else if (action.equals(START_SYNC_AUTO_INTENT)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                Intent in = new Intent(mContext, SyncService.class);
                in.setAction(START_SYNC_AUTO_INTENT);
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    mLog.addDebugMsg(1,"E", "Start intent error=" + e.getMessage());
                    mLog.addDebugMsg(1,"E", MiscUtil.getStackTraceString(e));
                }
            } else if (action.equals(QUERY_SYNC_TASK_INTENT)) {
                if (mLog.getLogLevel()>0) mLog.addDebugMsg(1, "I", "Receiver action=" + action);
                Intent in = new Intent(mContext, SyncService.class);
                in.setAction(QUERY_SYNC_TASK_INTENT);
                if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
                try {
                    mContext.startService(in);
                } catch(Exception e) {
                    mLog.addDebugMsg(1,"E", "Start intent error=" + e.getMessage());
                    mLog.addDebugMsg(1,"E", MiscUtil.getStackTraceString(e));
                }
            } else {
                mLog.addDebugMsg(1, "I", "Receiver action=" + action);
            }
        }
    }

}
