package company.caller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import wei.mark.standout.StandOutWindow;


public class ActivityCaller extends Activity {

    final String LOG_TAG = this.getClass().toString();
    ArrayList<Event> events = new ArrayList<Event>();

//    private String number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        // Answer button handler
        Button buttonAnswer = (Button) findViewById(R.id.buttonAnswer);
        buttonAnswer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(LOG_TAG, ": Answer");
                StandOutWindow.closeAll(ActivityCaller.this, TopWindow.class);
                StandOutWindow.show(ActivityCaller.this, TopWindow.class, StandOutWindow.DEFAULT_ID);
            }
        });

        // Decline button handler
        Button buttonDecline = (Button) findViewById(R.id.buttonDecline);
        buttonDecline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(LOG_TAG, ": Decline");
                finish();
            }
        });

        Intent intent = getIntent();
        String number = intent.getStringExtra("PhoneNumber");
        if((number != null) && (!number.isEmpty())) {
            Log.d(LOG_TAG, ": " + number);
            ListInfo(number);
//            number = "123";
//            Log.d(LOG_TAG, ": Test call of Caller activity for phone number " + number);
        }
        else {
            createDialog();
        }
    }

    private void ListInfo(String number)
    {
        Log.d(LOG_TAG, ": ListInfo()");
        String name = getContactName(number);
        if((name!= null) && (!name.isEmpty())) {
            setTitle(name);
        }
        else {
            setTitle(number);
        }
        getCallDetails(number, name);
        getCalendarData(name);
        createEventList();
    }

    private void getCallDetails(String number, String name) {

        Integer start_index = events.size();

        String[] projection = new String[] {
//                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, "number='" + number + "'", null, null);

        while (cursor.moveToNext()) {
//            String phNumber = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.NUMBER));
            String callType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
            Date callDayTime = new Date(Long.valueOf(callDate));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = "Duration is " + cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION)) + " sec";

            Event.EventType type = Event.EventType.EVENT_UNDEFINED;
            switch (Integer.parseInt(callType)) {
                case CallLog.Calls.OUTGOING_TYPE:
                    type = Event.EventType.EVENT_OUTGOING_CALL;
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    type = Event.EventType.EVENT_INCOMING_CALL;
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    type = Event.EventType.EVENT_MISSED_CALL;
                    break;
            }
            events.add(new Event(type, datetime, description));
        }

        cursor.close();

        Log.d(LOG_TAG, ": Found " + (events.size() - start_index) + " entries for number " + number);
    }



    private void createEventList() {
        ListView listRecords = (ListView) findViewById(R.id.listView);

        EventAdapter adapter = new EventAdapter(this, events);

        listRecords.setAdapter(adapter);

        // scroll to the bottom
        listRecords.setSelection(adapter.getCount() - 1);

    }

    private void getCalendarData(String name) {

        Integer start_index = events.size();

        String[] projection = new String[] {
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART
        };

        Cursor cursor = getContentResolver().query(CalendarContract.Events.CONTENT_URI, projection, null, null, null);

        while (cursor.moveToNext()) {
//            String phNumber = managedCursor.getString(managedCursor.getColumnIndex(CallLog.Calls.NUMBER));
            String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE));
            String desc = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            String start = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DTSTART));
            Date callDayTime = new Date(Long.valueOf(start));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = title + " // " + desc;

            Event.EventType type = Event.EventType.EVENT_CALENDAR;
            events.add(new Event(type, datetime, description));
        }

        cursor.close();

        Log.d(LOG_TAG, ": Found " + (events.size() - start_index) + " entries for name " + name);

    }

    private String getContactName(String number) {

        String name = null;

        // define the columns I want the query to return
        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID};

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        // query time
        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

        if(cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.d(LOG_TAG, ": Contact name for number " + number + " is " + name);
            } else {
                Log.d(LOG_TAG, ": Contact Not Found @ " + number);
            }
            cursor.close();
        }
        return name;
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
                    ListInfo(number);
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
