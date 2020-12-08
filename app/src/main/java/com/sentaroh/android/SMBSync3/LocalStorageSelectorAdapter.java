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
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sentaroh.android.Utilities3.ThemeUtil;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapCheckedTextView;

import java.util.ArrayList;

public class LocalStorageSelectorAdapter extends ArrayAdapter<LocalStorageSelectorAdapter.LocalStorageSelectorItem> {
    private ArrayList<LocalStorageSelectorItem> mItems=null;
    private Activity mActivity=null;
    private Context mContext=null;
    private int mSelectedPosition=0;

    public LocalStorageSelectorAdapter(Activity a, int resource, @NonNull ArrayList<LocalStorageSelectorItem> objects) {
        super(a, resource, objects);
        mItems=objects;
        mActivity=a;
        mContext=a.getApplicationContext();
    }

    @Override
    public LocalStorageSelectorItem getItem(int pos) {
        return mItems.get(pos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        LocalStorageSelectorItem ls_item=getItem(position);
        if (convertView == null) {
            view=(TextView)super.getView(position,convertView,parent);
        } else {
            view = (TextView)convertView;
        }
        if (ls_item.mounted) view.setText(ls_item.description+"("+ls_item.root_path+")");
        else view.setText(mContext.getString(R.string.msgs_main_external_storage_uuid_unusable)+"("+ls_item.uuid+")");
        view.setCompoundDrawablePadding(10);
        view.setCompoundDrawablesWithIntrinsicBounds(
                mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
                null, null, null);
        mSelectedPosition=position;
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            convertView = inflater.inflate(R.layout.spinner_dropdown_single_choice, null);
        }
        LocalStorageSelectorItem ls_item=getItem(position);
        String text = ls_item.description+"("+ls_item.root_path+")";
        if (!ls_item.mounted) text = mContext.getString(R.string.msgs_main_external_storage_uuid_unusable)+"("+ls_item.uuid+")";
        final NonWordwrapCheckedTextView text_view=(NonWordwrapCheckedTextView)convertView.findViewById(R.id.text1);
        SpannableStringBuilder sb=new SpannableStringBuilder(text);
        if (!ls_item.mounted) {
            if (ThemeUtil.isLightThemeUsed(mActivity)) {
                sb.setSpan(new ForegroundColorSpan(Color.RED), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.setSpan(new ForegroundColorSpan(Color.RED), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        text_view.setText(sb);
        if (position==mSelectedPosition) text_view.setChecked(true);
        else text_view.setChecked(false);

        text_view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                text_view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                setMultilineEllipsize(text_view, 3, TextUtils.TruncateAt.START);
            }
        });
        return convertView;
    }

    public static void setMultilineEllipsize(TextView view, int maxLines, TextUtils.TruncateAt where) {
        if (maxLines >= view.getLineCount()) {
            // ellipsizeする必要無し
            return;
        }
        float avail = 0.0f;
        for (int i = 0; i < maxLines; i++) {
            avail += view.getLayout().getLineMax(i);
        }
        CharSequence ellipsizedText = TextUtils.ellipsize(view.getText(), view.getPaint(), avail, where);
        view.setText(ellipsizedText);
    }


    public static class LocalStorageSelectorItem {
        public String uuid=null;
        public String root_path=null;
        public String description=null;
        public boolean mounted =false;
    }
}

