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

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_ACTIVITY;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_EXTERNAL;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SCHEDULE;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SHORTCUT;

public class HistoryListAdapter extends ArrayAdapter<HistoryListAdapter.HistoryListItem> {
    private static final Logger log= LoggerFactory.getLogger(HistoryListAdapter.class);
    private Activity mActivity;
    private int id;
    private ArrayList<HistoryListItem> items;

    private ThemeColorList mThemeColorList;

    private String mModeTest, mModeNormal;

    private String mTitleMode;
    private String mTitleCopied;
    private String mTitleMoved;
    private String mTitleDeleted;
    private String mTitleReplaced;
    private String mTitleIgnored;
    private String mTitleElapsed;

    public HistoryListAdapter(Activity a, int textViewResourceId,
                              ArrayList<HistoryListItem> objects) {
        super(a, textViewResourceId, objects);
        mActivity = a;
        id = textViewResourceId;
        items = objects;
        vi = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mThemeColorList = ThemeUtil.getThemeColorList(a);

        mModeNormal = a.getString(R.string.msgs_main_sync_history_mode_normal);
        mModeTest = a.getString(R.string.msgs_main_sync_history_mode_test);

        mTitleMode=mActivity.getString(R.string.msgs_main_sync_history_mode);
        mTitleCopied=mActivity.getString(R.string.msgs_main_sync_history_count_copied);
        mTitleMoved=mActivity.getString(R.string.msgs_main_sync_history_count_moved);
        mTitleDeleted=mActivity.getString(R.string.msgs_main_sync_history_count_deleted);
        mTitleReplaced=mActivity.getString(R.string.msgs_main_sync_history_count_replaced);
        mTitleIgnored=mActivity.getString(R.string.msgs_main_sync_history_count_ignored);
        mTitleElapsed=mActivity.getString(R.string.msgs_main_sync_history_elapsed_time);

    }

    @Override
    public HistoryListItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public void remove(int pos) {
        items.remove(pos);
    }

    @Override
    public void remove(HistoryListItem p) {
        items.remove(p);
    }

    public ArrayList<HistoryListItem> getSyncHistoryList() {
        return items;
    }

    public void setSyncHistoryList(ArrayList<HistoryListItem> p) {
        items = p;
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
            for (int i = 0; i < items.size(); i++) items.get(i).isChecked = p;
        }
    }

    public boolean isAnyItemSelected() {
        boolean result = false;
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).isChecked) {
                result = true;
                break;
            }
        return result;
    }

    public int getItemSelectedCount() {
        int result = 0;
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).isChecked) {
                result++;
            }
        return result;
    }

    public boolean isEmptyAdapter() {
        boolean result = false;
        if (items != null) {
            if (items.size() == 0 || items.get(0).sync_task.equals("")) result = true;
        } else {
            result = true;
        }
        return result;
    }

    private LayoutInflater vi = null;
    private Drawable ll_default = null;
