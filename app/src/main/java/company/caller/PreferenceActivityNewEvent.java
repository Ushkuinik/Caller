package company.caller;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
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

            case R.id.action_clear:
                mPreferenceFragmentNewEvent.clearPreferences();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void saveCalendarEvent() {
        Log.d(this.LOG_TAG, "saveCalendarEvent");

        String title = mPreferenceFragmentNewEvent.findPreference("event_title").getSummary().toString();
        String description = mPreferenceFragmentNewEvent.findPreference("event_description").getSummary().toString();
        String id = ((ListPreference)mPreferenceFragmentNewEvent.findPreference("list_calendar")).getValue();

        long dateCurrentTZ = ((PreferenceDate) mPreferenceFragmentNewEvent.findPreference("event_date")).getDate().getTimeInMillis();
        long timeCurrentTZ = ((PreferenceTime) mPreferenceFragmentNewEvent.findPreference("event_time")).getTime().getTimeInMillis();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateCurrentTZ);

        TimeZone timezone = c.getTimeZone();
        int offset = timezone.getOffset(dateCurrentTZ);
        long timeGMT = timeCurrentTZ + offset; // remove timezone offset from time
        long datetimeCurrentTZ = dateCurrentTZ + timeGMT; // timezone offset already included in date, therefor use only GMT time

        Log.d(this.LOG_TAG, "date_time_current_tz: " + datetimeCurrentTZ);
        Log.d(this.LOG_TAG, "calendar id: " + id);


        if( ((title != null) && (!title.isEmpty())) &&
            (description != null) &&
            ((id != null) && (Long.parseLong(id) != 0)) )
        {
            ContentResolver contentResolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, datetimeCurrentTZ);
            values.put(CalendarContract.Events.DTEND, datetimeCurrentTZ + 30 * 60 * 1000); // add half an hour
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.CALENDAR_ID, id);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, timezone.getID());
            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);

            // get the event ID that is the last element in the Uri
            long eventId = Long.parseLong(uri.getLastPathSegment());
            Toast.makeText(getApplicationContext(), "Event saved. Id: " + eventId, Toast.LENGTH_SHORT).show();
        }
        else {
            if((title == null) || (title.isEmpty())) {
                // TODO: focus to title
                Toast.makeText(getApplicationContext(), "Enter title", Toast.LENGTH_SHORT).show();
            }
            else if(description == null) {
                Toast.makeText(getApplicationContext(), "Incorrect description", Toast.LENGTH_SHORT).show();
            }
            else if((id == null) || (Long.parseLong(id) == 0))
                Toast.makeText(getApplicationContext(), "Calendar not set", Toast.LENGTH_SHORT).show();

        }
    }
    private void clearPreferences() {
        Log.d(this.LOG_TAG, "clearPreferences");
    }
}
