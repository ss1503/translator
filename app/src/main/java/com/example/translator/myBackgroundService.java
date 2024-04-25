package com.example.translator;

import static com.example.translator.FBref.refUsers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.example.translator.FBref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class myBackgroundService extends Service
{
    private FirebaseAuth mAuth;
    private ValueEventListener valueEventListener;
    private Camera mCamera;

    //constatns
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "camera_service_channel";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e("Service", "working");
        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getUid();


        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_LOW
            );

            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentText("Service is running")
                    .setContentTitle("Service enabled")
                    .setSmallIcon(R.drawable.ic_launcher_background);

            startForeground(NOTIFICATION_ID, notification.build());
        }



        valueEventListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Users user = null;
                Log.e("Service", "serving");
                user = snapshot.getValue(Users.class); // DONT EVER DELETE THIS AGAIN

                //TODO: OK NOW CHECK THE CAMERA SERVICE CODE TO CHECK WHERE YOU NEED IN INITIALIZE THE "CAMERA" OBJECT
                Camera.PictureCallback mPicture = new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        File pictureFile = Util.getOutputMediaFile(Util.MEDIA_TYPE_IMAGE);

                        if (pictureFile == null) {
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                        } catch (IOException e) {
                        }
                    }
                };

                assert user != null;
                user.setToCapture(0);
                refUsers.child(userId).setValue(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        assert userId != null;
        refUsers.child(userId).addValueEventListener(valueEventListener);

        return super.onStartCommand(intent, flags, startId);
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
