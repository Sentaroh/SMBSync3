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

import android.os.RemoteException;
import android.view.View;
import android.widget.LinearLayout;

import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


import static com.sentaroh.android.SMBSync3.Constants.*;

public class TwoWaySyncFile {
    static public int syncTwowayInternalToInternal(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        stwa.currSyncFileInfoList.clear();
        stwa.newSyncFileInfoList.clear();
        loadSyncFileInfoList(stwa, sti);

        SafFile3 mf=new SafFile3(stwa.appContext, from_path);
        SafFile3 tf=new SafFile3(stwa.appContext, to_path);
        int sync_result= syncPairInternalToInternal(stwa, sti, from_path, from_path, mf, to_path, to_path, tf);

        saveSyncFileInfoList(stwa, sti);

        return sync_result;
    }

    static private ArrayList<SafFile3> getFileList(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String base_dir, SafFile3 file) {
        stwa.util.addDebugMsg(1,"I", "getFileList entered, base="+base_dir+", dir="+file.getPath());
        ArrayList<SafFile3> s_fl=new ArrayList<SafFile3>();
        if (file.exists()) {
            SafFile3[] fl=file.listFiles();
            if (fl!=null) {
                for(SafFile3 item:fl) {
                    if (!item.getName().endsWith(SyncTaskItem.SYNC_TASK_TWO_WAY_CONFLICT_FILE_SUFFIX)) {
                        s_fl.add(item);
//                        String abs_path=item.getPath().replace(base_dir+"/", "");
//                        if (item.isDirectory()) {
//                            boolean selected=SyncThread.isDirectoryToBeProcessed(stwa, abs_path);
//                            if (selected){
//                                stwa.util.addDebugMsg(1,"I", "getFileList directory added. abs="+abs_path+", dir="+item.getPath());
//                            }
//                        } else {
//                            boolean selected=SyncThread.isFileSelected(stwa, sti, abs_path);
//                            if (selected){
//                                s_fl.add(item);
//                                stwa.util.addDebugMsg(1,"I", "getFileList file added. abs="+abs_path+", file="+item.getPath());
//                            }
//                        }
                    }
                    else {
                        stwa.util.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()," A file was ignored because file was previously conflict file. Path=",item.getPath());
                    }
                }
                Collections.sort(s_fl, new Comparator<SafFile3>(){
                    @Override
                    public int compare(SafFile3 o1, SafFile3 o2) {
                        return o1.getPath().compareToIgnoreCase(o2.getPath());
                    }
                });


            }
        }
        return s_fl;
    }

    static private int syncPairInternalToInternal(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                  String pair_a_base, String pair_a_path, SafFile3 pair_a_file,
                                                  String pair_b_base, String pair_b_path, SafFile3 pair_b_file) {
        if (stwa.util.getLogLevel() >= 2)
            stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, from=", pair_a_path, ", to=", pair_b_path);
        int sync_result = 0;
        stwa.jcifsNtStatusCode=0;
        try {
            ArrayList<SafFile3> pair_a_file_list= getFileList(stwa, sti, pair_a_base, pair_a_file);
            ArrayList<SafFile3> pair_b_file_list= getFileList(stwa, sti, pair_b_base, pair_b_file);
//            detectDeletedFile(stwa, sti, pair_a_file.getPath(), pair_a_file_list, pair_b_file.getPath(), pair_b_file_list, currSyncFileInfoList);
            boolean exit=false;
            int pair_a_cnt=0, pair_b_cnt=0;
            while(!exit && sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                if (!stwa.gp.syncThreadCtrl.isEnabled()) {
                    sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                    return sync_result;
                }
                if (pair_a_cnt<pair_a_file_list.size() && pair_b_cnt<pair_b_file_list.size()) {
                    SafFile3 pair_a_child_file=pair_a_file_list.get(pair_a_cnt);
                    String pair_a_child_file_path=pair_a_child_file.getPath().replace(pair_a_base, "");
                    SafFile3 pair_b_child_file=pair_b_file_list.get(pair_b_cnt);
                    String pair_b_child_file_path=pair_b_child_file.getPath().replace(pair_b_base, "");
                    if (pair_a_child_file_path.compareToIgnoreCase(pair_b_child_file_path)==0) {
                        //Same name
                        if (pair_a_child_file.isDirectory() && pair_b_child_file.isDirectory()) {
                            //下位のディレクトリーを処理
                            updateSyncFileInfo(stwa, sti, pair_a_child_file, pair_b_child_file);
                            sync_result=syncPairInternalToInternal(stwa, sti,
                                    pair_a_base, pair_a_child_file.getPath(), pair_a_child_file, pair_b_base, pair_b_child_file.getPath(), pair_b_child_file);
                            pair_a_cnt++;
                            pair_b_cnt++;
                        } else if (pair_a_child_file.isFile() && pair_b_child_file.isFile()) {
                            //Fileの同期
                            sync_result= syncFileInternalToInternal(stwa, sti,
                                    pair_a_child_file.getPath(), pair_a_child_file.length(), pair_a_child_file.lastModified(),
                                    pair_b_child_file.getPath(), pair_b_child_file.length(), pair_b_child_file.lastModified());
                            pair_a_cnt++;
                            pair_b_cnt++;
                        } else if (pair_a_child_file.isDirectory() != pair_b_child_file.isDirectory()) {
                            //ディレクトリーとファイルの名前が同じため同期不可
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "E", pair_a_path, pair_a_file.getName(),
                                    " ディレクトリーとファイルの名前が同じため同期不可");
                            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                            break;
                        }
                    } else if (pair_a_child_file_path.compareToIgnoreCase(pair_b_child_file_path)>0) {
                        //ターゲットディレクトリーをマスターにコピー
                        SafFile3 to_saf=new SafFile3(stwa.appContext, pair_a_base+"/"+pair_b_child_file_path);
                        TwoWaySyncFileInfoItem sfii=getSyncFileInfo(stwa, sti, to_saf.getPath());
                        if (sfii!=null) {
                            String type=pair_b_child_file.isDirectory()?CONFIRM_REQUEST_DELETE_DIR:CONFIRM_REQUEST_DELETE_FILE;
                            if (SyncThread.sendConfirmRequest(stwa, sti, "", type, pair_b_child_file.getPath())) {
                                sync_result=deleteLocalItem(stwa, sti, pair_b_child_file, to_saf);
                            }
                        } else {
                            sync_result= copyDirectoryInternalToInternal(stwa, sti, pair_b_child_file, to_saf);
                        }
                        pair_b_cnt++;
                    } else if (pair_a_child_file_path.compareToIgnoreCase(pair_b_child_file_path)<0) {
                        //マスターディレクトリーをターゲットにコピー
                        SafFile3 to_saf=new SafFile3(stwa.appContext, pair_b_base+"/"+pair_a_child_file_path);
                        TwoWaySyncFileInfoItem sfii=getSyncFileInfo(stwa, sti, to_saf.getPath());
                        if (sfii!=null) {
                            String type=pair_a_child_file.isDirectory()?CONFIRM_REQUEST_DELETE_DIR:CONFIRM_REQUEST_DELETE_FILE;
                            if (SyncThread.sendConfirmRequest(stwa, sti, type, "", pair_a_child_file.getPath()))
                                sync_result=deleteLocalItem(stwa, sti, pair_a_child_file, to_saf);
                        } else {
                            sync_result= copyDirectoryInternalToInternal(stwa, sti, pair_a_child_file, to_saf);
                        }
                        pair_a_cnt++;
                    }
                } else {
                    if (pair_a_cnt<pair_a_file_list.size()) {
                        SafFile3 pair_a_child_file=pair_a_file_list.get(pair_a_cnt);
                        String pair_a_child_file_path=pair_a_child_file.getPath().replace(pair_a_base, "");
                        SafFile3 out_path=new SafFile3(stwa.appContext, pair_b_file.getPath()+"/"+pair_a_child_file.getName());
                        //マスターをターゲットにコピー
                        if (pair_a_child_file.isDirectory()) {
                            TwoWaySyncFileInfoItem sfii=getSyncFileInfo(stwa, sti, out_path.getPath());
                            if (sfii!=null) {
                                String type=pair_a_child_file.isDirectory()?CONFIRM_REQUEST_DELETE_DIR:CONFIRM_REQUEST_DELETE_FILE;
                                if (SyncThread.sendConfirmRequest(stwa, sti, type, "", pair_a_child_file.getPath()))
                                    sync_result=deleteLocalItem(stwa, sti, pair_a_child_file, out_path);
                            } else {
                                sync_result= copyDirectoryInternalToInternal(stwa, sti, pair_a_child_file, out_path);
                            }
                        } else {
                            //Fileの同期
                            sync_result=copyFileInternalToInternal(stwa, sti, pair_a_child_file.getPath(), out_path.getPath());
                            updateSyncFileInfo(stwa, sti, pair_a_child_file.getPath(), pair_b_file.getPath());
                        }
                        pair_a_cnt++;
                    } else if (pair_b_cnt<pair_b_file_list.size()) {
                        SafFile3 pair_b_child_file=pair_b_file_list.get(pair_b_cnt);
                        String pair_b_child_file_path=pair_b_child_file.getPath().replace(pair_b_base, "");
                        SafFile3 out_path=new SafFile3(stwa.appContext, pair_a_file.getPath()+"/"+pair_b_file.getName());
                        //ターゲットをマスターにコピー
                        if (pair_b_child_file.isDirectory()) {
                            TwoWaySyncFileInfoItem sfii=getSyncFileInfo(stwa, sti, out_path.getPath());
                            if (sfii!=null) {
                                String type=pair_b_child_file.isDirectory()?CONFIRM_REQUEST_DELETE_DIR:CONFIRM_REQUEST_DELETE_FILE;
                                if (SyncThread.sendConfirmRequest(stwa, sti, type, "", pair_b_child_file.getPath()))
                                    sync_result=deleteLocalItem(stwa, sti, pair_b_child_file, out_path);
                            } else {
                                sync_result= copyDirectoryInternalToInternal(stwa, sti, pair_b_child_file, out_path);
                            }
                        } else {
                            //Fileの同期
                            sync_result=copyFileInternalToInternal(stwa, sti, pair_b_child_file.getPath(), out_path.getPath());
                            updateSyncFileInfo(stwa, sti, pair_a_file.getPath()+"/"+pair_b_file.getName(), out_path.getPath());
                        }
                        pair_b_cnt++;
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            putExceptionMessage(stwa, e, e.getMessage());
            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    };

    static private int copyDirectoryInternalToInternal(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 in_dir, SafFile3 out_dir) {
        stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName(), " entered, in=", in_dir.getPath(), ", out=", out_dir.getPath());
        if (in_dir.exists()) {
            if (in_dir.isDirectory()) {
                if (!sti.isSyncOptionSyncSubDirectory()) {
                    if (stwa.util.getLogLevel() >= 1)
                        stwa.util.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName(), " sync aborted. Sync sub directory option is disabled");
                    return 0;
                }
                SafFile3[] file_list=in_dir.listFiles();
                if (sti.isSyncOptionSyncEmptyDirectory()) {
                    if (!sti.isSyncTestMode() && !out_dir.exists()) {
                        out_dir.mkdirs();
                    }
                } else {
                    if (stwa.util.getLogLevel() >= 1)
                        stwa.util.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName(), " sync aborted. Sync empty directory option is disabled");
                    return 0;
                }
                for(SafFile3 child_file:file_list) {
                    if (child_file.isDirectory()) {
                        updateSyncFileInfo(stwa, sti, in_dir, out_dir);
                        copyDirectoryInternalToInternal(stwa, sti, child_file, new SafFile3(stwa.appContext, out_dir.getPath()+"/"+child_file.getName()));
                    } else {
                        copyFileInternalToInternal(stwa, sti, child_file.getPath(), out_dir.getPath()+"/"+child_file.getName());
                    }
                }
            } else {
                copyFileInternalToInternal(stwa, sti, in_dir.getPath(), out_dir.getPath());
            }
        }
        return 0;
    }

    static private int syncFileInternalToInternal(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                  String pair_a_path, long pair_a_length, long pair_a_last_mod,
                                                  String pair_b_path, long pair_b_length, long pair_b_last_mod) {
        int sync_result=0;
        int fc=isFileChanged(stwa, sti, pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
        if (fc!=FILE_WAS_NOT_CHANGED) {
            //Difference file detected
            if (fc==FILE_WAS_CONFLICT_BY_TIME || fc==FILE_WAS_CONFLICT_BY_LENGTH) {
                //競合ファイルが検出された
                if (stwa.util.getLogLevel() >= 2)
                    stwa.util.addDebugMsg(2, "I", "A conflict file has been detected.(Time)");
                if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_ASK_USER)) {
                    String conf_msg="双方のファイルで更新時刻に違いがあり同期元のファイルを判定できません、同期元とするファイルを選択してください。";
                    if (fc==FILE_WAS_CONFLICT_BY_LENGTH) conf_msg="双方のファイルでファイルサイズに違いがあり同期元のファイルを判定できません、同期元とするファイルを選択してください。";
                    int c_result = sendTwoWaySyncConfirmRequest(stwa, sti, conf_msg,
                            pair_a_path, pair_a_length, pair_a_last_mod, pair_b_path, pair_b_length, pair_b_last_mod);
                    if (c_result==TWOWAY_SYNC_CONFIRM_RESULT_SELECT_A) {//Slect pair A
                        renameConflictFile(stwa, sti, pair_b_path);
                        sync_result=copyFileInternalToInternal(stwa, sti, pair_a_path, pair_b_path);
                    } else if (c_result==TWOWAY_SYNC_CONFIRM_RESULT_SELECT_B) {//Slect pair B
                        renameConflictFile(stwa, sti, pair_a_path);
                        sync_result=copyFileInternalToInternal(stwa, sti, pair_b_path, pair_a_path);
                    } else if(c_result==TWOWAY_SYNC_CONFIRM_RESULT_SYNC_ABORT) {
                        sync_result= SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                    }
                } else if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_NEWER)) {
                    if (pair_a_last_mod > pair_b_last_mod) {
                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                                "競合ファイルを検出しましたがオプションにより新しいファイルを優先します。上書きされるファイル="+pair_b_path);
                        renameConflictFile(stwa, sti, pair_b_path);
                        sync_result = copyFileInternalToInternal(stwa, sti, pair_a_path, pair_b_path);
                    } else {
                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                                "競合ファイルを検出しましたがオプションにより新しいファイルを優先します。上書きされるファイル="+pair_a_path);
                        renameConflictFile(stwa, sti, pair_a_path);
                        sync_result = copyFileInternalToInternal(stwa, sti, pair_b_path, pair_a_path);
                    }
                } else if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_OLDER)) {
                    if (pair_a_last_mod < pair_b_last_mod) {
                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                                "競合ファイルを検出しましたがオプションにより古いファイルを優先します。上書きされるファイル="+pair_b_path);
                        renameConflictFile(stwa, sti, pair_b_path);
                        sync_result = copyFileInternalToInternal(stwa, sti, pair_a_path, pair_b_path);
                    } else {
                        SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                                "競合ファイルを検出しましたがオプションにより古いファイルを優先します。上書きされるファイル="+pair_a_path);
                        renameConflictFile(stwa, sti, pair_a_path);
                        sync_result = copyFileInternalToInternal(stwa, sti, pair_b_path, pair_a_path);
                    }
                } else if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_SOURCE_TO_DESTINATION)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                            "競合ファイルを検出しましたがオプションによりマスターからターゲットにファイルをコピーします。マスター="+pair_a_path+", ターゲット="+pair_b_path);
                    renameConflictFile(stwa, sti, pair_b_path);
                    sync_result=copyFileInternalToInternal(stwa, sti, pair_a_path, pair_b_path);
                } else if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_DESTINATION_TO_SOURCE)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                            "競合ファイルを検出しましたがオプションによりターゲットからマスターにファイルをコピーします。マスター="+pair_a_path+", ターゲット="+pair_b_path);
                    renameConflictFile(stwa, sti, pair_a_path);
                    sync_result=copyFileInternalToInternal(stwa, sti, pair_b_path, pair_a_path);
                } else if (sti.getSyncTwoWayConflictFileRule().equals(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_SKIP_SYNC_FILE)) {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", "", "",
                            "競合ファイルを検出しましたがオプションにより競合ファイルを無視しました。マスター="+pair_a_path+", ターゲット="+pair_b_path);
                }
            } else {
                stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " The file has been detected to have a difference.");
                //Fileをコピー
                if (pair_a_last_mod>pair_b_last_mod) {
                    //Pair A to Pair B
                    if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_COPY, "", pair_b_path)) {
                        sync_result=copyFileInternalToInternal(stwa, sti, pair_a_path, pair_b_path);
                    } else {
                        stwa.util.addLogMsg("W", sti.getSyncTaskName(), pair_b_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                    }
                } else if (pair_a_last_mod<pair_b_last_mod) {
                    //Pair B to Pair A
                    if (SyncThread.sendConfirmRequest(stwa, sti, CONFIRM_REQUEST_COPY, "", pair_a_path)) {
                        sync_result=copyFileInternalToInternal(stwa, sti, pair_b_path, pair_a_path);
                    } else {
                        stwa.util.addLogMsg("W", sti.getSyncTaskName(), pair_a_path, " "+stwa.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel));
                    }
                }
            }
        } else {
        }
        return sync_result;
    }

    static private void renameConflictFile(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String fp) {
        sti.setSyncTwoWayKeepConflictFile(true);
        if (sti.isSyncTwoWayKeepConflictFile() && !sti.isSyncTestMode()) {
            SafFile3 tmp=new SafFile3(stwa.appContext, fp);
            String new_fp="";
            if (fp.length()>=(255-33)) {
                new_fp=tmp.getPath().substring(0, (255-34))+ "." + Long.valueOf(System.currentTimeMillis()) + SyncTaskItem.SYNC_TASK_TWO_WAY_CONFLICT_FILE_SUFFIX;
            } else {
                new_fp=tmp.getPath()+ "." + Long.valueOf(System.currentTimeMillis()) + SyncTaskItem.SYNC_TASK_TWO_WAY_CONFLICT_FILE_SUFFIX;
            }
            SafFile3 to_sf=new SafFile3(stwa.appContext, new_fp);
            tmp.renameTo(to_sf);
            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", fp, "",
                    "競合ファイルを保存しました。競合ファイル="+fp+", 保存されたファイル="+to_sf.getPath());
        }
    }

    static private int deleteLocalItem(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 pair_a_file, SafFile3 pair_b_file) {
//        stwa.util.addDebugMsg(1, "I", "deleteLocalItem entered. path="+pair_a_file.getPath());
        int sync_status=0;
        if (pair_a_file.exists()) {
            if (pair_a_file.isDirectory()) {
                SafFile3[] fl=pair_a_file.listFiles();
                for(SafFile3 del_child:fl) {
                    SafFile3 pair_b_child=new SafFile3(stwa.appContext, pair_b_file.getPath()+"/"+del_child.getName());
                    if (del_child.isDirectory()) {
                        sync_status= deleteLocalItem(stwa, sti, del_child, pair_b_child);
                        if (sync_status== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                            removeTwoWaySyncFileInfoItemByDirectory(stwa, sti, del_child.getPath(), del_child.getPath());
                        } else {
                            return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                        }
                    } else {
                        boolean del_result=del_child.delete();
                        if (del_result) {
                            stwa.totalDeleteCount++;
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", del_child.getPath(), del_child.getName(),
                                    stwa.appContext.getString(R.string.msgs_mirror_task_file_deleted));
                        } else {
                            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "E", del_child.getPath(), del_child.getName(),
                                    stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed));
                            sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                        }
                    }
                }
                boolean del_result=pair_a_file.delete();
                if (del_result) {
                    stwa.totalDeleteCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", pair_a_file.getPath(), pair_a_file.getName(),
                            stwa.appContext.getString(R.string.msgs_mirror_task_dir_deleted));
                    removeTwoWaySyncFileInfoItemByDirectory(stwa, sti, pair_a_file.getPath(), pair_b_file.getPath());
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "E", pair_a_file.getPath(), pair_a_file.getName(),
                            stwa.appContext.getString(R.string.msgs_mirror_task_dir_delete_failed));
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            } else {
                boolean result=pair_a_file.delete();
                if (result) {
                    stwa.totalDeleteCount++;
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", pair_a_file.getPath(), pair_a_file.getName(),
                            stwa.appContext.getString(R.string.msgs_mirror_task_file_deleted));
                } else {
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "E", pair_a_file.getPath(), pair_a_file.getName(),
                            stwa.appContext.getString(R.string.msgs_mirror_task_file_delete_failed));
                    sync_status= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
                }
            }
        }
        return sync_status;
    }

    static private void removeTwoWaySyncFileInfoItem(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        TwoWaySyncFileInfoItem from_item=getSyncFileInfo(stwa, sti, from_path);
        TwoWaySyncFileInfoItem to_item=getSyncFileInfo(stwa, sti, to_path);
        if (from_item!=null) {
            boolean rc=stwa.currSyncFileInfoList.remove(from_item);
            if (rc) stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " from item was removed. fp="+from_item.getFilePath());
        }
        if (to_item!=null) {
            boolean rc=stwa.currSyncFileInfoList.remove(to_item);
            if (rc) stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " to item was removed. fp="+to_item.getFilePath());
        }
    }

    static private void removeTwoWaySyncFileInfoItemByDirectory(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        ArrayList<TwoWaySyncFileInfoItem> del_new=new ArrayList<TwoWaySyncFileInfoItem>();
        ArrayList<TwoWaySyncFileInfoItem> del_curr=new ArrayList<TwoWaySyncFileInfoItem>();
        for(TwoWaySyncFileInfoItem item:stwa.newSyncFileInfoList) {
            if (item.getFilePath().startsWith(from_path) || item.getFilePath().startsWith(to_path)) {
                del_new.add(item);
                stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " item was removed. fp="+item.getFilePath());
            }
        }
        for(TwoWaySyncFileInfoItem item:stwa.currSyncFileInfoList) {
            if (item.getFilePath().startsWith(from_path) || item.getFilePath().startsWith(to_path)) {
                del_curr.add(item);
                stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " item was removed. fp="+item.getFilePath());
            }
        }
        stwa.newSyncFileInfoList.removeAll(del_new);
        stwa.currSyncFileInfoList.removeAll(del_curr);
    }

    static private int copyFileInternalToInternal(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String from_path, String to_path) {
        stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " entered, in="+from_path+", out="+to_path);
        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        if (from_path.endsWith(SyncTaskItem.SYNC_TASK_TWO_WAY_CONFLICT_FILE_SUFFIX)) {
            stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " copy ignored because file is conflict keep file.");
            return sync_result;
        }
