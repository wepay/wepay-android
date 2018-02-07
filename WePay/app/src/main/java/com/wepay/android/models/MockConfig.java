package com.wepay.android.models;

import com.wepay.android.enums.PaymentMethod;

/**
 * The Class MockConfig contains the configuration required when using mock card reader and/or WepayClient implementation.
 */
public class MockConfig {

    /** The payment method to mock. Defaults to SWIPE.*/
    private PaymentMethod mockPaymentMethod = PaymentMethod.SWIPE;

    /** Determines if mock card reader implementation is used. Defaults to true.*/
    private boolean useMockCardReader = true;

    /** Determines if mock WepayClient implementation is used. Defaults to true.*/
    private boolean useMockWepayClient = true;

    /** Determines if a card reader timeout should be mocked. Defaults to false.*/
    private boolean cardReadTimeout = false;

    /** Determines if a card reading failure should be mocked. Defaults to false.*/
    private boolean cardReadFailure = false;

    /** Determines if a card tokenization failure should be mocked. Defaults to false.*/
    private boolean cardTokenizationFailure = false;

    /** Determines if a EMV authorization failure should be mocked. Defaults to false.*/
    private boolean EMVAuthFailure = false;

    /** Determines if a battery info failure should be mocked. Defaults to false.*/
    private boolean batteryLevelError = false;

    /** Determines if multiple EMV application should be mocked. Defaults to false.*/
    private boolean multipleEMVApplication = false;

    /** Determines if the mock card reader is available for the purpose of establishing a connection. Defaults to true.*/
    private boolean mockCardReaderDetected = true;

    /** Name of the device to mock. Defaults to Samsung SM-G900P.*/
    private String mockedDeviceName = "Samsung SM-G900P";

    /**
     * Default constructor.
     */
    public MockConfig() {

    }

    /**
     * Constructor with parameters to indicate whether mock card reader/WepayClient implementations will be used.
     *
     * @param useMockCardReader if the mock card reader implementation will be used.
     * @param useMockWepayClient If the mock WepayClient implementation will be used.
     */
    public MockConfig(boolean useMockCardReader, boolean useMockWepayClient) {
        this.useMockCardReader = useMockCardReader;
        this.useMockWepayClient = useMockWepayClient;
    }

    /**
     * Determines whether mocked card reader is used.
     *
     * @return if using mocked card reader
     */
    public boolean isUseMockCardReader() {
        return useMockCardReader;
    }

    /**
     * Sets the option for whether to use mocked card reader.
     * If not explicitly set to false, defaults to true.
     *
     * @param useMockCardReader whether to use mocked card reader
     * @return the MockConfig instance
     */
    public MockConfig setUseMockCardReader(boolean useMockCardReader) {
        this.useMockCardReader = useMockCardReader;
        return this;
    }

    /**
     * Determines if mocked WepayClient is used.
     *
     * @return if using mocked WepayClient
     */
    public boolean isUseMockWepayClient() {
        return useMockWepayClient;
    }

    /**
     * Sets the option for whether to use mocked WepayClient.
     * If not explicitly set to false, defaults to true.
     *
     * @param useMockWepayClient whether to used mocked WepayClient
     * @return the MockConfig instance
     */
    public MockConfig setUseMockWepayClient(boolean useMockWepayClient) {
        this.useMockWepayClient = useMockWepayClient;
        return this;
    }

    /**
     * Determines the mocked payment method used.
     *
     * @return the mocked payment method
     */
    public PaymentMethod getMockPaymentMethod() {
        return mockPaymentMethod;
    }

    /**
     * Sets the option for mocked payment method to use.
     * If not explicitly set to DIP, defaults to SWIPE.
     *
     * @param paymentMethod payment method to use
     * @return the MockConfig instance
     */
    public MockConfig setMockPaymentMethod(PaymentMethod paymentMethod) {
        mockPaymentMethod = paymentMethod;
        return this;
    }

    /**
     * Determines if card reader timeout is mocked.
     *
     * @return the card reader timeout config
     */
    public boolean isCardReadTimeout() {
        return cardReadTimeout;
    }

