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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

class SyncRequestItem implements Serializable {

    public String request_id = "NONAME";
    public String requestor = "";
    public String requestor_display = "";
    public String schedule_name = "";

    public final static int SYNC_RESULT_STATUS_SUCCESS = 0;
    public final static int SYNC_RESULT_STATUS_CANCEL = 1;
    public final static int SYNC_RESULT_STATUS_ERROR = 2;
    public final static int SYNC_RESULT_STATUS_WARNING = 3;
    public final static int SYNC_RESULT_STATUS_SKIP = 4;
    public int result=SYNC_RESULT_STATUS_SUCCESS;

    public boolean wifi_off_after_sync_ended = false;
    public boolean wifi_on_before_sync_start = false;
    public int start_delay_time_after_wifi_on = 0;

    public final static String OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE="0";
    public final static String OVERRIDE_SYNC_OPTION_ENABLED="1";
    public final static String OVERRIDE_SYNC_OPTION_DISABLED="2";

    public String overrideSyncOptionCharge=OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
    //    public String overrideSyncOptionWifiStatus=OVERRIDE_SYNC_OPTION_DO_NOT_CHANGE;
//    public ArrayList<String> overrideSyncOptionWifiApList=new ArrayList<String>();
//    public ArrayList<String> overrideSyncOptionWifiIpAddressList=new ArrayList<String>();
    public ArrayBlockingQueue<SyncTaskItem> sync_task_list = new ArrayBlockingQueue<SyncTaskItem>(1000);

    static public byte[] serialize(SyncRequestItem input) {
        byte[] out=null;
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream(1024*256);
            ObjectOutputStream oo=new ObjectOutputStream(baos);
            oo.writeObject(input);
            oo.flush();
            oo.close();
            out=baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static public SyncRequestItem deserialize(byte[]in_byte) {
        SyncRequestItem out=null;
        try {
            ByteArrayInputStream bais=new ByteArrayInputStream(in_byte);
            ObjectInputStream oi=new ObjectInputStream(bais);
            out= (SyncRequestItem) oi.readObject();
            oi.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }
}
