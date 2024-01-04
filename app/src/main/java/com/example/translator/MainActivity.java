package com.example.translator;

import static com.example.translator.FBref.FBST;
import static com.example.translator.FBref.refUsers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ActionMenuView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.example.translator.Users;
import com.example.translator.LoginActivity;

public class MainActivity extends AppCompatActivity {

    //consts
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 101;


    //components vars
    private ImageView iv;

    //global vars
    private String currentPath;
    private Bitmap bitmap;
    private FirebaseAuth mAuth;
    int rotate = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        //init components
        iv = (ImageView) findViewById(R.id.resultIv);


        //ask for permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_CAPTURE_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                //permission granted
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();

            }else {
                //permission denied

                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void signOut(View view)
    {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences settings = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("stayConnect", false);
        editor.commit();
        finish();
    }

    public void capture(View view) throws IOException {
        try {
            //creating temp local file for storing the picture
            String fileName = "tempfile";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imgFile = File.createTempFile(fileName, ".jpg", storageDir);
            currentPath = imgFile.getAbsolutePath();
            Uri imgUri = FileProvider.getUriForFile(MainActivity.this, "com.example.translator.fileprovider", imgFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);

            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);


        }catch (IOException e)
        {
            Toast.makeText(this, "Failed to create temp file", Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK)
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            bitmap = BitmapFactory.decodeFile(currentPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

            byte bytesOfImage[] = bytes.toByteArray();

            iv.setImageBitmap(bitmap);


            uploadImageToStorage(bytesOfImage);
        }
    }


    private void uploadImageToStorage(byte[] imgBytes)
    {
        final ProgressDialog pd;

        //saving image in firebase storage using date format
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String resDate = dateFormat.format(date);
        String id = mAuth.getCurrentUser().getUid();
        String storagePath = "scan_images/"+ id + "/image_" + resDate + ".jpg";

        StorageReference storageReference = FBST.getReference().child(storagePath);

        pd = ProgressDialog.show(this, "Upload image", "uploading...", true);

        storageReference.putBytes(imgBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Image failed to upload because " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        //update database
        updateDatabase(resDate);
    }

    private void updateDatabase(String date)
    {
        String id = mAuth.getCurrentUser().getUid();

        //getting current user database info
        refUsers.child(id).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task)
            {
                if(task.isSuccessful() && task.getResult().exists())
                {
                    //get current info
                    DataSnapshot data = task.getResult();
                    Users userTemp = data.getValue(Users.class);

                    //update database
                    TextTranslate t = new TextTranslate(date, "null", "null");

                    if(userTemp.getTranslate() == null)
                    {
                        userTemp.resetTranslate();
                    }

                    userTemp.addTranslate(t);
                    refUsers.child(id).setValue(userTemp);
                }
            }
        });
    }
}






















