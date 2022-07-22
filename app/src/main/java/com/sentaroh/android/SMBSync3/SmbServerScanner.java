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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.SMB_STATUS_ACCESS_DENIED;
import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.SMB_STATUS_INVALID_LOGON_TYPE;
import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.SMB_STATUS_UNKNOWN_ACCOUNT;
import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.SMB_STATUS_UNSUCCESSFULL;
import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.SMB_STATUS_UNTESTED_LOGIN;

//import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputLayout;
import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.DialogBackKeyListener;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThreadCtrl;
import com.sentaroh.android.Utilities3.Widget.CustomSpinnerAdapter;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;

public class SmbServerScanner {

    private ActivityMain mActivity=null;
    private CommonUtilities mUtil=null;
    private GlobalParameters mGp=null;

    // - scanner_smb_protocol: used to set the default smb protocol for the scanner based on an eventual configured server
    //   + sets library to get hostname (usually, SMB1 and SMB2 libs can detect SMB1/2/3 host names)
    //   + detects if SMB host is compatible with SMBv1, SMBv2/3 or both
    //   + leave empty to detect both SMB v1 and v2/3 compatible hosts
    // - port_number: ports to scan. If left empty, will scan both default 445 and 139 ports
    public SmbServerScanner(ActivityMain a, GlobalParameters gp, CommonUtilities cu, final NotifyEvent p_ntfy,
                            final String port_number, final String scanner_smb_protocol, boolean scan_start) {
        mActivity=a;
        mUtil=cu;
        mGp=gp;
        initDialog(p_ntfy, port_number, scanner_smb_protocol, scan_start);
    }

    private SmbServerScanAdapter mAdapter = null;
    private final static String SYNC_FOLDER_SMB_PROTOCOL_SMB123 = SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1 + " & " + SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
    private void initDialog(final NotifyEvent p_ntfy, final String port_number, final String scanner_smb_protocol, boolean scan_start) {
        if (!SyncThread.isWifiOn(mActivity)) {
            mUtil.showCommonDialog(false, "W", mActivity.getString(R.string.msgs_scan_ip_address_select_title),
                    mActivity.getString(R.string.msgs_scan_not_started_because_wifi_off), null);
            return;
        }

        //カスタムダイアログの生成
        final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.scan_smb_server_scan_dlg);

        //final ConstraintLayout ll_dlg_view = (ConstraintLayout) dialog.findViewById(R.id.scan_remote_ntwk_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = dialog.findViewById(R.id.scan_smb_server_scan_dlg_title_view);
        final TextView title = dialog.findViewById(R.id.scan_smb_server_scan_dlg_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final ScrollView scan_settings_scroll_view = dialog.findViewById(R.id.scan_smb_server_scan_dlg_settings_scroll_view);
        scan_settings_scroll_view.setScrollBarFadeDuration(0);
        scan_settings_scroll_view.setScrollbarFadingEnabled(false);

        final Button btn_scan = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        final TextView tvmsg = dialog.findViewById(R.id.scan_smb_server_scan_dlg_msg);
        //final TextView tv_result = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_result_title);
        tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_press_scan_btn));
        //tv_result.setVisibility(TextView.GONE);

        final String from = CommonUtilities.getIfIpAddress(mUtil).equals("")?"192.168.0.1":CommonUtilities.getIfIpAddress(mUtil);
        String subnet = from.substring(0, from.lastIndexOf("."));
        String subnet_o1, subnet_o2, subnet_o3;
        subnet_o1 = subnet.substring(0, subnet.indexOf("."));
        subnet_o2 = subnet.substring(subnet.indexOf(".") + 1, subnet.lastIndexOf("."));
        subnet_o3 = subnet.substring(subnet.lastIndexOf(".") + 1);
        final EditText baEt1 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o1);
        final EditText baEt2 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o2);
        final EditText baEt3 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o3);
        final EditText baEt4 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o4);
        final EditText eaEt5 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_end_address_o4);
        baEt1.setText(subnet_o1);
        baEt2.setText(subnet_o2);
        baEt3.setText(subnet_o3);
        baEt4.setText("1");
        baEt4.setSelection(1);
        eaEt5.setText("254");
        baEt4.requestFocus();

        final Spinner dlg_scan_smb_protocol_spinner = dialog.findViewById(R.id.scan_smb_scanner_dlg_smb_protocol_spinner);
        dlg_scan_smb_protocol_spinner.setOnItemSelectedListener(null);
        CommonUtilities.setSpinnerBackground(mActivity, dlg_scan_smb_protocol_spinner, mGp.isScreenThemeIsLight());
        final CustomSpinnerAdapter smb_level_spinner_adapter = new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        //smb_level_spinner_adapter.setDropDownTextWordwrapEnabled(true);
        smb_level_spinner_adapter.setSpinner(dlg_scan_smb_protocol_spinner);
        smb_level_spinner_adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        dlg_scan_smb_protocol_spinner.setPrompt(mActivity.getString(R.string.msgs_scan_dlg_smb_level_spinner_dlg_title));
        dlg_scan_smb_protocol_spinner.setAdapter(smb_level_spinner_adapter);

        smb_level_spinner_adapter.add(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1); // 0
        smb_level_spinner_adapter.add(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23); // 1
        smb_level_spinner_adapter.add(SYNC_FOLDER_SMB_PROTOCOL_SMB123); // 2

        if (scanner_smb_protocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) dlg_scan_smb_protocol_spinner.setSelection(0);
        else if (scanner_smb_protocol.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23)) dlg_scan_smb_protocol_spinner.setSelection(1);
        else dlg_scan_smb_protocol_spinner.setSelection(2);

        final EditText et_port_number = dialog.findViewById(R.id.scan_smb_server_scan_dlg_port_number);
        if (port_number.isEmpty() || TextUtils.isDigitsOnly(port_number)) {
            et_port_number.setText(port_number);
        } else {
            et_port_number.setText("");
        }
