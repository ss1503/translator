package com.example.translator;

import static com.example.translator.FBref.refUsers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.translator.FBref;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    //Firebase
    FirebaseAuth mauth;
    Intent gi;

    //vars
    ArrayList<String> imagesNames;
    ArrayList<TextTranslate> picturesScanned;
    ArrayAdapter<String> adp;
    String picChosen = "";

    //components
    ListView lv;
    Button confirmBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_history);

        //init firebase and intent
        gi = getIntent();
        mauth = FirebaseAuth.getInstance();

        //init components
        lv = (ListView) findViewById(R.id.picNamesLv);
        confirmBtn = (Button) findViewById(R.id.confirmBtn);

        //init vars
        imagesNames = new ArrayList<>();
        picturesScanned = new ArrayList<>();

        //setting up the list view
        getUsersPicturesNames();
        lv.setOnItemClickListener(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }

    private synchronized void getUsersPicturesNames() {
        String userId = Objects.requireNonNull(mauth.getCurrentUser()).getUid();

        refUsers.child(userId).child("translate").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //getting the dates of the pictures into the array to display in the list view
                for (DataSnapshot data : snapshot.getChildren()) {
                    picturesScanned.add(data.getValue(TextTranslate.class));
                    String resDate = formatDate(picturesScanned.get(picturesScanned.size() - 1).getDate());

                    imagesNames.add(resDate);
                }

                Toast.makeText(HistoryActivity.this, imagesNames.get(0), Toast.LENGTH_SHORT).show();

                adp = new ArrayAdapter<String>(HistoryActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, imagesNames);
                lv.setAdapter(adp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String formatDate(String date)
    {

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd.mm.yyyy-hh:mm:ss");

        try {
            // Parse the input date string into a Date object
            Date inputDate = inputDateFormat.parse(date);

            // Format the Date object into the desired output format
            String outputDateStr = outputDateFormat.format(inputDate);

            return outputDateStr;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        picChosen = imagesNames.get(position);
        confirmBtn.setEnabled(true);
    }

    public void confirmPic(View view)
    {
        gi.putExtra("picChosen", picChosen);
        setResult(RESULT_OK, gi);
        finish();
    }
}