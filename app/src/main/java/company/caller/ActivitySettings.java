package company.caller;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import wei.mark.standout.StandOutWindow;


/**
 * Settings screen of the Application
 *
 */
public class ActivitySettings extends Activity {

    final String LOG_TAG = this.getClass().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_settings);

        Switch toggleMainSwitch = (Switch) findViewById(R.id.switchMainSwitch);

        // check if CallDetectService is already started
        toggleMainSwitch.setChecked(isServiceRunning(CallDetectService.class));

        // Log CallDetectService state
        if(isServiceRunning(CallDetectService.class)) {
            Log.d(LOG_TAG, ": Service is already running");
        }
        else {
            Log.d(LOG_TAG, ": Service is shut down");
        }

        toggleMainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(ActivitySettings.this, CallDetectService.class);
                if (isChecked) {
                    startService(intent);
                }
                else {
                    stopService(intent);
                }
            }
        });


        Button button = (Button) findViewById(R.id.button);
        SeekBar seekCallLogDepth = (SeekBar) findViewById(R.id.seekCallLogDepth);
        SeekBar seekShutdownDelay = (SeekBar) findViewById(R.id.seekShutdownDelay);
        TextView textCallLogDepthValue = (TextView) findViewById(R.id.textCallLogDepthValue);
        TextView textCallLogDepthLabel = (TextView) findViewById(R.id.textCallLogDepthLabel);
        TextView textShutdownDelayValue = (TextView) findViewById(R.id.textShutdownDelayValue);
        CheckBox checkEnableCallLogEvents = (CheckBox) findViewById(R.id.checkEnableCallLogEvents);
        CheckBox checkEnableCalendarEvents = (CheckBox) findViewById(R.id.checkEnableCalendarEvents);


        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    createDialog();

                }
        });

        // retrieve preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int prefCallLogDepth = preferences.getInt("prefCallLogDepth", 20);
        int prefShutdownDelay = preferences.getInt("prefShutdownDelay", 0);
        boolean prefEnableCallLogEvents = preferences.getBoolean("prefEnableCallLogEvents", true);
        boolean prefEnableCalendarEvents = preferences.getBoolean("prefEnableCalendarEvents", true);

        seekCallLogDepth.setProgress(prefCallLogDepth);
        seekShutdownDelay.setProgress(prefShutdownDelay);
        seekCallLogDepth.setEnabled(prefEnableCallLogEvents);

        textCallLogDepthValue.setText(Integer.toString(prefCallLogDepth));
        textCallLogDepthValue.setEnabled(prefEnableCallLogEvents);
        textCallLogDepthLabel.setEnabled(prefEnableCallLogEvents);
        textShutdownDelayValue.setText(Integer.toString(prefShutdownDelay));

        checkEnableCallLogEvents.setChecked(prefEnableCallLogEvents);
        checkEnableCalendarEvents.setChecked(prefEnableCalendarEvents);

        // track changes of seekCallLogDepth (SeekBar)
        seekCallLogDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.textCallLogDepthValue)).setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // store value to preferences
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivitySettings.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("prefCallLogDepth", seekBar.getProgress());
                editor.commit();
            }
        });

        // track changes of seekShutdownDelay (SeekBar)
        seekShutdownDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                ((TextView) findViewById(R.id.textShutdownDelayValue)).setText(Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // store value to preferences
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivitySettings.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("prefShutdownDelay", seekBar.getProgress());
                editor.commit();
            }
        });

        checkEnableCallLogEvents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivitySettings.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("prefEnableCallLogEvents", isChecked);
                editor.commit();
                findViewById(R.id.textCallLogDepthValue).setEnabled(isChecked);
                findViewById(R.id.textCallLogDepthLabel).setEnabled(isChecked);
                findViewById(R.id.seekCallLogDepth).setEnabled(isChecked);
            }
        });

        checkEnableCalendarEvents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivitySettings.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("prefEnableCalendarEvents", isChecked);
                editor.commit();
            }
        });
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
