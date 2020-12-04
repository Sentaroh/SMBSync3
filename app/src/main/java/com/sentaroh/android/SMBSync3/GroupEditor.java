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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sentaroh.android.Utilities3.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;

import java.util.ArrayList;

import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync3.Constants.NAME_UNUSABLE_CHARACTER;

class GroupEditor {
    private GlobalParameters mGp = null;

    private Context mContext = null;
    private AppCompatActivity mActivity = null;

    private CommonUtilities mUtil = null;

    private GroupListItem mGroupListItem = null;

    private ArrayList<GroupListItem> mGroupList = null;
    private NotifyEvent mNotify = null;

    private boolean mEditMode = true;

    private boolean mInitialTime = true;

    private ThemeColorList mThemeColorList = null;

    private String mCurrentSyncTaskList="";

    public GroupEditor(CommonUtilities mu, AppCompatActivity a, GlobalParameters gp,
                       boolean edit_mode, ArrayList<GroupListItem> gl,
                       GroupListItem gi, NotifyEvent ntfy) {
        mContext = a;
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
            GroupListItem new_si= mGroupListItem.clone();
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
                        mContext.getString(R.string.msgs_help_group_title),
                        mContext.getString(R.string.msgs_help_group_file)) ;
            }
        });

        final Spinner sp_assigned_button=(Spinner)dialog.findViewById(R.id.group_item_edit_dlg_assigned_button);
        setSpinnerAssignedButton(mGp, sp_assigned_button, mGroupListItem, mGroupListItem.button);

        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_dlg_ok);

        final Button btn_cancel = (Button) dialog.findViewById(R.id.group_item_edit_dlg_cancel);

        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        final TextView tv_msg = (TextView) dialog.findViewById(R.id.group_item_edit_dlg_msg);

        final EditText et_name = (EditText) dialog.findViewById(R.id.group_item_edit_dlg_group_name);
        final CheckedTextView ctv_group_enabled=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_enabled);
        CommonDialog.setViewEnabled(mActivity, ctv_group_enabled, true);
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
            et_name.setVisibility(EditText.GONE);
            String subtitle=" ("+ mGroupListItem.groupName+")";
            dlg_subtitle.setText(subtitle);
            dlg_title.setText(mContext.getString(R.string.msgs_group_edit_title_edit));
        } else {
            dlg_title.setText(mContext.getString(R.string.msgs_group_edit_title_add));
            dlg_subtitle.setVisibility(TextView.GONE);
            String new_name="";
            for(int i=0;i<10000;i++) {
                if (i==0) new_name=mContext.getString(R.string.msgs_group_edit_title_new_group_name);
                else new_name=mContext.getString(R.string.msgs_group_edit_title_new_group_name)+" ("+i+")";
                if (!isGroupExists(mGroupList, new_name)) {
                    et_name.setText(new_name);
                    break;
                }
            }
            mGroupListItem.position=mGroupList.size();
        }
        ctv_group_enabled.setChecked(mGroupListItem.enabled);
        ctv_group_enabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!((CheckedTextView)view).isChecked();
                ((CheckedTextView)view).setChecked(isChecked);
                setOkButtonEnabledGroupEditor(dialog);
            }
        });

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
//                        tv_msg.setText(mContext.getString(R.string.msgs_group_list_edit_dlg_error_group_name_not_allowed_asterisk_in_first_char));
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

        btn_edit.setText(buildSyncTaskListInfo(mCurrentSyncTaskList));
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        String prof_list = (String) o[0];
                        tv_msg.setText("");
                        mCurrentSyncTaskList=prof_list;
                        btn_edit.setText(buildSyncTaskListInfo(mCurrentSyncTaskList));
                        setOkButtonEnabledGroupEditor(dialog);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                editSyncTaskList(mCurrentSyncTaskList, ntfy);
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
                    NotifyEvent ntfy = new NotifyEvent(mContext);
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
                            mContext.getString(R.string.msgs_group_confirm_msg_nosave), "", ntfy);
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

    private String buildSyncTaskListInfo(String task_list) {
        return task_list.replaceAll(NAME_LIST_SEPARATOR, ", ");
    }

    private boolean isButtonAlreadyAssigned(GlobalParameters gp, GroupListItem gi, int id) {
        boolean result=false;
        for(GroupListItem item:gp.syncGroupList) {
//            if (!item.groupName.equals(gi.groupName) && item.assigned_button==id) {
            if (item.button ==id) {
                result=true;
                break;
            }
        }
        return result;
    }

    private void setSpinnerAssignedButton(GlobalParameters gp, Spinner spinner, GroupListItem curr_item, int button_id) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_group_select_button_title));
        spinner.setAdapter(adapter);

        adapter.add(mContext.getString(R.string.msgs_group_button_not_assigned));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListItem.BUTTON_SHORTCUT1) || button_id== GroupListItem.BUTTON_SHORTCUT1) adapter.add(mContext.getString(R.string.msgs_group_name_for_shortcut1));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListItem.BUTTON_SHORTCUT2) || button_id== GroupListItem.BUTTON_SHORTCUT2) adapter.add(mContext.getString(R.string.msgs_group_name_for_shortcut2));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListItem.BUTTON_SHORTCUT3) || button_id== GroupListItem.BUTTON_SHORTCUT3) adapter.add(mContext.getString(R.string.msgs_group_name_for_shortcut3));
        if (!isButtonAlreadyAssigned(gp, curr_item, GroupListItem.BUTTON_SYNC_BUTTON) || button_id== GroupListItem.BUTTON_SYNC_BUTTON) adapter.add(mContext.getString(R.string.msgs_group_name_for_sync_button));

        int sel=0;
        for(int i=1;i<adapter.getCount();i++) {
            String item=adapter.getItem(i);
            if (item.equals(mContext.getString(R.string.msgs_group_name_for_shortcut1)) && button_id== GroupListItem.BUTTON_SHORTCUT1) {
                sel=i;
            }
            else if (item.equals(mContext.getString(R.string.msgs_group_name_for_shortcut2)) && button_id== GroupListItem.BUTTON_SHORTCUT2) {
                sel=i;
            }
            else if (item.equals(mContext.getString(R.string.msgs_group_name_for_shortcut3)) && button_id== GroupListItem.BUTTON_SHORTCUT3) {
                sel=i;
            }
            else if (item.equals(mContext.getString(R.string.msgs_group_name_for_sync_button)) && button_id== GroupListItem.BUTTON_SYNC_BUTTON) {
                sel=i;
            }
        }
        spinner.setSelection(sel);
    }

    private int getSpinnerAssignedButton(Spinner spinner) {
        String sel=(String)spinner.getSelectedItem();
        int result=0;
        if (sel==null) result= GroupListItem.BUTTON_NOT_ASSIGNED;
        else if (sel.equals(mContext.getString(R.string.msgs_group_button_not_assigned))) result= GroupListItem.BUTTON_NOT_ASSIGNED;
        else if (sel.equals(mContext.getString(R.string.msgs_group_name_for_shortcut1))) result= GroupListItem.BUTTON_SHORTCUT1;
        else if (sel.equals(mContext.getString(R.string.msgs_group_name_for_shortcut2))) result= GroupListItem.BUTTON_SHORTCUT2;
        else if (sel.equals(mContext.getString(R.string.msgs_group_name_for_shortcut3))) result= GroupListItem.BUTTON_SHORTCUT3;
        else if (sel.equals(mContext.getString(R.string.msgs_group_name_for_sync_button))) result= GroupListItem.BUTTON_SYNC_BUTTON;
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
        final CheckedTextView ctv_group_enabled=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_enabled);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        CommonDialog.setButtonEnabled(mActivity, btn_ok, !mEditMode);
        if (et_name.getText().length() == 0) {
            tv_msg.setText(mContext.getString(R.string.msgs_group_list_edit_dlg_error_sync_list_name_does_not_specified));
            CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
        } else {
            String e_msg= hasGroupNameUnusableCharacter(mContext, et_name.getText().toString());
            if (!e_msg.equals("")) {
                tv_msg.setText(e_msg);
                CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
                return;
            } else if (!mEditMode && isGroupExists(mGroupList, et_name.getText().toString().trim())) {
                //Name alread exists
                tv_msg.setText(mContext.getString(R.string.msgs_group_list_edit_dlg_error_sync_list_name_already_exists));
                CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
                return;
            }
            if (!ctv_auto_task_only.isChecked()) {
                if (mCurrentSyncTaskList.equals("")) {
                    tv_msg.setText(mContext.getString(R.string.msgs_group_edit_sync_task_list_not_specified));
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
                    tv_msg.setText(mContext.getString(R.string.msgs_group_can_not_sync_specified_task_does_not_exists, nf_task));
                    return;
                }
            }
            if (ctv_auto_task_only.isChecked() && !isAutoTaskExists(dialog)) {
                tv_msg.setVisibility(TextView.VISIBLE);
                tv_msg.setText(mContext.getString(R.string.msgs_auto_sync_task_not_found));
                return;
            }
            tv_msg.setText("");
            if (isGroupChanged(dialog)) CommonDialog.setButtonEnabled(mActivity, btn_ok, true);
            else CommonDialog.setButtonEnabled(mActivity, btn_ok, false);
        }

    }

    static public boolean isGroupExists(ArrayList<GroupListItem> gl, String name) {
        boolean result=false;
        for(GroupListItem item:gl) {
            if (item.groupName.equalsIgnoreCase(name)) {
                result=true;
                break;
            }
        }
        return result;
    }

    private void buildGroupItem(Dialog dialog, GroupListItem gi) {
//        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        final EditText et_name = (EditText) dialog.findViewById(R.id.group_item_edit_dlg_group_name);
        final CheckedTextView ctv_group_enabled=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_enabled);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
        final Spinner sp_assigned_button=(Spinner)dialog.findViewById(R.id.group_item_edit_dlg_assigned_button);
        gi.taskList=mCurrentSyncTaskList;
        gi.enabled=ctv_group_enabled.isChecked();
        gi.autoTaskOnly=ctv_auto_task_only.isChecked();
        gi.groupName=et_name.getText().toString().trim();
        gi.button =getSpinnerAssignedButton(sp_assigned_button);
    }

    private boolean mEditSyncTaskListEnabeDragDrop=true;
    private void setEditSyncTaskListEnabeDragDrop( boolean enabled) {
        mEditSyncTaskListEnabeDragDrop=enabled;
    }
    private boolean isEditSyncTaskListEnabeDragDrop() {
        return mEditSyncTaskListEnabeDragDrop;
    }

    private void editSyncTaskList(final String prof_list, final NotifyEvent p_ntfy) {
        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.group_item_edit_task_list_dlg);

        final LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_task_list_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_task_list_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView dlg_title = (TextView) dialog.findViewById(R.id.group_item_edit_task_list_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.group_item_edit_task_list_dlg_msg);

        final LinearLayout dlg_normal_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_task_list_dlg_normal_view);
        final LinearLayout dlg_select_view = (LinearLayout) dialog.findViewById(R.id.group_item_edit_task_list_dlg_select_view);
        final Button btn_task_list = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_add_task_list);
        final ImageButton ib_delete = (ImageButton) dialog.findViewById(R.id.context_button_delete);
        final ImageButton ib_select_all = (ImageButton) dialog.findViewById(R.id.context_button_select_all);
        final ImageButton ib_unselect_all = (ImageButton) dialog.findViewById(R.id.context_button_unselect_all);
        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_cancel);

        final RecyclerView rv_task_list = (RecyclerView) dialog.findViewById(R.id.group_item_edit_task_list_dlg_recycle_view);
        String[]task_array=prof_list.split(NAME_LIST_SEPARATOR);
        ArrayList<EditSyncTaskListItem>task_list=new ArrayList<EditSyncTaskListItem>();
        for(String item:task_array) {
            if (!item.equals("")) {
                EditSyncTaskListItem etli=new EditSyncTaskListItem();
                etli.taskName=item;
                task_list.add(etli);
            }
        }
        final EditSyncTaskAdapter adapter = new EditSyncTaskAdapter(task_list);

        rv_task_list.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mActivity);
        rv_task_list.setLayoutManager(layoutManager);
        rv_task_list.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rv_task_list.getContext(),
                new LinearLayoutManager(mActivity).getOrientation());
        rv_task_list.addItemDecoration(dividerItemDecoration);

        ItemTouchHelper.SimpleCallback scb=new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN , 0) {
            private Drawable defaultBackGroundColor=null;

            @Override
            public boolean isLongPressDragEnabled() {
                return isEditSyncTaskListEnabeDragDrop();
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int action_state) {
                if (viewHolder!=null && viewHolder.itemView!=null) {
                    viewHolder.itemView.setAlpha(0.5f);
//                    if (defaultBackGroundColor==null)defaultBackGroundColor=viewHolder.itemView.getBackground();
//                    if (ThemeUtil.isLightThemeUsed(mActivity)) viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
//                    else viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
                }
            }

            @Override
            public void clearView(RecyclerView recycle_view, RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.itemView!=null) {
                    viewHolder.itemView.setAlpha(1.0f);
//                        viewHolder.itemView.setBackground(defaultBackGroundColor);
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();
                EditSyncTaskListItem fromTask=adapter.recyclerViewDataList.get(fromPos);
                adapter.notifyItemMoved(fromPos, toPos);
                adapter.recyclerViewDataList.remove(fromPos);
                adapter.recyclerViewDataList.add(toPos, fromTask);
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                return true;// true if moved, false otherwise
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                final int fromPos = viewHolder.getAdapterPosition();
//                dataset.remove(fromPos);
//                adapter.notifyItemRemoved(fromPos);
            }
        };
        ItemTouchHelper ith  = new ItemTouchHelper(scb);
        ith.attachToRecyclerView(rv_task_list);

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        ib_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteSyncTask(dialog, adapter, null);
            }
        });
        ib_select_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(EditSyncTaskListItem etli:adapter.recyclerViewDataList) {
                    etli.checked=true;
                }
                if (adapter.isAnyItemChecked()) {
                    dlg_normal_view.setVisibility(LinearLayout.GONE);
                    dlg_select_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    dlg_normal_view.setVisibility(LinearLayout.VISIBLE);
                    dlg_select_view.setVisibility(LinearLayout.GONE);
                }
                setEditSyncTaskListEnabeDragDrop(!adapter.isAnyItemChecked());
                adapter.notifyDataSetChanged();
            }
        });
        ib_unselect_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(EditSyncTaskListItem etli:adapter.recyclerViewDataList) {
                    etli.checked=false;
                }
                if (adapter.isAnyItemChecked()) {
                    dlg_normal_view.setVisibility(LinearLayout.GONE);
                    dlg_select_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    dlg_normal_view.setVisibility(LinearLayout.VISIBLE);
                    dlg_select_view.setVisibility(LinearLayout.GONE);
                }
                setEditSyncTaskListEnabeDragDrop(!adapter.isAnyItemChecked());
                adapter.notifyDataSetChanged();
            }
        });
        NotifyEvent ntfy_delete=new NotifyEvent(mContext);
        ntfy_delete.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                final EditSyncTaskListItem task=(EditSyncTaskListItem) objects[0];
                deleteSyncTask(dialog, adapter, task);
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        adapter.setNotifyDeleteButton(ntfy_delete);

        dlg_normal_view.setVisibility(LinearLayout.VISIBLE);
        dlg_select_view.setVisibility(LinearLayout.GONE);
        NotifyEvent ntfy_checked=new NotifyEvent(mContext);
        ntfy_checked.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                final EditSyncTaskListItem task=(EditSyncTaskListItem) objects[0];
                setEditSyncTaskListEnabeDragDrop(!adapter.isAnyItemChecked());
                if (adapter.isAnyItemChecked()) {
                    dlg_normal_view.setVisibility(LinearLayout.GONE);
                    dlg_select_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    dlg_normal_view.setVisibility(LinearLayout.VISIBLE);
                    dlg_select_view.setVisibility(LinearLayout.GONE);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        adapter.setNotifyCheckBox(ntfy_checked);

        ArrayList<AddSyncTaskItem>add_task_list=getAddTaskList(prof_list);
        if (add_task_list.size()==0) {
            btn_task_list.setVisibility(Button.GONE);
        }
        btn_task_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String curr_task_list=buildSyncTaskList(adapter);
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        String[] add_list_array=((String)objects[0]).split(NAME_LIST_SEPARATOR);
                        for(String item:add_list_array) {
                            EditSyncTaskListItem etli=new EditSyncTaskListItem();
                            etli.taskName=item;
                            adapter.recyclerViewDataList.add(etli);
                        }
                        adapter.notifyDataSetChanged();
                        setOkButtonEnabledEditSyncTaskList(dialog, curr_task_list, adapter);
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);

                        final String curr_task_list=buildSyncTaskList(adapter);
                        ArrayList<AddSyncTaskItem>add_task_list=getAddTaskList(curr_task_list);
                        if (add_task_list.size()==0) {
                            btn_task_list.setVisibility(Button.GONE);
                        } else {
                            btn_task_list.setVisibility(Button.VISIBLE);
                        }
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {

                    }
                });
                addTaskList(curr_task_list, ntfy);
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String n_prof_list = buildSyncTaskList(adapter);
                p_ntfy.notifyToListener(true, new Object[]{n_prof_list});
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSyncTaskListChanged(prof_list, adapter)) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                            p_ntfy.notifyToListener(false, null);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                        }
                    });
                    mUtil.showCommonDialog(true, "W",
                            mContext.getString(R.string.msgs_group_confirm_msg_nosave), "", ntfy);
                    return;
                }
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });

        setOkButtonEnabledEditSyncTaskList(dialog, prof_list, adapter);

        dialog.show();
    }

    private void deleteSyncTask(Dialog dialog, EditSyncTaskAdapter adapter, EditSyncTaskListItem task) {
        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_ok);
        final Button btn_task_list = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_add_task_list);
        final ArrayList<EditSyncTaskListItem> del_task=new ArrayList<EditSyncTaskListItem>();
        NotifyEvent ntfy_conf=new NotifyEvent(mContext);
        ntfy_conf.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                for(EditSyncTaskListItem etli:del_task) {
                    adapter.recyclerViewDataList.remove(etli);
                }
                adapter.notifyDataSetChanged();
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);

                final String curr_task_list=buildSyncTaskList(adapter);
                ArrayList<AddSyncTaskItem>add_task_list=getAddTaskList(curr_task_list);
                if (add_task_list.size()==0) {
                    btn_task_list.setVisibility(Button.GONE);
                } else {
                    btn_task_list.setVisibility(Button.VISIBLE);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        if (adapter.isAnyItemChecked()) {
            String del_list="", sep="";
            for(EditSyncTaskListItem etli:adapter.recyclerViewDataList) {
                if (etli.checked) {
                    del_list+=sep+etli.taskName;
                    sep=", ";
                    del_task.add(etli);
                }
            }
            mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_group_edit_delete_sync_task), del_list, ntfy_conf);
        } else {
            del_task.add(task);
            mUtil.showCommonDialog(true, "W", mContext.getString(R.string.msgs_group_edit_delete_sync_task), task.taskName, ntfy_conf);
        }

    }

    private String buildSyncTaskList(EditSyncTaskAdapter adapter) {
        String n_prof_list = "", sep = "";
        for (int i = 0; i < adapter.getItemCount(); i++) {
            n_prof_list = n_prof_list + sep + adapter.recyclerViewDataList.get(i).taskName;
            sep = NAME_LIST_SEPARATOR;
        }
        return n_prof_list;
    }

    private boolean isSyncTaskListChanged(String org_list, EditSyncTaskAdapter adapter) {
        String new_list=buildSyncTaskList(adapter);
        if (org_list.equals(new_list)) return false;
        return true;
    }

    private void setOkButtonEnabledEditSyncTaskList(Dialog dialog, String org_task_list, EditSyncTaskAdapter adapter) {
        final RecyclerView lv_sync_list = (RecyclerView) dialog.findViewById(R.id.group_item_edit_task_list_dlg_recycle_view);
        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_edit_task_list_dlg_ok);
        TextView dlg_msg = (TextView) dialog.findViewById(R.id.group_item_edit_task_list_dlg_msg);
        boolean selected=false;
        String task_list="", sep="";
        for (int i = 0; i < adapter.getItemCount(); i++) {
            task_list+=sep+adapter.recyclerViewDataList.get(i).taskName;
            sep= NAME_LIST_SEPARATOR;
        }
        if (task_list.equals("")) {
            dlg_msg.setText(mContext.getString(R.string.msgs_group_info_sync_task_list_was_empty));
            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            return;
        }
        dlg_msg.setText("");
        if (!task_list.equals(org_task_list)) {
            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
        } else {
            CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        }
    }

    private void setViewVisibility(Dialog dialog) {
        final Button btn_edit = (Button) dialog.findViewById(R.id.group_item_edit_dlg_edit_sync_prof);
        final CheckedTextView ctv_auto_task_only=(CheckedTextView)dialog.findViewById(R.id.group_item_edit_dlg_auto_task_only);
        if (ctv_auto_task_only.isChecked()) btn_edit.setVisibility(Button.GONE);
        else btn_edit.setVisibility(Button.VISIBLE);
    }

    private ArrayList<AddSyncTaskItem>getAddTaskList(final String current_task_list) {
        ArrayList<AddSyncTaskItem>add_task_list=new ArrayList<AddSyncTaskItem>();
        String[] curr_task_list_array=current_task_list.split(NAME_LIST_SEPARATOR);
        for(SyncTaskItem item:mGp.syncTaskList) {
            String e_msg= TaskListUtils.hasSyncTaskNameUnusableCharacter(mContext, item.getSyncTaskName());
            if (e_msg.equals("")) {
                boolean found=false;
                for(String curr_item:curr_task_list_array) {
                    if (curr_item.equals(item.getSyncTaskName())) {
                        found=true;
                        break;
                    }
                }
                if (!found) {
                    AddSyncTaskItem atli=new AddSyncTaskItem();
                    atli.task_name=item.getSyncTaskName();
                    add_task_list.add(atli);
                }
            }
        }
        return add_task_list;
    }

    private void addTaskList(final String current_task_list, final NotifyEvent p_ntfy) {

        ArrayList<AddSyncTaskItem>add_task_list=getAddTaskList(current_task_list);
        if (add_task_list.size()==0) {
            mUtil.showCommonDialog(false, "W", mContext.getString(R.string.msgs_group_add_sync_task_no_task_exists_for_add), "", null);
            return;
        }

        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.group_item_add_task_list_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.group_item_add_task_list_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.group_item_add_task_list_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) dialog.findViewById(R.id.group_item_add_task_list_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);

        TextView dlg_msg = (TextView) dialog.findViewById(R.id.group_item_add_task_list_dlg_msg);

        final ImageButton ib_select_all = (ImageButton) dialog.findViewById(R.id.context_button_select_all);
        final ImageButton ib_unselect_all = (ImageButton) dialog.findViewById(R.id.context_button_unselect_all);
        final Button btn_ok = (Button) dialog.findViewById(R.id.group_item_add_task_list_dlg_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.group_item_add_task_list_dlg_cancel);

        final ListView lv_sync_list = (ListView) dialog.findViewById(R.id.group_item_add_task_list_dlg_task_list);
        final AddSyncTaskAdapter adapter = new AddSyncTaskAdapter(mActivity, R.layout.group_item_add_task_list_item_view, add_task_list);
        lv_sync_list.setAdapter(adapter);

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);

        NotifyEvent ntfy_check=new NotifyEvent(mContext);
        ntfy_check.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                for(int i=0;i<adapter.getCount();i++) {
                    if (adapter.getItem(i).checked) {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        break;
                    }
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        adapter.setNotifyCheckBox(ntfy_check);

        lv_sync_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AddSyncTaskItem atli=adapter.getItem(position);
                atli.checked=!atli.checked;
                adapter.notifyDataSetChanged();
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                for(int i=0;i<adapter.getCount();i++) {
                    if (adapter.getItem(i).checked) {
                        CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        break;
                    }
                }
            }
        });

        ib_select_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i=0;i<adapter.getCount();i++) {
                    adapter.getItem(i).checked=true;
                }
                adapter.notifyDataSetChanged();
                CommonDialog.setViewEnabled(mActivity, btn_ok, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, ib_select_all, mContext.getString(R.string.msgs_group_cont_label_select_all));

        ib_unselect_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i=0;i<adapter.getCount();i++) {
                    adapter.getItem(i).checked=false;
                }
                adapter.notifyDataSetChanged();
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mActivity, ib_unselect_all, mContext.getString(R.string.msgs_group_cont_label_unselect_all));

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String n_prof_list = "", sep = "";
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).checked) {
                        n_prof_list = n_prof_list + sep + adapter.getItem(i).task_name;
                        sep = NAME_LIST_SEPARATOR;
                    }
                }
                p_ntfy.notifyToListener(true, new Object[]{n_prof_list});
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });

        dialog.show();

    }

    static public void removeSyncTaskFromGroup(GlobalParameters gp, CommonUtilities cu, String task_name) {
        for(GroupListItem gli:gp.syncGroupList) {
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
        for(GroupListItem gli:gp.syncGroupList) {
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

    private class EditSyncTaskListItem {
        public String taskName="";
        public boolean checked=false;
    }

    private class EditSyncTaskAdapter extends RecyclerView.Adapter<EditSyncTaskAdapter.ViewHolder> {

        private ArrayList<EditSyncTaskListItem> recyclerViewDataList = new ArrayList<>();

        private NotifyEvent mNtfyDeleteButtonClick=null;
        public void setNotifyDeleteButton(NotifyEvent ntfy) {mNtfyDeleteButtonClick=ntfy;}

        private NotifyEvent mNtfyCheckBox=null;
        public void setNotifyCheckBox(NotifyEvent ntfy) {mNtfyCheckBox=ntfy;}

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder {

            // each data item is just a string in this case
            TextView mTextView, mErrorMessage;
            ImageButton mDeleteBtn;
            CheckBox mChecked;

            ViewHolder(View v) {
                super(v);
                mDeleteBtn = (ImageButton) v.findViewById(R.id.group_item_edit_task_list_item_del_btn);
                mTextView = (TextView)v.findViewById(R.id.group_item_edit_task_list_item_task_name);
                mErrorMessage = (TextView)v.findViewById(R.id.group_item_edit_task_list_item_error_message);
                mChecked=(CheckBox)v.findViewById(R.id.group_item_edit_task_list_item_checked);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public EditSyncTaskAdapter(ArrayList<EditSyncTaskListItem> dataset) {
            recyclerViewDataList = dataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_item_edit_task_list_view, parent, false);

            // set the view's size, margins, paddings and layout parameters

            return new ViewHolder(view);
        }

        public boolean isAnyItemChecked() {
            for(EditSyncTaskListItem etli:recyclerViewDataList) {
                if (etli.checked) return true;
            }
            return false;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final EditSyncTaskListItem o=recyclerViewDataList.get(position);
            holder.mTextView.setText(o.taskName);
            String[]task_array=o.taskName.split(NAME_LIST_SEPARATOR);
            holder.mErrorMessage.setText("");
            for(String item:task_array) {
                SyncTaskItem sti= TaskListUtils.getSyncTaskByName(mGp.syncTaskList, item);
                if (sti==null) {
                    holder.mErrorMessage.setText(mContext.getString(R.string.msgs_group_error_specified_task_does_not_exists));
                    break;
                }
            }
            if (holder.mErrorMessage.getText().length()==0) holder.mErrorMessage.setVisibility(TextView.GONE);
            else holder.mErrorMessage.setVisibility(TextView.VISIBLE);

            if (isAnyItemChecked()) holder.mDeleteBtn.setVisibility(ImageButton.INVISIBLE);
            else holder.mDeleteBtn.setVisibility(ImageButton.VISIBLE);

            holder.mDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mNtfyDeleteButtonClick!=null) mNtfyDeleteButtonClick.notifyToListener(true, new Object[]{recyclerViewDataList.get(position)});
                }
            });

            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.mChecked.performClick();
                    setEditSyncTaskListEnabeDragDrop(false);
                }
            });

            holder.mChecked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.checked=holder.mChecked.isChecked();
                    notifyDataSetChanged();
                    if (mNtfyCheckBox!=null) mNtfyCheckBox.notifyToListener(true, new Object[]{recyclerViewDataList.get(position)});
                }
            });
            holder.mChecked.setChecked(o.checked);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return recyclerViewDataList.size();
        }
    }

    private class AddSyncTaskItem {
        public String task_name="";
        public boolean checked=false;
    }
    private class AddSyncTaskAdapter extends ArrayAdapter<AddSyncTaskItem> {
        private int layout_id = 0;
        private Context context = null;
        private NotifyEvent mNtfyCheckbox;
        private int text_color = 0;

        private ArrayList<AddSyncTaskItem>mTaskList=null;

        public AddSyncTaskAdapter(Context c, int textViewResourceId, ArrayList<AddSyncTaskItem>tl) {
            super(c, textViewResourceId, tl);
            layout_id = textViewResourceId;
            context = c;
            mTaskList=tl;
        }

        public void setNotifyCheckBox(NotifyEvent ntfy) {mNtfyCheckbox=ntfy;}

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            final ViewHolder holder;
            final AddSyncTaskItem o = getItem(position);
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(layout_id, null);
                holder = new ViewHolder();
                holder.tv_name = (TextView) v.findViewById(R.id.group_item_add_task_list_item_task_name);
                holder.cb_selected = (CheckBox) v.findViewById(R.id.group_item_add_task_list_item_checked);
                text_color = holder.tv_name.getCurrentTextColor();
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            if (o != null) {
                holder.tv_name.setText(o.task_name);
                holder.tv_name.setTextColor(text_color);
            }

            holder.cb_selected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.checked=holder.cb_selected.isChecked();
                    if (mNtfyCheckbox!=null) mNtfyCheckbox.notifyToListener(true, new Object[]{o});
                }
            });
            holder.cb_selected.setChecked(o.checked);
            return v;

        }

        class ViewHolder {
            TextView tv_name;
            CheckBox cb_selected;
        }
    }

    static public String getSyncTaskList(Context c, GroupListItem gli, ArrayList<SyncTaskItem>task_list) {
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

    static public String hasValidSyncTaskList(Context c, GroupListItem o, ArrayList<SyncTaskItem>task_list) {
        String e_msg="";
        String[]task_list_array=o.taskList.split(NAME_LIST_SEPARATOR);
        for(String item:task_list_array) {
            SyncTaskItem sti= TaskListUtils.getSyncTaskByName(task_list, item);
            if (sti==null) {
                e_msg=c.getString(R.string.msgs_group_error_specified_task_does_not_exists, item);
                break;
            }
        }
        return e_msg;
    }

    static public String buildGroupExecuteSyncTaskList(Context c, GlobalParameters gp, CommonUtilities cu,
                                                       GroupListItem gli, ArrayList<String>exec_task_list, ArrayList<String>duplicate_list) {
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
