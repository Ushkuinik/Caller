package company.caller;

import android.content.Context;
import android.content.res.Resources;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Event class
 */
public class Event implements Comparable<Event>{

    private Context mContext;
    private int mId;
    private EventType mType;
    private long mTime; // date&time in milliseconds (UNIX time)
    private Date mDateTime; // date&time as Date object
    private String mTitle;
    private String mDescription;

    public enum EventType {
        EVENT_UNDEFINED,
        EVENT_INCOMING_CALL,
        EVENT_OUTGOING_CALL,
        EVENT_MISSED_CALL,
        EVENT_CALENDAR,
        EVENT_REMINDER
    }

    Event(Context _context, int _id, EventType _type, long _time, String _title, String _description) {
        mContext = _context;
        mId = _id;
        mType = _type;
        mTime = _time;
        mDateTime = new Date(mTime);
        mTitle = _title;
        mDescription = _description;

/*
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String datetime = formatter.format(callDayTime);


        Date callDayTime = new Date(Long.valueOf(callDate));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        String datetime = formatter.format(callDayTime);
        Resources r = this.mContext.getResources();
        String description = r.getString(R.string.strDurationIs) +
                cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION)) +
                r.getString(R.string.strSec);


        title = (title == null) ? "" : title;
        desc = (desc == null) ? "" : desc;

        Date callDayTime = new Date(Long.valueOf(start));

        SimpleDateFormat formatter = new SimpleDateFormat(mContext.getString(R.string.datetime_format));

        datetime = formatter.format(callDayTime);
        description = title + " // " + desc;
*/

    }

    /**
     * Compares this object to the specified object to determine their relative
     * order.
     *
     * @param _event the object to compare to this instance.
     * @return a negative integer if this instance is less than {@code another};
     * a positive integer if this instance is greater than
     * {@code _event}; 0 if this instance has the same order as
     * {@code _event}.
     * @throws ClassCastException if {@code another} cannot be converted into something
     *                            comparable to {@code this} instance.
     */
    @Override
    public int compareTo(Event _event) {
        return this.getDateTime().compareTo(_event.getDateTime());
    }

    public int getId() {
        return mId;
    }

    public Date getDateTime() {
        return mDateTime;
    }

    public String getDateTimeFormatted(String _format) {
        SimpleDateFormat formatter = new SimpleDateFormat(_format);
        return formatter.format(mDateTime);
    }

    public boolean isPast() {
        return (mDateTime.compareTo(new Date()) == -1);
    }

    public String getDescription() {

        String description = null;

        switch(mType) {
            case EVENT_INCOMING_CALL:
            case EVENT_OUTGOING_CALL:
                Resources r = this.mContext.getResources();
                description = r.getString(R.string.strDurationIs) + mDescription;
                break;

            case EVENT_MISSED_CALL:
                break;

            case EVENT_CALENDAR:
            case EVENT_REMINDER:
                description = mTitle + " // " + mDescription;
                break;
        }

        return description;
    }

    public EventType getType() {
        return mType;
    }
}
