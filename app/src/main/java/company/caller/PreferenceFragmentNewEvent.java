package company.caller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PreferenceFragmentNewEvent extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    final String LOG_TAG = this.getClass().toString();

    private String mNumber;
    private String mName;
    private String mEmail;

    Context mContext = null;

    public PreferenceFragmentNewEvent() {
        Log.d(this.LOG_TAG, "PreferenceFragmentNewEvent");
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        Log.d(this.LOG_TAG, "setArguments");

        mNumber = args.getString("number");
        mName = args.getString("name");
        mEmail = args.getString("email");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.LOG_TAG, "onCreate");

        addPreferencesFromResource(R.xml.preferences_new_calendar_event);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences_new_calendar_event, false);

        // populate Calendar list on creation
        final ListPreference listCalendar = (ListPreference) findPreference("list_calendar");
        setListPreferenceData(listCalendar);

        String summary;
        if((mEmail != null) && (mName != null)) {
            summary = String.format(getResources().getString(R.string.strDefaultEventDescription), mName);//   "Event related to " + mName + ".");
            findPreference("event_description").setSummary(summary);
        } else if(mName == null) {
            summary = String.format(getResources().getString(R.string.strDefaultEventDescription), mNumber);//   "Event related to " + mName + ".");
            findPreference("event_description").setSummary(summary);
        }

        initSummary(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        Log.d(this.LOG_TAG, "onResume");
        super.onResume();
        getPreferenceScreen().getSharedPreferences().
                registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(this.LOG_TAG, "onAttach");
        mContext = getActivity();
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
            String summary = (p.getEntry() != null) ? (String) p.getEntry() : (String) p.getSummary();
            _preference.setSummary(summary);
            Log.d(this.LOG_TAG, "updatePrefSummary, list summary: " + summary);
        }
        else if (_preference instanceof EditTextPreference) {
            EditTextPreference p = (EditTextPreference) _preference;
            String summary = (p.getText() != null) ? p.getText() : (String) p.getSummary();
            _preference.setSummary(summary);
            Log.d(this.LOG_TAG, "updatePrefSummary,text  summary:　" + summary);
        }
        else if (_preference instanceof PreferenceDate) {
            PreferenceDate p = (PreferenceDate) _preference;

            SimpleDateFormat formatter = new SimpleDateFormat(getResources().getString(R.string.date_format));
            String summary = formatter.format(new Date(p.getDate().getTimeInMillis()));
            _preference.setSummary(summary);
            Log.d(this.LOG_TAG, "updatePrefSummary, date summary:　" + summary);
        }
        else if (_preference instanceof PreferenceTime) {
            PreferenceTime p = (PreferenceTime) _preference;

            SimpleDateFormat formatter = new SimpleDateFormat(getResources().getString(R.string.time_format));
            String summary = formatter.format(new Date(p.getTime().getTimeInMillis()));
            _preference.setSummary(summary);
            Log.d(this.LOG_TAG, "updatePrefSummary, time summary:　" + summary);
        }
    }


    /**
     *
     */
    public void clearPreferences() {
        Log.d(this.LOG_TAG, "clearPreferences");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        sharedPreferences.edit().clear().commit();
    }



    /**
     *
     * @param _listPreference
     */
    protected void setListPreferenceData(ListPreference _listPreference) {

        ArrayList<CharSequence> names = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        Map<Long, String> calendars = enumCalendars();

        for (Map.Entry<Long, String> calendar : calendars.entrySet())
        {
            names.add(calendar.getValue());
            values.add(calendar.getKey().toString());

        }
        _listPreference.setEntries((CharSequence[]) names.toArray(new CharSequence[names.size()]));
        _listPreference.setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
        if(names.size() > 0)
            _listPreference.setDefaultValue(names.get(0));
    }



    /**
     *
     */
    private Map<Long, String> enumCalendars() {
        Log.d(this.LOG_TAG, "enumCalendars");

        Map<Long, String> calendars = new HashMap<Long, String>();

        // The indices for the projection array above.
        final int PROJECTION_ID_INDEX = 0;
        final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        final int PROJECTION_DISPLAY_NAME_INDEX = 2;
        final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;

        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };

        Cursor cursor = null;
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        if(mContext == null) {
            Log.d(this.LOG_TAG, "enumCalendars: no context! unable to get ContentResolver");
        }
        else {
            cursor = mContext.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    null);
            while(cursor.moveToNext()) {

                long id = cursor.getLong(PROJECTION_ID_INDEX);
                String name = cursor.getString(PROJECTION_DISPLAY_NAME_INDEX);
                String account = cursor.getString(PROJECTION_ACCOUNT_NAME_INDEX);

                calendars.put(id, name + " (" + account + ")");

            }
        }
        return calendars;
    }
}
