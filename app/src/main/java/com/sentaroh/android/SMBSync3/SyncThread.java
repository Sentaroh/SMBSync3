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

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;

import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sentaroh.android.SMBSync3.Constants.*;

public class SyncThread extends Thread {
    final private static Logger log= LoggerFactory.getLogger(SyncThread.class);
    private GlobalParameters mGp = null;

    private NotifyEvent mNotifyToService = null;

    public final static int SYNC_RETRY_INTERVAL = 30;

    class SyncThreadWorkArea {
        public GlobalParameters gp = null;
        public int logLevel=0;
        public Context appContext=null;
        public Handler uiHandler=null;

        public ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> currLastModifiedList = new ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry>();
        public ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> newLastModifiedList = new ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry>();

        public long fileSizeFilterValue=0L;
        public long fileDateFilterValue=0L;

        public Pattern fileExcludeFilterFileNamePattern = null;
        public Pattern fileIncludeFilterFileNamePattern = null;

        public Pattern fileExcludeFilterWithDirectoryPathPattern = null;
        public Pattern fileIncludeFilterWithDirectoryPathPattern = null;

        public ArrayList<String> matchFromBeginIncludeDirectoryList=new ArrayList<String>();
        public ArrayList<Pattern> matchFromBeginIncludeDirectoryListPattern=new ArrayList<Pattern>();

        public ArrayList<String> matchFromBeginExcludeDirectoryList=new ArrayList<String>();
        public ArrayList<Pattern> matchFromBeginExcludeDirectoryListPattern=new ArrayList<Pattern>();

        public ArrayList<String> matchAnyWhereExcludeDirectoryList =new ArrayList<String>();
        public ArrayList<Pattern> matchAnyWhereExcludeDirectoryListPattern =new ArrayList<Pattern>();

//        public Pattern directoryExcludePatternMatchFromBegin = null;
//        public Pattern directoryExcludePatternMatchAnyPosition = null;
//        public Pattern directoryIncludePatternMatchFromBegin = null;
////        public Pattern directoryIncludePatternMatchAnyPosition = null;
//        public ArrayList<String[]> directoryExcludeFilterStringByNameMatchFromBegin = null;
//        public ArrayList<String[]> directoryIncludeFilterStringByNameMatchFromBegin = null;
//        public ArrayList<String[]> directoryExcludeFilterStringByNameMatchAnyPosition = null;
////        public ArrayList<String[]> directoryIncludeFilterStringByNameMatchAnyPosition = null;

        public final boolean ALL_COPY = false;

        public int retryCount=0;

        public long totalTransferByte = 0, totalTransferTime = 0;
        public int totalCopyCount, totalDeleteCount, totalIgnoreCount = 0, totalMoveCount=0, totalRetryCount = 0, totalReplaceCount=0;

        public boolean lastModifiedIsFunctional = true;

        public JcifsAuth sourceSmbAuth =null;
        public String sourceSmbHost =null;
        public JcifsAuth destinationSmbAuth =null;
        public String destinationSmbHost =null;

        public int jcifsNtStatusCode=0;

        public CommonUtilities util = null;

        public MediaScannerConnection mediaScanner = null;

        public PrintWriter syncHistoryWriter = null;

        public int syncDifferentFileAllowableTime = 0;
        public int offsetOfDaylightSavingTime=0;

        public boolean localFileLastModListModified = false;

        public int confirmCopyResult = 0, confirmDeleteResult = 0, confirmMoveResult = 0, confirmArchiveResult=0;

        public ArrayList<String> smbFileList = null;

        public String exception_msg_area = "";

        public SyncTaskItem currentSTI = null;

        public SafFile3 lastWriteSafFile=null;
        public long syncBeginTime=0L;

        public String currentRequestor ="";
        public String currentRequestorDisplay ="";
    }

    private SyncThreadWorkArea mStwa = new SyncThreadWorkArea();

    public SyncThread(Context c, GlobalParameters gp, NotifyEvent ne) {
        mGp = gp;
        mStwa.appContext=c;
        mNotifyToService = ne;
        mStwa.util = new CommonUtilities(c, "SyncThread", mGp, null);
        mStwa.gp = mGp;
        mStwa.logLevel=mStwa.util.getLogLevel();

        mStwa.offsetOfDaylightSavingTime=TimeZone.getDefault().getDSTSavings();
        mStwa.uiHandler=new Handler();

        mGp.safMgr.refreshSafList();
        mGp.initJcifsOption(c);

    }

    private void listStorageInfo() {
        if (mStwa.logLevel>0) {
            ArrayList<String> sil= CommonUtilities.listSystemInfo(mStwa.appContext, mGp);
            for(String item:sil) mStwa.util.addDebugMsg(1, "I", item);
        }
    }

    @Override
    public void run() {
        if (!mGp.syncThreadActive) {
            mGp.syncThreadActive = true;
            defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);

            prepareMediaScanner();

            listStorageInfo();

            NotificationUtils.setNotificationIcon(mGp, mStwa.util, R.drawable.ic_48_smbsync_run_anim, R.drawable.ic_48_smbsync_run);

            loadLocalFileLastModList();

            mGp.syncThreadCtrl.initThreadCtrl();

            SyncRequestItem sri = mGp.syncRequestQueue.poll();
            boolean sync_error_detected = false;
            int sync_result = 0;
            boolean wifi_off_after_end = false;

            while (sri != null && sync_result == 0) {
                mStwa.currentRequestor =sri.requestor;
                mStwa.currentRequestorDisplay =sri.requestor_display;
                mStwa.util.addLogMsg("I", "", String.format(mStwa.appContext.getString(R.string.msgs_mirror_sync_request_started), sri.requestor_display));
                mStwa.util.addDebugMsg(1, "I", "Sync request option : Requestor=" + sri.requestor +
                        ", WiFi on=" + sri.wifi_on_before_sync_start +
                        ", WiFi delay=" + sri.start_delay_time_after_wifi_on + ", WiFi off=" + sri.wifi_off_after_sync_ended+", OverrideCharge="+sri.overrideSyncOptionCharge);

                boolean wifi_on_issued=performWiFiOnIfRequired(sri);

                mStwa.currentSTI = sri.sync_task_list.poll();
                long start_time = 0;

                while (mStwa.currentSTI != null &&
                        (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS ||
                         sync_result == SyncTaskItem.SYNC_RESULT_STATUS_WARNING ||
                         sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SKIP)) {
                    start_time = System.currentTimeMillis();
                    sendStartNotificationIntent(mStwa.appContext, mStwa.util, sri.requestor, mStwa.currentSTI.getSyncTaskName());

                    listSyncOption(mStwa.currentSTI);
                    setSyncTaskRunning(true);
                    showMsg(mStwa, false, mStwa.currentSTI.getSyncTaskName(), "I", "", "", mStwa.appContext.getString(R.string.msgs_mirror_task_started));

                    sync_result=initSyncParms(sri, mStwa.currentSTI);

                    if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                        sync_result = compileFilter(mStwa.currentSTI, mStwa.currentSTI.getFileNameFilter(), mStwa.currentSTI.getDirectoryFilter());
                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                            sync_result = performSync(mStwa.currentSTI);
                        }
                    }

                    saveLocalFileLastModList();

                    CommonUtilities.saveMessageList(mStwa.appContext, mGp);

                    postProcessSyncResult(mStwa.currentSTI, sync_result, (System.currentTimeMillis() - start_time), sri.requestor);
                    sendEndNotificationIntent(mStwa.appContext, mStwa.util, sri.requestor, mStwa.currentSTI.getSyncTaskName(), sync_result);
                    if ((mStwa.currentSTI != null || mGp.syncRequestQueue.size() > 0) &&
                            mStwa.currentSTI.getSyncTaskErrorOption()==SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_SKIP_UNCOND &&
                            sync_result == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR) {
                        showMsg(mStwa, false, mStwa.currentSTI.getSyncTaskName(), "W", "", "",
                                mStwa.appContext.getString(R.string.msgs_mirror_task_result_error_ignored));
                        sync_error_detected = true;
                        sync_result = SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
                    }
                    mStwa.currentSTI = sri.sync_task_list.poll();
                }

