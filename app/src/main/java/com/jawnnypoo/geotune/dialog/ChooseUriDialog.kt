package com.jawnnypoo.geotune.dialog

import android.content.Context
import android.support.v7.app.AppCompatDialog
import com.jawnnypoo.geotune.R
import kotlinx.android.synthetic.main.dialog_choose_uri.*

class ChooseUriDialog(context: Context) : AppCompatDialog(context) {

    private var listener: ((uriChoice: UriChoice) -> Unit)? = null

    init {
        setContentView(R.layout.dialog_choose_uri)
        rootNotification.setOnClickListener {
            listener?.invoke(UriChoice.NOTIFICATION)
        }
        rootMedia.setOnClickListener {
            listener?.invoke(UriChoice.MEDIA)
        }
    }

    fun setOnUriChoiceMadeListener(listener: (uriChoice: UriChoice) -> Unit) {
        this.listener = listener
    }

    enum class UriChoice {
        NOTIFICATION, MEDIA
    }
}
