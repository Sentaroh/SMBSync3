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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.text.Spannable;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.fragment.app.FragmentManager;

import com.sentaroh.android.JcifsFile2.JcifsUtil;
import com.sentaroh.android.SMBSync3.Log.LogUtil;


import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ShellCommandUtil;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.sentaroh.android.SMBSync3.Constants.DEFAULT_PREFS_FILENAME;
import static com.sentaroh.android.SMBSync3.Constants.GENERAL_IO_BUFFER_SIZE;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public final class CommonUtilities {
    private static Logger log= LoggerFactory.getLogger(CommonUtilities.class);
    private Context mContext = null;
    private LogUtil mLog = null;
    private GlobalParameters mGp = null;
    private String mLogIdent = "";

    private FragmentManager mFragMgr=null;

    public CommonUtilities(Context c, String li, GlobalParameters gp, FragmentManager fm) {
        mContext = c;// ContextはApplicationContext
        mLog = new LogUtil(c, li);
        mLogIdent = li;
        mGp = gp;
        mFragMgr=fm;
    }

    public int getLogLevel() {
        return mLog.getLogLevel();
    }

    public LogUtil getLogUtil() {
        return mLog;
    }

    static public void setViewEnabled(Activity a, View v, boolean enabled) {
        GlobalParameters gp=GlobalWorkArea.getGlobalParameter(a);
        CommonDialog.setViewEnabled(gp.themeColorList.theme_is_light, v, enabled);
    }

    static public void setViewEnabled(Context a, View v, boolean enabled) {
        GlobalParameters gp=GlobalWorkArea.getGlobalParameter(a);
        CommonDialog.setViewEnabled(gp.themeColorList.theme_is_light, v, enabled);
    }

    final public SharedPreferences getPrefMgr() {
        return getPrefMgr(mContext);
    }

    public static void setSpinnerBackground(Context c, Spinner spinner, boolean theme_is_light) {
        if (theme_is_light) spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background_light));
        else spinner.setBackground(c.getDrawable(R.drawable.spinner_color_background));
    }

    final static public SharedPreferences getPrefMgr(Context c) {
        return c.getSharedPreferences(DEFAULT_PREFS_FILENAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };
    public void showCommonDialogInfo(final boolean negative, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_INFO, title, msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };
    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };
    public void showCommonDialogError(final boolean negative, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_ERROR, title, msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };
    public void showCommonDialogDanger(final boolean negative, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    static public void showCommonDialog(FragmentManager fm, final boolean negative, String type, String title, String msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(fm, cdf, ntfy);
    };

    public void showCommonDialog(final boolean negative, String type, String title, Spannable msgtext, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, "");
        cdf.setMessageText(msgtext);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, int text_color, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialogWordWrap(final boolean negative, String type, String title, String msgtext, int text_color, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.setWordWrapEanbled(true);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, String ok_text, String cancel_text, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, String ok_text, String cancel_text, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialogDanger(boolean negative, String title, String msgtext, String ok_text, String cancel_text, NotifyEvent ntfy) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,ntfy);
    };

    public void showCommonDialog(Context c, boolean negative, String type, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };
    public void showCommonDialogInfo(Context c, boolean negative, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_INFO, title, msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };
    public void showCommonDialogWarn(Context c, boolean negative, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };
    public void showCommonDialogError(Context c, boolean negative, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_ERROR, title, msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };
    public void showCommonDialogDanger(Context c, boolean negative, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    static public void showCommonDialog(Context c, FragmentManager fm, final boolean negative, String type, String title, String msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(c, fm, cdf,cbl);
    };

    public void showCommonDialog(Context c, boolean negative, String type, String title, Spannable msgtext, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, "");
        cdf.setMessageText(msgtext);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    public void showCommonDialog(Context c, boolean negative, String type, String title, String msgtext, int text_color, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    public void showCommonDialogWordWrap(Context c, boolean negative, String type, String title, String msgtext, int text_color, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.setWordWrapEanbled(true);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    public void showCommonDialog(Context c, boolean negative, String type, String title, String msgtext, String ok_text, String cancel_text, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    public void showCommonDialogWarn(Context c, boolean negative, String title, String msgtext, String ok_text, String cancel_text, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

    public void showCommonDialogDanger(Context c, boolean negative, String title, String msgtext, String ok_text, String cancel_text, CallBackListener cbl) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(c, mFragMgr,cdf,cbl);
    };

//    public static String convertMakdownToHtml(Context c, String mark_down_fp) {
//        try {
//            InputStream is = c.getAssets().open(mark_down_fp);
//            BufferedReader br = new BufferedReader(new InputStreamReader(is), 1024*1024);
//            String mark_down_text="", line="", sep="";
//            while ((line = br.readLine()) != null) {
//                mark_down_text+=sep+line;
//                sep="\n";
//            }
//            br.close();
//            MarkdownProcessor processor = new MarkdownProcessor();
//            String html = processor.markdown(mark_down_text);
//            return html;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return "";
//    }

    public String getStringWithLangCode(Activity c, String lang_code, int res_id) {
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.setLocale(new Locale(lang_code));
        String result = c.createConfigurationContext(config).getText(res_id).toString();
        return result;
    }

    public String getStringWithLangCode(Activity c, String lang_code, int res_id, Object... value) {
        String text = getStringWithLangCode(c, lang_code, res_id);
        String result=text;
        if (value!=null && value.length>0) result=String.format(text, value);
        return result;
    }

    static public String getRootFilePath(String fp) {
        String reform_fp=StringUtil.removeRedundantDirectorySeparator(fp);
        if (reform_fp.startsWith("/storage/emulated/0")) return "/storage/emulated/0";
        else {
            String[] fp_parts=reform_fp.startsWith("/")?reform_fp.substring(1).split("/"):reform_fp.split("/");
            String rt_fp="/"+fp_parts[0]+"/"+fp_parts[1];
            return rt_fp;
        }
    }

    public static boolean isAllFileAccessAvailable() {
        return SafFile3.isAllFileAccessAvailable();
    }

    final public void setLogId(String li) {
        mLog.setLogId(li);
    }

    final static public String getExecutedMethodName() {
        String name = Thread.currentThread().getStackTrace()[3].getMethodName();
        return name;
    }

    final public void resetLogReceiver() {
        mLog.resetLogReceiver();
    }

    final public void flushLog() {
        mLog.flushLog();
    }

    final public void rotateLogFile() {
        mLog.rotateLogFile();
    }

    static public ArrayList<String> listSystemInfo(Context c, GlobalParameters gp) {

        ArrayList<String> out=SystemInfo.listSystemInfo(c, gp.safMgr);

        try {
            ContentResolver contentResolver = c.getContentResolver();
            int policy = Settings.System.getInt(contentResolver, Settings.Global.WIFI_SLEEP_POLICY);
            switch (policy) {
                case Settings.Global.WIFI_SLEEP_POLICY_DEFAULT:
                    // スリープ中のWiFi接続を維持しない
                    out.add("WIFI_SLEEP_POLICY_DEFAULT");
                    break;
                case Settings.Global.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED:
                    // スリープ中のWiFi接続を電源接続時にのみ維持する
                    out.add("WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED");
                    break;
                case Settings.Global.WIFI_SLEEP_POLICY_NEVER:
                    // スリープ中のWiFi接続を常に維持する
                    out.add("WIFI_SLEEP_POLICY_NEVER");
                    break;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        out.add("");
        try {
            out.add("Network information:");

            WifiManager wm=(WifiManager)c.getSystemService(Context.WIFI_SERVICE);
            try {
                out.add("   WiFi="+wm.isWifiEnabled());
            } catch(Exception e) {
                out.add("   WiFi status obtain error, error="+e.getMessage());
            }

            ConnectivityManager cm =(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork!=null) {
                String network=activeNetwork.getExtraInfo();

                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

                out.add("   Network="+network+", isConnected="+isConnected+", isWiFi="+isWiFi);
            } else {
                out.add("   No active network");
            }
            out.add("   IF Addr="+getLocalIpAddress());
        } catch (Exception ex) {
            out.add("Network address error. error="+ex.getMessage());
        }

        out.add("");
        out.add("Settings options:");
//        out.add("  Error option="+gp.settingErrorOption);
        out.add("  WiFi lock option="+gp.settingWifiLockRequired);
        out.add("  Write sync result log="+gp.settingWriteSyncResultLog);
        out.add("  No compress file type="+gp.settingNoCompressFileType);
        out.add("  Prevent sync start delay="+gp.settingPreventSyncStartDelay);
        out.add("  Management file directory="+gp.settingAppManagemsntDirectoryName);

        out.add("");
//        out.add("  Suppress AppSpecific directory warning="+gp.settingSupressAppSpecifiDirWarning);
        out.add("  Notification message when sync ended="+gp.settingNotificationMessageWhenSyncEnded);
        out.add("  Ringtone when sync ended="+gp.settingNotificationSoundWhenSyncEnded);
        out.add("  Notification sound volume="+gp.settingNotificationVolume);
        out.add("  Vibrate when sync ended="+gp.settingNotificationVibrateWhenSyncEnded);
        out.add("  Fix device oprientation portrait="+gp.settingFixDeviceOrientationToPortrait);
        out.add("  Screen theme="+gp.settingScreenTheme);
//        out.add("  Screen on if screen on at start of the sync="+gp.settingScreenOnIfScreenOnAtStartOfSync);

        out.add("");
        out.add("  Security use app startup="+gp.settingSecurityApplicationPasswordUseAppStartup);
        out.add("  Security use edit task="+gp.settingSecurityApplicationPasswordUseEditTask);
        out.add("  Security use export="+gp.settingSecurityApplicationPasswordUseExport);
        out.add("  Security re-init account and password="+gp.settingSecurityReinitSmbAccountPasswordValue);

        out.add("");
        out.add("  Sync message use standard text view="+gp.settingSyncMessageUseStandardTextView);
        out.add("  Exit clean="+gp.settingExitClean);

        return out;
    }

    final public static boolean isLocationServiceEnabled(Context c, GlobalParameters mGp) {
        if (Build.VERSION.SDK_INT>=27) {
            LocationManager lm= (LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT==27) {
                boolean gps=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean nw=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                return gps|nw;
            } else {
                return lm.isLocationEnabled();
            }
        }
        return false;
    }

    final public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    }

    final public void deleteLogFile() {
        mLog.deleteLogFile();
    }

    public String buildPrintMsg(String cat, String... msg) {
        return mLog.buildPrintLogMsg(cat, msg);
    }


    final static private String LIST_ITEM_SEPARATOR ="\u0000";
    final static private String LIST_ITEM_DUMMY_DATA ="\u0001";
    final static private String LIST_ITEM_ENCODE_CR_CHARACTER ="\u0003";
    synchronized static public void saveMessageList(Context c, GlobalParameters gp) {
//        Thread.dumpStack();
        if (gp.syncMessageList == null || (gp.syncMessageList!=null && gp.syncMessageList.size()==0)) return;
        long b_time= System.currentTimeMillis();
        if (gp.syncMessageListChanged) {
            try {
                SafFile3 df =new SafFile3(c, gp.settingAppManagemsntDirectoryName);
                if (!df.exists()) df.mkdirs();
                SafFile3 mf =new SafFile3(c, gp.settingAppManagemsntDirectoryName + "/.messages");
                mf.deleteIfExists();
                if (!mf.exists()) mf.createNewFile();
                OutputStream fos=mf.getOutputStream();
                BufferedOutputStream bos=new BufferedOutputStream(fos, GENERAL_IO_BUFFER_SIZE);
                PrintWriter pw=new PrintWriter(bos);
                StringBuffer sb=new StringBuffer(1024*5);
                synchronized (gp.syncMessageList) {
                    for (MessageListAdapter.MessageListItem smi:gp.syncMessageList) {
                        sb.setLength(0);
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getCategory()).append(LIST_ITEM_SEPARATOR); //msgCat
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getDate()).append(LIST_ITEM_SEPARATOR); //msgDate
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getTime()).append(LIST_ITEM_SEPARATOR); //msgTime
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getTitle()).append(LIST_ITEM_SEPARATOR); //msgTitle
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getMessage().replaceAll("\n", LIST_ITEM_ENCODE_CR_CHARACTER)).append(LIST_ITEM_SEPARATOR); //msgBody
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getPath()).append(LIST_ITEM_SEPARATOR); //msgPath
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getType()).append(LIST_ITEM_SEPARATOR); //msgType
                        pw.println(sb.toString());
                    }
                    gp.syncMessageListChanged =false;
                }
                pw.flush();
                pw.close();
            } catch(Exception e) {
                log.error(CommonUtilities.getExecutedMethodName()+" error.", e);
            }
        }
    }

    synchronized static public ArrayList<MessageListAdapter.MessageListItem> loadMessageList(Context c, GlobalParameters gp) {
        long b_time= System.currentTimeMillis();
        ArrayList<MessageListAdapter.MessageListItem> result=new ArrayList<MessageListAdapter.MessageListItem>(GlobalParameters.MESSAGE_LIST_INITIAL_VALUE);
        try {
            SafFile3 mf =new SafFile3(c, gp.settingAppManagemsntDirectoryName + "/.messages");
            if (mf!=null && mf.exists()) {
                InputStreamReader isr = new InputStreamReader(mf.getInputStream(), "UTF-8");
                BufferedReader bir=new BufferedReader(isr, GENERAL_IO_BUFFER_SIZE);
                String line=null;
                while((line=bir.readLine())!=null) {
                    String[] msg_array=line.split(LIST_ITEM_SEPARATOR);
                    if (msg_array.length>=7) {
                        MessageListAdapter.MessageListItem smi = new MessageListAdapter.MessageListItem(
                                msg_array[0].replace(LIST_ITEM_DUMMY_DATA, ""),//Cat
                                msg_array[1].replace(LIST_ITEM_DUMMY_DATA, ""), //msgDate
                                msg_array[2].replace(LIST_ITEM_DUMMY_DATA, ""), //msgTime
                                msg_array[3].replace(LIST_ITEM_DUMMY_DATA, ""), //msgTitle
                                msg_array[4].replace(LIST_ITEM_DUMMY_DATA, "").replaceAll(LIST_ITEM_ENCODE_CR_CHARACTER, "\n"), //msgBody
                                msg_array[5].replace(LIST_ITEM_DUMMY_DATA, ""), //msgPath
                                msg_array[6].replace(LIST_ITEM_DUMMY_DATA, "")); //msgType
                        result.add(smi);
                    }
                }
                bir.close();
            }
        } catch(Exception e) {
            log.error(CommonUtilities.getExecutedMethodName()+" error.", e);
        }
        return result;
    }

    private final int MAX_MSG_COUNT = 5000;

    final public void addLogMsg(boolean ui_thread, boolean has_result_type, boolean has_path, boolean has_title,
                                String cat, String title, String result_type, String path, String... msg) {
//		final SyncMessageItem mli=new SyncMessageItem(cat, "","", title, mLog.buildLogCatMsg("", cat, msg), path, type);
        String finalMsg = "";
        StringBuilder log_msg = new StringBuilder(512);
        for (int i = 0; i < msg.length; i++) log_msg.append(msg[i]);
        if (!log_msg.toString().equals("")) finalMsg = log_msg.toString();
        if (!title.equals("")) mLog.addLogMsg(cat, title.concat(": ").concat(finalMsg).concat(" ").concat(path).concat(result_type));
        else mLog.addLogMsg(cat, finalMsg.concat(" ").concat(path).concat(result_type));

        String[] dt = StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(System.currentTimeMillis()).split(" ");

        //addDebugMsg(2, "I", "cat=" + cat," dt[0]=" + dt[0] + " dt[1]=" + dt[1] + " title=" + title + " finalMsg=" + finalMsg + " path=" + path + " type=" + type);
        final MessageListAdapter.MessageListItem mli = new MessageListAdapter.MessageListItem(cat, dt[0], dt[1], title, finalMsg, path, result_type);
        if (ui_thread) {
            putMsgListArray(mli);
        } else {
            mGp.uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    putMsgListArray(mli);
                }
            });
        }
    }

    final public void addLogMsg(String cat, String task, String... msg) {
        addLogMsg(false, false, false, false, cat, task, "", "", msg);
    }

    final public void addLogMsgFromUI(String cat, String task, String... msg) {
        addLogMsg(true, false, false, false, cat, task, "", "", msg);
    }

    private void putMsgListArray(MessageListAdapter.MessageListItem mli) {
        synchronized (mGp.syncMessageList) {
            if (mGp.syncMessageList.size() > (MAX_MSG_COUNT + 200)) {
                for (int i = 0; i < 200; i++) mGp.syncMessageList.remove(0);
            }
            mGp.syncMessageList.add(mli);
            mGp.syncMessageListChanged =true;
            if (mGp.syncMessageListAdapter != null) {
                mGp.syncMessageListAdapter.notifyDataSetChanged();
                if (!mGp.freezeMessageViewScroll) {
                    mGp.syncMessageView.setSelection(mGp.syncMessageList.size()-1);
                }
            }
        }
    }

    final public void addDebugMsg(int lvl, String cat, String... msg) {
        mLog.addDebugMsg(lvl, cat, msg);
    }

    final public boolean isLogFileExists() {
        boolean result = false;
        result = mLog.isLogFileExists();
        if (mLog.getLogLevel() >= 3) addDebugMsg(3, "I", "Log file exists=" + result);
        return result;
    }

    final public String getLogFilePath() {
        return mLog.getLogFilePath();
    }

    public boolean isDebuggable() {
        PackageManager manager = mContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            return true;
        return false;
    }

    public boolean isWifiActive() {
        boolean ret = false;
        WifiManager mWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifi.isWifiEnabled()) ret = true;
        addDebugMsg(2, "I", "isWifiActive WifiEnabled=" + ret);
        return ret;
    }

    public static String decryptUserData(Context c, EncryptUtilV3.CipherParms cp_int, String enc_str) {
        String dec_str = null;
        try {
            byte[] dec_array = Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
            dec_str = EncryptUtilV3.decrypt(dec_array, cp_int);
        } catch (Exception e) {
            log.error(CommonUtilities.getExecutedMethodName()+" error.", e);
            e.printStackTrace();
        }
        return dec_str;
    }

    public static String encryptUserData(Context c, EncryptUtilV3.CipherParms cp_int, String user_data) {
        try {
            byte[] enc_byte = EncryptUtilV3.encrypt(user_data, cp_int);
            String enc_str = Base64Compat.encodeToString(enc_byte, Base64Compat.NO_WRAP);
            return enc_str;
        } catch (Exception e) {
            String stm = MiscUtil.getStackTraceString(e);
            log.error(CommonUtilities.getExecutedMethodName()+" error=" + e.getMessage() + "\n" + stm);
            e.printStackTrace();
            return "";
        }
    }

    public static boolean canSmbHostConnectable(String addr) {
        boolean result = false;
        if (JcifsUtil.canIpAddressAndPortConnectable(addr, 139, 3500) ||
                JcifsUtil.canIpAddressAndPortConnectable(addr, 445, 3500)) result = true;
        return result;
    }

    public static boolean canSmbHostConnectable(String addr, int port) {
        boolean result = false;
        result = JcifsUtil.canIpAddressAndPortConnectable(addr, port, 3500);
        return result;
    }

    static public boolean isSmbHost(CommonUtilities cu, String address, String scan_port) {
        boolean smbhost = false;
        if (scan_port.equals("")) {
            if (!JcifsUtil.canIpAddressAndPortConnectable(address, 445, 3500)) {
                smbhost = JcifsUtil.canIpAddressAndPortConnectable(address, 139, 3500);
            } else smbhost = true;
        } else {
            smbhost = JcifsUtil.canIpAddressAndPortConnectable(address, Integer.parseInt(scan_port), 3500);
        }
        cu.addDebugMsg(2, "I", "isIpAddrSmbHost Address=" + address + ", port=" + scan_port + ", smbhost=" + smbhost);
        return smbhost;
    }

    static public String getSmbHostName(CommonUtilities cu, String smb_level, String address) {
        String srv_name = JcifsUtil.getSmbHostNameByAddress(smb_level, address);
        cu.addDebugMsg(1, "I", "getSmbHostName Address=" + address + ", name=" + srv_name);
        return srv_name;
    }

    public static String getLocalIpAddress() {
        String result = "";
        result=getIfIpAddress("wlan0");
        if (result.equals("")) result = "192.168.0.1";
        return result;
    }

    public static String getIfIpAddress(String if_name) {
        String result = "";
        boolean exit = false;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress() && (inetAddress instanceof Inet4Address)) {
                        result = inetAddress.getHostAddress();
                        if (intf.getName().toLowerCase().equals(if_name)) {
                            exit = true;
                            break;
                        }
                    }
                }
                if (exit) break;
            }
        } catch (SocketException ex) {
            log.error("getIfIpAddress error, if="+if_name, ex);
        }
        return result;
    }

    public static boolean isIpAddressV6(String addr) {
        boolean result=false;
        InetAddress ia=getInetAddress(addr);
        if (ia!=null) result=ia instanceof Inet6Address;
        return result;
    }

    public static InetAddress getInetAddress(String addr) {
        InetAddress result=null;
        try {
            result= InetAddress.getByName(addr);
        } catch (Exception e) {
        }
        return result;
    }

    public static boolean isIpAddressV4(String addr) {
        boolean result=false;
        InetAddress ia=getInetAddress(addr);
        if (ia!=null) result=ia instanceof Inet4Address;
        return result;
    }

    public static String getIfIpAddress(CommonUtilities cu) {
        String result = "";
        boolean exit = false;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress() && (inetAddress instanceof Inet4Address)) {
                        result = inetAddress.getHostAddress();
                        exit = true;
                        break;
                    }
                }
                if (exit) break;
            }
            if (!exit) {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() &&(inetAddress instanceof Inet4Address)) {
                            result = inetAddress.getHostAddress();
                            exit = true;
                            break;
                        }
                    }
                    if (exit) break;
                }
            }
        } catch (SocketException ex) {
            cu.addDebugMsg(1,"I","getIfIpAddress() error="+ ex.toString());
        }
        return result;
    }

    public static String addScopeidToIpv6Address(String addr) {
        if (addr==null) return null;
        InetAddress ia=getInetAddress(addr);
        if (ia==null) return null;
        if ((ia instanceof Inet6Address)) {
            if (ia.isLinkLocalAddress()) {
                if (addr.contains("%")) return addr;
                else return addr+"%wlan0";
            }
        }
        return addr;
    }

    public static String resolveHostName(GlobalParameters gp, CommonUtilities cu, String smb_level, String hn) {
        String resolve_addr = JcifsUtil.getSmbHostIpAddressByHostName(smb_level, hn);
        if (resolve_addr != null) {//list dns name resolve
            try {
                InetAddress[] addr_list = InetAddress.getAllByName(hn);
                for (InetAddress item : addr_list) {
                    cu.addDebugMsg(1, "I", "resolveHostName DNS Query Name=" + hn + ", IP addr=" + item.getHostAddress());
                }
            } catch (Exception e) {
                cu.addDebugMsg(1, "I", "resolveHostName DNS Query failed. error="+e.getMessage());
            }
        }
//        resolve_addr="fe80::abd:43ff:fef6:482a";//for IPV6 Test
        cu.addDebugMsg(1, "I", "resolveHostName Name=" + hn + ", IP addr=" + resolve_addr+", smb="+smb_level);
        return resolve_addr;
    }

    static public boolean isIpV4PrivateAddress(String ip_v4_addr) {
        if (ip_v4_addr.startsWith("10.")) return true;
        else if (ip_v4_addr.startsWith("192.168.")) return true;
        else if (ip_v4_addr.startsWith("172.16.")) return true;
        else if (ip_v4_addr.startsWith("172.17.")) return true;
        else if (ip_v4_addr.startsWith("172.18.")) return true;
        else if (ip_v4_addr.startsWith("172.19.")) return true;
        else if (ip_v4_addr.startsWith("172.20.")) return true;
        else if (ip_v4_addr.startsWith("172.21.")) return true;
        else if (ip_v4_addr.startsWith("172.22.")) return true;
        else if (ip_v4_addr.startsWith("172.23.")) return true;
        else if (ip_v4_addr.startsWith("172.24.")) return true;
        else if (ip_v4_addr.startsWith("172.25.")) return true;
        else if (ip_v4_addr.startsWith("172.26.")) return true;
        else if (ip_v4_addr.startsWith("172.27.")) return true;
        else if (ip_v4_addr.startsWith("172.28.")) return true;
        else if (ip_v4_addr.startsWith("172.29.")) return true;
        else if (ip_v4_addr.startsWith("172.30.")) return true;
        else if (ip_v4_addr.startsWith("172.31.")) return true;
        return false;
    }

    static public boolean isPrivateAddress(String if_addr) {
        InetAddress ia=null;
        try {
            ia=InetAddress.getByName(if_addr);
            if (ia.isSiteLocalAddress()) return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            log.error("isPrivateAddress error, addr="+if_addr, e);
        }
        return false;
    }

    public static String getIfHwAddress(String if_name) {
        String result = "";
        boolean exit = false;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.isSiteLocalAddress() && (inetAddress instanceof Inet4Address)) {
                        if (intf.getName().equals(if_name)) {
                            for(int i=0;i<intf.getHardwareAddress().length;i++) result += String.format("%2h",intf.getHardwareAddress()[i]).replaceAll(" ","0");
                            exit = true;
                            break;
                        }
                    }
                }
                if (exit) break;
            }
        } catch (SocketException ex) {
            log.error("getIfHwAddress error, if="+if_name, ex);
        }
        return result;
    }

    private void sendMagicPacket(final String target_mac, final String if_network) {
//                sendMagicPacket("08:bd:43:f6:48:2a", if_ip);
        Thread th=new Thread(){
            @Override
            public void run() {
                byte[] broadcastMacAddress=new byte[6];
                for(int i=0;i<6;i++) broadcastMacAddress[i]=(byte)0xff;
                InetAddress broadcastIpAddress = null;
                try {
                    int j=if_network.lastIndexOf(".");
                    String if_ba=if_network.substring(0,if_network.lastIndexOf("."))+".255";
                    broadcastIpAddress = InetAddress.getByName(if_ba);//.getByAddress(new byte[]{-1,-1,-1,-1});

                    byte[] targetMacAddress=new byte[6];
                    String[] m_array=target_mac.split(":");
                    for(int i=0;i<6;i++) {
                        targetMacAddress[i]= Integer.decode("0x"+m_array[i]).byteValue();
                    }

                    byte[] magicPacket=new byte[102];
                    System.arraycopy(broadcastMacAddress,0, magicPacket,0,6);
                    for (int i=0;i<16;i++) {
                        System.arraycopy(targetMacAddress,0, magicPacket,(i*6)+6, 6);
                    }

// マジックパケットを任意のポートにブロードキャストするための UDPデータグラムパケット
                    DatagramPacket packet = new DatagramPacket(magicPacket, magicPacket.length, broadcastIpAddress, 9);

// マジックパケット 送信
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(packet);
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    static public void setCheckedTextViewListener(final CheckedTextView ctv) {
        ctv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv.toggle();
            }
        });
    }

    public ArrayList<HistoryListAdapter.HistoryListItem> loadHistoryList() {
        long b_time=System.currentTimeMillis();
        ArrayList<HistoryListAdapter.HistoryListItem> hl = new ArrayList<HistoryListAdapter.HistoryListItem>(GlobalParameters.HISTORY_LIST_INITIAL_VALUE);
        try {
            SafFile3 lf =new SafFile3(mContext, mGp.settingAppManagemsntDirectoryName + "/.history");
            if (lf.exists()) {
                InputStreamReader isr = new InputStreamReader(lf.getInputStream(), "UTF-8");
                BufferedReader bir=new BufferedReader(isr, 1024*100);
                String line = "";
                String[] l_array = null;
                while ((line = bir.readLine()) != null) {
                    l_array = line.split(LIST_ITEM_SEPARATOR);
                    if (l_array != null && l_array.length >= 16 && !l_array[3].equals("")) {
                        HistoryListAdapter.HistoryListItem hli = new HistoryListAdapter.HistoryListItem();
                        try {
                            hli.sync_date = l_array[0];
                            hli.sync_time = l_array[1];
                            hli.sync_elapsed_time = Long.parseLong(l_array[2]);
                            hli.sync_task = l_array[3];
                            hli.sync_status = Integer.valueOf(l_array[4]);
                            hli.sync_test_mode = l_array[5].equals("1") ? true : false;
                            hli.sync_result_no_of_copied = Integer.valueOf(l_array[6]);
                            hli.sync_result_no_of_deleted = Integer.valueOf(l_array[7]);
                            hli.sync_result_no_of_ignored = Integer.valueOf(l_array[8]);
                            hli.sync_result_no_of_moved = Integer.valueOf(l_array[9]);
                            hli.sync_result_no_of_replaced = Integer.valueOf(l_array[10]);
                            hli.sync_req = l_array[11];
                            hli.sync_error_text = l_array[12].replaceAll(LIST_ITEM_ENCODE_CR_CHARACTER, "\n");
                            hli.sync_result_no_of_retry = Integer.valueOf(l_array[13]);
                            hli.sync_transfer_speed=l_array[14];
                            hli.sync_result_file_path = l_array[15];
                            hl.add(hli);
                        } catch (Exception e) {
                            addLogMsg("W", "", "History list can not loaded");
                            e.printStackTrace();
                        }
                    }
                }
                bir.close();
                if (hl.size() > 1) {
                    Collections.sort(hl, new Comparator<HistoryListAdapter.HistoryListItem>() {
                        @Override
                        public int compare(HistoryListAdapter.HistoryListItem lhs, HistoryListAdapter.HistoryListItem rhs) {
                            if (rhs.sync_date.equals(lhs.sync_date)) {
                                if (rhs.sync_time.equals(lhs.sync_time)) {
                                    return lhs.sync_task.compareToIgnoreCase(rhs.sync_task);
                                } else return rhs.sync_time.compareTo(lhs.sync_time);
                            } else return rhs.sync_date.compareTo(lhs.sync_date);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error(CommonUtilities.getExecutedMethodName()+" error.", e);
        }
        return hl;
    }

    public static String getStoragePathFromUuid(String uuid) {
        if (uuid.equals(SAF_FILE_PRIMARY_UUID)) return SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX;
        else return SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+uuid;
    }

    final public void saveHistoryList(final ArrayList<HistoryListAdapter.HistoryListItem> hl) {
        if (hl == null || (hl!=null && hl.size()==0)) return;
        synchronized (hl) {
            try {
                SafFile3 df =new SafFile3(mContext, mGp.settingAppManagemsntDirectoryName);
                if (!df.exists()) df.mkdirs();
                SafFile3 mf =new SafFile3(mContext, mGp.settingAppManagemsntDirectoryName + "/.history");
                mf.deleteIfExists();
                if (!mf.exists()) mf.createNewFile();

                OutputStream fos=mf.getOutputStream();
                BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*100);
                PrintWriter bw=new PrintWriter(bos);

                int max = 500;
                StringBuilder sb_buf = new StringBuilder(1024 * 2);
                HistoryListAdapter.HistoryListItem shli = null;
                final ArrayList<HistoryListAdapter.HistoryListItem> del_list = new ArrayList<HistoryListAdapter.HistoryListItem>();
                for (int i = 0; i < hl.size(); i++) {
                    if (!hl.get(i).sync_task.equals("")) {
                        shli = hl.get(i);
                        if (i < max) {
                            sb_buf.setLength(0);
                            sb_buf.append(shli.sync_date).append(LIST_ITEM_SEPARATOR)                           //0
                                    .append(shli.sync_time).append(LIST_ITEM_SEPARATOR)                         //1
                                    .append(String.valueOf(shli.sync_elapsed_time)).append(LIST_ITEM_SEPARATOR) //2
                                    .append(shli.sync_task).append(LIST_ITEM_SEPARATOR)                         //3
                                    .append(shli.sync_status).append(LIST_ITEM_SEPARATOR)                       //4
                                    .append(shli.sync_test_mode ? "1" : "0").append(LIST_ITEM_SEPARATOR)        //5
                                    .append(shli.sync_result_no_of_copied).append(LIST_ITEM_SEPARATOR)          //6
                                    .append(shli.sync_result_no_of_deleted).append(LIST_ITEM_SEPARATOR)         //7
                                    .append(shli.sync_result_no_of_ignored).append(LIST_ITEM_SEPARATOR)         //8
                                    .append(shli.sync_result_no_of_moved).append(LIST_ITEM_SEPARATOR)           //9
                                    .append(shli.sync_result_no_of_replaced).append(LIST_ITEM_SEPARATOR)        //10
                                    .append(shli.sync_req).append(LIST_ITEM_SEPARATOR)                          //11
                                    .append(shli.sync_error_text.replaceAll("\n", LIST_ITEM_ENCODE_CR_CHARACTER)).append(LIST_ITEM_SEPARATOR)//12
                                    .append(shli.sync_result_no_of_retry).append(LIST_ITEM_SEPARATOR)           //13
                                    .append(shli.sync_transfer_speed).append(LIST_ITEM_SEPARATOR)               //14
                                    .append(shli.sync_result_file_path)                                         //15
                                    .append("\n");

                            bw.append(sb_buf.toString());
                        } else {
                            del_list.add(shli);
                            if (!shli.sync_result_file_path.equals("")) {
                                File tlf = new File(shli.sync_result_file_path);
                                if (tlf.exists()) tlf.delete();
                            }
                        }
                    }
                }
                bw.flush();
                bw.close();
            } catch (Exception e) {
                log.error(CommonUtilities.getExecutedMethodName()+" error.", e);
            }
        }
    }

    static public void setDialogBoxOutline(Context c, LinearLayout ll) {
        setDialogBoxOutline(c, ll, 3, 5);
    }

    static public void setDialogBoxOutline(Context c, LinearLayout ll, int padding_dp, int margin_dp) {
        ll.setBackgroundResource(R.drawable.dialog_box_outline);
        int padding=(int)toPixel(c.getResources(),padding_dp);
        ll.setPadding(padding, padding, padding, padding);

        ViewGroup.LayoutParams lp = ll.getLayoutParams();
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;
        int margin=(int)toPixel(c.getResources(), margin_dp);
        mlp.setMargins(margin, mlp.topMargin, margin, mlp.bottomMargin);
        ll.setLayoutParams(mlp);
    }

    final static public float toPixel(Resources res, int dip) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
        return px;
    }

    public static boolean isCharging(Context c, CommonUtilities cu) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryInfo = c.registerReceiver(null, ifilter);
        // Are we charging / charged?
        int status = batteryInfo.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean legacy_charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int bs=batteryInfo.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        int bl=batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int batteryLevel=(bs==0)?bl:(bl*100)/bs;

        // How are we charging?
        int chargePlug = batteryInfo.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
//        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        BatteryManager bm=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);;
        boolean bm_charging=bm.isCharging();
        cu.addDebugMsg(1, "I", "Battery status="+status+", level="+batteryLevel+", chargePlug="+chargePlug+", bm_charging="+bm_charging+", legacy_charging="+legacy_charging);

        return bm_charging;
    }

    static public boolean isExfatFileSystem(String uuid) {
        boolean result=false;
        String fs=getExternalStorageFileSystemName(uuid);
        if (fs.toLowerCase().contains("exfat")) result=true;
        return result;
    }

    static public String getExternalStorageFileSystemName(String uuid) {
        String result="";
        try {
            String resp= ShellCommandUtil.executeShellCommand(new String[]{"/bin/sh", "-c", "mount | grep -e ^/dev.*/mnt/media_rw/"+uuid});
            if (resp!=null && !resp.equals("")) {
                String[] fs_array=resp.split(" ");
                for(int i=0;i<fs_array.length;i++) {
                    if (fs_array[i].equals("type")) {
                        result=fs_array[i+1];
                    }
                }
            }
            log.debug("getExternalStorageFileSystemName result="+result+", uuid="+uuid);
        } catch (Exception e) {
            log.debug("getExternalStorageFileSystemName error="+e.toString()+MiscUtil.getStackTraceString(e));
        }
        return result;
    }

    public static void setEditTextPasteCopyEnabled(EditText et, boolean enabled) {
        if (enabled) {
            et.setCustomSelectionActionModeCallback(null);
        } else {
            et.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {}

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }
            });
        }
    }
}

