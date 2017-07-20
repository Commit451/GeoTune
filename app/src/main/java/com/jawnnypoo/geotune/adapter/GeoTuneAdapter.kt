package com.jawnnypoo.geotune.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.PopupMenu
import com.jawnnypoo.geotune.R
import com.jawnnypoo.geotune.data.GeoTune
import com.jawnnypoo.geotune.viewHolder.GeoTuneViewHolder
import java.util.*

/**
 * Shows the [GeoTune]s
 */
class GeoTuneAdapter(private val callback: Callback) : RecyclerView.Adapter<GeoTuneViewHolder>() {

    val geoTunes = mutableListOf<GeoTune>()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): GeoTuneViewHolder {
        val geoTuneViewHolder = GeoTuneViewHolder.inflate(viewGroup)
        geoTuneViewHolder.card.setOnClickListener {
            val geoTune = geoTuneViewHolder.itemView.tag as GeoTune
            callback.onGeoTuneClicked(geoTune)
        }
        geoTuneViewHolder.popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
            val geoTune = geoTuneViewHolder.itemView.tag as GeoTune
            when (item.itemId) {
                R.id.action_rename -> {
                    callback.onRenameClicked(geoTune)
                    return@OnMenuItemClickListener true
                }
                R.id.action_delete -> {
                    removeGeoTune(geoTune)
                    callback.onDeleteClicked(geoTune)
                    return@OnMenuItemClickListener true
                }
            }
            false
        })
        geoTuneViewHolder.datSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val geoTune = geoTuneViewHolder.itemView.tag as GeoTune
            if (isChecked == geoTune.isActive) {
                return@OnCheckedChangeListener
            }
            geoTune.isActive = isChecked
            callback.onGeoTuneSwitched(isChecked, geoTune)
        })
        geoTuneViewHolder.tune.setOnClickListener {
            val geoTune = geoTuneViewHolder.itemView.tag as GeoTune
            callback.onSetNotificationClicked(geoTune)
        }
        return geoTuneViewHolder
    }

    override fun onBindViewHolder(viewGeoTuneViewHolder: GeoTuneViewHolder, position: Int) {
        val geoTune = getGeoTune(position)
        viewGeoTuneViewHolder.itemView.tag = geoTune
        viewGeoTuneViewHolder.bind(geoTune)
    }

    override fun getItemCount(): Int {
        return geoTunes.size
    }

    fun setGeofences(geoTunes: ArrayList<GeoTune>?) {
        this.geoTunes.clear()
        if (geoTunes != null) {
            this.geoTunes.addAll(geoTunes)
        }
        notifyDataSetChanged()
    }

    fun addGeoTune(geoTune: GeoTune) {
        geoTunes.add(geoTune)
        notifyItemInserted(geoTunes.size - 1)
    }

    fun removeGeoTune(geoTune: GeoTune) {
        val indexOf = geoTunes.indexOf(geoTune)
        geoTunes.removeAt(indexOf)
        notifyItemRemoved(indexOf)
    }

    fun getGeoTune(position: Int): GeoTune {
        return geoTunes[position]
    }

    fun onGeoTuneChanged(geoTune: GeoTune) {
        val index = geoTunes.indexOf(geoTune)
        notifyItemChanged(index)
    }

    interface Callback {
        fun onGeoTuneClicked(geoTune: GeoTune)
        fun onSetNotificationClicked(geoTune: GeoTune)
        fun onRenameClicked(geoTune: GeoTune)
        fun onDeleteClicked(geoTune: GeoTune)
        fun onGeoTuneSwitched(isChecked: Boolean, geoTune: GeoTune)
    }
}
