package company.caller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.util.Log;

public abstract class EventRetriever extends AsyncTask<Contact, Event, Void>
{
	final String LOG_TAG = "EventRetriever";
    protected Context mContext;

	public EventRetriever(final Context c)
	{
		this.mContext  = c;

		// TODO Look for predefined number of events only (finish forcibly after reached)
	}

	@Override
	protected void onPreExecute()
	{
        Log.d(this.LOG_TAG, "onPreExecute");
        super.onPreExecute();

	    this.onSearchStarted();
	}

	@Override
    protected Void doInBackground(final Contact... params)
    {
        Log.d(this.LOG_TAG, "doInBackground");

		if (params == null)
		{
			// TODO No params error
			return null;
		}

		Contact contact = params[0];

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean prefEnableCallLogEvents = preferences.getBoolean("prefEnableCallLogEvents", true);
        boolean prefEnableCalendarEvents = preferences.getBoolean("prefEnableCalendarEvents", true);

        // store Contact info (number, name, email) into preferences to be used by PreferenceActivityNewEvent
        // these data will be stored until the next call is coming in
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("prefPhoneNumber", contact.getIncomingNumber());
        editor.putString("prefContactName", contact.name); // rewrite with null if no name
        if(contact.emails.size() > 0)
            editor.putString("prefContactEmail", contact.emails.get(0)); // rewrite with null if no emails
        editor.commit();

        if (prefEnableCallLogEvents) {
            ArrayList<Event> eventsCallLog = null;

            // Search for call log first
            for (String number : contact.numbers) {
                eventsCallLog = this.doSearchPhoneLogs(number);
                for(Event e : eventsCallLog) {
                    this.publishProgress(e);
                }
                eventsCallLog.clear();
            }
        }

        if (prefEnableCalendarEvents) {
            // Search for calendar events
            ArrayList<String> searchStrings = new ArrayList<String>();
            searchStrings.addAll(contact.numbers); // look for phone numbers
            searchStrings.addAll(contact.emails);  // ... emails
            if (contact.name != null)
                searchStrings.add(contact.name);   // ... name

            ArrayList<Event> eventsCalendar = null;
            ArrayList<Event> eventsAttendee = null;
            ArrayList<Event> eventsAttendeeClone = null;
            eventsCalendar = this.doSearchCalendarEvents(searchStrings);

            // Look for calendar events by attendees
            ArrayList<String> emails = new ArrayList<String>();
            for (String email : contact.emails) {
                emails.add(email);
            }
            eventsAttendee = this.doSearchCalendarAttendees(emails);
            eventsAttendeeClone = (ArrayList<Event>) eventsAttendee.clone();

            for(Event e1 : eventsCalendar) {
                int id = e1.getId();
                for(Event e2 : eventsAttendeeClone) {
                    if(id == e2.getId())
                        eventsAttendee.remove(e2);
                }
            }

            eventsCalendar.addAll(eventsAttendee);
            Collections.sort(eventsCalendar);
            for(Event e : eventsCalendar) {
                this.publishProgress(e);
            }

        }

	    return null;
    }

	@Override
	protected void onProgressUpdate(final Event... values)
	{
        Log.d(this.LOG_TAG, "onProgressUpdate");
        super.onProgressUpdate(values);

	    if ((values != null) && (values[0] != null))
	    {
	    	// TODO No event received error
	    	this.onNewEventFound(values[0]);
	    }
	}

	@Override
	protected void onPostExecute(final Void result)
	{
        Log.d(this.LOG_TAG, "onPostExecute");
        super.onPostExecute(result);

	    this.onSearchFinished();
	}

	abstract void onNewEventFound(Event event);

	abstract void onSearchStarted();

	abstract void onSearchFinished();

