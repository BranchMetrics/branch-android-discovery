package io.branch.search;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;

/**
 * Class for searching a user query with Branch.
 */

class BranchSearchInterface {
    static final String BRANCH_SEARCH_URL = "https://vulcan.branch.io/v1/search/";
    private static final String BRANCH_QUERYHINT_URL = "https://vulcan.branch.io/v2/queryhint";
    private static final String BRANCH_AUTOSUGGEST_URL = "https://vulcan.branch.io/v2/autosuggest";
    private static final String BRANCH_SERVICE_ENABLED_URL_PREFIX = "https://vulcan.branch.io/configuration/";
    private static final String BRANCH_SERVICE_ENABLED_URL_SUFFIX = ".json";

    @VisibleForTesting static URLConnectionNetworkHandler sRawHandler
            = URLConnectionNetworkHandler.initialize(BranchSearch.Channel.UNKNOWN);

    static boolean search(final BranchSearchRequest request,
                          final IBranchSearchEvents callback) {
        final BranchSearch search = BranchSearch.getInstance();
        if (search == null) {
            return false;
        }

        final BranchConfiguration configuration = search.getBranchConfiguration();
        JSONObject jsonPayload = createPayload(request, configuration, search.getBranchDeviceInfo());
        search.getNetworkHandler(BranchSearch.Channel.SEARCH).executePost(configuration.getUrl(), jsonPayload, new IURLConnectionEvents() {
            @Override
            public void onResult(final @NonNull JSONObject response) {
                if (callback != null) {
                    boolean isError = response instanceof BranchSearchError;
                    if (isError && ((BranchSearchError) response).getErrorCode()
                            == BranchSearchError.ERR_CODE.UNAUTHORIZED_ERR) {
                        // Check if the service is enabled. If it is, we'll return the original
                        // UNAUTHORIZED_ERR error. If it's not, we'll return SERVICE_DISABLED_ERR.
                        serviceEnabled(configuration.getBranchKey(), new IBranchServiceEnabledEvents() {
                            @Override
                            public void onBranchServiceEnabledResult(@NonNull BranchServiceEnabledResult result) {
                                if (result.isEnabled()) {
                                    callback.onBranchSearchError((BranchSearchError) response);
                                } else {
                                    BranchSearchError.ERR_CODE code = BranchSearchError.ERR_CODE.SERVICE_DISABLED_ERR;
                                    callback.onBranchSearchError(new BranchSearchError(code));
                                }
                            }
                        });
                    } else if (isError) {
                        callback.onBranchSearchError((BranchSearchError) response);
                    } else {
                        callback.onBranchSearchResult(BranchSearchResult.createFromJson(request, response));
                    }
                }
            }

        });

        return true;
    }

    static boolean autoSuggest(final BranchAutoSuggestRequest request,
                               final IBranchAutoSuggestEvents callback) {
        BranchSearch search = BranchSearch.getInstance();
        if (search == null) {
            return false;
        }

        JSONObject jsonPayload = createPayload(request,
                search.getBranchConfiguration(),
                search.getBranchDeviceInfo());
        search.getNetworkHandler(BranchSearch.Channel.AUTOSUGGEST).executePost(BRANCH_AUTOSUGGEST_URL, jsonPayload, new IURLConnectionEvents() {
            @Override
            public void onResult(@NonNull JSONObject response) {
                if (callback != null) {
                    if (response instanceof BranchSearchError) {
                        callback.onBranchAutoSuggestError((BranchSearchError) response);
                    } else {
                        callback.onBranchAutoSuggestResult(BranchAutoSuggestResult.createFromJson(response));
                    }
                }
            }

        });

        return true;
    }


    static boolean queryHint(final BranchQueryHintRequest request,
                             final IBranchQueryHintEvents callback) {
        BranchSearch search = BranchSearch.getInstance();
        if (search == null) {
            return false;
        }

        JSONObject jsonPayload = createPayload(request,
                search.getBranchConfiguration(),
                search.getBranchDeviceInfo());
        search.getNetworkHandler(BranchSearch.Channel.QUERYHINT).executePost(BRANCH_QUERYHINT_URL, jsonPayload, new IURLConnectionEvents() {
            @Override
            public void onResult(@NonNull JSONObject response) {
                if (callback != null) {
                    if (response instanceof BranchSearchError) {
                        callback.onBranchQueryHintError((BranchSearchError) response);
                    } else {
                        callback.onBranchQueryHintResult(BranchQueryHintResult.createFromJson(response));
                    }
                }
            }

        });

        return true;
    }

    static void serviceEnabled(@NonNull String branchKey,
                               final @NonNull IBranchServiceEnabledEvents callback) {
        // This can be called before initialization, so don't try to get the BranchSearch instance.
        // Also, we don't have a dedicated network channel, so use the raw handler.
        String url = BRANCH_SERVICE_ENABLED_URL_PREFIX + branchKey + BRANCH_SERVICE_ENABLED_URL_SUFFIX;
        sRawHandler.executeGet(url, new IURLConnectionEvents() {
            @Override
            public void onResult(@NonNull JSONObject response) {
                BranchServiceEnabledResult result;
                if (response instanceof BranchSearchError) {
                    result = BranchServiceEnabledResult.createFromError((BranchSearchError) response);
                } else {
                    result = BranchServiceEnabledResult.createFromJson(response);
                }
                // We do not have an error callback, see BranchServiceEnabledResult.createFromError
                callback.onBranchServiceEnabledResult(result);
            }
        });
    }

    @NonNull
    static JSONObject createPayload(@NonNull BranchDiscoveryRequest request,
                                    @NonNull BranchConfiguration configuration,
                                    @NonNull BranchDeviceInfo info) {
        JSONObject jsonPayload = request.toJson();
        info.addDeviceInfo(jsonPayload);
        configuration.addConfigurationInfo(jsonPayload);
        return jsonPayload;
    }
}
