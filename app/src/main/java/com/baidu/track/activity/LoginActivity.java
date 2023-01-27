package com.baidu.track.activity;

import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.utils.SharedPreferenceUtil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
public class LoginActivity extends BaseActivity {

    private static final String SP_ENTITY_NAME_KEY = "login_entityname";

    private TrackApplication trackApp = null;

    private EditText mEntityName;
    private Button mLogin;

    @Override
    protected int getContentViewId() {
        return R.layout.activity_login;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackApp = (TrackApplication) getApplicationContext();
        mEntityName = findViewById(R.id.entityName);
        mLogin = findViewById(R.id.login);
        mEntityName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                EditText textView = (EditText) view;
                String hintStr = textView.getHint().toString();
                if (hasFocus) {
                    textView.setHint("");
                } else {
                    textView.setHint(hintStr);
                }
            }
        });
        mEntityName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() >= 6) {
                    mLogin.setEnabled(true);
                    mLogin.setBackground(getResources()
                            .getDrawable(R.drawable.shape_rect_round_btn_enabled_bkg));
                } else {
                    mLogin.setEnabled(false);
                    mLogin.setBackground(getResources()
                            .getDrawable(R.drawable.shape_rect_round_btn_unenabled_bkg));
                }
                if (s != null && s.length() == 0) {
                    mEntityName.setHint("EntityName(6位字母或数字)");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        initData();
    }

    private void initData() {
        String entityName =
                SharedPreferenceUtil.getString(LoginActivity.this, SP_ENTITY_NAME_KEY, "");
        if (!TextUtils.isEmpty(entityName)) {
            mEntityName.setText(entityName);
        }
    }

    public void onLogin(View v) {
        String entityName = mEntityName.getText().toString();
        if (TextUtils.isEmpty(entityName) || entityName.length() < 6) {
            Toast.makeText(LoginActivity.this, "entityName为6为数字或字母",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferenceUtil.putString(LoginActivity.this, SP_ENTITY_NAME_KEY, entityName);
        trackApp.mTrace.setEntityName(entityName);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }

}
