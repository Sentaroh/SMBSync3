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

import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.EncryptUtilV1;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;

import com.sentaroh.android.SMBSync3.SyncConfiguration.SettingParameterItem;
import com.sentaroh.android.Utilities3.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.sentaroh.android.SMBSync3.Constants.GENERAL_IO_BUFFER_SIZE;

public class TaskListImportFromSMBSync2 {
    private static Logger log = LoggerFactory.getLogger(TaskListImportFromSMBSync2.class);

    static public boolean isSyncTaskListEncrypted(Context c, GlobalParameters gp, CommonUtilities mUtil, SafFile3 sf) {
        boolean result=false;
        BufferedReader br=null;
        try {
            InputStreamReader isr=new InputStreamReader(sf.getInputStream());
            br=new BufferedReader(isr, GENERAL_IO_BUFFER_SIZE);
            String pl=br.readLine();
            if (pl.startsWith(SMBSYNC2_PROF_VER8+SMBSYNC2_PROF_ENC)) {
                result=true;
            }
            br.close();
        } catch(Exception e) {
            try {if (br!=null) br.close();} catch(Exception ex) {};
        }
        return result;
    }

    static public boolean isSMBSync2SyncTaskList(Context c, GlobalParameters gp, CommonUtilities mUtil, SafFile3 sf) {
        boolean result=false;
        BufferedReader br=null;
        try {
            InputStreamReader isr=new InputStreamReader(sf.getInputStream());
            br=new BufferedReader(isr, GENERAL_IO_BUFFER_SIZE);
            String pl=br.readLine();
            result=isSMBSync2SyncTaskList(c, gp, mUtil, pl);
            br.close();
        } catch(Exception e) {
            try {if (br!=null) br.close();} catch(Exception ex) {};
        }
        return result;
    }

    static public boolean isSMBSync2SyncTaskList(Context c, GlobalParameters gp, CommonUtilities mUtil, String prof_data) {
        boolean result=false;
        if (prof_data.startsWith(SMBSYNC2_PROF_VER8)) {
            result=true;
        }
        return result;
    }

    static public boolean isCorrectPassowrd(String enc_str, String private_key) {
        if (enc_str==null || enc_str.equals("")) return false;
        if (private_key==null || private_key.equals("")) return false;
        EncryptUtilV1.CipherParms cp=EncryptUtilV1.initDecryptEnv(PROFILE_KEY_PREFIX +private_key);
        byte[] enc_array = Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
        String dec_str = EncryptUtilV1.decrypt(enc_array, cp);
        return dec_str==null?false:true;
    }

    static public void buildSyncTaskList(Context c, GlobalParameters gp, CommonUtilities mUtil,
                                         SafFile3 sf, String private_key, ArrayList<SyncTaskItem>sync_task, ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched) {
        ArrayList<SettingParameterItem>sync_setting=new ArrayList<SettingParameterItem>();
        boolean loaded=loadSyncTaskListFromFile(c, gp, mUtil, sf, private_key, sync_task, sync_setting);

        if (loaded) {
            boolean error_opt=false;
            for(SettingParameterItem spi:sync_setting) {
                if (spi.key.equals(SCHEDULER_SCHEDULE_SAVED_DATA_V5)) loadScheduleListV5(gp, spi.value, sync_sched);
//                if (spi.key.equals("settings_error_option")) {
//                    if (spi.value.equals("true")) error_opt=true;
//                }
            }
//            if (error_opt) {
//                for(SyncTaskItem item:sync_task) item.setSyncOptionStopSyncWhenError(false);
//            }
        }
    }

