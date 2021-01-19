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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.NotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sentaroh.android.SMBSync3.Constants.DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX;

public class FilterListAdapter extends ArrayAdapter<FilterListAdapter.FilterListItem> {
    private static final Logger log= LoggerFactory.getLogger(FilterListAdapter.class);
    private Context c;
    private int id;
    private ArrayList<FilterListItem> items;

    private boolean mShowIncludeExclue = true;

    public NotifyEvent mNotifyIncExcListener = null;

    public void setNotifyIncExcListener(NotifyEvent p) {
        mNotifyIncExcListener = p;
    }

    public void unsetNotifyIncExcListener() {
        mNotifyIncExcListener = null;
    }

    public NotifyEvent mNotifyDeleteListener = null;

    public void setNotifyDeleteListener(NotifyEvent p) {
        mNotifyDeleteListener = p;
    }

    public void unsetNotifyDeleteListener() {
        mNotifyDeleteListener = null;
    }

    private int mFilterType=0;
    public static final int FILTER_TYPE_DIRECTORY=1;
    public static final int FILTER_TYPE_FILE=2;
    public static final int FILTER_TYPE_ACCESS_POINT=3;
    public static final int FILTER_TYPE_IP_ADDRESS=4;
    public void setFilterType(int filter_type) {mFilterType=filter_type;}
    public int getFilterType() {return mFilterType;}

    private String mStringEnabled="", mStringDisabled="", mStringInclude="", mStringExclude="";

    public FilterListAdapter(Context context, int textViewResourceId,
                             ArrayList<FilterListItem> objects, int filter_type) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
        mShowIncludeExclue = true;

