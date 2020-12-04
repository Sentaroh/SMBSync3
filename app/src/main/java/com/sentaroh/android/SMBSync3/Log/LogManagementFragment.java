/*
The MIT License (MIT)
Copyright (c) 2018 Sentaroh

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
package com.sentaroh.android.SMBSync3.Log;

import android.content.Context;
import android.os.Bundle;

import com.sentaroh.android.SMBSync3.R;
import static com.sentaroh.android.SMBSync3.Constants.APPLICATION_TAG;
import com.sentaroh.android.Utilities3.LogUtil.CommonLogManagementFragment;

public class LogManagementFragment extends CommonLogManagementFragment {
    public static LogManagementFragment newInstance(Context c, boolean retainInstance, String title) {
        LogManagementFragment frag = new LogManagementFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("retainInstance", retainInstance);
        bundle.putString("title", title);
        bundle.putString("msgtext", c.getString(R.string.msgs_log_management_send_log_file_warning));
        bundle.putString("enableMsg", c.getString(R.string.msgs_log_management_enable_log_file_warning));
        bundle.putString("subject", APPLICATION_TAG+" log");
        frag.setArguments(bundle);
        return frag;
    }

}