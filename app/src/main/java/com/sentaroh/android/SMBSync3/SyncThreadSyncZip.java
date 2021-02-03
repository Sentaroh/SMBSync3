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
import android.content.Context;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.sentaroh.android.SMBSync3.SyncThread.SyncThreadWorkArea;

import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.Zip.BufferedZipFile3;
import com.sentaroh.android.Utilities3.Zip.ZipUtil;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import static com.sentaroh.android.SMBSync3.Constants.*;

public class SyncThreadSyncZip {

    static private void setZipEnvironment(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                             SafFile3 mf, ZipParameters zp) {
        String fp_prefix="";
        if (mf.getUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) fp_prefix=SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX;
        else fp_prefix=SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+mf.getUuid();

        zp.setDefaultFolderPath(fp_prefix+"/");

        if (sti.getDestinationZipCompressionLevel().equals(SyncTaskItem.ZIP_OPTION_COMPRESSION_LEVEL_FASTEST)) zp.setCompressionLevel(CompressionLevel.FASTEST);
        else if (sti.getDestinationZipCompressionLevel().equals(SyncTaskItem.ZIP_OPTION_COMPRESSION_LEVEL_FAST)) zp.setCompressionLevel(CompressionLevel.FAST);
        else if (sti.getDestinationZipCompressionLevel().equals(SyncTaskItem.ZIP_OPTION_COMPRESSION_LEVEL_NORMAL)) zp.setCompressionLevel(CompressionLevel.NORMAL);
        else if (sti.getDestinationZipCompressionLevel().equals(SyncTaskItem.ZIP_OPTION_COMPRESSION_LEVEL_MAXIMUM)) zp.setCompressionLevel(CompressionLevel.MAXIMUM);

        zp.setCompressionMethod(CompressionMethod.DEFLATE);

        if (sti.getDestinationZipEncryptMethod().equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_STANDARD)) {
            zp.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
            zp.setEncryptFiles(true);
        } else if (sti.getDestinationZipEncryptMethod().equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_AES128)) {
            zp.setEncryptionMethod(EncryptionMethod.AES);
            zp.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
            zp.setEncryptFiles(true);
        } else if (sti.getDestinationZipEncryptMethod().equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_AES256)) {
            zp.setEncryptionMethod(EncryptionMethod.AES);
            zp.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            zp.setEncryptFiles(true);
        }

    }

    static public int syncMirrorLocalToLocalZip(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String dest_file_path) {
        int sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        ZipParameters zp = new ZipParameters();
        SafFile3 osf=new SafFile3(stwa.appContext, dest_file_path);
        SafFile3 pdf=osf.getParentFile();
        if (!pdf.exists()) {
            pdf.mkdirs();
        }
        String out_temp_path=dest_file_path+".tmp";
        try {
            BufferedZipFile3 bzf = new BufferedZipFile3(stwa.appContext, dest_file_path, out_temp_path, sti.getDestinationZipFileNameEncoding());
            if (bzf != null) {
                bzf.setNoCompressExtentionList(stwa.gp.settingNoCompressFileType);
                bzf.setPassword(sti.getDestinationZipPassword());
                SafFile3 mf = new SafFile3(stwa.appContext, from_path);
                setZipEnvironment(stwa, sti, mf, zp);
                sync_result = moveCopyLocalToLocalZip(stwa, sti, false, from_path, from_path, mf, bzf, zp, null);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    sync_result = syncDeleteLocalToLocalZip(stwa, sti, from_path, from_path, bzf, zp);
                    if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                        try {
                            SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                            CallBackListener cbl=new CallBackListener() {
                                @Override
                                public void onCallBack(Context context, boolean positive, Object[] o) {
                                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) bzf.abort();
                                    else {
                                        int prog=(Integer)o[0];
                                        SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(),
                                                stwa.appContext.getString(R.string.msgs_mirror_file_zip_update_zip_file
                                                )+" "+prog+"%");
                                    }
                                }
                            };
                            if (!bzf.isAborted()) {
                                if (bzf.close(cbl)) {
                                    if (!bzf.isAborted()) {
                                        SafFile3 dest=new SafFile3(stwa.appContext, dest_file_path);
                                        dest.deleteIfExists();
                                        tmp.renameTo(dest);
                                    } else {
                                        sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                        tmp.deleteIfExists();
                                    }
                                } else {
//                                    sync_result=TaskListItem.SYNC_STATUS_ERROR;
                                    tmp.deleteIfExists();
                                }
                            } else {
                                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                bzf.destroy();
                                tmp.deleteIfExists();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                            if (tmp!=null) tmp.deleteIfExists();
                        }
                    } else {
                        bzf.destroy();
                        SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                        tmp.deleteIfExists();
                    }
                } else {
                    bzf.destroy();
                    SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                    tmp.deleteIfExists();
                }
            } else {
                stwa.util.addLogMsg("E",sti.getSyncTaskName(), "BufferedZipFile creation error");
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                tmp.deleteIfExists();
            }
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, from_path, dest_file_path);
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
            tmp.deleteIfExists();
        }
        return sync_result;
    }

    static private void putErrorMessage(SyncThreadWorkArea stwa, SyncTaskItem sti, Exception e, String from_path, String to_path) {
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

    static public int syncCopyLocalToLocalZip(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String dest_file_path) {
        int sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " source=", from_path, ", destination=", dest_file_path);
        ZipParameters zp = new ZipParameters();
        SafFile3 osf=new SafFile3(stwa.appContext, dest_file_path);
        SafFile3 pdf=osf.getParentFile();
        if (!pdf.exists()) {
            pdf.mkdirs();
        }
        String out_temp_path=dest_file_path+".tmp";
        try {
            BufferedZipFile3 bzf = new BufferedZipFile3(stwa.appContext, dest_file_path, out_temp_path, sti.getDestinationZipFileNameEncoding());
            if (bzf != null) {
                bzf.setNoCompressExtentionList(stwa.gp.settingNoCompressFileType);
                bzf.setPassword(sti.getDestinationZipPassword());
                SafFile3 mf = new SafFile3(stwa.appContext, from_path);
                setZipEnvironment(stwa, sti, mf, zp);
                sync_result = moveCopyLocalToLocalZip(stwa, sti, false, from_path, from_path, mf, bzf, zp, null);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    try {
                        SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                        CallBackListener cbl=new CallBackListener() {
                            @Override
                            public void onCallBack(Context context, boolean positive, Object[] o) {
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) bzf.abort();
                                else {
                                    int prog=(Integer)o[0];
                                    SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(),
                                            stwa.appContext.getString(R.string.msgs_mirror_file_zip_update_zip_file
                                            )+" "+prog+"%");
                                }
                            }
                        };
                        if (!bzf.isAborted()) {
                            if (bzf.close(cbl)) {
                                if (!bzf.isAborted()) {
                                    SafFile3 dest=new SafFile3(stwa.appContext, dest_file_path);
                                    dest.deleteIfExists();
                                    tmp.renameTo(dest);
                                } else {
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    tmp.deleteIfExists();
                                }
                            } else {
//                                    sync_result=TaskListItem.SYNC_STATUS_ERROR;
                                tmp.deleteIfExists();
                            }
                        } else {
                            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                            bzf.destroy();
                            tmp.deleteIfExists();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                        if (tmp!=null) tmp.deleteIfExists();
                    }
                } else {
                    bzf.destroy();
                    SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                    tmp.deleteIfExists();
                }
            } else {
                stwa.util.addLogMsg("E",sti.getSyncTaskName(), "BufferedZipFile creation error");
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                tmp.deleteIfExists();
            }
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, from_path, dest_file_path);
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
            tmp.deleteIfExists();
        }
        return sync_result;
    }

    static public int syncMoveLocalToLocalZip(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String dest_file_path) {
        int sync_result = SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " source=", from_path, ", destination=", dest_file_path);
        ZipParameters zp = new ZipParameters();
        SafFile3 osf=new SafFile3(stwa.appContext, dest_file_path);
        SafFile3 pdf=osf.getParentFile();
        if (!pdf.exists()) {
            pdf.mkdirs();
        }
        String out_temp_path=dest_file_path+".tmp";
        try {
            BufferedZipFile3 bzf = new BufferedZipFile3(stwa.appContext, dest_file_path, out_temp_path, sti.getDestinationZipFileNameEncoding());
            if (bzf != null) {
                bzf.setNoCompressExtentionList(stwa.gp.settingNoCompressFileType);
                bzf.setPassword(sti.getDestinationZipPassword());
                SafFile3 mf = new SafFile3(stwa.appContext, from_path);
                setZipEnvironment(stwa, sti, mf, zp);
                ArrayList<SafFile3>remove_list=new ArrayList<SafFile3>();
                sync_result = moveCopyLocalToLocalZip(stwa, sti, true, from_path, from_path, mf, bzf, zp, remove_list);
                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    try {
                        SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                        CallBackListener cbl=new CallBackListener() {
                            @Override
                            public void onCallBack(Context context, boolean positive, Object[] o) {
                                if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) bzf.abort();
                                else {
                                    int prog=(Integer)o[0];
                                    SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(),
                                            stwa.appContext.getString(R.string.msgs_mirror_file_zip_update_zip_file
                                            )+" "+prog+"%");
                                }
                            }
                        };
                        if (!bzf.isAborted()) {
                            if (bzf.close(cbl)) {
                                if (!bzf.isAborted()) {
                                    SafFile3 dest=new SafFile3(stwa.appContext, dest_file_path);
                                    dest.deleteIfExists();
                                    tmp.renameTo(dest);
                                    for(SafFile3 del_sf:remove_list) {
                                        SyncThreadSyncFile.deleteLocalItem(stwa, sti, mf);
                                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, mf.getName(),
                                                "", stwa.appContext.getString(R.string.msgs_mirror_task_file_moved));
                                    }
                                } else {
                                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                                    tmp.deleteIfExists();
                                }
                            } else {
                                //Close success without update
                                tmp.deleteIfExists();
                            }
                        } else {
                            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                            bzf.destroy();
                            tmp.deleteIfExists();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                        if (tmp!=null) tmp.deleteIfExists();
                    }
                } else {
                    bzf.destroy();
                    SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                    if (tmp!=null) tmp.deleteIfExists();
                }
            } else {
                stwa.util.addLogMsg("E",sti.getSyncTaskName(), "BufferedZipFile creation error");
                sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
                if (tmp!=null) tmp.deleteIfExists();
            }
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, from_path, dest_file_path);
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            SafFile3 tmp=new SafFile3(stwa.appContext, out_temp_path);
            if (tmp!=null) tmp.deleteIfExists();
        }
        return sync_result;
    }

    static private boolean isFileChanged(SyncThreadWorkArea stwa, SyncTaskItem sti, String to_path, SafFile3 mf,
                                         BufferedZipFile3 bzf, boolean ac, ZipParameters zp) {
        boolean result = false;
        FileHeader fh=bzf.getFileHeader(to_path.replace(zp.getDefaultFolderPath(),""));
        if (fh != null) {
            result = isFileChangedDetailCompare(stwa, sti, to_path,
                    true, ZipUtil.dosToJavaTme((int) fh.getLastModifiedTime()), fh.getUncompressedSize(),
                    mf.exists(), mf.lastModified(), mf.length(),
                    ac, zp);
        } else {
            result = true;
        }
        return result;
    }

    static final private boolean isFileChangedDetailCompare(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                            String lf_path, boolean lf_exists, long lf_time, long lf_length,
                                                            boolean mf_exists, long mf_time, long mf_length,
                                                            boolean ac, ZipParameters zp) {
        boolean diff = false;
        boolean exists_diff = false;

        long time_diff = Math.abs((mf_time - lf_time));
        long length_diff = Math.abs((mf_length - lf_length));

        if (mf_exists != lf_exists) exists_diff = true;
        if (exists_diff || (sti.isSyncOptionDifferentFileBySize() && length_diff > 0) || ac) {
            diff = true;
        } else {//Check lastModified()
            if (sti.isSyncOptionDifferentFileByTime()) {
                if (time_diff > stwa.syncDifferentFileAllowableTime) { //LastModified was changed
                    diff = true;
                } else diff = false;
            }
        }
//        stwa.util.addDebugMsg(3, "I", "isFileChangedDetailCompare");
//        if (mf_exists) stwa.util.addDebugMsg(3, "I", "Source file length=" + mf_length +
//                ", last modified(ms)=" + mf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((mf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Source file was not exists");
//        if (lf_exists) stwa.util.addDebugMsg(3, "I", "Destination file length=" + lf_length +
//                ", last modified(ms)=" + lf_time +
//                ", date=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time / 1000) * 1000));
//        else stwa.util.addDebugMsg(3, "I", "Destination file was not exists");
//        stwa.util.addDebugMsg(3, "I", "allcopy=" + ac + ",exists_diff=" + exists_diff +
//                ",time_diff=" + time_diff + ",length_diff=" + length_diff + ", diff=" + diff);

        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "isFileChangedDetailCompare(ZIP) fp="+lf_path+ ", exists_diff=" + exists_diff +
                ", time_diff=" + time_diff + ", length_diff=" + length_diff + ", diff=" + diff+", destination_time="+lf_time+", Source_time="+mf_time);
        return diff;
    }

    static private boolean createDirectoryToZip(SyncThreadWorkArea stwa, SyncTaskItem sti, String to_dir, BufferedZipFile3 bzf, ZipParameters zp) {
        boolean result = false;
        try {
            String ref_dir=to_dir.replace(zp.getDefaultFolderPath(), "");
            if (!ref_dir.endsWith("/")) ref_dir=ref_dir+"/";
            if (!bzf.exists(ref_dir)) {
                if (!sti.isSyncTestMode()) {
                    zp.setFileNameInZip(ref_dir);
                    bzf.addItem(to_dir, zp);
                } else {
                    result = true;
                }
                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " directory created, dir=" + to_dir);
            } else {
                if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " directory was already exist, dir=" + to_dir);
            }
            result=true;
        } catch (Exception e) {
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                    CommonUtilities.getExecutedMethodName() + " directory=" + to_dir + ", Zip=" + bzf.getInputZipFile().getPath());
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            result=false;
        }
        return result;
    }

    static private int moveCopyLocalToLocalZip(SyncThreadWorkArea stwa, SyncTaskItem sti, boolean move_file,
                                               String from_base, String from_path, SafFile3 mf, BufferedZipFile3 bzf, ZipParameters zp,
                                               ArrayList<SafFile3>remove_list) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, from=" + from_path +
                    ", dest=" + bzf.getInputZipFile().getPath() + ", move=" + move_file);
        int sync_result = 0;
        try {
            String relative_from_dir = from_path.substring(from_base.length());
            if (mf.exists()) {
                if (mf.isDirectory()) { // Directory copy
                    if (!SyncThread.isHiddenDirectory(stwa, sti, mf) && SyncThread.isDirectoryToBeProcessed(stwa, relative_from_dir)) {
                        if (!mf.canRead()) {
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", mf.getName(),
                                    "", stwa.appContext.getString(R.string.msgs_mirror_directory_ignored_because_access_not_granted, mf.getPath()));
                            stwa.totalIgnoreCount++;
                            return sync_result;
                        }
                        SafFile3[] children = mf.listFiles();
                        if (children != null) {
                            for (SafFile3 element : children) {
                                if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                    if (!element.getName().equals(".android_secure")) {
                                        if (element.isFile()) {
                                            sync_result = moveCopyLocalToLocalZip(stwa, sti, move_file, from_base, from_path + "/" + element.getName(),
                                                    element, bzf, zp, remove_list);
                                        } else {
                                            if (sti.isSyncOptionSyncSubDirectory()) {
                                                sync_result = moveCopyLocalToLocalZip(stwa, sti, move_file, from_base, from_path + "/" + element.getName(),
                                                        element, bzf, zp, remove_list);
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
                                if (!relative_from_dir.equals("") && SyncThread.isDirectoryIncluded(stwa, relative_from_dir)) {
                                    createDirectoryToZip(stwa, sti, from_path, bzf, zp);
                                }
                            }
                        } else {
                            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", "Directory was null, dir=" + mf.getPath());
                        }
                    }
                } else { // file copy
                    long mf_length=mf.length();
                    long mf_last_modified=mf.lastModified();
                    if (//SyncThread.isDirectorySelectedByFileName(stwa, relative_from_dir) &&
                            !SyncThread.isHiddenFile(stwa, sti, mf) &&
                            SyncThread.isFileSelected(stwa, sti, relative_from_dir, from_path, mf_length, mf_last_modified)) {
                        boolean tf_exists = false;
                        FileHeader fh = bzf.getFileHeader(from_path.replace(zp.getDefaultFolderPath(), ""));
                        tf_exists = fh == null ? false : true;
                        if (tf_exists && !sti.isSyncOverrideCopyMoveFile()) {
                            //Ignore override the file
                            if (move_file)
                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), from_path, stwa.appContext.getString(R.string.msgs_mirror_ignore_override_move_file));
                            else
                                stwa.util.addLogMsg("W", sti.getSyncTaskName(), from_path, stwa.appContext.getString(R.string.msgs_mirror_ignore_override_copy_file));
                        } else {
                            if (move_file) {
                                if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_MOVE, from_path, relative_from_dir)) {
                                    if (isFileChanged(stwa, sti, from_path, mf, bzf, stwa.ALL_COPY, zp)) {
                                        sync_result = copyFileLocalToLocalZip(stwa, sti, move_file, from_path.replace("/" + mf.getName(), ""),
                                                mf, relative_from_dir, mf.getName(), bzf, zp);
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                            stwa.totalMoveCount++;
                                            if (tf_exists) stwa.totalReplaceCount++;
                                            remove_list.add(mf);
                                        }
                                    } else {
                                        stwa.totalMoveCount++;
                                        remove_list.add(mf);
                                    }
                                } else {
                                    stwa.util.addLogMsg("W", sti.getSyncTaskName(), relative_from_dir, stwa.appContext.getString(R.string.msgs_mirror_confirm_move_cancel));
                                }
                            } else {
                                if (isFileChanged(stwa, sti, from_path, mf, bzf, stwa.ALL_COPY, zp)) {
                                    if (!tf_exists || SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_COPY, from_path, relative_from_dir)) {
                                        sync_result = copyFileLocalToLocalZip(stwa, sti, move_file, from_path.replace("/" + mf.getName(), ""),
                                                mf, relative_from_dir, mf.getName(), bzf, zp);
                                        if (sync_result == SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                                            String tmsg = tf_exists ? stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced) : stwa.appContext.getString(R.string.msgs_mirror_task_file_copied);
                                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", from_path, mf.getName(), "", tmsg);
                                            stwa.totalCopyCount++;
                                            if (tf_exists) stwa.totalReplaceCount++;
                                        }
                                    } else {
                                        stwa.util.addLogMsg("W", sti.getSyncTaskName(), relative_from_dir, stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                                    }
                                }
                            }
                            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl))
                                sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                        }
                    }
                }
            } else {
                stwa.gp.syncThreadCtrl.setThreadMessage(stwa.appContext.getString(R.string.msgs_mirror_task_source_not_found, from_path));
                SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "E", "", "", stwa.gp.syncThreadCtrl.getThreadMessage());
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
        } catch (IOException e) {
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                    CommonUtilities.getExecutedMethodName() + " From=" + from_path);
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static final private int syncDeleteLocalToLocalZip(SyncThreadWorkArea stwa, SyncTaskItem sti, String base_path, String from_path,
                                                       BufferedZipFile3 bzf, ZipParameters zp) {
        int sync_result = 0;
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " source=", from_path, ", destination=", bzf.getInputZipFile().getPath());
        ContentProviderClient mf_cpc=null;
        String zip_entry_path="";
        try {
            SafFile3 mf = new SafFile3(stwa.appContext, from_path);
            mf_cpc = mf.getContentProviderClient();
            String fp_prefix="";
            if (mf.getUuid().equals(SafFile3.SAF_FILE_PRIMARY_UUID)) fp_prefix=SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX+"/";
            else fp_prefix=SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+mf.getUuid()+"/";
            if (mf.exists()) {
                ArrayList<FileHeader> fhl = bzf.getFileHeaderList();
                ArrayList<FileHeader> remove_list = new ArrayList<FileHeader>();
                for (FileHeader fh_item : fhl) {
                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) {
                        sync_result = SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                        return sync_result;
                    }
                    SafFile3 mf_check = new SafFile3(stwa.appContext, fp_prefix+fh_item.getFileName());
                    zip_entry_path=mf_check.getPath();
                    boolean mf_exists = mf_check.exists(mf_cpc);
                    boolean isFileSelected=SyncThread.isFileSelected(stwa, sti, fh_item.getFileName());
                    if (isFileSelected) {
                        if (!mf_exists) {
                            if (!isAlreadyRemoved(fh_item, remove_list)) {
                                deleteZipItemWithConfirm(stwa, sti, fh_item, mf_check, remove_list);
                            } else {
                                remove_list.add(fh_item);
                            }
                        }
                    } else {
                        if (sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter()) deleteZipItemWithConfirm(stwa, sti, fh_item, mf_check, remove_list);
                    }
                }
                zip_entry_path="";//bzf.getInputZipFile().getPath();
                if (remove_list.size() > 0 && !sti.isSyncTestMode()) {
                    bzf.removeItem(remove_list);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                    CommonUtilities.getExecutedMethodName() + " source=" + from_path + ", destination=" + zip_entry_path);
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } finally {
            if (mf_cpc!=null) mf_cpc.release();
        }
        return sync_result;
    }

    static private void deleteZipItemWithConfirm(SyncThreadWorkArea stwa, SyncTaskItem sti, FileHeader fh_item, SafFile3 mf_check, ArrayList<FileHeader>remove_list) {
        if (!isAlreadyRemoved(fh_item, remove_list)) {
            String request_id="", deleted_msg="";
            if (fh_item.isDirectory()) {
                request_id=CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR;
                deleted_msg=stwa.appContext.getString(R.string.msgs_mirror_task_dir_deleted);
            } else {
                request_id=CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE;
                deleted_msg=stwa.appContext.getString(R.string.msgs_mirror_task_file_deleted);
            }
            if (SyncThread.sendConfirmRequest(stwa, sti, request_id, "", fh_item.getFileName())) {
                remove_list.add(fh_item);
                if (fh_item.isDirectory()) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", fh_item.getFileName(), mf_check.getName(), "", deleted_msg);
                    stwa.totalDeleteCount++;
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", fh_item.getFileName(), mf_check.getName(),"", deleted_msg);
                    stwa.totalDeleteCount++;
                }
            } else {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", fh_item.getFileName(), mf_check.getName(),
                        "", stwa.appContext.getString(R.string.msgs_mirror_confirm_delete_cancel));
            }
        } else {
            remove_list.add(fh_item);
        }

    }

    static private boolean isAlreadyRemoved(FileHeader fh, ArrayList<FileHeader>del_list) {
        boolean result=false;
        if (fh.isDirectory()) {
            for(FileHeader item:del_list) {
                String item_name=item.getFileName();
                String fh_name=fh.getFileName();
                if (item.isDirectory() && fh_name.startsWith(item_name)) {
                    result=true;
                    break;
                }
            }
        } else {
            for(FileHeader item:del_list) {
                String item_name=item.getFileName();
                String fh_name=fh.getFileName();
                if (item.isDirectory() && fh_name.startsWith(item_name)) {
                    result=true;
                    break;
                }
            }
        }
        return result;
    }

    static private int copyFileLocalToLocalZip(SyncThreadWorkArea stwa,
                                               SyncTaskItem sti, boolean move, String from_dir, SafFile3 mf, String to_dirx, String dest_path,
                                               BufferedZipFile3 bzf, ZipParameters zp) throws IOException {
        int sync_result=0;
        String to_dir = from_dir;
        long read_begin_time = System.currentTimeMillis();
        long file_read_bytes = mf.length();
        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        final String msg_text=move?stwa.appContext.getString(R.string.msgs_mirror_task_file_moving):stwa.appContext.getString(R.string.msgs_mirror_task_file_copying);
        try {
            InputStream is = mf.getInputStream();
            BufferedInputStream ifs = new BufferedInputStream(is, SYNC_IO_BUFFER_SIZE);
            String to_name = to_dir + "/" + dest_path;
            ZipParameters n_zp=new ZipParameters(zp);
            n_zp.setFileNameInZip(to_name.replace(zp.getDefaultFolderPath(), ""));
            CallBackListener cbl=new CallBackListener() {
                @Override
                public void onCallBack(Context context, boolean positive, Object[] o) {
                    if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) bzf.abort();
                    else {
                        int prog=(Integer)o[0];
                        SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(), mf.getName()+" "+String.format(msg_text, prog));
                    }
                }
            };
            bzf.addItem(mf, n_zp, cbl);
            ifs.close();
            if (SyncThread.isTaskCancelled(true, stwa.gp.syncThreadCtrl)) sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
        } catch (Exception e) {
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                    CommonUtilities.getExecutedMethodName() + " source=" + from_dir + ", destination=" + to_dir);
            SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
            stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        long file_read_time = System.currentTimeMillis() - read_begin_time;
        if (stwa.logLevel>=1) stwa.util.addDebugMsg(1, "I", to_dir + "/" + dest_path + " " + file_read_bytes + " bytes transfered in ",
                    file_read_time + " mili seconds at " +
                            SyncThread.calTransferRate(file_read_bytes, file_read_time));
        stwa.totalTransferByte += file_read_bytes;
        stwa.totalTransferTime += file_read_time;

        return sync_result;
    }

}
