package com.firetonton.tardisrecord.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firetonton.tardisrecord.helpers.IFabClickable;
import com.firetonton.tardisrecord.R;
import com.firetonton.tardisrecord.SettingsActivity;

import com.firetonton.tardisrecord.services.RecordService;

public class RecordFragment extends Fragment
        implements IFabClickable {

    private static final int MAX_SAVABLE_DURATION = 15 * 60;
    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;

    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
    int mTest;

    BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long nb = intent.getLongExtra(Intent.EXTRA_TEXT, -1);
            Log.d("onReceive", "receiving");
            updateProgression(nb);
        }
    };

    private void updateProgression(long nb) {
        ProgressBar progressBar = (ProgressBar) super.getActivity().findViewById(R.id.progressBar);
        int maxProgress = progressBar.getMax();

        if (nb > maxProgress)
            nb = maxProgress;

        TextView text = (TextView) super.getActivity().findViewById(R.id.textViewDuration);
        if (nb > 0)
            text.setText(""+(nb / 60)+"'"+(nb % 60)+"\"");
        else
            text.setText("--'--\"");

        progressBar.setProgress((int) nb);
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout llLayout = (RelativeLayout) inflater.inflate(R.layout.content_main, container, false);
        mTest = 42;

        Button button_one_min = (Button) llLayout.findViewById(R.id.button);
        button_one_min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBound) return;

                // Create and send a message to the service, using a supported 'what' value
                Message msg = Message.obtain(null, 42, 60, 0); // TODO what
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Snackbar.make(v, "Sauvegarde d'1m", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button button_two_min = (Button) llLayout.findViewById(R.id.button2);
        button_two_min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBound) return;

                // Create and send a message to the service, using a supported 'what' value
                Message msg = Message.obtain(null, 42, 3 * 60, 0); // TODO what
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Button button_three_min = (Button) llLayout.findViewById(R.id.button3);
        button_three_min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBound) return;

                // Create and send a message to the service, using a supported 'what' value
                Message msg = Message.obtain(null, 42, 5 * 60, 0); // TODO what
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Button button_stop = (Button) llLayout.findViewById(R.id.buttonStop);
        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doStopService();
            }
        });


        ProgressBar progressBar = (ProgressBar) llLayout.findViewById(R.id.progressBar);
        progressBar.setMax(MAX_SAVABLE_DURATION);

        return llLayout;
    }

    public void onFabClick(View view) {
                Snackbar.make(view, "Enregistrement démarré", Snackbar.LENGTH_LONG)
                        .show();
                // Check for permissions
                int permission = ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission == PackageManager.PERMISSION_GRANTED) {
                    permission = ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.RECORD_AUDIO);
                }

                // If we don't have permissions, ask user for permissions
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    String[] PERMISSIONS_STORAGE = {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    };
                    int REQUEST_EXTERNAL_STORAGE = 1;

                    ActivityCompat.requestPermissions(
                            getActivity(),
                            PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE
                    );
                }
                doStartService();
    }

    @Override
    public int getDrawableID() {
        return R.drawable.ic_fiber_manual_record_white_24dp;
    }

    public void onResume() {
        super.onResume();
        updateProgression(0);
        IntentFilter intentFilter = new IntentFilter("plop");
        getActivity().registerReceiver(mServiceReceiver, intentFilter);
    }

    public void onPause() {
        super.onPause();
        if (mServiceReceiver != null)
            getActivity().unregisterReceiver(mServiceReceiver);
    }

    private void doStartService() {
        Intent recordService = new Intent(getActivity(), RecordService.class);
        recordService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext());

        // Preference is stored as string ...
        int audioSourcePref = Integer.parseInt(
                sharedPref.getString(SettingsActivity.KEY_PREF_AUDIO_SOURCE,
                        Integer.toString(MediaRecorder.AudioSource.MIC)
                )
        );
        recordService.putExtra(SettingsActivity.KEY_PREF_AUDIO_SOURCE, audioSourcePref);
        getActivity().startService(recordService);
        if (!mBound)
            getActivity().bindService(new Intent(getActivity(), RecordService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
    }

    private void doStopService() {
        Intent recordService = new Intent(getActivity(), RecordService.class);
        recordService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        getActivity().stopService(recordService);
        updateProgression(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main, menu);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
