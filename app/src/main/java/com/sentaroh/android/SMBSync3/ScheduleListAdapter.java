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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;

class ScheduleListAdapter extends ArrayAdapter<ScheduleListAdapter.ScheduleListItem> {
    private static final Logger log= LoggerFactory.getLogger(ScheduleListAdapter.class);
    private int layout_id = 0;
    private Activity mActivity = null;
    private int text_color = 0;
    private NotifyEvent mCbNotify = null;
    private NotifyEvent mSwNotify = null;
    private NotifyEvent mSyncNotify = null;
    private ArrayList<ScheduleListItem> mScheduleList = null;
    private GlobalParameters mGp=null;

    public ScheduleListAdapter(Activity a, int textViewResourceId, ArrayList<ScheduleListItem> sl) {
        super(a, textViewResourceId, sl);
        layout_id = textViewResourceId;
        mActivity = a;
        mScheduleList = sl;
        mGp=GlobalWorkArea.getGlobalParameter(a);
    }

    public void setCbNotify(NotifyEvent ntfy) {
        mCbNotify = ntfy;
    }

    public void setSwNotify(NotifyEvent ntfy) {
        mSwNotify = ntfy;
    }

    public void setSyncNotify(NotifyEvent ntfy) {
        mSyncNotify = ntfy;
    }

    public void sort() {
        sort(new Comparator<ScheduleListItem>() {
            @Override
            public int compare(ScheduleListItem lhs, ScheduleListItem rhs) {
                String lhs_key=lhs.schedulePosition+" "+lhs.scheduleName;
                String rhs_key=rhs.schedulePosition+" "+rhs.scheduleName;
                return lhs_key.compareToIgnoreCase(rhs_key);
            }
        });
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (ScheduleListItem si : mScheduleList) si.isChecked = true;
        notifyDataSetChanged();
    }

    public void unselectAll() {
        for (ScheduleListItem si : mScheduleList) {
            si.isChecked = false;
        }
        notifyDataSetChanged();
    }

    private boolean mSelectMode = false;

    public void setSelectMode(boolean select_mode) {
        mSelectMode = select_mode;
        if (!mSelectMode) unselectAll();
        else notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return mSelectMode;
    }
//        @Override
//        public void add(ScheduleItem si) {
//            mScheduleList.add(si);
//        }
//
//        @Override
//        public ScheduleItem getItem(int pos) {
//            return mScheduleList.get(pos);
//        }
//        @Override
//        public int getCount() {
//            return mScheduleList.size();
//        }

