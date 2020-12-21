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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.sentaroh.android.SMBSync3.Constants.*;

class GroupListAdapter extends ArrayAdapter<GroupListAdapter.GroupListItem> {
    private static final Logger log= LoggerFactory.getLogger(GroupListAdapter.class);

    private int layout_id = 0;
    private Context mContext = null;
    private int text_color = 0;
    private NotifyEvent mCbNotify = null;
    private NotifyEvent mSwNotify = null;
    private NotifyEvent mSyncNotify = null;
    private ArrayList<GroupListItem> mGroupList = null;
    private GlobalParameters mGp=null;

    public GroupListAdapter(Context c, int textViewResourceId, ArrayList<GroupListItem> sl) {
        super(c, textViewResourceId, sl);
        layout_id = textViewResourceId;
        mContext = c;
        mGroupList = sl;
        mGp=GlobalWorkArea.getGlobalParameter(c);
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
        sort(mGroupList);
        notifyDataSetChanged();
    }

    static public void sort(ArrayList<GroupListItem> gl) {
        Collections.sort(gl, new Comparator<GroupListItem>() {
            @Override
            public int compare(GroupListItem lhs, GroupListItem rhs) {
                String lhs_key=lhs.position+" "+lhs.groupName;
                String rhs_key=rhs.position+" "+rhs.groupName;
                return lhs_key.compareToIgnoreCase(rhs_key);
            }
        });
    }

    public void selectAll() {
        for (GroupListItem si : mGroupList) {
            si.isChecked = true;
        }
        notifyDataSetChanged();
    }

    public void unselectAll() {
        for (GroupListItem si : mGroupList) {
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

    private Drawable ll_default_background_color=null;

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        final GroupListItem o = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(layout_id, null);
            holder = new ViewHolder();
            holder.ll_view=(LinearLayout)v.findViewById(R.id.group_list_item_view);
            ll_default_background_color=holder.ll_view.getBackground();
            holder.tv_name = (TextView) v.findViewById(R.id.group_list_item_group_name);
            holder.tv_task_list = (TextView) v.findViewById(R.id.group_list_item_task_list);
            holder.tv_error_message = (TextView) v.findViewById(R.id.group_list_item_error_message);
            holder.sw_enable = (Switch) v.findViewById(R.id.group_list_item_enable);
            holder.tv_button = (TextView) v.findViewById(R.id.group_list_item_button);
            holder.ib_sync=(ImageButton)v.findViewById(R.id.group_list_item_sync);
            holder.cb_checked=(CheckBox) v.findViewById(R.id.group_list_item_checked);
            text_color = holder.tv_name.getCurrentTextColor();
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        if (o != null) {
            holder.tv_name.setText(o.groupName);
            holder.tv_task_list.setText(o.taskList.replaceAll(NAME_LIST_SEPARATOR, ", "));
            if (o.autoTaskOnly) holder.tv_task_list.setText(mContext.getString(R.string.msgs_group_main_dlg_hdr_auto_task_only));

            if (o.button == GroupListItem.BUTTON_SHORTCUT1) holder.tv_button.setText(mContext.getString(R.string.msgs_group_name_for_shortcut1));
            else if (o.button == GroupListItem.BUTTON_SHORTCUT2) holder.tv_button.setText(mContext.getString(R.string.msgs_group_name_for_shortcut2));
            else if (o.button == GroupListItem.BUTTON_SHORTCUT3) holder.tv_button.setText(mContext.getString(R.string.msgs_group_name_for_shortcut3));
            else if (o.button == GroupListItem.BUTTON_SYNC_BUTTON) holder.tv_button.setText(mContext.getString(R.string.msgs_group_name_for_sync_button));
            else holder.tv_button.setText(mContext.getString(R.string.msgs_group_button_not_assigned));

            if (mSelectMode) {
                holder.cb_checked.setVisibility(CheckBox.VISIBLE);
                holder.ib_sync.setVisibility(CheckBox.GONE);
                holder.sw_enable.setVisibility(TextView.GONE);
            } else {
                holder.cb_checked.setVisibility(CheckBox.GONE);
                holder.sw_enable.setVisibility(TextView.VISIBLE);
                if (o.enabled) {
                    holder.ib_sync.setVisibility(CheckBox.VISIBLE);
                } else {
                    holder.ib_sync.setVisibility(CheckBox.INVISIBLE);
                }
            }

            if(isEnabled(position)) CommonDialog.setViewEnabled(mContext, holder.ll_view, true);
            else CommonDialog.setViewEnabled(mContext, holder.ll_view, false);

            holder.sw_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (o.enabled!=isChecked) {
                        o.enabled = isChecked;
                        if (mSwNotify != null)
                            mSwNotify.notifyToListener(true, new Object[]{position});
                    }
                    notifyDataSetChanged();
                }
            });
            holder.sw_enable.setChecked(o.enabled);

