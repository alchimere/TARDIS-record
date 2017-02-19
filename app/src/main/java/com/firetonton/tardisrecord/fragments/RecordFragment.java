package com.firetonton.tardisrecord.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firetonton.tardisrecord.R;
import com.firetonton.tardisrecord.SettingsActivity;
import com.firetonton.tardisrecord.helpers.IFabClickable;
import com.firetonton.tardisrecord.services.RecordService;
import com.triggertrap.seekarc.SeekArc;

public class RecordFragment extends Fragment
        implements IFabClickable {

    private static final int MAX_SAVABLE_DURATION = 30 * 60;
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
            RecordFragment.super.getActivity().findViewById(R.id.buttonStart)
                    .setVisibility(View.GONE);
            RecordFragment.super.getActivity().findViewById(R.id.buttonStop)
                    .setVisibility(View.VISIBLE);
        }
    };

    private void updateProgression(long nb) {
        SeekArc progressBar = (SeekArc) super.getActivity().findViewById(R.id.progressBar);
        int maxProgress = progressBar.getMax();

        if (nb > maxProgress)
            nb = maxProgress;

        TextView text = (TextView) super.getActivity().findViewById(R.id.textViewDuration);
        if (nb > 0) {
            text.setText(String.format("%02d'%02d\"", nb / 60, nb % 60)); //"" + (nb / 60) + "'" + (nb % 60) + "\"");
        }
        else
            text.setText("--'--\"");

        progressBar.setProgress((int) nb);

        SeekArc seekBar = (SeekArc) super.getActivity().findViewById(R.id.seekBar);
        if (nb <= 0)
            seekBar.setMax(1); // Set 1 because 0 is buggy
        else
            seekBar.setMax((int) nb);

        seekBar.setSweepAngle((int) progressBar.getProgressSweepAngle());
        if (nb == 0) {
            seekBar.setVisibility(View.INVISIBLE);
            super.getActivity().findViewById(R.id.textInfoHelp)
                    .setVisibility(View.INVISIBLE);
        }
        else {
            seekBar.setVisibility(View.VISIBLE);
            super.getActivity().findViewById(R.id.textInfoHelp)
                    .setVisibility(View.VISIBLE);
        }
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

        Button button_stop = (Button) llLayout.findViewById(R.id.buttonStop);
        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { doStopService(); }
        });
        Button button_start = (Button) llLayout.findViewById(R.id.buttonStart);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onStartService(v); }
        });


        SeekArc progressBar = (SeekArc) llLayout.findViewById(R.id.progressBar);
        progressBar.setMax(MAX_SAVABLE_DURATION);
        progressBar.setEnabled(false);

        SeekArc seekBar = (SeekArc) llLayout.findViewById(R.id.seekBar);
        seekBar.setMax(1); // Set 1 because 0 is buggy
        seekBar.setProgress(0);
        seekBar.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean requestedByUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(final SeekArc seekArc) {
                final int progress = seekArc.getProgress();
                if (progress > 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Capture")
                            .setMessage("Enregistrer les " + progress + " dernières secondes ?")
//                        .setIcon(android.R.drawable.ic_dialog_)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    seekArc.setProgress(0);
                                    Toast.makeText(RecordFragment.this.getActivity(), "Record " + progress + "s", Toast.LENGTH_SHORT).show();
                                    saveNSeconds(progress, RecordFragment.this.getView());
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    seekArc.setProgress(0);
                                }
                            }).show();
                }
            }
        });


        int width = progressBar.getLayoutParams().width;
//        int height = progressBar.getLayoutParams().height;
//        progressBar.setLayoutParams(new RelativeLayout.LayoutParams(width, height < width ? height : width));
//        seekBar.setLayoutParams(new RelativeLayout.LayoutParams(width, height < width ? height : width));
        progressBar.setLayoutParams(new RelativeLayout.LayoutParams(width, width));
        seekBar.setLayoutParams(new RelativeLayout.LayoutParams(width, width));

        return llLayout;
    }

    private void saveNSeconds(int nbSeconds, View v) {
        if (!mBound) {
            Log.e("SaveNSeconds", "Service not bound");
            return;
        }

        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, 42, nbSeconds, 0); // TODO what
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Snackbar.make(v, "Sauvegarde de "+nbSeconds+"s", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public void onFabClick(View view) {
        onStartService(view);
    }

    public void onStartService(View view) {
        Snackbar.make(view, "Enregistrement démarré", Snackbar.LENGTH_LONG)
                .show();
        if (checkPermissions())
            doStartService();
        else
            Snackbar.make(view, "Permission refusée", Snackbar.LENGTH_LONG)
                    .show();
    }

    private boolean checkPermissions() {
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

            return false;
        }
        return true;
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
        if (!mBound)
            getActivity().bindService(new Intent(getActivity(), RecordService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
    }

    public void onPause() {
        super.onPause();
        if (mServiceReceiver != null)
            getActivity().unregisterReceiver(mServiceReceiver);
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
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

        super.getActivity().findViewById(R.id.buttonStart).setVisibility(View.VISIBLE);
        super.getActivity().findViewById(R.id.buttonStop).setVisibility(View.GONE);
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
