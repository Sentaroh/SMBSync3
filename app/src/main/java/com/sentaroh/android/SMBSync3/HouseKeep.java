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
package com.sentaroh.android.SMBSync3;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sentaroh.android.SMBSync3.Log.LogUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ThreadCtrl;

import java.util.ArrayList;

class HouseKeep {

    private CommonUtilities mUtil=null;
    private Context mContext=null;
    private ActivityMain mActivity=null;
    private GlobalParameters mGp=null;
    private Handler mUiHandler=null;
    private ThreadCtrl mTcHousekeep = null;

    public HouseKeep(ActivityMain a, GlobalParameters gp, CommonUtilities cu) {
        mActivity=a;
        mContext=a;
        mGp=gp;
        mUtil=cu;
        mUiHandler=new Handler();
        performHouseKeep();
    }

    private void performHouseKeep() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTcHousekeep = new ThreadCtrl();
                Thread th2 = new Thread() {
                    @Override
                    public void run() {
                        mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_maintenance_last_mod_list_start_msg));
                        if (!mGp.syncThreadActive) {
                            mGp.syncThreadEnabled = false;
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadOpenDialog();
                                }
                            });

                            houseKeepResultLog();

                            houseKeepLocalFileLastModList();

                            String msg_txt = "";
                            if (mTcHousekeep.isEnabled()) {
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_end_msg);
                                mUtil.addLogMsg("I", "", msg_txt);
                                mUtil.showCommonDialogInfo(false, msg_txt, "", null);
                            } else {
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_cancel_msg);
                                mUtil.addLogMsg("W", "", msg_txt);
                                mUtil.showCommonDialogWarn(false, msg_txt, "", null);
                            }
                            mGp.uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadCloseDialog();
                                    mGp.syncThreadEnabled = true;
                                }
                            });

                        } else {
                            mUtil.addLogMsg("I", "", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
                            mUtil.showCommonDialogWarn(false,
                                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
                        }
                    }
                };
                th2.setPriority(Thread.MAX_PRIORITY);
                th2.start();
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        if (!mGp.syncThreadActive) {
            mUtil.showCommonDialogWarn(true,
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_confirm_start_msg), "", ntfy);
        } else {
            mUtil.addLogMsg("W", "", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
            mUtil.showCommonDialogWarn(false,
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
        }
    }

    private void houseKeepThreadOpenDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mActivity.setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
//        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSynctask.setVisibility(TextView.GONE);
        mGp.progressSpinMsg.setText(mContext.getString(R.string.msgs_progress_spin_dlg_housekeep_running));
        mGp.progressSpinCancel.setText(mContext.getString(R.string.msgs_progress_spin_dlg_housekeep_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new View.OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mTcHousekeep.setDisabled();
                        mGp.progressSpinCancel.setEnabled(false);
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}
                });
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_progress_spin_dlg_housekeep_cancel_confirm), "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        LogUtil.flushLog(mContext);
    }

    private void houseKeepThreadCloseDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " ended");
        LogUtil.flushLog(mContext);

        mGp.progressSpinCancelListener = null;
        mGp.progressSpinCancel.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        mActivity.setUiEnabled();
    }

    private int mResultLogDeleteCount = 0;

    private void houseKeepResultLog() {
        final ArrayList<String> del_list = new ArrayList<String>();
        mResultLogDeleteCount = 0;
        SafFile3 rlf = new SafFile3(mContext, mGp.settingAppManagemsntDirectoryName + "/result_log");
        if (!rlf.exists()) rlf.mkdirs();
        SafFile3[] fl = rlf.listFiles();
        if (fl != null && fl.length > 0) {
            String del_msg = "", sep = "- ";
            for (SafFile3 ll : fl) {
                boolean found = false;
                if (mGp.syncHistoryList.size() > 0) {
                    for (HistoryListAdapter.HistoryListItem shi : mGp.syncHistoryList) {
                        if (shi.sync_result_file_path.equals(ll.getPath())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    del_list.add(ll.getPath());
                    del_msg += sep + ll.getPath();
                    sep="\n- ";
                }
            }
            if (del_list.size() > 0) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        for (String del_fp : del_list) {
                            if (!deleteResultLogFile(del_fp)) {
                                break;
                            }
                        }
                        mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mUtil.addLogMsg("I", "", String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }
                });
                mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_maintenance_result_log_list_del_title), del_msg, ntfy);
                synchronized (mTcHousekeep) {
                    try {
                        mTcHousekeep.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean deleteResultLogFile(String fp) {
        boolean result = false;
        SafFile3 lf = new SafFile3(mContext, fp);
        if (lf.isDirectory()) {
            SafFile3[] fl = lf.listFiles();
            for (SafFile3 item : fl) {
                if (item.isDirectory()) {
                    if (!deleteResultLogFile(item.getPath())) {
                        mUtil.addLogMsg("I", "", "Delete failed, path=" + item.getPath());
                        return false;
                    }
                } else {
                    result = item.delete();
                    if (result) {
                        mResultLogDeleteCount++;
                        String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), item.getPath());
                        mUtil.addLogMsg("I", "", msg);
                    } else {
                        mUtil.addLogMsg("I", "", "Delete file failed, path=" + item.getPath());
                    }
                }
            }
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", "", msg);
            } else {
                mUtil.addLogMsg("I", "", "Delete directory failed, path=" + lf.getPath());
            }
        } else {
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", "", msg);
            } else {
                mUtil.addLogMsg("I", "", "Delete file failed, path=" + lf.getPath());
            }
        }
        return result;
    }

    private void houseKeepLocalFileLastModList() {
        ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> mCurrLastModifiedList = new ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> mNewLastModifiedList = new ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry> del_list = new ArrayList<FileLastModifiedTime.FileLastModifiedTimeEntry>();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {}

            @Override
            public void negativeResponse(Context c, Object[] o) {
                String en = (String) o[0];
                mUtil.addLogMsg("I", "", "Duplicate local file last modified entry was ignored, name=" + en);
            }
        });
        FileLastModifiedTime.loadLastModifiedList(mContext, mGp.settingAppManagemsntDirectoryName, mCurrLastModifiedList, mNewLastModifiedList, ntfy);
        if (mCurrLastModifiedList.size() > 0) {
            for (FileLastModifiedTime.FileLastModifiedTimeEntry li : mCurrLastModifiedList) {
                if (!mTcHousekeep.isEnabled()) break;
                if (li.getFilePath().startsWith("/storage/")) {
                    SafFile3 lf = new SafFile3(mContext, li.getFilePath());
                    if (!lf.exists()) {
                        del_list.add(li);
                        mUtil.addDebugMsg(1, "I", "Entery was deleted, fp=" + li.getFilePath());
                    }
                }
            }
            for (FileLastModifiedTime.FileLastModifiedTimeEntry li : del_list) {
                if (!mTcHousekeep.isEnabled()) break;
                mCurrLastModifiedList.remove(li);
            }
        }
        if (mTcHousekeep.isEnabled()) {
            mUtil.addLogMsg("I", "",
                    String.format(mContext.getString(R.string.msgs_maintenance_last_mod_list_del_count), del_list.size()));
            if (del_list.size() > 0)
                FileLastModifiedTime.saveLastModifiedList(mContext, mGp.settingAppManagemsntDirectoryName, mCurrLastModifiedList, mNewLastModifiedList);
        }
    }

}
