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

import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.Utilities3.SafManager3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TimeZone;

class SyncTaskItem implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private static Logger log= LoggerFactory.getLogger(SyncTaskItem.class);

    public SyncTaskItem(String stn, boolean pfa, boolean ic) {
        syncTasｋName = stn;
        syncTaskEnabled = pfa;
        isChecked = ic;
        initOffsetOfDst();
    }

    public SyncTaskItem() {
        initOffsetOfDst();
    }

    private void initOffsetOfDst() {
        if (TimeZone.getDefault().useDaylightTime()) setSyncOptionOffsetOfDst(TimeZone.getDefault().getDSTSavings()/(60*1000));
        else setSyncOptionOffsetOfDst(60);
    }

    public static final int SYNC_TASK_NAME_MAX_LENGTH=64;
    private String syncTasｋName = "";
    public String getSyncTaskName() {return syncTasｋName;}
    public void setSyncTaskName(String p) {syncTasｋName = p;}

    public final static String SYNC_TASK_TYPE_MIRROR = "M"; // index 0 (Indexes position from SYNC_TASK_TYPE_LIST not used for now)
    public final static String SYNC_TASK_TYPE_COPY = "C"; // index 1
    public final static String SYNC_TASK_TYPE_MOVE = "X"; // index 2
    public final static String SYNC_TASK_TYPE_SYNC = "S"; // index 3
    public final static String SYNC_TASK_TYPE_ARCHIVE = "A"; // index 4
    public final static String SYNC_TASK_TYPE_DEFAULT = SYNC_TASK_TYPE_COPY;
    public final static String SYNC_TASK_TYPE_DEFAULT_DESCRIPTION = "COPY";
    public final static String[] SYNC_TASK_TYPE_LIST=new String[]{SYNC_TASK_TYPE_MIRROR, SYNC_TASK_TYPE_COPY, SYNC_TASK_TYPE_MOVE, SYNC_TASK_TYPE_SYNC, SYNC_TASK_TYPE_ARCHIVE};
    private String syncTaskType = SYNC_TASK_TYPE_DEFAULT;
    public String getSyncTaskType() {return syncTaskType;}
    public void setSyncTaskType(String p) {
        syncTaskType = p;}

    private boolean syncTaskEnabled = true;
    public void setSyncTaskAuto(boolean p) {syncTaskEnabled = p;}
    public boolean isSyncTaskAuto() {return syncTaskEnabled;}

    private String syncTaskGroup = "";
    public String getSyncTaskGroup() {return syncTaskGroup;}
    public void setSyncTaskGroup(String grp_name) {syncTaskGroup = grp_name;}

    private boolean isChecked = false;
    public boolean isChecked() {return isChecked;}
    public void setChecked(boolean p) {isChecked = p;}

    private boolean syncOptionSyncTestMode = false;
    public boolean isSyncTestMode() {return syncOptionSyncTestMode;}
    public void setSyncTestMode(boolean p) {syncOptionSyncTestMode = p;}

    private int syncTaskPosition = 0;
    public int getSyncTaskPosition() {return syncTaskPosition;}
    public void setSyncTaskPosition(int p) {syncTaskPosition = p;}

    public final static String SYNC_FOLDER_TYPE_LOCAL = "LOCAL";
    public final static String SYNC_FOLDER_TYPE_SMB = "SMB";
    public final static String SYNC_FOLDER_TYPE_ZIP = "ZIP";
    public final static String SYNC_FOLDER_TYPE_DEFAULT = SYNC_FOLDER_TYPE_LOCAL;
    public final static String SYNC_FOLDER_TYPE_DEFAULT_DESCRIPTION = SYNC_FOLDER_TYPE_LOCAL;
    public final static String[] SYNC_FOLDER_TYPE_LIST=new String[]{SYNC_FOLDER_TYPE_LOCAL, SYNC_FOLDER_TYPE_ZIP, SYNC_FOLDER_TYPE_SMB};
    private String syncTaskSourceFolderType = SYNC_FOLDER_TYPE_LOCAL;
    public String getSourceFolderType() {return syncTaskSourceFolderType;}
    public void setSourceFolderType(String p) {syncTaskSourceFolderType = p;}

    public final static String SYNC_TASK_TWO_WAY_OPTION_ASK_USER = "0";
    public final static String SYNC_TASK_TWO_WAY_OPTION_COPY_NEWER = "1";
    public final static String SYNC_TASK_TWO_WAY_OPTION_COPY_OLDER = "2";
    public final static String SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_SOURCE_TO_DESTINATION = "3";
    public final static String SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_DESTINATION_TO_SOURCE = "4";
    public final static String SYNC_TASK_TWO_WAY_OPTION_SKIP_SYNC_FILE = "5";
    public final static String SYNC_TASK_TWO_WAY_OPTION_DEFAULT = SYNC_TASK_TWO_WAY_OPTION_ASK_USER;
    public final static String SYNC_TASK_TWO_WAY_OPTION_DEFAULT_DESCRIPTION = "Ask to user";
    public final static String[] SYNC_TASK_TWO_WAY_OPTION_LIST=new String[]{
            SYNC_TASK_TWO_WAY_OPTION_ASK_USER, SYNC_TASK_TWO_WAY_OPTION_COPY_NEWER, SYNC_TASK_TWO_WAY_OPTION_COPY_OLDER,
            SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_SOURCE_TO_DESTINATION, SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_DESTINATION_TO_SOURCE,
            SYNC_TASK_TWO_WAY_OPTION_SKIP_SYNC_FILE};
    public final static String SYNC_TASK_TWO_WAY_CONFLICT_FILE_SUFFIX=".smbsync3_conflict";
    private String syncTwoWayConflictOption =SYNC_TASK_TWO_WAY_OPTION_COPY_NEWER;
    public void setSyncTwoWayConflictFileRule(String p) {syncTwoWayConflictOption = p;}
    public String getSyncTwoWayConflictFileRule() {return syncTwoWayConflictOption;}

    private boolean syncTwoWayConflictKeepConflictFile = false;
    public void setSyncTwoWayKeepConflictFile(boolean keep_file) {syncTwoWayConflictKeepConflictFile=keep_file;}
    public boolean isSyncTwoWayKeepConflictFile() {return syncTwoWayConflictKeepConflictFile;}

    private boolean syncOptionDeterminChangedFileSizeGreaterThanDestinationFile = false;
    public boolean isSyncDifferentFileSizeGreaterThanDestinationFile() {return syncOptionDeterminChangedFileSizeGreaterThanDestinationFile;}
    public void setSyncDifferentFileSizeGreaterThanTagetFile(boolean p) {syncOptionDeterminChangedFileSizeGreaterThanDestinationFile = p;}

    private ArrayList<FilterListAdapter.FilterListItem> syncFileNameFilter = new ArrayList<FilterListAdapter.FilterListItem>();
    public ArrayList<FilterListAdapter.FilterListItem> getFileNameFilter() {return syncFileNameFilter;}
    public void setFileNameFilter(ArrayList<FilterListAdapter.FilterListItem> p) {syncFileNameFilter = p;}

    private ArrayList<FilterListAdapter.FilterListItem> syncDirectoryFilter = new ArrayList<FilterListAdapter.FilterListItem>();
    public ArrayList<FilterListAdapter.FilterListItem> getDirectoryFilter() {return syncDirectoryFilter;}
    public void setDirectoryFilter(ArrayList<FilterListAdapter.FilterListItem> p) {syncDirectoryFilter = p;}

    private boolean syncOptionRootDirFileToBeProcessed = true;
    public boolean isSyncProcessRootDirFile() {return syncOptionRootDirFileToBeProcessed;}
    public void setSyncProcessRootDirFile(boolean p) {syncOptionRootDirFileToBeProcessed = p;}

    private boolean syncOptionProcessOverrideCopyMove = true;
    public boolean isSyncOverrideCopyMoveFile() {return syncOptionProcessOverrideCopyMove;}
    public void setSyncOverrideCopyMoveFile(boolean p) {syncOptionProcessOverrideCopyMove = p;}

    private boolean syncOptionConfirmOverrideDelete = true;
    public boolean isSyncConfirmOverrideOrDelete() {return syncOptionConfirmOverrideDelete;}
    public void setSyncConfirmOverrideOrDelete(boolean p) {syncOptionConfirmOverrideDelete = p;}


    private boolean syncOptionNotUsedLastModifiedForRemote = false;
    public boolean isSyncDoNotResetFileLastModified() {return syncOptionNotUsedLastModifiedForRemote;}
    public void setSyncDoNotResetFileLastModified(boolean p) {syncOptionNotUsedLastModifiedForRemote = p;}

    private boolean syncFileTypeAudio = false;
    public boolean isSyncFileTypeAudio() {return syncFileTypeAudio;}
    public void setSyncFileTypeAudio(boolean p) {syncFileTypeAudio = p;}

    private boolean syncFileTypeImage = false;
    public boolean isSyncFileTypeImage() {return syncFileTypeImage;}
    public void setSyncFileTypeImage(boolean p) {syncFileTypeImage = p;}

    private boolean syncFileTypeVideo = false;
    public boolean isSyncFileTypeVideo() {return syncFileTypeVideo;}
    public void setSyncFileTypeVideo(boolean p) {syncFileTypeVideo = p;}

    public final static int NETWORK_ERROR_RETRY_COUNT=3;
    public final static int NETWORK_ERROR_RETRY_COUNT_DEFAULT =NETWORK_ERROR_RETRY_COUNT;
    public final static int NETWORK_ERROR_RETRY_COUNT_DEFAULT_DESCRIPTION =NETWORK_ERROR_RETRY_COUNT_DEFAULT;
    public final static int[] NETWORK_ERROR_RETRY_COUNT_LIST=new int[]{NETWORK_ERROR_RETRY_COUNT_DEFAULT};
    private int syncOptionRetryCount = NETWORK_ERROR_RETRY_COUNT_DEFAULT;

    public int getSyncOptionRetryCount() {return syncOptionRetryCount;}
    public void setSyncOptionRetryCount(int count) {syncOptionRetryCount = count;}

    private boolean syncOptionSyncEmptyDir = true;
    public boolean isSyncOptionSyncEmptyDirectory() {return syncOptionSyncEmptyDir;}
    public void setSyncOptionSyncEmptyDirectory(boolean p) {syncOptionSyncEmptyDir = p;}

    private boolean syncOptionSyncHiddenFile = true;
    public boolean isSyncOptionSyncHiddenFile() {return syncOptionSyncHiddenFile;}
    public void setSyncOptionSyncHiddenFile(boolean p) {syncOptionSyncHiddenFile = p;}

    private boolean syncOptionSyncHiddenDir = true;
    public boolean isSyncOptionSyncHiddenDirectory() {return syncOptionSyncHiddenDir;}
    public void setSyncOptionSyncHiddenDirectory(boolean p) {syncOptionSyncHiddenDir = p;}

    private boolean syncOptionSyncSubDir = true;
    public boolean isSyncOptionSyncSubDirectory() {return syncOptionSyncSubDir;}
    public void setSyncOptionSyncSubDirectory(boolean p) {syncOptionSyncSubDir = p;}

    private boolean syncOptionCreateDestinationDirectoryIfDoesNotExist = true;
    public boolean isSyncOptionCreateDestinationDirectoryIfDoesNotExist() {return syncOptionCreateDestinationDirectoryIfDoesNotExist;}
    public void setSyncOptionCreateDestinationDirectoryIfDoesNotExist(boolean p) {syncOptionCreateDestinationDirectoryIfDoesNotExist = p;}

    private boolean syncOptionUseSmallIoBuffer = false;
    public boolean isSyncOptionUseSmallIoBuffer() {return syncOptionUseSmallIoBuffer;}
    public void setSyncOptionUseSmallIoBuffer(boolean p) {syncOptionUseSmallIoBuffer = p;}

    private boolean syncOptionDeterminChangedFileBySize = true;
    public boolean isSyncOptionDifferentFileBySize() {return syncOptionDeterminChangedFileBySize;}
    public void setSyncOptionDifferentFileBySize(boolean p) {syncOptionDeterminChangedFileBySize = p;}

    private boolean syncOptionDeterminChangedFileByTime = true;
    public boolean isSyncOptionDifferentFileByTime() {return syncOptionDeterminChangedFileByTime;}
    public void setSyncOptionDifferentFileByTime(boolean p) {syncOptionDeterminChangedFileByTime = p;}

    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_0=0;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_1=1;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_2=3;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_3=5;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_4=10;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT=SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_2;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT_INDEX=2;
    public final static int SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT_DESCRIPTION=SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT;
    public final static int[] SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_LIST =new int[]{
            SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_0, SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_1, SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_2,
            SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_3, SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_4};
    private int syncOptionDeterminChangedFileByTimeValue = SYNC_FILE_DIFFERENCE_ALLOWABLE_TIME_DEFAULT;//Seconds
    public int getSyncOptionDifferentFileAllowableTime() {return syncOptionDeterminChangedFileByTimeValue;}
    public void setSyncOptionDifferentFileAllowableTime(int p) {syncOptionDeterminChangedFileByTimeValue = p;}

    public final static String WIFI_STATUS_WIFI_OFF = "OFF"; // list index 0
    public final static String WIFI_STATUS_WIFI_CONNECT_ANY_AP = "ANY_AP"; // list index 1
    public final static String WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS = "PRIVATE_IP_ADDRESS"; // list index 2
    public final static String WIFI_STATUS_WIFI_IP_ADDRESS_LIST = "IP_ADDRESS_LIST"; // list index 3
    public final static String WIFI_STATUS_WIFI_DEFAULT = WIFI_STATUS_WIFI_CONNECT_ANY_AP;
    public final static String WIFI_STATUS_WIFI_DEFAULT_DESCRIPTION = "Conn any AP";
    public final static String[] WIFI_STATUS_WIFI_LIST = new String[]{WIFI_STATUS_WIFI_OFF , WIFI_STATUS_WIFI_CONNECT_ANY_AP,
            WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS, WIFI_STATUS_WIFI_IP_ADDRESS_LIST};
    private String syncOptionWifiStatus = WIFI_STATUS_WIFI_DEFAULT;
    public String getSyncOptionWifiStatusOption() {return syncOptionWifiStatus;}
    public void setSyncOptionWifiStatusOption(String p) {syncOptionWifiStatus = p;}

    private ArrayList<FilterListAdapter.FilterListItem> syncOptionWifiAccessPointGrantList = new ArrayList<FilterListAdapter.FilterListItem>();
    public ArrayList<FilterListAdapter.FilterListItem> getSyncOptionWifiAccessPointGrantList() {return syncOptionWifiAccessPointGrantList;}
    public void setSyncOptionWifiAccessPointGrantList(ArrayList<FilterListAdapter.FilterListItem> p) {
        syncOptionWifiAccessPointGrantList = p;}

    private ArrayList<FilterListAdapter.FilterListItem> syncOptionWifiIPAddressGrantList = new ArrayList<FilterListAdapter.FilterListItem>();
    public ArrayList<FilterListAdapter.FilterListItem> getSyncOptionWifiIPAddressGrantList() {return syncOptionWifiIPAddressGrantList;}
    public void setSyncOptionWifiIPAddressGrantList(ArrayList<FilterListAdapter.FilterListItem> p) {
        syncOptionWifiIPAddressGrantList = p;}

    private boolean syncOptionSyncOnlyCharging = false;
    public void setSyncOptionSyncWhenCharging(boolean charging) {syncOptionSyncOnlyCharging = charging;}
    public boolean isSyncOptionSyncWhenCharging() {return syncOptionSyncOnlyCharging;}

    private boolean syncOptionDeleteFirstWhenMirror = false;
    public void setSyncOptionDeleteFirstWhenMirror(boolean first) {syncOptionDeleteFirstWhenMirror = first;}
    public boolean isSyncOptionDeleteFirstWhenMirror() {return syncOptionDeleteFirstWhenMirror;}

    private boolean syncOptionConfirmNotExistsExifDate = true;
    public void setSyncOptionConfirmNotExistsExifDate(boolean enabled) {syncOptionConfirmNotExistsExifDate=enabled;}
    public boolean isSyncOptionConfirmNotExistsExifDate() {return syncOptionConfirmNotExistsExifDate;}

    private boolean syncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile = false;
    public void setSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile(boolean enabled) {syncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile =enabled;}
    public boolean isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile() {return syncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile;}

    private boolean syncOptionIgnoreDstDifference = false;
    public void setSyncOptionIgnoreDstDifference(boolean enabled) {syncOptionIgnoreDstDifference =enabled;}
    public boolean isSyncOptionIgnoreDstDifference() {return syncOptionIgnoreDstDifference;}

    public static final int SYNC_OPTION_OFFSET_OF_DST_10_MIN=10; // index 0
    public static final int SYNC_OPTION_OFFSET_OF_DST_20_MIN=20; // index 1
    public static final int SYNC_OPTION_OFFSET_OF_DST_30_MIN=30; // index 2
    public static final int SYNC_OPTION_OFFSET_OF_DST_40_MIN=40; // index 3
    public static final int SYNC_OPTION_OFFSET_OF_DST_50_MIN=50; // index 4
    public static final int SYNC_OPTION_OFFSET_OF_DST_60_MIN=60; // index 5
    public static final int SYNC_OPTION_OFFSET_OF_DST_70_MIN=70; // index 6
    public static final int SYNC_OPTION_OFFSET_OF_DST_80_MIN=80; // index 7
    public static final int SYNC_OPTION_OFFSET_OF_DST_90_MIN=90; // index 8
    public static final int SYNC_OPTION_OFFSET_OF_DST_100_MIN=100; // index
    public static final int SYNC_OPTION_OFFSET_OF_DST_110_MIN=110; // index 10
    public static final int SYNC_OPTION_OFFSET_OF_DST_120_MIN=120; // index 11
    public static final int SYNC_OPTION_OFFSET_OF_DST_LIST_DEFAULT_ITEM_INDEX=5; // default list index
    public static final int[] SYNC_OPTION_OFFSET_OF_DST_LIST=new int[]{SYNC_OPTION_OFFSET_OF_DST_10_MIN, SYNC_OPTION_OFFSET_OF_DST_20_MIN,
            SYNC_OPTION_OFFSET_OF_DST_30_MIN, SYNC_OPTION_OFFSET_OF_DST_40_MIN, SYNC_OPTION_OFFSET_OF_DST_50_MIN, SYNC_OPTION_OFFSET_OF_DST_60_MIN,
            SYNC_OPTION_OFFSET_OF_DST_70_MIN, SYNC_OPTION_OFFSET_OF_DST_80_MIN, SYNC_OPTION_OFFSET_OF_DST_90_MIN, SYNC_OPTION_OFFSET_OF_DST_100_MIN,
            SYNC_OPTION_OFFSET_OF_DST_110_MIN, SYNC_OPTION_OFFSET_OF_DST_120_MIN};

    public static final int SYNC_OPTION_OFFSET_OF_DST_DEFAULT=SYNC_OPTION_OFFSET_OF_DST_60_MIN;
    private int syncOptionOffsetOfDst = SYNC_OPTION_OFFSET_OF_DST_DEFAULT;
    public void setSyncOptionOffsetOfDst(int offset) {syncOptionOffsetOfDst =offset;}
    public int getSyncOptionOffsetOfDst() {return syncOptionOffsetOfDst;}

    private boolean syncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters = false;
    public void setSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters(boolean enabled) {syncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters=enabled;}
    public boolean isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters() {return syncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters;}

    private boolean syncOptionSyncAllowGlobalIpAddress=false;
    public boolean isSyncOptionSyncAllowGlobalIpAddress() {return syncOptionSyncAllowGlobalIpAddress;}
    public void setSyncOptionSyncAllowGlobalIpAddress(boolean p) {syncOptionSyncAllowGlobalIpAddress = p;}

    private boolean syncOptionMoveOnlyRemoveSourceDirectoryIfEmpty =false;
    public boolean isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty() {return syncOptionMoveOnlyRemoveSourceDirectoryIfEmpty;}
    public void setSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty(boolean p) {syncOptionMoveOnlyRemoveSourceDirectoryIfEmpty = p;}

    final static public int SYNC_TASK_OPTION_ERROR_OPTION_STOP =0;
    final static public int SYNC_TASK_OPTION_ERROR_OPTION_SKIP_UNCOND =1;
    final static public int SYNC_TASK_OPTION_ERROR_OPTION_SKIP_NETWORK =2;
    private int syncOptionTaskErrorOption= SYNC_TASK_OPTION_ERROR_OPTION_STOP;
    public int getSyncTaskErrorOption() {return syncOptionTaskErrorOption;}
    public void setSyncTaskErrorOption(int error_option) {syncOptionTaskErrorOption=error_option;}

    private boolean syncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb =false;
    public boolean isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb() {return syncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb;}
    public void setSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb(boolean p) {syncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb = p;}

    private boolean syncOptionIgnoreDestinationFileNameLengthExceed255Byte =false;
    public boolean isSyncOptionIgnoreDestinationFileNameLengthExceed255Byte() {return syncOptionIgnoreDestinationFileNameLengthExceed255Byte;}
    public void setSyncOptionIgnoreDestinationFileNameLengthExceed255Byte(boolean p) {syncOptionIgnoreDestinationFileNameLengthExceed255Byte = p;}

    private boolean syncOptionRemoveDirectoryFileThatExcludedByFilter =false;
    public void setSyncOptionRemoveDirectoryFileThatExcludedByFilter(boolean enabled) { syncOptionRemoveDirectoryFileThatExcludedByFilter = enabled;}
    public boolean isSyncOptionRemoveDirectoryFileThatExcludedByFilter() {return syncOptionRemoveDirectoryFileThatExcludedByFilter;}

    public final static String SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT="";
    public final static String SYNC_FOLDER_SMB_PORT_DEFAULT_DESCRIPTION = "Autodetect SMB port";
    public final static String SYNC_FOLDER_SMB_PROTOCOL_SYSTEM = "0";
    public final static String SYNC_FOLDER_SMB_PROTOCOL_SMB1 = JcifsAuth.JCIFS_FILE_SMB1;
    public final static String SYNC_FOLDER_SMB_PROTOCOL_SMB23 = JcifsAuth.JCIFS_FILE_SMB23;
    public final static String SYNC_FOLDER_SMB_PROTOCOL_DEFAULT = SYNC_FOLDER_SMB_PROTOCOL_SMB23;
    public final static String SYNC_FOLDER_SMB_PROTOCOL_DEFAULT_DESCRIPTION = SYNC_FOLDER_SMB_PROTOCOL_DEFAULT;
    public final static String[] SYNC_FOLDER_SMB_PROTOCOL_LIST=new String[]{SYNC_FOLDER_SMB_PROTOCOL_SMB1, SYNC_FOLDER_SMB_PROTOCOL_SMB23};
    private String syncTaskSourceFolderSmbProtocol = SYNC_FOLDER_SMB_PROTOCOL_DEFAULT;
    public String getSourceSmbProtocol() {return syncTaskSourceFolderSmbProtocol;}
    public void setSourceSmbProtocol(String proto) {syncTaskSourceFolderSmbProtocol=proto;}

    private boolean syncTaskSourceFolderSmbIpcSigningEnforced = true;

    private boolean syncTaskSourceFolderSmbUseSmb2Negotiation = false;

    private String syncTaskSourceFolderStorageUuid = SafManager3.SAF_FILE_PRIMARY_UUID;
    public String getSourceStorageUuid() {return syncTaskSourceFolderStorageUuid;}
    public void setSourceStorageUuid(String p) {syncTaskSourceFolderStorageUuid = p;}

    private String syncTaskSourceFolderSmbAccountName = "";
    public void setSourceSmbAccountName(String p) {syncTaskSourceFolderSmbAccountName = p;}
    public String getSourceSmbAccountName() {return syncTaskSourceFolderSmbAccountName;}

    private String syncTaskSourceFolderSmbPassword = "";
    public void setSourceSmbPassword(String p) {syncTaskSourceFolderSmbPassword = p;}
    public String getSourceSmbPassword() {return syncTaskSourceFolderSmbPassword;}

    private String syncTaskSourceFolderSmbShareName = "";
    public void setSourceSmbShareName(String p) {syncTaskSourceFolderSmbShareName = p;}
    public String getSourceSmbShareName() {return syncTaskSourceFolderSmbShareName;}

    private String syncTaskSourceFolderDirName = "";
    public void setSourceDirectoryName(String p) {syncTaskSourceFolderDirName = p;}
    public String getSourceDirectoryName() {return syncTaskSourceFolderDirName;}

    private String syncTaskSourceFolderSmbIpAddress = "";
    public void setSourceSmbAddr(String p) {syncTaskSourceFolderSmbIpAddress = p;}
    public String getSourceSmbAddr() {return syncTaskSourceFolderSmbIpAddress;}

    private String syncTaskSourceFolderSmbPortNumber = SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT;
    public void setSourceSmbPort(String p) {syncTaskSourceFolderSmbPortNumber = p;}
    public String getSourceSmbPort() {return syncTaskSourceFolderSmbPortNumber;}

    private String syncTaskSourceFolderSmbHostName = "";
    public void setSourceSmbHostName(String p) {syncTaskSourceFolderSmbHostName = p;}
    public String getSourceSmbHostName() {return syncTaskSourceFolderSmbHostName;}

    private String syncTaskSourceFolderSmbDomain = "";
    public void setSourceSmbDomain(String p) {syncTaskSourceFolderSmbDomain = p;}
    public String getSourceSmbDomain() {return syncTaskSourceFolderSmbDomain;}

    private String syncTaskDestinationFolderType = SYNC_FOLDER_TYPE_DEFAULT;
    public String getDestinationFolderType() {return syncTaskDestinationFolderType;}
    public void setDestinationFolderType(String p) {syncTaskDestinationFolderType = p;}


    private String syncTaskDestinationFolderDirName = "";
    public String getDestinationDirectoryName() {return syncTaskDestinationFolderDirName;}
    public void setDestinationDirectoryName(String p) {syncTaskDestinationFolderDirName = p;}

    private String syncTaskDestinationFolderSmbAccountName = "";
    public void setDestinationSmbAccountName(String p) {syncTaskDestinationFolderSmbAccountName = p;}
    public String getDestinationSmbAccountName() {return syncTaskDestinationFolderSmbAccountName;}

    private String syncTaskDestinationFolderSmbPassword = "";
    public String getDestinationSmbPassword() {return syncTaskDestinationFolderSmbPassword;}
    public void setDestinationSmbPassword(String p) {syncTaskDestinationFolderSmbPassword = p;}

    private String syncTaskDestinationFolderSmbShareName = "";
    public String getDestinationSmbShareName() {return syncTaskDestinationFolderSmbShareName;}
    public void setDestinationSmbShareName(String p) {syncTaskDestinationFolderSmbShareName = p;}

    private String syncTaskDestinationFolderSmbIpAddress = "";
    public String getDestinationSmbAddr() {return syncTaskDestinationFolderSmbIpAddress;}
    public void setDestinationSmbAddr(String p) {syncTaskDestinationFolderSmbIpAddress = p;}

    private String syncTaskDestinationFolderSmbPortNumber = SYNC_FOLDER_SMB_PORT_NUMBER_DEFAULT;
    public String getDestinationSmbPort() {return syncTaskDestinationFolderSmbPortNumber;}
    public void setDestinationSmbPort(String p) {syncTaskDestinationFolderSmbPortNumber = p;}

    private String syncTaskDestinationFolderSmbHostName = "";
    public String getDestinationSmbHostName() {return syncTaskDestinationFolderSmbHostName;}
    public void setDestinationSmbHostname(String p) {syncTaskDestinationFolderSmbHostName = p;}

    private String syncTaskDestinationFolderSmbDomain = "";
    public String getDestinationSmbDomain() {return syncTaskDestinationFolderSmbDomain;}
    public void setDestinationSmbDomain(String p) {syncTaskDestinationFolderSmbDomain = p;}

    private boolean syncTaskDestinationFolderSmbUseSmb2Negotiation = false;

    private String syncTaskDestinationFolderStorageUuid = SafManager3.SAF_FILE_PRIMARY_UUID;
    public String getDestinationStorageUuid() {return syncTaskDestinationFolderStorageUuid;}
    public void setDestinationStorageUuid(String p) {syncTaskDestinationFolderStorageUuid = p;}

    private String syncTaskDestinationFolderSmbProtocol = SYNC_FOLDER_SMB_PROTOCOL_DEFAULT;
    public String getDestinationSmbProtocol() {return syncTaskDestinationFolderSmbProtocol;}
    public void setDestinationSmbProtocol(String proto) {syncTaskDestinationFolderSmbProtocol=proto;}

    private boolean syncTaskDestinationFolderSmbIpcSigningEnforced = true;

    private String syncTaskDestinationZipFileName = "";
    public String getDestinationZipOutputFileName() {return syncTaskDestinationZipFileName;}
    public void setDestinationZipOutputFileName(String p) {syncTaskDestinationZipFileName = p;}
    public final static String ZIP_OPTION_COMPRESS_LEVEL_FASTEST = "FASTEST";
    public final static String ZIP_OPTION_COMPRESS_LEVEL_FAST = "FAST";
    public final static String ZIP_OPTION_COMPRESS_LEVEL_NORMAL = "NORMAL";
    public final static String ZIP_OPTION_COMPRESS_LEVEL_MAXIMUM = "MAXIMUM";
    public final static String ZIP_OPTION_COMPRESS_LEVEL_DEFAULT = ZIP_OPTION_COMPRESS_LEVEL_NORMAL;
    public final static String ZIP_OPTION_COMPRESS_LEVEL_DEFAULT_DESCRIPTION = ZIP_OPTION_COMPRESS_LEVEL_DEFAULT;
    public final static String[] ZIP_OPTION_COMPRESSLEVEL_LIST =new String[]{ZIP_OPTION_COMPRESS_LEVEL_FASTEST, ZIP_OPTION_COMPRESS_LEVEL_FAST, ZIP_OPTION_COMPRESS_LEVEL_NORMAL, ZIP_OPTION_COMPRESS_LEVEL_MAXIMUM};

    public final static String ZIP_OPTION_COMPRESS_METHOD_STORE = "STORE";
    public final static String ZIP_OPTION_COMPRESS_METHOD_DEFLATE = "DEFLATE";
    public final static String ZIP_OPTION_COMPRESS_METHOD_DEFAULT = ZIP_OPTION_COMPRESS_METHOD_DEFLATE;
    public final static String ZIP_OPTION_COMPRESS_METHOD_DEFAULT_DESCRIPTION = ZIP_OPTION_COMPRESS_METHOD_DEFAULT;
    public final static String[] ZIP_OPTION_COMPRESS_METGOD_LIST =new String[]{ZIP_OPTION_COMPRESS_METHOD_STORE, ZIP_OPTION_COMPRESS_METHOD_DEFLATE};

    public final static String ZIP_OPTION_ENCRYPT_NONE = "NONE";
    public final static String ZIP_OPTION_ENCRYPT_STANDARD = "STANDARD";
    public final static String ZIP_OPTION_ENCRYPT_AES128 = "AES128";
    public final static String ZIP_OPTION_ENCRYPT_AES256 = "AES256";
    public final static String ZIP_OPTION_ENCRYPT_DEFAULT = ZIP_OPTION_ENCRYPT_NONE ;
    public final static String ZIP_OPTION_ENCRYPT_DEFAULT_DECRIPTION = ZIP_OPTION_ENCRYPT_DEFAULT ;
    public final static String[] ZIP_OPTION_ENCRYPT_LIST=new String[]{ZIP_OPTION_ENCRYPT_NONE, ZIP_OPTION_ENCRYPT_STANDARD, ZIP_OPTION_ENCRYPT_AES128, ZIP_OPTION_ENCRYPT_AES256};

    private String syncTaskDestinationZipCompOptionCompLevel = ZIP_OPTION_COMPRESS_LEVEL_DEFAULT;
    public String getDestinationZipCompressionLevel() {return syncTaskDestinationZipCompOptionCompLevel;}
    public void setDestinationZipCompressionLevel(String p) {syncTaskDestinationZipCompOptionCompLevel = p;}

    private String syncTaskDestinationZipCompOptionCompMethod = ZIP_OPTION_COMPRESS_METHOD_DEFAULT;
    public String getDestinationZipCompressionMethod() {return syncTaskDestinationZipCompOptionCompMethod;}
    public void setDestinationZipCompressionMethod(String p) {syncTaskDestinationZipCompOptionCompMethod = p;}

    private String syncTaskDestinationZipCompOptionEncrypt = ZIP_OPTION_ENCRYPT_DEFAULT;
    public String getDestinationZipEncryptMethod() {return syncTaskDestinationZipCompOptionEncrypt;}
    public void setDestinationZipEncryptMethod(String p) {syncTaskDestinationZipCompOptionEncrypt = p;}

    private String syncTaskDestinationZipCompOptionPassword = "";
    public String getDestinationZipPassword() {return syncTaskDestinationZipCompOptionPassword;}
    public void setDestinationZipPassword(String p) {syncTaskDestinationZipCompOptionPassword = p;}

    private String syncTaskDestinationZipCompOptionEncoding = "UTF-8";
    public String getDestinationZipFileNameEncoding() {return syncTaskDestinationZipCompOptionEncoding;}
    public void setDestinationZipFileNameEncoding(String p) {syncTaskDestinationZipCompOptionEncoding = p;}

    public final static String TEMPLATE_TAKEN_DATE = "%T-DATE%";
    public final static String TEMPLATE_TAKEN_TIME = "%T-TIME%";
    public final static String TEMPLATE_TAKEN_YEAR = "%T-YYYY%";
    public final static String TEMPLATE_TAKEN_YY = "%T-YY%";
    public final static String TEMPLATE_TAKEN_MONTH = "%T-MM%";
    public final static String TEMPLATE_TAKEN_DAY_OF_YEAR = "%T-DAY-OF-YEAR%";
    public final static String TEMPLATE_TAKEN_DAY = "%T-DD%";
    public final static String TEMPLATE_TAKEN_HOUR = "%T-HH%";
    public final static String TEMPLATE_TAKEN_MIN = "%T-MIN%";
    public final static String TEMPLATE_TAKEN_SEC = "%T-SS%";

    public final static String TEMPLATE_TAKEN_WEEK_DAY = "%T-WEEKDAY%";
    public final static String TEMPLATE_TAKEN_WEEK_DAY_LONG = "%T-WEEKDAY_LONG%";

    public final static String TEMPLATE_TAKEN_WEEK_NUMBER = "%T-WEEKNO%";

    public final static String TEMPLATE_EXEC_DATE = "%E-DATE%";
    public final static String TEMPLATE_EXEC_TIME = "%E-TIME%";
    public final static String TEMPLATE_EXEC_YEAR = "%E-YYYY%";
    public final static String TEMPLATE_EXEC_YY = "%E-YY%";
    public final static String TEMPLATE_EXEC_MONTH = "%E-MM%";
    public final static String TEMPLATE_EXEC_DAY_OF_YEAR = "%E-DAY-OF-YEAR%";
    public final static String TEMPLATE_EXEC_DAY = "%E-DD%";
    public final static String TEMPLATE_EXEC_HOUR = "%E-HH%";
    public final static String TEMPLATE_EXEC_MIN = "%E-MIN%";
    public final static String TEMPLATE_EXEC_SEC = "%E-SS%";

    public final static String TEMPLATE_EXEC_WEEK_DAY = "%E-WEEKDAY%";
    public final static String TEMPLATE_EXEC_WEEK_DAY_LONG = "%E-WEEKDAY_LONG%";

    public final static String TEMPLATE_EXEC_WEEK_NUMBER = "%E-WEEKNO%";

    public final static String TEMPLATE_ORIGINAL_NAME = "%ORIGINAL-NAME%";

    public final static String[] TEMPLATES =new String[]{
            TEMPLATE_TAKEN_DATE,
            TEMPLATE_TAKEN_TIME,
            TEMPLATE_TAKEN_YEAR,
            TEMPLATE_TAKEN_YY,
            TEMPLATE_TAKEN_MONTH,
            TEMPLATE_TAKEN_DAY_OF_YEAR,
            TEMPLATE_TAKEN_DAY,
            TEMPLATE_TAKEN_HOUR,
            TEMPLATE_TAKEN_MIN,
            TEMPLATE_TAKEN_SEC,
            TEMPLATE_TAKEN_WEEK_NUMBER,
            TEMPLATE_TAKEN_WEEK_DAY,
            TEMPLATE_TAKEN_WEEK_DAY_LONG,
            TEMPLATE_EXEC_DATE,
            TEMPLATE_EXEC_TIME,
            TEMPLATE_EXEC_YEAR,
            TEMPLATE_EXEC_YY,
            TEMPLATE_EXEC_MONTH,
            TEMPLATE_EXEC_DAY_OF_YEAR,
            TEMPLATE_EXEC_DAY,
            TEMPLATE_EXEC_HOUR,
            TEMPLATE_EXEC_MIN,
            TEMPLATE_EXEC_SEC,
            TEMPLATE_EXEC_WEEK_NUMBER,
            TEMPLATE_EXEC_WEEK_DAY,
            TEMPLATE_EXEC_WEEK_DAY_LONG
    };

    public final static String[] TEMPLATE_TAKENS =new String[]{
            TEMPLATE_TAKEN_DATE,
            TEMPLATE_TAKEN_TIME,
            TEMPLATE_TAKEN_YEAR,
            TEMPLATE_TAKEN_YY,
            TEMPLATE_TAKEN_MONTH,
            TEMPLATE_TAKEN_DAY_OF_YEAR,
            TEMPLATE_TAKEN_DAY,
            TEMPLATE_TAKEN_HOUR,
            TEMPLATE_TAKEN_MIN,
            TEMPLATE_TAKEN_SEC,
            TEMPLATE_TAKEN_WEEK_NUMBER,
            TEMPLATE_TAKEN_WEEK_DAY,
            TEMPLATE_TAKEN_WEEK_DAY_LONG
    };

    public final static String[] TEMPLATE_EXECS =new String[]{
            TEMPLATE_EXEC_DATE,
            TEMPLATE_EXEC_TIME,
            TEMPLATE_EXEC_YEAR,
            TEMPLATE_EXEC_YY,
            TEMPLATE_EXEC_MONTH,
            TEMPLATE_EXEC_DAY_OF_YEAR,
            TEMPLATE_EXEC_DAY,
            TEMPLATE_EXEC_HOUR,
            TEMPLATE_EXEC_MIN,
            TEMPLATE_EXEC_SEC,
            TEMPLATE_EXEC_WEEK_NUMBER,
            TEMPLATE_EXEC_WEEK_DAY,
            TEMPLATE_EXEC_WEEK_DAY_LONG
    };

    public final static int ARCHIVE_SUFFIX_DIGIT_NOT_USED = 0;
    public final static int ARCHIVE_SUFFIX_DIGIT_2_DIGIT = 2;
    public final static int ARCHIVE_SUFFIX_DIGIT_3_DIGIT = 3;
    public final static int ARCHIVE_SUFFIX_DIGIT_4_DIGIT = 4;
    public final static int ARCHIVE_SUFFIX_DIGIT_5_DIGIT = 5;
    public final static int ARCHIVE_SUFFIX_DIGIT_6_DIGIT = 6;
    public final static int ARCHIVE_SUFFIX_DIGIT_DEFAULT = ARCHIVE_SUFFIX_DIGIT_3_DIGIT;
    public final static int ARCHIVE_SUFFIX_DIGIT_DEFAULT_DESCRIPTION = ARCHIVE_SUFFIX_DIGIT_DEFAULT;
    public final static int[] ARCHIVE_SUFFIX_DIGIT_LIST = new int[]{ARCHIVE_SUFFIX_DIGIT_NOT_USED, ARCHIVE_SUFFIX_DIGIT_2_DIGIT,
            ARCHIVE_SUFFIX_DIGIT_3_DIGIT, ARCHIVE_SUFFIX_DIGIT_4_DIGIT, ARCHIVE_SUFFIX_DIGIT_5_DIGIT, ARCHIVE_SUFFIX_DIGIT_6_DIGIT};
    private String syncDestinationArchiveSuffixDigit = String.valueOf(ARCHIVE_SUFFIX_DIGIT_DEFAULT);
    public String getDestinationArchiveSuffixOption() {return syncDestinationArchiveSuffixDigit;}
    public void setDestinationArchiveSuffixOption(String digit) {syncDestinationArchiveSuffixDigit =digit;}

    private String syncDestinationArchiveRenameFileTemplate = TEMPLATE_ORIGINAL_NAME;
    public String getDestinationArchiveRenameFileTemplate() {return syncDestinationArchiveRenameFileTemplate;}
    public void setDestinationArchiveRenameFileTemplate(String template) {syncDestinationArchiveRenameFileTemplate =template;}

    public final static int ARCHIVE_RETAIN_FOR_A_0_DAYS = 0;
    public final static int ARCHIVE_RETAIN_FOR_A_7_DAYS = 1;
    public final static int ARCHIVE_RETAIN_FOR_A_30_DAYS = 2;
    public final static int ARCHIVE_RETAIN_FOR_A_60_DAYS = 3;
    public final static int ARCHIVE_RETAIN_FOR_A_90_DAYS = 4;
    public final static int ARCHIVE_RETAIN_FOR_A_180_DAYS = 5;
    public final static int ARCHIVE_RETAIN_FOR_A_1_YEARS = 6;
    public final static int ARCHIVE_RETAIN_FOR_A_DEFAULT = ARCHIVE_RETAIN_FOR_A_180_DAYS;
    public final static String ARCHIVE_RETAIN_FOR_A_DEFAULT_DESCRIPTION = "180 Days";
    public final static int[] ARCHIVE_RETAIN_FOR_A_LIST = new int[]{ARCHIVE_RETAIN_FOR_A_0_DAYS,
            ARCHIVE_RETAIN_FOR_A_7_DAYS, ARCHIVE_RETAIN_FOR_A_30_DAYS, ARCHIVE_RETAIN_FOR_A_60_DAYS,
            ARCHIVE_RETAIN_FOR_A_90_DAYS, ARCHIVE_RETAIN_FOR_A_180_DAYS, ARCHIVE_RETAIN_FOR_A_1_YEARS};
    private int syncDestinationArchiveRetentionPeriod = ARCHIVE_RETAIN_FOR_A_DEFAULT;
    public int getDestinationArchiveRetentionPeriod() {return syncDestinationArchiveRetentionPeriod;}
    public void setDestinationArchiveRetentionPeriod(int period) {syncDestinationArchiveRetentionPeriod =period;}

    private boolean syncDestinationArchiveIgnoreSourceDirectory = false;
    public boolean isDestinationArchiveIgnoreSourceDirectory() {return syncDestinationArchiveIgnoreSourceDirectory;}
    public void setDestinationArchiveIgnoreSourceDirectory(boolean ignore) {syncDestinationArchiveIgnoreSourceDirectory =ignore;}

    private String syncLastSyncTime = "";
    public void setLastSyncTime(String p) {syncLastSyncTime = p;}
    public String getLastSyncTime() {return syncLastSyncTime;}

    private boolean syncTaskIsRunning = false;
    public void setSyncTaskRunning(boolean p) {syncTaskIsRunning = p;}
    public boolean isSyncTaskRunning() {return syncTaskIsRunning;}

    private int syncLastSyncResult = 0;
    static public final int SYNC_RESULT_STATUS_SUCCESS = HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_SUCCESS;
    static public final int SYNC_RESULT_STATUS_CANCEL = HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_CANCEL;
    static public final int SYNC_RESULT_STATUS_ERROR = HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_ERROR;
    static public final int SYNC_RESULT_STATUS_WARNING = HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_WARNING;
    static public final int SYNC_RESULT_STATUS_SKIP = HistoryListAdapter.HistoryListItem.SYNC_RESULT_STATUS_SKIP;
    static public final int SYNC_RESULT_STATUS_DEFAULT = SYNC_RESULT_STATUS_SUCCESS;
    static public final String SYNC_RESULT_STATUS_DEFAULT_DESCRIPTION = "SUCCESS";
    public final static int[] SYNC_RESULT_STATUS_LIST = new int[]{SYNC_RESULT_STATUS_SUCCESS, SYNC_RESULT_STATUS_CANCEL, SYNC_RESULT_STATUS_ERROR, SYNC_RESULT_STATUS_WARNING};
    public int getLastSyncResult() {return syncLastSyncResult;}
    public void setLastSyncResult(int p) {syncLastSyncResult = p;}

    private int syncTaskStatusErrorCode =0;
    static public final int SYNC_TASK_STATUS_ERROR_NO_ERROR =0;
