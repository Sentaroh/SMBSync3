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
import android.content.SharedPreferences;
import android.util.Xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.zip.CRC32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.ScheduleConstants.SCHEDULE_ENABLED_KEY;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.syncFilterFileDateTypeValueArray;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.syncFilterFileSizeTypeValueArray;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.syncFilterFileSizeUnitValueArray;

import com.sentaroh.android.SMBSync3.Log.LogUtil;
import  com.sentaroh.android.Utilities3.EncryptUtilV3.CipherParms;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.StringUtil;

public class SyncConfiguration {
    private static Logger log = LoggerFactory.getLogger(SyncConfiguration.class);

    public final static String SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX = "<!--SMBSync3 Configuration file(Do not change this file) ";
    public final static String SYNC_TASK_CONFIG_FILE_IDENTIFIER_SUFFIX = " -->";
    public final static String SYNC_TASK_ENCRYPTED_CONFIG_FILE_IDENTIFIER = "<!--SMBSync3 Encrypted Configuration file(Do not change this file)-->";
//    private final static String SYNC_TASK_CONFIG_FILE_ENCRYPT_ID = "<!--Encrypted data-->";

    private final static String SYNC_TASK_XML_TAG_FILTER_ITEM = "filter";
    private final static String SYNC_TASK_XML_TAG_FILTER_INCLUDE = "include";
    private final static String SYNC_TASK_XML_TAG_FILTER_ENABLED = "enabled";
    private final static String SYNC_TASK_XML_TAG_FILTER_MIGRATE_FROM_SMBSYNC2 = "migrate_from_smbsync2";
    private final static String SYNC_TASK_XML_TAG_FILTER_VALUE = "value";

    private final static String SYNC_TASK_XML_TAG_CONFIG = "config_list";
    private final static String SYNC_TASK_XML_TAG_CONFIG_VERSION = "version";

    private final static String SYNC_TASK_XML_TAG_TASK = "task";
    private final static String SYNC_TASK_XML_TAG_OPTION = "option";
    private final static String SYNC_TASK_XML_TAG_SOURCE = "source";
    private final static String SYNC_TASK_XML_TAG_DESTINATION = "destination";

    //    private static final String SYNC_TASK_XML_TAG_ARCHIVE_CREATE_DIRECTORY_TEMPLATE = "archive_create_directory_template";
//    private static final String SYNC_TASK_XML_TAG_ARCHIVE_CREATE_DIRECTORY = "archive_create_directory";
//    private static final String SYNC_TASK_XML_TAG_ARCHIVE_ENABLED = "archive_enabled";
    private static final String SYNC_TASK_XML_TAG_ARCHIVE_RENAME_FILE_TEMPLATE = "archive_rename_file_template";
    private static final String SYNC_TASK_XML_TAG_ARCHIVE_RETENTION_PERIOD = "archive_retention_period";
    private static final String SYNC_TASK_XML_TAG_ARCHIVE_SUFFIX_OPTION = "archive_suffix_option";
//    private static final String SYNC_TASK_XML_TAG_ARCHIVE_USE_RENAME = "archive_use_rename";
    private static final String SYNC_TASK_XML_TAG_ARCHIVE_IGNORE_SOURCE_DIRECTORY_HIEARACHY = "archive_ignore_source_directory_hiearachy";

    private static final String SYNC_TASK_XML_TAG_FILTER_DIRECTORY = "filter_directory";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_NAME = "filter_file_name";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_IGNORE_0_BYTE_FILE = "filter_file_ignore_0_byte_file_size";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_TYPE = "filter_file_size_type";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_VALUE = "filter_file_size_value";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_UNIT = "filter_file_size_unit";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_DATE_TYPE = "filter_file_date_type";
    private static final String SYNC_TASK_XML_TAG_FILTER_FILE_DATE_VALUE = "filter_file_date_value";
    private static final String SYNC_TASK_XML_TAG_FILTER_IPADDR = "filter_ipaddr";
    private static final String SYNC_TASK_XML_TAG_FILTER_SSID = "filter_ssid";

    private static final String SYNC_TASK_XML_TAG_FOLDER_DIRECTORY = "directory";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_DOMAIN = "smb_server_domain_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_NAME = "smb_server_account_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_PASSWORD = "smb_server_account_password";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_NAME = "smb_server_encrypted_account_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_PASSWORD = "smb_server_encrypted_account_password";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ADDR = "smb_server_addr";
//    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_IPC_SIGNIN_ENFORCED = "smb_server_ipc_signin_enforced";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_NAME = "smb_server_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PORT = "smb_server_port";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PROTOCOL = "smb_server_protocol";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_SHARE_NAME = "smb_server_share_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_USE_SMB2_NEGO = "smb_server_use_smb2_nego";
    private static final String SYNC_TASK_XML_TAG_FOLDER_UUID = "uuid";
    private static final String SYNC_TASK_XML_TAG_FOLDER_TYPE = "type";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_LEVEL = "zip_compress_level";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_METHOD = "zip_compress_method";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPT_METHOD = "zip_encrypt_method";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_FILE_NAME_ENCODING = "zip_file_name_encoding";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_NAME = "zip_output_file_name";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_PASSWORD = "zip_output_file_password";
    private static final String SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPTED_OUTPUT_FILE_PASSWORD = "zip_encrypted_output_file_password";

    private static final String SYNC_TASK_XML_TAG_OPTION_ALLOW_GLOBAL_IP_ADDRESS = "allow_global_ip_address";
    private static final String SYNC_TASK_XML_TAG_OPTION_ALLOWABLE_TIME_FOR_DIFFERENT_FILE = "allowable_time_for_different_file";
    private static final String SYNC_TASK_XML_TAG_OPTION_CONFIIRM_OVERRIDE_OR_DELETE = "confirm_override_or_delete";
    private static final String SYNC_TASK_XML_TAG_OPTION_CONFIRM_NOT_EXIST_EXIF_DATE = "confirm_not_exist_exif_date";
    private static final String SYNC_TASK_XML_TAG_OPTION_DELETE_EMPTY_SOURCE_DIRECTORY_WHEN_MOVE = "delete_empty_source_directory_when_move";
    private static final String SYNC_TASK_XML_TAG_OPTION_DELETE_FIRST_WHEN_MIRROR = "delete_first_when_mirror";
    private static final String SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE = "detect_differeent_file_by_size";
    private static final String SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE_IF_GT_SOURCE = "detect_differeent_file_by_size_if_gt_source";
    private static final String SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_TIME = "detect_differeent_file_by_time";
    private static final String SYNC_TASK_XML_TAG_OPTION_DO_NOT_OVERRIDE_WHEN_DESTINATION_FILE_IS_NEWER_THAN_SOURCE = "do_not_override_when_destination_file_is_newer_than_source";
    private static final String SYNC_TASK_XML_TAG_OPTION_DO_NOT_RESET_FILE_LAST_MODIFIED_TIME = "do_not_reset_file_last_modified_time";
    private static final String SYNC_TASK_XML_TAG_OPTION_IGNORE_FILE_DIRECTORY_THAT_CONTAIN_UNUSABLE_CHARACTER = "ignore_file_directory_that_contain_unusable_character";
    //    private static final String SYNC_TASK_XML_TAG_OPTION_NETWORK_ERROR_RETRY_COUNT = "network_error_retry_count";
    private static final String SYNC_TASK_XML_TAG_OPTION_OVERRIDE_FILE_WHEN_COPY_OR_MOVE = "override_file_when_copy_or_move";
    private static final String SYNC_TASK_XML_TAG_OPTION_PERFORM_SYNC_WHEN_CHARGING = "perform_sync_when_charging";
    private static final String SYNC_TASK_XML_TAG_OPTION_PROCESS_ROOT_DIRECTORY_FILE = "process_root_directory_file";
    private static final String SYNC_TASK_XML_TAG_OPTION_SMB_DESTINATION_FOLDER_USE_SMALL_BUFFER = "smb_destination_folder_use_small_buffer";
    private static final String SYNC_TASK_XML_TAG_OPTION_ERROR_OPTION = "sync_task_error_option";
    private static final String SYNC_TASK_XML_TAG_OPTION_SYNC_EMPTY_DIRECTORY = "sync_empty_directory";
    private static final String SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_DIRECTORY = "sync_hidden_directory";
    private static final String SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_FILE = "sync_hidden_file";
    private static final String SYNC_TASK_XML_TAG_OPTION_SYNC_SUB_DIRECTORY = "sync_sub_directory";
//    private static final String SYNC_TASK_XML_TAG_OPTION_DESTINATION_USE_TAKEN_DATE_DIRECTORY_NAME_KEYWORD = "sync_destination_use_taken_date_directory_name_keyword";
//    private static final String SYNC_TASK_XML_TAG_OPTION_DESTINATION_USE_TAKEN_DATE_FILE_NAME_KEYWORD = "sync_destination_use_taken_date_file_name_keyword";
    private static final String SYNC_TASK_XML_TAG_OPTION_WIFI_STATUS = "wifi_status";
    private static final String SYNC_TASK_XML_TAG_OPTION_IGNORE_DST_DIFFERENCE = "ignore_dst_difference";
    private static final String SYNC_TASK_XML_TAG_OPTION_OFFSET_OF_DST = "offset_of_dst";
    private static final String SYNC_TASK_XML_TAG_OPTION_IGNORE_SOURCE_FILE_THAT_FILE_SIZE_GT_4GB = "ignore_source_file_that_file_size_gt_4gb";
    private static final String SYNC_TASK_XML_TAG_OPTION_REMOVE_DIRECTORY_FILE_THAT_EXCLUDED_BY_FILTER = "remove_dir_file_that_excluded_by_filter";
    private static final String SYNC_TASK_XML_TAG_OPTION_MAX_DESTINATION_FILE_NAME_LENGTH = "max_destination_file_name_length";

