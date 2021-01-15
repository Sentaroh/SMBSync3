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

//import static com.sentaroh.android.SMBSync2.Constants.*;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.ThemeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.logging.Handler;

public class TaskListAdapter extends ArrayAdapter<SyncTaskItem> {
    private static Logger log= LoggerFactory.getLogger(TaskListAdapter.class);

    private Context mContext;
    private Handler mUiHandler;
    private int id;
    private ArrayList<SyncTaskItem> items;
    private String mTaskTypeAuto, mTaskTypeManual, mTaskNoExec, mTaskStatusRunning, mTaskTypeTest;
    private String mTaskStatusSuccess, mTaskStatusError, mTaskSttatusCancel, mTaskStatusWarning, mTaskStatusSkip;

    private GlobalParameters mGp = null;

    private ColorStateList mTextColor=null;

    String mMirrorTypeMirror, mMirrorTypeMove, mMirrorTypeCopy, mMirrorTypeSync, mMirrorTypeArchive;

    public TaskListAdapter(Context c, int textViewResourceId, ArrayList<SyncTaskItem> objects, GlobalParameters gp) {
        super(c, textViewResourceId, objects);
        mContext = c;
        id = textViewResourceId;
        items = objects;
        mGp = gp;

        mTaskTypeAuto = mContext.getString(R.string.msgs_main_sync_list_array_task_auto);
        mTaskTypeTest = mContext.getString(R.string.msgs_main_sync_list_array_task_test);
        mTaskTypeManual = mContext.getString(R.string.msgs_main_sync_list_array_task_manual);
        mTaskNoExec = mContext.getString(R.string.msgs_main_sync_list_array_no_last_sync_time);
        mTaskStatusRunning = mContext.getString(R.string.msgs_main_sync_history_status_running);
        mTaskStatusSuccess = mContext.getString(R.string.msgs_main_sync_history_status_success);
        mTaskStatusError = mContext.getString(R.string.msgs_main_sync_history_status_error);
        mTaskSttatusCancel = mContext.getString(R.string.msgs_main_sync_history_status_cancel);
        mTaskStatusWarning = mContext.getString(R.string.msgs_main_sync_history_status_warning);
        mTaskStatusSkip = mContext.getString(R.string.msgs_main_sync_history_status_skip);

        mMirrorTypeMirror = mContext.getString(R.string.msgs_main_sync_list_array_mtype_mirr);
        mMirrorTypeCopy = mContext.getString(R.string.msgs_main_sync_list_array_mtype_copy);
        mMirrorTypeMove = mContext.getString(R.string.msgs_main_sync_list_array_mtype_move);
        mMirrorTypeSync = mContext.getString(R.string.msgs_main_sync_list_array_mtype_sync);
        mMirrorTypeArchive = mContext.getString(R.string.msgs_main_sync_list_array_mtype_archive);

    }

    public SyncTaskItem getItem(int i) {
        return items.get(i);
    }

    public void remove(int i) {
        items.remove(i);
        notifyDataSetChanged();
    }

//    public void replace(SyncTaskItem pli, int i) {
//        items.set(i, pli);
//        notifyDataSetChanged();
//    }

    private NotifyEvent mNotifySyncButtonEvent = null;

    public void setNotifySyncButtonEventHandler(NotifyEvent ntfy) {
        mNotifySyncButtonEvent = ntfy;
    }

    private NotifyEvent mNotifyCheckBoxEvent = null;

    public void setNotifyCheckBoxEventHandler(NotifyEvent ntfy) {
        mNotifyCheckBoxEvent = ntfy;
    }

    private boolean isShowCheckBox = false;

    public void setShowCheckBox(boolean p) {
        isShowCheckBox = p;
    }

    public boolean isShowCheckBox() {
        return isShowCheckBox;
    }

    public void setAllItemChecked(boolean p) {
        if (items != null) {
            for (int i = 0; i < items.size(); i++) items.get(i).setChecked(p);
        }
    }

    public boolean isEmptyAdapter() {
        boolean result = false;
        if (items != null) {
            if (items.size() == 0 || items.get(0).getSyncTaskType().equals("")) result = true;
        } else {
            result = true;
        }
        return result;
    }

    public ArrayList<SyncTaskItem> getArrayList() {
        return items;
    }

    public void setArrayList(ArrayList<SyncTaskItem> p) {
        items.clear();
        if (p != null) {
            for (int i = 0; i < p.size(); i++) items.add(p.get(i));
        }
        notifyDataSetChanged();
    }

