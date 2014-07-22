package company.caller;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 *
 *
 */
public class PrefCalendar extends PreferenceActivity{
    final String LOG_TAG = this.getClass().toString();
    private EditText mEditTitle;
    private PrefFragment m_PrefFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_PrefFragment = new PrefFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, m_PrefFragment).commit();
        setTitle(R.string.calendar_activity_title);

//        mEditTitle = (EditText) findViewById(R.id.event_title);
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

        String title = m_PrefFragment.findPreference("event_title").getSummary().toString();
        String description = m_PrefFragment.findPreference("event_description").getSummary().toString();

        long dateCurrentTZ = ((PreferenceDate)m_PrefFragment.findPreference("event_date")).getDate().getTimeInMillis();
        long timeCurrentTZ = ((PreferenceTime)m_PrefFragment.findPreference("event_time")).getTime().getTimeInMillis();

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


    /**
     *
     */
    static public class PrefFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        final String LOG_TAG = this.getClass().toString();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(this.LOG_TAG, "onCreate");
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.advanced_preferences);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.advanced_preferences, false);

            // populate Calendar list on creation
            final ListPreference listCalendar = (ListPreference) findPreference("list_calendar");
            setListPreferenceData(listCalendar);

            initSummary(getPreferenceScreen());        }

        @Override
        public void onResume() {
            Log.d(this.LOG_TAG, "onResume");
            super.onResume();
            getPreferenceScreen().getSharedPreferences().
                    registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(this.LOG_TAG, "onSharedPreferenceChanged");

            updatePrefSummary(findPreference(key));
        }

        @Override
        public void onPause() {
            Log.d(this.LOG_TAG, "onPause");
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }



        /**
         *
         * @param _preference
         */
        private void initSummary(Preference _preference) {
            Log.d(this.LOG_TAG, "initSummary");

            if (_preference instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup) _preference;
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    initSummary(group.getPreference(i));
                }
            }
            else {
                updatePrefSummary(_preference);
            }
        }



        /**
         *
         * @param _preference
         */
        private void updatePrefSummary(Preference _preference) {
            Log.d(this.LOG_TAG, "updatePrefSummary");

            if (_preference instanceof ListPreference) {
                ListPreference p = (ListPreference) _preference;
                _preference.setSummary(p.getEntry());
            }
            else if (_preference instanceof EditTextPreference) {
                EditTextPreference p = (EditTextPreference) _preference;
                _preference.setSummary(p.getText());
            }
        }

        protected static void setListPreferenceData(ListPreference _listPreference) {
            CharSequence[] entries = { "Calendar1", "Calendar2" };
            CharSequence[] entryValues = {"1" , "2"};
            _listPreference.setEntries(entries);
            _listPreference.setDefaultValue("1");
            _listPreference.setEntryValues(entryValues);
        }
    }
}
