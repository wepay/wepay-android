package com.wepay.android.internal;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.wepay.android.models.Config;
import com.wepay.android.models.Error;
import com.wepay.android.models.MockConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class WepayClient.
 */
public class WepayClient {
    /** The Constant BASE_URL_STAGE. */
    private static final String BASE_URL_STAGE = "https://stage.wepayapi.com/v2/";
    
    /** The Constant BASE_URL_PROD. */
    private static final String BASE_URL_PROD = "https://wepayapi.com/v2/";

    /** The Constant USER_AGENT. */
    private static final String WEPAY_API_VERSION = "2016-03-30";

    /** The Constant USER_AGENT. */
    private static final String USER_AGENT = "WePay Android SDK v3.0.0";

    /** The request queue */
    private static RequestQueue requestQueue = null;

    /** The client-side API call timeout duration in milliseconds */
    private static final int REQUEST_TIMEOUT_MS = 40000;

    /**
     * Credit card tokenization, for card information form manual input.
     *
     * @param config the config
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void creditCardCreate(Config config, Map<String, Object> params, ResponseHandler responseHandler) {
        post(config, "credit_card/create", params, responseHandler);
    }

    /**
     * Credit card tokenization, for swiped card.
     *
     * @param config the config
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void creditCardCreateSwipe(Config config, Map<String, Object> params, ResponseHandler responseHandler) {
        post(config, "credit_card/create_swipe", params, responseHandler);
    }

    /**
     * Credit card tokenization, for dipped card (EMV).
     *
     * @param config the config
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void creditCardCreateEMV(Config config, Map<String, Object> params, ResponseHandler responseHandler) {
        post(config, "credit_card/create_emv", params, responseHandler);
    }

    /**
     * Credit card authorization reversal.
     *
     * @param config the config
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void creditCardAuthReverse(Config config, Map<String, Object> params, ResponseHandler responseHandler) {
        post(config, "credit_card/auth_reverse", params, responseHandler);
    }

    /**
     * Store signature.
     *
     * @param config the config
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void checkoutSignatureCreate(Config config, Map<String, Object> params, ResponseHandler responseHandler) {
        post(config, "checkout/signature/create", params, responseHandler);
    }


    /**
     * Get the request.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param responseHandler the response handler
     */
    private static void get(Config config, String url, Map<String, Object> params, ResponseHandler responseHandler) {
        makeRequest(url, Request.Method.GET, config, params, responseHandler);
    }

    /**
     * Post the request.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param responseHandler the response handler
     */
    private static void post(Config config, String url, Map<String, Object> params, final ResponseHandler responseHandler) {

        MockConfig mockConfig = config.getMockConfig();
        if (mockConfig != null && mockConfig.isUseMockWepayClient()) {
            JSONObject response = new JSONObject();
            try {
                if ("credit_card/create".equals(url)) {
                    if (mockConfig.isCardTokenizationFailure()) {
                        responseHandler.onFailure(500, null, response);
                    } else {
                        response.put("credit_card_id", "1234567890");
                    }
                } else if ("credit_card/create_swipe".equals(url)) {
                    if (mockConfig.isCardTokenizationFailure()) {
                        responseHandler.onFailure(500, null, response);
                    } else {
                        response.put("credit_card_id", "1234567890");
                    }
                } else if ("credit_card/create_emv".equals(url)) {
                    if (mockConfig.isEMVAuthFailure()) {
                        responseHandler.onFailure(500, null, response);
                    }
                } else if ("checkout/signature/create".equals(url)) {
                    response.put("signature_url", "<signature url>");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            responseHandler.onSuccess(200, response);
        } else {
            makeRequest(url, Request.Method.POST, config, params, responseHandler);
        }
    }

    private static void makeRequest(String url, int method, Config config, Map<String, Object> params, final ResponseHandler responseHandler) {
        String fullUrl = getAbsoluteUrl(config, url);
        JSONObject requestBody = null;

        if (requestQueue == null) {
            init(config.getContext());
        }

        params.put("client_id", config.getClientId());

        try {
            requestBody = new JSONObject(new Gson().toJson(params));
        } catch (JSONException e) {
            LogHelper.log("Error: Unable to serialize request params to JSON. Params: " + params.toString() + ". Failure: " + e.getLocalizedMessage());
        }

        // Create the response listener.
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                responseHandler.onSuccess(200, response);
            }
        };

        // Create the error listener
        Response.ErrorListener errorListener =  new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                JSONObject response = new JSONObject();
                String errorResponse = "";

                if (error.networkResponse != null) {
                    try {
                        errorResponse = new String(error.networkResponse.data, HttpHeaderParser.parseCharset(error.networkResponse.headers));
                        response = new JSONObject(errorResponse);
                    } catch (UnsupportedEncodingException e) {
                        LogHelper.log("Error: Unable to convert error response to string. Failure: " + e.getLocalizedMessage());
                    } catch (JSONException e) {
                        LogHelper.log("Error: Unable to serialize response " + errorResponse + " into JSON. Failure: " + e.getLocalizedMessage());
                    }

                    responseHandler.onFailure(error.networkResponse.statusCode, error.getCause(), response);
                } else {
                    Integer errorCode = 500;
                    String errorDescription = null;

                    try {
                        response.put("error_code", errorCode);
                        response.put("error_domain", Error.ERROR_DOMAIN_API);
                        response.put("error", Error.ERROR_CATEGORY_API);

                        if (error.getCause() != null) {
                            errorDescription = error.getCause().getLocalizedMessage();
                            response.put("error_description", errorDescription);
                        }
                    } catch (JSONException e) {
                        LogHelper.log("Error: unable to populate error response. Caught exception: " + e.getLocalizedMessage());
                    }

                    LogHelper.log("Received error response, but response contains no data.");
                    LogHelper.log("Error response class: " + error.getClass().toString());

                    if (errorDescription != null) {
                        LogHelper.log("Error response description: " + errorDescription);
                    }

                    responseHandler.onFailure(errorCode, error.getCause(), response);
                }
            }
        };

        // Create a JSON Request with some of the methods overwritten.
        JsonObjectRequest request = new JsonObjectRequest(method, fullUrl, requestBody, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();

                headers.put("User-Agent", USER_AGENT);
                headers.put("Api-Version", WEPAY_API_VERSION);

                return headers;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String rawJSON = new String (response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    JSONObject responseJSON = new JSONObject(rawJSON);
                    responseJSON.put("headers", new JSONObject(response.headers));

                    return Response.success(responseJSON, HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    LogHelper.log("Error: Unable to parse header data into string.");
                } catch (JSONException e) {
                    LogHelper.log("Error: Unable to serialize string into JSON object.");
                }

                return super.parseNetworkResponse(response);
            }
        };

        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    /**
     * Gets the absolute url.
     *
     * @param config the config
     * @param relativeUrl the relative url
     * @return the absolute url
     */
    static String getAbsoluteUrl(Config config, String relativeUrl) {
        String environment = config.getEnvironment();

        if (environment.equalsIgnoreCase(Config.ENVIRONMENT_STAGE)) {
            return BASE_URL_STAGE + relativeUrl;
        } else if (environment.equalsIgnoreCase(Config.ENVIRONMENT_PRODUCTION)) {
            return BASE_URL_PROD + relativeUrl;
        } else {
            return environment + relativeUrl;
        }
    }

    private static void init(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public interface ResponseHandler {
        void onSuccess(int statusCode, JSONObject response);
        void onFailure(int statusCode, Throwable throwable, JSONObject errorResponse);
    }
}