/*
        et_port_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
*/
        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        final NotifyEvent ntfy_lv_click = new NotifyEvent(mActivity);
        ntfy_lv_click.setListener(new NotifyEvent.NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                dialog.dismiss();
                buildSmbServerParmsDlg((SmbServerScanResult)o[0], p_ntfy);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });

        baEt1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CommonUtilities.setViewEnabled(mActivity, btn_scan, isValidScanAddress(dialog));
            }
        });

        baEt2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CommonUtilities.setViewEnabled(mActivity, btn_scan, isValidScanAddress(dialog));
            }
        });

        baEt3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CommonUtilities.setViewEnabled(mActivity, btn_scan, isValidScanAddress(dialog));
            }
        });

        baEt4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CommonUtilities.setViewEnabled(mActivity, btn_scan, isValidScanAddress(dialog));
            }
        });

        eaEt5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                CommonUtilities.setViewEnabled(mActivity, btn_scan, isValidScanAddress(dialog));
            }
        });

        CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
        tvmsg.setText("");

        final ListView lv = dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_result_list);
        lv.setScrollingCacheEnabled(false);
        //lv.setScrollbarFadingEnabled(false);
        lv.setScrollBarFadeDuration(0);
        lv.setEnabled(false);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialog.dismiss();
                buildSmbServerParmsDlg(mScanResultList.get(i), p_ntfy);
            }
        });

        //SCANボタンの指定
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tvmsg.setText("");
                mScanResultList.clear();

                // On next scan start, ensure we cannot click search results while scan is running, thus dismissing the dialog
                // The scan thread would then continue to run in the background until it is completed
                lv.setEnabled(false);

                NotifyEvent ntfy = new NotifyEvent(mActivity);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        //mAdapter.notifyDataSetChanged();
                        //mAdapter.sort();
                        lv.setEnabled(true);
                        if (!mScanSmbErrorMessage.equals("")) {
                            tvmsg.setText(mScanSmbErrorMessage);
                        } else {
                            if (mScanResultList.size() < 1) {
                                tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_not_detected));
                                //tv_result.setVisibility(TextView.GONE);
                            } else {
                                tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_select_detected_host));
                                //tv_result.setVisibility(TextView.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }

                });
                //tv_result.setVisibility(TextView.GONE);
                String ba1 = baEt1.getText().toString();
                String ba2 = baEt2.getText().toString();
                String ba3 = baEt3.getText().toString();
                String ba4 = baEt4.getText().toString();
                String ea5 = eaEt5.getText().toString();
                String subnet = ba1 + "." + ba2 + "." + ba3;
                int begin_addr = Integer.parseInt(ba4);
                int end_addr = Integer.parseInt(ea5);

                String scan_smb_level = getSmbSelectedProtocol(dlg_scan_smb_protocol_spinner);
                boolean scan_smbv1 = false;
                boolean scan_smbv23 = false;
                if (scan_smb_level.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                    scan_smbv1 = true;
                } else if (scan_smb_level.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23)) {
                    scan_smbv23 = true;
                } else {
                    // default scan for both protocols (not used currently during refresh shares, only radio buttons for SMBv1 and SMBv2/3 are provided)
                    scan_smbv1 = true;
                    scan_smbv23 = true;
                }

                mAdapter = new SmbServerScanAdapter(mActivity, R.layout.scan_address_result_list_item, mScanResultList, ntfy_lv_click);
                lv.setAdapter(mAdapter);

                performSmbServerScan(dialog, lv, subnet, begin_addr, end_addr, scan_smbv1, scan_smbv23, ntfy);
            }
        });

        //CANCELボタンの指定
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });

        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btn_cancel.performClick();
            }
        });
        dialog.show();

        if (scan_start) btn_scan.performClick();
    }

    // called by initDialog() on server selected from scan results list view
    private void buildSmbServerParmsDlg(SmbServerScanResult scan_result, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.scan_smb_server_parm_dlg);

        final LinearLayout ll_title= dialog.findViewById(R.id.scan_smb_server_parm_dlg_title_view);
        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView tv_title= dialog.findViewById(R.id.scan_smb_server_parm_dlg_title);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        final TextView dlg_msg= dialog.findViewById(R.id.scan_smb_server_parm_dlg_msg);

        final ScrollView server_settings_scroll_view = dialog.findViewById(R.id.scan_smb_server_param_dlg_settings_scroll_view);
        server_settings_scroll_view.setScrollBarFadeDuration(0);
        server_settings_scroll_view.setScrollbarFadingEnabled(false);

        final RadioGroup dlg_smb_host_rg= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_rg);
        final RadioButton dlg_use_ip_addr= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_address);
        final RadioButton dlg_use_host_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_hostname);
        final TextView dlg_smb_host_selcted= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_selected);
        final RadioButton dlg_use_smb1= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb1);
        final RadioButton dlg_use_smb23= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb23);
        final EditText dlg_smb_port_number= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_port_number);
        final EditText dlg_smb_account_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_name);
        final EditText dlg_smb_account_password= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password);
        //final LinearLayout ll_dlg_smb_share_name=(LinearLayout) dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_view);

        final ListView dlg_smb_share_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_name);
        //dlg_smb_share_name.setScrollbarFadingEnabled(false);
        dlg_smb_share_name.setScrollBarFadeDuration(0);
        
        final ArrayList<String> share_name_list=new ArrayList<String>();
        final ArrayAdapter<String>share_list_adapter=new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_single_choice, share_name_list);
        dlg_smb_share_name.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dlg_smb_share_name.setAdapter(share_list_adapter);

        final Button btn_refresh= dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_refresh_share_list);

        final Button btn_ok= dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_ok);
        CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
        final Button btn_cancel= dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_cancel);

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        dlg_use_ip_addr.setChecked(true);
        if (scan_result.server_smb_name.equals("")) {
            CommonUtilities.setViewEnabled(mActivity, dlg_use_host_name, false);
        }
        dlg_smb_host_selcted.setText(scan_result.server_smb_ip_addr);
        dlg_smb_host_rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (dlg_use_ip_addr.getId()==i) dlg_smb_host_selcted.setText(scan_result.server_smb_ip_addr);
                else dlg_smb_host_selcted.setText(scan_result.server_smb_name);
            }
        });

        // After scan result, enable the SMB Level radio button only for the available protocols found during scan
        // Normally, if no SMB level protocol was found, the server cannot be selected and we should'nt have a situation
        // where both SMB level radio buttons are unchecked and disabled. However, we account for this situation
        dlg_use_smb23.setChecked(scan_result.smb23_available);
        if (!scan_result.smb23_available) dlg_use_smb1.setChecked(scan_result.smb1_available);

        buildShareListSelectorView(dialog, scan_result, share_list_adapter);
        CommonUtilities.setViewEnabled(mActivity, btn_ok, false);

        dlg_smb_share_name.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dlg_msg.setText("");
                CommonUtilities.setViewEnabled(mActivity, btn_ok, true);
            }
        });

        dlg_use_smb1.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dlg_use_smb23.setChecked(!b);
                scan_result.share_item_list.clear();
                scan_result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                scan_result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        dlg_use_smb23.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dlg_use_smb1.setChecked(!b);
                scan_result.share_item_list.clear();
                scan_result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                scan_result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        dlg_smb_port_number.setText(scan_result.server_smb_port_number);
        dlg_smb_port_number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                scan_result.share_item_list.clear();
                scan_result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                scan_result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        dlg_smb_account_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                scan_result.share_item_list.clear();
                scan_result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                scan_result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        final ThreadCtrl tc = new ThreadCtrl();
        dlg_smb_account_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!tc.isEnabled()) {
                    scan_result.share_item_list.clear();
                    scan_result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                    scan_result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                    buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                    CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
                }
            }
        });

        final TextInputLayout til_smb_account_password = dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password_view);
        til_smb_account_password.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tc.setEnabled(); // do not trigger dlg_smb_account_password.addTextChangedListener.afterTextChanged()
                tc.setThreadResultSuccess();
                if (dlg_smb_account_password.getTransformationMethod()!=null) {
                    dlg_smb_account_password.setTransformationMethod(null);
                } else {
                    dlg_smb_account_password.setTransformationMethod(new PasswordTransformationMethod());
                }
                tc.setDisabled();
            }
        });

        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NotifyEvent ntfy=new NotifyEvent(mActivity);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] arg1) {
                        SmbServerScanResult result = (SmbServerScanResult) arg1[0];
                        scan_result.smb1_nt_status_desc=result.smb1_nt_status_desc;
                        scan_result.smb1_available=result.smb1_available;
                        scan_result.smb23_nt_status_desc=result.smb23_nt_status_desc;
                        scan_result.smb23_available=result.smb23_available;
                        // Port num did not change on return result
                        //scan_result.server_smb_port_number=result.server_smb_port_number;
                        //dlg_smb_port_number.setText(scan_result.server_smb_port_number);
                        scan_result.share_item_list.clear();
                        scan_result.share_item_list.addAll(result.share_item_list);

                        buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                        CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {

                    }
                });
                final String acct=dlg_smb_account_name.getText().toString().equals("")?null:dlg_smb_account_name.getText().toString();
                final String pswd=dlg_smb_account_password.getText().toString().equals("")?null:dlg_smb_account_password.getText().toString();
                final String port=dlg_smb_port_number.getText().toString();
                final String smb_level=dlg_use_smb1.isChecked()? SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1:SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
                createSmbServerShareInfo(ntfy, smb_level, null, acct, pswd, scan_result.server_smb_ip_addr, scan_result.server_smb_name, port);
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String smb_host=dlg_use_ip_addr.isChecked()?scan_result.server_smb_ip_addr:scan_result.server_smb_name;
                String smb_level=dlg_use_smb1.isChecked()? SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1:SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
                String smb_portnum=dlg_smb_port_number.getText().toString();
                String smb_acct_name=dlg_smb_account_name.getText().toString();
                String smb_acct_pswd=dlg_smb_account_password.getText().toString();
                String smb_share_name=share_name_list.get(0);

                SparseBooleanArray checked = dlg_smb_share_name.getCheckedItemPositions();
                for (int i = 0; i <= share_name_list.size(); i++) {
                    if (checked.get(i)) {
                        smb_share_name=share_name_list.get(i);
                        break;
                    }
                }

                p_ntfy.notifyToListener(true, new Object[]{new String[]{smb_host, smb_level, smb_portnum, smb_acct_name, smb_acct_pswd, smb_share_name}});
                dialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                p_ntfy.notifyToListener(false, null);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

