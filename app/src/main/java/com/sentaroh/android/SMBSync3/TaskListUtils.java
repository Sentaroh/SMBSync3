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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.sentaroh.android.SMBSync3.SmbServerScan.SmbServerInfo;

import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.JcifsFile2.JcifsUtil;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.TreeFilelist.TreeFilelistAdapter;
import com.sentaroh.android.Utilities3.TreeFilelist.TreeFilelistItem;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.sentaroh.android.SMBSync3.Constants.DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX;
import static com.sentaroh.android.SMBSync3.Constants.NAME_UNUSABLE_CHARACTER;

public class TaskListUtils {

    //	private CustomContextMenu ccMenu=null;

    private Context mContext;
    private ActivityMain mActivity;

    private CommonUtilities mUtil;

    private GlobalParameters mGp = null;
    private FragmentManager mFragMgr = null;

    public TaskListUtils(CommonUtilities mu, ActivityMain a, GlobalParameters gp, FragmentManager fm) {
        mContext = a;
        mGp = gp;
        mUtil = mu;
		mActivity=a;
        mFragMgr = fm;
    }

    static public void setAllSyncTaskToUnchecked(boolean hideCheckBox, SyncTaskListAdapter pa) {
        pa.setAllItemChecked(false);
        if (hideCheckBox) pa.setShowCheckBox(false);
        pa.notifyDataSetChanged();
    }

    public void setSyncTaskToAuto(GlobalParameters gp) {
        SyncTaskItem item;

        for (int i = 0; i < gp.syncTaskListAdapter.getCount(); i++) {
            item = gp.syncTaskListAdapter.getItem(i);
            if (item.isChecked()) {
                item.setSyncTaskAuto(true);
            }
        }

        saveConfigListWithAutosave(mContext, gp, mUtil);
        mGp.syncTaskListAdapter.notifyDataSetChanged();
        gp.syncTaskListAdapter.setNotifyOnChange(true);
    }

    public void saveConfigListWithAutosave(Context c, GlobalParameters gp, CommonUtilities cu) {
        Thread th=new Thread(){
            @Override
            public void run() {
                String config_data= TaskListImportExport.saveTaskListToAppDirectory(c, gp.syncTaskList, gp.syncScheduleList, gp.syncGroupList);
                if (config_data!=null) TaskListImportExport.saveTaskListToAutosave(mActivity, c, mGp.settingAppManagemsntDirectoryName, config_data);
            }
        };
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
    }

    public void setSyncTaskToManual() {
        SyncTaskItem item;

        int pos = mGp.syncTaskView.getFirstVisiblePosition();
        int posTop = mGp.syncTaskView.getChildAt(0).getTop();
        for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
            item = mGp.syncTaskListAdapter.getItem(i);
            if (item.isChecked()) {
                item.setSyncTaskAuto(false);
//				item.setChecked(false);
            }
        }

        saveConfigListWithAutosave(mContext, mGp, mUtil);
        mGp.syncTaskListAdapter.notifyDataSetChanged();
        mGp.syncTaskListAdapter.setNotifyOnChange(true);
        mGp.syncTaskView.setSelectionFromTop(pos, posTop);
    }

    public void deleteSyncTask(final NotifyEvent p_ntfy) {
        final int[] dpnum = new int[mGp.syncTaskListAdapter.getCount()];
        String dpmsg = "", sep="";

        for (int i = 0; i < mGp.syncTaskListAdapter.getCount(); i++) {
            if (mGp.syncTaskListAdapter.getItem(i).isChecked()) {
                dpmsg +=sep+"-"+mGp.syncTaskListAdapter.getItem(i).getSyncTaskName();
                sep="\n";
                dpnum[i] = i;
            } else dpnum[i] = -1;
        }

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                ArrayList<SyncTaskItem> dpItemList = new ArrayList<SyncTaskItem>();
                int pos = mGp.syncTaskView.getFirstVisiblePosition();
                for (int i = 0; i < dpnum.length; i++) {
                    if (dpnum[i] != -1)
                        dpItemList.add(mGp.syncTaskListAdapter.getItem(dpnum[i]));
                }
                for (int i = 0; i < dpItemList.size(); i++) {
                    mGp.syncTaskListAdapter.remove(dpItemList.get(i));
                    mUtil.addDebugMsg(1,"I","Sync task deleted, name="+dpItemList.get(i).getSyncTaskName());
                    ScheduleEditor.removeSyncTaskFromSchedule(mGp, mUtil, mGp.syncScheduleList, dpItemList.get(i).getSyncTaskName());
                    GroupEditor.removeSyncTaskFromGroup(mGp, mUtil, dpItemList.get(i).getSyncTaskName());
                }
                if (mGp.syncTaskListAdapter.getCount()>0) saveConfigListWithAutosave(mContext, mGp, mUtil);

                mGp.syncTaskListAdapter.setNotifyOnChange(true);
                mGp.syncTaskView.setSelection(pos);

                if (mGp.syncTaskListAdapter.isEmptyAdapter()) {
                    mGp.syncTaskListAdapter.setShowCheckBox(false);
                }

                TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);

                p_ntfy.notifyToListener(true, null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                p_ntfy.notifyToListener(false, null);
            }
        });
        mUtil.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_delete_following_profile),
                mContext.getString(R.string.msgs_task_name_remove_with_schedule_group_task_list)+"\n\n"+dpmsg+"\n", ntfy);
    }

    public void renameSyncTask(final SyncTaskItem pli, final NotifyEvent p_ntfy) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etInput = (EditText) dialog.findViewById(R.id.single_item_input_dir);

        title.setText(mContext.getString(R.string.msgs_rename_profile));

        dlg_cmp.setVisibility(TextView.VISIBLE);
        dlg_cmp.setText(mContext.getString(R.string.msgs_task_name_rename_with_schedule_group_task_list));
        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);
        etInput.setText(pli.getSyncTaskName());
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (!arg0.toString().equalsIgnoreCase(pli.getSyncTaskName())) {
                    if (arg0.length()>SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH) {
                        dlg_msg.setText(mContext.getString(R.string.msgs_sync_task_name_length_invalid, SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH));
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    } else {
                        dlg_msg.setText("");
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }
                } else {
                    dlg_msg.setText(mContext.getString(R.string.msgs_duplicate_task_name));
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        //OK button
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();

                String new_name = etInput.getText().toString();
                String prev_name=pli.getSyncTaskName();
                pli.setSyncTaskName(new_name);
                mUtil.addDebugMsg(1,"I","Sync task renamed, from="+prev_name+", new="+new_name);
                ScheduleEditor.renameSyncTaskFromSchedule(mGp, mUtil, mGp.syncScheduleList, prev_name, new_name);
                GroupEditor.renameSyncTaskFromGroup(mGp, mUtil, prev_name, new_name);

                mGp.syncTaskListAdapter.sort();
                mGp.syncTaskListAdapter.notifyDataSetChanged();

                saveConfigListWithAutosave(mContext, mGp, mUtil);

                TaskListUtils.setAllSyncTaskToUnchecked(true, mGp.syncTaskListAdapter);

                p_ntfy.notifyToListener(true, null);
            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

    }

    public void invokeSelectSmbShareDlg(Dialog dialog) {
//		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);

        final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);
        final EditText edituser = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText editpass = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);
        final EditText editshare = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);
        final EditText edithost = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
        final CheckedTextView ctv_use_userpass = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText editport = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);
        final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
//        final CheckedTextView ctv_sync_folder_smb_ipc_enforced = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_ipc_signing_enforced);
//        final CheckedTextView ctv_sync_folder_smb_use_smb2_negotiation = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_use_smb2_negotiation);
        String remote_addr="", remote_user = "", remote_pass = "", remote_host="";

        if (ctv_use_userpass.isChecked()) {
            remote_user = edituser.getText().toString().trim();
            remote_pass = editpass.getText().toString();
        }

        final String smb_proto=(String)sp_sync_folder_smb_proto.getSelectedItem();
//        final boolean ipc_enforced=ctv_sync_folder_smb_ipc_enforced.isChecked();
//        final boolean smb2_negotiation=ctv_sync_folder_smb_use_smb2_negotiation.isChecked();
        String host=edithost.getText().toString().trim();
        if (JcifsUtil.isValidIpAddress(host)) {
            remote_addr = host;
        } else {
            if (host.contains(":")) remote_addr=host;
            else remote_host = host;
        }
        String remote_port = "";
        if (ctv_use_port_number.isChecked() && editport.getText().length() > 0)
            remote_port = editport.getText().toString();

        SmbServerInfo ssi=new SmbServerInfo();
        ssi.serverHostName =remote_host;
        ssi.serverHostAddress =remote_addr;
        ssi.serverShareName="";
        ssi.serverPort=remote_port;
        ssi.serverProtocol=smb_proto;
        ssi.serverAccountName=remote_user;
        ssi.serverAccountPassword=remote_pass;

        NotifyEvent ntfy = new NotifyEvent(mContext);
        //Listen setRemoteShare response
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context arg0, Object[] arg1) {
                editshare.setText((String) arg1[0]);
            }

            @Override
            public void negativeResponse(Context arg0, Object[] arg1) {
                if (arg1 != null) {
                    String msg_text = (String) arg1[0];
                    mUtil.showCommonDialog(false, "E", "SMB Error", msg_text, null);
                }
            }

        });
        selectRemoteShareDlg(ssi, ntfy);
    }

//    private String mSmbBaseUrl="";
    public void selectRemoteDirectoryDlg(Dialog p_dialog, final boolean show_create) {
//		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);

        final Spinner sp_sync_folder_smb_proto = (Spinner) p_dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);
        final EditText edithost = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
        final EditText edituser = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText editpass = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);
        final EditText editshare = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);
        final EditText editdir = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_name);
        final CheckedTextView ctv_use_userpass = (CheckedTextView) p_dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText editport = (EditText) p_dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);
        final CheckedTextView ctv_use_port_number = (CheckedTextView) p_dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
//        final CheckedTextView ctv_sync_folder_smb_ipc_enforced = (CheckedTextView) p_dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_ipc_signing_enforced);
//        final CheckedTextView ctv_sync_folder_smb_use_smb2_negotiation = (CheckedTextView) p_dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_use_smb2_negotiation);
        String t_remote_addr="", remote_user = "", remote_pass = "", t_remote_host="";
        if (ctv_use_userpass.isChecked()) {
            remote_user = edituser.getText().toString();
            remote_pass = editpass.getText().toString();
        }

        final String smb_proto=(String)sp_sync_folder_smb_proto.getSelectedItem();
