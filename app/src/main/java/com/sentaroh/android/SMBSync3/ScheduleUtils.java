package com.sentaroh.android.SMBSync3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import com.sentaroh.android.SMBSync3.Log.LogUtil;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_SET_TIMER;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_INTENT_TIMER_EXPIRED;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_LAST_DAY_OF_THE_MONTH;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_SCHEDULE_NAME_KEY;

class ScheduleUtils {
    private static Logger log = LoggerFactory.getLogger(ScheduleUtils.class);
    final static public long getNextScheduleTime(ScheduleListAdapter.ScheduleListItem sp) {
        return getNextScheduleTime(sp, 0L);
    }

    final static private long getNextScheduleTime(ScheduleListAdapter.ScheduleListItem sp, long offset) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis()+offset);
        long result = 0;
        int s_day = 1;
        try {
            s_day=Integer.parseInt(sp.scheduleDay);
        } catch(Exception e) {
            log.error("scheduleDay conversion error, value="+sp.scheduleDay,e);
        }
        int s_hrs =0;
        try {
            s_hrs=Integer.parseInt(sp.scheduleHours);
        } catch(Exception e) {
            log.error("scheduleHours conversion error, value="+sp.scheduleHours,e);
        }
        int s_min =0;
        try {
            s_min=Integer.parseInt(sp.scheduleMinutes);
        } catch(Exception e) {
            log.debug("scheduleMinutes conversion error, value="+sp.scheduleMinutes,e);
        }

        int c_year = cal.get(Calendar.YEAR);
        int c_month = cal.get(Calendar.MONTH);
        int c_day = cal.get(Calendar.DAY_OF_MONTH);
        int c_dw = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int c_hr = cal.get(Calendar.HOUR_OF_DAY);
        int c_mm = cal.get(Calendar.MINUTE);
        if (sp.scheduleType.equals(ScheduleListAdapter.ScheduleListItem.SCHEDULE_TYPE_EVERY_HOURS)) {
            if (c_mm >= s_min) {
                cal.set(c_year, c_month, c_day, c_hr, 0, 0);
                result = cal.getTimeInMillis() + (60 * 1000 * 60) + (60 * 1000 * s_min);
            } else {
                cal.set(c_year, c_month, c_day, c_hr, 0, 0);
                result = cal.getTimeInMillis() + (60 * 1000 * s_min);
            }
        } else if (sp.scheduleType.equals(ScheduleListAdapter.ScheduleListItem.SCHEDULE_TYPE_EVERY_DAY)) {
            cal.clear();
            cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
            if ((c_hr * 100 + c_mm) >= (s_hrs * 100 + s_min)) {
                result = cal.getTimeInMillis() + (60 * 1000 * 60 * 24) + (60 * 1000 * s_min);
            } else {
                result = cal.getTimeInMillis() + (60 * 1000 * s_min);
            }
        } else if (sp.scheduleType.equals(ScheduleListAdapter.ScheduleListItem.SCHEDULE_TYPE_EVERY_MONTH)) {
            int s_day_last_day=0, s_day_temp=0;
            cal.set(Calendar.YEAR, c_year);
            cal.set(Calendar.MONTH, c_month);
            s_day_last_day=cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (s_day==Integer.parseInt(SCHEDULE_LAST_DAY_OF_THE_MONTH)) {
                s_day_temp=s_day_last_day;
            } else {
                if (s_day>s_day_last_day) {
                    return 0;
                } else {
                    s_day_temp=s_day;
                }
            }
            cal.clear();
            cal.set(c_year, c_month, s_day_temp, s_hrs, s_min, 0);
            String curr= StringUtil.convDateTimeTo_YearMonthDayHourMinSec((System.currentTimeMillis()+59999));
            String cald=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(cal.getTimeInMillis());
            if ((System.currentTimeMillis()+59999)>=cal.getTimeInMillis()) {
                cal.add(Calendar.MONTH, 1);
            }
            result = cal.getTimeInMillis();
            log.debug("name="+sp.scheduleName+", c_year="+c_year+", c_month="+c_month+
                    ", s_day="+s_day_temp+", s_hrs="+s_hrs+", s_min="+s_min+", result="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec(result));
        } else if (sp.scheduleType.equals(ScheduleListAdapter.ScheduleListItem.SCHEDULE_TYPE_INTERVAL)) {
            if (sp.scheduleLastExecTime == 0) {
                sp.scheduleLastExecTime = System.currentTimeMillis();
                long nt = sp.scheduleLastExecTime;
                if ((sp.scheduleLastExecTime % (60 * 1000)) > 0)
                    nt = (sp.scheduleLastExecTime / (60 * 1000)) * (60 * 1000);
                result = nt + s_min * (60 * 1000);
            } else {
                long nt = sp.scheduleLastExecTime;
                long m_nt=0l;
                if ((sp.scheduleLastExecTime % (60 * 1000)) > 0){
                    m_nt = (sp.scheduleLastExecTime / (60 * 1000)) * (60 * 1000);
                    result = m_nt + s_min * (60 * 1000);
                } else {
                    result = nt + s_min * (60 * 1000);
                }

                log.debug("name="+sp.scheduleName+", m_nt="+m_nt+", nt="+nt+", s_min="+s_min+", result="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec(result));
            }
        } else if (sp.scheduleType.equals(ScheduleListAdapter.ScheduleListItem.SCHEDULE_TYPE_DAY_OF_THE_WEEK)) {
            boolean[] dwa = new boolean[]{false, false, false, false, false, false, false};
            for (int i = 0; i < sp.scheduleDayOfTheWeek.length(); i++) {
                String dw_s = sp.scheduleDayOfTheWeek.substring(i, i + 1);
                if (dw_s.equals("1")) dwa[i] = true;
            }
            int s_hhmm = Integer.parseInt(sp.scheduleHours) * 100 + s_min;
            int c_hhmm = c_hr * 100 + c_mm;
            int s_dw = 0;
            if (c_hhmm >= s_hhmm) {
                if (c_dw == 6) {
                    c_dw = 0;
                    s_dw = 1;
                    for (int i = c_dw; i < 7; i++) {
                        if (dwa[i]) {
                            break;
                        }
                        s_dw++;
                    }
                } else {
                    c_dw++;
                    s_dw = 1;
                    boolean found = false;
                    for (int i = c_dw; i < 7; i++) {
                        if (dwa[i]) {
                            found = true;
                            break;
                        }
                        s_dw++;
                    }
                    if (!found) {
                        for (int i = 0; i < c_dw; i++) {
                            if (dwa[i]) {
                                found = true;
                                break;
                            }
                            s_dw++;
                        }
                    }
                }
            } else {
                s_dw = 0;
                boolean found = false;
                for (int i = c_dw; i < 7; i++) {
                    if (dwa[i]) {
                        found = true;
                        break;
                    }
                    s_dw++;
                }
                if (!found) {
                    for (int i = 0; i < c_dw; i++) {
                        if (dwa[i]) {
                            found = true;
                            break;
                        }
                        s_dw++;
                    }
                }
            }
            cal.clear();
            cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
            result = cal.getTimeInMillis() + s_dw * (60 * 1000 * 60 * 24) + (60 * 1000 * s_min);
        }
        return result;
    }

    public static ScheduleListAdapter.ScheduleListItem getScheduleItem(ArrayList<ScheduleListAdapter.ScheduleListItem> sl, String name) {
        for (ScheduleListAdapter.ScheduleListItem si : sl) {
            if (si.scheduleName.equals(name)) {
                return si;
            }
        }
        return null;
    }

    public static void sendTimerRequest(Context c, GlobalParameters gp, LogUtil lu, String action) {
        lu.addDebugMsg(1, "I", "Timer request received, request="+action);
        if (action.equals(SCHEDULE_INTENT_SET_TIMER)) {
            setTimer(c, gp, lu);
        } else if (action.equals(SCHEDULE_INTENT_SET_TIMER_IF_NOT_SET)) {
            setTimerIfNotSet(c, gp, lu);
        } else {
            lu.addDebugMsg(1, "E", "Unkonwn timer request, request="+action);
        }

    }

    public static boolean isScheduleExists(ArrayList<ScheduleListAdapter.ScheduleListItem> sl, String name) {
        boolean result = false;
        for (ScheduleListAdapter.ScheduleListItem si : sl) {
            if (si.scheduleName.equals(name)) result = true;
        }
        return result;
    }

    static public SyncTaskItem getSyncTask(GlobalParameters gp, String job_name) {
        for (SyncTaskItem sji : gp.syncTaskList) {
            if (sji.getSyncTaskName().equals(job_name)) {
                return sji;
            }
        }
        return null;
    }

    public static void setScheduleInfo(Context c, GlobalParameters gp, CommonUtilities cu) {
        ArrayList<ScheduleListAdapter.ScheduleListItem> sl = gp.syncScheduleList;
        String sched_list="", sep="", first="";
        long latest_sched_time = -1;
        ArrayList<String> sched_array=new ArrayList<String>();
        boolean schedule_error=false;
        String error_sched_name="", error_task_name="";

        if (gp.settingScheduleSyncEnabled) {
            for (ScheduleListAdapter.ScheduleListItem si : sl) {
                if (si.scheduleEnabled) {
                    long time = getNextScheduleTime(si);
                    String dt=StringUtil.convDateTimeTo_YearMonthDayHourMin(time);
                    String item=dt+","+si.scheduleName;
                    if (si.syncAutoSyncTask) {
                        //NOP
                    } else {
                        if (!si.syncTaskList.equals("")) {
                            if (si.syncTaskList.indexOf(",")>0) {
                                String[] stl=si.syncTaskList.split(",");
                                for(String stn:stl) {
                                    if (getSyncTask(gp,stn)==null) {
                                        schedule_error=true;
                                        error_task_name="\""+stn+"\"";
                                        error_sched_name=si.scheduleName;
                                        break;
                                    }
                                }
                            } else {
                                if (getSyncTask(gp,si.syncTaskList)==null) {
                                    schedule_error=true;
                                    error_task_name="\""+si.syncTaskList+"\"";
                                    error_sched_name=si.scheduleName;
                                    break;
                                }
                            }
                        } else {
                            schedule_error=true;
                            error_task_name="\""+si.syncTaskList+"\"";
                            error_sched_name=si.scheduleName;
                            break;
                        }
                    }
                    sched_array.add(item);
                    if (schedule_error) break;
                }
            }
        }

        Collections.sort(sched_array);

        if (sched_array.size()>0) {
            String[] key=sched_array.get(0).split(",");
            for(String item:sched_array) {
                String[] s_key=item.split(",");
                if (key[0].equals(s_key[0])) {
                    sched_list+=sep+s_key[1];
                    sep=",";
                }
            }
            String sched_info ="";
            if (schedule_error) {
                gp.scheduleErrorText = String.format(c.getString(R.string.msgs_scheduler_info_next_schedule_main_error), error_sched_name, error_task_name);
                gp.scheduleErrorView.setText(gp.scheduleErrorText);
                gp.scheduleErrorView.setTextColor(gp.themeColorList.text_color_warning);
                gp.scheduleErrorView.setVisibility(TextView.VISIBLE);
            } else {
                gp.scheduleErrorText="";
                gp.scheduleErrorView.setVisibility(TextView.GONE);
            }
            sched_info = String.format(c.getString(R.string.msgs_scheduler_info_next_schedule_main_info), key[0], sched_list);
            gp.scheduleInfoText = sched_info;
            gp.scheduleInfoView.setText(gp.scheduleInfoText);
        } else {
            gp.scheduleInfoText = c.getString(R.string.msgs_scheduler_info_schedule_disabled);
            gp.scheduleInfoView.setText(gp.scheduleInfoText);
        }
    }

    public static String buildScheduleNextInfo(Context c, ScheduleListAdapter.ScheduleListItem sp) {
        long nst = -1;
        nst = getNextScheduleTime(sp);
        String sched_time = "", result = "";
        if (nst != -1) {
            sched_time = StringUtil.convDateTimeTo_YearMonthDayHourMin(nst);
            if (sp.scheduleEnabled) {
                result = c.getString(R.string.msgs_scheduler_info_schedule_enabled) + ", " + String.format(c.getString(R.string.msgs_scheduler_info_next_schedule_time), sched_time);
            } else {
                result = c.getString(R.string.msgs_scheduler_info_schedule_disabled);
            }
        } else {
            result = c.getString(R.string.msgs_scheduler_info_schedule_disabled);
        }
        return result;
    }

    static public void setTimerIfNotSet(Context c, GlobalParameters gp, LogUtil lu) {
        if (!isTimerScheduled(c)) {
            setTimer(c, gp, lu);
        } else {
            lu.addDebugMsg(1, "I", "setTimerIfNotSet request ignored.");
        }
    }

    static public void setTimer(Context c, GlobalParameters gp, LogUtil lu) {
        lu.addDebugMsg(1, "I", "setTimer entered, settingScheduleSyncEnabled="+gp.settingScheduleSyncEnabled);
        cancelTimer(c, lu);
        boolean scheduleEnabled = false;
        boolean schedule_done=false;
        for (ScheduleListAdapter.ScheduleListItem si : gp.syncScheduleList) if (si.scheduleEnabled) scheduleEnabled = true;
        if (scheduleEnabled && gp.settingScheduleSyncEnabled) {
            ArrayList<ScheduleListAdapter.ScheduleListItem> begin_sched_list = new ArrayList<ScheduleListAdapter.ScheduleListItem>();
            ArrayList<String> sched_list=new ArrayList<String>();
            for (ScheduleListAdapter.ScheduleListItem si : gp.syncScheduleList) {
                if (si.scheduleEnabled) {
                    long time = getNextScheduleTime(si);
                    String item= StringUtil.convDateTimeTo_YearMonthDayHourMin(time)+","+si.scheduleName;
                    sched_list.add(item);
                    lu.addDebugMsg(1,"I", "setTimer Schedule item added. item="+item);
                }
            }
            if (sched_list.size()>0) {
                Collections.sort(sched_list);
                String sched_time="";

                for(String item:sched_list) {
                    String[]sa=item.split(",");
                    if (sched_time.equals("")) {
                        sched_time=sa[0];
                        ScheduleListAdapter.ScheduleListItem si=getScheduleItem(gp.syncScheduleList, sa[1]);
                        if (si!=null) {
                            begin_sched_list.add(si);
                            lu.addDebugMsg(1,"I", "setTimer NextSchedule added. Name="+si.scheduleName+", "+sa[0]);
                        } else {
                            lu.addDebugMsg(1,"E", "setTimer Schedule can not be found. Name="+sa[1]);
                        }
                    } else if (sched_time.equals(sa[0])) {
                        ScheduleListAdapter.ScheduleListItem si=getScheduleItem(gp.syncScheduleList, sa[1]);
                        if (si!=null) {
                            begin_sched_list.add(si);
                            lu.addDebugMsg(1,"I", "setTimer NextSchedule added. Name="+si.scheduleName+", "+sa[0]);
                        } else {
                            lu.addDebugMsg(1,"E", "setTimer Schedule can not be found. Name="+sa[1]);
                        }
                    }
                }
            }
            if (begin_sched_list.size() > 0) {
                String sched_names = "", sep="";
                for (ScheduleListAdapter.ScheduleListItem si : begin_sched_list) {
                    sched_names += sep + si.scheduleName;
                    sep=",";
                }

                long time = getNextScheduleTime(begin_sched_list.get(0));
                lu.addDebugMsg(1, "I", "setTimer result=" + StringUtil.convDateTimeTo_YearMonthDayHourMinSec(time) + ", name=(" + sched_names+")");
                Intent in = new Intent();
                in.setAction(SCHEDULE_INTENT_TIMER_EXPIRED);
                in.putExtra(SCHEDULE_SCHEDULE_NAME_KEY, sched_names);
                in.setClass(c, SyncReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(c, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                try {
//                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                    schedule_done=true;
                } catch(Exception e) {
                    String stm= MiscUtil.getStackTraceString(e);
                    lu.addDebugMsg(1, "I", "setTimer failed. error="+e.getMessage()+"\n"+stm);
                }
            }
        }
        if (!schedule_done) lu.addDebugMsg(1, "I", "setTimer timer is not set");
    }

    private static boolean isTimerScheduled(Context c) {
        Intent iw = new Intent();
        iw.setAction(SCHEDULE_INTENT_TIMER_EXPIRED);
        iw.setClass(c, SyncReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, iw, PendingIntent.FLAG_NO_CREATE);
        if (pi == null) {
            return false;
        } else {
            return true;
        }
    }

    static private void cancelTimer(Context c, LogUtil lu) {
        lu.addDebugMsg(1, "I", "cancelTimer entered");
        Intent in = new Intent();
        in.setClass(c, SyncReceiver.class);
        in.setAction(SCHEDULE_INTENT_TIMER_EXPIRED);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

}
