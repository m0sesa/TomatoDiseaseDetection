package com.lola.tomatodiseasedetection;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    boolean isLoggedIn;
    String user;

    SharedPreferences sharedPreferences;

    ActionBar actionBar;
    TextInputEditText usernameTxtInEditTxt, passwordTxtInLayEditTxt;
    Button login;
    ProgressDialog loginProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // hide actionbar
        actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.hide();


        // set up sharedPreference and get stored values
        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        user = sharedPreferences.getString(getString(R.string.username), "");
        isLoggedIn = sharedPreferences.getBoolean(getString(R.string.login_status), false);


        if (isLoggedIn) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra(getString(R.string.username), user);
            startActivity(intent);
            finish();
        }

        usernameTxtInEditTxt = findViewById(R.id.username);
        usernameTxtInEditTxt.setText(user);
        passwordTxtInLayEditTxt = findViewById(R.id.password);
        passwordTxtInLayEditTxt.setOnEditorActionListener((unUsed1, i, unUsed2)->{
            if (i== EditorInfo.IME_ACTION_GO){
                attemptLogin(usernameTxtInEditTxt.getText(), passwordTxtInLayEditTxt.getText());
                return true;
            }else{
                return false;
            }
        });

        login = findViewById(R.id.btn_login);
        login.setOnClickListener((v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            attemptLogin(usernameTxtInEditTxt.getText(), passwordTxtInLayEditTxt.getText());
        }));

    }

    private void showProgressDialog(){
        loginProgressDialog = new ProgressDialog(this);
        loginProgressDialog.setMessage("Logging in");
        loginProgressDialog.setCancelable(false);
        loginProgressDialog.show();
    }

    private void dismissProgressDialog(){
        loginProgressDialog.dismiss();
    }

    private void attemptLogin(Editable usernameEd, Editable passwordEd){
        showProgressDialog();

        //TODO: Login actions //this is only a placeholder code
        new Handler().postDelayed(() -> {
            dismissProgressDialog();

            String username = Objects.requireNonNull(usernameEd).toString().trim();
            String password = Objects.requireNonNull(passwordEd).toString().trim();

            if (username.equals("ADMIN") && password.equals("admin")) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                saveUserSharedPreference(username);
            } else {
                Toast.makeText(this, "Username or password incorrect", Toast.LENGTH_LONG).show();
            }

        }, 1500);
    }

    private void saveUserSharedPreference(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.username), username);
        editor.putBoolean(getString(R.string.login_status), true);
        editor.apply();
    }
}