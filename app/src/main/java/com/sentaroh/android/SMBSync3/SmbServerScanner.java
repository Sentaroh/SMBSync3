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
import android.text.TextWatcher;
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
import android.widget.TextView;

import static com.sentaroh.android.SMBSync3.SmbServerScanner.SmbServerScanResult.*;

import com.sentaroh.android.JcifsFile2.JcifsAuth;
import com.sentaroh.android.JcifsFile2.JcifsException;
import com.sentaroh.android.JcifsFile2.JcifsFile;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.Dialog.DialogBackKeyListener;
import com.sentaroh.android.Utilities3.NotifyEvent;
import com.sentaroh.android.Utilities3.ThreadCtrl;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;


public class SmbServerScanner {

    private ActivityMain mActivity=null;
    private CommonUtilities mUtil=null;
    private GlobalParameters mGp=null;

    public SmbServerScanner(ActivityMain a, GlobalParameters gp, CommonUtilities cu, final NotifyEvent p_ntfy,
                            String port_number, boolean scan_start, final String smb_protocol) {
        mActivity=a;
        mUtil=cu;
        mGp=gp;
        initDialog(p_ntfy, port_number, scan_start, smb_protocol);
    }

    private void initDialog(final NotifyEvent p_ntfy, String port_number, boolean scan_start, final String smb_protocol) {
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

        LinearLayout ll_dlg_view = (LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_dlg_view);
//        ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.scan_smb_server_scan_dlg_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_title);
        title_view.setBackgroundColor(mGp.themeColorList.title_background_color);
        title.setTextColor(mGp.themeColorList.title_text_color);

        final Button btn_scan = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_msg);
        final TextView tv_result = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_result_title);
        tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_press_scan_btn));
        tv_result.setVisibility(TextView.GONE);

        final String from = CommonUtilities.getIfIpAddress(mUtil).equals("")?"192.168.0.1":CommonUtilities.getIfIpAddress(mUtil);
        String subnet = from.substring(0, from.lastIndexOf("."));
        String subnet_o1, subnet_o2, subnet_o3;
        subnet_o1 = subnet.substring(0, subnet.indexOf("."));
        subnet_o2 = subnet.substring(subnet.indexOf(".") + 1, subnet.lastIndexOf("."));
        subnet_o3 = subnet.substring(subnet.lastIndexOf(".") + 1, subnet.length());
        final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o1);
        final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o2);
        final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o3);
        final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o4);
        final EditText eaEt5 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_end_address_o4);
        baEt1.setText(subnet_o1);
        baEt2.setText(subnet_o2);
        baEt3.setText(subnet_o3);
        baEt4.setText("1");
        baEt4.setSelection(1);
        eaEt5.setText("254");
        baEt4.requestFocus();

        //final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_port_number);
        //et_port_number.setText(port_number);

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
                if (isValidScanAddress(dialog)) {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
                } else {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, false);
                }
            }
        });

        baEt2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isValidScanAddress(dialog)) {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
                } else {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, false);
                }
            }
        });

        baEt3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isValidScanAddress(dialog)) {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
                } else {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, false);
                }
            }
        });

        baEt4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isValidScanAddress(dialog)) {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
                } else {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, false);
                }
            }
        });

        eaEt5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isValidScanAddress(dialog)) {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
                } else {
                    CommonUtilities.setViewEnabled(mActivity, btn_scan, false);
                }
            }
        });

        CommonUtilities.setViewEnabled(mActivity, btn_scan, true);
        tvmsg.setText("");

        final ListView lv = (ListView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_result_list);
        final SmbServerScanAdapter adap = new SmbServerScanAdapter
                (mActivity, R.layout.scan_address_result_list_item, mScanResultList, ntfy_lv_click);
        lv.setAdapter(adap);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
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
                NotifyEvent ntfy = new NotifyEvent(mActivity);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        lv.setEnabled(true);
                        if (!mScanSmbErrorMessage.equals("")) {
                            tvmsg.setText(mScanSmbErrorMessage);
                        } else {
                            if (mScanResultList.size() < 1) {
                                tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_not_detected));
                                tv_result.setVisibility(TextView.GONE);
                            } else {
                                tvmsg.setText(mActivity.getString(R.string.msgs_scan_ip_address_select_detected_host));
                                tv_result.setVisibility(TextView.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }

                });
                tv_result.setVisibility(TextView.GONE);
                String ba1 = baEt1.getText().toString();
                String ba2 = baEt2.getText().toString();
                String ba3 = baEt3.getText().toString();
                String ba4 = baEt4.getText().toString();
                String ea5 = eaEt5.getText().toString();
                String subnet = ba1 + "." + ba2 + "." + ba3;
                int begin_addr = Integer.parseInt(ba4);
                int end_addr = Integer.parseInt(ea5);
                performSmbServerScan(dialog, lv, adap, subnet, begin_addr, end_addr, ntfy, smb_protocol);
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

    private void buildSmbServerParmsDlg(final SmbServerScanResult scan_result, final NotifyEvent p_ntfy) {
        final Dialog dialog=new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.scan_smb_server_parm_dlg);

        final LinearLayout ll_title=(LinearLayout) dialog.findViewById(R.id.scan_smb_server_parm_dlg_title_view);
        ll_title.setBackgroundColor(mGp.themeColorList.title_background_color);
        final TextView tv_title=(TextView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_title);
        tv_title.setTextColor(mGp.themeColorList.title_text_color);
        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_msg);
        final RadioGroup dlg_smb_host_rg=(RadioGroup)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_rg);
        final RadioButton dlg_use_ip_addr=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_address);
        final RadioButton dlg_use_host_name=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_id_hostname);
        final TextView dlg_smb_host_selcted=(TextView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_selected);
        final RadioButton dlg_use_smb1=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb1);
        final RadioButton dlg_use_smb23=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb23);
        final EditText dlg_smb_port_number=(EditText)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_port_number);
        final EditText dlg_smb_account_name=(EditText)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_name);
        final EditText dlg_smb_account_password=(EditText)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password);
        final LinearLayout ll_dlg_smb_share_name=(LinearLayout) dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_view);
        final ListView dlg_smb_share_name=(ListView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_name);

        final ArrayList<String> share_name_list=new ArrayList<String>();
        final ArrayAdapter<String>share_list_adapter=new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_single_choice, share_name_list);
        dlg_smb_share_name.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dlg_smb_share_name.setAdapter(share_list_adapter);

        final Button btn_refresh=(Button)dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_refresh_share_list);

        final Button btn_ok=(Button)dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_ok);
        CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.scan_smb_server_parm_dlg_btn_cancel);

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


        if (scan_result.smb23_available) dlg_use_smb23.setChecked(true);
        else {
            if (scan_result.smb1_available) {
                CommonUtilities.setViewEnabled(mActivity, dlg_use_smb1, true);
                dlg_use_smb1.setChecked(true);
            }
            if (!scan_result.smb23_available) CommonUtilities.setViewEnabled(mActivity, dlg_use_smb23, false);
        }
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
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
            }
        });

        dlg_use_smb23.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dlg_use_smb1.setChecked(!b);
                CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
                buildShareListSelectorView(dialog, scan_result, share_list_adapter);
            }
        });

        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NotifyEvent ntfy=new NotifyEvent(mActivity);
                ntfy.setListener(new NotifyEvent.NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        buildShareListSelectorView(dialog, scan_result, share_list_adapter);
                        CommonUtilities.setViewEnabled(mActivity, btn_ok, false);
                    }

                    @Override
                    public void negativeResponse(Context context, Object[] objects) {

                    }
                });
                final Handler hndl=new Handler();
                final String acct=dlg_smb_account_name.getText().toString().equals("")?null:dlg_smb_account_name.getText().toString();
                final String pswd=dlg_smb_account_password.getText().toString().equals("")?null:dlg_smb_account_password.getText().toString();
                Thread th=new Thread(){
                    @Override
                    public void run() {
                        SmbServerScanResult result= createSmbServerShareInfo(null, acct, pswd,
                                scan_result.server_smb_ip_addr, scan_result.server_smb_name);
                        scan_result.smb1_nt_status_desc=result.smb1_nt_status_desc;
                        scan_result.smb1_available=result.smb1_available;
                        scan_result.smb23_nt_status_desc=result.smb23_nt_status_desc;
                        scan_result.smb23_available=result.smb23_available;
                        scan_result.share_item_list.clear();
                        scan_result.share_item_list.addAll(result.share_item_list);
                        hndl.post(new Runnable() {
                            @Override
                            public void run() {
                                ntfy.notifyToListener(true, null);
                            }
                        });
                    }
                };
                th.start();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String smb_host=dlg_use_ip_addr.isChecked()?scan_result.server_smb_ip_addr:scan_result.server_smb_name;
                String smb_level=dlg_use_smb1.isChecked()?SMB_LEVEL_SMB1: SmbServerScanResult.SMB_LEVEL_SMB23;
                String smb_portnum=dlg_smb_port_number.getText().toString();
                String smb_acct_name=dlg_smb_account_name.getText().toString();
                String smb_acct_pswd=dlg_smb_account_password.getText().toString();
                String smb_share_name=share_name_list.get(0);

                SparseBooleanArray checked = dlg_smb_share_name.getCheckedItemPositions();
                for (int i = 0; i <= share_name_list.size(); i++) {
                    if (checked.get(i) == true) {
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

    private void buildShareListSelectorView(Dialog dialog, SmbServerScanResult scan_result, ArrayAdapter adapter) {
        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_msg);
        final RadioButton dlg_use_smb1=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb1);
        final RadioButton dlg_use_smb23=(RadioButton)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_smb_protocol_smb23);
        final LinearLayout ll_dlg_smb_share_name=(LinearLayout) dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_view);
        final ListView dlg_smb_share_name=(ListView)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_share_name);
        adapter.clear();
        dlg_smb_share_name.setAdapter(null);
        boolean share_added=false;
        if (dlg_use_smb1.isChecked()) {
            updateShareListSelectorAdapter(dialog, SMB_LEVEL_SMB1, dlg_msg, adapter, scan_result.smb1_nt_status_desc, scan_result.share_item_list);
        } else {
            updateShareListSelectorAdapter(dialog, SMB_LEVEL_SMB23, dlg_msg, adapter, scan_result.smb23_nt_status_desc, scan_result.share_item_list);
        }
        if (adapter.getCount()>0) ll_dlg_smb_share_name.setVisibility(LinearLayout.VISIBLE);
        else ll_dlg_smb_share_name.setVisibility(LinearLayout.GONE);

