package com.ibm.airlock.common;


import com.ibm.airlock.common.cache.DefaultContext;
import com.ibm.airlock.common.cache.DefaultPersistenceHandler;
import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.cache.pref.FilePreferencesFactory;
import com.ibm.airlock.common.dependency.ProductDiModule;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.log.DefaultLog;
import com.ibm.airlock.common.dependency.DaggerProductDiComponent;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.BaseOkHttpClientBuilder;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.net.OkHttpConnectionManager;
import com.ibm.airlock.common.services.InfraAirlockService;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Base64;
import com.ibm.airlock.common.util.Base64Decoder;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.DefaultLocaleProvider;

import javax.xml.bind.DatatypeConverter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link AbstractAirlockProductManager} is used for tests and could be used as base class for
 * the custom {@link AirlockProductManager} implementation
 *
 * @author Denis Voloshin
 */
public class DefaultAirlockProductManager extends AbstractAirlockProductManager {

    private static final String TAG = "DefaultAirlockProductManager";
    private ConnectionManager connectionManager;

    static {
        Base64.init(new Base64Decoder() {
            @Override
            public byte[] decode(String str) {
                return DatatypeConverter.parseBase64Binary(str);
            }
        });
        Logger.setLogger(new DefaultLog());
        System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
        InMemoryCache.setDefaultExpirationPeriod(TimeUnit.MINUTES.toMillis(10)); // 10 minute
        InfraAirlockService.setFeaturesMapTimeToLive(TimeUnit.SECONDS.toMillis(10)); // 10 seconds
    }

    DefaultAirlockProductManager(String productName, String airlockDefaults, String encryptionKey, String appVersion) {
        super(productName, airlockDefaults, encryptionKey, appVersion);
        connectionManager = new OkHttpConnectionManager(new BaseOkHttpClientBuilder(), encryptionKey);
    }

    /**
     * Asynchronously downloads the current list of features from the server.
     *
     * @param callback Callback to be called when the function returns.
     */
    public void pullFeatures(final AirlockCallback callback) {

    }

    @Override
    public AirlockClient createClient(String clientId) throws AirlockInvalidFileException {

        context = productName != null ? new DefaultContext(clientId, FilePreferencesFactory
                .getAirlockCacheDirectory(), airlockDefaults, productName, encryptionKey, appVersion) : new DefaultContext(clientId, FilePreferencesFactory
                .getAirlockCacheDirectory(), airlockDefaults, encryptionKey, appVersion);

        productDiComponent = DaggerProductDiComponent.builder().productDiModule(
                new ProductDiModule(context, connectionManager, new DefaultPersistenceHandler(context), airlockDefaults, context.getAirlockProductName(), appVersion, encryptionKey)).build();
        productDiComponent.inject(this);

        streamsService.init(productDiComponent);
        infraAirlockService.init(productDiComponent);
        notificationService.init(productDiComponent);

        setLocale(persistenceHandler);
        initServices(productDiComponent);
        return new DefaultAirlockClient(this, clientId);
    }


    @Override
    protected void setLocale(PersistenceHandler persistenceHandler) {
        DefaultLocaleProvider defaultLocaleProvider = null;
        try {
            defaultLocaleProvider = new DefaultLocaleProvider(persistenceHandler.read(Constants.SP_CURRENT_LOCALE, Locale.getDefault().toString()));
        } catch (Exception e) {
            Logger.log.e(TAG, e.getMessage());
        }

        if (defaultLocaleProvider != null) {
            infraAirlockService.setLocaleProvider(defaultLocaleProvider);
        } else {
            infraAirlockService.setLocaleProvider(new DefaultLocaleProvider(Locale.getDefault().toString()));
        }
    }

    @Override
    public void reset(boolean simulateUninstall) {
        try {

            DefaultPersistenceHandler defaultPersistenceHandler = new DefaultPersistenceHandler(context);
            if (simulateUninstall) {
                defaultPersistenceHandler.reset(context);
            } else {
                defaultPersistenceHandler.clearInMemory();
            }

        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
        }
    }

    /**
     * Returns a new  instance of AirlockProductManagerBuilder object allows to build {@link AirlockProductManager}
     * later {@link AirlockProductManager} might be used to create an instance of {@link AirlockClient}.
     *
     * @return a new instance of AirlockProductManagerBuilder
     */
    public static AirlockProductManagerBuilder builder() {
        return new AirlockProductManagerBuilder();
    }


    /**
     * AirlockProductManagerBuilder object allows to build {@link AirlockProductManager}
     * later {@link AirlockProductManager} might be used to create an instance of {@link AirlockClient}.
     */
    public static class AirlockProductManagerBuilder {
        protected String airlockDefaults;
        protected String encryptionKey = "";
        protected String productName;
        protected String appVersion;
        @SuppressWarnings({"unused"})
        protected Long timeout;


        /**
         * Sets airlock defaults values as String on JSON representation
         *
         * @param pAirlockDefaults airlock defaults
         * @return This builder
         */
        public AirlockProductManagerBuilder withAirlockDefaults(String pAirlockDefaults) {
            airlockDefaults = pAirlockDefaults;
            return this;
        }

        /**
         * Sets airlock product encryption key
         * which used by communication layer to decode the remote encrypted configuration
         *
         * @param pEncryptionKey encryption key
         * @return This builder
         */
        @SuppressWarnings("unused")
        public AirlockProductManagerBuilder withSecretKey(String pEncryptionKey) {
            encryptionKey = pEncryptionKey;
            return this;
        }


        /**
         * Sets airlock product name, could be any value,
         * preferred value is the airlock product name defined in the airlock server
         *
         * @param pProductName airlock product name
         * @return This builder
         */
        public AirlockProductManagerBuilder withProductName(String pProductName) {
            productName = pProductName;
            return this;
        }

        /**
         * Sets current app version the airlock SDK will be used in.
         * The app version has to match to the airlock feature version range
         *
         * @param pAppVersion current app version
         * @return This builder
         */
        public AirlockProductManagerBuilder withAppVersion(String pAppVersion) {
            appVersion = pAppVersion;
            return this;
        }

        /**
         * Sets communication timeout  period the SDK will wait for the fetching remote config request.
         *
         * @param pTimeout time out period
         * @return This builder
         */
        @SuppressWarnings("unused")
        public AirlockProductManagerBuilder withConnectionTimeout(Long pTimeout) {
            timeout = pTimeout;
            return this;
        }

        /**
         * Returns an instance of {@link AirlockProductManager  } created from the fields set on this builder.
         *
         * @return A AirlockProductManager.
         */
        public AirlockProductManager build() {
            return new DefaultAirlockProductManager(productName, airlockDefaults, encryptionKey, appVersion);
        }
    }
}