    /**
     * Sets the option for whether to mock card reader timeout.
     * If not explicitly set to true, defaults to false.
     *
     * @param cardReadTimeout whether to mock card reader timeout
     * @return the MockConfig instance
     */
    public MockConfig setCardReadTimeout(boolean cardReadTimeout) {
        this.cardReadTimeout = cardReadTimeout;
        return this;
    }

    /**
     * Determines if a card reading failure is mocked.
     *
     * @return the card reading failure config
     */
    public boolean isCardReadFailure() {
        return cardReadFailure;
    }

    /**
     * Sets the option for whether to mock a card reading failure.
     * If not explicitly set to true, defaults to false.
     *
     * @param cardReadFailure whether to mock a card reading failure
     * @return the MockConfig instance
     */
    public MockConfig setCardReadFailure(boolean cardReadFailure) {
        this.cardReadFailure = cardReadFailure;
        return this;
    }

    /**
     * Determines if a card tokenization failure is mocked.
     *
     * @return the card tokenization failure config
     */
    public boolean isCardTokenizationFailure() {
        return cardTokenizationFailure;
    }

    /**
     * Sets the option for whether to mock card tokenization failure.
     * If not explicitly set to true, defaults to false.
     *
     * @param cardTokenizationFailure the card tokenization failure config
     * @return the MockConfig instance
     */
    public MockConfig setCardTokenizationFailure(boolean cardTokenizationFailure) {
        this.cardTokenizationFailure = cardTokenizationFailure;
        return this;
    }

    /**
     *  Determines if an EMV authorization failure is mocked.
     *
     * @return the EMV authorization failure config
     */
    public boolean isEMVAuthFailure() {
        return EMVAuthFailure;
    }

    /**
     * Sets the option for whether to mock an EMV authorization failure.
     * If not explicitly set to true, defaults to false.
     *
     * @param EMVAuthFailure the EMV authorization failure config
     * @return the MockConfig instance
     */
    public MockConfig setEMVAuthFailure(boolean EMVAuthFailure) {
        this.EMVAuthFailure = EMVAuthFailure;
        return this;
    }

    /**
     *  Determines if a battery level error is mocked.
     *
     * @return the battery level error config
     */
    public boolean isBatteryLevelError() {
        return batteryLevelError;
    }

    /**
     * Sets the option for whether to mock a battery level error.
     * If not explicitly set to true, defaults to false.
     *
     * @param batteryLevelError the battery level error config
     * @return the MockConfig instance
     */
    public MockConfig setBatteryLevelError(boolean batteryLevelError) {
        this.batteryLevelError = batteryLevelError;
        return this;
    }

    /**
     * Determines if card having multiple EMV applications is mocked.
     *
     * @return the multiple EMV applications config
     */
    public boolean isMultipleEMVApplication() {
        return multipleEMVApplication;
    }

    /**
     * Sets the option for whether to mock a card with multiple EMV applications.
     * If not explicitly set to true, defaults to false.
     *
     * @param multipleEMVApplication whether to mock card with multiple EMV applications
     * @return the MockConfig instance
     */
    public MockConfig setMultipleEMVApplication(boolean multipleEMVApplication) {
        this.multipleEMVApplication = multipleEMVApplication;
        return this;
    }

    /**
     * Determines if the mock card reader is available for the purpose of establishing a connection.
     *
     * @return the card reader isDetected config
     */
    public boolean isMockCardReaderDetected() {
        return mockCardReaderDetected;
    }

    /**
     * Sets the option for whether to mock a card reader that is available for the purpose of
     * establishing a connection. If not explicitly set to false, defaults to true.
     *
     * @param isDetected the card reader isDetected config
     * @return the MockConfig instance
     */
    public MockConfig setMockCardReaderDetected(boolean isDetected) {
        this.mockCardReaderDetected = isDetected;
        return this;
    }

    /**
     * Determines name of the device being mocked.
     *
     * @return name of mocked device
     */
    public String getMockedDeviceName() {
        return mockedDeviceName;
    }

    /**
     * Sets name for the mocked device.
     *
     * @param mockedDeviceName name of mocked device
     * @return the MockConfig instance
     */
    public MockConfig setMockedDeviceName(String mockedDeviceName) {
        this.mockedDeviceName = mockedDeviceName;
        return this;
    }
}
