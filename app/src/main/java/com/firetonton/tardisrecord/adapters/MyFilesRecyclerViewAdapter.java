package com.firetonton.tardisrecord.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.firetonton.tardisrecord.ItemFragment.OnListFragmentInteractionListener;
import com.firetonton.tardisrecord.R;
import com.firetonton.tardisrecord.dummy.DummyContent.DummyItem;
import com.firetonton.tardisrecord.fragments.FilesFragment;

import java.util.Calendar;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link FilesFragment.OnFilesFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyFilesRecyclerViewAdapter extends RecyclerView.Adapter<MyFilesRecyclerViewAdapter.ViewHolder> {

    private final List<DummyItem> mValues;
    private final FilesFragment.OnFilesFragmentInteractionListener mListener;

    public MyFilesRecyclerViewAdapter(List<DummyItem> items, FilesFragment.OnFilesFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(holder.mItem.file.lastModified());

        String dayBadge = String.format("%1$tb\n%1$td", cal);

        holder.mDayBadgeView.setText(dayBadge);
        holder.mContentView.setText(
                String.format("%1$TH:%1$TM", cal)
                +" - Taille: "+(holder.mItem.file.length()/1024)+"ko");
        holder.mDetailView.setText(holder.mItem.file.getName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onFilesFragmentInteraction(holder.mItem);
                }
            }
        });
        Button shareBtn = (Button) holder.mView.findViewById(R.id.shareFileButton);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    //mListener.onShareItem(holder.mItem);
                    Toast.makeText(holder.mView.getContext(), "Partage à venir", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        //public final TextView mIdView;
        public final TextView mDayBadgeView;
        public final TextView mContentView;
        public final TextView mDetailView;
        public DummyItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mIdView = (TextView) view.findViewById(R.id.id);
            mDayBadgeView = (TextView) view.findViewById(R.id.dayBadge);
            mContentView = (TextView) view.findViewById(R.id.content);
            mDetailView = (TextView) view.findViewById(R.id.details);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