//        final boolean ipc_enforced=ctv_sync_folder_smb_ipc_enforced.isChecked();
//        final boolean smb2_negotiation=ctv_sync_folder_smb_use_smb2_negotiation.isChecked();

        final String p_dir = editdir.getText().toString();

        String host = edithost.getText().toString();
        if (JcifsUtil.isValidIpAddress(host)) {
            t_remote_addr = host;
        } else {
            if (host.contains(":")) t_remote_addr = host;
            else t_remote_host = host;
        }
        String t_remote_port = "";
        if (ctv_use_port_number.isChecked() && editport.getText().length() > 0)
            t_remote_port = editport.getText().toString();
        final String remote_addr=t_remote_addr;
        final String remote_host=t_remote_host;
        final String remote_share = editshare.getText().toString();
        final String remote_port=t_remote_port;

        SmbServerInfo ssi=new SmbServerInfo();
        ssi.serverHostName =remote_host;
        ssi.serverHostAddress =remote_addr;
        ssi.serverShareName=remote_share;
        ssi.serverPort=remote_port;
        ssi.serverProtocol=smb_proto;
        ssi.serverAccountName=remote_user;
        ssi.serverAccountPassword=remote_pass;

        final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                @SuppressWarnings("unchecked")
                ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>) o[0];

                for (int i = 0; i < rfl.size(); i++) {
                    if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
                }
                Collections.sort(rows);
                NotifyEvent ntfy_sel=new NotifyEvent(mContext);
                ntfy_sel.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] o) {
                        String sel = (String)o[0];
                        editdir.setText(sel);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                remoteDirectorySelector(rows, ssi, p_dir, show_create, ntfy_sel);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                String msg_text = (String) o[0];
                mUtil.showCommonDialog(false, "E", "SMB Error", msg_text, null);
            }
        });