//        scanSmbServerEnableSmbScanSelectorOkButton(dialog);

        if (dlg_msg.getText().length()==0 && adapter.getCount()>0) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_select_smb_share_name));
        }

        dlg_smb_share_name.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    private void updateShareListSelectorAdapter(Dialog dialog, String smb_level,
                                                TextView dlg_msg, ArrayAdapter adapter, String nt_status_desc, ArrayList<SmbServerScanShareInfo>sl) {
        if (nt_status_desc.equals(SMB_STATUS_ACCESS_DENIED)) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_correct_account));
        } else if (nt_status_desc.equals(SMB_STATUS_INVALID_LOGON_TYPE)) {
            dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_enable_account));
        } else if (nt_status_desc.equals(SMB_STATUS_UNKNOWN_ACCOUNT)) {
            final EditText dlg_smb_account_name=(EditText)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_name);
            final EditText dlg_smb_account_password=(EditText)dialog.findViewById(R.id.scan_smb_server_parm_dlg_smb_server_account_password);
            String acct=dlg_smb_account_name.getText().toString();
            String pswd=dlg_smb_account_password.getText().toString();
            if (acct.equals("") && pswd.equals("")) {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_account_password_not_specified));
            } else {
                dlg_msg.setText(mActivity.getString(R.string.msgs_task_edit_sync_folder_dlg_edit_smb_server_parm_specify_correct_account_password));
            }
        } else {
            dlg_msg.setText("");
        }
        for(SmbServerScanShareInfo item:sl) {
            if (item.smb_level.equals(smb_level)) {
                adapter.add(item.share_name);
            }
        }
    }

    private int mScanCompleteCount = 0, mScanAddrCount = 0;
    private ArrayList<String> mScanRequestedAddrList = new ArrayList<String>();
    private ArrayList<SmbServerScanResult> mScanResultList = new ArrayList<SmbServerScanResult>();
    private String mLockScanCompleteCount = "";

    private void performSmbServerScan(
            final Dialog dialog,
            final ListView lv_ipaddr,
            final SmbServerScanAdapter adap,
//            final ArrayList<SmbServerScanAdapter.NetworkScanListItem> ipAddressList,
            final String subnet, final int begin_addr, final int end_addr,
            final NotifyEvent p_ntfy, final String smb_protcol) {
        final Handler handler = new Handler();
        final ThreadCtrl tc = new ThreadCtrl();
        final LinearLayout ll_addr = (LinearLayout) dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_address);
        final LinearLayout ll_prog = (LinearLayout) dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress);
        final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress_msg);
        final Button btn_scan = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        final Button scan_cancel = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress_cancel);

