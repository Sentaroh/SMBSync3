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

import com.sentaroh.android.Utilities3.ThemeUtil;

public class Constants {

	public static final String APPLICATION_TAG="SMBSync3";
	public static final String APPLICATION_ID ="com.sentaroh.android."+APPLICATION_TAG;
	public static long SERIALIZABLE_NUMBER=1L;

	public static final String DEFAULT_PREFS_FILENAME="default_preferences";

	public static final String CONFIRM_REQUEST_COPY ="Copy";
	public static final String CONFIRM_REQUEST_DELETE_FILE ="DeleteFile";
	public static final String CONFIRM_REQUEST_DELETE_ZIP_ITEM_FILE ="DeleteZipItemFile";
	public static final String CONFIRM_REQUEST_DELETE_ZIP_ITEM_DIR ="DeleteZipItemDir";
	public static final String CONFIRM_REQUEST_DELETE_DIR ="DeleteDir";
	public static final String CONFIRM_REQUEST_MOVE ="Move";
    public static final String CONFIRM_REQUEST_ARCHIVE_DATE_FROM_FILE ="Archive";
    public static final String CONFIRM_REQUEST_CONFLICT_FILE ="Conflict";
	public static final int CONFIRM_RESP_YES = 1;
	public static final int CONFIRM_RESP_YESALL = 2;
	public static final int CONFIRM_RESP_NO = -1;
	public static final int CONFIRM_RESP_NOALL = -2;
	public static final int CONFIRM_RESP_CANCEL = -10;

    public static final int CONFIRM_CONFLICT_RESP_SELECT_A = 21;
    public static final int CONFIRM_CONFLICT_RESP_SELECT_B = 22;
    public static final int CONFIRM_CONFLICT_RESP_NO = -21;
    public static final int CONFIRM_CONFLICT_RESP_CANCEL = -30;

	static public final int GENERAL_IO_BUFFER_SIZE =1024*1024;
	static public final int SYNC_IO_BUFFER_SIZE=1024*1024*4;

	public final static String LOCAL_FILE_LAST_MODIFIED_FILE_LIST_NAME_V1 ="local_file_last_modified_V1";
	public final static String LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST ="*";

	public final static String START_SYNC_INTENT = APPLICATION_ID +".ACTION_START_SYNC";
	public final static String START_SYNC_EXTRA_PARM_SYNC_TASK ="Task";
	public final static String START_SYNC_EXTRA_PARM_SYNC_GROUP ="Group";
	public final static String START_SYNC_EXTRA_PARM_REQUESTOR ="Requestor";
	public final static String START_SYNC_EXTRA_PARM_REQUEST_ID ="RequestID";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_KEY ="SYNC_RESULT";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_SUCCESS ="SUCCESS";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_ERROR ="ERROR";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_WARNING ="WARNING";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_CANCEL ="CANCEL";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_CODE_NOT_FOUND ="NOT_FOUND";
	public final static String START_SYNC_EXTRA_PARM_SYNC_RESULT_TASK_NAME_KEY ="TASK_NAME";
	public final static String START_SYNC_EXTRA_PARM_REQUESTOR_SHORTCUT ="Shortcut";
	public final static String START_SYNC_AUTO_INTENT = APPLICATION_ID +".ACTION_AUTO_SYNC";

	public final static String QUERY_SYNC_TASK_INTENT = APPLICATION_ID +".ACTION_QUERY_SYNC_TASK";
	public final static String QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE ="TaskType";
	public final static String QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL ="All";
	public final static String QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO ="Auto";
	public final static String QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL ="Manual";
	public final static String QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST ="Test";
	public final static String REPLY_SYNC_TASK_INTENT = APPLICATION_ID +".ACTION_REPLY_SYNC_TASK";
	public final static String REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT ="SYNC_COUNT";
	public final static String REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY ="SYNC_LIST";
	
	public final static String BROADCAST_INTENT_SYNC_STARTED = APPLICATION_ID +".ACTION_SYNC_STARTED";
	public final static String BROADCAST_INTENT_SYNC_ENDED = APPLICATION_ID +".ACTION_SYNC_ENDED";

