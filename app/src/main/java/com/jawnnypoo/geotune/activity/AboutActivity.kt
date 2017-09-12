package com.jawnnypoo.geotune.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import com.jawnnypoo.geotune.R

/**
 * Whatchu know about me?
 */
class AboutActivity : BaseActivity() {

    companion object {

        fun newInstance(context: Context): Intent {
            val intent = Intent(context, AboutActivity::class.java)
            return intent
        }
    }

    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        ButterKnife.bind(this)

        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        findViewById<View>(R.id.apache).setOnClickListener { gotoLink(getString(R.string.apache_url)) }
        findViewById<View>(R.id.john_credit).setOnClickListener { gotoLink(getString(R.string.jawn_url)) }
        findViewById<View>(R.id.kyrsten_credit).setOnClickListener { gotoLink(getString(R.string.kyrsten_url)) }
    }

    fun gotoLink(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}
