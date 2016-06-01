/*
 * 
 */
package com.wepay.android.internal;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wepay.android.models.Config;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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
    private static final String USER_AGENT = "WePay Android SDK v1.0.0";

    /** The client. */
    private static AsyncHttpClient client = new AsyncHttpClient();

    /**
     * Gets the.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void get(Config config, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.setUserAgent(USER_AGENT);
        client.addHeader("Api-Version", WEPAY_API_VERSION);

        params.put("client_id", config.getClientId());
        String abs = getAbsoluteUrl(config, url);

        client.get(abs, params, responseHandler);
    }

    /**
     * Post.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param responseHandler the response handler
     */
    public static void post(Config config, String url, Map<String, Object> params, AsyncHttpResponseHandler responseHandler) {
        String contentType = "application/json";
        client.setUserAgent(USER_AGENT);
        client.addHeader("Api-Version", WEPAY_API_VERSION);

        params.put("client_id", config.getClientId());
        JSONObject jsonParams = null;
        try {
            jsonParams = new JSONObject(new Gson().toJson(params));
        } catch (JSONException e) {
            e.printStackTrace();
            // Do nothing, the http library will throw an appropriate error
        }

        StringEntity entity = null;

        String abs = getAbsoluteUrl(config, url);

        try {
            entity = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            // Do nothing, the http library will throw an appropriate error
        }

        client.post(config.getContext(), abs, entity, contentType, responseHandler);
    }

    /**
     * Gets the absolute url.
     *
     * @param config the config
     * @param relativeUrl the relative url
     * @return the absolute url
     */
    private static String getAbsoluteUrl(Config config, String relativeUrl) {
        String environment = config.getEnvironment();

        if (environment.equalsIgnoreCase(Config.ENVIRONMENT_STAGE)) {
            return BASE_URL_STAGE + relativeUrl;
        } else if (environment.equalsIgnoreCase(Config.ENVIRONMENT_PRODUCTION)) {
            return BASE_URL_PROD + relativeUrl;
        } else {
            return environment + relativeUrl;
        }
    }
}