//        createRemoteFileList(remurl, p_dir, ipc_enforced, smb_proto, ntfy, true);
        createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, "", ntfy, true);
    }

    private void remoteDirectorySelector(final ArrayList<TreeFilelistItem> rows, SmbServerInfo ssi, final String p_dir,
                                         final boolean show_create, final NotifyEvent p_ntfy) {
        //カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.common_file_selector_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.common_file_selector_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.common_file_selector_dlg_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.common_file_selector_dlg_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
//				subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);

        final TextView tv_empty = (TextView) dialog.findViewById(R.id.common_file_selector_empty);
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.common_file_selector_dlg_msg);
        dlg_msg.setVisibility(TextView.GONE);

        final Spinner mp = (Spinner) dialog.findViewById(R.id.common_file_selector_storage_spinner);
        mp.setVisibility(LinearLayout.GONE);
        final LinearLayout dir_name_view = (LinearLayout) dialog.findViewById(R.id.common_file_selector_dir_name_view);
        dir_name_view.setVisibility(LinearLayout.GONE);
        final EditText et_dir_name = (EditText) dialog.findViewById(R.id.common_file_selector_dir_name);

        final String directory_pre="smb://"+ssi.serverHostAddress +"/"+ssi.serverShareName;

        final NonWordwrapTextView tv_home = (NonWordwrapTextView) dialog.findViewById(R.id.common_file_selector_filepath);
        tv_home.setText(directory_pre);

        final Button btn_create = (Button) dialog.findViewById(R.id.common_file_selector_create_btn);
        if (show_create) btn_create.setVisibility(Button.VISIBLE);
        else btn_create.setVisibility(Button.GONE);
        title.setText(mContext.getString(R.string.msgs_select_remote_dir));
        final Button btn_ok = (Button) dialog.findViewById(R.id.common_file_selector_btn_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.common_file_selector_btn_cancel);
        final Button btn_up = (Button) dialog.findViewById(R.id.common_file_selector_up_btn);
        final Button btn_top = (Button) dialog.findViewById(R.id.common_file_selector_top_btn);
        final Button btn_refresh = (Button) dialog.findViewById(R.id.common_file_selector_refresh_btn);

        if (mGp.isScreenThemeIsLight()) {
            btn_up.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_dark, 0, 0, 0);
            btn_top.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_dark, 0, 0, 0);
        } else {
            btn_up.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_light, 0, 0, 0);
            btn_top.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_light, 0, 0, 0);
        }

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final ListView lv = (ListView) dialog.findViewById(R.id.common_file_selector_list);
        final TreeFilelistAdapter tfa = new TreeFilelistAdapter(mActivity, true, false);
        if (rows.size()==0) {
            tv_empty.setVisibility(TextView.VISIBLE);
            lv.setVisibility(ListView.GONE);
        } else {
            tv_empty.setVisibility(TextView.GONE);
            lv.setVisibility(ListView.VISIBLE);
        }
        tfa.setDataList(rows);
        tfa.setSelectable(false);
        lv.setAdapter(tfa);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);

        setTopUpButtonEnabled(dialog, false);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                final int pos = tfa.getItem(idx);
                final TreeFilelistItem tfi = tfa.getDataItem(pos);
                if (tfi.isDir()) {
                    final String n_dir=tfi.getPath()+tfi.getName()+"/";
                    if (tfi.getSubDirItemCount()>=0) {
                        NotifyEvent ntfy = new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context c, Object[] o) {
                                setTopUpButtonEnabled(dialog, true);
                                tv_home.setText(directory_pre+n_dir);
                                ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>) o[0];
                                ArrayList<TreeFilelistItem> new_tfl = new ArrayList<TreeFilelistItem>();
                                for (int i = 0; i < rfl.size(); i++) {
                                    if (rfl.get(i).isDir() && rfl.get(i).canRead()) new_tfl.add(rfl.get(i));
                                }
                                Collections.sort(new_tfl);
                                if (new_tfl.size()==0) {
                                    tv_empty.setVisibility(TextView.VISIBLE);
                                    lv.setVisibility(ListView.GONE);
                                } else {
                                    tv_empty.setVisibility(TextView.GONE);
                                    lv.setVisibility(ListView.VISIBLE);
                                }
                                tfa.setDataList(new_tfl);
                            }

                            @Override
                            public void negativeResponse(Context c, Object[] o) {
                                String msg_text = (String) o[0];
                                mUtil.showCommonDialog(false, "E", "SMB Error", msg_text, null);
                            }
                        });
                        createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, n_dir, ntfy, true);
                    }
                } else {

                }
            }
        });

        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> items, View view, int idx, long id) {
                final int pos = tfa.getItem(idx);
                final TreeFilelistItem tfi = tfa.getDataItem(pos);
                tfi.setChecked(true);
                tfa.notifyDataSetChanged();
                return true;
            }
        });

        NotifyEvent ctv_ntfy = new NotifyEvent(mContext);
        ctv_ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (o != null) {
                    int pos = (Integer) o[0];
                    final TreeFilelistItem tfi = tfa.getDataItem(pos);
                    et_dir_name.setText((tfi.getPath()+tfi.getName()).substring(1));
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        tfa.setCbCheckListener(ctv_ntfy);

        btn_create.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ne=new NotifyEvent(mContext);
                ne.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        btn_refresh.performClick();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                createRemoteDirectoryDlg(ssi, tv_home.getText().toString(), ne);
            }
        });

        btn_refresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy_refresh=new NotifyEvent(mContext);
                ntfy_refresh.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] o) {
                        tv_home.setText(directory_pre+tv_home.getText().toString().replace(directory_pre,""));
                        ArrayList<TreeFilelistItem> new_rfl = (ArrayList<TreeFilelistItem>) o[0];

                        if (new_rfl.size()==0) {
                            tv_empty.setVisibility(TextView.VISIBLE);
                            lv.setVisibility(ListView.GONE);
                        } else {
                            tv_empty.setVisibility(TextView.GONE);
                            lv.setVisibility(ListView.VISIBLE);
                        }

                        tfa.setDataList(new_rfl);
                        tfa.notifyDataSetChanged();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, "/"+tv_home.getText().toString().replace(directory_pre,""), ntfy_refresh, true);
            }
        });

        btn_up.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final String c_dir=tv_home.getText().toString().replace(directory_pre,"");
                String t_dir=c_dir.substring(0,c_dir.lastIndexOf("/"));
                final String n_dir=t_dir.lastIndexOf("/")>0?t_dir.substring(0,t_dir.lastIndexOf("/"))+"/":"";

                NotifyEvent ntfy_refresh=new NotifyEvent(mContext);
                ntfy_refresh.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] o) {
                        tv_home.setText(directory_pre+n_dir);
                        if (n_dir.equals("")) setTopUpButtonEnabled(dialog, false);
                        else setTopUpButtonEnabled(dialog, true);
                        ArrayList<TreeFilelistItem> new_rfl = (ArrayList<TreeFilelistItem>) o[0];
                        if (new_rfl.size()==0) {
                            tv_empty.setVisibility(TextView.VISIBLE);
                            lv.setVisibility(ListView.GONE);
                        } else {
                            tv_empty.setVisibility(TextView.GONE);
                            lv.setVisibility(ListView.VISIBLE);
                        }
                        tfa.setDataList(new_rfl);
                        tfa.notifyDataSetChanged();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, "/"+n_dir, ntfy_refresh, true);
            }
        });

        btn_top.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                NotifyEvent ntfy_refresh=new NotifyEvent(mContext);
                ntfy_refresh.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] o) {
                        setTopUpButtonEnabled(dialog, false);
                        tv_home.setText(directory_pre);
                        ArrayList<TreeFilelistItem> new_rfl = (ArrayList<TreeFilelistItem>) o[0];
                        if (new_rfl.size()==0) {
                            tv_empty.setVisibility(TextView.VISIBLE);
                            lv.setVisibility(ListView.GONE);
                        } else {
                            tv_empty.setVisibility(TextView.GONE);
                            lv.setVisibility(ListView.VISIBLE);
                        }
                        tfa.setDataList(new_rfl);
                        tfa.notifyDataSetChanged();
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, "", ntfy_refresh, true);
            }
        });

        btn_ok.setVisibility(Button.VISIBLE);
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                String sel=tv_home.getText().toString().replace(directory_pre,"");
                if (sel.endsWith("/")) p_ntfy.notifyToListener(true, new Object[]{sel.substring(0,sel.length()-1)});
                else p_ntfy.notifyToListener(true, new Object[]{sel});
            }
        });

        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });

        dialog.show();

    }

    private void setTopUpButtonEnabled(Dialog dialog, boolean p) {
        final Button btnTop = (Button)dialog.findViewById(R.id.common_file_selector_top_btn);
        final Button btnUp = (Button)dialog.findViewById(R.id.common_file_selector_up_btn);

        CommonDialog.setViewEnabled(mActivity, btnUp, p);
        CommonDialog.setViewEnabled(mActivity, btnTop, p);
    };

    private void createRemoteDirectoryDlg(SmbServerInfo ssi, final String c_dir, final NotifyEvent p_ntfy) {
        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);

        final LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);

        final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);
        dlg_title.setText(mContext.getString(R.string.msgs_file_select_edit_dlg_create));
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
//        dlg_cmp.setTextColor(mGp.themeColorList.text_color_primary);
        final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etDir=(EditText) dialog.findViewById(R.id.single_item_input_dir);

        dlg_cmp.setText(mContext.getString(R.string.msgs_file_select_edit_parent_directory)+":"+c_dir);

        CommonDialog.setDlgBoxSizeCompact(dialog);

        CommonDialog.setViewEnabled(mActivity, btnOk, false);
        final Handler hndl=new Handler();
        etDir.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    CommonDialog.setViewEnabled(mActivity, btnOk, true);
                } else {
                    CommonDialog.setViewEnabled(mActivity, btnOk, false);
                    dlg_msg.setText("");
                }
            }
        });

        //OK button
        btnOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final String creat_dir=etDir.getText().toString();
                final String n_path=c_dir+"/"+creat_dir+"/";
                NotifyEvent ne_exists=new NotifyEvent(mContext);
                ne_exists.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        boolean n_exists=(boolean)objects[0];
                        if (n_exists) {
                            hndl.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
                                  }
                            });
                            return;
                        }
                        NotifyEvent ntfy_confirm=new NotifyEvent(mContext);
                        ntfy_confirm.setListener(new NotifyEvent.NotifyEventListener(){
                            @Override
                            public void positiveResponse(Context c, final Object[] o) {
                                NotifyEvent notify_create=new NotifyEvent(mContext);
                                notify_create.setListener(new NotifyEvent.NotifyEventListener() {
                                    @Override
                                    public void positiveResponse(Context context, final Object[] objects) {
                                        hndl.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                p_ntfy.notifyToListener(true, null);
                                                dialog.dismiss();
                                            }
                                        });
                                    }
                                    @Override
                                    public void negativeResponse(Context context, final Object[] objects) {
                                        hndl.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                final String e_msg=(String)objects[0];
                                                hndl.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mUtil.showCommonDialog(false,"E","SMB Error",e_msg,null);
                                                        dialog.dismiss();
                                                        p_ntfy.notifyToListener(false, null);
                                                    }
                                                });

                                            }
                                        });
                                    }
                                });
                                createRemoteDirectory(ssi, c_dir+"/"+etDir.getText().toString(), notify_create);
                            }
                            @Override
                            public void negativeResponse(Context c, Object[] o) {}
                        });
                        CommonDialog cd=new CommonDialog(mContext, mFragMgr);
                        cd.showCommonDialog(true, "W", mContext.getString(R.string.msgs_file_select_edit_confirm_create_directory), n_path, ntfy_confirm);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {
                    }
                });
                isRemoteItemExists(ssi, n_path, ne_exists);
            }
        });
        // CANCELボタンの指定
        btnCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });
        dialog.show();
    };

    private void isRemoteItemExists(SmbServerInfo ssi, final String new_dir, final NotifyEvent p_ntfy) {
        final Dialog dialog=CommonDialog.showProgressSpinIndicator(mActivity);
        dialog.show();
        Thread th=new Thread(){
             @Override
             public void run() {
                  JcifsAuth auth=null;
                  if (ssi.serverProtocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                      auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, ssi.serverDomainName, ssi.serverAccountName, ssi.serverAccountPassword);
                  } else {
                      auth=new JcifsAuth(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23, ssi.serverDomainName, ssi.serverAccountName, ssi.serverAccountPassword);
                  }
                  try {
                      JcifsFile jf=new JcifsFile(new_dir, auth);
                      if (jf.exists()) p_ntfy.notifyToListener(true, new Object[] {true});
                      else p_ntfy.notifyToListener(true, new Object[] {false});
                  } catch (MalformedURLException e) {
                      e.printStackTrace();
                      mUtil.addDebugMsg(1, "E", e.toString());
                      p_ntfy.notifyToListener(false, new Object[]{e.toString()});
                  } catch (JcifsException e) {
                      e.printStackTrace();
                      String suggest_msg=getJcifsErrorSugestionMessage(mContext, MiscUtil.getStackTraceString(e));
                      String cause="";
                      String un="";
                      if (mGp.settingSecurityReinitSmbAccountPasswordValue  && !mGp.settingSecurityApplicationPasswordHashValue.equals("")) {
                          if (ssi.serverAccountName!=null) un=(ssi.serverAccountName.equals(""))?"":"????????";
                          else un=null;
                      } else {
                          un=ssi.serverAccountName;
                      }
                      String[] e_msg= JcifsUtil.analyzeNtStatusCode(e, new_dir, un);
                      if (e.getCause()!=null) {
                          String tc=e.getCause().toString();
                          cause=tc.substring(tc.indexOf(":")+1);
                          e_msg[0]=cause+"\n"+e_msg[0];
                      }
                      String error_msg=suggest_msg.equals("")?e_msg[0]:suggest_msg+"\n"+e_msg[0];
                      mUtil.addDebugMsg(1, "E", error_msg);
                      p_ntfy.notifyToListener(false, new Object[]{error_msg});
                  }
                  dialog.dismiss();
             }
        };
        th.start();
    }

    public static String getJcifsErrorSugestionMessage(Context c, String error_msg) {
        String sugget_msg="";
        if (isJcifsErrorChangeProtocolRequired(error_msg)) {
            sugget_msg=c.getString(R.string.msgs_task_edit_sync_folder_dlg_smb_protocol_suggestion_message);
        }
        return sugget_msg;
    }

    public static boolean isJcifsErrorChangeProtocolRequired(String msg_text) {
        boolean result=false;
        String[] change_required_msg=new String[]{"This client is not compatible with the server"};
        for(String item:change_required_msg) {
            if (msg_text.contains(item)) {
                result=true;
                break;
            }
        }
        return result;
    }

    static public String hasSyncTaskNameUnusableCharacter(Context c, String task_name) {
        for(String item:NAME_UNUSABLE_CHARACTER) {
            if (task_name.contains(item)) return c.getString(R.string.msgs_task_name_contains_invalid_character,item);
        }
        return "";
    }

    private void createRemoteDirectory(SmbServerInfo ssi, final String new_dir, final NotifyEvent p_ntfy) {
        final Dialog dialog=CommonDialog.showProgressSpinIndicator(mActivity);
        dialog.show();
        Thread th=new Thread(){
            @Override
            public void run() {
                JcifsAuth auth=null;
                if (ssi.serverProtocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                    auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, ssi.serverDomainName, ssi.serverAccountName, ssi.serverAccountPassword);
                } else {
                    auth=new JcifsAuth(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23, ssi.serverDomainName, ssi.serverAccountName, ssi.serverAccountPassword);
                }
                try {
                    JcifsFile jf=new JcifsFile(new_dir, auth);
                    jf.mkdirs();
                    if (jf.exists()) p_ntfy.notifyToListener(true, null);
                    else p_ntfy.notifyToListener(false, null);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    mUtil.addDebugMsg(1, "E", e.toString());
                    p_ntfy.notifyToListener(false, new Object[]{e.toString()});
                } catch (JcifsException e) {
                    e.printStackTrace();
                    String suggest_msg=getJcifsErrorSugestionMessage(mContext, MiscUtil.getStackTraceString(e));
                    String cause="";
                    String un="";
                    if (mGp.settingSecurityReinitSmbAccountPasswordValue) {
                        un=(ssi.serverAccountName.equals(""))?"":"????????";
                    } else {
                        un=ssi.serverAccountName;
                    }
                    String[] e_msg= JcifsUtil.analyzeNtStatusCode(e, new_dir, un);
                    if (e.getCause()!=null) {
                        String tc=e.getCause().toString();
                        cause=tc.substring(tc.indexOf(":")+1);
                        e_msg[0]=cause+"\n"+e_msg[0];
                    }
                    String error_msg=suggest_msg.equals("")?e_msg[0]:suggest_msg+"\n"+e_msg[0];
                    mUtil.addDebugMsg(1, "E", error_msg);
                    p_ntfy.notifyToListener(false, new Object[]{error_msg});
                }
                dialog.dismiss();
            }
        };
        th.start();
    }

    public void editDirectoryFilterDlg(final SyncTaskItem sti, final NotifyEvent p_ntfy) {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.filter_list_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.filter_list_edit_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        final ImageButton dlg_help = (ImageButton) dialog.findViewById(R.id.filter_list_edit_help);
        dlg_help.setVisibility(ImageButton.VISIBLE);
        dlg_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TaskEditor.showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_directory_filter_title),
                        mContext.getString(R.string.msgs_help_directory_filter_file));
            }
        });
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.filter_list_edit_msg);
        final TextView dlg_add_guide=(TextView)dialog.findViewById(R.id.filter_list_edit_add_guide);
        dlg_add_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_directory));
        final LinearLayout guide_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_add_guide_view);
        guide_view.setVisibility(LinearLayout.VISIBLE);
        final LinearLayout parent_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_parent_view);
        final NonWordwrapTextView dlg_parent_dir = (NonWordwrapTextView) dialog.findViewById(R.id.filter_list_edit_parent);
        parent_view.setVisibility(LinearLayout.VISIBLE);
        String parent_dir ="/"+sti.getSourceDirectoryName();
        dlg_parent_dir.setVisibility(TextView.VISIBLE);
        dlg_parent_dir.setText(parent_dir);

        ArrayList<FilterListItem> filterList = new ArrayList<FilterListItem>();
        for (int i = 0; i < sti.getDirectoryFilter().size(); i++) {
            FilterListItem fli=sti.getDirectoryFilter().get(i).clone();
            filterList.add(fli);
        }
        final FilterListAdapter filterAdapter = new FilterListAdapter(mActivity, R.layout.filter_list_item_view,
                filterList, FilterListAdapter.FILTER_TYPE_DIRECTORY);
        final ListView lv = (ListView) dialog.findViewById(R.id.filter_list_edit_listview);
        lv.setAdapter(filterAdapter);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);

        if (!hasEnabledFilters(filterAdapter))
            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));

        title.setText(mContext.getString(R.string.msgs_filter_list_dlg_dir_filter));
        final Button dirbtn = (Button) dialog.findViewById(R.id.filter_list_edit_list_dir_btn);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final EditText et_filter = (EditText) dialog.findViewById(R.id.filter_list_edit_new_filter);
        final Button addbtn = (Button) dialog.findViewById(R.id.filter_list_edit_add_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_list_edit_cancel_btn);
        final Button btn_ok = (Button) dialog.findViewById(R.id.filter_list_edit_ok_btn);

        final LinearLayout add_include_select_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_add_include_exclude_view);
        add_include_select_view.setVisibility(LinearLayout.VISIBLE);
        final RadioButton add_include_btn = (RadioButton) dialog.findViewById(R.id.filter_list_edit_add_include_exclude_radio_button_include);
        final RadioButton add_exclude_btn = (RadioButton) dialog.findViewById(R.id.filter_list_edit_add_include_exclude_radio_button_exclude);
        add_include_btn.setChecked(true);

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        NotifyEvent ntfy_switch = new NotifyEvent(mContext);
        ntfy_switch.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                FilterListItem fli=(FilterListItem)o[0];
                if (!hasEnabledFilters(filterAdapter)) {
                    dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    return;
                }
                String new_filter_string="";
                if (isFilterListChanged(sti.getDirectoryFilter(), filterAdapter)) {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    dlg_msg.setText("");
                } else {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    dlg_msg.setText("");
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) { }
        });
        filterAdapter.setNotifyIncExcListener(ntfy_switch);

        NotifyEvent ntfy_delete = new NotifyEvent(mContext);
        ntfy_delete.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                dlg_msg.setText("");
            }
            @Override
            public void negativeResponse(Context c, Object[] o) { }
        });
        filterAdapter.setNotifyDeleteListener(ntfy_delete);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, final int idx, long id) {
                FilterListItem fli = filterAdapter.getItem(idx);
                if (fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        filterAdapter.sort();
                        filterAdapter.notifyDataSetChanged();
                        dlg_msg.setText("");
                        if (!hasEnabledFilters(filterAdapter)) {
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                            return;
                        }
                        if (isFilterListChanged(sti.getDirectoryFilter(), filterAdapter)) {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        } else {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        }
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                editFilterItem(idx, filterAdapter, fli, fli.getFilter(), "", ntfy, FilterListAdapter.FILTER_TYPE_DIRECTORY);
            }
        });

        et_filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
