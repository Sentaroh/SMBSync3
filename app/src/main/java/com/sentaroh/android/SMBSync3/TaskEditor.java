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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sentaroh.android.SMBSync3.LocalStorageSelectorAdapter.LocalStorageSelectorItem;

import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.CommonFileSelector2;
import com.sentaroh.android.Utilities3.MiscUtil;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.SafFile3;
import com.sentaroh.android.Utilities3.SafManager3;
import com.sentaroh.android.Utilities3.SafStorage3;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities3.Widget.NonWordwrapTextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.KeyEvent.KEYCODE_BACK;
import static com.sentaroh.android.SMBSync3.Constants.*;
import static com.sentaroh.android.SMBSync3.SmbServerScan.SmbServerScanResult.SMB_LEVEL_SMB1;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.TEMPLATE_ORIGINAL_NAME;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.ARCHIVE_RETAIN_FOR_A_DEFAULT;
import static com.sentaroh.android.SMBSync3.SyncTaskItem.ARCHIVE_SUFFIX_DIGIT_DEFAULT;
import static com.sentaroh.android.Utilities3.SafFile3.SAF_FILE_PRIMARY_UUID;

public class TaskEditor extends DialogFragment {
    private final static String SUB_APPLICATION_TAG = "SyncTask ";

    private Dialog mDialog = null;
    private boolean mTerminateRequired = true;
    private Context mContext = null;
    private ActivityMain mActivity=null;
    private TaskEditor mFragment = null;
    private GlobalParameters mGp = null;
    private TaskListUtils mTaskUtil = null;
    private CommonUtilities mUtil = null;

    private FragmentManager mFragMgr = null;

    public static TaskEditor newInstance() {
        TaskEditor frag = new TaskEditor();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    }

    public TaskEditor() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        // Ignore orientation change to keep activity from restarting
        super.onConfigurationChanged(newConfig);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        reInitViewWidget();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        View view = super.onCreateView(inflater, container, savedInstanceState);
        CommonDialog.setDlgBoxSizeLimit(mDialog, true);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mContext = getActivity();
        mFragment = this;
        mFragMgr = this.getFragmentManager();
        mActivity=(ActivityMain)getActivity();
        mUtil = new CommonUtilities(mContext, "SyncTaskEditor", mGp, getFragmentManager());
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mTerminateRequired) {
            this.dismiss();
        }
    }

    @Override
    final public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    final public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mContext == null) mContext = getActivity();
        mGp=GlobalWorkArea.getGlobalParameter(mContext);
        if (mUtil == null) mUtil = new CommonUtilities(mContext, "SyncTaskEditor", mGp, getFragmentManager());
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    final public void onDetach() {
        super.onDetach();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    final public void onStart() {
        CommonDialog.setDlgBoxSizeLimit(mDialog, true);
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mTerminateRequired) mDialog.cancel();
    }

    @Override
    final public void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    public void onDestroyView() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onCancel(DialogInterface di) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (!mTerminateRequired) {
            final Button btnCancel = (Button) mDialog.findViewById(R.id.edit_profile_sync_dlg_btn_cancel);
            btnCancel.performClick();
        }
        mFragment.dismiss();
        super.onCancel(di);
    }

    @Override
    public void onDismiss(DialogInterface di) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        super.onDismiss(di);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        mDialog = new Dialog(getActivity(), mGp.applicationTheme);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (!mTerminateRequired) {
            initViewWidget();
            editSyncTask(mOpType, mCurrentSyncTaskItem);
        }
        mProgressSipnDialogForActivity.dismiss();
        return mDialog;
    }

    class SavedViewContents {
        CharSequence prof_name_et;
        int prof_name_et_spos;
        int prof_name_et_epos;
        boolean cb_active;

        boolean sync_task_edit_ok_button_enabled =false;
        boolean sync_task_swap_source_destination_button_enabled =false;

        public int sync_opt = -1;
        public boolean sync_process_root_dir_file, sync_conf_required, sync_use_smbsync_last_mod, sync_do_not_reset_remote_file,
                sync_retry, sync_empty_dir, sync_hidden_dir, sync_hidden_file, sync_sub_dir, sync_use_ext_dir_fileter, sync_delete_first;
        public boolean sync_UseRemoteSmallIoArea;
        public int sync_source_pos = -1, sync_destination_pos = -1;

        public String sync_source_foder_info = "";
        public Drawable sync_source_foder_icon =null;
        public String sync_destination_foder_info = "";
        public Drawable sync_destination_foder_icon =null;

        public String sync_file_filter_info = "";
        public String sync_dir_filter_info = "";

        public boolean sync_process_override;
        public boolean sync_copy_by_rename;

        public String sync_wifi_option =SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP;

        public boolean sync_test_mode;
        public boolean sync_diff_use_file_size;
        public boolean sync_diff_use_last_mod;

        public boolean sync_show_special_option;

        public int sync_diff_last_mod_value = -1;

        public boolean sync_diff_file_size_gt_destination =false;

        public boolean task_skip_if_network_not_satisfied=false;

        public boolean sync_when_cahrging=false;
        public boolean allow_global_ip_addr=false;
        public boolean never_overwrite_destination_file_newer_than_the_source_file =false;
        public boolean ignore_dst_difference=false;
        public int dst_offset_value=-1;
        public boolean ignore_unusable_character_used_directory_file_name=false;
        public boolean sync_remove_source_if_empty =false;

        public boolean destination_directory_same_as_source_directory=false;

        //        public boolean keep_conflict_file=false;
        public boolean specific_file_type=false;
        public boolean specific_file_type_audio=false;
        public boolean specific_file_type_image=false;
        public boolean specific_file_type_video=false;
        public boolean specific_diretory=false;

    }

    private SavedViewContents saveViewContents() {
        SavedViewContents sv = new SavedViewContents();

        final EditText et_sync_main_task_name = (EditText) mDialog.findViewById(R.id.edit_sync_task_task_name);
        final CheckedTextView ctv_auto = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_ctv_auto);
        final Spinner spinnerSyncOption = (Spinner) mDialog.findViewById(R.id.edit_sync_task_sync_type);

		final Button swap_source_destination = (Button)mDialog.findViewById(R.id.edit_sync_task_swap_source_and_destination_btn);
        final Button source_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_source_folder_info_btn);
        final Button destination_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_btn);

        final Button sync_task_edit_btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_sync_dlg_btn_ok);

        final Button dir_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_dir_filter_btn);
        final Button file_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_file_filter_btn);
//		final TextView dlg_file_filter=(TextView) mDialog.findViewById(R.id.sync_filter_summary_file_filter);
//		final TextView dlg_dir_filter=(TextView) mDialog.findViewById(R.id.sync_filter_summary_dir_filter);

        final LinearLayout ll_special_option_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_special_option_view);

        final CheckedTextView ctvTestMode = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_test_mode);

        final CheckedTextView ct_specific_file_type = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_specific);
        final LinearLayout ll_specific_file_type_view = (LinearLayout) mDialog.findViewById(R.id.sync_filter_file_type_detail_view);
        final CheckedTextView ct_specific_file_type_audio = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_audio);
        final CheckedTextView ct_specific_file_type_image = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_image);
        final CheckedTextView ct_specific_file_type_video = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_video);

        final CheckedTextView ct_specific_directory = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_sub_directory_specific);
        final LinearLayout ll_specific_directory_view = (LinearLayout) mDialog.findViewById(R.id.sync_filter_sub_directory_detail_view);

        final CheckedTextView ctv_task_sync_when_cahrging =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_start_when_charging);
        final CheckedTextView ctvProcessRootDirFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_source_root_dir_file);
        final CheckedTextView ctvConfirmOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_confirm_override_delete_file);
        final Spinner spinnerSyncWifiStatus = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_wifi_status);
        final CheckedTextView ctv_sync_allow_global_ip_addr =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_sync_allow_global_ip_address);
        final CheckedTextView ctvShowSpecialOption = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_show_special_option);
        final CheckedTextView ctvSyncSubDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_sub_dir);
        final CheckedTextView ctvSyncEmptyDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_empty_directory);
        final CheckedTextView ctvSyncHiddenDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_directory);
        final CheckedTextView ctvSyncHiddenFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_file);
        final CheckedTextView ctvProcessOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_process_override_delete_file);
        final CheckedTextView ctvDeleteFirst = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_delete_first_when_mirror);

        final CheckedTextView ctvIgnoreFilterRemoveDirFileDesNotExistsInSource = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_remove_dir_file_excluded_by_filter);

        final CheckedTextView ctvRetry = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_retry_if_error_occured);
        final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_remote_small_io_area);
        final CheckedTextView ctvDoNotResetRemoteFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_do_mot_reset_file_last_mod_time);
        final CheckedTextView ctvDiffUseFileSize = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_file_size);
        final CheckedTextView ctvDeterminChangedFileSizeGtDestination = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_file_size_greater_than_destination);
        final CheckedTextView ctDeterminChangedFileByTime = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_last_mod_time);
        final Spinner spinnerSyncDiffTimeValue = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_diff_file_determin_time_value);
        final CheckedTextView ctv_never_overwrite_destination_file_newer_than_the_source_file =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file);
        final CheckedTextView ctv_ignore_dst_difference =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_ignore_dst_difference);
        final Spinner spinnerSyncDstOffsetValue =(Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_offset_daylight_saving_time_value);
        final CheckedTextView ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_unusable_character_used_directory_file_name);
        final CheckedTextView ctv_sync_remove_source_if_empty =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_remove_directory_if_empty_when_move);
        final CheckedTextView ctv_edit_sync_tak_option_keep_conflict_file =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file);

        sv.prof_name_et = et_sync_main_task_name.getText();
        sv.prof_name_et_spos = et_sync_main_task_name.getSelectionStart();
        sv.prof_name_et_epos = et_sync_main_task_name.getSelectionEnd();
        sv.cb_active = ctv_auto.isChecked();
        sv.sync_opt = spinnerSyncOption.getSelectedItemPosition();

        sv.sync_task_edit_ok_button_enabled =sync_task_edit_btn_ok.isEnabled();
        sv.sync_source_foder_icon =source_folder_info.getCompoundDrawables()[0];
        sv.sync_task_swap_source_destination_button_enabled =swap_source_destination.isEnabled();
        sv.sync_destination_foder_icon =destination_folder_info.getCompoundDrawables()[0];

        sv.sync_source_foder_info = source_folder_info.getText().toString();
        sv.sync_destination_foder_info = destination_folder_info.getText().toString();

        sv.sync_file_filter_info = file_filter_btn.getText().toString();
        sv.sync_dir_filter_info = dir_filter_btn.getText().toString();

        sv.sync_process_root_dir_file = ctvProcessRootDirFile.isChecked();
        sv.sync_sub_dir = ctvSyncSubDir.isChecked();
        sv.sync_empty_dir = ctvSyncEmptyDir.isChecked();
        sv.sync_hidden_dir = ctvSyncHiddenDir.isChecked();
        sv.sync_hidden_file = ctvSyncHiddenFile.isChecked();
        sv.sync_process_override=ctvProcessOverride.isChecked();
        sv.sync_conf_required = ctvConfirmOverride.isChecked();
        sv.sync_delete_first = ctvDeleteFirst.isChecked();

        sv.sync_wifi_option = getSpinnerSyncTaskWifiOptionValue(spinnerSyncWifiStatus);

        sv.sync_show_special_option = ctvShowSpecialOption.isChecked();

        sv.sync_do_not_reset_remote_file = ctvDoNotResetRemoteFile.isChecked();
        sv.sync_retry = ctvRetry.isChecked();
        sv.sync_UseRemoteSmallIoArea = ctvSyncUseRemoteSmallIoArea.isChecked();

        sv.sync_test_mode = ctvTestMode.isChecked();
        sv.sync_diff_use_file_size = ctvDiffUseFileSize.isChecked();
        sv.sync_diff_file_size_gt_destination =ctvDeterminChangedFileSizeGtDestination.isChecked();
        sv.sync_diff_use_last_mod = ctDeterminChangedFileByTime.isChecked();

        sv.sync_diff_last_mod_value = spinnerSyncDiffTimeValue.getSelectedItemPosition();

        sv.sync_when_cahrging=ctv_task_sync_when_cahrging.isChecked();
        sv.allow_global_ip_addr=ctv_sync_allow_global_ip_addr.isChecked();
        sv.never_overwrite_destination_file_newer_than_the_source_file =ctv_never_overwrite_destination_file_newer_than_the_source_file.isChecked();
        sv.ignore_dst_difference=ctv_ignore_dst_difference.isChecked();
        sv.dst_offset_value=spinnerSyncDstOffsetValue.getSelectedItemPosition();
        sv.ignore_unusable_character_used_directory_file_name=ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name.isChecked();
        sv.sync_remove_source_if_empty =ctv_sync_remove_source_if_empty.isChecked();
        sv.destination_directory_same_as_source_directory=ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.isChecked();

        sv.specific_file_type=ct_specific_file_type.isChecked();
        sv.specific_file_type_audio=ct_specific_file_type_audio.isChecked();
        sv.specific_file_type_image=ct_specific_file_type_image.isChecked();
        sv.specific_file_type_video=ct_specific_file_type_video.isChecked();
        sv.specific_diretory=ct_specific_directory.isChecked();

        return sv;
    }

    private void performClickNoSound(View v) {
        v.setSoundEffectsEnabled(false);
        v.performClick();
        v.setSoundEffectsEnabled(true);
    }

    private void restoreViewContents(final SavedViewContents sv) {
        final EditText et_sync_main_task_name = (EditText) mDialog.findViewById(R.id.edit_sync_task_task_name);
        final CheckedTextView ctv_auto = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_ctv_auto);
        final Spinner spinnerSyncOption = (Spinner) mDialog.findViewById(R.id.edit_sync_task_sync_type);

		final Button swap_source_destination = (Button)mDialog.findViewById(R.id.edit_sync_task_swap_source_and_destination_btn);
        final Button source_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_source_folder_info_btn);
        final Button destination_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_btn);

        final Button edit_sync_task_ok_btn = (Button) mDialog.findViewById(R.id.edit_profile_sync_dlg_btn_ok);

        final Button dir_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_dir_filter_btn);
        final Button file_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_file_filter_btn);

        final LinearLayout ll_special_option_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_special_option_view);

        final CheckedTextView ctvTestMode = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_test_mode);

        final CheckedTextView ct_specific_file_type = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_specific);
        final LinearLayout ll_specific_file_type_view = (LinearLayout) mDialog.findViewById(R.id.sync_filter_file_type_detail_view);
        final CheckedTextView ct_specific_file_type_audio = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_audio);
        final CheckedTextView ct_specific_file_type_image = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_image);
        final CheckedTextView ct_specific_file_type_video = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_video);

        final CheckedTextView ct_specific_directory = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_sub_directory_specific);
        final LinearLayout ll_specific_directory_view = (LinearLayout) mDialog.findViewById(R.id.sync_filter_sub_directory_detail_view);

        final CheckedTextView ctv_task_sync_when_cahrging =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_start_when_charging);
        final CheckedTextView ctvProcessRootDirFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_source_root_dir_file);
        final CheckedTextView ctvConfirmOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_confirm_override_delete_file);
        final Spinner spinnerSyncWifiStatus = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_wifi_status);
        final CheckedTextView ctv_sync_allow_global_ip_addr =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_sync_allow_global_ip_address);
        final CheckedTextView ctvShowSpecialOption = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_show_special_option);
        final CheckedTextView ctvSyncSubDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_sub_dir);
        final CheckedTextView ctvSyncEmptyDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_empty_directory);
        final CheckedTextView ctvSyncHiddenDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_directory);
        final CheckedTextView ctvSyncHiddenFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_file);
        final CheckedTextView ctvProcessOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_process_override_delete_file);
        final CheckedTextView ctvDeleteFirst = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_delete_first_when_mirror);

        final CheckedTextView ctvIgnoreFilterRemoveDirFileDesNotExistsInSource = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_remove_dir_file_excluded_by_filter);

        final CheckedTextView ctvRetry = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_retry_if_error_occured);
        final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_remote_small_io_area);
        final CheckedTextView ctvDoNotResetRemoteFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_do_mot_reset_file_last_mod_time);
        final CheckedTextView ctvDiffUseFileSize = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_file_size);
        final CheckedTextView ctvDeterminChangedFileSizeGtDestination = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_file_size_greater_than_destination);
        final CheckedTextView ctDeterminChangedFileByTime = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_last_mod_time);
        final Spinner spinnerSyncDiffTimeValue = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_diff_file_determin_time_value);
        final CheckedTextView ctv_never_overwrite_destination_file_newer_than_the_source_file =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file);
        final CheckedTextView ctv_ignore_dst_difference =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_ignore_dst_difference);
        final Spinner spinnerSyncDstOffsetValue =(Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_offset_daylight_saving_time_value);
        final CheckedTextView ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_unusable_character_used_directory_file_name);
        final CheckedTextView ctv_sync_remove_source_if_empty =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_remove_directory_if_empty_when_move);
        final CheckedTextView ctv_edit_sync_tak_option_keep_conflict_file =(CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file);

        et_sync_main_task_name.setText(sv.prof_name_et);
        ctvTestMode.setChecked(sv.sync_test_mode);
        if (sv.sync_test_mode) CommonDialog.setViewEnabled(getActivity(), ctv_auto, false);
        ctv_auto.setChecked(sv.cb_active);
        CommonDialog.setViewEnabled(getActivity(), spinnerSyncOption, false);
        spinnerSyncOption.setSelection(sv.sync_opt);

        CommonDialog.setViewEnabled(getActivity(), swap_source_destination, sv.sync_task_swap_source_destination_button_enabled);

        source_folder_info.setText(sv.sync_source_foder_info);
        source_folder_info.setCompoundDrawablePadding(32);
        source_folder_info.setCompoundDrawablesWithIntrinsicBounds(sv.sync_source_foder_icon, null, null, null);

        destination_folder_info.setText(sv.sync_destination_foder_info);
        destination_folder_info.setCompoundDrawablePadding(32);
        destination_folder_info.setCompoundDrawablesWithIntrinsicBounds(sv.sync_destination_foder_icon, null, null, null);

        file_filter_btn.setText(sv.sync_file_filter_info);
        dir_filter_btn.setText(sv.sync_dir_filter_info);

        ctvProcessRootDirFile.setChecked(sv.sync_process_root_dir_file);
        ctvSyncSubDir.setChecked(sv.sync_sub_dir);
        CommonDialog.setViewEnabled(getActivity(), ctvSyncSubDir, sv.sync_process_root_dir_file);

        ctvSyncEmptyDir.setChecked(sv.sync_empty_dir);
        ctvSyncHiddenDir.setChecked(sv.sync_hidden_dir);
        ctvSyncHiddenFile.setChecked(sv.sync_hidden_file);
        ctvProcessOverride.setChecked(sv.sync_process_override);
        ctvConfirmOverride.setChecked(sv.sync_conf_required);
        ctvDeleteFirst.setChecked(sv.sync_delete_first);

        CommonDialog.setViewEnabled(getActivity(), spinnerSyncWifiStatus, false);

        int wifi_opt_sel=0;
        if (sv.sync_wifi_option.equals(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP)) wifi_opt_sel=1;
        else if (sv.sync_wifi_option.equals(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS)) wifi_opt_sel=2;
        else if (sv.sync_wifi_option.equals(SyncTaskItem.WIFI_STATUS_WIFI_IP_ADDRESS_LIST)) wifi_opt_sel=3;
        spinnerSyncWifiStatus.setSelection(wifi_opt_sel);

        ctvShowSpecialOption.setChecked(false);
        if (sv.sync_show_special_option) {
            performClickNoSound(ctvShowSpecialOption);
        }

        ctvDoNotResetRemoteFile.setChecked(sv.sync_do_not_reset_remote_file);
        ctvRetry.setChecked(sv.sync_retry);
        ctvSyncUseRemoteSmallIoArea.setChecked(sv.sync_UseRemoteSmallIoArea);

        ctvDiffUseFileSize.setChecked(sv.sync_diff_use_file_size);
        ctvDeterminChangedFileSizeGtDestination.setChecked(sv.sync_diff_file_size_gt_destination);
        ctDeterminChangedFileByTime.setChecked(sv.sync_diff_use_last_mod);

        CommonDialog.setViewEnabled(getActivity(), spinnerSyncDiffTimeValue, false);
        spinnerSyncDiffTimeValue.setSelection(sv.sync_diff_last_mod_value);

        ctv_task_sync_when_cahrging.setChecked(sv.sync_when_cahrging);
        ctv_sync_allow_global_ip_addr.setChecked(sv.allow_global_ip_addr);
        ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(sv.never_overwrite_destination_file_newer_than_the_source_file);

        ctv_ignore_dst_difference.setChecked(false);
        if (sv.ignore_dst_difference) {
            performClickNoSound(ctv_ignore_dst_difference);
        }

        spinnerSyncDstOffsetValue.setSelection(sv.dst_offset_value);
        ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name.setChecked(sv.ignore_unusable_character_used_directory_file_name);
        ctv_sync_remove_source_if_empty.setChecked(sv.sync_remove_source_if_empty);

        ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.setChecked(sv.destination_directory_same_as_source_directory);

        ct_specific_file_type.setChecked(false);
        ct_specific_file_type_audio.setChecked(sv.specific_file_type_audio);
        ct_specific_file_type_image.setChecked(sv.specific_file_type_image);
        ct_specific_file_type_video.setChecked(sv.specific_file_type_video);
        if (sv.specific_file_type || sv.specific_file_type_audio || sv.specific_file_type_image || sv.specific_file_type_video || !sv.sync_file_filter_info.equals("")) {
            performClickNoSound(ct_specific_file_type);
        }

        ct_specific_directory.setChecked(false);
        if (sv.specific_diretory || !sv.sync_dir_filter_info.equals("")) {
            performClickNoSound(ct_specific_directory);
        }

        Handler hndl2 = new Handler();
        hndl2.postDelayed(new Runnable() {
            @Override
            public void run() {
                CommonDialog.setViewEnabled(getActivity(), edit_sync_task_ok_btn, sv.sync_task_edit_ok_button_enabled);
                CommonDialog.setViewEnabled(getActivity(), spinnerSyncOption, true);
                CommonDialog.setViewEnabled(getActivity(), spinnerSyncWifiStatus, true);
                CommonDialog.setViewEnabled(getActivity(), spinnerSyncDiffTimeValue, true);
            }
        }, 500);

    }

    public void reInitViewWidget() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (!mTerminateRequired) {
            SavedViewContents sv = null;
            sv = saveViewContents();
            initViewWidget();
            restoreViewContents(sv);
            restoreEditSyncFolderContents();
            CommonDialog.setDlgBoxSizeLimit(mDialog, true);
        }
    }

    private void restoreEditSyncFolderContents() {
        if (mEditFolderDialog!=null) {
            SyncFolderEditValue new_sfev=buildSyncFolderEditValue(mEditFolderDialog, mEditFolderSfev);
            mEditFolderSfev=new_sfev;
            final Button btn_sync_folder_ok = (Button) mEditFolderDialog.findViewById(R.id.edit_profile_remote_btn_ok);
            final TextView dlg_msg = (TextView) mEditFolderDialog.findViewById(R.id.edit_sync_folder_dlg_msg);
            final boolean ok_button_enabled=btn_sync_folder_ok.isEnabled();
            editSyncFolder(true, mEditFolderSti, mEditFolderSfev, mEditFolderNotify);
            Handler hndl=new Handler();
            hndl.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final Button new_btn_sync_folder_ok = (Button) mEditFolderDialog.findViewById(R.id.edit_profile_remote_btn_ok);
                    final TextView new_dlg_msg = (TextView) mEditFolderDialog.findViewById(R.id.edit_sync_folder_dlg_msg);
                    setSyncFolderOkButtonEnabled(new_btn_sync_folder_ok, ok_button_enabled);
                }
            }, 500);
        }
    }

    public void initViewWidget() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    private String mOpType = "";
    private SyncTaskItem mCurrentSyncTaskItem;
    private NotifyEvent mNotifyComplete = null;

    public void showDialog(FragmentManager fm, Fragment frag,
                           final String op_type,
                           final SyncTaskItem pli,
                           TaskListUtils pm,
                           CommonUtilities ut,
                           GlobalParameters gp,
                           NotifyEvent ntfy) {
        mGp = gp;
        ut.addDebugMsg(1,"I","showDialog enterd");
        mTerminateRequired = false;
        mOpType = op_type;
        mCurrentSyncTaskItem = pli;
        mTaskUtil = pm;
        mNotifyComplete = ntfy;
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(frag, null);
        ft.commitAllowingStateLoss();

        mProgressSipnDialogForActivity =CommonDialog.showProgressSpinIndicator((Activity)ntfy.getContext());
        mProgressSipnDialogForActivity.show();
    }

    private Dialog mProgressSipnDialogForActivity =null;

    private void setDialogMsg(final TextView tv, final String msg) {
        if (msg.equals("")) {
            tv.setVisibility(TextView.GONE);
            tv.setText("");
        } else {
            tv.setVisibility(TextView.VISIBLE);
            tv.setText(msg);
        }
    }

    public static String removeInvalidCharForFileDirName(String in_str) {
        String out = in_str
                .replaceAll(":", "")
                .replaceAll("\\\\", "")
                .replaceAll("\\*", "")
                .replaceAll("\\?", "")
                .replaceAll("\"", "")
                .replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll("\\|", "");
        return out;
    }

    public static String removeInvalidCharForFileName(String in_str) {
        String out = in_str
                .replaceAll(":", "")
                .replaceAll("\\\\", "")
                .replaceAll("\\*", "")
                .replaceAll("\\?", "")
                .replaceAll("\"", "")
                .replaceAll("/", "")
                .replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll("\\|", "");
        return out;
    }


    private void setSyncFolderSmbListener(final Dialog dialog, final SyncTaskItem sti, final SyncFolderEditValue sfev, final NotifyEvent ntfy) {
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);
        dlg_msg.setVisibility(TextView.VISIBLE);
        final CheckedTextView ctv_sync_folder_edit_smb_detail = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_edit_smb_server_detail);
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);

        final Button btn_search_host = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_search_remote_host);
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);
        final EditText et_sync_folder_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_name);
        final EditText et_remote_host = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
        //		final LinearLayout ll_sync_folder_port = (LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_port_option_view);
        final CheckedTextView ctv_sync_folder_use_port = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
        final EditText et_sync_folder_port = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);
        final LinearLayout ll_dir_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_view);
        final CheckedTextView ctv_sync_folder_use_pswd = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText et_sync_folder_domain = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_domain);
        final EditText et_sync_folder_user = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText et_sync_folder_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);
        final CheckedTextView ctv_show_password = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_show_smb_account_password);
        final Button btn_sync_folder_list_share = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_share_btn);
        final EditText et_sync_folder_share_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);

        sp_sync_folder_smb_proto.setOnItemSelectedListener(null);
        setSpinnerSyncFolderSmbProto(sti, sp_sync_folder_smb_proto, sfev.folder_smb_protocol);

        if (!sfev.folder_smb_addr.equals("")) et_remote_host.setText(sfev.folder_smb_addr);
        else et_remote_host.setText(sfev.folder_smb_hostname);

        CommonUtilities.setCheckedTextViewListener(ctv_sync_folder_use_port);
        if (!sfev.folder_smb_port.equals("")) {
            ctv_sync_folder_use_port.setChecked(true);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_port, true);
            et_sync_folder_port.setText(sfev.folder_smb_port);
        } else {
            if (sfev.folder_smb_use_port_number) ctv_sync_folder_use_port.setChecked(true);
            else ctv_sync_folder_use_port.setChecked(false);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_port, false);
        }
        ctv_sync_folder_use_port.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctv_sync_folder_use_port.isChecked();
                ctv_sync_folder_use_port.setChecked(isChecked);
                CommonDialog.setViewEnabled(getActivity(), et_sync_folder_port, isChecked);
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });

        et_sync_folder_domain.setVisibility(EditText.GONE);
        if (!sfev.folder_smb_account.equals("") || !sfev.folder_smb_password.equals("") || sfev.folder_error_code!= SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR) {
            ctv_sync_folder_use_pswd.setChecked(true);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_user, true);
            et_sync_folder_user.setText(sfev.folder_smb_account);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_pswd, true);
            et_sync_folder_pswd.setText(sfev.folder_smb_password);
        } else {
            if (sfev.folder_smb_use_account_name_password) ctv_sync_folder_use_pswd.setChecked(true);
            else ctv_sync_folder_use_pswd.setChecked(false);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_user, false);
            CommonDialog.setViewEnabled(getActivity(), et_sync_folder_pswd, false);
        }
        if (mGp.settingSecurityReinitSmbAccountPasswordValue && !mGp.settingSecurityApplicationPasswordHashValue.equals("")) {
            et_sync_folder_user.setText("");
            et_sync_folder_pswd.setText("");
        }
        sfev.folder_smb_use_pswd =ctv_sync_folder_use_pswd.isChecked();
        ctv_sync_folder_use_pswd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctv_sync_folder_use_pswd.isChecked();
                ctv_sync_folder_use_pswd.setChecked(isChecked);
                CommonDialog.setViewEnabled(getActivity(), et_sync_folder_user, isChecked);
                CommonDialog.setViewEnabled(getActivity(), et_sync_folder_pswd, isChecked);
                CommonDialog.setViewEnabled(getActivity(), ctv_show_password, isChecked);
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });

        CommonDialog.setViewEnabled(getActivity(), ctv_show_password, ctv_sync_folder_use_pswd.isChecked());
        if (mGp.settingSecurityHideShowPasswordButton) ctv_show_password.setVisibility(CheckedTextView.GONE);
        else ctv_show_password.setVisibility(CheckedTextView.VISIBLE);
        ctv_show_password.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckedTextView ctv=(CheckedTextView)view;
                ctv.setChecked(!ctv.isChecked());
                if (!ctv.isChecked()) et_sync_folder_pswd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                else et_sync_folder_pswd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        });

        et_sync_folder_share_name.setText(sfev.folder_smb_share);

        et_remote_host.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });
        et_sync_folder_port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });

        et_sync_folder_user.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });
        et_sync_folder_pswd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });
        et_sync_folder_share_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });

        sp_sync_folder_smb_proto.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSyncFolderViewVisibility(dialog, sti, sfev.is_source_folder, sfev);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btn_search_host.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final NotifyEvent ntfy_search_result=new NotifyEvent(mContext);
                ntfy_search_result.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] o) {
                        String[] result=(String[])o[0];
                        String smb_host=result[0];
                        String smb_level=result[1];
                        String smb_portnum=result[2];
                        String smb_acct_name=result[3];
                        String smb_acct_pswd=result[4];
                        String smb_share_name=result[5];
                        mUtil.addDebugMsg(1,"I", "editSyncFolder(SMB) selected value, host="+smb_host+", protocol="+smb_level+", port="+smb_portnum+
                                ", account="+smb_acct_name+", share="+smb_share_name);
                        et_remote_host.setText(smb_host);
                        et_sync_folder_port.setText(smb_portnum);
                        ctv_sync_folder_use_port.setChecked(!smb_portnum.equals(""));
                        et_sync_folder_user.setText(smb_acct_name);
                        ctv_sync_folder_use_pswd.setChecked(!smb_acct_name.equals("") || !smb_acct_pswd.equals(""));
                        et_sync_folder_pswd.setText(smb_acct_pswd);
                        et_sync_folder_share_name.setText(smb_share_name);
                        if (smb_level.equals(SMB_LEVEL_SMB1)) sp_sync_folder_smb_proto.setSelection(0);
                        else sp_sync_folder_smb_proto.setSelection(1);
                    }

                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                    }

                });
                String port_num = "";
                if (ctv_sync_folder_use_port.isChecked()) port_num = et_sync_folder_port.getText().toString();
                final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);
                SmbServerScan ss=new SmbServerScan(mActivity, mGp, mUtil);
                ss.scanSmbServerDlg(ntfy_search_result, port_num, true, (String)sp_sync_folder_smb_proto.getSelectedItem());
            }
        });

        btn_sync_folder_list_share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTaskUtil.invokeSelectSmbShareDlg(dialog);
            }
        });

        setSyncFolderSmbDetailView(dialog, false);
        ctv_sync_folder_edit_smb_detail.setChecked(false);
        ctv_sync_folder_edit_smb_detail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!((CheckedTextView)view).isChecked();
                ((CheckedTextView)view).setChecked(isChecked);
                setSyncFolderSmbDetailView(dialog, isChecked);
                setSyncFolderViewVisibility(dialog, sti, sfev.is_source_folder, sfev);
            }
        });

        final Button btn_sync_folder_edit_dir_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_edit_smb_dir_keyword);