    private static final String SYNC_TASK_XML_TAG_TASK_AUTO_TASK = "auto";
    private static final String SYNC_TASK_XML_TAG_TASK_ERROR_SOURCE = "error_source";
    private static final String SYNC_TASK_XML_TAG_TASK_ERROR_DESTINATION = "error_destination";
    private static final String SYNC_TASK_XML_TAG_TASK_GROUP_NAME = "group";
    private static final String SYNC_TASK_XML_TAG_TASK_LAST_SYNC_RESULT = "last_sync_result";
    private static final String SYNC_TASK_XML_TAG_TASK_LAST_SYNC_TIME = "last_sync_time";
    private static final String SYNC_TASK_XML_TAG_TASK_NAME = "name";
    private static final String SYNC_TASK_XML_TAG_TASK_POSITION = "position";
    private static final String SYNC_TASK_XML_TAG_TASK_TEST_MODE = "test";
    private static final String SYNC_TASK_XML_TAG_TASK_TYPE = "type";
    private static final String SYNC_TASK_XML_TAG_TASK_ERROR = "error";

    private final static String SYNC_TASK_XML_TAG_SETTINGS = "settings";
    private final static String SYNC_TASK_XML_TAG_SETTINGS_ITEM = "settings_item";
    private final static String SYNC_TASK_XML_TAG_SETTINGS_KEY = "key";
    private final static String SYNC_TASK_XML_TAG_SETTINGS_TYPE = "type";
    public final static String SYNC_TASK_XML_TAG_SETTINGS_TYPE_BOOLEAN = "Boolean";
    public final static String SYNC_TASK_XML_TAG_SETTINGS_TYPE_INT = "Integer";
    public final static String SYNC_TASK_XML_TAG_SETTINGS_TYPE_LONG = "Long";
    public final static String SYNC_TASK_XML_TAG_SETTINGS_TYPE_STRING = "String";
    private final static String SYNC_TASK_XML_TAG_SETTINGS_VALUE = "value";

    private final static String SYNC_TASK_XML_TAG_SCHEDULE = "schedule";
    private static final String SCHEDULE_XML_TAG_NAME = "name";
    private static final String SCHEDULE_XML_TAG_ENABLED = "enabled";
    private static final String SCHEDULE_XML_TAG_TYPE = "type";
    private static final String SCHEDULE_XML_TAG_DAY = "day";
    private static final String SCHEDULE_XML_TAG_HOUR = "hour";
    private static final String SCHEDULE_XML_TAG_MIN = "min";
    private static final String SCHEDULE_XML_TAG_DAY_OF_THE_WEEK = "day_of_the_week";
    private static final String SCHEDULE_XML_TAG_EXECUTE_AUTO_TASK = "execute_auto_task";
    private static final String SCHEDULE_XML_TAG_EXECUTE_TASK_LIST = "execute_task_list";
    private static final String SCHEDULE_XML_TAG_CHANGE_CHARGING_OPTION = "change_charging_option";
    private static final String SCHEDULE_XML_TAG_LAST_EXEC_TIME = "last_exec_time";
    private static final String SCHEDULE_XML_TAG_POSITION = "position";
    private static final String SCHEDULE_XML_TAG_GROUP_LIST = "group_list";
    private static final String SCHEDULE_XML_TAG_WIFI_ON_BEFORE_SYNC_BEGIN = "wifi_on_before_sync_begin";
    private static final String SCHEDULE_XML_TAG_WIFI_OFF_AFTER_SYNC_END = "wifi_off_after_sync_end";
    private static final String SCHEDULE_XML_TAG_WIFI_ON_DELAY_TIME = "wifi_on_delay_time";

    private final static String SYNC_TASK_XML_TAG_GROUP = "group";
    private static final String GROUP_XML_TAG_GROUP_NAME = "name";
    private static final String GROUP_XML_TAG_BUTTON = "button";
    private static final String GROUP_XML_TAG_ENABLED = "enabled";
    private static final String GROUP_XML_TAG_AUTO_TASK_ONLY = "auto_task_only";
    private static final String GROUP_XML_TAG_TASK_LIST = "task_list";
    private static final String GROUP_XML_TAG_POSITION = "position";

