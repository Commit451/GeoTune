package com.jawnnypoo.geotune.dialog

import android.content.Context
import android.support.v7.app.AppCompatDialog
import butterknife.ButterKnife
import butterknife.OnClick
import com.jawnnypoo.geotune.R

class ChooseUriDialog(context: Context) : AppCompatDialog(context) {

    private var listener: ((uriChoice: UriChoice) -> Unit)? = null

    @OnClick(R.id.choice_notification)
    fun onChoiceNotification() {
        listener?.invoke(UriChoice.NOTIFICATION)
    }

    @OnClick(R.id.choice_media)
    fun onChoiceMedia() {
        listener?.invoke(UriChoice.MEDIA)
    }

    init {
        setContentView(R.layout.dialog_choose_uri)
        ButterKnife.bind(this)
    }

    fun setOnUriChoiceMadeListener(listener: (uriChoice: UriChoice) -> Unit) {
        this.listener = listener
    }

    enum class UriChoice {
        NOTIFICATION, MEDIA
    }
}