	final public static String[] NAME_UNUSABLE_CHARACTER=new String[]{","};
	final public static String NAME_LIST_SEPARATOR =",";

	final public static String APP_SHORTCUT_ID_KEY ="shortcut_id";
	final public static int APP_SHORTCUT_ID_VALUE_BUTTON1 = GroupListAdapter.GroupListItem.BUTTON_SHORTCUT1;
	final public static int APP_SHORTCUT_ID_VALUE_BUTTON2 = GroupListAdapter.GroupListItem.BUTTON_SHORTCUT2;
	final public static int APP_SHORTCUT_ID_VALUE_BUTTON3 = GroupListAdapter.GroupListItem.BUTTON_SHORTCUT3;

	final public static long FAT32_MAX_FILE_SIZE=(((long)Integer.MAX_VALUE)*2)-1;

    public final static String SYNC_REQUEST_ACTIVITY ="ACTIVITY";
	public final static String SYNC_REQUEST_EXTERNAL ="EXTERNAL";
	public final static String SYNC_REQUEST_SHORTCUT ="SHORTCUT";
	public final static String SYNC_REQUEST_SCHEDULE ="SCHEDULE";
	
	public static final String NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_NO = "0";
	public static final String NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ALWAYS = "1";
	public static final String NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_SUCCESS = "2";
	public static final String NOTIFICATION_MESSAGE_WHEN_SYNC_ENDED_ERROR = "3";
	
	public static final String NOTIFICATION_SOUND_WHEN_SYNC_ENDED_NO = "0";
	public static final String NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ALWAYS = "1";
	public static final String NOTIFICATION_SOUND_WHEN_SYNC_ENDED_SUCCESS = "2";
	public static final String NOTIFICATION_SOUND_WHEN_SYNC_ENDED_ERROR = "3";

	public static final String NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_NO = "0";
	public static final String NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ALWAYS = "1";
	public static final String NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_SUCCESS = "2";
	public static final String NOTIFICATION_VIBRATE_WHEN_SYNC_ENDED_ERROR = "3";

    public static final String SCREEN_THEME_STANDARD = String.valueOf(ThemeUtil.THEME_DEFAULT);
    public static final String SCREEN_THEME_LIGHT = String.valueOf(ThemeUtil.THEME_LIGHT);
    public static final String SCREEN_THEME_BLACK = String.valueOf(ThemeUtil.THEME_BLACK);

    public static final String STORED_SECRET_KEY_VALIDATION_KEY ="stored_secret_key_validation_key";

	public static final String DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX ="\\";
	public static final int DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX_LENGTH =DIRECTORY_FILTER_MATCH_ANY_WHERE_PREFIX.length();

    public static final String[] ARCHIVE_FILE_TYPE=
			new String[]{"gif", "jpg", "jpeg", "jpe", "png", "mp4", "mov"};

	static final public String DEFAULT_NOCOMPRESS_FILE_TYPE =
			"aac;apk;avi;gif;ico;gz;jar;jpe;jpeg;jpg;m3u;m4a;m4u;mov;movie;mp2;mp3;mpe;mpeg;mpg;mpga;png;qt;ra;ram;svg;tgz;wmv;zip;";

	final public static String JCIFS_OPTION_CLIENT_ATTR_EXPIRATION_PERIOD="jcifs.smb.client.attrExpirationPeriod";
	final public static String JCIFS_OPTION_NETBIOS_RETRY_TIMEOUT="jcifs.netbios.retryTimeout";
	final public static String JCIFS_OPTION_SMB_LM_COMPATIBILITY="jcifs.smb.lmCompatibility";
	final public static String JCIFS_OPTION_CLIENT_USE_EXTENDED_SECUEITY="jcifs.smb.client.useExtendedSecurity";
	final public static String JCIFS_OPTION_CLIENT_RESPONSE_TIMEOUT="jcifs.smb.client.responseTimeout";
	final public static String JCIFS_OPTION_CLIENT_DISABLE_PLAIN_TEXT_PASSWORDS="jcifs.smb.client.disablePlainTextPasswords";

}
