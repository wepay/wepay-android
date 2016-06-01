/*
 * 
 */
package com.wepay.android.models;

import android.location.Address;

import com.wepay.android.enums.PaymentMethod;

import java.util.HashMap;

/**
 * The Class PaymentInfo represents all the information obtained via a particular payment method.
 */
public class PaymentInfo {

    /** The first name. */
    private String firstName = null;

    /** The last name. */
    private String lastName = null;

    /** The email. */
    private String email = null;

    /** The payment description. */
    private String paymentDescription = null;

    /** The billing address. */
    private Address billingAddress = null;

    /** The shipping address. */
    private Address shippingAddress = null;

    /** The payment method. */
    private PaymentMethod paymentMethod = null;

    /** The manual info. */
    private Object manualInfo = null;

    /** The card reader info. */
    private Object cardReaderInfo = null;

    /** Determines if the info was obtained via virtual terminal. */
    private boolean virtualTerminal = false;

    /**
     * Instantiates a new payment info. Use this constructor when representing manually obtained card data.
     * Note: For virtual terminal, name is optional. A placeholder name will be inserted if it is not provided.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @param email the email
     * @param paymentDescription the payment description
     * @param billingAddress the billing address
     * @param shippingAddress the shipping address
     * @param paymentMethod the payment method
     * @param ccNumber the cc number
     * @param cvv the cvv
     * @param expMonth the expiration month
     * @param expYear the expiration year
     * @param virtualTerminal the virtual terminal flag
     */
    public PaymentInfo(String firstName, String lastName, String email,
            String paymentDescription, Address billingAddress,
            Address shippingAddress, PaymentMethod paymentMethod,
            String ccNumber, String cvv, String expMonth, String expYear,
            boolean virtualTerminal) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.paymentDescription = paymentDescription;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.virtualTerminal = virtualTerminal;

        HashMap<String, String> manualInfo = new HashMap<String, String>();
        manualInfo.put("cc_number", ccNumber);
        manualInfo.put("cvv", cvv);
        manualInfo.put("expiration_month", expMonth);
        manualInfo.put("expiration_year", expYear);

        this.manualInfo = manualInfo;

        // for virtual terminal. name is optional. We insert a placeholder name if not provided
        if (this.isVirtualTerminal()) {
            if (this.firstName == null && this.lastName == null) {
                this.firstName = "Virtual Terminal";
                this.lastName = "User";
            }
        }
    }

    /** \internal
     * Instantiates a new payment info.
     * Use this constructor to represent payment info obtained via a card reader.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @param paymentDescription the payment description
     * @param paymentMethod the payment method
     * @param cardReaderInfo the card reader info
     */
    public PaymentInfo(String firstName, String lastName, String paymentDescription, PaymentMethod paymentMethod, Object cardReaderInfo) {
        this(firstName, lastName, null, paymentDescription, null, null, paymentMethod, null, null, null, null, false);
        this.cardReaderInfo = cardReaderInfo;
    }

    /**
     * Gets the first name.
     *
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Gets the last name.
     *
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Gets the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the payment description.
     *
     * @return the paymentDescription
     */
    public String getPaymentDescription() {
        return paymentDescription;
    }

    /**
     * Gets the billing address.
     *
     * @return the billingAddress
     */
    public Address getBillingAddress() {
        return billingAddress;
    }

    /**
     * Gets the shipping address.
     *
     * @return the shippingAddress
     */
    public Address getShippingAddress() {
        return shippingAddress;
    }

    /**
     * Gets the payment method.
     *
     * @return the paymentMethod
     */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * Gets the manual info.
     *
     * @return the manualInfo
     */
    public Object getManualInfo() {
        return manualInfo;
    }

    /** \internal
     * Gets the card reader info.
     *
     * @return the cardReaderInfo
     */
    public Object getCardReaderInfo() {
        return cardReaderInfo;
    }

    /**
     * Determines if the card info was obtained via Virtual Terminal.
     *
     * @return true if virtual terminal, else false
     */
    public boolean isVirtualTerminal() {
        return virtualTerminal;
    }

    /**
     * Allows adding an email if one is not already present. The call will be ignored if an email is already present.
     *
     * @param email the email to be added
     */
    public void addEmail(String email) {
        if (this.email == null) {
            this.email = email;
        }
    }

    /**
     * Gets the full name
     *
     * @return full name if available, otherwise null
     */
    public String getFullName() {
        String firstName = this.getFirstName() != null ? this.getFirstName() : "Unknown";
        String lastName = this.getLastName() != null ? this.getLastName() : "Unknown";

        String name = firstName + " " + lastName;
        name = name.trim();

        if (name.equalsIgnoreCase("")) {
            return null;
        } else {
            return name;
        }
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("firstName:" 			+ this.firstName 			+ "\n");
        sb.append("lastName:" 			+ this.lastName 			+ "\n");
        sb.append("email:" 				+ this.email 				+ "\n");
        sb.append("paymentDescription:" + this.paymentDescription 	+ "\n");
        sb.append("paymentMethod:" 		+ this.paymentMethod 		+ "\n");
        sb.append("billingAddress:" 	+ this.addressToString(this.billingAddress) + "\n");
        sb.append("shippingAddress:" 	+ this.addressToString(this.shippingAddress) + "\n");
        sb.append("}");

        return sb.toString();
    }

    private String addressToString(Address address) {
        if (address == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\taddressLine1:" 	+ address.getAddressLine(0) 		+ "\n");
        sb.append("\taddressLine2:" 	+ address.getAddressLine(1) 		+ "\n");
        sb.append("\tlocality    :" 	+ address.getLocality()	 			+ "\n");
        sb.append("\tpostalCode  :" 	+ address.getPostalCode() 			+ "\n");
        sb.append("\tcountryCode :" 	+ address.getCountryCode() 			+ "\n");
        sb.append("}");

        return sb.toString();
    }
}
