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

import android.content.ContentProviderClient;

import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;

import com.sentaroh.android.SMBSync3.SyncThread.SyncThreadWorkArea;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.SafFile3;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static com.sentaroh.android.SMBSync3.Constants.*;

public class SyncThreadSyncFile {
    static private boolean isSafDirectory(SafFile3 sf, ContentProviderClient cpc) {
        return cpc!=null?sf.isDirectory(cpc):sf.isDirectory();
    }

    static private boolean isSafExists(SafFile3 sf, ContentProviderClient cpc) {
        return cpc!=null?sf.exists(cpc):sf.exists();
    }

    static private SafFile3[] listSafFiles(SafFile3 sf, ContentProviderClient cpc) {
        return cpc!=null?sf.listFiles(cpc):sf.listFiles();
    }

    static final private int syncDeleteLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_base,
                                                    String source_dir, String to_base, String destination_dir, SafFile3 tf, ContentProviderClient cpc, boolean isTakenDateUsed) {
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " source=", source_dir, ", destination=", destination_dir);
        String relative_dir = destination_dir.replace(to_base, "");
        if (relative_dir.startsWith("/")) relative_dir = relative_dir.substring(1);
        SafFile3 mf = new SafFile3(stwa.appContext, source_dir);
        boolean mf_exists=mf.exists();
        if (isSafDirectory(tf, cpc)) { // Directory Delete
            boolean isDirectoryToBeProcessed=SyncThread.isDirectoryToBeProcessed(stwa, relative_dir);
            if (isDirectoryToBeProcessed) {
                if (mf_exists) {
                    if (!SyncThread.isHiddenDirectory(stwa, sti, tf)) {
                        SafFile3[] children = listSafFiles(tf, cpc);
                        if (children != null) {
                            for (SafFile3 element : children) {
                                String child_file_name = element.getName();
                                boolean isFile=!isSafDirectory(element, cpc);
                                if (isFile) {
                                    sync_result = syncDeleteLocalToLocal(stwa, sti, from_base, source_dir + "/" + child_file_name,
                                            to_base, destination_dir + "/" + child_file_name, element, cpc, isTakenDateUsed);
                                } else {
                                    if (sti.isSyncOptionSyncSubDirectory()) {
                                        sync_result = syncDeleteLocalToLocal(stwa, sti, from_base, source_dir + "/" + child_file_name,
                                                to_base, destination_dir + "/" + child_file_name, element, cpc, isTakenDateUsed);
                                    } else {
                                        stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, source_dir + "/" + child_file_name));
                                        stwa.totalIgnoreCount++;
                                    }
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                } else if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS){
                                    break;
                                }
                            }
                        } else {
                            stwa.util.addDebugMsg(1, "W", sti.getSyncTaskName(), "File list was null, fp=" + tf.getPath());
                        }
                    }
                } else {
                    sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                }
            } else {
                if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                    sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                }
            }
        } else { // file Delete
            if (!SyncThread.isHiddenFile(stwa, sti, tf)) {
                boolean isFileSelected=SyncThread.isFileSelected(stwa, sti, relative_dir);
                if (isFileSelected) {
                    if (!mf_exists) {
                        sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                    }
                } else {
                    if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                        sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                    }
                }
            }
        }

        return sync_result;
    }

    static final private int syncDeleteLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_base,
                                                  String source_dir, String to_base, String destination_dir, JcifsFile tf) {
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " source=", source_dir, ", destination=", destination_dir);
        String relative_dir = destination_dir.substring(to_base.length());
        if (relative_dir.startsWith("/")) relative_dir = relative_dir.substring(1);
        SafFile3 mf = new SafFile3(stwa.appContext,  source_dir);
        boolean mf_exists=mf.exists();
        try {
            if (tf.isDirectory()) { // Directory Delete
                boolean isDirectoryToBeProcessed=SyncThread.isDirectoryToBeProcessed(stwa, relative_dir);
                if (isDirectoryToBeProcessed) {
                    if (mf_exists) {
                        if (!SyncThread.isHiddenDirectory(stwa, sti, tf) ) {
                            JcifsFile[] children = tf.listFiles();
                            if (children != null) {
                                for (JcifsFile element : children) {
                                    String tmp = element.getName();
                                    if (tmp.lastIndexOf("/") > 0) tmp = tmp.substring(0, tmp.lastIndexOf("/"));
                                    while (stwa.retryCount > 0) {
                                        if (element.isDirectory()) {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = syncDeleteLocalToSmb(stwa, sti, from_base, source_dir + "/" + tmp,
                                                        to_base, destination_dir+tmp+"/", element);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, source_dir + "/" + tmp));
                                                stwa.totalIgnoreCount++;
                                            }
                                        } else {
                                            sync_result = syncDeleteLocalToSmb(stwa, sti, from_base, source_dir + "/" + tmp,
                                                    to_base, destination_dir+tmp+"/", element);
                                        }
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                                            stwa.retryCount--;
                                            if (stwa.retryCount > 0)
                                                sync_result = waitRetryInterval(stwa);
                                            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                                                break;
                                        } else {
                                            break;
                                        }
                                    }
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                    if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();

                                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                        sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                        break;
                                    } else if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS){
                                        break;
                                    }
                                }
                            } else {
                                stwa.util.addDebugMsg(1, "W", sti.getSyncTaskName(), "File list was null, fp=" + tf.getPath());
                            }
                        }
                    } else {
                        sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }
                } else {
                    if (sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter()){// && !isDirectoryToBeProcessed)) {
                        sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }
                }
            } else { // file Delete
                if (!SyncThread.isHiddenDirectory(stwa, sti, tf) ) {
                    boolean isFileSelected=SyncThread.isFileSelected(stwa, sti, relative_dir);
                    if (isFileSelected) {
                        if (!mf_exists) {
                            sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    } else {
                        if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                            sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    }
                }
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti, e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sync_result;
    }

    static final private int syncDeleteSmbToLocal(SyncThreadWorkArea stwa,
                                                  SyncTaskItem sti, String from_base, String source_dir, String to_base, String destination_dir,
                                                  SafFile3 tf, ArrayList<String> smb_fl, ContentProviderClient cpc) {
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " source=", source_dir, ", destination=", destination_dir);
        try {
            String relative_dir = destination_dir.substring(to_base.length());
            if (relative_dir.startsWith("/")) relative_dir = relative_dir.substring(1);
            boolean mf_exists=isSmbFileExists(stwa, smb_fl, source_dir);
            if (isSafDirectory(tf, cpc)) { // Directory Delete
                boolean isDirectoryToBeProcessed=SyncThread.isDirectoryToBeProcessed(stwa, relative_dir);
                if (isDirectoryToBeProcessed) {
                    if (mf_exists) {
                        if (!SyncThread.isHiddenDirectory(stwa, sti, tf)) {
                            SafFile3[] children = listSafFiles(tf, cpc);
                            if (children != null) {
                                for (SafFile3 element : children) {
                                    String tmp = element.getName();
                                    boolean isFile=!isSafDirectory(element, cpc);
                                    while (stwa.retryCount > 0) {
                                        if (isFile) {
                                            sync_result = syncDeleteSmbToLocal(stwa, sti, from_base, source_dir + tmp,
                                                    to_base, destination_dir + "/" + tmp, element, smb_fl, cpc);
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = syncDeleteSmbToLocal(stwa, sti, from_base, source_dir + tmp+"/" ,
                                                        to_base, destination_dir + "/" + tmp, element, smb_fl, cpc);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, source_dir + tmp));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                                            stwa.retryCount--;
                                            if (stwa.retryCount > 0)
                                                sync_result = waitRetryInterval(stwa);
                                            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                                                break;
                                        } else {
                                            break;
                                        }
                                    }
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                    if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();

                                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                        sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                        break;
                                    } else if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS){
                                        break;
                                    }
                                }
                            } else {
                                stwa.util.addDebugMsg(1, "W", sti.getSyncTaskName(), "File list was null, fp=" + tf.getPath());
                            }
                        }
                    } else {
                        sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }
                } else {
                    if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                        sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }

                }
            } else { // file Delete
                if (!SyncThread.isHiddenFile(stwa, sti, tf)) {
                    boolean isFileSelected=SyncThread.isFileSelected(stwa, sti, relative_dir);
                    if (isFileSelected) {
                        if (!mf_exists) {
                            sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    } else {
                        if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                            sync_result= deleteLocalItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    }
                }
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti, e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sync_result;
    }

    static final private int syncDeleteSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_base,
                                                String source_dir, String to_base, String destination_dir, JcifsFile tf, ArrayList<String> smb_fl) {
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " source=", source_dir, ", destination=", destination_dir);
        try {
            String relative_dir = destination_dir.substring(to_base.length());
            if (relative_dir.startsWith("/")) relative_dir = relative_dir.substring(1);
            boolean mf_exists=isSmbFileExists(stwa, smb_fl, source_dir);
            if (tf.isDirectory()) { // Directory Delete
                boolean isDirectoryToBeProcessed=SyncThread.isDirectoryToBeProcessed(stwa, relative_dir);
                if (isDirectoryToBeProcessed) {
                    if (mf_exists) {
                        if (!SyncThread.isHiddenDirectory(stwa, sti, tf)) {
                            JcifsFile[] children = tf.listFiles();
                            if (children != null) {
                                for (JcifsFile element : children) {
                                    String tmp_fname = element.getName();
                                    while (stwa.retryCount > 0) {
                                        if (element.isFile()) {
                                            sync_result = syncDeleteSmbToSmb(stwa, sti, from_base, source_dir + tmp_fname,
                                                    to_base, destination_dir + tmp_fname, element, smb_fl);
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()){
                                                sync_result = syncDeleteSmbToSmb(stwa, sti, from_base, source_dir + tmp_fname,
                                                        to_base, destination_dir + tmp_fname, element, smb_fl);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, source_dir + tmp_fname));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                                            stwa.retryCount--;
                                            if (stwa.retryCount > 0)
                                                sync_result = waitRetryInterval(stwa);
                                            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                                                break;
                                        } else {
                                            break;
                                        }
                                    }
                                    if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;

                                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                        sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                        break;
                                    } else if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS){
                                        break;
                                    }
                                }
                            } else {
                                stwa.util.addDebugMsg(1, "W", sti.getSyncTaskName(), "File list was null, fp=" + tf.getPath());
                            }
                        }
                    } else {
                        sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }
                } else {
                    if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                        sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, tf, destination_dir);
                    }
                }
            } else { // file Delete
                if (!SyncThread.isHiddenFile(stwa, sti, tf)) {
                    boolean isFileSelected=SyncThread.isFileSelected(stwa, sti, relative_dir);
                    if (isFileSelected) {
                        if (!mf_exists) {
                            sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    } else {
                        if ((sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter())){
                            sync_result= deleteSmbItemForSyncDelete(stwa, sti, CONFIRM_REQUEST_DELETE_FILE, tf, destination_dir);
                        }
                    }
                }
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti, e, source_dir, destination_dir);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sync_result;
    }

    static private int deleteLocalItemForSyncDelete(SyncThreadWorkArea stwa, SyncTaskItem sti, String confirm_id, SafFile3 tf, String destination_dir) {
        int sync_result=0;
        String msg="";
        if (SyncThread.sendConfirmRequest(stwa, sti, confirm_id, "", destination_dir)) {
            sync_result=deleteLocalItem(stwa, sti, tf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalDeleteCount++;
                if (confirm_id.equals(CONFIRM_REQUEST_DELETE_DIR)) msg=stwa.appContext.getString(R.string.msgs_mirror_task_dir_deleted);
                else msg=stwa.appContext.getString(R.string.msgs_mirror_task_file_deleted);
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(), "", msg);
            } else {
                if (confirm_id.equals(CONFIRM_REQUEST_DELETE_DIR)) msg=stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed);
                else msg=stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed);
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(), "", msg);
            }
        } else {
            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(),
                    "", stwa.appContext.getString(R.string.msgs_mirror_confirm_delete_cancel));
        }
        return sync_result;
    }

    static private int deleteSmbItemForSyncDelete(SyncThreadWorkArea stwa, SyncTaskItem sti, String confirm_id, JcifsFile tf, String destination_dir) {
        int sync_result=0;
        String msg="";
        if (SyncThread.sendConfirmRequest(stwa, sti, confirm_id, "", destination_dir)) {
            sync_result=deleteSmbItem(stwa, sti, tf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalDeleteCount++;
                if (confirm_id.equals(CONFIRM_REQUEST_DELETE_DIR)) msg=stwa.appContext.getString(R.string.msgs_mirror_task_dir_deleted);
                else msg=stwa.appContext.getString(R.string.msgs_mirror_task_file_deleted);
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(), "", msg);
            } else {
                if (confirm_id.equals(CONFIRM_REQUEST_DELETE_DIR)) msg=stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed);
                else msg=stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed);
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(), "", msg);
            }
        } else {
            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", destination_dir, tf.getName(),
                    "", stwa.appContext.getString(R.string.msgs_mirror_confirm_delete_cancel));
        }
        return sync_result;
    }

    private static boolean isSmbFileExists(SyncThreadWorkArea stwa, ArrayList<String> smb_fl, String fp) throws IOException, JcifsException {
        boolean mf_exists = (Collections.binarySearch(stwa.smbFileList, fp) >= 0);
        if (!mf_exists) {
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " file list not found, fp=" + fp);
            JcifsFile mf = new JcifsFile(fp, stwa.sourceSmbAuth);
            mf_exists = mf.exists();
        }
        return mf_exists;
    }

    static private String convertToExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, InputStream is,
                                                long file_last_modified, String from_path, String to_path) {
        String parsed_to_path=to_path;
        String[] taken_date=null;
        long taken_millis=0L;
        if (isMovieFile(from_path) || isPictureFile(from_path)) {
            if (isPictureFile(from_path)) taken_date=SyncThreadArchiveFile.getExifDateTime(stwa, is);
            else taken_date=SyncThreadArchiveFile.getMp4ExifDateTime(stwa, is);
            try {is.close();} catch(Exception e){};
            if (taken_date!=null && taken_date.length==2 && taken_date[0]!=null && taken_date[1]!=null) {
                SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
                Date date = null;
                try {
                    date = sdFormat.parse(taken_date[0]+" "+taken_date[1]);
                    taken_millis=date.getTime();
                } catch (ParseException e) {
                    taken_millis=file_last_modified;
                    stwa.util.addLogMsg("W",sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_taken_date_convert_error)+from_path);
                    stwa.util.addLogMsg("W",sti.getSyncTaskName(), e.getMessage());
                }
            } else {
//                    stwa.util.addDebugMsg(1,"W","convertToExifDateTime EXIF date not available.");
                stwa.util.addLogMsg("W",sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_taken_date_can_not_obtain_from_the_file)+from_path);
                taken_millis=file_last_modified;
            }
        } else {
            taken_millis=file_last_modified;
        }
        parsed_to_path=SyncThread.replaceKeywordTakenDateValue(to_path, taken_millis);
        return parsed_to_path;
    }

    static private boolean isMovieFile(String fp) {
        boolean result=false;
        if (fp.toLowerCase().endsWith(".mp4") ||
                fp.toLowerCase().endsWith(".mov")) result=true;
        return result;
    }
    static private boolean isPictureFile(String fp) {
        boolean result=false;
        if (fp.toLowerCase().endsWith(".gif") ||
                fp.toLowerCase().endsWith(".jpg") ||
                fp.toLowerCase().endsWith(".jpeg") ||
                fp.toLowerCase().endsWith(".jpe") ||
                fp.toLowerCase().endsWith(".png")) result=true;
        return result;
    }

    static public int syncMirrorLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        String to_path_converted=SyncThread.replaceKeywordExecutionDateValue(to_path, stwa.syncBeginTime);
        boolean isTakenDateUsed=false;//SyncThread.isTakenDateConvertRequired(to_path_converted);
        SafFile3 mf = new SafFile3(stwa.appContext, from_path);
        int sync_result =0;
        stwa.lastWriteSafFile=null;

        SafFile3 tf = new SafFile3(stwa.appContext, to_path_converted);
        ContentProviderClient cpc=tf.isSafFile()?tf.getContentProviderClient():null;
        try {
            if (sti.isSyncOptionDeleteFirstWhenMirror()) {
                sync_result = syncDeleteLocalToLocal(stwa, sti, from_path, from_path, to_path_converted, to_path_converted, tf, cpc, isTakenDateUsed);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = moveCopyLocalToLocal(stwa, sti, false, from_path, from_path, mf, to_path_converted, to_path_converted, cpc, isTakenDateUsed);
                }
            } else {
                sync_result = moveCopyLocalToLocal(stwa, sti, false, from_path, from_path, mf, to_path_converted, to_path_converted, cpc, isTakenDateUsed);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = syncDeleteLocalToLocal(stwa, sti, from_path, from_path, to_path_converted, to_path_converted, tf, cpc, isTakenDateUsed);
                }
            }
        } finally {
            if (cpc!=null) cpc.release();
        }

        return sync_result;
    }


    static public int syncCopyLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        String to_path_converted=SyncThread.replaceKeywordExecutionDateValue(to_path, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path_converted);
        stwa.lastWriteSafFile=null;

        SafFile3 mf = new SafFile3(stwa.appContext, from_path);
        ContentProviderClient cpc=null;
        int sync_result=0;
        try {
            cpc=mf.getContentProviderClient();
            sync_result= moveCopyLocalToLocal(stwa, sti, false, from_path, from_path, mf, to_path_converted, to_path_converted, cpc, isTakenDateUsed);
        } finally {
            if (cpc!=null) cpc.release();
        }
        return sync_result;
    }

    static public int syncMoveLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        String to_path_converted=SyncThread.replaceKeywordExecutionDateValue(to_path, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path_converted);
        stwa.lastWriteSafFile=null;

        SafFile3 mf = new SafFile3(stwa.appContext,  from_path);
        ContentProviderClient cpc=null;
        int sync_result=0;
        try {
            cpc=mf.getContentProviderClient();
            sync_result= moveCopyLocalToLocal(stwa, sti, true, from_path, from_path, mf, to_path_converted, to_path_converted, cpc, isTakenDateUsed);
        } finally {
            if (cpc!=null) cpc.release();
        }
        return sync_result;
    }

    static private int moveCopyLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file,
                                            String from_base, String from_path, SafFile3 mf, String to_base, String to_path, ContentProviderClient cpc, boolean isTakenDateUsed) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, from=" + from_path + ", to=" + to_path + ", move=" + move_file);
        int sync_result = 0;
        if (!sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && !SyncThread.isValidFileDirectoryName(stwa, sti, from_path)) {
            if (sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) return sync_result;
            else return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        try {
            if (isSafExists(mf, cpc)) {
                String relative_from_path = from_path.substring(from_base.length());
                if (relative_from_path.startsWith("/")) relative_from_path = relative_from_path.substring(1);
                if (isSafDirectory(mf, cpc)) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, relative_from_path)) {
                        if (!mf.canRead()) {
                            String msg="";
                            if (mf.isDirectory()) msg=stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath());
                            else msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_because_access_not_granted, mf.getPath());
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(), "", msg);
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        SafFile3[] children = listSafFiles(mf, cpc);
                        if (children != null) {
                            for (SafFile3 element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    if (!element.getName().equals(".android_secure")) {
                                        if (!isSafDirectory(element, cpc)) {
                                            sync_result = moveCopyLocalToLocal(stwa, sti, move_file, from_base, from_path+"/"+element.getName(),
                                                    element, to_base, to_path+"/"+element.getName(), cpc, isTakenDateUsed);
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = moveCopyLocalToLocal(stwa, sti, move_file, from_base, from_path+"/"+element.getName(),
                                                        element, to_base, to_path+"/"+element.getName(), cpc, isTakenDateUsed);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, from_path));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                    }
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                            if (sti.isSyncOptionSyncEmptyDirectory()) {
                                if (!isTakenDateUsed && SyncThread.isDirectoryIncluded(stwa, relative_from_path)) {
                                    SyncThread.createDirectoryToLocalStorage(stwa, sti, to_path);
                                }
                            }

                            if (sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() && move_file) {
                                String[] fl=mf.list();
                                if (fl==null) {
                                    stwa.gp.syncThreadCtrl.setThreadMessage("Sync aborted because SafFile3#list() returned null");
                                    SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                                } else {
                                    if (fl.length==0) {
                                        if (!from_base.equals(mf.getPath())) {
                                            deleteLocalDirectoryForMove(stwa, sti, move_file, mf);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                } else { // file copy
                    long mf_length=mf.length();
                    long mf_last_modified=mf.lastModified();
                    if (!SyncThread.isHiddenFile(stwa, sti, mf) && SyncThread.isFileSelected(stwa, sti, relative_from_path, from_path, mf_length, mf_last_modified)) {
                        String parsed_to_path=to_path;
                        if (isTakenDateUsed)
                            parsed_to_path=convertToExifDateTime(stwa, sti, mf.getInputStream(), mf_last_modified, from_path, to_path);
                        if (from_path.equals(parsed_to_path)) {
                            stwa.util.addLogMsg("W",stwa.appContext.getString(R.string.msgs_mirror_same_file_ignored,from_path));
                            stwa.totalIgnoreCount++;
                        } else {
                            SafFile3 tf = new SafFile3(stwa.appContext, parsed_to_path);
                            if (isTakenDateUsed) SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParent());
                            int sr=SyncThread.checkFileNameLength(stwa, sti, tf.getName());
                            if (sr==SyncTaskItem.SYNC_RESULT_STATUS_ERROR) return sr;
                            if (sr==SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
                                stwa.totalIgnoreCount++;
                                return sync_result;
                            }
                            if (sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() &&
                                    !sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && tf.length()>FAT32_MAX_FILE_SIZE) {
                                String e_msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_file_size_gt_4gb, tf.getPath());
                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), e_msg);
                                stwa.totalIgnoreCount++;
                                return sync_result;
                            }
                            boolean tf_exists = tf.exists();
                            String conf_type="";
                            if (move_file) conf_type=CONFIRM_REQUEST_MOVE;
                            else conf_type=CONFIRM_REQUEST_COPY;
                            if (SyncThread.isFileChanged(stwa, sti, parsed_to_path, tf, mf, stwa.ALL_COPY) ){
                                if (tf_exists) {
                                    if (sti.isSyncOverrideCopyMoveFile()) {
                                        if (SyncThread.checkSourceFileNewerThanDestinationFile(stwa, sti, parsed_to_path, mf.lastModified(), tf.lastModified())) {
                                            if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                                sync_result= moveCopyLocalToLocalFile(stwa, sti,move_file, mf, tf, tf_exists) ;
                                            } else {
                                                if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                                else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                            }
                                        }
                                    } else {
                                        stwa.totalIgnoreCount++;
                                        if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_move_file));
                                        else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_copy_file));
                                    }
                                } else {
                                    sync_result= moveCopyLocalToLocalFile(stwa, sti,move_file, mf, tf, tf_exists) ;
                                }
                            } else {
                                if (move_file) {
                                    if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                        sync_result=deleteLocalItem(stwa, sti, mf);
                                        if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                            stwa.totalMoveCount++;
                                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                    "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                                        } else {
                                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                    "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete));
                                        }
                                    } else {
                                        if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                        else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                    }
                                }
                            }
                            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (Exception e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    private static int moveCopyLocalToLocalFile(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file, SafFile3 mf, SafFile3 tf, boolean tf_exists) {
        int sync_result = 0;
        if (!sti.isSyncTestMode()) {
            sync_result = SyncThreadCopyFile.copyFileLocalToLocal(stwa, sti, mf, tf);
            if (stwa.lastModifiedIsFunctional) {
                SyncThread.deleteLocalFileLastModifiedEntry(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath());
            } else {
                SyncThread.updateLocalFileLastModifiedList(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath(), tf.lastModified(), mf.lastModified());
            }
            SyncThread.scanMediaFile(stwa, sti, tf.getPath());
            stwa.lastWriteSafFile=tf;
        }
        if (move_file) {
            sync_result=deleteLocalItem(stwa, sti, mf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalMoveCount++;
                if (tf_exists) stwa.totalReplaceCount++;
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete));
                deleteLocalItem(stwa, sti, tf);
            }
        } else {
            stwa.totalCopyCount++;
            if (tf_exists) {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced));
                stwa.totalReplaceCount++;
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_copied));
            }
        }
        return sync_result;
    }

    private static int moveCopyLocalToSmbFile(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file, SafFile3 mf, JcifsFile tf, boolean tf_exists) {
        int sync_result = 0;
        if (!sti.isSyncTestMode()) {
            while (stwa.retryCount > 0) {
                sync_result = SyncThreadCopyFile.copyFileLocalToSmb(stwa, sti, mf, tf);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR &&
                        SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                    stwa.retryCount--;
                    if (stwa.retryCount > 0)
                        sync_result = waitRetryInterval(stwa);
                    if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                        break;
                } else {
                    break;
                }
            }
            if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
            if (stwa.lastModifiedIsFunctional) {
                SyncThread.deleteLocalFileLastModifiedEntry(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath());
            } else {
                long tf_lmod=System.currentTimeMillis();
                try {
                    tf_lmod=tf.getLastModified();
                } catch(JcifsException e) {
                    stwa.util.addDebugMsg(1,"W", "Target file last modified time can not obtained, e="+e.getMessage()+", NTSTATUS="+e.getNtStatus());
                }
                SyncThread.updateLocalFileLastModifiedList(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath(), tf_lmod, mf.lastModified());
            }
            SyncThread.scanMediaFile(stwa, sti, tf.getPath());
        }
        if (move_file) {
            sync_result= deleteLocalItem(stwa, sti, mf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalMoveCount++;
                if (tf_exists) stwa.totalReplaceCount++;
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete, mf.getPath()));
                try {
                    if (tf.exists()) tf.delete();
                } catch(JcifsException e) {
                    stwa.util.addDebugMsg(1,"W", "Target file can not deleted, e="+e.getMessage()+", NTSTATUS="+e.getNtStatus());
                }
            }
        } else {
            stwa.totalCopyCount++;
            if (tf_exists) stwa.totalReplaceCount++;
            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                    "", stwa.appContext.getString(R.string.msgs_mirror_task_file_copied));
        }
        return sync_result;
    }

    private static int moveCopySmbToLocalFile(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                              boolean move_file, JcifsFile mf, SafFile3 tf, boolean tf_exists) throws JcifsException {
        int sync_result = 0;
        while (stwa.retryCount > 0) {
            sync_result = SyncThreadCopyFile.copyFileSmbToLocal(stwa, sti, mf, tf);
            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR &&
                    SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                stwa.retryCount--;
                if (stwa.retryCount > 0)
                    sync_result = waitRetryInterval(stwa);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                    break;
            } else {
                break;
            }
        }
        if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
            if (!sti.isSyncTestMode()) {
                if (stwa.lastModifiedIsFunctional) {
                    SyncThread.deleteLocalFileLastModifiedEntry(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath());
                } else {
                    SyncThread.updateLocalFileLastModifiedList(stwa, stwa.currLastModifiedList, stwa.newLastModifiedList, tf.getPath(), tf.lastModified(), mf.getLastModified());
                }
                SyncThread.scanMediaFile(stwa, sti, tf.getPath());
                stwa.lastWriteSafFile=tf;
            }
            if (move_file) {
                sync_result= deleteSmbItem(stwa, sti, mf);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    stwa.totalMoveCount++;
                    if (tf_exists) stwa.totalReplaceCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete, mf.getPath()));
                    tf.delete();
                }
            } else {
                stwa.totalCopyCount++;
                if (tf_exists) {
                    stwa.totalReplaceCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_copied));
                }
            }
        }

        return sync_result;
    }

    private static int moveCopySmbToSmbFile(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                            boolean move_file, JcifsFile mf, JcifsFile tf, boolean tf_exists) throws JcifsException {
        int sync_result = 0;
        while (stwa.retryCount > 0) {
            sync_result = SyncThreadCopyFile.copyFileSmbToSmb(stwa, sti, mf, tf);
            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                stwa.retryCount--;
                if (stwa.retryCount > 0)
                    sync_result = waitRetryInterval(stwa);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                    break;
            } else {
                break;
            }
        }
        if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
            if (move_file) {
                sync_result=deleteSmbItem(stwa, sti, mf);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    stwa.totalMoveCount++;
                    if (tf_exists) stwa.totalReplaceCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete));
                }
            } else {
                stwa.totalCopyCount++;
                if (tf_exists) {
                    stwa.totalReplaceCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", tf.getPath(), mf.getName(), "", stwa.appContext.getString(R.string.msgs_mirror_task_file_copied));
                }
            }
        }
        return sync_result;
    }

    static private int deleteLocalDirectoryForMove(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file, SafFile3 mf) {
        int sync_result=0;
        if (sti.isSyncConfirmOverrideOrDelete()) {
            if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, "", mf.getPath())) {
                sync_result=deleteLocalItem(stwa, sti, mf);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    stwa.totalMoveCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                            stwa.appContext.getString(R.string.msgs_mirror_task_dir_moved));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed));
                }
            }
        } else {
            sync_result=deleteLocalItem(stwa, sti, mf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalMoveCount++;
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_dir_moved));
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed));
            }
        }
        return sync_result;
    }

    static private int deleteSmbDirectoryForMove(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file, JcifsFile mf) {
        int sync_result=0;
        if (sti.isSyncConfirmOverrideOrDelete()) {
            if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_DELETE_DIR, "", mf.getPath())) {
                sync_result=deleteSmbItem(stwa, sti, mf);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    stwa.totalMoveCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                            " "+stwa.appContext.getString(R.string.msgs_mirror_task_dir_moved));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed));
                }
            }
        } else {
            sync_result=deleteSmbItem(stwa, sti, mf);
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalMoveCount++;
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                        " "+stwa.appContext.getString(R.string.msgs_mirror_task_dir_moved));
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed));
            }
        }
        return sync_result;
    }

    static public int syncMirrorLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime)+"/";
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);
        SafFile3 mf = new SafFile3(stwa.appContext,  from_path);
        int sync_result =0;
        JcifsFile tf = null;
        try {
            tf = new JcifsFile(to_path, stwa.destinationSmbAuth);
            if (sti.isSyncOptionDeleteFirstWhenMirror()) {
                sync_result = syncDeleteLocalToSmb(stwa, sti, from_path, from_path, to_path, to_path, tf);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = moveCopyLocalToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, isTakenDateUsed);
                }
            } else {
                sync_result = moveCopyLocalToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, isTakenDateUsed);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = syncDeleteLocalToSmb(stwa, sti, from_path, from_path, to_path, to_path, tf);
                }
            }
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sync_result;
    }

    static public int syncCopyLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime)+"/";
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        SafFile3 mf = new SafFile3(stwa.appContext,  from_path);
        return moveCopyLocalToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, isTakenDateUsed);
    }

    static public int syncMoveLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime)+"/";
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);
        SafFile3 mf = new SafFile3(stwa.appContext,  from_path);
        return moveCopyLocalToSmb(stwa, sti, true, from_path, from_path, mf, to_path, to_path, isTakenDateUsed);
    }

    static private int moveCopyLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file,
                                          String from_base, String from_path, SafFile3 mf, String to_base, String to_path, boolean isTakenDateUsed) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, from=" + from_path + ", to=" + to_path + ", move=" + move_file);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;

        if (!SyncThread.isValidFileDirectoryName(stwa, sti, from_path)) {
            if (sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) return sync_result;
            else return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        try {
            if (mf.exists()) {
                String relative_from_path = from_path.substring(from_base.length());
                if (relative_from_path.startsWith("/")) relative_from_path = relative_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, relative_from_path)) {
                        if (!mf.canRead()) {
                            String msg="";
                            if (mf.isDirectory()) msg=stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath());
                            else msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_because_access_not_granted, mf.getPath());
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(), "", msg);
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        SafFile3[] children = mf.listFiles();
                        if (children != null) {
                            for (SafFile3 element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    if (!element.getName().equals(".android_secure")) {
                                        if (element.isFile()) {
                                            sync_result = moveCopyLocalToSmb(stwa, sti, move_file, from_base,
                                                    from_path+"/"+element.getName(), element, to_base, to_path+element.getName(), isTakenDateUsed);
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = moveCopyLocalToSmb(stwa, sti, move_file, from_base, from_path+"/"+element.getName(),
                                                        element, to_base, to_path +element.getName()+"/", isTakenDateUsed);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, from_path));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                        if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                    }
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                            if (sti.isSyncOptionSyncEmptyDirectory()) {
                                if (!isTakenDateUsed && SyncThread.isDirectoryIncluded(stwa, relative_from_path)) {
                                    SyncThread.createDirectoryToSmb(stwa, sti, to_path, stwa.destinationSmbAuth);
                                }
                            }
                            if (sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() && move_file) {
                                String[] fl=mf.list();
                                if (fl==null) {
                                    stwa.gp.syncThreadCtrl.setThreadMessage("Sync aborted because SafFile3#list() returned null");
                                    SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                                } else {
                                    if (fl.length==0) {
                                        if (!from_base.equals(mf.getPath())) {
                                            deleteLocalDirectoryForMove(stwa, sti, move_file, mf);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                } else { // file copy
                    long mf_length=mf.length();
                    long mf_last_modified=mf.lastModified();
                    if (!SyncThread.isHiddenFile(stwa, sti, mf) && SyncThread.isFileSelected(stwa, sti, relative_from_path, from_path, mf_length, mf_last_modified)) {
                        String parsed_to_path=to_path;
                        if (isTakenDateUsed)
                            parsed_to_path=convertToExifDateTime(stwa, sti, mf.getInputStream(), mf_last_modified, from_path, to_path);
                        JcifsFile tf = new JcifsFile(parsed_to_path, stwa.destinationSmbAuth);
                        if (isTakenDateUsed){
                            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParent());
                        }
                        boolean tf_exists = tf.exists();
                        String conf_type="";
                        if (move_file) conf_type=CONFIRM_REQUEST_MOVE;
                        else conf_type=CONFIRM_REQUEST_COPY;
                        if (SyncThread.isFileChangedForLocalToRemote(stwa, sti, from_path, mf, tf, stwa.ALL_COPY)){
                            if (tf_exists) {
                                if (sti.isSyncOverrideCopyMoveFile()) {
                                    if (SyncThread.checkSourceFileNewerThanDestinationFile(stwa, sti, parsed_to_path, mf_last_modified, tf.getLastModified())) {
                                        if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                            sync_result= moveCopyLocalToSmbFile(stwa, sti,move_file, mf, tf, tf_exists) ;
                                        } else {
                                            if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                            else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                        }
                                    }
                                } else {
                                    stwa.totalIgnoreCount++;
                                    if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_move_file));
                                    else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_copy_file));
                                }
                            } else {
                                sync_result= moveCopyLocalToSmbFile(stwa, sti,move_file, mf, tf, tf_exists) ;
                            }
                        } else {
                            if (move_file) {
                                if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                    sync_result= deleteLocalItem(stwa, sti, mf);
                                    if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                        stwa.totalMoveCount++;
                                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                                    } else {
                                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                "", stwa.appContext.getString(R.string.msgs_mirror_task_file_move_failed_delete, mf.getPath()));
                                        SafFile3 sf =new SafFile3(stwa.appContext, parsed_to_path);
                                    }
                                } else {
                                    if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                    else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                }
                            }
                        }
                        if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int syncMirrorSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        stwa.smbFileList = new ArrayList<String>();
        int sync_result =0;
        stwa.lastWriteSafFile=null;
        JcifsFile mf = null;
        ContentProviderClient cpc=null;
        try {
            SafFile3 tf = new SafFile3(stwa.appContext, to_path);
            cpc=(tf.isSafFile()?tf.getContentProviderClient():null);
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            if (sti.isSyncOptionDeleteFirstWhenMirror()) {
                sync_result =syncDeleteSmbToLocal(stwa, sti, from_path, from_path, to_path, to_path, tf, stwa.smbFileList, cpc);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = moveCopySmbToLocal(stwa, sti, false, from_path, from_path, mf, to_path, to_path, stwa.smbFileList, isTakenDateUsed);
                }
            } else {
                sync_result = moveCopySmbToLocal(stwa, sti, false, from_path, from_path, mf, to_path, to_path, stwa.smbFileList, isTakenDateUsed);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    Collections.sort(stwa.smbFileList);
                    sync_result = syncDeleteSmbToLocal(stwa, sti, from_path, from_path, to_path, to_path, tf, stwa.smbFileList, cpc);
                }
            }
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } finally {
            if (cpc!=null) cpc.release();
        }
        stwa.smbFileList = null;
        return sync_result;
    }

    static public int syncCopySmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        JcifsFile mf = null;
        stwa.lastWriteSafFile=null;
        int sync_result=0;
        try {
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            sync_result= moveCopySmbToLocal(stwa, sti, false, from_path, from_path, mf, to_path, to_path, null, isTakenDateUsed);
        } catch (MalformedURLException e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), e.getMessage());//e.toString());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), e.getMessage());//e.toString());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int syncMoveSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        JcifsFile mf = null;
        stwa.lastWriteSafFile=null;
        int sync_result=0;
        try {
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            sync_result= moveCopySmbToLocal(stwa, sti, true, from_path, from_path, mf, to_path, to_path, null, isTakenDateUsed);
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static private int moveCopySmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file,
                                          String from_base, String from_path, JcifsFile mf, String to_base, String to_path, ArrayList<String> smb_fl, boolean isTakenDateUsed) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, from=" + from_path + ", to=" + to_path + ", move=" + move_file);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        try {
            if (mf.exists()) {
                String relative_from_path = from_path.substring(from_base.length());
                if (relative_from_path.startsWith("/")) relative_from_path = relative_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, relative_from_path)) {
                        if (!mf.canRead()) {
                            String msg="";
                            if (mf.isDirectory()) msg=stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath());
                            else msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_because_access_not_granted, mf.getPath());
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(), "", msg);
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        JcifsFile[] children = mf.listFiles();
                        if (children != null) {
                            for (JcifsFile element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    while (stwa.retryCount > 0) {
                                        if (element.isFile()) {
                                            sync_result = moveCopySmbToLocal(stwa, sti, move_file, from_base,
                                                    from_path + element.getName(), element, to_base, to_path + "/" + element.getName(), smb_fl, isTakenDateUsed);
                                            if (smb_fl != null && sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                                                smb_fl.add(element.getPath());
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = moveCopySmbToLocal(stwa, sti, move_file, from_base, from_path + element.getName(),
                                                        element, to_base, to_path + "/" + element.getName().replace("/", ""), smb_fl, isTakenDateUsed);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, from_path));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                                            stwa.retryCount--;
                                            if (stwa.retryCount > 0)
                                                sync_result = waitRetryInterval(stwa);
                                            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                                                break;
                                        } else {
                                            break;
                                        }
                                    }
                                    if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
                                    if (smb_fl != null && sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) smb_fl.add(element.getPath());
                                    element.close();
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                            if (sti.isSyncOptionSyncEmptyDirectory()) {
                                if (!isTakenDateUsed && SyncThread.isDirectoryIncluded(stwa, relative_from_path)) {
                                    SyncThread.createDirectoryToLocalStorage(stwa, sti, to_path);
                                }
                            }
                            if (sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() && move_file) {
                                String[] fl=mf.list();
                                if (fl==null) {
                                    stwa.gp.syncThreadCtrl.setThreadMessage("Sync aborted because SafFile3#list() returned null");
                                    SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                                } else {
                                    if (fl.length==0) {
                                        if (!from_base.equals(mf.getPath())) {
                                            deleteSmbDirectoryForMove(stwa, sti, move_file, mf);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                } else { // file copy
                    long mf_length=mf.length();
                    long mf_last_modified=mf.getLastModified();
                    if (!SyncThread.isHiddenFile(stwa, sti, mf) && SyncThread.isFileSelected(stwa, sti, relative_from_path, from_path, mf_length, mf_last_modified)) {
                        String parsed_to_path=to_path;
                        if (isTakenDateUsed)
                            parsed_to_path=convertToExifDateTime(stwa, sti, mf.getInputStream(), mf_last_modified, from_path, to_path);
                        SafFile3 tf = new SafFile3(stwa.appContext, parsed_to_path);
                        if (isTakenDateUsed){
                            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParent());
                        }
                        int sr=SyncThread.checkFileNameLength(stwa, sti, tf.getName());
                        if (sr==SyncTaskItem.SYNC_RESULT_STATUS_ERROR) return sr;
                        if (sr==SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }

                        if (sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() &&
                                !sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && tf.length()>FAT32_MAX_FILE_SIZE) {
                            String e_msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_file_size_gt_4gb, tf.getPath());
                            stwa.util.addLogMsg("W", sti.getSyncTaskName(), e_msg);
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        boolean tf_exists = tf.exists();
                        String conf_type="";
                        if (move_file) conf_type=CONFIRM_REQUEST_MOVE;
                        else conf_type=CONFIRM_REQUEST_COPY;
                        if (SyncThread.isFileChanged(stwa, sti, parsed_to_path, tf, mf, stwa.ALL_COPY)){
                            if (tf_exists) {
                                if (sti.isSyncOverrideCopyMoveFile()) {
                                    if (SyncThread.checkSourceFileNewerThanDestinationFile(stwa, sti, parsed_to_path, mf_last_modified, tf.lastModified())) {
                                        if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                            sync_result= moveCopySmbToLocalFile(stwa, sti, move_file, mf, tf, tf_exists);
                                        } else {
                                            if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                            else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                        }
                                    }
                                } else {
                                    stwa.totalIgnoreCount++;
                                    if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_move_file));
                                    else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_copy_file));
                                }
                            } else {
                                sync_result= moveCopySmbToLocalFile(stwa, sti, move_file, mf, tf, tf_exists);
                            }
                        } else {
                            if (move_file) {
                                if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                    sync_result=deleteSmbItem(stwa, sti, mf);
                                    if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                        stwa.totalMoveCount++;
                                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                                    } else {
                                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                                                "", stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed));
                                    }
                                } else {
                                    stwa.totalIgnoreCount++;
                                    stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                }
                            }
                        }
                        if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static private int deleteSmbItem(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile del_file) {
        int sync_result=0;
        if (sti.isSyncTestMode()) {
            return sync_result;
        }

        if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
            sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            return sync_result;
        }
        try {
            if (del_file.isDirectory()) {
                JcifsFile[] del_list=del_file.listFiles();
                if (del_list!=null) {
                    for(JcifsFile child_item:del_list) {
                        sync_result=deleteSmbItem(stwa, sti, child_item);
                        if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                            break;
                        }
                    }
                }
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    del_file.delete();
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
                }
            } else {
                del_file.delete();
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
            }
        } catch(JcifsException e) {
            e.printStackTrace();
            putErrorMessageJcifs(stwa, sti, e, del_file.getPath(), "");
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int deleteLocalItem(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 del_item) {
        int sync_result=0;
        if (sti.isSyncTestMode()) {
            return sync_result;
        }
        if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
            sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            return sync_result;
        }

        if (del_item.isDirectory()) {
            try {
                SafFile3[] file_list=del_item.listFiles();
                if (file_list!=null) {
                    for(SafFile3 child_item:file_list) {
                        sync_result=deleteLocalItem(stwa, sti, child_item);
                        if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                            return sync_result;
                        }
                    }
                }
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    boolean del_rc=del_item.delete();
                    SyncThread.scanMediaFile(stwa, sti, del_item.getPath());
                    if (!del_rc) {
                        stwa.util.addLogMsg("E",sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed)+" "+del_item.getPath());
                        sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                putErrorMessage(stwa, sti, e, del_item.getPath(), "");
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } else {
            boolean del_rc=false;
            try {
                del_rc=del_item.delete();
                if (del_rc) {
                    SyncThread.scanMediaFile(stwa, sti, del_item.getPath());
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
                }
                else {
                    stwa.util.addLogMsg("E",sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed, del_item.getPath()));
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            } catch(Exception e) {
                e.printStackTrace();
                putErrorMessage(stwa, sti, e, del_item.getPath(), "");
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        }
        return sync_result;
    }

    static public int syncMirrorSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        JcifsFile mf = null;
        JcifsFile tf = null;
        stwa.smbFileList = new ArrayList<String>();
        int sync_result =0;
        try {
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            tf = new JcifsFile(to_path, stwa.destinationSmbAuth);
            if (sti.isSyncOptionDeleteFirstWhenMirror()) {
                sync_result = syncDeleteSmbToSmb(stwa, sti, from_path, from_path, to_path, to_path, tf, stwa.smbFileList);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                    sync_result = moveCopySmbToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, stwa.smbFileList, isTakenDateUsed);
            } else {
                sync_result =sync_result = moveCopySmbToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, stwa.smbFileList, isTakenDateUsed);
                Collections.sort(stwa.smbFileList);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                    syncDeleteSmbToSmb(stwa, sti, from_path, from_path, to_path, to_path, tf, stwa.smbFileList);
            }
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        stwa.smbFileList = null;
        return sync_result;
    }

    static public int syncCopySmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        JcifsFile mf = null;
        int sr=0;
        try {
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            sr = moveCopySmbToSmb(stwa, sti, false, from_path, from_path, mf, to_path, to_path, null, isTakenDateUsed);
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sr;
    }

    static public int syncMoveSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path_before) {
        String to_path=SyncThread.replaceKeywordExecutionDateValue(to_path_before, stwa.syncBeginTime);
        boolean isTakenDateUsed=SyncThread.isTakenDateConvertRequired(to_path);

        JcifsFile mf = null;
        try {
            mf = new JcifsFile(from_path, stwa.sourceSmbAuth);
            return moveCopySmbToSmb(stwa, sti, true, from_path, from_path, mf, to_path, to_path, null, isTakenDateUsed);
        } catch (MalformedURLException e) {
            putErrorMessage(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
    }

    static private int moveCopySmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file,
                                        String from_base, String from_path, JcifsFile mf, String to_base, String to_path, ArrayList<String> smb_fl, boolean isTakenDateUsed) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, from=" + from_path + ", to=" + to_path + ", move=" + move_file);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        JcifsFile tf;
        try {
            if (mf.exists()) {
                String relative_from_path = from_path.substring(from_base.length());
                if (relative_from_path.startsWith("/")) relative_from_path = relative_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, relative_from_path)) {
                        if (!mf.canRead()) {
                            String msg="";
                            if (mf.isDirectory()) msg=stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath());
                            else msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_because_access_not_granted, mf.getPath());
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(), "", msg);
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        JcifsFile[] children = mf.listFiles();
                        if (children != null) {
                            for (JcifsFile element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    while (stwa.retryCount > 0) {
                                        if (element.isFile()) {
                                            sync_result = moveCopySmbToSmb(stwa, sti, move_file, from_base, from_path + element.getName(),
                                                    element, to_base, to_path + element.getName(), smb_fl, isTakenDateUsed);
                                            if (smb_fl != null && sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                                                stwa.smbFileList.add(element.getPath());
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = moveCopySmbToSmb(stwa, sti, move_file, from_base, from_path + element.getName(),
                                                        element, to_base, to_path + element.getName(), smb_fl, isTakenDateUsed);
                                            } else {
                                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, from_path));
                                                stwa.totalIgnoreCount++;
                                            }
                                        }
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_ERROR && SyncThread.isJcifsRetryRequiredError(stwa.jcifsNtStatusCode)) {
                                            stwa.retryCount--;
                                            if (stwa.retryCount > 0)
                                                sync_result = waitRetryInterval(stwa);
                                            if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_CANCEL)
                                                break;
                                        } else {
                                            break;
                                        }
                                    }
                                    if (sync_result!= SyncTaskItem.SYNC_RESULT_STATUS_ERROR) stwa.retryCount=sti.getSyncOptionRetryCount();
                                    if (smb_fl != null && sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                                        smb_fl.add(element.getPath());
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                            if (sti.isSyncOptionSyncEmptyDirectory()) {
                                if (!isTakenDateUsed && SyncThread.isDirectoryIncluded(stwa, relative_from_path)) {
                                    SyncThread.createDirectoryToSmb(stwa, sti, to_path, stwa.destinationSmbAuth);
                                }
                            }
                            if (sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() && move_file) {
                                String[] fl=mf.list();
                                if (fl==null) {
                                    stwa.gp.syncThreadCtrl.setThreadMessage("Sync aborted because SafFile3#list() returned null");
                                    SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                                } else {
                                    if (fl.length==0) {
                                        if (!from_base.equals(mf.getPath())) {
                                            deleteSmbDirectoryForMove(stwa, sti, move_file, mf);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                } else { // file copy
                    long mf_length=mf.length();
                    long mf_last_modified=mf.getLastModified();
                    if (!SyncThread.isHiddenFile(stwa, sti, mf) && SyncThread.isFileSelected(stwa, sti, relative_from_path, from_path, mf_length, mf_last_modified)) {
                        String parsed_to_path=to_path;
                        if (isTakenDateUsed)
                            parsed_to_path=convertToExifDateTime(stwa, sti, mf.getInputStream(), mf_last_modified, from_path, to_path);
                        if (from_path.equals(parsed_to_path)) {
                            stwa.util.addLogMsg("W",stwa.appContext.getString(R.string.msgs_mirror_same_file_ignored,from_path));
                            stwa.totalIgnoreCount++;
                        } else {
                            tf = new JcifsFile(parsed_to_path, stwa.destinationSmbAuth);
                            if (isTakenDateUsed) SyncThread.createDirectoryToSmb(stwa, sti, tf.getParent(), stwa.destinationSmbAuth);
                            boolean tf_exists = tf.exists();
                            if (!tf_exists || tf.isFile()) {
                                String conf_type=move_file?CONFIRM_REQUEST_MOVE:CONFIRM_REQUEST_COPY;
                                if (SyncThread.isFileChanged(stwa, sti, parsed_to_path, tf, mf, stwa.ALL_COPY)){
                                    if (tf_exists) {
                                        if (sti.isSyncOverrideCopyMoveFile()) {
                                            if (SyncThread.checkSourceFileNewerThanDestinationFile(stwa, sti, parsed_to_path, mf_last_modified, tf.getLastModified())){
                                                if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                                    sync_result= moveCopySmbToSmbFile(stwa, sti, move_file, mf, tf, tf_exists);
                                                } else {
                                                    if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                                    else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                                }
                                            }
                                        } else {
                                            stwa.totalIgnoreCount++;
                                            if (move_file) stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_move_file));
                                            else stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_ignore_override_copy_file));
                                        }
                                    } else {
                                        sync_result= moveCopySmbToSmbFile(stwa, sti, move_file, mf, tf, tf_exists);
                                    }
                                } else {
                                    if (move_file) {
                                        if (SyncThread.sendConfirmRequest(stwa, sti, conf_type, from_path, parsed_to_path)) {
                                            sync_result=deleteSmbItem(stwa, sti, mf);
                                            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                                stwa.totalMoveCount++;
                                                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", parsed_to_path, mf.getName(),
                                                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                                            } else {
                                                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", mf.getPath(), mf.getName(),
                                                        "", stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed));
                                            }
                                        } else {
                                            stwa.totalIgnoreCount++;
                                            stwa.util.addLogMsg("W", sti.getSyncTaskName(), parsed_to_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                        }
                                    }
                                }
                            } else {
                                stwa.util.addLogMsg("E", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_with_same_name_as_the_file_found)+parsed_to_path);
                                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                            }
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (JcifsException e) {
            putErrorMessageJcifs(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putErrorMessage(stwa, sti,e, from_path, to_path);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        return sync_result;
    }

    static private int waitRetryInterval(SyncThreadWorkArea stwa) {
        int result = 0;
        if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
            result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
        } else {
            synchronized (stwa.gp.syncThreadCtrl) {
                try {
                    stwa.gp.syncThreadCtrl.wait(1000 * SyncThread.SYNC_RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
        }
        return result;
    }

    static private void putErrorMessage(SyncThreadWorkArea stwa, SyncTaskItem sti, Exception e, String from_path, String to_path) {
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", e.getMessage());
        if (e.getCause()!=null) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", e.getCause().toString());

        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

    static private void putErrorMessageJcifs(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsException e, String from_path, String to_path) {
        String suggest_msg= TaskListUtils.getJcifsErrorSugestionMessage(stwa.appContext, MiscUtil.getStackTraceString(e));
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
        if (!suggest_msg.equals("")) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", suggest_msg);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", e.getMessage());
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", "NT Status="+ String.format("0x%8x",e.getNtStatus()));

        if (e.getCause()!=null) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getCause().toString());
        stwa.jcifsNtStatusCode=e.getNtStatus();
        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

}
