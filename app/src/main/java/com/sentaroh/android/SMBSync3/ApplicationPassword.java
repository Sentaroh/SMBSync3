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
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.sentaroh.android.Utilities3.Base64Compat;
import com.sentaroh.android.Utilities3.CallBackListener;
import com.sentaroh.android.Utilities3.Dialog.CommonDialog;
import com.sentaroh.android.Utilities3.EncryptUtilV3;
import com.sentaroh.android.Utilities3.NotifyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

public class ApplicationPassword {

    final private static Logger log= LoggerFactory.getLogger(ApplicationPassword.class);

    final static private long APPLICATION_PASSWORD_VALIDITY_PERIOD =30*60*1000;//30 Min

    private static final String APPLICATION_PASSWORD_HASH_VALUE = "settings_application_password_hash_value";
    static public String getPasswordHashValue(SharedPreferences prefs) {
        return prefs.getString(APPLICATION_PASSWORD_HASH_VALUE, "");
    }

    static public void savePasswordHashValue(GlobalParameters gp, SharedPreferences prefs, String hv) {
        prefs.edit().putString(APPLICATION_PASSWORD_HASH_VALUE, hv).commit();
        gp.settingSecurityApplicationPasswordHashValue=hv;
    }

    public final static int APPLICATION_PASSWORD_RESOURCE_EXPORT_TASK_LIST=1;
    public final static int APPLICATION_PASSWORD_RESOURCE_START_APPLICATION=2;
    public final static int APPLICATION_PASSWORD_RESOURCE_EDIT_SYNC_TASK=3;
    public final static int APPLICATION_PASSWORD_RESOURCE_INVOKE_SECURITY_SETTINGS =4;

    static private boolean isAuthRequired(final GlobalParameters gp, int resource_id) {
        boolean result=false;
        if (!gp.settingSecurityApplicationPasswordHashValue.equals("")) {
            switch (resource_id) {
                case APPLICATION_PASSWORD_RESOURCE_EXPORT_TASK_LIST:
                    if (gp.settingSecurityApplicationPasswordUseExport) result=true;
                    break;
                case APPLICATION_PASSWORD_RESOURCE_START_APPLICATION:
                    if (gp.settingSecurityApplicationPasswordUseAppStartup) result=true;
                    break;
                case APPLICATION_PASSWORD_RESOURCE_EDIT_SYNC_TASK:
                    if (gp.settingSecurityApplicationPasswordUseEditTask) result=true;
                    break;
                case APPLICATION_PASSWORD_RESOURCE_INVOKE_SECURITY_SETTINGS:
                    result=true;
                    break;
                default :
                    break;
            }
        }
        return result;
    }

    static public void authentication(final GlobalParameters gp, final Activity mActivity, final FragmentManager fm,
                                      final CommonUtilities mUtil, boolean force_auth, int resource_id, final CallBackListener cbl) {
        NotifyEvent ntfy=new NotifyEvent(mActivity);
        ntfy.setListener(new NotifyEvent.NotifyEventListener(){
            @Override
            public void positiveResponse(Context context, Object[] objects) {
                cbl.onCallBack(mActivity, true, null);
            }
            @Override
            public void negativeResponse(Context context, Object[] objects) {
                cbl.onCallBack(mActivity, false, null);
            }
        });
        authentication(gp, mActivity, fm, mUtil, force_auth, ntfy, resource_id);
    }

