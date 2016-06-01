package com.wepay.android.models;

public class AuthorizationInfo {

    private double authorizedAmount;
    private String transactionToken;
    private String tokenId;

    /**
     * Instantiates a new authorization info.
     * @param authorizedAmount the authorized amount
     * @param transactionToken the transaction token
     * @param tokenId the token id
     */
    public AuthorizationInfo(double authorizedAmount, String transactionToken, String tokenId) {
        this.authorizedAmount = authorizedAmount;
        this.transactionToken = transactionToken;
        this.tokenId = tokenId;
    }

    /**
     * Gets the authorized amount.
     *
     * @return the authorized amount
     */
    public double getAuthorizedAmount() {
        return authorizedAmount;
    }

    /**
     * Gets the token id.
     *
     * @return the token id
     */
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Gets the transaction token.
     *
     * @return the transaction token
     */
    public String getTransactionToken() {
        return transactionToken;
    }
}