    static private final String PROFILE_KEY_PREFIX = "*SMBSync2*";
    static private boolean loadSyncTaskListFromFile(Context c, GlobalParameters gp, CommonUtilities mUtil,
                                                    SafFile3 sf, String private_key, ArrayList<SyncTaskItem>sync_task,
                                                    ArrayList<SettingParameterItem>sync_setting) {
        boolean result=false;
        try {
            boolean auto_save=false;
            if (sf.getName().toLowerCase().endsWith(".stf")) auto_save=true;
            InputStreamReader isr=new InputStreamReader(sf.getInputStream());
            BufferedReader br=new BufferedReader(isr, GENERAL_IO_BUFFER_SIZE);
            String pl=br.readLine();
            if (pl.startsWith(SMBSYNC2_PROF_VER8+SMBSYNC2_PROF_ENC)) {
                String enc_str=pl.substring(9);
                EncryptUtilV1.CipherParms cp=EncryptUtilV1.initDecryptEnv(PROFILE_KEY_PREFIX +private_key);
                byte[] enc_array = Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
                String dec_str = EncryptUtilV1.decrypt(enc_array, cp);
                if (dec_str!=null) {
                    pl=br.readLine();
                    while(pl!=null) {
                        enc_str=pl.substring(6);
                        enc_array = Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
                        dec_str = EncryptUtilV1.decrypt(enc_array, cp);
                        addSyncTaskListVer8(c, dec_str, sync_task, mUtil, auto_save);
                        if (sync_setting != null) addImportSettingsParm(dec_str, sync_setting);
                        pl=br.readLine();
                    }
                    result=true;
                } else {
                    mUtil.showCommonDialog(false,"W", "Import task list error", "Can not decrypt task list data.", null);
                }
            } else if (pl.startsWith(SMBSYNC2_PROF_VER8+SMBSYNC2_PROF_DEC)) {
                pl=br.readLine();
                while(pl!=null) {
                    String dec_str=pl.substring(6);
                    addSyncTaskListVer8(c, dec_str, sync_task, mUtil, auto_save);
                    if (sync_setting != null) addImportSettingsParm(pl.replace(SMBSYNC2_PROF_VER8, ""), sync_setting);
                    pl=br.readLine();
                }
                result=true;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    static private void addImportSettingsParm(String pl, ArrayList<SettingParameterItem> ispl) {
        String tmp_ps = pl;//pl.substring(7,pl.length());
        String[] tmp_pl = tmp_ps.split("\t");// {"type","name","active",options...};
        if (tmp_pl[0] != null && tmp_pl.length >= 3 && tmp_pl[0].equals(SMBSYNC2_PROF_TYPE_SETTINGS)) {
            SettingParameterItem ppli = new SettingParameterItem();
            if (tmp_pl[1] != null) ppli.key = tmp_pl[1];
            if (tmp_pl[2] != null) ppli.type = tmp_pl[2];
            if (tmp_pl.length >= 4 && tmp_pl[3] != null) ppli.value = tmp_pl[3];
            if (!ppli.key.equals("") && !ppli.type.equals("")) {
                ispl.add(ppli);
            }
        }
    }

    private static final String SCHEDULER_SCHEDULE_SAVED_DATA_V5 = "scheduler_schedule_saved_data_v5_key";
    private final static String SMBSYNC2_PROF_TYPE_SETTINGS="T";
    private final static String SMBSYNC2_PROF_VER8="PROF 8";
    private final static String SMBSYNC2_PROF_ENC="ENC";
    private final static String SMBSYNC2_PROF_DEC="DEC";

    private final static String SMBSYNC2_PROF_TYPE_SYNC="S";
    private final static String SYNC_FOLDER_TYPE_INTERNAL = "INT";
    private final static String SYNC_FOLDER_TYPE_SDCARD = "EXT";
    private final static String SYNC_FOLDER_TYPE_USB="USB";
    private final static String SYNC_FOLDER_TYPE_SMB = "SMB";
    private final static String SYNC_FOLDER_TYPE_ZIP = "ZIP";

    public static final String SCHEDULER_SEPARATOR_DUMMY_DATA = "\u0000";
    public static final String SCHEDULER_SEPARATOR_ENTRY = "\u0001";
    public static final String SCHEDULER_SEPARATOR_ITEM = "\u0002";

    final private static String SMBSYNC2_TASK_END_MARK="end";
    final static public void loadScheduleListV5(GlobalParameters gp, String v5_data, ArrayList<ScheduleListAdapter.ScheduleListItem>sync_sched) {
        String[] sd_array = v5_data.split(SCHEDULER_SEPARATOR_ENTRY);
        int nc=0;
        for (String sd_sub : sd_array) {
            if (sd_sub.equals(SMBSYNC2_TASK_END_MARK)) break;
            String[] sub_array = sd_sub.split(SCHEDULER_SEPARATOR_ITEM);
            if (sub_array.length >= 14) {
                for (String item : sub_array) item = item.replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                ScheduleListAdapter.ScheduleListItem si = new ScheduleListAdapter.ScheduleListItem();
                si.scheduleEnabled = sub_array[0].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "").equals("1") ? true : false;
                String sched_name=sub_array[1].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                String unusable=SyncConfiguration.hasUnusableCharacter(sched_name);
                if (unusable.equals("")) si.scheduleName = sched_name;
                else {
                    si.scheduleName = sched_name.replaceAll(unusable, "_");
                    log.info("SMBSync2 schedule name contains unusable chanaracter : \""+unusable+"\" replaced : \"_\"");
                }

                if (sub_array[2].length() > 0) si.schedulePosition = Integer.valueOf(sub_array[2].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, ""));
                si.scheduleType = sub_array[3].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                si.scheduleHours = sub_array[4].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                si.scheduleMinutes = sub_array[5].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                si.scheduleDayOfTheWeek = sub_array[6].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
//                si.scheduleIntervalFirstRunImmed = sub_array[7].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "").equals("1") ? true : false;
                if (sub_array[8].length() > 0) si.scheduleLastExecTime = Long.valueOf(sub_array[8].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, ""));
                si.syncTaskList = sub_array[9].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                si.syncGroupList = sub_array[10].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                si.syncWifiOnBeforeStart = sub_array[11].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "").equals("1") ? true : false;
                si.syncWifiOffAfterEnd = sub_array[12].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "").equals("1") ? true : false;
                if (sub_array[13].length() > 0) si.syncDelayAfterWifiOn = Integer.valueOf(sub_array[13].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, ""));

