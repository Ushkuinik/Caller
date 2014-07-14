package company.caller;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The Window, which will float over Call screen when call incomes.
 * It will be shown and closed by CallDetectService.
 * The window will contain a list of known activity related to calling contact such as call history
 * and calendar appointments
 *
 * @see StandOutWindow
 */
public class TopWindow extends StandOutWindow {

    private final String LOG_TAG = this.getClass().toString();
    private View view = null;
    private int id;



    @Override
    public String getAppName() {
        return "Caller";
    }



    @Override
    public int getAppIcon() {
        return 0;
    }



    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        Log.d(LOG_TAG, ": createAndAttachView");
        // create a new layout from body.xml
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.top_window, frame, true);
        this.id = id;
    }



    @Override
    public StandOutLayoutParams getParams(int id, wei.mark.standout.ui.Window window) {

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        return new StandOutLayoutParams(
                id,
                width,    // width
                height / 3 * 2,    // height
                StandOutLayoutParams.TOP,   // xpos
                StandOutLayoutParams.LEFT);   // ypos
    }


    /**
     * The window should not be resizable and movable. It should be always on top until closed by
     * CallDetectService.
     *
     * @param id
     *              The id of the window.
     * @return      Flags with window parameters
     */
    @Override
    public int getFlags(int id) {
        return  super.getFlags(id) |  StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_DECORATION_CLOSE_DISABLE
                | StandOutFlags.FLAG_DECORATION_RESIZE_DISABLE
                | StandOutFlags.FLAG_DECORATION_MAXIMIZE_DISABLE
                | StandOutFlags.FLAG_DECORATION_MOVE_DISABLE
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                | StandOutFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE
                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_ALL_DISABLE
                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE
                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE;
    }


    /**
     * Receives a phone number of incoming call from CallDetectService and starts ListView
     * populating.
     *
     * @param id
     *            The id of your receiving window.
     * @param requestCode
     *            The sending window provided this request code to declare what
     *            kind of data is being sent.
     * @param data
     *            A bundle of parceleable data that was sent to your receiving
     *            window.
     * @param fromCls
     *            The sending window's class. Provided if the sender wants a
     *            result.
     * @param fromId
     *            The sending window's id. Provided if the sender wants a
     */
    @Override
    public void onReceiveData(int id, int requestCode, Bundle data,
                              Class<? extends StandOutWindow> fromCls, int fromId) {
        Log.d(LOG_TAG, ": onReceiveData");

        switch (requestCode) {
            case CallDetectService.GOT_PHONE_NUMBER:
                Window window = getWindow(id);
                if (window == null) {
                    String errorText = String.format(Locale.US,
                            "%s received data but TopWindow id: %d is not open.",
                            getAppName(), id);
                    Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show();
                    return;
                }
                String number = data.getString("phoneNumber");
                number = number.replace("-", "");
                Log.d(LOG_TAG, ": onReceiveData(phoneNumber = " + number + ")");

                //listContactEvents(number);
                AsyncListFiller mt = new AsyncListFiller(id);
                mt.execute();
                break;

            default:
                Log.d("TopWindow", "Unexpected data received.");
                break;
        }
    }




    /**
     * Looks for contact name and enumerates CallLog and Calendar activities
     *
     * @param number
     *            phone number of incoming call
     */
    private void listContactEvents(String number) {
        Log.d(LOG_TAG, ": listContactEvents");
        String name = getContactName(number);

        if((name!= null) && (!name.isEmpty())) {
            setTitle(id, name);
        }
        else {
            setTitle(id, number);
        }

        ArrayList<Event> events = new ArrayList<Event>();

        enumCallLog(number, events); // list call log

        // look through calendar only if we have a name
        if(name != null) {
            enumCalendarEvents(name, events); // list calendar events
            enumCalendarAttendees(name, events); // list calendar events with specified attendee
        }

//        events.add(new Event(Event.EventType.EVENT_CALENDAR, "2014/08/01 15:00", "Meeting // Common meeting"));
//        events.add(new Event(Event.EventType.EVENT_REMINDER, "2014/09/01 19:00", "Dinner // Meeting with Agent Smith"));

        createEventList(events);
    }



    /**
     * Looks through CallLog for calls from specified contact
     *
     * @param number
     *              phone number of incoming call
     * @param events
     *              array of Events to be populated
     * @see CallLog
     */
    private void enumCallLog(String number, ArrayList<Event> events) {
        Log.d(LOG_TAG, ": enumCallLog");

        Integer start_index = events.size();

        String[] projection = new String[] {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, "number='" + number + "'", null, null);

        while (cursor.moveToNext()) {
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

        Log.d(LOG_TAG, ": enumCallLog found " + (events.size() - start_index) + " entries for number " + number);
    }



    /**
     * Constructs ListView from Events array. Uses custom EventAdapter
     *
     * @param events
     *              array of Events to be converted to ListView
     * @see EventAdapter
     */
    private void createEventList(ArrayList<Event> events) {
        Log.d(LOG_TAG, ": createEventList");

        ListView listRecords = (ListView) view.findViewById(R.id.listView);
        EventAdapter adapter = new EventAdapter(this, events);
        listRecords.setAdapter(adapter);

        // scroll to the bottom
        listRecords.smoothScrollToPosition(adapter.getCount() - 1);

    }


    /**
     * Looks through Calendar.Events for scheduled activities with
     * specified contact
     *
     * @param name
     *              contact name
     * @param events
     *              array of Events to be populated
     * @see CalendarContract
     */
    private void enumCalendarEvents(String name, ArrayList<Event> events) {
        Log.d(LOG_TAG, ": enumCalendarEvents");

        Integer start_index = events.size();

        name = name.toLowerCase(); // convert name lower case to simplify our search

        String[] projection = new String[] {
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART
        };

        Cursor cursor = getContentResolver().query(CalendarContract.Events.CONTENT_URI, projection, null, null, null);
        Log.d(LOG_TAG, ": enumCalendarEvents found " + cursor.getCount() + " records");

        while (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE));
            String desc = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            title = (title == null) ? "" : title;
            desc = (desc == null) ? "" : desc;

            String start = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DTSTART));
            Date callDayTime = new Date(Long.valueOf(start));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = title + " // " + desc;

            if( (title.toLowerCase().contains(name)) ||
                (desc.toLowerCase().contains(name)) ) {
                events.add(new Event(Event.EventType.EVENT_CALENDAR, datetime, description));
            }
        }

        cursor.close();

        Log.d(LOG_TAG, ": enumCalendarEvents found " + (events.size() - start_index) + " entries for name " + name);
    }

    /**
     * Looks through Calendar.Attendees for scheduled activities with
     * specified contact
     *
     * @param name
     *              contact name
     * @param events
     *              array of Events to be populated
     * @see CalendarContract
     */
    private void enumCalendarAttendees(String name, ArrayList<Event> events) {
        Log.d(LOG_TAG, ": enumCalendarAttendees");

        Integer start_index = events.size();
        String datetime = "";
        String description = "";

        name = name.toLowerCase(); // convert name lower case to simplify our search

        String[] projection = new String[] {
                CalendarContract.Attendees.EVENT_ID,
                CalendarContract.Attendees.ATTENDEE_NAME
        };

        Cursor cursor = getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, projection, null, null, null);
        Log.d(LOG_TAG, ": enumCalendarAttendees found " + cursor.getCount() + " records");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.EVENT_ID));
            String attendee = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));

            Log.d(LOG_TAG, ": enumCalendarAttendees found " + attendee);

            // if attendee was found, look for related event to get its time and description
            if(attendee.toLowerCase().contains(name)) {

                String[] projection2 = new String[]{
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DESCRIPTION,
                        CalendarContract.Events.DTSTART
                };

                String selection2 = CalendarContract.Events._ID + " = ?";
                String[] selectionArgs2 = new String[]{id};


                Cursor cursor2 = getContentResolver().query(
                        CalendarContract.Events.CONTENT_URI,
                        projection2,
                        selection2,
                        selectionArgs2,
                        null);

                // there should be only one record for certain id
                while (cursor2.moveToNext()) {

                    String title = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.TITLE));
                    String desc = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                    String start = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DTSTART));
                    title = (title == null) ? "" : title;
                    desc = (desc == null) ? "" : desc;

                    Date callDayTime = new Date(Long.valueOf(start));
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

                    datetime = formatter.format(callDayTime);
                    description = title + " // " + desc;
                }

                cursor2.close();
            }

            events.add(new Event(Event.EventType.EVENT_CALENDAR, datetime, description));
        }

        cursor.close();

        Log.d(LOG_TAG, ": enumCalendarAttendees found " + (events.size() - start_index) + " entries for name " + name);
    }


    /**
     * Looks for contact name
     *
     * @param number
     *              phone number of incoming call
     * @return      contact name if it is in the phone book, otherwise <tt>null</tt>
     * @see ContactsContract
     */
    private String getContactName(String number) {

        String name = null;

        // we look for only contact name
        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME};

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

        if(cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.d(LOG_TAG, ": Contact name for number " + number + " is " + name);
            } else {
                Log.d(LOG_TAG, ": Contact not found for number " + number);
            }
            cursor.close();
        }
        return name;
    }




    class AsyncListFiller extends AsyncTask<Void,  ArrayList<Event>, Void> {
        private int window_id;
        ArrayList<Event> events = new ArrayList<Event>();
        ListView listRecords = (ListView) view.findViewById(R.id.listView);
        EventAdapter adapter = new EventAdapter(TopWindow.this, events);


        AsyncListFiller(int id) {
            window_id = id;
            listRecords.setAdapter(adapter);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setTitle(window_id, "Begin");

        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<Event> localEvents = new ArrayList<Event>();

            try {

                localEvents.clear();
                Log.d(LOG_TAG, ": doInBackground1");
                for(int i = 1; i < 6; i++) {
                    localEvents.add(new Event(Event.EventType.EVENT_CALENDAR, "2014/08/01 15:00", "Event " + i));
                }
                publishProgress(localEvents);
                TimeUnit.SECONDS.sleep(3);

                localEvents.clear();
                Log.d(LOG_TAG, ": doInBackground2");
                for(int i = 1; i < 6; i++) {
                    localEvents.add(new Event(Event.EventType.EVENT_REMINDER, "2014/08/01 15:00", "Event " + i));
                }
                publishProgress(localEvents);
                TimeUnit.SECONDS.sleep(3);

                localEvents.clear();
                Log.d(LOG_TAG, ": doInBackground3");
                for(int i = 1; i < 6; i++) {
                    localEvents.add(new Event(Event.EventType.EVENT_MISSED_CALL, "2014/08/01 15:00", "Event " + i));
                }
                publishProgress(localEvents);
                TimeUnit.SECONDS.sleep(3);

                } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }



        protected void onProgressUpdate(ArrayList<Event>... param) {
            Log.d(LOG_TAG, ": onProgressUpdate");
            for(Event e : param[0])
                events.add(e);
            adapter.notifyDataSetChanged();
            listRecords.smoothScrollToPosition(adapter.getCount() - 1); // smoothScrollToPosition looks better then setSelection

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            setTitle(window_id, "End");
        }
    }
}
