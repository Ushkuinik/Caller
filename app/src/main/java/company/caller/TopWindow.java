package company.caller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

/**
 * The Window, which will float over Call screen when call incomes.
 * It will be shown and closed by CallDetectService.
 * The window will contain a list of known activity related to calling contact such as call history
 * and calendar appointments
 *
 * @see StandOutWindow
 */
public class TopWindow extends StandOutWindow {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    private final String LOG_TAG = this.getClass().toString();
    private View view = null;
    private int id;    // window id

    private ArrayList<Event> events = new ArrayList<Event>();
    private EventAdapter     adapter;

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {

            if (myIntent.getAction().equals(BCAST_CONFIGCHANGED)) {

                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y / 2;

                Window window = getWindow(id);

                if( (window != null) && (window.edit() != null)) {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Log.d(LOG_TAG, "LANDSCAPE, width: " + width + ", height: " + height);
                        window.edit().setSize(width, height).commit();
                    }
                    else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Log.d(LOG_TAG, "PORTRAIT, width: " + width + ", height: " + height);
                        window.edit().setSize(width, height).commit();
                    }
                }
            }
        }
    };



    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(this.LOG_TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }



    @Override
    public String getAppName() {
        return "Caller";
    }



    @Override
    public int getAppIcon() {
        return R.drawable.ic_cross_gray;
    }


    /**
     *
     * @param id
     *            The id representing the window.
     * @param frame - body frame to be filled by custom content
     */
    @Override
    public void createAndAttachView(final int id, final FrameLayout frame) {
        Log.d(this.LOG_TAG, "createAndAttachView");

        //LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        this.view = layoutInflater.inflate(R.layout.top_window, frame, true);
        this.id = id;

        if(adapter != null)
            adapter.clear();

        View btnQuit = view.findViewById(R.id.btnQuit);
        btnQuit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(TopWindow.this);
                int prefEnableCallLogEvents = preferences.getInt("prefShutdownDelay", 0);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        StandOutWindow.closeAll(TopWindow.this, TopWindow.class);
                    }
                }, prefEnableCallLogEvents * 1000);
            }
        });


        View btnSettings = view.findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Settings was pressed!", Toast.LENGTH_SHORT).show();
            }
        });

        View btnCalendar = view.findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Calendar was pressed!", Toast.LENGTH_SHORT).show();
            }
        });

        View txtTitle = view.findViewById(R.id.txtTitle);
        txtTitle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Title was pressed!", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public StandOutLayoutParams getParams(final int id, final wei.mark.standout.ui.Window window) {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        return new StandOutLayoutParams(
                id,
                width,    // width
                height / 2,    // height
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
    public int getFlags(final int id) {
        int flags = StandOutFlags.FLAG_DECORATION_CLOSE_DISABLE
                | StandOutFlags.FLAG_DECORATION_RESIZE_DISABLE
                | StandOutFlags.FLAG_DECORATION_MAXIMIZE_DISABLE
                | StandOutFlags.FLAG_DECORATION_MOVE_DISABLE
//                | StandOutFlags.FLAG_BODY_MOVE_ENABLE
//                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH
//                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
//                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE // StandOutWindow incorrectly checks screen boundaries after phone rotated
                | StandOutFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE
                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_ALL_DISABLE
//                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE
                | StandOutFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE;

        flags &= ~StandOutFlags.FLAG_DECORATION_SYSTEM; // switch off DECORATION_SYSTEM to disable window title
        Log.d(this.LOG_TAG, "getFlags: " + flags);
        return flags;
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
    public void onReceiveData(final int id, final int requestCode, final Bundle data,
                              final Class<? extends StandOutWindow> fromCls, final int fromId) {
        Log.d(this.LOG_TAG, "onReceiveData");

        switch (requestCode) {
            case CallDetectService.GOT_PHONE_NUMBER:
                Window window = getWindow(id);
                if (window == null) {
                	// TODO Log error
                    String errorText = String.format(Locale.US,
                            "%s received data but TopWindow id: %d is not open.",
                            getAppName(), id);
                    Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show();
                    return;
                }
                String number = data.getString("phoneNumber");
                number = number.replace("-", "");
                Log.d(this.LOG_TAG, "onReceiveData(phoneNumber = " + number + ")");

                this.listContactEvents(number);
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
    private void listContactEvents(final String number) {
        Log.d(this.LOG_TAG, "listContactEvents");

        Contact contact = getContactInfo(number);

        contact.name = getContactName(number);

        if ((contact.name != null) && (!contact.name.isEmpty())) {
            setTitle(contact.name);
        }
        else {
            setTitle(number);
        }

        PhoneDataRetriever retriever = new PhoneDataRetriever(this);

        retriever.execute(contact);

        this.createEventList(this.events);
    }



    public void setTitle(final String _title)
    {
        ((TextView)view.findViewById(R.id.txtTitle)).setText(_title);
    }



    /**
     * Constructs ListView from Events array. Uses custom EventAdapter
     *
     * @param events
     *              array of Events to be converted to ListView
     * @see EventAdapter
     */
    private void createEventList(final ArrayList<Event> events) {
        Log.d(this.LOG_TAG, "createEventList");

        ListView listRecords = (ListView) this.view.findViewById(R.id.listView);
        this.adapter = new EventAdapter(this, events);
        listRecords.setAdapter(this.adapter);

        // scroll to the bottom
        //listRecords.setSelection(this.adapter.getCount() - 1);
    }


    /**
     * Looks for contact name
     *
     * @param number
     *              phone number of incoming call
     * @return      contact name if it is in the phone book, otherwise <tt>null</tt>
     * @see ContactsContract
     */
    private String getContactName(final String number) {
        Log.d(this.LOG_TAG, "getContactName");

        String name = null;

        // we look for only contact name
        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME
        };

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

        if(cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.d(this.LOG_TAG, "Contact name for number " + number + " is " + name);
            } else {
                Log.d(this.LOG_TAG, "Contact not found for number " + number);
            }
            cursor.close();
        }
        return name;
    }



    /**
     * Looks for contact info by phone number
     *
     * @param _number
     *              phone number of incoming call
     * @return      filled Contact object in contact is in phone book, otherwise <tt>null</tt>
     * @see ContactsContract
     * @see Contact
     */
    private Contact getContactInfo(final String _number) {
        Log.d(this.LOG_TAG, "getContactInfo");

        Contact contact = new Contact();

        // 1. Get contact id
        String[] projection = new String[] {
                ContactsContract.PhoneLookup._ID,
        };

        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(_number));

        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {

                contact.id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

                Log.d(this.LOG_TAG, "Contact found. Id: " + id);

                // 2. Get all phones and emails, related to this contact id
                String[] projection2 = new String[] {
                        ContactsContract.Data._ID,
                        ContactsContract.Data.DATA1,
                        ContactsContract.Data.MIMETYPE

                };

                String select2 = ContactsContract.Data.CONTACT_ID + "=?" + " AND ("
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' OR "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "')";

                String [] selectArgs2 = new String[] {contact.id};

                Cursor cursor2 = getContentResolver().query(
                        ContactsContract.Data.CONTENT_URI,
                        projection2,
                        select2,
                        selectArgs2,
                        null);

                if(cursor2 != null) {
                    if (cursor2.moveToFirst()) {
                        do {
                            String data = cursor2.getString(cursor2.getColumnIndex(ContactsContract.Data.DATA1));
                            String mime = cursor2.getString(cursor2.getColumnIndex(ContactsContract.Data.MIMETYPE));

                            Log.d(this.LOG_TAG, "Data: data: " + data + ", mime = " + mime);

                            if(mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                                // TODO Need to define number normalization rules (stripe '-', '+', '(', ')', etc.)
                                contact.numbers.add(data.replace("-", ""));
                                contact.numbers.add(data.replace("+7", "8"));
                                contact.numbers.add(data.replace("(", ""));
                                contact.numbers.add(data.replace(")", ""));
                            }
                            else if(mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                                contact.emails.add(data.toLowerCase());
                            }
                        } while(cursor2.moveToNext());
                    }
                    else {
                        Log.d(this.LOG_TAG, "Contact not found");
                    }
                    cursor2.close();
                }
            }
            else {
                Log.d(this.LOG_TAG, "Contact not found");
            }
            cursor.close();
        }

        return contact;
    }



    /**
     *
     */
    private class PhoneDataRetriever extends EventRetriever
	{
        private final String LOG_TAG = this.getClass().toString();

		public PhoneDataRetriever(final Context c)
        {
            super(c);
            Log.d(this.LOG_TAG, "PhoneDataRetriever");
        }

		@Override
        void onNewEventFound(final Event event)
        {
            TopWindow.this.adapter.add(event);
            ListView listRecords = (ListView) TopWindow.this.view.findViewById(R.id.listView);
            listRecords.setSelection(TopWindow.this.adapter.getCount() - 1);
//          listRecords.smoothScrollToPosition(TopWindow.this.adapter.getCount() - 1);
        }

		@Override
        void onSearchStarted()
        {
	        // TODO Show infinite progress...
        }

		@Override
        void onSearchFinished()
        {
	        // TODO Hide infinite progress
            // Close window if no events were found
            if(TopWindow.this.adapter.getCount() == 0)
                close(TopWindow.this.id);
        }
	}
}