//                    guide_view.setVisibility(ScrollView.VISIBLE);
                    dirbtn.setVisibility(Button.GONE);
                    lv.setVisibility(ListView.GONE);
                    parent_view.requestLayout();
                    String filter= StringUtil.removeRedundantCharacter(s.toString(), ";", true, true);
                    String[]filter_array=filter.split(";");
                    String err_msg=FilterListItem.checkDirectoryFilterError(mContext, filter);
                    if (!err_msg.equals("")) {
                        dlg_msg.setText(err_msg);
                        CommonDialog.setViewEnabled(mActivity, addbtn, false);
                        return;
                    }
                    String dup_filter=getDuplicateFilter(filter, filterAdapter);
                    if (!dup_filter.equals("")) {
                        String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                        dlg_msg.setText(String.format(mtxt, dup_filter));
                        CommonDialog.setViewEnabled(mActivity, addbtn, false);
                        CommonDialog.setViewEnabled(mActivity, dirbtn, true);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    } else {
                        dlg_msg.setText("");
                        CommonDialog.setViewEnabled(mActivity, addbtn, true);
                        CommonDialog.setViewEnabled(mActivity, dirbtn, false);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        if (FilterListItem.hasMatchAnyWhereFilter(filter)) {
                            CommonDialog.setViewEnabled(mActivity, add_include_btn, false);
                            if (!add_exclude_btn.isChecked()) {
                                add_exclude_btn.setChecked(true);
                                mUtil.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_filter_list_match_any_where_change_to_exclude, filter), "", null);
                            }
                        } else {
                            CommonDialog.setViewEnabled(mActivity, add_include_btn, true);
                        }
                        if (!hasEnabledFilters(filterAdapter)) {
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                            return;
                        }
                    }
                } else {
//                    guide_view.setVisibility(ScrollView.GONE);
                    dirbtn.setVisibility(Button.VISIBLE);
                    lv.setVisibility(ListView.VISIBLE);
                    parent_view.requestLayout();
                    CommonDialog.setViewEnabled(mActivity, addbtn, false);
                    CommonDialog.setViewEnabled(mActivity, dirbtn, true);
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        CommonDialog.setViewEnabled(mActivity, addbtn, false);
        addbtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newfilter =StringUtil.removeRedundantCharacter(et_filter.getText().toString(), ";", true, true);
                dlg_msg.setText("");
                et_filter.setText("");
//                guide_view.setVisibility(ScrollView.GONE);
                dirbtn.setVisibility(Button.VISIBLE);
                lv.setVisibility(ListView.VISIBLE);
                parent_view.requestLayout();
                FilterListItem fli=new FilterListItem(newfilter, add_include_btn.isChecked());
                if (fli.hasMatchAnyWhereFilter()) fli.setInclude(false);
                filterAdapter.add(fli);
                filterAdapter.setNotifyOnChange(true);
                filterAdapter.sort();
                CommonDialog.setViewEnabled(mActivity, dirbtn, true);
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        // Directoryボタンの指定
        dirbtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] arg1) {
                        String org_filter_list="";
                        for(FilterListItem fli:sti.getDirectoryFilter()) org_filter_list+=fli.toString();
                        String new_filter_list="";
                        for(FilterListItem fli:filterAdapter.getFilterList()) new_filter_list+=fli.toString();

                        if (!org_filter_list.equals(new_filter_list)) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        dlg_msg.setText("");
                    }
                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                        if (arg1 != null) {
                            String msg_text = (String) arg1[0];
                            mUtil.showCommonDialog(false, "E", "SMB Error", msg_text, null);
                        }
                    }
                });
                listDirectoryFilter(sti, filterAdapter, ntfy);
            }
        });

        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isFilterListChanged(sti.getDirectoryFilter(), filterAdapter)) {
                    NotifyEvent ntfy_cancel_confirm=new NotifyEvent(mContext);
                    ntfy_cancel_confirm.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                            p_ntfy.notifyToListener(false, null);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) { }
                    });
                    mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_filter_list_filter_was_changed), "", ntfy_cancel_confirm);
                } else {
                    dialog.dismiss();
                    p_ntfy.notifyToListener(false, null);
                }
            }
        });

        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });

        // OKボタンの指定
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                sti.getDirectoryFilter().clear();
                if (filterAdapter.getCount() > 0) {
                    for (int i = 0; i < filterAdapter.getCount(); i++) {
                        FilterListItem fli=filterAdapter.getItem(i);
                        String sort_result=FilterListItem.sort(fli.getFilter());
                        fli.setFilter(sort_result);
                        if (!fli.isDeleted())sti.getDirectoryFilter().add(fli);
                    }
                }
                p_ntfy.notifyToListener(true, null);
            }
        });
        dialog.show();

    }

    public void editFileFilterDlg(final ArrayList<FilterListItem> file_filter, final NotifyEvent p_ntfy) {
        ArrayList<FilterListItem> filterList = new ArrayList<FilterListItem>();
        final FilterListAdapter filterAdapter;

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.filter_list_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.filter_list_edit_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final TextView dlg_add_guide=(TextView)dialog.findViewById(R.id.filter_list_edit_add_guide);
        dlg_add_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_file));
        final LinearLayout guide_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_add_guide_view);
        guide_view.setVisibility(LinearLayout.VISIBLE);
        final LinearLayout parent_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_parent_view);

        Button dirbtn = (Button) dialog.findViewById(R.id.filter_list_edit_list_dir_btn);
        dirbtn.setVisibility(Button.GONE);

        filterAdapter = new FilterListAdapter(mActivity, R.layout.filter_list_item_view, filterList, FilterListAdapter.FILTER_TYPE_FILE);
        ListView lv = (ListView) dialog.findViewById(R.id.filter_list_edit_listview);

        for (int i = 0; i < file_filter.size(); i++) {
            FilterListItem fli=file_filter.get(i).clone();
            filterAdapter.add(fli);
        }
        lv.setAdapter(filterAdapter);
        filterAdapter.setNotifyOnChange(true);

        title.setText(mContext.getString(R.string.msgs_filter_list_dlg_file_filter));
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.filter_list_edit_msg);
        dlg_msg.setText("");

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final EditText et_filter = (EditText) dialog.findViewById(R.id.filter_list_edit_new_filter);
        final Button addBtn = (Button) dialog.findViewById(R.id.filter_list_edit_add_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_list_edit_cancel_btn);
        final Button btn_ok = (Button) dialog.findViewById(R.id.filter_list_edit_ok_btn);
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        if (!hasEnabledFilters(filterAdapter)) {
            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
        }

        NotifyEvent ntfy_switch = new NotifyEvent(mContext);
        ntfy_switch.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                FilterListItem fli=(FilterListItem)o[0];
                if (!hasEnabledFilters(filterAdapter)) {
                    dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    return;
                }
                if (isFilterListChanged(file_filter, filterAdapter)) {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    dlg_msg.setText("");
                } else {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    dlg_msg.setText("");
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) { }
        });
        filterAdapter.setNotifyIncExcListener(ntfy_switch);

        NotifyEvent ntfy_delete = new NotifyEvent(mContext);
        ntfy_delete.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                dlg_msg.setText("");
                dlg_msg.setVisibility(TextView.GONE);
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });
        filterAdapter.setNotifyDeleteListener(ntfy_delete);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                FilterListItem fli = filterAdapter.getItem(idx);
                if (fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        filterAdapter.notifyDataSetChanged();
                        if (!hasEnabledFilters(filterAdapter)) {
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                            return;
                        }
                        if (isFilterListChanged(file_filter, filterAdapter)) {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        } else {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        }
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}

                });
                editFilterItem(idx, filterAdapter, fli, fli.getFilter(), "", ntfy, FilterListAdapter.FILTER_TYPE_FILE);
            }
        });

        // Addボタンの指定
        CommonDialog.setViewEnabled(mActivity, addBtn, false);
        et_filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