    public static String[] createConfigurationDataArray(Context c, GlobalParameters gp, CommonUtilities cu, SafFile3 sf) {
        try {
            return createConfigurationDataArray(c, gp, cu, sf.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] createConfigurationDataArray(Context c, GlobalParameters gp, CommonUtilities cu, InputStream is) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is), GENERAL_IO_BUFFER_SIZE);
            String[] config_array = new String[2];
            config_array[0] = br.readLine();
            String line="";
            StringBuilder sb=new StringBuilder(1024*1024);
            while((line=br.readLine())!=null) {
                sb.append(line);
            }
            br.close();
            config_array[1]=sb.toString();
            return config_array;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isSavedSyncTaskListFile(Context c, GlobalParameters gp, CommonUtilities cu, SafFile3 sf) {
        try {
            String[] config_array = createConfigurationDataArray(c, gp, cu, sf);
            return isSavedSyncTaskListFile(c, gp, cu, config_array);
        } catch (Exception e) {
            e.printStackTrace();
            cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" failed.\n", MiscUtil.getStackTraceString(e));
            return false;
        }
    }

    public static long calculateSyncConfigCrc32(String input) {
        CRC32 crc = new CRC32();
        crc.update((input+"SMBSync3").getBytes());
        return crc.getValue();
    }

    public static boolean isSavedSyncTaskListFile(Context c, GlobalParameters gp, CommonUtilities cu, String[] config_array) {
        boolean result = false;
        String id=SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX;//.substring(0, SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX.length()-1);
        if (config_array[0].startsWith(id)) {
            String[] parts=config_array[0].substring(SYNC_TASK_CONFIG_FILE_IDENTIFIER_PREFIX.length()).split(" ");
            long saved_crc=(parts[1].equals(""))?0:Long.parseLong(parts[0]);
            long cal_crc= calculateSyncConfigCrc32(config_array[1]);
            if (cal_crc==saved_crc) result=true;
            else {
                cu.addDebugMsg(1, "E", "isSavedSyncTaskListFile failed, CRC code unmatched.");
            }
        } else if (config_array[0].equals(SYNC_TASK_ENCRYPTED_CONFIG_FILE_IDENTIFIER)) {
            result = true;
        } else {
            cu.addDebugMsg(1, "E", "isSavedSyncTaskListFile failed, header record does not exists.");
        }
        return result;
    }

    public static boolean isSavedSyncTaskListFileEncrypted(Context c, GlobalParameters gp, CommonUtilities cu, SafFile3 sf) {
        try {
            String[] config_array = createConfigurationDataArray(c, gp, cu, sf);
            return isSavedSyncTaskListFileEncrypted(c, gp, cu, config_array);
        } catch (Exception e) {
            e.printStackTrace();
            cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" failed.\n", MiscUtil.getStackTraceString(e));
            return false;
        }

    }

    public static boolean isSavedSyncTaskListFileEncrypted(Context c, GlobalParameters gp, CommonUtilities cu, String[] config_array) {
        boolean result = false;
        if (config_array[0].equals(SYNC_TASK_ENCRYPTED_CONFIG_FILE_IDENTIFIER)) {
            if (!config_array[1].startsWith("<?xml")) {
                result = true;
            }
        }
        return result;
    }

    public static final int ENCRYPT_MODE_NO_ENCRYPT = 0;
    public static final int ENCRYPT_MODE_ENCRYPT_VITAL_DATA = 1;
    public static final int ENCRYPT_MODE_ENCRYPT_WHOLE_DATA = 2;

    public final static String CONFIG_LIST_VER1 = "1.0.1";

    synchronized public static String createXmlData(Context c, GlobalParameters gp, CommonUtilities cu,
                               ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                               ArrayList<GroupListAdapter.GroupListItem>group_list, int enc_mode, CipherParms cp_enc) {
        cu.addDebugMsg(1, "I", "buildConfigData enc_mode=" + enc_mode + ", cp_enc=" + cp_enc);
        String config_data = null;
        synchronized (sync_task_list) {
            try {
                DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dbuilder = dbfactory.newDocumentBuilder();

                Document main_document = dbuilder.newDocument();
                Element config_tag = main_document.createElement(SYNC_TASK_XML_TAG_CONFIG);
                config_tag.setAttribute(SYNC_TASK_XML_TAG_CONFIG_VERSION, CONFIG_LIST_VER1);

                for (SyncTaskItem item : sync_task_list) {
                    Element task_tag = createXmlTaskElement(c, gp, cu, main_document, item);
                    config_tag.appendChild(task_tag);

                    Element source_tag = createXmlSourceElement(c, gp, cu, main_document, item, enc_mode, cp_enc);
                    task_tag.appendChild(source_tag);

                    Element destination_tag = createXmlDestinationElement(c, gp, cu, main_document, item, enc_mode, cp_enc);
                    task_tag.appendChild(destination_tag);

                }

                for (ScheduleListAdapter.ScheduleListItem item : schedule_list) {
                    Element schedule_tag = createXmlScheduleData(c, gp, cu, main_document, item);
                    config_tag.appendChild(schedule_tag);
                }

                for (GroupListAdapter.GroupListItem item : group_list) {
                    Element group_tag = createXmlGroupData(c, gp, cu, main_document, item);
                    config_tag.appendChild(group_tag);
                }

                Element setting_tag = createXmlSettingsElement(c, gp, cu, main_document);
                config_tag.appendChild(setting_tag);

                main_document.appendChild(config_tag);

                TransformerFactory tffactory = TransformerFactory.newInstance();
                Transformer transformer = tffactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","5");
                StringWriter sw = new StringWriter();
                transformer.transform(new DOMSource(main_document), new StreamResult(sw));
                sw.flush();
                sw.close();
                String prof = sw.toString();
                if (enc_mode == ENCRYPT_MODE_ENCRYPT_WHOLE_DATA) {
                    if (prof != null) {
                        config_data = CommonUtilities.encryptUserData(c, cp_enc, prof);
                    } else {
                        cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Sync task list not saved because null CipherParms supplied.");
                    }
                } else {
                    config_data = prof;
                }
            } catch (Exception e) {
                e.printStackTrace();
                cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+"  failed.", MiscUtil.getStackTraceString(e));
                config_data=null;
            }
            cu.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName(), "  ended");
        }
        return config_data;
    }

    private static Element createXmlScheduleData(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, ScheduleListAdapter.ScheduleListItem item) {
        Element schedule_tag = main_document.createElement(SYNC_TASK_XML_TAG_SCHEDULE);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_NAME, item.scheduleName);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_ENABLED, item.scheduleEnabled ? "true" : "false");
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_TYPE, item.scheduleType);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_DAY, item.scheduleDay);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_HOUR, item.scheduleHours);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_MIN, item.scheduleMinutes);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_DAY_OF_THE_WEEK, item.scheduleDayOfTheWeek);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_EXECUTE_AUTO_TASK, item.syncAutoSyncTask ? "true" : "false");
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_EXECUTE_TASK_LIST, item.syncTaskList);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_CHANGE_CHARGING_OPTION, item.syncOverrideOptionCharge);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_LAST_EXEC_TIME, String.valueOf(item.scheduleLastExecTime));
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_POSITION, String.valueOf(item.schedulePosition));
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_GROUP_LIST, item.syncGroupList);
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_WIFI_ON_BEFORE_SYNC_BEGIN, item.syncWifiOnBeforeStart ? "true" : "false");
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_WIFI_OFF_AFTER_SYNC_END, item.syncWifiOffAfterEnd ? "true" : "false");
        schedule_tag.setAttribute(SCHEDULE_XML_TAG_WIFI_ON_DELAY_TIME, String.valueOf(item.syncDelayAfterWifiOn));
        return schedule_tag;
    }

    private static void buildSyncTaskScheduleFromXml(Context c, GlobalParameters gp, CommonUtilities cu,
                      XmlPullParser xpp, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list, ScheduleListAdapter.ScheduleListItem item) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_NAME)) {
                String unusable=hasUnusableCharacter(xpp.getAttributeValue(i));
                String schedule_name=xpp.getAttributeValue(i);
                if (unusable.equals("")) {
                    if (!ScheduleUtils.isScheduleExists(schedule_list, schedule_name)) item.scheduleName = schedule_name;
                    else {
                        cu.addDebugMsg(1, "E", "Sync task already exists : "+schedule_name);
                    }
                } else {
                    item.scheduleName = xpp.getAttributeValue(i).replaceAll(unusable, "");
                    cu.addDebugMsg(1, "E", "Schudule name contains unusable chanaracter : "+unusable+", schedule name="+schedule_name);
                }
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_ENABLED)) {
                item.scheduleEnabled = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_TYPE)) {
                item.scheduleType = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_DAY)) {
                item.scheduleDay = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_HOUR)) {
                item.scheduleHours = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_MIN)) {
                item.scheduleMinutes = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_DAY_OF_THE_WEEK)) {
                item.scheduleDayOfTheWeek = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_EXECUTE_AUTO_TASK)) {
                item.syncAutoSyncTask = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_EXECUTE_TASK_LIST)) {
                item.syncTaskList = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_CHANGE_CHARGING_OPTION)) {
                item.syncOverrideOptionCharge = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_LAST_EXEC_TIME)) {
                try {
                    item.scheduleLastExecTime = Long.valueOf(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_POSITION)) {
                try {
                    item.schedulePosition = Integer.valueOf(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_GROUP_LIST)) {
                item.syncGroupList = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_WIFI_ON_BEFORE_SYNC_BEGIN)) {
                item.syncWifiOnBeforeStart = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_WIFI_OFF_AFTER_SYNC_END)) {
                item.syncWifiOffAfterEnd = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(SCHEDULE_XML_TAG_WIFI_ON_DELAY_TIME)) {
                try {
                    item.syncDelayAfterWifiOn = Integer.valueOf(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            }
        }
    }

    private static Element createXmlGroupData(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, GroupListAdapter.GroupListItem item) {
        Element group_tag = main_document.createElement(SYNC_TASK_XML_TAG_GROUP);
        group_tag.setAttribute(GROUP_XML_TAG_GROUP_NAME, item.groupName);
        group_tag.setAttribute(GROUP_XML_TAG_ENABLED, item.enabled ? "true" : "false");
        group_tag.setAttribute(GROUP_XML_TAG_AUTO_TASK_ONLY, item.autoTaskOnly ? "true" : "false");
        group_tag.setAttribute(GROUP_XML_TAG_TASK_LIST, item.taskList);
        group_tag.setAttribute(GROUP_XML_TAG_BUTTON, String.valueOf(item.button));
        group_tag.setAttribute(GROUP_XML_TAG_POSITION, String.valueOf(item.position));
        return group_tag;
    }

    private static void buildSyncTaskGroupFromXml(Context c, GlobalParameters gp, CommonUtilities cu, XmlPullParser xpp, ArrayList<GroupListAdapter.GroupListItem> group_list, GroupListAdapter.GroupListItem item) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_GROUP_NAME)) {
                String unusable=hasUnusableCharacter(xpp.getAttributeValue(i));
                String group_name=xpp.getAttributeValue(i);
                if (unusable.equals("")) {
                    if (!GroupEditor.isGroupExists(group_list, group_name)) item.groupName = group_name;
                    else {
                        cu.addDebugMsg(1, "E", "Sync group already exists : "+group_name);
                    }
                } else {
                    item.groupName = xpp.getAttributeValue(i).trim().replaceAll(unusable, "");
                    cu.addDebugMsg(1, "E", "Group name contains unusable chanaracter : "+unusable+", group name="+group_name);
                }
            } else if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_ENABLED)) {
                item.enabled = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_AUTO_TASK_ONLY)) {
                item.autoTaskOnly = xpp.getAttributeValue(i).equals("true");
            } else if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_TASK_LIST)) {
                item.taskList = xpp.getAttributeValue(i);
            } else if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_BUTTON)) {
                try {
                    item.button = Integer.parseInt(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(GROUP_XML_TAG_POSITION)) {
                try {
                    item.position = Integer.parseInt(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            }
        }
    }

    private static Element createXmlSettingDataItemString(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, String key, String def_value) {
        Element setting_item = main_document.createElement(SYNC_TASK_XML_TAG_SETTINGS_ITEM);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_KEY, key);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_TYPE, SYNC_TASK_XML_TAG_SETTINGS_TYPE_STRING);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_VALUE, getStringSettingParameter(c, key, def_value));
        return setting_item;
    }

    private static Element createXmlSettingDataItemBoolean(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, String key, boolean def_value) {
        Element setting_item = main_document.createElement(SYNC_TASK_XML_TAG_SETTINGS_ITEM);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_KEY, key);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_TYPE, SYNC_TASK_XML_TAG_SETTINGS_TYPE_BOOLEAN);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_VALUE, getBooleanSettingParameter(c, key, def_value));
        return setting_item;
    }

    private static Element createXmlSettingDataItemInt(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, String key, int def_value) {
        Element setting_item = main_document.createElement(SYNC_TASK_XML_TAG_SETTINGS_ITEM);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_KEY, key);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_TYPE, SYNC_TASK_XML_TAG_SETTINGS_TYPE_INT);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_VALUE, getIntSettingParameter(c, key, def_value));
        return setting_item;
    }

    private static Element createXmlSettingDataItemLong(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, String key, long def_value) {
        Element setting_item = main_document.createElement(SYNC_TASK_XML_TAG_SETTINGS_ITEM);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_KEY, key);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_TYPE, SYNC_TASK_XML_TAG_SETTINGS_TYPE_LONG);
        setting_item.setAttribute(SYNC_TASK_XML_TAG_SETTINGS_VALUE, getLongSettingParameter(c, key, def_value));
        return setting_item;
    }

    private static Element createXmlSettingsElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document) {
        Element setting_tag = main_document.createElement(SYNC_TASK_XML_TAG_SETTINGS);

        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_wifi_lock), false));

        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_sync_history_log), false));
        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_force_screen_on_while_sync), false));

        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_no_compress_file_type), DEFAULT_NOCOMPRESS_FILE_TYPE));

        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_notification_message_when_sync_ended), NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS));//gp.settingNotificationMessageWhenSyncEnded);
        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_playback_ringtone_when_sync_ended), NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS));//gp.settingNotificationSoundWhenSyncEnded);
        setting_tag.appendChild(createXmlSettingDataItemInt(c, gp, cu, main_document, c.getString(R.string.settings_playback_ringtone_volume), 100));//String.valueOf(gp.settingNotificationVolume));

        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_vibrate_when_sync_ended), NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS));//gp.settingNotificationVibrateWhenSyncEnded);
        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_device_orientation_portrait), false));//gp.settingFixDeviceOrientationToPortrait?"true":"false");
        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_screen_theme), SCREEN_THEME_DEFAULT));//gp.settingScreenTheme);
        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_screen_theme_language), GlobalParameters.APPLICATION_LANGUAGE_SETTING_SYSTEM_DEFAULT));
        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_display_font_scale_factor), GlobalParameters.FONT_SCALE_FACTOR_NORMAL));

        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_smb_lm_compatibility), GlobalParameters.SMB_LM_COMPATIBILITY_DEFAULT));//gp.settingsSmbLmCompatibility);
        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_smb_use_extended_security), true));//gp.settingsSmbUseExtendedSecurity);
        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_smb_disable_plain_text_passwords), false));//gp.settingsSmbDisablePlainTextPasswords);
        setting_tag.appendChild(createXmlSettingDataItemString(c, gp, cu, main_document, c.getString(R.string.settings_smb_client_response_timeout), GlobalParameters.SMB_CLIENT_RESPONSE_TIMEOUT_DEFAULT));//gp.settingsSmbClientResponseTimeout);

        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, c.getString(R.string.settings_exit_clean), false));//gp.settingExitClean?"true":"false");

        setting_tag.appendChild(createXmlSettingDataItemBoolean(c, gp, cu, main_document, SCHEDULE_ENABLED_KEY, true));

        return setting_tag;
    }

    static private String getBooleanSettingParameter(Context c, int res_id, boolean default_value) {
        return getBooleanSettingParameter(c, c.getString(res_id), default_value);
    }

    static private String getBooleanSettingParameter(Context c, String key, boolean default_value) {
        SharedPreferences prefs = CommonUtilities.getSharedPreference(c);
        return (prefs.getBoolean(key, default_value) ? "true" : "false");
    }

    static private String getStringSettingParameter(Context c, int res_id, String default_value) {
        return getStringSettingParameter(c, c.getString(res_id), default_value);
    }

    static private String getStringSettingParameter(Context c, String key, String default_value) {
        SharedPreferences prefs = CommonUtilities.getSharedPreference(c);
        return (prefs.getString(key, default_value));
    }

    static private String getIntSettingParameter(Context c, int res_id, int default_value) {
        return getIntSettingParameter(c, c.getString(res_id), default_value);
    }

    static private String getIntSettingParameter(Context c, String key, int default_value) {
        SharedPreferences prefs = CommonUtilities.getSharedPreference(c);
        return (String.valueOf(prefs.getInt(key, default_value)));
    }

    static private String getIntSettingParameter(Context c, int res_id, long default_value) {
        return getLongSettingParameter(c, c.getString(res_id), default_value);
    }

    static private String getLongSettingParameter(Context c, String key, long default_value) {
        SharedPreferences prefs = CommonUtilities.getSharedPreference(c);
        return (String.valueOf(prefs.getLong(key, default_value)));
    }

    private static Element createXmlTaskElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, SyncTaskItem item) {
        Element task_tag = main_document.createElement(SYNC_TASK_XML_TAG_TASK);
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_AUTO_TASK, item.isSyncTaskAuto() ? "true" : "false");
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_TEST_MODE, item.isSyncTestMode() ? "true" : "false");
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_POSITION, String.valueOf(item.getSyncTaskPosition()));
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_GROUP_NAME, item.getSyncTaskGroup());
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_LAST_SYNC_RESULT, String.valueOf(item.getLastSyncResult()));
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_LAST_SYNC_TIME, item.getLastSyncTime());
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_ERROR_SOURCE, String.valueOf(item.getSourceFolderStatusError()));
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_ERROR_DESTINATION, String.valueOf(item.getDestinationFolderStatusError()));

        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_TYPE, item.getSyncTaskType());
        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_NAME, item.getSyncTaskName());

        task_tag.setAttribute(SYNC_TASK_XML_TAG_TASK_ERROR, String.valueOf(item.getSyncTaskStatusErrorCode()));

        createXmlOptionElement(c, gp, cu, main_document, task_tag, item);

        if (item.getDirectoryFilter().size() > 0)
            createXmlFilterElement(c, gp, cu, main_document, task_tag, SYNC_TASK_XML_TAG_FILTER_DIRECTORY, item.getDirectoryFilter());
        if (item.getFileNameFilter().size() > 0)
            createXmlFilterElement(c, gp, cu, main_document, task_tag, SYNC_TASK_XML_TAG_FILTER_FILE_NAME, item.getFileNameFilter());
        if (item.getSyncOptionWifiAccessPointGrantList().size() > 0)
            createXmlFilterElement(c, gp, cu, main_document, task_tag, SYNC_TASK_XML_TAG_FILTER_SSID, item.getSyncOptionWifiAccessPointGrantList());
        if (item.getSyncOptionWifiIPAddressGrantList().size() > 0)
            createXmlFilterElement(c, gp, cu, main_document, task_tag, SYNC_TASK_XML_TAG_FILTER_IPADDR, item.getSyncOptionWifiIPAddressGrantList());

        return task_tag;
    }

    private static void createXmlOptionElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, Element task_tag, SyncTaskItem item) {
        Element option_tag = main_document.createElement(SYNC_TASK_XML_TAG_OPTION);

        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_IGNORE_0_BYTE_FILE, item.isSyncOptionIgnoreFileSize0ByteFile()? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_TYPE, item.getSyncFilterFileSizeType());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_VALUE, item.getSyncFilterFileSizeValue());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_UNIT, item.getSyncFilterFileSizeUnit());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_DATE_TYPE, item.getSyncFilterFileDateType());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_FILE_DATE_VALUE, item.getSyncFilterFileDateValue());

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_ALLOW_GLOBAL_IP_ADDRESS, item.isSyncOptionSyncAllowAllIpAddress() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_CONFIIRM_OVERRIDE_OR_DELETE, item.isSyncConfirmOverrideOrDelete() ? "true" : "false");

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_CONFIRM_NOT_EXIST_EXIF_DATE, item.isSyncOptionConfirmNotExistsExifDate() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DELETE_EMPTY_SOURCE_DIRECTORY_WHEN_MOVE, item.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DELETE_FIRST_WHEN_MIRROR, item.isSyncOptionDeleteFirstWhenMirror() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE, item.isSyncOptionDifferentFileBySize() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE_IF_GT_SOURCE, item.isSyncDifferentFileSizeGreaterThanDestinationFile() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_TIME, item.isSyncOptionDifferentFileByTime() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DO_NOT_OVERRIDE_WHEN_DESTINATION_FILE_IS_NEWER_THAN_SOURCE, item.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DO_NOT_RESET_FILE_LAST_MODIFIED_TIME, item.isSyncDoNotResetFileLastModified() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_ALLOWABLE_TIME_FOR_DIFFERENT_FILE, String.valueOf(item.getSyncOptionDifferentFileAllowableTime()));
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_IGNORE_FILE_DIRECTORY_THAT_CONTAIN_UNUSABLE_CHARACTER, item.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters() ? "true" : "false");

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_OVERRIDE_FILE_WHEN_COPY_OR_MOVE, item.isSyncOverrideCopyMoveFile() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_PERFORM_SYNC_WHEN_CHARGING, item.isSyncOptionSyncWhenCharging() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_PROCESS_ROOT_DIRECTORY_FILE, item.isSyncProcessRootDirFile() ? "true" : "false");
//        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_NETWORK_ERROR_RETRY_COUNT, item.getSyncOptionRetryCount());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_SMB_DESTINATION_FOLDER_USE_SMALL_BUFFER, item.isSyncOptionUseSmallIoBuffer() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_SYNC_EMPTY_DIRECTORY, item.isSyncOptionSyncEmptyDirectory() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_DIRECTORY, item.isSyncOptionSyncHiddenDirectory() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_FILE, item.isSyncOptionSyncHiddenFile() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_SYNC_SUB_DIRECTORY, item.isSyncOptionSyncSubDirectory() ? "true" : "false");
//        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DESTINATION_USE_TAKEN_DATE_DIRECTORY_NAME_KEYWORD, item.isDestinationUseTakenDateTimeToDirectoryNameKeyword()?"true":"false");
//        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_DESTINATION_USE_TAKEN_DATE_FILE_NAME_KEYWORD, item.isDestinationUseTakenDateTimeToFileNameKeyword()?"true":"false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_WIFI_STATUS, item.getSyncOptionWifiStatusOption());
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_IGNORE_DST_DIFFERENCE, item.isSyncOptionIgnoreDstDifference() ? "true" : "false");
        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_OFFSET_OF_DST, String.valueOf(item.getSyncOptionOffsetOfDst()));

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_IGNORE_SOURCE_FILE_THAT_FILE_SIZE_GT_4GB, String.valueOf(item.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() ? "true" : "false"));

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_REMOVE_DIRECTORY_FILE_THAT_EXCLUDED_BY_FILTER, String.valueOf(item.isSyncOptionRemoveDirectoryFileThatExcludedByFilter() ? "true" : "false"));

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_MAX_DESTINATION_FILE_NAME_LENGTH, String.valueOf(item.getSyncOptionMaxDestinationFileNameLength()));

        option_tag.setAttribute(SYNC_TASK_XML_TAG_OPTION_ERROR_OPTION, item.getSyncTaskErrorOption());

        task_tag.appendChild(option_tag);
    }

    private static void createXmlFilterElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, Element task_tag, String filter_type, ArrayList<FilterListAdapter.FilterListItem> filter_list) {
        if (filter_list.size() > 0) {
            Element user_file_filter_tag = main_document.createElement(filter_type);
            for (FilterListAdapter.FilterListItem ff_item : filter_list) {
                Element user_file_filter_entry_tag = main_document.createElement(SYNC_TASK_XML_TAG_FILTER_ITEM);
                user_file_filter_entry_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_INCLUDE, ff_item.isInclude() ? "true" : "false");
                user_file_filter_entry_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_ENABLED, ff_item.isEnabled() ? "true" : "false");
                user_file_filter_entry_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_MIGRATE_FROM_SMBSYNC2, ff_item.isMigrateFromSmbsync2() ? "true" : "false");
                user_file_filter_entry_tag.setAttribute(SYNC_TASK_XML_TAG_FILTER_VALUE, ff_item.getFilter());
                user_file_filter_tag.appendChild(user_file_filter_entry_tag);
            }
            task_tag.appendChild(user_file_filter_tag);
        }
    }

    private static Element createXmlSourceElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, SyncTaskItem item, int enc_mode, CipherParms cp_int) {
        Element source_tag = main_document.createElement(SYNC_TASK_XML_TAG_SOURCE);
        source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_TYPE, item.getSourceFolderType());
        source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_DIRECTORY, item.getSourceDirectoryName());

        if (item.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_DOMAIN, item.getSourceSmbDomain());
            if (!item.getSourceSmbAccountName().equals("")) {
                if (cp_int != null && (enc_mode == ENCRYPT_MODE_ENCRYPT_VITAL_DATA)) {
                    String enc_str = CommonUtilities.encryptUserData(c, cp_int, item.getSourceSmbAccountName());
                    source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_NAME, enc_str);
                } else {
                    source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_NAME, item.getSourceSmbAccountName());
                }
            }
            if (!item.getSourceSmbAccountPassword().equals("")) {
                if (cp_int != null && (enc_mode == ENCRYPT_MODE_ENCRYPT_VITAL_DATA)) {
                    String enc_str = CommonUtilities.encryptUserData(c, cp_int, item.getSourceSmbAccountPassword());
                    source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_PASSWORD, enc_str);
                } else {
                    source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_PASSWORD, item.getSourceSmbAccountPassword());
                }
            }
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_NAME, item.getSourceSmbHost());
//            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ADDR, item.getSourceSmbAddr());
//            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_IPC_SIGNIN_ENFORCED, item.isSourceSmbIpcSigningEnforced()?"true":"false");
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PORT, item.getSourceSmbPort());
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PROTOCOL, item.getSourceSmbProtocol());
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_SHARE_NAME, item.getSourceSmbShareName());
//            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_USE_SMB2_NEGO, item.isSourceSmbUseSmb2Negotiation()?"true":"false");
        } else {
            source_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_UUID, item.getSourceStorageUuid());
        }

        return source_tag;
    }

    private static Element createXmlDestinationElement(Context c, GlobalParameters gp, CommonUtilities cu, Document main_document, SyncTaskItem item, int enc_mode, CipherParms cp_int) {
        Element destination_tag = main_document.createElement(SYNC_TASK_XML_TAG_DESTINATION);
        destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_TYPE, item.getDestinationFolderType());
        destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_DIRECTORY, item.getDestinationDirectoryName());

        if (item.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_DOMAIN, item.getDestinationSmbDomain());
            if (!item.getDestinationSmbAccountName().equals("")) {
                if (cp_int != null && (enc_mode == ENCRYPT_MODE_ENCRYPT_VITAL_DATA)) {
                    String enc_str = CommonUtilities.encryptUserData(c, cp_int, item.getDestinationSmbAccountName());
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_NAME, enc_str);
                } else {
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_NAME, item.getDestinationSmbAccountName());
                }
            }
            if (!item.getDestinationSmbPassword().equals("")) {
                if (cp_int != null && (enc_mode == ENCRYPT_MODE_ENCRYPT_VITAL_DATA)) {
                    String enc_str = CommonUtilities.encryptUserData(c, cp_int, item.getDestinationSmbPassword());
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_PASSWORD, enc_str);
                } else {
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_PASSWORD, item.getDestinationSmbPassword());
                }
            }
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ADDR, item.getDestinationSmbAddr());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_NAME, item.getDestinationSmbHost());
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_IPC_SIGNIN_ENFORCED, item.isDestinationSmbIpcSigningEnforced()?"true":"false");
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PORT, item.getDestinationSmbPort());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PROTOCOL, item.getDestinationSmbProtocol());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_SHARE_NAME, item.getDestinationSmbShareName());
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_USE_SMB2_NEGO, item.isDestinationSmbUseSmb2Negotiation()?"true":"false");
        } else {
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_UUID, item.getDestinationStorageUuid());

