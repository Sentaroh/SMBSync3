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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.CommonFileSelector2;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;

import com.sentaroh.android.SMBSync3.SyncConfiguration.SettingParameterItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.crypto.SecretKey;

import static com.sentaroh.android.SMBSync3.Constants.APPLICATION_TAG;
import static com.sentaroh.android.SMBSync3.Constants.GENERAL_IO_BUFFER_SIZE;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.ENCRYPT_MODE_ENCRYPT_VITAL_DATA;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.ENCRYPT_MODE_ENCRYPT_WHOLE_DATA;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.ENCRYPT_MODE_NO_ENCRYPT;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.SYNC_TASK_CONFIG_FILE_IDENTIFIER_SUFFIX;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.SYNC_TASK_ENCRYPTED_CONFIG_FILE_IDENTIFIER;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.isSavedSyncTaskListFile;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.isSavedSyncTaskListFileEncrypted;
import static com.sentaroh.android.SMBSync3.SyncConfiguration.createConfigurationDataArray;

public class TaskListImportExport {
    private static Logger log = LoggerFactory.getLogger(TaskListImportExport.class);

    private ActivityMain mActivity=null;
    private GlobalParameters mGp=null;
    private CommonUtilities mUtil=null;

    public TaskListImportExport(ActivityMain a, GlobalParameters gp, CommonUtilities cu) {
        mActivity=a;
        mGp=gp;
        mUtil=cu;
    }

    public void exportSyncTaskListDlg() {
        mUtil.addDebugMsg(1,"I","exportSyncTaskListDlg entered");
        NotifyEvent ntfy_file_select = new NotifyEvent(mActivity);
        ntfy_file_select.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                final Uri fpath = (Uri) o[0];
                NotifyEvent ntfy_pswd = new NotifyEvent(mActivity);
                ntfy_pswd.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mGp.profilePassword = (String) o[0];
                        boolean encrypt_required = false;
                        if (!mGp.profilePassword.equals("")) encrypt_required = true;
                        exportSyncTaskListToFile(fpath, encrypt_required);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                promptPasswordForExport(ntfy_pswd);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String dt=sdf.format(System.currentTimeMillis());
        String fn=APPLICATION_TAG+"_config_"+dt+".stf";

        CommonFileSelector2 fsdf=
                CommonFileSelector2.newInstance(true, true, false, CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE,
                        true, SafManager3.SAF_FILE_PRIMARY_UUID, "/SMBSync3", fn, mActivity.getString(R.string.msgs_select_export_file));
        fsdf.showDialog(false, mActivity.getSupportFragmentManager(), fsdf, ntfy_file_select);
    }