        mStringEnabled=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_filter_enabled);
        mStringDisabled=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_filter_disabled);
        mStringInclude=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_include);
        mStringExclude=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_exclude);

        mFilterType=filter_type;
    }

    public FilterListAdapter(Context context, int textViewResourceId,
                             ArrayList<FilterListItem> objects, boolean show_inc_exc, int filter_type) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
        mShowIncludeExclue = show_inc_exc;

        mStringEnabled=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_filter_enabled);
        mStringDisabled=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_filter_disabled);
        mStringInclude=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_include);
        mStringExclude=c.getString(R.string.msgs_task_sync_task_filter_list_dlg_exclude);

        mFilterType=filter_type;
    }

    public ArrayList<FilterListItem> getFilterList() {
        return items;
    }

    @Override
    public FilterListItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public void add(FilterListItem fli) {
        items.add(fli);
        notifyDataSetChanged();
    }

    public void remove(int i) {
        items.remove(i);
        notifyDataSetChanged();
    }

    public void replace(FilterListItem fli, int i) {
        items.set(i, fli);
        notifyDataSetChanged();
    }

    public void sort() {
        sort(items);
    }

    static public void sort(ArrayList<FilterListItem> filter_list) {
        Collections.sort(filter_list, new Comparator<FilterListItem>() {
            @Override
            public int compare(FilterListItem lhs, FilterListItem rhs) {
                String key_lhs=(lhs.getFilter().startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)?"0":"1")+lhs.getFilter();
                String key_rhs=(rhs.getFilter().startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)?"0":"1")+rhs.getFilter();
                return key_lhs.compareToIgnoreCase(key_rhs);
            }
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder = new ViewHolder();
            holder.ll_file_directory_only_view=(LinearLayout) v.findViewById(R.id.filter_list_item_file_directory_only_view);
            holder.btn_row_delbtn = (ImageButton) v.findViewById(R.id.filter_list_item_del_btn);
            holder.tv_row_filter = (TextView) v.findViewById(R.id.filter_list_item_filter);

            holder.del_msg = c.getString(R.string.msgs_filter_list_filter_deleted);

            holder.sw_row_enabled =(Switch) v.findViewById(R.id.filter_list_item_filter_enabled);
            holder.rb_row_include =(RadioButton) v.findViewById(R.id.filter_list_item_include);
            holder.rb_row_exclude =(RadioButton) v.findViewById(R.id.filter_list_item_exclude);

            holder.tv_error_message=(TextView)v.findViewById(R.id.filter_list_item_error_message);
            holder.tv_error_message.setTextColor(Color.RED);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        final FilterListItem o = getItem(position);

        if (o != null) {
            holder.tv_row_filter.setText(o.getFilter());
            holder.tv_row_filter.setVisibility(View.VISIBLE);
//            holder.rb_grp.setVisibility(View.VISIBLE);

            if (o.isDeleted()) {
                holder.btn_row_delbtn.setAlpha(0.3f);
                holder.tv_row_filter.setEnabled(false);
                holder.btn_row_delbtn.setEnabled(false);
                holder.tv_row_filter.setText(holder.del_msg + " : " + o.getFilter());
                holder.ll_file_directory_only_view.setVisibility(LinearLayout.GONE);
                holder.sw_row_enabled.setVisibility(Switch.GONE);
            } else {
                holder.btn_row_delbtn.setAlpha(1.0f);
                holder.tv_row_filter.setEnabled(true);
                holder.btn_row_delbtn.setEnabled(true);
                if (mShowIncludeExclue) {
                    holder.sw_row_enabled.setVisibility(Switch.VISIBLE);
                    holder.ll_file_directory_only_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    holder.sw_row_enabled.setVisibility(Switch.GONE);
                    holder.ll_file_directory_only_view.setVisibility(LinearLayout.GONE);
                }
            }

            final int p = position;
            holder.btn_row_delbtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.delete();
                    notifyDataSetChanged();

                    if (mNotifyDeleteListener != null)
                        mNotifyDeleteListener.notifyToListener(true, new Object[]{o});
                }

            });
            if (getFilterType()==FILTER_TYPE_DIRECTORY) {
                String error_message=FilterListItem.checkDirectoryFilterError(c, o.getFilter());
                if (!error_message.equals("")) {
                    holder.tv_error_message.setVisibility(TextView.VISIBLE);
                    holder.tv_error_message.setText(error_message);
                } else {
                    holder.tv_error_message.setVisibility(TextView.GONE);
                }
            } else if (getFilterType()==FILTER_TYPE_FILE) {
                String error_message=FilterListItem.checkFileFilterError(c, o.getFilter());
                if (!error_message.equals("")) {
                    holder.tv_error_message.setVisibility(TextView.VISIBLE);
                    holder.tv_error_message.setText(error_message);
                } else {
                    holder.tv_error_message.setVisibility(TextView.GONE);
                }
            } else {
                String error_message=FilterListItem.checkApAndAddressFilterError(c, o.getFilter());
                if (!error_message.equals("")) {
                    holder.tv_error_message.setVisibility(TextView.VISIBLE);
                    holder.tv_error_message.setText(error_message);
                } else {
                    holder.tv_error_message.setVisibility(TextView.GONE);
                }
            }

            if (o.hasMatchAnyWhereFilter()) CommonUtilities.setViewEnabled(c, holder.rb_row_include, false);
            else CommonUtilities.setViewEnabled(c, holder.rb_row_include, true);

            holder.sw_row_enabled.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.setEnabled(holder.sw_row_enabled.isChecked());
                    if (mNotifyIncExcListener!=null) mNotifyIncExcListener.notifyToListener(true, new Object[]{o});
                }
            });

            holder.rb_row_include.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.setInclude(holder.rb_row_include.isChecked());
                    if (mNotifyIncExcListener!=null) mNotifyIncExcListener.notifyToListener(true, new Object[]{o});
                }
            });

            holder.rb_row_exclude.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    o.setInclude(holder.rb_row_include.isChecked());
                    if (mNotifyIncExcListener!=null) mNotifyIncExcListener.notifyToListener(true, new Object[]{o});
                }
            });

            holder.sw_row_enabled.setChecked(o.isEnabled());
            if (o.isInclude()) holder.rb_row_include.setChecked(true);
            else holder.rb_row_exclude.setChecked(true);

        }

        return v;
    }

    private static class ViewHolder {
        TextView tv_row_filter, tv_error_message;
        Switch sw_row_enabled;
        RadioButton rb_row_include, rb_row_exclude;
        Button btn_row_filter;
        ImageButton btn_row_delbtn;
        //		EditText et_filter;
        RadioGroup rb_grp;
        LinearLayout ll_file_directory_only_view;
        String del_msg;
    }

    public static class FilterListItem implements Comparable<FilterListItem>, Externalizable {

        private String filter="";
        private boolean includeFilter=true;// false:Exclude, true: Include
        private boolean regExp=false;
        private boolean migrateFromSmbsync2=false;
        private boolean filterEnabled=true;
        transient private boolean deleted = false;

        public String toString() {
            return getFilter()+", include="+isInclude()+", enabled="+isEnabled()+", migrate_from_smbsync2="+isMigrateFromSmbsync2();
        }

        public FilterListItem() {
        }

        public FilterListItem(String filter, boolean include) {
            this.filter = filter;
            this.includeFilter = include;
            this.deleted = false;
        }

        public FilterListItem(String filter, boolean include, boolean match_from_begin) {
            this.filter = filter;
            this.includeFilter = include;
            this.deleted = false;
        }

        public boolean hasMatchAnyWhereFilter() {
            return hasMatchAnyWhereFilter(getFilter());
        }

        static public boolean hasMatchAnyWhereFilter(String filter) {
            String[] filter_array=filter.split(";");
            for(String item:filter_array) {
                if (item.startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) return true;
            }
            return false;
        }

        private static String hasInvalidCharForFileDirFilter(String in_str) {
            if (in_str.contains(":")) return ":";
    //        if (in_str.contains("*")) return "*";
    //        if (in_str.contains("?")) return "?";
            if (in_str.contains("\"")) return "\"";
            if (in_str.contains("<")) return "<";
            if (in_str.contains(">")) return ">";
            if (in_str.contains("|")) return "|";
            if (in_str.contains("\n")) return "CR";
            if (in_str.contains("\t")) return "TAB";
            String printable=in_str.replaceAll("\\p{C}", "");
            if (in_str.length()!=printable.length()) return "UNPRINTABLE";
            return "";
        }

        static public String checkDirectoryFilterError(Context c, String filter) {
            String[] filter_array=filter.split(";");
            for(String item:filter_array) {
                if (!item.equals("")) {
                    String error_char= hasInvalidCharForFileDirFilter(item);
                    if (!error_char.equals("")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, error_char, item);
                        return mtxt;
                    }
                    if (item.substring(1).contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_multiple_back_slash, item);
                        return mtxt;
                    }
                    if (item.startsWith("/") || item.endsWith("/")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_start_or_end_char_not_allowed_slash, item);
                        return mtxt;
                    }
                    if (item.endsWith("/*")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_slash_asterisk, item);
                        return mtxt;
                    }
                    if (item.contains("**")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_multiple_asterisk, item);
                        return mtxt;
                    }
    //                String[] directory_array=item.split("/");
    //                for(String directory_part:directory_array) {
    //                    if (directory_part.contains("* ") || directory_part.contains(" *") || directory_part.contains(" * ")) {
    //                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_blank_with_asterisk);
    //                        return mtxt;
    //                    }
    //                }
                    if (item.startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                        if (item.startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX+"\\")) {
                            String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_multiple_back_slash, item);
                            return mtxt;
                        }
                        if (item.startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX+"*/")) {
                            String mtxt = c.getString(R.string.msgs_filter_list_filter_match_any_where_not_allowed_asterisk_slash, item);
                            return mtxt;
                        }
                        if (item.startsWith(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX+"/")) {
                            String mtxt = c.getString(R.string.msgs_filter_list_filter_start_or_end_char_not_allowed_slash, item);
                            return mtxt;
                        }
    //                    Pattern pattern= Pattern.compile("[^\\*\\.]");
    //                    Matcher mt=pattern.matcher(item.substring(1));
                        if (isWildCardOnly(item.substring(1))){//!mt.find()) {
                            String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_asterisk_only, item);
                            return mtxt;
                        }
                    } else {
    //                    Pattern pattern= Pattern.compile("[^\\*\\.]");
    //                    Matcher mt=pattern.matcher(item);
                        if (isWildCardOnly(item)){//!mt.find()) {
                            String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_asterisk_only, item);
                            return mtxt;
                        }
                    }
                }
            }
            return "";
        }

        static final Pattern WILDCARD_ONLY_PATTERN= Pattern.compile("[^\\*\\.]");
        static private boolean isWildCardOnly(String filter) {
            Matcher mt=WILDCARD_ONLY_PATTERN.matcher(filter);
            return !mt.find();
        }

        static public String checkApAndAddressFilterError(Context c, String filter) {
            String[] filter_array=filter.split(";");
            for(String item:filter_array) {
                if (!item.equals("")) {
                    String error_char= hasInvalidCharForFileDirFilter(item);
                    if (!error_char.equals("")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, error_char, item);
                        return mtxt;
                    }
                    if (item.contains("/")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, "/", item);
                        return mtxt;
                    }
                    if (item.contains("**")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_multiple_asterisk, item);
                        return mtxt;
                    }
                    if (item.contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX, item);
                        return mtxt;
                    }
    //            Pattern pattern= Pattern.compile("[^\\*\\.]");
    //            Matcher mt=pattern.matcher(item);
                    if (isWildCardOnly(item)){//!mt.find()) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_asterisk_only, item);
                        return mtxt;
                    }
                }
            }
            return "";
        }

        static public String checkFileFilterError(Context c, String filter) {
            String[] filter_array=filter.split(";");
            for(String item:filter_array) {
                if (!item.equals("")) {
                    if (item.startsWith("/") || item.endsWith("/")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_start_or_end_char_not_allowed_slash, item);
                        return mtxt;
                    }
                    if (item.contains("**")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_multiple_asterisk, item);
                        return mtxt;
                    }
                    String error_char= hasInvalidCharForFileDirFilter(item);
                    if (!error_char.equals("")) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, error_char, item);
                        return mtxt;
                    }
                    if (item.contains(DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX)) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_contains_not_allowed_character, DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX, item);
                        return mtxt;
                    }
    //            Pattern pattern= Pattern.compile("[^\\*\\.]");
    //            Matcher mt=pattern.matcher(item);
                    if (isWildCardOnly(item)){//!mt.find()) {
                        String mtxt = c.getString(R.string.msgs_filter_list_filter_not_allowed_asterisk_only, item);
                        return mtxt;
                    }
                }
            }
            return "";
        }


        public void setMigrateFromSmbsync2(boolean migrate) {
            migrateFromSmbsync2=migrate;
            if (migrate) setEnabled(false);
        }

        public boolean isMigrateFromSmbsync2() {return migrateFromSmbsync2;}

        public boolean isEnabled() {return filterEnabled;}

        public void setEnabled(boolean enabled) {filterEnabled=enabled;}

        public String getFilter() {
            return this.filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public boolean isInclude() {
            return this.includeFilter;
        }

        public void setInclude(boolean include) {
            this.includeFilter = include;
        }

        public boolean isRegExp() {
            return this.regExp;
        }

        public void setRegExp(boolean include) {
            this.regExp = include;
        }

        public boolean isDeleted() {
            return this.deleted;
        }

        public void delete() {
            this.deleted = true;
        }

        public void sortFilter() {
            String result=sort(getFilter());
            setFilter(result);
        }

        static public String sort(String filter) {
            if (filter.contains(";")) {
                ArrayList<String>fl=new ArrayList<String>();
                String[]filter_array=filter.split(";");
                for(String item:filter_array) {
                    fl.add(item);
                }
                Collections.sort(fl);
                String sort_result="", sep="";
                for(String sr_item:fl) {
                    sort_result+=sep+sr_item;
                    sep=";";
                }
                return sort_result;
            } else {
                return filter;
            }
        }

        @Override
        public FilterListItem clone() {
            FilterListItem npfli = null;
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

                npfli = (FilterListItem) ois.readObject();
                ois.close();
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            return npfli;

        }

        @Override
        public int compareTo(FilterListItem o) {
            if (this.filter != null)
                return this.filter.toLowerCase().compareTo(o.getFilter().toLowerCase());
                //				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
            else
                throw new IllegalArgumentException();
        }

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.writeUTF(filter);
            objectOutput.writeBoolean(includeFilter);
            objectOutput.writeBoolean(filterEnabled);
            objectOutput.writeBoolean(regExp);
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws ClassNotFoundException, IOException {
            filter=objectInput.readUTF();
            includeFilter=objectInput.readBoolean();
            filterEnabled=objectInput.readBoolean();
            regExp=objectInput.readBoolean();
        }
    }
}

