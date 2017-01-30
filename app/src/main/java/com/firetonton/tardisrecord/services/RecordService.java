package com.firetonton.tardisrecord.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.firetonton.tardisrecord.helpers.CircleAudioRecorder;
import com.firetonton.tardisrecord.MainActivity;
import com.firetonton.tardisrecord.fragments.RecordFragment;
import com.firetonton.tardisrecord.R;
import com.firetonton.tardisrecord.SettingsActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by bertho_y on 1/11/17.
 */

public class RecordService extends Service {

    public static final int MAIN_NOTIFICATION_ID = 42;

    public interface ACTIONS {
        String SAVE_1_MIN = "action_save_1_min";
        String SAVE_5_MIN = "action_save_5_min";
        String SAVE_15_MIN = "action_save_15_min";
    }

    private static final String LOG_TAG = "RecordService";
    private static final String TAG = LOG_TAG;

    private CircleAudioRecorder mRecorder = null;
    private int mRecordSource = MediaRecorder.AudioSource.MIC;

    final Messenger messenger = new Messenger(new ServiceHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        boolean doStartup = true;
        Log.d(TAG, "onStartCommand: "+startId);
        if (intent != null) {
            mRecordSource = intent.getIntExtra(SettingsActivity.KEY_PREF_AUDIO_SOURCE,
                    MediaRecorder.AudioSource.MIC);
            if (intent.getAction() != null) {
                Log.d(TAG, "intent action: " + intent.getAction());
                try {
                    // Save asked duration
                    switch (intent.getAction()) {
                        case ACTIONS.SAVE_1_MIN:
                            this.extractNSeconds(60);
                            doStartup = false;
                            break;
                        case ACTIONS.SAVE_5_MIN:
                            this.extractNSeconds(5 * 60);
                            doStartup = false;
                            break;
                        case ACTIONS.SAVE_15_MIN:
                            this.extractNSeconds(15 * 60);
                            doStartup = false;
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (doStartup) {
            this.pushRecordingNotification();
            this.startRecording();
        }

        return START_STICKY;
    }

    public void pushSavingNotification() {
        Intent notificationIntent = new Intent(this, RecordFragment.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        final Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("TARDIS Record")
                .setTicker("Saving in progress ...")
                .setContentText("Saving in progress ...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .setColor(0xff0000)
                .build();

        startForeground(MAIN_NOTIFICATION_ID, notification);
    }

    public void pushRecordingNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent save1MinIntent = new Intent(this, RecordService.class);
        save1MinIntent.setAction(ACTIONS.SAVE_1_MIN);
        PendingIntent pSave1MinIntent = PendingIntent.getService(this, 0, save1MinIntent, 0);

        Intent save5MinIntent = new Intent(this, RecordService.class);
        save5MinIntent.setAction(ACTIONS.SAVE_5_MIN);
        PendingIntent pSave5MinIntent = PendingIntent.getService(this, 0, save5MinIntent, 0);

        Intent save15MinIntent = new Intent(this, RecordService.class);
        save15MinIntent.setAction(ACTIONS.SAVE_15_MIN);
        PendingIntent pSave15MinIntent = PendingIntent.getService(this, 0, save15MinIntent, 0);

        final Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("TARDIS Record")
                .setTicker("Recording ...")
                .setContentText("Recording ...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_access_time_black_24dp, "1m", pSave1MinIntent)
                .addAction(R.drawable.ic_access_time_black_24dp, "5m", pSave5MinIntent)
                .addAction(R.drawable.ic_access_time_black_24dp, "15m", pSave15MinIntent)
                .setColor(0xff0000)
                .build();

        startForeground(MAIN_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopRecording();
        Log.d(TAG, "onDestroy: ");
        stopSelf();
    }


    private String getFilePath(String suffix) {
        String str = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "TardisRecord";
        long time = System.currentTimeMillis();

        File folder = new File(str);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (!success) {
            return null; // TODO something else
        }

        if (suffix != null)
            suffix = "-"+suffix;
        else
            suffix = "";
        return str
                + File.separator
                + "record-"+time+suffix+".wav";
    }

    private File[] getFileList() {
        String str = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "TardisRecord";
        File folder = new File(str);
        File files[] = folder.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.startsWith("record-");
                    }
                }
        );
        Arrays.sort(files,
                new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        // Sort reverse order
                        return rhs.getName().compareTo(lhs.getName());
                    }
                });

        return files;
    }



    private void startRecording() {
        if (mRecorder != null) {
            return ;
        }

        this.stopRecording();

        mRecorder = new CircleAudioRecorder();
        mRecorder.setAudioSource(mRecordSource);
        mRecorder.setProgressListener(new CircleAudioRecorder.ProgressListener() {
            @Override
            public void onProgress(long bufferedRecordDuration, long totalRecordDuration) {
                Log.d(TAG, "total duration: "+totalRecordDuration);
                Intent msg = new Intent("plop");
                msg.putExtra(Intent.EXTRA_TEXT, bufferedRecordDuration);
                sendBroadcast(msg);
            }
        });
        mRecorder.startRecord();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stopRecord();
            mRecorder = null;
        }
    }

    private void extractNSeconds(int nb_seconds) throws IOException {
        pushSavingNotification();
        try {
            mRecorder.writeLastNSecondsToFile(nb_seconds, getFilePath("" + nb_seconds));
        } catch (IOException e) {
            pushRecordingNotification();
            throw e;
        }
        pushRecordingNotification();
    }



    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handle message "+msg.what);
            switch (msg.what) {
                case 42:
                    try {
                        extractNSeconds(msg.arg1);
                    } catch (IOException e) {
                        // TODO exception management
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "TODO handle exception !!", Toast.LENGTH_SHORT).show();
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
