/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.client;

import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.util.EPServiceProviderName;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for instances of {@link EPServiceProvider}.
 */
public final class EPServiceProviderManager {
    private static Map<String, EPServiceProviderSPI> runtimes = new ConcurrentHashMap<String, EPServiceProviderSPI>();

    /**
     * Returns the default EPServiceProvider. The URI value for the service returned is "default".
     *
     * @return default instance of the service.
     */
    public static EPServiceProvider getDefaultProvider() {
        return getProvider(EPServiceProviderName.DEFAULT_ENGINE_URI, new Configuration());
    }

    /**
     * Returns the default EPServiceProvider. The URI value for the service returned is "default".
     *
     * @param configuration is the configuration for the service
     * @return default instance of the service.
     * @throws ConfigurationException to indicate a configuration problem
     */
    public static EPServiceProvider getDefaultProvider(Configuration configuration) throws ConfigurationException {
        return getProvider(EPServiceProviderName.DEFAULT_ENGINE_URI, configuration);
    }

    /**
     * Returns an EPServiceProvider for a given provider URI.
     * <p>
     * Use the URI of "default" or null to return the default service provider.
     *
     * @param providerURI - the provider URI
     * @return EPServiceProvider for the given provider URI.
     */
    public static EPServiceProvider getProvider(String providerURI) {
        return getProvider(providerURI, new Configuration());
    }

    /**
     * Returns an EPServiceProvider for a given provider URI.
     * Use the URI of "default" or null to return the default service provider.
     *
     * @param providerURI   - the provider URI. If null provided it assumes "default".
     * @param configuration is the configuration for the service
     * @return EPServiceProvider for the given provider URI.
     * @throws ConfigurationException to indicate a configuration problem
     */
    public static EPServiceProvider getProvider(String providerURI, Configuration configuration) throws ConfigurationException {
        String providerURINonNull = (providerURI == null) ? EPServiceProviderName.DEFAULT_ENGINE_URI : providerURI;

        if (runtimes.containsKey(providerURINonNull)) {
            EPServiceProviderSPI provider = runtimes.get(providerURINonNull);
            if (provider.isDestroyed()) {
                provider = getProviderInternal(configuration, providerURINonNull);
                runtimes.put(providerURINonNull, provider);
            } else {
                provider.setConfiguration(configuration);
            }
            return provider;
        }

        // New runtime
        EPServiceProviderSPI runtime = getProviderInternal(configuration, providerURINonNull);
        runtimes.put(providerURINonNull, runtime);
        runtime.postInitialize();

        return runtime;
    }

    /**
     * Returns an existing provider. Returns null if the provider for the given URI has not been initialized
     * or the provider for the given URI is in destroyed state.
     *
     * @param providerURI - the provider URI. If null provided it assumes "default".
     * @return EPServiceProvider for the given provider URI.
     */
    public static EPServiceProvider getExistingProvider(String providerURI) {
        String providerURINonNull = (providerURI == null) ? EPServiceProviderName.DEFAULT_ENGINE_URI : providerURI;
        EPServiceProviderSPI provider = runtimes.get(providerURINonNull);
        if (provider == null || provider.isDestroyed()) {
            return null;
        }
        return provider;
    }

    /**
     * Returns a list of known provider URIs.
     * <p>
     * Returns a the value "default" for the default provider.
     * <p>
     * Returns URIs for all engine instances including destroyed instances.
     *
     * @return array of URI strings
     */
    public static String[] getProviderURIs() {
        Set<String> uriSet = runtimes.keySet();
        return uriSet.toArray(new String[uriSet.size()]);
    }

    private static EPServiceProviderSPI getProviderInternal(Configuration configuration, String providerURINonNull) {
        return new EPServiceProviderImpl(configuration, providerURINonNull, runtimes);
    }
}
