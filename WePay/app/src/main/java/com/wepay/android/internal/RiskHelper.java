package com.wepay.android.internal;

import com.threatmetrix.TrustDefenderMobile.EndNotifier;
import com.threatmetrix.TrustDefenderMobile.ProfilingResult;
import com.threatmetrix.TrustDefenderMobile.THMStatusCode;
import com.threatmetrix.TrustDefenderMobile.TrustDefenderMobile;
import com.wepay.android.models.Config;

public class RiskHelper {

    /** The Constant PROFILING_TIMEOUT_SECS. */
    final public static int PROFILING_TIMEOUT_SECS = 30;

    /** The WePay ThreatMetrix org id. */
    final private static String WEPAY_THREATMETRIX_ORG_ID = "ncwzrc4k";

    /** The config. */
    private Config config = null;

    /** The session id. */
    private String sessionId;

    /**
     * The TrustDefenderMobile profile
     */
    final private TrustDefenderMobile profile = new TrustDefenderMobile(WEPAY_THREATMETRIX_ORG_ID);

    /**
     * The constructor
     *
     * @param config the WePay config
     */
    public RiskHelper(Config config) {
        // save the config
        this.config = config;
    }


    /**
     * Gets the session id.
     *
     * @return the session id - may be null if profiling fails
     */
    public String getSessionId() {
        // if a profiling is not currently in progress
        if (this.sessionId == null) {
            // initialize profiling again
            this.initProfiling();

            // start profiling
            this.sessionId = this.startProfiling();
        }

        return this.sessionId;
    }


    /**
     * Initialize the TrustDefenderMobile profiling with options.
     */
    private void initProfiling() {
        // get a reference to the risk helper for passing inside
        final RiskHelper riskHelper = this;

        // initialize Threatmetrix with profiling options
        this.profile.init(new com.threatmetrix.TrustDefenderMobile.Config()
                .setDisableOkHttp(true)
                .setTimeout(PROFILING_TIMEOUT_SECS)
                .setRegisterForLocationServices(this.config.isUseLocation())
                .setContext(this.config.getContext())
                .setEndNotifier(new EndNotifier() {
                    @Override
                    public void complete(ProfilingResult result) {
                        // Called once profiling is complete
                        if (result.getStatus() == THMStatusCode.THM_OK) {
                            // success!
                            // do nothing special
                        } else {
                            // failed
                            // do nothing special
                        }

                        // stop requesting location
                        riskHelper.stopLocationServices();

                        // delete the sessionId
                        riskHelper.sessionId = null;

                        // cleanup resources
                        profile.tidyUp();
                    }
                }));
    }

    /**
     * Start profiling.
     *
     * @return the session id
     */
    private String startProfiling() {
        // start profiling
        THMStatusCode status = this.profile.doProfileRequest();

        if (status == THMStatusCode.THM_OK) {
            // The profiling successfully started, return session id
            return this.profile.getResult().getSessionID();
        } else {
            // profiling failed, return null
            return null;
        }
    }

    /**
     * Stop location services.
     */
    private void stopLocationServices() {
        this.profile.pauseLocationServices(true);
    }

}