//        TwoWaySyncFileInfoItem sfii= getSyncFileInfo(stwa, sti, to_path);
////        if (sfii!=null && (sfii.isDeleteRequired())) {
//        if (sfii!=null) {
//            SafFile3 tf=new SafFile3(stwa.appContext, to_path);
//            if (!tf.exists()) {
//                SafFile3 sf=new SafFile3(stwa.appContext, from_path);
//                boolean rc=sf.delete();
//                removeTwoWaySyncFileInfoItem(stwa, sti, from_path, to_path);
//                int dc=sf.getParentFile().getCount();
//                stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " file deleted because pair file was deleted. fp="+from_path+", dir count="+dc);
//                if (dc==0) {
//                    SafFile3 parent_dir=sf.getParentFile();
//                    boolean del_result=parent_dir.delete();
//                    if (del_result) stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " directory deleted because pair directory was deleted. dir="+parent_dir.getPath());
//                    else {
//                        stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + " directory delete failed. dir="+parent_dir.getPath());
//                        stwa.util.addDebugMsg(2,"I",CommonUtilities.getExecutedMethodName() + "   error="+parent_dir.getLastErrorMessage());
//                        sync_result=TaskListItem.SYNC_STATUS_ERROR;
//                    }
//                }
//                return sync_result;
//            }
//        }
        if (!sti.isSyncTestMode()) {
            //Write mode
            String dest_path=to_path;
            SafFile3 in_file=new SafFile3(stwa.appContext, from_path);
            SafFile3 df=new SafFile3(stwa.appContext, to_path);
            SafFile3 out_temp=new SafFile3(stwa.appContext, df.getAppDirectoryCache()+"/"+df.getName());
            out_temp.deleteIfExists();
            try {
                out_temp.createNewFile();
            }catch(Exception e) {
                e.printStackTrace();
            }
            InputStream fis=null;
            OutputStream fos=null;
            try {
                fis=in_file.getInputStream();
                fos=out_temp.getOutputStream();
                sync_result=copyFile(stwa, sti, dest_path, in_file.getName(), in_file.length(), fos, fis);
                if (sync_result== SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS) {
                    out_temp.setLastModified(in_file.lastModified());
                    SafFile3 parent_dir=df.getParentFile();
                    if (!parent_dir.exists()) {
                        boolean created=parent_dir.mkdirs();
                        if (stwa.util.getLogLevel() >= 2)
                            stwa.util.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName(), " directory created="+created+", dir="+parent_dir.getPath());
                    }
                    boolean dest_file_exists=df.exists();
                    if (dest_file_exists) df.delete();
                    out_temp.moveTo(df);

                    String tmsg = dest_file_exists ? stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced) : stwa.appContext.getString(R.string.msgs_mirror_task_file_copied);
                    SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", dest_path, in_file.getName(), tmsg);
                    stwa.totalCopyCount++;
                    if (dest_file_exists) stwa.totalReplaceCount++;
                } else {
                    if (out_temp.exists()) out_temp.delete();
                }
                updateSyncFileInfo(stwa, sti, in_file, df);
            } catch(Exception e) {
                if (fis!=null) try {fis.close();} catch(IOException e1) {}
                if (fos!=null) try {fos.close();} catch(IOException e1) {}
                if (out_temp.exists()) out_temp.delete();
                putExceptionMessage(stwa, e, e.getMessage());
            }
        } else {
            //Test mode
            SafFile3 df=new SafFile3(stwa.appContext, to_path);
            boolean dest_file_exists=df.exists();
            String tmsg = dest_file_exists ? stwa.appContext.getString(R.string.msgs_mirror_task_file_replaced) : stwa.appContext.getString(R.string.msgs_mirror_task_file_copied);
            SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "I", to_path, df.getName(), tmsg);
        }
        return sync_result;
    }

    private final static int SHOW_PROGRESS_THRESHOLD_VALUE = 1024 * 1024 * 4;
    private final static int IO_AREA_SIZE = 1024 * 1024;
    public final static int LARGE_BUFFERED_STREAM_BUFFER_SIZE = 1024 * 1024 * 4;

    static private int copyFile(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String to_path, String to_name,
                                long file_size, OutputStream fos, InputStream fis) {
        long read_begin_time = System.currentTimeMillis();
        int sync_result=0;

        int buffer_size=LARGE_BUFFERED_STREAM_BUFFER_SIZE;//, io_area_size=IO_AREA_SIZE;
        boolean show_prog = (file_size > SHOW_PROGRESS_THRESHOLD_VALUE);
        if (sti.isSyncOptionUseSmallIoBuffer() && sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            buffer_size=1024*16-1;
//            io_area_size=1024*16-1;
            show_prog=(file_size > 1024*64);
        }

        try {
            byte[] buff=new byte[buffer_size];
            int rc=0;
            long file_read_bytes = 0;
            while((rc=fis.read(buff))>0) {
                if (!stwa.gp.syncThreadCtrl.isEnabled()) {
                    fis.close();
                    fos.flush();
                    fos.close();
                    return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
                };
                fos.write(buff, 0, rc);
                file_read_bytes += rc;
                if (show_prog && file_size > file_read_bytes) {
                    SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(), to_name + " " +
                            String.format(stwa.appContext.getString(R.string.msgs_mirror_task_file_copying), (file_read_bytes * 100) / file_size));
                }

            }
            fis.close();
            fos.flush();
            fos.close();

            long file_read_time = System.currentTimeMillis() - read_begin_time;

            if (stwa.util.getLogLevel() >= 1)
                stwa.util.addDebugMsg(1, "I", to_path + " " + file_read_bytes + " bytes transfered in ",file_read_time + " mili seconds at " +
                        SyncThread.calTransferRate(file_read_bytes, file_read_time));
            stwa.totalTransferByte += file_read_bytes;
            stwa.totalTransferTime += file_read_time;
        } catch(IOException e) {
            try {fis.close();} catch(IOException e1) {}
            try {fos.close();} catch(IOException e1) {}
            putExceptionMessage(stwa, e, e.getMessage());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    private static final int FILE_WAS_NOT_CHANGED=0;
    private static final int FILE_WAS_CONFLICT_BY_TIME=1;
    private static final int FILE_WAS_CONFLICT_BY_LENGTH=2;
    private static final int FILE_WAS_CHANGED=3;
    static private int isFileChanged(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti,
                                     String pair_a_path, long pair_a_length, long pair_a_last_mod,
                                     String pair_b_path, long pair_b_length, long pair_b_last_mod) {
        int result=0;

        TwoWaySyncFileInfoItem pair_a_sfi= getSyncFileInfo(stwa, sti, pair_a_path);
//        TwoWaySyncFileInfoItem pair_b_sfi= getSyncFileInfo(stwa, sti, pair_b_path);
        boolean file_found=false;
        if (pair_a_sfi==null) {
            updateSyncFileInfo(stwa, sti, pair_a_path, false, System.currentTimeMillis(), pair_a_length, pair_a_last_mod,
                                          pair_b_path, false, System.currentTimeMillis(), pair_b_length, pair_b_last_mod);
            pair_a_sfi=new TwoWaySyncFileInfoItem(false, false, false, 0L, pair_a_path, pair_a_length, pair_a_last_mod);
//                    false, false, 0L, pair_b_path, pair_b_length, pair_b_last_mod);
        } else {
            file_found=true;
            pair_a_sfi.setReferenced(true);
        }

//        long diff_length_a_b=Math.abs(a_length-b_length);
        long diff_last_mod_a_b=Math.abs(pair_a_last_mod-pair_b_last_mod);

//        long diff_length_s_b=Math.abs(sfi.fileSize-b_length);
        long diff_last_mod_s_b=Math.abs(pair_a_sfi.getFileLastModified()-pair_b_last_mod);

        long allowed_time=0;//(long)sti.getSyncOptionDifferentFileAllowableTime();

//        boolean is_length_diff_a_b=diff_length_a_b!=0;
        boolean is_last_mod_diff_a_b=diff_last_mod_a_b>allowed_time;

//        boolean is_length_diff_s_b=diff_length_s_b!=0;
        boolean is_last_mod_diff_s_b=diff_last_mod_s_b>allowed_time;

//        boolean is_pair_a_saved_changed_length =a_length==sfi.fileSize;
        boolean is_pair_a_saved_changed_last_mod =pair_a_last_mod!=pair_a_sfi.getFileLastModified();

//        boolean is_pair_b_saved_changed_length =b_length==sfi.fileSize;
        boolean is_pair_b_saved_changed_last_mod =Math.abs(pair_b_last_mod-pair_a_sfi.getFileLastModified())>allowed_time;

        if (stwa.util.getLogLevel() >= 2) {
            stwa.util.addDebugMsg(2,"I","isFileChanged File attrinute.");
            stwa.util.addDebugMsg(2,"I","   Pair_A="+pair_a_path+", Length="+pair_a_length+", LastModified="+pair_a_last_mod);
            stwa.util.addDebugMsg(2,"I","   Pair_B="+pair_b_path+", Length="+pair_b_length+", LastModified="+pair_b_last_mod);
            stwa.util.addDebugMsg(2,"I","   Last sync Pair_A="+pair_a_sfi.getFilePath()+", Length="+pair_a_sfi.getFileSize()+", LastModified="+pair_a_sfi.getFileLastModified()+", found="+file_found);
        }
        if (is_last_mod_diff_a_b) {
            //Difference file detected
            if (is_pair_a_saved_changed_last_mod && is_pair_b_saved_changed_last_mod) {
                //競合ファイルが検出された
                result=FILE_WAS_CONFLICT_BY_TIME;
                stwa.util.addDebugMsg(2,"I", CommonUtilities.getExecutedMethodName(), " A conflict file has been detected.(Time)");
            } else {
                result=FILE_WAS_CHANGED;
                stwa.util.addDebugMsg(2,"I", CommonUtilities.getExecutedMethodName(), " The file has been detected to have a difference.");
                //Fileをコピー
            }
        } else {
            if (pair_a_length!=pair_b_length) {
                result=FILE_WAS_CONFLICT_BY_LENGTH;
//                stwa.util.addLogMsg("W","The last modified time is the same, but the file size is different and cannot be synchronized. Copy the file manually or delete the files you don't need.");
                if (stwa.util.getLogLevel() >= 2) {
                    stwa.util.addDebugMsg(2,"W","Last update time is the same, but file size is different.");
                    stwa.util.addDebugMsg(2,"I","   Pair_A="+pair_a_path+", Length="+pair_a_length+", " +
                            "LastModified="+ StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(pair_a_last_mod));
                    stwa.util.addDebugMsg(2,"I","   Pair_B="+pair_b_path+", Length="+pair_b_length+", " +
                            "LastModified="+ StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(pair_b_last_mod));
                }
            }
        }
        stwa.util.addDebugMsg(2,"I", CommonUtilities.getExecutedMethodName(), " result="+result+", Path=",pair_a_path);
        return result;
    }

    static private void putExceptionMessage(SyncThread.SyncThreadWorkArea stwa, Exception e, String e_msg) {
        String st_msg=MiscUtil.getStackTraceString(e);
        stwa.util.addLogMsg("E",stwa.currentSTI.getSyncTaskName()," Error="+e_msg+st_msg);
    }

    public static void showConfirmDialogConflict(GlobalParameters gp, CommonUtilities cu, ISvcClient sc, final String method, String msg,
                                                 final String pair_a_path, final long pair_a_length, final long pair_a_last_mod,
                                                 final String pair_b_path, final long pair_b_length, final long pair_b_last_mod) {
        cu.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        gp.confirmOverrideView.setVisibility(LinearLayout.GONE);
        gp.confirmConflictView.setVisibility(LinearLayout.VISIBLE);
        gp.confirmDialogShowed = true;
        gp.confirmDialogFilePathPairA = pair_a_path;
        gp.confirmDialogFileLengthPairA = pair_a_length;
        gp.confirmDialogFileLastModPairA = pair_a_last_mod;
        gp.confirmDialogFilePathPairB = pair_b_path;
        gp.confirmDialogFileLengthPairB = pair_b_length;
        gp.confirmDialogFileLastModPairB = pair_b_last_mod;
        gp.confirmDialogMethod = method;
        gp.confirmDialogMessage = msg;
        gp.confirmDialogConflictFilePathA.setText(gp.confirmDialogFilePathPairA);
        gp.confirmDialogConflictFileLengthA.setText(String.format("%,d",gp.confirmDialogFileLengthPairA)+" bytes");
        gp.confirmDialogConflictFileLastModA.setText(StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(gp.confirmDialogFileLastModPairA));
        gp.confirmDialogConflictFilePathB.setText(gp.confirmDialogFilePathPairB);
        gp.confirmDialogConflictFileLengthB.setText(String.format("%,d",gp.confirmDialogFileLengthPairB)+" bytes");
        gp.confirmDialogConflictFileLastModB.setText(StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(gp.confirmDialogFileLastModPairB));

        gp.confirmView.setVisibility(LinearLayout.VISIBLE);
        gp.confirmView.setBackgroundColor(gp.themeColorList.title_background_color);
        gp.confirmView.bringToFront();
        gp.confirmMsg.setText(msg);

        // Select Aボタンの指定
        gp.confirmDialogConflictButtonSelectAListener = new View.OnClickListener() {
            public void onClick(View v) {
                gp.confirmView.setVisibility(LinearLayout.GONE);
                ActivityMain.sendConfirmResponse(gp, sc, CONFIRM_CONFLICT_RESP_SELECT_A);
            }
        };
        gp.confirmDialogConflictButtonSelectA.setOnClickListener(gp.confirmDialogConflictButtonSelectAListener);

        // Select Bボタンの指定
        gp.confirmDialogConflictButtonSelectBListener = new View.OnClickListener() {
            public void onClick(View v) {
                gp.confirmView.setVisibility(LinearLayout.GONE);
                ActivityMain.sendConfirmResponse(gp, sc, CONFIRM_CONFLICT_RESP_SELECT_B);
            }
        };
        gp.confirmDialogConflictButtonSelectB.setOnClickListener(gp.confirmDialogConflictButtonSelectBListener);

        // Ignoreボタンの指定
        gp.confirmDialogConflictButtonSyncIgnoreFileListener = new View.OnClickListener() {
            public void onClick(View v) {
                gp.confirmView.setVisibility(LinearLayout.GONE);
                ActivityMain.sendConfirmResponse(gp, sc, CONFIRM_CONFLICT_RESP_NO);
            }
        };
        gp.confirmDialogConflictButtonSyncIgnoreFile.setOnClickListener(gp.confirmDialogConflictButtonSyncIgnoreFileListener);

        // Ignoreボタンの指定
        gp.confirmDialogConflictButtonCancelSyncTaskListener = new View.OnClickListener() {
            public void onClick(View v) {
                gp.confirmView.setVisibility(LinearLayout.GONE);
                ActivityMain.sendConfirmResponse(gp, sc, CONFIRM_CONFLICT_RESP_CANCEL);
            }
        };
        gp.confirmDialogConflictButtonCancelSyncTask.setOnClickListener(gp.confirmDialogConflictButtonCancelSyncTaskListener);

    }

    public final static int TWOWAY_SYNC_CONFIRM_RESULT_IGNORE=0;
    public final static int TWOWAY_SYNC_CONFIRM_RESULT_SELECT_A=1;
    public final static int TWOWAY_SYNC_CONFIRM_RESULT_SELECT_B=2;
    public final static int TWOWAY_SYNC_CONFIRM_RESULT_SYNC_ABORT=3;
    static final public int sendTwoWaySyncConfirmRequest(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String msg,
                                                         String pair_a_path, long pair_a_length, long pair_a_last_mod,
                                                         String pair_b_path, long pair_b_length, long pair_b_last_mod) {
        int result = TWOWAY_SYNC_CONFIRM_RESULT_IGNORE;
        if (stwa.util.getLogLevel() >= 2) {
            stwa.util.addDebugMsg(2, "I", "sendTwoWaySyncConfirmRequest entered PairA=", pair_a_path+", PairB="+pair_b_path);
        }
        String type=CONFIRM_REQUEST_CONFLICT_FILE;
        try {
            NotificationUtil.showOngoingMsg(stwa.gp, stwa.util, 0, msg);
            stwa.gp.confirmDialogShowed = true;
            stwa.gp.confirmDialogMethod = type;
            stwa.gp.syncThreadConfirm.initThreadCtrl();
            stwa.gp.releaseWakeLock(stwa.util);
            if (stwa.gp.callbackStub != null) {
                stwa.gp.callbackStub.cbShowConfirmDialog(type, msg, pair_a_path, pair_a_length, pair_a_last_mod,
                        pair_b_path, pair_b_length, pair_b_last_mod);
            }
            synchronized (stwa.gp.syncThreadConfirm) {
                stwa.gp.syncThreadConfirmWait = true;
                stwa.gp.syncThreadConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
                stwa.gp.syncThreadConfirmWait = false;
            }
            stwa.gp.acquireWakeLock(stwa.appContext, stwa.util);
            if (stwa.gp.syncThreadConfirm.getExtraDataInt()==CONFIRM_CONFLICT_RESP_CANCEL) result=TWOWAY_SYNC_CONFIRM_RESULT_SYNC_ABORT;
            else if (stwa.gp.syncThreadConfirm.getExtraDataInt()==CONFIRM_CONFLICT_RESP_SELECT_A) result=TWOWAY_SYNC_CONFIRM_RESULT_SELECT_A;
            else if (stwa.gp.syncThreadConfirm.getExtraDataInt()==CONFIRM_CONFLICT_RESP_SELECT_B) result=TWOWAY_SYNC_CONFIRM_RESULT_SELECT_B;
        } catch (RemoteException e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), "RemoteException occured");
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        } catch (InterruptedException e) {
            stwa.util.addLogMsg("E", sti.getSyncTaskName(), "InterruptedException occured");
            SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        }

        if (result==TWOWAY_SYNC_CONFIRM_RESULT_SYNC_ABORT) stwa.gp.syncThreadCtrl.setDisabled();
        if (stwa.util.getLogLevel() >= 2)
            stwa.util.addDebugMsg(2, "I", "sendConfirmRequest result=" + result);

        return result;
    }

    static private TwoWaySyncFileInfoItem getSyncFileInfo(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String fpath) {
        TwoWaySyncFileInfoItem srch=new TwoWaySyncFileInfoItem();
        srch.setFilePath(fpath);
        int idx=Collections.binarySearch(stwa.currSyncFileInfoList, srch, new Comparator<TwoWaySyncFileInfoItem>() {
            @Override
            public int compare(TwoWaySyncFileInfoItem o1, TwoWaySyncFileInfoItem o2) {
                return o1.getFilePath().compareToIgnoreCase(o2.getFilePath());
            }
        });
        TwoWaySyncFileInfoItem result=null;
        if (idx<0) {
            for(TwoWaySyncFileInfoItem item:stwa.newSyncFileInfoList) {
                if (item.getFilePath().equals(srch.getFilePath())) {
                    result=item;
                    break;
                }
            }
        } else {
            result=stwa.currSyncFileInfoList.get(idx);
        }
        return result;
    }

    static private void updateSyncFileInfo(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, String fpath_a, String fpath_b) {
        SafFile3 lf_a=new SafFile3(stwa.appContext, fpath_a);
        SafFile3 lf_b=new SafFile3(stwa.appContext, fpath_b);
        if (lf_a.exists() && lf_b.exists()) {
            updateSyncFileInfo(stwa, sti, fpath_a, lf_a.isDirectory(), System.currentTimeMillis(), lf_a.length(), lf_a.lastModified(),
                                          fpath_b, lf_b.isDirectory(), System.currentTimeMillis(), lf_b.length(), lf_b.lastModified());
        } else {
            stwa.util.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName() + " update failed because file does not exists. FP="+fpath_a);
        }
    }

    static private void updateSyncFileInfo(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 lf_a, SafFile3 lf_b) {
        if (lf_a.exists() && lf_b.exists()) {
            updateSyncFileInfo(stwa, sti, lf_a.getPath(), lf_a.isDirectory(), System.currentTimeMillis(), lf_a.length(), lf_a.lastModified(),
                                          lf_b.getPath(), lf_b.isDirectory(), System.currentTimeMillis(), lf_b.length(), lf_b.lastModified());
        } else {
            stwa.util.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName() + " update failed because file does not exists. FP="+lf_a.getPath());
        }
    }

    static private void updateSyncFileInfo(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti,
                                           String a_fpath, boolean a_directory, long a_sync_time, long a_file_size, long a_file_last_modified,
                                           String b_fpath, boolean b_directory, long b_sync_time, long b_file_size, long b_file_last_modified) {
        TwoWaySyncFileInfoItem a_item=getSyncFileInfo(stwa, sti, a_fpath);
        TwoWaySyncFileInfoItem b_item=getSyncFileInfo(stwa, sti, b_fpath);
        if (a_item!=null) {
            a_item.setFileLastModified(a_file_last_modified);
            a_item.setFileSize(a_file_size);
            a_item.setLastSyncTime(a_sync_time);
        } else {
            TwoWaySyncFileInfoItem new_item=new TwoWaySyncFileInfoItem();
            new_item.setFilePath(a_fpath);
            new_item.setFileLastModified(a_file_last_modified);
            new_item.setFileSize(a_file_size);
            new_item.setLastSyncTime(a_sync_time);
            new_item.setDirectory(a_directory);
            stwa.newSyncFileInfoList.add(new_item);
        }

        if (b_item!=null) {
            b_item.setFileLastModified(b_file_last_modified);
            b_item.setFileSize(b_file_size);
            b_item.setLastSyncTime(b_sync_time);
        } else {
            TwoWaySyncFileInfoItem new_item=new TwoWaySyncFileInfoItem();
            new_item.setFilePath(b_fpath);
            new_item.setFileLastModified(b_file_last_modified);
            new_item.setFileSize(b_file_size);
            new_item.setLastSyncTime(b_sync_time);
            new_item.setDirectory(b_directory);
            stwa.newSyncFileInfoList.add(new_item);
        }

    }

    static public final String TWOWAY_SYNC_FILE_MANAGEMENT_FILE_NAME=".twoway_sync_file_list";

    static public void saveSyncFileInfoList(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti) {
        if (stwa.newSyncFileInfoList.size()>0) {//merge current list
            stwa.currSyncFileInfoList.addAll(stwa.newSyncFileInfoList);
            Collections.sort(stwa.currSyncFileInfoList, new Comparator<TwoWaySyncFileInfoItem>() {
                @Override
                public int compare(TwoWaySyncFileInfoItem o1, TwoWaySyncFileInfoItem o2) {
                    return o1.getFilePath().compareToIgnoreCase(o2.getFilePath());
                }
            });
            stwa.newSyncFileInfoList.clear();
        }

        try {
            SafFile3 sf=new SafFile3(stwa.appContext, stwa.gp.settingAppManagemsntDirectoryName+"/"+TWOWAY_SYNC_FILE_MANAGEMENT_FILE_NAME);
            sf.deleteIfExists();
            sf.createNewFile();
            OutputStream fos = sf.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 1024);
            ZipOutputStream zos = new ZipOutputStream(bos);
            ZipEntry ze = new ZipEntry("sync_list.txt");
            zos.putNextEntry(ze);
            OutputStreamWriter osw = new OutputStreamWriter(zos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw, 1024 * 1024 * 4);

            StringBuffer pl = new StringBuffer(512);
            String last_fp = "";
            String new_fp = "";
            for(TwoWaySyncFileInfoItem item:stwa.currSyncFileInfoList) {
                new_fp = item.getFilePath();
                if (!last_fp.equals(new_fp)) {
                    pl.append(new_fp)
                            .append("\t")
                            .append(String.valueOf(item.getLastSyncTime()))
                            .append("\t")
                            .append(String.valueOf(item.getFileSize()))
                            .append("\t")
                            .append(String.valueOf(item.getFileLastModified()))
                            .append("\t")
                            .append(item.isDirectory()?"1":"0")
                            .append("\n");

                    bw.append(pl);
                    pl.setLength(0);
                } else {
                }
            }

            bw.flush();
            bw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void loadSyncFileInfoList(SyncThread.SyncThreadWorkArea stwa, SyncTaskItem sti) {
        try {
            SafFile3 sf=new SafFile3(stwa.appContext, stwa.gp.settingAppManagemsntDirectoryName+"/"+TWOWAY_SYNC_FILE_MANAGEMENT_FILE_NAME);
            InputStream fis = sf.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 1024);
            ZipInputStream zis = new ZipInputStream(bis);
            zis.getNextEntry();
            InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
            BufferedReader br = new BufferedReader(isr, 1024 * 1024 * 4);

            String line = null;

            ArrayList<TwoWaySyncFileInfoItem> fl=new ArrayList<TwoWaySyncFileInfoItem>();

            String[] l_array = null;
            String last_fp = "";
            while ((line = br.readLine()) != null) {
                l_array = line.split("\t");
                if (l_array != null && l_array.length >= 5) {
                    if (!last_fp.equals(l_array[0])) {
                        TwoWaySyncFileInfoItem item=new TwoWaySyncFileInfoItem(
                                false, false, false, Long.valueOf(l_array[1]), l_array[0], Long.valueOf(l_array[2]), Long.valueOf(l_array[3]));
                        item.setDirectory(l_array[4].equals("1")?true:false);
                        fl.add(item);
                        last_fp = l_array[0];
                    } else {
                        stwa.util.addDebugMsg(1,"W",CommonUtilities.getExecutedMethodName() + " duplicate entry detected, fp="+l_array[0]);
                    }
                }
            }
            fis.close();

            stwa.currSyncFileInfoList.addAll(fl);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

