package haw_hamburg.de.egdremote.utils;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;

public class WaitingDialog {

    private static Dialog waitingDialog;

    public static void show(Context context){
        hide();
        waitingDialog = new Dialog(context);
        waitingDialog.setCanceledOnTouchOutside(false);
        waitingDialog.setContentView(new ProgressBar(context));

        waitingDialog.show();
    }

    public static void hide(){
        if(waitingDialog != null && waitingDialog.isShowing()){
            try{
                waitingDialog.cancel();
            }catch(Exception e){}
            waitingDialog = null;
        }
    }
}
