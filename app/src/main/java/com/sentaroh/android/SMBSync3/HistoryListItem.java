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

import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_ACTIVITY;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_EXTERNAL;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SCHEDULE;
import static com.sentaroh.android.SMBSync3.Constants.SYNC_REQUEST_SHORTCUT;

public class HistoryListItem {
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