//                    guide_view.setVisibility(ScrollView.VISIBLE);
                    lv.setVisibility(ListView.GONE);
                    parent_view.requestLayout();

                    String new_filter=StringUtil.removeRedundantCharacter(s.toString().trim(), ";", true, true);
                    String error_message=FilterListItem.checkFileFilterError(mContext, new_filter);
                    if (!error_message.equals("")) {
                        dlg_msg.setText(error_message);
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        return;
                    }
                    String dup_filter=getDuplicateFilter(new_filter, filterAdapter);
                    if (!dup_filter.equals("")) {
                        String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                        dlg_msg.setText(String.format(mtxt, dup_filter));
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    } else {
                        if (!hasEnabledFilters(filterAdapter)) {
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            dlg_msg.setText(mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists));
                            return;
                        }
                        dlg_msg.setText("");
                        CommonDialog.setViewEnabled(mActivity, addBtn, true);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    }
                } else {
                    CommonDialog.setViewEnabled(mActivity, addBtn, false);
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
//                    guide_view.setVisibility(ScrollView.GONE);
                    lv.setVisibility(ListView.VISIBLE);
                    parent_view.requestLayout();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                guide_view.setVisibility(ScrollView.GONE);
                lv.setVisibility(ListView.VISIBLE);
                parent_view.requestLayout();

                dlg_msg.setText("");
                String newfilter = et_filter.getText().toString().trim();
                et_filter.setText("");
                filterAdapter.add(new FilterListItem(newfilter, true));
                filterAdapter.setNotifyOnChange(true);
                filterAdapter.sort();
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isFilterListChanged(file_filter, filterAdapter)) {
                    NotifyEvent ntfy_cancel_confirm=new NotifyEvent(mContext);
                    ntfy_cancel_confirm.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) { }
                    });
                    mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_filter_list_filter_was_changed), "", ntfy_cancel_confirm);
                } else {
                    dialog.dismiss();
                }
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        // OKボタンの指定
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                file_filter.clear();
                if (filterAdapter.getCount() > 0) {
                    for (int i = 0; i < filterAdapter.getCount(); i++) {
                        if (!filterAdapter.getItem(i).isDeleted()) file_filter.add(filterAdapter.getItem(i));
                    }
                }
                p_ntfy.notifyToListener(true, null);
            }
        });
        dialog.show();
    }

    public void editIPAddressFilterDlg(final ArrayList<FilterListItem> addr_list, final NotifyEvent p_ntfy) {
        ArrayList<FilterListItem> filterList = new ArrayList<FilterListItem>();
        final FilterListAdapter filterAdapter;

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.filter_list_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.filter_list_edit_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        title.setText(mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_addr_title));

        final TextView dlg_add_guide=(TextView)dialog.findViewById(R.id.filter_list_edit_add_guide);
        dlg_add_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_ip_address));
        final LinearLayout guide_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_add_guide_view);
        final LinearLayout parent_view = (LinearLayout) dialog.findViewById(R.id.filter_list_edit_parent_view);
        guide_view.setVisibility(LinearLayout.VISIBLE);

        Button add_current_addr = (Button) dialog.findViewById(R.id.filter_list_edit_list_dir_btn);
        add_current_addr.setText(mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_addr_add_current_addr));

        filterAdapter = new FilterListAdapter(mActivity,
                R.layout.filter_list_item_view, filterList, false, FilterListAdapter.FILTER_TYPE_IP_ADDRESS);
        ListView lv = (ListView) dialog.findViewById(R.id.filter_list_edit_listview);

        for (int i = 0; i < addr_list.size(); i++) {
            FilterListItem fli=addr_list.get(i).clone();
            filterAdapter.add(fli);
        }
        lv.setAdapter(filterAdapter);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.filter_list_edit_msg);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final EditText et_filter = (EditText) dialog.findViewById(R.id.filter_list_edit_new_filter);
        et_filter.setHint(mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_addr_hint));
        et_filter.setKeyListener(DigitsKeyListener.getInstance("0123456789.*"));

        final Button addBtn = (Button) dialog.findViewById(R.id.filter_list_edit_add_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_list_edit_cancel_btn);
        final Button btn_ok = (Button) dialog.findViewById(R.id.filter_list_edit_ok_btn);
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        NotifyEvent ntfy_delete = new NotifyEvent(mContext);
        ntfy_delete.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                dlg_msg.setText("");
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        filterAdapter.setNotifyDeleteListener(ntfy_delete);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                FilterListItem fli = filterAdapter.getItem(idx);
                if (fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        filterAdapter.notifyDataSetChanged();
                        if (isFilterListChanged(addr_list, filterAdapter)) {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        } else {
                            dlg_msg.setText("");
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        }
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}

                });
                editFilterItem(idx, filterAdapter, fli, fli.getFilter(),
                        mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_addr_edit_title), ntfy, FilterListAdapter.FILTER_TYPE_IP_ADDRESS);
            }
        });

        // Addボタンの指定
        CommonDialog.setViewEnabled(mActivity, addBtn, false);
        et_filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0) {
//                    guide_view.setVisibility(ScrollView.VISIBLE);
                    lv.setVisibility(ListView.GONE);
                    parent_view.requestLayout();
                    String new_filter=StringUtil.removeRedundantCharacter(s.toString().trim(), ";", true, true);
                    String error_message=FilterListItem.checkApAndAddressFilterError(mContext, new_filter);
                    if (!error_message.equals("")) {
                        dlg_msg.setText(error_message);
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        return;
                    }
                    if (!isValidIpV4Address(new_filter)) {
                        dlg_msg.setText(mContext.getString(R.string.msgs_filter_list_invalid_ip_address));
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        return;
                    }
                    String dup_filter=getDuplicateFilter(new_filter, filterAdapter);
                    if (!dup_filter.equals("")) {
                        String mtxt = mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_duplicate_addr_specified);
                        dlg_msg.setText(String.format(mtxt, dup_filter));
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    } else {
                        dlg_msg.setText("");
                        CommonDialog.setViewEnabled(mActivity, addBtn, true);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    }
                } else {
//                    guide_view.setVisibility(ScrollView.GONE);
                    lv.setVisibility(ListView.VISIBLE);
                    parent_view.requestLayout();
                    CommonDialog.setViewEnabled(mActivity, addBtn, false);
                    CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        add_current_addr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                String ip_addr = CommonUtilities.getIfIpAddress(mUtil);
                if (!ip_addr.equals("")) {
                    String dup_filter=getDuplicateFilter(ip_addr, filterAdapter);
                    if (!dup_filter.equals("")) {
                        String mtxt = mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_duplicate_addr_specified);
                        dlg_msg.setText(String.format(mtxt, ip_addr));
                        CommonDialog.setViewEnabled(mActivity, addBtn, false);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    } else {
                        dlg_msg.setText("");
                        filterAdapter.add(new FilterListItem(ip_addr, true));
                        filterAdapter.setNotifyOnChange(true);
                        filterAdapter.sort();
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }
                } else {
                    String mtxt = mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_ap_not_connected);
                    dlg_msg.setText(mtxt);
                }
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                guide_view.setVisibility(ScrollView.GONE);
                lv.setVisibility(ListView.VISIBLE);
                parent_view.requestLayout();

                dlg_msg.setText("");
                String newfilter = et_filter.getText().toString().trim();
                et_filter.setText("");
                filterAdapter.add(new FilterListItem(newfilter, true));
                filterAdapter.setNotifyOnChange(true);
                filterAdapter.sort();
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isFilterListChanged(addr_list, filterAdapter)) {
                    NotifyEvent ntfy_cancel_confirm=new NotifyEvent(mContext);
                    ntfy_cancel_confirm.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) { }
                    });
                    mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_filter_list_filter_was_changed), "", ntfy_cancel_confirm);
                } else {
                    dialog.dismiss();
                }
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        // OKボタンの指定
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                addr_list.clear();
                if (filterAdapter.getCount() > 0) {
                    for (int i = 0; i < filterAdapter.getCount(); i++) {
                        if (!filterAdapter.getItem(i).isDeleted()) addr_list.add(filterAdapter.getItem(i));
                    }
                }
                p_ntfy.notifyToListener(true, null);
            }
        });
        dialog.show();

    }

    private void editFilterItem(final int edit_idx, final FilterListAdapter fa,
                                final FilterListItem fli, final String filter, String title_text, final NotifyEvent p_ntfy,
                                final int filter_type) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.filter_item_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.filter_item_edit_dlg_view);
        CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_item_edit_dlg_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.filter_item_edit_dlg_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);
        if (!title_text.equals("")) title.setText(title_text);
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.filter_item_edit_dlg_msg);
        dlg_msg.setVisibility(TextView.VISIBLE);

        final TextView dlg_guide=(TextView)dialog.findViewById(R.id.filter_item_edit_dlg_guide);
        if (filter_type==FilterListAdapter.FILTER_TYPE_DIRECTORY) dlg_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_directory));
        else if (filter_type==FilterListAdapter.FILTER_TYPE_FILE) dlg_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_file));
        else if (filter_type==FilterListAdapter.FILTER_TYPE_ACCESS_POINT) dlg_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_access_point));
        else if (filter_type==FilterListAdapter.FILTER_TYPE_IP_ADDRESS) dlg_guide.setText(mContext.getString(R.string.msgs_filter_list_guide_ip_address));

        final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_item_edit_dlg_cancel_btn);
        final Button btn_ok = (Button) dialog.findViewById(R.id.filter_item_edit_dlg_ok_btn);
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);
        final EditText et_filter = (EditText) dialog.findViewById(R.id.filter_item_edit_dlg_filter);
        et_filter.setText(filter);
        if (filter_type==FilterListAdapter.FILTER_TYPE_IP_ADDRESS) et_filter.setKeyListener(DigitsKeyListener.getInstance("0123456789.*"));

        final CheckBox cb_enabled=(CheckBox)dialog.findViewById(R.id.filter_item_edit_dlg_enabled);
        final RadioButton rb_include=(RadioButton) dialog.findViewById(R.id.filter_item_edit_dlg_include);
        final RadioButton rb_exclude=(RadioButton) dialog.findViewById(R.id.filter_item_edit_dlg_exclude);

        if (filter_type==FilterListAdapter.FILTER_TYPE_ACCESS_POINT || filter_type==FilterListAdapter.FILTER_TYPE_IP_ADDRESS) {
            cb_enabled.setVisibility(CheckBox.GONE);
            rb_include.setVisibility(RadioButton.GONE);
            rb_exclude.setVisibility(RadioButton.GONE);
        }

        cb_enabled.setChecked(fli.isEnabled());
        if (fli.isInclude()) rb_include.setChecked(true);
        else rb_exclude.setChecked(true);

        final ArrayList<FilterListItem>current_fl_exclude_edit_item=new ArrayList<FilterListItem>();
        for(int i=0;i<fa.getCount();i++) {
            if (i!=edit_idx) current_fl_exclude_edit_item.add(fa.getItem(i));
        }

        if (filter.contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
            CommonDialog.setViewEnabled(mActivity, rb_include, false);
            rb_exclude.setChecked(true);
        }

        cb_enabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterListItem new_fli=buildFilterItem(dialog);
                if (isFilterItemChanged(fli, new_fli) && dlg_msg.getText().length()==0) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        rb_include.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterListItem new_fli=buildFilterItem(dialog);
                if (isFilterItemChanged(fli, new_fli) && dlg_msg.getText().length()==0) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        rb_exclude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterListItem new_fli=buildFilterItem(dialog);
                if (isFilterItemChanged(fli, new_fli) && dlg_msg.getText().length()==0) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        et_filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                if (s.length()==0) {
                    String mtxt = mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
                    dlg_msg.setText(mtxt);
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    return;
                } else {
                    String newfilter =StringUtil.removeRedundantCharacter(s.toString(), ";", true, true);
                    if (filter_type==FilterListAdapter.FILTER_TYPE_DIRECTORY) {
                        dlg_msg.setText("");
                        String err_msg=FilterListItem.checkDirectoryFilterError(mContext, newfilter);
                        if (!err_msg.equals("")) {
                            dlg_msg.setVisibility(TextView.VISIBLE);
                            dlg_msg.setText(err_msg);
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            return;
                        }
                        String changed_filter=getChangedFilter(filter, newfilter);
                        if (!changed_filter.equals("")) {
                            String dup_filter=getDuplicateFilter(newfilter, current_fl_exclude_edit_item);
                            if (!dup_filter.equals("")) {
                                String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                                dlg_msg.setText(String.format(mtxt, dup_filter));
                                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                                return;
                            }
                        }
                        if (newfilter.equals(filter))CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        if (newfilter.contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                            CommonDialog.setViewEnabled(mActivity, rb_include, false);
                            if (!rb_exclude.isChecked()) {
                                rb_exclude.setChecked(true);
                                mUtil.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_filter_list_match_any_where_change_to_exclude, newfilter), "", null);
                            }
                        } else {
                            CommonDialog.setViewEnabled(mActivity, rb_include, true);
                        }
                        FilterListItem new_fli=buildFilterItem(dialog);
                        if (isFilterItemChanged(fli, new_fli)) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    } else if (filter_type==FilterListAdapter.FILTER_TYPE_FILE) {
                        String err_msg=FilterListItem.checkFileFilterError(mContext, newfilter);
                        if (!err_msg.equals("")) {
                            dlg_msg.setVisibility(TextView.VISIBLE);
                            dlg_msg.setText(err_msg);
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            return;
                        }
                        String changed_filter=getChangedFilter(filter, newfilter);
                        if (!changed_filter.equals("")) {
                            String dup_filter=getDuplicateFilter(newfilter, current_fl_exclude_edit_item);
                            if (!dup_filter.equals("")) {
                                String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                                dlg_msg.setText(String.format(mtxt, dup_filter));
                                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                                return;
                            }
                            dlg_msg.setText("");
                        }
                        if (newfilter.equals(filter))CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        FilterListItem new_fli=buildFilterItem(dialog);
                        if (isFilterItemChanged(fli, new_fli)) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    } else if (filter_type==FilterListAdapter.FILTER_TYPE_ACCESS_POINT || filter_type==FilterListAdapter.FILTER_TYPE_IP_ADDRESS) {
                        String err_msg=FilterListItem.checkApAndAddressFilterError(mContext, newfilter);
                        if (!err_msg.equals("")) {
                            dlg_msg.setVisibility(TextView.VISIBLE);
                            dlg_msg.setText(err_msg);
                            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                            return;
                        }
                        String changed_filter=getChangedFilter(filter, newfilter);
                        if (!changed_filter.equals("")) {
                            String dup_filter=getDuplicateFilter(newfilter, current_fl_exclude_edit_item);
                            if (!dup_filter.equals("")) {
                                String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                                dlg_msg.setText(String.format(mtxt, dup_filter));
                                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                                return;
                            }
                            dlg_msg.setText("");
                        }
                        if (newfilter.equals(filter))CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                        FilterListItem new_fli=buildFilterItem(dialog);
                        if (isFilterItemChanged(fli, new_fli)) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }
                }
            }
        });


        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FilterListItem new_fli=buildFilterItem(dialog);
                if (isFilterItemChanged(fli, new_fli)) {
                    NotifyEvent ntfy_cancel_confirm=new NotifyEvent(mContext);
                    ntfy_cancel_confirm.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) { }
                    });
                    mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_filter_list_filter_was_changed), "", ntfy_cancel_confirm);
                } else {
                    dialog.dismiss();
                }
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        // OKボタンの指定
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newfilter = StringUtil.removeRedundantCharacter(et_filter.getText().toString(), ";", true, true);
                dialog.dismiss();
                fli.setFilter(newfilter);
                fli.setEnabled(cb_enabled.isChecked());
                fli.setInclude(rb_include.isChecked());
                boolean org_inc=fli.isInclude();
                if (fli.hasMatchAnyWhereFilter()) {
                    fli.setInclude(false);
                    if (org_inc) {
                        CommonDialog.showCommonDialog(mActivity.getSupportFragmentManager(), false, "W",
                                mContext.getString(R.string.msgs_filter_edit_dlg_title),
                                mContext.getString(R.string.msgs_filter_list_match_any_where_change_to_exclude, fli.getFilter()), null);
                    }
                }

                fa.sort();
                fa.setNotifyOnChange(true);
                if (p_ntfy != null) p_ntfy.notifyToListener(true, null);
            }
        });
        dialog.show();
    }

    private boolean hasEnabledFilters(FilterListAdapter fla) {
        String result="";
        if (fla.getCount()==0) return true;
        for(int i=0;i<fla.getCount();i++) {
            if (fla.getItem(i).isEnabled()) return true;
        }
        return false;
    }

    private String getDuplicateFilter(String new_filter, FilterListAdapter fa) {
        ArrayList<FilterListItem>fl=new ArrayList<FilterListItem>();
        for(int i=0;i<fa.getCount();i++) fl.add(fa.getItem(i));
        return getDuplicateFilter(new_filter, fl);
    }

    private String getDuplicateFilter(String new_filter, ArrayList<FilterListItem> fl) {
        //new_filter内での重複チェック
        String[]mew_filter_array=new_filter.split(";");
        if (mew_filter_array.length>1) {
            for(int i=0;i<mew_filter_array.length;i++) {
                for(int j=0;j<mew_filter_array.length;j++) {
                    if (i!=j) {
                        if (mew_filter_array[i].equals(mew_filter_array[j])) {
                            return mew_filter_array[i];
                        }
                    }
                }
            }
        }
        if (fl==null || fl.size() == 0) return "";
        //new_filterとAdapterとの重複チェック
        for (int i = 0; i < fl.size(); i++) {
            if (!fl.get(i).isDeleted()) {
                String[] current_filter_array=fl.get(i).getFilter().split((";"));
                for(String new_item:mew_filter_array) {
                    for(String current_item:current_filter_array) {
                        if (new_item.equalsIgnoreCase(current_item)) return new_item;
                    }
                }
            }
        }
        return "";
    }

    private String getChangedFilter(String current_filter, String new_filter) {
        if (current_filter==null || new_filter==null) return "";
        String[] current_filter_array=current_filter.split(";");
        String[] mew_filter_array=new_filter.split(";");
        String changed_filter="";
        for (String new_item:mew_filter_array) {
            if (!new_item.equals("")) {
                boolean found=false;
                for(String current_item:current_filter_array) {
                    if (new_item.equalsIgnoreCase(current_item)) {
                        found=true;
                        break;
                    }
                }
                if (!found) changed_filter+=new_item+";";
            }
        }
        return changed_filter;
    }

    private boolean isValidIpV4Address(String addr) {
        boolean result=true;
        String[] addr_array=addr.split("\\.");

        for(String parts:addr_array) {
            if (!parts.equals("")) {
                int oct=-1;
                try {
                    String new_parts=StringUtil.replaceAllCharacter(parts, "*", "");
                    if (!new_parts.equals("")) {
                        oct=Integer.parseInt(new_parts);
                        if (oct>254) {
                            result=false;
                            break;
                        }
                    }
                } catch(Exception e) {
                    result=false;
                }
            }
        }
        return result;
    }

    private boolean isFilterListChanged(ArrayList<FilterListItem>org, FilterListAdapter fla) {
        String org_filter_content="";
        for(int i=0;i<org.size();i++) {
            org_filter_content+=org.get(i).toString()+";";
        }

        String new_filter_content="";
        for(int i=0;i<fla.getCount();i++) {
            if (!fla.getItem(i).isDeleted()) new_filter_content+=fla.getItem(i).toString()+";";
        }
        boolean result=!org_filter_content.equals(new_filter_content);
        return result;
    }

    private boolean isFilterItemChanged(FilterListItem org_filter, FilterListItem new_filter) {
        if (org_filter.getFilter().equals(new_filter.getFilter())
                && org_filter.isEnabled()==new_filter.isEnabled()
                && org_filter.isInclude()==new_filter.isInclude()) {
            return false;
        }
        return true;
    }

    private FilterListItem buildFilterItem(Dialog dialog) {
        final EditText et_filter = (EditText) dialog.findViewById(R.id.filter_item_edit_dlg_filter);
        final CheckBox cb_enabled=(CheckBox)dialog.findViewById(R.id.filter_item_edit_dlg_enabled);
        final RadioButton rb_include=(RadioButton) dialog.findViewById(R.id.filter_item_edit_dlg_include);
        final RadioButton rb_exclude=(RadioButton) dialog.findViewById(R.id.filter_item_edit_dlg_exclude);
        FilterListItem fli=new FilterListItem();
        fli.setFilter(StringUtil.removeRedundantCharacter(et_filter.getText().toString(), ";", true, true));
        fli.setEnabled(cb_enabled.isChecked());
        fli.setInclude(rb_include.isChecked());
        return fli;
    }

    static public SyncTaskItem getSyncTaskByName(ArrayList<SyncTaskItem> t_prof, String task_name) {
        SyncTaskItem stli = null;

        for (SyncTaskItem li : t_prof) {
            if (li.getSyncTaskName().equalsIgnoreCase(task_name)) {
                stli = li;
                break;
            }
        }
        return stli;
    }

    static public SyncTaskItem getSyncTaskByName(SyncTaskListAdapter t_prof, String task_name) {
        return getSyncTaskByName(t_prof.getArrayList(), task_name);
    }

    private void listDirectoryFilter(SyncTaskItem sti, FilterListAdapter fla, final NotifyEvent p_ntfy) {
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            listLocalDirectoryFilter(sti, fla, p_ntfy);
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            listSmbDirectoryFilter(sti, fla, p_ntfy);
        }
    }

    private void listLocalDirectoryFilter(final SyncTaskItem sti, final FilterListAdapter fla, final NotifyEvent p_ntfy) {
        final String m_uuid = sti.getSourceStorageUuid();
        final String c_dir =
                sti.getSourceDirectoryName().equals("")?CommonUtilities.getStoragePathFromUuid(m_uuid):CommonUtilities.getStoragePathFromUuid(m_uuid)+"/"+sti.getSourceDirectoryName();

        NotifyEvent ntfy=new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<TreeFilelistItem> tfl=(ArrayList<TreeFilelistItem>)objects[0];
                if (tfl.size()==0) {
                    String msg=mContext.getString(R.string.msgs_dir_empty);
                    mUtil.showCommonDialog(false,"W",msg,"",null);
                    return;
                }

                //カスタムダイアログの生成
                final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.item_select_list_dlg);

                LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