                if (sub_array.length >= 15 && sub_array[14]!=null && sub_array[14].length() > 0) si.scheduleDay = sub_array[14].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");

                if (sub_array.length >= 16 && sub_array[15]!=null && sub_array[15].length() > 0) si.syncAutoSyncTask = sub_array[15].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "").equals("1") ? true : false;
                if (!si.syncTaskList.equals("")) si.syncAutoSyncTask=false;

                if (sub_array.length >= 17 && sub_array[16]!=null && sub_array[16].length() > 0) {
                    si.syncOverrideOptionCharge = sub_array[16].replace(SCHEDULER_SEPARATOR_DUMMY_DATA, "");
                }

                if (si.scheduleLastExecTime == 0) si.scheduleLastExecTime = System.currentTimeMillis();

                sync_sched.add(si);
            }
        }
    }

    private static String getUuid(Context c, String sd_or_usb) {
        ArrayList<SafManager3.StorageVolumeInfo> svl= SafManager3.getStorageVolumeInfo(c);
        String uuid="XXXX-XXXX";
        for(SafManager3.StorageVolumeInfo sv_item:svl) {
            if (sv_item.description.toLowerCase().contains(sd_or_usb.toLowerCase())) {
                uuid=sv_item.uuid;
                break;
            }
        }
        return uuid;
    }

    final private static String WHOLE_DIRECTORY_FILTER_PREFIX="\\\\";
    final private static String MATCH_ANY_WHERE_DIRECTORY_FILTER_PREFIX="\\";
    private static void addSyncTaskListVer8(Context c, String pl, ArrayList<SyncTaskItem> sync, CommonUtilities util, boolean auto_save) {
        if (!pl.startsWith(SMBSYNC2_PROF_TYPE_SYNC)) return; //ignore settings entry
        String list1 = "", list2 = "", list3 = "", list4="", npl = "";
        int ls = pl.indexOf("[");
        int le = pl.lastIndexOf("]\t");
        String list = pl.substring(ls, le + 2);
        npl = pl.replace(list, "");

        String[] list_array = list.split("]\t");
        list1 = list_array[0].substring(1);
        list2 = list_array[1].substring(1);
        list3 = list_array[2].substring(1);
        if (list_array.length>=4) list4 = list_array[3].substring(1);

        String[] tmp_pl = npl.split("\t");// {"type","name","active",options...};
        String[] parm = new String[200];
        for (int i = 0; i < 200; i++) parm[i] = "";
        for (int i = 0; i < tmp_pl.length; i++) {
            if (tmp_pl[i] == null) parm[i] = "";
            else {
                if (tmp_pl[i] == null) parm[i] = "";
                else parm[i] = convertToSpecChar(tmp_pl[i]);//.trim());
            }
        }

        if (parm[0].equals(SMBSYNC2_PROF_TYPE_SYNC)) {//Sync
            ArrayList<FilterListAdapter.FilterListItem> ff = new ArrayList<FilterListAdapter.FilterListItem>();
            ArrayList<FilterListAdapter.FilterListItem> df = new ArrayList<FilterListAdapter.FilterListItem>();
//            ArrayList<FilterListItem> wifi_ap_list = new ArrayList<FilterListItem>();
            ArrayList<FilterListAdapter.FilterListItem> wifi_addr_list = new ArrayList<FilterListAdapter.FilterListItem>();
            ff.clear();
            if (list1.length() != 0) {//File filter
                String[] fp = list1.split("\t");
                for (int i = 0; i < fp.length; i++) {
                    String filter=convertToSpecChar(fp[i]);
                    if (filter.length()>=2) {
                        FilterListAdapter.FilterListItem fli=new FilterListAdapter.FilterListItem();
                        String filter_include=filter.substring(0,1);
                        String filter_val=filter.substring(1);
                        if (filter_include.equals("I")) fli.setInclude(true);
                        else fli.setInclude(false);
                        fli.setFilter(filter_val);
                        fli.setMigrateFromSmbsync2(true);
                        ff.add(fli);
                    }
                }
            }

            df.clear();
            if (list2.length() != 0) {//Directory filter
                String[] dp = list2.split("\t");
                for (int i = 0; i < dp.length; i++) {
                    String filter=convertToSpecChar(dp[i]);
                    if (filter.length()>=2) {
                        FilterListAdapter.FilterListItem fli=new FilterListAdapter.FilterListItem();
                        String filter_include=filter.substring(0,1);
                        String filter_val=filter.substring(1);
                        if (filter_include.equals("I")) fli.setInclude(true);
                        else fli.setInclude(false);
                        if (filter_val.startsWith(WHOLE_DIRECTORY_FILTER_PREFIX)) {
                            String filter_temp=filter_val.substring(2);
                            String reformat_filter=filter_temp.endsWith("/*")?filter_temp.substring(0, filter_temp.length()-2):filter_temp;
                            String filter_new= StringUtil.removeRedundantCharacter(reformat_filter, "/", true, true);
                            fli.setFilter(MATCH_ANY_WHERE_DIRECTORY_FILTER_PREFIX+filter_new);
                            fli.setInclude(false);
                        } else if (filter_val.startsWith(MATCH_ANY_WHERE_DIRECTORY_FILTER_PREFIX)) {
                            String filter_temp=filter_val.substring(1);
                            String reformat_filter=filter_temp.endsWith("/*")?filter_temp.substring(0, filter_temp.length()-2):filter_temp;
                            String filter_new=StringUtil.removeRedundantCharacter(reformat_filter, "/", true, true);
                            fli.setFilter(MATCH_ANY_WHERE_DIRECTORY_FILTER_PREFIX+filter_new);
                            fli.setInclude(false);
                        } else {
                            String filter_temp=filter_val;
                            String reformat_filter=filter_temp.endsWith("/*")?filter_temp.substring(0, filter_temp.length()-2):filter_temp;
                            String filter_new=StringUtil.removeRedundantCharacter(reformat_filter, "/", true, true);
                            fli.setFilter(filter_new);
                        }
                        fli.setMigrateFromSmbsync2(true);
                        fli.setEnabled(false);
                        df.add(fli);
                    }
                }
                FilterListAdapter.sort(df);
            }

            wifi_addr_list.clear();
            if (list4.length() != 0) {//Wifi IP Address list
                String[] al = list4.split("\t");
                for (int i = 0; i < al.length; i++) {
                    String filter=convertToSpecChar(al[i]);
                    if (filter.length()>=2) {
                        FilterListAdapter.FilterListItem fli=new FilterListAdapter.FilterListItem();
                        String filter_include=filter.substring(0,1);
                        String filter_val=filter.substring(1);
                        if (filter_include.equals("I")) fli.setInclude(true);
                        else fli.setInclude(false);
                        fli.setFilter(filter_val);
                        wifi_addr_list.add(fli);
                    }
                }
            }

            SyncTaskItem stli = new SyncTaskItem(parm[1].replaceAll(",", ""), parm[2].equals("0") ? false : true, false);
            String sync_task_name=parm[1];
            String unusable=SyncConfiguration.hasUnusableCharacter(sync_task_name);
            if (unusable.equals("")) {
                if (sync_task_name.length()>SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH) {
                    stli.setSyncTaskName(sync_task_name.substring(0,SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH));
                    log.info(String.format("SMBSync2 sync task name length is exceeds %s character", SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH));
                } else {
                    stli.setSyncTaskName(sync_task_name);
                }
            } else {
                stli.setSyncTaskName(sync_task_name.replaceAll(unusable, "_"));
                log.info("SMBSync2 sync task name contains unusable chanaracter : \""+unusable+"\" replaced : \"_\"");
            }
            stli.setSyncTaskType(parm[3]);

            if (parm[4].equals(SYNC_FOLDER_TYPE_INTERNAL)) {
                stli.setSourceFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
            } else if (parm[4].equals(SYNC_FOLDER_TYPE_SDCARD)) {
                stli.setSourceFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                String uuid=getUuid(c, "sd");
                stli.setSourceStorageUuid(uuid);
            } else if (parm[4].equals(SYNC_FOLDER_TYPE_USB)) {
                stli.setSourceFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                String uuid=getUuid(c, "usb");
                stli.setSourceStorageUuid(uuid);
            } else if (parm[4].equals(SYNC_FOLDER_TYPE_ZIP)) {
                stli.setSourceFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP);
            } else if (parm[4].equals(SYNC_FOLDER_TYPE_SMB)) {
                stli.setSourceFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_SMB);
                if (!auto_save) {
                    stli.setSourceSmbAccountName(parm[5]);
                    stli.setSourceSmbPassword(parm[6]);
                } else {
                    stli.setSourceFolderStatusError(stli.getSourceFolderStatusError()|SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME|SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD);
                }
            }
            stli.setSourceSmbShareName(parm[7]);
            stli.setSourceDirectoryName(parm[8]);
            stli.setSourceSmbAddr(parm[9]);
            stli.setSourceSmbPort(parm[10]);
            stli.setSourceSmbHostName(parm[11]);
            stli.setSourceSmbDomain(parm[12]);

            if (parm[13].equals(SYNC_FOLDER_TYPE_INTERNAL)) {
                stli.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
            } else if (parm[13].equals(SYNC_FOLDER_TYPE_SDCARD)) {
                stli.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                String uuid=getUuid(c, "sd");
                stli.setDestinationStorageUuid(uuid);
            } else if (parm[13].equals(SYNC_FOLDER_TYPE_USB)) {
                stli.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                String uuid=getUuid(c, "usb");
                stli.setDestinationStorageUuid(uuid);
            } else if (parm[13].equals(SYNC_FOLDER_TYPE_ZIP)) {
                stli.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP);
            } else if (parm[13].equals(SYNC_FOLDER_TYPE_SMB)) {
                stli.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_SMB);
                if (!auto_save) {
                    stli.setDestinationSmbAccountName(parm[14]);
                    stli.setDestinationSmbPassword(parm[15]);
                } else {
                    stli.setDestinationFolderStatusError(stli.getDestinationFolderStatusError()|SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME|SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD);
                }
            }
            stli.setDestinationSmbShareName(parm[16]);
            stli.setDestinationDirectoryName(parm[17]);
            stli.setDestinationSmbAddr(parm[18]);
            stli.setDestinationSmbPort(parm[19]);
            stli.setDestinationSmbHostname(parm[20]);
            stli.setDestinationSmbDomain(parm[21]);

            stli.setFileNameFilter(ff);
            stli.setDirectoryFilter(df);