                if (sri.wifi_off_after_sync_ended && wifi_on_issued) wifi_off_after_end = true;
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL || sync_result== SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
                    //Put not executed sync task
                    putNotExcutedTaskname(sri, mStwa.currentSTI, sri.requestor_display);
                } else {
                    //Continue sync
                    mStwa.util.addLogMsg("I", "", String.format(mStwa.appContext.getString(R.string.msgs_mirror_sync_request_ended), sri.requestor_display));
                    sri = mGp.syncRequestQueue.poll();
                }
            }

            if (Build.VERSION.SDK_INT<29 && wifi_off_after_end) {
                if (isWifiOn()) {
                    mStwa.util.addDebugMsg(1, "I", "WiFi off issued");
                    setWifiOff();
                }
            }

            if (sync_error_detected) {
                showMsg(mStwa, false, "", "W", "", "",
                        mStwa.appContext.getString(R.string.msgs_mirror_task_sync_request_error_detected));
            }

            saveLocalFileLastModList();
            CommonUtilities.saveMessageList(mStwa.appContext, mGp);

            NotificationUtils.setNotificationIcon(mGp, mStwa.util, R.drawable.ic_48_smbsync_wait, R.drawable.ic_48_smbsync_wait);
            NotificationUtils.reShowOngoingMsg(mGp, mStwa.util);

            mGp.syncThreadActive = false;

            closeMediaScanner();

            mNotifyToService.notifyToListener(true, new Object[]{sync_result});
        }
        System.gc();
    }

    static public void sendStartNotificationIntent(Context c, CommonUtilities cu, String requestor, String task_name) {
        if (!requestor.equals(SYNC_REQUEST_EXTERNAL)) return ;
        Intent in = new Intent(BROADCAST_INTENT_SYNC_STARTED);
        in.putExtra(START_SYNC_EXTRA_PARM_SYNC_RESULT_TASK_NAME_KEY, task_name);
        c.sendBroadcast(in, null);
        cu.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Task="+task_name);
    }

    static public void sendEndNotificationIntent(Context c, CommonUtilities cu, String requestor, String task_name, int sync_result) {
        if (!requestor.equals(SYNC_REQUEST_EXTERNAL)) return ;
        Intent in = new Intent(BROADCAST_INTENT_SYNC_ENDED);
        String rc="";
        if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) rc= START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_SUCCESS;
        else if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_ERROR) rc= START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_ERROR;
        else if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) rc= START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_CANCEL;
        else if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_WARNING) rc= START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_WARNING;
        else rc= START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_NOT_FOUND;
        in.putExtra(START_SYNC_EXTRA_PARM_SYNC_RESULT_TASK_NAME_KEY, task_name);
        in.putExtra(START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_KEY,rc);
        c.sendBroadcast(in, null);
        cu.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Task="+task_name+", result="+rc);
    }

    private boolean performWiFiOnIfRequired(SyncRequestItem sri) {
        boolean wifi_on_issued=false;
        if (sri.wifi_on_before_sync_start) {
            if (!isWifiOn()) {
                if (Build.VERSION.SDK_INT<29) {
                    wifi_on_issued=true;
                    setWifiOn();
                    if (sri.start_delay_time_after_wifi_on > 0) {
                        mStwa.util.addLogMsg("I", String.format(mStwa.appContext.getString(R.string.msgs_mirror_sync_start_was_delayed), sri.start_delay_time_after_wifi_on));
                        SystemClock.sleep(1000 * sri.start_delay_time_after_wifi_on);
                        if (!isWifiOn()) {
                            mStwa.util.addLogMsg("E",mStwa.appContext.getString(R.string.msgs_mirror_sync_wifi_can_not_enabled));
                        }
                    }
                    mStwa.util.addDebugMsg(1, "I", "WiFi IP Addr="+CommonUtilities.getIfIpAddress("wlan0"));
                } else {
                    mStwa.util.addDebugMsg(1, "I", "performWiFiOnIfRequired ignored");
                }
            }
        }
        return wifi_on_issued;
    }

    private void putNotExcutedTaskname(SyncRequestItem sri, SyncTaskItem sti, String prev_req_id) {
        ArrayList<String> task_list=new ArrayList<String>();
        ArrayList<String> sched_list=new ArrayList<String>();
        ArrayList<String> req_list=new ArrayList<String>();
        while(sti!=null) {
            task_list.add(sti.getSyncTaskName());
            sched_list.add(sri.schedule_name);
//            String req_display_name=HistoryListItem.getSyncStartRequestorDisplayName(mStwa.appContext, sri.request_id);
            req_list.add(prev_req_id);
            sti=sri.sync_task_list.poll();
        }
        sri = mGp.syncRequestQueue.poll();
        while(sri!=null) {
            sti=sri.sync_task_list.poll();
            while(sti!=null) {
                task_list.add(sti.getSyncTaskName());
                sched_list.add(sri.schedule_name);
                req_list.add(sri.requestor);
                sti=sri.sync_task_list.poll();
            }
            sri = mGp.syncRequestQueue.poll();
        }
        if (task_list.size()>0) {
            mStwa.util.addLogMsg("W", "",
                    mStwa.appContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_but_not_executed_title));
            for(int i=0;i<task_list.size();i++) {
                if (sched_list.get(i).equals("")) {
                    mStwa.util.addLogMsg("W","",
                            mStwa.appContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_but_not_executed_msg,
                                    task_list.get(i), req_list.get(i)));
                } else {
                    mStwa.util.addLogMsg("W","",
                            mStwa.appContext.getString(R.string.msgs_svc_received_start_sync_task_request_accepted_but_not_executed_msg_schedule,
                            sched_list.get(i), task_list.get(i), req_list.get(i)));
                }
            }

        }

        mStwa.util.addLogMsg("I", "", String.format(mStwa.appContext.getString(R.string.msgs_mirror_sync_request_ended), prev_req_id));

    }

    private void setSyncTaskRunning(boolean running) {
        SyncTaskItem c_sti = TaskListUtils.getSyncTaskByName(mGp.syncTaskList, mStwa.currentSTI.getSyncTaskName());

        c_sti.setSyncTaskRunning(running);

        if (running) openSyncResultLog(c_sti);
        else closeSyncResultLog();

        refreshSyncTaskListAdapter();
    }

    private void listSyncOption(SyncTaskItem sti) {
        mStwa.util.addDebugMsg(1, "I", "Sync Task : Type=" + sti.getSyncTaskType());
        String mst_uid="";
        mStwa.util.addDebugMsg(1, "I", "   Source Type=" + sti.getSourceFolderType() +
                ", SMB Protocol=" + sti.getSourceSmbProtocol() +
                ", SMB Host=" + sti.getSourceSmbHost() +
                ", SMB Port=" + sti.getSourceSmbPort() +
                ", SMB Share=" + sti.getSourceSmbShareName() +
                ", SMB Account name=" + (sti.getSourceSmbAccountName().equals("")?"":"????????")+
                ", SMB Account password=" + (sti.getSourceSmbAccountPassword().equals("")?"":"********")+
                ", Directory=" + sti.getSourceDirectoryName() +
                ", StorageUuid=" + sti.getSourceStorageUuid()+
                "");
        mStwa.util.addDebugMsg(1, "I", "   Destination Type=" + sti.getDestinationFolderType() +
                ", SMB Protocol=" + sti.getDestinationSmbProtocol() +
                ", SMB Host=" + sti.getDestinationSmbHost() +
                ", SMB Port=" + sti.getDestinationSmbPort() +
                ", SMB Share=" + sti.getDestinationSmbShareName() +
                ", SMB Account name=" + (sti.getDestinationSmbAccountName().equals("")?"":"????????")+
                ", SMB Account password=" + (sti.getDestinationSmbPassword().equals("")?"":"********")+
                ", Directory=" + sti.getDestinationDirectoryName() +
                ", StorageUuid=" + sti.getDestinationStorageUuid() +
                "");
        mStwa.util.addDebugMsg(1, "I", "   Sync option :");
        mStwa.util.addDebugMsg(1, "I", "      Auto sync task=" + sti.isSyncTaskAuto());
        mStwa.util.addDebugMsg(1, "I", "      Test mode=" + sti.isSyncTestMode());
        mStwa.util.addDebugMsg(1, "I", "      Sync only charging=" + sti.isSyncOptionSyncWhenCharging());
        mStwa.util.addDebugMsg(1, "I", "      Process destination root directory file=" + sti.isSyncProcessRootDirFile());
        mStwa.util.addDebugMsg(1, "I", "      Confirm override/delete file=" + sti.isSyncConfirmOverrideOrDelete());

        mStwa.util.addDebugMsg(1, "I", "      Sync task error option=" + sti.getSyncTaskErrorOption());

        mStwa.util.addDebugMsg(1, "I", "      WiFi Status Option=" + sti.getSyncOptionWifiStatusOption());

        mStwa.util.addDebugMsg(1, "I", "      Allow all IP address=" + sti.isSyncOptionSyncAllowAllIpAddress());

        mStwa.util.addDebugMsg(1, "I", "      Sync Subdirectory=" + sti.isSyncOptionSyncSubDirectory());
        mStwa.util.addDebugMsg(1, "I", "      Sync Empty Directory=" + sti.isSyncOptionSyncEmptyDirectory());
        mStwa.util.addDebugMsg(1, "I", "      Sync Hidden Directory=" + sti.isSyncOptionSyncHiddenDirectory());
        mStwa.util.addDebugMsg(1, "I", "      Sync Hidden File=" + sti.isSyncOptionSyncHiddenFile());
        mStwa.util.addDebugMsg(1, "I", "      Sync Override copy/move file=" + sti.isSyncOverrideCopyMoveFile());

        mStwa.util.addDebugMsg(1, "I", "      Network error retry count=" + sti.getSyncOptionRetryCount());
        mStwa.util.addDebugMsg(1, "I", "      Use small I/O buffer=" + sti.isSyncOptionUseSmallIoBuffer());

        mStwa.util.addDebugMsg(1, "I", "      Delete first when mirror=" + sti.isSyncOptionDeleteFirstWhenMirror());
        mStwa.util.addDebugMsg(1, "I", "      Remove directory/File that excluded by filter=" + sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter());
        mStwa.util.addDebugMsg(1, "I", "      Do not reset file last modified time=" + sti.isSyncDoNotResetFileLastModified());

        mStwa.util.addDebugMsg(1, "I", "      Use file size to determine if files are different=" + sti.isSyncOptionDifferentFileBySize());
        mStwa.util.addDebugMsg(1, "I", "      Sync different file size greater than destination file=" + sti.isSyncDifferentFileSizeGreaterThanDestinationFile());
        mStwa.util.addDebugMsg(1, "I", "      Use time of last modification to determine if files are different=" + sti.isSyncOptionDifferentFileByTime());
        mStwa.util.addDebugMsg(1, "I", "      Min allowed time difference(in seconds) between source and destination files=" + sti.getSyncOptionDifferentFileAllowableTime());
        mStwa.util.addDebugMsg(1, "I", "      Do not overwrite destination file if it is newer than the Source file="+sti.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile());

        mStwa.util.addDebugMsg(1, "I", "      Ignore DST Difference=" + sti.isSyncOptionIgnoreDstDifference());
        mStwa.util.addDebugMsg(1, "I", "      Offset of DST Value(Min)=" + sti.getSyncOptionOffsetOfDst());

        mStwa.util.addDebugMsg(1, "I", "      Skip directory and file names that contain invalid character="+sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters());
        mStwa.util.addDebugMsg(1, "I", "      Delete empty directory when sync type MOVE=" + sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty());
        mStwa.util.addDebugMsg(1, "I", "      Confirm exif date when exif data does not exists=" + sti.isSyncOptionConfirmNotExistsExifDate());
        mStwa.util.addDebugMsg(1, "I", "      Ignore source files that are lager than 4GB when sync to external storage=" + sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb());

        mStwa.util.addDebugMsg(1, "I", "   SMB1 Option, LM Compatiibility=" + mGp.settingsSmbLmCompatibility +
                ", Use extended security=" + mGp.settingsSmbUseExtendedSecurity +
                ", Client reponse timeout=" + mGp.settingsSmbClientResponseTimeout +
                ", Disable plain text passwords=" + mGp.settingsSmbDisablePlainTextPasswords +
                "");
    }

    private int initSyncParms(SyncRequestItem sri, SyncTaskItem sti) {
        int sync_result=0;
        String mst_dom=null, mst_user=null, mst_pass=null;
        mst_dom=sti.getSourceSmbDomain().equals("")?null:sti.getSourceSmbDomain();
        mst_user=sti.getSourceSmbAccountName().equals("")?null:sti.getSourceSmbAccountName();
        mst_pass=sti.getSourceSmbAccountPassword().equals("")?null:sti.getSourceSmbAccountPassword();
        Properties prop=new Properties();
        prop.setProperty(JCIFS_OPTION_CLIENT_RESPONSE_TIMEOUT, mGp.settingsSmbClientResponseTimeout);
        if (sti.getSourceSmbProtocol().equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
            try {
                mStwa.sourceSmbAuth =new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, mst_dom, mst_user, mst_pass);
            } catch(JcifsException e) {
                e.printStackTrace();
                String e_msg=String.format("JcifsException occured while %s file creation, SMB_Level=%s error=%s", "Source", sti.getSourceSmbProtocol(), e.getMessage());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                mGp.syncThreadCtrl.setThreadMessage(e_msg);
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } else {
            try {
                mStwa.sourceSmbAuth =new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, mst_dom, mst_user, mst_pass, prop);
            } catch(JcifsException e) {
                e.printStackTrace();
                String e_msg=String.format("JcifsException occured while %s file creation, SMB_Level=%s error=%s", "Source", sti.getSourceSmbProtocol(), e.getMessage());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                mGp.syncThreadCtrl.setThreadMessage(e_msg);
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        }

        String tgt_dom=null, tgt_user=null, tgt_pass=null;
        tgt_dom=sti.getDestinationSmbDomain().equals("")?null:sti.getDestinationSmbDomain();
        tgt_user=sti.getDestinationSmbAccountName().equals("")?null:sti.getDestinationSmbAccountName();
        tgt_pass=sti.getDestinationSmbPassword().equals("")?null:sti.getDestinationSmbPassword();
        if (sti.getDestinationSmbProtocol().equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
            try {
                mStwa.destinationSmbAuth =new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, tgt_dom, tgt_user, tgt_pass);
            } catch(JcifsException e) {
                e.printStackTrace();
                String e_msg=String.format("JcifsException occured while %s file creation, SMB_Level=%s error=%s", "Destination", sti.getSourceSmbProtocol(), e.getMessage());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                mGp.syncThreadCtrl.setThreadMessage(e_msg);
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } else {
            try {
                mStwa.destinationSmbAuth =new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, tgt_dom, tgt_user, tgt_pass, prop);
            } catch(JcifsException e) {
                e.printStackTrace();
                String e_msg=String.format("JcifsException occured while %s file creation, SMB_Level=%s error=%s", "Destination", sti.getSourceSmbProtocol(), e.getMessage());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                mGp.syncThreadCtrl.setThreadMessage(e_msg);
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        }

        mStwa.syncDifferentFileAllowableTime = sti.getSyncOptionDifferentFileAllowableTime() * 1000;//Convert to milisec
        mStwa.offsetOfDaylightSavingTime=sti.getSyncOptionOffsetOfDst()*60*1000;//Convert to milisec

        mStwa.totalTransferByte = mStwa.totalTransferTime = 0;
        mStwa.totalCopyCount = mStwa.totalDeleteCount = mStwa.totalIgnoreCount = mStwa.totalRetryCount = mStwa.totalMoveCount=mStwa.totalReplaceCount=0;

        if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            SafFile3 sf=mStwa.gp.safMgr.getRootSafFile(sti.getDestinationStorageUuid());
            if (sf!=null) {
                String app_dir=sf.getAppDirectoryFiles();
                if (app_dir!=null) mStwa.lastModifiedIsFunctional = isSetLastModifiedFunctional(app_dir);
                else mStwa.lastModifiedIsFunctional=false;
            } else {
                mStwa.lastModifiedIsFunctional =false;
            }
        } else if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            mStwa.lastModifiedIsFunctional = true;
        } else mStwa.lastModifiedIsFunctional = false;
        mStwa.util.addDebugMsg(1, "I", "lastModifiedIsFunctional=" + mStwa.lastModifiedIsFunctional);

        sync_result=checkNetworkOption(mStwa.currentSTI);
        if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS || sync_result== SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
            boolean charge_status=mStwa.currentSTI.isSyncOptionSyncWhenCharging();
            if (sri.overrideSyncOptionCharge.equals(ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_ENABLED)) {
                charge_status=true;
                if (mStwa.currentSTI.isSyncOptionSyncWhenCharging()!=charge_status) {
                    mStwa.util.addDebugMsg(1, "I", "Charge staus option was enabled by schedule option.");
                }
            } else if (sri.overrideSyncOptionCharge.equals(ScheduleListAdapter.ScheduleListItem.OVERRIDE_SYNC_OPTION_DISABLED)) {
                charge_status=false;
                if (mStwa.currentSTI.isSyncOptionSyncWhenCharging()!=charge_status) {
                    mStwa.util.addDebugMsg(1, "I", "Charge staus option was disabled by schedule option.");
                }
            }
            if ((charge_status && CommonUtilities.isCharging(mStwa.appContext, mStwa.util)) || !charge_status) {
                sync_result = checkStorageAccess(mStwa.currentSTI);
                if (sync_result==SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) sync_result = checkSmbAccess(mStwa.currentSTI);
            } else {
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                String be = mStwa.appContext.getString(R.string.msgs_mirror_sync_cancelled_battery_option_not_satisfied);
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", be);
                mGp.syncThreadCtrl.setThreadMessage(be);
            }
        }
        return sync_result;
    }

    private void postProcessSyncResult(SyncTaskItem sti, int sync_result, long et, String req_id) {
        int t_et_sec = (int) (et / 1000);
        int t_et_ms = (int) (et - (t_et_sec * 1000));

        String sync_et = String.valueOf(t_et_sec) + "." + String.format("%3d", t_et_ms).replaceAll(" ", "0");

        String error_msg = "";
        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR || sync_result == SyncTaskItem.SYNC_RESULT_STATUS_WARNING || sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SKIP) {
            error_msg = mGp.syncThreadCtrl.getThreadMessage();
        }
        String transfer_rate = calTransferRate(mStwa.totalTransferByte, mStwa.totalTransferTime);
        addHistoryList(sti, sync_result,
                mStwa.totalCopyCount, mStwa.totalDeleteCount, mStwa.totalIgnoreCount, mStwa.totalMoveCount, mStwa.totalRetryCount, mStwa.totalReplaceCount,
                error_msg, et, transfer_rate, req_id);

        showMsg(mStwa, true, sti.getSyncTaskName(), "I", "", "",
                String.format(mStwa.appContext.getString(R.string.msgs_mirror_task_no_of_copy),
                        mStwa.totalCopyCount, mStwa.totalMoveCount, mStwa.totalReplaceCount, mStwa.totalDeleteCount, mStwa.totalIgnoreCount, sync_et));
        showMsg(mStwa, true, sti.getSyncTaskName(), "I", "", "",
                String.format(mStwa.appContext.getString(R.string.msgs_mirror_task_avg_rate), transfer_rate));

        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
            showMsg(mStwa, false, sti.getSyncTaskName(), "I", "", "", mStwa.appContext.getString(R.string.msgs_mirror_task_result_ok));
        } else if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
            showMsg(mStwa, false, sti.getSyncTaskName(), "W", "", "", mStwa.appContext.getString(R.string.msgs_mirror_task_result_ok));
        } else if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
            showMsg(mStwa, false, sti.getSyncTaskName(), "W", "", "", mStwa.appContext.getString(R.string.msgs_mirror_task_result_cancel));
        } else if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SKIP) {
            showMsg(mStwa, false, sti.getSyncTaskName(), "I", "", "", mStwa.appContext.getString(R.string.msgs_mirror_task_result_skip));
        } else if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
            showMsg(mStwa, false, sti.getSyncTaskName(), "E", "", "",
                    mStwa.appContext.getString(R.string.msgs_mirror_task_result_error_ended));
        }

        setSyncTaskRunning(false);
        TaskListImportExport.saveTaskListToAppDirectory(mStwa.appContext, mGp, mStwa.util, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
    }

    private void loadLocalFileLastModList() {
        mStwa.localFileLastModListModified = false;
        NotifyEvent ntfy = new NotifyEvent(mStwa.appContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                String en = (String) o[0];
                mStwa.util.addDebugMsg(1, "W","", "Duplicate local file last modified entry was ignored, name=" + en);
            }
        });
        FileLastModifiedTime.loadLastModifiedList(mStwa.appContext, mGp.settingAppManagemsntDirectoryName, mStwa.currLastModifiedList, mStwa.newLastModifiedList, ntfy);
    }

    private void saveLocalFileLastModList() {
        if (mStwa.localFileLastModListModified) {
            long b_time = System.currentTimeMillis();
            mStwa.localFileLastModListModified = false;
            FileLastModifiedTime.saveLastModifiedList(mStwa.appContext, mGp.settingAppManagemsntDirectoryName, mStwa.currLastModifiedList, mStwa.newLastModifiedList);
            mStwa.util.addDebugMsg(1, "I", "saveLastModifiedList elapsed time=" + (System.currentTimeMillis() - b_time));
        }
    }

    private int checkStorageAccess(SyncTaskItem sti) {
        int sync_result = 0;
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL) ||
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            if (!sti.getSourceStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) {
                boolean mount=mStwa.gp.safMgr.isUuidMounted(sti.getSourceStorageUuid());
                boolean reg=mStwa.gp.safMgr.isUuidRegistered(sti.getSourceStorageUuid());
                if ( !mount||!reg ) {
                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    String e_msg = "";
                    e_msg = String.format(mStwa.appContext.getString(R.string.msgs_mirror_external_sdcard_select_required), sti.getSourceStorageUuid());
                    showMsg(mStwa, true, sti.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    return sync_result;
                }
            }
            if (!sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) {
                boolean mount=mStwa.gp.safMgr.isUuidMounted(sti.getDestinationStorageUuid());
                boolean reg=mStwa.gp.safMgr.isUuidRegistered(sti.getDestinationStorageUuid());
                if ( !mount||!reg ) {
                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    String e_msg = "";
                    e_msg = String.format(mStwa.appContext.getString(R.string.msgs_mirror_external_sdcard_select_required), sti.getDestinationStorageUuid());
                    showMsg(mStwa, true, sti.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    return sync_result;
                }
            }

        }
        if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
            if (!sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) {
                boolean mount=mStwa.gp.safMgr.isUuidMounted(sti.getDestinationStorageUuid());
                boolean reg=mStwa.gp.safMgr.isUuidRegistered(sti.getDestinationStorageUuid());
                if ( !mount||!reg ) {
                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    String e_msg = "";
                    e_msg = String.format(mStwa.appContext.getString(R.string.msgs_mirror_external_sdcard_select_required), sti.getDestinationStorageUuid());
                    showMsg(mStwa, true, sti.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    return sync_result;
                }
            }
        }

        return sync_result;
    }

    private int checkSmbAccess(SyncTaskItem sti) {
        int sync_result = 0;
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            InetAddress ia=CommonUtilities.getInetAddress(sti.getSourceSmbHost());
            boolean found=true;
            if (ia==null) {
                found=false;
            } else {
                String addr=ia.getHostAddress();
                if (addr==null) found=false;
            }
            if (!found) {
                String msg = mStwa.appContext.getString(R.string.msgs_mirror_remote_name_not_found, sti.getSourceSmbHost());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", msg);
                mGp.syncThreadCtrl.setThreadMessage(msg);
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                return sync_result;
            }
            mStwa.sourceSmbHost =CommonUtilities.buildSmbUrlAddressElement(sti.getSourceSmbHost(), sti.getSourceSmbPort());
            boolean reachable=false;
            if (sti.getSourceSmbPort().equals("")) {
                reachable=CommonUtilities.canSmbHostConnectable(sti.getSourceSmbHost());
            } else {
                try {
                    int port_no=Integer.valueOf(sti.getSourceSmbPort());
                    reachable=CommonUtilities.canSmbHostConnectable(sti.getSourceSmbHost(), port_no);
                } catch(Exception e) {
                    mStwa.util.addDebugMsg(1,"I","Invalid Source SMB port number="+sti.getSourceSmbPort());
                    reachable=CommonUtilities.canSmbHostConnectable(sti.getSourceSmbHost());
                }
            }
            mStwa.util.addDebugMsg(1,"I","Source SMB Address reachable="+reachable+", addr="+sti.getSourceSmbHost());
            if (!reachable) {
                String msg="";
                if (sti.getSourceSmbPort().equals("")) msg=mStwa.appContext.getString(R.string.msgs_mirror_smb_addr_not_connected, mStwa.sourceSmbHost);
                else msg=mStwa.appContext.getString(R.string.msgs_mirror_smb_addr_not_connected, mStwa.sourceSmbHost +":"+sti.getSourceSmbPort());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", msg);
                mGp.syncThreadCtrl.setThreadMessage(msg);
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                return sync_result;
            }
        }

        if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            InetAddress ia=CommonUtilities.getInetAddress(sti.getDestinationSmbHost());
            boolean found=true;
            if (ia==null) {
                found=false;
            } else {
                String addr=ia.getHostAddress();
                if (addr==null) found=false;
            }
            if (!found) {
                String msg = mStwa.appContext.getString(R.string.msgs_mirror_remote_name_not_found, sti.getDestinationSmbHost());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", msg);
                mGp.syncThreadCtrl.setThreadMessage(msg);
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                return sync_result;
            }
            mStwa.destinationSmbHost =CommonUtilities.buildSmbUrlAddressElement(sti.getDestinationSmbHost(), sti.getDestinationSmbPort());
            boolean reachable=false;
            if (sti.getDestinationSmbPort().equals("")) {
                reachable=CommonUtilities.canSmbHostConnectable(mStwa.destinationSmbHost);
            } else {
                try {
                    int port_no=Integer.valueOf(sti.getDestinationSmbPort());
                    reachable=CommonUtilities.canSmbHostConnectable(mStwa.destinationSmbHost, port_no);
                } catch(Exception e) {
                    mStwa.util.addDebugMsg(1,"I","Invalid Destination SMB port number="+sti.getDestinationSmbPort());
                    reachable=CommonUtilities.canSmbHostConnectable(mStwa.destinationSmbHost);
                }
            }
            mStwa.util.addDebugMsg(1,"I","Destination SMB Address reachable="+reachable+", addr="+mStwa.destinationSmbHost);
            if (!reachable) {
                String msg="";
                if (sti.getDestinationSmbPort().equals("")) msg=mStwa.appContext.getString(R.string.msgs_mirror_smb_addr_not_connected, mStwa.destinationSmbHost);
                else msg=mStwa.appContext.getString(R.string.msgs_mirror_smb_addr_not_connected, mStwa.destinationSmbHost +":"+sti.getDestinationSmbPort());
                showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", msg);
                mGp.syncThreadCtrl.setThreadMessage(msg);
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                return sync_result;
            }
        }
        return sync_result;
    }

    private boolean isIpaddressConnectable(String addr, int port) {
        boolean result = false;
        result = isIpAddressAndPortConnected(addr, port, 3500);//1000);
        return result;
    }

    final public boolean isIpAddressAndPortConnected(String address, int port, int timeout) {
        boolean reachable = false;
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(address, port)), timeout);
            reachable = true;
            socket.close();
        } catch (IOException e) {
            mStwa.util.addDebugMsg(1, "I", e.getMessage());
            for (StackTraceElement ste:e.getStackTrace()) mStwa.util.addDebugMsg(1, "I", ste.toString());
        } catch (Exception e) {
//            e.printStackTrace();
            mStwa.util.addDebugMsg(1, "I", e.getMessage());
            for (StackTraceElement ste:e.getStackTrace()) mStwa.util.addDebugMsg(1, "I", ste.toString());
        }
        return reachable;
    }

    private UncaughtExceptionHandler defaultUEH;
    private UncaughtExceptionHandler unCaughtExceptionHandler =
            new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
                    NotificationUtils.setNotificationIcon(mGp, mStwa.util, R.drawable.ic_48_smbsync_wait, R.drawable.ic_48_smbsync_wait);
                    ex.printStackTrace();
                    String st_msg = MiscUtil.getStackTraceString(ex.getStackTrace());
                    mGp.syncThreadCtrl.setThreadResultError();
                    String end_msg = ex.toString() + st_msg;
                    if (mStwa.gp.safMgr != null) {
                        String saf_msg=mStwa.gp.safMgr.getLastErrorMessage();
                        if (saf_msg.length()>0) end_msg += "\n\nSafManager Messages\n" + saf_msg;

                        File[] fl = mStwa.appContext.getExternalFilesDirs(null);
                        if (fl != null) {
                            for (File f : fl) {
                                if (f != null) end_msg += "\n" + "LocalFilesDirs=" + f.getPath();
                            }
                        }
                    }

                    mGp.syncThreadCtrl.setThreadMessage(end_msg);
                    showMsg(mStwa, true, "", "E", "", "", end_msg);
                    showMsg(mStwa, false, "", "E", "", "",
                            mStwa.appContext.getString(R.string.msgs_mirror_task_result_error_ended));

                    if (mStwa.currentSTI != null) {
                        sendEndNotificationIntent(mStwa.appContext, mStwa.util, mStwa.currentRequestor, mStwa.currentSTI.getSyncTaskName(), HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR);
                        addHistoryList(mStwa.currentSTI, HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR,
                                mStwa.totalCopyCount, mStwa.totalDeleteCount, mStwa.totalIgnoreCount, mStwa.totalMoveCount, mStwa.totalRetryCount, mStwa.totalReplaceCount,
                                end_msg, 0L, "", mStwa.currentRequestorDisplay);
//        			mUtil.saveHistoryList(mGp.syncHistoryList);
                        setSyncTaskRunning(false);
                    }
                    mGp.syncThreadCtrl.setDisabled();

                    mGp.syncThreadActive = false;
                    mGp.dialogWindowShowed = false;
                    mGp.syncRequestQueue.clear();

