package com.jawnnypoo.geotune.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.PopupMenu;

import com.jawnnypoo.geotune.R;
import com.jawnnypoo.geotune.data.GeoTune;
import com.jawnnypoo.geotune.viewHolder.GeoTuneViewHolder;

import java.util.ArrayList;

/**
 * Shows the {@link GeoTune}s
 */
public class GeoTuneAdapter extends RecyclerView.Adapter<GeoTuneViewHolder>{

    private ArrayList<GeoTune> mGeoTunes;
    private Callback mCallback;

    public GeoTuneAdapter(Callback callback) {
        mCallback = callback;
        mGeoTunes = new ArrayList<>();
    }

    @Override
    public GeoTuneViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        final GeoTuneViewHolder geoTuneViewHolder = GeoTuneViewHolder.inflate(viewGroup);
        geoTuneViewHolder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoTune geoTune = (GeoTune) geoTuneViewHolder.itemView.getTag();
                mCallback.onGeoTuneClicked(geoTune);
            }
        });
        geoTuneViewHolder.popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                GeoTune geoTune = (GeoTune) geoTuneViewHolder.itemView.getTag();
                switch (item.getItemId()) {
                    case R.id.action_rename:
                        mCallback.onRenameClicked(geoTune);
                        return true;
                    case R.id.action_delete:
                        removeGeoTune(geoTune);
                        mCallback.onDeleteClicked(geoTune);
                        return true;
                }
                return false;
            }
        });
        geoTuneViewHolder.datSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GeoTune geoTune = (GeoTune) geoTuneViewHolder.itemView.getTag();
                if (isChecked == geoTune.isActive()) {
                    return;
                }
                geoTune.setActive(isChecked);
                mCallback.onGeoTuneSwitched(isChecked, geoTune);
            }
        });
        geoTuneViewHolder.tune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoTune geoTune = (GeoTune) geoTuneViewHolder.itemView.getTag();
                mCallback.onSetNotificationClicked(geoTune);
            }
        });
        return geoTuneViewHolder;
    }

    @Override
    public void onBindViewHolder(GeoTuneViewHolder viewGeoTuneViewHolder, int position) {
        GeoTune geoTune = getGeoTune(position);
        viewGeoTuneViewHolder.itemView.setTag(geoTune);
        viewGeoTuneViewHolder.bind(geoTune);
    }

    @Override
    public int getItemCount() {
        return mGeoTunes.size();
    }

    public void setGeofences(ArrayList<GeoTune> geoTunes) {
        mGeoTunes.clear();
        if (geoTunes != null) {
            mGeoTunes.addAll(geoTunes);
        }
        notifyDataSetChanged();
    }

    public void addGeoTune(GeoTune geoTune) {
        mGeoTunes.add(geoTune);
        notifyItemInserted(mGeoTunes.size() - 1);
    }

    public void removeGeoTune(GeoTune geoTune) {
        int indexOf = mGeoTunes.indexOf(geoTune);
        mGeoTunes.remove(indexOf);
        notifyItemRemoved(indexOf);
    }

    public GeoTune getGeoTune(int position) {
        return mGeoTunes.get(position);
    }

    public ArrayList<GeoTune> getGeoTunes() {
        return mGeoTunes;
    }

    public void onGeoTuneChanged(GeoTune geoTune) {
        int index = mGeoTunes.indexOf(geoTune);
        notifyItemChanged(index);
    }

    public interface Callback {
        void onGeoTuneClicked(GeoTune geoTune);
        void onSetNotificationClicked(GeoTune geoTune);
        void onRenameClicked(GeoTune geoTune);
        void onDeleteClicked(GeoTune geoTune);
        void onGeoTuneSwitched(boolean isChecked, GeoTune geoTune);
    }
}
