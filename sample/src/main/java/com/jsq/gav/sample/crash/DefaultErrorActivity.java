/*
 * Copyright 2015 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jsq.gav.sample.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jsq.gav.sample.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DefaultErrorActivity extends Activity {

    private final String PASSWORD = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());

    private int mClickTotalCount = 10;
    private int mClickCount = 0;
    private long mClickInterval = 300;
    private long mPreClickTime = 0;

    //ERROR DETAILS 按钮
    private Button mMoreInfoButton;
    //重新启动 按钮
    private Button mRestartButton;
    //小斑马图片
    private ImageView mErrorImageView;
    //输入密码dialog
    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.customactivityoncrash_default_error_activity);

        //Close/restart button logic:
        //If a class if set, use restart.
        //Else, use close and just finish the app.
        //It is recommended that you follow this logic if implementing a custom error activity.
        mRestartButton = (Button) findViewById(R.id.customactivityoncrash_error_activity_restart_button);

        final Class<? extends Activity> restartActivityClass = CustomActivityOnCrash.getRestartActivityClassFromIntent(getIntent());
        final CustomActivityOnCrash.EventListener eventListener = CustomActivityOnCrash.getEventListenerFromIntent(getIntent());

        if (restartActivityClass != null) {
            mRestartButton.setText(R.string.customactivityoncrash_error_activity_restart_app);
            mRestartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DefaultErrorActivity.this, restartActivityClass);
                    CustomActivityOnCrash.restartApplicationWithIntent(DefaultErrorActivity.this, intent, eventListener);
                }
            });
        } else {
            mRestartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomActivityOnCrash.closeApplication(DefaultErrorActivity.this, eventListener);
                }
            });
        }

        mMoreInfoButton = (Button) findViewById(R.id.customactivityoncrash_error_activity_more_info_button);

        if (CustomActivityOnCrash.isShowErrorDetailsFromIntent(getIntent())) {
            mMoreInfoButton.setVisibility(View.VISIBLE);
        } else {
            mMoreInfoButton.setVisibility(View.GONE);
        }
        mMoreInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //We retrieve all the error data and show it

                AlertDialog dialog = new AlertDialog.Builder(DefaultErrorActivity.this)
                        .setTitle(R.string.customactivityoncrash_error_activity_error_details_title)
                        .setMessage(CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent()))
                        .setPositiveButton(R.string.customactivityoncrash_error_activity_error_details_close, null)
                        .setNeutralButton(R.string.customactivityoncrash_error_activity_error_details_copy,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        copyErrorToClipboard();
                                        Toast.makeText(DefaultErrorActivity.this, R.string.customactivityoncrash_error_activity_error_details_copied, Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .show();
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.customactivityoncrash_error_activity_error_details_text_size));
            }
        });

//        int defaultErrorActivityDrawableId = CustomActivityOnCrash.getDefaultErrorActivityDrawableIdFromIntent(getIntent());

        //=========================wangpan 2018/06/22===============================
        //如果"ERROR DETAILS"按钮隐藏, 则通过快速多次点击斑马图片显示(正式服查看app错误日志用)
        mErrorImageView = ((ImageView) findViewById(R.id.customactivityoncrash_error_activity_image));
        mErrorImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showErrorDetailsButton();
            }
        });

        //=========================wangpan 2018/06/22===============================

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            errorImageView.setImageDrawable(getResources().getDrawable(defaultErrorActivityDrawableId, getTheme()));
//        } else {
//            //noinspection deprecation
//            errorImageView.setImageDrawable(getResources().getDrawable(defaultErrorActivityDrawableId));
//        }
    }

    /**
     * 显示错误详情按钮
     */
    private void showErrorDetailsButton() {
        if (mMoreInfoButton.getVisibility() == View.VISIBLE) {
            return;
        }
        if (mClickCount == 0) {
            mPreClickTime = System.currentTimeMillis();
            mClickCount++;
        } else {
            long clickTime = System.currentTimeMillis();
            if (clickTime - mPreClickTime <= mClickInterval) {
                mClickCount++;
                mPreClickTime = clickTime;
            } else {
                mClickCount = 0;
                mPreClickTime = 0;
            }
        }
        if (mClickCount == mClickTotalCount) {
            mClickCount = 0;
            mPreClickTime = 0;
            //mMoreInfoButton.setVisibility(View.VISIBLE);
            showInputPwdDialog();
        }
    }

    /**
     * 显示输入密码dialog
     */
    private void showInputPwdDialog() {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.view_input_pwd, null);
        final EditText etPwd = (EditText) view.findViewById(R.id.et_pwd);
        //输入框左右晃动动画
        final ObjectAnimator tranAnim = ObjectAnimator.ofFloat(etPwd, "translationX", 0, 20, 0);
        tranAnim.setDuration(100);
        tranAnim.setRepeatCount(5);
        tranAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                etPwd.setFocusable(false);
                etPwd.setFocusableInTouchMode(false);
                etPwd.clearFocus();
                //禁止输入
                etPwd.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                });
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                etPwd.setFocusable(true);
                etPwd.setFocusableInTouchMode(true);
                etPwd.requestFocus();
                etPwd.findFocus();
                //还原
                etPwd.setOnTouchListener(null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        Button btnConfirm = (Button) view.findViewById(R.id.btn_confirm);
        builder.setView(view);
        mAlertDialog = builder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = etPwd.getText().toString().trim();
                if (TextUtils.isEmpty(pwd)) {
                    tranAnim.start();
                    Toast.makeText(DefaultErrorActivity.this, "清先输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (PASSWORD.equals(pwd)) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                    mMoreInfoButton.setVisibility(View.VISIBLE);
                } else {
                    etPwd.setText("");
                    tranAnim.start();
                    Toast.makeText(DefaultErrorActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mAlertDialog.show();*/
    }

    private void copyErrorToClipboard() {
        String errorInformation =
                CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.customactivityoncrash_error_activity_error_details_clipboard_label), errorInformation);
            clipboard.setPrimaryClip(clip);
        } else {
            //noinspection deprecation
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(errorInformation);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //界面销毁的时候关闭对话框
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
}
