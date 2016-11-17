package com.wepay.android.models;

import android.content.Context;

import com.google.gson.Gson;

import java.util.LinkedHashMap;

/**
 * The Class Config contains the configuration required to initialize the sdk.
 */
public class Config {

    /** The constant string representing the staging environment. */
    final public static String ENVIRONMENT_STAGE = "stage";

    /** The constant string representing the production environment. */
    final public static String ENVIRONMENT_PRODUCTION = "production";

    /** The client id. */
    private String clientId;

    /** The environment. */
    private String environment;

    /** The context. */
    private Context context;

    /** Determines if we should use location services. Defaults to false.*/
    private boolean useLocation = false;

    /** Determines if we should use test EMV cards. Defaults to false.*/
    private boolean useTestEMVCards = false;

    /** Determines if the card reader should automatically stop after a transaction is completed. Defaults to true. */
    private boolean stopCardReaderAfterTransaction = true;

    /** Determines if the transaction should automatically restart after a successful swipe. Defaults to false. */
    private boolean restartTransactionAfterSuccess = false;

    /** Determines if the transaction should automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR). Defaults to true. */
    private boolean restartTransactionAfterGeneralError = true;

    /** Determines if the transaction should automatically restart after an error other than general error. Defaults to false. */
    private boolean restartTransactionAfterOtherErrors = false;

    private MockConfig mockConfig;

    /**
     * Instantiates a new config.
     *
     * @param context the application context
     * @param clientId the client id for your WePay app
     * @param environment the environment (use one of the provided constants - ENVIRONMENT_STAGING or ENVIRONMENT_PRODUCTION)
     */
    public Config(Context context, String clientId, String environment) {
        this.context = context;
        this.clientId = clientId;
        this.environment = environment;
    }

    /**
     * Gets the context.
     *
     * @return the context
     */
    public Context getContext() {
        return this.context;
    }

    /**
     * Gets the client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * Gets the environment.
     *
     * @return the environment
     */
    public String getEnvironment() {
        return this.environment;
    }

    /**
     * Determines if we should use location services.
     *
     * @return the use location config
     */
    public boolean isUseLocation() {
        return this.useLocation;
    }

    /**
     * Sets the option for using location services for fraud detection purposes.
     * If not explicitly set to true, defaults to false.
     *
     * @param useLocation the permission to use location
     * @return the config
     */
    public Config setUseLocation(boolean useLocation) {
        this.useLocation = useLocation;
        return this;
    }

    /**
     * Determines if we should use test EMV cards.
     *
     * @return the use test EMV cards config
     */
    public boolean isUseTestEMVCards() {
        return this.useTestEMVCards;
    }

    /**
     * Sets the option for using test EMV cards.
     * If not explicitly set to true, defaults to false.
     *
     * @param useTestEMVCards the permission to use location
     * @return the config
     */
    public Config setUseTestEMVCards(boolean useTestEMVCards) {
        this.useTestEMVCards = useTestEMVCards;
        return this;
    }

    /**
     * Determines if the card reader should automatically stop after a transaction is completed.
     *
     * @return true, if the card reader restarts after success
     */
    public boolean shouldStopCardReaderAfterTransaction() {
        return this.stopCardReaderAfterTransaction;
    }

    /**
     * Sets the option for the card reader to automatically stop after a transaction.
     * If not explicitly set to false, defaults to true.
     *
     * @param stopCardReaderAfterTransaction the flag to determine if the card reader should automatically stop after a transaction.
     * @return the config
     */
    public Config setStopCardReaderAfterTransaction(boolean stopCardReaderAfterTransaction) {
        this.stopCardReaderAfterTransaction = stopCardReaderAfterTransaction;
        return this;
    }

    /**
     * Determines if the transaction should automatically restart after a successful swipe.
     *
     * @return true, if the transaction restarts after success.
     */
    public boolean shouldRestartTransactionAfterSuccess() {
        return this.restartTransactionAfterSuccess;
    }

    /**
     * Sets the option for the transaction to automatically restart after a successful swipe.
     * If not explicitly set to true, defaults to false.
     *
     * @param restartTransactionAfterSuccess the flag to determine if the transaction should automatically restart after a successful swipe.
     * @return the config
     */
    public Config setRestartTransactionAfterSuccess(boolean restartTransactionAfterSuccess) {
        this.restartTransactionAfterSuccess = restartTransactionAfterSuccess;
        return this;
    }

    /**
     * Determines if the transaction should automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR).
     *
     * @return true, if the transaction restarts after a general error.
     */
    public boolean shouldRestartTransactionAfterGeneralError() {
        return this.restartTransactionAfterGeneralError;
    }

    /**
     * Sets the option for the transaction to automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR).
     * If not explicitly set to false, defaults to true.
     *
     * @param restartTransactionAfterGeneralError the flag to determine if the transaction should automatically restart after a general error.
     * @return the config
     */
    public Config setRestartTransactionAfterGeneralError(boolean restartTransactionAfterGeneralError) {
        this.restartTransactionAfterGeneralError = restartTransactionAfterGeneralError;
        return this;
    }

    /**
     * Determines if the transaction should automatically restart after an error other than general error.
     *
     * @return true, if the transaction restarts after an error other than general error.
     */
    public boolean shouldRestartTransactionAfterOtherErrors() {
        return this.restartTransactionAfterOtherErrors;
    }

    /**
     * Sets the option for the transaction to automatically restart after an error other than general error.
     * If not explicitly set to true, defaults to false.
     *
     * @param restartTransactionAfterOtherErrors the flag to determine if the transaction should automatically restart after an error other than general error.
     * @return the config
     */
    public Config setRestartTransactionAfterOtherErrors(boolean restartTransactionAfterOtherErrors) {
        this.restartTransactionAfterOtherErrors = restartTransactionAfterOtherErrors;
        return this;
    }

    /**
     * Gets the MockConfig instance.
     *
     * @return the MockConfig instance
     */
    public MockConfig getMockConfig() {
        return mockConfig;
    }

    /**
     * Sets the MockConfig instance to be used.
     *
     * @param mockConfig the MockConfig instance
     * @return the config
     */
    public Config setMockConfig(MockConfig mockConfig) {
        if (environment.equals(ENVIRONMENT_PRODUCTION)) {
            throw new IllegalArgumentException("Mock cannot be used in production environment. Use stage environment instead.");
        }
        this.mockConfig = mockConfig;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        LinkedHashMap<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("context", this.context);
        configMap.put("clientId", this.clientId);
        configMap.put("environment", this.environment);

        return new Gson().toJson(configMap, LinkedHashMap.class);
    }
}