    public void exportSyncTaskListToFile(final Uri file_uri, final boolean encrypt_required) {
        final SafFile3 fsf=new SafFile3(mActivity, file_uri);
        NotifyEvent ntfy = new NotifyEvent(mActivity);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                String pswd=encrypt_required?mGp.profilePassword:null;
                try {
                    if (!fsf.exists()) fsf.createNewFile();
                } catch (Exception e) {
                    String msg="Export file creation error. error=" + e.getMessage() +", File="+fsf.getPath();
                    mUtil.addDebugMsg(1, "I", msg);
                    mUtil.showCommonDialog(false, "E",
                            mActivity.getString(R.string.msgs_export_prof_fail), msg, null);
                    e.printStackTrace();
                    return;
                }
                boolean rc= saveTaskListToExportFile(mActivity, fsf, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList, pswd);
                if (rc) {
                    mUtil.showCommonDialog(false, "I",
                            mActivity.getString(R.string.msgs_export_prof_success), "File=" + fsf.getPath(), null);
                    mUtil.addDebugMsg(1, "I", "Profile was exported. fn=" + fsf.getPath());
                    putExportedFileList(fsf);
                } else {
                    mUtil.showCommonDialog(false, "E",
                            mActivity.getString(R.string.msgs_export_prof_fail), "File=" + fsf.getPath(), null);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (fsf!=null && fsf.exists()) {
            mUtil.showCommonDialog(true, "W", mActivity.getString(R.string.msgs_export_prof_title),
                    fsf.getPath() + " " + mActivity.getString(R.string.msgs_override), ntfy);
        } else {
            ntfy.notifyToListener(true, null);
        }
    }

    private static final String SAVED_SYNC_TASK_FILE_LIST=".saved_sync_task_file_list";
    private void putExportedFileList(SafFile3 sf) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        ArrayList<SafFile3> saved_file_list=getExportedFileList();
        boolean save_required=false;
        boolean found=false;
        for(SafFile3 item:saved_file_list) {
            if (item.getPath().equals(sf.getPath())) {
                found=true;
                break;
            }
        }
        if (!found) {
            saved_file_list.add(sf);
            Collections.sort(saved_file_list, new Comparator<SafFile3>(){
                @Override
                public int compare(SafFile3 o1, SafFile3 o2) {
                    return (int)(o2.lastModified()-o1.lastModified());
                }
            });
            if (saved_file_list.size()>9) {
                for(int i=9;i<saved_file_list.size();i++) saved_file_list.remove(i);
            }
            save_required=true;
        }
        if (save_required) {
            String saved_list="";
            for(SafFile3 fp_item:saved_file_list) {
                saved_list+=fp_item.getPath()+"\n";
            }

            try {
                SafFile3 df=new SafFile3(mActivity, mGp.settingAppManagemsntDirectoryName);
                if (!df.exists()) df.mkdirs();
                SafFile3 lf=new SafFile3(mActivity, mGp.settingAppManagemsntDirectoryName+"/"+SAVED_SYNC_TASK_FILE_LIST);
                /*WROKAROUND */ if (lf.exists()) lf.delete();
                lf.createNewFile();
                OutputStream fos=lf.getOutputStream();
                fos.write(saved_list.getBytes());
                fos.flush();
                fos.close();
                mUtil.addDebugMsg(1,"I","Exported file list saved, list="+saved_list);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    private ArrayList<SafFile3> getExportedFileList() {
        String saved_list_string=null;
        try {
            SafFile3 df=new SafFile3(mActivity, mGp.settingAppManagemsntDirectoryName+"/"+SAVED_SYNC_TASK_FILE_LIST);
            if (df.exists()) {
                InputStream fis=df.getInputStream();
                byte[] buff=new byte[GENERAL_IO_BUFFER_SIZE];
                int rc=fis.read(buff);
                fis.close();
                saved_list_string=new String(buff,0,rc);
            }
        } catch(Exception e) {}
        ArrayList<SafFile3> file_list=new ArrayList<SafFile3>();
        if (saved_list_string!=null) {
            String[] saved_array=saved_list_string.split("\n");
            for (String saved_fp:saved_array) {
                if (saved_fp!=null && !saved_fp.equals("")) {
                    SafFile3 sf=new SafFile3(mActivity, saved_fp);
                    if (sf!=null && sf.exists()) file_list.add(sf);
                }
            }
            Collections.sort(file_list, new Comparator<SafFile3>(){
                @Override
                public int compare(SafFile3 o1, SafFile3 o2) {
                    return (int)(o2.lastModified()-o1.lastModified());
                }
            });
        }
        return file_list;
    }

    public void importSyncTaskListDlg(final NotifyEvent p_ntfy) {
        final ArrayList<String> auto_saved_selector_list=new ArrayList<String>();
        final ArrayList<SafFile3> auto_saved_file_list=createAutoSaveFileList(mActivity, mGp, mUtil);
        Collections.sort(auto_saved_file_list, new Comparator<SafFile3>() {
            @Override
            public int compare(SafFile3 o1, SafFile3 o2) {
//                return (int)(o2.lastModified()-o1.lastModified());
                return o2.getName().compareToIgnoreCase(o1.getName());
            }
        });
        SimpleDateFormat sdf =new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        boolean latest_used=false;
        for(SafFile3 item:auto_saved_file_list) {
            String entry="";
            if (!latest_used) {
                latest_used=true;
                entry= String.format(mActivity.getString(R.string.msgs_import_autosave_dlg_autosave_enty_item_latest), sdf.format(item.lastModified()));
            } else {
                entry= String.format(mActivity.getString(R.string.msgs_import_autosave_dlg_autosave_enty_item), sdf.format(item.lastModified()));
            }
            auto_saved_selector_list.add(entry);
        }

        final ArrayList<SafFile3> manual_save_file_list=getExportedFileList();
        final ArrayList<String> manual_save_selector_list=new ArrayList<String>();
        for(int i=0;i<manual_save_file_list.size();i++) {
            SafFile3 item=manual_save_file_list.get(i);
            String dt=sdf.format(item.lastModified());
            String entry="";
            if (i==0) entry= String.format(mActivity.getString(R.string.msgs_import_autosave_dlg_autosave_enty_item_latest), dt);
            else entry= String.format(mActivity.getString(R.string.msgs_import_autosave_dlg_autosave_enty_item), dt);

            manual_save_selector_list.add(item.getPath()+"\n"+entry);
        }

        if (auto_saved_selector_list.size()==0 && manual_save_selector_list.size()==0) {
            importSyncTaskListDlgWithFileSelection(p_ntfy);
            return;
        }

        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.import_autosave_dlg);

        final LinearLayout ll_title=(LinearLayout) dialog.findViewById(R.id.import_autosave_dlg_title_view);
        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView tv_title=(TextView)dialog.findViewById(R.id.import_autosave_dlg_title);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
//        if (Build.VERSION.SDK_INT>=23) tv_msg.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);

        final Button btn_ok=(Button)dialog.findViewById(R.id.import_autosave_dlg_btn_ok);
        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.import_autosave_dlg_btn_cancel);
        final Button btn_select=(Button)dialog.findViewById(R.id.import_autosave_dlg_select_exported_file);

        CommonDialog.setDlgBoxSizeLimit(dialog,true);


        final ListView auto_save_list_view = (ListView) dialog.findViewById(R.id.import_autosave_dlg_autosave_listview);
        final ListView manual_save_list_view = (ListView) dialog.findViewById(R.id.import_autosave_dlg_manual_save_listview);

        SavedTaskListFileSelectorAdapter manual_save_adapter=new SavedTaskListFileSelectorAdapter(mActivity, R.layout.sync_task_list_save_file_selector_item,
                manual_save_selector_list);
        manual_save_list_view.setAdapter(manual_save_adapter);
        manual_save_list_view.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        manual_save_list_view.setScrollingCacheEnabled(false);
        manual_save_list_view.setScrollbarFadingEnabled(false);
        manual_save_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                auto_save_list_view.setItemChecked(-1, true);
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        SavedTaskListFileSelectorAdapter auto_save_adapter=new SavedTaskListFileSelectorAdapter(mActivity, R.layout.sync_task_list_save_file_selector_item,
                auto_saved_selector_list);
        auto_save_list_view.setAdapter(auto_save_adapter);
        auto_save_list_view.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        auto_save_list_view.setScrollingCacheEnabled(false);
        auto_save_list_view.setScrollbarFadingEnabled(false);
        auto_save_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
                manual_save_list_view.setItemChecked(-1, true);
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importSyncTaskListDlgWithFileSelection(p_ntfy);
                Handler hndl=new Handler();
                hndl.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                },500);
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SparseBooleanArray checked_auto_save = auto_save_list_view.getCheckedItemPositions();
                SparseBooleanArray checked_manual_save = manual_save_list_view.getCheckedItemPositions();
                boolean auto_selected=false;
                for (int i = 0; i <= auto_saved_selector_list.size(); i++) {
                    if (checked_auto_save.get(i) == true) {
                        importSyncTaskList(p_ntfy, auto_saved_file_list.get(i), true);
                        auto_selected=true;
                        break;
                    }
                }
                if (!auto_selected) {
                    for (int i = 0; i <= manual_save_file_list.size(); i++) {
                        if (checked_manual_save.get(i) == true) {
                            importSyncTaskList(p_ntfy, manual_save_file_list.get(i), false);
                            break;
                        }
                    }
                }
                Handler hndl=new Handler();
                hndl.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                },100);
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_cancel.performClick();
            }
        });

        dialog.show();

    }

    private static ArrayList<SafFile3> createAutoSaveFileList(Context c, GlobalParameters mGp, CommonUtilities util) {
        ArrayList<SafFile3> as_fl=new ArrayList<SafFile3>();
        SafFile3 df=new SafFile3(c, mGp.settingAppManagemsntDirectoryName+"/autosave");
        SafFile3[] fl=df.listFiles();
        if (fl!=null) {
            for(SafFile3 item:fl) {
                if (item.isFile() && item.getName().endsWith(".stf")) {
                    as_fl.add(item);
                }
            }
            Collections.sort(as_fl, new Comparator<SafFile3>(){
                @Override
                public int compare(SafFile3 o1, SafFile3 o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }
        return as_fl;
    }


    public void importSyncTaskListDlgWithFileSelection(final NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1,"I","importSyncTaskListDlg entered");

        NotifyEvent ntfy = new NotifyEvent(mActivity);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                final Uri fpath = (Uri) o[0];
                SafFile3 sf=new SafFile3(mActivity, fpath);
                importSyncTaskList(p_ntfy, sf, false);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        CommonFileSelector2 fsdf=
                CommonFileSelector2.newInstance(true, false, false, CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE,
                        true, true, SafManager3.SAF_FILE_PRIMARY_UUID, "", "", mActivity.getString(R.string.msgs_select_import_file));
        fsdf.showDialog(false, mActivity.getSupportFragmentManager(), fsdf, ntfy);
    }

    private void importSyncTaskList(final NotifyEvent p_ntfy, final SafFile3 sf, final boolean from_auto_save) {
        if (isSavedSyncTaskListFile(mActivity, sf)) {
            importSMBSync3SyncTaskList(p_ntfy, sf, from_auto_save);
        } else {
            if (TaskListImportFromSMBSync2.isSMBSync2SyncTaskList(mActivity, mGp, mUtil, sf)) {
                importSMBSync2SyncTaskList(p_ntfy, sf);
            } else {
                mUtil.showCommonDialog(false, "W",
                        String.format(mActivity.getString(R.string.msgs_export_import_profile_specified_file_was_not_sync_task_saved_file), sf.getPath()),
                        "", null);
                p_ntfy.notifyToListener(false, null);
            }
        }
    }

    private void importSMBSync3SyncTaskList(final NotifyEvent p_ntfy, final SafFile3 sf, final boolean from_auto_save) {
        NotifyEvent ntfy_pswd = new NotifyEvent(mActivity);
        ntfy_pswd.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mGp.profilePassword = (String) o[0];
                ArrayList<SyncTaskItem>sync_task=new ArrayList<SyncTaskItem>();
                ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched=new ArrayList<ScheduleListAdapter.ScheduleListItem>();
                ArrayList<GroupListAdapter.GroupListItem>sync_group=new ArrayList<GroupListAdapter.GroupListItem>();
                ArrayList<SettingParameterItem>sync_setting=new ArrayList<SettingParameterItem>();
                if (from_auto_save) loadTaskListFromAutosaveFile(mActivity, sf, sync_task, sync_sched, sync_setting, sync_group);
                else loadTaskListFromExportFile(mActivity, sf, sync_task, sync_sched, sync_setting, sync_group, mGp.profilePassword);
                final TaskListAdapter tfl = new TaskListAdapter(mActivity, R.layout.sync_task_item_view, sync_task, mGp);
                if (tfl.getCount() > 0) {
                    importSyncTaskItemSelector(false, tfl, sync_sched, sync_setting, sync_group, sf, p_ntfy, from_auto_save);
                } else {
                    mUtil.showCommonDialog(false, "W", mActivity.getString(R.string.msgs_export_import_profile_no_import_items), "", null);
                    p_ntfy.notifyToListener(false, null);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String[] config_array= SyncConfiguration.createConfigurationDataArray(sf);
        if (isSavedSyncTaskListFile(mActivity, sf)) {
            if (isSavedSyncTaskListFileEncrypted(mActivity, sf)) {
                promptPasswordForImport(sf, config_array[1], ntfy_pswd);
            } else {
                ntfy_pswd.notifyToListener(true, new Object[]{""});
            }
        } else {
            mUtil.showCommonDialog(false, "W",
                    String.format(mActivity.getString(R.string.msgs_export_import_profile_specified_file_was_not_sync_task_saved_file), sf.getPath()),
                    "", null);
            p_ntfy.notifyToListener(false, null);
        }
    }

    private void importSMBSync2SyncTaskList(final NotifyEvent p_ntfy, final SafFile3 sf) {
        NotifyEvent ntfy_pswd = new NotifyEvent(mActivity);
        ntfy_pswd.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mGp.profilePassword = (String) o[0];
                ArrayList<SyncTaskItem>sync_task=new ArrayList<SyncTaskItem>();
                ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched=new ArrayList<ScheduleListAdapter.ScheduleListItem>();
                ArrayList<GroupListAdapter.GroupListItem>sync_group=new ArrayList<GroupListAdapter.GroupListItem>();
                ArrayList<SettingParameterItem>sync_setting=new ArrayList<SettingParameterItem>();
                TaskListImportFromSMBSync2.buildSyncTaskList(mActivity, mGp, mUtil, sf, mGp.profilePassword, sync_task, sync_sched);
                final TaskListAdapter tfl = new TaskListAdapter(mActivity, R.layout.sync_task_item_view, sync_task, mGp);
                if (tfl.getCount() > 0) {
                    importSyncTaskItemSelector(true, tfl, sync_sched, sync_setting, sync_group, sf, p_ntfy, false);
                } else {
                    mUtil.showCommonDialog(false, "W", mActivity.getString(R.string.msgs_export_import_profile_no_import_items), "", null);
                    p_ntfy.notifyToListener(false, null);
                }
            }
            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String[] config_array= SyncConfiguration.createConfigurationDataArray(sf);
        if (TaskListImportFromSMBSync2.isSyncTaskListEncrypted(mActivity, mGp, mUtil, sf)) {
            promptPasswordForImport(sf, config_array[0], ntfy_pswd);
        } else {
            ntfy_pswd.notifyToListener(true, new Object[]{""});
        }
    }

    public void promptPasswordForImport(SafFile3 sf, String enc_data, final NotifyEvent ntfy_pswd) {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.password_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mActivity, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.password_input_title);
//        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
//        title.setTextColor(mGp.themeColorList.text_color_dialog_title);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
        final CheckedTextView ctv_protect = (CheckedTextView) dialog.findViewById(R.id.password_input_ctv_protect);
        final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
        final EditText et_password = (EditText) dialog.findViewById(R.id.password_input_password);
        final EditText et_confirm = (EditText) dialog.findViewById(R.id.password_input_password_confirm);
        et_confirm.setVisibility(EditText.GONE);
        btn_ok.setText(mActivity.getString(R.string.msgs_export_import_pswd_btn_ok));
        ctv_protect.setVisibility(CheckedTextView.GONE);

        dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_password_required));

        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (arg0.length() > 0) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        //OK button
        final Handler hndl=new Handler();

        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String passwd = et_password.getText().toString();

                boolean correct_password=false;
                if (TaskListImportFromSMBSync2.isSMBSync2SyncTaskList(mActivity, mGp, mUtil, enc_data)) {
                    correct_password= TaskListImportFromSMBSync2.isCorrectPassowrd(enc_data.substring(9), passwd);
                } else {
                    correct_password= isCorrectPassword(mActivity, enc_data, passwd);
                }

                if (correct_password) {
                    CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                    dialog.dismiss();
                    ntfy_pswd.notifyToListener(true, new Object[]{passwd});
                } else {
                    mUtil.showCommonDialog(false, "E", mActivity.getString(R.string.msgs_export_import_pswd_invalid_password), "", null);
                }

            }
        });
        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                ntfy_pswd.notifyToListener(false, null);
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

    public void promptPasswordForExport(final NotifyEvent ntfy_pswd) {

        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.setContentView(R.layout.password_input_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mActivity, ll_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.password_input_title);
//        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
//        title.setTextColor(mGp.themeColorList.text_color_dialog_title);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
        final CheckedTextView ctv_protect = (CheckedTextView) dialog.findViewById(R.id.password_input_ctv_protect);
        final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
        final EditText et_password = (EditText) dialog.findViewById(R.id.password_input_password);
        final EditText et_confirm = (EditText) dialog.findViewById(R.id.password_input_password_confirm);

        dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_specify_password));

        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);

        ctv_protect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv_protect.toggle();
                boolean isChecked = ctv_protect.isChecked();
                setPasswordFieldVisibility(isChecked, et_password, et_confirm, btn_ok, dlg_msg);
            }
        });

        ctv_protect.setChecked(mGp.settingExportedTaskEncryptRequired);
        setPasswordFieldVisibility(mGp.settingExportedTaskEncryptRequired, et_password, et_confirm, btn_ok, dlg_msg);

        CommonDialog.setViewEnabled(mActivity, et_password, true);
        CommonDialog.setViewEnabled(mActivity, et_confirm, false);
        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                setPasswordPromptOkButton(et_password, et_confirm, btn_ok, dlg_msg);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        et_confirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                setPasswordPromptOkButton(et_password, et_confirm, btn_ok, dlg_msg);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

        //OK button
        btn_ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String passwd = et_password.getText().toString();
                if ((ctv_protect.isChecked() && !mGp.settingExportedTaskEncryptRequired) ||
                        (!ctv_protect.isChecked() && mGp.settingExportedTaskEncryptRequired)) {
                    mGp.settingExportedTaskEncryptRequired = ctv_protect.isChecked();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    prefs.edit().putBoolean(mActivity.getString(R.string.settings_exported_profile_encryption),
                            ctv_protect.isChecked()).commit();
                }
                if (!ctv_protect.isChecked()) {
                    dialog.dismiss();
                    ntfy_pswd.notifyToListener(true, new Object[]{""});
                } else {
                    if (!passwd.equals(et_confirm.getText().toString())) {
                        //Unmatch
                        dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
                    } else {
                        dialog.dismiss();
                        ntfy_pswd.notifyToListener(true, new Object[]{passwd});
                    }
                }
            }
        });

        // CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                ntfy_pswd.notifyToListener(false, null);
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

    private void setPasswordFieldVisibility(boolean isChecked, EditText et_password,
                                            EditText et_confirm, Button btn_ok, TextView dlg_msg) {
        if (isChecked) {
            et_password.setVisibility(EditText.VISIBLE);
            et_confirm.setVisibility(EditText.VISIBLE);
            setPasswordPromptOkButton(et_password, et_confirm, btn_ok, dlg_msg);
        } else {
            dlg_msg.setText("");
            et_password.setVisibility(EditText.GONE);
            et_confirm.setVisibility(EditText.GONE);
            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
        }
    }

    private void setPasswordPromptOkButton(EditText et_passwd, EditText et_confirm,
                                           Button btn_ok, TextView dlg_msg) {
        String password = et_passwd.getText().toString();
        String confirm = et_confirm.getText().toString();
        if (password.length() > 0 && et_confirm.getText().length() == 0) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
            CommonDialog.setViewEnabled(mActivity, et_confirm, true);
        } else if (password.length() > 0 && et_confirm.getText().length() > 0) {
            CommonDialog.setViewEnabled(mActivity, et_confirm, true);
            if (!password.equals(confirm)) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
            } else {
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                dlg_msg.setText("");
            }
        } else if (password.length() == 0 && confirm.length() == 0) {
            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_specify_password));
            CommonDialog.setViewEnabled(mActivity, et_passwd, true);
            CommonDialog.setViewEnabled(mActivity, et_confirm, false);
        } else if (password.length() == 0 && confirm.length() > 0) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
        }
    }

    private void importSyncTaskItemSelector(final boolean from_smbsync2, final TaskListAdapter tfl, final ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched,
                                            final ArrayList<SettingParameterItem>sync_setting, ArrayList<GroupListAdapter.GroupListItem>sync_group,
                                            final SafFile3 sf, final NotifyEvent p_ntfy, final boolean from_auto_save) {
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.export_import_profile_dlg);
        dialog.setCanceledOnTouchOutside(false);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.export_import_profile_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        ArrayList<ImportTaskListItemAdapter.ExportImportListItem> eipl = new ArrayList<ImportTaskListItemAdapter.ExportImportListItem>();

        for (int i = 0; i < tfl.getCount(); i++) {
            SyncTaskItem pl = tfl.getItem(i);
            ImportTaskListItemAdapter.ExportImportListItem eipli = new ImportTaskListItemAdapter.ExportImportListItem();

            eipli.isChecked = true;
            eipli.item_name = pl.getSyncTaskName();
            eipl.add(eipli);
        }
        final ImportTaskListItemAdapter imp_list_adapt = new ImportTaskListItemAdapter(mActivity, R.layout.export_import_profile_list_item, eipl);

        ListView lv = (ListView) dialog.findViewById(R.id.export_import_profile_listview);
        lv.setAdapter(imp_list_adapt);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.export_import_profile_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.export_import_profile_title);
