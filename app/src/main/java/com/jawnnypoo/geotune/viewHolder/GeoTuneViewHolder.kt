package com.jawnnypoo.geotune.viewHolder

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.commit451.addendum.recyclerview.bindView
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.data.GeoTune

class GeoTuneViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    companion object {

        fun inflate(parent: ViewGroup): GeoTuneViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_geofence, parent, false)
            return GeoTuneViewHolder(view)
        }
    }

    val card: View by bindView(R.id.card_view)
    val name: TextView by bindView(R.id.geotune_name)
    val tune: TextView by bindView(R.id.geotune_tune)
    val datSwitch: SwitchCompat by bindView(R.id.geotune_switch)
    val overflow: ImageView by bindView(R.id.geotune_overflow)
    val popupMenu=PopupMenu(view.context, overflow)

    init {
        popupMenu.menuInflater.inflate(R.menu.geotune_menu, popupMenu.menu)
        overflow.setOnClickListener { popupMenu.show() }
    }

    fun bind(geoTune: GeoTune) {
        name.text = geoTune.name
        if (geoTune.tuneUri == null) {
            tune.text = itemView.context.getString(R.string.default_notification_tone)
        } else {
            if (geoTune.tuneName == null) {
                tune.text = ""
            } else {
                tune.text = geoTune.tuneName
            }
        }
        datSwitch.isChecked = geoTune.isActive
    }
}