//        if (sfev.is_source_folder) btn_sync_folder_edit_dir_rule.setVisibility(Button.GONE);
        btn_sync_folder_edit_dir_rule.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        boolean changed=(boolean)objects[0];
                        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);
                        if (changed) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);
                        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                boolean disable_taken_date=false;
                if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) disable_taken_date=true;
                editDirectoryFileNameRule(true, disable_taken_date, sti, sfev, et_sync_folder_dir_name,
                        mContext.getString(R.string.msgs_task_sync_task_edit_directory_name_keyword), ntfy);
            }
        });

        et_sync_folder_dir_name.setText(sfev.folder_directory);
        et_sync_folder_dir_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                for(int i = s.length()-1; i >= 0; i--){
                    if(s.charAt(i) == '\n'){
                        s.delete(i, i + 1);
                        break;
                    }
                }
                if (s.length()>0) {
                    String new_name = removeInvalidCharForFileDirName(s.toString());
                    if (s.length() != new_name.length()) {
                        //remove invalid char
                        et_sync_folder_dir_name.setText(new_name);
                        if (new_name.length() > 0)
                            et_sync_folder_dir_name.setSelection(new_name.length());
                        mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_task_sync_task_dlg_dir_name_has_invalid_char), "", null);

                    }
                    setSyncFolderArchiveFileImage(dialog, sti, new_name, true);
                }
                String e_msg="";
                if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR) && !sfev.is_source_folder) {
                    e_msg=checkTakenDateParameterUsed(et_sync_folder_dir_name.getText().toString());
                    if (!e_msg.equals("")) {
                        dlg_msg.setVisibility(TextView.VISIBLE);
                        dlg_msg.setText(e_msg);
                        CommonDialog.setViewEnabled(mActivity, btn_sync_folder_ok, false);
                    } else {
                        dlg_msg.setText("");
                    }
                } else {
                    dlg_msg.setText("");
                }
                if (e_msg.equals("")) setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
        });

        btn_sync_folder_smb_list_dir.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String sel = sp_sync_folder_type.getSelectedItem().toString();
                mTaskUtil.selectRemoteDirectoryDlg(dialog, !sfev.is_source_folder);
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
        });
    }

    private void setSyncFolderSmbDetailView(Dialog dialog, boolean enabled) {
        final CheckedTextView ctv_sync_folder_edit_smb_detail = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_edit_smb_server_detail);
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);

        final Button btn_search_host = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_search_remote_host);
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);
        final EditText et_sync_folder_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_name);
        final EditText et_remote_host = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
        //		final LinearLayout ll_sync_folder_port = (LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_port_option_view);
        final CheckedTextView ctv_sync_folder_use_port = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
        final EditText et_sync_folder_port = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);
        final LinearLayout ll_dir_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_view);
        final CheckedTextView ctv_sync_folder_use_pswd = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText et_sync_folder_domain = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_domain);
        final EditText et_sync_folder_user = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText et_sync_folder_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);
        final CheckedTextView ctv_show_password = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_show_smb_account_password);
        final Button btn_sync_folder_list_share = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_share_btn);
        final EditText et_sync_folder_share_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);

        final TextView tv_hdr_smb_protocol=(TextView)dialog.findViewById(R.id.edit_sync_folder_dlg_hdr_smb_protocol);

        CommonDialog.setViewEnabled(mActivity, btn_search_host, !ctv_sync_folder_edit_smb_detail.isChecked());
        CommonDialog.setViewEnabled(mActivity, tv_hdr_smb_protocol, enabled);
        CommonDialog.setViewEnabled(mActivity, sp_sync_folder_smb_proto, enabled);
        CommonDialog.setViewEnabled(mActivity, et_remote_host, enabled);
        CommonDialog.setViewEnabled(mActivity, ctv_sync_folder_use_port, enabled);
        CommonDialog.setViewEnabled(mActivity, et_sync_folder_port, enabled);
        CommonDialog.setViewEnabled(mActivity, ctv_sync_folder_use_pswd, enabled);
        if (enabled) {
            if (ctv_sync_folder_use_pswd.isChecked()) {
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_domain, true);
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_user, true);
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_pswd, true);
                CommonDialog.setViewEnabled(mActivity, ctv_show_password, true);
            } else {
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_domain, false);
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_user, false);
                CommonDialog.setViewEnabled(mActivity, et_sync_folder_pswd, false);
                CommonDialog.setViewEnabled(mActivity, ctv_show_password, false);
            }
        } else {
            CommonDialog.setViewEnabled(mActivity, et_sync_folder_domain, false);
            CommonDialog.setViewEnabled(mActivity, et_sync_folder_user, false);
            CommonDialog.setViewEnabled(mActivity, et_sync_folder_pswd, false);
            CommonDialog.setViewEnabled(mActivity, ctv_show_password, false);
        }
        CommonDialog.setViewEnabled(mActivity, btn_sync_folder_list_share, enabled);
        CommonDialog.setViewEnabled(mActivity, et_sync_folder_share_name, enabled);

    }

    private void setSyncFolderLocalListener(final Dialog dialog, final SyncTaskItem sti, final SyncFolderEditValue sfev, final NotifyEvent ntfy) {
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);

        final LinearLayout ll_sync_folder_local_storage = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_storage_selector_view);
        final Spinner sp_sync_folder_local_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_selector);
        sp_sync_folder_local_storage_selector.setOnItemSelectedListener(null);
        setSpinnerSyncFolderStorageSelector(sti, sp_sync_folder_local_storage_selector, sfev.folder_storage_uuid);
        final Button btn_sync_folder_permission = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_request_permission);

        final Button btn_sync_folder_local_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_internal_directory_btn);

        final EditText et_sync_folder_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_name);
        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);

        et_sync_folder_dir_name.setText(sfev.folder_directory.startsWith("/")?sfev.folder_directory:"/"+sfev.folder_directory);
        final LinearLayout ll_dir_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_view);

        et_sync_folder_dir_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                String e_msg="";
                if (s.length() > 0) {
                    for(int i = s.length()-1; i >= 0; i--){
                        if(s.charAt(i) == '\n'){
                            s.delete(i, i + 1);
                            break;
                        }
                    }
//                    if (sfev.is_source_folder) {
//                        String new_dir=removeKeyword(s.toString());
//                        if (!s.toString().equals(new_dir)) {
//                            mUtil.showCommonDialogWarn(false,
//                                    mContext.getString(R.string.msgs_task_sync_task_dlg_remove_directory_keyword),
//                                    s.toString()+"\n"+new_dir, null);
//                            s.clear();
//                            s.append(new_dir);
//                        }
//                    } else {
//                        if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
//                            e_msg=checkTakenDateParameterUsed(et_sync_folder_dir_name.getText().toString());
//                            if (!e_msg.equals("")) {
//                                dlg_msg.setText(e_msg);
//                                CommonDialog.setViewEnabled(mActivity, btn_sync_folder_ok, false);
//                            } else {
//                                dlg_msg.setText("");
//                            }
//                        } else {
//                            dlg_msg.setText("");
//                        }
//                    }
                    if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                        e_msg=checkTakenDateParameterUsed(et_sync_folder_dir_name.getText().toString());
                        if (!e_msg.equals("")) {
                            dlg_msg.setText(e_msg);
                            CommonDialog.setViewEnabled(mActivity, btn_sync_folder_ok, false);
                        } else {
                            dlg_msg.setText("");
                        }
                    } else {
                        dlg_msg.setText("");
                    }

                    String new_name ="";
                    if (e_msg.equals("")) {
                        new_name = removeInvalidCharForFileDirName(s.toString());
                        if (s.length() != new_name.length()) {
                            //remove invalid char
                            et_sync_folder_dir_name.setText(new_name);
                            if (new_name.length() > 0) et_sync_folder_dir_name.setSelection(new_name.length());
                            mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_task_sync_task_dlg_dir_name_has_invalid_char)
                                    , "", null);
                            s.clear();
                            s.append(new_name);
                        }
                    }

                    sfev.folder_directory=s.toString();
                    et_file_template.callOnClick();

                    setSyncFolderArchiveFileImage(dialog, sti, new_name, true);
                }
                if (e_msg.equals("")) setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
        });

        sp_sync_folder_local_storage_selector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                checkSyncFolderValidation(dialog, sti, sfev);
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
                LocalStorageSelectorItem sl=(LocalStorageSelectorItem)sp_sync_folder_local_storage_selector.getSelectedItem();
                mUtil.addDebugMsg(1,"I","isExfatFileSystem="+CommonUtilities.isExfatFileSystem(sl.uuid));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        if (mGp.safMgr.isStoragePermissionRequired()) btn_sync_folder_permission.setVisibility(Button.VISIBLE);
        else btn_sync_folder_permission.setVisibility(Button.GONE);
        btn_sync_folder_permission.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        if (mGp.safMgr.isStoragePermissionRequired()) btn_sync_folder_permission.setVisibility(Button.VISIBLE);
                        else btn_sync_folder_permission.setVisibility(Button.GONE);
                        setSpinnerSyncFolderStorageSelector(sti, sp_sync_folder_local_storage_selector, sfev.folder_storage_uuid);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                requestLocalStoragePermission(mActivity, mGp, mUtil, ntfy);
            }
        });

        final Button btn_sync_folder_edit_internal_dir_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_edit_internal_dir_keyword);
//        if (sfev.is_source_folder) btn_sync_folder_edit_internal_dir_rule.setVisibility(Button.GONE);
        btn_sync_folder_edit_internal_dir_rule.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        boolean changed=(boolean)objects[0];
                        if (changed) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);
                        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                boolean disable_taken_date=false;
                if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) disable_taken_date=true;
                editDirectoryFileNameRule(true, disable_taken_date, sti, sfev, et_sync_folder_dir_name,
                        mContext.getString(R.string.msgs_task_sync_task_edit_directory_name_keyword), ntfy);
            }
        });

        btn_sync_folder_local_list_dir.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String sel = sp_sync_folder_type.getSelectedItem().toString();

                NotifyEvent ntfy = new NotifyEvent(mContext);
                //Listen setRemoteShare response
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] arg1) {
                        EditText et_sync_folder_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_name);
                        LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_local_storage_selector.getSelectedItem();
//                        Uri zip_uri = (Uri) arg1[0];
                        String fp=((String)arg1[1]).replace(sel_item.root_path, "");
                        if (fp.endsWith("/")) et_sync_folder_dir_name.setText(fp.substring(0, fp.length()-1));
                        else et_sync_folder_dir_name.setText(fp);
                    }

                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                        setDialogMsg(dlg_msg, "");
                    }
                });
                LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_local_storage_selector.getSelectedItem();
//                SafFile3 sf=new SafFile3(mContext, sel_item.root_path);
                CommonFileSelector2 fsdf=CommonFileSelector2.newInstance(true, !sfev.is_source_folder, true,
                                CommonFileSelector2.DIALOG_SELECT_CATEGORY_DIRECTORY,
                                true, true, sel_item.uuid, "", "", mContext.getString(R.string.msgs_select_local_dir));
                fsdf.showDialog(false, getActivity().getSupportFragmentManager(), fsdf, ntfy);

                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
        });
    }

    static public void requestLocalStoragePermission(final ActivityMain activity,
                                                     final GlobalParameters gp, final CommonUtilities ut, final NotifyEvent p_ntfy) {
//        final Spinner sp_sync_folder_local_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_selector);
        NotifyEvent ntfy=new NotifyEvent(activity);
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                ArrayList<String>uuid_list=(ArrayList<String>)objects[0];
                final NotifyEvent ntfy_response=new NotifyEvent(context);
                ntfy_response.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        final int resultCode=(Integer)objects[0];
                        final Intent data=(Intent)objects[1];
                        final String uuid=(String)objects[2];

                        if (resultCode == Activity.RESULT_OK) {
                            if (data==null || data.getDataString()==null) {
                                String msg=activity.getString(R.string.msgs_storage_permission_msg_grant_permission_failed_null);
                                ut.showCommonDialogWarn(false, msg, "", null);
                                ut.addLogMsg("E", "", msg, "");
                                return;
                            }
                            ut.addDebugMsg(1, "I", "Intent=" + data.getData().toString());
                            if (!gp.safMgr.isRootTreeUri(data.getData())) {
                                ut.addDebugMsg(1, "I", "Selected UUID="+ SafManager3.getUuidFromUri(data.getData().toString()));
                                String em=gp.safMgr.getLastErrorMessage();
                                if (em.length()>0) ut.addDebugMsg(1, "I", "SafMessage="+em);

                                NotifyEvent ntfy_retry = new NotifyEvent(context);
                                ntfy_retry.setListener(new NotifyEvent.NotifyEventListener() {
                                    @Override
                                    public void positiveResponse(Context c, Object[] o) {
                                        requestStoragePermissionByUuid(activity, ut, uuid, ntfy_response);
                                    }

                                    @Override
                                    public void negativeResponse(Context c, Object[] o) {}
                                });
                                ut.showCommonDialogWarn(true, context.getString(R.string.msgs_main_external_storage_select_retry_select_msg),
                                        data.getData().getPath(), ntfy_retry);
                            } else {
                                ut.addDebugMsg(1, "I", "Selected UUID="+SafManager3.getUuidFromUri(data.getData().toString()));
                                String em=gp.safMgr.getLastErrorMessage();
                                if (em.length()>0) ut.addDebugMsg(1, "I", "SafMessage="+em);
                                boolean rc=gp.safMgr.addUuid(data.getData());
                                if (!rc) {
                                    String msg=activity.getString(R.string.msgs_storage_permission_msg_add_uuid_failed);
                                    String saf_msg=gp.safMgr.getLastErrorMessage();
                                    ut.showCommonDialogWarn(false, msg, saf_msg, null);
                                    ut.addLogMsg("E", "", msg, "\n", saf_msg);
                                }
                                if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
                            }
                        } else {
                            ut.showCommonDialogWarn(false,
                                    context.getString(R.string.msgs_main_external_storage_request_permission),
                                    context.getString(R.string.msgs_main_external_storage_select_required_cancel_msg), null);

                        }
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                for(String uuid:uuid_list) {
                    requestStoragePermissionByUuid(activity, ut, uuid, ntfy_response);
                }
            }

            @Override
            public void negativeResponse(Context context, Object[] objects) {}
        });
        StoragePermission sp=new StoragePermission(activity, ntfy);
        sp.showDialog();

    }

    static public void requestStoragePermissionByUuid(final ActivityMain a, final CommonUtilities cu, final String uuid, final NotifyEvent ntfy) {
        cu.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" enterd");
        Intent intent = null;
        StorageManager sm = (StorageManager) a.getSystemService(Context.STORAGE_SERVICE);
        ArrayList<SafManager3.StorageVolumeInfo>vol_list=SafManager3.getStorageVolumeInfo(a);
        for(SafManager3.StorageVolumeInfo svi:vol_list) {
            if (svi.uuid.equals(uuid)) {
                if (Build.VERSION.SDK_INT>=29) {
                    if (!svi.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                        intent=svi.volume.createOpenDocumentTreeIntent();
                    }
                } else {
                    if (!svi.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                        intent=svi.volume.createAccessIntent(null);
                    }
                }
                if (intent!=null) {
                    try {
                        ActivityResultLauncher<Intent> activity_launcher = a.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                ntfy.notifyToListener(true, new Object[]{result.getResultCode(), result.getData(), uuid});
                            }
                        });
                        activity_launcher.launch(intent);
                    } catch(Exception e) {
                        String st= MiscUtil.getStackTraceString(e);
                        cu.showCommonDialog(false, "E",
                                a.getString(R.string.msgs_storage_permission_msg_saf_error_occured), e.getMessage()+"\n"+st, null);
                    }
                    break;
                }
            }
        }
    }

    private void setSyncFolderArchiveListener(final Dialog dialog, final SyncTaskItem n_sti, final SyncFolderEditValue sfev, final NotifyEvent ntfy) {

        final LinearLayout archive_option_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_view);

        if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
            if (sfev.is_source_folder) archive_option_view.setVisibility(LinearLayout.GONE);
            else archive_option_view.setVisibility(LinearLayout.VISIBLE);
        } else {
            archive_option_view.setVisibility(LinearLayout.GONE);
        }

        final Spinner sp_sync_retain_period = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_retention_period);
        final Spinner sp_sync_suffix_option = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_suffix_seqno);
        sp_sync_suffix_option.setOnItemSelectedListener(null);
        setSpinnerSyncTaskArchiveSuffixSeq(sp_sync_suffix_option, sfev.archive_file_name_suffix_digit);
        sp_sync_retain_period.setOnItemSelectedListener(null);
        setSpinnerSyncTaskPictureRetainPeriod(sp_sync_retain_period, Integer.valueOf(sfev.archive_retention_period));

        final CheckedTextView ctv_ignore_source_directory_hierarchy=(CheckedTextView)dialog.findViewById(R.id.edit_sync_folder_dlg_archive_ignore_source_directory_hierarchy);
        ctv_ignore_source_directory_hierarchy.setChecked(sfev.archive_ignore_source_directory_hiearachy);
        ctv_ignore_source_directory_hierarchy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!ctv_ignore_source_directory_hierarchy.isChecked();
                ctv_ignore_source_directory_hierarchy.setChecked(isChecked);
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
        });

        final LinearLayout template_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_template_view);
        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);
        et_file_template.setTextColor(mGp.themeColorList.text_color_primary);

        final Button btn_sync_folder_edit_file_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_keyword);
        if (sfev.is_source_folder) btn_sync_folder_edit_file_rule.setVisibility(Button.GONE);
        btn_sync_folder_edit_file_rule.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        boolean changed=(boolean)objects[0];
