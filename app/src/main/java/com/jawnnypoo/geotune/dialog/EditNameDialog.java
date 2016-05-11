package com.jawnnypoo.geotune.dialog;

import android.content.Context;
import android.support.v7.app.AppCompatDialog;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

import com.jawnnypoo.geotune.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditNameDialog extends AppCompatDialog {

    @BindView(R.id.name) EditText mName;

    private OnEditNameDialogListener mListener;

    public interface OnEditNameDialogListener {
        void onNameEdited(String name);
    }

    @OnClick(R.id.done)
    void onDoneClick() {
        returnName();
    }

    public EditNameDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_edit_name);
        ButterKnife.bind(this);
        mName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                returnName();
                return true;
            }
        });
    }

    private void returnName() {
        if (mListener != null) {
            mListener.onNameEdited(mName.getText().toString());
        }
    }

    public void setOnEditNameListener(OnEditNameDialogListener listener) {
        mListener = listener;
    }

    public void setName(String name) {
        mName.setText(name);
    }
}