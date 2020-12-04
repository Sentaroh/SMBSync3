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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.ThemeColorList;
import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("ALL")
public class MessageListAdapter extends BaseAdapter {
    private static final Logger log= LoggerFactory.getLogger(MessageListAdapter.class);
    private Context c;
    private int id;
    private ArrayList<MessageListItem> showList;
    private ArrayList<MessageListItem> messageList;

    private GlobalParameters mGp=null;

    private ThemeColorList mThemeColorList;
    private int mTextColorSyncStarted;
    private int mTextColorSyncSuccess;
    private int mTextColorSyncCancel;
    private int mTextColorSyncDelete;
    private int mTextColorSyncReplace;

    private static final int FOREST_GREEN = 0xff228B22;//rgb(34,139,34)
    private static final int DKCYAN = 0xff0088ff;//rgb(0, 136, 255)

    public MessageListAdapter(Context context, int textViewResourceId, ArrayList<MessageListItem> objects, GlobalParameters gp) {
        c = context;
        id = textViewResourceId;
        messageList = objects;
        showList=new ArrayList<MessageListItem>(5500);
        mGp=gp;
        vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mThemeColorList = gp.themeColorList;

        if (ThemeUtil.getAppTheme(c)==ThemeUtil.THEME_DEFAULT) {
            mTextColorSyncStarted = Color.WHITE;
            mTextColorSyncSuccess = Color.GREEN;
            mTextColorSyncCancel = mThemeColorList.text_color_warning;//Color.YELLOW
            mTextColorSyncDelete = mThemeColorList.text_color_error;//Color.RED
            mTextColorSyncReplace = mThemeColorList.text_color_warning;//Color.YELLOW
        } else if (ThemeUtil.getAppTheme(c)==ThemeUtil.THEME_BLACK) {
            mTextColorSyncStarted = Color.WHITE;
            mTextColorSyncSuccess = Color.GREEN;
            mTextColorSyncCancel = mThemeColorList.text_color_warning;//Color.YELLOW
            mTextColorSyncDelete = mThemeColorList.text_color_error;//Color.RED
            mTextColorSyncReplace = mThemeColorList.text_color_warning;//Color.YELLOW
        } else {
            mTextColorSyncStarted = DKCYAN;
            mTextColorSyncSuccess = FOREST_GREEN;
            mTextColorSyncCancel = mThemeColorList.text_color_warning;
            mTextColorSyncDelete = mThemeColorList.text_color_error;//Color.RED
            mTextColorSyncReplace = mThemeColorList.text_color_warning;
        }
        updateShowList();
    }


    private boolean filterByCategoryInfo=true;
    private boolean filterByCategoryWarn=true;
    private boolean filterByCategoryError=true;
    public void setFilterInfo(boolean enabled) {
        filterByCategoryInfo=enabled;
        notifyDataSetChanged();
    }
    public void setFilterWarn(boolean enabled) {
        filterByCategoryWarn=enabled;
        notifyDataSetChanged();
    }
    public void setFilterError(boolean enabled) {
        filterByCategoryError=enabled;
        notifyDataSetChanged();
    }