//    static public final int SYNC_TASK_ERROR_DIRECTORY_FILTER=1;
    public boolean isSyncTaskError() {return !(syncTaskStatusErrorCode == SYNC_TASK_STATUS_ERROR_NO_ERROR);}
    public int getSyncTaskStatusErrorCode() {return syncTaskStatusErrorCode;}
    public void setSyncTaskStatusErrorCode(int error) {
        syncTaskStatusErrorCode =error;}

    static public final int SYNC_FOLDER_STATUS_ERROR_NO_ERROR =0;
    static public final int SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME =1;
    static public final int SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD =2;
    static public final int SYNC_FOLDER_STATUS_ERROR_ZIP_PASSWORD =4;
    private int syncSourceFolderStatusError = SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR;
    public boolean isSyncFolderStatusError() {return (syncSourceFolderStatusError + syncDestinationFolderStatusError)!= SYNC_FOLDER_STATUS_ERROR_NO_ERROR;}
    public int getSourceFolderStatusError() {return syncSourceFolderStatusError;}
    public void setSourceFolderStatusError(int error_code) {
        syncSourceFolderStatusError = error_code;}
    private int syncDestinationFolderStatusError = SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR;
    public int getDestinationFolderStatusError() {return syncDestinationFolderStatusError;}
    public void setDestinationFolderStatusError(int error_code) {
        syncDestinationFolderStatusError = error_code;}

    private boolean isChanged=false;
    public boolean isChanged() {return isChanged;}
    public void setChanged(boolean changed) {isChanged=changed;}

    @Override
    public SyncTaskItem clone() {
        SyncTaskItem npfli = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            oos.flush();
            oos.close();

            baos.flush();
            byte[] ba_buff = baos.toByteArray();
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(ba_buff);
            ObjectInputStream ois = new ObjectInputStream(bais);

            npfli = (SyncTaskItem) ois.readObject();
            ois.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return npfli;
    }

    public boolean isSame(SyncTaskItem sti) {
        boolean result = false;
        if ((syncTasｋName.equals(sti.getSyncTaskName()) &&
                (isChanged==sti.isChanged()) &&
                (syncTaskGroup.equals(sti.getSyncTaskGroup())) &&
                (syncTaskEnabled==sti.isSyncTaskAuto()) &&
                (syncOptionSyncTestMode==sti.isSyncTestMode()) &&
                (syncTaskType.equals(sti.getSyncTaskType())) &&
                (syncTaskStatusErrorCode ==sti.getSyncTaskStatusErrorCode()) &&
                (syncSourceFolderStatusError ==sti.getSourceFolderStatusError()) &&
                (syncDestinationFolderStatusError ==sti.getDestinationFolderStatusError()) &&
                (syncTwoWayConflictOption.equals(sti.getSyncTwoWayConflictFileRule())) &&
                (syncTwoWayConflictKeepConflictFile==sti.isSyncTwoWayKeepConflictFile() ))&&
                (syncTaskSourceFolderType.equals(sti.getSourceFolderType())) &&
                (syncTaskSourceFolderDirName.equals(sti.getSourceDirectoryName())) &&
                (syncTaskSourceFolderSmbShareName.equals(sti.getSourceSmbShareName())) &&
                (syncTaskSourceFolderSmbIpAddress.equals(sti.getSourceSmbAddr())) &&
                (syncTaskSourceFolderSmbHostName.equals(sti.getSourceSmbHostName())) &&
                (syncTaskSourceFolderSmbPortNumber.equals(sti.getSourceSmbPort())) &&
                (syncTaskSourceFolderSmbAccountName.equals(sti.getSourceSmbAccountName())) &&
                (syncTaskSourceFolderSmbPassword.equals(sti.getSourceSmbPassword())) &&
                (syncTaskSourceFolderSmbDomain.equals(sti.getSourceSmbDomain())) &&
                (syncTaskSourceFolderSmbProtocol.equals(sti.getSourceSmbProtocol())) &&

                (syncOptionWifiStatus.equals(sti.getSyncOptionWifiStatusOption())) &&

                (syncTaskSourceFolderStorageUuid.equals(sti.getSourceStorageUuid()))) {
            if ((syncTaskDestinationFolderType.equals(sti.getDestinationFolderType())) &&
                    (syncTaskDestinationFolderDirName.equals(sti.getDestinationDirectoryName())) &&
                    (syncTaskDestinationFolderSmbShareName.equals(sti.getDestinationSmbShareName())) &&
                    (syncTaskDestinationFolderSmbIpAddress.equals(sti.getDestinationSmbAddr())) &&
                    (syncTaskDestinationFolderSmbHostName.equals(sti.getDestinationSmbHostName())) &&
                    (syncTaskDestinationFolderSmbPortNumber.equals(sti.getDestinationSmbPort())) &&
                    (syncTaskDestinationFolderSmbAccountName.equals(sti.getDestinationSmbAccountName())) &&
                    (syncTaskDestinationFolderSmbPassword.equals(sti.getDestinationSmbPassword())) &&
                    (syncTaskDestinationFolderSmbDomain.equals(sti.getDestinationSmbDomain())) &&
                    (syncTaskDestinationFolderSmbProtocol.equals(sti.getDestinationSmbProtocol())) &&
                    (syncTaskDestinationFolderStorageUuid.equals(sti.getDestinationStorageUuid()))) {
                if ((syncTaskDestinationZipFileName.equals(sti.getDestinationZipOutputFileName())) &&
                        (syncTaskDestinationZipCompOptionCompLevel.equals(sti.getDestinationZipCompressionLevel())) &&
                        (syncTaskDestinationZipCompOptionCompMethod.equals(sti.getDestinationZipCompressionMethod())) &&
                        (syncTaskDestinationZipCompOptionEncrypt.equals(sti.getDestinationZipEncryptMethod())) &&
                        (syncTaskDestinationZipCompOptionPassword.equals(sti.getDestinationZipPassword())) &&
                        (syncTaskDestinationZipCompOptionEncoding.equals(sti.getDestinationZipFileNameEncoding())) &&
                        (syncDestinationArchiveRenameFileTemplate.equals(sti.getDestinationArchiveRenameFileTemplate())) &&
                        (syncDestinationArchiveRetentionPeriod ==sti.getDestinationArchiveRetentionPeriod()) &&
                        (syncDestinationArchiveSuffixDigit.equals(sti.getDestinationArchiveSuffixOption())) &&
                        (syncDestinationArchiveIgnoreSourceDirectory==sti.isDestinationArchiveIgnoreSourceDirectory()) &&
                        (syncFileTypeAudio==sti.isSyncFileTypeAudio()) &&
                        (syncFileTypeImage==sti.isSyncFileTypeImage()) &&
                        (syncFileTypeVideo==sti.isSyncFileTypeVideo()) &&

                        (syncOptionRootDirFileToBeProcessed==sti.isSyncProcessRootDirFile()) &&
                        (syncOptionProcessOverrideCopyMove==sti.isSyncOverrideCopyMoveFile()) &&
                        (syncOptionConfirmOverrideDelete==sti.isSyncConfirmOverrideOrDelete()) &&
                        (syncOptionNotUsedLastModifiedForRemote==sti.isSyncDoNotResetFileLastModified()) &&
                        (syncOptionDeterminChangedFileSizeGreaterThanDestinationFile==sti.isSyncDifferentFileSizeGreaterThanDestinationFile()) &&
                        (syncOptionRetryCount==sti.getSyncOptionRetryCount()) &&
                        (syncOptionSyncEmptyDir==sti.isSyncOptionSyncEmptyDirectory()) &&
                        (syncOptionSyncHiddenFile==sti.isSyncOptionSyncHiddenFile()) &&
                        (syncOptionSyncHiddenDir==sti.isSyncOptionSyncHiddenDirectory()) &&
                        (syncOptionSyncSubDir==sti.isSyncOptionSyncSubDirectory()) &&
                        (syncOptionCreateDestinationDirectoryIfDoesNotExist==sti.isSyncOptionCreateDestinationDirectoryIfDoesNotExist()) &&
                        (syncOptionUseSmallIoBuffer==sti.isSyncOptionUseSmallIoBuffer()) &&
                        (syncOptionDeterminChangedFileBySize==sti.isSyncOptionDifferentFileBySize()) &&
                        (syncOptionDeterminChangedFileByTime==sti.isSyncOptionDifferentFileByTime()) &&
                        (syncOptionDeterminChangedFileByTimeValue == sti.getSyncOptionDifferentFileAllowableTime()) &&
                        (syncOptionDeleteFirstWhenMirror==sti.isSyncOptionDeleteFirstWhenMirror()) &&
                        (syncOptionConfirmNotExistsExifDate==sti.isSyncOptionConfirmNotExistsExifDate()) &&

                        (syncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile ==sti.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile()) &&

                        (syncOptionIgnoreDstDifference==sti.isSyncOptionIgnoreDstDifference()) &&
                        (syncOptionOffsetOfDst==sti.getSyncOptionOffsetOfDst()) &&

                        (syncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters==sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters()) &&

                        (syncOptionWifiStatus.equals(sti.getSyncOptionWifiStatusOption())) &&

                        (syncOptionSyncAllowGlobalIpAddress==sti.isSyncOptionSyncAllowGlobalIpAddress()) &&

                        (syncOptionMoveOnlyRemoveSourceDirectoryIfEmpty ==sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty()) &&

                        (syncOptionTaskErrorOption ==sti.getSyncTaskErrorOption()) &&

                        (syncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb ==sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb()) &&

                        (syncOptionIgnoreDestinationFileNameLengthExceed255Byte==sti.isSyncOptionIgnoreDestinationFileNameLengthExceed255Byte()) &&

                        (syncOptionRemoveDirectoryFileThatExcludedByFilter ==sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter()) &&

                        (syncOptionSyncOnlyCharging==sti.isSyncOptionSyncWhenCharging())) {

                    String ff_cmp1 = "";
                    for (FilterListAdapter.FilterListItem item : syncFileNameFilter) ff_cmp1 += item.toString()+" ";

                    String ff_cmp2 = "";
                    for (FilterListAdapter.FilterListItem item : sti.getFileNameFilter()) ff_cmp2 += item.toString()+" ";

                    String df_cmp1 = "";
                    for (FilterListAdapter.FilterListItem item : syncDirectoryFilter) df_cmp1 += item.toString()+" ";

                    String df_cmp2 = "";
                    for (FilterListAdapter.FilterListItem item : sti.getDirectoryFilter()) df_cmp2 += item.toString()+" ";

                    String wap_cmp1 = "";
                    for (FilterListAdapter.FilterListItem item : syncOptionWifiAccessPointGrantList) wap_cmp1 += item.toString()+" ";

                    String wap_cmp2 = "";
                    for (FilterListAdapter.FilterListItem item : sti.getSyncOptionWifiAccessPointGrantList()) wap_cmp2 += item.toString()+" ";

                    String wad_cmp1 = "";
                    for (FilterListAdapter.FilterListItem item : syncOptionWifiIPAddressGrantList) wad_cmp1 += item.toString()+" ";

                    String wad_cmp2 = "";
                    for (FilterListAdapter.FilterListItem item : sti.getSyncOptionWifiIPAddressGrantList()) wad_cmp2 += item.toString()+" ";

                    if ((ff_cmp1.equals(ff_cmp2)) &&
                            (df_cmp1.equals(df_cmp2)) &&
                            (wap_cmp1.equals(wap_cmp2)) &&
                            (wad_cmp1.equals(wad_cmp2))
                    ) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