//	private int mTextColorPrimary=-1;
    private ColorStateList mTextColor=null;

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        View v = convertView;
        if (v == null) {
            v = vi.inflate(id, null);
            holder = new ViewHolder();
            holder.cb_sel = (CheckBox) v.findViewById(R.id.history_list_view_cb);
            holder.tv_date = (TextView) v.findViewById(R.id.history_list_view_date);
            holder.tv_time = (TextView) v.findViewById(R.id.history_list_view_time);
            holder.tv_prof = (TextView) v.findViewById(R.id.history_list_view_prof);
            holder.tv_status = (TextView) v.findViewById(R.id.history_list_view_status);
            holder.tv_req = (TextView) v.findViewById(R.id.history_list_view_requestor);
            holder.tv_seq = (TextView) v.findViewById(R.id.history_list_view_seq);
            holder.tv_error = (TextView) v.findViewById(R.id.history_list_view_error_text);
            holder.ll_main = (LinearLayout) v.findViewById(R.id.history_list_view);
            holder.tv_tr = (TextView) v.findViewById(R.id.history_list_view_transfer_speed);
            holder.tv_info = (TextView) v.findViewById(R.id.history_list_view_hist_info);
//            holder.tv_date.setTextColor(mThemeColorList.text_color_primary);
//            holder.tv_time.setTextColor(mThemeColorList.text_color_primary);
//            holder.tv_prof.setTextColor(mThemeColorList.text_color_primary);
//            holder.tv_status.setTextColor(mThemeColorList.text_color_primary);
//            holder.tv_seq.setTextColor(mThemeColorList.text_color_primary);
//            holder.tv_error.setTextColor(mThemeColorList.text_color_error);

            if (ll_default != null) ll_default = holder.ll_main.getBackground();
//    		if (mTextColorPrimary==-1) mTextColorPrimary=holder.tv_date.getCurrentTextColor();
            if (mTextColor==null) mTextColor=holder.tv_date.getTextColors();
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        final HistoryListItem o = getItem(position);

        if (o != null) {
            if (!o.sync_task.equals("")) {
//        		holder.ll_main.setBackgroundDrawable(ll_default);

                holder.tv_seq.setText(String.format("%1$3d", position + 1));
                holder.tv_seq.setVisibility(TextView.VISIBLE);
                holder.tv_date.setVisibility(TextView.VISIBLE);
                holder.tv_time.setVisibility(TextView.VISIBLE);
                holder.tv_status.setVisibility(TextView.VISIBLE);
//                holder.ll_count.setVisibility(TextView.VISIBLE);

                holder.tv_date.setText(o.sync_date);
                holder.tv_time.setText(o.sync_time);
                holder.tv_prof.setText(o.sync_task);

                holder.tv_req.setText(HistoryListItem.getSyncStartRequestorDisplayName(mActivity, o.sync_req));

                String st_text = "";
                if (o.sync_status == HistoryListItem.SYNC_RESULT_STATUS_SUCCESS) {
                    st_text = mActivity.getString(R.string.msgs_main_sync_history_status_success);
                    holder.tv_status.setTextColor(mTextColor);
                } else if (o.sync_status == HistoryListItem.SYNC_RESULT_STATUS_ERROR) {
                    st_text = mActivity.getString(R.string.msgs_main_sync_history_status_error);
                    holder.tv_status.setTextColor(mThemeColorList.text_color_error);
                    holder.tv_error.setTextColor(mThemeColorList.text_color_error);
                } else if (o.sync_status == HistoryListItem.SYNC_RESULT_STATUS_CANCEL) {
                    st_text = mActivity.getString(R.string.msgs_main_sync_history_status_cancel);
                    holder.tv_status.setTextColor(mThemeColorList.text_color_warning);
                } else if (o.sync_status == HistoryListItem.SYNC_RESULT_STATUS_WARNING) {
                    st_text = mActivity.getString(R.string.msgs_main_sync_history_status_warning);
                    holder.tv_status.setTextColor(mThemeColorList.text_color_warning);
                    holder.tv_error.setTextColor(mThemeColorList.text_color_warning);
                } else if (o.sync_status == HistoryListItem.SYNC_RESULT_STATUS_SKIP) {
                    st_text = mActivity.getString(R.string.msgs_main_sync_history_status_skip);
                    holder.tv_status.setTextColor(mThemeColorList.text_color_warning);
                    holder.tv_error.setTextColor(mThemeColorList.text_color_warning);
                }

                holder.tv_tr.setText(o.sync_transfer_speed);

                holder.tv_status.setText(st_text);

                int t_et_sec = (int) (o.sync_elapsed_time / 1000);
                int t_et_ms = (int) (o.sync_elapsed_time - (t_et_sec * 1000));
                String sync_et = String.valueOf(t_et_sec) + "." + String.format("%3d", t_et_ms).replaceAll(" ", "0");
                holder.tv_info.setText(String.format(mTitleMode+"%s, "+//1
                                mTitleCopied+"%s, "+//2
                                mTitleMoved+"%s, "+//3
                                mTitleReplaced+"%s, "+//4
                                mTitleDeleted+"%s, "+//5
                                mTitleIgnored+"%s, "+//6
                                mTitleElapsed+"%s",//7
                        o.sync_test_mode ? mModeTest : mModeNormal,//1
                        Integer.toString(o.sync_result_no_of_copied),//2
                        Integer.toString(o.sync_result_no_of_moved),//3
                        Integer.toString(o.sync_result_no_of_replaced),//4
                        Integer.toString(o.sync_result_no_of_deleted),//5
                        Integer.toString(o.sync_result_no_of_ignored),//6
                        sync_et));

                if (o.sync_error_text != null && !o.sync_error_text.equals("")) {
                    holder.tv_error.setVisibility(TextView.VISIBLE);
                    holder.tv_error.setText(o.sync_error_text);
                } else {
                    holder.tv_error.setVisibility(TextView.GONE);
                }

                if (isShowCheckBox) holder.cb_sel.setVisibility(TextView.VISIBLE);
                else holder.cb_sel.setVisibility(TextView.GONE);

                holder.cb_sel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                     @Override
                     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                         if (o.sync_task.equals("")) return;
                         items.get(position).isChecked = isChecked;
                         if (mNotifyCheckBoxEvent != null && isShowCheckBox)
                             mNotifyCheckBoxEvent.notifyToListener(true, null);
                     }
                });
                holder.cb_sel.setChecked(items.get(position).isChecked);
            } else {
                holder.tv_seq.setVisibility(TextView.GONE);
                holder.tv_date.setVisibility(TextView.GONE);
                holder.tv_time.setVisibility(TextView.GONE);
                holder.tv_status.setVisibility(TextView.GONE);
                holder.ll_count.setVisibility(TextView.GONE);
                holder.tv_error.setVisibility(TextView.GONE);
                holder.cb_sel.setVisibility(TextView.GONE);
            }

        }
        return v;
    }

    static class ViewHolder {
        CheckBox cb_sel;
        TextView tv_date, tv_time, tv_prof, tv_status;//, tv_cnt_copied, tv_cnt_deleted, tv_cnt_moved;
        TextView tv_error, tv_seq, tv_req;//, tv_mode;
        TextView tv_info;
        TextView tv_et;
        TextView tv_tr;
        LinearLayout ll_count, ll_main;
    }

    public static class HistoryListItem {
        public boolean isChecked = false;

        public String sync_date = null;
        public String sync_time = null;
        public long sync_elapsed_time = 0L;
        public String sync_transfer_speed = null;
        public String sync_task = "";
        public String sync_req = "";
        public boolean sync_test_mode = false;
        public int sync_status = SYNC_RESULT_STATUS_SUCCESS;
        public final static int SYNC_RESULT_STATUS_SUCCESS = 0;
        public final static int SYNC_RESULT_STATUS_CANCEL = 1;
        public final static int SYNC_RESULT_STATUS_ERROR = 2;
        public final static int SYNC_RESULT_STATUS_WARNING = 3;
        public final static int SYNC_RESULT_STATUS_SKIP = 4;

        public int sync_result_no_of_copied = 0;
        public int sync_result_no_of_deleted = 0;
        public int sync_result_no_of_ignored = 0;
        public int sync_result_no_of_moved = 0;
        public int sync_result_no_of_replaced = -1;
        public int sync_result_no_of_retry = 0;

        public String sync_error_text = "";

        public String sync_result_file_path = "";

        static public String getSyncStartRequestorDisplayName(Context c, String request_id) {
            String display_name="";
            if (request_id.equals(SYNC_REQUEST_ACTIVITY)) display_name=c.getString(R.string.msgs_svc_received_start_sync_task_request_intent_activity);
            else if (request_id.equals(SYNC_REQUEST_EXTERNAL)) display_name=c.getString(R.string.msgs_svc_received_start_sync_task_request_intent_external);
            else if (request_id.equals(SYNC_REQUEST_SHORTCUT)) display_name=c.getString(R.string.msgs_svc_received_start_sync_task_request_intent_shortcut);
            else if (request_id.equals(SYNC_REQUEST_SCHEDULE)) display_name=c.getString(R.string.msgs_svc_received_start_sync_task_request_intent_schedule);
            return display_name;
        }

    }
}

