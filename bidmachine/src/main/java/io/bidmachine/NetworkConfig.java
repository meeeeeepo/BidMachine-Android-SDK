package io.bidmachine;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.bidmachine.unified.UnifiedAdRequestParams;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to store and provide Network specific configuration.
 * Inheritors should implement at least constructor with {@link Map<String, String>} argument, which is required to load
 * config from json.
 */
public abstract class NetworkConfig {

    @Nullable
    private NetworkAdapter networkAdapter;
    @Nullable
    private Map<String, String> networkParams;
    @Nullable
    private Map<String, String> baseMediationConfig;
    @Nullable
    private EnumMap<AdsFormat, Map<String, String>> typedMediationConfigs;
    @Nullable
    private AdsType[] supportedAdsTypes;
    @Nullable
    private AdsType[] mergedAdsTypes;
    @NonNull
    private NetworkConfigParams networkConfigParams = new NetworkConfigParams() {
        @Nullable
        @Override
        public Map<String, String> obtainNetworkParams() {
            return networkParams != null ? new HashMap<>(networkParams) : null;
        }

        @Nullable
        @Override
        public EnumMap<AdsFormat, Map<String, String>> obtainNetworkMediationConfigs(AdsFormat... adsFormats) {
            EnumMap<AdsFormat, Map<String, String>> resultMap = null;
            if (adsFormats != null && adsFormats.length > 0) {
                for (AdsFormat format : adsFormats) {
                    Map<String, String> resultConfig = null;
                    if (typedMediationConfigs != null) {
                        Map<String, String> typedConfig = typedMediationConfigs.get(format);
                        if (typedConfig != null) {
                            resultConfig = prepareTypedMediationConfig(typedConfig);
                        }
                    }
                    if (resultConfig != null) {
                        if (resultMap == null) {
                            resultMap = new EnumMap<>(AdsFormat.class);
                        }
                        resultMap.put(format, resultConfig);
                    }
                }
            }
            return resultMap;
        }
    };

    protected NetworkConfig(@Nullable Map<String, String> networkParams) {
        withNetworkParams(networkParams);
    }

    @NonNull
    protected abstract NetworkAdapter createNetworkAdapter();

    /**
     * @return unique Network key
     */
    @NonNull
    public String getKey() {
        return obtainNetworkAdapter().getKey();
    }

    /**
     * @return Network version
     */
    @Nullable
    public String getVersion() {
        return obtainNetworkAdapter().getVersion();
    }

    /**
     * @return Network {@link NetworkAdapter} implementation
     */
    @NonNull
    public NetworkAdapter obtainNetworkAdapter() {
        if (networkAdapter == null) {
            networkAdapter = createNetworkAdapter();
        }
        return networkAdapter;
    }

    /**
     * Sets Network global configuration (will be used for {@link NetworkAdapter#initialize(ContextProvider, UnifiedAdRequestParams, NetworkConfigParams)})
     *
     * @param config map of parameters which will be used to initialize Network
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T extends NetworkConfig> T withNetworkParams(@Nullable Map<String, String> config) {
        this.networkParams = config;
        return (T) this;
    }

    /**
     * Sets global parameter specific for current Network (see {@link #withNetworkParams(Map)})
     *
     * @param key   parameter key
     * @param value parameter value
     */
    @SuppressWarnings({"unchecked", "unused"})
    public <T extends NetworkConfig> T setNetworkParam(@NonNull String key, @NonNull String value) {
        if (networkParams == null) {
            networkParams = new HashMap<>();
        }
        networkParams.put(key, value);
        return (T) this;
    }

    /**
     * Sets `base` Network mediation configuration (will be used for {@link HeaderBiddingAdapter#collectHeaderBiddingParams(ContextProvider, UnifiedAdRequestParams, HeaderBiddingCollectParamsCallback, Map)}).
     * Will be merged with config provided for certain {@link AdsFormat}
     *
     * @param config map of parameters which will be used for Network mediation
     */
    @SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
    public <T extends NetworkConfig> T withBaseMediationConfig(@Nullable Map<String, String> config) {
        this.baseMediationConfig = config;
        return (T) this;
    }

