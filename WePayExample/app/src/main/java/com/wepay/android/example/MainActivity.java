package com.wepay.android.example;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.wepay.android.AuthorizationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.CheckoutHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.WePay;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import java.util.ArrayList;
import java.util.Locale;

/**
 * The Class MainActivity.
 */
public class MainActivity extends ActionBarActivity implements CardReaderHandler, TokenizationHandler, AuthorizationHandler, CheckoutHandler, OnClickListener {

    TextView tvStatus, tvConsole;

    WePay wepay;

    View mLayout;

    SharedPreferences preferences;

    Context context;

    String clientId;

    String environment;

    long accountId;

    double amount = 22.61; // magic success amount

    public static final String TAG = "wepay_sdk";

    private static final int REQUEST_AUDIO = 0;

    Boolean isFirstAskForAudioPermission = true;


    /* (non-Javadoc)
     * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For Android 6 / M / API 23 and later
        this.requestAudioPermission();

        this.setupUI();
        
        // Initialize WePay
        context = getApplicationContext();

        // Fetch settings
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.clientId = preferences.getString("client_id_text", getString(R.string.pref_default_client_id_name));
        this.environment = preferences.getString("api_endpoint_text", getString(R.string.pref_default_api_endpoint_name));
        this.accountId = Long.valueOf(preferences.getString("account_id_text", getString(R.string.pref_default_account_id_name)));

		Log.d(TAG, clientId);
        Log.d(TAG, environment);
        Log.d(TAG, String.format("%d", accountId));

        this.writeToConsole("clientId: " + clientId);
        this.writeToConsole("environment: " + environment);
        this.writeToConsole("accountId: " + String.format("%d", accountId));

        // Initialize and configure the wepay object with current settings
        Config config = new Config(context, clientId, environment).setUseLocation(false).setUseTestEMVCards(true);
        this.wepay = new WePay(config);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, 1);
                break;
        }

        return true;
    }

    /**
     * Setup user interface.
     */
    private void setupUI() {
        tvStatus = (TextView) findViewById(R.id.statusView);
        tvConsole = (TextView) findViewById(R.id.consoleView);
        tvConsole.setMovementMethod(new ScrollingMovementMethod());

        ((ImageButton) findViewById(R.id.buttonManual)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.buttonInfo)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.buttonTokenize)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.buttonSignature)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.buttonStopCardReader)).setOnClickListener(this);
    }

    /**
     * AuthorizationHandler - onEMVApplicationSelectionRequested
     */
    @Override
    public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications) {
        int selectedIndex = 0;

        this.writeToConsole("\nPerforming application selection:\n");
        this.writeToConsole(applications.toString());
        this.writeToConsole("\nselected Index: " + selectedIndex + " (" + applications.get(selectedIndex) + ")");

        callback.useApplicationAtIndex(selectedIndex);
    }

    /**
     * AuthorizationHandler - onAuthorizationSuccess
     */
    @Override
    public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
        this.writeToConsole("\nAuthorized amount: " + String.valueOf(authorizationInfo.getAuthorizedAmount()));
        this.writeToConsole("Token id: " + authorizationInfo.getTokenId());
        this.writeToConsole("Transaction Token: " + authorizationInfo.getTransactionToken());
        this.setStatusText("Authorized!");
    }

    /**
     * AuthorizationHandler - onAuthorizationError
     */
    @Override
    public void onAuthorizationError(PaymentInfo paymentInfo, Error error) {
        this.writeToConsole("\nAuthorization failed! error:");
        this.writeToConsole(error.toString());
        this.setStatusText("Authorization failed!");
    }

    /**
     * CardReaderHandler - onSuccess
     */
    @Override
    public void onSuccess(PaymentInfo paymentInfo) {
        this.writeToConsole("\nSuccess! Info from card reader:");
        this.writeToConsole(paymentInfo.toString());
        this.setStatusText("Dip/Swipe succeeded");
    }

    /**
     * CardReaderHandler - onError
     */
    @Override
    public void onError(final Error error) {
        this.writeToConsole("\nDip/Swipe failed! error:");
        this.writeToConsole(error.toString());
        this.setStatusText("Dip/Swipe failed");
    }

    /**
     * CardReaderHandler - onStatusChange
     */
    @Override
    public void onStatusChange(final CardReaderStatus status) {
        if (status.equals(CardReaderStatus.NOT_CONNECTED)) {
            // show UI that prompts the user to connect the card reader
            this.setStatusText("Connect card reader and wait");
        } else if (status.equals(CardReaderStatus.WAITING_FOR_CARD)) {
            // show UI that prompts the user to dip/swipe
            this.setStatusText("Dip/Swipe card");
        } else if (status.equals(CardReaderStatus.SWIPE_DETECTED)) {
            // provide feedback to the user that a swipe was detected
            this.setStatusText("Swipe detected");
        } else if (status.equals(CardReaderStatus.CARD_DIPPED)) {
            // provide feedback to the user that a dip was detected
             this.setStatusText("Dip detected");
        } else if (status.equals(CardReaderStatus.TOKENIZING)) {
            // provide feedback to the user that the card is being tokenized
            this.setStatusText("Tokenizing card...");
        }  else if (status.equals(CardReaderStatus.STOPPED)) {
            // provide feedback to the user that the card reader was stopped
            this.setStatusText("Card Reader Stopped");
        } else {
            this.setStatusText(status.toString());
        }

        this.writeToConsole("Card Reader Status: " + status.toString());
    }

    /**
     * CardReaderHandler - onReaderResetRequested
     */
    @Override
    public void onReaderResetRequested(CardReaderResetCallback callback) {
        callback.resetCardReader(false);
    }

    /**
     * CardReaderHandler - onTransactionInfoRequested
     */
    @Override
    public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
        CurrencyCode currencyCode = CurrencyCode.USD;
        this.writeToConsole("using transaction info: " + this.amount + " " + currencyCode + " " + this.accountId);
        callback.useTransactionInfo(this.amount, currencyCode, this.accountId);
    }

    /**
     * CardReaderHandler - onPayerEmailRequested
     */
    @Override
    public void onPayerEmailRequested(CardReaderEmailCallback callback) {
        callback.insertPayerEmail("android-example@wepay.com");
    }

    /**
     * TokenizationHandler - onSuccess
     */
    @Override
    public void onSuccess(final PaymentInfo paymentInfo, final PaymentToken token) {
        this.writeToConsole("\nSuccess! Token Id:");
        this.writeToConsole(token.getTokenId());
        this.setStatusText("Tokenization succeeded");

        // Send the token to your server so that it can be used to charge the card
    }

    /**
     * TokenizationHandler - onError
     */
    @Override
    public void onError(final PaymentInfo paymentInfo, final Error error) {
        this.writeToConsole("\nTokenization failed! error:");
        this.writeToConsole(error.toString());
        this.setStatusText("Tokenization failed");
    }

    /**
     * CheckoutHandler - onError
     */
    @Override
    public void onError(Bitmap image, String checkoutId, Error error) {
        this.writeToConsole("\nSignature failed! error:");
        this.writeToConsole(error.toString());
        this.setStatusText("Signature failed");
    }

    /**
     * CheckoutHandler - onSuccess
     */
    @Override
    public void onSuccess(String signatureUrl, String checkoutId) {
        this.writeToConsole("\nSuccess! Signature url:");
        this.writeToConsole(signatureUrl);
        this.setStatusText("Signature succeeded");
    }


    /**
     * On click handler for UI buttons.
     */
    @Override
    public void onClick(View btn) {

        // Perform the action
        if (btn.getId() == R.id.buttonTokenize) {
            this.resetConsole();
            this.writeToConsole("Initializing Card Reader for Tokenizing");
            this.setStatusText("Initializing Card Reader");
            this.wepay.startCardReaderForTokenizing(this, this, this);
        } else if (btn.getId() == R.id.buttonInfo) {
            this.resetConsole();
            this.writeToConsole("Initializing Card Reader for Info");
            this.setStatusText("Initializing Card Reader");
            this.wepay.startCardReaderForReading(this);
        } else if (btn.getId() == R.id.buttonManual) {
            this.resetConsole();

            Address address = new Address(Locale.getDefault());
            address.setAddressLine(0, "380 Portage ave");
            address.setLocality("Palo Alto");
            address.setPostalCode("94306");
            address.setCountryCode("US");

            PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
                    "Visa xxxx-1234", address,
                    address, PaymentMethod.MANUAL,
                    "4242424242424242", "123", "01", "18", true);

            this.setStatusText("Testing Manual");

            this.writeToConsole("Testing Manual");
            this.writeToConsole(paymentInfo.toString());

            this.wepay.tokenize(paymentInfo, this);
        } else if (btn.getId() == R.id.buttonSignature) {
            this.resetConsole();
            this.writeToConsole("Storing Signature");
            this.setStatusText("Storing Signature");

            Bitmap signature = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.dd_signature);

            // Below, use a checkoutId from a checkout you created recently (via a /checkout/create API call), otherwise an error will occur.
            // If you do obtain a valid checkout id, remember to change the clientId above to the one associated with the checkout.
            // The placeholder checkoutId below is invalid, and will result in an appropriate error.

            String checkoutId = "12345678";
            this.wepay.storeSignatureImage(signature, checkoutId, this);
        } else if (btn.getId() == R.id.buttonStopCardReader) {
            // Print message to screen
            this.writeToConsole("Stop Card Reader selected");

            // Change status label
            this.setStatusText("Stopping Card Reader...");

            // Make WePay API call
            this.wepay.stopCardReader();
        }
    }


    /**
     * Write to the on-screen console.
     */
    private void writeToConsole(final String message) {
        tvConsole.setText(tvConsole.getText() + "\n" + message);
    }

    /**
     * Reset the on-screen console.
     */
    private void resetConsole() {
        tvConsole.setText("Console Logs:");
    }

    /**
     * Sets the status text.
     */
    private void setStatusText(final String status) {
        tvStatus.setText("Status: " + status);
    }

    ////////////////////////////////
    // Android M - Audio permissions
    ////////////////////////////////

    private void requestAudioPermission() {

        mLayout = findViewById(R.id.example_main_layout);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG, "Audio permission has NOT been granted. Requesting permission.");

            // BEGIN_INCLUDE(audio_permission_request)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example if the user has previously denied the permission.
                Log.i(TAG,
                        "Displaying audio permission rationale to provide additional context.");
                Snackbar.make(mLayout, "Permission to record audio is required for using the card reader.",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.RECORD_AUDIO},
                                        REQUEST_AUDIO);
                            }
                        })
                        .show();
            } else {

                // Audio permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_AUDIO);
            }
            // END_INCLUDE(audio_permission_request)
        } else {
            // Do nothing, we have audio permissions already
            Log.i(TAG, "Audio permission has previously been granted.");
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_AUDIO) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for audio permission.
            Log.i(TAG, "Received response for Audio permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Audio permission has been granted, preview can be displayed
                Log.i(TAG, "Audio permission has now been granted. Showing preview.");
                Snackbar.make(mLayout, "audio permission available",
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Audio permission was NOT granted.");

                if (this.isFirstAskForAudioPermission) {
                    this.isFirstAskForAudioPermission = false;
                    this.requestAudioPermission();
                } else {
                    Snackbar.make(mLayout, "audio permission not granted, card reader will not work",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
            // END_INCLUDE(permission_result)

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}