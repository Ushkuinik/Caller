package company.caller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.util.Log;

import wei.mark.standout.StandOutWindow;

/**
 * Call detect service.
 * This service is needed, because MainActivity can lost it's focus,
 * and calls will not be detected.
 *
 * @author Moskvichev Andrey V.
 *
 */
public class CallDetectService extends Service {
    public static final int GOT_PHONE_NUMBER = 0;
    static final String LOG_TAG = "CallDetectService";
    private TelephonyManager tm;
    private CallStateListener callStateListener;

    private class CallStateListener extends PhoneStateListener {
        private Context context;

        public CallStateListener(Context context) {
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(LOG_TAG, ": Incoming call (CALL_STATE_RINGING)");
                    // called when someone is ringing to this phone

                    StandOutWindow.closeAll(context, TopWindow.class);
                    StandOutWindow.show(context, TopWindow.class, StandOutWindow.DEFAULT_ID);
                    Bundle bundle = new Bundle();
                    bundle.putString("phoneNumber", incomingNumber);
                    StandOutWindow.sendData(context, TopWindow.class, StandOutWindow.DEFAULT_ID, GOT_PHONE_NUMBER, bundle, null, 0);
/*
                    Toast.makeText(context,
                            "[Caller] Incoming: " + incomingNumber,
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(context, ActivityCaller.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("PhoneNumber", incomingNumber);
                    context.startActivity(intent);
*/
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    StandOutWindow.closeAll(context, TopWindow.class);
                    Log.d(LOG_TAG, ": (CALL_STATE_IDLE)");
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(LOG_TAG, ": (CALL_STATE_OFFHOOK)");
                    break;
            }
        }
    }

    public CallDetectService() {
        Log.d(LOG_TAG, ": constructor");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, ": onStartCommand, startId: " + startId);

        setRunning(true);

        callStateListener = new CallStateListener(this);
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if(tm != null) {
            tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
//        int res = super.onStartCommand(intent, flags, startId);
//        return res;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, ": onDestroy");

        super.onDestroy();

        setRunning(false);

        if(tm != null) {
            tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        }
        callStateListener = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, ": onBind");
        // not supporting binding
        return null;
    }

    private void setRunning(boolean running) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(LOG_TAG, running);
        editor.apply();
    }

    public static boolean isRunning(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return pref.getBoolean(LOG_TAG, false);
    }
}