//                ll_dlg_view.setBackgroundColor(mGp.themeColorList.title_background_color);

                final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
                final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
                final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
                title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
                title.setTextColor(mGp.themeColorList.title_text_color);

                title.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));

                subtitle.setText(mContext.getString(R.string.msgs_current_dir) + " " + c_dir);
                final TextView dlg_msg = (TextView) dialog.findViewById(R.id.item_select_list_dlg_msg);
                final Button btn_ok = (Button) dialog.findViewById(R.id.item_select_list_dlg_ok_btn);

                final LinearLayout ll_context = (LinearLayout) dialog.findViewById(R.id.context_view_file_select);
                ll_context.setVisibility(LinearLayout.VISIBLE);
                final ImageButton ib_select_all = (ImageButton) ll_context.findViewById(R.id.context_button_select_all);
                final ImageButton ib_unselect_all = (ImageButton) ll_context.findViewById(R.id.context_button_unselect_all);

                dlg_msg.setVisibility(TextView.VISIBLE);

                CommonDialog.setDlgBoxSizeLimit(dialog, true);

                final ListView lv = (ListView) dialog.findViewById(R.id.list_view);
                final TreeFilelistAdapter tfa = new TreeFilelistAdapter(mActivity, false, false);
                lv.setAdapter(tfa);
                tfa.setDataList(tfl);
                lv.setScrollingCacheEnabled(false);
                lv.setScrollbarFadingEnabled(false);

                NotifyEvent ntfy_expand_close = new NotifyEvent(mContext);
                ntfy_expand_close.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        int idx = (Integer) o[0];
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        expandHideLocalDirTree(true, m_uuid, pos, tfi, tfa);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                tfa.setExpandCloseListener(ntfy_expand_close);
                lv.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        expandHideLocalDirTree(true, m_uuid, pos, tfi, tfa);
                    }
                });
                lv.setOnItemLongClickListener(new OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> items, View view, int idx, long id) {
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        tfi.setChecked(true);
                        tfa.notifyDataSetChanged();
                        return true;
                    }
                });

                ib_select_all.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i = 0; i < tfa.getDataItemCount(); i++) {
                            TreeFilelistItem tfli = tfa.getDataItem(i);
                            if (!tfli.isHideListItem()) tfa.setDataItemIsSelected(i);
                        }
                        tfa.notifyDataSetChanged();
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }
                });

                ib_unselect_all.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i = 0; i < tfa.getDataItemCount(); i++) {
                            tfa.setDataItemIsUnselected(i);
                        }
                        tfa.notifyDataSetChanged();
                        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    }
                });

                //OKボタンの指定
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                //Listen setRemoteShare response
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] arg1) {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }

                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                        boolean checked = false;
                        for (int i = 0; i < tfa.getDataItemCount(); i++) {
                            if (tfa.getDataItem(i).isChecked()) {
                                checked = true;
                                break;
                            }
                        }
                        if (checked) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    }
                });
                tfa.setCbCheckListener(ntfy);

                btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
                btn_ok.setVisibility(Button.VISIBLE);
                btn_ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (!addDirFilter(true, tfa, fla, c_dir, dlg_msg, sti, false)) return;
                        addDirFilter(false, tfa, fla, c_dir, dlg_msg, sti, false);
                        dialog.dismiss();
                        p_ntfy.notifyToListener(true, null);
                    }
                });

                //CANCELボタンの指定
                final Button btn_cancel = (Button) dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
                btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
                btn_cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        p_ntfy.notifyToListener(true, null);
                    }
                });

                // Cancelリスナーの指定
                dialog.setOnCancelListener(new Dialog.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        btn_cancel.performClick();
                    }
                });

                dialog.show();

            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {
                mUtil.showCommonDialog(false, "E", "Local file list creation aborted", "", null);
            }
        });
        createLocalFilelist(true, m_uuid, c_dir, ntfy);
    }

    private void listSmbDirectoryFilter(final SyncTaskItem sti, final FilterListAdapter fla, final NotifyEvent p_ntfy) {
        final String host_addr = sti.getSourceSmbAddr();
        final String host_name = sti.getSourceSmbHostName();
        final String host_share = sti.getSourceSmbShareName();
        final String h_port = !sti.getSourceSmbPort().equals("")?":"+sti.getSourceSmbPort() : "";
        final String host_port=h_port;
        String remdir_tmp="";
        if (sti.getSourceDirectoryName().equals("/") || sti.getSourceDirectoryName().equals("")) {
            remdir_tmp = "/";
        } else {
            remdir_tmp = sti.getSourceDirectoryName().startsWith("/")?sti.getSourceDirectoryName()+"/":"/" + sti.getSourceDirectoryName() + "/";
        }
        final String remdir = remdir_tmp;
        final String smb_proto=sti.getSourceSmbProtocol();

        SmbServerInfo ssi=new SmbServerInfo();
        ssi.serverHostName =host_name;
        ssi.serverHostAddress =host_addr;
        ssi.serverShareName=host_share;
        ssi.serverPort=host_port;
        ssi.serverProtocol=smb_proto;
        ssi.serverAccountName=sti.getSourceSmbAccountName();
        ssi.serverAccountPassword=sti.getSourceSmbPassword();

        NotifyEvent ntfy = new NotifyEvent(mContext);
        // set thread response
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                @SuppressWarnings("unchecked")
                ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>) o[0];

                if (rfl.size()==0) {
                    String msg=mContext.getString(R.string.msgs_dir_empty);
                    mUtil.showCommonDialog(false,"W",msg,"",null);
                    return;
                }

                //カスタムダイアログの生成
                final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.item_select_list_dlg);

                LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
