package com.jawnnypoo.geotune.viewHolder;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.jawnnypoo.geotune.R;
import com.jawnnypoo.geotune.data.GeoTune;

public class GeoTuneViewHolder extends RecyclerView.ViewHolder {

    public static GeoTuneViewHolder inflate(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_geofence, parent, false);
        return new GeoTuneViewHolder(view);
    }

    public View card;
    public TextView name;
    public TextView tune;
    public SwitchCompat datSwitch;
    public ImageView overflow;
    public PopupMenu popupMenu;

    public GeoTuneViewHolder(View view) {
        super(view);
        card = view.findViewById(R.id.card_view);
        name = (TextView) view.findViewById(R.id.geotune_name);
        tune = (TextView) view.findViewById(R.id.geotune_tune);
        datSwitch = (SwitchCompat) view.findViewById(R.id.geotune_switch);
        overflow = (ImageView) view.findViewById(R.id.geotune_overflow);
        popupMenu = new PopupMenu(view.getContext(), overflow);
        popupMenu.getMenuInflater().inflate(R.menu.geotune_menu, popupMenu.getMenu());
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
    }

    public void bind(GeoTune geoTune) {
        name.setText(geoTune.getName());
        if (geoTune.getTuneUri() == null) {
            tune.setText(itemView.getContext().getString(R.string.default_notification_tone));
        } else {
            if (geoTune.getTuneName() == null) {
                tune.setText("");
                //TODO
                //new GetFileNameTask(itemView.getContext(), geoTune, this).execute(geoTune.getTuneUri());
            } else {
                tune.setText(geoTune.getTuneName());
            }
        }
        datSwitch.setChecked(geoTune.isActive());
    }
}
