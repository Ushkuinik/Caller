package company.caller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.util.Log;

public abstract class EventRetriever extends AsyncTask<String, Event, Void>
{
	protected Context mContext;
	final String LOG_TAG = "EventRetriever";

	public EventRetriever(final Context c)
	{
		this.mContext  = c;

		// TODO Look for predefined number of events only (finish forcibly after reached)
	}

	@Override
	protected void onPreExecute()
	{
        Log.d(this.LOG_TAG, ": onPreExecute");
	    super.onPreExecute();

	    this.onSearchStarted();
	}

	@Override
    protected Void doInBackground(final String... params)
    {
        Log.d(this.LOG_TAG, ": doInBackground");

		if ((params == null) || (params[0].isEmpty()))
		{
			// TODO No params error
			return null;
		}

		String phoneNo = params[0];
        try {

            // Do phone logs search first
            this.doSearchPhoneLogs(phoneNo);

            if (params[1] == null)
            {
                // TODO No "name" param
                return null;
            }

            String contact = params[1].toLowerCase();

            TimeUnit.SECONDS.sleep(2);
            this.doSearchCalendarEvents(contact);
            this.doSearchCalendarAttendees(contact);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	    return null;
    }

	@Override
	protected void onProgressUpdate(final Event... values)
	{
        Log.d(this.LOG_TAG, ": onProgressUpdate");
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
        Log.d(this.LOG_TAG, ": onPostExecute");
	    super.onPostExecute(result);

	    this.onSearchFinished();
	}

	abstract void onNewEventFound(Event event);

	abstract void onSearchStarted();

	abstract void onSearchFinished();

	private void doSearchPhoneLogs(final String phoneNum)
	{
		Log.d(this.LOG_TAG, ": doSearchPhoneLogs");

		String[] projection = new String[] {
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        Cursor cursor = this.mContext.getContentResolver().query(
        		CallLog.Calls.CONTENT_URI, projection, "number='" + phoneNum + "'", null, null);

        while (cursor.moveToNext()) {
            String callType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String callDate = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
            Date callDayTime = new Date(Long.valueOf(callDate));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = "Duration is " + cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION)) + " sec";

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

	private void doSearchCalendarEvents(final String contactName)
	{
		Log.d(this.LOG_TAG, ": doSearchCalendarEvents");

        String[] projection = new String[] {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART
        };

        // FIXME Look for specified contact name only (if possible)
        Cursor cursor = this.mContext.getContentResolver().query(CalendarContract.Events.CONTENT_URI, projection, null, null, null);
        Log.d(this.LOG_TAG, ": doSearchCalendarEvents found " + cursor.getCount() + " events");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Events._ID));
            String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE));
            String desc = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION));
            title = (title == null) ? "" : title;
            desc = (desc == null) ? "" : desc;

            /*
            // Get attendees for this event
            String[] projection2 = new String[] {
                    CalendarContract.Attendees.EVENT_ID,
                    CalendarContract.Attendees.ATTENDEE_NAME
            };

            String selection2 = CalendarContract.Attendees.EVENT_ID + "=?";
            String[] selectionArgs2 = new String[] {id};
            foundAttendee = false;

            Cursor cursor2 = this.mContext.getContentResolver().query(
                    CalendarContract.Attendees.CONTENT_URI,
                    projection2,
                    selection2,
                    selectionArgs2,
                    null);
//            Log.d(this.LOG_TAG, ": doSearchCalendarEvents found " + cursor2.getCount() + " attendees for event id: " + id);

            while (cursor2.moveToNext()) {
                String attendee = cursor2.getString(cursor2.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));

                Log.d(this.LOG_TAG, ": doSearchCalendarEvents found attendee:" + attendee);

                if (attendee.toLowerCase().contains(contactName)) {
                    foundAttendee = true;
                    break;
                }
            }
            cursor2.close();
            */

            String start = cursor.getString(cursor.getColumnIndex(CalendarContract.Events.DTSTART));
            Date callDayTime = new Date(Long.valueOf(start));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            String datetime = formatter.format(callDayTime);
            String description = title + " // " + desc;

            if ((title.toLowerCase().contains(contactName)) ||
                (desc.toLowerCase().contains(contactName))) {
            	Event newEvent = new Event(Event.EventType.EVENT_CALENDAR, datetime, description);
                Log.d(this.LOG_TAG, ": Calendar event found. id: " + id + ", description: " + description);

                this.publishProgress(newEvent);
            }
        }

        cursor.close();
	}

	private void doSearchCalendarAttendees(String _email)
	{
		Log.d(this.LOG_TAG, ": doSearchCalendarAttendees");

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
        Log.d(this.LOG_TAG, ": doSearchCalendarAttendees found " + cursor.getCount() + " records");

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.EVENT_ID));
            String attendee = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));
            String email = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));

            Log.d(this.LOG_TAG, ": doSearchCalendarAttendees found attendee: " + attendee);

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
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

                    datetime = formatter.format(callDayTime);
                    description = title + " // " + desc;
                    Log.d(this.LOG_TAG, ": Calendar event found. Description: " + description);
                }

                cursor2.close();

                Event newEvent = new Event(Event.EventType.EVENT_CALENDAR, datetime, description);
                this.publishProgress(newEvent);
            }

        }

        cursor.close();
	}
}