//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_CREATE_DIRECTORY_TEMPLATE, item.getArchiveCreateDirectoryTemplate());
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_CREATE_DIRECTORY, item.isArchiveCreateDirectory()?"true":"false");
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_ENABLED, item.isArchiveEnabled()?"true":"false");
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_RENAME_FILE_TEMPLATE, item.getDestinationArchiveRenameFileTemplate());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_RETENTION_PERIOD, String.valueOf(item.getSyncFilterArchiveRetentionPeriod()));
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_SUFFIX_OPTION, item.getDestinationArchiveSuffixOption());
//            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_USE_RENAME, item.isArchiveUseRename()?"true":"false");
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_ARCHIVE_IGNORE_SOURCE_DIRECTORY_HIEARACHY, item.isDestinationArchiveIgnoreSourceDirectory()?"true":"false");

            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_LEVEL, item.getDestinationZipCompressionLevel());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_METHOD, item.getDestinationZipCompressionMethod());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPT_METHOD, item.getDestinationZipEncryptMethod());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_FILE_NAME_ENCODING, item.getDestinationZipFileNameEncoding());
            destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_NAME, item.getDestinationZipOutputFileName());
            if (!item.getDestinationZipPassword().equals("")) {
                if (cp_int != null && (enc_mode == ENCRYPT_MODE_ENCRYPT_VITAL_DATA)) {
                    String enc_str = CommonUtilities.encryptUserData(c, cp_int, item.getDestinationZipPassword());
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPTED_OUTPUT_FILE_PASSWORD, enc_str);
                } else {
                    destination_tag.setAttribute(SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_PASSWORD, item.getDestinationZipPassword());
                }
            }
        }

        return destination_tag;
    }

    private static void putTaskListValueErrorMessage(CommonUtilities cu, String item_name, Object assumed_value) {
        String msg_template = "Invalid \"%s\" was detected while reading the task list, so \"%s\" was set to \"%s\".";
        String val = "";
        if (assumed_value instanceof String) val = (String) assumed_value;
        else if (assumed_value instanceof Integer) val = String.valueOf((Integer) assumed_value);
        else val = "?????";
        cu.addDebugMsg(1, "E", String.format(msg_template, item_name, val, item_name));
    }

    private static boolean isValidTaskItemValue(String[] valid_value, String obtained_value) {
        boolean result = false;
        for (String item : valid_value) {
            if (item.equals(obtained_value)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static boolean isValidTaskItemValue(int[] valid_value, int obtained_value) {
        boolean result = false;
        for (int item : valid_value) {
            if (item == obtained_value) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static boolean buildConfigurationList(Context c, GlobalParameters gp, CommonUtilities cu, String config_data,
                                                 ArrayList<SyncTaskItem> sync_task_list, ArrayList<ScheduleListAdapter.ScheduleListItem> schedule_list,
                                                 ArrayList<SettingParameterItem> setting_parm_list,
                                                 ArrayList<GroupListAdapter.GroupListItem> group_list,
                                                 int enc_mode, CipherParms cp_enc) {
        boolean result = true;
        try {
            sync_task_list.clear();
            XmlPullParser xpp = Xml.newPullParser();

            InputStream xpp_is = null;
            if (enc_mode == ENCRYPT_MODE_ENCRYPT_WHOLE_DATA) {
                String dec_str = CommonUtilities.decryptUserData(c, cp_enc, config_data);
                if (dec_str != null) {
//                    cu.addDebugMsg(1,"E","decrypt success, xml="+dec_str);
                    xpp.setInput(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dec_str.getBytes()))));
                } else {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" decrypt failed");
                    return false;
                }
            } else {
                xpp.setInput(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(config_data.getBytes()))));
            }

            int eventType = xpp.getEventType();
            String config_ver = "";
            SyncTaskItem sync_task_item = null;
            ArrayList<FilterListAdapter.FilterListItem> filter_list = null;
            ScheduleListAdapter.ScheduleListItem schedule_item = null;
            GroupListAdapter.GroupListItem group_item = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" Start Document, name=" + xpp.getName());
                        break;
                    case XmlPullParser.START_TAG:
                        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" Start Tag=" + xpp.getName());
                        if (xpp.getName().equals(SYNC_TASK_XML_TAG_CONFIG)) {
                            if (xpp.getAttributeCount() == 1) {
                                config_ver = xpp.getAttributeValue(0);
                                cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" Version=" + xpp.getAttributeValue(0));
                            }
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_TASK)) {
                            sync_task_item = new SyncTaskItem();
                            buildSyncTaskElementFromXml(c, gp, cu, xpp, sync_task_list, sync_task_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_OPTION)) {
                            buildSyncTaskOptionFromXml(c, gp, cu, xpp, sync_task_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_SOURCE)) {
                            buildSyncTaskSourceFolderFromXml(c, gp, cu, xpp, cp_enc, sync_task_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_DESTINATION)) {
                            buildSyncTaskDestinationFolderFromXml(c, gp, cu, xpp, cp_enc, sync_task_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_FILTER_ITEM)) {
                            boolean include = false;
                            boolean enabled = true;
                            boolean migrate_from_smbsyn2 = false;
                            String filter_val = "", filter_type = "";
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_INCLUDE)) {
                                    if (xpp.getAttributeValue(i).toLowerCase().equals("true")) include = true;
                                } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_ENABLED)) {
                                    if (xpp.getAttributeValue(i).toLowerCase().equals("false")) enabled = false;
                                } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_MIGRATE_FROM_SMBSYNC2)) {
                                    if (xpp.getAttributeValue(i).toLowerCase().equals("true")) migrate_from_smbsyn2 = true;
                                } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_VALUE)) {
                                    filter_val = xpp.getAttributeValue(i);
                                }
                            }
                            FilterListAdapter.FilterListItem fli = new FilterListAdapter.FilterListItem(filter_val, include);
                            fli.setEnabled(enabled);
                            fli.setMigrateFromSmbsync2(migrate_from_smbsyn2);
                            filter_list.add(fli);
                            cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" filter added=" + filter_list.get(filter_list.size() - 1));
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_FILTER_DIRECTORY)) {
                            filter_list = sync_task_item.getDirectoryFilter();
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_FILTER_FILE_NAME)) {
                            filter_list = sync_task_item.getFileNameFilter();
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_FILTER_SSID)) {
                            filter_list = sync_task_item.getSyncOptionWifiAccessPointGrantList();
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_FILTER_IPADDR)) {
                            filter_list = sync_task_item.getSyncOptionWifiIPAddressGrantList();
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_SCHEDULE)) {
                            schedule_item = new ScheduleListAdapter.ScheduleListItem();
                            buildSyncTaskScheduleFromXml(c, gp, cu, xpp, schedule_list, schedule_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_GROUP)) {
                            group_item = new GroupListAdapter.GroupListItem();
                            buildSyncTaskGroupFromXml(c, gp, cu, xpp, group_list, group_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_SETTINGS)) {
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_SETTINGS_ITEM)) {
                            String key = "", type = "", value = "";
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_SETTINGS_KEY))
                                    key = xpp.getAttributeValue(i);
                                else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_SETTINGS_TYPE))
                                    type = xpp.getAttributeValue(i);
                                else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_SETTINGS_VALUE))
                                    value = xpp.getAttributeValue(i);
                            }
                            if (setting_parm_list!=null) setting_parm_list.add(createSettingParmItem(c, gp, cu, key, type, value));
                        }
                        break;
                    case XmlPullParser.TEXT:
                        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" Text=" + xpp.getText() + ", name=" + xpp.getName());
                        break;
                    case XmlPullParser.END_TAG:
                        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" End Tag=" + xpp.getName());
                        if (xpp.getName().equals(SYNC_TASK_XML_TAG_TASK)) {
//                            log.trace("loadTaskList TAG="+xpp.getName());
                            sync_task_list.add(sync_task_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_SCHEDULE)) {
                            schedule_list.add(schedule_item);
                        } else if (xpp.getName().equals(SYNC_TASK_XML_TAG_GROUP)) {
                            group_list.add(group_item);
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" End Document=" + xpp.getName());
                        break;
                }
                eventType = xpp.next();
            }
            cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" End of document");
            cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" Task list size="+sync_task_list.size()+", Schedule list size="+schedule_list.size()+", Group list size="+group_list.size());

        } catch (Exception e) {
            e.printStackTrace();
            cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" failed.", MiscUtil.getStackTraceString(e));
            result = false;
        }
        return result;
    }


    private static SettingParameterItem createSettingParmItem(Context c, GlobalParameters gp, CommonUtilities cu, String key, String type, String value) {
        SettingParameterItem sp = new SettingParameterItem();
        sp.key = key;
        sp.type = type;
        sp.value = value;
        cu.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName()+" key=" + key + ", type=" + type + ", valu=" + value);
        return sp;
    }

    private static SettingParameterItem createSettingParmItem(Context c, GlobalParameters gp, CommonUtilities cu, int key_res_id, String type, String value) {
        return createSettingParmItem(c, gp, cu, c.getString(key_res_id), type, value);
    }

    public static String hasUnusableCharacter(String input) {
        for(String item:NAME_UNUSABLE_CHARACTER) {
            if (input.contains(item)) return item;
        }
        return "";
    }

    private static void buildSyncTaskElementFromXml(Context c, GlobalParameters gp, CommonUtilities cu,
                                                    XmlPullParser xpp, ArrayList<SyncTaskItem> task_list, SyncTaskItem sti) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_NAME)) {
                String unusable=hasUnusableCharacter(xpp.getAttributeValue(i));
                String task_name=xpp.getAttributeValue(i);
                if (unusable.equals("")) {
                    if (!TaskListUtils.isSyncTaskExists(task_name, task_list)) sti.setSyncTaskName(xpp.getAttributeValue(i));
                    else {
                        cu.addDebugMsg(1, "E", "Sync task already exists : "+task_name);
                    }
                } else {
                    sti.setSyncTaskName(xpp.getAttributeValue(i).replaceAll(unusable, ""));
                    cu.addDebugMsg(1, "E", "Sync task name contains unusable chanaracter : "+unusable+", task name="+task_name);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_TYPE)) {
                if (isValidTaskItemValue(SyncTaskItem.SYNC_TASK_TYPE_LIST, xpp.getAttributeValue(i))) {
                    sti.setSyncTaskType(xpp.getAttributeValue(i));
                } else {
                    sti.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Sync task type", SyncTaskItem.SYNC_TASK_TYPE_DEFAULT_DESCRIPTION);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_ERROR)) {
                try {
                    sti.setSyncTaskStatusErrorCode(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid error code=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_AUTO_TASK)) {
                sti.setSyncTaskAuto(xpp.getAttributeValue(i).toLowerCase().equals("true") ? true : false);
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_TEST_MODE)) {
                sti.setSyncTestMode(xpp.getAttributeValue(i).toLowerCase().equals("true") ? true : false);
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_POSITION)) {
                try {
                    sti.setSyncTaskPosition(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid sync task position vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_GROUP_NAME)) {
                sti.setSyncTaskGroup(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_LAST_SYNC_RESULT)) {
                try {
                    sti.setLastSyncResult(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_LAST_SYNC_TIME)) {
                sti.setLastSyncTime(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_ERROR_SOURCE)) {
                try {
                    sti.setSourceFolderStatusError(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid source folder error vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_TASK_ERROR_DESTINATION)) {
                try {
                    sti.setDestinationFolderStatusError(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid destination folder error vallue=" + xpp.getAttributeValue(i));
                }
            }
        }
    }

    private static void buildSyncTaskOptionFromXml(Context c, GlobalParameters gp, CommonUtilities cu,
                                                   XmlPullParser xpp, SyncTaskItem sti) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_IGNORE_0_BYTE_FILE)) {
                sti.setSyncOptionIgnoreFileSize0ByteFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_TYPE)) {
                if (isValidTaskItemValue(syncFilterFileSizeTypeValueArray, xpp.getAttributeValue(i))) {
                    sti.setSyncFilterFileSizeType(xpp.getAttributeValue(i));
                } else {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Filter file size type error. Vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_VALUE)) {
                try {
                    int val=Integer.parseInt(xpp.getAttributeValue(i));
                    sti.setSyncFilterFileSizeValue(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Filter file size value error. Vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_SIZE_UNIT)) {
                if (isValidTaskItemValue(syncFilterFileSizeUnitValueArray, xpp.getAttributeValue(i))) {
                    sti.setSyncFilterFileSizeUnit(xpp.getAttributeValue(i));
                } else {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Filter file size unit error. Vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_DATE_TYPE)) {
                if (isValidTaskItemValue(syncFilterFileDateTypeValueArray, xpp.getAttributeValue(i))) {
                    sti.setSyncFilterFileDateType(xpp.getAttributeValue(i));
                } else {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Filter file date type error. Vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FILTER_FILE_DATE_VALUE)) {
                try {
                    int val=Integer.parseInt(xpp.getAttributeValue(i));
                    sti.setSyncFilterFileDateValue(xpp.getAttributeValue(i));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Filter file date value error. Vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_ALLOW_GLOBAL_IP_ADDRESS)) {
                sti.setSyncOptionSyncAllowAllIpAddress(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_CONFIIRM_OVERRIDE_OR_DELETE)) {
                sti.setSyncConfirmOverrideOrDelete(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_CONFIRM_NOT_EXIST_EXIF_DATE)) {
                sti.setSyncOptionConfirmNotExistsExifDate(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DELETE_EMPTY_SOURCE_DIRECTORY_WHEN_MOVE)) {
                sti.setSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DELETE_FIRST_WHEN_MIRROR)) {
                sti.setSyncOptionDeleteFirstWhenMirror(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE)) {
                sti.setSyncOptionDifferentFileBySize(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_SIZE_IF_GT_SOURCE)) {
                sti.setSyncDifferentFileSizeGreaterThanTagetFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DETECT_DIFFERENT_FILE_BY_TIME)) {
                sti.setSyncOptionDifferentFileByTime(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DO_NOT_OVERRIDE_WHEN_DESTINATION_FILE_IS_NEWER_THAN_SOURCE)) {
                sti.setSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_DO_NOT_RESET_FILE_LAST_MODIFIED_TIME)) {
                sti.setSyncDoNotResetFileLastModified(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_ALLOWABLE_TIME_FOR_DIFFERENT_FILE)) {
                try {
                    if (isValidTaskItemValue(SyncTaskItem.SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_LIST, Integer.valueOf(xpp.getAttributeValue(i)))) {
                        sti.setSyncOptionDifferentFileAllowableTime(Integer.valueOf(xpp.getAttributeValue(i)));
                    } else {
                        sti.setSyncOptionDifferentFileAllowableTime(SyncTaskItem.SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT);
                        putTaskListValueErrorMessage(cu, "Min allowed time", String.valueOf(SyncTaskItem.SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT));
                    }
                } catch (Exception e) {
                    sti.setSyncOptionDifferentFileAllowableTime(SyncTaskItem.SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Min allowed time", String.valueOf(SyncTaskItem.SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_IGNORE_FILE_DIRECTORY_THAT_CONTAIN_UNUSABLE_CHARACTER)) {
                sti.setSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_OVERRIDE_FILE_WHEN_COPY_OR_MOVE)) {
                sti.setSyncOverrideCopyMoveFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_PERFORM_SYNC_WHEN_CHARGING)) {
                sti.setSyncOptionSyncWhenCharging(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_PROCESS_ROOT_DIRECTORY_FILE)) {
                sti.setSyncProcessRootDirFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
//            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_NETWORK_ERROR_RETRY_COUNT)) {
//                sti.setSyncOptionRetryCount(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_SMB_DESTINATION_FOLDER_USE_SMALL_BUFFER)) {
                sti.setSyncOptionUseSmallIoBuffer(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_SYNC_EMPTY_DIRECTORY)) {
                sti.setSyncOptionSyncEmptyDirectory(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_DIRECTORY)) {
                sti.setSyncOptionSyncHiddenDirectory(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_SYNC_HIDDEN_FILE)) {
                sti.setSyncOptionSyncHiddenFile(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_SYNC_SUB_DIRECTORY)) {
                sti.setSyncOptionSyncSubDirectory(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_WIFI_STATUS)) {
                if (isValidTaskItemValue(SyncTaskItem.WIFI_STATUS_WIFI_LIST, xpp.getAttributeValue(i))) {
                    sti.setSyncOptionWifiStatusOption(xpp.getAttributeValue(i));
                } else {
                    sti.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP);
                    putTaskListValueErrorMessage(cu, "WiFi status option", String.valueOf(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_IGNORE_DST_DIFFERENCE)) {
                sti.setSyncOptionIgnoreDstDifference(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_OFFSET_OF_DST)) {
                try {
                    if (isValidTaskItemValue(SyncTaskItem.SYNC_OPTION_OFFSET_OF_DST_LIST, Integer.valueOf(xpp.getAttributeValue(i)))) {
                        sti.setSyncOptionOffsetOfDst(Integer.valueOf(xpp.getAttributeValue(i)));
                    } else {
                        sti.setSyncOptionOffsetOfDst(SyncTaskItem.SYNC_OPTION_OFFSET_OF_DST_DEFAULT);
                        putTaskListValueErrorMessage(cu, "Offset of DST", String.valueOf(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP));
                    }
                } catch (Exception e) {
                    sti.setSyncOptionOffsetOfDst(SyncTaskItem.SYNC_OPTION_OFFSET_OF_DST_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Offset of DST", String.valueOf(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_IGNORE_SOURCE_FILE_THAT_FILE_SIZE_GT_4GB)) {
                sti.setSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_REMOVE_DIRECTORY_FILE_THAT_EXCLUDED_BY_FILTER)) {
                sti.setSyncOptionRemoveDirectoryFileThatExcludedByFilter(xpp.getAttributeValue(i).toLowerCase().equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_MAX_DESTINATION_FILE_NAME_LENGTH)) {
                sti.setSyncOptionMaxDestinationFileNameLength(Integer.parseInt(xpp.getAttributeValue(i)));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_OPTION_ERROR_OPTION)) {
                sti.setSyncTaskErrorOption(xpp.getAttributeValue(i));
            }
        }
    }

    private static void buildSyncTaskSourceFolderFromXml(Context c, GlobalParameters gp, CommonUtilities cu,
                                                         XmlPullParser xpp, CipherParms cp_int, SyncTaskItem sti) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_TYPE)) {
                sti.setSourceFolderType(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_DIRECTORY)) {
                sti.setSourceDirectoryName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_DOMAIN)) {
                sti.setSourceSmbDomain(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_NAME)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    String dec_str = CommonUtilities.decryptUserData(c, cp_int, xpp.getAttributeValue(i));
                    if (dec_str == null)
                        sti.setSourceFolderStatusError(sti.getSourceFolderStatusError() | SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME);
                    else sti.setSourceSmbAccountName(dec_str);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_NAME)) {
                sti.setSourceSmbAccountName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_PASSWORD)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    String dec_str = CommonUtilities.decryptUserData(c, cp_int, xpp.getAttributeValue(i));
                    if (dec_str == null)
                        sti.setSourceFolderStatusError(sti.getSourceFolderStatusError() | SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD);
                    else sti.setSourceSmbPassword(dec_str);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_PASSWORD)) {
                sti.setSourceSmbPassword(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ADDR)) {
                if (sti.getSourceSmbHost().equals("")) sti.setSourceSmbHost(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_NAME)) {
                if (sti.getSourceSmbHost().equals("")) sti.setSourceSmbHost(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PORT)) {
                try {
                    if (!SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT.equals(xpp.getAttributeValue(i))) {
                        int port = Integer.parseInt(xpp.getAttributeValue(i));
                        sti.setSourceSmbPort(xpp.getAttributeValue(i));
                    } else {
                        sti.setSourceSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT);
                    }
                } catch (Exception e) {
                    sti.setSourceSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Source SMB port number", String.valueOf(SyncTaskItem.SYNC_FOLDER_SMB_PORT_DEFAULT_DESCRIPTION));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PROTOCOL)) {
                if (isValidTaskItemValue(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_LIST, xpp.getAttributeValue(i))) {
                    sti.setSourceSmbProtocol(xpp.getAttributeValue(i));
                } else {
                    sti.setSourceSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Source SMB protocol", String.valueOf(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_SHARE_NAME)) {
                sti.setSourceSmbShareName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_UUID)) {
                sti.setSourceStorageUuid(xpp.getAttributeValue(i));
            }
        }
    }

    private static void buildSyncTaskDestinationFolderFromXml(Context c, GlobalParameters gp, CommonUtilities cu,
                                                              XmlPullParser xpp, CipherParms cp_int, SyncTaskItem sti) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_TYPE)) {
                sti.setDestinationFolderType(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_DIRECTORY)) {
                sti.setDestinationDirectoryName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_DOMAIN)) {
                sti.setDestinationSmbDomain(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_NAME)) {
                sti.setDestinationSmbAccountName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_NAME)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    String dec_str = CommonUtilities.decryptUserData(c, cp_int, xpp.getAttributeValue(i));
                    if (dec_str == null)
                        sti.setDestinationFolderStatusError(sti.getDestinationFolderStatusError() | SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME);
                    else sti.setDestinationSmbAccountName(dec_str);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ACCOUNT_PASSWORD)) {
                sti.setDestinationSmbPassword(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ENCRYPTED_ACCOUNT_PASSWORD)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    String dec_str = CommonUtilities.decryptUserData(c, cp_int, xpp.getAttributeValue(i));
                    if (dec_str == null)
                        sti.setDestinationFolderStatusError(sti.getDestinationFolderStatusError() | SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD);
                    else sti.setDestinationSmbPassword(dec_str);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_ADDR)) {
                if (sti.getDestinationSmbHost().equals("")) sti.setDestinationSmbHost(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_NAME)) {
                if (sti.getDestinationSmbHost().equals("")) sti.setDestinationSmbHost(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PORT)) {
                try {
                    if (!SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT.equals(xpp.getAttributeValue(i))) {
                        int port = Integer.parseInt(xpp.getAttributeValue(i));
                        sti.setDestinationSmbPort(xpp.getAttributeValue(i));
                    } else {
                        sti.setDestinationSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT);
                    }
                } catch (Exception e) {
                    sti.setDestinationSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Destination SMB port number", String.valueOf(SyncTaskItem.SYNC_FOLDER_SMB_PORT_DEFAULT_DESCRIPTION));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_PROTOCOL)) {
                if (isValidTaskItemValue(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_LIST, xpp.getAttributeValue(i))) {
                    sti.setDestinationSmbProtocol(xpp.getAttributeValue(i));
                } else {
                    sti.setDestinationSmbPort(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT);
                    putTaskListValueErrorMessage(cu, "Destination SMB protocol", String.valueOf(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT));
                }

            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_SMB_SERVER_SHARE_NAME)) {
                sti.setDestinationSmbShareName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_UUID)) {
                sti.setDestinationStorageUuid(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_ARCHIVE_RENAME_FILE_TEMPLATE)) {
                sti.setDestinationArchiveRenameFileTemplate(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_ARCHIVE_RETENTION_PERIOD)) {
                try {
                    sti.setSyncFilterArchiveRetentionPeriod(Integer.valueOf(xpp.getAttributeValue(i)));
                } catch (Exception e) {
                    cu.addDebugMsg(1, "E", CommonUtilities.getExecutedMethodName()+" Invalid Archive retention vallue=" + xpp.getAttributeValue(i));
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_ARCHIVE_SUFFIX_OPTION)) {
                sti.setDestinationArchiveSuffixOption(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_ARCHIVE_IGNORE_SOURCE_DIRECTORY_HIEARACHY)) {
                sti.setDestinationArchiveIgnoreSourceDirectory(xpp.getAttributeValue(i).equals("true"));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_LEVEL)) {
                sti.setDestinationZipCompressionLevel(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_COMPRESS_METHOD)) {
                sti.setDestinationZipCompressionMethod(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPT_METHOD)) {
                sti.setDestinationZipEncryptMethod(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_FILE_NAME_ENCODING)) {
                sti.setDestinationZipFileNameEncoding(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_NAME)) {
                sti.setDestinationZipOutputFileName(xpp.getAttributeValue(i));
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_ENCRYPTED_OUTPUT_FILE_PASSWORD)) {
                if (!xpp.getAttributeValue(i).equals("")) {
                    String dec_str = CommonUtilities.decryptUserData(c, cp_int, xpp.getAttributeValue(i));
                    if (dec_str == null)
                        sti.setDestinationFolderStatusError(sti.getDestinationFolderStatusError() | SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ZIP_PASSWORD);
                    else sti.setDestinationZipPassword(dec_str);
                }
            } else if (xpp.getAttributeName(i).equals(SYNC_TASK_XML_TAG_FOLDER_ZIP_OUTPUT_FILE_PASSWORD)) {
                sti.setDestinationZipPassword(xpp.getAttributeValue(i));
            }
        }
    }

    public static class SettingParameterItem {
        public String key="";
        public String type="";
        public String value="";
    }
}
