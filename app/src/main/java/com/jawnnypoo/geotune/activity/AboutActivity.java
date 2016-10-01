package com.jawnnypoo.geotune.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.jawnnypoo.geotune.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Whatchu know about me?
 */
public class AboutActivity extends BaseActivity{

    public static Intent newInstance(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        return intent;
    }

    @BindView(R.id.toolbar) Toolbar mToolbar;

    private final View.OnClickListener mNavigationIconClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
           onBackPressed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        mToolbar.setNavigationIcon(R.drawable.ic_back);
        mToolbar.setNavigationOnClickListener(mNavigationIconClickListener);

        findViewById(R.id.apache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoLink(getString(R.string.apache_url));
            }
        });
        findViewById(R.id.john_credit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoLink(getString(R.string.jawn_url));
            }
        });
        findViewById(R.id.kyrsten_credit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoLink(getString(R.string.kyrsten_url));
            }
        });
    }

    private void gotoLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
