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
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapButton;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import java.util.ArrayList;

import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync3.Constants.NAME_UNUSABLE_CHARACTER;

public class GroupEditor {
    private GlobalParameters mGp = null;

    private AppCompatActivity mActivity = null;

    private CommonUtilities mUtil = null;

    private GroupListAdapter.GroupListItem mGroupListItem = null;

    private ArrayList<GroupListAdapter.GroupListItem> mGroupList = null;
    private NotifyEvent mNotify = null;

    private boolean mEditMode = true;

    private boolean mInitialTime = true;

    private ThemeColorList mThemeColorList = null;

    private String mCurrentSyncTaskList="";

    public GroupEditor(CommonUtilities mu, AppCompatActivity a, GlobalParameters gp,
                       boolean edit_mode, ArrayList<GroupListAdapter.GroupListItem> gl,
                       GroupListAdapter.GroupListItem gi, NotifyEvent ntfy) {
        mActivity = a;
        mGp = gp;
        mUtil = mu;
        mGroupListItem = gi;

        mEditMode = edit_mode;

        mNotify = ntfy;
        mGroupList = gl;

        mThemeColorList = ThemeUtil.getThemeColorList(a);

        initDialog();

        Handler hndl=new Handler();
        hndl.postDelayed(new Runnable() {
            @Override
            public void run() {
                mInitialTime=false;
            }
        },500);
    }

//    private void setGroupChangedx(Dialog dialog, GroupItem curr_si) {
//        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_dlg_ok);
//        if (mInitialTime) {
//            mGroupChanged =!mEditMode;
//        } else {
////            Thread.dumpStack();
//            GroupItem new_si=curr_si.clone();
//            buildGroupItem(dialog, new_si);
//            if (mEditMode) mGroupChanged = !curr_si.isSame(new_si);
//            else mGroupChanged =true;
//            CommonDialog.setButtonEnabled(mActivity, btn_ok, mGroupChanged);
//        }
//    }

    private boolean isGroupChanged(Dialog dialog) {
        boolean result=false;
        if (mEditMode) {
            GroupListAdapter.GroupListItem new_si= mGroupListItem.clone();
            buildGroupItem(dialog, new_si);
            result = !mGroupListItem.isSame(new_si);
        } else {
            result=true;
        }

        return result;
    }

    private void initDialog() {
        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.group_item_edit_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) dialog.findViewById(R.id.group_item_edit_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);
        TextView dlg_subtitle = (TextView) dialog.findViewById(R.id.group_item_edit_dlg_title_sub);
        dlg_subtitle.setVisibility(TextView.VISIBLE);
        dlg_subtitle.setTextColor(mGp.themeColorList.title_text_color);
        final ImageButton ib_help=(ImageButton) dialog.findViewById(R.id.group_item_edit_dlg_help);

        ib_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskEditor.showFieldHelp(mActivity, mGp,
                        mActivity.getString(R.string.msgs_help_group_title),
                        mActivity.getString(R.string.msgs_help_group_file)) ;
            }
        });

        final Spinner sp_assigned_button=(Spinner)dialog.findViewById(R.id.group_item_edit_dlg_assigned_button);
        setSpinnerAssignedButton(mGp, sp_assigned_button, mGroupListItem, mGroupListItem.button);

        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_dlg_ok);

        final Button btn_cancel = (Button) dialog.findViewById(R.id.group_item_edit_dlg_cancel);

        final LinearLayout ll_sync_task_list_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_task_view);
        final NonWordwrapButton btn_edit = (NonWordwrapButton) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
