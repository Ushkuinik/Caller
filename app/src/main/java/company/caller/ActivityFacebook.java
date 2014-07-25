package company.caller;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 *
 */
public class ActivityFacebook extends FragmentActivity {
    private FragmentFacebook fragmentFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            fragmentFacebook = new FragmentFacebook();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragmentFacebook)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            fragmentFacebook = (FragmentFacebook) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
    }
}
