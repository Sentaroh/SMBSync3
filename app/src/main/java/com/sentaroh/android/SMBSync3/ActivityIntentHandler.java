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
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sentaroh.android.SMBSync3.Constants.NAME_LIST_SEPARATOR;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST;
import static com.sentaroh.android.SMBSync3.Constants.QUERY_SYNC_TASK_INTENT;
import static com.sentaroh.android.SMBSync3.Constants.REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY;
import static com.sentaroh.android.SMBSync3.Constants.REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT;
import static com.sentaroh.android.SMBSync3.Constants.REPLY_SYNC_TASK_INTENT;
import static com.sentaroh.android.SMBSync3.Constants.START_SYNC_EXTRA_PARM_SYNC_TASK;

public class ActivityIntentHandler extends Activity {
    private static final Logger log= LoggerFactory.getLogger(ActivityIntentHandler.class);
    private GlobalParameters mGp=null;
    private CommonUtilities mUtil = null;
    private Context c;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(GlobalParameters.setNewLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_transrucent);
        c=ActivityIntentHandler.this;

        if (mGp == null) {
            mGp =GlobalWorkArea.getGlobalParameter(c);
        }
        if (mUtil == null) mUtil = new CommonUtilities(c, "IntentHandler", mGp, null);

        mGp.loadConfigList(c, mUtil);

        final Intent received_intent=getIntent();
        if (received_intent.getAction()!=null && !received_intent.getAction().equals("")) {
            if (received_intent.getAction().equals(QUERY_SYNC_TASK_INTENT)) {
                querySyncTask(c, received_intent);
            } else {
                String action=received_intent.getAction();
                String task_list=received_intent.getStringExtra(START_SYNC_EXTRA_PARM_SYNC_TASK);
                if (task_list==null) {
                    SyncWorker.startSyncWorkerByAction(c, mGp, mUtil, action, "", "");
                } else {
                    SyncWorker.startSyncWorkerByAction(c, mGp, mUtil, action, "", task_list);
                }
            }
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        mUtil.flushLog();
        CommonUtilities.saveMessageList(c, mGp);
        System.gc();
//		android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void querySyncTask(Context c, Intent in) {
        StringBuilder reply_list = new StringBuilder();
        String sep = "";
        String task_type = QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO;
        if (in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE) != null)
            task_type = in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE);
        mUtil.addDebugMsg(1, "I", "extra=" + in.getExtras() + ", str=" + in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE));
        int reply_count = 0;
        if (mGp.syncTaskList.size() > 0) {
            for (int i = 0; i < mGp.syncTaskList.size(); i++) {
                SyncTaskItem sti = mGp.syncTaskList.get(i);
                if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST.toLowerCase())) {
                    if (sti.isSyncTestMode()) {
                        reply_list.append(sep).append(sti.getSyncTaskName());
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO.toLowerCase())) {
                    if (sti.isSyncTaskAuto()) {
                        reply_list.append(sep).append(sti.getSyncTaskName());
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL.toLowerCase())) {
                    if (!sti.isSyncTaskAuto()) {
                        reply_list.append(sep).append(sti.getSyncTaskName());
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL.toLowerCase())) {
                    reply_list.append(sep).append(sti.getSyncTaskName());
                    sep = NAME_LIST_SEPARATOR;
                    reply_count++;
                }
            }
        }
        Intent reply = new Intent(REPLY_SYNC_TASK_INTENT);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT, reply_count);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY, reply_list.toString());
        mUtil.addDebugMsg(1, "I", "query result, count="+reply_count+", list=["+reply_list+"]");
        c.sendBroadcast(reply);
    }

}