//        btn_edit.setDebugEnable(true);
//        btn_edit.setWordWrapEnabled(false);
        final TextView tv_msg = (TextView) dialog.findViewById(R.id.group_item_edit_dlg_msg);

        final LinearLayout ll_group_name_view=(LinearLayout)dialog.findViewById(R.id.group_item_edit_dlg_group_name_view);
        final EditText et_name = (EditText) dialog.findViewById(R.id.group_item_edit_dlg_group_name);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        setViewVisibility(dialog);

        sp_assigned_button.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setOkButtonEnabledGroupEditor(dialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (mEditMode) {
            et_name.setText(mGroupListItem.groupName);
//            et_name.setVisibility(EditText.GONE);
            ll_group_name_view.setVisibility(LinearLayout.GONE);
            String subtitle=" ("+ mGroupListItem.groupName+")";
            dlg_subtitle.setText(subtitle);
            dlg_title.setText(mActivity.getString(R.string.msgs_group_edit_title_edit));
        } else {
            dlg_title.setText(mActivity.getString(R.string.msgs_group_edit_title_add));
            dlg_subtitle.setVisibility(TextView.GONE);
            String new_name="";
            for(int i=0;i<10000;i++) {
                if (i==0) new_name=mActivity.getString(R.string.msgs_group_edit_title_new_group_name);
                else new_name=mActivity.getString(R.string.msgs_group_edit_title_new_group_name)+" ("+i+")";
                if (!isGroupExists(mGroupList, new_name)) {
                    et_name.setText(new_name);
                    break;
                }
            }
            mGroupListItem.position=mGroupList.size();
        }

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
//                if (s.length()>0) {
//                    if (s.toString().startsWith(GROUP_SYSTEM_PREFIX)) {
//                        tv_msg.setText(mActivity.getString(R.string.msgs_group_list_edit_dlg_error_group_name_not_allowed_asterisk_in_first_char));
//                        return;
//                    }
//                }
                setOkButtonEnabledGroupEditor(dialog);
            }
        });

        ctv_auto_task_only.setChecked(mGroupListItem.autoTaskOnly);
        setViewVisibility(dialog);
        ctv_auto_task_only.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!((CheckedTextView)view).isChecked();
                ((CheckedTextView)view).setChecked(isChecked);
                setOkButtonEnabledGroupEditor(dialog);
                setViewVisibility(dialog);
            }
        });

        mCurrentSyncTaskList= mGroupListItem.taskList;
        setEditTaskListButtonLabel(dialog);
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mActivity);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        String prof_list = (String) o[0];
                        tv_msg.setText("");
                        mCurrentSyncTaskList=prof_list;
                        setEditTaskListButtonLabel(dialog);
                        setOkButtonEnabledGroupEditor(dialog);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                EditSyncTaskList est=new EditSyncTaskList(mActivity, mGp, mUtil);
                est.editSyncTaskList(mCurrentSyncTaskList, ntfy);
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                buildGroupItem(dialog, mGroupListItem);
                if (mNotify != null) mNotify.notifyToListener(true, new Object[]{mGroupListItem});
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_ok.isEnabled()) {//isGroupChanged(dialog)) {
                    NotifyEvent ntfy = new NotifyEvent(mActivity);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                        }
                    });
                    mUtil.showCommonDialog(true, "W",
                            mActivity.getString(R.string.msgs_edit_sync_task_list_confirm_msg_nosave), "", ntfy);
                } else {
                    dialog.dismiss();
                }


            }
        });

        setOkButtonEnabledGroupEditor(dialog);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                btn_cancel.performClick();
            }
        });