//            stli.setSyncOptionWifiAccessPointWhiteList(wifi_ap_list);
            stli.setSyncOptionWifiIPAddressGrantList(wifi_addr_list);

            stli.setSyncProcessRootDirFile(parm[22].equals("1") ? true : false);
//            boolean processRootDirectoryFile=(parm[22].equals("1") ? true : false);
//            if (processRootDirectoryFile) {
//                stli.setSyncTaskErrorCode(stli.getSyncTaskErrorCode()|SyncTaskItem.SYNC_TASK_ERROR_PROCESS_ROOT_DIRECTORY_FILE);
//            }

            stli.setSyncOverrideCopyMoveFile(parm[23].equals("1") ? true : false);
            stli.setSyncConfirmOverrideOrDelete(parm[24].equals("1") ? true : false);

//            stli.setSyncDetectLastModidiedBySmbsync(parm[25].equals("1") ? true : false);

            stli.setSyncDoNotResetFileLastModified(parm[26].equals("1") ? true : false);

//            stli.setSyncOptionRetryCount(parm[27]);

            stli.setSyncOptionSyncEmptyDirectory(parm[28].equals("1") ? true : false);
            stli.setSyncOptionSyncHiddenFile(parm[29].equals("1") ? true : false);
            stli.setSyncOptionSyncHiddenDirectory(parm[30].equals("1") ? true : false);

            stli.setSyncOptionSyncSubDirectory(parm[31].equals("1") ? true : false);
            stli.setSyncOptionUseSmallIoBuffer(parm[32].equals("1") ? true : false);
            stli.setSyncTestMode(true);//parm[33].equals("1") ? true : false);
            try {stli.setSyncOptionDifferentFileAllowableTime(Integer.parseInt(parm[34]));} catch(Exception e) {}
            stli.setSyncOptionDifferentFileByTime(parm[35].equals("1") ? true : false);

            if (parm[37].equals("0")) stli.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_OFF);
            else if (parm[37].equals("1")) stli.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS);
            else if (parm[37].equals("3")) stli.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS);
            else if (parm[37].equals("4")) stli.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_IP_ADDRESS_LIST);
            else stli.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS);

