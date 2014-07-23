package company.caller;

import java.util.ArrayList;

/**
 * Detailed information about contact
 */
public class Contact {

    private final int INDEX_INCOMING_NUMBER = 0;
    /**
     * Displayed contacts name
     */
    String name;

    String id;

    /**
     * Phone number
     */
    ArrayList<String> numbers;

    /**
     * eMails
     */
    ArrayList<String> emails;


    /**
     *
     * @param _number incoming number. It will always be the first in the array
     */
    Contact(String _number) {
        // TODO: If _number is invalid, throw exception
        numbers = new ArrayList<String>();
        emails = new ArrayList<String>();
        numbers.add(_number);
    }

    public String getIncomingNumber() {
        return numbers.get(INDEX_INCOMING_NUMBER);
    }

}
