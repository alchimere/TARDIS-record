package com.firetonton.tardisrecord.fragments;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.firetonton.tardisrecord.adapters.MyFilesRecyclerViewAdapter;
import com.firetonton.tardisrecord.R;
import com.firetonton.tardisrecord.dummy.DummyContent.DummyItem;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFilesFragmentInteractionListener}
 * interface.
 */
public class FilesFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnFilesFragmentInteractionListener mListener;
    private List<DummyItem> mItems;
    private MediaPlayer mPlayer;

    public static final int PLAYER_PROGRESS = 1;

    private Handler mProgressBarHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PLAYER_PROGRESS:
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                updatePlayerBar();

                                Message newMsg = obtainMessage(PLAYER_PROGRESS);
                                sendMessageDelayed(newMsg, 500);
                            }
                        });
                    }
                    break;
            }
        }
    };

    private void updatePlayerBar() {
        SeekBar seekBar = (SeekBar) getActivity().findViewById(R.id.playProgress);
        TextView progressText = (TextView) getActivity().findViewById(R.id.playTimestamp);

        if (mPlayer != null && mPlayer.isPlaying()) {
            int curPos = mPlayer.getCurrentPosition();
            int newPos = 0;
            if (mPlayer.getDuration() > 0)
                newPos = seekBar.getMax() * curPos / mPlayer.getDuration();
            seekBar.setProgress(newPos);
            progressText.setText(String.format("%02d:%02d", curPos / 60000, curPos / 1000 % 60));
            getActivity().findViewById(R.id.playerLayout).setVisibility(View.VISIBLE);
        }
        else {
            seekBar.setProgress(0);
            progressText.setText("--:--");
            getActivity().findViewById(R.id.playerLayout).setVisibility(View.GONE);
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilesFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FilesFragment newInstance(int columnCount) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files_list, container, false);

        SeekBar progressBar = (SeekBar) view.findViewById(R.id.playProgress);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean mTouching = false;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mTouching && mPlayer != null && mPlayer.isPlaying())
                    mPlayer.seekTo(mPlayer.getDuration() * progress / seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mTouching = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTouching = false;
            }
        });

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList(new Runnable() {
                    @Override public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        recyclerView.setAdapter(new MyFilesRecyclerViewAdapter(mItems, mListener));
                    }
                });
            }
        });


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int findVisibleElementPosition = manager.findFirstCompletelyVisibleItemPosition();
                swipeRefreshLayout.setEnabled(findVisibleElementPosition == 0);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        Context context = recyclerView.getContext();
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }

        recyclerView.setAdapter(new MyFilesRecyclerViewAdapter(new ArrayList<DummyItem>(), mListener));

        refreshList(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(false);
                recyclerView.setAdapter(new MyFilesRecyclerViewAdapter(mItems, mListener));
            }
        });
        return view;
    }

    // Refresh list and run the given runnable
    private void refreshList(final Runnable runnable) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                mItems = getFileListAsItems();
                if (runnable != null && getActivity() != null)
                    getActivity().runOnUiThread(runnable);
            }
        })).start();
    }

    private List<DummyItem> getFileListAsItems() {
        File[] fileList = getFileList();
        List<DummyItem> finalList = new LinkedList<>();

        for (File file : fileList) {
            finalList.add(new DummyItem(file));
        }

        Collections.sort(finalList, new Comparator<DummyItem>() {
            @Override
            public int compare(DummyItem lhs, DummyItem rhs) {
                return rhs.id.compareTo(lhs.id);
            }
        });

        return finalList;
    }
    
    private File[] getFileList() {
        String str = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "TardisRecord";
        File folder = new File(str);
        return folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("record-");
            }
        });
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = new OnFilesFragmentInteractionListener() {
            @Override
            public void onFilesFragmentInteraction(DummyItem item) {
                if (mPlayer == null)
                    mPlayer = new MediaPlayer();
                mPlayer.reset();

                try {
                    mPlayer.setDataSource(getContext(), Uri.fromFile(item.file));
                    mPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        return false;
                    }
                });
                mPlayer.start();
                mProgressBarHandler.sendEmptyMessage(PLAYER_PROGRESS);
            }
        };

        mProgressBarHandler.sendEmptyMessage(PLAYER_PROGRESS);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFilesFragmentInteractionListener {
        void onFilesFragmentInteraction(DummyItem item);
    }
}
