package com.example.johnb.openingsfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

public class ValidTimeFragment extends DialogFragment {


    private OnInteractionListener mListener;
    private Context context;

    public ValidTimeFragment(Context context){
        this.context = context;
    }
    public void setInteractionListener(OnInteractionListener listener){mListener = listener;}

    public interface OnInteractionListener {
        void onInteraction(boolean isValid);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Create a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //Inflate (turn xml into code) your layout
        View contentView = getActivity().getLayoutInflater().inflate(R.layout.fragment_valid_time, null);

        //Get any Views you need (Any local variables accessed from inside an anonymous inner class must be final)


        //Pass your data to the builder (these can be chained)
        builder.setTitle("Attention")
                .setView(contentView)
                .setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onInteraction(true);

                    }
                })
        .setNegativeButton("SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                mListener.onInteraction(false);
            }
        });

        //Build the dialog and return it
        return builder.create();
    }
}
