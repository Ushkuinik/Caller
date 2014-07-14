package company.caller;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
        ViewHolder holder;


        View view = convertView;
        if (view == null) {
            view = this.inflater.inflate(R.layout.event, parent, false);

            holder = new ViewHolder();
            holder.datetime = (TextView) view.findViewById(R.id.textDatetime);
            holder.description = (TextView) view.findViewById(R.id.textDescription);
            holder.imageType = (ImageView) view.findViewById(R.id.iconType);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        Event event = getEvent(position);
        Log.d(this.LOG_TAG, ": getView: event.desc: " + event.datetime + ", event.desc: " + event.description);

//        ((TextView) view.findViewById(R.id.textDatetime)).setText(event.datetime);
        //((TextView) view.findViewById(R.id.textDescription)).setText(event.description);
//        TextView description = (TextView) view.findViewById(R.id.textDescription);
//        ImageView imageType = (ImageView) view.findViewById(R.id.iconType);
        holder.datetime.setText(event.datetime);
        switch (event.type) {
            case EVENT_INCOMING_CALL:
                holder.imageType.setImageResource(R.drawable.ic_arrow_green);
                holder.description.setVisibility(View.GONE);
                break;

            case EVENT_OUTGOING_CALL:
                holder.imageType.setImageResource(R.drawable.ic_arrow_red);
                holder.description.setVisibility(View.GONE);
                break;

            case EVENT_MISSED_CALL:
                holder.imageType.setImageResource(R.drawable.ic_cross_red);
                holder.description.setVisibility(View.GONE);
                break;

            case EVENT_CALENDAR:
                holder.imageType.setImageResource(R.drawable.ic_calendar);
                holder.description.setText(event.description);
                break;

            case EVENT_REMINDER:
                holder.imageType.setImageResource(R.drawable.ic_bell);
                holder.description.setText(event.description);
                break;

            case EVENT_UNDEFINED:
            default:
                holder.imageType.setImageResource(R.drawable.ic_launcher);
                break;
        }
        return view;
    }

    Event getEvent(final int position) {
        return (this.getItem(position));
    }


    static class ViewHolder {
        TextView datetime;
        TextView description;
        ImageView imageType;
    }

}