//    private void scanSmbServerEnableSmbScanSelectorOkButton(Dialog dialog) {
//        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_msg);
//        final Button btn_ok=(Button)dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_ok);
//
//        if (dlg_msg.getText().toString().length()>0) CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
//        else CommonUtilities.setViewEnabled(mActivity, btn_ok, true);
//    }

    private void buildShareListSelectorView(Dialog dialog, SmbServerScanResult scan_result, ArrayAdapter<String> adapter) {
        final TextView dlg_msg= dialog.findViewById(R.id.scan_smb_server_parm_dlg_msg);
        final ScrollView server_settings_scroll_view = dialog.findViewById(R.id.scan_smb_server_param_dlg_settings_scroll_view);
        final RadioButton dlg_use_ip_addr= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_address);
        final RadioButton dlg_use_host_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_hostname);
        final RadioButton dlg_use_smb1= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb1);
        final RadioButton dlg_use_smb23= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb23);
        final EditText dlg_smb_port_number= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_port_number);
        final EditText dlg_smb_account_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_name);
        final EditText dlg_smb_account_password= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password);
        final TextInputLayout til_smb_account_password = dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password_view);
        final Button btn_refresh= dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_refresh_share_list);
        final LinearLayout ll_dlg_smb_share_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_view);
        final ListView dlg_smb_share_name= dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_name);
        final Button btn_ok= dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_ok);

        String nt_status_desc = "";
        String smb_level = "";
        CommonUtilities.setViewEnabled(mActivity, dlg_use_smb1, scan_result.smb1_available);
        CommonUtilities.setViewEnabled(mActivity, dlg_use_smb23, scan_result.smb23_available);
        if (dlg_use_smb1.isChecked()) {
            nt_status_desc = scan_result.smb1_nt_status_desc;
            smb_level = SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1;
        } else if (dlg_use_smb23.isChecked()) {
            nt_status_desc = scan_result.smb23_nt_status_desc;
            smb_level = SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
        } else {
            // Normally, if no SMB level protocol was found, the server cannot be selected and we should'nt have a situation
            // where both SMB level radio buttons are unchecked and disabled. However, we account for this situation
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_no_smb_level_error));

            //server_settings_scroll_view.setVisibility(ScrollView.GONE);
            CommonUtilities.setViewEnabled(mActivity, server_settings_scroll_view, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_use_ip_addr, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_use_host_name, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_use_smb1, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_use_smb23, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_smb_port_number, false);
            CommonUtilities.setViewEnabled(mActivity, dlg_smb_account_name, false);
            //CommonUtilities.setViewEnabled(mActivity, dlg_smb_account_password, false);
            CommonUtilities.setViewEnabled(mActivity, til_smb_account_password, false);
            CommonUtilities.setViewEnabled(mActivity, btn_refresh, false);

            ll_dlg_smb_share_name.setVisibility(LinearLayout.GONE);

            CommonUtilities.setViewEnabled(mActivity, btn_ok, false);

            mUtil.addDebugMsg(1, "E", "buildShareListSelectorView Exception: No SMB level enabled. address="+scan_result.server_smb_ip_addr + " , port="+scan_result.server_smb_port_number +
                    " , smb1_nt_status_desc="+scan_result.smb1_nt_status_desc + " , smb23_nt_status_desc"+scan_result.smb23_nt_status_desc);
            return;
        }

        mUtil.addDebugMsg(1, "I", "buildShareListSelectorView: smb_level="+smb_level + " , nt_status_desc="+nt_status_desc);

        adapter.clear();
        dlg_smb_share_name.setAdapter(null);
        for(SmbServerScanShareInfo item:scan_result.share_item_list) {
            if (item.smb_level.equals(smb_level)) {
                adapter.add(item.share_name);
            }
        }

        dlg_smb_share_name.setAdapter(adapter);
        adapter.notifyDataSetChanged();