//                        if (changed) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);
//                        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                editDirectoryFileNameRule(false, false, n_sti, sfev, et_file_template,
                        mContext.getString(R.string.msgs_task_sync_task_edit_file_name_keyword), ntfy);
            }
        });

        final TextView tv_template = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_new_name);

        sp_sync_suffix_option.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                et_file_template.callOnClick();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        et_file_template.setText(sfev.archive_file_name_template);
        et_file_template.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_template.setText(getSyncTaskArchiveTemplateNewName(sp_sync_suffix_option.getSelectedItemPosition(),
                        et_file_template.getText().toString(), sfev.folder_directory, n_sti));
                checkArchiveOkButtonEnabled(sfev, n_sti, dialog);
            }
        });
        et_file_template.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    for(int i = s.length()-1; i >= 0; i--){
                        if(s.charAt(i) == '\n'){
                            s.delete(i, i + 1);
                            break;
                        }
                    }

                    int prev_length=s.length();
                    for(int i = s.length()-1; i >= 0; i--){
                        if(s.charAt(i) == '/' || s.charAt(i) == '"' || s.charAt(i) == ':' || s.charAt(i) == '\\' || s.charAt(i) == '*'
                                || s.charAt(i) == '<' || s.charAt(i) == '>' || s.charAt(i) == '|'){
                            s.delete(i, i + 1);
                            break;
                        }
                    }

                    if (s.length()!=prev_length) {
                        mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_task_sync_task_dlg_file_name_has_invalid_char)
                                , "", null);
                    }
                    tv_template.setText(getSyncTaskArchiveTemplateNewName(sp_sync_suffix_option.getSelectedItemPosition(),
                            et_file_template.getText().toString(), sfev.folder_directory, n_sti));
                    checkArchiveOkButtonEnabled(sfev, n_sti, dialog);
                } else {
                    checkArchiveOkButtonEnabled(sfev, n_sti, dialog);
                }
            }
        });
        tv_template.setText(getSyncTaskArchiveTemplateNewName(sp_sync_suffix_option.getSelectedItemPosition(),
                et_file_template.getText().toString(), sfev.folder_directory, n_sti));


        sp_sync_retain_period.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                tv_template.setText(getSyncTaskArchiveTemplateNewName(sp_sync_suffix_option.getSelectedItemPosition(),
                        et_file_template.getText().toString(), sfev.folder_directory, n_sti));
                checkArchiveOkButtonEnabled(sfev, n_sti, dialog);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void setSyncFolderArchiveFileImage(final Dialog dialog, SyncTaskItem n_sti, final String destination_dir, boolean create_directory) {
        final Spinner sp_sync_retain_period = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_retention_period);

        final LinearLayout template_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_template_view);
        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);
        final TextView tv_template = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_new_name);
    }

    private void setSyncFolderZipListener(final Dialog dialog, final SyncTaskItem sti, final SyncFolderEditValue sfev, final NotifyEvent ntfy) {
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);
        final CheckedTextView ctv_sync_folder_show_zip_password = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_show_zip_password);

        if (mGp.settingSecurityHideShowPasswordButton) ctv_sync_folder_show_zip_password.setVisibility(CheckedTextView.GONE);
        else ctv_sync_folder_show_zip_password.setVisibility(CheckedTextView.VISIBLE);

        final Spinner sp_sync_folder_zip_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_storage_selector);
        sp_sync_folder_zip_storage_selector.setOnItemSelectedListener(null);
        setSpinnerSyncFolderStorageSelector(sti, sp_sync_folder_zip_storage_selector, sfev.folder_storage_uuid);
        final Button btn_sync_folder_permission = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_local_storage_request_permission);

        sp_sync_folder_zip_storage_selector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                checkSyncFolderValidation(dialog, sti, sfev);
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        if (mGp.safMgr.isStoragePermissionRequired()) btn_sync_folder_permission.setVisibility(Button.VISIBLE);
        else btn_sync_folder_permission.setVisibility(Button.GONE);
        btn_sync_folder_permission.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        if (mGp.safMgr.isStoragePermissionRequired()) btn_sync_folder_permission.setVisibility(Button.VISIBLE);
                        else btn_sync_folder_permission.setVisibility(Button.GONE);
                        setSpinnerSyncFolderStorageSelector(sti, sp_sync_folder_zip_storage_selector, sfev.folder_storage_uuid);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                requestLocalStoragePermission(mActivity, mGp, mUtil, ntfy);
            }
        });

        final LinearLayout ll_zip_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_view);

        final Button btn_zip_filelist = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_filelist_btn);
        final EditText et_zip_file = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_file_name);
        final TextView tv_zip_dir = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_dir_name);

        String zip_dir="", zip_file="";
        if (sfev.zip_file_name.lastIndexOf("/")>1) {
            //Directory付
            zip_dir="/"+sfev.zip_file_name.substring(0,sfev.zip_file_name.lastIndexOf("/"));
            zip_file=sfev.zip_file_name.substring(sfev.zip_file_name.lastIndexOf("/")+1);
        } else {
            zip_dir="/";
            if (sfev.zip_file_name.length()!=0) zip_file=sfev.zip_file_name;
        }
        tv_zip_dir.setText(zip_dir);
        et_zip_file.setText(zip_file);

        final Button btn_sync_folder_edit_zip_dir_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_edit_zip_dir_keyword);
        if (sfev.is_source_folder) btn_sync_folder_edit_zip_dir_rule.setVisibility(Button.GONE);
        btn_sync_folder_edit_zip_dir_rule.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        boolean changed=(boolean)objects[0];
                        if (changed) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);
                        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                boolean disable_taken_date=false;
                if (sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) disable_taken_date=true;
                editDirectoryFileNameRule(true, disable_taken_date, sti, sfev, et_zip_file,
                        mContext.getString(R.string.msgs_task_sync_task_edit_file_name_keyword), ntfy);
            }
        });

        final Spinner sp_comp_level = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_comp_level);
        sp_comp_level.setOnItemSelectedListener(null);
        setSpinnerSyncFolderZipCompressionLevel(sp_comp_level, sfev.zip_comp_level);
        final Spinner sp_zip_enc_method=(Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_method);
        sp_zip_enc_method.setOnItemSelectedListener(null);
        setSpinnerSyncFolderZipEncryptMethod(sp_zip_enc_method, sfev.zip_enc_method);
        final LinearLayout ll_zip_password_view=(LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_password_view);
        final EditText et_zip_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_password);
        final EditText et_zip_conf_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_confirm);
//        final Button btn_zip_select_sdcard = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_select_document_tree);
        final LinearLayout ll_conf_pswd_view=(LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_confirm_view);

        ctv_sync_folder_show_zip_password.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!ctv_sync_folder_show_zip_password.isChecked();
                ctv_sync_folder_show_zip_password.setChecked(isChecked);
                if (isChecked) {
                    et_zip_pswd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ll_conf_pswd_view.setVisibility(LinearLayout.GONE);
                } else {
                    et_zip_pswd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ll_conf_pswd_view.setVisibility(LinearLayout.VISIBLE);
                }
            }
        });

        if (sfev.zip_enc_method.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_NONE)) {
            ll_zip_password_view.setVisibility(LinearLayout.GONE);
        } else {
            ll_zip_password_view.setVisibility(LinearLayout.VISIBLE);
        }
        if (!mGp.settingSecurityReinitZipPasswordValue) {
            et_zip_pswd.setText(sfev.zip_file_password);
            et_zip_conf_pswd.setText(sfev.zip_file_password);
        }
        sp_zip_enc_method.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String zip_enc_method=(String)sp_zip_enc_method.getSelectedItem();
                if (!zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_none))) {
                    ll_zip_password_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    ll_zip_password_view.setVisibility(LinearLayout.GONE);
                }
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
                checkSyncFolderValidation(dialog, sti, sfev);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sp_comp_level.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btn_zip_filelist.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_zip_storage_selector.getSelectedItem();
                        Uri zip_uri = (Uri) o[0];
                        String fd=((String)o[2]).replace(sel_item.root_path, "");
                        String fn=(String)o[3];
                        EditText et_zip_file = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_file_name);
                        TextView tv_zip_dir = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_dir_name);
                        et_zip_file.setText(fn);
                        tv_zip_dir.setText(fd.equals("")?"/":fd);
                        setSyncFolderOkButtonEnabledIfFolderChanged(dialog, sfev);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_zip_storage_selector.getSelectedItem();
                String uuid=sel_item.uuid;

                String title = mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_select_file_title);
                CommonFileSelector2 fsdf=
                        CommonFileSelector2.newInstance(true, true, true, CommonFileSelector2.DIALOG_SELECT_CATEGORY_FILE, true, uuid, "", "", title);
                fsdf.showDialog(false, getActivity().getSupportFragmentManager(), fsdf, ntfy);
            }
        });
        et_zip_file.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                for(int i = s.length()-1; i >= 0; i--){
                    if(s.charAt(i) == '\n'){
                        s.delete(i, i + 1);
                        checkSyncFolderValidation(dialog, sti, sfev);
                        return;
                    }
                }
                if (s.charAt(s.length()-1)=='/' || s.charAt(s.length()-1)=='"'
                        || s.charAt(s.length()-1)==':'
                        || s.charAt(s.length()-1)=='\\'
                        || s.charAt(s.length()-1)=='*'
                        || s.charAt(s.length()-1)=='<'
                        || s.charAt(s.length()-1)=='>'
                        || s.charAt(s.length()-1)=='|') {
                    s.delete(s.length()-1, s.length());
                    mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_task_sync_task_dlg_file_name_has_invalid_char), "", null);
                }
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });
        et_zip_pswd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) CommonDialog.setViewEnabled(getActivity(), et_zip_conf_pswd, true);
                else CommonDialog.setViewEnabled(getActivity(), et_zip_conf_pswd, false);
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });

        if (et_zip_pswd.getText().length() > 0) CommonDialog.setViewEnabled(getActivity(), et_zip_conf_pswd, true);
        else CommonDialog.setViewEnabled(getActivity(), et_zip_conf_pswd, false);

        et_zip_conf_pswd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkSyncFolderValidation(dialog, sti, sfev);
            }
        });
    }

    private Dialog mEditFolderDialog=null;
    private SyncFolderEditValue mEditFolderSfev=null;
    private SyncTaskItem mEditFolderSti=null;
    private NotifyEvent mEditFolderNotify=null;

    private void editSyncFolder(boolean rebuild, SyncTaskItem sti, final SyncFolderEditValue sfev, final NotifyEvent ntfy) {
        mUtil.addDebugMsg(1, "I", "editSyncFolder entered");
        Dialog t_dialog = null;
        if (rebuild) {
            t_dialog = mEditFolderDialog;
            t_dialog.setContentView(R.layout.edit_sync_folder_dlg);
        } else {
            t_dialog = new Dialog(this.getActivity(), mGp.applicationTheme);
            mEditFolderDialog=t_dialog;
            mEditFolderSfev=sfev;
            mEditFolderSti=sti;
            mEditFolderNotify=ntfy;
            t_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            t_dialog.setCanceledOnTouchOutside(false);
            t_dialog.setContentView(R.layout.edit_sync_folder_dlg);
        }
        final Dialog dialog = t_dialog;

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);

        final Spinner spinnerSyncType = (Spinner) mDialog.findViewById(R.id.edit_sync_task_sync_type);


        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_title);
        dlg_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);
        dlg_title.setText(sfev.folder_title);

        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        sp_sync_folder_type.setOnItemSelectedListener(null);
        setSpinnerSyncFolderType(sti, sp_sync_folder_type, sfev.folder_type, sfev.is_source_folder);

        if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
            if (sfev.folder_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                mUtil.showCommonDialogWarn(false,
                        mContext.getString(R.string.msgs_sync_folder_archive_zip_folder_not_supported), "", null);
            }
        }

        setSyncFolderSmbListener(dialog, sti, sfev, ntfy);
        setSyncFolderLocalListener(dialog, sti, sfev, ntfy);
        setSyncFolderArchiveListener(dialog, sti, sfev, ntfy);
        setSyncFolderZipListener(dialog, sti, sfev, ntfy);

        setSyncFolderFieldHelpListener(dialog, sfev.folder_type);

        final Button btn_sync_folder_cancel = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_cancel);
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);

        setSyncFolderViewVisibility(dialog, sti, sfev.is_source_folder, sfev);

        sp_sync_folder_type.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSyncFolderViewVisibility(dialog, sti, sfev.is_source_folder, sfev);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
        btn_sync_folder_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncFolderEditValue nsfev = buildSyncFolderEditValue(dialog, sfev);
                final TextView tv_template = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_new_name);
                nsfev.folder_error_code= SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR;
                ntfy.notifyToListener(true, new Object[]{nsfev});
                dialog.dismiss();
                mEditFolderDialog=null;
            }
        });

        btn_sync_folder_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_sync_folder_ok.isEnabled()) {
                    NotifyEvent ntfy = new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                            mEditFolderDialog=null;
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    mUtil.showCommonDialogWarn(true,
                            mContext.getString(R.string.msgs_task_sync_folder_dlg_confirm_msg_nosave), "", ntfy);
                } else {
                    ntfy.notifyToListener(false, null);
                    dialog.dismiss();
                    mEditFolderDialog=null;
                }
            }
        });

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int kc, KeyEvent keyEvent) {
                switch (kc) {
                    case KEYCODE_BACK:
                        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            btn_sync_folder_cancel.performClick();
                        }
                        return true;
                    default:
                }
                return false;
            }
        });

        dialog.show();
    }

//    private boolean mDirectoryNameRuleUseTakenDate=false;
//    private boolean mFileNameRuleUseTakenDate=false;

    private void editDirectoryFileNameRule(boolean edit_directory_rule, boolean disable_taken_date, final SyncTaskItem sti,
                                           final SyncFolderEditValue sfev, final EditText etInput,
                                           final String title_text, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.setContentView(R.layout.edit_sync_folder_edit_keywor_dlg);

        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_msg);
        dlg_msg.setVisibility(TextView.GONE);

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        TextView dlg_title = (TextView) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_title);
        dlg_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);
        dlg_title.setText(title_text);
        ImageButton ib_help=(ImageButton)dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_help);
        setSyncFolderKeywordHelpListener(dialog, !disable_taken_date);
        final Button dlg_ok = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_ok);
        CommonDialog.setViewEnabled(mActivity, dlg_ok,false);
        final Button dlg_cancel = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_cancel);

        final NonWordwrapTextView dlg_image = (NonWordwrapTextView) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_image);

        final NonWordwrapTextView dlg_taken_title=(NonWordwrapTextView)dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_taken_title);
        final NonWordwrapTextView dlg_exec_title=(NonWordwrapTextView)dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_exec_title);
        dlg_taken_title.setText(mContext.getString(R.string.msgs_sync_folder_archive_add_taken_date_button));
        dlg_taken_title.setWordWrapEnabled(true);
        dlg_exec_title.setText(mContext.getString(R.string.msgs_sync_folder_archive_add_sync_begin_date_button));
        dlg_exec_title.setWordWrapEnabled(true);

        final LinearLayout dlg_ll_taken = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_view);
        final Button dlg_taken_date = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_date);
        final Button dlg_taken_time = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_time);
        final Button dlg_taken_day_of_year = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_day_of_year);
        final Button dlg_taken_yyyy = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_yyyy);
        final Button dlg_taken_yy = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_yy);
        final Button dlg_taken_mm = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_mm);
        final Button dlg_taken_dd = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_dd);
        final Button dlg_taken_hh = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_hh);
        final Button dlg_taken_min = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_min);
        final Button dlg_taken_sec = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_sec);
        final Button dlg_taken_week_number = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_week_number);
        final Button dlg_taken_week_day = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_week_day);
        final Button dlg_taken_week_day_long = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_taken_week_day_long);
        if (disable_taken_date) dlg_ll_taken.setVisibility(LinearLayout.GONE);

        final Button dlg_exec_date = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_date);
        final Button dlg_exec_time = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_time);
        final Button dlg_exec_day_of_year = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_day_of_year);
        final Button dlg_exec_yyyy = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_yyyy);
        final Button dlg_exec_yy = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_yy);
        final Button dlg_exec_mm = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_mm);
        final Button dlg_exec_dd = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_dd);
        final Button dlg_exec_hh = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_hh);
        final Button dlg_exec_min = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_min);
        final Button dlg_exec_sec = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_sec);
        final Button dlg_exec_week_number = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_week_number);
        final Button dlg_exec_week_day = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_week_day);
        final Button dlg_exec_week_day_long = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_exec_week_day_long);

        final LinearLayout ll_dlg_file_name = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_file_name_view);
        final Button dlg_org_name = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_org_name);

        final Button dlg_const_minus = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_const_minus);
        final Button dlg_const_slash = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_const_slash);
        final Button dlg_const_underbar = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_const_underbar);

        final EditText dlg_keyword = (EditText) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_keyword);

        CommonDialog.setDlgBoxSizeCompact(dialog);

        if (edit_directory_rule) {
            ll_dlg_file_name.setVisibility(LinearLayout.GONE);
        }

        if (sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
            dlg_ll_taken.setVisibility(LinearLayout.GONE);
        } else {
            if (sfev.is_source_folder) {
                dlg_ll_taken.setVisibility(LinearLayout.GONE);
            }
        }

        dlg_keyword.setText(etInput.getText());
        dlg_keyword.requestFocus();
        dlg_keyword.setSelection(etInput.getText().length());
        dlg_keyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    for(int i = s.length()-1; i >= 0; i--){
                        if(s.charAt(i) == '\n'){
                            s.delete(i, i + 1);
                            break;
                        }
                    }
                    String cv=getConvertedDirectoryFileName(s.toString(), System.currentTimeMillis(), "DSC-001");
                    dlg_image.setText(cv);
                    if (etInput.getText().toString().equals(dlg_keyword.getText().toString())) CommonDialog.setViewEnabled(mActivity, dlg_ok, false);
                    else CommonDialog.setViewEnabled(mActivity, dlg_ok, true);
                } else {
                    CommonDialog.setViewEnabled(mActivity, dlg_ok, false);
                }
            }
        });
        String cv=getConvertedDirectoryFileName(dlg_keyword.getText().toString(), System.currentTimeMillis(), "DSC-001");
        dlg_image.setText(cv);

        if (edit_directory_rule) {
            dlg_const_slash.setVisibility(Button.VISIBLE);
        } else {
            dlg_const_slash.setVisibility(Button.GONE);
        }

        setSyncFolderKeywordButtonListener(dialog, dlg_taken_date, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_DATE, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_time, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_TIME, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_day_of_year, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_DAY_OF_YEAR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_yyyy, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_YEAR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_yy, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_YY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_mm, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_MONTH, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_dd, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_DAY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_hh, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_HOUR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_min, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_MIN, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_sec, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_SEC, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_week_number, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_WEEK_NUMBER, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_week_day, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_taken_week_day_long, dlg_keyword, SyncTaskItem.TEMPLATE_TAKEN_WEEK_DAY_LONG, etInput.getText().toString());

        setSyncFolderKeywordButtonListener(dialog, dlg_exec_date, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_DATE, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_time, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_TIME, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_day_of_year, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_DAY_OF_YEAR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_yyyy, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_YEAR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_yy, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_YY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_mm, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_MONTH, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_dd, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_DAY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_hh, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_HOUR, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_min, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_MIN, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_sec, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_SEC, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_week_number, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_WEEK_NUMBER, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_week_day, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY, etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_exec_week_day_long, dlg_keyword, SyncTaskItem.TEMPLATE_EXEC_WEEK_DAY_LONG, etInput.getText().toString());

        setWeekDayButtonLabel(dlg_taken_week_day, dlg_taken_week_day_long,
                mContext.getString(R.string.msgs_sync_folder_archive_btn_title_week_day),
                mContext.getString(R.string.msgs_sync_folder_archive_btn_title_week_day_long));
        setWeekDayButtonLabel(dlg_exec_week_day, dlg_exec_week_day_long,
                mContext.getString(R.string.msgs_sync_folder_archive_btn_title_week_day),
                mContext.getString(R.string.msgs_sync_folder_archive_btn_title_week_day_long));

        setSyncFolderKeywordButtonListener(dialog, dlg_org_name, dlg_keyword, TEMPLATE_ORIGINAL_NAME, etInput.getText().toString());

        setSyncFolderKeywordButtonListener(dialog, dlg_const_minus, dlg_keyword, "-", etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_const_slash, dlg_keyword, "/", etInput.getText().toString());
        setSyncFolderKeywordButtonListener(dialog, dlg_const_underbar, dlg_keyword, "_", etInput.getText().toString());

        final String init_dir=etInput.getText().toString();
        dlg_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                etInput.setText(dlg_keyword.getText());
                dialog.dismiss();
            }
        });

        dlg_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dlg_ok.isEnabled()) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            dialog.dismiss();
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    mUtil.showCommonDialogWarn(true, mContext.getString(R.string.msgs_task_sync_task_edit_keyword_quit_warning), "", ntfy);
                } else {
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    private void setWeekDayButtonLabel(Button short_button, Button long_button, String short_label, String long_label) {
        Date date_sunday=new Date("2020/07/26");
        Date date_monday=new Date("2020/07/27");
        String s_sunday=SyncThread.getWeekday(date_sunday.getTime());
        String s_monday=SyncThread.getWeekday(date_monday.getTime());
        String l_sunday=SyncThread.getWeekdayLong(date_sunday.getTime());
        String l_monday=SyncThread.getWeekdayLong(date_monday.getTime());
        String new_short_label=String.format(short_label, s_sunday, s_monday);
        String new_long_label=String.format(long_label, l_sunday, l_monday);

        short_button.setText(new_short_label);
        long_button.setText(new_long_label);

    }

    private void setSyncFolderKeywordButtonListener(final Dialog dialog, final Button btn, final EditText et, final String key_word, String original_string) {
        final NonWordwrapTextView dlg_image = (NonWordwrapTextView) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_image);
        final Button dlg_ok = (Button) dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_btn_ok);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et.getSelectionStart() == et.getSelectionEnd()) et.getText().insert(et.getSelectionStart(), key_word);
                else et.getText().replace(et.getSelectionStart(), et.getSelectionEnd(), key_word);
                String cv=getConvertedDirectoryFileName(et.getText().toString(), System.currentTimeMillis(), "DSC-001");
                dlg_image.setText(cv);
                if (et.getText().toString().equals(original_string.toString())) CommonDialog.setViewEnabled(mActivity, dlg_ok, false);
                else CommonDialog.setViewEnabled(mActivity, dlg_ok, true);

            }
        });
    }

    private SyncFolderEditValue buildSyncFolderEditValue(Dialog dialog, SyncFolderEditValue org_sfev) {
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);
        final LinearLayout ll_sync_folder_local_storage = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_storage_selector_view);
        final Spinner sp_sync_folder_local_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_selector);
        final Spinner sp_sync_folder_zip_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_storage_selector);

        final EditText et_remote_host = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
//        final CheckedTextView ctv_sync_folder_smb_ipc_enforced = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_ipc_signing_enforced);
//        final CheckedTextView ctv_sync_folder_smb_use_smb2_negotiation = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_use_smb2_negotiation);
        final CheckedTextView ctv_sync_folder_use_port = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
        final EditText et_sync_folder_port = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);
        final CheckedTextView ctv_sync_folder_use_pswd = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText et_sync_folder_domain = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_domain);
        final EditText et_sync_folder_user = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText et_sync_folder_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);

        final Button btn_sync_folder_list_share = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_share_btn);
        final EditText et_sync_folder_share_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);
        final Button btn_sync_folder_local_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_internal_directory_btn);
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);

        final EditText et_sync_folder_internal_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_name);
        final EditText et_sync_folder_smb_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_directory_name);


        final LinearLayout ll_dir_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_view);
        final LinearLayout ll_zip_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_view);

        final TextView tv_zip_dir = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_dir_name);
        final EditText et_zip_file = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_file_name);
        final Spinner sp_comp_level = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_comp_level);
        final Spinner sp_zip_enc_method=(Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_method);
        final EditText et_zip_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_password);
        final EditText et_zip_conf_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_confirm);

        final Spinner sp_sync_retain_period = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_retention_period);
        final Spinner sp_sync_suffix_option = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_suffix_seqno);
        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);

        final CheckedTextView ctv_ignore_source_directory_hierarchy=(CheckedTextView)dialog.findViewById(R.id.edit_sync_folder_dlg_archive_ignore_source_directory_hierarchy);

        SyncFolderEditValue nsfev = org_sfev.clone();

        String sel = sp_sync_folder_type.getSelectedItem().toString();
        if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local))) {//Internal
            String dir=et_sync_folder_internal_dir_name.getText().toString().trim().startsWith("/")?
                    et_sync_folder_internal_dir_name.getText().toString().trim().substring(1):
                    et_sync_folder_internal_dir_name.getText().toString().trim();
            nsfev.folder_directory=dir;
            while(nsfev.folder_directory.endsWith("/")) {
                nsfev.folder_directory=nsfev.folder_directory.substring(0, nsfev.folder_directory.length()-1);
            }
            LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_local_storage_selector.getSelectedItem();
            if (sel_item.mounted) {
                SafFile3 sf=new SafFile3(mContext, sel_item.root_path);
                String uuid=sf.getUuid();
                nsfev.folder_storage_uuid = uuid;
            }
            nsfev.folder_type = SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL;
            if (nsfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                nsfev.archive_file_name_template=et_file_template.getText().toString();
                nsfev.archive_retention_period=String.valueOf(sp_sync_retain_period.getSelectedItemPosition());
                nsfev.archive_file_name_suffix_digit=getArchiveSuffixOptionFromSpinner(sp_sync_suffix_option);
                nsfev.archive_ignore_source_directory_hiearachy =ctv_ignore_source_directory_hierarchy.isChecked();
            }
        } else if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_zip))) {//ZIP
            LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_zip_storage_selector.getSelectedItem();
            String uuid=sel_item.uuid;
            nsfev.folder_storage_uuid = uuid;

            String cl = sp_comp_level.getSelectedItem().toString();
            if (cl.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_fastest))) nsfev.zip_comp_level = SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_FASTEST;
            else if (cl.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_fast))) nsfev.zip_comp_level = SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_FAST;
            else if (cl.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_normal))) nsfev.zip_comp_level = SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_NORMAL;
            else if (cl.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_maximum))) nsfev.zip_comp_level = SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_MAXIMUM;
