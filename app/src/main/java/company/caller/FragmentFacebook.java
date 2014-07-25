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
import java.util.List;

/**
 *
 */
public class FragmentFacebook extends Fragment {

    private static final String LOG_TAG = "FragmentFacebook";
    private UiLifecycleHelper uiHelper;
    private Bundle savedInstanceState;
    private Button sendRequestButton;


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
        authButton.setReadPermissions(Arrays.asList("user_likes", "user_status", "friends_birthday"));

        sendRequestButton = (Button) view.findViewById(R.id.sendRequestButton);
        sendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequestDialog();
            }
        });

        return view;
    }



    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        Log.d(this.LOG_TAG, "onSessionStateChange");

        if (state.isOpened()) {
            Log.d(LOG_TAG, "Logged in");
            makeFriendsRequest();
            sendRequestButton.setVisibility(View.VISIBLE);
        }
        else if (state.isClosed()) {
            Log.d(LOG_TAG, "Logged out");
            sendRequestButton.setVisibility(View.INVISIBLE);
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
            onSessionStateChange(session, session.getState(), null);
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


    private void makeFriendsRequest() {
        Log.d(this.LOG_TAG, "makeFriendsRequest");
        Request myFriendsRequest = Request.newMyFriendsRequest(Session.getActiveSession(),
                new Request.GraphUserListCallback() {

                    @Override
                    public void onCompleted(List<GraphUser> users, Response response) {
                        Log.d(LOG_TAG, "response: " + response.getRawResponse());
                        if (response.getError() == null) {

                        }
                        else
                            Log.d(LOG_TAG, "error: " + response.getError());
                    }

                });
        // Add birthday to the list of info to get.
        Bundle requestParams = myFriendsRequest.getParameters();
        requestParams.putString("fields", "name,birthday");
        myFriendsRequest.setParameters(requestParams);
        myFriendsRequest.executeAsync();
    }

    private void sendRequestDialog() {
        Bundle params = new Bundle();
        params.putString("message", "Learn how to make your Android apps social");

        WebDialog requestsDialog = (
                new WebDialog.RequestsDialogBuilder(getActivity(),
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error != null) {
                            if (error instanceof FacebookOperationCanceledException) {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Network Error",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            final String requestId = values.getString("request");
                            if (requestId != null) {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Request sent",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                })
                .build();
        requestsDialog.show();
    }
}