//        Handler hndl = new Handler();
//        hndl.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mInitialTime = false;
//                CommonDialog.setButtonEnabled(mActivity, btn_ok, !mEditMode);
//            }
//        }, 500);
//
        dialog.show();
    }

    private void setEditTaskListButtonLabel(Dialog dialog) {
        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        String stl=buildSyncTaskListInfo(mCurrentSyncTaskList);
        if (stl.equals("")) btn_edit.setText(mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_edit_prof));
        else btn_edit.setText(stl);
    }

    private String buildSyncTaskListInfo(String task_list) {
        String[] array=task_list.split(NAME_LIST_SEPARATOR);
        String out="", sep="";
        for(String item:array) {
            out+=sep+item;
            sep=", ";
        }
        return out;
    }

    private boolean isButtonAlreadyAssigned(GlobalParameters gp, GroupListAdapter.GroupListItem gi, int id) {
        boolean result=false;
        for(GroupListAdapter.GroupListItem item:gp.syncGroupList) {
//            if (!item.groupName.equals(gi.groupName) && item.assigned_button==id) {
            if (item.button ==id) {
                result=true;
                break;
            }
        }
        return result;
    }

    private void setSpinnerAssignedButton(GlobalParameters gp, Spinner spinner, GroupListAdapter.GroupListItem curr_item, int button_id) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mActivity.getString(R.string.msgs_group_select_button_title));
        spinner.setAdapter(adapter);

        adapter.add(mActivity.getString(R.string.msgs_group_button_not_assigned));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1) || button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1) adapter.add(mActivity.getString(R.string.msgs_group_name_for_shortcut1));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2) || button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2) adapter.add(mActivity.getString(R.string.msgs_group_name_for_shortcut2));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3) || button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3) adapter.add(mActivity.getString(R.string.msgs_group_name_for_shortcut3));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListAdapter.GroupListItem.BUTTON_SYNC_BUTTON) || button_id== GroupListAdapter.GroupListItem.BUTTON_SYNC_BUTTON) adapter.add(mActivity.getString(R.string.msgs_group_name_for_sync_button));

        int sel=0;
        for(int i=1;i<adapter.getCount();i++) {
            String item=adapter.getItem(i);
            if (item.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut1)) && button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1) {
                sel=i;
            }
            else if (item.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut2)) && button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2) {
                sel=i;
            }
            else if (item.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut3)) && button_id== GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3) {
                sel=i;
            }
            else if (item.equals(mActivity.getString(R.string.msgs_group_name_for_sync_button)) && button_id== GroupListAdapter.GroupListItem.BUTTON_SYNC_BUTTON) {
                sel=i;
            }
        }
        spinner.setSelection(sel);
    }

    private int getSpinnerAssignedButton(Spinner spinner) {
        String sel=(String)spinner.getSelectedItem();
        int result=0;
        if (sel==null) result= GroupListAdapter.GroupListItem.BUTTON_NOT_ASSIGNED;
        else if (sel.equals(mActivity.getString(R.string.msgs_group_button_not_assigned))) result= GroupListAdapter.GroupListItem.BUTTON_NOT_ASSIGNED;
        else if (sel.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut1))) result= GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1;
        else if (sel.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut2))) result= GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2;
        else if (sel.equals(mActivity.getString(R.string.msgs_group_name_for_shortcut3))) result= GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3;
        else if (sel.equals(mActivity.getString(R.string.msgs_group_name_for_sync_button))) result= GroupListAdapter.GroupListItem.BUTTON_SYNC_BUTTON;
        return result;
    }

    private boolean isAutoTaskExists(Dialog dialog) {
        boolean auto_found=false;
        for(SyncTaskItem sti:mGp.syncTaskList) {
            if (sti.isSyncTaskAuto() && !sti.isSyncTestMode()) {
                auto_found=true;
                break;
            }
        }
        return auto_found;
    }


    private String getNotExistsSyncTaskName(String task_list) {
        String error_item_name="";
        if (task_list.indexOf(NAME_LIST_SEPARATOR)>0) {
            String[] stl=task_list.split(NAME_LIST_SEPARATOR);
            String sep="";
            for(String stn:stl) {
                if (ScheduleUtils.getSyncTask(mGp,stn)==null) {
                    error_item_name=sep+stn;
                    sep= NAME_LIST_SEPARATOR;
                }
            }
        } else {
            if (ScheduleUtils.getSyncTask(mGp, task_list)==null) {
                error_item_name=task_list;
            }
        }
        return  error_item_name;
    }

    static public String hasGroupNameUnusableCharacter(Context c, String name) {
        for(String item:NAME_UNUSABLE_CHARACTER) {
            if (name.contains(item)) return c.getString(R.string.msgs_group_list_edit_dlg_error_schedule_name_contains_unusabel_character,item);
        }
        return "";
    }

    private void setOkButtonEnabledGroupEditor(Dialog dialog) {
        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_dlg_ok);
        final EditText et_name = (EditText) dialog.findViewById(R.id.group_item_edit_dlg_group_name);
        final TextView tv_msg = (TextView) dialog.findViewById(R.id.group_item_edit_dlg_msg);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
