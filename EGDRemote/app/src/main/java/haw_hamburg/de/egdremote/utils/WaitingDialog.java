package haw_hamburg.de.egdremote.utils;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;

/*
Author: Turan Elchuev, turan.elchuev@haw-hamburg.de, 02/2019

This class shows a full screen Progress bar dialog, e.g. when the user
has to wait without further interaction with GUI until some background process is completed.
*/


public class WaitingDialog {

    private static Dialog waitingDialog;

    // Show waiting dialog
    public static void show(Context context) {
        hide();
        waitingDialog = new Dialog(context);
        waitingDialog.setCanceledOnTouchOutside(false);
        waitingDialog.setContentView(new ProgressBar(context));

        waitingDialog.show();
    }

    // hide waiting dialog
    public static void hide() {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            try {
                waitingDialog.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            waitingDialog = null;
        }
    }
}
