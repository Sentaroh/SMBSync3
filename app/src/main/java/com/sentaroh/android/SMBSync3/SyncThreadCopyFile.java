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

import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sentaroh.android.SMBSync3.SyncThread.SyncThreadWorkArea;
import com.sentaroh.android.Utilities3.SafFile3;

import static com.sentaroh.android.SMBSync3.Constants.SYNC_IO_BUFFER_SIZE;

public class SyncThreadCopyFile {


    static public int copyFileLocalToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 mf, SafFile3 tf) {
        int sync_result=0;
        if (stwa.lastModifiedIsFunctional) sync_result= copyFileLocalToLocalSetLastModified(stwa, sti, mf, tf);
            else sync_result= copyFileLocalToLocalUnsetLastModified(stwa, sti, mf, tf);
            return sync_result;
    }

    static private int copyFileLocalToLocalUnsetLastModified(SyncThreadWorkArea stwa, SyncTaskItem sti,
                                                             SafFile3 mf, SafFile3 tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());

        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile().getPath());

            String to_file_temp = tf.getPath()+".tmp";
            SafFile3 t_df = new SafFile3(stwa.appContext, to_file_temp);
            t_df.createNewFile();

            int result=copyFile(stwa, sti, mf.getParentFile().getPath(), tf.getParentFile().getPath(), mf.getName(), mf.length(), mf.getInputStream(), t_df.getOutputStream());
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                t_df.deleteIfExists();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp="+tf.getPath()+
                    ", destination="+t_df.lastModified()+", source="+mf.lastModified()+", destination_size="+t_df.length()+", source_size="+mf.length());

            tf.deleteIfExists();
            if (!t_df.renameTo(mf)) {
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "SafFile3 renameTo Error="+t_df.getLastErrorMessage());
                t_df.deleteIfExists();
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
            SyncThread.scanMediaFile(stwa, sti, mf.getPath());
        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;

    }

    static private int copyFileLocalToLocalSetLastModified(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 mf, SafFile3 tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());
        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;


        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            String to_file_temp=tf.getAppDirectoryCache()+"/"+mf.getName();//System.currentTimeMillis();//mf.getName();

            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile().getPath());

            InputStream is =null;
            long m_saf_length=-1;
            is = mf.getInputStream();

            OutputStream os =null;
            File temp_file=new File(to_file_temp);
            os=new FileOutputStream(temp_file);//stwa.appContext.getContentResolver().openOutputStream(temp_sf.getUri());

            int result=copyFile(stwa, sti, mf.getParentFile().getPath(), tf.getParentFile().getPath(), mf.getName(), mf.length(), mf.getInputStream(), os);
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                if (temp_file.exists()) temp_file.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }

            SafFile3 temp_sf=new SafFile3(stwa.appContext, to_file_temp);
            try {
                temp_file.setLastModified(mf.lastModified());
            } catch(Exception e) {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", tf.getPath(), mf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_file_set_last_modified_failed));
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "Error="+e.getMessage());
            }
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp="+to_file_temp+
                    ", destination="+temp_sf.lastModified()+", source="+mf.lastModified()+", destination_size="+temp_sf.length()+", source_size="+mf.length()+
                    ", m_saf_size="+m_saf_length);

            tf.deleteIfExists();
            if (!temp_sf.moveTo(tf)){//WithRename(tf)) {
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "SafFile3 moveTo Error="+temp_sf.getLastErrorMessage());
                if (temp_file.exists()) temp_file.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
            SyncThread.scanMediaFile(stwa, sti, mf.getPath());
        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static public int copyFileLocalToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, SafFile3 mf, JcifsFile tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());

        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            String to_file_temp = tf.getPath()+"."+ System.currentTimeMillis()+".tmp";//"/temp.tmp";
            SyncThread.createDirectoryToSmb(stwa, sti, tf.getParent(), stwa.destinationAuth);

            JcifsFile temp_out=new JcifsFile(to_file_temp, stwa.destinationAuth);
            InputStream is=mf.getInputStream();
            OutputStream os = temp_out.getOutputStream();

            int result=copyFile(stwa, sti, mf.getParentFile().getPath(), tf.getParent(), mf.getName(),
                    mf.length(), mf.getInputStream(), os);
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                if (tf.exists()) tf.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }

            try {
                if (!sti.isSyncDoNotResetFileLastModified()) temp_out.setLastModified(mf.lastModified());
            } catch(JcifsException e) {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", tf.getPath(), mf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_file_set_last_modified_failed));
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "Error="+e.getMessage());
            }
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp=",tf.getPath(),
                    ", destination="+temp_out.getLastModified(),", source="+mf.lastModified(),", destination_size="+temp_out.length(),", source_size="+mf.length());
            if (tf.exists()) tf.delete();
            temp_out.renameTo(tf);
        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static private void putErrorMessage(SyncThreadWorkArea stwa, SyncTaskItem sti, Exception e, String from_path, String to_path) {
        e.printStackTrace();
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
        if (e.getCause()!=null) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getCause().toString());

        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

    static private void putErrorMessageJcifs(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsException e, String from_path, String to_path) {
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "",
                CommonUtilities.getExecutedMethodName() + " From=" + from_path + ", To=" + to_path);
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getMessage());
        SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", "NT Status="+ String.format("0x%8x",e.getNtStatus()));

        if (e.getCause()!=null) SyncThread.showMsg(stwa, true, sti.getSyncTaskName(), "I", "", "", e.getCause().toString());
        stwa.jcifsNtStatusCode=e.getNtStatus();
        SyncThread.printStackTraceElement(stwa, e.getStackTrace());
        stwa.gp.syncThreadCtrl.setThreadMessage(e.getMessage());
    }

    static public int copyFileSmbToSmb(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile mf, JcifsFile tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());

        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        String to_file_temp = tf.getPath()+"."+ System.currentTimeMillis()+".tmp";//"/temp.tmp";

        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            JcifsFile temp_out = new JcifsFile(to_file_temp, stwa.destinationAuth);
            SyncThread.createDirectoryToSmb(stwa, sti, tf.getParent(), stwa.destinationAuth);

            int result=copyFile(stwa, sti, mf.getParent(), tf.getParent(), mf.getName(),
                    mf.length(), mf.getInputStream(), temp_out.getOutputStream());
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                if (temp_out.exists()) temp_out.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }

            try {
                if (!sti.isSyncDoNotResetFileLastModified()) temp_out.setLastModified(mf.getLastModified());
            } catch(JcifsException e) {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", tf.getPath(), mf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_file_set_last_modified_failed));
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "Error="+e.getMessage());
            }
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp="+tf.getPath()+
                    ", destination="+temp_out.getLastModified()+", source="+mf.getLastModified()+", destination_size="+temp_out.length()+", source_size="+mf.length());
            if (tf.exists()) tf.delete();
            temp_out.renameTo(tf);

        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }


        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }

    static public int copyFileSmbToLocal(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile mf, SafFile3 tf) {
        int sync_result=0;
        if (stwa.lastModifiedIsFunctional) sync_result= copyFileSmbToLocalSetLastModified(stwa, sti, mf, tf);
        else sync_result= copyFileSmbToLocalUnsetLastModified(stwa, sti, mf, tf);
        return sync_result;
    }

    static private int copyFileSmbToLocalUnsetLastModified(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile mf, SafFile3 tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());

        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile());

            InputStream is = mf.getInputStream();
            OutputStream os =null;
            String to_file_temp=tf.getPath()+".tmp";//mf.getName();
            SafFile3 t_df = new SafFile3(stwa.appContext, to_file_temp);
            os = t_df.getOutputStream();

            int result=copyFile(stwa, sti, mf.getParent(), tf.getParentFile().getPath(), mf.getName(), mf.length(), mf.getInputStream(), os);
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                t_df.deleteIfExists();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp="+tf.getPath()+
                    ", destination="+t_df.lastModified()+", source="+mf.getLastModified()+", destination_size="+t_df.length()+", source_size="+mf.length());

            tf.deleteIfExists();
            if (!t_df.renameTo(tf)) {
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "SafFile3 renameTo Error="+t_df.getLastErrorMessage());
                t_df.deleteIfExists();
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
            SyncThread.scanMediaFile(stwa, sti, tf.getPath());
        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return sync_result;
    }

    static private int copyFileSmbToLocalSetLastModified(SyncThreadWorkArea stwa, SyncTaskItem sti, JcifsFile mf, SafFile3 tf) {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from=", mf.getPath(), ", to=", tf.getPath());
        if (sti.isSyncTestMode()) return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;

        int sync_result= SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
        try {
            String to_file_temp=tf.getAppDirectoryCache()+"/"+mf.getName();

            SyncThread.createDirectoryToLocalStorage(stwa, sti, tf.getParentFile().getPath());

            File temp_file=new File(to_file_temp);
            OutputStream os =new FileOutputStream(temp_file);//stwa.appContext.getContentResolver().openOutputStream(from_sf.getUri());
            int result=copyFile(stwa, sti, mf.getParent(), tf.getParentFile().getPath(), mf.getName(), mf.length(), mf.getInputStream(), os);
            if (result== SyncTaskItem.SYNC_RESULT_STATUS_CANCEL) {
                if (temp_file.exists()) temp_file.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_CANCEL;
            }
            try {
                temp_file.setLastModified(mf.getLastModified());
            } catch(Exception e) {
                SyncThread.showMsg(stwa, false, sti.getSyncTaskName(), "W", tf.getPath(), mf.getName(),
                        stwa.appContext.getString(R.string.msgs_mirror_file_set_last_modified_failed));
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "Error="+e.getMessage());
            }
            SafFile3 from_sf=new SafFile3(stwa.appContext, to_file_temp);
            if (stwa.logLevel>=1) stwa.util.addDebugMsg(1,"I", CommonUtilities.getExecutedMethodName(), " After copy fp="+to_file_temp+
                    ", destination="+from_sf.lastModified()+", source="+mf.getLastModified()+", destination_size="+from_sf.length()+", source_size="+mf.length());

            tf.deleteIfExists();
            if (!from_sf.moveTo(tf)) {//WithRename(tf)) {
                stwa.util.addLogMsg("W", sti.getSyncTaskName(), "SafFile3 moveTo Error="+from_sf.getLastErrorMessage());
                if (temp_file.exists()) temp_file.delete();
                return SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
            }
            SyncThread.scanMediaFile(stwa, sti, tf.getPath());
        } catch(IOException e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(JcifsException e) {
            putErrorMessageJcifs(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        } catch(Exception e) {
            putErrorMessage(stwa, sti, e, mf.getPath(), tf.getPath());
            sync_result= SyncTaskItem.SYNC_RESULT_STATUS_ERROR;
        }

        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }


    private final static int SHOW_PROGRESS_THRESHOLD_VALUE = 1024 * 1024 * 4;

    static public int copyFile(SyncThreadWorkArea stwa, SyncTaskItem sti, String from_dir, String to_dir,
                               String file_name, long file_size, InputStream ifs, OutputStream ofs) throws IOException {
        if (stwa.logLevel>=2) stwa.util.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" from_dir=", from_dir, ", to_dir=", to_dir, ", name=", file_name);

        long read_begin_time = System.currentTimeMillis();

        int io_area_size=SYNC_IO_BUFFER_SIZE;
        boolean show_prog = (file_size > SHOW_PROGRESS_THRESHOLD_VALUE);
        if (sti.isSyncOptionUseSmallIoBuffer() && sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            io_area_size=1024*16-1;
            show_prog=(file_size > 1024*64);
        }

        int buffer_read_bytes = 0;
        long file_read_bytes = 0;
        byte[] buffer = new byte[io_area_size];
        while ((buffer_read_bytes = ifs.read(buffer)) > 0) {
            ofs.write(buffer, 0, buffer_read_bytes);
            file_read_bytes += buffer_read_bytes;
            if (show_prog && file_size > file_read_bytes) {
//                int prog=(int)((file_read_bytes * 100) / file_size);
                SyncThread.showProgressMsg(stwa, sti.getSyncTaskName(), file_name + " " +
                        stwa.appContext.getString(R.string.msgs_mirror_task_file_copying,(file_read_bytes * 100) / file_size));
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

        stwa.util.addDebugMsg(1, "I", to_dir+"/"+file_name + " " + file_read_bytes + " bytes transfered in ",file_read_time + " mili seconds at " +
                            SyncThread.calTransferRate(file_read_bytes, file_read_time));
        stwa.totalTransferByte += file_read_bytes;
        stwa.totalTransferTime += file_read_time;

        return SyncTaskItem.SYNC_RESULT_STATUS_SUCCESS;
    }

}
