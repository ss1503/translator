package com.example.translator;

import static com.example.translator.FBref.refUsers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    //firebase vars
    FirebaseAuth mAuth;

    //components vars
    EditText nameEdt, emailEdt, passwordEdt;

    Intent gi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_register);

        gi = getIntent();

        //init firebase reference
        mAuth = FirebaseAuth.getInstance();

        //init vars
        nameEdt = (EditText) findViewById(R.id.nameEdt);
        emailEdt = (EditText) findViewById(R.id.emailEdt);
        passwordEdt = (EditText) findViewById(R.id.passwordEdt);

    }

    public void finish_register(View view)
    {
        String nameVal = nameEdt.getText().toString();
        String emailVal = emailEdt.getText().toString();
        String passwordVal = passwordEdt.getText().toString();

        if(!emailVal.equals("") && !passwordVal.equals("") && !nameVal.equals(""))
        {
            mAuth.createUserWithEmailAndPassword(emailVal, passwordVal).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful())
                    {
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        Users user = new Users(userId, nameVal);

                        //create new user in firebase database
                        refUsers.child(userId).setValue(user);

                        Toast.makeText(RegisterActivity.this, "Register successful", Toast.LENGTH_SHORT).show();

                        FirebaseAuth.getInstance().signOut();

                        gi.putExtra("email", emailVal);
                        gi.putExtra("password", passwordVal);
                        setResult(RESULT_OK, gi);
                        finish();
                    }
                    else {
                        Toast.makeText(RegisterActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
        }

    }
}