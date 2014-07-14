package company.caller;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

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
    private View             view = null;
    private int              id;

    private ArrayList<Event> events = new ArrayList<Event>();
    private EventAdapter     adapter;


    @Override
    public String getAppName() {
        return "Caller";
    }



    @Override
    public int getAppIcon() {
        return 0;
    }



    @Override
    public void createAndAttachView(final int id, final FrameLayout frame) {
        Log.d(this.LOG_TAG, ": createAndAttachView");

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        this.view = inflater.inflate(R.layout.top_window, frame, true);
        this.id = id;
        if(adapter != null)
            adapter.clear();
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
    public int getFlags(final int id) {
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
    public void onReceiveData(final int id, final int requestCode, final Bundle data,
                              final Class<? extends StandOutWindow> fromCls, final int fromId) {
        Log.d(this.LOG_TAG, ": onReceiveData");

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
                Log.d(this.LOG_TAG, ": onReceiveData(phoneNumber = " + number + ")");

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
        Log.d(this.LOG_TAG, ": listContactEvents");

        String name = getContactName(number);
        Contact contact = getContactInfo(number);

/*
        if ((name != null) && (!name.isEmpty())) {
            setTitle(this.id, name);
        }
        else {
            setTitle(this.id, number);
        }

        PhoneDataRetriever retriever = new PhoneDataRetriever(this);
        // TODO Pass name and number as SearchParams class members instead of array elements
        retriever.execute(number, name);

        this.createEventList(this.events);
*/
    }



    /**
     * Constructs ListView from Events array. Uses custom EventAdapter
     *
     * @param events
     *              array of Events to be converted to ListView
     * @see EventAdapter
     */
    private void createEventList(final ArrayList<Event> events) {
        Log.d(this.LOG_TAG, ": createEventList");

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
        Log.d(this.LOG_TAG, ": getContactName");

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
                Log.d(this.LOG_TAG, ": Contact name for number " + number + " is " + name);
            } else {
                Log.d(this.LOG_TAG, ": Contact not found for number " + number);
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
        Log.d(this.LOG_TAG, ": getContactInfo");

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

                Log.d(this.LOG_TAG, ": Contact found. Id: " + id);
            }
            else {
                Log.d(this.LOG_TAG, ": Contact not found");
                return null;
            }
            cursor.close();
        }

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

                    Log.d(this.LOG_TAG, ": Data: data: " + data + ", mime = " + mime);

                    if(mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        // TODO Need to define number normalization rules (stripe '-', '+', '(', ')', etc.)
                        contact.numbers.add(data.replace("-", ""));
                    }
                    else if(mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                        contact.emails.add(data.toLowerCase());
                    }
                } while(cursor2.moveToNext());
            }
            else {
                Log.d(this.LOG_TAG, ": Contact not found");
            }
            cursor2.close();
        }
/*
        for(String number: contact.numbers)
            Log.d(this.LOG_TAG, ": phone: " + number);

        for(String email: contact.emails)
            Log.d(this.LOG_TAG, ": email: " + email);
*/
        return contact;
    }




    private class PhoneDataRetriever extends EventRetriever
	{
        private final String LOG_TAG = this.getClass().toString();

		public PhoneDataRetriever(final Context c)
        {
            super(c);
            Log.d(this.LOG_TAG, ": PhoneDataRetriever");
        }

		@Override
        void onNewEventFound(final Event event)
        {
	        TopWindow.this.adapter.add(event);
            ListView listRecords = (ListView) TopWindow.this.view.findViewById(R.id.listView);
            listRecords.smoothScrollToPosition(TopWindow.this.adapter.getCount() - 1);
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
        }
	}
}
