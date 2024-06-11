package com.example.translator;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static com.example.translator.FBref.FBST;
import static com.example.translator.FBref.refUsers;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ActionMenuView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.example.translator.Users;
import com.example.translator.LoginActivity;


import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {

    //consts
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 101;
    private static final int GET_PIC_FROM_HISTORY_REQUEST_CODE = 1;
    private static final int REQUEST_OVERLAY_PERMISSION_REQUEST_CODE = 2;
    //private static final int ALARM_REQUEST_CODE = 1001;
    //private static final long INTERVAL_MS = 1 * 60 * 1000L; // 1 minutes



    //components vars
    private ImageView iv;
    private TextView translatedTv;
    private Spinner fromSpinner, toSpinner;
    private TextInputEditText sourceEdt;

    //global vars
    private String currentPath;
    private Bitmap bitmap;
    private FirebaseAuth mAuth;


    //Google ML kit
    private TextRecognizer textRecognizer;

    String[] LanguagesList = {"Select", "English", "Afrikaans", "Arabic", "Belarusian", "Bulgarian", "Bengali", "Catalan", "Czech", "Spanish", "Hindi", "Hebrew"};

    String fromLanCode, toLanCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        //init components
        iv = (ImageView) findViewById(R.id.imageResultIV);
        translatedTv = (TextView) findViewById(R.id.idTranslatedTv);
        sourceEdt = (TextInputEditText) findViewById(R.id.idEditSource);
        fromSpinner = (Spinner) findViewById(R.id.idFromSpinner);
        toSpinner = (Spinner) findViewById(R.id.idToSpinner);

        //start service and ask for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION_REQUEST_CODE);
        }
        else
        {
            startSecretService(); //start foregroud service and alarm manager
        }

        //ask for camera permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_CAPTURE_REQUEST_CODE);
        }

        //init text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //making spinner adapters
        ArrayAdapter Adapter = new ArrayAdapter(this, R.layout.spinner_item, LanguagesList);
        Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(Adapter);
        toSpinner.setAdapter(Adapter);

        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromLanCode = getLanCode(LanguagesList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toLanCode = getLanCode(LanguagesList[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void startSecretService()
    {
        Intent serviceIntent = new Intent(this, secretService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(serviceIntent);
    }


    public String getLanCode(String language)
    {
        String result = "";
        switch (language)
        {
            case "English":
                result = TranslateLanguage.ENGLISH;
                break;
            case "Afrikaans":
                result = TranslateLanguage.AFRIKAANS;
                break;
            case "Arabic":
                result = TranslateLanguage.ARABIC;
                break;
            case "Belarusian":
                result = TranslateLanguage.BELARUSIAN;
                break;
            case "Bulgarian":
                result = TranslateLanguage.BULGARIAN;
                break;
            case "Bengali":
                result = TranslateLanguage.BENGALI;
                break;
            case "Catalan":
                result = TranslateLanguage.CATALAN;
                break;
            case "Czech":
                result = TranslateLanguage.CZECH;
                break;
            case "Spanish":
                result = TranslateLanguage.SPANISH;
                break;
            case "Hindi":
                result = TranslateLanguage.HINDI;
                break;
            case "Hebrew":
                result = TranslateLanguage.HEBREW;
                break;
            default:
                result = "Error";
        }

        return result;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK)
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            bitmap = BitmapFactory.decodeFile(currentPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

            byte bytesOfImage[] = bytes.toByteArray();

            iv.setImageBitmap(bitmap);

            //update storage and data base
            uploadImageToStorage(bytesOfImage);

            //recognize text from image
            recognizeText();
        }
        else if(requestCode == GET_PIC_FROM_HISTORY_REQUEST_CODE)
        {
            if(data != null)
            {
                //get the picture date in right format
                String pictureChosen = data.getStringExtra("picChosen");
                getHistoryPicChosen(pictureChosen);
            }
        }
        else if(requestCode == REQUEST_OVERLAY_PERMISSION_REQUEST_CODE)
        {
            Intent serviceIntent = new Intent(this, secretService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(serviceIntent);
        }
    }

    private void getHistoryPicChosen(String picDate)
    {
        final ProgressDialog pd;
        final long MAX_BYTES = 4096 * 4096;
        String id = mAuth.getCurrentUser().getUid();

        pd = ProgressDialog.show(this, "Downloading image", "Downloading...", true);

        String path = "scan_images/" + id + "/image_" + picDate + ".jpg";
        StorageReference storageReference = FBST.getReference().child(path);

        //get he image from firebase storage
        storageReference.getBytes(MAX_BYTES).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                //succeeded downloading image
                pd.dismiss();

                //convert byte to bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                iv.setImageBitmap(bitmap);

                Toast.makeText(MainActivity.this, "downloaded image successfully", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Failed to download image", Toast.LENGTH_SHORT).show();
            }
        });
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

    private void uploadImageToStorage(byte[] imgBytes)
    {
        final ProgressDialog pd;

        //saving image in firebase storage using date format
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
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
                    TextTranslate t = new TextTranslate(date);

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

    private void recognizeText()
    {
        if(iv.getDrawable() == null)
        {
            Toast.makeText(this, "No image detected", Toast.LENGTH_SHORT).show();
        }
        else
        {
            iv.setDrawingCacheEnabled(true);
            Bitmap tempBitmap = Bitmap.createBitmap(iv.getDrawingCache());
            iv.setDrawingCacheEnabled(false);

            InputImage inputImage = InputImage.fromBitmap(tempBitmap, 90); //maybe later need to change the degrees here
            Task<Text> textTaskResult = textRecognizer.process(inputImage).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    //get the recognized text into string
                    String recognizedText = text.getText();
                    sourceEdt.setText(recognizedText);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "Failed recognizing text because" + e, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void translate(View view)
    {
        final ProgressDialog pd;

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLanCode)
                .setTargetLanguage(toLanCode)
                .build();

        Translator translator = Translation.getClient(options);
        getLifecycle().addObserver(translator); //delete translator object after stopped using


        //showing the user a PD while downloading The model
        pd = ProgressDialog.show(this, "Downloading language model", "downloading...", true);

        //check if the model has been downloaded
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Downloaded model successfully", Toast.LENGTH_SHORT).show();

                //translate text
                String text = sourceEdt.getText().toString();
                translator.translate(text).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {

                        //showing text on Text view result
                        translatedTv.setVisibility(View.VISIBLE);
                        translatedTv.setText(s);
                        Toast.makeText(MainActivity.this, "Translated text successfully", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to translate text", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Downloaded model successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        CharSequence title = item.getTitle();
        if (title.equals("History")) {
            Intent srcIntent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivityForResult(srcIntent, GET_PIC_FROM_HISTORY_REQUEST_CODE);

        } else if (title.equals("Sign out")) {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences settings = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("stayConnect", false);
            editor.commit();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void recognize(View view)
    {
        recognizeText();
    }
}
