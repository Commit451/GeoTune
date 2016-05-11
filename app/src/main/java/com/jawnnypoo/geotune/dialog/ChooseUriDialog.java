package com.jawnnypoo.geotune.dialog;

import android.content.Context;
import android.support.v7.app.AppCompatDialog;

import com.jawnnypoo.geotune.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChooseUriDialog extends AppCompatDialog {

    public interface OnUriChoiceMadeListener {
        void onChoiceMade(UriChoice choice);
    }

    private OnUriChoiceMadeListener mListener;

    public enum UriChoice {
        NOTIFICATION, MEDIA
    }

    @OnClick(R.id.choice_notification)
    void onChoiceNotification() {
        if (mListener != null) {
            mListener.onChoiceMade(UriChoice.NOTIFICATION);
        }
    }

    @OnClick(R.id.choice_media)
    void onChoiceMedia() {
        if (mListener != null) {
            mListener.onChoiceMade(UriChoice.MEDIA);
        }
    }

    public ChooseUriDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_choose_uri);
        ButterKnife.bind(this);
    }

    public void setOnUriChoiceMadeListener(OnUriChoiceMadeListener listener) {
        mListener = listener;
    }
}