//        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        CommonDialog.setButtonEnabled(mActivity, btn_ok, !mEditMode);
        if (et_name.getText().length() == 0) {
            tv_msg.setText(mActivity.getString(R.string.msgs_group_list_edit_dlg_error_sync_list_name_does_not_specified));
            CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
        } else {
            String e_msg= hasGroupNameUnusableCharacter(mActivity, et_name.getText().toString());
            if (!e_msg.equals("")) {
                tv_msg.setText(e_msg);
                CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
                return;
            } else if (!mEditMode && isGroupExists(mGroupList, et_name.getText().toString().trim())) {
                //Name alread exists
                tv_msg.setText(mActivity.getString(R.string.msgs_group_list_edit_dlg_error_sync_list_name_already_exists));
                CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
                return;
            }
            if (!ctv_auto_task_only.isChecked()) {
                if (mCurrentSyncTaskList.equals("")) {
                    tv_msg.setText(mActivity.getString(R.string.msgs_group_edit_sync_task_list_not_specified));
                    CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
                    return;
                }
                String[]task_list_array=mCurrentSyncTaskList.split(NAME_LIST_SEPARATOR);
                String nf_task="", sep="";
                for(String item:task_list_array) {
                    SyncTaskItem sti= TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item);
                    if (sti==null) {
                        nf_task+=sep+item;
                        sep= NAME_LIST_SEPARATOR;
                    }
                }
                if (!nf_task.equals("")) {
                    tv_msg.setText(mActivity.getString(R.string.msgs_group_can_not_sync_specified_task_does_not_exists, nf_task));
                    return;
                }
            }
            if (ctv_auto_task_only.isChecked() && !isAutoTaskExists(dialog)) {
                tv_msg.setVisibility(TextView.VISIBLE);
                tv_msg.setText(mActivity.getString(R.string.msgs_auto_sync_task_not_found));
                return;
            }
            tv_msg.setText("");
            if (isGroupChanged(dialog)) CommonDialog.setButtonEnabled(mActivity, btn_ok, true);
            else CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
        }

    }

    static public boolean isGroupExists(ArrayList<GroupListAdapter.GroupListItem> gl, String name) {
        boolean result=false;
        for(GroupListAdapter.GroupListItem item:gl) {
            if (item.groupName.equalsIgnoreCase(name)) {
                result=true;
                break;
            }
        }
        return result;
    }

    private void buildGroupItem(Dialog dialog, GroupListAdapter.GroupListItem gi) {
//        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        final EditText et_name = (EditText) dialog.findViewById(R.id.group_item_edit_dlg_group_name);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
        final Spinner sp_assigned_button=(Spinner)dialog.findViewById(R.id.group_item_edit_dlg_assigned_button);
        gi.taskList=mCurrentSyncTaskList;
        gi.autoTaskOnly=ctv_auto_task_only.isChecked();
        gi.groupName=et_name.getText().toString().trim();
        gi.button =getSpinnerAssignedButton(sp_assigned_button);
    }

    private void setViewVisibility(Dialog dialog) {
        final LinearLayout ll_sync_task_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_task_view);
