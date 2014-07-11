package company.caller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ba-nikolay on 2014/07/09.
 */
public class EventAdapter extends BaseAdapter {
    Context context;
    LayoutInflater inflater;
    ArrayList<Event> events;

    EventAdapter(Context context, ArrayList<Event> events) {
        this.context = context;
        this.events = events;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.event, parent, false);
        }

        Event event = getEvent(position);

        ((TextView) view.findViewById(R.id.textDatetime)).setText(event.datetime);
        ((TextView) view.findViewById(R.id.textDescription)).setText(event.description + "");
        ImageView imageType = (ImageView) view.findViewById(R.id.iconType);
        switch(event.type) {
            case EVENT_INCOMING_CALL:
                imageType.setImageResource(R.drawable.ic_arrow_green);
                break;
            case EVENT_OUTGOING_CALL:
                imageType.setImageResource(R.drawable.ic_arrow_red);
                break;
            case EVENT_MISSED_CALL:
                imageType.setImageResource(R.drawable.ic_cross_red);
                break;
            case EVENT_CALENDAR:
                imageType.setImageResource(R.drawable.ic_calendar);
                break;
            case EVENT_REMINDER:
                imageType.setImageResource(R.drawable.ic_bell);
                break;
            case EVENT_UNDEFINED:
            default:
                imageType.setImageResource(R.drawable.ic_launcher);
                break;
        }

        return view;
    }

    Event getEvent(int position) {
        return ((Event) getItem(position));
    }
}
