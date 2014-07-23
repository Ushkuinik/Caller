package company.caller;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;


/**
 *
 *
 */
public class PreferenceActivityNewEvent extends PreferenceActivity{
    final String LOG_TAG = this.getClass().toString();
    private EditText mEditTitle;
    private PreferenceFragmentNewEvent mPreferenceFragmentNewEvent;
    private String mNumber;
    private String mName;
    private String mEmail;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.LOG_TAG, "onCreate");

        // 1. If contact IS NOT in phone book: Description <- Event related to [number] {get Number from SharedPreferences}
        // 2. If contact IS in phone book, but HAS NO email: Description <- Event related to [name] {get Number+Name from SharedPreferences}
        // 3. If contact IS in phone book, a HAS email: Description <- Event related to [name], add contact as Attendee {get Number+Name+eMail from SharedPreferences}
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mNumber = preferences.getString("prefPhoneNumber", null);
        mName = preferences.getString("prefContactName", null);
        mEmail = preferences.getString("prefContactEmail", null);
        if(mNumber == null)
            throw new RuntimeException("Phone number should not be null");

        mPreferenceFragmentNewEvent = new PreferenceFragmentNewEvent();
        Bundle args = new Bundle();
        args.putString("number", mNumber);
        args.putString("name", mName);
        args.putString("email", mEmail);
        mPreferenceFragmentNewEvent.setArguments(args);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mPreferenceFragmentNewEvent).commit();
        setTitle(R.string.calendar_activity_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(this.LOG_TAG, "onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_new_calendar_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(this.LOG_TAG, "onOptionsItemSelected");

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                saveCalendarEvent();
                return true;

            case R.id.action_clear:
                mPreferenceFragmentNewEvent.clearPreferences();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void saveCalendarEvent() {
        Log.d(this.LOG_TAG, "saveCalendarEvent");

        String calendarId = ((ListPreference)mPreferenceFragmentNewEvent.findPreference("list_calendar")).getValue();
        String title = mPreferenceFragmentNewEvent.findPreference("event_title").getSummary().toString();
        String description = mPreferenceFragmentNewEvent.findPreference("event_description").getSummary().toString();

        long dateCurrentTZ = ((PreferenceDate) mPreferenceFragmentNewEvent.findPreference("event_date")).getDate().getTimeInMillis();
        long timeCurrentTZ = ((PreferenceTime) mPreferenceFragmentNewEvent.findPreference("event_time")).getTime().getTimeInMillis();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateCurrentTZ);

        TimeZone timezone = c.getTimeZone();
        int offset = timezone.getOffset(dateCurrentTZ);
        long timeGMT = timeCurrentTZ + offset; // remove timezone offset from time
        long datetimeCurrentTZ = dateCurrentTZ + timeGMT; // timezone offset already included in date, therefor use only GMT time

        if( ((title != null) && (!title.isEmpty())) &&
            (description != null) &&
            ((calendarId != null) && (Long.parseLong(calendarId) != 0)) )
        {
            ContentResolver contentResolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, datetimeCurrentTZ);
            values.put(CalendarContract.Events.DTEND, datetimeCurrentTZ + 30 * 60 * 1000); // add half an hour
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timezone.getID());
            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);

            // get the event ID that is the last element in the Uri
            long eventId = Long.parseLong(uri.getLastPathSegment());
            Toast.makeText(getApplicationContext(), "Event saved. Id: " + eventId, Toast.LENGTH_SHORT).show();

            if(this.mEmail != null)
                addAttendee(eventId, this.mName, this.mEmail);
        }
        else {
            if((title == null) || (title.isEmpty())) {
                // TODO: focus to title
                Toast.makeText(getApplicationContext(), "Enter title", Toast.LENGTH_SHORT).show();
            }
            else if(description == null) {
                Toast.makeText(getApplicationContext(), "Incorrect description", Toast.LENGTH_SHORT).show();
            }
            else if((calendarId == null) || (Long.parseLong(calendarId) == 0))
                Toast.makeText(getApplicationContext(), "Calendar not set", Toast.LENGTH_SHORT).show();
        }
    }



    private void addAttendee(long eventId, String mName, String mEmail) {
        Log.d(this.LOG_TAG, "addAttendee");

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Attendees.ATTENDEE_NAME, mName);
        values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, mEmail);
        values.put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE);
        values.put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_OPTIONAL);
        values.put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarContract.Attendees.ATTENDEE_STATUS_INVITED);
        values.put(CalendarContract.Attendees.EVENT_ID, eventId);
        Uri uri = cr.insert(CalendarContract.Attendees.CONTENT_URI, values);

        long attendeeId = Long.parseLong(uri.getLastPathSegment());
        Toast.makeText(getApplicationContext(), "Attendee added. Id: " + attendeeId, Toast.LENGTH_SHORT).show();
    }
}
