package com.moyinoluwa.hellolagos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    // A dialog that is presented until the Firebase authentication finished.
    private ProgressDialog mAuthProgressDialog;

    // A reference to the Firebase
    private Firebase mFirebaseRef;

    // Data from the authenticated user
    private AuthData mAuthData;

    // Username for an authenticated user
    private String mUsername;

    // Listener for Firebase session changes
    private Firebase.AuthStateListener mAuthStateListener;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private AutoCompleteTextView mUsernameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Allow for offline functionality
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                String username = mUsernameView.getText().toString();

                // Confirm that the fields are not empty
                if (email.matches("") || password.matches("") || username.matches("")) {
                    Toast.makeText(LoginActivity.this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show();
                } else {
                    loginWithPassword();
                }
            }
        });


        // Create the Firebase ref that is used for all authentication with Firebase
        mFirebaseRef = new Firebase(getResources().getString(R.string.firebase_url));

        // Setup the progress dialog that is displayed later when authenticating with Firebase
        mAuthProgressDialog = new ProgressDialog(this);

        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                mAuthProgressDialog.hide();
                setAuthenticatedUser(authData);
            }
        };
        // Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
        // user
        mFirebaseRef.addAuthStateListener(mAuthStateListener);
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {

            setupUsername();

            // show a provider specific status text
            String name = null;
            if (authData.getProvider().equals("password")) {
                name = authData.getUid();
                Toast.makeText(this, "Welcome, " + mUsername, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
            if (name != null) {
                Toast.makeText(this, "Logged in as " + mUsername, Toast.LENGTH_SHORT).show();
            }

            // Open MainActivity
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
        }
        this.mAuthData = authData;
        // invalidate options menu to hide/show the logout button
        supportInvalidateOptionsMenu();
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;

        public AuthResultHandler(String provider) {
            this.provider = provider;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            mAuthProgressDialog.hide();
            Log.i(TAG, provider + " auth successful");
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            mAuthProgressDialog.hide();
            showErrorDialog(firebaseError.toString());
        }
    }

    /**
     * Login an existing user
     */
    public void loginWithPassword() {
        mAuthProgressDialog.show();

        mFirebaseRef.authWithPassword(mEmailView.getText().toString(), mPasswordView.getText().toString(), new AuthResultHandler("password") {
            @Override
            public void onAuthenticated(AuthData authData) {
                // Cache the username
                saveUsername();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // There was an error probably because the user account has not been created
                // Create a new user instead
                createNewUser();
            }
        });

        // Cache the username
        saveUsername();
    }

    /**
     * Creates a new account for the user
     */
    public void createNewUser() {
        mAuthProgressDialog.show();

        mFirebaseRef.createUser(mEmailView.getText().toString(), mPasswordView.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>() {

            @Override
            public void onSuccess(Map<String, Object> result) {
                System.out.println("Successfully created user account with uid: " + result.get("uid"));
                mAuthProgressDialog.hide();

                /* Opens MainActivity */
                saveUsername();
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
            }
        });
    }

    /**
     * Retrieve the username
     */
    private void setupUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        // Load the stored username
        mUsername = prefs.getString("username", null);
    }

    /**
     * Save the username
     */
    private void saveUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        prefs.edit().putString("username", mUsernameView.getText().toString()).apply();
    }
}