//            else if (cl.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_ultra))) nsfev.zip_comp_level = TaskListItem.ZIP_OPTION_COMP_LEVEL_ULTRA;

            String zip_enc_method = sp_zip_enc_method.getSelectedItem().toString();
            if (zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_none))) nsfev.zip_enc_method = SyncTaskItem.ZIP_OPTION_ENCRYPT_NONE;
            else if (zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_zip_crypto))) nsfev.zip_enc_method = SyncTaskItem.ZIP_OPTION_ENCRYPT_STANDARD;
            else if (zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_aes128))) nsfev.zip_enc_method = SyncTaskItem.ZIP_OPTION_ENCRYPT_AES128;
            else if (zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_aes256))) nsfev.zip_enc_method = SyncTaskItem.ZIP_OPTION_ENCRYPT_AES256;

            if (tv_zip_dir.getText().toString().trim().equals("/")) nsfev.zip_file_name=et_zip_file.getText().toString().trim();
            else nsfev.zip_file_name=tv_zip_dir.getText().toString().trim().substring(1)+"/"+et_zip_file.getText().toString().trim();

            if (!nsfev.zip_enc_method.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_NONE)) {
                nsfev.zip_file_password = et_zip_pswd.getText().toString();
            } else {
                nsfev.zip_file_password = "";
            }

            if (mGp.settingSecurityReinitZipPasswordValue && et_zip_pswd.getText().toString().length()>0)  nsfev.isChanged=true;

            nsfev.folder_type = SyncTaskItem.SYNC_FOLDER_TYPE_ZIP;
        } else if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb))) {//smb
            String dir=et_sync_folder_smb_dir_name.getText().toString().trim().startsWith("/")?
                    et_sync_folder_smb_dir_name.getText().toString().trim().substring(1):
                    et_sync_folder_smb_dir_name.getText().toString().trim();
            nsfev.folder_directory=dir;
            while(nsfev.folder_directory.endsWith("/")) {
                nsfev.folder_directory=nsfev.folder_directory.substring(0, nsfev.folder_directory.length()-1);
            }
            nsfev.folder_type = SyncTaskItem.SYNC_FOLDER_TYPE_SMB;
            String host=et_remote_host.getText().toString();
            if (CommonUtilities.isIpAddressV6(host) || CommonUtilities.isIpAddressV4(host)) {
                nsfev.folder_smb_addr = host;
                nsfev.folder_smb_hostname ="";
            } else {
                nsfev.folder_smb_hostname = host;
                nsfev.folder_smb_addr ="";
            }
            nsfev.folder_smb_domain = et_sync_folder_domain.getText().toString().trim();
            if (ctv_sync_folder_use_port.isChecked())
                nsfev.folder_smb_port = et_sync_folder_port.getText().toString();
            else nsfev.folder_smb_port = "";
            if (ctv_sync_folder_use_pswd.isChecked()) {
                nsfev.folder_smb_account = et_sync_folder_user.getText().toString().trim();
                nsfev.folder_smb_password = et_sync_folder_pswd.getText().toString();
            } else {
                nsfev.folder_smb_account = "";
                nsfev.folder_smb_password = "";
            }

            if (mGp.settingSecurityReinitSmbAccountPasswordValue &&
                    (et_sync_folder_user.getText().toString().length()>0 || et_sync_folder_pswd.getText().toString().length()>0) )  nsfev.isChanged=true;

            nsfev.folder_smb_use_pswd =ctv_sync_folder_use_pswd.isChecked();
            nsfev.folder_smb_share = et_sync_folder_share_name.getText().toString().trim();
            nsfev.folder_smb_protocol=getSmbSelectedProtocol(sp_sync_folder_smb_proto);
            if (nsfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                nsfev.archive_file_name_template=et_file_template.getText().toString();
                nsfev.archive_retention_period=String.valueOf(sp_sync_retain_period.getSelectedItemPosition());
                nsfev.archive_file_name_suffix_digit=getArchiveSuffixOptionFromSpinner(sp_sync_suffix_option);
                nsfev.archive_ignore_source_directory_hiearachy =ctv_ignore_source_directory_hierarchy.isChecked();
            }
        }
        return nsfev;
    }

    private void setSyncFolderOkButtonEnabled(Button ok_btn, boolean enabled) {
        CommonDialog.setViewEnabled(getActivity(), ok_btn, enabled);
    }

    private void setSyncFolderSmbListDirectoryButtonEnabled(Dialog dialog, boolean enabled) {
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);
        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, enabled);
    }

    private void setSyncFolderViewVisibility(final Dialog dialog, SyncTaskItem sti, final boolean source, SyncFolderEditValue org_sfev) {
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);
        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final LinearLayout ll_sync_folder_local_storage = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_storage_selector_view);
        final Spinner sp_sync_folder_local_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_selector);
        final Spinner sp_sync_folder_smb_proto = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_protocol);
//        final CheckedTextView ctv_sync_folder_smb_ipc_enforced = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_ipc_signing_enforced);
//        final CheckedTextView ctv_sync_folder_smb_use_smb2_negotiation = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_smb_use_smb2_negotiation);
        final LinearLayout ll_sync_folder_smb_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_smb_view);
        final LinearLayout ll_sync_folder_internal_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_view);
        final LinearLayout ll_sync_folder_zip_view = (LinearLayout) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_view);

        final Button btn_sync_folder_local_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_internal_directory_btn);
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);
        final Button btn_sync_folder_edit_dir_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_edit_smb_dir_keyword);

        final LinearLayout ll_internal_keyword_view = (LinearLayout) ll_sync_folder_internal_view.findViewById(R.id.edit_sync_folder_dlg_internal_dir_keyword_view);
        final LinearLayout ll_smb_keyword_view = (LinearLayout) ll_sync_folder_smb_view.findViewById(R.id.edit_sync_folder_dlg_smb_dir_keyword_view);

        setSyncFolderSmbListDirectoryButtonEnabled(dialog, true);
        String sel = sp_sync_folder_type.getSelectedItem().toString();

//        if (source) {
//            ll_internal_keyword_view.setVisibility(LinearLayout.GONE);
//            ll_smb_keyword_view.setVisibility(LinearLayout.GONE);
//        } else {
//            ll_internal_keyword_view.setVisibility(LinearLayout.VISIBLE);
//            ll_smb_keyword_view.setVisibility(LinearLayout.VISIBLE);
//        }
        if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb))) {
            ll_sync_folder_internal_view.setVisibility(LinearLayout.GONE);
            ll_sync_folder_smb_view.setVisibility(LinearLayout.VISIBLE);
            ll_sync_folder_zip_view.setVisibility(LinearLayout.GONE);
            checkSyncFolderValidation(dialog, sti, org_sfev);
            setSyncFolderFieldHelpListener(dialog, SyncTaskItem.SYNC_FOLDER_TYPE_SMB);
        } else if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local))) {
            ll_sync_folder_internal_view.setVisibility(LinearLayout.VISIBLE);
            ll_sync_folder_smb_view.setVisibility(LinearLayout.GONE);
            ll_sync_folder_zip_view.setVisibility(LinearLayout.GONE);
            checkSyncFolderValidation(dialog, sti, org_sfev);
            setSyncFolderFieldHelpListener(dialog, SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
        } else if (sel.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_zip))) {
            ll_sync_folder_internal_view.setVisibility(LinearLayout.GONE);
            ll_sync_folder_smb_view.setVisibility(LinearLayout.GONE);
            ll_sync_folder_zip_view.setVisibility(LinearLayout.VISIBLE);
            checkSyncFolderValidation(dialog, sti, org_sfev);
            setSyncFolderFieldHelpListener(dialog, SyncTaskItem.SYNC_FOLDER_TYPE_ZIP);
        }
    }

    private boolean checkSyncFolderValidation(Dialog dialog, SyncTaskItem sti, SyncFolderEditValue org_sfev) {
        boolean result = true;
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.edit_sync_folder_dlg_msg);
        setDialogMsg(dlg_msg, "");

        final Spinner sp_sync_folder_type = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_folder_type);
        final CheckedTextView ctv_sync_folder_edit_smb_detail = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_edit_smb_server_detail);
        final Spinner sp_sync_folder_local_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_selector);
        final Spinner sp_sync_folder_zip_storage_selector = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_storage_selector);
        final Button btn_sync_folder_permission = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_local_storage_request_permission);

        final Button btn_sync_folder_local_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_internal_directory_btn);

        final EditText et_remote_host = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_server);
        final CheckedTextView ctv_sync_folder_use_port = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_remote_port_number);
        final EditText et_sync_folder_port = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_port);

        final CheckedTextView ctv_sync_folder_use_pswd = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_user_pass);
        final EditText et_sync_folder_user = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_user);
        final EditText et_sync_folder_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_remote_pass);

        final EditText et_sync_folder_share_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_share_name);

        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);

//		final LinearLayout ll_sync_folder_smb_view = (LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_smb_host_view);
//		final Button btn_search_host = (Button)dialog.findViewById(R.id.edit_sync_folder_dlg_search_remote_host);
//		final EditText et_sync_folder_domain = (EditText)dialog.findViewById(R.id.edit_sync_folder_dlg_remote_domain);
        final Button btn_sync_folder_smb_list_share = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_share_btn);
        final Button btn_sync_folder_smb_list_dir = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_list_smb_directory_btn);
        final Button btn_sync_folder_edit_smb_dir_rule = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_edit_smb_dir_keyword);
//		final EditText et_sync_folder_dir_name = (EditText)dialog.findViewById(R.id.edit_sync_folder_dlg_directory_name);
//		final Button btn_sdcard_select_sdcard = (Button)dialog.findViewById(R.id.edit_sync_folder_dlg_show_select_document_tree);
//		final CheckedTextView ctv_sync_folder_use_usb_folder = (CheckedTextView)dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_use_usb_folder);
//		final Button btn_sync_folder_cancel = (Button)dialog.findViewById(R.id.edit_profile_remote_btn_cancel);

//		final LinearLayout ll_dir_view = (LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_directory_view);
//		final LinearLayout ll_zip_view = (LinearLayout)dialog.findViewById(R.id.edit_sync_folder_dlg_zip_view);

//		final CheckedTextView ctv_sync_folder_zip_file_name_time_stamp = (CheckedTextView)dialog.findViewById(R.id.edit_sync_folder_dlg_zip_file_time_stamp);
        final Button btn_zip_filelist = (Button) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_filelist_btn);
        final EditText et_sync_folder_zip_file_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_file_name);
        final Spinner sp_zip_enc_method=(Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_method);
        final EditText et_zip_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_password);
        final EditText et_zip_conf_pswd = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_zip_enc_confirm);
        final EditText et_sync_folder_dir_name = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_internal_directory_name);
        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);

        final CheckedTextView ctv_sync_folder_show_zip_password = (CheckedTextView) dialog.findViewById(R.id.edit_sync_folder_dlg_ctv_show_zip_password);

        String sel_type = sp_sync_folder_type.getSelectedItem().toString();
        if (sel_type.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local))) {
            LocalStorageSelectorItem sel_item=(LocalStorageSelectorItem)sp_sync_folder_local_storage_selector.getSelectedItem();
            String uuid=sel_item.uuid;
            if (Build.VERSION.SDK_INT<=28) {
                if (!uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                    if (SafManager3.isUuidMounted(mContext, uuid)) {
                        if (!SafManager3.isUuidRegistered(mContext, uuid)) {
                            result = false;
                            setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_auth_press_select_btn));
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                        } else {
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, true);
                        }
                    } else {
                        result = false;
                        setDialogMsg(dlg_msg, String.format(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_mounted), uuid));
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                    }
                }
            } else {
                if (SafManager3.isUuidMounted(mContext, uuid)) {
                    if (!SafManager3.isUuidRegistered(mContext, uuid)) {
                        result = false;
                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_auth_press_select_btn));
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                    } else {
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, true);
                    }
                } else {
                    result = false;
                    setDialogMsg(dlg_msg, String.format(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_mounted), uuid));
                    CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                }
            }
            if (result && !org_sfev.is_source_folder) {
                if (org_sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                    String e_msg=checkTakenDateParameterUsed(et_sync_folder_dir_name.getText().toString());
                    if (!e_msg.equals("")) {
                        result = false;
                        setDialogMsg(dlg_msg, e_msg);
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                    }
                }
            }
        } else if (sel_type.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb))) {
            String remote_host = et_remote_host.getText().toString().trim();

            String sync_folder_port = et_sync_folder_port.getText().toString().trim();

            String sync_folder_user = et_sync_folder_user.getText().toString().trim();
            String sync_folder_pswd = et_sync_folder_pswd.getText().toString();

            String folder_share_name = et_sync_folder_share_name.getText().toString().trim();

            if (remote_host.equals("")) {
                result = false;
                setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_specify_host_address_or_name));
                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, false);
                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, false);
                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, false);
            } else {
                if (ctv_sync_folder_use_port.isChecked() && sync_folder_port.equals("")) {
                    result = false;
                    CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, false);
                    CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, false);
                    CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, false);
                    setSyncFolderSmbListDirectoryButtonEnabled(dialog, false);
                    setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_specify_host_port_number));
                } else {
                    if (ctv_sync_folder_use_pswd.isChecked()) {
                        if (sync_folder_user.equals("") && sync_folder_pswd.equals("")) {
                            result = false;
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, false);
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, false);
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, false);
                            setSyncFolderSmbListDirectoryButtonEnabled(dialog, false);
                            setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_specify_host_userid_pswd));
                        }
                    }
                    if (result && folder_share_name.equals("")) {
                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_main_sync_profile_dlg_specify_host_share_name));
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, ctv_sync_folder_edit_smb_detail.isChecked());
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, false);
                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, false);
                        result = false;
                    } else {
                        if (result) {
                            if (result && !org_sfev.is_source_folder) {
                                if (org_sfev.task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
                                    String e_msg=checkTakenDateParameterUsed(et_sync_folder_dir_name.getText().toString());
                                    if (!e_msg.equals("")) {
                                        result = false;
                                        setDialogMsg(dlg_msg, e_msg);
                                        CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_local_list_dir, false);
                                    }
                                }
                            }
                            if (result) {
                                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, true);
                                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, ctv_sync_folder_edit_smb_detail.isChecked());
                                CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, true);
                            }
                        } else {
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_edit_smb_dir_rule, false);
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_share, false);
                            CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_smb_list_dir, false);
                        }
                    }
                }
            }
            boolean enabled = true;
            if (ctv_sync_folder_use_pswd.isChecked()) {
            }
            if (ctv_sync_folder_use_port.isChecked()) {
                if (et_sync_folder_port.getText().toString().equals("")) enabled = false;
            }
            if (et_remote_host.getText().toString().equals("")) enabled = false;
        } else if (sel_type.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_zip))) {
            result = false;

            if (et_sync_folder_zip_file_name.getText().length() > 0) {
                String zip_enc_method=(String)sp_zip_enc_method.getSelectedItem();
                if (zip_enc_method.equals(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_none))) {
                    result = true;
                } else {
                    if (et_zip_pswd.getText().length() > 0) {
                        if (!ctv_sync_folder_show_zip_password.isChecked()) {
                            if (et_zip_pswd.getText().toString().equals(et_zip_conf_pswd.getText().toString())) {
                                result = true;
                            } else {
                                setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_diff_zip_password));
                            }
                        } else {
                            result = true;
                        }
                    } else {
                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_specify_zip_password));
                    }
                }
            } else {
                setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_specify_zip_file_name));
            }
        }
        if (result) {
            setSyncFolderOkButtonEnabledIfFolderChanged(dialog, org_sfev);
        } else {
            setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
        }

        return result;
    }

    private String checkTakenDateParameterUsed(String dir_name) {
        String item=SyncThread.getTakenDateConversionItem(dir_name);
        if (!item.equals("")) {
            return mContext.getString(R.string.msgs_task_sync_task_edit_keyword_mirror_can_not_used_taken_date, item);
        }
        return  "";
    }

    private void setSyncFolderOkButtonEnabledIfFolderChanged(Dialog dialog, SyncFolderEditValue org_sfev) {
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);
        SyncFolderEditValue nsfev = buildSyncFolderEditValue(dialog, org_sfev);
        boolean same = nsfev.isSame(org_sfev);
        if (!same) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);
        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);
    }

    static public void showDialogWithHideOption(final Activity activity, final GlobalParameters gp, CommonUtilities cu,
                                                boolean ok_visible, String ok_label, boolean cancel_visible, String cancel_label,
                                                String title_text, String msg_text, String suppress_text,
                                                NotifyEvent p_ntfy) {

        final Dialog dialog = new Dialog(activity, gp.applicationTheme);//, android.R.style.Theme_Black);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.show_warning_message_dlg);

        final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.confirm_app_specific_dlg_view);
//        dlg_view.setBackgroundColor(gp.themeColorList.text_background_color);
        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.show_warning_message_dlg_title_view);
        final ScrollView msg_view = (ScrollView) dialog.findViewById(R.id.show_warning_message_dlg_msg_view);
//        msg_view.setBackgroundColor(Color.DKGRAY);
        final LinearLayout btn_view = (LinearLayout) dialog.findViewById(R.id.show_warning_message_dlg_btn_view);
//        btn_view.setBackgroundColor(Color.DKGRAY);
        final TextView title = (TextView) dialog.findViewById(R.id.show_warning_message_dlg_title);
        title_view.setBackgroundColor(gp.themeColorList.title_background_color);
        title.setText(title_text);
        title.setTextColor(gp.themeColorList.title_text_color);

        TextView dlg_msg=((TextView) dialog.findViewById(R.id.show_warning_message_dlg_msg));
        dlg_msg.setText(msg_text);

        final Button btnOk = (Button) dialog.findViewById(R.id.show_warning_message_dlg_close);
        btnOk.setText(ok_label);
        btnOk.setVisibility(ok_visible?Button.VISIBLE:Button.GONE);
        final Button btnCancel = (Button) dialog.findViewById(R.id.show_warning_message_dlg_cancel);
        btnCancel.setText(cancel_label);
        btnCancel.setVisibility(cancel_visible?Button.VISIBLE:Button.GONE);
        final CheckedTextView ctvSuppr = (CheckedTextView) dialog.findViewById(R.id.show_warning_message_dlg_ctv_suppress);
        CommonUtilities.setCheckedTextViewListener(ctvSuppr);
        ctvSuppr.setText(suppress_text);

        CommonDialog.setDlgBoxSizeCompact(dialog);
        ctvSuppr.setChecked(false);

        btnOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(true, new Object[]{ctvSuppr.isChecked()});
            }
        });
        btnCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, new Object[]{ctvSuppr.isChecked()});
            }
        });

        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnCancel.performClick();
            }
        });
        dialog.show();
    }

    private void invokeEditDirFilterDlg(final Dialog dialog, final SyncTaskItem n_sti, final String type, final TextView dlg_msg) {
        final TextView dlg_dir_filter = (TextView) dialog.findViewById(R.id.sync_filter_edit_dir_filter_btn);
        NotifyEvent ntfy = new NotifyEvent(mContext);
        //Listen setRemoteShare response
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context arg0, Object[] arg1) {
                dlg_dir_filter.setText(buildDirectoryFilterInfo(n_sti.getDirectoryFilter()));
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void negativeResponse(Context arg0, Object[] arg1) {
            }

        });
        mTaskUtil.editDirectoryFilterDlg(n_sti, ntfy);

    }

    private void invokeEditFileFilterDlg(Dialog dialog, final SyncTaskItem n_sti, final String type, final TextView dlg_msg) {
        final TextView dlg_file_filter = (TextView) dialog.findViewById(R.id.sync_filter_edit_file_filter_btn);
        NotifyEvent ntfy = new NotifyEvent(mContext);
        //Listen setRemoteShare response
        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context arg0, Object[] arg1) {
                dlg_file_filter.setText(buildFilterInfo(n_sti.getFileNameFilter()));
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void negativeResponse(Context arg0, Object[] arg1) {
            }

        });
        mTaskUtil.editFileFilterDlg(n_sti.getFileNameFilter(), ntfy);

    }

    private String buildSourceSyncFolderInfo(SyncTaskItem sti, Button info_btn, ImageView info_icon) {
        String info = "";
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            String dir = sti.getSourceDirectoryName();
            if (dir.equals("")) info = CommonUtilities.getStoragePathFromUuid(sti.getSourceStorageUuid());
            else {
                if (dir.startsWith("/")) info = CommonUtilities.getStoragePathFromUuid(sti.getSourceStorageUuid())+"/"+dir;
                else info = CommonUtilities.getStoragePathFromUuid(sti.getSourceStorageUuid())+"/"+dir;
            }
            int img_res=0;
            if (sti.getSourceStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_mobile;
            else {
                if (SafManager3.isUuidRegistered(mContext, sti.getSourceStorageUuid())) img_res=R.drawable.ic_32_external_media;
                else img_res=R.drawable.ic_32_external_media_bad;
            }
            info_icon.setImageDrawable(mContext.getResources().getDrawable(img_res, null));
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            String host = sti.getSourceSmbAddr();
            if (sti.getSourceSmbAddr().equals("")) host = sti.getSourceSmbHostName();
            String share = sti.getSourceSmbShareName();
            String dir = sti.getSourceDirectoryName();
            if (dir.equals("")) info = "smb://" + host + "/" + share;
            else {
                if (dir.startsWith("/")) info = "smb://" + host + "/" + share + dir;
                else info = "smb://" + host + "/" + share + "/" + dir;
            }
            info_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_32_server, null));
        }
        return info;
    }

    private String buildDestinationSyncFolderInfo(SyncTaskItem sti, Button info_btn, ImageView info_icon) {
        String info = "";
        if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            String dir = sti.getDestinationDirectoryName();
            if (dir.equals("")) info = CommonUtilities.getStoragePathFromUuid(sti.getDestinationStorageUuid());
            else {
                if (dir.startsWith("/")) info = CommonUtilities.getStoragePathFromUuid(sti.getDestinationStorageUuid())+"/"+dir;
                else info = CommonUtilities.getStoragePathFromUuid(sti.getDestinationStorageUuid())+"/"+dir;
            }
            int img_res=0;
            if (sti.getDestinationStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_mobile;
            else {
                if (SafManager3.isUuidRegistered(mContext, sti.getDestinationStorageUuid())) img_res=R.drawable.ic_32_external_media;
                else img_res=R.drawable.ic_32_external_media_bad;
            }
            info_icon.setImageDrawable(mContext.getResources().getDrawable(img_res, null));
        } else if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
            info = CommonUtilities.getStoragePathFromUuid(sti.getDestinationStorageUuid())+"/"+ sti.getDestinationZipOutputFileName();
            int img_res=0;
            if (sti.getDestinationStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) img_res=R.drawable.ic_32_archive;
            else {
                if (mGp.safMgr.isUuidRegistered(sti.getDestinationStorageUuid())) {
                    img_res=R.drawable.ic_32_archive;
                } else {
                    img_res=R.drawable.ic_32_archive_bad;
                }
            }
            info_icon.setImageDrawable(mContext.getResources().getDrawable(img_res, null));
        } else if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            String host = sti.getDestinationSmbAddr();
            if (sti.getDestinationSmbAddr().equals("")) host = sti.getDestinationSmbHostName();
            String share = sti.getDestinationSmbShareName();
            String dir = sti.getDestinationDirectoryName();
            if (dir.equals("")) info = "smb://" + host + "/" + share;
            else {
                if (dir.startsWith("/")) info = "smb://" + host + "/" + share + dir;
                else info = "smb://" + host + "/" + share + "/" + dir;
            }
            info_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_32_server, null));
        }
        return info;
    }

    private void setSpinnerSyncFolderZipCompressionLevel(Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_title));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_fastest));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_fast));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_normal));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_comp_level_maximum));

        int sel = 2;
        if (cv.equals(SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_FASTEST)) sel = 0;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_FAST)) sel = 1;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_NORMAL)) sel = 2;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_COMPRESS_LEVEL_MAXIMUM)) sel = 3;

        spinner.setSelection(sel);
        adapter.notifyDataSetChanged();
    }

    private void setSpinnerSyncFolderZipEncryptMethod(Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_select_msg));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_none));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_zip_crypto));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_aes128));
        adapter.add(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_zip_encrypt_aes256));

        int sel = 0;
        if (cv.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_NONE)) sel = 0;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_STANDARD)) sel = 1;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_AES128)) sel = 2;
        else if (cv.equals(SyncTaskItem.ZIP_OPTION_ENCRYPT_AES256)) sel = 3;

        spinner.setSelection(sel);
        adapter.notifyDataSetChanged();
    }

    private void setSpinnerSyncFolderStorageSelector(SyncTaskItem sti, Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mContext, spinner, mGp.isScreenThemeIsLight());
        ArrayList<LocalStorageSelectorItem> selector_list=new ArrayList<LocalStorageSelectorItem>();
        final LocalStorageSelectorAdapter adapter = new LocalStorageSelectorAdapter(mActivity, android.R.layout.simple_spinner_item, selector_list);
        mGp.safMgr.refreshSafList();
        adapter.setDropDownViewResource(R.layout.non_wordwrap_simple_spinner_item);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_select_local_storage));
        spinner.setAdapter(adapter);

        int sel_no = -1;
        ArrayList<SafStorage3>svl=mGp.safMgr.getSafStorageList();
        for(SafStorage3 ss:svl) {
            LocalStorageSelectorItem sl_item=new LocalStorageSelectorItem();
            sl_item.description=ss.description;
            sl_item.uuid=ss.uuid;
            sl_item.mounted =true;
            if (Build.VERSION.SDK_INT<=29) {
                if (ss.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                    sl_item.root_path=SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX;
                    adapter.add(sl_item);
                } else {
                    if (SafManager3.isUuidRegistered(mContext, ss.uuid)) {
                        sl_item.root_path=SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+ss.uuid;
                        adapter.add(sl_item);
                    }
                }
            } else {
                if (SafManager3.isUuidRegistered(mContext, ss.uuid)) {
                    if (ss.uuid.equals(SAF_FILE_PRIMARY_UUID)) {
                        sl_item.root_path=SafFile3.SAF_FILE_PRIMARY_STORAGE_PREFIX;
                        adapter.add(sl_item);
                    } else {
                        sl_item.root_path=SafFile3.SAF_FILE_EXTERNAL_STORAGE_PREFIX+ss.uuid;
                        adapter.add(sl_item);
                    }
                }
            }
        }
        for (int i=0;i<adapter.getCount();i++) {
            if (adapter.getItem(i).uuid.equals(cv)) {
                sel_no=i;
                break;
            }
        }
        if (sel_no==-1) {
            spinner.setSelection(adapter.getCount());
            if (!cv.equals("")) {
                LocalStorageSelectorItem sl_item=new LocalStorageSelectorItem();
                sl_item.root_path="";
                sl_item.description="Unknown";
                sl_item.uuid=cv;
                sl_item.mounted =false;
                adapter.add(sl_item);
            }
        } else {
            spinner.setSelection(sel_no);
        }

        adapter.notifyDataSetChanged();
    }

    private String getSmbSelectedProtocol(Spinner spinner) {
        if (spinner.getSelectedItem()==null) {
            return SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
        }
        return (String)spinner.getSelectedItem();
    }

    private void setSpinnerSyncFolderSmbProto(SyncTaskItem sti, Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        mGp.safMgr.refreshSafList();
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_smb_protocol_select_title));
        spinner.setAdapter(adapter);

        adapter.add(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1);
        adapter.add(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23);

        if (cv.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) spinner.setSelection(0);
        else if (cv.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23)) spinner.setSelection(1);
        else spinner.setSelection(1);
    }

    private void setSpinnerSyncTaskErrorOption(SyncTaskItem sti, Spinner spinner, int cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_sync_task_sync_option_error_prompt));
        spinner.setAdapter(adapter);

        adapter.add(mContext.getString(R.string.msgs_task_sync_task_sync_option_error_stop));
        adapter.add(mContext.getString(R.string.msgs_task_sync_task_sync_option_error_ignore));
        adapter.add(mContext.getString(R.string.msgs_task_sync_task_sync_option_error_skip_network));

        spinner.setSelection(cv, false);
    }

    private void setSpinnerSyncFolderType(SyncTaskItem sti, Spinner spinner, String cv, boolean source) {
        final Spinner spinnerSyncType = (Spinner) mDialog.findViewById(R.id.edit_sync_task_sync_type);
        String sync_type=spinnerSyncType.getSelectedItem().toString();
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        mGp.safMgr.refreshSafList();
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_prompt));
        spinner.setAdapter(adapter);
        int sel = 0;
        if (source) {
            if (!sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                    adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local));
                    adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb));
                    if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) sel = 0;
                    else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) sel = 1;
                } else {
                    adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local));
                    adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb));
                    if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) sel = 0;
                    else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) sel = 1;
                }
            } else {
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local));
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb));
                if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) sel = 0;
                else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) sel = 1;
            }
        } else {
            if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local));
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb));
                if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) sel = 0;
                else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) sel = 1;
            } else {
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_local));
                adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_smb));
                if (!sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE) &&
                        !sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_SYNC))
                    adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_zip));
                if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) sel = 0;
                else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) sel = 1;
                else if (cv.equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                    if (!sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) sel = 2;
                }
            }
        }
        spinner.setSelection(sel);
        adapter.notifyDataSetChanged();
    }

    private void setSpinnerSyncTaskType(Spinner spinnerSyncOption, String prof_syncopt, String destination_folder_type) {
        CommonUtilities.setSpinnerBackground(mActivity, spinnerSyncOption, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapterSyncOption = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapterSyncOption.setDropDownTextWordwrapEnabled(true);
//        adapterSyncOption.setDebug(true);
        adapterSyncOption.setSpinner(spinnerSyncOption);
        adapterSyncOption.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinnerSyncOption.setPrompt(mContext.getString(R.string.msgs_main_sync_profile_dlg_syncopt_prompt));
        spinnerSyncOption.setAdapter(adapterSyncOption);
        adapterSyncOption.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_mirror));
        adapterSyncOption.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_copy));
        adapterSyncOption.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_move));
        adapterSyncOption.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_archive));
