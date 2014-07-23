package company.caller;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.Calendar;


/**
 *
 *
 */
public class PrefCalendar extends PreferenceActivity{
    final String LOG_TAG = this.getClass().toString();
    private EditText mEditTitle;
    private PreferenceFragmentNewEvent mPreferenceFragmentNewEvent;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferenceFragmentNewEvent = new PreferenceFragmentNewEvent();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mPreferenceFragmentNewEvent).commit();
        setTitle(R.string.calendar_activity_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_new_calendar_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                saveCalendarEvent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        Log.d(this.LOG_TAG, "onPreferenceStartFragment");

        Toast.makeText(getApplicationContext(), "onPreferenceStartFragment", Toast.LENGTH_SHORT).show();

        return super.onPreferenceStartFragment(caller, pref);
    }

*/
    private void saveCalendarEvent() {
        Log.d(this.LOG_TAG, "saveCalendarEvent");

        String title = mPreferenceFragmentNewEvent.findPreference("event_title").getSummary().toString();
        String description = mPreferenceFragmentNewEvent.findPreference("event_description").getSummary().toString();

        long dateCurrentTZ = ((PreferenceDate) mPreferenceFragmentNewEvent.findPreference("event_date")).getDate().getTimeInMillis();
        long timeCurrentTZ = ((PreferenceTime) mPreferenceFragmentNewEvent.findPreference("event_time")).getTime().getTimeInMillis();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateCurrentTZ);

        int offset = c.getTimeZone().getOffset(dateCurrentTZ);
        long timeGMT = timeCurrentTZ + offset; // remove timezone offset from time
        long datetimeCurrentTZ = dateCurrentTZ + timeGMT; // timezone offset already included in date, therefor use only GMT time

        Log.d(this.LOG_TAG, "date_time_current_tz: " + datetimeCurrentTZ);


        if( (title != null) && (description != null) )
        {
            long calID = 3;

            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, datetimeCurrentTZ);
            values.put(CalendarContract.Events.DTEND, datetimeCurrentTZ + 60 * 60 * 1000); // add one hour
            values.put(CalendarContract.Events.TITLE, "Jazzercise");
            values.put(CalendarContract.Events.DESCRIPTION, "Group workout");
            values.put(CalendarContract.Events.CALENDAR_ID, calID);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "America/Los_Angeles");
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

            // get the event ID that is the last element in the Uri
            long eventID = Long.parseLong(uri.getLastPathSegment());
            //
            // ... do something with event ID
            //
            //
        }

//        Toast.makeText(getApplicationContext(), "Save button was pressed! Title: " + s, Toast.LENGTH_SHORT).show();

    }
}
