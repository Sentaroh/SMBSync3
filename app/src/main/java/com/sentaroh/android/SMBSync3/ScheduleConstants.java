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


import static com.sentaroh.android.SMBSync3.Constants.APPLICATION_ID;
import static com.sentaroh.android.SMBSync3.Constants.APPLICATION_TAG;

public class ScheduleConstants {
//    public static final String SCHEDULER_SCHEDULE_TYPE_KEY = "scheduler_schedule_type_key";
//
//    public static final String SCHEDULER_SYNC_PROFILE_KEY = "scheduler_sync_profile_key";
//
    public static final String SCHEDULE_SCHEDULE_NAME_KEY = "scheduler_schedule_name_key";
    public static final String SCHEDULE_ENABLED_KEY = "scheduler_enabled_key";

    public static final String SCHEDULE_INTENT_TIMER_EXPIRED = APPLICATION_ID + ".ACTION_TIMER_EXPIRED";
    public static final String SCHEDULE_INTENT_SET_TIMER = APPLICATION_ID + ".ACTION_SET_TIMER";
    public static final String SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET = APPLICATION_ID + ".ACTION_SET_TIMER_IF_NOT_SET";


}