            holder.cb_checked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isChecked = holder.cb_checked.isChecked();
                    o.isChecked = isChecked;
                    if (mCbNotify != null) mCbNotify.notifyToListener(true, new Object[]{o});
                }
            });
            holder.cb_checked.setChecked(o.isChecked);

            if (o.autoTaskOnly) {
                holder.tv_error_message.setVisibility(TextView.GONE);
                holder.tv_error_message.setText("");
            } else {
                holder.tv_error_message.setText("");
                holder.tv_error_message.setVisibility(TextView.GONE);
                String e_msg=GroupEditor.hasValidSyncTaskList(mContext, o, mGp.syncTaskList);
                if (!e_msg.equals("")) {
                    holder.tv_error_message.setVisibility(TextView.VISIBLE);
                    holder.tv_error_message.setText(e_msg);
                    holder.ib_sync.setVisibility(ImageButton.INVISIBLE);
                }
            }

            holder.ib_sync.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (o.enabled) {
                        CommonDialog.showPopupMessageAsUpAnchorView((Activity)mContext,
                                holder.ib_sync, mContext.getString(R.string.msgs_task_cont_label_sync_specific,
                                        GroupEditor.getSyncTaskList(mContext, o, mGp.syncTaskList)), 2);
                    }
                    return true;
                }
            });

            holder.ib_sync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mSyncNotify!=null && o.enabled) mSyncNotify.notifyToListener(true, new Object[]{o});
                }
            });
        }
        return v;

    }

    class ViewHolder {
        TextView tv_name, tv_task_list, tv_error_message;
        Switch sw_enable;
        TextView tv_button;
        LinearLayout ll_view;
//        Switch sw_enabled;
        CheckBox cb_checked;
        ImageButton ib_sync;
    }

    static class GroupListItem implements Cloneable, Serializable {
        public String groupName="";
        public boolean enabled=true;
        public boolean isChecked=false;
        public int position=0;
        public boolean autoTaskOnly=false;
        public String taskList="";

        final static public int BUTTON_NOT_ASSIGNED=0;
        final static public int BUTTON_SHORTCUT1=1;
        final static public int BUTTON_SHORTCUT2=2;
        final static public int BUTTON_SHORTCUT3=3;
        final static public int BUTTON_SYNC_BUTTON =9;
        public int button =BUTTON_NOT_ASSIGNED;

        public GroupListItem() {
            //NOP
        }

        public boolean isSame(GroupListItem new_gi) {
            boolean result=false;
            if (this.groupName.equalsIgnoreCase(new_gi.groupName)
                    && this.enabled==new_gi.enabled
                    && this.autoTaskOnly==new_gi.autoTaskOnly
                    && this.position==new_gi.position
                    && this.button ==new_gi.button) {
                if (this.taskList.equalsIgnoreCase(new_gi.taskList)) result=true;
            }
            return result;
        }

        @Override
        public GroupListItem clone() {
            GroupListItem new_gi = null;
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

                new_gi = (GroupListItem) ois.readObject();
                ois.close();
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            return new_gi;
        }


    }
}

