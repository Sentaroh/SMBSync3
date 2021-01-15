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

import android.media.ExifInterface;
import android.os.Build;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.mp4.Mp4Directory;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import static com.sentaroh.android.SMBSync3.Constants.*;
import com.sentaroh.android.SMBSync3.SyncThread.SyncThreadWorkArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncThreadArchiveFile {
    private static final Logger log= LoggerFactory.getLogger(SyncThreadArchiveFile.class);

    static private void putExceptionMsg(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path, Exception e) {
        String suggest_msg= TaskListUtils.getJcifsErrorSugestionMessage(stwa.appContext, MiscUtil.getStackTraceString(e));
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", e.getMessage());
        if (!suggest_msg.equals("")) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", suggest_msg);
        if (e.getCause()!=null) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", e.getCause().toString());
        if (e instanceof JcifsException) stwa.jcifsNtStatusCode=((JcifsException)e).getNtStatus();
        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

    static private String createArchiveSmbNewFilePath(SyncThreadWorkArea stwa, SyncTaskItem sti, String path, String fn, String fe)
            throws MalformedURLException, JcifsException {
        String result="";

        for (int i=1;i<100000;i++) {
            String suffix_base= String.format("_%d", i);
            JcifsFile jf=new JcifsFile(fn+suffix_base+fe, stwa.destinationAuth);
            if (!jf.exists()) {
                result=fn+suffix_base+fe;
                break;
            }
        }

        return result;
    }

    static private String createArchiveLocalNewFilePath(SyncThreadWorkArea stwa, SyncTaskItem sti, String path, String fn, String fe) {
        String result="";

        for (int i=1;i<100000;i++) {
            String suffix_base= String.format("_%d", i);
            SafFile3 jf=new SafFile3(stwa.appContext, path+"/"+fn+suffix_base+fe);
            if (!jf.exists()) {
                result=fn+suffix_base+fe;
                break;
            }
        }

        return result;
    }

    static private int moveFileInternalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                             File mf, JcifsFile tf, String to_path) throws Exception, JcifsException {
        int sync_result=0;
        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
            if (!sti.isSyncTestMode()) {
                String dir=tf.getParent();
                JcifsFile jf_dir=new JcifsFile(dir,stwa.destinationAuth);
                if (!jf_dir.exists()) jf_dir.mkdirs();
                while (stwa.retryCount > 0) {
                    sync_result= copyFile(stwa, sti, new FileInputStream(mf), tf.getOutputStream(), from_path, to_path,
                            tf.getName(), sti.isSyncOptionUseSmallIoBuffer());
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
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    try {
                        tf.setLastModified(mf.lastModified());
                    } catch(JcifsException e) {
                        // nop
                    }
                    mf.delete();
                    stwa.totalDeleteCount++;
                    SyncThread.scanMediaFile(stwa, sti,  mf.getPath());
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }
        return sync_result;
    }

    static public int syncArchiveLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                    String from_path, String to_path) {
        SafFile3 mf = new SafFile3(stwa.appContext,  from_path);
        int sync_result = buildArchiveListLocalToLocal(stwa, sti, from_path, from_path, mf, to_path, to_path);
        return sync_result;
    }

    static private int moveFileLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                            SafFile3 mf, SafFile3 tf, String to_path, String file_name) throws Exception {
        int result=0;
        if (tf.getAppDirectoryCache()!=null) result= moveFileLocalToLocalSetLastMod(stwa, sti, from_path, mf, tf, to_path, file_name);
        else result= moveFileLocalToLocalUnsetLastMod(stwa, sti, from_path, mf, tf, to_path, file_name);
        return result;
    }

    static private int moveFileLocalToLocalSetLastMod(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                                      SafFile3 mf, SafFile3 tf, String to_path, String file_name) throws Exception {
        int sync_result=0;
        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
//            SafFile3 m_df = new SafFile3(stwa.appContext, from_path);
            if (!sti.isSyncTestMode()) {
                SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile());

                String temp_dir=tf.getAppDirectoryCache();
                File ld=new File(temp_dir);
                if (!ld.exists()) ld.mkdirs();
                String temp_path=temp_dir+"/"+tf.getName();//System.currentTimeMillis()+".tmp";

                File temp_file=new File(temp_path);
                OutputStream os=new FileOutputStream(temp_file);

                sync_result= copyFile(stwa, sti, mf.getInputStream(),
                        os, from_path, to_path, file_name, sti.isSyncOptionUseSmallIoBuffer());

                temp_file.setLastModified(mf.lastModified());

                SafFile3 temp_sf=new SafFile3(stwa.appContext, temp_path);
                temp_sf.moveTo(tf);

            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    mf.delete();
                    stwa.totalDeleteCount++;
                    SyncThread.scanMediaFile(stwa, sti, from_path);
                    SyncThread.scanMediaFile(stwa, sti, tf.getPath());
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }
        return sync_result;
    }

    static private int moveFileLocalToLocalUnsetLastMod(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                                        SafFile3 mf, SafFile3 tf, String to_path, String file_name) throws Exception {
        int sync_result=0;
        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
//            SafFile3 m_df = new SafFile3(stwa.appContext, from_path);
            if (!sti.isSyncTestMode()) {
                SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile());

                String temp_path=tf.getPath()+"."+System.currentTimeMillis();
                File temp_file=new File(temp_path);
                OutputStream os=new FileOutputStream(temp_file);

                sync_result= copyFile(stwa, sti, mf.getInputStream(),
                        os, from_path, to_path, file_name, sti.isSyncOptionUseSmallIoBuffer());

                temp_file.setLastModified(mf.lastModified());

                SafFile3 temp_sf=new SafFile3(stwa.appContext, temp_path);
                tf.deleteIfExists();
                temp_sf.renameTo(tf);
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    mf.delete();
                    stwa.totalDeleteCount++;
                    SyncThread.scanMediaFile(stwa, sti, from_path);
                    SyncThread.scanMediaFile(stwa, sti, tf.getPath());
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }
        return sync_result;
    }

    static private int archiveFileLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3[] children,
                                               String from_path, String to_path) throws Exception {
        int file_seq_no=0, sync_result=0;
        ArrayList<ArchiveFileListItem> fl= buildSafFileList(stwa, sti, children);
        for(ArchiveFileListItem item:fl) {
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                break;
            }
            if (!item.date_from_exif && sti.isSyncOptionConfirmNotExistsExifDate()) {
                if (!SyncThread.sendArchiveConfirmRequest(stwa, sti, CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE, item.full_path)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", item.full_path, item.file_name,
                            "", stwa.appContext.getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_cancel));
                    continue;
                }
            }
            if (sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() &&
                    !sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && item.file_size>FAT32_MAX_FILE_SIZE) {
                String e_msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_file_size_gt_4gb, item.full_path);
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), e_msg);
            } else {
                int sr=SyncThread.checkFileNameLength(stwa, sti, item.file_name);
                if (sr==SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
                    sync_result=sr;
                    break;
                } else if (sr==SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
                    stwa.totalIgnoreCount++;
                    break;
                } else if (sr==SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    String to_file_name="", to_file_ext="", to_file_seqno="";
                    file_seq_no++;
                    if (item.file_name.lastIndexOf(".")>=0) {
                        to_file_ext=item.file_name.substring(item.file_name.lastIndexOf("."));
                        to_file_name=item.file_name.substring(0,item.file_name.lastIndexOf("."));
                    } else {
                        to_file_name=item.file_name;
                    }
                    to_file_seqno=getFileSeqNumber(stwa, sti, file_seq_no);
                    to_file_name= convertFileNameWithDate(stwa, sti, item, to_file_name)+to_file_seqno;
                    String temp_dir= convertKeywordWithDate(stwa, sti, to_path, item);
                    SafFile3 tf=new SafFile3(stwa.appContext,  temp_dir+"/"+to_file_name+to_file_ext);

                    if (tf.exists()) {
                        String new_name=createArchiveLocalNewFilePath(stwa, sti, temp_dir, to_file_name, to_file_ext) ;
                        if (new_name.equals("")) {
                            stwa.util.addLogMsg("E",sti.getSyncTaskName(), "Archive sequence number overflow error.");
                            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                            break;
                        } else {
                            tf=new SafFile3(stwa.appContext, temp_dir+"/"+new_name);
                            sync_result= moveFileLocalToLocal(stwa, sti, item.full_path, (SafFile3)item.file, tf, tf.getPath(), new_name);
                        }
                    } else {
                        sync_result= moveFileLocalToLocal(stwa, sti, item.full_path, (SafFile3)item.file, tf, tf.getPath(), to_file_name+to_file_ext);
                    }
                }
            }
        }
        return sync_result;
    }

    static private int buildArchiveListLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                          String from_base, String from_path, SafFile3 mf, String to_base, String to_path) {
        stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, from=", from_path, ", to=" + to_path);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        File tf;

        if (!sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && !SyncThread.isValidFileDirectoryName(stwa, sti, from_path)) {
            if (sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) return sync_result;
            else return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        try {
            if (mf.exists()) {
                if (!mf.canRead()) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath()));
                    stwa.totalIgnoreCount++;
                    return sync_result;
                }
                String t_from_path = from_path.substring(from_base.length());
                if (t_from_path.startsWith("/")) t_from_path = t_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, t_from_path)) {
                        SafFile3[] children = mf.listFiles();
                        archiveFileLocalToLocal(stwa, sti, children, from_path, to_path);
                        if (children != null) {
                            for (SafFile3 element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    if (!from_path.equals(to_path)) {
                                        if (!element.getName().equals(".android_secure")) {
                                            if (element.isDirectory()) {
                                                if (sti.isSyncOptionSyncSubDirectory()) {
                                                    if (!sti.isDestinationArchiveIgnoreSourceDirectory()) {
                                                        sync_result = buildArchiveListLocalToLocal(stwa, sti, from_base, from_path + "/" + element.getName(),
                                                                element, to_base, to_path + "/" + element.getName());
                                                    } else {
                                                        sync_result = buildArchiveListLocalToLocal(stwa, sti, from_base, from_path + "/" + element.getName(),
                                                                element, to_base, to_path);
                                                    }
                                                } else {
                                                    stwa.util.addLogMsg("W", sti.getSyncTaskName(), stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_sync_sub_directory_disabled, from_path));
                                                    stwa.totalIgnoreCount++;
                                                }
                                            }
                                        }
                                    } else {
                                        stwa.util.addDebugMsg(1, "W", String.format(stwa.appContext.getString(R.string.msgs_mirror_same_directory_ignored), from_path + "/" + element.getName()));
                                    }
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                        } else {
                            stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (Exception e) {
            putExceptionMsg(stwa, sti, from_path, to_path, e);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int syncArchiveLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        SafFile3 mf = new SafFile3(stwa.appContext, from_path);
        int sync_result = buildArchiveListLocalToSmb(stwa, sti, from_path, from_path, mf, to_path, to_path);
        return sync_result;
    }

    static private int moveFileLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                          SafFile3 mf, JcifsFile tf, String to_path, String file_name) throws Exception, JcifsException {
        int sync_result=0;

        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
            if (!sti.isSyncTestMode()) {
                String dir=tf.getParent();
                JcifsFile jf_dir=new JcifsFile(dir,stwa.destinationAuth);
                if (!jf_dir.exists()) jf_dir.mkdirs();
                while (stwa.retryCount > 0) {
                    sync_result= copyFile(stwa, sti, mf.getInputStream(),
                            tf.getOutputStream(), from_path, to_path,
                            tf.getName(), sti.isSyncOptionUseSmallIoBuffer());
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
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    try {
                        tf.setLastModified(mf.lastModified());
                    } catch(JcifsException e) {
                        // nop
                    }
                    if (!sti.isSyncTestMode()) {
                        stwa.totalDeleteCount++;
                        mf.delete();
                        SyncThread.scanMediaFile(stwa, sti, from_path);
                    }
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }

        return sync_result;
    }

    static private int archiveFileLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3[] children,
                                             String from_path, String to_path) throws Exception, JcifsException {
        int file_seq_no=0, sync_result=0;
        ArrayList<ArchiveFileListItem> fl= buildSafFileList(stwa, sti, children);
        for(ArchiveFileListItem item:fl) {
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                break;
            }
            if (!item.date_from_exif && sti.isSyncOptionConfirmNotExistsExifDate()) {
                if (!SyncThread.sendArchiveConfirmRequest(stwa, sti, CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE, item.full_path)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", item.full_path, item.file_name,
                            "", stwa.appContext.getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_cancel));
                    continue;
                }
            }
            String to_file_name="", to_file_ext="", to_file_seqno="";
            file_seq_no++;
            if (item.file_name.lastIndexOf(".")>=0) {
                to_file_ext=item.file_name.substring(item.file_name.lastIndexOf("."));
                to_file_name=item.file_name.substring(0,item.file_name.lastIndexOf("."));
            } else {
                to_file_name=item.file_name;
            }
            to_file_seqno=getFileSeqNumber(stwa, sti, file_seq_no);
            to_file_name= convertFileNameWithDate(stwa, sti, item, to_file_name)+to_file_seqno;
            String temp_dir= convertKeywordWithDate(stwa, sti, to_path, item);
            JcifsFile tf=new JcifsFile(temp_dir+"/"+to_file_name+to_file_ext, stwa.destinationAuth);
            if (tf.exists()) {
                String new_name=createArchiveLocalNewFilePath(stwa, sti, to_path, to_path+"/"+temp_dir+"/"+to_file_name+to_file_seqno,to_file_ext) ;
                if (new_name.equals("")) {
                    stwa.util.addLogMsg("E",sti.getSyncTaskName(), "Archive sequence number overflow error.");
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    break;
                } else {
                    tf=new JcifsFile(new_name, stwa.destinationAuth);
                    sync_result= moveFileLocalToSmb(stwa, sti, item.full_path, (SafFile3)item.file, tf, tf.getPath(), new_name);
                }
            } else {
                sync_result= moveFileLocalToSmb(stwa, sti, item.full_path, (SafFile3)item.file, tf, tf.getPath(),
                        to_path+"/"+temp_dir+"/"+to_file_name+to_file_ext);
            }

        }
        return sync_result;
    }

    static private int buildArchiveListLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                     String from_base, String from_path, SafFile3 mf, String to_base, String to_path) {
        stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, from=", from_path, ", to=", to_path);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        JcifsFile tf;

        if (!SyncThread.isValidFileDirectoryName(stwa, sti, from_path)) {
            if (sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) return sync_result;
            else return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        try {
            if (mf.exists()) {
                if (!mf.canRead()) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath()));
                    stwa.totalIgnoreCount++;
                    return sync_result;
                }
                String t_from_path = from_path.substring(from_base.length());
                if (t_from_path.startsWith("/")) t_from_path = t_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, t_from_path)) {
                        SafFile3[] children = mf.listFiles();
                        if (children != null) {
                            archiveFileLocalToSmb(stwa, sti, children, from_path, to_path);
                            for (SafFile3 element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    if (!element.getName().equals(".android_secure")) {
                                        while (stwa.retryCount > 0) {
                                            if (element.isDirectory()) {
                                                if (sti.isSyncOptionSyncSubDirectory()) {
                                                    if (!sti.isDestinationArchiveIgnoreSourceDirectory()) {
                                                        sync_result = buildArchiveListLocalToSmb(stwa, sti, from_base, from_path + "/" + element.getName(),
                                                                element, to_base, to_path + "/" + element.getName());
                                                    } else {
                                                        sync_result = buildArchiveListLocalToSmb(stwa, sti, from_base, from_path + "/" + element.getName(),
                                                                element, to_base, to_path);
                                                    }
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
                                        if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS)
                                            break;
                                    }
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                        } else {
                            stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (JcifsException e) {
            putExceptionMsg(stwa, sti, from_path, to_path, e);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch (Exception e) {
            putExceptionMsg(stwa, sti, from_path, to_path, e);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int syncArchiveSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        stwa.smbFileList = new ArrayList<String>();
        JcifsFile mf = null;
        try {
            mf = new JcifsFile(from_path, stwa.sourceAuth);
        } catch (Exception e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), e.getMessage());//e.toString());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }
        int sync_result = buildArchiveListSmbToLocal(stwa, sti, from_path, from_path, mf, to_path, to_path);
        return sync_result;
    }

    static private int moveFileSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                          JcifsFile mf, SafFile3 tf, String to_path, String file_name) throws Exception, JcifsException {
        int result=0;
        if (tf.getAppDirectoryCache()!=null) result= moveFileSmbToLocalSetLastMod(stwa, sti, from_path, mf, tf, to_path, file_name);
        else result= moveFileSmbToLocalUnsetLastMod(stwa, sti, from_path, mf, tf, to_path, file_name);
        return result;
    }

    static private int moveFileSmbToLocalUnsetLastMod(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                                      JcifsFile mf, SafFile3 tf, String to_path, String file_name) throws Exception, JcifsException {
        int sync_result=0;

        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
            if (!sti.isSyncTestMode()) {
                SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile());

                String temp_path=tf.getPath()+".tmp";
                SafFile3 temp_saf=new SafFile3(stwa.appContext, temp_path);
                temp_saf.deleteIfExists();
                temp_saf.createNewFile();
                OutputStream os=temp_saf.getOutputStream();

                while ( stwa.retryCount> 0) {
                    sync_result= copyFile(stwa, sti, mf.getInputStream(), os, from_path, to_path, file_name, sti.isSyncOptionUseSmallIoBuffer());
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
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    tf.deleteIfExists();
                    temp_saf.renameTo(tf);
                }
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    mf.delete();
                    stwa.totalDeleteCount++;
                    SyncThread.scanMediaFile(stwa, sti, tf.getPath());
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }

        return sync_result;
    }

    static private int moveFileSmbToLocalSetLastMod(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                                    JcifsFile mf, SafFile3 tf, String to_path, String file_name) throws Exception, JcifsException {
        int sync_result=0;

        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
            if (!sti.isSyncTestMode()) {
                SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile());

                String temp_dir=tf.getAppDirectoryCache();
                File ld=new File(temp_dir);
                if (!ld.exists()) ld.mkdirs();
                String temp_path=temp_dir+"/"+tf.getName();//System.currentTimeMillis()+".tmp";
                File temp_file=new File(temp_path);
                SafFile3 temp_saf=new SafFile3(stwa.appContext, temp_path);
                OutputStream os=new FileOutputStream(temp_file);

                while ( stwa.retryCount> 0) {
                    sync_result= copyFile(stwa, sti, mf.getInputStream(), os, from_path, to_path, file_name, sti.isSyncOptionUseSmallIoBuffer());
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
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    temp_file.setLastModified(mf.getLastModified());
                    temp_saf.moveTo(tf);
                }
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    mf.delete();
                    stwa.totalDeleteCount++;
                    SyncThread.scanMediaFile(stwa, sti, tf.getPath());
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }

        return sync_result;
    }

    static private int archiveFileSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile[] children,
                                             String from_path, String to_path) throws Exception, JcifsException {
        int file_seq_no=0, sync_result=0;
        ArrayList<ArchiveFileListItem> fl=buildSmbFileList(stwa, sti, children);
        for(ArchiveFileListItem item:fl) {
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                break;
            }
            if (!item.date_from_exif && sti.isSyncOptionConfirmNotExistsExifDate()) {
                if (!SyncThread.sendArchiveConfirmRequest(stwa, sti, CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE, item.full_path)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", item.full_path, item.file_name,
                            "", stwa.appContext.getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_cancel));
                    continue;
                }
            }
            if (sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() &&
                    !sti.getDestinationStorageUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID) && item.file_size>FAT32_MAX_FILE_SIZE) {
                String e_msg=stwa.appContext.getString(R.string.msgs_mirror_file_ignored_file_size_gt_4gb, item.full_path);
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), e_msg);
            } else {
                int sr=SyncThread.checkFileNameLength(stwa, sti, item.file_name);
                if (sr==SyncTaskItem.SYNC_RESULT_STATUS_ERROR) {
                    sync_result=sr;
                    break;
                } else if (sr==SyncTaskItem.SYNC_RESULT_STATUS_WARNING) {
                    stwa.totalIgnoreCount++;
                    break;
                } else if (sr==SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    String to_file_name="", to_file_ext="", to_file_seqno="";
                    file_seq_no++;
                    if (item.file_name.lastIndexOf(".")>=0) {
                        to_file_ext=item.file_name.substring(item.file_name.lastIndexOf("."));
                        to_file_name=item.file_name.substring(0,item.file_name.lastIndexOf("."));
                    } else {
                        to_file_name=item.file_name;
                    }
                    to_file_seqno=getFileSeqNumber(stwa, sti, file_seq_no);
                    to_file_name= convertFileNameWithDate(stwa, sti, item, to_file_name)+to_file_seqno;
                    String temp_dir= convertKeywordWithDate(stwa, sti, to_path, item);
                    SafFile3 tf=new SafFile3(stwa.appContext,  temp_dir+"/"+to_file_name+to_file_ext);

                    if (tf.exists()) {
                        String new_name=createArchiveLocalNewFilePath(stwa, sti, to_path, temp_dir+"/"+to_file_name,to_file_ext) ;
                        if (new_name.equals("")) {
                            stwa.util.addLogMsg("E",sti.getSyncTaskName(), "Archive sequence number overflow error.");
                            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                            break;
                        } else {
                            tf=new SafFile3(stwa.appContext, new_name);
                            sync_result= moveFileSmbToLocal(stwa, sti, item.full_path, (JcifsFile)item.file, tf, tf.getPath(), new_name);
                        }
                    } else {
                        sync_result= moveFileSmbToLocal(stwa, sti, item.full_path, (JcifsFile)item.file, tf, tf.getPath(),
                                to_path+"/"+temp_dir+"/"+to_file_name+to_file_ext);
                    }
                }
            }
        }
        return sync_result;
    }

    static private int buildArchiveListSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                     String from_base, String from_path, JcifsFile mf, String to_base, String to_path) {
        stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, from=", from_path + ", to=", to_path);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        File tf;
        try {
            if (mf.exists()) {
                if (!mf.canRead()) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath()));
                    stwa.totalIgnoreCount++;
                    return sync_result;
                }
                String t_from_path = from_path.substring(from_base.length());
                if (t_from_path.startsWith("/")) t_from_path = t_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, t_from_path)) {
                        JcifsFile[] children = mf.listFiles();
                        if (children != null) {
                            archiveFileSmbToLocal(stwa, sti, children, from_path, to_path);
                            for (JcifsFile element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    while (stwa.retryCount > 0) {
                                        if (element.isDirectory()) {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                if (!sti.isDestinationArchiveIgnoreSourceDirectory()) {
                                                    sync_result = buildArchiveListSmbToLocal(stwa, sti, from_base, from_path + element.getName(),
                                                            element, to_base, to_path + "/" + element.getName().replace("/", ""));
                                                } else {
                                                    sync_result = buildArchiveListSmbToLocal(stwa, sti, from_base, from_path + element.getName(),
                                                            element, to_base, to_path);
                                                }
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
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                        } else {
                            stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (Exception e) {
            putExceptionMsg(stwa, sti, from_path, to_path, e);
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int syncArchiveSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                          String from_path, String to_path) {
        JcifsFile mf = null;
        try {
            mf = new JcifsFile(from_path, stwa.sourceAuth);
        } catch (MalformedURLException e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", Source file error");
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
        int sync_result = buildArchiveListSmbToSmb(stwa, sti, from_path, from_path, mf, to_path, to_path);
        return sync_result;
    }

    static private int moveFileSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path,
                                        JcifsFile mf, JcifsFile tf, String to_path, String file_name) throws Exception, JcifsException {
        int sync_result=0;

        if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, to_path)) {
            if (!sti.isSyncTestMode()) {
                String dir=tf.getParent();
                JcifsFile jf_dir=new JcifsFile(dir,stwa.destinationAuth);
                if (!jf_dir.exists()) jf_dir.mkdirs();
                while (stwa.retryCount > 0) {
                    sync_result= copyFile(stwa, sti, mf.getInputStream(), tf.getOutputStream(), from_path, to_path, file_name, sti.isSyncOptionUseSmallIoBuffer());
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
            }
            if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                stwa.totalCopyCount++;
                SyncThread.showArchiveMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, to_path, mf.getName(), tf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_archived));
                if (!sti.isSyncTestMode()) {
                    try {
                        tf.setLastModified(mf.getLastModified());
                    } catch(JcifsException e) {
                        // nop
                    }
                    mf.delete();
                    stwa.totalDeleteCount++;
                }
            }
        } else {
            stwa.util.addLogMsg("W", sti.getSyncTaskName(), to_path, " ", stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
        }

        return sync_result;
    }

    static private int archiveFileSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile[] children,
                                           String from_path, String to_path) throws Exception, JcifsException {
        int file_seq_no=0, sync_result=0;
        ArrayList<ArchiveFileListItem> fl=buildSmbFileList(stwa, sti, children);
        for(ArchiveFileListItem item:fl) {
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                break;
            }
            if (!item.date_from_exif && sti.isSyncOptionConfirmNotExistsExifDate()) {
                if (!SyncThread.sendArchiveConfirmRequest(stwa, sti, CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE, item.full_path)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", item.full_path, item.file_name,
                            "", stwa.appContext.getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_cancel));
                    continue;
                }
            }
            String to_file_name="", to_file_ext="", to_file_seqno="";
            file_seq_no++;
            if (item.file_name.lastIndexOf(".")>=0) {
                to_file_ext=item.file_name.substring(item.file_name.lastIndexOf("."));
                to_file_name=item.file_name.substring(0,item.file_name.lastIndexOf("."));
            } else {
                to_file_name=item.file_name;
            }
            to_file_seqno=getFileSeqNumber(stwa, sti, file_seq_no);
            to_file_name= convertFileNameWithDate(stwa, sti, item, to_file_name)+to_file_seqno;
            String temp_dir= convertKeywordWithDate(stwa, sti, to_path, item);
            JcifsFile tf=new JcifsFile(temp_dir+"/"+to_file_name+to_file_ext, stwa.destinationAuth);

            if (tf.exists()) {
                String new_name=createArchiveSmbNewFilePath(stwa, sti, to_path, to_path+"/"+temp_dir+"/"+to_file_name+to_file_seqno,to_file_ext) ;
                if (new_name.equals("")) {
                    stwa.util.addLogMsg("E",sti.getSyncTaskName(), "Archive sequence number overflow error.");
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                    break;
                } else {
                    tf=new JcifsFile(new_name, stwa.destinationAuth);
                    sync_result= moveFileSmbToSmb(stwa, sti, item.full_path, (JcifsFile)item.file, tf, tf.getPath(), new_name);
                }
            } else {
                sync_result= moveFileSmbToSmb(stwa, sti, item.full_path, (JcifsFile)item.file, tf, tf.getPath(),
                        to_path+"/"+temp_dir+"/"+to_file_name+to_file_ext);
            }
        }
        return sync_result;
    }

    static private int buildArchiveListSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                String from_base, String from_path, JcifsFile mf, String to_base, String to_path) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, from=", from_path, ", to=", to_path);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        JcifsFile tf;
        try {
            if (mf.exists()) {
                if (!mf.canRead()) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(),
                            "", stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath()));
                    stwa.totalIgnoreCount++;
                    return sync_result;
                }
                String t_from_path = from_path.substring(from_base.length());
                if (t_from_path.startsWith("/")) t_from_path = t_from_path.substring(1);
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, t_from_path)) {
                        JcifsFile[] children = mf.listFiles();
                        if (children != null) {
                            archiveFileSmbToSmb(stwa, sti, children, from_path, to_path);
                            for (JcifsFile element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    while (stwa.retryCount > 0) {
                                        if (element.isDirectory()) {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = buildArchiveListSmbToSmb(stwa, sti, from_base, from_path + element.getName(),
                                                        element, to_base, to_path + element.getName());
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
                                    if (sync_result != SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) break;
                                } else {
                                    return sync_result;
                                }
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                                    sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    break;
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (Exception e) {
            putExceptionMsg(stwa, sti, from_path, to_path, e);
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
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl))
                result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
        }
        return result;
    }

    final static int SHOW_PROGRESS_THRESHOLD_VALUE=512;

    static private int copyFile(SyncThreadWorkArea stwa, SyncTaskItem sti, InputStream ifs, OutputStream ofs, String from_path,
                                String to_path, String file_name, boolean small_buffer) throws Exception {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", "copyFile from=", from_path, ", to=", to_path);

        int io_area_size=0;
        if (small_buffer) {
            io_area_size=1024*16-1;
        } else {
            io_area_size=SYNC_IO_BUFFER_SIZE;
        }

        long read_begin_time = System.currentTimeMillis();
        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        int buffer_read_bytes = 0;
        long file_read_bytes = 0;
        long file_size = ifs.available();
        boolean show_prog = (file_size > SHOW_PROGRESS_THRESHOLD_VALUE);
        byte[] buffer = new byte[io_area_size];
        while ((buffer_read_bytes = ifs.read(buffer)) > 0) {
            ofs.write(buffer, 0, buffer_read_bytes);
            file_read_bytes += buffer_read_bytes;
            if (show_prog && file_size > file_read_bytes) {
                SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(), file_name + " " +
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_copying, (file_read_bytes * 100) / file_size));
            }
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                ifs.close();
                ofs.flush();
                ofs.close();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }
        }
        ifs.close();
        ofs.flush();
        ofs.close();

        long file_read_time = System.currentTimeMillis() - read_begin_time;

        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", to_path + " " + file_read_bytes + " bytes transfered in ",
                    file_read_time + " mili seconds at " +
                            SyncThread.calTransferRate(file_read_bytes, file_read_time));
        stwa.totalTransferByte += file_read_bytes;
        stwa.totalTransferTime += file_read_time;

        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }

    static private class ArchiveFileListItem {
        Object file=null;
        String shoot_date="", shoot_time="";
        String file_name="";
        String full_path="";
        boolean date_from_exif=true;
        long file_size=0L;
    }

    static private ArrayList<ArchiveFileListItem> buildSafFileList(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3[] children) {
        ArrayList<ArchiveFileListItem> fl=new ArrayList<ArchiveFileListItem>();
        for(SafFile3 element:children) {
            if (element.isFile() && isFileTypeArchiveDestination(element.getName())) {
                String[] date_time=getFileExifDateTime(stwa, sti, element);
                ArchiveFileListItem afli=new ArchiveFileListItem();
                afli.file=element;
                afli.file_name=element.getName();
                afli.full_path=element.getPath();
                afli.file_size=element.length();
                if (date_time==null || date_time[0]==null) {
                    String[] dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(element.lastModified()).split(" ");
                    afli.shoot_date=dt[0].replace("/","-");
                    afli.shoot_time=dt[1].replace(":","-");
                    afli.date_from_exif=false;
                } else {
                    afli.shoot_date=date_time[0].replace("/","-");
                    afli.shoot_time=date_time[1].replace(":","-");
                }
                if (isFileArchiveRequired(stwa, sti, afli)) fl.add(afli);
            }
        }
        Collections.sort(fl, new Comparator<ArchiveFileListItem>(){
            @Override
            public int compare(ArchiveFileListItem ri, ArchiveFileListItem li) {
                return (ri.shoot_date+ri.shoot_time+ri.file_name).compareToIgnoreCase(li.shoot_date+li.shoot_time+li.file_name);
            }
        });
        return fl;
    }

    static final private boolean isFileTypeArchiveDestination(String name) {
        boolean result=false;
        for(String item:ARCHIVE_FILE_TYPE) {
            if (name.toLowerCase().endsWith("."+item)) {
                result=true;
                break;
            }
        }
        return result;
    }

    static private ArrayList<ArchiveFileListItem> buildSmbFileList(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile[] children) throws JcifsException {
        ArrayList<ArchiveFileListItem> fl=new ArrayList<ArchiveFileListItem>();
        for(JcifsFile element:children) {
            if (element.isFile() && isFileTypeArchiveDestination(element.getName())) {
                String[] date_time=getFileExifDateTime(stwa, sti, element);
                ArchiveFileListItem afli=new ArchiveFileListItem();
                afli.file=element;
                afli.file_name=element.getName();
                afli.full_path=element.getPath();
                afli.file_size=element.length();
                if (date_time==null || date_time[0]==null) {
                    String[] dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(element.getLastModified()).split(" ");
                    afli.shoot_date=dt[0].replace("/","-");
                    afli.shoot_time=dt[1].replace(":","-");
                    afli.date_from_exif=false;
                } else {
                    afli.shoot_date=date_time[0].replace("/","-");
                    afli.shoot_time=date_time[1].replace(":","-");
                }
                if (isFileArchiveRequired(stwa, sti, afli)) fl.add(afli);
            }
        }
        Collections.sort(fl, new Comparator<ArchiveFileListItem>(){
            @Override
            public int compare(ArchiveFileListItem ri, ArchiveFileListItem li) {
                return (ri.shoot_date+ri.shoot_time+ri.file_name).compareToIgnoreCase(li.shoot_date+li.shoot_time+li.file_name);
            }
        });
        return fl;
    }

    static final public boolean isFileArchiveRequired(SyncThreadWorkArea stwa, SyncTaskItem sti, ArchiveFileListItem afli) {
        Calendar cal= Calendar.getInstance() ;
        String[] dt=afli.shoot_date.split("-");
        String[] tm=afli.shoot_time.split("-");
        cal.set(Integer.parseInt(dt[0]), Integer.parseInt(dt[1])-1, Integer.parseInt(dt[2]),
                Integer.parseInt(tm[0]), Integer.parseInt(tm[1]), Integer.parseInt(tm[2]));
        String c_ft=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(cal.getTimeInMillis());
        long exp_time=0, day_mili=1000L*60L*60L*24L;
        if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_7_DAYS) exp_time=day_mili*7L;
        else if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_30_DAYS) exp_time=day_mili*30L;
        else if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_60_DAYS) exp_time=day_mili*60L;
        else if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_90_DAYS) exp_time=day_mili*90L;
        else if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_180_DAYS) exp_time=day_mili*180L;
        else if (sti.getDestinationArchiveRetentionPeriod()== SyncTaskItem.ARCHIVE_RETAIN_FOR_A_1_YEARS) {
            int n_year=cal.getTime().getYear();
            Calendar n_cal= Calendar.getInstance() ;
            n_cal.setTimeInMillis(cal.getTimeInMillis());
            n_cal.add(Calendar.YEAR, 1);
            exp_time=n_cal.getTimeInMillis()-cal.getTimeInMillis();
        }
        String n_exp=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(cal.getTimeInMillis()+exp_time);