    public void setArrayList(ArrayList<SyncTaskItem> p, boolean notify_data_set_changed) {
        items.clear();
        if (p != null) {
            for (int i = 0; i < p.size(); i++) items.add(p.get(i));
        }
        if (notify_data_set_changed) notifyDataSetChanged();
    }

    public void sort() {
        TaskListUtils.sortSyncTaskList(items);
    }

    private Drawable mDefaultBackground = null;
//    private Drawable ib_default = null;

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder_tmp;

        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder_tmp = new ViewHolder();
            holder_tmp.tv_row_name = (TextView) v.findViewById(R.id.sync_task_name);
            holder_tmp.tv_row_active = (TextView) v.findViewById(R.id.sync_task_enabled);
            holder_tmp.cbv_row_cb1 = (CheckBox) v.findViewById(R.id.sync_task_selected);
            holder_tmp.ib_row_sync=(ImageButton)v.findViewById(R.id.sync_task_perform_sync);
//            ib_default=holder_tmp.ib_row_sync.getBackground();

            holder_tmp.tv_row_source = (TextView) v.findViewById(R.id.sync_task_source_info);
            holder_tmp.tv_row_destination = (TextView) v.findViewById(R.id.sync_task_destination_info);
            holder_tmp.tv_row_synctype = (TextView) v.findViewById(R.id.sync_task_sync_type);
            holder_tmp.iv_row_sync_dir_image = (ImageView) v.findViewById(R.id.sync_task_direction_image);
            holder_tmp.iv_row_image_source = (ImageView) v.findViewById(R.id.sync_task_source_icon);
            holder_tmp.iv_row_image_destination = (ImageView) v.findViewById(R.id.sync_task_destination_icon);

            holder_tmp.ll_sync = (LinearLayout) v.findViewById(R.id.profile_list_sync_layout);
            holder_tmp.ll_entry = (LinearLayout) v.findViewById(R.id.profile_list_entry_layout);
            holder_tmp.ll_view = (LinearLayout) v.findViewById(R.id.profile_list_view);
            if (mDefaultBackground != null) mDefaultBackground = holder_tmp.ll_view.getBackground();

            holder_tmp.tv_last_sync_time = (TextView) v.findViewById(R.id.sync_task_sync_result_time);
            holder_tmp.tv_last_sync_result = (TextView) v.findViewById(R.id.sync_task_sync_result_status);
            holder_tmp.ll_last_sync = (LinearLayout) v.findViewById(R.id.sync_task_sync_result_view);

            holder_tmp.tv_stop_condition=(TextView) v.findViewById(R.id.sync_task_sync_stop_condition);