//        if (mGp.debuggable) adapterSyncOption.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync));

        int sel=0;
        if (prof_syncopt.equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) sel=0;
        else if (prof_syncopt.equals(SyncTaskItem.SYNC_TASK_TYPE_COPY)) sel=1;
        else if (prof_syncopt.equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) sel=2;
        else if (prof_syncopt.equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) sel=3;
//		else if (mGp.debuggable && prof_syncopt.equals(SyncTaskItem.SYNC_TASK_TYPE_SYNC)) sel=4;

        spinnerSyncOption.setSelection(sel);
        adapterSyncOption.notifyDataSetChanged();
    }

    private void setSpinnerTwoWaySyncConflictRule(Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
//        adapter.setDebug(true, "SpinnerTwoWaySyncConflictRule");
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_prompt));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_ask_user));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_newer));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_older));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_from_source_to_destination));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_from_destination_to_source));
        adapter.add(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_skip_sync_file));
        spinner.setAdapter(adapter);

        int sel=Integer.parseInt(cv);
        spinner.setSelection(sel);
    }

    private void setSpinnerSyncTaskWifiOption(Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_prompt));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_off));
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_any_ap));
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_private_address));
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_specific_address));

        int sel = 0;
        if (cv.equals(SyncTaskItem.WIFI_STATUS_WIFI_OFF)) sel = 0;
        else if (cv.equals(SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP)) sel = 1;
        else if (cv.equals(SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS)) sel = 2;
        else if (cv.equals(SyncTaskItem.WIFI_STATUS_WIFI_IP_ADDRESS_LIST)) sel = 3;

        spinner.setSelection(sel);

        adapter.notifyDataSetChanged();
    }

    private String getSpinnerSyncTaskWifiOptionValue(Spinner spinner) {
        String value=SyncTaskItem.WIFI_STATUS_WIFI_OFF;
        int sel_pos=spinner.getSelectedItemPosition();
        if (sel_pos==1) value=SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP;
        else if (sel_pos==2) value=SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS;
        else if (sel_pos==3) value=SyncTaskItem.WIFI_STATUS_WIFI_IP_ADDRESS_LIST;
        return value;
    }

    private void setSpinnerSyncTaskDiffTimeValue(Spinner spinner, int cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_main_sync_profile_dlg_diff_time_value_option_prompt));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_diff_time_value_option_1));
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_diff_time_value_option_3));
        adapter.add(mContext.getString(R.string.msgs_main_sync_profile_dlg_diff_time_value_option_10));

        int sel = 0;
        if (cv == 1) sel = 0;
        else if (cv == 3) sel = 1;
        else if (cv == 10) sel = 2;

        spinner.setSelection(sel);

        adapter.notifyDataSetChanged();
    }

    private void setSpinnerSyncDstOffsetValue(Spinner spinner, int cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_task_sync_task_sync_option_offset_of_dst_time_value_title));
        spinner.setAdapter(adapter);
        for(int i=10;i<121;i+=10) adapter.add(String.valueOf(i));
        if (cv>=10 && cv<=120) {
            spinner.setSelection((cv/10)-1);
        }

        adapter.notifyDataSetChanged();
    }

    private String buildDirectoryFilterInfo(ArrayList<FilterListAdapter.FilterListItem> filter_list) {
        String info=buildFilterInfo(filter_list);
        if (info.equals("")) {
            if (filter_list.size()>0) {
                return mContext.getString(R.string.msgs_task_sync_task_filter_list_dlg_hint_enabled_filter_does_not_exists);
            } else {
                return mContext.getString(R.string.msgs_task_sync_task_dlg_dir_filter_not_specified);
            }
        }
        return info;
    }

    private String buildFileFilterInfo(ArrayList<FilterListAdapter.FilterListItem> filter_list) {
        String info=buildFilterInfo(filter_list);
        if (info.equals("")) {
            if (filter_list.size()>0) {
                return mContext.getString(R.string.msgs_task_sync_task_filter_list_dlg_hint_enabled_filter_does_not_exists);
            } else {
                return mContext.getString(R.string.msgs_task_sync_task_dlg_file_filter_not_specified);
            }
        }
        return info;
    }

    private String buildFilterInfo(ArrayList<FilterListAdapter.FilterListItem> filter_list) {
        String info = "";
        String inc_char = "\u2295"; //(+) ASCII
        String exc_char = "\u2296"; //(-) ASCII

        if (filter_list != null && filter_list.size() > 0) {
            String t_info = "", cn = "";
            for (int i = 0; i < filter_list.size(); i++) {
                if (!filter_list.get(i).isDeleted() && filter_list.get(i).isEnabled()) {
                    if (filter_list.get(i).isInclude()) t_info += cn+inc_char+ filter_list.get(i).getFilter()+";";
                    else t_info +=cn+ exc_char+ filter_list.get(i).getFilter()+";";
                    cn=" ";
                }
            }
            if (!t_info.equals("")) info = t_info;
        }
        return info;
    }

    private String checkTaskNameValidity(String type, String t_name, TextView tv, Button ok) {
        String result = "";
        if (type.equals(TASK_EDIT_METHOD_EDIT)) {
        } else {
            if (t_name.length() > 0) {
                if (TaskListUtils.getSyncTaskByName(mGp.syncTaskListAdapter, t_name) == null) {
                    result = "";
                } else {
                    result = mContext.getString(R.string.msgs_duplicate_task_name);
                }
            } else {
                result = mContext.getString(R.string.msgs_specify_task_name);
            }
        }
        return result;
    }

    public static final String TASK_EDIT_METHOD_ADD="ADD";
    public static final String TASK_EDIT_METHOD_EDIT="EDIT";
    public static final String TASK_EDIT_METHOD_COPY="COPY";
    private void editSyncTask(final String type, final SyncTaskItem pfli) {
        final SyncTaskItem n_sti = pfli.clone();
        mUtil.addDebugMsg(1,"I","editSyncTask entered, type="+type+", task="+pfli.getSyncTaskName());

        mGp.safMgr.refreshSafList();

        // カスタムダイアログの生成
        mDialog.setContentView(R.layout.edit_sync_task_dlg);

        LinearLayout ll_dlg_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.title_background_color);

        final LinearLayout title_view = (LinearLayout) mDialog.findViewById(R.id.edit_profile_sync_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView dlg_title = (TextView) mDialog.findViewById(R.id.edit_profile_sync_title);
        dlg_title.setTextColor(mGp.themeColorList.title_text_color);
//        dlg_title.setBackgroundColor(mGp.themeColorList.title_background_color);

        final TextView dlg_title_sub = (TextView) mDialog.findViewById(R.id.edit_profile_sync_title_sub);
        dlg_title_sub.setTextColor(mGp.themeColorList.title_text_color);
//        dlg_title_sub.setBackgroundColor(mGp.themeColorList.title_background_color);

        final TextView dlg_msg = (TextView) mDialog.findViewById(R.id.edit_sync_task_msg);
        dlg_msg.setTextColor(mGp.themeColorList.text_color_error);
        dlg_msg.setVisibility(TextView.GONE);

        final Button destination_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_btn);
        final ImageView destination_folder_icon=(ImageView)mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_icon);
        final Button swap_source_destination = (Button) mDialog.findViewById(R.id.edit_sync_task_swap_source_and_destination_btn);
        final Button source_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_source_folder_info_btn);
        final ImageView source_folder_icon=(ImageView)mDialog.findViewById(R.id.edit_sync_task_source_folder_info_icon);

        final EditText et_sync_main_task_name = (EditText) mDialog.findViewById(R.id.edit_sync_task_task_name);
        if (type.equals(TASK_EDIT_METHOD_EDIT)) {
            et_sync_main_task_name.setText(n_sti.getSyncTaskName());
            et_sync_main_task_name.setVisibility(EditText.GONE);
            dlg_title.setText(mContext.getString(R.string.msgs_edit_sync_profile));
            dlg_title_sub.setText(" (" + n_sti.getSyncTaskName() + ")");
        } else if (type.equals(TASK_EDIT_METHOD_COPY)) {
            et_sync_main_task_name.setText(n_sti.getSyncTaskName());
            dlg_title.setText(mContext.getString(R.string.msgs_copy_sync_profile));
            dlg_title_sub.setText(" (" + n_sti.getSyncTaskName() + ")");
        } else if (type.equals(TASK_EDIT_METHOD_ADD)) {
            dlg_title.setText(mContext.getString(R.string.msgs_add_sync_profile));
            dlg_title_sub.setVisibility(TextView.GONE);
            n_sti.setSyncOptionWifiStatusOption(SyncTaskItem.WIFI_STATUS_WIFI_OFF);
        }

        final LinearLayout ll_advanced_network_option_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_advanced_network_option_view);
        if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) || n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            ll_advanced_network_option_view.setVisibility(LinearLayout.VISIBLE);
        } else {
            ll_advanced_network_option_view.setVisibility(LinearLayout.GONE);
        }

        final CheckedTextView ctv_auto = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_ctv_auto);
        CommonUtilities.setCheckedTextViewListener(ctv_auto);
        ctv_auto.setChecked(n_sti.isSyncTaskAuto());
        setCtvListenerForEditSyncTask(ctv_auto, type, n_sti, dlg_msg);

        final CheckedTextView ctv_confirm_exif_date = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_confirm_exif_date);
        CommonUtilities.setCheckedTextViewListener(ctv_confirm_exif_date);
        ctv_confirm_exif_date.setChecked(n_sti.isSyncOptionConfirmNotExistsExifDate());
        setCtvListenerForEditSyncTask(ctv_confirm_exif_date, type, n_sti, dlg_msg);

        final CheckedTextView ctv_ignore_file_size_gt_4gb = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_source_file_that_file_size_gt_4gb);
        CommonUtilities.setCheckedTextViewListener(ctv_ignore_file_size_gt_4gb);
        ctv_ignore_file_size_gt_4gb.setChecked(n_sti.isSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb());
        setCtvListenerForEditSyncTask(ctv_ignore_file_size_gt_4gb, type, n_sti, dlg_msg);

        final CheckedTextView ctv_ignore_file_name_length_255_byte = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_file_name_length_exceed_255_byte);
        CommonUtilities.setCheckedTextViewListener(ctv_ignore_file_name_length_255_byte);
        ctv_ignore_file_name_length_255_byte.setChecked(n_sti.isSyncOptionIgnoreDestinationFileNameLengthExceed255Byte());
        setCtvListenerForEditSyncTask(ctv_ignore_file_name_length_255_byte, type, n_sti, dlg_msg);

        final CheckedTextView ctv_edit_sync_tak_option_keep_conflict_file = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file);
        final LinearLayout ll_edit_sync_tak_option_keep_conflict_file=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file_view);
        ctv_edit_sync_tak_option_keep_conflict_file.setChecked(n_sti.isSyncTwoWayKeepConflictFile());
        setCtvListenerForEditSyncTask(ctv_edit_sync_tak_option_keep_conflict_file, type, n_sti, dlg_msg);

        final CheckedTextView ctvIgnoreFilterRemoveDirFileDesNotExistsInSource = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_remove_dir_file_excluded_by_filter);
        final LinearLayout ctvIgnoreFilterRemoveDirFileDesNotExistsInSourceView = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_sync_remove_dir_file_excluded_by_filter);
        final CheckedTextView ctvDeleteFirst = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_delete_first_when_mirror);
        final LinearLayout ctvDeleteFirstView = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_sync_delete_first_when_mirror);

        final Spinner spinnerTwoWaySyncConflictRule = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_conflict_file_rule_value);
        final LinearLayout ll_spinnerTwoWaySyncConflictRule=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_conflict_file_rule_view);
        spinnerTwoWaySyncConflictRule.setOnItemSelectedListener(null);
        setSpinnerTwoWaySyncConflictRule(spinnerTwoWaySyncConflictRule, n_sti.getSyncTwoWayConflictFileRule());
        spinnerTwoWaySyncConflictRule.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final Spinner spinnerSyncType = (Spinner) mDialog.findViewById(R.id.edit_sync_task_sync_type);
        spinnerSyncType.setOnItemSelectedListener(null);
        setSpinnerSyncTaskType(spinnerSyncType, n_sti.getSyncTaskType(), n_sti.getDestinationFolderType());
        spinnerSyncType.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSyncTaskTypeFromSpinnere(spinnerSyncType, n_sti);
                if (spinnerSyncType.getSelectedItem().toString().equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_archive))) {
                    if (n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                        n_sti.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                        mUtil.showCommonDialogWarn(false,
                                mContext.getString(R.string.msgs_sync_folder_archive_zip_folder_not_supported), "", null);
                        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                        destination_folder_info.requestLayout();
                    }
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        final LinearLayout ll_wifi_condition_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_wifi_condition_view);
        final LinearLayout ll_wifi_wl_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_wl_view);
        final LinearLayout ll_wifi_wl_address_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_address_list_view);
        final Button edit_wifi_addr_list = (Button) mDialog.findViewById(R.id.edit_sync_task_option_btn_edit_address_white_list);
        setWifiApWhiteListInfo(n_sti.getSyncOptionWifiIPAddressGrantList(), edit_wifi_addr_list);
        final CheckedTextView ctv_sync_allow_global_ip_addr = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_sync_allow_global_ip_address);
        ctv_sync_allow_global_ip_addr.setChecked(n_sti.isSyncOptionSyncAllowGlobalIpAddress());
        setCtvListenerForEditSyncTask(ctv_sync_allow_global_ip_addr, type, n_sti, dlg_msg);

        final CheckedTextView ctv_task_sync_when_cahrging = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_start_when_charging);
//        CommonUtilities.setCheckedTextViewListener(ctv_task_sync_when_cahrging);
        ctv_task_sync_when_cahrging.setChecked(n_sti.isSyncOptionSyncWhenCharging());
        setCtvListenerForEditSyncTask(ctv_task_sync_when_cahrging, type, n_sti, dlg_msg);

        final CheckedTextView ctv_never_overwrite_destination_file_newer_than_the_source_file = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file);
        ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(n_sti.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile());
        ctv_never_overwrite_destination_file_newer_than_the_source_file.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctv_never_overwrite_destination_file_newer_than_the_source_file.isChecked();
                if (isChecked) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(isChecked);
                            checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    mUtil.showCommonDialogDanger(true,
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file),
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file_warning),
                            mContext.getString(R.string.msgs_common_dialog_confirm),
                            mContext.getString(R.string.msgs_common_dialog_cancel),
                            ntfy );
                } else {
                    checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                }
            }
        });
        ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(n_sti.isSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile());

        final CheckedTextView ctv_ignore_dst_difference = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_ignore_dst_difference);
        ctv_ignore_dst_difference.setChecked(n_sti.isSyncOptionIgnoreDstDifference());

        final CheckedTextView ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_unusable_character_used_directory_file_name);
        ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name.setChecked(n_sti.isSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters());
        setCtvListenerForEditSyncTask(ctv_edit_sync_task_option_ignore_unusable_character_used_directory_file_name, type, n_sti, dlg_msg);

        final CheckedTextView ctv_sync_remove_source_if_empty = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_remove_directory_if_empty_when_move);
        ctv_sync_remove_source_if_empty.setChecked(n_sti.isSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty());
        setCtvListenerForEditSyncTask(ctv_sync_remove_source_if_empty, type, n_sti, dlg_msg);

        final Spinner sp_sync_task_option_error_option=(Spinner)mDialog.findViewById(R.id.edit_sync_task_option_error_option_value);
        sp_sync_task_option_error_option.setOnItemSelectedListener(null);
        setSpinnerSyncTaskErrorOption(n_sti, sp_sync_task_option_error_option, n_sti.getSyncTaskErrorOption());
        Handler hndl=new Handler();
        hndl.postDelayed(new Runnable(){
            @Override
            public void run() {
                sp_sync_task_option_error_option.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position!=SyncTaskItem.SYNC_TASK_OPTION_ERROR_OPTION_STOP) {
                            NotifyEvent ntfy=new NotifyEvent(mContext);
                            ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                                @Override
                                public void positiveResponse(Context context, Object[] objects) {
                                    checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                                }

                                @Override
                                public void negativeResponse(Context context, Object[] objects) {
                                    sp_sync_task_option_error_option.setSelection(n_sti.getSyncTaskErrorOption(), false);
                                }
                            });
                            mUtil.showCommonDialogWarn(true, mContext.getString(R.string.msgs_task_sync_task_sync_option_error_title),
                                    mContext.getString(R.string.msgs_task_sync_task_sync_option_error_ignore_warning_message), ntfy);
                        } else {
                            checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
        }, 100);

        final Spinner spinnerSyncWifiStatus = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_wifi_status);
        spinnerSyncWifiStatus.setOnItemSelectedListener(null);
        setSpinnerSyncTaskWifiOption(spinnerSyncWifiStatus, n_sti.getSyncOptionWifiStatusOption());
        if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) || n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            ll_wifi_condition_view.setVisibility(Button.VISIBLE);
        } else {
            ll_wifi_condition_view.setVisibility(Button.GONE);
        }

        spinnerSyncWifiStatus.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setSyncTaskFieldHelpListener(mDialog, n_sti);

        source_folder_info.setText(buildSourceSyncFolderInfo(n_sti, source_folder_info, source_folder_icon));
        source_folder_info.requestLayout();
        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
        destination_folder_info.requestLayout();

        boolean is_all_file_type = false;
        if (!n_sti.isSyncFileTypeAudio() && !n_sti.isSyncFileTypeImage() && !n_sti.isSyncFileTypeVideo() &&
                n_sti.getFileNameFilter().size() == 0) is_all_file_type = true;

        boolean is_all_sub_dir = false;
        if (n_sti.getDirectoryFilter().size() == 0) is_all_sub_dir = true;

        final Button dir_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_dir_filter_btn);
        final Button file_filter_btn = (Button) mDialog.findViewById(R.id.sync_filter_edit_file_filter_btn);
//		final TextView dlg_file_filter=(TextView) mDialog.findViewById(R.id.sync_filter_summary_file_filter);
        file_filter_btn.setText(buildFileFilterInfo(n_sti.getFileNameFilter()));