//                    TaskListEditUtil.saveSyncTaskListToFile(mGp, mStwa.appContext, mStwa.util, false, "", "", mGp.syncTaskList, false);
                    TaskListImportExport.saveTaskListToAppDirectory(mStwa.appContext, mGp, mStwa.util, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);

                    mNotifyToService.notifyToListener(false, null);
                    // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
                }
            };

    private void refreshSyncTaskListAdapter() {
        mStwa.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mGp.syncTaskListAdapter != null) {
                    int run_task = -1;
                    for (int i = 0; i < mGp.syncTaskList.size(); i++)
                        if (mGp.syncTaskList.get(i).isSyncTaskRunning()) run_task = i;
                    mGp.syncTaskListAdapter.notifyDataSetChanged();
//                    mGp.syncTaskListView.setSelection(run_task);
                }
            }
        });
    }

    private final static String SAF_FILE_PRIMARY_STORAGE_ANDROID_APP_DIRECTORY="%1$s/Android/data/%2$s";
    private final static String SAF_FILE_EXTERNAL_STORAGE_ANDROID_APP_DIRECTORY="/storage/%1$s/Android/data/%2$s";

    private void makeLocalCacheDirectory(String uuid) {
        if (uuid.equals(SafFile3.SAF_FILE_PRIMARY_UUID)) {
            File cd=new File(String.format(SAF_FILE_PRIMARY_STORAGE_ANDROID_APP_DIRECTORY, mGp.externalStoragePrefix, APPLICATION_ID)+"/cache");
            if (!cd.exists()) cd.mkdirs();
        } else {
            File cd=new File(String.format(SAF_FILE_EXTERNAL_STORAGE_ANDROID_APP_DIRECTORY, uuid, APPLICATION_ID)+"/cache");
            if (!cd.exists()) cd.mkdirs();
        }
    }

    private int performSync(SyncTaskItem sti) {
        int sync_result = 0;
        mStwa.syncBeginTime = System.currentTimeMillis();
        mStwa.retryCount=sti.getSyncOptionRetryCount();
        String from, from_temp, to, to_temp;
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL) &&
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            from_temp = buildStorageDir(sti.getSourceStorageUuid(), sti.getSourceDirectoryName());
            from=replaceKeywordExecutionDateValue(from_temp, mStwa.syncBeginTime);

            to = buildStorageDir(sti.getDestinationStorageUuid(), sti.getDestinationDirectoryName());

            mStwa.util.addDebugMsg(1, "I", "Sync Local-To-Local From=" + from + ", To=" + to);

            makeLocalCacheDirectory(sti.getDestinationStorageUuid());

            if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) {
                sync_result = SyncThreadSyncFile.syncCopyLocalToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
                sync_result = SyncThreadSyncFile.syncMoveLocalToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                sync_result = SyncThreadSyncFile.syncMirrorLocalToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                sync_result = SyncThreadArchiveFile.syncArchiveLocalToLocal(mStwa, sti, from, to);
            }
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL) &&
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
            from_temp = buildStorageDir(sti.getSourceStorageUuid(), sti.getSourceDirectoryName());
            from=replaceKeywordExecutionDateValue(from_temp, mStwa.syncBeginTime);

            if (sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) to = Environment.getExternalStorageDirectory().getPath()+"/"+sti.getDestinationZipOutputFileName();
            else to = "/storage/"+sti.getDestinationStorageUuid()+"/"+ sti.getDestinationZipOutputFileName();

            mStwa.util.addDebugMsg(1, "I", "Sync Local-To-ZIP From=" + from + ", To=" + to);

            makeLocalCacheDirectory(sti.getDestinationStorageUuid());

            if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) {
                sync_result = SyncThreadSyncZip.syncCopyLocalToLocalZip(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
                sync_result = SyncThreadSyncZip.syncMoveLocalToLocalZip(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                sync_result = SyncThreadSyncZip.syncMirrorLocalToLocalZip(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                showMsg(mStwa, false, sti.getSyncTaskName(), "W", "", "","The request was ignored because Zip can not be used as a destination for the archive.");
            }
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL) &&
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            //Internal to SMB
            from_temp = buildStorageDir(sti.getSourceStorageUuid(), sti.getSourceDirectoryName());
            from=replaceKeywordExecutionDateValue(from_temp, mStwa.syncBeginTime);

            to = buildSmbHostUrl(mStwa.destinationSmbHost, sti.getDestinationSmbShareName(), sti.getDestinationDirectoryName());

            mStwa.util.addDebugMsg(1, "I", "Sync Local-To-SMB From=" + from + ", To=" + to);

            if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) {
                sync_result = SyncThreadSyncFile.syncCopyLocalToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
                sync_result = SyncThreadSyncFile.syncMoveLocalToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                sync_result = SyncThreadSyncFile.syncMirrorLocalToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                sync_result = SyncThreadArchiveFile.syncArchiveLocalToSmb(mStwa, sti, from, to);
            }
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            //Local to Internal
            from_temp = buildSmbHostUrl(mStwa.sourceSmbHost, sti.getSourceSmbShareName(), sti.getSourceDirectoryName()) + "/";
            from=replaceKeywordExecutionDateValue(from_temp, mStwa.syncBeginTime);

            to = buildStorageDir(sti.getDestinationStorageUuid(), sti.getDestinationDirectoryName());

            mStwa.util.addDebugMsg(1, "I", "Sync SMB-To-Local From=" + from + ", To=" + to);

            makeLocalCacheDirectory(sti.getDestinationStorageUuid());

            if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) {
                sync_result = SyncThreadSyncFile.syncCopySmbToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
                sync_result = SyncThreadSyncFile.syncMoveSmbToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                sync_result = SyncThreadSyncFile.syncMirrorSmbToLocal(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                sync_result = SyncThreadArchiveFile.syncArchiveSmbToLocal(mStwa, sti, from, to);
            }
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            //Local to Internal
            from_temp = buildSmbHostUrl(mStwa.sourceSmbHost, sti.getSourceSmbShareName(), sti.getSourceDirectoryName()) + "/";
            from=replaceKeywordExecutionDateValue(from_temp, mStwa.syncBeginTime);

            to = buildSmbHostUrl(mStwa.destinationSmbHost, sti.getDestinationSmbShareName(), sti.getDestinationDirectoryName())+"/";

            mStwa.util.addDebugMsg(1, "I", "Sync SMB-To-SMB From=" + from + ", To=" + to);

            if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) {
                sync_result = SyncThreadSyncFile.syncCopySmbToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
                sync_result = SyncThreadSyncFile.syncMoveSmbToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                sync_result = SyncThreadSyncFile.syncMirrorSmbToSmb(mStwa, sti, from, to);
            } else if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                sync_result = SyncThreadArchiveFile.syncArchiveSmbToSmb(mStwa, sti, from, to);
            }
        } else {
            sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            String be = mStwa.appContext.getString(R.string.msgs_mirror_invalid_folder_combination, sti.getSourceFolderType(), sti.getDestinationFolderType());
            showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", be);
            mGp.syncThreadCtrl.setThreadMessage(be);
        }
        return sync_result;
    }

    public static int checkFileNameLength(SyncThreadWorkArea stwa, SyncTaskItem sti, String file_name) {
        if (file_name.getBytes().length>sti.getSyncOptionMaxDestinationFileNameLength()) {
            String e_msg="";
            e_msg=stwa.appContext.getString(R.string.msgs_mirror_ignore_file_name_gt_255_byte, sti.getSyncOptionMaxDestinationFileNameLength(), file_name.getBytes().length, file_name);
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), e_msg);
            return SyncTaskItem.SYNC_RESULT_STATUS_WARNING;
        }
        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }

    static public String replaceKeywordValue(String replaceable_string, Long taken_time_millis, long exec_time_millis) {

        String taken_val = replaceKeywordTakenDateValue(replaceable_string, taken_time_millis);
        String exec_val=replaceKeywordExecutionDateValue(taken_val, exec_time_millis);
        return exec_val;
    }

    static public String getWeekday(long time) {
        SimpleDateFormat sdf=new SimpleDateFormat("EEE");
        String e_tmp=sdf.format(time).toLowerCase();
        String e_week_day=e_tmp.endsWith(".")?e_tmp.substring(0, e_tmp.length()-1):e_tmp;
        return e_week_day;
    }

    static public String getWeekdayLong(long time) {
        SimpleDateFormat sdf=new SimpleDateFormat("EEEE");
        String e_tmp=sdf.format(time).toLowerCase();
        String e_week_day=e_tmp.endsWith(".")?e_tmp.substring(0, e_tmp.length()-1):e_tmp;
        return e_week_day;
    }

    static public String replaceKeywordTakenDateValue(String replaceable_string, Long taken_time_millis) {
        String c_date = StringUtil.convDateTimeTo_YearMonthDayHourMinSec(taken_time_millis);
        String[] array=c_date.split(" ");
        String[] date_array=array[0].split("/");
        String[] time_array=array[1].split(":");
        String c_date_yyyy = date_array[0];
        String c_date_mm = date_array[1];
        String c_date_dd = date_array[2];
        String c_date_hour = time_array[0];
        String c_date_min = time_array[1];
        String c_date_sec = time_array[2];
        SimpleDateFormat sdf = new SimpleDateFormat("DDD");
        Date c_datex = new Date();
        c_datex.setTime(taken_time_millis);
        String c_day_of_year = sdf.format(c_datex);

        sdf=new SimpleDateFormat("EEE");
        String tmp=sdf.format(taken_time_millis).toLowerCase();
        String week_day=tmp.endsWith(".")?tmp.substring(0, tmp.length()-1):tmp;

        sdf=new SimpleDateFormat("EEEE");
        String week_day_long=sdf.format(taken_time_millis).toLowerCase();

        sdf=new SimpleDateFormat("w");
        String week_no=sdf.format(taken_time_millis);

        String to_temp = null;
        to_temp = replaceKeywordTakenDateValue(replaceable_string, c_date_yyyy, c_date_mm, c_date_dd, c_date_hour, c_date_min, c_date_sec, c_day_of_year,
                week_no, week_day, week_day_long);

        return to_temp;
    }

    static public String replaceKeywordExecutionDateValue(String replaceable_string, long exec_time_millis) {
        String e_date = StringUtil.convDateTimeTo_YearMonthDayHourMinSec(exec_time_millis);
        String[] array=e_date.split(" ");
        String[] date_array=array[0].split("/");
        String[] time_array=array[1].split(":");
        String e_date_yyyy = date_array[0];
        String e_date_mm = date_array[1];
        String e_date_dd = date_array[2];
        String e_date_hour = time_array[0];
        String e_date_min = time_array[1];
        String e_date_sec = time_array[2];
        SimpleDateFormat e_sdf = new SimpleDateFormat("DDD");
        Date e_datex = new Date();
        e_datex.setTime(exec_time_millis);
        String e_day_of_year = e_sdf.format(e_datex);

        SimpleDateFormat sdf=new SimpleDateFormat("EEE");
        String tmp=sdf.format(exec_time_millis).toLowerCase();
        String week_day=tmp.endsWith(".")?tmp.substring(0, tmp.length()-1):tmp;

        sdf=new SimpleDateFormat("EEEE");
        String week_day_long=sdf.format(exec_time_millis).toLowerCase();

        sdf=new SimpleDateFormat("w");
        String week_no=sdf.format(exec_time_millis);

        String to_temp = null;
        to_temp = replaceKeywordExecutionDateValue(replaceable_string, e_date_yyyy, e_date_mm, e_date_dd, e_date_hour, e_date_min, e_date_sec, e_day_of_year,
                week_no, week_day, week_day_long);

        return to_temp;
    }

    static public boolean isDateConvertRequired(String replaceable_string) {
        if (isTakenDateConvertRequired(replaceable_string)) return true;
        else if (isExecutionDateConvertRequired(replaceable_string)) return true;
        else return false;
    }

    static public boolean isTakenDateConvertRequired(String replaceable_string) {
        boolean result=false;
        if (replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_DATE) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_TIME) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_DAY_OF_YEAR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_YEAR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_YY)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_MONTH)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_DAY)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_HOUR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_MIN)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_SEC) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_WEEK_NUMBER) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY_LONG)

        )
            result=true;
        return result;
    }

    static public String removeTakenDateParameter(String replaceable_string) {
        String result=replaceable_string.replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DATE, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_TIME, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DAY_OF_YEAR, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_YEAR, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_YY, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_MONTH, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DAY, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_HOUR, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_MIN, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_SEC, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_NUMBER, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY, "")
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY_LONG, "");

        return result;
    }

    static public String getTakenDateConversionItem(String replaceable_string) {
        String used_parm="", sep="";
        for(String item:SyncTaskItem.TEMPLATE_TAKENS) {
            if (replaceable_string.contains(item)) {
                used_parm+=sep+item;
                sep=", ";
            }
        }
        return used_parm;
    }

    static public boolean isExecutionDateConvertRequired(String replaceable_string) {
        boolean result=false;
        if (replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_DATE) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_TIME) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_DAY_OF_YEAR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_YEAR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_YY)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_MONTH)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_DAY)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_HOUR)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_MIN)||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_SEC) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_WEEK_NUMBER) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY) ||
                replaceable_string.contains(SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY_LONG)
        )
            result=true;
        return result;
    }

    static public String replaceKeywordTakenDateValue(String replaceable_string,
                                             String c_date_yyyy, String c_date_mm, String c_date_dd, String c_date_hour, String c_date_min,
                                             String c_date_sec, String c_day_of_year, String c_week_number, String c_week_day, String c_week_day_long) {
        String to_temp = null;
        to_temp = replaceable_string
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DATE, c_date_yyyy+"-"+c_date_mm+"-"+c_date_dd)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_TIME, c_date_hour+"-"+c_date_min+"-"+c_date_sec)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DAY_OF_YEAR, c_day_of_year)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_YEAR, c_date_yyyy)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_YY, c_date_yyyy.substring(2))
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_MONTH, c_date_mm)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_DAY, c_date_dd)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_HOUR, c_date_hour)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_MIN, c_date_min)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_SEC, c_date_sec)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_NUMBER, c_week_number)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY, c_week_day)
                .replaceAll(SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY_LONG, c_week_day_long)
        ;
        return to_temp;
    }

    static public String replaceKeywordExecutionDateValue(String replaceable_string,
                                             String e_date_yyyy, String e_date_mm, String e_date_dd, String e_date_hour, String e_date_min,
                                             String e_date_sec, String e_day_of_year,
                                             String e_week_number, String e_week_day, String e_week_day_long) {
        String to_temp = null;
        to_temp = replaceable_string
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_DATE, e_date_yyyy+"-"+e_date_mm+"-"+e_date_dd)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_TIME, e_date_hour+"-"+e_date_min+"-"+e_date_sec)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_DAY_OF_YEAR, e_day_of_year)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_YEAR, e_date_yyyy)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_YY, e_date_yyyy.substring(2))
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_MONTH, e_date_mm)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_DAY, e_date_dd)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_HOUR, e_date_hour)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_MIN, e_date_min)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_SEC, e_date_sec)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_WEEK_NUMBER, e_week_number)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY, e_week_day)
                .replaceAll(SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY_LONG, e_week_day_long)
        ;
        return to_temp;
    }

    private String buildStorageDir(String uuid, String dir) {
        String base="";
        if (uuid.equals(SafFile3.SAF_FILE_PRIMARY_UUID)) base=mGp.externalStoragePrefix;
        else base="/storage/"+uuid;
        if (dir.equals("")) return base;
        else {
            if (dir.startsWith("/")) return base + dir;
            else return base + "/" + dir;
        }
    }

    static public String buildSmbHostUrl(String addr, String share, String dir) {
        String result = "";
        String smb_host = "smb://";
//        if (!addr.equals("")) smb_host = smb_host + addr;
//        else smb_host = smb_host + hostname;
        smb_host = smb_host + addr;
//        if (!port.equals("")) smb_host = smb_host + ":" + port;
        smb_host = smb_host + "/" + share;
        if (!dir.equals("")) {
            if (dir.startsWith("/")) result = smb_host + dir;
            else result = smb_host + "/" + dir;
        } else {
            result = smb_host;
        }
        return result;
    }

    public static String removeInvalidCharForFileDirName(String in_str) {
        String out = in_str.replaceAll(":", "")
                .replaceAll("\\\\", "")
                .replaceAll("\\*", "")
                .replaceAll("\\?", "")
                .replaceAll("\"", "")
                .replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll("|", "");
        return out;
    }

    static public String getUnprintableCharacter(String in) {
        String removed=in.replaceAll("\\p{C}", "");
        for(int i=0;i<removed.length();i++) {
            String in_sub=in.substring(i,i+1);
            String out_sub=removed.substring(i,i+1);
            if (!in_sub.equals(out_sub)) {
                return "Pos="+(i+1)+", 0x"+StringUtil.getHexString(in_sub.getBytes(),0,in_sub.getBytes().length).toUpperCase();
            }
        }
        return "";
    }

    static public String removeUnprintableCharacter(String in) {
        String removed=in.replaceAll("\\p{C}", "");
        return removed;
    }

    public static String hasInvalidCharForFileDirName(String in_str) {
        if (in_str.contains(":")) return ":";
        if (in_str.contains("\\")) return "\\";
        if (in_str.contains("*")) return "*";
        if (in_str.contains("?")) return "?";
        if (in_str.contains("\"")) return "\"";
        if (in_str.contains("<")) return "<";
        if (in_str.contains(">")) return ">";
        if (in_str.contains("|")) return "|";
        if (in_str.contains("\n")) return "CR";
        if (in_str.contains("\t")) return "TAB";
//        String printable=in_str.replaceAll("\\p{C}", "");
//        if (in_str.length()!=printable.length()) return "UNPRINTABLE("+getUnprintableCharacter(in_str)+")";
        return "";
    }

    public static boolean isValidFileDirectoryName(SyncThreadWorkArea stwa, SyncTaskItem sti, String in_str) {
        if (!hasInvalidCharForFileDirName(in_str).equals("")) {
            if (sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) {
                showMsg(stwa, false, stwa.currentSTI.getSyncTaskName(), "W", "", "",
                        String.format(stwa.appContext.getString(R.string.msgs_mirror_invalid_file_directory_name_character_skipped),
                                hasInvalidCharForFileDirName(in_str), in_str), stwa.appContext.getString(R.string.msgs_mirror_task_file_ignored));
            } else {
                showMsg(stwa, false, stwa.currentSTI.getSyncTaskName(), "E", "", "",
                        String.format(stwa.appContext.getString(R.string.msgs_mirror_invalid_file_directory_name_character_error),
                                hasInvalidCharForFileDirName(in_str), in_str), stwa.appContext.getString(R.string.msgs_mirror_task_file_failed));
            }
            return false;
        }
        return true;
    }

