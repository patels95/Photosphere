package com.patels95.sanam.stormy.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

import com.patels95.sanam.stormy.R;

/**
 * Created by Sanam on 1/16/15.
 */
public class AlertDialogFragment extends DialogFragment{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.error_title))
                .setMessage(context.getString(R.string.error_msg))
                .setPositiveButton(context.getString(R.string.error_ok_text), null);

        AlertDialog dialog = builder.create();
        return dialog;
    }
}