    private Drawable ll_default_background_color=null;

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        final ScheduleListItem o = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(layout_id, null);
            holder = new ViewHolder();
            holder.ll_view=(LinearLayout)v.findViewById(R.id.schedule_sync_list_view);
            ll_default_background_color=holder.ll_view.getBackground();
            holder.tv_name = (TextView) v.findViewById(R.id.schedule_sync_list_name);
            holder.tv_info = (TextView) v.findViewById(R.id.schedule_sync_list_info);
            holder.tv_time_info = (TextView) v.findViewById(R.id.schedule_sync_list_time_info);
            holder.tv_error_info = (TextView) v.findViewById(R.id.schedule_sync_list_error_info);
            holder.tv_error_info.setTextColor(mGp.themeColorList.text_color_warning);
            holder.swEnabled=(Switch)v.findViewById(R.id.schedule_sync_list_switch);
            holder.ib_sync=(ImageButton) v.findViewById(R.id.schedule_sync_list_sync);
            holder.cbChecked = (CheckBox) v.findViewById(R.id.schedule_sync_list_checked);
            text_color = holder.tv_name.getCurrentTextColor();
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        if (o != null) {
            holder.tv_name.setText(o.scheduleName);
            holder.tv_info.setText(ScheduleUtils.buildScheduleNextInfo(mActivity, o));

            if (mSelectMode) {
                holder.cbChecked.setVisibility(CheckBox.VISIBLE);
                holder.swEnabled.setVisibility(CheckBox.GONE);
                holder.ib_sync.setVisibility(CheckBox.GONE);
            } else {
                holder.cbChecked.setVisibility(CheckBox.GONE);
                holder.swEnabled.setVisibility(CheckBox.VISIBLE);
                holder.ib_sync.setVisibility(CheckBox.VISIBLE);
            }

            holder.ll_view.setBackground(ll_default_background_color);
            if (!mGp.settingScheduleSyncEnabled) {
                holder.ll_view.setBackgroundColor(mGp.themeColorList.text_color_disabled);
                holder.swEnabled.setEnabled(false);
            } else {
                holder.swEnabled.setEnabled(true);
            }
            String time_info = "";
            if (o.scheduleType.equals(ScheduleListItem.SCHEDULE_TYPE_EVERY_HOURS)) {
                time_info = mActivity.getString(R.string.msgs_scheduler_main_spinner_sched_type_every_hour) + " " + o.scheduleMinutes + " " +
                        mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_minute);
            } else if (o.scheduleType.equals(ScheduleListItem.SCHEDULE_TYPE_EVERY_DAY)) {
                time_info = mActivity.getString(R.string.msgs_scheduler_main_spinner_sched_type_every_day) + " " + o.scheduleHours + ":" + o.scheduleMinutes;
            } else if (o.scheduleType.equals(ScheduleListItem.SCHEDULE_TYPE_EVERY_MONTH)) {
                String ld=o.scheduleDay.equals("99")? mActivity.getString(R.string.msgs_scheduler_info_last_day_of_the_month):o.scheduleDay;
                time_info = mActivity.getString(R.string.msgs_scheduler_main_spinner_sched_type_every_month) + " " + ld + " " + o.scheduleHours + ":" + o.scheduleMinutes;
            } else if (o.scheduleType.equals(ScheduleListItem.SCHEDULE_TYPE_DAY_OF_THE_WEEK)) {
                String day_of_the_week = "";
                if (o.scheduleDayOfTheWeek.substring(0, 1).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_sun);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_sun);
                }
                if (o.scheduleDayOfTheWeek.substring(1, 2).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_mon);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_mon);
                }
                if (o.scheduleDayOfTheWeek.substring(2, 3).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_tue);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_tue);
                }
                if (o.scheduleDayOfTheWeek.substring(3, 4).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_wed);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_wed);
                }
                if (o.scheduleDayOfTheWeek.substring(4, 5).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_thu);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_thu);
                }
                if (o.scheduleDayOfTheWeek.substring(5, 6).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_fri);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_fri);
                }
                if (o.scheduleDayOfTheWeek.substring(6, 7).equals("1")) {
                    if (day_of_the_week.length() == 0)
                        day_of_the_week += mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_sat);
                    else
                        day_of_the_week += "," + mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_sat);
                }
                time_info = mActivity.getString(R.string.msgs_scheduler_main_spinner_sched_type_day_of_week) +
                        " " + day_of_the_week + " " + o.scheduleHours + ":" + o.scheduleMinutes;
            } else if (o.scheduleType.equals(ScheduleListItem.SCHEDULE_TYPE_INTERVAL)) {
                time_info = mActivity.getString(R.string.msgs_scheduler_main_spinner_sched_type_interval) + " " + o.scheduleMinutes + " " +
                        mActivity.getString(R.string.msgs_scheduler_main_dlg_hdr_minute);
            }
            String sync_prof = "";
            if (o.syncAutoSyncTask) {
                sync_prof = mActivity.getString(R.string.msgs_scheduler_info_sync_all_active_profile);
                holder.tv_error_info.setVisibility(TextView.GONE);
            } else {
                String error_msg=isValidScheduleItem(mActivity, mGp, o);
                if (!error_msg.equals("")) {
                    holder.tv_error_info.setVisibility(TextView.VISIBLE);
                    holder.tv_error_info.setText(error_msg);
                } else {
                    holder.tv_error_info.setVisibility(TextView.GONE);
                    holder.tv_error_info.setText("");
                }
                sync_prof = String.format(mActivity.getString(R.string.msgs_scheduler_info_sync_selected_profile),
                        o.syncTaskList);
            }
            holder.tv_time_info.setText(time_info + " " + sync_prof);

            holder.cbChecked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isChecked = holder.cbChecked.isChecked();
                    o.isChecked = isChecked;
                    if (mCbNotify != null)
                        mCbNotify.notifyToListener(true, new Object[]{isChecked});
                }
            });
            holder.cbChecked.setChecked(o.isChecked);

            holder.swEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (o.scheduleEnabled!=isChecked) {
                        o.scheduleEnabled = isChecked;
                        if (mSwNotify != null)
                            mSwNotify.notifyToListener(true, new Object[]{position});
                    }
                    notifyDataSetChanged();
                }
            });
            holder.swEnabled.setChecked(o.scheduleEnabled);

            holder.ib_sync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (o.syncAutoSyncTask) {
                        if (mSyncNotify != null) mSyncNotify.notifyToListener(true, new Object[]{o});
                    } else {
                        String error=isValidScheduleItem(mActivity, mGp, o);
                        if (error.equals("")) {
                            if (mSyncNotify != null) mSyncNotify.notifyToListener(true, new Object[]{o});
                        }
                    }
                }
            });

            holder.ib_sync.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    CommonDialog.showPopupMessageAsUpAnchorView((ActivityMain)(parent.getContext()), v,
                            mActivity.getString(R.string.msgs_schedule_sync_task_start_label, o.scheduleName), 2);
                    return true;
                }
            });

        }
        return v;

    }

    static public String isValidScheduleItem(Context c, GlobalParameters gp, ScheduleListItem sched_item) {
        boolean schedule_error=false;
        String error_item_name="";
        String error_msg="";
        if (sched_item.syncTaskList.equals("")) {
            schedule_error=true;
        } else {
            String[] task_name_array=sched_item.syncTaskList.split(NAME_LIST_SEPARATOR);
            String sep="";
            for(String stn:task_name_array) {
                if (ScheduleUtils.getSyncTask(gp,stn)==null) {
                    schedule_error=true;
                    error_item_name=sep+stn;
                    sep=",";
                }
            }
        }
        if (schedule_error) {
            if (sched_item.syncTaskList.equals("")) {
                error_msg=c.getString(R.string.msgs_scheduler_info_sync_task_list_was_empty);
            } else {
                error_msg=c.getString(R.string.msgs_scheduler_info_sync_task_was_not_found, error_item_name);
            }
        }
        return error_msg;
    }

    class ViewHolder {
        TextView tv_name, tv_info, tv_enabled, tv_time_info, tv_error_info;
        LinearLayout ll_view;
        CheckBox cbChecked;
        Switch swEnabled;
        ImageButton ib_sync;
    }

    public static class ScheduleListItem implements Serializable, Cloneable {
        public static final String SCHEDULE_TYPE_EVERY_HOURS = "H";
        public static final String SCHEDULE_TYPE_EVERY_DAY = "D";
        public static final String SCHEDULE_TYPE_DAY_OF_THE_WEEK = "W";
        public static final String SCHEDULE_TYPE_EVERY_MONTH = "M";
        public static final String SCHEDULE_TYPE_INTERVAL = "I";
        public static final String SCHEDULE_TYPE_DEFAULT = SCHEDULE_TYPE_EVERY_DAY;
        public static final String[] SCHEDULE_TYPE_LIST =new String[]{SCHEDULE_TYPE_EVERY_HOURS, SCHEDULE_TYPE_EVERY_DAY,
                SCHEDULE_TYPE_DAY_OF_THE_WEEK, SCHEDULE_TYPE_EVERY_MONTH, SCHEDULE_TYPE_INTERVAL};

        public boolean scheduleEnabled = false;

        public String scheduleName = "";

        public int schedulePosition = 0;

        public String scheduleType = SCHEDULE_TYPE_DEFAULT;
        public String scheduleDay = "01";
        public String scheduleHours = "00";
        public String scheduleMinutes = "00";
        public String scheduleDayOfTheWeek = "0000000";

        public long scheduleLastExecTime = 0;

        public String syncTaskList = "";

        public boolean syncAutoSyncTask=true;

        public String syncGroupList = "";

        public boolean syncWifiOnBeforeStart = false;
        public boolean syncWifiOffAfterEnd = false;
        public int syncDelayAfterWifiOn = 5;

        public final static String OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE="0";
        public final static String OVERRIDE_SYNC_OPTION_ENABLED="1";
        public final static String OVERRIDE_SYNC_OPTION_DISABLED="2";
        public final static String OVERRIDE_SYNC_OPTION_DEFAULT=OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
        public final static String[] OVERRIDE_SYNC_OPTION_LIST=new String[]{OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE, OVERRIDE_SYNC_OPTION_ENABLED, OVERRIDE_SYNC_OPTION_DISABLED};
        public String syncOverrideOptionCharge=OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;

        //    public String syncOverrideOptionWifiStatus=OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
        //    public ArrayList<String>syncOverrideOptionWifiApList=new ArrayList<String>();
        //    public ArrayList<String>syncOverrideOptionWifiIpAddressList=new ArrayList<String>();

        public transient boolean isChecked = false;
    //    public transient boolean isChanged = false;

        public boolean isSame(ScheduleListItem new_item) {
            if (
                    this.scheduleEnabled ==new_item.scheduleEnabled &&
                    this.scheduleName.equals(new_item.scheduleName) &&
                    this.schedulePosition==new_item.schedulePosition &&

                    this.scheduleType.equals(new_item.scheduleType) &&
                    this.scheduleDay.equals(new_item.scheduleDay) &&
                    this.scheduleHours.equals(new_item.scheduleHours) &&
                    this.scheduleMinutes.equals(new_item.scheduleMinutes) &&
                    this.scheduleDayOfTheWeek.equals(new_item.scheduleDayOfTheWeek) &&

                    this.syncTaskList.equals(new_item.syncTaskList) &&

                    this.syncAutoSyncTask==new_item.syncAutoSyncTask &&

                    this.syncGroupList.equals(new_item.syncGroupList) &&

                    this.syncWifiOnBeforeStart==new_item.syncWifiOnBeforeStart &&
                    this.syncWifiOffAfterEnd==new_item.syncWifiOffAfterEnd &&
                    this.syncDelayAfterWifiOn==new_item.syncDelayAfterWifiOn &&

                    this.scheduleLastExecTime==new_item.scheduleLastExecTime &&

                    this.syncOverrideOptionCharge.equals(new_item.syncOverrideOptionCharge)
            ) {
                return true;
            }
            return false;
        }

        @Override
        public ScheduleListItem clone() {
            ScheduleListItem new_si = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(this);

                oos.flush();
                oos.close();

                baos.flush();
                byte[] ba_buff = baos.toByteArray();
                baos.close();

                ByteArrayInputStream bais = new ByteArrayInputStream(ba_buff);
                ObjectInputStream ois = new ObjectInputStream(bais);

                new_si = (ScheduleListItem) ois.readObject();
                ois.close();
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return new_si;
        }
    }
}

