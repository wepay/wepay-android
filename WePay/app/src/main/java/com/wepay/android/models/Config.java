/*
 * 
 */
package com.wepay.android.models;

import android.content.Context;

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

    /** Determines if the card reader should automatically restart after a successful swipe. Defaults to false. */
    private boolean restartCardReaderAfterSuccess = false;

    /** Determines if the card reader should automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR). Defaults to true. */
    private boolean restartCardReaderAfterGeneralError = true;

    /** Determines if the card reader should automatically restart after an error other than general error. Defaults to false. */
    private boolean restartCardReaderAfterOtherErrors = false;

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
     * Determines if the card reader should automatically restart after a successful swipe.
     *
     * @return true, if the card reader restarts after success
     */
    public boolean shouldRestartCardReaderAfterSuccess() {
        return this.restartCardReaderAfterSuccess;
    }

    /**
     * Sets the option for the card reader to automatically restart after a successful swipe.
     * If not explicitly set to true, defaults to false.
     *
     * @param restartCardReaderAfterSuccess the flag to determine if the card reader should automatically restart after a successful swipe.
     * @return the config
     */
    public Config setRestartCardReaderAfterSuccess(boolean restartCardReaderAfterSuccess) {
        this.restartCardReaderAfterSuccess = restartCardReaderAfterSuccess;
        return this;
    }

    /**
     * Determines if the card reader should automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR).
     *
     * @return true, if the card reader restarts after a general error
     */
    public boolean shouldRestartCardReaderAfterGeneralError() {
        return this.restartCardReaderAfterGeneralError;
    }

    /**
     * Sets the option for the card reader to automatically restart after a general error (errorCategory:ERROR_CATEGORY_CARD_READER, errorCode:CARD_READER_GENERAL_ERROR).
     * If not explicitly set to false, defaults to true.
     *
     * @param restartCardReaderAfterGeneralError the flag to determine if the card reader should automatically restart after a general error.
     * @return the config
     */
    public Config setRestartCardReaderAfterGeneralError(boolean restartCardReaderAfterGeneralError) {
        this.restartCardReaderAfterGeneralError = restartCardReaderAfterGeneralError;
        return this;
    }

    /**
     * Determines if the card reader should automatically restart after an error other than general error.
     *
     * @return true, if the card reader restarts after an error other than general error.
     */
    public boolean shouldRestartCardReaderAfterOtherErrors() {
        return this.restartCardReaderAfterOtherErrors;
    }

    /**
     * Sets the option for the card reader to automatically restart after an error other than general error.
     * If not explicitly set to true, defaults to false.
     *
     * @param restartCardReaderAfterOtherErrors the flag to determine if the card reader should automatically restart after an error other than general error.
     * @return the config
     */
    public Config setRestartCardReaderAfterOtherErrors(boolean restartCardReaderAfterOtherErrors) {
        this.restartCardReaderAfterOtherErrors = restartCardReaderAfterOtherErrors;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("context:" 		+ this.context 		+ "\n");
        sb.append("clientId:" 		+ this.clientId 	+ "\n");
        sb.append("environment:" 	+ this.environment 	+ "\n");
        sb.append("}");

        return sb.toString();
    }
}
