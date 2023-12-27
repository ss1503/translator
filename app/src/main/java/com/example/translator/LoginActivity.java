package com.example.translator;

import static com.example.translator.FBref.refUsers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.translator.FBref;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    //firebase vars
    private FirebaseAuth mAuth;

    //component vars
    private EditText email;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //init firebase reference
        mAuth = FirebaseAuth.getInstance();

        //init component vars
        email = (EditText) findViewById(R.id.UsernameEdt);
        password = (EditText) findViewById(R.id.PasswordEdt);

    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and move to main activity
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null)
        {
            Intent sourceIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(sourceIntent);
        }
    }


    public void signIn(View view)
    {
        String emailVal = email.getText().toString();
        String passwordVal = password.getText().toString();

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

    public void register(View view)
    {
        String emailVal = email.getText().toString();
        String passwordVal = password.getText().toString();

        mAuth.createUserWithEmailAndPassword(emailVal, passwordVal)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, move to main activity
                            Toast.makeText(LoginActivity.this, "Register successful", Toast.LENGTH_SHORT).show();

                            //create new user
                            String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            Users user = new Users(userId);

                            //create new user in firebase database
                            refUsers.child(userId).setValue(user);

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}