    /**
     * Sets specific `base` Network mediation configuration parameter (see {@link #withBaseMediationConfig(Map)})
     *
     * @param key   parameter key
     * @param value parameter value
     */
    @SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
    public <T extends NetworkConfig> T setBaseMediationParam(@NonNull String key, @NonNull String value) {
        if (baseMediationConfig == null) {
            baseMediationConfig = new HashMap<>();
        }
        baseMediationConfig.put(key, value);
        return (T) this;
    }

    /**
     * Sets Network mediation configuration (will be used for {@link HeaderBiddingAdapter#collectHeaderBiddingParams(ContextProvider, UnifiedAdRequestParams, HeaderBiddingCollectParamsCallback, Map)}).
     * Will be used only for provided {@link AdsFormat}
     *
     * @param adsFormat certain {@link AdsFormat} that should use provided {@param config}
     * @param config    map of parameters to be used with Network Mediation
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T extends NetworkConfig> T withMediationConfig(@NonNull AdsFormat adsFormat,
                                                           @Nullable Map<String, String> config) {
        if (config == null) {
            if (typedMediationConfigs != null) {
                typedMediationConfigs.remove(adsFormat);
            }
        } else {
            if (typedMediationConfigs == null) {
                typedMediationConfigs = new EnumMap<>(AdsFormat.class);
            }
            typedMediationConfigs.put(adsFormat, config);
        }
        return (T) this;
    }

    /**
     * Sets supported {@link AdsType}s for Network
     *
     * @param adsType required {@link AdsType}s
     */
    public NetworkConfig forAdTypes(@NonNull AdsType... adsType) {
        this.supportedAdsTypes = adsType;
        return this;
    }

    /**
     * Method which returns parameters to be used for mediation process.
     * If no specific parameters were provided - will return default values set by {@link NetworkConfig#withBaseMediationConfig(Map)}
     *
     * @param adsType         required {@link AdsType}
     * @param adRequestParams provided typed {@link UnifiedAdRequestParams}
     * @return map of parameters for provided {@link AdsType} and {@link AdContentType} to be used for mediation process
     */
    @Nullable
    public <T extends UnifiedAdRequestParams> Map<String, String> peekMediationConfig(@NonNull AdsType adsType,
                                                                                      @NonNull T adRequestParams,
                                                                                      @NonNull AdContentType adContentType) {
        Map<String, String> resultConfig = null;
        if (typedMediationConfigs != null) {
            Map<String, String> typedConfig = null;
            for (Map.Entry<AdsFormat, Map<String, String>> entry : typedMediationConfigs.entrySet()) {
                if (entry.getKey().isMatch(adsType, adRequestParams, adContentType)) {
                    typedConfig = entry.getValue();
                }
            }
            if (typedConfig != null) {
                // Copy provided config since we shouldn't modify it
                resultConfig = prepareTypedMediationConfig(typedConfig);
            }
        }
        return resultConfig;
    }

    /**
     * Method which returns array of merged {@link NetworkAdapter#getSupportedTypes()} and {@link NetworkConfig#getSupportedAdsTypes()}.
     * Will be called only once per app session.
     *
     * @return array of supported {@link AdsType}s
     */
    AdsType[] getSupportedAdsTypes() {
        if (mergedAdsTypes == null) {
            AdsType[] adapterSupportedTypes = obtainNetworkAdapter().getSupportedTypes();
            ArrayList<AdsType> resultList = new ArrayList<>();
            for (AdsType adsType : adapterSupportedTypes) {
                if (supportedAdsTypes == null || contains(supportedAdsTypes, adsType)) {
                    resultList.add(adsType);
                }
            }
            mergedAdsTypes = resultList.toArray(new AdsType[0]);
        }
        return mergedAdsTypes;
    }

    @NonNull
    NetworkConfigParams getNetworkConfigParams() {
        return networkConfigParams;
    }

    private Map<String, String> prepareTypedMediationConfig(@NonNull Map<String, String> config) {
        Map<String, String> resultConfig = new HashMap<>();
        if (networkParams != null && useNetworkParamsAsMediationBase()) {
            resultConfig.putAll(networkParams);
        }
        if (baseMediationConfig != null) {
            resultConfig.putAll(baseMediationConfig);
        }
        resultConfig.putAll(config);
        return resultConfig;
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean useNetworkParamsAsMediationBase() {
        return true;
    }

    private boolean contains(Object[] array, Object v) {
        for (Object o : array) {
            if (o == v) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkConfig that = (NetworkConfig) o;
        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}