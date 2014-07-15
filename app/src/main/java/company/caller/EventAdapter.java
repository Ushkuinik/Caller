package company.caller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 *
 */
public class EventAdapter extends ArrayAdapter<Event> {
    private final String LOG_TAG = this.getClass().toString();
    private LayoutInflater inflater;
    private ArrayList<Event> events;

    EventAdapter(final Context context, final ArrayList<Event> events) {
    	super(context, R.layout.event, events);

        this.events   = events;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return this.events.size();
    }

    @Override
    public Event getItem(final int position) {
        return this.events.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
//        Log.d(this.LOG_TAG, ": getView");

        View view = convertView;
        if (view == null) {
            view = this.inflater.inflate(R.layout.event, parent, false);

        }

        Event event = getEvent(position);
//        Log.d(this.LOG_TAG, ": getView: event.desc: " + event.datetime + ", event.desc: " + event.description);

        ((TextView) view.findViewById(R.id.textDatetime)).setText(event.datetime);
        ((TextView) view.findViewById(R.id.textDescription)).setText(event.description);
//        TextView description = (TextView) view.findViewById(R.id.textDescription);
        ImageView imageType = (ImageView) view.findViewById(R.id.iconType);

        switch (event.type) {
            case EVENT_INCOMING_CALL:
                imageType.setImageResource(R.drawable.ic_arrow_green);
                //description.setVisibility(View.GONE);
                break;

            case EVENT_OUTGOING_CALL:
                imageType.setImageResource(R.drawable.ic_arrow_red);
                //description.setVisibility(View.GONE);
                break;

            case EVENT_MISSED_CALL:
                imageType.setImageResource(R.drawable.ic_cross_red);
                //description.setVisibility(View.GONE);
                break;

            case EVENT_CALENDAR:
                imageType.setImageResource(R.drawable.ic_calendar);
                //description.setText(event.description);
                break;

            case EVENT_REMINDER:
                imageType.setImageResource(R.drawable.ic_bell);
                //description.setText(event.description);
                break;

            case EVENT_UNDEFINED:
            default:
                imageType.setImageResource(R.drawable.ic_launcher);
                break;
        }
        return view;
    }

    Event getEvent(final int position) {
        return (this.getItem(position));
    }
}

