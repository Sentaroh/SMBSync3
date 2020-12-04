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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sentaroh.android.SMBSync3.Constants.DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX;

public class FilterListItem implements Comparable<FilterListItem>, Externalizable {

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