    private Pattern filterPattern=null;
    private String filterByString="";
    public void setFilterString(String filter) {
        filterByString=filter;
        if (filterByString.equals("")) {
            filterPattern=null;
            notifyDataSetChanged();
        } else {
            filterPattern=Pattern.compile(filterByString, Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
            notifyDataSetChanged();
        }
    }

    public void setFilterString(String filter, boolean case_sensitive) {
        filterByString=filter;
        if (filterByString.equals("")) {
        } else {
            if (!case_sensitive) filterPattern=Pattern.compile(filterByString, Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
            else filterPattern=Pattern.compile(filterByString, Pattern.MULTILINE);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        showList.clear();
        messageList.clear();
        updateShowList();
    }

    public void refresh() {
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        updateShowList();
    }

    private void updateShowList() {
        showList.clear();
        for(MessageListItem mi:messageList) {
            boolean cat_filtered=false;
            if (mi.getCategory().equals(MessageListItem.MESSAGE_CATEGORY_INFO)) {
                if (filterByCategoryInfo) cat_filtered=true;
            } else if (mi.getCategory().equals(MessageListItem.MESSAGE_CATEGORY_WARN)) {
                if (filterByCategoryWarn) cat_filtered=true;
            } else if (mi.getCategory().equals(MessageListItem.MESSAGE_CATEGORY_ERROR)) {
                if (filterByCategoryError) cat_filtered=true;
            }
            if (cat_filtered) {
                if (filterByString!=null && !filterByString.equals("")) {
                    String message_string=mi.getDate().concat(" ").concat(mi.getTime()).concat(" ").concat(mi.getMessage());
                    if (message_string.length()>filterByString.length()) {
                        Matcher matcher=filterPattern.matcher(message_string);
                        if (matcher.find()) {
                            showList.add(mi);
                        }
                    }
                } else {
                    showList.add(mi);
                }
            }
        }
        super.notifyDataSetChanged();
    }

    final public void remove(MessageListItem mli) {
        messageList.remove(mli);
        showList.remove(mli);
        super.notifyDataSetChanged();
    }

    final public void remove(int i) {
        MessageListItem del_item=messageList.get(i);
        messageList.remove(i);
        showList.remove(del_item);
        super.notifyDataSetChanged();
    }

    final public void add(MessageListItem mli) {
        messageList.add(mli);
        showList.add(mli);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return showList.size();
    }

    @Override
    final public MessageListItem getItem(int i) {
        return showList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    final public ArrayList<MessageListItem> getMessageList() {
        return messageList;
    }

    final public void setMessageList(List<MessageListItem> p) {
        messageList.clear();
        if (p != null) messageList.addAll(p);
        notifyDataSetChanged();
    }

    private LayoutInflater vi;

    private ColorStateList mTextColor=null;

    private Drawable mDefaultBackgroundColor=null;
    private ColorStateList mDefaultForegroundColor=null;

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        View v = convertView;
        if (v == null) {
            v = vi.inflate(id, null);
            holder = new ViewHolder();
            holder.tv_row_seq=(TextView) v.findViewById(R.id.message_list_item_seq_no);
            holder.tv_row_msg = (NonWordwrapTextView) v.findViewById(R.id.message_list_item_msg);
//            holder.tv_row_msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, mGp.displayFontSizeMedium);
            holder.tv_row_time = (TextView) v.findViewById(R.id.message_list_item_time);
//            holder.tv_row_time.setTextSize(TypedValue.COMPLEX_UNIT_SP, mGp.displayFontSizeMedium);
            holder.tv_row_date = (TextView) v.findViewById(R.id.message_list_item_date);
//            holder.tv_row_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, mGp.displayFontSizeMedium);

            holder.tv_row_path = (NonWordwrapTextView) v.findViewById(R.id.message_list_item_path);
            holder.tv_row_path.setWordWrapEnabled(mGp.settingSyncMessageUseStandardTextView);
            holder.tv_row_type = (TextView) v.findViewById(R.id.message_list_item_type);
            holder.tv_row_title = (TextView) v.findViewById(R.id.message_list_tem_title);

            v.setTag(holder);
            if (mTextColor==null) mTextColor=holder.tv_row_msg.getTextColors();
        } else {
            holder = (ViewHolder) v.getTag();
        }
        MessageListItem o = getItem(position);
        if (o != null) {
            boolean show_type;
            holder.tv_row_seq.setText(String.valueOf(position+1)+") ");
            if(o.getType().length() == 0 || o.getType().equals("")) {
                holder.tv_row_type.setVisibility(View.GONE);
                show_type = false;
            } else {
                holder.tv_row_type.setVisibility(View.VISIBLE);
                show_type = true;
            }

            boolean show_path;
            if(o.getPath().length() == 0 || o.getPath().equals("")) {
                holder.tv_row_path.setVisibility(View.GONE);
                show_path = false;
            } else {
                holder.tv_row_path.setVisibility(View.VISIBLE);
                show_path = true;
            }

            boolean show_title;
            if(o.getTitle().length() == 0 || o.getTitle().equals("")) {
                holder.tv_row_title.setVisibility(View.GONE);
                show_title = false;
            } else {
                holder.tv_row_title.setVisibility(View.VISIBLE);
                show_title = true;
            }

            boolean show_msg;
            if(o.getMessage().length() == 0 || o.getMessage().equals("")) {
                holder.tv_row_msg.setVisibility(View.GONE);
                show_msg = false;
            } else {
                holder.tv_row_msg.setVisibility(View.VISIBLE);
                show_msg = true;
            }

            String cat = o.getCategory();
            String message = o.getMessage();

            int col_header = 0;
            int col_type = 0;

            if (cat.equals("W")) {
                col_header = mThemeColorList.text_color_warning;
                col_type = col_header;
            } else if (cat.equals("E")) {
                col_header = mThemeColorList.text_color_error;
                col_type = col_header;
            } else {//CAT=I
                if (message.endsWith(c.getString(R.string.msgs_mirror_task_started))) {
                    col_header = mTextColorSyncStarted;
                } else if (message.endsWith(c.getString(R.string.msgs_mirror_task_result_ok))) {
                    col_header = mTextColorSyncSuccess;
                } else if (message.endsWith(c.getString(R.string.msgs_mirror_task_result_cancel))) {
                    col_header = mTextColorSyncCancel;
                }
            }

            if (show_type) {
                if (o.getType().equals(c.getString(R.string.msgs_mirror_task_file_deleted))
                        || o.getType().equals(c.getString(R.string.msgs_mirror_task_dir_deleted))) {
                    col_type = mTextColorSyncDelete;
                } else if (o.getType().equals(c.getString(R.string.msgs_mirror_task_file_replaced))) {
                    col_type = mTextColorSyncReplace;
                } else if (o.getType().equals(c.getString(R.string.msgs_mirror_confirm_move_cancel)) ||
                        o.getType().equals(c.getString(R.string.msgs_mirror_confirm_copy_cancel)) ||
                        o.getType().equals(c.getString(R.string.msgs_mirror_confirm_delete_cancel)) ||
                        o.getType().equals(c.getString(R.string.msgs_mirror_confirm_archive_date_time_from_file_cancel)) ||
                        o.getType().equals(c.getString(R.string.msgs_mirror_task_file_ignored))) {
                    col_type = mTextColorSyncCancel;
                }
            }

            //set messages color
            if (col_header == 0) {
                holder.tv_row_seq.setTextColor(mTextColor);
                holder.tv_row_time.setTextColor(mTextColor);
                holder.tv_row_date.setTextColor(mTextColor);
                if (show_title) holder.tv_row_title.setTextColor(mTextColor);
                if (show_msg) holder.tv_row_msg.setTextColor(mTextColor);
                if (show_path) holder.tv_row_path.setTextColor(mTextColor);
            } else {
                holder.tv_row_seq.setTextColor(col_header);
                holder.tv_row_time.setTextColor(col_header);
                holder.tv_row_date.setTextColor(col_header);
                if (show_title) holder.tv_row_title.setTextColor(col_header);
                if (show_msg) holder.tv_row_msg.setTextColor(col_header);
                if (show_path) holder.tv_row_path.setTextColor(col_header);
            }
            if (show_type) {
                if (col_type == 0) holder.tv_row_type.setTextColor(mTextColor);
                else holder.tv_row_type.setTextColor(col_type);
            }

            //set messages text
            holder.tv_row_time.setText(o.getTime());
            holder.tv_row_date.setText(o.getDate());
            if (show_title) holder.tv_row_title.setText(o.getTitle());
            if (show_msg) {
                holder.tv_row_msg.setText(o.getMessage());
                holder.tv_row_msg.requestLayout();
            }
            if (show_path) {
                holder.tv_row_path.setText(o.getPath());
                holder.tv_row_path.requestLayout();
            }
            if (show_type) holder.tv_row_type.setText(o.getType());

        }
        return v;
    }

    private class ViewHolder {
        TextView tv_row_time, tv_row_date, tv_row_title, tv_row_type, tv_row_seq;
        NonWordwrapTextView  tv_row_msg, tv_row_path;
    }
}