            if (mTextColor==null) mTextColor=holder_tmp.tv_row_name.getTextColors();
            v.setTag(holder_tmp);
        } else {
            holder_tmp = (ViewHolder) v.getTag();
        }
        final ViewHolder holder=holder_tmp;
        final SyncTaskItem o = getItem(position);
        if (o != null) {
            boolean sync_btn_disable=false;

            holder.ll_view.setBackgroundDrawable(mDefaultBackground);
//            holder.ib_row_sync.setBackgroundDrawable(ib_default);

            String act = "";
            if (o.isSyncTaskAuto()) {
                if (!o.isSyncTestMode()) act = mTaskTypeAuto;
                else act = mTaskTypeTest;
            } else {
                if (!o.isSyncTestMode()) act = mTaskTypeManual;
                else act = mTaskTypeTest;
            }
            holder.tv_row_active.setText(act);
            holder.tv_row_name.setText(o.getSyncTaskName());

            holder.ll_sync.setVisibility(LinearLayout.VISIBLE);
            holder.ll_last_sync.setVisibility(LinearLayout.VISIBLE);
            holder.tv_row_active.setVisibility(LinearLayout.VISIBLE);
            holder.cbv_row_cb1.setVisibility(LinearLayout.VISIBLE);

            String synctp = "";
            if (o.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) synctp = mMirrorTypeMirror;
            else if (o.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) synctp = mMirrorTypeMove;
            else if (o.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) synctp = mMirrorTypeCopy;
            else if (o.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_SYNC)) synctp = mMirrorTypeSync;
            else if (o.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) synctp = mMirrorTypeArchive;
            else synctp = "ERR";

            holder.tv_row_synctype.setText(synctp);

            String result = "";
            if (o.isSyncTaskRunning()) {
                result = mTaskStatusRunning;
                if (ThemeUtil.isLightThemeUsed(mContext)) holder.ll_view.setBackgroundColor(Color.argb(255, 0, 192, 192));
                else holder.ll_view.setBackgroundColor(Color.argb(255, 0, 128, 128));
            } else {
                if (o.getLastSyncResult() == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_SUCCESS) {
                    result = mTaskStatusSuccess;
                } else if (o.getLastSyncResult() == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_CANCEL) {
                    result = mTaskSttatusCancel;
                } else if (o.getLastSyncResult() == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR) {
                    result = mTaskStatusError;
                } else if (o.getLastSyncResult() == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_WARNING) {
                    result = mTaskStatusWarning;
                } else if (o.getLastSyncResult() == HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_SKIP) {
                    result = mTaskStatusSkip;
                }
            }
            if (o.getSyncTaskErrorOption()==SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_STOP) {
                holder_tmp.tv_stop_condition.setText(mContext.getString(R.string.msgs_task_sync_task_dlg_task_stop_condition_stop));
            } else if (o.getSyncTaskErrorOption()==SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_SKIP_UNCOND) {
                holder_tmp.tv_stop_condition.setText(mContext.getString(R.string.msgs_task_sync_task_dlg_task_stop_condition_skip_uncondition));
            } else if (o.getSyncTaskErrorOption()==SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_SKIP_NETWORK) {
                holder_tmp.tv_stop_condition.setText(mContext.getString(R.string.msgs_task_sync_task_dlg_task_stop_condition_skip_network));
            }
            if (!o.getLastSyncTime().equals("")) {
                holder.tv_last_sync_result.setTextColor(mTextColor);
                holder.tv_last_sync_time.setText(o.getLastSyncTime());
                holder.tv_last_sync_result.setText(result);
            } else {
                holder_tmp.ll_last_sync.setVisibility(LinearLayout.GONE);
                holder.tv_last_sync_result.setTextColor(mTextColor);
                holder.tv_last_sync_time.setText("");
                holder.tv_last_sync_result.setText("");
            }
            if (!o.isSyncFolderStatusError()) {
                if (o.isSyncTestMode()) {
                    if (ThemeUtil.isLightThemeUsed(mContext)) holder.ll_view.setBackgroundColor(Color.argb(64, 255, 32, 255));
                    else holder.ll_view.setBackgroundColor(Color.argb(64, 255, 0, 128));
                }
            } else {
                holder.ll_view.setBackgroundColor(Color.argb(64, 255, 0, 0));
            }
            int img_res=0;
            if (o.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
                String dir = o.getSourceDirectoryName();
                holder.tv_row_source.setText(CommonUtilities.getStoragePathFromUuid(o.getSourceStorageUuid())+"/"+dir);
                if (o.getSourceStorageUuid().equals(SafManager3.SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_mobile;
                else {
                    if (SafManager3.isUuidRegistered(mContext, o.getSourceStorageUuid())) img_res=R.drawable.ic_32_external_media;
                    else {
                        img_res=R.drawable.ic_32_external_media_bad;
                        sync_btn_disable=true;
                    }
                }
                if (holder.resid_row_image_source!=img_res) holder.iv_row_image_source.setImageResource(img_res);
                holder.resid_row_image_source=img_res;
            } else if (o.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                String host = o.getSourceSmbAddr();
                if (o.getSourceSmbAddr().equals("")) host = o.getSourceSmbHostName();
                String share = o.getSourceSmbShareName();
                String dir = o.getSourceDirectoryName();
                if (dir.equals("")) holder.tv_row_source.setText("smb://" + host + "/" + share);
                else {
                    if (dir.startsWith("/")) holder.tv_row_source.setText("smb://" + host + "/" + share + dir);
                    else holder.tv_row_source.setText("smb://" + host + "/" + share + "/"+ dir);
                }
                img_res=R.drawable.ic_32_server;
                if (holder.resid_row_image_source!=img_res) holder.iv_row_image_source.setImageResource(img_res);
                holder.resid_row_image_source=img_res;
            }
            if (o.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
                String dir = o.getDestinationDirectoryName();
                holder.tv_row_destination.setText(CommonUtilities.getStoragePathFromUuid(o.getDestinationStorageUuid())+"/"+dir);
                if (o.getDestinationStorageUuid().equals(SafManager3.SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_mobile;
                else {
                    if (SafManager3.isUuidRegistered(mContext, o.getDestinationStorageUuid())) img_res=R.drawable.ic_32_external_media;
                    else {
                        img_res=R.drawable.ic_32_external_media_bad;
                        sync_btn_disable=true;
                    }
                }
                if (holder.resid_row_image_destination!=img_res) holder.iv_row_image_destination.setImageResource(img_res);
                holder.resid_row_image_destination=img_res;
            } else if (o.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                holder.tv_row_destination.setText(CommonUtilities.getStoragePathFromUuid(o.getDestinationStorageUuid())+"/"+o.getDestinationZipOutputFileName());
                if (o.getDestinationStorageUuid().equals(SafManager3.SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_archive;
                else {
                    if (SafManager3.isUuidRegistered(mContext, o.getDestinationStorageUuid())) img_res=R.drawable.ic_32_archive;
                    else {
                        img_res=R.drawable.ic_32_archive_bad;
                        sync_btn_disable=true;
                    }
                }
                if (holder.resid_row_image_destination!=img_res) holder.iv_row_image_destination.setImageResource(img_res);
                holder.resid_row_image_destination=img_res;
            } else if (o.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                String host = o.getDestinationSmbAddr();
                if (o.getDestinationSmbAddr().equals("")) host = o.getDestinationSmbHostName();
                String share = o.getDestinationSmbShareName();
                String dir = o.getDestinationDirectoryName();
                if (dir.equals("")) holder.tv_row_destination.setText("smb://" + host + "/" + share);
                else holder.tv_row_destination.setText("smb://" + host + "/" + share + "/" + dir);
                img_res=R.drawable.ic_32_server;
                if (holder.resid_row_image_destination!=img_res) holder.iv_row_image_destination.setImageResource(img_res);
                holder.resid_row_image_destination=img_res;
            }
            holder.tv_row_source.requestLayout();
            holder.tv_row_destination.requestLayout();

            String e_msg= TaskListUtils.hasSyncTaskNameUnusableCharacter(mContext, o.getSyncTaskName());
            if (!e_msg.equals("")) {
                sync_btn_disable=true;
                holder.ll_last_sync.setVisibility(LinearLayout.VISIBLE);
                holder.tv_last_sync_result.setText(e_msg);
                holder.tv_last_sync_result.setTextColor(mGp.themeColorList.text_color_warning);
            }

            if (isShowCheckBox) {
                holder.cbv_row_cb1.setVisibility(CheckBox.VISIBLE);
                holder.ib_row_sync.setVisibility(CheckBox.GONE);
            } else {
                holder.cbv_row_cb1.setVisibility(CheckBox.GONE);
                if (o.isSyncFolderStatusError() || sync_btn_disable) {
                    holder.ib_row_sync.setVisibility(CheckBox.INVISIBLE);
                } else {
                    holder.ib_row_sync.setVisibility(CheckBox.VISIBLE);
                }
            }
            final int p = position;

            holder.ib_row_sync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!o.isSyncFolderStatusError()) {
                        holder.ib_row_sync.setEnabled(false);
                        if (mNotifySyncButtonEvent!=null) mNotifySyncButtonEvent.notifyToListener(true,new Object[]{o});
                        holder.ib_row_sync.postDelayed(new Runnable(){
                            @Override
                            public void run() {
                                holder.ib_row_sync.setEnabled(true);
                            }
                        },1000);
                    }
                }
            });

            holder.ib_row_sync.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View view) {
                    CommonDialog.showPopupMessageAsUpAnchorView((Activity)mContext, holder.ib_row_sync, mContext.getString(R.string.msgs_task_cont_label_sync_specific, o.getSyncTaskName()), 2);
                    return true;
                }
            });

            holder.cbv_row_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    o.setChecked(isChecked);
                    if (mNotifyCheckBoxEvent != null && isShowCheckBox)
                        mNotifyCheckBoxEvent.notifyToListener(true, null);
                }
            });
            holder.cbv_row_cb1.setChecked(items.get(position).isChecked());

            notifyDataSetChanged();
        }
        return v;
    }

    private class ViewHolder {
        TextView tv_row_name, tv_row_active;
        CheckBox cbv_row_cb1;
        ImageButton ib_row_sync;

        TextView tv_row_synctype, tv_row_source, tv_row_destination, tv_stop_condition;
        ImageView iv_row_sync_dir_image;
        ImageView iv_row_image_source, iv_row_image_destination;

        int resid_row_image_source=0, resid_row_image_destination=0;

        TextView tv_last_sync_time, tv_last_sync_result;
        LinearLayout ll_sync, ll_entry, ll_last_sync, ll_view;
    }
}

