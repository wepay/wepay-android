package com.wepay.android.example;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wepay.android.AuthorizationHandler;
import com.wepay.android.BatteryLevelHandler;
import com.wepay.android.CalibrationHandler;
import com.wepay.android.CardReaderHandler;
import com.wepay.android.CheckoutHandler;
import com.wepay.android.TokenizationHandler;
import com.wepay.android.WePay;
import com.wepay.android.enums.CalibrationResult;
import com.wepay.android.enums.CardReaderStatus;
import com.wepay.android.enums.CurrencyCode;
import com.wepay.android.enums.LogLevel;
import com.wepay.android.enums.PaymentMethod;
import com.wepay.android.models.AuthorizationInfo;
import com.wepay.android.models.CalibrationParameters;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.PaymentInfo;
import com.wepay.android.models.PaymentToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Class MainActivity.
 */
public class MainActivity extends ActionBarActivity implements CardReaderHandler, TokenizationHandler, AuthorizationHandler, CheckoutHandler, CalibrationHandler, BatteryLevelHandler, OnClickListener {

    ListView list;
    ArrayAdapter<String> listAdapter;
    AlertDialog deviceSelectionDialog;

    TextView tvStatus, tvConsole;

    WePay wepay;

    View mLayout;

    SharedPreferences preferences;

    Context context;

    String clientId;

    String environment;

    long accountId;

    boolean cardReaderStarted;

    BigDecimal amount = new BigDecimal("22.61"); // magic success amount

    public static final String TAG = "wepay_sdk";

    private static final int REQUEST_PERMISSION = 1;

    private Map<String, PermissionRequest> permissionRequests = new HashMap<>();

