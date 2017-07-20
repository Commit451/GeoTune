package com.jawnnypoo.geotune.dialog

import android.content.Context
import android.support.v7.app.AppCompatDialog
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.jawnnypoo.geotune.R

class EditNameDialog(context: Context) : AppCompatDialog(context) {

    @BindView(R.id.name) lateinit var textName: EditText

    private var listener: ((name: String) -> Unit)? = null

    @OnClick(R.id.done)
    fun onDoneClick() {
        returnName()
    }

    init {
        setContentView(R.layout.dialog_edit_name)
        ButterKnife.bind(this)
        textName.setOnEditorActionListener { _, _, _ ->
            returnName()
            true
        }
    }

    fun returnName() {
        listener?.invoke(textName.text.toString())
    }

    fun setOnEditNameListener(listener: (name: String) -> Unit) {
        this.listener = listener
    }

    fun setName(name: String?) {
        textName.append(name)
    }
}