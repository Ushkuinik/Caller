package company.caller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import wei.mark.standout.StandOutWindow;

/**
 *
 *
 */
public class CallDetectService extends Service {
    public static final int GOT_PHONE_NUMBER = 0;
    final String LOG_TAG = this.getClass().toString();
    private TelephonyManager tm;
    private CallStateListener callStateListener;

    public CallDetectService() {
        Log.d(LOG_TAG, "constructor");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand, startId: " + startId);

        callStateListener = new CallStateListener(this);
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        super.onDestroy();

        if (tm != null) {
            tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        }
        callStateListener = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");

        return null;
    }

    private class CallStateListener extends PhoneStateListener {
        private Context context;

        public CallStateListener(Context context) {
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(LOG_TAG, "[CALL_STATE_RINGING]");
                    // called when someone is ringing to this phone

                    StandOutWindow.closeAll(context, TopWindow.class);
                    StandOutWindow.show(context, TopWindow.class, StandOutWindow.DEFAULT_ID);
                    Bundle bundle = new Bundle();
                    bundle.putString("phoneNumber", incomingNumber);
                    StandOutWindow.sendData(context, TopWindow.class, StandOutWindow.DEFAULT_ID, GOT_PHONE_NUMBER, bundle, null, 0);
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    int prefEnableCallLogEvents = preferences.getInt("prefShutdownDelay", 0);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            StandOutWindow.closeAll(context, TopWindow.class);
                        }
                    }, prefEnableCallLogEvents * 1000);
                    Log.d(LOG_TAG, "[CALL_STATE_IDLE]");
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(LOG_TAG, "[CALL_STATE_OFFHOOK]");
                    break;
            }
        }
    }
}
