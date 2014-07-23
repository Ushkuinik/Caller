package company.caller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            // Search for call log first
            for (String number : contact.numbers) {
                this.doSearchPhoneLogs(number);
            }
        }

        if (prefEnableCalendarEvents) {
            // Search for calendar events
            ArrayList<String> searchStrings = new ArrayList<String>();
            searchStrings.addAll(contact.numbers); // look for phone numbers
            searchStrings.addAll(contact.emails);  // ... emails
            if (contact.name != null)
                searchStrings.add(contact.name);   // ... name

            this.doSearchCalendarEvents(searchStrings);

            // Look for calendar events by attendees
            for (String email : contact.emails) {
                this.doSearchCalendarAttendees(email);
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

	private void doSearchPhoneLogs(final String phoneNum)
	{
        Log.d(this.LOG_TAG, "doSearchPhoneLogs");

		String[] projection = new String[] {
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int prefCallLogDepth = preferences.getInt("prefCallLogDepth", 0);

        String sortOrder = null;
        if (prefCallLogDepth != 0)
            sortOrder = CallLog.Calls._ID + " ASC LIMIT '" + prefCallLogDepth + "'";

        Cursor cursor = this.mContext.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                "number='" + phoneNum + "'",
                null,
                sortOrder);

        while (cursor.moveToNext()) {
            String callType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
            Date callDayTime = new Date(Long.valueOf(callDate));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            Resources r = this.mContext.getResources();
            String description = r.getString(R.string.strDurationIs) +
                    cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION)) +
                    r.getString(R.string.strSec);

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

            Event newEvent = new Event(type, datetime, description);
            this.publishProgress(newEvent);
        }

        cursor.close();
	}

    private void doSearchCalendarEvents(List<String> strings) {
        Log.d(this.LOG_TAG, "doSearchCalendarEvents");

        String[] projection = new String[] {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART
        };

        String selection = "";
        ArrayList<String> argsList = new ArrayList<String>();

        for (String s : strings) {
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
            String desc = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            String start = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DTSTART));
            title = (title == null) ? "" : title;
            desc = (desc == null) ? "" : desc;

            Date callDayTime = new Date(Long.valueOf(start));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = title + " // " + desc;

            Event.EventType type = Event.EventType.EVENT_CALENDAR;
            if (doCheckReminder(Long.parseLong(id)))
                type = Event.EventType.EVENT_REMINDER;

            Event newEvent = new Event(type, datetime, description);
            Log.d(this.LOG_TAG, "Calendar event found. id: " + id + ", description: " + description);

            this.publishProgress(newEvent);
        }

        cursor.close();
	}


    /**
     * Checks if event has a reminder
     *
     * @param _id event id
     * @return true if specified event has a reminder, false otherwise
     */
    private boolean doCheckReminder(long _id) {
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



	private void doSearchCalendarAttendees(String _email)
	{
        Log.d(this.LOG_TAG, "doSearchCalendarAttendees");

        String datetime = "";
        String description = "";
        if(_email == null)
            return;
        String email_lower = _email.toLowerCase();

        String[] projection = new String[] {
                CalendarContract.Attendees.EVENT_ID,
                CalendarContract.Attendees.ATTENDEE_NAME,
                CalendarContract.Attendees.ATTENDEE_EMAIL
        };

        Cursor cursor = this.mContext.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, projection, null, null, null);
        Log.d(this.LOG_TAG, "doSearchCalendarAttendees found " + cursor.getCount() + " records");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.EVENT_ID));
            String attendee = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));
            String email = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL));

            Log.d(this.LOG_TAG, "doSearchCalendarAttendees found attendee: " + attendee);

            // if attendee was found, look for related event to get its time and description
            if(email.toLowerCase().contains(email_lower)) {

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

                    String title = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.TITLE));
                    String desc = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DESCRIPTION));
                    String start = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Events.DTSTART));
                    title = (title == null) ? "" : title;
                    desc = (desc == null) ? "" : desc;

                    Date callDayTime = new Date(Long.valueOf(start));

                    SimpleDateFormat formatter = new SimpleDateFormat(mContext.getString(R.string.datetime_format));

                    datetime = formatter.format(callDayTime);
                    description = title + " // " + desc;
                    Log.d(this.LOG_TAG, "Calendar event found. Description: " + description);
                }

                cursor2.close();

                Event newEvent = new Event(Event.EventType.EVENT_CALENDAR, datetime, description);
                this.publishProgress(newEvent);
            }

        }

        cursor.close();
	}
}
