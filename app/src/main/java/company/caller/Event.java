package company.caller;

/**
 *
 */
public class Event {
    public EventType type;
    public String datetime;
    public String description;

    Event(EventType _type, String _datetime, String _description) {
        type = _type;
        datetime = _datetime;
        description = _description;
    }

    public enum EventType {
        EVENT_UNDEFINED,
        EVENT_INCOMING_CALL,
        EVENT_OUTGOING_CALL,
        EVENT_MISSED_CALL,
        EVENT_CALENDAR,
        EVENT_REMINDER
    }
}
