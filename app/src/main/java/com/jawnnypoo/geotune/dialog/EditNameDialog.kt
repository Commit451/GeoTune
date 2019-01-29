package com.jawnnypoo.geotune.dialog

import android.content.Context
import android.support.v7.app.AppCompatDialog
import com.jawnnypoo.geotune.R
import kotlinx.android.synthetic.main.dialog_edit_name.*

class EditNameDialog(context: Context) : AppCompatDialog(context) {

    private var listener: ((name: String) -> Unit)? = null

    init {
        setContentView(R.layout.dialog_edit_name)
        textName.setOnEditorActionListener { _, _, _ ->
            returnName()
            true
        }
        done.setOnClickListener {
            returnName()
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
