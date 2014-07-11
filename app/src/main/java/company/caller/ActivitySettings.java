package company.caller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Button;
import android.view.View;

import wei.mark.standout.StandOutWindow;


/**
 *
 *
 */
public class ActivitySettings extends Activity {

    final String LOG_TAG = this.getClass().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Switch toggleMainSwitch = (Switch) findViewById(R.id.switchMainSwitch);

        // check if CallDetectService is already started
        toggleMainSwitch.setChecked(CallDetectService.isRunning(this));

        // Log CallDetectService state
        if(CallDetectService.isRunning(this)) {
            Log.d(LOG_TAG, ": Service is already running");
        }
        else {
            Log.d(LOG_TAG, ": Service is shut down");
        }

        toggleMainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDetectEnabled(isChecked);
            }
        });


        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    createDialog();

                    /*
                    Intent intent = new Intent(ActivitySettings.this, ActivityCaller.class);
                    //intent.putExtra("PhoneNumber", "");
                    startActivity(intent);
                    */
                }
        });
    }

    private void setDetectEnabled(boolean enable) {

        Intent intent = new Intent(this, CallDetectService.class);
        if (enable) {
            startService(intent);
        }
        else {
            stopService(intent);
        }
    }

    private void createDialog() {

        Log.d(LOG_TAG, ": prepare dialog to ask number");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_number, null);
        alertDialogBuilder.setView(promptView);

        final EditText input = (EditText) promptView.findViewById(R.id.userInput);

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // get user input and set it to result
                String number = input.getText().toString();
                if(!number.isEmpty()) {
                    Log.d(LOG_TAG, ": Dialog got number " + number);
                    StandOutWindow.closeAll(ActivitySettings.this, TopWindow.class);
                    StandOutWindow.show(ActivitySettings.this, TopWindow.class, StandOutWindow.DEFAULT_ID);
                    Bundle bundle = new Bundle();
                    bundle.putString("phoneNumber", number);
                    StandOutWindow.sendData(ActivitySettings.this, TopWindow.class, StandOutWindow.DEFAULT_ID, CallDetectService.GOT_PHONE_NUMBER, bundle, null, 0);
                }
                else {
                    Log.d(LOG_TAG, ": Dialog got empty number");
                }
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(LOG_TAG, ": Dialog cancelled");
                        dialog.cancel();
                    }
                });

        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();

    }
}