//    static public boolean canAccessLocalDirectory(String path) {
//        boolean result=true;
//        if (path.endsWith(".android_secure")) result=false;
//        else {
//            if (Build.VERSION.SDK_INT>=30) {
//                String[] fp_array=path.split("/");
//                if (path.startsWith("/storage/emulated/0")) {
//                    String abs_dir=path.replace("/storage/emulated/0", "");
//                    if (!abs_dir.equals("")) {
//                        if (abs_dir.startsWith("/Android/data") || abs_dir.startsWith("/Android/obb")) {
//                            result=false;
//                        }
//                    }
//                } else {
//                    if (fp_array.length>=3) {
//                        String abs_dir=path.replace("/"+fp_array[1]+"/"+fp_array[2], "");
//                        if (!abs_dir.equals("")) {
//                            if (abs_dir.startsWith("/Android/data") || abs_dir.startsWith("/Android/obb")) {
//                                result=false;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return result;
//    }

    final public static boolean createDirectoryToLocalStorage(SyncThreadWorkArea stwa, SyncTaskItem sti, String dir) {
        boolean result = false;
        SafFile3 sf = new SafFile3(stwa.appContext, dir);
        return createDirectoryToLocalStorage(stwa, sti, sf);
    }

    final public static boolean createDirectoryToLocalStorage(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 sf) {
        boolean result = false;
        if (!sf.exists()) {
            if (!sti.isSyncTestMode()) {
                result = sf.mkdirs();
                if (result && stwa.logLevel>=1)
                    stwa.util.addDebugMsg(1, "I", "createDirectoryToLocalStorage directory created, dir=" + sf.getPath());
            } else {
                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "createDirectoryToLocalStorage directory created, dir=" + sf.getPath());
            }
        }
        return result;
    }

    final public static void createDirectoryToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String dir,
                                                  JcifsAuth auth) throws MalformedURLException, JcifsException {
        try {
            JcifsFile sf = new JcifsFile(dir, auth);
            if (!sti.isSyncTestMode()) {
                if (!sf.exists()) {
                    sf.mkdirs();
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "createDirectoryToSmb directory created, dir=" + dir);
                }
            } else {
                if (!sf.exists()) {
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "createDirectoryToSmb directory created, dir=" + dir);
                }
            }
        } catch(JcifsException e) {
            String suggest_msg= TaskListUtils.getJcifsErrorSugestionMessage(stwa.appContext, MiscUtil.getStackTraceString(e));
            if (suggest_msg.equals("")) showMsg(stwa, false, sti.getSyncTaskName(), "E", dir, "","SMB create directory error, "+e.getMessage());
            else showMsg(stwa, false, sti.getSyncTaskName(), "E", dir, "","SMB create directory error, "+e.getMessage()+"\n"+suggest_msg);
            throw(e);
        }
    }

    private int checkNetworkOption(SyncTaskItem sti) {
        int sync_status= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        String e_msg="";

        if (!sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                !sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) return 0;

        String if_addr=CommonUtilities.getIfIpAddress(mStwa.util);
        if (!CommonUtilities.isPrivateAddress(if_addr) && !sti.isSyncOptionSyncAllowAllIpAddress()) {
            e_msg=mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_ip_address_is_global, if_addr);
            showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
            mGp.syncThreadCtrl.setThreadMessage(e_msg);
            sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } else {
            if (sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_OFF)) {
                //NOP
            } else if (sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS)) {
                if (!CommonUtilities.isPrivateAddress(if_addr)) {
                    e_msg=mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_wifi_connect_not_local_addr);
                    showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            } else if (sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_IP_ADDRESS_LIST)) {
                if (!if_addr.equals("")) {
                    ArrayList<FilterListAdapter.FilterListItem> wl = sti.getSyncOptionWifiIPAddressGrantList();
                    boolean found=isWifiFilterMatched(sti, wl, if_addr);
                    if (!found) {
                        if (sti.getSyncTaskErrorOption()==SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_SKIP_NETWORK) {
                            sync_status= SyncTaskItem.SYNC_RESULT_STATUS_SKIP;
                            e_msg = mStwa.appContext.getString(R.string.msgs_mirror_sync_skipped_wifi_address_conn_other);
                            showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                            mGp.syncThreadCtrl.setThreadMessage(e_msg);
                        } else {
                            sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                            e_msg = mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_wifi_address_conn_other);
                            showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                            mGp.syncThreadCtrl.setThreadMessage(e_msg);
                        }
                    }
                } else {
                    e_msg = mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_wifi_ap_not_connected);
                    showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            } else if (sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP)) {
                if (isWifiOn()) {
                    boolean isConnected = false;
                    boolean isWiFi = false;
                    ConnectivityManager cm =(ConnectivityManager)mStwa.appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork!=null) {
                        String network=activeNetwork.getExtraInfo();
                        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                        isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
                    }
                    if (!isWiFi || !isConnected) {//getWifiConnectedAP().equals("")) {
                        e_msg = mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_wifi_ap_not_connected);
                        showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                        mGp.syncThreadCtrl.setThreadMessage(e_msg);
                        sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    }
                } else {
                    e_msg = mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_wifi_is_off);
                    showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            }
            if (sync_status== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS && !sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_OFF)) {
                if (if_addr.equals("")) {//IP Addressが必要だがIP Addressが取得できない
                    e_msg=mStwa.appContext.getString(R.string.msgs_mirror_sync_can_not_start_ip_address_not_obtained);
                    showMsg(mStwa, true, mStwa.currentSTI.getSyncTaskName(), "E", "", "", e_msg);
                    mGp.syncThreadCtrl.setThreadMessage(e_msg);
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            }
        }

        mStwa.util.addDebugMsg(1, "I", "checkNetworkOption exited, " + "option=" + sti.getSyncOptionWifiStatusOption() + ", result=" + sync_status);
        return sync_status;
    }

    private boolean isWifiFilterMatched(SyncTaskItem sti, ArrayList<FilterListAdapter.FilterListItem>fl, String value) {
        int flags = Pattern.CASE_INSENSITIVE|Pattern.MULTILINE;
        boolean matched = false;
        if (fl.size()>0) {
            String filter_val="";
            String filter_sep="";
            for (FilterListAdapter.FilterListItem al : fl) {
                if (al.isInclude()) {
                    String[] filter_array=al.getFilter().split(";");
                    for(String filter_item:filter_array) {
                        filter_val+=filter_sep+"^"+MiscUtil.convertRegExp(filter_item)+"$";
                        filter_sep="|";
                    }
                }
            }
            if (!filter_val.equals("")) {
                Pattern pattern=Pattern.compile(filter_val, flags);
                Matcher mt = pattern.matcher(value);
                if (mt.find()) {
                    matched = true;
                    mStwa.util.addDebugMsg(1, "I", "isWifiFilterMatched filter matched=" + value+", pattern="+pattern);
                } else {
                    mStwa.util.addDebugMsg(1, "I", "isWifiFilterMatched filter unmatched=" + value+", pattern="+pattern);
                }
            } else {
                matched=true;
            }
        }
        return matched;
    }