//            stli.setLastSyncTime(parm[38]);
//            try {stli.setLastSyncResult(Integer.parseInt(parm[39]));} catch(Exception e) {}

            try {if (!parm[40].equals("") && !parm[40].equals(SMBSYNC2_TASK_END_MARK))stli.setSyncTaskPosition(Integer.parseInt(parm[40]));} catch(Exception e) {}

//            if (!parm[43].equals("") && !parm[43].equals(SMBSYNC2_TASK_END_MARK))
//                stli.setMasterRemovableStorageID(parm[43]);
//            if (!parm[44].equals("") && !parm[44].equals(SMBSYNC2_TASK_END_MARK))
//                stli.setTargetRemovableStorageID(parm[44]);

            if (!parm[45].equals("") && !parm[45].equals(SMBSYNC2_TASK_END_MARK))
                stli.setSyncFileTypeAudio(parm[45].equals("1") ? true : false);
            if (!parm[46].equals("") && !parm[46].equals(SMBSYNC2_TASK_END_MARK))
                stli.setSyncFileTypeImage(parm[46].equals("1") ? true : false);
            if (!parm[47].equals("") && !parm[47].equals(SMBSYNC2_TASK_END_MARK))
                stli.setSyncFileTypeVideo(parm[47].equals("1") ? true : false);

            if (!parm[48].equals("") && !parm[48].equals(SMBSYNC2_TASK_END_MARK)) {
                if (parm[48].startsWith("/")) stli.setDestinationZipOutputFileName(parm[48].substring(1));
                else stli.setDestinationZipOutputFileName(parm[48]);
            }
            if (!parm[49].equals("") && !parm[49].equals(SMBSYNC2_TASK_END_MARK)) {
                if (parm[49].equalsIgnoreCase("ULTRA")) stli.setDestinationZipCompressionLevel(SyncTaskItem.ZIP_OPTION_COMPRESSION_LEVEL_MAXIMUM);
                else stli.setDestinationZipCompressionLevel(parm[49]);
            }
            if (!parm[50].equals("") && !parm[50].equals(SMBSYNC2_TASK_END_MARK))
                stli.setDestinationZipCompressionMethod(parm[50]);
            if (!parm[51].equals("") && !parm[51].equals(SMBSYNC2_TASK_END_MARK))
                stli.setDestinationZipEncryptMethod(parm[51]);
            if (!parm[52].equals("") && !parm[52].equals(SMBSYNC2_TASK_END_MARK)) {
                if (!parm[52].equals("")) {
                    if (parm[13].equals(SYNC_FOLDER_TYPE_ZIP)) {
                        if (!auto_save) stli.setDestinationZipPassword(parm[52]);
                        else {
                            stli.setDestinationFolderStatusError(stli.getDestinationFolderStatusError()|SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ZIP_PASSWORD);
                        }
                    }
                }
            }
            if (!parm[53].equals("") && !parm[53].equals(SMBSYNC2_TASK_END_MARK))
                if (parm[53].equals("1")) stli.setSyncTaskErrorOption(SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_SKIP_NETWORK);

            if (!parm[54].equals("") && !parm[54].equals(SMBSYNC2_TASK_END_MARK))
                stli.setSyncOptionSyncWhenCharging(parm[54].equals("1") ? true : false);

            if (!parm[55].equals("") && !parm[55].equals(SMBSYNC2_TASK_END_MARK)) {
                boolean sdcard=parm[55].equals("1") ? true : false;
                if (sdcard) {
                    String uuid=getUuid(c, "sd");
                    stli.setDestinationStorageUuid(uuid);
                }
//                stli.setTargetZipUseExternalSdcard(parm[55].equals("1") ? true : false);
            }

            if (!parm[58].equals("") && !parm[58].equals(SMBSYNC2_TASK_END_MARK))
                stli.setDestinationZipFileNameEncoding(parm[58]);

            if (!parm[59].equals("") && !parm[59].equals(SMBSYNC2_TASK_END_MARK))
                stli.setSyncOptionDifferentFileBySize((parm[59].equals("1") ? true : false));

