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

import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerInfo;

import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.JcifsFile2.JcifsUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.TreeFilelist.TreeFilelistItem;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Properties;

public class ReadSmbFilelist implements Runnable {
    private ThreadCtrl getFLCtrl = null;
    private GlobalParameters mGp=null;

    private ArrayList<TreeFilelistItem> remoteFileList;
    private String smbUrl, remoteDir, remoteHost, remoteHostshare, remoteHostPort;
    private boolean isIpV6Address=false;

    private NotifyEvent notifyEvent;

    private boolean readDirOnly = false;
    private boolean readSubDirCnt = true;

    private CommonUtilities mUtil = null;

//    private String mHostName = "", mHostAddr = "", remoteHostName = "";

    private Context mContext = null;

    private SmbServerInfo mSmbServerInfo =null;
    private String mRemoteUserNameForLog="";

    private String mSmbLevel = JcifsAuth.JCIFS_FILE_SMB1;

    static final String OPCD_READ_SHARE="rd_share";
    static final String OPCD_READ_FILELIST="rd_filelist";
    private String mOpCode=OPCD_READ_SHARE;
    public ReadSmbFilelist(Context c, ThreadCtrl ac, String op, SmbServerInfo rauth, String rd,
                           ArrayList<TreeFilelistItem> fl,
                           NotifyEvent ne, boolean dironly, boolean dc, GlobalParameters gp) {
        mContext = c;
        mUtil = new CommonUtilities(mContext, "FileList", gp, null);
        remoteFileList = fl;
        remoteHost =rauth.serverHostName;
        remoteHostshare=rauth.serverShareName;
        remoteHostPort=rauth.serverPort;
        remoteDir = rd;
        getFLCtrl = ac; //new ThreadCtrl();
        notifyEvent = ne;
        mGp=gp;
        mOpCode=op;
        mSmbServerInfo=rauth;

        readDirOnly = dironly;
        readSubDirCnt = dc;

        if (rauth.serverAccountName!=null) mRemoteUserNameForLog=(rauth.serverAccountName.equals(""))?"":"????????";
        else mRemoteUserNameForLog=null;
        mSmbLevel = rauth.serverProtocol;

        mUtil.addDebugMsg(1, "I", "ReadSmbFilelist Host=" + remoteHost + ", HostShare="+remoteHostshare+", Port="+remoteHostPort,
                ", Dir=" +remoteDir+", user="+mRemoteUserNameForLog+", smb_proto="+mSmbLevel+", OP="+op);

    }

    @Override
    public void run() {
        defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);

        getFLCtrl.setThreadResultSuccess();
        getFLCtrl.setThreadMessage("");

        mUtil.addDebugMsg(1, "I", "ReadSmbFilelist started, readSubDirCnt=" + readSubDirCnt + ", readDirOnly=" + readDirOnly);

        boolean error_exit = false;

        String w_addr=remoteHost;
        if (CommonUtilities.isIpAddressV6(remoteHost)) {
            isIpV6Address=true;
            w_addr=CommonUtilities.addScopeidToIpv6Address(remoteHost);
        }
        InetAddress ia=CommonUtilities.getInetAddress(w_addr);
        if (ia!=null) {
            String addr=ia.getHostAddress();
            if (addr!=null) {
                boolean connected=remoteHostPort.equals("")?
                        CommonUtilities.canSmbHostConnectable(w_addr):CommonUtilities.canSmbHostConnectable(w_addr, remoteHostPort);
                if (!connected) {
                    error_exit = true;
                    if (getFLCtrl.isEnabled()) {
                        getFLCtrl.setThreadResultError();
                        getFLCtrl.setThreadMessage(mContext.getString(R.string.msgs_mirror_smb_addr_not_connected, w_addr));
                    } else {
                        getFLCtrl.setThreadResultCancelled();
                    }
                }
            }
        } else {
            error_exit = true;
            if (getFLCtrl.isEnabled()) {
                getFLCtrl.setThreadResultError();
                getFLCtrl.setThreadMessage(mContext.getString(R.string.msgs_mirror_remote_name_not_found, w_addr));
            } else {
                getFLCtrl.setThreadResultCancelled();
            }
        }

        if (!error_exit) {
            buildRemoteUrl();
            if (mOpCode.equals(OPCD_READ_FILELIST)) readFileList();
            else readShareList();
        }