	private ArrayList<Event> doSearchPhoneLogs(final String phoneNum)
	{
        Log.d(this.LOG_TAG, "doSearchPhoneLogs(" + phoneNum + ")");
        ArrayList<Event> events = new ArrayList<Event>();

		String[] projection = new String[] {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int prefCallLogDepth = preferences.getInt("prefCallLogDepth", 20) + 1;

        String sortOrder = CallLog.Calls._ID + " DESC"; // last records will be first
        if (prefCallLogDepth != 0)
            sortOrder += " LIMIT '" + prefCallLogDepth + "'";

        Cursor cursor = this.mContext.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                "number='" + phoneNum + "'",
                null,
                sortOrder);

        cursor.moveToLast();
        while (cursor.moveToPrevious()) {
            String id = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));
            String callType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
            String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));

            // TODO Group consequent unanswered calls
            Event.EventType type;

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

                default:
                	type = Event.EventType.EVENT_UNDEFINED;
                	break;
            }

            events.add(new Event(mContext, Integer.parseInt(id), type, Long.parseLong(callDate), null, duration));
            Log.d(this.LOG_TAG, "Found call record");

        }

        cursor.close();
        return events;
	}

    private ArrayList<Event> doSearchCalendarEvents(List<String> _strings) {
        Log.d(this.LOG_TAG, "doSearchCalendarEvents");
        ArrayList<Event> events = new ArrayList<Event>();

        String[] projection = new String[] {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART
        };

        String selection = "";
        ArrayList<String> argsList = new ArrayList<String>();

        for (String s : _strings) {
            if (!selection.isEmpty())
                selection += " OR ";

            selection +=
                    CalendarContract.Events.TITLE + " LIKE ? OR " +
                            CalendarContract.Events.DESCRIPTION + " LIKE ?";

            argsList.add("%" + s + "%");
            argsList.add("%" + s + "%");
        }

        String[] selectionArgs = argsList.toArray(new String[0]);

        Cursor cursor = this.mContext.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );
        Log.d(this.LOG_TAG, "doSearchCalendarEvents found " + cursor.getCount() + " events");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Events._ID));
            String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE));
            String description = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            String start = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DTSTART));

            Event.EventType type = (doCheckReminder(Integer.parseInt(id))) ?
                    Event.EventType.EVENT_REMINDER :
                    Event.EventType.EVENT_CALENDAR;

            events.add(new Event(mContext, Integer.parseInt(id), type, Long.valueOf(start), title, description));
            Log.d(this.LOG_TAG, "Calendar event found. id: " + id + ", description: " + description);
        }

        cursor.close();
        return events;
	}


    /**
     * Checks if event has a reminder
     *
     * @param _id event id
     * @return true if specified event has a reminder, false otherwise
     */
    private boolean doCheckReminder(int _id) {
        Log.d(this.LOG_TAG, "doCheckReminder");

        boolean result = false;
        String[] projection = new String[]{CalendarContract.Reminders.EVENT_ID};

        Cursor cursor = CalendarContract.Reminders.query(
                this.mContext.getContentResolver(),
                _id,
                projection
        );

        if ((cursor != null) && (cursor.getCount() > 0)) {
            result = true;
            Log.d(this.LOG_TAG, "doCheckReminder found reminder");
            cursor.close();
        }

        return result;
    }


    /**
     *
     * @param _strings list of emails
     * @return events array
     */
	private ArrayList<Event> doSearchCalendarAttendees(ArrayList<String> _strings)
	{
        Log.d(this.LOG_TAG, "doSearchCalendarAttendees");
        for(String s : _strings) {
            Log.d(this.LOG_TAG, "email : " + s);
        }

        ArrayList<Event> events = new ArrayList<Event>();

        String title = "";
        String description = "";
        String start = "";

        String[] projection = new String[] {
                CalendarContract.Attendees.EVENT_ID,
                CalendarContract.Attendees.ATTENDEE_NAME,
                CalendarContract.Attendees.ATTENDEE_EMAIL
        };

        String selection = "";
        ArrayList<String> argsList = new ArrayList<String>();

        for (String s : _strings) {
            if (!selection.isEmpty())
                selection += " OR ";

            selection += CalendarContract.Attendees.ATTENDEE_EMAIL + " LIKE ? ";
            argsList.add("%" + s + "%");
        }
        String[] selectionArgs = argsList.toArray(new String[0]);

        Cursor cursor = this.mContext.getContentResolver().query(
                CalendarContract.Attendees.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        Log.d(this.LOG_TAG, "doSearchCalendarAttendees found " + cursor.getCount() + " records");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.EVENT_ID));
            String attendee = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));
            String email = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL));

            Log.d(this.LOG_TAG, "doSearchCalendarAttendees found id: " + id + " attendee: " + attendee + " email: " + email);

            // if attendee was found, look for related event to get its time and description
            String[] projection2 = new String[]{
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DESCRIPTION,
                    CalendarContract.Events.DTSTART
            };

            String selection2 = CalendarContract.Events._ID + " = ?";
            String[] selectionArgs2 = new String[]{id};

            Cursor cursor2 = this.mContext.getContentResolver().query(
                    CalendarContract.Events.CONTENT_URI,
                    projection2,
                    selection2,
                    selectionArgs2,
                    null);

            // there should be only one record for certain id
            while (cursor2.moveToNext()) {

                title = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.TITLE));
                description = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                start = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DTSTART));
                Log.d(this.LOG_TAG, "Calendar event found");
            }

            cursor2.close();
            Event.EventType type = (doCheckReminder(Integer.parseInt(id))) ?
                    Event.EventType.EVENT_REMINDER :
                    Event.EventType.EVENT_CALENDAR;
            events.add(new Event(mContext, Integer.parseInt(id), type, Long.parseLong(start), title, description));

        }

        cursor.close();
        return events;
	}
}