//        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
        if (ctv_auto_task_only.isChecked()) ll_sync_task_view.setVisibility(LinearLayout.GONE);
        else ll_sync_task_view.setVisibility(LinearLayout.VISIBLE);
    }

    static public void removeSyncTaskFromGroup(GlobalParameters gp, CommonUtilities cu, String task_name) {
        for(GroupListAdapter.GroupListItem gli:gp.syncGroupList) {
            if (!gli.autoTaskOnly && !gli.taskList.equals("")) {
                String[] name_array=gli.taskList.split(NAME_LIST_SEPARATOR);
                String new_list="", sep="";
                for(String name_item:name_array) {
                    if (!name_item.equals(task_name)) {
                        new_list+=sep+name_item;
                        sep=NAME_LIST_SEPARATOR;
                        cu.addDebugMsg(1, "I", "Sync task name removed from froup="+gli.groupName+", task="+task_name);
                    }
                }
                if (!new_list.equals("")) gli.taskList=new_list;
            }
        }
    }

    static public void renameSyncTaskFromGroup(GlobalParameters gp, CommonUtilities cu, String old_name, String new_name) {
        for(GroupListAdapter.GroupListItem gli:gp.syncGroupList) {
            if (!gli.autoTaskOnly && !gli.taskList.equals("")) {
                String[] name_array=gli.taskList.split(NAME_LIST_SEPARATOR);
                String new_list="", sep="";
                for(String name_item:name_array) {
                    if (name_item.equals(old_name)) {
                        new_list+=sep+new_name;
                        sep=NAME_LIST_SEPARATOR;
                        cu.addDebugMsg(1, "I", "Sync task name rename from froup="+gli.groupName+", old task="+old_name+", new task="+new_name);
                    } else {
                        new_list+=sep+name_item;
                        sep=NAME_LIST_SEPARATOR;
                    }
                }
                if (!gli.taskList.equals(new_list)) gli.taskList=new_list;
            }
        }
    }

    static public String getSyncTaskList(Context c, GroupListAdapter.GroupListItem gli, ArrayList<SyncTaskItem>task_list) {
        if (gli.autoTaskOnly) {
            String list="", sep="";
            for(SyncTaskItem sti:task_list) {
                if (!sti.isSyncTaskError() && !sti.isSyncTestMode() && sti.isSyncTaskAuto()) {
                    list+=sep+sti.getSyncTaskName();
                    sep=",";
                }
            }
            return list;
        } else {
            return gli.taskList;
        }
    }

    static public String hasValidSyncTaskList(Context c, GroupListAdapter.GroupListItem o, ArrayList<SyncTaskItem>task_list) {
        String e_msg="";
        String[]task_list_array=o.taskList.split(NAME_LIST_SEPARATOR);
        for(String item:task_list_array) {
            SyncTaskItem sti= TaskListUtils.getSyncTaskByName(task_list, item);
            if (sti==null) {
                e_msg=c.getString(R.string.msgs_edit_sync_task_list_error_specified_task_does_not_exists, item);
                break;
            }
        }
        return e_msg;
    }

    static public String buildGroupExecuteSyncTaskList(Context c, GlobalParameters gp, CommonUtilities cu,
                                                       GroupListAdapter.GroupListItem gli, ArrayList<String>exec_task_list, ArrayList<String>duplicate_list) {
        String e_msg="";
        if (gli.autoTaskOnly) {
            for(SyncTaskItem sti:gp.syncTaskList) {
                if (!sti.isSyncTaskError() && !sti.isSyncTestMode() && sti.isSyncTaskAuto()) {
                    if (!exec_task_list.contains(sti.getSyncTaskName())) {
                        exec_task_list.add(sti.getSyncTaskName());
                    } else {
                        cu.addDebugMsg(1, "I", "Sync task already exists, Group="+gli.groupName+", task="+sti.getSyncTaskName());
                        duplicate_list.add(gli.groupName+NAME_LIST_SEPARATOR+sti.getSyncTaskName());
                    }
                }
            }
            if (exec_task_list.size()==0) {
                e_msg=c.getString(R.string.msgs_group_exec_sync_task_no_auto_task_exists, gli.groupName);
            }
        } else {
            String[] sync_task_array=gli.taskList.split(NAME_LIST_SEPARATOR);
            for(String tn_item:sync_task_array) {
                if (!exec_task_list.contains(tn_item)) {
                    SyncTaskItem sti= TaskListUtils.getSyncTaskByName(gp.syncTaskList, tn_item);
                    if (sti==null) {
                        e_msg=c.getString(R.string.msgs_group_exec_sync_task_no_sync_task_exists, gli.groupName, tn_item);
                    } else {
                        exec_task_list.add(tn_item);
                    }
                } else {
                    cu.addDebugMsg(1, "I", "Sync task already exists, Group="+gli.groupName+", task="+tn_item);
                    duplicate_list.add(gli.groupName+NAME_LIST_SEPARATOR+tn_item);
                }
            }
        }
        return e_msg;
    }

}
