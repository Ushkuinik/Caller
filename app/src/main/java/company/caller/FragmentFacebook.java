package company.caller;

//import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.WebDialog;

import java.util.Arrays;

/**
 *
 */
public class FragmentFacebook extends Fragment {

    private static final String LOG_TAG = "FragmentFacebook";
    private UiLifecycleHelper uiHelper;
    private Bundle savedInstanceState;
    private Button buttonLogin;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        Log.d(this.LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }



    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            Log.d(LOG_TAG, "StatusCallback");
            onSessionStateChange(session, state, exception);
        }
    };



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(this.LOG_TAG, "onCreateView");

        View view = inflater.inflate(R.layout.facebook, container, false);

        LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
        authButton.setFragment(this);
        authButton.setReadPermissions(Arrays.asList("user_events"));

        Button buttonLogin = (Button) view.findViewById(R.id.buttonLogin);

        return view;
    }



    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        Log.d(this.LOG_TAG, "onSessionStateChange");

        if (state.isOpened()) {
            Log.d(LOG_TAG, "Logged in");
            makeEventRequest();

            if(buttonLogin != null)
                buttonLogin.setText("Logout");
        }
        else if (state.isClosed()) {
            Log.d(LOG_TAG, "Logged out");

            if(buttonLogin != null)
                buttonLogin.setText("Login");
        }
    }


    @Override
    public void onResume() {
        Log.d(this.LOG_TAG, "onResume");
        super.onResume();

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
//            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(this.LOG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        Log.d(this.LOG_TAG, "onPause");
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(this.LOG_TAG, "onDestroy");
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(this.LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }


    private void makeEventRequest() {
        Log.d(this.LOG_TAG, "makeEventRequest");
        Request meRequest = Request.newMeRequest(Session.getActiveSession(),
                new Request.GraphUserCallback() {

                    /**
                     * The method that will be called when the request completes.
                     *
                     * @param user     the GraphObject representing the returned user, or null
                     * @param response the Response of this request, which may include error information if the request was unsuccessful
                     */
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        Log.d(LOG_TAG, "response: " + response.getRawResponse());
                    }
                }
        );

        Bundle requestParams = meRequest.getParameters();
        requestParams.putString("fields", "id,name");
        meRequest.setParameters(requestParams);
        meRequest.executeAsync();
    }
}