//        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
//        title.setTextColor(mGp.themeColorList.text_color_dialog_title);
        title.setText(mActivity.getString(R.string.msgs_export_import_profile_title));
        final TextView from_path = (TextView) dialog.findViewById(R.id.export_import_profile_from_file_path);
        from_path.setVisibility(TextView.VISIBLE);
        from_path.setText(sf.getPath());
//        LinearLayout ll_filelist = (LinearLayout) dialog.findViewById(R.id.export_import_profile_file_list);
//        ll_filelist.setVisibility(LinearLayout.GONE);
        final Button ok_btn = (Button) dialog.findViewById(R.id.export_import_profile_dlg_btn_ok);
        Button cancel_btn = (Button) dialog.findViewById(R.id.export_import_profile_dlg_btn_cancel);

        final Button rb_select_all = (Button) dialog.findViewById(R.id.export_import_profile_list_select_all);
        final Button rb_unselect_all = (Button) dialog.findViewById(R.id.export_import_profile_list_unselect_all);
        final CheckedTextView ctv_import_schedule = (CheckedTextView) dialog.findViewById(R.id.export_import_profile_list_ctv_restore_schedule);
        final CheckedTextView ctv_import_group = (CheckedTextView) dialog.findViewById(R.id.export_import_profile_list_ctv_restore_group);
        if (sync_sched.size()==0) CommonDialog.setViewEnabled(mActivity, ctv_import_schedule, false);
        ctv_import_schedule.setChecked(true);
        ctv_import_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                CommonDialog.setViewEnabled(mActivity, ok_btn, true);
            }
        });
        if (sync_group.size()==0) CommonDialog.setViewEnabled(mActivity, ctv_import_group, false);
        ctv_import_group.setChecked(true);
        ctv_import_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                CommonDialog.setViewEnabled(mActivity, ok_btn, true);
            }
        });
        final CheckedTextView ctv_import_setting = (CheckedTextView) dialog.findViewById(R.id.export_import_profile_list_ctv_restore_setting);
        if (sync_setting.size()==0) CommonDialog.setViewEnabled(mActivity, ctv_import_setting, false);
        ctv_import_setting.setChecked(true);
        ctv_import_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                CommonDialog.setViewEnabled(mActivity, ok_btn, true);
            }
        });
        final CheckedTextView ctv_reset_profile = (CheckedTextView) dialog.findViewById(R.id.export_import_profile_list_ctv_reset_profile);

        ctv_reset_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CheckedTextView) v).toggle();
                setImportOkBtnEnabled(ctv_reset_profile, imp_list_adapt, ok_btn);
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                setImportOkBtnEnabled(ctv_reset_profile, imp_list_adapt, ok_btn);
            }
        });

        rb_select_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < imp_list_adapt.getCount(); i++)
                    imp_list_adapt.getItem(i).isChecked = true;
                imp_list_adapt.notifyDataSetChanged();
                ctv_import_group.setChecked(true);
                ctv_import_schedule.setChecked(true);
                ctv_import_setting.setChecked(true);
                CommonDialog.setViewEnabled(mActivity, ok_btn, true);
            }
        });
        rb_unselect_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < imp_list_adapt.getCount(); i++)
                    imp_list_adapt.getItem(i).isChecked = false;
                imp_list_adapt.notifyDataSetChanged();
                ctv_import_group.setChecked(false);
                ctv_import_schedule.setChecked(false);
                ctv_import_setting.setChecked(false);
                CommonDialog.setViewEnabled(mActivity, ok_btn, false);
            }
        });

        NotifyEvent ntfy_ctv_listener = new NotifyEvent(mActivity);
        ntfy_ctv_listener.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setImportOkBtnEnabled(ctv_reset_profile, imp_list_adapt, ok_btn);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        imp_list_adapt.setCheckButtonListener(ntfy_ctv_listener);


        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (ctv_reset_profile.isChecked()) mGp.syncTaskListAdapter.clear();
                importSelectedSyncTaskItem(from_smbsync2, imp_list_adapt, tfl, sync_sched, sync_setting, sync_group,
                        p_ntfy, from_auto_save,
                        ctv_import_schedule.isChecked(), ctv_import_setting.isChecked(), ctv_import_group.isChecked());
                dialog.dismiss();
            }
        });
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void setImportOkBtnEnabled(
            final CheckedTextView ctv_reset_profile,
            final ImportTaskListItemAdapter imp_list_adapt,
            final Button ok_btn) {
        if (imp_list_adapt.isItemSelected()) CommonDialog.setViewEnabled(mActivity, ok_btn, true);
        else CommonDialog.setViewEnabled(mActivity, ok_btn, false);
    }

    private void importSelectedSyncTaskItem(final boolean from_smbsync2,
                                            final ImportTaskListItemAdapter imp_list_adapt,
                                            final TaskListAdapter tfl,
                                            final ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched,
                                            final ArrayList<SettingParameterItem>sync_setting,
                                            final ArrayList<GroupListAdapter.GroupListItem>sync_group,
                                            final NotifyEvent p_ntfy, final boolean from_auto_save, final boolean import_schedule,
                                            final boolean import_setting, final boolean import_group) {
        String repl_list = "";
        for (int i = 0; i < imp_list_adapt.getCount(); i++) {
            ImportTaskListItemAdapter.ExportImportListItem eipli = imp_list_adapt.getItem(i);
            if (eipli.isChecked &&
                    TaskListUtils.getSyncTaskByName(mGp.syncTaskListAdapter, eipli.item_name) != null) {
                repl_list += eipli.item_name + "\n";
            }
        }

        NotifyEvent ntfy = new NotifyEvent(mActivity);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                String imp_list_temp = "";
                String imp_error_temp = "";
                for (int i = 0; i < tfl.getCount(); i++) {
                    SyncTaskItem ipfli = tfl.getItem(i);
                    ImportTaskListItemAdapter.ExportImportListItem eipli = imp_list_adapt.getItem(i);
                    if (eipli.isChecked) {
                        imp_list_temp +="-"+ipfli.getSyncTaskName() + "\n";
                        SyncTaskItem mpfli = TaskListUtils.getSyncTaskByName(mGp.syncTaskListAdapter, ipfli.getSyncTaskName());
                        if (mpfli != null) {
                            mGp.syncTaskListAdapter.remove(mpfli);
                            ipfli.setSyncTaskPosition(mpfli.getSyncTaskPosition());
                            mGp.syncTaskListAdapter.add(ipfli);
                        } else {
                            ipfli.setSyncTaskPosition(mGp.syncTaskListAdapter.getCount());
                            mGp.syncTaskListAdapter.add(ipfli);
                        }
                        if (ipfli.isSyncFolderStatusError()) {
                            imp_error_temp+=ipfli.getSyncTaskName()+"\n";
                        }
                    }
                }
                mGp.syncTaskListAdapter.sort();
                mGp.syncTaskView.setSelection(0);

                if (import_schedule && sync_sched!=null && sync_sched.size()>0) {
                    mGp.syncScheduleList.clear();
                    mGp.syncScheduleList.addAll(sync_sched);
                    imp_list_temp+="-"+mActivity.getString(R.string.msgs_export_import_profile_schedule_parms)+"\n";
                }

                if (import_group && sync_group!=null && sync_group.size()>0) {
                    mGp.syncGroupList.clear();
                    mGp.syncGroupList.addAll(sync_group);
                    imp_list_temp+="-"+mActivity.getString(R.string.msgs_export_import_profile_group_parms)+"\n";
                }

                if (import_setting && sync_setting!=null && sync_setting.size()>0) {
                    putSettingParm(mActivity, sync_setting);
                    imp_list_temp+="-"+mActivity.getString(R.string.msgs_export_import_profile_setting_parms)+"\n";
                }

                if (imp_list_temp.length() > 0) imp_list_temp += " ";
                final String imp_list=imp_list_temp;
                final String imp_smb=imp_error_temp;

                final String config_data= saveTaskListToAppDirectory(mActivity, mGp.syncTaskList, mGp.syncScheduleList, mGp.syncGroupList);
                if (config_data!=null) {
                    NotifyEvent ntfy_success=new NotifyEvent(mActivity);
                    ntfy_success.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            if (from_auto_save && !imp_smb.equals("")) {
                                NotifyEvent ntfy_decrypt_warning=new NotifyEvent(mActivity);
                                ntfy_decrypt_warning.setListener(new NotifyEvent.NotifyEventListener() {
                                    @Override
                                    public void positiveResponse(Context context, Object[] objects) {
                                        p_ntfy.notifyToListener(true, null);
                                    }
                                    @Override
                                    public void negativeResponse(Context context, Object[] objects) {}
                                });
                                mUtil.showCommonDialog(false, "W",mActivity.getString(R.string.msgs_import_autosave_dlg_title),
                                        mActivity.getString(R.string.msgs_export_import_profile_smb_account_name_password_not_restored)+"\n\n"+imp_smb, ntfy_decrypt_warning);
                                mUtil.addLogMsg("W","",
                                        mActivity.getString(R.string.msgs_export_import_profile_smb_account_name_password_not_restored)+"\n"+imp_smb);
                            } else {
                                p_ntfy.notifyToListener(true, null);
                            }
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    String msg_text="", level="I";
                    if (from_smbsync2) {
                        msg_text=mActivity.getString(R.string.msgs_export_import_profile_import_success_from_smbsync2)+"\n\n"+imp_list;
                        level="W";
                        mUtil.showCommonDialog(false, level, mActivity.getString(R.string.msgs_import_autosave_dlg_title),
                                msg_text, Color.RED, ntfy_success);
                        mUtil.addLogMsg(level, "", msg_text);
                    } else {
                        msg_text=mActivity.getString(R.string.msgs_export_import_profile_import_success)+"\n\n"+imp_list;
                        mUtil.showCommonDialog(false, level, mActivity.getString(R.string.msgs_import_autosave_dlg_title),
                                msg_text, ntfy_success);
                        mUtil.addLogMsg(level, "", msg_text);
                    }
//                    SpannableStringBuilder sb=new SpannableStringBuilder(msg_text);
//                    ForegroundColorSpan fg_span = new ForegroundColorSpan(mGp.themeColorList.text_color_error);
//                    sb.setSpan(fg_span, 0, msg_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                } else {
                    mUtil.showCommonDialog(false, "E",mActivity.getString(R.string.msgs_import_autosave_dlg_title),
                            mActivity.getString(R.string.msgs_export_import_profile_import_failed), null);
                    mUtil.addLogMsg("E","", mActivity.getString(R.string.msgs_export_import_profile_import_failed));
                    p_ntfy.notifyToListener(true, null);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (!repl_list.equals("")) {
            //Confirm
            mUtil.showCommonDialog(true, "W", mActivity.getString(R.string.msgs_import_autosave_dlg_title),
                    mActivity.getString(R.string.msgs_export_import_profile_confirm_override)+"\n\n"+repl_list, ntfy);
        } else {
            ntfy.notifyToListener(true, null);
        }

    }

    final static private String CONFIG_FILE_NAME = "config.xml";
    public static String saveTaskListToAppDirectory(Context c,
                                                    ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list, ArrayList<GroupListAdapter.GroupListItem>group_list) {
        try {
            SecretKey sk = KeyStoreUtils.getStoredKey(c, KeyStoreUtils.KEY_STORE_ALIAS);
            EncryptUtilV3.CipherParms cp_int = EncryptUtilV3.initCipherEnv(sk, KeyStoreUtils.KEY_STORE_ALIAS);

            String config_data = SyncConfiguration.createXmlData(c, sync_task_list, schedule_list, group_list, ENCRYPT_MODE_ENCRYPT_VITAL_DATA, cp_int);

            OutputStream os = c.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            BufferedOutputStream bos = new BufferedOutputStream(os, GENERAL_IO_BUFFER_SIZE);
            PrintWriter pw = new PrintWriter(bos);
            long cal_crc=SyncConfiguration.calculateSyncConfigCrc32(config_data.replaceAll("\n", ""));
//            log.info("saved crc="+cal_crc);
            pw.println(SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX+cal_crc+SYNC_TASK_CONFIG_FILE_IDENTIFIER_SUFFIX);
            pw.println(config_data);

            pw.flush();
            pw.close();
            return config_data;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+" failed.", e);
            return null;
        }
    }

    final static private int MAX_AUTOSAVE_FILE_COUNT = 50;

    public static boolean saveTaskListToAutosave(Activity a, Context c, String management_directory, String config_data) {
        try {
            SafFile3 asd = new SafFile3(c, management_directory + "/autosave");
            if (asd != null) {
                if (!asd.exists()) asd.mkdirs();
                SafFile3[] fl = asd.listFiles();
                if (fl.length > MAX_AUTOSAVE_FILE_COUNT) {
                    ArrayList<SafFile3> sfl = new ArrayList<SafFile3>();
                    for (SafFile3 sf_item : fl) sfl.add(sf_item);
                    Collections.sort(sfl, new Comparator<SafFile3>() {
                        @Override
                        public int compare(SafFile3 l1, SafFile3 r1) {
                            return (int) (l1.lastModified() - r1.lastModified());
                        }
                    });
                    if (sfl.size() >= (MAX_AUTOSAVE_FILE_COUNT - 1)) {
                        for (int i = 0; i < (sfl.size() - (MAX_AUTOSAVE_FILE_COUNT - 1)); i++) {
                            sfl.get(i).deleteIfExists();
                            if (log.isDebugEnabled())
                                log.debug(CommonUtilities.getExecutedMethodName()+" autosave file was deleted, name=" + sfl.get(i).getName());
                        }
                    }
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String fn = "autosave_" + sdf.format(System.currentTimeMillis()) + ".stf";

                SafFile3 asf = new SafFile3(c, management_directory + "/autosave/" + fn);
                if (asf != null) {
                    if (!asf.exists()) asf.createNewFile();
                    OutputStream os = asf.getOutputStream();
                    BufferedOutputStream bos = new BufferedOutputStream(os, GENERAL_IO_BUFFER_SIZE);
                    PrintWriter pw = new PrintWriter(bos);
                    long cal_crc=SyncConfiguration.calculateSyncConfigCrc32(config_data.replaceAll("\n", ""));
                    pw.println(SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX+cal_crc+SYNC_TASK_CONFIG_FILE_IDENTIFIER_SUFFIX);
                    pw.println(config_data);
                    pw.flush();
                    pw.close();
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = CommonDialog.getToastShort(a, c.getString(R.string.msgs_import_autosave_dlg_autosave_completed));
                            toast.setGravity(Gravity.BOTTOM, 0, (int) CommonUtilities.toPixel(a.getResources(), 150));
                            toast.show();
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+"  failed.", e);
        }
        return false;
    }

    public static boolean saveTaskListToExportFile(Context c, Uri file_uri,
                                                   ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list, ArrayList<GroupListAdapter.GroupListItem>group_list, String priv_key) {
        SafFile3 sf = new SafFile3(c, file_uri);
        return saveTaskListToExportFile(c, sf, sync_task_list, schedule_list, group_list, priv_key);
    }

    public static boolean saveTaskListToExportFile(Context c, SafFile3 sf,
                                                   ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                                                   ArrayList<GroupListAdapter.GroupListItem>group_list, String priv_key) {
        try {
            EncryptUtilV3.CipherParms cp_int = null;
            String config_data = null;
            String file_id= "";
            if (priv_key != null && !priv_key.equals("")) {
                cp_int = EncryptUtilV3.initCipherEnv(priv_key);
                config_data = SyncConfiguration.createXmlData(c, sync_task_list, schedule_list, group_list, ENCRYPT_MODE_ENCRYPT_WHOLE_DATA, cp_int);
                file_id=SYNC_TASK_ENCRYPTED_CONFIG_FILE_IDENTIFIER;
            } else {
                config_data = SyncConfiguration.createXmlData(c, sync_task_list, schedule_list, group_list, ENCRYPT_MODE_NO_ENCRYPT, null);
                long cal_crc=SyncConfiguration.calculateSyncConfigCrc32(config_data.replaceAll("\n", ""));
                file_id=SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX+cal_crc+SYNC_TASK_CONFIG_FILE_IDENTIFIER_SUFFIX;
            }
            if (config_data != null) {
                try {
                    if (sf != null) {
                        OutputStream os = sf.getOutputStream();
                        os.write((file_id + "\n" + config_data + "\n").getBytes());
                        os.flush();
                        os.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+"  failed.", e);
            return false;
        }
    }

    public static boolean loadTaskListFromAppDirectory(Context c,
                                                       ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                                                       ArrayList<SettingParameterItem> setting_parm_list, ArrayList<GroupListAdapter.GroupListItem> group_list) {
        boolean result = false;
        try {
            SecretKey priv_key = KeyStoreUtils.getStoredKey(c, KeyStoreUtils.KEY_STORE_ALIAS);
            EncryptUtilV3.CipherParms cp_int = null;
            if (priv_key != null) {
                cp_int = EncryptUtilV3.initCipherEnv(priv_key, KeyStoreUtils.KEY_STORE_ALIAS);

                InputStream fis = c.openFileInput(CONFIG_FILE_NAME);

                String[] config_array = SyncConfiguration.createConfigurationDataArray(fis);
                result=SyncConfiguration.isSavedSyncTaskListFile(c, config_array);
                if (result) {
                    result = SyncConfiguration.buildConfigurationList(c, config_array[1], sync_task_list, schedule_list, setting_parm_list,
                            group_list, ENCRYPT_MODE_ENCRYPT_VITAL_DATA, cp_int);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+" failed.", e);
        }
        return result;
    }

    public static boolean loadTaskListFromAutosaveFile(Context c, SafFile3 sf,
                                                       ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                                                       ArrayList<SettingParameterItem> setting_parm_list, ArrayList<GroupListAdapter.GroupListItem> group_list) {
        boolean result = false;
        try {
            String[] config_array = SyncConfiguration.createConfigurationDataArray(sf);

            if (isSavedSyncTaskListFile(c, config_array)) {
                SecretKey priv_key = KeyStoreUtils.getStoredKey(c, KeyStoreUtils.KEY_STORE_ALIAS);
                EncryptUtilV3.CipherParms cp_int = null;
                if (priv_key != null) {
                    cp_int = EncryptUtilV3.initCipherEnv(priv_key, KeyStoreUtils.KEY_STORE_ALIAS);
                    result = SyncConfiguration.buildConfigurationList(c, config_array[1], sync_task_list, schedule_list, setting_parm_list,
                            group_list, ENCRYPT_MODE_ENCRYPT_VITAL_DATA, cp_int);
                } else {
                    log.error(CommonUtilities.getExecutedMethodName()+" decrypt initialize failed.");
                }
            } else {
                log.error(CommonUtilities.getExecutedMethodName()+" Specified file was not sync task list.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+" failed.", e);
        }
        return result;
    }

    public static boolean isCorrectPassword(Context c, String tl_data, String dec_key) {
        if (tl_data == null || tl_data.equals("")) return false;
        if (dec_key == null || dec_key.equals("")) return false;

        boolean result = false;
        EncryptUtilV3.CipherParms cp_int = null;
        if (dec_key != null && !dec_key.equals("")) {
            cp_int = EncryptUtilV3.initCipherEnv(dec_key);
            String dec_str = CommonUtilities.decryptUserData(c, cp_int, tl_data);
            if (dec_str != null) result = true;
        }
        return result;
    }

    public static boolean loadTaskListFromExportFile(Context c, SafFile3 sf,
                                                     ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                                                     ArrayList<SettingParameterItem> setting_parm_list, ArrayList<GroupListAdapter.GroupListItem> group_list,
                                                     String dec_key) {
        boolean result = false;
        try {
            String[] config_array = SyncConfiguration.createConfigurationDataArray(sf);

            if (isSavedSyncTaskListFile(c, config_array)) {
                if (isSavedSyncTaskListFileEncrypted(c, config_array)) {
                    EncryptUtilV3.CipherParms cp_int = null;
                    if (dec_key != null && !dec_key.equals("")) {
                        cp_int = EncryptUtilV3.initCipherEnv(dec_key);
                        result = SyncConfiguration.buildConfigurationList(c, config_array[1], sync_task_list, schedule_list, setting_parm_list,
                                group_list, ENCRYPT_MODE_ENCRYPT_WHOLE_DATA, cp_int);
                    }
                } else {
                    result = SyncConfiguration.buildConfigurationList(c, config_array[1], sync_task_list, schedule_list, setting_parm_list,
                            group_list, ENCRYPT_MODE_NO_ENCRYPT, null);
                }
            } else {
                log.error(CommonUtilities.getExecutedMethodName()+" Specified file was not sync task list.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(CommonUtilities.getExecutedMethodName()+" failed.", e);
        }
        return result;
    }

    public static boolean putSettingParm(Context c, ArrayList<SettingParameterItem> setting_parm_list) {
        boolean result = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        try {
            SharedPreferences.Editor pe = prefs.edit();
            for (SettingParameterItem sp : setting_parm_list) {
                if (sp.type.equals(SyncConfiguration.SYNC_TASK_XML_TAG_SETTINGS_TYPE_BOOLEAN)) {
                    if (sp.value.equals("true")) pe.putBoolean(sp.key, true);
                    else pe.putBoolean(sp.key, false);
                } else if (sp.type.equals(SyncConfiguration.SYNC_TASK_XML_TAG_SETTINGS_TYPE_STRING)) {
                    pe.putString(sp.key, sp.value);
                } else if (sp.type.equals(SyncConfiguration.SYNC_TASK_XML_TAG_SETTINGS_TYPE_INT)) {
                    pe.putInt(sp.key, Integer.valueOf(sp.value));
                } else if (sp.type.equals(SyncConfiguration.SYNC_TASK_XML_TAG_SETTINGS_TYPE_LONG)) {
                    pe.putLong(sp.key, Long.valueOf(sp.value));
                }
            }
            pe.commit();
        } catch (Exception e) {
            log.error(CommonUtilities.getExecutedMethodName()+" failed.", e);
            e.printStackTrace();
            result = false;
        }
        return result;
    }


    private class SavedTaskListFileSelectorAdapter extends ArrayAdapter<String> {
        private Context c;
        private int id;
        private ArrayList<String> items;

        public SavedTaskListFileSelectorAdapter(Context context, int resource, ArrayList<String> objects) {
            super(context, resource, objects);
            c = context;
            id = resource;
            items = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
                holder = new ViewHolder();
                holder.tv_text1 =(TextView) v.findViewById(android.R.id.text1);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            final String o = getItem(position);

            if (o != null) {
                holder.tv_text1.setText(o);
            }

            return v;
        }

        private class ViewHolder {
            TextView tv_text1;
            TextView tv_text2;
        }
    }

    static private class ImportTaskListItemAdapter extends ArrayAdapter<ImportTaskListItemAdapter.ExportImportListItem> {
        private Context c;
        private int id;
        private ArrayList<ExportImportListItem> items;

        public ImportTaskListItemAdapter(Context context, int textViewResourceId, ArrayList<ExportImportListItem> objects) {
            super(context, textViewResourceId, objects);
            c = context;
            id = textViewResourceId;
            items = objects;
        }

        @Override
        final public int getCount() {
            return items.size();
        }

        public boolean isItemSelected() {
            boolean result = false;

            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isChecked) {
                    result = true;
                    break;
                }
            }

            return result;
        }

        private NotifyEvent cb_ntfy = null;

        final public void setCheckButtonListener(NotifyEvent ntfy) {
            cb_ntfy = ntfy;
        }

        @Override
        final public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
                holder = new ViewHolder();
                holder.ctv_item = (CheckedTextView) v.findViewById(R.id.export_import_profile_list_item_item);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            final ExportImportListItem o = items.get(position);
            if (o != null) {
                holder.ctv_item.setText(o.item_name);
                // 必ずsetChecked前にリスナを登録
                holder.ctv_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.ctv_item.toggle();
                        boolean isChecked = holder.ctv_item.isChecked();
                        o.isChecked = isChecked;
                        if (cb_ntfy != null) cb_ntfy.notifyToListener(true, null);
                    }
                });
                holder.ctv_item.setChecked(o.isChecked);
            }
            return v;
        }

        private class ViewHolder {
            CheckedTextView ctv_item;
        }

        private static class ExportImportListItem {
            public boolean isChecked = false;
            public String item_name = "";
        }

    }

}