//		final TextView dlg_dir_filter=(TextView) mDialog.findViewById(R.id.sync_filter_summary_dir_filter);
        dir_filter_btn.setText(buildDirectoryFilterInfo(n_sti.getDirectoryFilter()));

        final LinearLayout ll_file_filter_detail = (LinearLayout) mDialog.findViewById(R.id.sync_filter_file_type_detail_view);
        final LinearLayout ll_dir_filter_detail = (LinearLayout) mDialog.findViewById(R.id.sync_filter_sub_directory_detail_view);

        final CheckedTextView ctvSyncFileTypeSpecific = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_specific);
        ctvSyncFileTypeSpecific.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvSyncFileTypeSpecific.isChecked();
                ctvSyncFileTypeSpecific.setChecked(isChecked);
                if (isChecked) ll_file_filter_detail.setVisibility(Button.VISIBLE);
                else ll_file_filter_detail.setVisibility(Button.GONE);
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        ctvSyncFileTypeSpecific.setChecked(!is_all_file_type);
        if (!is_all_file_type) ll_file_filter_detail.setVisibility(Button.VISIBLE);
        else ll_file_filter_detail.setVisibility(Button.GONE);

        final CheckedTextView ctvSyncFileTypeAudio = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_audio);
        ctvSyncFileTypeAudio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvSyncFileTypeAudio.isChecked();
                ctvSyncFileTypeAudio.setChecked(isChecked);
                if (isChecked) {
                    String f_ext="", sep="";
                    for(String item:SYNC_FILE_TYPE_AUDIO) {
                        f_ext+=sep+item;
                        sep=", ";
                    }
                    mUtil.showCommonDialogInfo(false,
                            mContext.getString(R.string.msgs_task_sync_task_sync_file_type_add_filter_title),f_ext,null );
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        ctvSyncFileTypeAudio.setChecked(n_sti.isSyncFileTypeAudio());

        final CheckedTextView ctvSyncFileTypeImage = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_image);
        ctvSyncFileTypeImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvSyncFileTypeImage.isChecked();
                ctvSyncFileTypeImage.setChecked(isChecked);
                if (isChecked) {
                    String f_ext="", sep="";
                    for(String item:SYNC_FILE_TYPE_IMAGE) {
                        f_ext+=sep+item;
                        sep=", ";
                    }
                    mUtil.showCommonDialogInfo(false,
                            mContext.getString(R.string.msgs_task_sync_task_sync_file_type_add_filter_title),f_ext,null );
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        ctvSyncFileTypeImage.setChecked(n_sti.isSyncFileTypeImage());

        final CheckedTextView ctvSyncFileTypeVideo = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_video);
        ctvSyncFileTypeVideo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvSyncFileTypeVideo.isChecked();
                ctvSyncFileTypeVideo.setChecked(isChecked);
                if (isChecked) {
                    String f_ext="", sep="";
                    for(String item:SYNC_FILE_TYPE_VIDEO) {
                        f_ext+=sep+item;
                        sep=", ";
                    }
                    mUtil.showCommonDialogInfo(false,
                            mContext.getString(R.string.msgs_task_sync_task_sync_file_type_add_filter_title),f_ext,null );
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        ctvSyncFileTypeVideo.setChecked(n_sti.isSyncFileTypeVideo());

        final CheckedTextView ctvSyncSpecificSubDir = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_sub_directory_specific);
        ctvSyncSpecificSubDir.setChecked(!is_all_sub_dir);
        ctvSyncSpecificSubDir.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvSyncSpecificSubDir.isChecked();
                ctvSyncSpecificSubDir.setChecked(isChecked);
                if (isChecked) ll_dir_filter_detail.setVisibility(Button.VISIBLE);
                else ll_dir_filter_detail.setVisibility(Button.GONE);
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        if (!is_all_sub_dir) ll_dir_filter_detail.setVisibility(Button.VISIBLE);
        else ll_dir_filter_detail.setVisibility(Button.GONE);

        final CheckedTextView ctvProcessRootDirFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_source_root_dir_file);
        CommonUtilities.setCheckedTextViewListener(ctvProcessRootDirFile);
        ctvProcessRootDirFile.setChecked(n_sti.isSyncProcessRootDirFile());

        final CheckedTextView ctvSyncSubDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_sub_dir);
        CommonUtilities.setCheckedTextViewListener(ctvSyncSubDir);
        ctvSyncSubDir.setChecked(n_sti.isSyncOptionSyncSubDirectory());

        if (n_sti.isSyncProcessRootDirFile()) {
            ctvProcessRootDirFile.setChecked(true);
            ctvSyncSubDir.setChecked(n_sti.isSyncOptionSyncSubDirectory());
        } else {
            ctvProcessRootDirFile.setChecked(false);
            ctvSyncSubDir.setChecked(true);
            CommonDialog.setViewEnabled(getActivity(), ctvSyncSubDir,false);
        }

        ctvProcessRootDirFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctvProcessRootDirFile.toggle();
                boolean isChecked = ctvProcessRootDirFile.isChecked();
                if (!isChecked) {
                    CommonDialog.setViewEnabled(getActivity(), ctvSyncSubDir,false);
                    ctvSyncSubDir.setChecked(true);
                } else {
                    CommonDialog.setViewEnabled(getActivity(), ctvSyncSubDir,true);
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });

        setCtvListenerForEditSyncTask(ctvSyncSubDir, type, n_sti, dlg_msg);

        final CheckedTextView ctvSyncEmptyDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_empty_directory);
        CommonUtilities.setCheckedTextViewListener(ctvSyncEmptyDir);
        ctvSyncEmptyDir.setChecked(n_sti.isSyncOptionSyncEmptyDirectory());
        setCtvListenerForEditSyncTask(ctvSyncEmptyDir, type, n_sti, dlg_msg);

        final CheckedTextView ctvSyncHiddenDir = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_directory);
        CommonUtilities.setCheckedTextViewListener(ctvSyncHiddenDir);
        ctvSyncHiddenDir.setChecked(n_sti.isSyncOptionSyncHiddenDirectory());
        setCtvListenerForEditSyncTask(ctvSyncHiddenDir, type, n_sti, dlg_msg);

        final CheckedTextView ctvSyncHiddenFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_file);
        CommonUtilities.setCheckedTextViewListener(ctvSyncHiddenFile);
        ctvSyncHiddenFile.setChecked(n_sti.isSyncOptionSyncHiddenFile());
        setCtvListenerForEditSyncTask(ctvSyncHiddenFile, type, n_sti, dlg_msg);

        final CheckedTextView ctvProcessOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_process_override_delete_file);
        ctvProcessOverride.setChecked(n_sti.isSyncOverrideCopyMoveFile());
        setCtvListenerForEditSyncTask(ctvProcessOverride, type, n_sti, dlg_msg);

        final CheckedTextView ctvConfirmOverride = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_confirm_override_delete_file);
        CommonUtilities.setCheckedTextViewListener(ctvConfirmOverride);
        ctvConfirmOverride.setChecked(n_sti.isSyncConfirmOverrideOrDelete());
        setCtvListenerForEditSyncTask(ctvConfirmOverride, type, n_sti, dlg_msg);

        CommonUtilities.setCheckedTextViewListener(ctvDeleteFirst);
        ctvDeleteFirst.setChecked(n_sti.isSyncOptionDeleteFirstWhenMirror());
        setCtvListenerForEditSyncTask(ctvDeleteFirst, type, n_sti, dlg_msg);

        ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.setChecked(n_sti.isSyncOptionRemoveDirectoryFileThatExcludedByFilter());
        ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.isChecked();
                if (isChecked) {//if checked, display warning that also files not matching the filters will be removed from the destination
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.setChecked(isChecked);//true
                            checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        }
                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                            checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        }
                    });
                    mUtil.showCommonDialogDanger(true,
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_remove_dir_file_excluded_by_filter_warn_dialog_title),
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_remove_dir_file_excluded_by_filter_warn_dialog),
                            mContext.getString(R.string.msgs_common_dialog_confirm),
                            mContext.getString(R.string.msgs_common_dialog_cancel),
                            ntfy);
                } else {
                    ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.setChecked(isChecked);
                    checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                }

            }
        });

        final LinearLayout ll_special_option_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_special_option_view);
        final CheckedTextView ctvShowSpecialOption = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_show_special_option);
        ll_special_option_view.setVisibility(LinearLayout.GONE);
        ctvShowSpecialOption.setChecked(false);
        ctvShowSpecialOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvShowSpecialOption.isChecked();
                ctvShowSpecialOption.setChecked(isChecked);
                if (isChecked) ll_special_option_view.setVisibility(LinearLayout.VISIBLE);
                else ll_special_option_view.setVisibility(LinearLayout.GONE);
            }
        });

        final CheckedTextView ctvDoNotResetFileLastMod = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_do_mot_reset_file_last_mod_time);
        CommonUtilities.setCheckedTextViewListener(ctvDoNotResetFileLastMod);
        ctvDoNotResetFileLastMod.setChecked(n_sti.isSyncDoNotResetFileLastModified());
        ctvDoNotResetFileLastMod.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isChecked=!((CheckedTextView)view).isChecked();
                if (isChecked) {
                    NotifyEvent ntfy=new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            ((CheckedTextView)view).setChecked(isChecked);
                            checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {}
                    });
                    mUtil.showCommonDialogDanger(true,
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_not_set_file_last_modified),
                            mContext.getString(R.string.msgs_task_sync_task_sync_option_not_set_file_last_modified_warning),
                            mContext.getString(R.string.msgs_common_dialog_confirm),
                            mContext.getString(R.string.msgs_common_dialog_cancel),
                            ntfy );
                } else {
                    ((CheckedTextView)view).setChecked(isChecked);
                    checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                }
            }
        });

        final CheckedTextView ctvRetry = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_retry_if_error_occured);
        CommonUtilities.setCheckedTextViewListener(ctvRetry);
        if (n_sti.getSyncOptionRetryCount()==0) ctvRetry.setChecked(false);
        else ctvRetry.setChecked(true);
        setCtvListenerForEditSyncTask(ctvRetry, type, n_sti, dlg_msg);

        final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_remote_small_io_area);
        CommonUtilities.setCheckedTextViewListener(ctvSyncUseRemoteSmallIoArea);
        ctvSyncUseRemoteSmallIoArea.setChecked(n_sti.isSyncOptionUseSmallIoBuffer());
        setCtvListenerForEditSyncTask(ctvSyncUseRemoteSmallIoArea, type, n_sti, dlg_msg);

        final CheckedTextView ctvTestMode = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_test_mode);
        ctvTestMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !ctvTestMode.isChecked();
                ctvTestMode.setChecked(isChecked);
                CommonDialog.setViewEnabled(getActivity(), ctv_auto, !isChecked);
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        ctvTestMode.setChecked(n_sti.isSyncTestMode());
        CommonDialog.setViewEnabled(getActivity(), ctv_auto, !ctvTestMode.isChecked());

        final LinearLayout ll_use_file_last_mod = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_sync_diff_use_last_mod_time_view);
        final LinearLayout ll_last_mod_allowed_time = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_diff_file_determin_time_value_view);
        final CheckedTextView ctvDeterminChangedFileSizeGtDestination = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_file_size_greater_than_destination);
        ctvDeterminChangedFileSizeGtDestination.setChecked(n_sti.isSyncDifferentFileSizeGreaterThanDestinationFile());
        final CheckedTextView ctvDiffUseFileSize = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_file_size);
        final CheckedTextView ctDeterminChangedFileByTime = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_last_mod_time);

        CommonUtilities.setCheckedTextViewListener(ctvDiffUseFileSize);
        ctvDiffUseFileSize.setChecked(n_sti.isSyncOptionDifferentFileBySize());
        ctvDiffUseFileSize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !((CheckedTextView) v).isChecked();
                if (isChecked) {
                    ((CheckedTextView) v).setChecked(isChecked);
                    ctvDeterminChangedFileSizeGtDestination.setEnabled(true);
                } else {
                    if (!ctDeterminChangedFileByTime.isChecked()) {
                        NotifyEvent ntfy=new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                ((CheckedTextView) v).setChecked(isChecked);
                                ctvDeterminChangedFileSizeGtDestination.setEnabled(false);
                                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        mUtil.showCommonDialogDanger(true,
                                mContext.getString(R.string.msgs_task_sync_task_sync_option_copy_no_compare_title),
                                mContext.getString(R.string.msgs_task_sync_task_sync_option_copy_no_compare_warning),
                                mContext.getString(R.string.msgs_common_dialog_confirm),
                                mContext.getString(R.string.msgs_common_dialog_cancel),
                                ntfy );
                    } else {
                        ((CheckedTextView) v).setChecked(isChecked);
                    }
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });

        setCtvListenerForEditSyncTask(ctvDeterminChangedFileSizeGtDestination, type, n_sti, dlg_msg);

        CommonDialog.setViewEnabled(getActivity(), ctvDeterminChangedFileSizeGtDestination, ctvDiffUseFileSize.isChecked());

        final LinearLayout ll_DeterminChangedFileByTimeDependantView=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_sync_diff_use_last_mod_time_dependant_view);
        final LinearLayout ll_syncDiffTimeView=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_diff_file_determin_time_value_view);
        final Spinner spinnerSyncDiffTimeValue = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_diff_file_determin_time_value);
        spinnerSyncDiffTimeValue.setOnItemSelectedListener(null);
        setSpinnerSyncTaskDiffTimeValue(spinnerSyncDiffTimeValue, n_sti.getSyncOptionDifferentFileAllowableTime());

        final Spinner spinnerSyncDstOffsetValue = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_offset_daylight_saving_time_value);
        spinnerSyncDstOffsetValue.setOnItemSelectedListener(null);
        setSpinnerSyncDstOffsetValue(spinnerSyncDstOffsetValue, n_sti.getSyncOptionOffsetOfDst());
        final LinearLayout ll_offset_dst_view=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_spinner_offset_daylight_saving_time_value_view);
        if (n_sti.isSyncOptionIgnoreDstDifference()) {
            ll_offset_dst_view.setVisibility(LinearLayout.VISIBLE);
        } else {
            ll_offset_dst_view.setVisibility(LinearLayout.GONE);
        }

//        CommonUtilities.setCheckedTextViewListener(ctDeterminChangedFileByTime);
        ctDeterminChangedFileByTime.setChecked(n_sti.isSyncOptionDifferentFileByTime());
        if (n_sti.isSyncOptionDifferentFileByTime()) {
            ll_DeterminChangedFileByTimeDependantView.setVisibility(LinearLayout.VISIBLE);
        } else {
            ll_DeterminChangedFileByTimeDependantView.setVisibility(LinearLayout.GONE);
            ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(false);
            ctv_ignore_dst_difference.setChecked(false);
        }
        ctDeterminChangedFileByTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !((CheckedTextView) v).isChecked();
                if (isChecked) {
                    ((CheckedTextView) v).setChecked(isChecked);
                    ll_DeterminChangedFileByTimeDependantView.setVisibility(LinearLayout.VISIBLE);
                    if (ctv_ignore_dst_difference.isChecked()) {
                        ll_offset_dst_view.setVisibility(LinearLayout.VISIBLE);
                    } else {
                        ll_offset_dst_view.setVisibility(LinearLayout.GONE);
                    }
                } else {
                    if (!ctvDiffUseFileSize.isChecked()) {
                        NotifyEvent ntfy=new NotifyEvent(mContext);
                        ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context context, Object[] objects) {
                                ((CheckedTextView) v).setChecked(isChecked);
                                ll_DeterminChangedFileByTimeDependantView.setVisibility(LinearLayout.GONE);
                                ctv_never_overwrite_destination_file_newer_than_the_source_file.setChecked(false);
                                ctv_ignore_dst_difference.setChecked(false);
                                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                            }

                            @Override
                            public void negativeResponse(Context context, Object[] objects) {}
                        });
                        mUtil.showCommonDialogDanger(true,
                                mContext.getString(R.string.msgs_task_sync_task_sync_option_copy_no_compare_title),
                                mContext.getString(R.string.msgs_task_sync_task_sync_option_copy_no_compare_warning),
                                mContext.getString(R.string.msgs_common_dialog_confirm),
                                mContext.getString(R.string.msgs_common_dialog_cancel),
                                ntfy );
                    } else {
                        ((CheckedTextView) v).setChecked(isChecked);
                    }
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });

        ctv_ignore_dst_difference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !((CheckedTextView) v).isChecked();
                ((CheckedTextView) v).setChecked(isChecked);
                if (isChecked) {
                    ll_offset_dst_view.setVisibility(LinearLayout.VISIBLE);
                } else {
                    ll_offset_dst_view.setVisibility(LinearLayout.GONE);
                }
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });

        spinnerSyncDiffTimeValue.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSyncDstOffsetValue.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        CommonDialog.setDlgBoxSizeLimit(mDialog, true);

        final Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_sync_dlg_btn_ok);

        et_sync_main_task_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                n_sti.setSyncTaskName(s.toString());
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
        if (n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) CommonDialog.setViewEnabled(getActivity(), swap_source_destination,false);
        else CommonDialog.setViewEnabled(getActivity(), swap_source_destination, true);

        source_folder_info.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        SyncFolderEditValue nsfev = (SyncFolderEditValue) o[0];
                        nsfev.is_source_folder = true;
                        String prev_source_folder_type = n_sti.getSourceFolderType();
                        n_sti.setChanged(nsfev.isChanged);
                        n_sti.setSourceDirectoryName(nsfev.folder_directory);
                        n_sti.setSourceStorageUuid(nsfev.folder_storage_uuid);
                        n_sti.setSourceSmbAddr(nsfev.folder_smb_addr);
                        n_sti.setSourceSmbDomain(nsfev.folder_smb_domain);
                        n_sti.setSourceSmbHostName(nsfev.folder_smb_hostname);
                        n_sti.setSourceSmbPort(nsfev.folder_smb_port);
                        n_sti.setSourceSmbPassword(nsfev.folder_smb_password);
                        n_sti.setSourceSmbShareName(nsfev.folder_smb_share);
                        n_sti.setSourceSmbAccountName(nsfev.folder_smb_account);
                        n_sti.setSourceFolderType(nsfev.folder_type);
                        n_sti.setSourceSmbProtocol(nsfev.folder_smb_protocol);
                        n_sti.setSourceFolderStatusError(nsfev.folder_error_code);
                        source_folder_info.setText(buildSourceSyncFolderInfo(n_sti, source_folder_info, source_folder_icon));
                        source_folder_info.requestLayout();
                        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                        destination_folder_info.requestLayout();

                        setSpinnerSyncTaskType(spinnerSyncType, n_sti.getSyncTaskType(), n_sti.getDestinationFolderType());
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                        if (!prev_source_folder_type.equals(n_sti.getSourceFolderType())) {
                            ll_wifi_condition_view.setVisibility(LinearLayout.GONE);
                            if ((!n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                                    !n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB))) {
                                ll_advanced_network_option_view.setVisibility(LinearLayout.GONE);
                                ll_wifi_condition_view.setVisibility(LinearLayout.VISIBLE);
                            } else if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                                    n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                                ll_wifi_condition_view.setVisibility(LinearLayout.VISIBLE);
                                n_sti.setDestinationFolderType(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL);
                                ll_advanced_network_option_view.setVisibility(LinearLayout.VISIBLE);
                                destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                                destination_folder_info.requestLayout();
                                String msg=mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_change_destination_folder_to_local);
                                mUtil.showCommonDialogWarn(true, msg, "", null);
                            } else if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) ||
                                    n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                                ll_advanced_network_option_view.setVisibility(LinearLayout.VISIBLE);
                                ll_wifi_condition_view.setVisibility(LinearLayout.VISIBLE);
                                if (n_sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_OFF)) {
                                    String msg="", opt_temp="";
                                    opt_temp= SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP;
                                    msg=mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_change_wifi_confition_to_any);
                                    final String option=opt_temp;
                                    NotifyEvent ntfy = new NotifyEvent(mContext);
                                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                                        @Override
                                        public void positiveResponse(Context context, Object[] objects) {
                                            n_sti.setSyncOptionWifiStatusOption(option);
                                            spinnerSyncWifiStatus.setSelection(1);
                                        }

                                        @Override
                                        public void negativeResponse(Context context, Object[] objects) {
                                        }
                                    });
                                    mUtil.showCommonDialogWarn(true, msg, "", ntfy);
                                }
                            }
                        } else {
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mGp.safMgr.refreshSafList();
                        source_folder_info.setText(buildSourceSyncFolderInfo(n_sti, source_folder_info, source_folder_icon));
                        source_folder_info.requestLayout();
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                    }

                });
                SyncFolderEditValue sfev = new SyncFolderEditValue();
                sfev.task_type=n_sti.getSyncTaskType();
                sfev.is_source_folder = true;
                sfev.folder_title = mContext.getString(R.string.msgs_main_sync_profile_dlg_title_source);
                sfev.folder_directory = n_sti.getSourceDirectoryName();
                sfev.folder_storage_uuid = n_sti.getSourceStorageUuid();
                sfev.folder_smb_addr = n_sti.getSourceSmbAddr();
                sfev.folder_smb_domain = n_sti.getSourceSmbDomain();
                sfev.folder_smb_hostname = n_sti.getSourceSmbHostName();
                sfev.folder_smb_port = n_sti.getSourceSmbPort();
                sfev.folder_smb_password = n_sti.getSourceSmbPassword();
                sfev.folder_smb_share = n_sti.getSourceSmbShareName();
                sfev.folder_smb_account = n_sti.getSourceSmbAccountName();
                sfev.folder_smb_protocol=n_sti.getSourceSmbProtocol();
                sfev.folder_type = n_sti.getSourceFolderType();
                if (!sfev.folder_smb_account.equals("") || !sfev.folder_smb_password.equals("")) {
                    sfev.folder_smb_use_pswd =true;
                } else {
                    sfev.folder_smb_use_pswd =false;
                }
                sfev.folder_error_code=n_sti.getSourceFolderStatusError();
                editSyncFolder(false, n_sti, sfev, ntfy);
            }
        });

        swap_source_destination.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotifyEvent ntfy_swap=new NotifyEvent(mContext);
                ntfy_swap.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        SyncTaskItem t_sti = n_sti.clone();

                        String new_dir=t_sti.getDestinationDirectoryName();
                        if (SyncThread.isTakenDateConvertRequired(new_dir)) {
                            new_dir=SyncThread.removeTakenDateParameter(new_dir);
                            mUtil.showCommonDialogWarn(false, mContext.getString(R.string.msgs_task_sync_task_dlg_swap_warning_title),
                                    mContext.getString(R.string.msgs_task_sync_task_dlg_swap_directory_name_parameter_removed), null);
                        }
                        n_sti.setSourceDirectoryName(new_dir);
                        n_sti.setSourceStorageUuid(t_sti.getDestinationStorageUuid());
                        n_sti.setSourceFolderType(t_sti.getDestinationFolderType());
                        n_sti.setSourceSmbAddr(t_sti.getDestinationSmbAddr());
                        n_sti.setSourceSmbDomain(t_sti.getDestinationSmbDomain());
                        n_sti.setSourceSmbHostName(t_sti.getDestinationSmbHostName());
                        n_sti.setSourceSmbPassword(t_sti.getDestinationSmbPassword());
                        n_sti.setSourceSmbPort(t_sti.getDestinationSmbPort());
                        n_sti.setSourceSmbShareName(t_sti.getDestinationSmbShareName());
                        n_sti.setSourceSmbAccountName(t_sti.getDestinationSmbAccountName());
                        n_sti.setSourceSmbProtocol(t_sti.getDestinationSmbProtocol());
                        n_sti.setSourceFolderStatusError(t_sti.getDestinationFolderStatusError());

                        n_sti.setDestinationDirectoryName(t_sti.getSourceDirectoryName());
                        n_sti.setDestinationStorageUuid(t_sti.getSourceStorageUuid());
                        n_sti.setDestinationFolderType(t_sti.getSourceFolderType());
                        n_sti.setDestinationSmbAddr(t_sti.getSourceSmbAddr());
                        n_sti.setDestinationSmbDomain(t_sti.getSourceSmbDomain());
                        n_sti.setDestinationSmbHostname(t_sti.getSourceSmbHostName());
                        n_sti.setDestinationSmbPassword(t_sti.getSourceSmbPassword());
                        n_sti.setDestinationSmbPort(t_sti.getSourceSmbPort());
                        n_sti.setDestinationSmbShareName(t_sti.getSourceSmbShareName());
                        n_sti.setDestinationSmbAccountName(t_sti.getSourceSmbAccountName());
                        n_sti.setDestinationSmbProtocol(t_sti.getSourceSmbProtocol());
                        n_sti.setDestinationFolderStatusError(t_sti.getSourceFolderStatusError());

                        source_folder_info.setText(buildSourceSyncFolderInfo(n_sti, source_folder_info, source_folder_icon));
                        source_folder_info.requestLayout();
                        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                        destination_folder_info.requestLayout();

                        setSpinnerSyncTaskType(spinnerSyncType, n_sti.getSyncTaskType(), n_sti.getDestinationFolderType());
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                mUtil.showCommonDialogDanger(true,
                        mContext.getString(R.string.msgs_task_sync_task_dlg_swap_warning_title),
                        mContext.getString(R.string.msgs_task_sync_task_dlg_swap_warning_message),
                        mContext.getString(R.string.msgs_common_dialog_confirm),
                        mContext.getString(R.string.msgs_common_dialog_cancel),
                        ntfy_swap);
            }
        });

        destination_folder_info.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        String prev_destination_folder_type = n_sti.getDestinationFolderType();
                        SyncFolderEditValue nsfev = (SyncFolderEditValue) o[0];
                        nsfev.is_source_folder = false;
                        n_sti.setChanged(nsfev.isChanged);
                        n_sti.setDestinationDirectoryName(nsfev.folder_directory);
                        n_sti.setDestinationStorageUuid(nsfev.folder_storage_uuid);
                        n_sti.setDestinationSmbAddr(nsfev.folder_smb_addr);
                        n_sti.setDestinationSmbDomain(nsfev.folder_smb_domain);
                        n_sti.setDestinationSmbHostname(nsfev.folder_smb_hostname);
                        n_sti.setDestinationSmbPort(nsfev.folder_smb_port);
                        n_sti.setDestinationSmbPassword(nsfev.folder_smb_password);
                        n_sti.setDestinationSmbShareName(nsfev.folder_smb_share);
                        n_sti.setDestinationSmbAccountName(nsfev.folder_smb_account);
                        n_sti.setDestinationFolderType(nsfev.folder_type);
                        n_sti.setDestinationSmbProtocol(nsfev.folder_smb_protocol);

                        n_sti.setDestinationZipCompressionLevel(nsfev.zip_comp_level);
                        n_sti.setDestinationZipEncryptMethod(nsfev.zip_enc_method);
                        n_sti.setDestinationZipOutputFileName(nsfev.zip_file_name);
                        n_sti.setDestinationZipPassword(nsfev.zip_file_password);
                        n_sti.setDestinationFolderStatusError(nsfev.folder_error_code);

                        source_folder_info.setText(buildSourceSyncFolderInfo(n_sti, source_folder_info, source_folder_icon));
                        source_folder_info.requestLayout();
                        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                        destination_folder_info.requestLayout();

                        n_sti.setDestinationArchiveRenameFileTemplate(nsfev.archive_file_name_template);
                        n_sti.setDestinationArchiveRetentionPeriod(Integer.valueOf(nsfev.archive_retention_period));
                        n_sti.setDestinationArchiveSuffixOption(nsfev.archive_file_name_suffix_digit);
                        n_sti.setDestinationArchiveIgnoreSourceDirectory(nsfev.archive_ignore_source_directory_hiearachy);

                        if (n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) CommonDialog.setViewEnabled(getActivity(), swap_source_destination, false);
                        else CommonDialog.setViewEnabled(getActivity(), swap_source_destination, true);

                        setSpinnerSyncTaskType(spinnerSyncType, n_sti.getSyncTaskType(), n_sti.getDestinationFolderType());
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);

                        if (!prev_destination_folder_type.equals(n_sti.getDestinationFolderType())) {
                            ll_wifi_condition_view.setVisibility(LinearLayout.VISIBLE);
                            ll_advanced_network_option_view.setVisibility(LinearLayout.GONE);
                            if (!n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) &&
                                    !n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                                ll_wifi_condition_view.setVisibility(LinearLayout.GONE);
                            } else if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB) ||
                                    n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                                ll_advanced_network_option_view.setVisibility(LinearLayout.VISIBLE);
                                if (n_sti.getSyncOptionWifiStatusOption().equals(SyncTaskItem.WIFI_STATUS_WIFI_OFF)) {
                                    String msg="", opt_temp="";
                                    if ((Build.VERSION.SDK_INT>=27 && CommonUtilities.isLocationServiceEnabled(mContext, mGp)) ) {
                                        opt_temp= SyncTaskItem.WIFI_STATUS_WIFI_CONNECT_ANY_AP;
                                        msg=mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_change_wifi_confition_to_any);
                                    } else {
                                        opt_temp= SyncTaskItem.WIFI_STATUS_WIFI_HAS_PRIVATE_IP_ADDRESS;
                                        msg=mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_change_wifi_confition_has_private);
                                    }
                                    final String option=opt_temp;
                                    NotifyEvent ntfy = new NotifyEvent(mContext);
                                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                                        @Override
                                        public void positiveResponse(Context context, Object[] objects) {
                                            n_sti.setSyncOptionWifiStatusOption(option);
                                            spinnerSyncWifiStatus.setSelection(1);
                                        }

                                        @Override
                                        public void negativeResponse(Context context, Object[] objects) {
                                        }
                                    });
                                    mUtil.showCommonDialogWarn(true, msg, "", ntfy);
                                }
                            }
                        } else {
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mGp.safMgr.refreshSafList();
                        destination_folder_info.setText(buildDestinationSyncFolderInfo(n_sti, destination_folder_info, destination_folder_icon));
                        destination_folder_info.requestLayout();
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                    }

                });
                SyncFolderEditValue sfev = new SyncFolderEditValue();
                sfev.task_type=n_sti.getSyncTaskType();
                sfev.is_source_folder = false;
                sfev.folder_title = mContext.getString(R.string.msgs_main_sync_profile_dlg_title_destination);
                sfev.folder_directory = n_sti.getDestinationDirectoryName();
                sfev.folder_storage_uuid = n_sti.getDestinationStorageUuid();
                sfev.folder_smb_addr = n_sti.getDestinationSmbAddr();
                sfev.folder_smb_domain = n_sti.getDestinationSmbDomain();
                sfev.folder_smb_hostname = n_sti.getDestinationSmbHostName();
                sfev.folder_smb_port = n_sti.getDestinationSmbPort();
                sfev.folder_smb_password = n_sti.getDestinationSmbPassword();
                sfev.folder_smb_share = n_sti.getDestinationSmbShareName();
                sfev.folder_smb_account = n_sti.getDestinationSmbAccountName();
                sfev.folder_type = n_sti.getDestinationFolderType();
                sfev.folder_smb_protocol = n_sti.getDestinationSmbProtocol();

                if (!sfev.folder_smb_account.equals("") || !sfev.folder_smb_password.equals("")) {
                    sfev.folder_smb_use_pswd =true;
                } else {
                    sfev.folder_smb_use_pswd =false;
                }

                sfev.zip_comp_level = n_sti.getDestinationZipCompressionLevel();
                sfev.zip_enc_method = n_sti.getDestinationZipEncryptMethod();
                sfev.zip_file_name = n_sti.getDestinationZipOutputFileName();
                sfev.zip_file_password = n_sti.getDestinationZipPassword();

                sfev.archive_file_name_template=n_sti.getDestinationArchiveRenameFileTemplate();
                sfev.archive_retention_period=String.valueOf(n_sti.getDestinationArchiveRetentionPeriod());
                sfev.archive_file_name_suffix_digit=n_sti.getDestinationArchiveSuffixOption();
                sfev.archive_ignore_source_directory_hiearachy =n_sti.isDestinationArchiveIgnoreSourceDirectory();

                sfev.folder_error_code=n_sti.getDestinationFolderStatusError();
                editSyncFolder(false, n_sti, sfev, ntfy);
            }
        });

        // wifi address listボタンの指定
        edit_wifi_addr_list.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                //Listen setRemoteShare response
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context arg0, Object[] arg1) {
                        setWifiApWhiteListInfo(n_sti.getSyncOptionWifiIPAddressGrantList(), edit_wifi_addr_list);
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                    }

                    @Override
                    public void negativeResponse(Context arg0, Object[] arg1) {
                        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
                    }

                });
                mTaskUtil.editIPAddressFilterDlg(n_sti.getSyncOptionWifiIPAddressGrantList(), ntfy);
            }
        });

        // file filterボタンの指定
        file_filter_btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                invokeEditFileFilterDlg(mDialog, n_sti, type, dlg_msg);
            }
        });
        // directory filterボタンの指定
        dir_filter_btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                invokeEditDirFilterDlg(mDialog, n_sti, type, dlg_msg);
            }
        });

        final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_sync_dlg_btn_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mGp.syncTaskListAdapter.notifyDataSetChanged();
                if (btn_ok.isEnabled()) {
                    NotifyEvent ntfy = new NotifyEvent(mContext);
                    ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context context, Object[] objects) {
                            mUtil.addDebugMsg(1,"I","editSyncTask edit cancelled, type="+type+", task="+pfli.getSyncTaskName());
                            mFragment.dismiss();
                            if (mNotifyComplete != null)
                                mNotifyComplete.notifyToListener(false, null);
                        }

                        @Override
                        public void negativeResponse(Context context, Object[] objects) {
                        }
                    });
                    mUtil.showCommonDialogWarn(true,
                            mContext.getString(R.string.msgs_task_sync_folder_dlg_confirm_msg_nosave), "", ntfy);
                } else {
                    mUtil.addDebugMsg(1,"I","editSyncTask edit cancelled");
                    mFragment.dismiss();
                    if (mNotifyComplete != null) mNotifyComplete.notifyToListener(false, null);
                }
            }
        });
        // Cancelリスナーの指定
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int kc, KeyEvent keyEvent) {
                switch (kc) {
                    case KEYCODE_BACK:
                        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            btn_cancel.performClick();
                        }
                        return true;
                    // break;
                    default:
                        // break;
                }

                return false;
            }
        });
        // OKボタンの指定
        CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
        btn_ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final SyncTaskItem new_stli = buildSyncTaskListItem(mDialog, n_sti);
                NotifyEvent ntfy_destination_dir_not_specified = new NotifyEvent(mContext);
                ntfy_destination_dir_not_specified.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        if (type.equals(TASK_EDIT_METHOD_EDIT)) {
                            mGp.syncTaskListAdapter.remove(pfli);
                            mGp.syncTaskListAdapter.add(new_stli);
                            mGp.syncTaskListAdapter.sort();
                            mGp.syncTaskListAdapter.notifyDataSetChanged();
                        } else if (type.equals(TASK_EDIT_METHOD_COPY)) {
                            mGp.syncTaskListAdapter.setAllItemChecked(false);
                            new_stli.setChecked(true);
                            new_stli.setSyncTaskPosition(mGp.syncTaskListAdapter.getCount());
                            mGp.syncTaskListAdapter.add(new_stli);
                            mGp.syncTaskListAdapter.sort();
                            mGp.syncTaskListAdapter.notifyDataSetChanged();
                            mGp.syncTaskView.setSelection(mGp.syncTaskListAdapter.getCount() - 1);
                        } else if (type.equals(TASK_EDIT_METHOD_ADD)) {
                            new_stli.setSyncTaskPosition(mGp.syncTaskListAdapter.getCount());
                            mGp.syncTaskListAdapter.add(new_stli);
                            mGp.syncTaskListAdapter.sort();
                            mGp.syncTaskListAdapter.notifyDataSetChanged();
                            mGp.syncTaskView.setSelection(mGp.syncTaskListAdapter.getCount() - 1);
                        }
                        if (mNotifyComplete != null) mNotifyComplete.notifyToListener(true, null);
                        mTaskUtil.saveConfigListWithAutosave(mContext, mGp, mUtil);
                        mFragment.dismissAllowingStateLoss();
                        mUtil.addDebugMsg(1,"I","editSyncTask edit saved, type="+type+", task="+new_stli.getSyncTaskName());
                        ((ActivityMain)getActivity()).refreshOptionMenu();
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {}
                });
                if (!new_stli.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP) && new_stli.getDestinationDirectoryName().equals("")) {
                    mUtil.showCommonDialogWarn(true,
                            mContext.getString(R.string.msgs_main_sync_profile_dlg_destination_directory_not_specified), "",
                            ntfy_destination_dir_not_specified);
                } else {
                    ntfy_destination_dir_not_specified.notifyToListener(true, null);
                }
            }
        });

        checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
    }

    private String removeKeyword(String input_string) {
        String new_val=input_string;
        if (input_string.contains("%")) {
            String new_dir=input_string;
            for(String kw_item: SyncTaskItem.TEMPLATES) {
                new_dir=new_dir.replaceAll(kw_item, "");
            }
            if (!input_string.equals(new_dir)) {
                while(new_dir.endsWith("/")) new_dir=new_dir.substring(0, new_dir.length()-1);
                new_val=new_dir;
            }
        }
        return new_val;
    }