//        scanSmbServerEnableSmbScanSelectorOkButton(dialog);

        if (adapter.getCount() > 0) ll_dlg_smb_share_name.setVisibility(LinearLayout.VISIBLE);
        else ll_dlg_smb_share_name.setVisibility(LinearLayout.GONE);

        String acct=dlg_smb_account_name.getText().toString();
        String pswd=dlg_smb_account_password.getText().toString();
        if (nt_status_desc.equals(SMB_STATUS_ACCESS_DENIED)) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_correct_account));
        } else if (nt_status_desc.equals(SMB_STATUS_INVALID_LOGON_TYPE)) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_enable_account));
        } else if (nt_status_desc.equals(SMB_STATUS_UNKNOWN_ACCOUNT)) {
            if (acct.equals("") || pswd.equals("")) {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_account_password_not_specified));
            } else {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_correct_account_password));
            }
        } else if (nt_status_desc.equals(SMB_STATUS_UNTESTED_LOGIN)) {
            // During scan for servers, no credentials are provided
            // Also, on edit selected server settings
            if (acct.equals("") || pswd.equals("")) {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_account_password_empty));
            } else {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_refresh_shares));
            }
        } else if (nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
            // on refresh shares, server cannot be reached because server params (port, SMB level) were edited in the dialog and differ now from what scan result returned
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_server_connection_error));
        } else if (adapter.getCount() <= 0) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_shares_list_empty));
        } else {
            dlg_msg.setText("");
        }

        if (dlg_msg.getText().length()==0 && adapter.getCount() > 0) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_select_smb_share_name));
        }
    }

    private int mScanCompleteCount = 0, mScanAddrCount = 0;
    // private int mStartedThreadsCount = 0, mTh2ResultEntered = 0, mTh2ResultOut = 0 //debug scanner multithreading
    private final ArrayList<String> mScanRequestedAddrList = new ArrayList<String>();
    private final ArrayList<SmbServerScanResult> mScanResultList = new ArrayList<SmbServerScanResult>(); // UI adapter
    private final ArrayList<SmbServerScanResult> mThreadScanResultList = new ArrayList<SmbServerScanResult>(); // non UI network scan threads
    private static final Object mLockThreadsScanResultList = new Object(); //for mThreadScanResultList
    private static final Object mLockScanProgress = new Object(); //for mScanRequestedAddrList and mScanCompleteCount
    private static final Object mLockSmbServerScanAdapter = new Object(); //mScanResultList and mAdapter

    private void performSmbServerScan(
            final Dialog dialog,
            final ListView lv_ipaddr,
//            final ArrayList<SmbServerScanAdapter.NetworkScanListItem> ipAddressList,
            final String subnet, final int begin_addr, final int end_addr,
            final boolean scan_smbv1, final boolean scan_smbv23, final NotifyEvent p_ntfy) {
        final Handler handler = new Handler();
        final ThreadCtrl tc = new ThreadCtrl();
        final LinearLayout ll_addr = dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_address);
        final LinearLayout ll_prog = dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress);
        final TextView tvmsg = dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress_msg);
        final Button btn_scan = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        final Button scan_cancel = dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress_cancel);

