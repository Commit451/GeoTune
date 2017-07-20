package com.jawnnypoo.geotune.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View

import com.jawnnypoo.geotune.R

import butterknife.BindView
import butterknife.ButterKnife

/**
 * Whatchu know about me?
 */
class AboutActivity : BaseActivity() {

    @BindView(R.id.toolbar) internal var toolbar: Toolbar? = null

    private val mNavigationIconClickListener = View.OnClickListener { onBackPressed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        ButterKnife.bind(this)

        toolbar!!.setNavigationIcon(R.drawable.ic_back)
        toolbar!!.setNavigationOnClickListener(mNavigationIconClickListener)

        findViewById(R.id.apache).setOnClickListener { gotoLink(getString(R.string.apache_url)) }
        findViewById(R.id.john_credit).setOnClickListener { gotoLink(getString(R.string.jawn_url)) }
        findViewById(R.id.kyrsten_credit).setOnClickListener { gotoLink(getString(R.string.kyrsten_url)) }
    }

    private fun gotoLink(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    companion object {

        fun newInstance(context: Context): Intent {
            val intent = Intent(context, AboutActivity::class.java)
            return intent
        }
    }
}