//        boolean result=(System.currentTimeMillis()>cal.getTimeInMillis());
        boolean result=(System.currentTimeMillis()>(cal.getTimeInMillis()+exp_time));
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I","isFileArchiveRequired path=",afli.full_path,", shoot date=",afli.shoot_date,
                ", shoot time=", afli.shoot_time,", exif="+afli.date_from_exif,", archive required="+result, ", " +
                "retention period="+sti.getDestinationArchiveRetentionPeriod(), ", expiration date=", n_exp, ", expiration period="+exp_time);
        return result;
    }

    static private String convertKeywordWithDate(SyncThreadWorkArea stwa, SyncTaskItem sti, String before_value, ArchiveFileListItem afli) {
        String temp_dir="";

        Date shoot_date=null;
        SimpleDateFormat sdf_date_shoot = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
        long tu=System.currentTimeMillis();
        try {
            shoot_date=sdf_date_shoot.parse(afli.shoot_date+" "+afli.shoot_time);
            tu=shoot_date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        temp_dir=SyncThread.replaceKeywordValue(before_value, tu, stwa.syncBeginTime);
        return temp_dir;
    }

    static private String convertFileNameWithDate(SyncThreadWorkArea stwa, SyncTaskItem sti, ArchiveFileListItem afli, String original_name) {
        String to_file_name=original_name;
        if (!sti.getDestinationArchiveRenameFileTemplate().equals("")) {
            String convert_name_with_date= convertKeywordWithDate(stwa, sti, sti.getDestinationArchiveRenameFileTemplate(), afli);
            to_file_name=convert_name_with_date.replaceAll(SyncTaskItem.TEMPLATE_ORIGINAL_NAME, original_name);
        }
        return to_file_name;
    }

    static private String getFileSeqNumber(SyncThreadWorkArea stwa, SyncTaskItem sti, int seq_no) {
        String seqno="";
        if (sti.getDestinationArchiveSuffixOption().equals("2")) seqno= String.format("_%02d", seq_no);
        else if (sti.getDestinationArchiveSuffixOption().equals("3")) seqno= String.format("_%03d", seq_no);
        else if (sti.getDestinationArchiveSuffixOption().equals("4")) seqno= String.format("_%04d", seq_no);
        else if (sti.getDestinationArchiveSuffixOption().equals("5")) seqno= String.format("_%05d", seq_no);
        else if (sti.getDestinationArchiveSuffixOption().equals("6")) seqno= String.format("_%06d", seq_no);
        return seqno;
    }

    static final public String[] getFileExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, File lf) {
        String[] date_time=null;
        try {
            FileInputStream fis=new FileInputStream(lf);
            FileInputStream fis_retry=new FileInputStream(lf);
            date_time=getFileExifDateTime(stwa, sti, fis, fis_retry, lf.lastModified(), lf.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date_time;
    }

    static final public String[] getFileExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 sf) {
        String[] date_time=null;
        try {
            InputStream fis=sf.getInputStream();
            InputStream fis_retry=sf.getInputStream();
            date_time=getFileExifDateTime(stwa, sti, fis, fis_retry, sf.lastModified(), sf.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date_time;
    }

    static final public String[] getFileExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile lf) throws JcifsException {
        String[] date_time=null;
        InputStream fis=lf.getInputStream();
        InputStream fis_retry=lf.getInputStream();
        date_time=getFileExifDateTime(stwa, sti, fis, fis_retry, lf.getLastModified(), lf.getName());
        return date_time;
    }

    static final public String[] getFileExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, InputStream fis,
                                                     InputStream fis_retry, long last_mod, String file_name) {
        String[] date_time=null;
        if (file_name.endsWith(".mp4") || file_name.endsWith(".mov") ) {
            date_time=getMp4ExifDateTime(stwa, fis);
        } else {
            try {
                date_time=getExifDateTime(stwa, fis);//, buff);
                fis.close();
                if (date_time==null || date_time[0]==null) {
                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read exif date and time failed, name="+file_name);
                    if (Build.VERSION.SDK_INT>=24) {
                        ExifInterface ei = new ExifInterface(fis_retry);
                        String dt=ei.getAttribute(ExifInterface.TAG_DATETIME);
                        if (dt!=null) {
                            date_time=new String[2];
                            if (dt.endsWith("Z")) {
                                String[] date=dt.split("T");
                                date_time[0]=date[0].replaceAll(":", "/");//Date
                                date_time[1]=date[1].substring(0,date[1].length()-1);//Time
                            } else {
                                String[] date=dt.split(" ");
                                date_time[0]=date[0].replaceAll(":", "/");//Date
                                date_time[1]=date[1];//Time
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I","Read exif date and time failed by ExifInterface, name="+file_name);
                        }
                        fis_retry.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
            }
        }
//        if (date_time==null || date_time[0]==null) {
//            date_time= StringUtil.convDateTimeTo_YearMonthDayHourMinSec(last_mod).split(" ");
//        }
        try {fis_retry.close();} catch(Exception e) {};
        return date_time;
    }

    static private void putExceptionMessage(SyncThreadWorkArea stwa, StackTraceElement[] st, String e_msg) {
        String st_msg=MiscUtil.getStackTraceString(st);
        stwa.util.addDebugMsg(1,"E",stwa.currentSTI.getSyncTaskName()," Error="+e_msg+st_msg);
    }

    static final private String[] parseDateValue(String date_val) {
        String[] result=null;
        if (date_val!=null) {
            Date date=new Date(date_val);
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String converted_date=sdf.format(date);
            result=converted_date.split(" ");
        }
        return result;
    }

    static final public String[] getMp4ExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, File lf) {
        String[] result=null;
        try {
            InputStream fis=new FileInputStream(lf);
            result=getMp4ExifDateTime(stwa, fis);
        } catch (Exception e) {
            e.printStackTrace();
            putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
        }
        return result;
    }

    static final public String[] getMp4ExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 sf) {
        String[] result=null;
        InputStream fis=null;
        try {
            fis=stwa.appContext.getContentResolver().openInputStream(sf.getUri());
            result=getMp4ExifDateTime(stwa, fis);
        } catch (Exception e) {
            e.printStackTrace();
            putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
        }
        return result;
    }

    static final public String[] getMp4ExifDateTime(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile lf) throws JcifsException {
        String[] result=null;
        InputStream fis=lf.getInputStream();
        result=getMp4ExifDateTime(stwa, fis);
        return result;
    }

    static final public String[] getMp4ExifDateTime(SyncThreadWorkArea stwa, InputStream fis)  {
        String[] result=null;
        try {
            Metadata metaData;
            metaData = ImageMetadataReader.readMetadata(fis);
            Mp4Directory directory=null;
            if (metaData!=null) {
                directory=metaData.getFirstDirectoryOfType(Mp4Directory.class);
                if (directory!=null) {
                    String date = directory.getString(Mp4Directory.TAG_CREATION_TIME);
                    result=parseDateValue(date);
                    if (result!=null && result[0].startsWith("1904")) result=null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
        }
        try {
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
        }
        return result;
    }

    static public String[] getExifDateTime(SyncThreadWorkArea stwa, InputStream fis) {
        // Read date and time from EXIF segment

        // Description of Exif file format
        // https://www.media.mit.edu/pia/Research/deepview/exif.html
        // http://dsas.blog.klab.org/archives/52123322.html
        // https://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html

        BufferedInputStream bis=new BufferedInputStream(fis, 1024*32);
        String[] result=null;
        try {
            byte[] buff=readExifData(bis, 2);
            if (buff!=null && buff[0]==(byte)0xff && buff[1]==(byte)0xd8) { //JPEG SOI
                while(buff!=null) {// find dde1 jpeg segemnt
                    buff=readExifData(bis, 4);
                    if (buff!=null) {
                        if (buff[0]==(byte)0xff && buff[1]==(byte)0xe1) { //APP1マーカ
                            int seg_size=getIntFrom2Byte(false, buff[2], buff[3]);
                            buff=readExifData(bis, 14);
                            if (buff!=null) {
                                boolean little_endian=false;
                                if (buff[6]==(byte)0x49 && buff[7]==(byte)0x49) little_endian=true;
                                int ifd_offset=getIntFrom4Byte(little_endian, buff[10], buff[11], buff[12], buff[13]);

                                byte[] ifd_buff=new byte[seg_size+ifd_offset];
                                System.arraycopy(buff,6,ifd_buff,0,8);
                                buff=readExifData(bis, seg_size);
                                if (buff!=null) {
                                    System.arraycopy(buff,0,ifd_buff,8,seg_size);
                                    result=process0thIfdTag(little_endian, ifd_buff, ifd_offset);
                                    break;
                                } else {
                                    if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read Exif date and time failed, because unpredical EOF reached.");
                                    return null;
                                }
                            } else {
                                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read Exif date and time failed, because unpredical EOF reached.");
                                return null;
                            }
                        } else {
                            int offset=((int)buff[2]&0xff)*256+((int)buff[3]&0xff)-2;
                            if (offset<1) {
                                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read Exif date and time failed, because invalid offset.");
                                return null;
                            }
                            buff=readExifData(bis, offset);
                        }
                    } else {
                        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read Exif date and time failed, because unpredical EOF reached.");
                        return null;
                    }
                }

            } else {
                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"W","Read exif date and time failed, because Exif header can not be found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            putExceptionMessage(stwa, e.getStackTrace(), e.getMessage());
            return null;
        }
        return result;
    }

    static private byte[] readExifData(BufferedInputStream bis, int read_size) throws Exception {
        byte[] buff=new byte[read_size];
        int rc=bis.read(buff,0,read_size);
        if (rc>0) return buff;
        else return null;
    }

    static private int getIntFrom2Byte(boolean little_endian, byte b1, byte b2) {
        int result=0;
        if (little_endian) result=((int)b2&0xff)*256+((int)b1&0xff);
        else result=((int)b1&0xff)*256+((int)b2&0xff);
        return result;
    }

    static private int getIntFrom4Byte(boolean little_endian, byte b1, byte b2, byte b3, byte b4) {
        int result=0;
        if (little_endian) result=((int)b4&0xff)*65536+((int)b3&0xff)*4096+((int)b2&0xff)*256+((int)b1&0xff);
        else result=((int)b1&0xff)*65536+((int)b2&0xff)*4096+((int)b3&0xff)*256+((int)b4&0xff);
        return result;
    }

    static private String[] process0thIfdTag(boolean little_endian, byte[]ifd_buff, int ifd_offset) {
        int count=getIntFrom2Byte(little_endian, ifd_buff[ifd_offset+0], ifd_buff[ifd_offset+1]);
        int i=0;
        int ba=ifd_offset+2;
        String[] result=null;
        while(i<count) {
            int tag_number=getIntFrom2Byte(little_endian, ifd_buff[ba+0], ifd_buff[ba+1]);
            int tag_offset=getIntFrom4Byte(little_endian, ifd_buff[ba+8], ifd_buff[ba+9], ifd_buff[ba+10], ifd_buff[ba+11]);

            if (tag_number==(0x8769&0xffff)) {//Exif IFD
                result=processExifIfdTag(little_endian, ifd_buff, tag_offset);
                break;
            }
            ba+=12;
            i++;
        }
        return result;
    }

    static private String[] processExifIfdTag(boolean little_endian, byte[]ifd_buff, int ifd_offset) {
        int count=getIntFrom2Byte(little_endian, ifd_buff[ifd_offset+0], ifd_buff[ifd_offset+1]);
        int i=0;
        int ba=ifd_offset+2;
        String[] date_time=new String[2];
        while(i<count) {
            int tag_number=getIntFrom2Byte(little_endian, ifd_buff[ba+0], ifd_buff[ba+1]);
            int tag_offset=getIntFrom4Byte(little_endian, ifd_buff[ba+8], ifd_buff[ba+9], ifd_buff[ba+10], ifd_buff[ba+11]);
            if (tag_number==(0x9003&0xffff)) {//Date&Time TAG
                String[] date = new String(ifd_buff, tag_offset, 19).split(" "); // YYYY:MM:DD HH:MM:SS
                if (date.length==2) {
                    date_time[0]=date[0].replaceAll(":", "/");//Date
                    date_time[1]=date[1];//Time
                    break;
                }
            }
            ba+=12;
            i++;
        }
        return date_time;
    }

}