    static public void authentication(final GlobalParameters gp, final Activity mActivity, final FragmentManager fm,
                                      final CommonUtilities mUtil, boolean force_auth, final NotifyEvent notify_check, int resource_id) {
        if (!isAuthRequired(gp, resource_id)) {
            notify_check.notifyToListener(true,null);
            return;
        }

//        boolean auth_ok=false;
//        if (gp.settingSecurityApplicationPasswordHashValue.equals("")) {
//            if (notify_check!=null) notify_check.notifyToListener(true,null);
//            return;
//        }
        if (!force_auth) {
            if (gp.appPasswordAuthValidated) {
                if ((gp.appPasswordAuthLastTime + APPLICATION_PASSWORD_VALIDITY_PERIOD)> System.currentTimeMillis()) {
                    if (notify_check!=null) notify_check.notifyToListener(true,null);
                    return;
                }
                gp.appPasswordAuthValidated=false;
            }
        }

        final Dialog dialog = new Dialog(mActivity, gp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.password_input_dlg);

        LinearLayout ll_view = (LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mActivity, ll_view);

        LinearLayout ll_title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
        ll_title_view.setBackgroundColor(gp.themeColorList.title_background_color);

        final TextView tv_title=(TextView)dialog.findViewById(R.id.password_input_title);
        tv_title.setTextColor(gp.themeColorList.title_text_color);
        tv_title.setText(mActivity.getString(R.string.msgs_security_application_password_auth_title));
        final TextView tv_msg=(TextView)dialog.findViewById(R.id.password_input_msg);
        tv_msg.setText(mActivity.getString(R.string.msgs_security_application_password_auth_specify_password));
        final CheckedTextView ctv_prot=(CheckedTextView)dialog.findViewById(R.id.password_input_ctv_protect);
        ctv_prot.setVisibility(CheckedTextView.GONE);
        final EditText et_pswd1=(EditText)dialog.findViewById(R.id.password_input_password);
        final EditText et_pswd2=(EditText)dialog.findViewById(R.id.password_input_password_confirm);
        et_pswd2.setVisibility(EditText.GONE);

        final TextView tv_warn=(TextView)dialog.findViewById(R.id.password_input_warning_msg);
        tv_warn.setVisibility(TextView.VISIBLE);
        tv_warn.setText(mActivity.getString(R.string.msgs_security_application_password_create_forget_recovery));

        final Button btn_ok=(Button)dialog.findViewById(R.id.password_input_ok_btn);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.password_input_cancel_btn);

        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);

        et_pswd1.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (et_pswd1.length()>0) CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                else CommonDialog.setViewEnabled(mActivity, btn_ok, false);
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (notify_check!=null) notify_check.notifyToListener(false,null);
                dialog.dismiss();
            }
        });

        CommonDialog.setViewEnabled(mActivity, btn_ok, false);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonDialog.setViewEnabled(mActivity, btn_ok, false);
                final Handler hndl=new Handler();
                String decrypted_hv="";
                String input_hv="";
                try {
//                    input_hv= EncryptUtilV3.makeSHA256Hash(et_pswd1.getText().toString());
                    input_hv=getStretchedPassword(et_pswd1.getText().toString());
                    SecretKey enc_key= KeyStoreUtils.getStoredKey(mActivity, KeyStoreUtils.KEY_STORE_ALIAS);

                    EncryptUtilV3.CipherParms cp_int = EncryptUtilV3.initCipherEnv(enc_key, KeyStoreUtils.KEY_STORE_ALIAS);
                    byte[] encrypted_hv= Base64Compat.decode(gp.settingSecurityApplicationPasswordHashValue, Base64Compat.NO_WRAP);
                    decrypted_hv =EncryptUtilV3.decrypt(encrypted_hv, cp_int);
                } catch (Exception e) {
                    e.printStackTrace();
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.flush();
                    pw.close();

                    mUtil.showCommonDialog(false, "E", mActivity.getString(R.string.msgs_security_application_password_authentaication_error), sw.toString(), null);
                    mUtil.addLogMsg("E","", mActivity.getString(R.string.msgs_security_application_password_authentaication_error));
                    mUtil.addLogMsg("E","", sw.toString());
                }
                if (decrypted_hv!=null && decrypted_hv.equals(input_hv)) {
                    if (resource_id==ApplicationPassword.APPLICATION_PASSWORD_RESOURCE_START_APPLICATION) {
                        gp.appPasswordAuthValidated=false;
                        gp.appPasswordAuthLastTime= 0L;
                    } else {
                        gp.appPasswordAuthValidated=true;
                        gp.appPasswordAuthLastTime= System.currentTimeMillis();
                    }
                    dialog.dismiss();
                    if (notify_check!=null) notify_check.notifyToListener(true,null);
                } else {
                    tv_msg.setText(mActivity.getString(R.string.msgs_security_application_password_auth_wrong_password_specified));
                    hndl.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            CommonDialog.setViewEnabled(mActivity, btn_ok, true);
                        }
                    },1000);
                }
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_cancel.performClick();
            }
        });

        dialog.show();
    }

    static public void createPassword(final GlobalParameters gp, final Activity mActivity, final FragmentManager fm,
                                      final CommonUtilities mUtil, final String title, final NotifyEvent ntfy_create) {
        final Dialog dialog = new Dialog(mActivity, gp.applicationTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(R.layout.password_input_dlg);

        LinearLayout ll_view = (LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
        CommonUtilities.setDialogBoxOutline(mActivity, ll_view);

        LinearLayout ll_title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
        ll_title_view.setBackgroundColor(gp.themeColorList.title_background_color);

        final TextView tv_title=(TextView)dialog.findViewById(R.id.password_input_title);
        tv_title.setTextColor(gp.themeColorList.title_text_color);
        tv_title.setText(title);
        final TextView tv_msg=(TextView)dialog.findViewById(R.id.password_input_msg);
        tv_msg.setText(mActivity.getString(R.string.msgs_security_application_password_auth_specify_password));
        final CheckedTextView ctv_prot=(CheckedTextView)dialog.findViewById(R.id.password_input_ctv_protect);
        ctv_prot.setVisibility(CheckedTextView.GONE);
        final EditText et_pswd1=(EditText)dialog.findViewById(R.id.password_input_password);
        final EditText et_pswd2=(EditText)dialog.findViewById(R.id.password_input_password_confirm);

        final TextView tv_warn=(TextView)dialog.findViewById(R.id.password_input_warning_msg);
        tv_warn.setVisibility(TextView.VISIBLE);
        tv_warn.setText(mActivity.getString(R.string.msgs_security_application_password_create_forget_warning));

        final Button btn_ok=(Button)dialog.findViewById(R.id.password_input_ok_btn);
        final Button btn_cancel=(Button)dialog.findViewById(R.id.password_input_cancel_btn);

        CommonDialog.setDlgBoxSizeCompactWithInput(dialog);

//        tv_title.setText("Application startup password");

        et_pswd1.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkNewPasswordSpecification(mActivity, gp, btn_ok, et_pswd1, et_pswd2, tv_msg);
            }
        });
        et_pswd2.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkNewPasswordSpecification(mActivity, gp, btn_ok, et_pswd1, et_pswd2, tv_msg);
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ntfy_create.notifyToListener(false, null);
                dialog.dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String encrypted_hv="";
                try {
//                    String user_pw_hv=EncryptUtilV3.makeSHA256Hash(et_pswd1.getText().toString());
                    String user_pw_hv=getStretchedPassword(et_pswd1.getText().toString());
                    SecretKey dec_key= KeyStoreUtils.getStoredKey(mActivity, KeyStoreUtils.KEY_STORE_ALIAS);
                    EncryptUtilV3.CipherParms cp_int = EncryptUtilV3.initCipherEnv(dec_key, KeyStoreUtils.KEY_STORE_ALIAS);
                    encrypted_hv=Base64Compat.encodeToString(EncryptUtilV3.encrypt(user_pw_hv, cp_int), Base64Compat.NO_WRAP);
                    ntfy_create.notifyToListener(true, new Object[]{encrypted_hv});
                } catch (Exception e) {
                    e.printStackTrace();
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.flush();
                    pw.close();

                    mUtil.showCommonDialog(false,"E",mActivity.getString(R.string.msgs_security_application_password_creation_error),sw.toString(), null);
                    mUtil.addLogMsg("E","",mActivity.getString(R.string.msgs_security_application_password_creation_error));
                    mUtil.addLogMsg("E","",sw.toString());

                    ntfy_create.notifyToListener(false, null);
                }
                dialog.dismiss();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_cancel.performClick();
            }
        });

        dialog.show();

    }

    private static final int STRETCH_COUNT=1000;
    private static String getStretchedPassword(String password) {
        String salt = null;
        String hash = null;
        try {
            salt = EncryptUtilV3.makeSHA256Hash(password);
            for (int i = 0; i < STRETCH_COUNT; i++) {
                hash = EncryptUtilV3.makeSHA256Hash(hash + salt + password);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;
    }

    static private void checkNewPasswordSpecification(Context c, GlobalParameters gp, Button btn_ok, EditText et_pswd1, EditText et_pswd2, TextView msg) {
        btn_ok.setEnabled(false);
        if (et_pswd1.getText().length()>=4) {
            if (et_pswd1.getText().length()>0 && et_pswd2.getText().length()>0) {
                if (et_pswd1.getText().toString().equals(et_pswd2.getText().toString())) {
                    btn_ok.setEnabled(true);
                    msg.setText("");
//                    msg.setText(c.getString(R.string.msgs_password_input_preference_match));
                } else {
                    msg.setText(c.getString(R.string.msgs_password_input_preference_unmatch));
                }
            } else {
                if (et_pswd1.getText().length()==0) {
                    msg.setText(c.getString(R.string.msgs_password_input_preference_new_not_specified));
                } else {
                    msg.setText(c.getString(R.string.msgs_password_input_preference_conf_not_specified));
                }
            }
        } else {
            msg.setText(c.getString(R.string.msgs_security_application_password_create_min_length_is_4_digit));
        }
    }

}
