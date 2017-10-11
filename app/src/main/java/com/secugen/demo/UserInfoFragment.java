package com.secugen.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



public class UserInfoFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface EnrollUserDialogListener {
        public void onDialogPositiveClick(DialogFragment dialogFragment,DialogInterface dialog);
        public void onDialogNegativeClick(DialogFragment dialogFragment);
    }

    // Use this instance of the interface to deliver action events
    EnrollUserDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (EnrollUserDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
       AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
       // Get the layout inflater
       LayoutInflater inflater = getActivity().getLayoutInflater();

       // Inflate and set the layout for the dialog
       // Pass null as the parent view because its going in the dialog layout
       builder.setView(inflater.inflate(R.layout.dialog_enroll, null))
               // Add action buttons
               .setPositiveButton(R.string.enroll, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int id) {
                     // sign in the user ...
                      mListener.onDialogPositiveClick(UserInfoFragment.this,dialog);
                  }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                      mListener.onDialogNegativeClick(UserInfoFragment.this);

                  }
               });
       return builder.create();
    }
   }