//        final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.scan_remote_ntwk_ctv_use_port);
        final EditText et_port_number = dialog.findViewById(R.id.scan_smb_server_scan_dlg_port_number);
        final String scan_port = et_port_number.getText().toString();

        tvmsg.setText("");
        scan_cancel.setText(R.string.msgs_scan_progress_spin_dlg_addr_cancel);
        ll_addr.setVisibility(LinearLayout.GONE);
        ll_prog.setVisibility(LinearLayout.VISIBLE);
        btn_scan.setVisibility(Button.GONE);
        btn_cancel.setVisibility(Button.GONE);
        mAdapter.setButtonEnabled(false);
        CommonUtilities.setViewEnabled(mActivity, scan_cancel, true);
        dialog.setOnKeyListener(new DialogBackKeyListener(mActivity));
        dialog.setCancelable(false);

        mScanResultList.clear();
        mThreadScanResultList.clear();
        // CANCELボタンの指定
        scan_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scan_cancel.setText(mActivity.getString(R.string.msgs_progress_dlg_canceling));
                CommonUtilities.setViewEnabled(mActivity, scan_cancel, false);
                mUtil.addDebugMsg(1, "W", "IP Address list creation was cancelled");
                tc.setDisabled();
            }
        });
        dialog.show();

        mUtil.addDebugMsg(1, "I", "Scan IP address ransge is " + subnet + "." + begin_addr + " - " + end_addr);

        mScanRequestedAddrList.clear();

        final String scan_prog = mActivity.getString(R.string.msgs_ip_address_scan_progress);
        String p_txt = String.format(scan_prog, 0);
        tvmsg.setText(p_txt);

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {//non UI thread
                //mStartedThreadsCount = 0;
                //mTh2ResultEntered = 0;
                //mTh2ResultOut = 0;
                mScanCompleteCount = 0;
                mScanAddrCount = end_addr - begin_addr + 1;
                int scan_threads = 100;
                for (int i = begin_addr; i <= end_addr; i += scan_threads) {
                    if (!tc.isEnabled()) break;
                    boolean scan_end = false;
                    for (int j = i; j < (i + scan_threads); j++) {
                        if (!tc.isEnabled()) break;
                        if (j <= end_addr) {
                            startSmbServerScanThread(handler, tc, lv_ipaddr, tvmsg, subnet + "." + j, scan_port, scan_smbv1, scan_smbv23);
                        } else {
                            scan_end = true;
                            break;
                        }
                    }
                    if (!scan_end) {
                        for (int wc = 0; wc < 210; wc++) {
                            if (!tc.isEnabled()) break;
                            synchronized (mLockScanProgress) {
                                if (mScanRequestedAddrList.size() == 0) break;
                            }
                            SystemClock.sleep(30); //msecs
                        }
                    } else {
                        break;
                    }
                }

                // Wait for all threads to complete, up to max of 100 seconds
                for (int i = 0; i < 1000; i++) {
                    SystemClock.sleep(100);
                    synchronized (mLockScanProgress) {
                        if (mScanRequestedAddrList.size() == 0) break;
                    }
                }
                handler.post(new Runnable() {//UI thread, all scan threads completed
                    @Override
                    public void run() {
                        synchronized (mLockSmbServerScanAdapter) {
                            mAdapter.notifyDataSetChanged();
                            mAdapter.sort();
                            //lv_ipaddr.setSelection(lv_ipaddr.getCount());
                            closeSmbServerScanProgressDlg(dialog, p_ntfy);
                        }
                    }
                });
            }
        });
        th.setPriority(Thread.MIN_PRIORITY);
        th.start();
    }

    private void closeSmbServerScanProgressDlg(final Dialog dialog, final NotifyEvent p_ntfy) {
        final LinearLayout ll_addr = dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_address);
        final LinearLayout ll_prog = dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress);
        final Button btn_scan = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        ll_addr.setVisibility(LinearLayout.VISIBLE);
        ll_prog.setVisibility(LinearLayout.GONE);
        btn_scan.setVisibility(Button.VISIBLE);
        btn_cancel.setVisibility(Button.VISIBLE);
        mAdapter.setButtonEnabled(true);
        dialog.setOnKeyListener(null);
        dialog.setCancelable(true);
        if (p_ntfy != null) p_ntfy.notifyToListener(true, null);
    }

    private String mScanSmbErrorMessage="";
    private void startSmbServerScanThread(final Handler handler,
                                          final ThreadCtrl tc,
                                          final ListView lv_ipaddr,
                                          final TextView tvmsg,
                                          final String addr,
                                          final String scan_port,
                                          final boolean scan_smbv1,
                                          final boolean scan_smbv23) {
        final String scan_prog = mActivity.getString(R.string.msgs_ip_address_scan_progress);
        if (!tc.isEnabled()) return;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {//non UI thread
                //mStartedThreadsCount++;
                if (!tc.isEnabled()) return;
//                byte[] oo=new byte[Integer.MAX_VALUE];
                synchronized (mLockScanProgress) {
                    mScanRequestedAddrList.add(addr);
                }

                boolean found = false;
                int i = -1;
                String[] ports = { "445", "139" };
                if (scan_port != null && !scan_port.equals("")) {
                    ports = new String[] { scan_port };
                }

                for (String port : ports) {
                    i++;
                    found = CommonUtilities.isSmbHost(mUtil, addr, port, 3500);
                    if (found) break;
                }

                final SmbServerInfo ssi = new SmbServerInfo();
                //ssi.serverHostName = addr;
                //ssi.serverProtocol = scan_smbv1 ? SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1 : SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
                ssi.serverPort = ports[i];

                //if (true) {//debug to add all scanned servers
                if (found) {
                    // start a new non UI background thread to scan for SMBv1 and SMBv2/3 protocols on the found server
                    Thread th2 = new Thread(new Runnable() {
                        @Override
                        public void run() {//non UI thread
                            final SmbServerScanResult result = createSmbServerInfo(tc, scan_smbv1, scan_smbv23, null, null, null, addr, ssi.serverPort);
                            //final SmbServerScanResult result = createSmbServerInfo(tc, false, false, null, null, null, addr, port); //debug to add all scanned servers
                            //mTh2ResultOut++;

                            synchronized (mLockThreadsScanResultList) {
                                if (result != null) mThreadScanResultList.add(result);
                            }
                            handler.post(new Runnable() {//UI thread, createSmbServerInfo() thread completed, update the results ListView
                                @Override
                                public void run() {
                                    synchronized (mLockSmbServerScanAdapter) {
                                        synchronized (mLockThreadsScanResultList) {
                                            mScanResultList.clear(); // hacky way to not use AsyncTask. It also makes the ListView properly expand to bottom of screen if enough elements
                                            mScanResultList.addAll(mThreadScanResultList);
                                        }

                                        //mAdapter.sort(); // sorts the results dynamically. However, we won't see the last current added server as the list gets sorted each time
                                        mAdapter.notifyDataSetChanged();
                                        lv_ipaddr.setSelection(lv_ipaddr.getCount()); // ensure the selection/listView is set to the last added server
                                    }
                                    synchronized (mLockScanProgress) {
                                        mScanCompleteCount++;
                                        mScanRequestedAddrList.remove(addr);

                                        //String p_txt = String.format(scan_prog+" mStarted="+mStartedThreadsCount +" Entered="+mTh2ResultEntered + " Out"+mTh2ResultOut, (mScanCompleteCount * 100) / mScanAddrCount);
                                        String p_txt = String.format(scan_prog, (mScanCompleteCount * 100) / mScanAddrCount);
                                        tvmsg.setText(p_txt);
                                    }
                                }
                            });
                        }
                    });
                    th2.setPriority(Thread.MIN_PRIORITY);
                    th2.start();
                } else {
                    // server not found, update progress
                    handler.post(new Runnable() {// UI thread
                        @Override
                        public void run() {
                            //mTh2ResultOut++;
                            synchronized (mLockScanProgress) {
                                mScanCompleteCount++;
                                mScanRequestedAddrList.remove(addr);

                                //String p_txt = String.format(scan_prog+" mStarted="+mStartedThreadsCount +" Entered="+mTh2ResultEntered + " Out"+mTh2ResultOut, (mScanCompleteCount * 100) / mScanAddrCount);
                                String p_txt = String.format(scan_prog, (mScanCompleteCount * 100) / mScanAddrCount);
                                tvmsg.setText(p_txt);
                            }
                        }
                    });
                }
            }
        });
        th.setPriority(Thread.MIN_PRIORITY);
        th.start();
    }

    // must be called from non UI background thread
    private SmbServerScanResult createSmbServerInfo(final ThreadCtrl tc, boolean scan_smbv1, boolean scan_smbv23, String domain, String user, String pass, String address, String port) {
        //mTh2ResultEntered++;
        if (tc != null && !tc.isEnabled()) {
            //p_ntfy.notifyToListener(false, null);
            return null;
        }
        SmbServerScanResult result = new SmbServerScanResult();
        result.server_smb_ip_addr = address==null? "":address;
        result.server_smb_port_number = port==null? "":port;

        String srv_name = null;
        if (scan_smbv1) {
            // Try to get server hostname by IP/addres
            srv_name = CommonUtilities.getSmbHostName(mUtil, SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1, result.server_smb_ip_addr);
            result.server_smb_name = srv_name==null? "":srv_name;
            try {
                result.scan_for_smb1 = true;
                JcifsAuth auth = new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, domain, user, pass);
                result.smb1_nt_status_desc = isSmbServerAvailable(auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                if (!result.smb1_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                    result.smb1_available = true;
                    result.smb1_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                } else {
                    result.smb1_available = false;
                }
            } catch(JcifsException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "I", "JcifsException occured, error=" + e.getMessage());
                mScanSmbErrorMessage=mActivity.getString(R.string.msgs_scan_smb_server_scan_dlg_scan_error);
            }
        }

        if (scan_smbv23) {
            if (srv_name == null || srv_name.equals("")) {
                srv_name = CommonUtilities.getSmbHostName(mUtil, SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23, result.server_smb_ip_addr);
                result.server_smb_name = srv_name==null? "":srv_name;
            }
            try {
                result.scan_for_smb23 = true;
                Properties prop = new Properties();
                prop.setProperty("jcifs.smb.client.responseTimeout", mGp.settingsSmbClientResponseTimeout);
                JcifsAuth auth = new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, domain, user, pass, JcifsAuth.SMB_CLIENT_MIN_VERSION, JcifsAuth.SMB_CLIENT_MAX_VERSION, prop);
                result.smb23_nt_status_desc = isSmbServerAvailable(auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                if (!result.smb23_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                    result.smb23_available = true;
                    result.smb23_nt_status_desc = SMB_STATUS_UNTESTED_LOGIN;
                } else {
                    result.smb23_available = false;
                }
            } catch(JcifsException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "I", "JcifsException occured, error=" + e.getMessage());
                mScanSmbErrorMessage=mActivity.getString(R.string.msgs_scan_smb_server_scan_dlg_scan_error);
            }
        }
        return result;
    }

    private void createSmbServerShareInfo(final NotifyEvent p_ntfy, String smb_level, String domain, String user, String pass, String address, String srv_name, String port) {
        SmbServerScanResult result = new SmbServerScanResult();
        result.server_smb_ip_addr = address==null? "":address;
        result.server_smb_name = srv_name==null? "":srv_name;
        result.server_smb_port_number = port==null? "":port;

        final Handler hndl=new Handler();
        final Dialog dialog=CommonDialog.showProgressSpinIndicator(mActivity);
        dialog.show();
        Thread th=new Thread(){
            @Override
            public void run() {
                boolean scan_smbv1 = false;
                boolean scan_smbv23 = false;
                if (smb_level.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1)) {
                    scan_smbv1 = true;
                } else if (smb_level.equals(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23)) {
                    scan_smbv23 = true;
                } else {
                    // default scan for both protocols (not used currently during refresh shares, only radio buttons for SMBv1 and SMBv2/3 are provided)
                    scan_smbv1 = true;
                    scan_smbv23 = true;
                }

                if (scan_smbv1) {
                    try {
                        JcifsAuth auth = new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, domain, user, pass);
                        result.smb1_nt_status_desc = isSmbServerAvailable(auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                        if (!result.smb1_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                            result.smb1_available = true;
                            ArrayList<SmbServerScanShareInfo> sl = createSmbServerShareList(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1, auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                            result.share_item_list.addAll(sl);
                        } else {
                            result.smb1_available = false;
                        }
                    } catch(JcifsException e) {
                        e.printStackTrace();
                        mUtil.addDebugMsg(1, "I", "JcifsException occured, error=" + e.getMessage());
                    }
                }

                if (scan_smbv23) {
                    try {
                        Properties prop = new Properties();
                        prop.setProperty("jcifs.smb.client.responseTimeout", "3000");
                        JcifsAuth auth = new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, domain, user, pass, JcifsAuth.SMB_CLIENT_MIN_VERSION, JcifsAuth.SMB_CLIENT_MAX_VERSION, prop);
                        result.smb23_nt_status_desc = isSmbServerAvailable(auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                        if (!result.smb23_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                            result.smb23_available = true;
                            ArrayList<SmbServerScanShareInfo> sl = createSmbServerShareList(SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23, auth, result.server_smb_ip_addr, result.server_smb_name, result.server_smb_port_number);
                            result.share_item_list.addAll(sl);
                        } else {
                            result.smb23_available = false;
                        }
                    } catch(JcifsException e) {
                        e.printStackTrace();
                        mUtil.addDebugMsg(1, "I", "JcifsException occured, error=" + e.getMessage());
                    }
                }

                hndl.post(new Runnable() {
                    @Override
                    public void run() {
                        p_ntfy.notifyToListener(true, new Object[]{result});
                        dialog.dismiss();
                    }
                });
            }
        };
        th.start();
    }

    private String isSmbServerAvailable(JcifsAuth auth, String address, String srv_name, String port) {
        boolean result=false;
        String smb_ip_addr = address==null? "":address;
        String smb_hostname = srv_name==null? "":srv_name;
        String port_number = port==null? "":port;

        String host = smb_ip_addr;
        if (host.equals("")) host = smb_hostname;

        String url_prefix=CommonUtilities.buildSmbUrlAddressElement(host, port_number);
        url_prefix = "smb://" + url_prefix;
        mUtil.addDebugMsg(1, "I", "buildRemoteUrl url_prefix="+url_prefix);

        String server_status="";
        try {
            // we just try to connect to the server without credentials
            //JcifsFile[] share_file_list=null;
            //JcifsFile sf = new JcifsFile(url_prefix, auth);
            //share_file_list=sf.listFiles();
            JcifsFile sf = new JcifsFile(url_prefix, auth);
            sf.listFiles();
            server_status="";
            result=true;
            try {
                sf.close();
            } catch(Exception e) {
                mUtil.addDebugMsg(1,"I","close() failed. Error=",e.getMessage());
            }
        } catch (JcifsException e) {
            //e.printStackTrace();
            if (e.getNtStatus()==0xc0000001) server_status=SMB_STATUS_UNSUCCESSFULL;                 //
            else if (e.getNtStatus()==0xc0000022) server_status=SMB_STATUS_ACCESS_DENIED;  //
            else if (e.getNtStatus()==0xc000015b) server_status=SMB_STATUS_INVALID_LOGON_TYPE;  //
            else if (e.getNtStatus()==0xc000006d) server_status=SMB_STATUS_UNKNOWN_ACCOUNT;  //
            else server_status=Integer.toHexString(e.getNtStatus());
            mUtil.addDebugMsg(1,"I","isSmbServerAvailable smb_level="+auth.getSmbLevel() + ", url_prefix="+url_prefix+
                    ", status="+server_status+ String.format(", status=0x%8h",e.getNtStatus())+", result="+result);
        } catch (MalformedURLException e) {
            //log.info("Test logon failed." , e);
            //e.printStackTrace();
        }
        mUtil.addDebugMsg(1, "I", "isSmbServerAvailable smb_level="+auth.getSmbLevel() + ", smb_ip_addr="+smb_ip_addr + ", smb_hostname="+smb_hostname + ", url_prefix="+url_prefix + ", result="+server_status);
        return server_status;
    }
        
    private ArrayList<SmbServerScanShareInfo> createSmbServerShareList(String smb_level, JcifsAuth auth, String address, String srv_name, String port) {
        String smb_ip_addr = address==null? "":address;
        String smb_hostname = srv_name==null? "":srv_name;
        String port_number = port==null? "":port;

        String host = smb_ip_addr;
        if (host.equals("")) host = smb_hostname;

        String url_prefix=CommonUtilities.buildSmbUrlAddressElement(host, port_number);
        url_prefix = "smb://" + url_prefix;
        //mUtil.addDebugMsg(1, "I", "buildRemoteUrl url_prefix="+url_prefix);

        mUtil.addDebugMsg(1, "I", "createSmbServerShareList level="+smb_level + ", smb_ip_addr="+smb_ip_addr + ", smb_hostname="+smb_hostname + ", url_prefix="+url_prefix);

        ArrayList<SmbServerScanShareInfo> result=new ArrayList<SmbServerScanShareInfo>();
        JcifsFile[] share_file_list=null;
        try {
            JcifsFile sf = new JcifsFile(url_prefix, auth);
            share_file_list=sf.listFiles();
            for(JcifsFile item:share_file_list) {
                mUtil.addDebugMsg(1, "I", "   Share="+item.getName());
                SmbServerScanShareInfo share_item=new SmbServerScanShareInfo();
                share_item.smb_level=smb_level;
                share_item.share_name=item.getName().substring(0, item.getName().length()-1);
                if (!share_item.share_name.endsWith("$")) {
                    result.add(share_item);
                }
            }
            try {
                sf.close();
            } catch(Exception e) {
                mUtil.addDebugMsg(1,"I","close() failed. Error=",e.getMessage());
            }
        } catch (JcifsException e) {
            mUtil.addDebugMsg(1,"I","createSmbServerShareList level="+smb_level + ", url_prefix="+url_prefix +
                    String.format(", status=0x%8h",e.getNtStatus()));

        } catch (MalformedURLException e) {
//            log.info("Test logon failed." , e);
        }
        return result;
    }

    private boolean isValidScanAddress(Dialog dialog) {
        boolean result = false;
        final EditText baEt1 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o1);
        final EditText baEt2 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o2);
        final EditText baEt3 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o3);
        final EditText baEt4 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o4);
        final EditText eaEt5 = dialog.findViewById(R.id.scan_smb_server_scan_dlg_end_address_o4);
        final TextView tvmsg = dialog.findViewById(R.id.scan_smb_server_scan_dlg_msg);

        String ba1 = baEt1.getText().toString();
        String ba2 = baEt2.getText().toString();
        String ba3 = baEt3.getText().toString();
        String ba4 = baEt4.getText().toString();
        String ea5 = eaEt5.getText().toString();

        tvmsg.setText("");
        if (ba1.equals("")) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
            baEt1.requestFocus();
            return false;
        } else if (ba2.equals("")) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
            baEt2.requestFocus();
            return false;
        } else if (ba3.equals("")) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
            baEt3.requestFocus();
            return false;
        } else if (ba4.equals("")) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
            baEt4.requestFocus();
            return false;
        } else if (ea5.equals("")) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
            eaEt5.requestFocus();
            return false;
        }
        int iba1=0, iba2=0, iba3=0, iba4=0, iea5=0;
        try {
            iba1 = Integer.parseInt(ba1);
            if (iba1 > 255) {
                tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
                baEt1.requestFocus();
                return false;
            }
        } catch(Exception e) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
            baEt1.requestFocus();
            return false;
        }

        try {
            iba2 = Integer.parseInt(ba2);
            if (iba2 > 255) {
                tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
                baEt2.requestFocus();
                return false;
            }
        } catch(Exception e) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
            baEt2.requestFocus();
            return false;
        }
        try {
            iba3 = Integer.parseInt(ba3);
            if (iba3 > 255) {
                tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
                baEt3.requestFocus();
                return false;
            }
        } catch(Exception e) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
            baEt3.requestFocus();
            return false;
        }
        try {
            iba4 = Integer.parseInt(ba4);
        } catch(Exception e) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
            baEt4.requestFocus();
            return false;
        }
        try {
            iea5 = Integer.parseInt(ea5);
        } catch(Exception e) {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
            eaEt5.requestFocus();
            return false;
        }

        if (iba1==10) {
            //NOP
        } else if (iba1==172) {
            if (iba2>=16 && iba2<=31) {
                //NOP
            } else {
                tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_not_private));
                return false;
            }
        } else if (iba1==192 && iba2==168) {
            //NOP
        } else {
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_not_private));
            return false;
        }

        if (iba4 > 0 && iba4 < 255) {
            if (iea5 > 0 && iea5 < 255) {
                if (iba4 <= iea5) {
                    result = true;
                } else {
//                    baEt4.requestFocus();
                    tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_end_range_error));
                    return false;
                }
            } else {
//                eaEt5.requestFocus();
                tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_addr_gt_end_addr));
                return false;
            }
        } else {
            baEt4.requestFocus();
            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
            return false;
        }

