/*
 * 
 */
package com.wepay.android.internal;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.wepay.android.models.Config;
import com.wepay.android.models.MockConfig;
import java.io.IOException;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class WepayClient.
 */
public class WepayClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** The Constant BASE_URL_STAGE. */
    private static final String BASE_URL_STAGE = "https://stage.wepayapi.com/v2/";

    /** The Constant BASE_URL_PROD. */
    private static final String BASE_URL_PROD = "https://wepayapi.com/v2/";

    /** The Constant USER_AGENT. */
    private static final String WEPAY_API_VERSION = "2016-03-30";

    /** The Constant USER_AGENT. */
    private static final String USER_AGENT = "WePay Android SDK v3.0.0-prerelease-1";

    /** The client. */
    //private static AsyncHttpClient client = new AsyncHttpClient();
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request requestWithUserAgent = original.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Api-Version", WEPAY_API_VERSION)
                    .build();
                return chain.proceed(requestWithUserAgent);
            }
        })
        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build();

    private static final Handler threadHandler = new Handler(Looper.getMainLooper());


    /**
     * Gets the.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param handler the response handler
     */
    public static void get(Config config, String url,  Map<String, Object> params, final HttpResponseHandler handler) {
        String abs = getAbsoluteUrl(config, url);
        HttpUrl.Builder builder = HttpUrl.parse(abs).newBuilder()
            .addQueryParameter("client_id", config.getClientId());

        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                builder.addQueryParameter(param.getKey(), (String)param.getValue());
            }
        }

        final Request request = new Request.Builder()
            .url(builder.build())
            .get()
            .build();

        client.newCall(request).enqueue(new CallbackHandler(handler));
    }

    /**
     * Credit card tokenization, for card information form manual input.
     *
     * @param config the config
     * @param params the params
     * @param handler the response handler
     */
    public static void creditCardCreate(Config config, Map<String, Object> params, final HttpResponseHandler handler) {
        post(config, "credit_card/create", params, handler);
    }

    /**
     * Credit card tokenization, for swiped card.
     *
     * @param config the config
     * @param params the params
     * @param handler the response handler
     */
    public static void creditCardCreateSwipe(Config config, Map<String, Object> params, final HttpResponseHandler handler) {
        post(config, "credit_card/create_swipe", params, handler);
    }

    /**
     * Credit card tokenization, for dipped card (EMV).
     *
     * @param config the config
     * @param params the params
     * @param handler the response handler
     */
    public static void creditCardCreateEMV(Config config, Map<String, Object> params, final HttpResponseHandler handler) {
        post(config, "credit_card/create_emv", params, handler);
    }

    /**
     * Credit card authorization reversal.
     *
     * @param config the config
     * @param params the params
     * @param handler the response handler
     */
    public static void creditCardAuthReverse(Config config, Map<String, Object> params, final HttpResponseHandler handler) {
        post(config, "credit_card/auth_reverse", params, handler);
    }

    /**
     * Store signature.
     *
     * @param config the config
     * @param params the params
     * @param handler the response handler
     */
    public static void checkoutSignatureCreate(Config config, Map<String, Object> params, final HttpResponseHandler handler) {
        post(config, "checkout/signature/create", params, handler);
    }


    /**
     * Post.
     *
     * @param config the config
     * @param url the url
     * @param params the params
     * @param handler the response handler
     */
    private static void post(Config config, String url, Map<String, Object> params, final HttpResponseHandler handler) {
        MockConfig mockConfig = config.getMockConfig();
        String abs = getAbsoluteUrl(config, url);

        if (mockConfig != null && mockConfig.isUseMockWepayClient()) {
            Request request = new Request.Builder()
                .url(abs)
                .build();

            JSONObject response = new JSONObject();
            try {
                if ("credit_card/create".equals(url)) {
                    if (mockConfig.isCardTokenizationFailure()) {
                        handler.onFailure(500, response, null);
                    } else {
                        response.put("credit_card_id", "1234567890");
                    }
                } else if ("credit_card/create_swipe".equals(url)) {
                    if (mockConfig.isCardTokenizationFailure()) {
                        handler.onFailure(500, response, null);
                    } else {
                        response.put("credit_card_id", "1234567890");
                    }
                } else if ("credit_card/create_emv".equals(url)) {
                    if (mockConfig.isEMVAuthFailure()) {
                        handler.onFailure(500, response, null);
                    }
                } else if ("checkout/signature/create".equals(url)) {
                    response.put("signature_url", "<signature url>");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            handler.onSuccess(200, response);
        } else {
            params.put("client_id", config.getClientId());
            JSONObject jsonParams = null;
            try {
                jsonParams = new JSONObject(new Gson().toJson(params));
            } catch (JSONException e) {
                e.printStackTrace();
                // Do nothing, the http library will throw an appropriate error
            }

            RequestBody body = RequestBody.create(JSON, jsonParams.toString());
            Request request = new Request.Builder()
                .url(abs)
                .post(body)
                .build();

            client.newCall(request).enqueue(new CallbackHandler(handler));
        }
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

    public interface HttpResponseHandler {

        void onSuccess(int statusCode, JSONObject response);

        void onFailure(int statusCode, JSONObject error, Throwable throwable);
    }

    private static class CallbackHandler implements Callback {

        private final HttpResponseHandler handler;

        public CallbackHandler(HttpResponseHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onFailure(Call call, final IOException throwable) {
            threadHandler.post(new Runnable() {
                @Override
                public void run() {
                    handler.onFailure(500, null, throwable);
                }
            });
        }

        @Override
        public void onResponse(Call call, final Response response) throws IOException {
            threadHandler.post(new Runnable() {
                @Override
                public void run() {
                    ResponseBody body = response.body();
                    String json = null;
                    try {
                        json = body.string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    JSONObject object = convertToJSONObject(json);
                    if (response.isSuccessful()) {
                        handler.onSuccess(response.code(), object);
                    } else {
                        handler.onFailure(response.code(), object, new RuntimeException(json));
                    }

                    body.close();
                }
            });
        }

        private JSONObject convertToJSONObject(String json) {
            JSONObject object = null;
            try {
                object = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return object;
        }
    }
}
