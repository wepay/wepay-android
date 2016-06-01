/*
 * 
 */
package com.wepay.android.models;

/**
 * The Class PaymentToken represents payment information that was obtained from the user and is stored on WePay servers. This token can be used to complete the payment transaction via WePay's web APIs.
 */
public class PaymentToken {

    /** The token id. */
    private String tokenId;

    /**
     * Instantiates a new payment token.
     *
     * @param tokenId the token id
     */
    public PaymentToken(String tokenId) {
        this.tokenId = tokenId;
    }

    /**
     * Gets the token id.
     *
     * @return the token id
     */
    public String getTokenId() {
        return this.tokenId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("tokenId:" + this.tokenId + "\n");
        sb.append("}");

        return sb.toString();
    }

}
