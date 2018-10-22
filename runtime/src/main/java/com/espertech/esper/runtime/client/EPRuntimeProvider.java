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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeImpl;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for instances of {@link EPRuntime}.
 */
public final class EPRuntimeProvider {
    /**
     * For the default runtime instance the URI value is "default".
     */
    public static final String DEFAULT_RUNTIME_URI = "default";

    private static Map<String, EPRuntimeSPI> runtimes = new ConcurrentHashMap<>();

    /**
     * Returns the runtime for the default URI. The URI value for the runtime returned is "default".
     *
     * @return default runtime
     */
    public static EPRuntime getDefaultRuntime() {
        return getRuntime(EPRuntimeProvider.DEFAULT_RUNTIME_URI, new Configuration());
    }

    /**
     * Returns the default runtime. The URI value for the runtime returned is "default".
     *
     * @param configuration is the configuration for the runtime
     * @return default instance of the runtime.
     * @throws ConfigurationException to indicate a configuration problem
     */
    public static EPRuntime getDefaultRuntime(Configuration configuration) throws ConfigurationException {
        return getRuntime(EPRuntimeProvider.DEFAULT_RUNTIME_URI, configuration);
    }

    /**
     * Returns a runtime for a given runtime URI.
     * <p>
     * Use the URI of "default" or null to return the default runtime.
     *
     * @param uri - the URI
     * @return runtime for the given URI.
     */
    public static EPRuntime getRuntime(String uri) {
        return getRuntime(uri, new Configuration());
    }

    /**
     * Returns a runtime for a given URI.
     * Use the URI of "default" or null to return the default runtime.
     *
     * @param uri           - the runtime URI. If null provided it assumes "default".
     * @param configuration is the configuration for the runtime
     * @return Runtime for the given URI.
     * @throws ConfigurationException to indicate a configuration problem
     */
    public static EPRuntime getRuntime(String uri, Configuration configuration) throws ConfigurationException {
        String runtimeURINonNull = (uri == null) ? EPRuntimeProvider.DEFAULT_RUNTIME_URI : uri;

        if (runtimes.containsKey(runtimeURINonNull)) {
            EPRuntimeSPI runtime = runtimes.get(runtimeURINonNull);
            if (runtime.isDestroyed()) {
                runtime = getRuntimeInternal(configuration, runtimeURINonNull);
                runtimes.put(runtimeURINonNull, runtime);
            } else {
                runtime.setConfiguration(configuration);
            }
            return runtime;
        }

        // New runtime
        EPRuntimeSPI runtime = getRuntimeInternal(configuration, runtimeURINonNull);
        runtimes.put(runtimeURINonNull, runtime);
        runtime.postInitialize();

        return runtime;
    }

    /**
     * Returns an existing runtime. Returns null if the runtime for the given URI has not been initialized
     * or the runtime for the given URI is in destroyed state.
     *
     * @param uri - the URI. If null provided it assumes "default".
     * @return Runtime for the given URI.
     */
    public static EPRuntime getExistingRuntime(String uri) {
        String runtimeURINonNull = (uri == null) ? EPRuntimeProvider.DEFAULT_RUNTIME_URI : uri;
        EPRuntimeSPI runtime = runtimes.get(runtimeURINonNull);
        if (runtime == null || runtime.isDestroyed()) {
            return null;
        }
        return runtime;
    }

    /**
     * Returns a list of known URIs.
     * <p>
     * Returns a the value "default" for the default runtime.
     *
     * @return array of URI strings
     */
    public static String[] getRuntimeURIs() {
        Set<String> uriSet = runtimes.keySet();
        return uriSet.toArray(new String[uriSet.size()]);
    }

    /**
     * Returns an indicator whether a runtime for the given URI is allocated (true) or is not allocated (false)
     *
     * @param uri runtime uri
     * @return indicator
     */
    public static boolean hasRuntime(String uri) {
        return runtimes.containsKey(uri);
    }

    private static EPRuntimeSPI getRuntimeInternal(Configuration configuration, String runtimeURINonNull) {
        return new EPRuntimeImpl(configuration, runtimeURINonNull, runtimes);
    }
}