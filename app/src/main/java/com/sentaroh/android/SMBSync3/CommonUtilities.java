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
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.sentaroh.android.JcifsFile2.JcifsUtil;
import com.sentaroh.android.SMBSync3.Log.LogUtil;


import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.ShellCommandUtil;
import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.SystemInfo;

import org.markdownj.MarkdownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;

import static android.content.Context.USAGE_STATS_SERVICE;
import static com.sentaroh.android.SMBSync3.Constants.GENERAL_IO_BUFFER_SIZE;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public final class CommonUtilities {
    private static final Logger log= LoggerFactory.getLogger(CommonUtilities.class);
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

    public static void setViewEnabled(Activity a, View v, boolean enabled) {
//        Thread.dumpStack();
        GlobalParameters gp=GlobalWorkArea.getGlobalParameter(a);
        CommonDialog.setViewEnabled(gp.themeColorList.theme_is_light, v, enabled);
    }

    public SharedPreferences getSharedPreference() {
        return getSharedPreference(mContext);
    }

    public static SharedPreferences getSharedPreference(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static void setSpinnerBackground(Context c, Spinner spinner, boolean theme_is_light) {
        if (theme_is_light) spinner.setBackground(ContextCompat.getDrawable(c, R.drawable.spinner_color_background_light));
        else spinner.setBackground(ContextCompat.getDrawable(c, R.drawable.spinner_color_background));
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    }

    public void showCommonDialogInfo(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_INFO, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    }

    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    }

    public void showCommonDialogError(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_ERROR, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    }

    // not used
    public void showCommonDialogDanger(final boolean negative, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext);
        cdf.showDialog(mFragMgr,cdf, listener);
    }

    public static void showCommonDialog(FragmentManager fm, final boolean negative, String type, String title, String msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.showDialog(fm, cdf, listener);
    }

    public void showCommonDialog(final boolean negative, String type, String title, Spannable msgtext, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, "");
        cdf.setMessageText(msgtext);
        cdf.showDialog(mFragMgr,cdf,listener);
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, int text_color, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext);
        cdf.setTextColor(text_color);
        cdf.showDialog(mFragMgr,cdf,listener);
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    }

    public void showCommonDialog(final boolean negative, String type, String title, String msgtext,
                                 String ok_text, String cancel_text, String extra_btn_label,
                                 Object listener, CallBackListener cbl, boolean max) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, type, title, msgtext, ok_text, cancel_text, extra_btn_label);
        cdf.showDialog(mFragMgr, cdf, listener, cbl, max);
    }

    public void showCommonDialogWarn(final boolean negative, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_WARN, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    }

    public void showCommonDialogDanger(boolean negative, String title, String msgtext, String ok_text, String cancel_text, Object listener) {
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(negative, MessageDialogFragment.CATEGORY_DANGER, title, msgtext, ok_text, cancel_text);
        cdf.showDialog(mFragMgr,cdf,listener);
    }

    public static String convertMakdownToHtml(Context c, String mark_down_fp) {
//        long b_time=System.currentTimeMillis();
        String html ="";
        try {
            InputStream is = c.getAssets().open(mark_down_fp);
            MarkdownProcessor processor = new MarkdownProcessor();
            html=processor.markdown(false, is);
        } catch(Exception e) {
            log.error("MarkDown conversion error.", e);
            e.printStackTrace();
        }
//        Log.v(APPLICATION_TAG, "convertMakdownToHtml elapsed time="+(System.currentTimeMillis()-b_time));
        return html;
    }

    public String getStringWithLangCode(Activity c, String lang_code, int res_id) {
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.setLocale(new Locale(lang_code));
        return c.createConfigurationContext(config).getText(res_id).toString();
    }

    // not used
    public String getStringWithLangCode(Activity c, String lang_code, int res_id, Object... value) {
        String text = getStringWithLangCode(c, lang_code, res_id);
        String result=text;
        if (value!=null && value.length>0) result=String.format(text, value);
        return result;
    }

    // not used
    public static String getRootFilePath(String fp) {
        String reform_fp=StringUtil.removeRedundantDirectorySeparator(fp);
        String is_pre=Environment.getExternalStorageDirectory().toString();
        if (reform_fp.startsWith(is_pre)) return is_pre;
        else {
            String[] fp_parts=reform_fp.startsWith("/")?reform_fp.substring(1).split("/"):reform_fp.split("/");
            return "/"+fp_parts[0]+"/"+fp_parts[1];
        }
    }

    // not used
    public static boolean isAllFileAccessAvailable() {
        return SafFile3.isAllFileAccessAvailable();
    }

    // not used
    public void setLogId(String li) {
        mLog.setLogId(li);
    }

    public static String getExecutedMethodName() {
        return Thread.currentThread().getStackTrace()[3].getMethodName();
    }

    // not used
    public void resetLogReceiver() {
        mLog.resetLogReceiver();
    }

    public void flushLog() {
        mLog.flushLog();
    }

    // not used
    public void rotateLogFile() {
        mLog.rotateLogFile();
    }

    public static ArrayList<String> listSystemInfo(Context c, GlobalParameters gp) {

        ArrayList<String> out=SystemInfo.listSystemInfo(c, gp.safMgr);

        if (Build.VERSION.SDK_INT >= 28) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) c.getSystemService(USAGE_STATS_SERVICE);
            if (usageStatsManager != null) {
                out.add("AppStnadbyBuket="+usageStatsManager.getAppStandbyBucket());
            }
        }

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
                if (wm.getConnectionInfo()!=null) out.add("   LinkSpeed="+wm.getConnectionInfo().getLinkSpeed());
                else out.add("   LinkSpeed=-1");
            } catch(Exception e) {
                out.add("   WiFi status obtain error, error="+e.getMessage());
            }

            ConnectivityManager cm =(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork!=null) {
                String network=activeNetwork.getExtraInfo();

                boolean isConnected = activeNetwork.isConnectedOrConnecting();
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
        out.add("  Exit clean="+gp.settingExitClean);

        return out;
    }

    public static boolean isLocationServiceEnabled(Context c, GlobalParameters mGp) {
        if (Build.VERSION.SDK_INT >= 27) {
            LocationManager lm = (LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT == 27) {
                boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean nw = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                return gps | nw;
            } else if (Build.VERSION.SDK_INT >= 28) {
                return lm.isLocationEnabled();
            }
        }
        return false;
    }

    // not used
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
    }

    // not used
    public void deleteLogFile() {
        mLog.deleteLogFile();
    }

    public String buildPrintMsg(String cat, String... msg) {
        return mLog.buildPrintLogMsg(cat, msg);
    }

    public static void setAboutFindListener(Activity a, LinearLayout ll, WebView wv) {
        final EditText et_find_string=(EditText)ll.findViewById(R.id.dlg_find_view_search_word);
        final ImageButton ib_find_next=(ImageButton) ll.findViewById(R.id.dlg_find_view_find_next);
        final ImageButton ib_find_prev=(ImageButton) ll.findViewById(R.id.dlg_find_view_find_prev);
        final TextView tv_find_count=(TextView) ll.findViewById(R.id.dlg_find_view_find_count);

//        wv.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading (WebView view, String url) {
//                return false;
//            }
//
//            @Override
//            public void onPageFinished (WebView view, String url) {
//            }
//        });

        ib_find_next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wv.findNext(true);
            }
        });
        ib_find_next.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CommonDialog.showPopupMessageAsUpAnchorView(a, ib_find_next,
                        a.getString(R.string.msgs_find_view_search_next_label), 2);
                return true;
            }
        });

        ib_find_prev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wv.findNext(false);
            }
        });
        ib_find_prev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CommonDialog.showPopupMessageAsUpAnchorView(a, ib_find_next,
                        a.getString(R.string.msgs_find_view_search_prev_label), 2);
                return true;
            }
        });

        CommonUtilities.setViewEnabled(a, ib_find_next, false);
        CommonUtilities.setViewEnabled(a, ib_find_prev, false);

        final ColorStateList default_text_color=tv_find_count.getTextColors();
        wv.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int i, int i1, boolean b) {
                if (et_find_string.getText().length()>0) {
                    if (i1>0) {
                        String str = String.format("%d/%d", (i+1), i1);
                        tv_find_count.setText(str);
                        tv_find_count.setTextColor(default_text_color);
                        CommonUtilities.setViewEnabled(a, ib_find_next, true);
                        CommonUtilities.setViewEnabled(a, ib_find_prev, true);
                    } else {
                        tv_find_count.setText("0/0");
                        tv_find_count.setTextColor(Color.RED);
                        CommonUtilities.setViewEnabled(a, ib_find_next, false);
                        CommonUtilities.setViewEnabled(a, ib_find_prev, false);
                    }
                } else {
                    CommonUtilities.setViewEnabled(a, ib_find_next, false);
                    CommonUtilities.setViewEnabled(a, ib_find_prev, false);
                    tv_find_count.setText("");
                    tv_find_count.setTextColor(default_text_color);
                }
            }
        });

        et_find_string.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0) {
                    wv.findAllAsync(s.toString());
                } else {
                    CommonUtilities.setViewEnabled(a, ib_find_next, false);
                    CommonUtilities.setViewEnabled(a, ib_find_prev, false);
                    wv.findAllAsync("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public static void setWebViewListener(GlobalParameters gp, WebView wv, int zf) {
//        wv.setBackgroundColor(Color.LTGRAY);
//        if (Build.VERSION.SDK_INT>=29) {
//            if (!gp.themeColorList.theme_is_light) {
//                wv.getSettings().setForceDark(WebSettingsCompat.FORCE_DARK_ON);
//            }
//        }
//        if (gp.applicationTheme==R.style.Main) {
//            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
//                WebSettingsCompat.setForceDark(wv.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
//                wv.setBackgroundColor(Color.DKGRAY);
//            }
//        } if (gp.applicationTheme==R.style.MainLight) {
//        } else {
//            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
//                WebSettingsCompat.setForceDark(wv.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
//                wv.setBackgroundColor(Color.BLACK);
//            }
//        }
        wv.getSettings().setTextZoom(zf);
//        wv.getSettings().setBuiltInZoomControls(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading (WebView view, WebResourceRequest request) {
                // Returning false means we are going to load this url in the webView itself
                return false;
            }
        });
        wv.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if(event.getAction() == KeyEvent.ACTION_DOWN){
                    WebView webView = (WebView) v;
                    switch(keyCode){
                        case KeyEvent.KEYCODE_BACK:
                            if(webView.canGoBack()){
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }
                return false;
            }
        });
    }

    final public static String LIST_ITEM_DATA_SEPARATOR ="\u0000";
    final public static String LIST_ITEM_DUMMY_DATA ="\u0001";
    final public static String LIST_ITEM_ENCODE_CR_CHARACTER ="\u0003";
    final public static String LIST_ITEM_LINE_SEPARATOR="\n";
    synchronized public static void saveMessageList(Context c, GlobalParameters gp) {
//        Thread.dumpStack();
        if (gp.syncMessageList == null || gp.syncMessageList.size() == 0) return;
        //long b_time= System.currentTimeMillis();
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
                StringBuilder sb=new StringBuilder(1024*5);
                synchronized (gp.mLockSyncMessageList) {
                    for (MessageListAdapter.MessageListItem smi:gp.syncMessageList) {
                        sb.setLength(0);
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getCategory()).append(LIST_ITEM_DATA_SEPARATOR); //msgCat
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getDate()).append(LIST_ITEM_DATA_SEPARATOR); //msgDate
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getTime()).append(LIST_ITEM_DATA_SEPARATOR); //msgTime
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getTitle()).append(LIST_ITEM_DATA_SEPARATOR); //msgTitle
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getMessage().replaceAll("\n", LIST_ITEM_ENCODE_CR_CHARACTER)).append(LIST_ITEM_DATA_SEPARATOR); //msgBody
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getPath()).append(LIST_ITEM_DATA_SEPARATOR); //msgPath
                        sb.append(LIST_ITEM_DUMMY_DATA).append(smi.getType()).append(LIST_ITEM_DATA_SEPARATOR); //msgType
                        pw.println(sb);
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

    public static ArrayList<MessageListAdapter.MessageListItem> loadMessageList(Context c, GlobalParameters gp) {
        //long b_time= System.currentTimeMillis();
        ArrayList<MessageListAdapter.MessageListItem> result=new ArrayList<MessageListAdapter.MessageListItem>(GlobalParameters.MESSAGE_LIST_INITIAL_VALUE);
        try {
            SafFile3 mf =new SafFile3(c, gp.settingAppManagemsntDirectoryName + "/.messages");
            if (mf!=null && mf.exists()) {
                InputStreamReader isr = new InputStreamReader(mf.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader bir=new BufferedReader(isr, GENERAL_IO_BUFFER_SIZE);
                String line=null;
                while((line=bir.readLine())!=null) {
                    String[] msg_array=line.split(LIST_ITEM_DATA_SEPARATOR);
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

    public void addLogMsg(boolean ui_thread, boolean has_result_type, boolean has_path, boolean has_title,
                                String cat, String title, String result_type, String path, String... msg) {
//		final SyncMessageItem mli=new SyncMessageItem(cat, "","", title, mLog.buildLogCatMsg("", cat, msg), path, type);
        String finalMsg = "";
        StringBuilder log_msg = new StringBuilder(512);
        for (String s : msg) log_msg.append(s);
        if (!log_msg.toString().equals("")) finalMsg = log_msg.toString();
        if (!title.equals("")) {
            if (finalMsg.equals("")) mLog.addLogMsg(cat, title.concat(": ").concat(path).concat(result_type));
            else mLog.addLogMsg(cat, title.concat(": ").concat(finalMsg).concat(" ").concat(path).concat(result_type));
        } else {
            mLog.addLogMsg(cat, finalMsg.concat(" ").concat(path).concat(result_type));
        }

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

    public void addLogMsg(String cat, String task, String... msg) {
        addLogMsg(false, false, false, false, cat, task, "", "", msg);
    }

    // not used
    public void addLogMsgFromUI(String cat, String task, String... msg) {
        addLogMsg(true, false, false, false, cat, task, "", "", msg);
    }

    private void putMsgListArray(MessageListAdapter.MessageListItem mli) {
        final int MAX_MSG_COUNT = 5000;
        synchronized (mGp.mLockSyncMessageList) {
            if (mGp.syncMessageList.size() > (MAX_MSG_COUNT + 200)) {
                mGp.syncMessageList.subList(0, 200).clear();
                //for (int i = 0; i < 200; i++) mGp.syncMessageList.remove(0);
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

    public void addDebugMsg(int lvl, String cat, String... msg) {
        mLog.addDebugMsg(lvl, cat, msg);
    }

    // not used
    public boolean isLogFileExists() {
        boolean result = false;
        result = mLog.isLogFileExists();
        if (mLog.getLogLevel() >= 3) addDebugMsg(3, "I", "Log file exists=" + result);
        return result;
    }

    // not used
    public String getLogFilePath() {
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
        return (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
    }

    // not used
    public boolean isWifiActive() {
        boolean ret = false;
        WifiManager mWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mWifi.isWifiEnabled()) ret = true;
        addDebugMsg(2, "I", "isWifiActive WifiEnabled=" + ret);
        return ret;
    }

    public static String decryptUserData(Context c, EncryptUtilV3.CipherParms cp_int, String enc_str) {
        String dec_str = null;
        byte[] dec_array = Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
        dec_str = EncryptUtilV3.decrypt(dec_array, cp_int);
        return dec_str;
    }

    public static String encryptUserData(Context c, EncryptUtilV3.CipherParms cp_int, String user_data) {
        byte[] enc_byte = EncryptUtilV3.encrypt(user_data, cp_int);
        return Base64Compat.encodeToString(enc_byte, Base64Compat.NO_WRAP);
    }

    public static String buildSmbUrlAddressElement(String host, String port) {
        String url_port=port.equals("")?"":":"+port;
        String url_address="";
        if (CommonUtilities.isIpAddressV6(host)) {
            if (host.contains(":")) {
                String conv_address=addScopeidToIpv6Address(host);
                url_address=conv_address==null?"["+host+url_port+"]":"["+conv_address+url_port+"]";
            } else {
                url_address=host+url_port;
            }
        } else {
            url_address=host+url_port;
        }
        return url_address;
    }

    public static boolean canSmbHostConnectable(String addr) {
        return JcifsUtil.canIpAddressAndPortConnectable(addr, 139, 3500) ||
                JcifsUtil.canIpAddressAndPortConnectable(addr, 445, 3500);
    }

    public static boolean canSmbHostConnectable(String addr, String port) {
        boolean result = false;
        result = JcifsUtil.canIpAddressAndPortConnectable(addr, Integer.parseInt(port), 3500);
        return result;
    }

    public static boolean canSmbHostConnectable(String addr, int port) {
        boolean result = false;
        result = JcifsUtil.canIpAddressAndPortConnectable(addr, port, 3500);
        return result;
    }

    // not used
    public static boolean isSmbHost(CommonUtilities cu, String address, String scan_port) {
        return isSmbHost(cu, address, scan_port, 3500);
    }

    public static boolean isSmbHost(CommonUtilities cu, String address, String scan_port, int time_out) {
        boolean smbhost = false;
        if (scan_port.equals("")) {
            if (!JcifsUtil.canIpAddressAndPortConnectable(address, 445, time_out)) {
                smbhost = JcifsUtil.canIpAddressAndPortConnectable(address, 139, time_out);
            } else smbhost = true;
        } else {
            smbhost = JcifsUtil.canIpAddressAndPortConnectable(address, Integer.parseInt(scan_port), time_out);
        }
        cu.addDebugMsg(2, "I", "isIpAddrSmbHost Address=" + address + ", port=" + scan_port + ", smbhost=" + smbhost);
        return smbhost;
    }

    public static String getSmbHostName(CommonUtilities cu, String smb_level, String address) {
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
            e.printStackTrace();
        }
        return result;
    }

    // not used
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

    // not used
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

    // not used
    public static boolean isIpV4PrivateAddress(String ip_v4_addr) {
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

    public static boolean isPrivateAddress(String if_addr) {
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

    // not used
    public static String getIfHwAddress(String if_name) {
        StringBuilder result = new StringBuilder();
        boolean exit = false;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.isSiteLocalAddress() && (inetAddress instanceof Inet4Address)) {
                        if (intf.getName().equals(if_name)) {
                            for(int i=0;i<intf.getHardwareAddress().length;i++) result.append(String.format("%2h", intf.getHardwareAddress()[i]).replaceAll(" ", "0"));
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
        return result.toString();
    }

    // not used
    private void sendMagicPacket(final String target_mac, final String if_network) {
//                sendMagicPacket("08:bd:43:f6:48:2a", if_ip);
        Thread th=new Thread(){
            @Override
            public void run() {
                byte[] broadcastMacAddress=new byte[6];
                for(int i=0;i<6;i++) broadcastMacAddress[i]=(byte)0xff;
                InetAddress broadcastIpAddress = null;
                try {
                    //int j=if_network.lastIndexOf(".");
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    public static void setCheckedTextViewListener(final CheckedTextView ctv) {
        ctv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv.toggle();
            }
        });
    }

    public ArrayList<HistoryListAdapter.HistoryListItem> loadHistoryList() {
        //long b_time=System.currentTimeMillis();
        ArrayList<HistoryListAdapter.HistoryListItem> hl = new ArrayList<HistoryListAdapter.HistoryListItem>(GlobalParameters.HISTORY_LIST_INITIAL_VALUE);
        try {
            SafFile3 lf =new SafFile3(mContext, mGp.settingAppManagemsntDirectoryName + "/.history");
            if (lf.exists()) {
                InputStreamReader isr = new InputStreamReader(lf.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader bir=new BufferedReader(isr, 1024*100);
                String line = "";
                String[] l_array = null;
                while ((line = bir.readLine()) != null) {
                    l_array = line.split(LIST_ITEM_DATA_SEPARATOR);
                    if (l_array != null && l_array.length >= 16 && !l_array[3].equals("")) {
                        HistoryListAdapter.HistoryListItem hli = new HistoryListAdapter.HistoryListItem();
                        try {
                            hli.sync_date = l_array[0];
                            hli.sync_time = l_array[1];
                            hli.sync_elapsed_time = Long.parseLong(l_array[2]);
                            hli.sync_task = l_array[3];
                            hli.sync_status = Integer.parseInt(l_array[4]);
                            hli.sync_test_mode = l_array[5].equals("1");
                            hli.sync_result_no_of_copied = Integer.parseInt(l_array[6]);
                            hli.sync_result_no_of_deleted = Integer.parseInt(l_array[7]);
                            hli.sync_result_no_of_ignored = Integer.parseInt(l_array[8]);
                            hli.sync_result_no_of_moved = Integer.parseInt(l_array[9]);
                            hli.sync_result_no_of_replaced = Integer.parseInt(l_array[10]);
                            hli.sync_req = l_array[11];
                            hli.sync_error_text = l_array[12].replaceAll(LIST_ITEM_ENCODE_CR_CHARACTER, "\n");
                            hli.sync_result_no_of_retry = Integer.parseInt(l_array[13]);
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
                    hl.sort(new Comparator<HistoryListAdapter.HistoryListItem>() {
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
        if (uuid.equals(SAF_FILE_PRIMARY_UUID)) return Environment.getExternalStorageDirectory().getPath();// /storage/emulated/0
        else return SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+uuid;
    }

    public final Object mLockHistoryListAdapter = new Object();
    public void saveHistoryList(final ArrayList<HistoryListAdapter.HistoryListItem> hl) {
        if (hl == null || hl.size() == 0) return;
        synchronized (mLockHistoryListAdapter) {
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
                //final ArrayList<HistoryListAdapter.HistoryListItem> del_list = new ArrayList<HistoryListAdapter.HistoryListItem>();
                for (int i = 0; i < hl.size(); i++) {
                    if (!hl.get(i).sync_task.equals("")) {
                        shli = hl.get(i);
                        if (i < max) {
                            sb_buf.setLength(0);
                            sb_buf.append(shli.sync_date).append(LIST_ITEM_DATA_SEPARATOR)                           //0
                                    .append(shli.sync_time).append(LIST_ITEM_DATA_SEPARATOR)                         //1
                                    .append(shli.sync_elapsed_time).append(LIST_ITEM_DATA_SEPARATOR) //2
                                    .append(shli.sync_task).append(LIST_ITEM_DATA_SEPARATOR)                         //3
                                    .append(shli.sync_status).append(LIST_ITEM_DATA_SEPARATOR)                       //4
                                    .append(shli.sync_test_mode ? "1" : "0").append(LIST_ITEM_DATA_SEPARATOR)        //5
                                    .append(shli.sync_result_no_of_copied).append(LIST_ITEM_DATA_SEPARATOR)          //6
                                    .append(shli.sync_result_no_of_deleted).append(LIST_ITEM_DATA_SEPARATOR)         //7
                                    .append(shli.sync_result_no_of_ignored).append(LIST_ITEM_DATA_SEPARATOR)         //8
                                    .append(shli.sync_result_no_of_moved).append(LIST_ITEM_DATA_SEPARATOR)           //9
                                    .append(shli.sync_result_no_of_replaced).append(LIST_ITEM_DATA_SEPARATOR)        //10
                                    .append(shli.sync_req).append(LIST_ITEM_DATA_SEPARATOR)                          //11
                                    .append(shli.sync_error_text.replaceAll("\n", LIST_ITEM_ENCODE_CR_CHARACTER)).append(LIST_ITEM_DATA_SEPARATOR)//12
                                    .append(shli.sync_result_no_of_retry).append(LIST_ITEM_DATA_SEPARATOR)           //13
                                    .append(shli.sync_transfer_speed).append(LIST_ITEM_DATA_SEPARATOR)               //14
                                    .append(shli.sync_result_file_path)                                              //15
                                    .append("\n");

                            bw.append(sb_buf.toString());
                        } else {
                            //del_list.add(shli);
                            if (!shli.sync_result_file_path.equals("")) {
                                // Delete result_log/sync_result_file_date.txt for the history items removed
                                // Optional: keep them, or move to result_log.old direcory
                                File tlf = new File(shli.sync_result_file_path);
                                if (!tlf.exists() || !tlf.delete()) log.error("saveHistoryList: Error, failed to prune shli.sync_result_file_path="+tlf.getName());
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

    public static void setDialogBoxOutline(Context c, LinearLayout ll) {
        setDialogBoxOutline(c, ll, 3, 5);
    }

    public static void setDialogBoxOutline(Context c, LinearLayout ll, int padding_dp, int margin_dp) {
        ll.setBackgroundResource(R.drawable.dialog_box_outline);
        int padding=(int)toPixel(c.getResources(),padding_dp);
        ll.setPadding(padding, padding, padding, padding);

        ViewGroup.LayoutParams lp = ll.getLayoutParams();
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;
        int margin=(int)toPixel(c.getResources(), margin_dp);
        mlp.setMargins(margin, mlp.topMargin, margin, mlp.bottomMargin);
        ll.setLayoutParams(mlp);
    }

    public static float toPixel(Resources res, int dip) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
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

        BatteryManager bm=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);
        boolean bm_charging=bm.isCharging();
        cu.addDebugMsg(1, "I", "Battery status="+status+", level="+batteryLevel+", chargePlug="+chargePlug+", bm_charging="+bm_charging+", legacy_charging="+legacy_charging);

        return bm_charging;
    }

    public static boolean isIgnoringBatteryOptimizations(Context c) {
        String packageName = c.getPackageName();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(packageName);
    }

    public static boolean isExfatFileSystem(String uuid) {
        boolean result=false;
        String fs=getExternalStorageFileSystemName(uuid);
        if (fs.toLowerCase().contains("exfat")) result=true;
        return result;
    }

    public static String getExternalStorageFileSystemName(String uuid) {
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
            log.debug("getExternalStorageFileSystemName error="+ e);
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