//                ll_dlg_view.setBackgroundColor(mGp.themeColorList.title_background_color);

                final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
                final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
                final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
                title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
                title.setTextColor(mGp.themeColorList.title_text_color);

                title.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));
                subtitle.setText((remdir.equals("//")) ? host_name+host_addr+"/"+host_share : host_name+host_addr+"/"+host_share+  remdir);
                final TextView dlg_msg = (TextView) dialog.findViewById(R.id.item_select_list_dlg_msg);
                final Button btn_ok = (Button) dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
                dlg_msg.setVisibility(TextView.VISIBLE);

                CommonDialog.setDlgBoxSizeLimit(dialog, true);

                final ListView lv = (ListView) dialog.findViewById(R.id.list_view);
                final TreeFilelistAdapter tfa = new TreeFilelistAdapter(mActivity, false, false);
                final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
                for (int i = 0; i < rfl.size(); i++) {
                    if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
                }
                Collections.sort(rows);

                tfa.setDataList(rows);
                lv.setAdapter(tfa);
                lv.setScrollingCacheEnabled(false);
                lv.setScrollbarFadingEnabled(false);

                NotifyEvent ntfy_expand_close = new NotifyEvent(mContext);
                ntfy_expand_close.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        int idx = (Integer) o[0];
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        expandHideRemoteDirTree(ssi, pos, tfi, tfa);
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}
                });
                tfa.setExpandCloseListener(ntfy_expand_close);
                lv.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        expandHideRemoteDirTree(ssi, pos, tfi, tfa);
                    }
                });
                lv.setOnItemLongClickListener(new OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> items, View view, int idx, long id) {
                        final int pos = tfa.getItem(idx);
                        final TreeFilelistItem tfi = tfa.getDataItem(pos);
                        tfi.setChecked(true);
                        tfa.notifyDataSetChanged();
                        return true;
                    }
                });

                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                //Listen setRemoteShare response
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] o) {
                        Integer pos=(Integer)o[0];
                        boolean isChecked=(boolean)o[1];
                        TreeFilelistItem tfi=tfa.getDataItem(pos);
                        String sel="";
                        if (tfi.getPath().length() == 1) sel = tfi.getName();
                        else sel = tfi.getPath() + tfi.getName();
                        try {
                            sel = sel.substring(remdir.length());
                            String dup_filter=getDuplicateFilter(sel, fla);
                            if (!dup_filter.equals("")) {
                                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                                tfi.setChecked(false);
                                tfa.notifyDataSetChanged();
                                String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                                String dup_msg= String.format(mtxt, sel);
                                Toast toast=CommonDialog.getToastLong(mActivity, dup_msg);
                                toast.show();
                            } else {
                                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                            }
                        } catch(Exception e) {
                            mUtil.showCommonDialog(false,"E","Error","sel="+sel+", remdir="+remdir+"\n"+
                                    e.getMessage()+"\n"+ MiscUtil.getStackTraceString(e),null);
                        }
                    }

                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                        if (tfa.isDataItemIsSelected()) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    }
                });
                tfa.setCbCheckListener(ntfy);

                btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
                btn_ok.setVisibility(Button.VISIBLE);
                btn_ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (!addDirFilter(true, tfa, fla, remdir, dlg_msg, sti, true)) return;
                        addDirFilter(false, tfa, fla, remdir, dlg_msg, sti, true);
                        dialog.dismiss();
                        p_ntfy.notifyToListener(true, null);
                    }
                });

                final Button btn_cancel = (Button) dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
                btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
                btn_cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        p_ntfy.notifyToListener(true, null);
                    }
                });

                dialog.setOnCancelListener(new Dialog.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        btn_cancel.performClick();
                    }
                });
                dialog.show();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                p_ntfy.notifyToListener(false, o);
            }
        });
        createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, remdir, ntfy, true);
    }

    private boolean addDirFilter(boolean check_only, TreeFilelistAdapter tfa,
                                 FilterListAdapter fla, String cdir, TextView dlg_msg, SyncTaskItem sti, boolean smb_filter) {
        String sel = "", add_msg = "";
        //check duplicate entry
        for (int i = 0; i < tfa.getCount(); i++) {
            if (tfa.getDataItem(i).isChecked()) {
                if (tfa.getDataItem(i).getPath().length() == 1) sel = "/"+tfa.getDataItem(i).getName();
                else {
                    sel = tfa.getDataItem(i).getPath() + tfa.getDataItem(i).getName();
                    if (sel.startsWith("/")) {
                        if (cdir.endsWith("/")) sel = sel.substring(cdir.length());
                        else sel = sel.substring(cdir.length()+1);
                    }
                }
                String dup_filter=getDuplicateFilter(sel, fla);
                if (!dup_filter.equals("")) {
                    String mtxt = mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
                    dlg_msg.setText(String.format(mtxt, sel));
                    return false;
                } else {
                    dlg_msg.setText("");
                }
                if (!smb_filter) {
                }
                if (!check_only) {
                    fla.add(new FilterListItem(sel, true, true));
                    if (add_msg.length() == 0) add_msg = sel;
                    else add_msg = add_msg + "," + sel;
                }
            }
        }
        if (!check_only) {
            fla.setNotifyOnChange(true);
            fla.sort();
            dlg_msg.setText(String.format(mContext.getString(R.string.msgs_filter_list_dlg_filter_added),
                    add_msg));
        }
        return true;
    }

    private String detectedInvalidChar = "", detectedInvalidCharMsg = "";

    public boolean hasInvalidChar(String in_text, String[] invchar) {
        for (int i = 0; i < invchar.length; i++) {
            if (in_text.indexOf(invchar[i]) >= 0) {
                if (invchar[i].equals("\t")) {
                    detectedInvalidCharMsg = "TAB";
                    detectedInvalidChar = "\t";
                } else {
                    detectedInvalidCharMsg = detectedInvalidChar = invchar[i];
                }
                return true;
            }

        }
        return false;
    }

    public String getInvalidCharMsg() {
        return detectedInvalidCharMsg;
    }

    public String removeInvalidChar(String in) {
        if (detectedInvalidChar == null || detectedInvalidChar.length() == 0) return in;
        String out = "";
        for (int i = 0; i < in.length(); i++) {
            if (in.substring(i, i + 1).equals(detectedInvalidChar)) {
                //ignore
            } else {
                out = out + in.substring(i, i + 1);
            }
        }
        return out;
    }

    public boolean isSyncTaskExists(String prof_name) {
        return isSyncTaskExists(prof_name, mGp.syncTaskListAdapter.getArrayList());
    }

    static public boolean isSyncTaskExists(String prof_name, ArrayList<SyncTaskItem> pfl) {
        boolean dup = false;

        for (int i = 0; i <= pfl.size() - 1; i++) {
            SyncTaskItem item = pfl.get(i);
            String prof_chk = item.getSyncTaskName();
            if (prof_chk.equalsIgnoreCase(prof_name)) {
                dup = true;
                break;
            }
        }
        return dup;
    }

    static public boolean isSyncTaskAuto(GlobalParameters gp,
                                         String prof_name) {
        boolean active = false;

        for (int i = 0; i <= gp.syncTaskListAdapter.getCount() - 1; i++) {
            String item_key = gp.syncTaskListAdapter.getItem(i).getSyncTaskName();
            if (item_key.equalsIgnoreCase(prof_name)) {
                active = gp.syncTaskListAdapter.getItem(i).isSyncTaskAuto();
            }
        }
        return active;
    }

    static public boolean isSyncTaskSelected(SyncTaskListAdapter pa) {
        boolean result = false;

        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked()) {
                result = true;
                break;
            }
        }
        return result;
    }

    static public int getSyncTaskSelectedItemCount(SyncTaskListAdapter pa) {
        int result = 0;

        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked()) {
                result++;
            }
        }
        return result;
    }

    private void createLocalFilelist(final boolean dironly, final String uuid, final String dir, final NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1, "I", "createLocalFilelist entered. dir="+dir);
        final long b_time= System.currentTimeMillis();
        final Dialog pd=CommonDialog.showProgressSpinIndicator(mActivity);
        final ThreadCtrl tc = new ThreadCtrl();
        tc.setEnabled();
        tc.setThreadResultSuccess();
        pd.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tc.setDisabled();//disableAsyncTask();
                mUtil.addDebugMsg(1, "W", "localFileList creation was cancelled.");
            }
        });
        pd.show();

        final Handler hndl = new Handler();
        Thread th=new Thread(){
             @Override
             public void run() {
                  final ArrayList<TreeFilelistItem> tfl = new ArrayList<TreeFilelistItem>();
                  String fp="";
                  if (dir.equals("")) fp = "/";
                  else {
                      fp = dir + "/";
                  }

                  SafFile3 lf =new SafFile3(mContext, dir);
                  final SafFile3[] ff = lf.listFiles();
                  TreeFilelistItem tfi = null;
                  if (ff != null) {
                      for (int i = 0; i < ff.length; i++) {
                          if (!tc.isEnabled()) break;
                          if (ff[i].canRead()) {
                              int dirct = 0;
                              if (ff[i].isDirectory()) {
                                  SafFile3[] lfl = ff[i].listFiles();
                                  if (lfl != null) {
                                      for (int j = 0; j < lfl.length; j++) {
                                          if (lfl[j].canRead()) {
                                              if (!tc.isEnabled()) break;
                                              if (dironly) {
                                                  if (lfl[j].isDirectory()) dirct++;
                                              } else {
                                                  dirct++;
                                              }
                                          }
                                      }
                                  }
                              }
                              tfi = new TreeFilelistItem(ff[i].getName(),
                                      "" + ", ", ff[i].isDirectory(), 0, 0, false,
                                      ff[i].canRead(), ff[i].canWrite(),
                                      ff[i].isHidden(), fp, 0);
                              tfi.setSubDirItemCount(dirct);
                              if (ff[i].isDirectory() && ff[i].canRead()) {
                                  tfl.add(tfi);
                              }
                          }
                      }
                      if (tc.isEnabled()) Collections.sort(tfl);
                  }
                  hndl.post(new Runnable(){
                      @Override
                      public void run() {
                          mUtil.addDebugMsg(1, "I", "createLocalFilelist ended, tc="+tc.isEnabled()+", elapsed time="+(System.currentTimeMillis()-b_time));
                          p_ntfy.notifyToListener(tc.isEnabled(), new Object[]{tfl});
                          pd.dismiss();
                      }
                  });
             }
        };
        th.start();
    }

    private void createRemoteFileList(String opcd, SmbServerInfo ssi, String remdir,
                                      final NotifyEvent p_event, boolean readSubDirCnt) {
        mUtil.addDebugMsg(1, "I", "createRemoteFilelist entered.");
        final long b_time= System.currentTimeMillis();

        final ArrayList<TreeFilelistItem> remoteFileList = new ArrayList<TreeFilelistItem>();
        final ThreadCtrl tc = new ThreadCtrl();
        tc.setEnabled();
        tc.setThreadResultSuccess();

        final Dialog dialog=CommonDialog.showProgressSpinIndicator(mActivity);

        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tc.setDisabled();//disableAsyncTask();
                mUtil.addDebugMsg(1, "W", "createRemoteFileList cancelled.");
            }
        });

        final Handler hndl = new Handler();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                hndl.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        String err;
                        Collections.sort(remoteFileList);
                        mUtil.addDebugMsg(1, "I", "createRemoteFileList result=" + tc.getThreadResult() +
                                ", msg=" + tc.getThreadMessage() + ", tc=" + tc.isEnabled()+", elapsed time="+(System.currentTimeMillis()-b_time));
                        if (tc.isThreadResultSuccess()) {
                            p_event.notifyToListener(true, new Object[]{remoteFileList});
                        } else {
                            if (tc.isThreadResultError()) {
                                String suggest_msg=getJcifsErrorSugestionMessage(mContext, tc.getThreadMessage());
                                if (suggest_msg.equals("")) err = mContext.getString(R.string.msgs_filelist_error) + "\n" + tc.getThreadMessage();
                                else err = mContext.getString(R.string.msgs_filelist_error)+"\n"+suggest_msg+"\n" + tc.getThreadMessage();
                                p_event.notifyToListener(false, new Object[]{err});
                            }
                        }
                    }
                });
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {}
        });

        Thread tf = new Thread(new ReadSmbFilelist(mContext, tc, opcd, ssi, remdir, remoteFileList, ntfy, true, readSubDirCnt, mGp));
        tf.start();

        dialog.show();
    }

    public void selectRemoteShareDlg(SmbServerInfo ssi, final NotifyEvent p_ntfy) {

        NotifyEvent ntfy = new NotifyEvent(mContext);
        // set thread response
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                final ArrayList<String> rows = new ArrayList<String>();
                @SuppressWarnings("unchecked")
                ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>) o[0];
                for (TreeFilelistItem item:rfl) rows.add(item.getName());
                if (rows.size() < 1) {
                    mUtil.showCommonDialog(false, "W",
                            mContext.getString(R.string.msgs_share_list_not_obtained), "", null);
                    return;
                }
                Collections.sort(rows, String.CASE_INSENSITIVE_ORDER);
                //カスタムダイアログの生成
                final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.item_select_list_dlg);

                LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
                CommonUtilities.setDialogBoxOutline(mContext, ll_dlg_view);
