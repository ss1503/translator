package com.example.translator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    //firebase vars
    private FirebaseAuth mAuth;

    //component vars
    private EditText email;
    private EditText password;
    private CheckBox rememberCb;


    //vars

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_login);

        //init firebase reference
        mAuth = FirebaseAuth.getInstance();

        //init component vars
        email = (EditText) findViewById(R.id.UsernameEdt);
        password = (EditText) findViewById(R.id.PasswordEdt);
        rememberCb = (CheckBox) findViewById(R.id.rememberCb);

    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and move to main activity
        FirebaseUser currentUser = mAuth.getCurrentUser();

        //check if user want to stay connected
        SharedPreferences settings = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        boolean isChecked = settings.getBoolean("stayConnect", false);

        if(currentUser != null && isChecked)
        {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }


    public void signIn(View view)
    {
        //save if user wants to be signed in
        if(rememberCb.isChecked())
        {
            SharedPreferences settings = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("stayConnect", true);
            editor.commit();
        }


        String emailVal = email.getText().toString();
        String passwordVal = password.getText().toString();

        if(!emailVal.equals("") && !passwordVal.equals(""))
        {
            mAuth.signInWithEmailAndPassword(emailVal, passwordVal)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Toast.makeText(LoginActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                // If sign in fails, display a message to the user.
                                Toast.makeText(LoginActivity.this, "Sign in failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        else{
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
        }
    }

    public void register(View view)
    {
        Intent si = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivityForResult(si, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null)
        {
            email.setText(data.getStringExtra("email"));
            password.setText(data.getStringExtra("password"));
        }
    }
}