//    private String getWifiConnectedAP() {
//        String result = "";
//        WifiManager wm = (WifiManager) mStwa.appContext.getSystemService(Context.WIFI_SERVICE);
//        if (isWifiOn()) {
//            result = CommonUtilities.getWifiSsidName(wm);
//            mStwa.util.addDebugMsg(1, "I", "getWifiConnectedAP SSID=" + result);
//        } else {
//            mStwa.util.addDebugMsg(1, "I", "getWifiConnectedAP WiFi is not enabled.");
//        }
//        return result;
//    }

    public boolean isWifiOn() {
        return isWifiOn(mStwa.appContext);
    }

    static public boolean isWifiOn(Context c) {
        boolean result = false;
        WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        result = wm.isWifiEnabled();
        return result;
    }

    private boolean setWifiOn() {
        boolean result = false;
        if (Build.VERSION.SDK_INT<=28) {
            WifiManager wm = (WifiManager) mStwa.appContext.getSystemService(Context.WIFI_SERVICE);
            result = wm.setWifiEnabled(true);
        } else {
            mStwa.util.addDebugMsg(1, "I", "setWifiOn ignored by SDK>28");
        }
        return result;
    }

    private boolean setWifiOff() {
        boolean result = false;
        if (Build.VERSION.SDK_INT<=28) {
            WifiManager wm = (WifiManager) mStwa.appContext.getSystemService(Context.WIFI_SERVICE);
            result = wm.setWifiEnabled(false);
        } else {
            mStwa.util.addDebugMsg(1, "I", "setWifiOff ignored by SDK>28");
        }

        return result;
    }

    static public void showProgressMsg(final SyncThreadWorkArea stwa, final String task_name, final String msg) {
        NotificationUtils.showOngoingMsg(stwa.gp, stwa.util, 0, task_name, msg);
        stwa.gp.progressSpinSyncprofText = task_name;
        stwa.gp.progressSpinMsgText = msg;
        if (stwa.gp.dialogWindowShowed && stwa.gp.progressSpinSynctask != null) {
            stwa.gp.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (stwa.gp.progressSpinSynctask != null && stwa.gp.activityIsForeground) {
                        stwa.gp.progressSpinSynctask.setText(stwa.gp.progressSpinSyncprofText);
                        stwa.gp.progressSpinMsg.setText(stwa.gp.progressSpinMsgText);
                    }
                }
            });
        }
    }

    static public void showMsg(final SyncThreadWorkArea stwa, boolean log_only,
                               final String task_name, final String cat,
                               final String full_path, final String file_name, final String msg) {
        showMsg(stwa, log_only, task_name, cat, full_path, file_name, msg, "");
    }

    static public void showMsg(final SyncThreadWorkArea stwa, boolean log_only,
                               final String task_name, final String cat,
                               final String full_path, final String file_name, final String msg, String result_type) {
        String notif_msg = "";
        String[] notif_msg_list = {file_name, msg, result_type};
        for (String msg_part : notif_msg_list) {
            if (!msg_part.equals("")) {
                if (notif_msg.equals("")) notif_msg = msg_part;
                else notif_msg = notif_msg.concat(" ").concat(msg_part);
            }
        }

        //ongoing progress message in top of screen during sync, not saved to Messages tab
        stwa.gp.progressSpinSyncprofText = task_name;//title of in app progress notification
        stwa.gp.progressSpinMsgText = notif_msg;//text of in app progress notification

        if (!log_only) {
            NotificationUtils.showOngoingMsg(stwa.gp, stwa.util, System.currentTimeMillis(), task_name, file_name, msg, result_type);
            if (stwa.gp.dialogWindowShowed && stwa.gp.progressSpinSynctask != null) {
                stwa.gp.uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (stwa.gp.progressSpinSynctask != null && stwa.gp.activityIsForeground) {
                            stwa.gp.progressSpinSynctask.setText(stwa.gp.progressSpinSyncprofText);
                            stwa.gp.progressSpinMsg.setText(stwa.gp.progressSpinMsgText);
                        }
                    }
                });
            }
        }
        //display message in GUI Messages TAB
        stwa.util.addLogMsg(false, true, true, false, cat, task_name, result_type, full_path, msg);

        //buildPrintMsg(): print the sync log text file, shown if we click the Sync event in History TAB
        String lm = "";
        String[] print_msg_list = {task_name+":", full_path, msg, result_type};
        for (String msg_part : print_msg_list) {
            if (!msg_part.equals("")) {
                if (lm.equals("")) lm = msg_part;
                else lm = lm.concat(" ").concat(msg_part);
            }
        }

        if (stwa.gp.settingWriteSyncResultLog && stwa.syncHistoryWriter != null) {
            String print_msg = stwa.util.buildPrintMsg(cat, lm);
            stwa.syncHistoryWriter.println(print_msg);
        }
    }

    static public void showArchiveMsg(final SyncThreadWorkArea stwa, boolean log_only,
                                      final String task_name, final String cat,
                                      final String full_path, final String archive_path, final String from_file_name,
                                      final String to_file_name, final String msg) {
        showArchiveMsg(stwa, log_only, task_name, cat, full_path, archive_path, from_file_name, to_file_name, msg, "");
    }

    static public void showArchiveMsg(final SyncThreadWorkArea stwa, boolean log_only,
                                      final String task_name, final String cat,
                                      final String full_path, final String archive_path, final String from_file_name,
                                      final String to_file_name, final String msg, String result_type) {
        String notif_msg = "";
        String[] notif_msg_list = {from_file_name, "-->", to_file_name, msg, result_type};
        for (String msg_part : notif_msg_list) {
            if (!msg_part.equals("")) {
                if (notif_msg.equals("")) notif_msg = msg_part;
                else notif_msg = notif_msg.concat(" ").concat(msg_part);
            }
        }

        //ongoing progress message in top of screen during sync, not saved to Messages tab
        stwa.gp.progressSpinSyncprofText = task_name;//title of in app progress notification
        stwa.gp.progressSpinMsgText = notif_msg;//text of in app progress notification

        if (!log_only) {//set onscreen notification progress
            NotificationUtils.showOngoingMsg(stwa.gp, stwa.util, System.currentTimeMillis(), task_name, from_file_name, msg, result_type);//system notification if app in background
            if (stwa.gp.dialogWindowShowed && stwa.gp.progressSpinSynctask != null) {
                stwa.gp.uiHandler.post(new Runnable() {//in app progress notification
                    @Override
                    public void run() {
                        if (stwa.gp.progressSpinSynctask != null && stwa.gp.activityIsForeground) {
                            stwa.gp.progressSpinSynctask.setText(stwa.gp.progressSpinSyncprofText);
                            stwa.gp.progressSpinMsg.setText(stwa.gp.progressSpinMsgText);
                        }
                    }
                });
            }
        }

        //display message in GUI Messages TAB
        String printed_path = full_path + " --> " + archive_path;
        stwa.util.addLogMsg(false, true, true, false, cat, task_name, result_type, printed_path, msg);

        //buildPrintMsg(): print the sync log text file, shown if we click the Sync event in History TAB
        String lm = "";
        String[] print_msg_list = {task_name, printed_path, msg, result_type};
        for (String msg_part : print_msg_list) {
            if (!msg_part.equals("")) {
                if (lm.equals("")) lm = msg_part;
                else lm = lm.concat(" ").concat(msg_part);
            }
        }

        if (stwa.gp.settingWriteSyncResultLog && stwa.syncHistoryWriter != null) {
            String print_msg = stwa.util.buildPrintMsg(cat, lm);
            stwa.syncHistoryWriter.println(print_msg);
        }
    }

    public static void printStackTraceElement(SyncThreadWorkArea stwa, StackTraceElement[] ste) {
        String print_msg = "", ste_log="", sep="";
        for (int i = 0; i < ste.length; i++) {
            ste_log+=sep+ste[i].toString();
            sep="\n";
            if (stwa.syncHistoryWriter != null) {
                print_msg = stwa.util.buildPrintMsg("E", ste[i].toString());
                stwa.syncHistoryWriter.println(print_msg);
            }
        }
        stwa.util.addLogMsg("E", stwa.currentSTI.getSyncTaskName(), ste_log);
    }

    static final public boolean sendConfirmRequest(SyncThreadWorkArea stwa, SyncTaskItem sti, String type, String from_path, String to_path) {
        boolean result = true;
        int rc = 0;
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "sendConfirmRequest entered type=" , type ,
                ", Override="+sti.isSyncOverrideCopyMoveFile(), ", Confirm=" + sti.isSyncConfirmOverrideOrDelete(),
                ", from path=", from_path+", to_path="+to_path);
        if (sti.isSyncConfirmOverrideOrDelete()) {
            boolean ignore_confirm = true;
            if (type.equals(CONFIRM_REQUEST_DELETE_DIR) || type.equals(CONFIRM_REQUEST_DELETE_FILE) ||
                    type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR) || type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE) ) {
                if (stwa.confirmDeleteResult == CONFIRM_RESP_YES_ALL) result = true;
                else if (stwa.confirmDeleteResult == CONFIRM_RESP_NO_ALL) result = false;
                else ignore_confirm = false;
            } else if (type.equals(CONFIRM_REQUEST_COPY)) {
                if (stwa.confirmCopyResult == CONFIRM_RESP_YES_ALL) result = true;
                else if (stwa.confirmCopyResult == CONFIRM_RESP_NO_ALL) result = false;
                else ignore_confirm = false;
            } else if (type.equals(CONFIRM_REQUEST_MOVE)) {
                if (stwa.confirmMoveResult == CONFIRM_RESP_YES_ALL) result = true;
                else if (stwa.confirmMoveResult == CONFIRM_RESP_NO_ALL) result = false;
                else ignore_confirm = false;
            }
            if (!ignore_confirm) {
                try {
                    String msg = "";
                    if (type.equals(CONFIRM_REQUEST_DELETE_DIR) || type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR)) {
                        msg = stwa.appContext.getString(R.string.msgs_mirror_confirm_please_check_confirm_msg_delete_dir);
                    } else if (type.equals(CONFIRM_REQUEST_DELETE_FILE) || type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE)) {
                        msg = stwa.appContext.getString(R.string.msgs_mirror_confirm_please_check_confirm_msg_delete_file);
                    } else if (type.equals(CONFIRM_REQUEST_COPY)) {
                        msg = stwa.appContext.getString(R.string.msgs_mirror_confirm_please_check_confirm_msg_copy);
                    } else if (type.equals(CONFIRM_REQUEST_MOVE)) {
                        msg = stwa.appContext.getString(R.string.msgs_mirror_confirm_please_check_confirm_msg_move);
                    }
                    NotificationUtils.showOngoingMsg(stwa.gp, stwa.util, 0, msg);
                    stwa.gp.confirmDialogShowed = true;
                    stwa.gp.confirmDialogFilePathPairA = from_path;
                    stwa.gp.confirmDialogFilePathPairB = to_path;
                    stwa.gp.confirmDialogMethod = type;
                    stwa.gp.syncThreadConfirm.initThreadCtrl();
                    stwa.gp.releaseWakeLock(stwa.util);
                    if (stwa.gp.callbackShowConfirmDialog != null) {
                        stwa.gp.callbackShowConfirmDialog.onCallBack(stwa.appContext, true, new Object[]{type, "", from_path, 0L,0L,to_path,0L,0L});
                    }
                    synchronized (stwa.gp.syncThreadConfirm) {
                        stwa.gp.syncThreadConfirmWait = true;
                        stwa.gp.syncThreadConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
                        stwa.gp.syncThreadConfirmWait = false;
                    }
                    stwa.gp.acquireWakeLock(stwa.appContext, stwa.util);
                    if (type.equals(CONFIRM_REQUEST_DELETE_DIR) || type.equals(CONFIRM_REQUEST_DELETE_FILE) ||
                            type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR) || type.equals(CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE)) {
                        rc = stwa.confirmDeleteResult = stwa.gp.syncThreadConfirm.getExtraDataInt();
                        if (stwa.confirmDeleteResult > 0) result = true;
                        else result = false;
                        if (stwa.confirmDeleteResult == CONFIRM_RESP_CANCEL) cancelTask(stwa.gp.syncThreadCtrl);//stwa.gp.syncThreadCtrl.setDisabled();
                    } else if (type.equals(CONFIRM_REQUEST_COPY)) {
                        rc = stwa.confirmCopyResult = stwa.gp.syncThreadConfirm.getExtraDataInt();
                        if (stwa.confirmCopyResult > 0) result = true;
                        else result = false;
                        if (stwa.confirmCopyResult == CONFIRM_RESP_CANCEL) cancelTask(stwa.gp.syncThreadCtrl);//stwa.gp.syncThreadCtrl.setDisabled();
                    } else if (type.equals(CONFIRM_REQUEST_MOVE)) {
                        rc = stwa.confirmMoveResult = stwa.gp.syncThreadConfirm.getExtraDataInt();
                        if (stwa.confirmMoveResult > 0) result = true;
                        else result = false;
                        if (stwa.confirmMoveResult == CONFIRM_RESP_CANCEL) cancelTask(stwa.gp.syncThreadCtrl);//stwa.gp.syncThreadCtrl.setDisabled();
                    }