//                ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

                final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
                final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
                final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
                title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
                title.setTextColor(mGp.themeColorList.title_text_color);
//                subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);

                title.setText(mContext.getString(R.string.msgs_select_remote_share));
                subtitle.setVisibility(TextView.GONE);

                final Button btn_cancel = (Button) dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
                final Button btn_ok = (Button) dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);

                CommonDialog.setDlgBoxSizeLimit(dialog, false);

                final ListView lv = (ListView) dialog.findViewById(R.id.list_view);
                lv.setAdapter(new ArrayAdapter<String>(mActivity,
                        android.R.layout.simple_list_item_single_choice, rows));
                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                lv.setScrollingCacheEnabled(false);
                lv.setScrollbarFadingEnabled(false);

                lv.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                    }
                });
                //CANCELボタンの指定
                btn_cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        p_ntfy.notifyToListener(false, null);
                    }
                });
                //OKボタンの指定
                btn_ok.setVisibility(Button.VISIBLE);
                btn_ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        SparseBooleanArray checked = lv.getCheckedItemPositions();
                        for (int i = 0; i <= rows.size(); i++) {
                            if (checked.get(i) == true) {
                                p_ntfy.notifyToListener(true, new Object[]{rows.get(i)});
                                break;
                            }
                        }
                    }
                });
                // Cancelリスナーの指定
                dialog.setOnCancelListener(new Dialog.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        btn_cancel.performClick();
                    }
                });
                dialog.show();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                p_ntfy.notifyToListener(false, o);
            }
        });
        createRemoteFileList(ReadSmbFilelist.OPCD_READ_SHARE, ssi, null, ntfy, false);

    }

    private void expandHideRemoteDirTree(SmbServerInfo ssi, final int pos,
                                         final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
        if (tfi.getSubDirItemCount() == 0) return;
        if (tfi.isChildListExpanded()) {
            tfa.hideChildItem(tfi, pos);
        } else {
            if (tfi.isSubDirLoaded())
                tfa.reshowChildItem(tfi, pos);
            else {
                if (tfi.isSubDirLoaded())
                    tfa.reshowChildItem(tfi, pos);
                else {
                    NotifyEvent ne = new NotifyEvent(mContext);
                    ne.setListener(new NotifyEvent.NotifyEventListener() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void positiveResponse(Context c, Object[] o) {
                            tfa.addChildItem(tfi, (ArrayList<TreeFilelistItem>) o[0], pos);
                        }

                        @Override
                        public void negativeResponse(Context c, Object[] o) {
                        }
                    });
                    createRemoteFileList(ReadSmbFilelist.OPCD_READ_FILELIST, ssi, tfi.getPath() + tfi.getName() + "/", ne, true);
                }
            }
        }
    }

    private void expandHideLocalDirTree(boolean dironly, String uuid, final int pos, final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
        if (tfi.getSubDirItemCount() == 0) return;
        if (tfi.isChildListExpanded()) {
            tfa.hideChildItem(tfi, pos);
        } else {
            if (tfi.isSubDirLoaded())
                tfa.reshowChildItem(tfi, pos);
            else {
                if (tfi.isSubDirLoaded()) tfa.reshowChildItem(tfi, pos);
                else {
                    NotifyEvent ne = new NotifyEvent(mContext);
                    ne.setListener(new NotifyEvent.NotifyEventListener() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void positiveResponse(Context c, Object[] o) {
                            tfa.addChildItem(tfi, (ArrayList<TreeFilelistItem>) o[0], pos);
                        }

                        @Override
                        public void negativeResponse(Context c, Object[] o) {
                            mUtil.showCommonDialog(false, "E", "Local file list creation aborted", "", null);
                        }
                    });
                    createLocalFilelist(dironly, uuid, tfi.getPath() + tfi.getName(), ne);
                }
            }
        }
    }

    static public void sortSyncTaskList(ArrayList<SyncTaskItem> items) {
        Collections.sort(items, new Comparator<SyncTaskItem>() {
            @Override
            public int compare(SyncTaskItem l_item, SyncTaskItem r_item) {

                if (l_item.getSyncTaskPosition() == r_item.getSyncTaskPosition())
                    return l_item.getSyncTaskName().compareToIgnoreCase(r_item.getSyncTaskName());
                else {
                    String l_key = String.format("%3d", l_item.getSyncTaskPosition()) + l_item.getSyncTaskName();
                    String r_key = String.format("%3d", r_item.getSyncTaskPosition()) + r_item.getSyncTaskName();
                    return l_key.compareToIgnoreCase(r_key);
                }
            }
        });
        for (int i = 0; i < items.size(); i++) items.get(i).setSyncTaskPosition(i);
    }

}