    /* (non-Javadoc)
     * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.example_main_layout);

        this.requestAppPermissions();

        this.setupUI();

        // Initialize WePay
        context = getApplicationContext();

        // Fetch settings
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.clientId = preferences.getString("client_id_text", getString(R.string.pref_default_client_id_name));
        this.environment = preferences.getString("api_endpoint_text", Config.ENVIRONMENT_STAGE);
        this.accountId = Long.valueOf(preferences.getString("account_id_text", getString(R.string.pref_default_account_id_name)));

		Log.d(TAG, clientId);
        Log.d(TAG, environment);
        Log.d(TAG, String.format("%d", accountId));

        this.writeToConsole("clientId: " + clientId);
        this.writeToConsole("environment: " + environment);
        this.writeToConsole("accountId: " + String.format("%d", accountId));

        // Initialize and configure the wepay object with current settings
        Config config = new Config(context, clientId, environment)
                .setUseLocation(false)
                .setUseTestEMVCards(true)
                .setStopCardReaderAfterOperation(false)
                .setLogLevel(LogLevel.ALL);
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

        findViewById(R.id.buttonManual).setOnClickListener(this);
        findViewById(R.id.buttonInfo).setOnClickListener(this);
        findViewById(R.id.buttonTokenize).setOnClickListener(this);
        findViewById(R.id.buttonStopCardReader).setOnClickListener(this);
        findViewById(R.id.buttonCalibrate).setOnClickListener(this);
        findViewById(R.id.buttonBattery).setOnClickListener(this);
        findViewById(R.id.buttonSignature).setOnClickListener(this);
        findViewById(R.id.buttonGetRememberedCardReader).setOnClickListener(this);
        findViewById(R.id.buttonForgetRememberedCardReader).setOnClickListener(this);
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
     * CardReaderHandler - onCardReaderSelection
     */
    @Override
    public void onCardReaderSelection(final CardReaderSelectionCallback callback, ArrayList<String> devices) {
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_available_readers, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Discovered devices:");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.useCardReaderAtIndex(-1);
            }
        });
        list = (ListView) view.findViewById(R.id.available_readers_list);
        listAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        list.setAdapter(listAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                callback.useCardReaderAtIndex(position);
                deviceSelectionDialog.dismiss();

            }
        });
        listAdapter.addAll(devices);
        deviceSelectionDialog = builder.create();
        deviceSelectionDialog.show();
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
     * CardReaderHandler - onEMVApplicationSelectionRequested
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
     * CardReaderHandler - onPayerEmailRequested
     */
    @Override
    public void onPayerEmailRequested(CardReaderEmailCallback callback) {
        callback.insertPayerEmail("android-example@wepay.com");
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
     * CheckoutHandler - onError
     */
    @Override
    public void onError(Bitmap image, String checkoutId, Error error) {
        this.writeToConsole("\nSignature failed! error:");
        this.writeToConsole(error.getErrorDescription());
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
     * CalibrationHandler - onProgress
     */
    @Override
    public void onProgress(final double progress) {
        writeToConsole("calibration progress: " + String.format("%.2f", progress * 100));
    }

    /**
     * CalibrationHandler - onComplete
     */
    @Override
    public void onComplete(final CalibrationResult result, final CalibrationParameters params) {
        writeToConsole("\nCalibration Result: " + result.toString());

        if (params != null) {
            try {
                JSONObject res = new JSONObject(params.toString());
                writeToConsole("Calibration json:\n" + res.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        this.setStatusText("Calibration Result: " + result.toString());
    }

    /**
     * BatteryLevelHandler - onBatteryLevel
     */
    @Override
    public void onBatteryLevel(int batteryLevel) {
        writeToConsole("\nBattery Level: " + batteryLevel);
        this.setStatusText("Battery Level: " + batteryLevel);
    }

    /**
     * BatteryLevelHandler - onBatteryLevelError
     */
    @Override
    public void onBatteryLevelError(Error error) {
        this.writeToConsole("\nGetting Battery Level failed! error:");
        this.writeToConsole(error.toString());
        this.setStatusText("Getting Battery Level failed!");
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
            this.wepay.startTransactionForTokenizing(this, this, this);
            this.cardReaderStarted = true;
        } else if (btn.getId() == R.id.buttonInfo) {
            this.resetConsole();
            this.writeToConsole("Initializing Card Reader for Info");
            this.setStatusText("Initializing Card Reader");
            this.wepay.startTransactionForReading(this);
            this.cardReaderStarted = true;
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
                    "4242424242424242", "123", "01", "25", true);

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
            this.resetConsole();
            // Print message to screen
            this.writeToConsole("Stop Card Reader selected");

            // Change status label
            this.setStatusText(this.cardReaderStarted ? "Stopping Card Reader..." : "Card reader not started");

            // Make WePay API call
            this.wepay.stopCardReader();
        } else if (btn.getId() == R.id.buttonCalibrate) {
            this.resetConsole();
            // Print message to screen
            this.writeToConsole("Calibrate Card Reader selected");

            // Change status label
            this.setStatusText("Calibrating Card Reader...");

            // Make WePay API call
            this.wepay.calibrateCardReader(this);
        }  else if (btn.getId() == R.id.buttonBattery) {
            this.resetConsole();
            // Print message to screen
            this.writeToConsole("Battery Level selected");

            // Change status label
            this.setStatusText("Getting Battery Level...");

            // Make WePay API call
            this.wepay.getCardReaderBatteryLevel(this, this);
            this.cardReaderStarted = true;
        } else if (btn.getId() == R.id.buttonGetRememberedCardReader) {
            String cardReader = wepay.getRememberedCardReader();
            String rememberedCardReaderMessage = "No remembered card reader exists";

            this.setStatusText("Getting remembered card reader");

            if (cardReader != null) {
                rememberedCardReaderMessage = "Remembered card reader is " + cardReader;
            }

            this.writeToConsole(rememberedCardReaderMessage);
        } else if (btn.getId() == R.id.buttonForgetRememberedCardReader) {
            this.setStatusText("Forgetting remembered card reader");
            this.wepay.forgetRememberedCardReader();
            this.writeToConsole("Forgot the remembered card reader");
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
    // Android M - Permissions
    ////////////////////////////////

    private void requestAppPermissions() {
        PermissionRequest audioPermissionRequest, coarseLocationPermissionRequest;

        audioPermissionRequest = new PermissionRequest(Manifest.permission.RECORD_AUDIO, "record audio");
        coarseLocationPermissionRequest = new PermissionRequest(Manifest.permission.ACCESS_COARSE_LOCATION, "coarse location");

        this.addPendingPermission(audioPermissionRequest);
        this.addPendingPermission(coarseLocationPermissionRequest);

        // For Android 6 / M / API 23 and later
        this.requestPendingPermissions(this.permissionRequests, REQUEST_PERMISSION);
    }

    /**
     * Adds a request permission to the permissionRequests map if the app does not yet have
     * permissions.
     * */
    private void addPendingPermission(PermissionRequest permission) {
        int permissionLevel = ActivityCompat.checkSelfPermission(this, permission.androidPermissionType);

        if (permissionLevel != PackageManager.PERMISSION_GRANTED) {
            this.permissionRequests.put(permission.androidPermissionType, permission);
        } else {
            // Do nothing, we have permissions already
            Log.i(TAG, permission.shortPermissionType + " has previously been granted.");
        }
    }

    private void requestPendingPermissions(Map<String, PermissionRequest> permissions, final int requestType) {
        if (permissions.size() > 0) {
            final String[] androidPermissions = new String[permissions.size()];
            int index = 0;
            String shortPermissions = "";

            for (PermissionRequest permission : permissions.values()) {
                androidPermissions[index] = permission.androidPermissionType;
                index++;

                shortPermissions += permission.shortPermissionType;

                if (index < permissions.size()) {
                    shortPermissions += ", ";
                }
            }

            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            Snackbar.make(mLayout, shortPermissions + " is required for using the card reader.",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                androidPermissions, requestType);
                    }
                })
                .show();
        }
    }

    private void handleRequestPermissionsResponse(int grantResult, String permissionType) {
        // BEGIN_INCLUDE(permission_result)
        // Received permission result for permission.
        Log.i(TAG, "Received response for " + permissionType + " permission request.");

        PermissionRequest permissionRequest = this.permissionRequests.get(permissionType);
        String shortPermissionType = permissionType;

        if (permissionRequest != null) {
            shortPermissionType = permissionRequest.shortPermissionType;
        }

        // Check if the only required permission has been granted
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Permission has been granted, preview can be displayed
            Log.i(TAG, permissionType + " permission has now been granted. Showing preview.");
            Snackbar.make(mLayout, shortPermissionType + " permission available",
                    Snackbar.LENGTH_SHORT).show();

            // Remove the granted permission from our permission requests.
            this.permissionRequests.remove(permissionType);
        } else {
            Log.i(TAG, permissionType + " permission was NOT granted.");

            if (permissionRequest.isFirstAskForPermission) {
                permissionRequest.isFirstAskForPermission = false;
            } else {
                Snackbar.make(mLayout, shortPermissionType + " permission not granted, card reader will not work",
                        Snackbar.LENGTH_SHORT).show();

                // The user refuses to grant this permission, so remove it from the list.
                this.permissionRequests.remove(permissionType);
            }
        }
        // END_INCLUDE(permission_result)
    }

    private boolean isRequestCodeHandled(int requestCode) {
        return requestCode == REQUEST_PERMISSION;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (isRequestCodeHandled(requestCode)) {
            for (int i = 0; i < permissions.length; ++i) {
                handleRequestPermissionsResponse(grantResults[i], permissions[i]);
            }

            this.requestPendingPermissions(this.permissionRequests, requestCode);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private class PermissionRequest {
        String androidPermissionType;
        String shortPermissionType;
        Boolean isFirstAskForPermission;

        PermissionRequest(String androidPermissionType,
                          String shortPermissionType) {
            this.androidPermissionType = androidPermissionType;
            this.shortPermissionType = shortPermissionType;
            this.isFirstAskForPermission = true;
        }
    }
}