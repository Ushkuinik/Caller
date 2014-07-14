package company.caller;

import java.util.ArrayList;

/**
 * Detailed information about contact
 */
public class Contact {


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

    Contact() {
        numbers = new ArrayList<String>();
        emails = new ArrayList<String>();
    }
}
