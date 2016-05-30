package com.example.johnb.openingsfinder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;


public class NewEventFragment extends DialogFragment{

    private OnDoneListener listener;

    public void setDoneListener(OnDoneListener listener){ this.listener = listener;}

    public interface OnDoneListener{
        void OnDone(int durationInMinutes);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Create a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //Inflate (turn xml into code) your layout
        View contentView = getActivity().getLayoutInflater().inflate(R.layout.fragment_new_event, null);

        //Get any Views you need (Any local variables accessed from inside an anonymous inner class must be final)
        final NumberPicker durationHours = (NumberPicker) contentView.findViewById(R.id.hours);
        String[] nums = new String[24];
        for(int i=0; i<nums.length; i++)
            nums[i] = Integer.toString(i);

        durationHours.setMinValue(0);
        durationHours.setMaxValue(23);
        durationHours.setWrapSelectorWheel(false);
        durationHours.setDisplayedValues(nums);
        durationHours.setValue(0);
        durationHours.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        final NumberPicker durationMinutes = (NumberPicker) contentView.findViewById(R.id.minutes);
        String[] minuteValues = new String[4];

        for (int i = 0; i < minuteValues.length; i++) {
            String number = Integer.toString(i*15);
            minuteValues[i] = number.length() < 2 ? "0" + number : number;
        }

        durationMinutes.setDisplayedValues(minuteValues);

        durationMinutes.setMinValue(0);
        durationMinutes.setMaxValue(3);
        durationMinutes.setWrapSelectorWheel(false);
        durationMinutes.setValue(0);
        durationMinutes.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);


        //Pass your data to the builder (these can be chained)
        builder.setTitle("")
                .setView(contentView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //duration = duration in minutes
                        int duration = durationHours.getValue() * 60 + durationMinutes.getValue() * 15;
                        listener.OnDone(duration);

                    }
                });

        //Build the dialog and return it
        return builder.create();
    }
}