//        final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.scan_remote_ntwk_ctv_use_port);
        //final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_port_number);

        tvmsg.setText("");
        scan_cancel.setText(R.string.msgs_scan_progress_spin_dlg_addr_cancel);
        ll_addr.setVisibility(LinearLayout.GONE);
        ll_prog.setVisibility(LinearLayout.VISIBLE);
        btn_scan.setVisibility(Button.GONE);
        btn_cancel.setVisibility(Button.GONE);
        adap.setButtonEnabled(false);
        CommonUtilities.setViewEnabled(mActivity, scan_cancel, true);
        dialog.setOnKeyListener(new DialogBackKeyListener(mActivity));
        dialog.setCancelable(false);

        mScanResultList.clear();
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

        Thread th=new Thread(new Runnable() {
            @Override
            public void run() {//non UI thread
                mScanCompleteCount = 0;
                mScanAddrCount = end_addr - begin_addr + 1;
                int scan_thread = 100;
                String scan_port = "";
                //if (et_port_number.getText().length()>0) scan_port = et_port_number.getText().toString();
                for (int i = begin_addr; i <= end_addr; i += scan_thread) {
                    if (!tc.isEnabled()) break;
                    boolean scan_end = false;
                    for (int j = i; j < (i + scan_thread); j++) {
                        if (!tc.isEnabled()) break;
                        if (j <= end_addr) {
                            startSmbServerScanThread(handler, tc, dialog, p_ntfy,
                                    lv_ipaddr, adap, tvmsg, subnet + "." + j,
//                                    ipAddressList,
                                    scan_port, smb_protcol);
                        } else {
                            scan_end = true;
                        }
                    }
                    if (!scan_end) {
                        for (int wc = 0; wc < 210; wc++) {
                            if (!tc.isEnabled()) break;
                            synchronized (mScanRequestedAddrList) {
                                if (mScanRequestedAddrList.size() == 0) break;
                            }
                            SystemClock.sleep(30);
                        }
                    }
                }
                if (!tc.isEnabled()) {
                    for (int i = 0; i < 1000; i++) {
                        SystemClock.sleep(100);
                        synchronized (mScanRequestedAddrList) {
                            if (mScanRequestedAddrList.size() == 0) break;
                        }
                    }
                    handler.post(new Runnable() {// UI thread
                        @Override
                        public void run() {
                            adap.sort();
                            closeSmbServerScanProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
                        }
                    });
                } else {
                    for (int i = 0; i < 1000; i++) {
                        SystemClock.sleep(100);
                        synchronized (mScanRequestedAddrList) {
                            if (mScanRequestedAddrList.size() == 0) break;
                        }
                    }
                    handler.post(new Runnable() {// UI thread
                        @Override
                        public void run() {
                            synchronized (mLockScanCompleteCount) {
                                adap.sort();
                                lv_ipaddr.setSelection(lv_ipaddr.getCount());
                                adap.notifyDataSetChanged();
                                closeSmbServerScanProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
                            }
                        }
                    });
                }
            }
        });
        th.setPriority(Thread.MIN_PRIORITY);
        th.start();
    }

    private void closeSmbServerScanProgressDlg(
            final Dialog dialog,
            final NotifyEvent p_ntfy,
            final ListView lv_ipaddr,
            final SmbServerScanAdapter adap,
            final TextView tvmsg) {
        final LinearLayout ll_addr = (LinearLayout) dialog.findViewById(R.id.scan_smb_server_scan_dlg_scan_address);
        final LinearLayout ll_prog = (LinearLayout) dialog.findViewById(R.id.scan_smb_server_scan_dlg_progress);
        final Button btn_scan = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_ok);
        final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_smb_server_scan_dlg_btn_cancel);
        ll_addr.setVisibility(LinearLayout.VISIBLE);
        ll_prog.setVisibility(LinearLayout.GONE);
        btn_scan.setVisibility(Button.VISIBLE);
        btn_cancel.setVisibility(Button.VISIBLE);
        adap.setButtonEnabled(true);
        dialog.setOnKeyListener(null);
        dialog.setCancelable(true);
        if (p_ntfy != null) p_ntfy.notifyToListener(true, null);

    }

    private String mScanSmbErrorMessage="";
    private void startSmbServerScanThread(final Handler handler,
                                          final ThreadCtrl tc,
                                          final Dialog dialog,
                                          final NotifyEvent p_ntfy,
                                          final ListView lv_ipaddr,
                                          final SmbServerScanAdapter adap,
                                          final TextView tvmsg,
                                          final String addr,
//                                              final ArrayList<SmbServerScanAdapter.NetworkScanListItem> ipAddressList,
                                          final String scan_port, final String smb_level) {
        final String scan_prog = mActivity.getString(R.string.msgs_ip_address_scan_progress);
        if (!tc.isEnabled()) return;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {//non UI thread
                if (!tc.isEnabled()) return;
//                byte[] oo=new byte[Integer.MAX_VALUE];
                synchronized (mScanRequestedAddrList) {
                    mScanRequestedAddrList.add(addr);
                }
                if (CommonUtilities.isSmbHost(mUtil, addr, scan_port, 3500)) {
                    final String srv_name = CommonUtilities.getSmbHostName(mUtil, smb_level, addr);
                    mScanResultList.add(createSmbServerShareInfo(null, null, null, addr, srv_name));

                    handler.post(new Runnable() {// UI thread
                        @Override
                        public void run() {
                            synchronized (mScanRequestedAddrList) {
                                mScanRequestedAddrList.remove(addr);
                                synchronized (adap) {
//                                    adap.add(smb_server_item);
                                    adap.sort();
                                }
                            }
                            synchronized (mLockScanCompleteCount) {
                                mScanCompleteCount++;
                            }
                        }
                    });
                } else {
                    synchronized (mScanRequestedAddrList) {
                        mScanRequestedAddrList.remove(addr);
                    }
                    synchronized (mLockScanCompleteCount) {
                        mScanCompleteCount++;
                    }
                }
                handler.post(new Runnable() {// UI thread
                    @Override
                    public void run() {
                        synchronized (mLockScanCompleteCount) {
                            lv_ipaddr.setSelection(lv_ipaddr.getCount());
//                            adap.notifyDataSetChanged();
                            String p_txt = String.format(scan_prog, (mScanCompleteCount * 100) / mScanAddrCount);
                            tvmsg.setText(p_txt);
                        }
                    }
                });
            }
        });
        th.setPriority(Thread.MIN_PRIORITY);
        th.start();
    }

    final private SmbServerScanResult createSmbServerShareInfo(String domain, String user, String pass, String address, String srv_name) {
        SmbServerScanResult result=new SmbServerScanResult();
        result.server_smb_ip_addr=address;
        result.server_smb_name=srv_name;

        try {
            JcifsAuth auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB1, domain, user, pass);
            result.smb1_nt_status_desc=isSmbServerAvailable(SMB_LEVEL_SMB1, auth, address);
            if (!result.smb1_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                result.smb1_available=true;
                ArrayList<SmbServerScanShareInfo> sl=createSmbServerShareList(SMB_LEVEL_SMB1, auth, address);
                result.share_item_list.addAll(sl);
            }
        } catch(JcifsException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "I", "JcifsException occured, error="+e.getMessage());
        }

        try {
            Properties prop=new Properties();
            prop.setProperty("jcifs.smb.client.responseTimeout", mGp.settingsSmbClientResponseTimeout);
            JcifsAuth auth=new JcifsAuth(JcifsAuth.JCIFS_FILE_SMB23, domain, user, pass, JcifsAuth.SMB_CLIENT_MIN_VERSION, JcifsAuth.SMB_CLIENT_MAX_VERSION, prop);
            result.smb23_nt_status_desc =isSmbServerAvailable(SMB_LEVEL_SMB23, auth, address);
            if (!result.smb23_nt_status_desc.equals(SMB_STATUS_UNSUCCESSFULL)) {
                result.smb23_available =true;
                ArrayList<SmbServerScanShareInfo> sl=createSmbServerShareList(SMB_LEVEL_SMB23, auth, address);
                result.share_item_list.addAll(sl);
            }
        } catch(JcifsException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "I", "JcifsException occured, error="+e.getMessage());
        }

        return result;
    }

    final private String isSmbServerAvailable(String smb_level, JcifsAuth auth, String address) {
        boolean result=false;
        JcifsFile[] share_file_list=null;
        String server_status="";
        try {
            JcifsFile sf = new JcifsFile("smb://"+address, auth);
            share_file_list=sf.listFiles();
            server_status="";
            mUtil.addDebugMsg(1,"I","isSmbServerAvailable level="+smb_level+", address="+address+", result="+server_status);
            result=true;
            try {
                sf.close();
            } catch(Exception e) {
                mUtil.addDebugMsg(1,"I","close() failed. Error=",e.getMessage());
            }
        } catch (JcifsException e) {
//            e.printStackTrace();
            if (e.getNtStatus()==0xc0000001) server_status=SMB_STATUS_UNSUCCESSFULL;                 //
            else if (e.getNtStatus()==0xc0000022) server_status=SMB_STATUS_ACCESS_DENIED;  //
            else if (e.getNtStatus()==0xc000015b) server_status=SMB_STATUS_INVALID_LOGON_TYPE;  //
            else if (e.getNtStatus()==0xc000006d) server_status=SMB_STATUS_UNKNOWN_ACCOUNT;  //
            mUtil.addDebugMsg(1,"I","isSmbServerAvailable level="+smb_level+", address="+address+
                    ", statue="+server_status+ String.format(", status=0x%8h",e.getNtStatus())+", result="+result);

        } catch (MalformedURLException e) {
//            log.info("Test logon failed." , e);
        }
        return server_status;
    }

    final private ArrayList<SmbServerScanShareInfo> createSmbServerShareList(String smb_level, JcifsAuth auth, String address) {
        ArrayList<SmbServerScanShareInfo> result=new ArrayList<SmbServerScanShareInfo>();
        JcifsFile[] share_file_list=null;
        try {
            JcifsFile sf = new JcifsFile("smb://"+address, auth);
            share_file_list=sf.listFiles();
            mUtil.addDebugMsg(1,"I","createSmbServerShareList level="+smb_level+", address="+address);
            for(JcifsFile item:share_file_list) {
                mUtil.addDebugMsg(1,"I","   Share="+item.getName());
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
            mUtil.addDebugMsg(1,"I","createSmbServerShareList level="+smb_level+", address="+address+
                    String.format(", status=0x%8h",e.getNtStatus()));

        } catch (MalformedURLException e) {
//            log.info("Test logon failed." , e);
        }
        return result;
    }

    private boolean isValidScanAddress(Dialog dialog) {
        boolean result = false;
        final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o1);
        final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o2);
        final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o3);
        final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_begin_address_o4);
        final EditText eaEt5 = (EditText) dialog.findViewById(R.id.scan_smb_server_scan_dlg_end_address_o4);
        final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_smb_server_scan_dlg_msg);

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

    static public class SmbServerInfo {
        public String serverHostIpAddress ="";
        public String serverHostName ="";
        public String serverProtocol="";
        public String serverPort="";
        public String serverShareName="";

        public String serverDomainName ="";
        public String serverAccountName="";
        public String serverAccountPassword="";
    }

    public class SmbServerScanResult {
        public static final String SMB_LEVEL_SMB1="SMB1";
        public static final String SMB_LEVEL_SMB23 ="SMB23";
        public static final String SMB_STATUS_UNSUCCESSFULL="Unsuccessfull";
        public static final String SMB_STATUS_ACCESS_DENIED="Access denied";
        public static final String SMB_STATUS_INVALID_LOGON_TYPE="Invalid login type";
        public static final String SMB_STATUS_UNKNOWN_ACCOUNT="Unknown account or invalid password";
        //    public String server_smb_level= "SMB1";
        public boolean smb1_available=false;
        public String smb1_nt_status_desc="";
        public boolean smb23_available =false;
        public String smb23_nt_status_desc ="";
        public String server_smb_name= "";
        public String server_smb_ip_addr= "";
        public String server_smb_port_number= "139";
        public ArrayList<SmbServerScanShareInfo> share_item_list=new ArrayList<SmbServerScanShareInfo>();
    }

    public class SmbServerScanShareInfo {
        public String smb_level= "SMB1";
        public String share_name="";
    }

    private static class SmbServerScanAdapter extends ArrayAdapter<SmbServerScanResult> {

        private ArrayList<SmbServerScanResult> mResultList = null;
        private int mResourceId = 0;
        private Context mActivity;
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
            synchronized (mResultList) {
                mResultList.add(item);
                notifyDataSetChanged();
            }
        }

        public void sort() {
            synchronized (mResultList) {
                Collections.sort(mResultList, new Comparator<SmbServerScanResult>() {
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
                holder.tv_name = (TextView) v.findViewById(R.id.scan_result_list_item_server_name);
                holder.tv_addr = (TextView) v.findViewById(R.id.scan_result_list_item_server_addr);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            if (o != null) {
                String smb_level="";
                if (o.smb1_available) smb_level+="SMB1 ";
                if (o.smb23_available) smb_level+="SMB2/3 ";
                holder.tv_name.setText(o.server_smb_name+"\n"+smb_level);
                holder.tv_addr.setText(o.server_smb_ip_addr);
                if (o.server_smb_name.equals("")) holder.tv_name.setEnabled(false);
                else holder.tv_name.setEnabled(true);
            }
            return v;
        }

        class ViewHolder {
            TextView tv_name, tv_addr;
        }

    }
}