//                } catch (RemoteException e) {
//                    stwa.util.addLogMsg("E", sti.getSyncTaskName(), "RemoteException occured");
//                    printStackTraceElement(stwa, e.getStackTrace());
                } catch (InterruptedException e) {
                    stwa.util.addLogMsg("E", sti.getSyncTaskName(), "InterruptedException occured");
                    printStackTraceElement(stwa, e.getStackTrace());
                }
            }
        }
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "sendConfirmRequest result=" + result, ", rc=" + rc);

        return result;
    }

    static final public boolean sendArchiveConfirmRequest(SyncThreadWorkArea stwa, SyncTaskItem sti, String type, String url) {
        boolean result = true;
        int rc = 0;
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "sendArchiveConfirmRequest entered type=" , type ,
                ", fp=", url);
        boolean ignore_confirm = true;
        if (type.equals(CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE)) {
            if (stwa.confirmArchiveResult == CONFIRM_RESP_YES_ALL) result = true;
            else if (stwa.confirmArchiveResult == CONFIRM_RESP_NO_ALL) result = false;
            else ignore_confirm = false;
        }
        if (!ignore_confirm) {
            try {
                String msg = "";
                if (type.equals(CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE)) {
                    msg = stwa.appContext.getString(R.string.msgs_mirror_confirm_please_check_confirm_msg_archive);
                }
                NotificationUtils.showOngoingMsg(stwa.gp, stwa.util, 0, msg);
                stwa.gp.confirmDialogShowed = true;
                stwa.gp.confirmDialogFilePathPairA = url;
                stwa.gp.confirmDialogMethod = type;
                stwa.gp.syncThreadConfirm.initThreadCtrl();
                stwa.gp.releaseWakeLock(stwa.util);
                if (stwa.gp.callbackShowConfirmDialog != null) {
                    stwa.gp.callbackShowConfirmDialog.onCallBack(stwa.appContext, true, new Object[]{type, "", url, 0L,0L,null,0L,0L});
                }
                synchronized (stwa.gp.syncThreadConfirm) {
                    stwa.gp.syncThreadConfirmWait = true;
                    stwa.gp.syncThreadConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
                    stwa.gp.syncThreadConfirmWait = false;
                }
                stwa.gp.acquireWakeLock(stwa.appContext, stwa.util);
                if (type.equals(CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE)) {
                    rc = stwa.confirmArchiveResult = stwa.gp.syncThreadConfirm.getExtraDataInt();
                    if (stwa.confirmArchiveResult > 0) result = true;
                    else result = false;
                    if (stwa.confirmArchiveResult == CONFIRM_RESP_CANCEL) cancelTask(stwa.gp.syncThreadCtrl);//stwa.gp.syncThreadCtrl.setDisabled();
                }
//            } catch (RemoteException e) {
//                stwa.util.addLogMsg("E", sti.getSyncTaskName(), "RemoteException occured");
//                printStackTraceElement(stwa, e.getStackTrace());
            } catch (InterruptedException e) {
                stwa.util.addLogMsg("E", sti.getSyncTaskName(), "InterruptedException occured");
                printStackTraceElement(stwa, e.getStackTrace());
            }
        }
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "sendArchiveConfirmRequest result=" + result, ", rc=" + rc);

        return result;
    }

    static final public boolean isLocalFileLastModifiedFileItemExists(SyncThreadWorkArea stwa,
                                                                    SyncTaskItem sti,
                                                                    ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                                    ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                                    String fp) {
        FileLastModifiedTime.FileLastModifiedTimeEntry item = FileLastModifiedTime.isFileItemExists(
                curr_last_modified_list, new_last_modified_list, fp);
        boolean result=false;
        if (item!=null) result=true;
        if (stwa.logLevel>=3) stwa.util.addDebugMsg(3, "I", "isLocalFileLastModifiedWasDifferent result=" + result + ", item=" + fp);
        return result;
    }

    static final public FileLastModifiedTime.FileLastModifiedTimeEntry getLocalFileLastModifiedFileItemExists(SyncThreadWorkArea stwa,
                                                                                                              SyncTaskItem sti,
                                                                                                              ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                                                                              ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                                                                              String fp) {
        FileLastModifiedTime.FileLastModifiedTimeEntry item = FileLastModifiedTime.isFileItemExists(
                curr_last_modified_list, new_last_modified_list, fp);
        if (stwa.logLevel>=3) stwa.util.addDebugMsg(3, "I", "isLocalFileLastModifiedWasDifferent result=" + item + ", item=" + fp);
        return item;
    }

    static final public boolean isLocalFileLastModifiedWasDifferent(SyncThreadWorkArea stwa,
                                                                    SyncTaskItem sti,
                                                                    ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                                    ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                                    String fp, long l_lm, long r_lm) {
        boolean result = FileLastModifiedTime.isCurrentListWasDifferent(
                curr_last_modified_list, new_last_modified_list,
                fp, l_lm, r_lm, stwa.syncDifferentFileAllowableTime,
                sti.isSyncOptionIgnoreDstDifference(), stwa.offsetOfDaylightSavingTime);

        if (stwa.logLevel>=3) stwa.util.addDebugMsg(3, "I", "isLocalFileLastModifiedWasDifferent result=" + result + ", item=" + fp);
        return result;
    }

    static final public void deleteLocalFileLastModifiedEntry(SyncThreadWorkArea stwa,
                                                              ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                              ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                              String fp) {
        if (FileLastModifiedTime.deleteLastModifiedItem(curr_last_modified_list, new_last_modified_list, fp)) stwa.localFileLastModListModified = true;
        if (stwa.logLevel>=3) stwa.util.addDebugMsg(3, "I", "deleteLocalFileLastModifiedEntry entry=" + fp);
    }

    static final public boolean updateLocalFileLastModifiedList(SyncThreadWorkArea stwa,
                                                                ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                                ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                                String to_dir, long l_lm, long r_lm) {
        if (stwa.lastModifiedIsFunctional) return false;
        stwa.localFileLastModListModified = true;
        return FileLastModifiedTime.updateLastModifiedList(curr_last_modified_list, new_last_modified_list, to_dir, l_lm, r_lm);
    }

    static final public void addLastModifiedItem(SyncThreadWorkArea stwa,
                                                 ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> curr_last_modified_list,
                                                 ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> new_last_modified_list,
                                                 String to_dir, long l_lm, long r_lm) {
        FileLastModifiedTime.addLastModifiedItem(curr_last_modified_list, new_last_modified_list, to_dir, l_lm, r_lm);
        if (stwa.logLevel>=3) stwa.util.addDebugMsg(3, "I", "addLastModifiedItem entry=" + to_dir);
    }

    final private boolean isSetLastModifiedFunctional(String app_specific_file_dir) {
        boolean result =
                FileLastModifiedTime.isSetLastModifiedFunctional(app_specific_file_dir);
        if (mStwa.logLevel>=1) mStwa.util.addDebugMsg(1, "I", "isSetLastModifiedFunctional result=" + result + ", Directory=" + app_specific_file_dir);
        return result;
    }

    static final public boolean isFileChanged(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                              String fp, SafFile3 lf,//Destination
                                              JcifsFile hf, boolean ac) //Source
            throws JcifsException {
        long hf_time = 0, hf_length = 0;
        boolean hf_exists = hf.exists();

        if (hf_exists) {
            hf_time = hf.getLastModified();
            hf_length = hf.length();
        }
        return isFileChangedDetailCompare(stwa, sti, fp, lf, hf_exists, hf_time, hf_length, ac);
    }

    static final public boolean isFileChanged(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                              String fp, SafFile3 mf, //Destination
                                              SafFile3 tf, boolean ac)//Source
            throws JcifsException {
        long tf_time = 0, tf_length = 0;
        boolean tf_exists = tf.exists();

        if (tf_exists) {
            if (tf.isSafFile()) {
                long[] array=tf.getLastModifiedAndLength();
                tf_time = array[0];
                tf_length = array[1];
            } else {
                tf_time = tf.lastModified();
                tf_length = tf.length();
            }
        }
        return isFileChangedDetailCompare(stwa, sti, fp, mf, tf_exists, tf_time, tf_length, ac);
    }

    static final public boolean isFileChanged(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                              String fp, JcifsFile mf,//Destination
                                              JcifsFile tf, boolean ac)//Source
            throws JcifsException {

        long mf_time = 0, mf_length = 0;
        boolean mf_exists = mf.exists();

        if (mf_exists) {
            mf_time = mf.getLastModified();
            mf_length = mf.length();
        }

        return isFileChangedDetailCompare(stwa, sti, fp,
                mf_exists, mf_time, mf_length, mf.getPath(),
                tf.exists(), tf.getLastModified(), tf.length(), ac);

    }

    static final public boolean checkSourceFileNewerThanDestinationFile(SyncThreadWorkArea stwa, SyncTaskItem sti, String fp,
                                                                        long source_time, long destination_time) {
        boolean result=true;
        if (sti.isSyncOptionDifferentFileByTime() && sti.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile()) {
            if (stwa.lastModifiedIsFunctional) {//Use lastModified
                if (source_time>destination_time) {
                    result=true;
                } else {
                    result=false;
                    stwa.totalIgnoreCount++;
                    String fn=fp.substring(fp.lastIndexOf("/"));
                    showMsg(stwa, false, sti.getSyncTaskName(), "W", fp, fn, stwa.appContext.getString(R.string.msgs_task_sync_task_sync_option_ignore_never_overwrite_destination_file_if_it_is_newer_than_the_source_file_option_enabled),
                            stwa.appContext.getString(R.string.msgs_mirror_task_file_ignored));
                }
            } else {
                FileLastModifiedTime.FileLastModifiedTimeEntry flme=getLocalFileLastModifiedFileItemExists(stwa, sti, stwa.currLastModifiedList, stwa.newLastModifiedList, fp);
                if (flme==null) {
                    result=true;
                } else {
                    if (destination_time==flme.getRemoteFileLastModified()) {
                        if (source_time>destination_time) {
                            result=true;
                        } else {
                            result=false;
                            stwa.totalIgnoreCount++;
                            String fn=fp.substring(fp.lastIndexOf("/"));
                            showMsg(stwa, false, sti.getSyncTaskName(), "W", fp, fn, stwa.appContext.getString(R.string.msgs_task_sync_task_sync_option_ignore_never_overwrite_destination_file_if_it_is_newer_than_the_source_file_option_enabled),
                                    stwa.appContext.getString(R.string.msgs_mirror_task_file_ignored));
                        }
                    } else {
                        if (source_time>destination_time) {
                            result=true;
                        } else {
                            result=false;
                            stwa.totalIgnoreCount++;
                            String fn=fp.substring(fp.lastIndexOf("/"));
                            showMsg(stwa, false, sti.getSyncTaskName(), "W", fp, fn, stwa.appContext.getString(R.string.msgs_task_sync_task_sync_option_ignore_never_overwrite_destination_file_if_it_is_newer_than_the_source_file_option_enabled),
                                    stwa.appContext.getString(R.string.msgs_mirror_task_file_ignored));
                        }
                    }
                }
            }
        }
        return result;
    }

    static final private boolean isFileChangedDetailCompare(SyncThreadWorkArea stwa, SyncTaskItem sti, String fp,
                                                            SafFile3 lf,//Destination
                                                            boolean tf_exists, long tf_time, long tf_length,//Source
                                                            boolean ac) throws JcifsException {
        long lf_time = 0, lf_length = 0;
        boolean lf_exists=false;

        lf_exists=lf.exists();
        if (lf_exists) {
            if (lf.isSafFile()) {
                long[] array=lf.getLastModifiedAndLength();
                lf_time = array[0];
                lf_length = array[1];
            } else {
                lf_time = lf.lastModified();
                lf_length = lf.length();
            }
        }

        return isFileChangedDetailCompare(stwa, sti, fp,
                lf_exists, lf_time, lf_length, lf.getPath(),//Destination
                tf_exists, tf_time, tf_length, ac);//Source
    }

    static final private boolean isFileChangedDetailCompare(SyncThreadWorkArea stwa, SyncTaskItem sti, String fp,
                                                            boolean lf_exists, long lf_time, long lf_length, String lf_path,//Destination
                                                            boolean tf_exists, long tf_time, long tf_length, boolean ac) {//Source
        boolean diff = false;
        boolean exists_diff = false;

        long time_diff = Math.abs((tf_time - lf_time));
        long length_diff = Math.abs((tf_length - lf_length));

        if (tf_exists != lf_exists) exists_diff = true;
        if (exists_diff || (sti.isSyncOptionDifferentFileBySize() && length_diff > 0) || ac) {
            if (sti.isSyncDifferentFileSizeGreaterThanDestinationFile()) {
                if (tf_length>lf_length) {
                    diff = true;
                }
            } else {
                diff = true;
            }
            if (diff && !stwa.lastModifiedIsFunctional) {//Use lastModified
                if (lf_exists) {
                    updateLocalFileLastModifiedList(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList,
                            lf_path, lf_time, tf_time);
                } else {
                    boolean updated =
                            updateLocalFileLastModifiedList(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList,
                                    lf_path, lf_time, tf_time);
                    if (!updated)
                        addLastModifiedItem(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList,
                                lf_path, lf_time, tf_time);
                }
            }
        } else {//Check lastModified()
            if (sti.isSyncOptionDifferentFileByTime()) {
                if (stwa.lastModifiedIsFunctional) {//Use lastModified
                    if (time_diff > stwa.syncDifferentFileAllowableTime) { //LastModified was changed
                        if (sti.isSyncOptionIgnoreDstDifference()) {
                            if (Math.abs(time_diff-stwa.offsetOfDaylightSavingTime)<=stwa.syncDifferentFileAllowableTime) {
                                diff=false;
                            } else {
                                diff=true;
                            }
                        } else {
                            diff = true;
                        }
                    } else {
                        diff = false;
                    }
                } else {//Use Filelist
                    boolean found=isLocalFileLastModifiedFileItemExists(stwa, sti, stwa.currLastModifiedList, stwa.newLastModifiedList, lf_path);
                    if (!found) {
                        if (time_diff > stwa.syncDifferentFileAllowableTime) {
                            if (sti.isSyncOptionIgnoreDstDifference()) {
                                if (Math.abs(time_diff-stwa.offsetOfDaylightSavingTime)<=stwa.syncDifferentFileAllowableTime) {
                                    diff=false;
                                } else {
                                    diff=true;
                                }
                            } else {
                                diff = true;
                            }
                        } else {
                            diff = false;
                        }
                        addLastModifiedItem(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, lf_path, lf_time, tf_time );
                    } else {
                        diff = isLocalFileLastModifiedWasDifferent(stwa, sti,
                                stwa.currLastModifiedList,
                                stwa.newLastModifiedList,
                                lf_path, lf_time, tf_time);
                    }
//                    stwa.util.addDebugMsg(3, "I", "isFileChangedDetailCompare FilItem Exists="+found);
                }
            }
        }
//        stwa.util.addDebugMsg(3, "I", "isFileChangedDetailCompare");
//        if (lf_exists) stwa.util.addDebugMsg(3, "I", "Source file length=" + lf_length +
//                ", last modified(ms)=" + lf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Source file was not exists");
//        if (tf_exists) stwa.util.addDebugMsg(3, "I", "Destination file length=" + tf_length +
//                ", last modified(ms)=" + tf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((tf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Destination file was not exists");
//        stwa.util.addDebugMsg(3, "I", "allcopy=" + ac + ",exists_diff=" + exists_diff +
//                ",time_diff=" + time_diff + ",length_diff=" + length_diff + ", diff=" + diff);
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "isFileChanged fp="+fp+ ", exists_diff=" + exists_diff +
                ", time_diff=" + time_diff + ", length_diff=" + length_diff + ", diff=" + diff+", destination_time="+lf_time+", source_time="+tf_time);
        return diff;
    }

    static final public boolean isFileChangedForLocalToRemote(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                              String fp, SafFile3 lf, JcifsFile hf, boolean ac) throws JcifsException {
        boolean diff = false;
        long hf_time = 0, hf_length = 0;
        boolean hf_exists = hf.exists();//Destination

        if (hf_exists) {
            hf_time = hf.getLastModified();
            hf_length = hf.length();
        }
        long lf_time = 0, lf_length = 0;
        boolean lf_exists = lf.exists();//Source
        boolean exists_diff = false;

        if (lf_exists) {
            lf_time = lf.lastModified();
            lf_length = lf.length();
        }
        long time_diff = Math.abs((hf_time - lf_time));
        long length_diff = Math.abs((hf_length - lf_length));

        if (hf_exists != lf_exists) exists_diff = true;
        if (exists_diff || (sti.isSyncOptionDifferentFileBySize() && length_diff > 0) || ac) {
            diff = true;
        } else {//Check lastModified()
            if (sti.isSyncOptionDifferentFileByTime()) {
                if ((time_diff > stwa.syncDifferentFileAllowableTime)) { //LastModified was changed
                    if (sti.isSyncOptionIgnoreDstDifference()) {
                        if (Math.abs(time_diff-stwa.offsetOfDaylightSavingTime)<=stwa.syncDifferentFileAllowableTime) {
                            diff=false;
                        } else {
                            diff=true;
                        }
                    } else {
                        diff = true;
                    }
                } else {
                    diff = false;
                }

            }
        }
//        stwa.util.addDebugMsg(3, "I", "isFileChangedForLocalToRemote");
//        if (hf_exists) stwa.util.addDebugMsg(3, "I", "Remote file length=" + hf_length +
//                ", last modified(ms)=" + hf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((hf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Remote file was not exists");
//        if (lf_exists) stwa.util.addDebugMsg(3, "I", "Local  file length=" + lf_length +
//                ", last modified(ms)=" + lf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Local  file was not exists");
//        stwa.util.addDebugMsg(3, "I", "allcopy=" + ac + ",exists_diff=" + exists_diff +
//                ",time_diff=" + time_diff +//", time_zone_diff="+time_diff_tz1+
//                ",length_diff=" + length_diff + ", diff=" + diff);
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isFileChangedForLocalToRemote fp="+fp+ ", exists_diff=" + exists_diff +
                ", time_diff=" + time_diff + ", length_diff=" + length_diff + ", diff=" + diff+", destination_time="+hf_time+", source_time="+lf_time);
        return diff;
    }

    static public boolean isJcifsRetryRequiredError(int sc) {
        if (sc==0
                || sc==0xc0000022
                || sc==0xc0000043 || sc==0xc0000033 || sc==0xc000006d || sc==0xc000006e || sc==0xc000006f
                || sc==0xc0000070  || sc==0xc0000071  || sc==0xc0000072 //logon failur

        ) return false;
        else return true;
    }

    static public boolean isHiddenDirectory(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 lf) {
        boolean result = false;
        if (sti.isSyncOptionSyncHiddenDirectory()) result = false;
        else {
            if (lf.getName().startsWith(".")) result = true;
        }
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isHiddenDirectory(Local) result=" + result + ", Name=" + lf.getName());
        return result;
    }

    static public boolean isHiddenDirectory(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile hf) throws JcifsException {
        boolean result = false;
        if (sti.isSyncOptionSyncHiddenDirectory()) result = false;
        else {
            if (hf.isHidden()) result = true;
        }
        String name = hf.getName().replace("/", "");
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isHiddenDirectory(Remote) result=" + result + ", Name=" + name);
        return result;
    }

    static public boolean isHiddenFile(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 lf) {
        boolean result = false;
        if (sti.isSyncOptionSyncHiddenFile()) result = false;
        else {
            if (lf.getName().startsWith(".")) result = true;
        }
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isHiddenFile(Local) result=" + result + ", Name=" + lf.getName());
        return result;
    }

    static public boolean isHiddenFile(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile hf) throws JcifsException {
        boolean result = false;
        if (sti.isSyncOptionSyncHiddenFile()) result = false;
        else {
            if (hf.isHidden()) result = true;
        }
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isHiddenFile(Remote) result=" + result + ", Name=" + hf.getName().replace("/", ""));
        return result;
    }

    final static int debug_level_2=2;
    final static int debug_level_3=3;
    static final public boolean isFileSelected(SyncThreadWorkArea stwa, SyncTaskItem sti, String relative_file_path, String full_path,
                                               long file_size, long last_modified_time) {
        boolean selected=true;
        if (sti.isSyncOptionIgnoreFileSize0ByteFile()) {
            if (file_size==0) {
                selected=false;
                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W", "File was ignored, Reason=(File size equals 0), FP="+full_path);
            }
        }
        if (selected && !sti.getSyncFilterFileSizeType().equals(SyncTaskItem.FILTER_FILE_SIZE_TYPE_NONE)) {
            if (sti.getSyncFilterFileSizeType().equals(SyncTaskItem.FILTER_FILE_SIZE_TYPE_LESS_THAN)) {
                if (file_size<stwa.fileSizeFilterValue) selected=true;
                else {
                    selected=false;
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W", "File was ignored, Reason=(File size greater than "+
                            (sti.getSyncFilterFileSizeValue()+" "+sti.getSyncFilterFileSizeUnit())+
                            "). FP="+full_path+", Size="+file_size);
                }
            } else {
                if (file_size>stwa.fileSizeFilterValue) selected=true;
                else {
                    selected=false;
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W", "File was ignored, Reason=(File size less than "+
                            (sti.getSyncFilterFileSizeValue()+" "+sti.getSyncFilterFileSizeUnit())+
                            "). FP="+full_path+", Size="+file_size);
                }
            }
        }
        if (selected) {
            if (sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_OLDER_THAN)) {
                if (last_modified_time<stwa.fileDateFilterValue) selected=true;
                else {
                    selected=false;
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W",
                            String.format("File was ignored, Reason=(File last modified date not older than %1$s. File last modified date=%2$s), FP=%3$s",
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(stwa.fileDateFilterValue),
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(last_modified_time),
                                    full_path));
                }
            } else if (sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_NEWER_THAN)) {
                if (last_modified_time>stwa.fileDateFilterValue) selected=true;
                else {
                    selected=false;
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W",
                            String.format("File was ignored, Reason=(File last modified date not newer than %1$s. File last modified date=%2$s), FP=%3$s",
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(stwa.fileDateFilterValue),
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(last_modified_time),
                                    full_path));
                }
            } else if (sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_AFTER_SYNC_BEGIN_DAY)) {
                if (last_modified_time>stwa.fileDateFilterValue) selected=true;
                else {
                    selected=false;
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "W",
                            String.format("File was ignored, Reason=(File last modified date not \"after begin sync daty\" %1$s. File last modified date=%2$s), FP=%3$s",
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(stwa.fileDateFilterValue),
                                    StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(last_modified_time),
                                    full_path));
                }
            }
        }

        if (selected) selected=isFileSelected(stwa, sti, relative_file_path);
        return selected;

    }

    static final public boolean isFileSelected(SyncThreadWorkArea stwa, SyncTaskItem sti, String relative_file_path) {
        long b_time=System.currentTimeMillis();
        boolean included = false;

        String source_directory = "", tmp_path = relative_file_path;
        if (relative_file_path.startsWith("/")) tmp_path = relative_file_path.substring(1);
        String source_file_name="";
        if (tmp_path.lastIndexOf("/")>=0) {
            source_directory=tmp_path.substring(0, tmp_path.lastIndexOf("/"));
            source_file_name=tmp_path.substring(source_directory.length()+1);//tmp_path.indexOf("/")+1);
        } else {
            source_file_name=tmp_path;
        }
        if (!sti.isSyncProcessRootDirFile()) {//「root直下のファイルは処理するオプションが無効
            if (source_directory.equals("")) {
                //root直下なので処理しない
                if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "isFileSelected not filtered, because Source Dir not processed was effective");
                return false;
            }
        }

        boolean directory_include=isDirectoryIncluded(stwa, source_directory);

        if (directory_include) {
            boolean excluded=false;
            if (stwa.fileExcludeFilterFileNamePattern == null) {
                //nop
            } else {
                Matcher mt;
                if (stwa.fileExcludeFilterWithDirectoryPathPattern!=null) {
                    mt = stwa.fileExcludeFilterWithDirectoryPathPattern.matcher(relative_file_path);
                    if (mt.find()) {
                        included = false;
                        excluded=true;
                    }
                }
                if (included) {
                    mt = stwa.fileExcludeFilterFileNamePattern.matcher(source_file_name);
                    if (mt.find()) {
                        included = false;
                        excluded=true;
                    }
                }
                if (stwa.logLevel>=debug_level_3) stwa.util.addDebugMsg(debug_level_3, "I", "isFileSelected Exclude file result=" + included);
            }
            if (!excluded) {
                if (stwa.fileIncludeFilterFileNamePattern == null) {
                    // nothing filter
                    included = true;
                } else {
                    Matcher mt;
                    if (stwa.fileIncludeFilterWithDirectoryPathPattern!=null) {
                        mt = stwa.fileIncludeFilterWithDirectoryPathPattern.matcher(relative_file_path);
                        if (mt.find()) included = true;
                    }
                    if (!included) {
                        mt = stwa.fileIncludeFilterFileNamePattern.matcher(source_file_name);
                        if (mt.find()) included = true;
                    }
                    if (stwa.logLevel>=debug_level_3) stwa.util.addDebugMsg(debug_level_3, "I", "isFileSelected Include file result=" + included);
                }
            }
        }

        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isFileSelected result=" + included +", directory_include="+directory_include+
                    ", Directory="+source_directory+", File="+source_file_name+", elapsed time="+(System.currentTimeMillis()-b_time));
        return included;
    }

    static public boolean isDirectoryIncluded(SyncThreadWorkArea stwa, String relative_dir) {
        long b_time=System.currentTimeMillis();
        boolean directory_exclude=false;
        boolean directory_include=false;
        String pattern_string="";
        if (stwa.matchFromBeginExcludeDirectoryListPattern.size()>0) {
            for(Pattern pattern:stwa.matchFromBeginExcludeDirectoryListPattern) {
                if (stwa.logLevel>=debug_level_3) pattern_string+=pattern.toString()+";";
                Matcher mt=pattern.matcher("/".concat(relative_dir).concat("/"));
                if (mt.find()) {
                    directory_exclude=true;
                    break;
                }
            }
            if (stwa.logLevel>=debug_level_3)
                stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryIncluded exclude from begin, Directory exclude="+directory_exclude+
                        ", pattern="+pattern_string+", dir="+"/"+relative_dir+"/");
        }

        if (!directory_exclude && stwa.matchAnyWhereExcludeDirectoryListPattern.size()>0) {
            directory_exclude=isDirectoryExcludeMatchedAnyWhere(stwa, relative_dir, stwa.matchAnyWhereExcludeDirectoryListPattern);
        }

        if (!directory_exclude) {
            if (stwa.matchFromBeginIncludeDirectoryListPattern.size()>0) {// || stwa.directoryIncludePatternMatchAnyPosition!=null) {
                for(Pattern pattern:stwa.matchFromBeginIncludeDirectoryListPattern) {
                    if (stwa.logLevel>=debug_level_3) pattern_string+=pattern.toString()+";";
                    Matcher mt=pattern.matcher("/".concat(relative_dir).concat("/"));
                    if (mt.find()) {
                        directory_include=true;
                        break;
                    }
                }
                if (stwa.logLevel>=debug_level_3)
                    stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryIncluded include from begin, Directory include="+directory_include+
                        ", pattern="+pattern_string+", dir="+"/"+relative_dir+"/");
            } else {
                directory_include=true;
            }
        }
        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryIncluded result="+directory_include+", elapsed time="+(System.currentTimeMillis()-b_time));

        return directory_include;
    }


    static final public boolean isDirectoryToBeProcessed(SyncThreadWorkArea stwa, String relative_dir) {
        long b_time=System.currentTimeMillis();
        boolean inc = false, exc = false, result = false;
        String reformed_abs_path=StringUtil.removeRedundantCharacter(relative_dir, "/", true, true);

        boolean include_filter_specified=true;
        boolean exclude_filter_specified=true;

        if (!reformed_abs_path.equals("")) {
            Matcher mt;
            if (stwa.matchFromBeginExcludeDirectoryList.size()==0 && stwa.matchAnyWhereExcludeDirectoryList.size()==0) {
                // nothing filter
                exc = false;
                exclude_filter_specified=false;
            } else {
                if (stwa.matchAnyWhereExcludeDirectoryList.size()>0) {
                    exc= isDirectoryExcludeMatchedAnyWhere(stwa, reformed_abs_path, stwa.matchAnyWhereExcludeDirectoryListPattern);
                }

                if (!exc && stwa.matchFromBeginExcludeDirectoryList.size()>0) {
                    exc= isDirectoryExcludeMatchedFromBeginByDirectoryHierarchy(stwa, reformed_abs_path, stwa.matchFromBeginExcludeDirectoryList);
                }
            }
            if (!exc) {
                if (stwa.matchFromBeginIncludeDirectoryList.size()==0) {
                    // nothing filter
                    inc = true;
                    include_filter_specified=false;
                } else {
                    inc= isDirectoryIncludeMatchedFromBeginByDirectoryHierarchy(stwa, reformed_abs_path, stwa.matchFromBeginIncludeDirectoryList);
                }
            }
            result=inc;
        } else {
            result=true;
        }

        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryToBeProcessed" +
                 " include matched=" + inc + ", include specified="+include_filter_specified+
                ", exclude matched=" + exc + ", exclude specified="+exclude_filter_specified+
                ", result=" + result + ", dir=" + reformed_abs_path+", elapse time="+(System.currentTimeMillis()-b_time));
        return result;
    }

    static private boolean isDirectoryExcludeMatchedAnyWhere(SyncThreadWorkArea stwa, String reformed_abs_path, ArrayList<Pattern>pattern_list) {
        long b_time=System.currentTimeMillis();
        boolean exc=false;
        String exc_dir="/".concat(reformed_abs_path).concat("/");
        String match_pattern="";
        for(Pattern exc_pattern:pattern_list) {
            Matcher exc_mt=exc_pattern.matcher(exc_dir);
            if (exc_mt.find()) {
                exc=true;
                match_pattern=exc_pattern.toString();
                break;
            }
        }
        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryExcludeMatchedAnyWhere result="+exc+", pattern="+match_pattern+", dir="+exc_dir+
                    ", elapsed time="+(System.currentTimeMillis()-b_time));
        return exc;
    }

    static private boolean isDirectoryExcludeMatchedFromBeginByDirectoryHierarchy(SyncThreadWorkArea stwa, String reformed_abs_path, ArrayList<String>filter_list) {
        long b_time=System.currentTimeMillis();
        int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
        boolean filter_matched=false, filter_specified=false;

        String[] directory_array=reformed_abs_path.split("/");
        String match_pattern="";
        if (filter_list.size()>0) {
            filter_specified=true;
            for(String filter:filter_list) {
                String[] exc_filter_array=filter.split("/");

                if (directory_array.length>=exc_filter_array.length) {
                    Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp(filter), flags);
                    Matcher mt=pattern.matcher(reformed_abs_path);
                    if (stwa.logLevel>=debug_level_3) match_pattern=pattern.toString();
                    if (mt.find()) {
                        filter_matched=true;
                        break;
                    }
                }
            }
        }
        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryExcludeMatchedFromBeginByDirectoryHierarchy result="+filter_matched+", pattern="+match_pattern+", dir="+reformed_abs_path+
                    ", elapsed time="+(System.currentTimeMillis()-b_time));
        return filter_matched;
    }

    static private boolean isDirectoryIncludeMatchedFromBeginByDirectoryHierarchy(SyncThreadWorkArea stwa, String reformed_abs_path, ArrayList<String>filter_list) {
        //ディレクトリーの階層で比較して一致する場合にはtrue、ディレクトリーの階層とは"/"で区切られたディレクトリー名の集合
        long b_time=System.currentTimeMillis();
        int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
        boolean filter_matched=false, filter_specified=false;

        String[] directory_array=reformed_abs_path.split("/");
        String matching_filter="/", matching_directory="/", inc_pattern="";
        if (filter_list.size()>0) {
            filter_specified=true;
            for(String filter:filter_list) {
                String[] inc_filter_array=filter.split("/");
                boolean found=false;
                matching_filter="/";
                matching_directory="/";
                //比較用のDirectory Pathとパターンの作成(同じ階層までのディレクトリー名を比較)
                int loop_count=Math.min(directory_array.length, inc_filter_array.length);
                for(int i=0;i<loop_count;i++) {
                    matching_filter=matching_filter.concat(inc_filter_array[i]).concat("/");
                    matching_directory=matching_directory.concat(directory_array[i]).concat("/");
                }
                Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp(matching_filter), flags);
                Matcher mt=pattern.matcher(matching_directory);
                if (stwa.logLevel>=debug_level_3) inc_pattern=pattern.toString();
                if (mt.find()) {
                    filter_matched=true;
                    break;
                }
            }
        }
        if (stwa.logLevel>=debug_level_3)
            stwa.util.addDebugMsg(debug_level_3, "I", "isDirectoryMatchedFromBeginByDirectoryHierarchy result="+filter_matched+", pattern="+inc_pattern+", dir="+matching_directory+
                    ", elapsed time="+(System.currentTimeMillis()-b_time));
        return filter_matched;
    }

    private void addPresetFileFilter(ArrayList<FilterListAdapter.FilterListItem> ff, String[] preset_ff) {
        for (String add_str : preset_ff) {
            boolean found = false;
            for (FilterListAdapter.FilterListItem ff_str : ff) {
                if (ff_str.getFilter().equals(add_str)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                FilterListAdapter.FilterListItem fli=new FilterListAdapter.FilterListItem(add_str, true);
                ff.add(fli);
            } else {
                mStwa.util.addDebugMsg(1, "I", "addPresetFileFilter" + " Duplicate file filter=" + add_str);
            }
        }
    }

//    final private static String WHOLE_DIRECTORY_FILTER_PREFIX="\\\\";

    private String convertFilterToRegExp(ArrayList<FilterListAdapter.FilterListItem> filter_array, String prefix, String suffix) {
        String out_filter = "";
        String filter, cni = "";

        for(FilterListAdapter.FilterListItem in_filter:filter_array) {
            String rf_file_filter=in_filter.getFilter();
            while(rf_file_filter.indexOf(";;")>=0) rf_file_filter=rf_file_filter.replaceAll(";;",";");
            if (rf_file_filter.endsWith(";")) rf_file_filter=rf_file_filter.substring(0,rf_file_filter.length()-1);
            out_filter = out_filter + cni + prefix+ MiscUtil.convertRegExp(rf_file_filter)+suffix;
            cni = "|";
        }
        return out_filter;
    }

    private ArrayList<Pattern[]> convertDirectoryFilterToRegExp(ArrayList<FilterListAdapter.FilterListItem> filter_array, String prefix, String suffix) {
        ArrayList<Pattern[]>fpl=new ArrayList<Pattern[]>();
        for(FilterListAdapter.FilterListItem item:filter_array) {
            String[] dir_parts=item.getFilter().split("/");
            Pattern[] dir_pattern=new Pattern[dir_parts.length];
            for(int i=0;i<dir_parts.length;i++) {
                dir_pattern[i]=Pattern.compile("^"+ MiscUtil.convertRegExp(dir_parts[i])+"$");
//                mStwa.util.addDebugMsg(1,"I","pattern="+dir_pattern[i].pattern());
            }
            fpl.add(dir_pattern);
        }
        return fpl;
    }

    private ArrayList<String[]> convertDirectoryFilterToDirName(ArrayList<FilterListAdapter.FilterListItem> filter_array) {
        ArrayList<String[]>fpl=new ArrayList<String[]>();
        for(FilterListAdapter.FilterListItem item:filter_array) {
            String[] dir_parts=item.getFilter().split("/");
            fpl.add(dir_parts);
        }
        return fpl;
    }

    final private int compileFilter(SyncTaskItem sti, ArrayList<FilterListAdapter.FilterListItem> s_ff, ArrayList<FilterListAdapter.FilterListItem> s_df) {
        mStwa.util.addDebugMsg(1, "I", "compileFilter Ignore 0 byte file="+sti.isSyncOptionIgnoreFileSize0ByteFile());
        if (!sti.getSyncFilterFileSizeType().equals(SyncTaskItem.FILTER_FILE_SIZE_TYPE_NONE)) {
            long filter_value=Long.parseLong(sti.getSyncFilterFileSizeValue());
            if (sti.getSyncFilterFileSizeUnit().equals(SyncTaskItem.FILTER_FILE_SIZE_UNIT_BYTE)) mStwa.fileSizeFilterValue=filter_value;
            else if (sti.getSyncFilterFileSizeUnit().equals(SyncTaskItem.FILTER_FILE_SIZE_UNIT_KIB)) mStwa.fileSizeFilterValue=filter_value*1024;
            else if (sti.getSyncFilterFileSizeUnit().equals(SyncTaskItem.FILTER_FILE_SIZE_UNIT_MIB)) mStwa.fileSizeFilterValue=filter_value*1024*1024;
            else if (sti.getSyncFilterFileSizeUnit().equals(SyncTaskItem.FILTER_FILE_SIZE_UNIT_GIB)) mStwa.fileSizeFilterValue=filter_value*1024*1024*1024;
        } else {
            mStwa.fileSizeFilterValue=-1;
        }
        mStwa.util.addDebugMsg(1, "I", "compileFilter file size filter type="+sti.getSyncFilterFileSizeType()+
                ", filter value="+sti.getSyncFilterFileSizeValue()+", filter unit="+sti.getSyncFilterFileSizeUnit()+", calc size="+mStwa.fileSizeFilterValue);
        if (sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_AFTER_SYNC_BEGIN_DAY)) {
            Calendar curr_date=Calendar.getInstance();
            int year=curr_date.get(Calendar.YEAR);
            int month=curr_date.get(Calendar.MONTH);
            int day=curr_date.get(Calendar.DAY_OF_MONTH);
            curr_date.clear();
            curr_date.set(year, month, day);
            mStwa.fileDateFilterValue=curr_date.getTimeInMillis();
            mStwa.util.addDebugMsg(1, "I", "compileFilter file date filter type="+sti.getSyncFilterFileDateType()+
                    ", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(mStwa.fileDateFilterValue));
        } else if (sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_NEWER_THAN) || sti.getSyncFilterFileDateType().equals(SyncTaskItem.FILTER_FILE_DATE_TYPE_OLDER_THAN)) {
            int filter_value=-1*Integer.parseInt(sti.getSyncFilterFileDateValue());
            Calendar curr_date=Calendar.getInstance();
            curr_date.add(Calendar.DAY_OF_YEAR, filter_value);
            int year=curr_date.get(Calendar.YEAR);
            int month=curr_date.get(Calendar.MONTH);
            int day=curr_date.get(Calendar.DAY_OF_MONTH);
            curr_date.clear();
            curr_date.set(year, month, day);
            mStwa.fileDateFilterValue=curr_date.getTimeInMillis();
            mStwa.util.addDebugMsg(1, "I", "compileFilter file date filter type="+sti.getSyncFilterFileDateType()+
                    ", value="+sti.getSyncFilterFileDateValue()+
                    ", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(mStwa.fileDateFilterValue));
        } else {
            mStwa.fileDateFilterValue=-1;
            mStwa.util.addDebugMsg(1, "I", "compileFilter file date filter=NONE");
        }

        ArrayList<FilterListAdapter.FilterListItem> include_file_filter = new ArrayList<FilterListAdapter.FilterListItem>();
        ArrayList<FilterListAdapter.FilterListItem> exclude_file_filter = new ArrayList<FilterListAdapter.FilterListItem>();
        ArrayList<FilterListAdapter.FilterListItem> include_file_filter_with_directory_path = new ArrayList<FilterListAdapter.FilterListItem>();
        ArrayList<FilterListAdapter.FilterListItem> exclude_file_filter_with_directory_path = new ArrayList<FilterListAdapter.FilterListItem>();
        for(FilterListAdapter.FilterListItem file_filter:s_ff) {
            String filter=file_filter.getFilter();
            String[] filter_array=filter.split(";");
            for(String part:filter_array) {
                FilterListAdapter.FilterListItem new_fli=new FilterListAdapter.FilterListItem(part, file_filter.isInclude());
                if (part.contains("/")) {
                    if (file_filter.isInclude()) include_file_filter_with_directory_path.add(new_fli);
                    else  exclude_file_filter_with_directory_path.add(new_fli);
                } else {
                    if (file_filter.isInclude()) include_file_filter.add(new_fli);
                    else  exclude_file_filter.add(new_fli);
                }
            }
        }
        Collections.sort(include_file_filter);
        Collections.sort(exclude_file_filter);
        Collections.sort(include_file_filter_with_directory_path);
        Collections.sort(exclude_file_filter_with_directory_path);

        String reg_exp_include_file_filter_file_name=convertFilterToRegExp(include_file_filter, "^", "$");
        String reg_exp_exclude_file_filter_file_name=convertFilterToRegExp(exclude_file_filter, "^", "$");
        String reg_exp_include_file_filter_with_directory_path=convertFilterToRegExp(include_file_filter_with_directory_path, "^", "$");
        String reg_exp_exclude_file_filter_with_directory_path=convertFilterToRegExp(exclude_file_filter_with_directory_path, "^", "$");

        mStwa.matchAnyWhereExcludeDirectoryList.clear();
        mStwa.matchFromBeginIncludeDirectoryList.clear();
        mStwa.matchFromBeginExcludeDirectoryList.clear();
        for (FilterListAdapter.FilterListItem fli:s_df) {
            if (fli.isEnabled()) {
                String[] filter_array=fli.getFilter().split(";");
                for(String item:filter_array) {
                    if (item.contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                        mStwa.matchAnyWhereExcludeDirectoryList.add(item.substring(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX_LENGTH));
                    } else {
                        if (fli.isInclude()) mStwa.matchFromBeginIncludeDirectoryList.add(item);
                        else mStwa.matchFromBeginExcludeDirectoryList.add(item);
                    }
                }
            }
        }
        Collections.sort(mStwa.matchAnyWhereExcludeDirectoryList);
        Collections.sort(mStwa.matchFromBeginExcludeDirectoryList);
        Collections.sort(mStwa.matchFromBeginIncludeDirectoryList);

        int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;

        mStwa.matchAnyWhereExcludeDirectoryListPattern.clear();
        if (mStwa.matchAnyWhereExcludeDirectoryList.size()>0) {
            for(String item:mStwa.matchAnyWhereExcludeDirectoryList) {
                Pattern pattern=Pattern.compile(MiscUtil.convertRegExp("/"+item+"/"), flags);
                mStwa.matchAnyWhereExcludeDirectoryListPattern.add(pattern);
            }
        }


        mStwa.matchFromBeginIncludeDirectoryListPattern.clear();
        if (mStwa.matchFromBeginIncludeDirectoryList.size()>0) {
            for(String item:mStwa.matchFromBeginIncludeDirectoryList) {
                Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp("/"+item+"/"), flags);
                mStwa.matchFromBeginIncludeDirectoryListPattern.add(pattern);
            }
        }

        mStwa.matchFromBeginExcludeDirectoryListPattern.clear();
        if (mStwa.matchFromBeginExcludeDirectoryList.size()>0) {
            for(String item:mStwa.matchFromBeginExcludeDirectoryList) {
                Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp("/"+item+"/"), flags);
                mStwa.matchFromBeginExcludeDirectoryListPattern.add(pattern);
            }
        }

        mStwa.fileIncludeFilterFileNamePattern = mStwa.fileExcludeFilterFileNamePattern = null;
        mStwa.fileIncludeFilterWithDirectoryPathPattern = mStwa.fileExcludeFilterWithDirectoryPathPattern = null;

        if (reg_exp_include_file_filter_file_name.length() != 0)
            mStwa.fileIncludeFilterFileNamePattern = Pattern.compile("(" + reg_exp_include_file_filter_file_name + ")", flags);
        if (reg_exp_exclude_file_filter_file_name.length() != 0)
            mStwa.fileExcludeFilterFileNamePattern = Pattern.compile("(" + reg_exp_exclude_file_filter_file_name + ")", flags);

        if (reg_exp_include_file_filter_with_directory_path.length() != 0)
            mStwa.fileIncludeFilterWithDirectoryPathPattern = Pattern.compile("(" + reg_exp_include_file_filter_with_directory_path + ")", flags);
        if (reg_exp_exclude_file_filter_with_directory_path.length() != 0)
            mStwa.fileExcludeFilterWithDirectoryPathPattern = Pattern.compile("(" + reg_exp_exclude_file_filter_with_directory_path + ")", flags);


        String dir_inc="", dir_exc="", any_exc="";
        for(String item:mStwa.matchFromBeginIncludeDirectoryList) dir_inc+=item+";";
        for(String item:mStwa.matchFromBeginExcludeDirectoryList) dir_exc+=item+";";
        for(String item:mStwa.matchAnyWhereExcludeDirectoryList) any_exc+=item+";";
        mStwa.util.addDebugMsg(1, "I", "compileFilter" + " File filter File name only Include=" + reg_exp_include_file_filter_file_name + ", Exclude=" + reg_exp_exclude_file_filter_file_name);
        mStwa.util.addDebugMsg(1, "I", "             " + " File filter with directory path Include=" + reg_exp_include_file_filter_with_directory_path + ", exclude=" + reg_exp_exclude_file_filter_with_directory_path);
        mStwa.util.addDebugMsg(1, "I", "             " + " Directory Match from begin exclude=" + dir_exc);
        mStwa.util.addDebugMsg(1, "I", "             " + " Directory Match from begin include=" + dir_inc);
        mStwa.util.addDebugMsg(1, "I", "             " + " Directory Match from any position exclude=" + any_exc);

        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }

    static public boolean isTaskCancelled(boolean wait, final ThreadCtrl tc) {
        if (wait) tc.writeLockWait();
        return isTaskCancelled(tc);
    }

    static public boolean isTaskCancelled(final ThreadCtrl tc) {
        if (!tc.isEnabled()) return true;
        return false;
    }

    static public void cancelTask(final ThreadCtrl tc) {
        synchronized(tc) {
            tc.setDisabled();
            tc.releaseWriteLock();
        }
    }

    static public void lockTask(final ThreadCtrl tc) {
        tc.acuireWriteLock();
    }

    static public void unlockTask(final ThreadCtrl tc) {
        tc.releaseWriteLock();
    }

    private void prepareMediaScanner() {
        mStwa.mediaScanner = new MediaScannerConnection(mStwa.appContext, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                if (mStwa.util.getLogLevel() >= 1)
                    mStwa.util.addDebugMsg(1, "I", "MediaScanner connected.");
                synchronized (mStwa.mediaScanner) {
                    mStwa.mediaScanner.notify();
                }
            }
            @Override
            public void onScanCompleted(final String fp, final Uri uri) {
                if (mStwa.util.getLogLevel() >= 1)
                    mStwa.util.addDebugMsg(1, "I", "MediaScanner scan completed. fn=", fp, ", Uri=" + uri);
            }
        });
        mStwa.mediaScanner.connect();
        if (!mStwa.mediaScanner.isConnected()) {
            synchronized (mStwa.mediaScanner) {
                try {
                    mStwa.mediaScanner.wait(1000*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void closeMediaScanner() {
        mStwa.util.addDebugMsg(1, "I", "MediaScanner disconnect requested.");
        mStwa.mediaScanner.disconnect();
    }

    static public void scanMediaFile(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 sf) {
        String fp=sf.getParent();
        if (getFileExtention(fp).equals("")) {
            stwa.util.addDebugMsg(1, "I", "MediaScanner scan ignored because file extention does not exists. fp=", fp);
            return;
        }
        if (!stwa.mediaScanner.isConnected()) {
            stwa.util.addLogMsg("W", fp, "Media scanner not invoked, because mdeia scanner was not connected.");
            return;
        }
        stwa.util.addDebugMsg(1, "I", "MediaScanner scan request issued. fp=", fp);
        stwa.mediaScanner.scanFile(fp, null);
    }

    static public String getFileExtention(String fp) {
        String ft="", fn="";
        int sep_pos=fp.lastIndexOf("/");
        if (sep_pos>=0) {
            fn=fp.substring(sep_pos+1);
        } else {
            fn=fp;
        }
        int ext_pos=fn.lastIndexOf(".");
        if (ext_pos >= 0) {
            ft = fn.substring(ext_pos+1).toLowerCase();
        }
        return ft;
    }

    private String mSyncHistroryResultFilepath = "";

    //source: https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
    final private static String[] SYNC_LOG_FILE_INVALID_CHARS =new String[]{"<", ">", ":", "\"", "/", "\\", "|", "?", "*", " "};
    final private static String[] SYNC_LOG_FILE_INVALID_CHARS_TAIL =new String[]{".", " "};
    final private String createSyncResultFilePath(String syncProfName) {
        String dir = mGp.settingAppManagemsntDirectoryName + "/result_log";
        File tlf = new File(dir);
        if (!tlf.exists()) {
            boolean create = tlf.mkdirs();
        }
        String dt = StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis()).replaceAll("/", "-").replaceAll(":", "").replaceAll(" ", "_");
        String fn = "result_" + syncProfName;
        if ((fn.length() + dt.length()) > 250) {//250 = 255-5 for "_" and ".txt" appended at end of file name
            fn = fn.substring(0, 250 - dt.length());
        }

        fn += "_" + dt;
        for(String invalid_str: SYNC_LOG_FILE_INVALID_CHARS) {
            fn = fn.replaceAll(Pattern.quote(invalid_str), "_");
        }
        for(String invalid_str: SYNC_LOG_FILE_INVALID_CHARS_TAIL) {
            if (fn.endsWith(invalid_str))
                fn = fn.replaceFirst(Pattern.quote(invalid_str), "_");
        }

        fn+=".txt";
        String fp = dir + "/" + fn;
        return fp;
    }

    final private void openSyncResultLog(SyncTaskItem sti) {
        if (!mStwa.gp.settingWriteSyncResultLog) return;
        mSyncHistroryResultFilepath = createSyncResultFilePath(sti.getSyncTaskName());
        if (mStwa.syncHistoryWriter != null) closeSyncResultLog();
        if (mSyncHistroryResultFilepath!=null) {
            try {
                SafFile3 mf =new SafFile3(mStwa.appContext, mSyncHistroryResultFilepath);
                SafFile3 dr=mf.getParentFile();
                if (!dr.exists()) dr.mkdirs();
                if (!mf.exists()) mf.createNewFile();
                OutputStream fos=mf.getOutputStream();
                PrintWriter bow=new PrintWriter(new BufferedOutputStream(fos, GENERAL_IO_BUFFER_SIZE));
                mStwa.syncHistoryWriter = new PrintWriter(bow, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mStwa.util.addDebugMsg(1, "W", "Result log file not created, fp="+mSyncHistroryResultFilepath);
        }
    }

    private void closeSyncResultLog() {
        if (mStwa.syncHistoryWriter != null) {
            final PrintWriter pw = mStwa.syncHistoryWriter;
            Thread th = new Thread() {
                @Override
                public void run() {
                    pw.flush();
                    pw.close();
                }
            };
            th.start();
            mStwa.syncHistoryWriter = null;
        }
    }

    final private void addHistoryList(SyncTaskItem sti,
                                      int status, int copy_cnt, int del_cnt, int ignore_cnt, int move_cnt, int retry_cnt, int replace_cnt,
                                      String error_msg, long sync_elapsed_time, String sync_transfer_speed, String req_id) {
        String date_time = StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
        String date = date_time.substring(0, 10);
        String time = date_time.substring(11);
        final HistoryListAdapter.HistoryListItem hli = new HistoryListAdapter.HistoryListItem();
        hli.sync_date = date;
        hli.sync_time = time;
        hli.sync_elapsed_time = sync_elapsed_time;
        hli.sync_transfer_speed = sync_transfer_speed;
        hli.sync_task = sti.getSyncTaskName();
        hli.sync_status = status;
        hli.sync_test_mode = sti.isSyncTestMode();

        hli.sync_result_no_of_copied = copy_cnt;
        hli.sync_result_no_of_deleted = del_cnt;
        hli.sync_result_no_of_ignored = ignore_cnt;
        hli.sync_result_no_of_moved = move_cnt;
        hli.sync_result_no_of_replaced = replace_cnt;
        hli.sync_result_no_of_retry = retry_cnt;
        hli.sync_req = req_id;
        hli.sync_error_text = error_msg;
        hli.sync_result_file_path = mSyncHistroryResultFilepath;
        SyncTaskItem pfli = TaskListUtils.getSyncTaskByName(mGp.syncTaskList, sti.getSyncTaskName());
        if (pfli != null) {
            pfli.setLastSyncTime(date + " " + time);
            pfli.setLastSyncResult(status);
        }
        if (mGp.syncHistoryListAdapter != null) {
            mGp.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mGp.syncHistoryList.add(0, hli);
                    mGp.syncHistoryListAdapter.notifyDataSetChanged();
                    mStwa.util.saveHistoryList(mGp.syncHistoryList);
                }
            });
        } else {
            mGp.syncHistoryList.add(0, hli);
            mStwa.util.saveHistoryList(mGp.syncHistoryList);
        }
    }

    static final public String calTransferRate(long tb, long tt) {
        String tfs = null;
        String units = null;
        BigDecimal bd_tr;

        if (tb == 0) return "0 Bytes/sec";

        if (tt == 0) return "N/A"; // elapsed time 0 msec

        BigDecimal dfs = new BigDecimal(tb * 1.000000);
        BigDecimal dft1 = new BigDecimal(tt * 1.000);
        BigDecimal dft2 = new BigDecimal(1000.000);
        BigDecimal dft = new BigDecimal("0.000000");
        dft = dft1.divide(dft2); // convert elapsed time from msec to sec
        BigDecimal bd_tr1 = dfs.divide(dft, 6, BigDecimal.ROUND_HALF_UP); // transfer speed in bytes /sec

        if (bd_tr1.compareTo(new BigDecimal(1048576)) >= 0) {//  MB/sec (transfer speed >= 1024 * 1024 bytes)
            units = " MBytes/sec";
            BigDecimal bd_tr2 = new BigDecimal(1024 * 1024 * 1.000000);
            bd_tr = bd_tr1.divide(bd_tr2, 2, BigDecimal.ROUND_HALF_UP);
        } else if (bd_tr1.compareTo(new BigDecimal(1024)) >= 0) { // KB/sec (transfer speed >= 1024 bytes)
            units = " KBytes/sec";
            BigDecimal bd_tr2 = new BigDecimal(1024 * 1.000000);
            bd_tr = bd_tr1.divide(bd_tr2, 1, BigDecimal.ROUND_HALF_UP);
        } else { // Bytes/sec
            units = " Bytes/sec";
            bd_tr = bd_tr1.setScale(0, RoundingMode.HALF_UP);
        }

        // proper formatting and grouping
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        DecimalFormat formatter = new DecimalFormat("###,###.###", formatSymbols);
        tfs = formatter.format(bd_tr) + units;

        return tfs;
    }
}