        mUtil.addDebugMsg(1, "I", "ReadSmbFilelist ended.");
        notifyEvent.notifyToListener(true, null);
    }

    private void readFileList() {
        remoteFileList.clear();
        try {
            JcifsAuth auth=null;
            String acct=mSmbServerInfo.serverAccountName.equals("")?null:mSmbServerInfo.serverAccountName;
            String pswd=mSmbServerInfo.serverAccountPassword.equals("")?null:mSmbServerInfo.serverAccountPassword;
            String dom=mSmbServerInfo.serverDomainName.equals("")?null:mSmbServerInfo.serverDomainName;
            if (mSmbServerInfo.serverProtocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, dom, acct, pswd);
            } else {
                Properties prop=new Properties();
                prop.setProperty("jcifs.smb.client.responseTimeout", mGp.settingsSmbClientResponseTimeout);
                auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, dom, acct, pswd, prop);
            }
            JcifsFile remoteFile = new JcifsFile(smbUrl + remoteDir, auth);

            JcifsFile[] fl = remoteFile.listFiles();

            for (int i = 0; i < fl.length; i++) {
                String fn = fl[i].getName();
                if (fn.endsWith("/")) fn = fn.substring(0, fn.length() - 1);
                if (getFLCtrl.isEnabled()) {
                    int dirct = 0;
                    String fp = fl[i].getPath();
                    if (fp.endsWith("/")) fp = fp.substring(0, fp.lastIndexOf("/"));
                    fp = fp.substring(smbUrl.length() + 1, fp.length());
                    if (fp.lastIndexOf("/") > 0) fp = "/" + fp.substring(0, fp.lastIndexOf("/") + 1);
                    else fp = "/";
                    try {
                        if (!fn.equals("System Volume Information") && fl[i].canRead()) {
                            if (readSubDirCnt) {
                                JcifsFile tdf = new JcifsFile(fl[i].getPath(), auth);
                                JcifsFile[] tfl = null;
                                try {
                                    if (fl[i].isDirectory()) {
                                        tfl = tdf.listFiles();
                                        if (readDirOnly) {
                                            for (int j = 0; j < tfl.length; j++) {
                                                if (tfl[j].isDirectory()) dirct++;
                                            }
                                        } else {
                                            dirct = tfl.length;
                                        }
                                    }
                                    TreeFilelistItem fi = new TreeFilelistItem(
                                            fn,
                                            "",
                                            fl[i].isDirectory(),
                                            fl[i].length(),
                                            fl[i].getLastModified(),
                                            false,
                                            fl[i].canRead(),
                                            fl[i].canWrite(),
                                            fl[i].isHidden(),
                                            fp, 0);
                                    fi.setSubDirItemCount(dirct);
                                    if (readDirOnly) {
                                        if (fi.isDir()) {
                                            remoteFileList.add(fi);
                                            mUtil.addDebugMsg(2, "I", "filelist added :" + fn + ",isDir=" +
                                                    fl[i].isDirectory() + ", canRead=" + fl[i].canRead() +
                                                    ", canWrite=" + fl[i].canWrite() + ",fp=" + fp + ", dircnt=" + dirct);
                                        } else {
                                            fi.setEnableItem(false);
                                            remoteFileList.add(fi);
                                        }
                                    } else {
                                        remoteFileList.add(fi);
                                        mUtil.addDebugMsg(2, "I", "filelist added :" + fn + ",isDir=" +
                                                fl[i].isDirectory() + ", canRead=" + fl[i].canRead() +
                                                ", canWrite=" + fl[i].canWrite() + ",fp=" + fp + ", dircnt=" + dirct);
                                    }
                                } catch (JcifsException e) {
                                }
                            }
                        } else {
                            mUtil.addDebugMsg(2, "I", "filelist ignored :" + fn + ",isDir=" +
                                    fl[i].isDirectory() + ", canRead=" + fl[i].canRead() +
                                    ", canWrite=" + fl[i].canWrite() + ",fp=" + fp + ", dircnt=" + dirct);
                            mUtil.addDebugMsg(2, "I", "filelist ignored :" + fn);
                        }
                    } catch (JcifsException e) {
                        e.printStackTrace();
                    }
                } else {
                    getFLCtrl.setThreadResultCancelled();
                    mUtil.addDebugMsg(1, "W", "File list creation cancelled by main task.");
                    break;
                }
            }

        } catch (JcifsException e) {
            e.printStackTrace();
            String cause="";
            String[] e_msg= JcifsUtil.analyzeNtStatusCode(e, smbUrl + remoteDir, mRemoteUserNameForLog);
            if (e.getCause()!=null) {
                String tc=e.getCause().toString();
                cause=tc.substring(tc.indexOf(":")+1);
                mUtil.addDebugMsg(1, "E", cause.substring(cause.indexOf(":")+1));
                e_msg[0]=cause+"\n"+e_msg[0];
            }
            mUtil.addDebugMsg(1, "E", e.toString());
            getFLCtrl.setThreadMessage(e_msg[0]);
            if (getFLCtrl.isEnabled()) {
                getFLCtrl.setThreadResultError();
                getFLCtrl.setDisabled();
            } else {
                getFLCtrl.setThreadResultCancelled();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "E", e.toString());
            if (getFLCtrl.isEnabled()) {
                getFLCtrl.setThreadResultError();
                getFLCtrl.setThreadMessage(e.getMessage());
                getFLCtrl.setDisabled();
            } else {
                getFLCtrl.setThreadResultCancelled();
            }
        }
    }

    private void buildRemoteUrl() {
        String t_share="";
        if (remoteDir!=null) {
            if (remoteDir.equals("")) t_share=remoteHostshare+"/";
            else t_share=remoteHostshare;
        }
        String w_addr =CommonUtilities.buildSmbUrlAddressElement(remoteHost, remoteHostPort);
        smbUrl ="smb://"+ w_addr +"/"+t_share;
        if (isIpV6Address) {
            if (remoteHost.contains(":")) {
                smbUrl ="smb://"+ w_addr +"/"+t_share;
            }
        }
        mUtil.addDebugMsg(1, "I", "buildRemoteUrl result="+ smbUrl);
    }

    private void readShareList() {
        remoteFileList.clear();
        JcifsFile[] fl=null;
        try {
            JcifsAuth auth=null;
            String acct=mSmbServerInfo.serverAccountName.equals("")?null:mSmbServerInfo.serverAccountName;
            String pswd=mSmbServerInfo.serverAccountPassword.equals("")?null:mSmbServerInfo.serverAccountPassword;
            String dom=mSmbServerInfo.serverDomainName.equals("")?null:mSmbServerInfo.serverDomainName;
            if (mSmbServerInfo.serverProtocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, dom, acct, pswd);
            } else {
                Properties prop=new Properties();
                prop.setProperty("jcifs.smb.client.responseTimeout", mGp.settingsSmbClientResponseTimeout);
                auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, dom, acct, pswd, prop);
            }

            JcifsFile remoteFile = new JcifsFile(smbUrl, auth);
            fl = remoteFile.listFiles();
        } catch (JcifsException e) {
            e.printStackTrace();
            String cause="";
            if (e.getCause()!=null) {
                cause=e.getCause().toString();
                mUtil.addDebugMsg(1, "E", cause.substring(cause.indexOf(":")+1));
            }
            mUtil.addDebugMsg(1, "E", e.toString());
            getFLCtrl.setThreadResultError();
            String[] e_msg = JcifsUtil.analyzeNtStatusCode(e, smbUrl, mRemoteUserNameForLog);
            if (!cause.equals("")) getFLCtrl.setThreadMessage(cause.substring(cause.indexOf(":")+1)+"\n"+e_msg[0]);
            else getFLCtrl.setThreadMessage(e_msg[0]);

            getFLCtrl.setDisabled();
            return;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (fl!=null) {
            for (JcifsFile item:fl) {
                String fn = item.getName().substring(0,item.getName().length()-1);
                String fp = item.getPath().substring(0,item.getPath().length()-1);
                if (getFLCtrl.isEnabled()) {
                    if (!fn.endsWith("$")) {
                        TreeFilelistItem fi = new TreeFilelistItem(
                                fn,
                                "",
                                true,//fl[i].isDirectory(),
                                0,//fl[i].length(),
                                0,//fl[i].lastModified(),
                                false,
                                true,//fl[i].canRead(),
                                false,//fl[i].canWrite(),
                                false,//fl[i].isHidden(),
                                fp, 0);
                        remoteFileList.add(fi);
                        mUtil.addDebugMsg(2, "I", "filelist added :" + fn);
                    }

                } else {
                    getFLCtrl.setThreadResultCancelled();
                    mUtil.addDebugMsg(1, "W", "File list creation cancelled by main task.");
                    break;
                }
            }
        } else {
            mUtil.addDebugMsg(1, "W", "Share name can not be found.");
        }
    }

    // Default uncaught exception handler variable
    private UncaughtExceptionHandler defaultUEH;

    // handler listener
    private UncaughtExceptionHandler unCaughtExceptionHandler =
            new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
                    ex.printStackTrace();
                    StackTraceElement[] st = ex.getStackTrace();
                    String st_msg = "";
                    for (int i = 0; i < st.length; i++) {
                        st_msg += "\n at " + st[i].getClassName() + "." +
                                st[i].getMethodName() + "(" + st[i].getFileName() +
                                ":" + st[i].getLineNumber() + ")";
                    }
                    getFLCtrl.setThreadResultError();
                    String end_msg = ex.toString() + st_msg;
                    getFLCtrl.setThreadMessage(end_msg);
                    getFLCtrl.setDisabled();
                    notifyEvent.notifyToListener(true, null);
                    // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
                }
            };

}