//    private String buildArchiveInfo(SyncTaskItem sti, Button btn) {
//        return "Archive info";
//    }

    private void setCtvListenerForEditSyncTask(final CheckedTextView ctv, final String type, final SyncTaskItem n_sti, final TextView dlg_msg) {
        ctv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !((CheckedTextView) v).isChecked();
                ((CheckedTextView) v).setChecked(isChecked);
                checkSyncTaskOkButtonEnabled(mDialog, type, n_sti, dlg_msg);
            }
        });
    }

    private void checkArchiveOkButtonEnabled(SyncFolderEditValue current_sfev, SyncTaskItem n_sti, Dialog dialog) {
        final Button btn_sync_folder_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);
        final Spinner sp_sync_retain_period = (Spinner) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_retention_period);

        final EditText et_file_template = (EditText) dialog.findViewById(R.id.edit_sync_folder_dlg_archive_file_name_template);

        boolean changed=false;

        SyncFolderEditValue nsfev=buildSyncFolderEditValue(dialog, current_sfev);
        changed=!current_sfev.isSame(nsfev);
        if (!changed) {
            if (!et_file_template.getText().toString().equals(n_sti.getDestinationArchiveRenameFileTemplate())) changed=true;
            else if (n_sti.getDestinationArchiveRetentionPeriod()!=sp_sync_retain_period.getSelectedItemPosition()) changed=true;
        }
        if (changed) setSyncFolderOkButtonEnabled(btn_sync_folder_ok, true);//CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_ok, true);
        else setSyncFolderOkButtonEnabled(btn_sync_folder_ok, false);//CommonDialog.setViewEnabled(getActivity(), btn_sync_folder_ok, false);

    }

    private String getArchiveSuffixOptionFromSpinner(Spinner spinner) {
        String result="4";
        String sel=spinner.getSelectedItem().toString();

        if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_0))) result="0";
        else if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_2))) result="2";
        else if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_3))) result="3";
        else if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_4))) result="4";
        else if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_5))) result="5";
        else if (sel.equals(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_6))) result="6";

        return result;
    }

    private String getConvertedDirectoryFileName(String template, long time, String org_name) {
        String result="";

        Date c_date = new Date("2000/01/02 01:20:30");
        Date e_date = new Date();
        String taken_converted=SyncThread.replaceKeywordTakenDateValue(template, c_date.getTime());
        String exec_converted=SyncThread.replaceKeywordExecutionDateValue(taken_converted, e_date.getTime());
        result=exec_converted.replaceAll(SyncTaskItem.TEMPLATE_ORIGINAL_NAME, org_name);
        return result;
    }

    private String getSyncTaskArchiveTemplateNewName(int suffix_option, String file_template, String destination_dir, SyncTaskItem n_sti) {
        String result="";
        String suffix="";
        if (suffix_option==1) suffix="_01";
        else if (suffix_option==2) suffix="_001";
        else if (suffix_option==3) suffix="_0001";
        else if (suffix_option==4) suffix="_00001";
        else if (suffix_option==5) suffix="_000001";

        String new_name=getConvertedDirectoryFileName(file_template, System.currentTimeMillis(), "DSC-001")+suffix+".jpg";
        if (destination_dir.equals("")) result=new_name;
        else {
            String tgt_dir_temp=getConvertedDirectoryFileName(destination_dir, System.currentTimeMillis(), "");
            result=tgt_dir_temp.startsWith("/")?tgt_dir_temp+"/"+new_name:"/"+tgt_dir_temp+"/"+new_name;
        }

        return result;
    }

    private void setSpinnerSyncTaskArchiveSuffixSeq(Spinner spinner, String cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_prompt_title));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_0));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_2));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_3));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_4));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_5));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_suffix_seq_digit_6));

        int sel=0;
        if (cv.equals("0")) sel=0;
        else if (cv.equals("2")) sel=1;
        else if (cv.equals("3")) sel=2;
        else if (cv.equals("4")) sel=3;
        else if (cv.equals("5")) sel=4;
        else if (cv.equals("6")) sel=5;
        spinner.setSelection(sel);

    }

    private void setSpinnerSyncTaskPictureRetainPeriod(Spinner spinner, int cv) {
        CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        adapter.setDropDownTextWordwrapEnabled(true);
        adapter.setSpinner(spinner);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setPrompt(mContext.getString(R.string.msgs_sync_folder_archive_period_prompt_title));
        spinner.setAdapter(adapter);
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_0_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_7_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_30_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_60_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_90_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_180_days));
        adapter.add(mContext.getString(R.string.msgs_sync_folder_archive_period_1_years));
        spinner.setSelection(cv);
    }

    private SyncTaskItem buildSyncTaskListItem(Dialog dialog, SyncTaskItem base_stli) {
        final EditText et_sync_main_task_name = (EditText) dialog.findViewById(R.id.edit_sync_task_task_name);
        final CheckedTextView ctv_auto = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_ctv_auto);

        final Spinner spinnerSyncType = (Spinner) dialog.findViewById(R.id.edit_sync_task_sync_type);
        final Spinner spinnerSyncWifiStatus = (Spinner) dialog.findViewById(R.id.edit_sync_task_option_spinner_wifi_status);

        final CheckedTextView ctv_edit_sync_tak_option_keep_conflict_file = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file);
        final Spinner spinnerTwoWaySyncConflictRule = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_conflict_file_rule_value);

        final CheckedTextView ctv_task_sync_when_cahrging = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_start_when_charging);
        final CheckedTextView ctv_never_overwrite_destination_file_newer_than_the_source_file = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_never_overwrite_destination_file_if_it_is_newer_than_the_source_file);
        final CheckedTextView ctv_ignore_unusable_character_used_directory_file_name = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_unusable_character_used_directory_file_name);

        final CheckedTextView ctv_ignore_dst_difference = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_ignore_dst_difference);
        final Spinner spinnerSyncDstOffsetValue = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_offset_daylight_saving_time_value);

        final Button swap_source_destination = (Button) dialog.findViewById(R.id.edit_sync_task_swap_source_and_destination_btn);
        final Button source_folder_info = (Button) dialog.findViewById(R.id.edit_sync_task_source_folder_info_btn);
        final Button destination_folder_info = (Button) dialog.findViewById(R.id.edit_sync_task_destination_folder_info_btn);

        final Button dir_filter_btn = (Button) dialog.findViewById(R.id.sync_filter_edit_dir_filter_btn);
        final Button file_filter_btn = (Button) dialog.findViewById(R.id.sync_filter_edit_file_filter_btn);

        final CheckedTextView ctvSyncFileTypeSpecific = (CheckedTextView) dialog.findViewById(R.id.sync_filter_file_type_specific);

        final CheckedTextView ctvSyncFileTypeAudio = (CheckedTextView) dialog.findViewById(R.id.sync_filter_file_type_audio);
        final CheckedTextView ctvSyncFileTypeImage = (CheckedTextView) dialog.findViewById(R.id.sync_filter_file_type_image);
        final CheckedTextView ctvSyncFileTypeVideo = (CheckedTextView) dialog.findViewById(R.id.sync_filter_file_type_video);
        final CheckedTextView ctvSyncSpecificSubDir = (CheckedTextView) dialog.findViewById(R.id.sync_filter_sub_directory_specific);
        final CheckedTextView ctvProcessRootDirFile = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_source_root_dir_file);
        final CheckedTextView ctvSyncSubDir = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_sub_dir);
        final CheckedTextView ctvSyncEmptyDir = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_empty_directory);
        final CheckedTextView ctvSyncHiddenDir = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_directory);
        final CheckedTextView ctvSyncHiddenFile = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_file);
        final CheckedTextView ctvProcessOverride = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_process_override_delete_file);
        final CheckedTextView ctvConfirmOverride = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_confirm_override_delete_file);
        final CheckedTextView ctvDeleteFirst = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_delete_first_when_mirror);

        final CheckedTextView ctvIgnoreFilterRemoveDirFileDesNotExistsInSource = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_remove_dir_file_excluded_by_filter);

        final CheckedTextView ctvShowSpecialOption = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_show_special_option);
        final CheckedTextView ctvDoNotResetFileLastMod = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_do_mot_reset_file_last_mod_time);
        final CheckedTextView ctvDeterminChangedFileSizeGtDestination = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_file_size_greater_than_destination);
        final CheckedTextView ctvRetry = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_retry_if_error_occured);
        final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_remote_small_io_area);
        final CheckedTextView ctvTestMode = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_test_mode);
        final CheckedTextView ctvDiffUseFileSize = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_file_size);
        final CheckedTextView ctDeterminChangedFileByTime = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_last_mod_time);

        final CheckedTextView ctv_sync_allow_global_ip_addr = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_sync_allow_global_ip_address);

        final CheckedTextView ctv_sync_remove_source_if_empty = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_remove_directory_if_empty_when_move);
        final Spinner sp_sync_task_option_error_option=(Spinner)mDialog.findViewById(R.id.edit_sync_task_option_error_option_value);

        final CheckedTextView ctv_confirm_exif_date = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_confirm_exif_date);
        final CheckedTextView ctv_ignore_file_size_gt_4gb = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_source_file_that_file_size_gt_4gb);
        final CheckedTextView ctv_ignore_file_name_length_255_byte = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_file_name_length_exceed_255_byte);

        final Spinner spinnerSyncDiffTimeValue = (Spinner) dialog.findViewById(R.id.edit_sync_task_option_spinner_diff_file_determin_time_value);
        final Button btn_ok = (Button) dialog.findViewById(R.id.edit_profile_sync_dlg_btn_ok);

        SyncTaskItem nstli = base_stli.clone();

        nstli.setSyncTaskAuto(ctv_auto.isChecked());
        nstli.setSyncTaskName(et_sync_main_task_name.getText().toString());

        setSyncTaskTypeFromSpinnere(spinnerSyncType, nstli);

        nstli.setSyncProcessRootDirFile(ctvProcessRootDirFile.isChecked());

        nstli.setSyncOptionSyncSubDirectory(ctvSyncSubDir.isChecked());
        nstli.setSyncOptionSyncEmptyDirectory(ctvSyncEmptyDir.isChecked());
        nstli.setSyncOptionSyncHiddenDirectory(ctvSyncHiddenDir.isChecked());
        nstli.setSyncOptionSyncHiddenFile(ctvSyncHiddenFile.isChecked());

        nstli.setSyncOverrideCopyMoveFile(ctvProcessOverride.isChecked());

        nstli.setSyncConfirmOverrideOrDelete(ctvConfirmOverride.isChecked());

        nstli.setSyncOptionDeleteFirstWhenMirror(ctvDeleteFirst.isChecked());

        nstli.setSyncTaskErrorOption(sp_sync_task_option_error_option.getSelectedItemPosition());

        String wifi_sel = getSpinnerSyncTaskWifiOptionValue(spinnerSyncWifiStatus);
        nstli.setSyncOptionWifiStatusOption(wifi_sel);
        nstli.setSyncOptionSyncAllowGlobalIpAddress(ctv_sync_allow_global_ip_addr.isChecked());

        nstli.setSyncOptionSyncWhenCharging(ctv_task_sync_when_cahrging.isChecked());

        nstli.setSyncDoNotResetFileLastModified(ctvDoNotResetFileLastMod.isChecked());

        if (ctvRetry.isChecked()) nstli.setSyncOptionRetryCount(3);
        else nstli.setSyncOptionRetryCount(0);
        nstli.setSyncOptionUseSmallIoBuffer(ctvSyncUseRemoteSmallIoArea.isChecked());
        nstli.setSyncTestMode(ctvTestMode.isChecked());
        nstli.setSyncOptionDifferentFileBySize(ctvDiffUseFileSize.isChecked());
        nstli.setSyncDifferentFileSizeGreaterThanTagetFile(ctvDeterminChangedFileSizeGtDestination.isChecked());
        nstli.setSyncOptionDifferentFileByTime(ctDeterminChangedFileByTime.isChecked());

        nstli.setSyncOptionIgnoreDstDifference(ctv_ignore_dst_difference.isChecked());
        try {
            String dst_offset=(String)spinnerSyncDstOffsetValue.getSelectedItem();
            nstli.setSyncOptionOffsetOfDst(Integer.valueOf(dst_offset));
        } catch(Exception e) {}

        nstli.setSyncOptionDoNotOverwriteDestinationFileIfItIsNewerThanTheSourceFile(ctv_never_overwrite_destination_file_newer_than_the_source_file.isChecked());

        nstli.setSyncOptionIgnoreDirectoriesOrFilesThatContainUnusableCharacters(ctv_ignore_unusable_character_used_directory_file_name.isChecked());

        String diff_val = spinnerSyncDiffTimeValue.getSelectedItem().toString();
        nstli.setSyncOptionDifferentFileAllowableTime(Integer.valueOf(diff_val));
        if (!ctvSyncFileTypeSpecific.isChecked()) {
            nstli.getFileNameFilter().clear();
            nstli.setSyncFileTypeAudio(false);
            nstli.setSyncFileTypeImage(false);
            nstli.setSyncFileTypeVideo(false);
        } else {
            nstli.setSyncFileTypeAudio(ctvSyncFileTypeAudio.isChecked());
            nstli.setSyncFileTypeImage(ctvSyncFileTypeImage.isChecked());
            nstli.setSyncFileTypeVideo(ctvSyncFileTypeVideo.isChecked());
        }

        if (!ctvSyncSpecificSubDir.isChecked()) {
            nstli.getDirectoryFilter().clear();
        }

        nstli.setSyncOptionMoveOnlyRemoveSourceDirectoryIfEmpty(ctv_sync_remove_source_if_empty.isChecked());

        nstli.setSyncOptionConfirmNotExistsExifDate(ctv_confirm_exif_date.isChecked());

        nstli.setSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb(ctv_ignore_file_size_gt_4gb.isChecked());

        nstli.setSyncOptionRemoveDirectoryFileThatExcludedByFilter(ctvIgnoreFilterRemoveDirFileDesNotExistsInSource.isChecked());

        nstli.setSyncOptionIgnoreDestinationFileNameLengthExceed255Byte(ctv_ignore_file_name_length_255_byte.isChecked());

        nstli.setSyncTwoWayKeepConflictFile(ctv_edit_sync_tak_option_keep_conflict_file.isChecked());
        setTwoWaySyncConflictRuleFromSpinnere(spinnerTwoWaySyncConflictRule, nstli);

        return nstli;
    }

    static public void showFieldHelp(Activity a, GlobalParameters mGp, String title, String help_msg) {
        Dialog dialog = new Dialog(a, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.help_view);
        LinearLayout ll_view = (LinearLayout) dialog.findViewById(R.id.help_view_title_view);
        ll_view.setBackgroundColor(mGp.themeColorList.title_background_color);

        TextView dlg_tv = (TextView) dialog.findViewById(R.id.help_view_title_text);
        dlg_tv.setTextColor(mGp.themeColorList.title_text_color);

        final EditText et_find_string=(EditText)dialog.findViewById(R.id.help_view_find_value);
        final ImageButton ib_find_next=(ImageButton) dialog.findViewById(R.id.help_view_find_next);
        final ImageButton ib_find_prev=(ImageButton) dialog.findViewById(R.id.help_view_find_prev);
        final TextView tv_find_count=(TextView) dialog.findViewById(R.id.help_view_find_count);

        WebView dlg_wb = (WebView) dialog.findViewById(R.id.help_view_help);

        dlg_wb.loadUrl("file:///android_asset/" + help_msg);
        dlg_wb.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        dlg_wb.getSettings().setBuiltInZoomControls(false);
        dlg_wb.setInitialScale(0);

        ib_find_next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg_wb.findNext(true);
            }
        });

        ib_find_prev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg_wb.findNext(false);
            }
        });

        CommonDialog.setViewEnabled(a, ib_find_next, false);
        CommonDialog.setViewEnabled(a, ib_find_prev, false);

        final ColorStateList default_text_color=tv_find_count.getTextColors();
        dlg_wb.setFindListener(new WebView.FindListener() {
            @Override
            public void onFindResultReceived(int i, int i1, boolean b) {
                if (et_find_string.getText().length()>0) {
                    if (i1>0) {
                        tv_find_count.setText((i+1)+"/"+i1);
                        tv_find_count.setTextColor(default_text_color);
                        CommonDialog.setViewEnabled(a, ib_find_next, true);
                        CommonDialog.setViewEnabled(a, ib_find_prev, true);
                    } else {
                        tv_find_count.setText(0+"/"+0);
                        tv_find_count.setTextColor(Color.RED);
                        CommonDialog.setViewEnabled(a, ib_find_next, false);
                        CommonDialog.setViewEnabled(a, ib_find_prev, false);
                    }
                } else {
                    CommonDialog.setViewEnabled(a, ib_find_next, false);
                    CommonDialog.setViewEnabled(a, ib_find_prev, false);
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
                    dlg_wb.findAllAsync(s.toString());
                } else {
                    CommonDialog.setViewEnabled(a, ib_find_next, false);
                    CommonDialog.setViewEnabled(a, ib_find_prev, false);
                    dlg_wb.findAllAsync("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dlg_tv.setText(title);

        dialog.show();
    }

    private void setTwoWaySyncConflictRuleFromSpinnere(Spinner spinner, SyncTaskItem n_stli) {
        String so = mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_newer);
        if (spinner.getSelectedItemPosition()<spinner.getAdapter().getCount()) so=spinner.getSelectedItem().toString();
        if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_ask_user)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_ASK_USER);
        else if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_newer)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_NEWER);
        else if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_older)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_OLDER);
        else if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_from_source_to_destination)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_SOURCE_TO_DESTINATION);
        else if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_copy_from_destination_to_source)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_COPY_FROM_DESTINATION_TO_SOURCE);
        else if (so.equals(mContext.getString(R.string.msgs_task_twoway_sync_conflict_copy_rurle_skip_sync_file)))
            n_stli.setSyncTwoWayConflictFileRule(SyncTaskItem.SYNC_TASK_TWO_WAY_OPTION_SKIP_SYNC_FILE);
    }

    private void setSyncTaskTypeFromSpinnere(Spinner spinner, SyncTaskItem n_stli) {
        String so = mContext.getString(R.string.msgs_main_sync_profile_dlg_mirror);
        if (spinner.getSelectedItemPosition()<spinner.getAdapter().getCount()) so=spinner.getSelectedItem().toString();
        if (so.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_mirror)))
            n_stli.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_MIRROR);
        else if (so.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_copy)))
            n_stli.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_COPY);
        else if (so.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_move)))
            n_stli.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_MOVE);
        else if (so.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync)))
            n_stli.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_SYNC);
        else if (so.equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_archive)))
            n_stli.setSyncTaskType(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE);
    }

    private void setSyncTaskFieldHelpListener(Dialog dialog, SyncTaskItem sti) {
        final ImageButton help_sync_option = (ImageButton) dialog.findViewById(R.id.edit_profile_sync_help);
        help_sync_option.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_sync_task_title),
                        mContext.getString(R.string.msgs_help_sync_task_file));
            }
        });
    }

    private void setSyncFolderFieldHelpListener(Dialog dialog, final String f_type) {
        final ImageButton help_sync_folder = (ImageButton) dialog.findViewById(R.id.edit_sync_folder_dlg_help);
        help_sync_folder.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (f_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
                    showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_sync_folder_internal_title),
                            mContext.getString(R.string.msgs_help_sync_folder_internal_file));
                } else if (f_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                    showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_sync_folder_smb_title),
                            mContext.getString(R.string.msgs_help_sync_folder_smb_file));
                } else if (f_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                    showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_sync_folder_zip_title),
                            mContext.getString(R.string.msgs_help_sync_folder_zip_file));
                }
            }
        });
    }

    private void setSyncFolderKeywordHelpListener(Dialog dialog, final boolean exec_only) {
        ImageButton ib_help=(ImageButton)dialog.findViewById(R.id.edit_sync_folder_edit_keywor_dlg_help);
        ib_help.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFieldHelp(mActivity, mGp, mContext.getString(R.string.msgs_help_sync_folder_keyword_title),
                        mContext.getString(R.string.msgs_help_sync_folder_keyword));
            }
        });
    }

    private void checkSyncTaskOkButtonEnabled(Dialog dialog, String type, SyncTaskItem n_sti, TextView dlg_msg) {
        final LinearLayout ll_file_filter = (LinearLayout) mDialog.findViewById(R.id.sync_filter_file_detail_view);

//        final CheckedTextView ctvSyncEmptyDir = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_empty_directory);
//        final CheckedTextView ctvSyncHiddenDir = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_directory);
//        final CheckedTextView ctvSyncHiddenFile = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_hidden_file);

        final LinearLayout ll_ctvProcessOverride = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_process_override_delete_file);
        final LinearLayout ll_ctvConfirmOverride = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_confirm_override_delete_file);

