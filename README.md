# Getting Started                         {#mainpage}

![WePay logo](https://go.wepay.com/frontend/images/wepay-logo.svg "WePay")

## Introduction
The WePay Android SDK enables collection of payments via various payment methods.

It is meant for consumption by [WePay](http://www.wepay.com) partners who are developing their own Android apps aimed at merchants and/or consumers.

Regardless of the payment method used, the SDK will ultimately return a Payment Token, which must be redeemed via a server-to-server [API](http://www.wepay.com/developer) call to complete the transaction.

## Payment methods
There are two types of payment methods:
+ Consumer payment methods - to be used in apps where consumers directly pay and/or make donations
+ Merchant payment methods - to be used in apps where merchants collect payments from their customers
 
The WePay Android SDK supports the following payment methods:
 - EMV Card Reader: Using an EMV Card Reader, a merchant can accept in-person payments by prosessing a consumer's EMV-enabled chip card. Traditional magnetic strip cards can be processed as well.
 - Manual Entry (Consumer/Merchant): The Manual Entry payment method lets consumer and merchant apps accept payments by allowing the user to manually enter card info.

## Installation
In the following steps, [version] represent one particular sdk version identifier such as 1.0.0
Replace [version] in following steps with the sdk version you are using 

+ Add the following jars to the libs directory under app directory of your project source:

    1. wepay-android-[version].aar
    2. wepay-android-[version]-javadoc.jar
    3. wepay-android-[version]-sources.jar

    For example, if you are using sdk version 1.0.0, you need to include the following files

    1. wepay-android-1.0.0.aar
    2. wepay-android-1.0.0-javadoc.jar
    3. wepay-android-1.0.0-sources.jar

+ Open build.gradle file for your app module (not the build.gradle file of the project) and add the following
~~~{.java}
repositories{
    flatDir{
        dirs 'libs'
    }
}
~~~
+ Also add the following to the dependencies closure 
~~~{.java}
compile(name:'wepay-android-[version]', ext:'aar')
compile 'com.google.code.gson:gson:2.2.2'
~~~
    
    As an example, if you are using sdk version 1.0.0, you need to add the following in dependencies closure

~~~{.java}
compile(name:'wepay-android-1.0.0', ext:'aar')
compile 'com.google.code.gson:gson:2.2.2'
~~~

+ Open your app's manifest.xml and add the following permissions under the manifest tag:
~~~{.java}
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
~~~

+ Android 6 / M / API 23 and later require a more complicated mechanism of requesting audio permissions from the user. See the WePayExample app's MainActivity.java for a sample implementation.
+ Android 6 also requires COARSE_LOCATION permission in order to scan for Bluetooth devices. This is most likely a bug in Android. You can view a discussion of the topic [here] (https://code.google.com/p/android/issues/detail?id=189090&q=ble%20android%206.0&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars).

+ Clean and build the project using your IDE or from the command line by going to the project's base directory and running:
~~~{.java}
./gradlew clean build
~~~
+ Done!

Note: Card reader functionality is not available in this SDK by default. If you want to use this SDK with WePay card readers, send an email to mobile@wepay.com.

## Documentation
HTML documentation is hosted on our [Github Pages Site](http://wepay.github.io/wepay-android/).

Pdf documentation is available on the [releases page](https://github.com/wepay/wepay-android/releases/latest) or as a direct [download](https://github.com/wepay/wepay-android/raw/master/documentation/wepay-android.pdf).

## SDK Organization

### com.wepay.android.WePay
The WePay class is the starting point for consuming the SDK, and is the primary class you will interact with.
It exposes all the methods you can call to accept payments via the supported payment methods.
Detailed reference documentation is available on the reference page for the Wepay class.

### Interfaces
The SDK uses interfaces to repond to API calls. You will implement the relevant interfaces to receive responses to the API calls you make.
Detailed reference documentation is available on the reference page for each interface:
- com.wepay.android.AuthorizationHandler
- com.wepay.android.CardReaderHandler
- com.wepay.android.CheckoutHandler
- com.wepay.android.TokenizationHandler

### Data Models and Enums
All other classes in the SDK are data models and Enums that are used to exchange data between your app and the SDK. 
Detailed reference documentation is available on the reference page for each class.

## Next Steps
Head over to the com.wepay.android.WePay class reference to see all the API methods available.
When you are ready, look at the samples below to learn how to interact with the SDK.


## Error Handling
com.wepay.android.models.Error serves as documentation for all errors surfaced by the WePay Android SDK.


## Samples

### See the WePayExample app for a working implementation of all API methods.

### Initializing the SDK

+ Complete the installation steps (above).
+ Include the wepay packages
~~~{.java}
import com.wepay.android.*;
import com.wepay.android.models.*;
import com.wepay.android.enums.*;
~~~
+ Define a property to store the Wepay object
~~~{.java}
WePay wepay;
~~~
+ Create a com.wepay.android.models.Config object
~~~{.java}
String clientId = "your_client_id";
Context context = getApplicationContext();
String environment = Config.ENVIRONMENT_STAGE;

Config config = new Config(context, clientId, environment);
~~~
+ Initialize the WePay object and assign it to the property
~~~{.java}
this.wepay = new WePay(config);
~~~

#####(optional) Providing permission to use location services for fraud detection

+ Open your app's manifest.xml and add the following permission under the manifest tag:
~~~{.java}
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"></uses-permission>
~~~
+ Set the option on the config object, before initializing the WePay object
~~~{.java}
config.setUseLocation(true);
~~~

### Integrating the Card Reader payment methods (Swipe+Dip)

+ Implement the AuthorizationHandler, CardReaderHandler and TokenizationHandler interfaces
~~~{.java}
public class MainActivity extends ActionBarActivity implements AuthorizationHandler, CardReaderHandler, TokenizationHandler
~~~
+ Implement the AuthorizationHandler interface methods
~~~{.java}
@Override
public void onEMVApplicationSelectionRequested(ApplicationSelectionCallback callback, ArrayList<String> applications) {
    // Ask the payer to select an application from the list, 
    // then execute the callback with the index of the selected application
    callback.useApplicationAtIndex(0);
}

@Override
public void onAuthorizationSuccess(PaymentInfo paymentInfo, AuthorizationInfo authorizationInfo) {
    // Send the tokenId (authorizationInfo.getTokenId()) and transactionToken (authorizationInfo.getTransactionToken()) to your server
    // Your server will use these values to make a /checkout/create call to complete the transaction
}

@Override
public void onAuthorizationError(PaymentInfo paymentInfo, Error error) {
    // handle the error
}
~~~
+ Implement the CardReaderHandler interface methods
~~~{.java}
@Override
public void onSuccess(PaymentInfo paymentInfo) {
    // use the payment info (for display/recordkeeping)
    // wait for card tokenization response
}

@Override
public void onError(Error error) {
    // handle the error
}

@Override
public void onStatusChange(CardReaderStatus status) {
    if (status.equals(CardReaderStatus.NOT_CONNECTED)) {
        // show UI that prompts the user to connect the card reader
        this.setStatusText("Connect card reader and wait");
    } else if (status.equals(CardReaderStatus.WAITING_FOR_CARD)) {
        // show UI that prompts the user to swipe/dip
        this.setStatusText("Swipe/Dip card");
    } else if (status.equals(CardReaderStatus.SWIPE_DETECTED)) {
        // provide feedback to the user that a swipe was detected
        this.setStatusText("Swipe detected");
    } else if (status.equals(CardReaderStatus.CARD_DIPPED)) {
        // provide feedback to the user that a dip was detected
        // also let them know they should not remove the card
        this.setStatusText("Card dipped, do not remove card");
    } else if (status.equals(CardReaderStatus.TOKENIZING)) {
        // provide feedback to the user that the card is being tokenized
        this.setStatusText("Tokenizing card...");
    } else if (status.equals(CardReaderStatus.AUTHORIZING)) {
        // provide feedback to the user that the card is being authorized
        this.setStatusText("Authorizing card...");
    }  else if (status.equals(CardReaderStatus.STOPPED)) {
        // provide feedback to the user that the card reader was stopped
        this.setStatusText("card reader Stopped");
    } else {
        // handle all other status change notifications
        this.setStatusText(status.toString());
    }
}

@Override
public void onReaderResetRequested(CardReaderResetCallback callback) {
    // decide if you want to reset the reader, 
    // then execute the callback with the appropriate response
    callback.resetCardReader(false);
}

@Override
public void onTransactionInfoRequested(CardReaderTransactionInfoCallback callback) {
    // provide the amount, currency code and WePay account ID of the merchant
    callback.useTransactionInfo(new BigDecimal("21.61"), CurrencyCode.USD, accountId);
}

@Override
public void onPayerEmailRequested(CardReaderEmailCallback callback) {
    // provide the email address of the payer
    callback.insertPayerEmail("android-example@wepay.com");
}
~~~
+ Implement the TokenizationHandler interface methods
~~~{.java}
@Override
public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
    // Send the tokenId (paymentToken.getTokenId()) to your server
    // Your server would use the tokenId to make a /checkout/create call to complete the transaction
}


@Override
public void onError(PaymentInfo paymentInfo, Error error) {
    // Handle error
}
~~~
+ Make the WePay API call, passing in the instance(s) of the class(es) that implemented the interface methods
~~~{.java}
this.wepay.startCardReaderForTokenizing(this, this, this);
// Show UI asking the user to insert the card reader and wait for it to be ready
~~~
+ That's it! The following sequence of events will occur:
    1. The user inserts the card reader (or it is already inserted)
    2. The SDK tries to detect the card reader and initialize it.
        - If the card reader is not detected, the `onStatusChange` method will be called with `status = NOT_CONNECTED`
        - If the card reader is successfully detected, then the `onStatusChange` method will be called with `status = CONNECTED`.
    3. Next, the SDK checks if the card reader is correctly configured (the `onStatusChange` method will be called with `status = CHECKING_READER`).
        - If the card reader is already configured, the App is given a chance to force configuration. The SDK calls the `onReaderResetRequested` method, and the app must execute the callback method, telling the SDK whether or not the reader should be reset.
        - If the reader was not configured, or the app requested a reset, the card reader is configured (the `onStatusChange` method will be called with `status = CONFIGURING_READER`)
    4. Next, if the card reader is successfully initialized, the SDK asks the app for transaction information by calling the `onTransactionInfoRequested` method. The app must execute the callback method, telling the SDK what the amount, currency code and merchant account id is.
    5. Next, the `onStatusChange` method will be called with `status = WAITING_FOR_CARD` 
    6. If the user inserts a card successfully, the `onStatusChange:` method will be called with `status = CARD_DIPPED`
    7. If the card has multiple applications on it, the payer must choose one:
        - The SDK calls the `onEMVApplicationSelectionRequested` method with a list of Applications on the card.
        - The app must display these Applications to the payer and allow them to choose which application they want to use.
        - Once the payer has decided, the app must inform the SDK of the choice by executing the calback method and passing in the index of the chosen application.
    8. Next, the SDK extracts card data from the card.
        - If the SDK is unable to obtain data from the card, the `onError` method will be called with the appropriate error, and processing will stop (the `onStatusChange` method will be called with `status = STOPPED`)
        - Otherwise, the SDK attempts to ask the App for the payer’s email by calling the `onPayerEmailRequested` method
    9. The app must execute the callback method and pass in the payer’s email address.
    10. Next, the `onSuccess` method is called with the obtained payment info.
    11. Next, the SDK will automatically send the obtained EMV card info to WePay's servers for authorization (the `onStatusChange` method will be called with `status = AUTHORIZING`)
    12. If authorization fails, the `onAuthorizationError` method will be called and processing will stop.
    13. If authorization succeeds, the `onAuthorizationSuccess` method will be called.
    14. Done!

Note: After the card is inserted into the reader, it must not be removed until a successful auth response (or an error) is returned.

### Integrating the Manual payment method

+ Implement the TokenizationHandler interface
~~~{.java}
public class MainActivity extends ActionBarActivity implements TokenizationHandler
~~~
+ Implement the TokenizationHandler interface methods
~~~{.java}
@Override
public void onSuccess(PaymentInfo paymentInfo, PaymentToken token) {
    // Send the tokenId (paymentToken.getTokenId()) to your server
    // Your server would use the tokenId to make a /checkout/create call to complete the transaction
}


@Override
public void onError(PaymentInfo paymentInfo, Error error) {
    // Handle error
}
~~~
+ Instantiate a PaymentInfo object using the user's credit card and address data
~~~{.java}
Address address = new Address(Locale.getDefault());
address.setAddressLine(0, "380 Portage ave");
address.setLocality("Palo Alto");
address.setPostalCode("94306");
address.setCountryCode("US");

PaymentInfo paymentInfo = new PaymentInfo("Android", "Tester", "a@b.com",
        "Visa xxxx-1234", address,
        address, PaymentMethod.MANUAL,
        "4242424242424242", "123", "01", "18", true);
~~~
+ Make the WePay API call, passing in the instance of the class that implemented the TokenizationHandler interface methods
~~~{.java}
this.wepay.tokenize(paymentInfo, this);
~~~
+ That's it! The following sequence of events will occur:
    1. The SDK will send the obtained payment info to WePay's servers for tokenization
    2. If the tokenization succeeds, TokenizationHandler's `onSuccess` method will be called
    3. Otherwise, if the tokenization fails, TokenizationHandler's `onError` method will be called with the appropriate error

### Integrating the Store Signature API

+ Implement the CheckoutHandler interface
~~~{.java}
public class MainActivity extends ActionBarActivity implements CheckoutHandler
~~~
+ Implement the CheckoutHandler interface methods
~~~{.java}
@Override
public void onSuccess(String signatureUrl, String checkoutId) {
    // success! nothing to do here
}


@Override
public void onError(Bitmap image, String checkoutId, Error error) {
    // handle the error
}
~~~
+ Obtain the checkout_id associated with this signature from your server
~~~{.java}
String checkoutId = this.obtainCheckoutId();
~~~
+ Instantiate a Bitmap object containing the user's signature
~~~{.java}
Bitmap signature = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.dd_signature);
~~~
+ Make the WePay API call, passing in the instance of the class that implemented the CheckoutHandler interface methods
~~~{.java}
this.wepay.storeSignatureImage(signature, checkoutId, this);
~~~
+ That's it! The following sequence of events will occur:
    1. The SDK will send the obtained signature to WePay's servers for tokenization
    2. If the operation succeeds, CheckoutHandler's `onSuccess` method will be called
    3. Otherwise, if the operation fails, CheckoutHandler's `onError` method will be called with the appropriate error

### Integrating the Calibration API

Sometimes, the card reader will not work with Android devices that we have not seen before. It is possible to calibrate the card reader to these new devices so that it starts working. The calibration only needs to be performed once, and only if the card reader is not detected on first use. After successful calibration, the reader can be used on the user's device as usual.

+ Implement the CalibrationHandler interface
~~~{.java}
public class MainActivity extends ActionBarActivity implements CalibrationHandler
~~~
+ Implement the CalibrationHandler interface methods
~~~{.java}
@Override
public void onProgress(final double progress) {
    // show progress
}


@Override
public void onComplete(final CalibrationResult result, final CalibrationParameters params) {
    // show result to the user
    // send the calibration params to WePay
}
~~~
+ Make the WePay API call, passing in the instance of the class that implemented the CalibrationHandler interface methods
~~~{.java}
this.wepay.calibrateCardReader(this);
~~~
+ That's it! The following sequence of events will occur:
    1. The SDK will attempt to calibrate the reader
    2. CalibrationHandler's `onProgress` method will be called periodically to indicate the current progress
    3. When the process is completed, CalibrationHandler's `onComplete` method will be called with the result
    4. The card reader must be plugged in before attempting calibration, otherwise the process will fail

Note: If calibration succeeds, you must obtain the calibration parameters and email them to mobile@wepay.com. We will bake these parameters into the SDK, so that future users with the same devices will not have to run the calibration process.

### Integrating the Battery Level API

+ Implement the BatteryLevelHandler interface
~~~{.java}
public class MainActivity extends ActionBarActivity implements BatteryLevelHandler
~~~
+ Implement the BatteryLevelHandler interface methods
~~~{.java}
@Override
public void onBatteryLevel(int batteryLevel) {
    // show result to the user
}

@Override
public void onBatteryLevelError(Error error) {
    // handle the error
}
~~~
+ Make the WePay API call, passing in the instance of the class that implemented the BatteryLevelHandler interface methods
~~~{.java}
this.wepay.getCardReaderBatteryLevel(this);
~~~
+ That's it! The following sequence of events will occur:
    1. The SDK will attempt to read the battery level of the card reader
    2. If the operation succeeds, BatteryLevelHandler's `onBatteryLevel` method will be called with the result
    3. Otherwise, if the operation fails, BatteryLevelHandler's `onBatteryLevelError` method will be called with the appropriate error
    4. The card reader must be plugged in before attempting to get battery level, otherwise the process will fail

### Test/develop using mock card reader and mock WepayClient

+ To use mock card reader implementation instead of using the real reader, instantiate a MockConfig object and pass it to Config:
~~~{.java}
MockConfig mockConfig = new MockConfig().setUseMockCardReader(true);
config.setMockConfig(mockConfig);
~~~
+ To use mock WepayClient implementation instead of interacting with the real WePay server, set the corresponding option on the mockConfig object:
~~~{.java}
mockConfig.setUseMockWepayClient(true);
~~~
+ Other options are also available:
~~~{.java}
mockConfig.setMockPaymentMethod(PaymentMethod.SWIPE) // Payment method to mock; Defaults to SWIPE.
.setCardReadTimeout(true) // To mock a card reader timeout; Defaults to false.
.setCardReadFailure(true) // To mock a failure for card reading; Defaults to false.
.setCardTokenizationFailure(true) // To mock a failure for card tokenization; Defaults to false.
.setEMVAuthFailure(true) // To mock a failure for EMV authorization; Defaults to false.
.setMultipleEMVApplication(true) // To mock multiple EMV applications on card to choose from; Defaults to false.
.setBatteryLevelError(true); // To mock an error while fetching battery level; Defaults to false.
~~~

### Integration tests and unit tests
All the integration tests and unit tests are located in the `src/androidTest/java/` directory. The tests are instrumented tests so be sure to have a connected running physical device or emulator before running the tests. 

##### From Android Studio

+ To run a single test, right-click the test method and select "Run".
+ To run all test methods in a class, right-click the class and select "Run".
+ To run all tests in a directory, right-click the directory and select "Run tests".

##### From the command line

Change to this project's directory and call the connectedAndroidTest (or cAT) task:
~~~
./gradlew cAT
~~~

+ HTML test result files can be found at: `<path_to_your_project>/app/build/reports/androidTests/connected/` directory.
+ XML test result files: `<path_to_your_project>/app/build/outputs/androidTest-results/connected/` directory.