//            if (!parm[60].equals("") && !parm[60].equals(SMBSYNC2_TASK_END_MARK))
//                stli.setSyncOptionUseExtendedDirectoryFilter1((parm[60].equals("1") ? true : false));

//            if (!parm[61].equals("") && !parm[61].equals(SMBSYNC2_TASK_END_MARK))
//                stli.setMasterLocalMountPoint(parm[61]);

//            if (!parm[62].equals("") && !parm[62].equals(SMBSYNC2_TASK_END_MARK))
//                stli.setMasterLocalMountPoint(parm[62]);

            if (!parm[63].equals("") && !parm[63].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncTaskGroup(parm[63]);

            if (!parm[64].equals("") && !parm[64].equals(SMBSYNC2_TASK_END_MARK)) {
                if (parm[64].equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) stli.setSourceSmbProtocol(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1);
                else stli.setSourceSmbProtocol(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23);
            }

            if (!parm[65].equals("") && !parm[65].equals(SMBSYNC2_TASK_END_MARK)) {
                if (parm[65].equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) stli.setDestinationSmbProtocol(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1);
                else stli.setDestinationSmbProtocol(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23);
            }

//            if (!parm[66].equals("") && !parm[66].equals(SMBSYNC2_TASK_END_MARK)) stli.setMasterSmbIpcSigningEnforced((parm[66].equals("1") ? true : false));
//            if (!parm[67].equals("") && !parm[67].equals(SMBSYNC2_TASK_END_MARK)) stli.setTargetSmbIpcSigningEnforced((parm[67].equals("1") ? true : false));

            if (!parm[68].equals("") && !parm[68].equals(SMBSYNC2_TASK_END_MARK)) stli.setDestinationArchiveRenameFileTemplate(parm[68]);
//            if (!parm[69].equals("") && !parm[69].equals(SMBSYNC2_TASK_END_MARK)) stli.setArchiveUseRename((parm[69].equals("1") ? true : false));
            try {if (!parm[70].equals("") && !parm[70].equals(SMBSYNC2_TASK_END_MARK)) stli.setDestinationArchiveRetentionPeriod(Integer.parseInt(parm[70]));} catch(Exception e) {}

//            if (!parm[71].equals("") && !parm[71].equals(SMBSYNC2_TASK_END_MARK)) stli.setArchiveCreateDirectory((parm[71].equals("1") ? true : false));
            if (!parm[72].equals("") && !parm[72].equals(SMBSYNC2_TASK_END_MARK)) stli.setDestinationArchiveSuffixOption(parm[72]);

//            if (!parm[73].equals("") && !parm[73].equals(SMBSYNC2_TASK_END_MARK)) stli.setArchiveCreateDirectoryTemplate(parm[73]);
//            if (!parm[74].equals("") && !parm[74].equals(SMBSYNC2_TASK_END_MARK)) stli.setArchiveEnabled((parm[74].equals("1") ? true : false));

            if (!parm[75].equals("") && !parm[75].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncDifferentFileSizeGreaterThanTagetFile((parm[75].equals("1") ? true : false));

            if (!parm[76].equals("") && !parm[76].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionDeleteFirstWhenMirror((parm[76].equals("1") ? true : false));

            if (!parm[77].equals("") && !parm[77].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionConfirmNotExistsExifDate((parm[77].equals("1") ? true : false));

//            if (!parm[78].equals("") && !parm[78].equals(SMBSYNC2_TASK_END_MARK)) stli.setTargetZipUseUsb((parm[78].equals("1") ? true : false));

            if (!parm[79].equals("") && !parm[79].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile((parm[79].equals("1") ? true : false));

            if (!parm[80].equals("") && !parm[80].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters((parm[80].equals("1") ? true : false));

//            if (!parm[81].equals("") && !parm[81].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionDoNotUseRenameWhenSmbFileWrite((parm[81].equals("1") ? true : false));

//            if (!parm[82].equals("") && !parm[82].equals(SMBSYNC2_TASK_END_MARK)) stli.setTargetUseTakenDateTimeToDirectoryNameKeyword((parm[82].equals("1") ? true : false));
            boolean use_exif_date_time=false;
            if (!parm[82].equals("") && !parm[82].equals(SMBSYNC2_TASK_END_MARK)) use_exif_date_time=(parm[82].equals("1") ? true : false);

            if (!parm[83].equals("") && !parm[83].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncTwoWayConflictFileRule(parm[83]);
            if (!parm[84].equals("") && !parm[84].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncTwoWayKeepConflictFile((parm[84].equals("1") ? true : false));

//            if (!parm[85].equals("") && !parm[85].equals(SMBSYNC2_TASK_END_MARK)) stli.setMasterSmbUseSmb2Negotiation((parm[85].equals("1") ? true : false));
//            if (!parm[86].equals("") && !parm[86].equals(SMBSYNC2_TASK_END_MARK)) stli.setTargetSmbUseSmb2Negotiation((parm[86].equals("1") ? true : false));

            if (!parm[87].equals("") && !parm[87].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionSyncAllowGlobalIpAddress((parm[87].equals("1") ? true : false));

            if (!parm[88].equals("") && !parm[88].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty((parm[88].equals("1") ? true : false));

            if (!parm[91].equals("") && !parm[91].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionIgnoreDstDifference((parm[91].equals("1") ? true : false));
            if (!parm[92].equals("") && !parm[92].equals(SMBSYNC2_TASK_END_MARK))
                try {stli.setSyncOptionOffsetOfDst(Integer.valueOf(parm[92]));} catch(Exception e) {};

//            if (!parm[93].equals("") && !parm[93].equals(SMBSYNC2_TASK_END_MARK)) stli.setsyncOptionUseDirectoryFilterV2((parm[93].equals("1") ? true : false));

            if (!parm[94].equals("") && !parm[94].equals(SMBSYNC2_TASK_END_MARK)) stli.setSyncOptionRemoveDirectoryFileThatExcludedByFilter((parm[94].equals("1") ? true : false));

            String dir=convertDirectoryFileParameter(use_exif_date_time, stli.getDestinationDirectoryName());
            stli.setDestinationDirectoryName(dir);
            String file_template=convertDirectoryFileParameter(true, stli.getDestinationArchiveRenameFileTemplate());
            stli.setDestinationArchiveRenameFileTemplate(file_template);

//            if (stli.getDirectoryFilter().size()>0) {
//                stli.setSyncTaskErrorCode(stli.getSyncTaskErrorCode()|SyncTaskItem.SYNC_TASK_ERROR_DIRECTORY_FILTER);
//            }

            sync.add(stli);
        }
    }

    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_YEAR="%YEAR%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_MONTH="%MONTH%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_YYYYMMDD="%YYYYMMDD%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_HHMMSS="%HHMMSS%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_DAY="%DAY%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_DAY_OF_YEAR="%DAY-OF-YEAR%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_DATE="%DATE%";
    private static final String SMBSYNC2_REPLACEABLE_KEYWORD_TIME="%TIME%";

    public static final String SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY ="%WEEKDAY%";
    public static final String SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY_LONG ="%WEEKDAY_LONG%";
    public static final String SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_NUMBER ="%WEEKNO%";

    private static String convertDirectoryFileParameter(boolean exif, String from_parm) {
        String result="";
        if (exif) {
            result=from_parm.replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_YEAR, SyncTaskItem.TEMPLATE_TAKEN_YEAR)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_MONTH, SyncTaskItem.TEMPLATE_TAKEN_MONTH)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_YYYYMMDD, SyncTaskItem.TEMPLATE_TAKEN_YEAR +SyncTaskItem.TEMPLATE_TAKEN_MONTH +SyncTaskItem.TEMPLATE_TAKEN_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_HHMMSS, SyncTaskItem.TEMPLATE_TAKEN_HOUR +SyncTaskItem.TEMPLATE_TAKEN_MIN +SyncTaskItem.TEMPLATE_TAKEN_SEC)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DAY, SyncTaskItem.TEMPLATE_TAKEN_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DAY_OF_YEAR, SyncTaskItem.TEMPLATE_TAKEN_DAY_OF_YEAR)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DATE, SyncTaskItem.TEMPLATE_TAKEN_DATE)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_TIME, SyncTaskItem.TEMPLATE_TAKEN_TIME)

                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY, SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY_LONG, SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY_LONG)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_NUMBER, SyncTaskItem.TEMPLATE_TAKEN_WEEK_NUMBER)
            ;
        } else {
            result=from_parm.replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_YEAR, SyncTaskItem.TEMPLATE_EXEC_YEAR)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_MONTH, SyncTaskItem.TEMPLATE_EXEC_MONTH)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_YYYYMMDD, SyncTaskItem.TEMPLATE_EXEC_YEAR +SyncTaskItem.TEMPLATE_EXEC_MONTH +SyncTaskItem.TEMPLATE_EXEC_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_HHMMSS, SyncTaskItem.TEMPLATE_EXEC_HOUR +SyncTaskItem.TEMPLATE_EXEC_MIN +SyncTaskItem.TEMPLATE_EXEC_SEC)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DAY, SyncTaskItem.TEMPLATE_EXEC_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DAY_OF_YEAR, SyncTaskItem.TEMPLATE_EXEC_DAY_OF_YEAR)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_DATE, SyncTaskItem.TEMPLATE_EXEC_DATE)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_TIME, SyncTaskItem.TEMPLATE_EXEC_TIME)

                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY, SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_DAY_LONG, SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY_LONG)
                    .replaceAll(SMBSYNC2_REPLACEABLE_KEYWORD_WEEK_NUMBER, SyncTaskItem.TEMPLATE_EXEC_WEEK_NUMBER)
            ;
        }
        return result;
    }

    private static String convertToSpecChar(String in) {
        if (in == null || in.length() == 0) return "";
        boolean cont = true;
        String out = in;
        while (cont) {
            if (out.indexOf("\u0001") >= 0) out = out.replace("\u0001", "[");
            else cont = false;
        }

        cont = true;
        while (cont) {
            if (out.indexOf("\u0002") >= 0) out = out.replace("\u0002", "]");
            else cont = false;
        }

        return out;
    }

    private static String convertToCodeChar(String in) {
        if (in == null || in.length() == 0) return "";
        boolean cont = true;
        String out = in;
        while (cont) {
            if (out.indexOf("[") >= 0) out = out.replace("[", "\u0001");
            else cont = false;
        }

        cont = true;
        while (cont) {
            if (out.indexOf("]") >= 0) out = out.replace("]", "\u0002");
            else cont = false;
        }
        return out;
    }

}