//        if (!CommonUtilities.isIpV4PrivateAddress(ba1+"."+ba2+"."+ba3+"."+ba4)) {
//            result = false;
//            tvmsg.setText(mActivity.getString(R.string.msgs_ip_address_range_dlg_not_private));
//        }

        return result;
    }

    private String getSmbSelectedProtocol(Spinner spinner) {
        if (spinner.getSelectedItem()==null) {
            return SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT;
        }
        return (String)spinner.getSelectedItem();
    }

    static public class SmbServerInfo {
        public String serverHostIpAddress ="";
        public String serverHostName ="";
        //public String serverHostNameOrIP = "";
        public String serverProtocol="";
        public String serverPort="";
        public String serverShareName="";

        public String serverDomainName ="";
        public String serverAccountName="";
        public String serverAccountPassword="";
    }

    public static class SmbServerScanResult {
        public static final String SMB_STATUS_UNSUCCESSFULL="Unsuccessfull";
        public static final String SMB_STATUS_ACCESS_DENIED="Access denied";
        public static final String SMB_STATUS_INVALID_LOGON_TYPE="Invalid login type";
        public static final String SMB_STATUS_UNKNOWN_ACCOUNT="Unknown account or invalid password";
        public static final String SMB_STATUS_UNTESTED_LOGIN="Login failed because no user or password were provided"; // during scan for servers, SMB_STATUS_UNKNOWN_ACCOUNT

        // True if the scan query that listed the server was started with the corresponding SMB level selected (SMBv1, SMBv23 or "SMBv1 & SMBv23")
        public boolean scan_for_smb1=false;
        public boolean scan_for_smb23=false;

        // true if the found server supports the corresponding SMB level
        public boolean smb1_available=false;
        public boolean smb23_available =false;

        public String smb1_nt_status_desc="";
        public String smb23_nt_status_desc ="";
        public String server_smb_name= "";
        public String server_smb_ip_addr= "";
        public String server_smb_port_number= "445";
        public ArrayList<SmbServerScanShareInfo> share_item_list=new ArrayList<SmbServerScanShareInfo>();
    }

    private static class SmbServerScanShareInfo {
        public String smb_level= SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_DEFAULT;
        public String share_name="";
    }

    private static class SmbServerScanAdapter extends ArrayAdapter<SmbServerScanResult> {

        private ArrayList<SmbServerScanResult> mResultList = null;
        private static final Object mLockResultList = new Object(); //for mResultList
        private int mResourceId = 0;
        private final Context mActivity;
        private NotifyEvent mNtfyEvent = null;
        private boolean mButtonEnabled = true;

        public SmbServerScanAdapter(Context context, int resource,
                                    ArrayList<SmbServerScanResult> objects, NotifyEvent ntfy) {
            super(context, resource, objects);
            mResultList = objects;
            mResourceId = resource;
            mActivity = context;
            mNtfyEvent = ntfy;
        }

        public void setButtonEnabled(boolean p) {
            mButtonEnabled = p;
            notifyDataSetChanged();
        }

        @Override
        public void add(SmbServerScanResult item) {
            synchronized (mLockResultList) {
                mResultList.add(item);
                notifyDataSetChanged();
            }
        }

        public void sort() {
            synchronized (mLockResultList) {
                mResultList.sort(new Comparator<SmbServerScanResult>() {
                    @Override
                    public int compare(SmbServerScanResult lhs, SmbServerScanResult rhs) {
                        String r_o4 = rhs.server_smb_ip_addr.substring(rhs.server_smb_ip_addr.lastIndexOf(".") + 1);
                        String r_key = String.format("%3s", Integer.parseInt(r_o4)).replace(" ", "0");
                        String l_o4 = lhs.server_smb_ip_addr.substring(lhs.server_smb_ip_addr.lastIndexOf(".") + 1);
                        String l_key = String.format("%3s", Integer.parseInt(l_o4)).replace(" ", "0");
                        return l_key.compareTo(r_key);
                    }
                });
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            ViewHolder holder;
            final SmbServerScanResult o = getItem(position);
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(mResourceId, null);
                holder = new ViewHolder();
                holder.tv_name = v.findViewById(R.id.scan_result_list_item_server_name);
                holder.tv_addr = v.findViewById(R.id.scan_result_list_item_server_addr);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            if (o != null) {
                String smb_level = "";
                String sep = "";
                if (o.smb1_available) {
                    smb_level += SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB1;
                    sep = " ";
                }
                if (o.smb23_available) {
                    smb_level += sep;
                    smb_level += SyncTaskItem.SYNC_FOLDER_SMB_PROTOCOL_SMB23;
                }

                sep = "\n";
                if (o.server_smb_name.equals("")) {
                    sep = "";
                }
                String smb_name_txt = o.server_smb_name + sep + smb_level;
                holder.tv_name.setText(smb_name_txt);
                holder.tv_addr.setText(o.server_smb_ip_addr);
                //if (o.server_smb_name.equals("")) holder.tv_name.setEnabled(false);
                //else holder.tv_name.setEnabled(true);

                // Grey-out invalid entries, we disable selecting them in isEnabled() method
                if (!o.smb1_available && !o.smb23_available) {
                    // Server responds to be SMB compatible but doesn't provide a compatible SMB v1/2/3 level support
                    // Exp: scanning uses port 135 while SMB server is listening on a different port
                    holder.tv_name.setEnabled(false);
                    holder.tv_addr.setEnabled(false);
                }
                if (o.scan_for_smb1 && !o.scan_for_smb23) {
                    // SMB v1 only scan, servers with only SMB v2/3 are greyed
                    if (o.smb1_available) {
                        holder.tv_name.setEnabled(true);
                        holder.tv_addr.setEnabled(true);
                    } else {
                        holder.tv_name.setEnabled(false);
                        holder.tv_addr.setEnabled(false);
                    }
                }
                if (o.scan_for_smb23 && !o.scan_for_smb1) {
                    // SMB v2/3 only scan, servers with only SMB v1 are greyed
                    if (o.smb23_available) {
                        holder.tv_name.setEnabled(true);
                        holder.tv_addr.setEnabled(true);
                    } else {
                        holder.tv_name.setEnabled(false);
                        holder.tv_addr.setEnabled(false);
                    }
                }
            }
            return v;
        }

        // the method returns true if all the list items can be selected
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        // The method returns true for a particular list item position so that the list item at that position can be selected
        // On scan result, if we select SMBv1 and results have an SMBv2 only server, do not allow the server to be selectable
        // Same if we select SMBv23 and results have an SMBv1 only server, do not allow the server to be selectable
        @Override
        public boolean isEnabled(int position) {
            final SmbServerScanResult o = getItem(position);
            boolean is_selectable = o.smb1_available || o.smb23_available;
            // Server responds to be SMB compatible but doesn't provide a compatible SMB v1/2/3 level support
            // Exp: scanning uses port 135 while SMB server is listening on a different port -> Server cannot be selected in results
            if (o.scan_for_smb1 && !o.scan_for_smb23) {
                // SMB v1 only scan, servers with only SMB v2/3 cannot be selected
                if (!o.smb1_available) is_selectable = false;
            }
            if (o.scan_for_smb23 && !o.scan_for_smb1) {
                // SMB v2/3 only scan, servers with only SMB v1 cannot be selected
                if (!o.smb23_available) is_selectable = false;
            }
            return is_selectable;
        }

        static class ViewHolder {
            TextView tv_name, tv_addr;
        }
    }
}