//        final CheckedTextView ctUseExtendedDirectoryFilter1 = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_extended_filter1);
//        final CheckedTextView ctvShowSpecialOption = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_show_special_option);
        final LinearLayout ll_ctvDoNotResetFileLastMod = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_do_mot_reset_file_last_mod_time);

//        final CheckedTextView ctvRetry = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_retry_if_error_occured);
//        final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_use_remote_small_io_area);
//        final CheckedTextView ctvTestMode = (CheckedTextView) dialog.findViewById(R.id.edit_sync_task_option_ctv_sync_test_mode);

        final LinearLayout ll_wifi_condition_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_wifi_condition_view);
        final LinearLayout ll_wifi_wl_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_wl_view);
        final LinearLayout ll_wifi_wl_address_view = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_address_list_view);
        final CheckedTextView ctv_sync_allow_global_ip_addr = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_sync_allow_global_ip_address);
        final Spinner spinnerSyncWifiStatus = (Spinner) mDialog.findViewById(R.id.edit_sync_task_option_spinner_wifi_status);

        final LinearLayout ll_ctvDiffUseFileSize = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_sync_diff_use_file_size);
        final LinearLayout ll_ctDeterminChangedFileByTime = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_sync_diff_use_last_mod_time_view);

        final CheckedTextView ctvDoNotResetRemoteFile = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_do_mot_reset_file_last_mod_time);

        final LinearLayout ll_diff_time_allowed_time = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_diff_file_determin_time_value_view);

        final LinearLayout ll_sync_remove_source_if_empty = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_remove_directory_if_empty_when_move_view);

        final CheckedTextView ctv_ignore_dst_difference = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_ignore_dst_difference);

        final CheckedTextView ctvDeterminChangedFileSizeGtDestination = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_file_size_greater_than_destination);
        final CheckedTextView ctvDiffUseFileSize = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_file_size);

        final CheckedTextView ctDeterminChangedFileByTime = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_diff_use_last_mod_time);
        final LinearLayout ll_DeterminChangedFileByTime_dependant_view=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_sync_diff_use_last_mod_time_dependant_view);

        final CheckedTextView ctv_sync_remove_source_if_empty = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_remove_directory_if_empty_when_move);
        final Spinner sp_sync_task_option_error_option=(Spinner)mDialog.findViewById(R.id.edit_sync_task_option_error_option_value);

        final LinearLayout ll_edit_sync_tak_option_keep_conflict_file=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_keep_conflic_file_view);
        final LinearLayout ll_spinnerTwoWaySyncConflictRule=(LinearLayout)mDialog.findViewById(R.id.edit_sync_task_option_twoway_sync_conflict_file_rule_view);

        final CheckedTextView ctvIgnoreFilterRemoveDirFileDesNotExistsInSource = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_remove_dir_file_excluded_by_filter);
        final LinearLayout ctvIgnoreFilterRemoveDirFileDesNotExistsInSourceView = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_sync_remove_dir_file_excluded_by_filter);
        final CheckedTextView ctvDeleteFirst = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ctv_sync_delete_first_when_mirror);
        final LinearLayout ctvDeleteFirstView = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ll_sync_delete_first_when_mirror);

        final LinearLayout ll_ignore_file_size_gt_4gb = (LinearLayout) mDialog.findViewById(R.id.edit_sync_task_option_ignore_source_file_that_file_size_gt_4gb_view);
        final CheckedTextView ctv_ignore_file_size_gt_4gb = (CheckedTextView) mDialog.findViewById(R.id.edit_sync_task_option_ignore_source_file_that_file_size_gt_4gb);

        final Button destination_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_btn);
        final ImageView destination_folder_icon=(ImageView)mDialog.findViewById(R.id.edit_sync_task_destination_folder_info_icon);
        final Button swap_source_destination = (Button) mDialog.findViewById(R.id.edit_sync_task_swap_source_and_destination_btn);
        final Button source_folder_info = (Button) mDialog.findViewById(R.id.edit_sync_task_source_folder_info_btn);
        final ImageView source_folder_icon=(ImageView)mDialog.findViewById(R.id.edit_sync_task_source_folder_info_icon);

        final EditText et_sync_main_task_name = (EditText) mDialog.findViewById(R.id.edit_sync_task_task_name);

        final Button btn_ok = (Button) dialog.findViewById(R.id.edit_profile_sync_dlg_btn_ok);
        String t_name_msg = checkTaskNameValidity(type, n_sti.getSyncTaskName(), dlg_msg, btn_ok);
        boolean error_detected = false;
        ll_edit_sync_tak_option_keep_conflict_file.setVisibility(LinearLayout.GONE);
        ll_spinnerTwoWaySyncConflictRule.setVisibility(LinearLayout.GONE);
        swap_source_destination.setVisibility(Button.VISIBLE);

        ll_wifi_wl_view.setVisibility(Button.GONE);
        ll_wifi_wl_address_view.setVisibility(Button.GONE);
        ctv_sync_allow_global_ip_addr.setVisibility(CheckedTextView.VISIBLE);
        if (spinnerSyncWifiStatus.getSelectedItem().toString().equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_private_address))) {
            ctv_sync_allow_global_ip_addr.setVisibility(CheckedTextView.GONE);
        } else if (spinnerSyncWifiStatus.getSelectedItem().toString().equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_specific_address))) {
            ll_wifi_wl_view.setVisibility(Button.VISIBLE);
            ll_wifi_wl_address_view.setVisibility(Button.VISIBLE);
        } else {
//            ll_task_skip_if_network_not_satisfied.setVisibility(LinearLayout.GONE);
//            ctv_task_skip_if_network_not_satisfied.setChecked(false);
        }

        if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
            ctvIgnoreFilterRemoveDirFileDesNotExistsInSourceView.setVisibility(LinearLayout.VISIBLE);
            ctvDeleteFirstView.setVisibility(LinearLayout.VISIBLE);
        } else {
            ctvIgnoreFilterRemoveDirFileDesNotExistsInSourceView.setVisibility(LinearLayout.GONE);
            ctvDeleteFirstView.setVisibility(LinearLayout.GONE);
        }

        if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MOVE)) {
            ll_sync_remove_source_if_empty.setVisibility(CheckedTextView.VISIBLE);
        } else {
            ll_sync_remove_source_if_empty.setVisibility(CheckedTextView.GONE);
            ctv_sync_remove_source_if_empty.setChecked(false);
        }

        if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
            ll_file_filter.setVisibility(LinearLayout.GONE);

            ll_ctvProcessOverride.setVisibility(CheckedTextView.GONE);
//            ll_ctvConfirmOverride.setVisibility(CheckedTextView.GONE);

            ll_ctvDoNotResetFileLastMod.setVisibility(CheckedTextView.GONE);

            ll_ctvDiffUseFileSize.setVisibility(CheckedTextView.GONE);
            ll_ctDeterminChangedFileByTime.setVisibility(CheckedTextView.GONE);

            ll_diff_time_allowed_time.setVisibility(CheckedTextView.GONE);
        } else {
            ll_file_filter.setVisibility(LinearLayout.VISIBLE);

            ll_ctvProcessOverride.setVisibility(CheckedTextView.VISIBLE);
//            ll_ctvConfirmOverride.setVisibility(CheckedTextView.VISIBLE);

            ll_ctvDoNotResetFileLastMod.setVisibility(CheckedTextView.VISIBLE);

            ll_ctvDiffUseFileSize.setVisibility(CheckedTextView.VISIBLE);

            if (ctvDiffUseFileSize.isChecked() && ctvDeterminChangedFileSizeGtDestination.isChecked()) {
                ll_ctDeterminChangedFileByTime.setVisibility(LinearLayout.GONE);
                ll_diff_time_allowed_time.setVisibility(LinearLayout.GONE);
            } else {
                ll_ctDeterminChangedFileByTime.setVisibility(LinearLayout.VISIBLE);
                ll_diff_time_allowed_time.setVisibility(LinearLayout.VISIBLE);
            }

            if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_SYNC)) {
                ll_edit_sync_tak_option_keep_conflict_file.setVisibility(LinearLayout.VISIBLE);
                ll_spinnerTwoWaySyncConflictRule.setVisibility(LinearLayout.VISIBLE);
                swap_source_destination.setVisibility(Button.GONE);
            }
        }

        if (n_sti.getDestinationStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) {
            ll_ignore_file_size_gt_4gb.setVisibility(LinearLayout.GONE);
            n_sti.setSyncOptionIgnoreDestinationFileWhenSourceFileSizeGreaterThan4Gb(false);
            ctv_ignore_file_size_gt_4gb.setChecked(false);
        } else {
            ll_ignore_file_size_gt_4gb.setVisibility(LinearLayout.VISIBLE);
        }

        if (n_sti.getSyncTaskType().equals(SyncTaskItem.SYNC_TASK_TYPE_MIRROR)) {
            if (t_name_msg.equals("")) t_name_msg=checkTakenDateParameterUsed(n_sti.getDestinationDirectoryName());
        }

        if (t_name_msg.equals("")) {
            t_name_msg= TaskListUtils.hasSyncTaskNameUnusableCharacter(mContext, et_sync_main_task_name.getText().toString());
        }
        if (t_name_msg.equals("")) {
            if (et_sync_main_task_name.getText().length()>SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH) t_name_msg=mContext.getString(R.string.msgs_sync_task_name_length_invalid, SyncTaskItem.SYNC_TASK_NAME_MAX_LENGTH);
        }

        if (t_name_msg.equals("")) {
            String e_msg = checkSourceDestinationCombination(dialog, n_sti);
            if (!e_msg.equals("")) {
                CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
                setDialogMsg(dlg_msg, e_msg);
            } else {
                if (spinnerSyncWifiStatus.getSelectedItem().toString().equals(mContext.getString(R.string.msgs_main_sync_profile_dlg_wifi_option_wifi_connect_specific_address))) {
                        if (n_sti.getSyncOptionWifiIPAddressGrantList().size() == 0) {
                            CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
                            setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_sync_task_dlg_wifi_address_not_specified));
                            error_detected = true;
                        }
                }
                if (!error_detected) {
                    String filter_msg = "";
                    filter_msg = checkFilter(dialog, type, n_sti);
                    if (filter_msg.equals("")) {
                        String s_msg = checkStorageStatus(dialog, type, n_sti);
                        if (s_msg.equals("")) {
                            if (n_sti.isSyncFolderStatusError()) {
                                if (n_sti.getSourceFolderStatusError() != SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR) {
                                    if ((n_sti.getSourceFolderStatusError()&SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME)!=0 ||
                                            (n_sti.getSourceFolderStatusError()&SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD)!=0) {
                                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_import_failed_source_folder_smb_account_password));
                                    }
                                } else if (n_sti.getDestinationFolderStatusError() != SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR) {
                                    if ((n_sti.getDestinationFolderStatusError()&SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_NAME)!=0 ||
                                            (n_sti.getDestinationFolderStatusError()&SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ACCOUNT_PASSWORD)!=0) {
                                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_import_failed_destination_folder_smb_account_password));
                                    } else if ((n_sti.getDestinationFolderStatusError()&SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_ZIP_PASSWORD)!=0) {
                                        setDialogMsg(dlg_msg, mContext.getString(R.string.msgs_task_edit_sync_folder_dlg_import_failed_destination_folder_zip_password));
                                    }
                                }
                            } else {
                                setDialogMsg(dlg_msg, s_msg);
                                if (isSyncTaskChanged(n_sti, mCurrentSyncTaskItem)) CommonDialog.setViewEnabled(getActivity(), btn_ok, true);
                                else CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
                            }
                        } else {
                            setDialogMsg(dlg_msg, s_msg);
                            if (isSyncTaskChanged(n_sti, mCurrentSyncTaskItem)) CommonDialog.setViewEnabled(getActivity(), btn_ok, true);
                            else CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
                        }
                    } else {
                        setDialogMsg(dlg_msg, filter_msg);
                        CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
                    }
                }
            }
        } else {
            setDialogMsg(dlg_msg, t_name_msg);
            CommonDialog.setViewEnabled(getActivity(), btn_ok, false);
        }
    }

    private boolean isSyncTaskChanged(SyncTaskItem curr_stli, SyncTaskItem org_stli) {
        SyncTaskItem new_stli = buildSyncTaskListItem(mDialog, curr_stli);
        String n_type = new_stli.getSyncTaskType();
        String c_type = mCurrentSyncTaskItem.getSyncTaskType();

        boolean result = !new_stli.isSame(org_stli);
        return result;
    }

    private String checkFilter(Dialog dialog, String type, SyncTaskItem n_sti) {
        String result = "";
        final CheckedTextView ctvSyncFileTypeSpecific = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_specific);
        final CheckedTextView ctvSyncFileTypeAudio = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_audio);
        final CheckedTextView ctvSyncFileTypeImage = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_image);
        final CheckedTextView ctvSyncFileTypeVideo = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_file_type_video);
        final CheckedTextView ctvSyncSpecificSubDir = (CheckedTextView) mDialog.findViewById(R.id.sync_filter_sub_directory_specific);

        boolean error_detected = false;

        if (ctvSyncFileTypeSpecific.isChecked()) {
            if (ctvSyncFileTypeAudio.isChecked() || ctvSyncFileTypeImage.isChecked() || ctvSyncFileTypeVideo.isChecked()) {

            } else {
                if (n_sti.getFileNameFilter().size() == 0) {
                    result = mContext.getString(R.string.msgs_task_sync_task_sync_file_type_detail_not_specified);
                    error_detected = true;
                }
            }
        }

        if (!error_detected) {
            if (ctvSyncSpecificSubDir.isChecked()) {
                if (n_sti.getDirectoryFilter().size() == 0) {
                    result = mContext.getString(R.string.msgs_task_sync_task_sync_sub_directory_dir_filter_not_specified);
                } else {
                    int enabled_count=0;
                    for(FilterListAdapter.FilterListItem fli:n_sti.getDirectoryFilter()) {
                        if (fli.isEnabled()) {
                            enabled_count++;
                        }
                    }
                    if (enabled_count==0) {
                        result = mContext.getString(R.string.msgs_task_sync_task_filter_list_enabled_filter_not_exists);
                    }
                }
            }
        }
        return result;
    }

    private String checkStorageStatus(Dialog dialog, String type, SyncTaskItem n_sti) {
        String emsg = "";
        if (n_sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            if (!n_sti.getSourceStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) {
                if (SafManager3.isUuidMounted(mContext, n_sti.getSourceStorageUuid())) {
                    if (!SafManager3.isUuidRegistered(mContext, n_sti.getSourceStorageUuid())) {
                        emsg = mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_auth_please_edit_source);
                    }
                } else {
                    emsg=String.format(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_mounted), n_sti.getSourceStorageUuid());
                }
            }
        }
        if (emsg.equals("") && n_sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            if (!n_sti.getDestinationStorageUuid().equals(SAF_FILE_PRIMARY_UUID)) {
                if (SafManager3.isUuidMounted(mContext, n_sti.getDestinationStorageUuid())) {
                    if (!SafManager3.isUuidRegistered(mContext, n_sti.getDestinationStorageUuid())) {
                        emsg = mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_auth_please_edit_destination);
                    }
                } else {
                    emsg=String.format(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_sdcard_not_mounted), n_sti.getDestinationStorageUuid());
                }
            }
        }
        return emsg;
    }

    private void setWifiApWhiteListInfo(ArrayList<FilterListAdapter.FilterListItem> wpal, Button edit_wifi_ap_list) {
        if (wpal.size() > 0) {
            String ap_list = "", sep = "";
            for (FilterListAdapter.FilterListItem wapl : wpal) {
                ap_list += sep + wapl.getFilter();
                sep = ",";
            }
            edit_wifi_ap_list.setText(ap_list);
        } else {
            edit_wifi_ap_list.setText(mContext.getString(R.string.msgs_filter_list_dlg_not_specified));
        }
    }

    static public String checkDirectoryFilterForSameDirectoryAccess(Context c, SyncTaskItem sti) {
        String msg="";
        String source_dir=sti.getSourceDirectoryName().equals("")?"":sti.getSourceDirectoryName().toLowerCase();
        boolean select_found=false;
        boolean select_specified=false;
        boolean exclude_found=false;
        boolean exclude_specified=false;
        for(FilterListAdapter.FilterListItem item:sti.getDirectoryFilter()) {
            String filter=source_dir.equals("")?item.getFilter():source_dir+"/"+item.getFilter();
            Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp(filter.toLowerCase())+"$");
            if (item.isInclude()) {
                select_specified=true;
                Matcher mt=pattern.matcher(sti.getDestinationDirectoryName());
                if (mt.find()) {
                    select_found=true;
                    break;
                }
            }
        }
        for(FilterListAdapter.FilterListItem item:sti.getDirectoryFilter()) {
            String filter=source_dir.equals("")?item.getFilter():source_dir+"/"+item.getFilter();
            Pattern pattern=Pattern.compile("^"+MiscUtil.convertRegExp(filter.toLowerCase())+"$");
            if (!item.isInclude()) {//Exclude only
                exclude_specified=true;
                Matcher mt=pattern.matcher(sti.getDestinationDirectoryName());
                if (mt.find()) {
                    exclude_found=true;
                    break;
                }
            }
        }
        if (!select_specified && !exclude_specified) {
            msg = c.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_same_dir);
        } else if (select_specified && !exclude_specified) {
            if (select_found) msg = c.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_same_dir);
        } else if (!select_specified && exclude_specified) {
            if (!exclude_found) msg = c.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_same_dir);
        } else if (select_specified && exclude_specified) {
            if (select_found && !exclude_found) msg = c.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_same_dir);
        }
        return msg;
    }

    private String isSameDirectoryAccess(Dialog dialog, SyncTaskItem sti) {
        String msg="";
        if (sti.getSourceDirectoryName().equals("")) {
            if (sti.getDestinationDirectoryName().equals("")) {
                msg=mContext.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_internal);
            } else {
                //Sourceが上位
                msg= checkDirectoryFilterForSameDirectoryAccess(mContext, sti);
            }
        } else {
            if (sti.getDestinationDirectoryName().equals("")) {
                //Valid combination
            } else {
                if (sti.getSourceDirectoryName().toLowerCase().equals(sti.getDestinationDirectoryName().toLowerCase())) {
                    msg=mContext.getString(R.string.msgs_main_sync_profile_dlg_invalid_source_destination_combination_internal);
                } else {
                    if (!sti.getDestinationDirectoryName().toLowerCase().equals(sti.getSourceDirectoryName().toLowerCase())) {
                        //Valid combination
                    } else  if (sti.getDestinationDirectoryName().toLowerCase().startsWith(sti.getSourceDirectoryName().toLowerCase())) {
                        //Sourceが上位
                        msg= checkDirectoryFilterForSameDirectoryAccess(mContext, sti);
                    }
                }
            }
        }
        return msg;
    }

    private String checkSourceDestinationCombination(Dialog dialog, SyncTaskItem sti) {
        String result = "";
        if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
            if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
                if (sti.getSourceStorageUuid().equals(sti.getDestinationStorageUuid())) result= isSameDirectoryAccess(dialog, sti);
            }
        } else if (sti.getSourceFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
            if (sti.getDestinationFolderType().equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                if (sti.getSourceSmbAddr().equalsIgnoreCase(sti.getDestinationSmbAddr()) &&
                        sti.getSourceSmbDomain().equalsIgnoreCase(sti.getDestinationSmbDomain()) &&
                        sti.getSourceSmbHostName().equalsIgnoreCase(sti.getDestinationSmbHostName()) &&
                        sti.getSourceSmbShareName().equalsIgnoreCase(sti.getDestinationSmbShareName())) {
                    result= isSameDirectoryAccess(dialog, sti);
                }
            }
        }
        return result;
    }

    static class SyncFolderEditValue implements Serializable, Cloneable {
        public boolean isChanged=false;
        public String task_type="";
        public String folder_title = "";
        public boolean is_source_folder = false;
        public boolean folder_smb_use_pswd =false;
        public String folder_type = "";
        public String folder_directory = "";
        public String folder_storage_uuid = "";
        public String folder_smb_account = "";
        public String folder_smb_password = "";
        public String folder_smb_domain = "";
        public String folder_smb_addr = "";
        public String folder_smb_hostname = "";
        public String folder_smb_share = "";
        public String folder_smb_port = "";
        public String folder_smb_protocol = "1";
        public boolean folder_smb_ipc_enforced=true;
        public boolean folder_smb_use_smb2_negotiation=false;
        public boolean folder_smb_use_port_number =false;
        public boolean folder_smb_use_account_name_password =false;

        public String zip_comp_level = "";
        public String zip_enc_method = "";
        public String zip_file_name = "";
        public String zip_file_password = "";

        public String archive_file_name_template= TEMPLATE_ORIGINAL_NAME;
        public String archive_retention_period=String.valueOf(ARCHIVE_RETAIN_FOR_A_DEFAULT);
        public String archive_file_name_suffix_digit=String.valueOf(ARCHIVE_SUFFIX_DIGIT_DEFAULT);

        public boolean archive_ignore_source_directory_hiearachy =false;

        public int folder_error_code= SyncTaskItem.SYNC_FOLDER_STATUS_ERROR_NO_ERROR;

        public SyncFolderEditValue(){};

        @Override
        public SyncFolderEditValue clone() {
            SyncFolderEditValue npfli = null;
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

                npfli = (SyncFolderEditValue) ois.readObject();
                ois.close();
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            return npfli;
        }

        public boolean isSame(SyncFolderEditValue comp) {
            boolean result = false;
            if (folder_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_LOCAL)) {
                if (folder_type.equals(comp.folder_type) &&
                    isChanged==comp.isChanged &&
                    folder_directory.equals(comp.folder_directory) &&
                    folder_storage_uuid.equals(comp.folder_storage_uuid)
                    ) {
                    if (task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                        if (archive_file_name_suffix_digit.equals(comp.archive_file_name_suffix_digit) &&
                                archive_retention_period.equals(comp.archive_retention_period) &&
                                archive_file_name_template.equals(comp.archive_file_name_template) &&
                                archive_ignore_source_directory_hiearachy==comp.archive_ignore_source_directory_hiearachy)
                            result=true;
                    } else {
                        result = true;
                    }
                }
            } else if (folder_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_SMB)) {
                if (folder_type.equals(comp.folder_type) &&
                        isChanged==comp.isChanged &&
                        folder_directory.equals(comp.folder_directory) &&
                        folder_smb_account.equals(comp.folder_smb_account) &&
                        folder_smb_password.equals(comp.folder_smb_password) &&
                        folder_smb_domain.equals(comp.folder_smb_domain) &&
                        folder_smb_addr.equals(comp.folder_smb_addr) &&
                        folder_smb_hostname.equals(comp.folder_smb_hostname) &&
                        folder_smb_share.equals(comp.folder_smb_share) &&
                        folder_smb_port.equals(comp.folder_smb_port) &&
                        folder_smb_protocol.equals(comp.folder_smb_protocol) &&
                        (folder_smb_use_pswd == comp.folder_smb_use_pswd)  &&
                        (folder_smb_ipc_enforced==comp.folder_smb_ipc_enforced) &&
                        (folder_smb_use_smb2_negotiation==comp.folder_smb_use_smb2_negotiation)
                    ) {
                    if (task_type.equals(SyncTaskItem.SYNC_TASK_TYPE_ARCHIVE)) {
                        if (archive_file_name_suffix_digit.equals(comp.archive_file_name_suffix_digit) &&
                                archive_retention_period.equals(comp.archive_retention_period) &&
                                archive_file_name_template.equals(comp.archive_file_name_template) &&
                                archive_ignore_source_directory_hiearachy==comp.archive_ignore_source_directory_hiearachy
                        )
                            result=true;
                    } else {
                        result = true;
                    }
                }
            } else if (folder_type.equals(SyncTaskItem.SYNC_FOLDER_TYPE_ZIP)) {
                if (folder_type.equals(comp.folder_type) &&
                        isChanged==comp.isChanged &&
                        folder_directory.equals(comp.folder_directory) &&
                        folder_storage_uuid.equals(comp.folder_storage_uuid) &&
                        zip_comp_level.equals(comp.zip_comp_level) &&
                        zip_enc_method.equals(comp.zip_enc_method) &&
                        zip_file_name.equals(comp.zip_file_name) &&
                        zip_file_password.equals(comp.zip_file_password)) result = true;
            }
            return result;
        }
    }
}
