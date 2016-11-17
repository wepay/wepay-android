package com.wepay.android.models;

import java.math.BigDecimal;

public class AuthorizationInfo {

    private BigDecimal authorizedAmount;
    private String transactionToken;
    private String tokenId;

    /**
     * Instantiates a new authorization info.
     * @param authorizedAmount the authorized amount
     * @param transactionToken the transaction token
     * @param tokenId the token id
     */
    public AuthorizationInfo(BigDecimal authorizedAmount, String transactionToken, String tokenId) {
        this.authorizedAmount = authorizedAmount;
        this.transactionToken = transactionToken;
        this.tokenId = tokenId;
    }

    /**
     * Gets the authorized amount.
     *
     * @return the authorized amount
     */
    public BigDecimal getAuthorizedAmount() {
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
