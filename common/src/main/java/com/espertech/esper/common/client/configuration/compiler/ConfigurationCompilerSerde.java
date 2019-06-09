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
package com.espertech.esper.common.client.configuration.compiler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serialization and Deserialization options.
 */
public class ConfigurationCompilerSerde implements Serializable {
    private static final long serialVersionUID = -1742511809811951823L;

    private boolean enableExtendedBuiltin = true;
    private boolean enableSerializable = false;
    private boolean enableExternalizable = false;
    private boolean enableSerializationFallback = false;
    private List<String> serdeProviderFactories = new ArrayList<>(2);

    /**
     * Returns indicator whether the runtime provides the serde for extended builtin classes (see doc).
     *
     * @return indicator
     */
    public boolean isEnableExtendedBuiltin() {
        return enableExtendedBuiltin;
    }

    /**
     * Sets indicator whether the runtime provides the serde for extended builtin classes (see doc).
     *
     * @param enableExtendedBuiltin indicator
     */
    public void setEnableExtendedBuiltin(boolean enableExtendedBuiltin) {
        this.enableExtendedBuiltin = enableExtendedBuiltin;
    }

    /**
     * Returns indicator whether the runtime considers the {@link java.io.Serializable} interface for serializing types that implement Serializable
     *
     * @return indicator
     */
    public boolean isEnableSerializable() {
        return enableSerializable;
    }

    /**
     * Sets indicator whether the runtime considers the {@link java.io.Serializable} interface for serializing types that implement Serializable
     *
     * @param enableSerializable indicator
     */
    public void setEnableSerializable(boolean enableSerializable) {
        this.enableSerializable = enableSerializable;
    }

    /**
     * Returns indicator whether the runtime considers the {@link java.io.Externalizable} interface for serializing types that implement Externalizable.
     *
     * @return indicator
     */
    public boolean isEnableExternalizable() {
        return enableExternalizable;
    }

    /**
     * Sets indicator whether the runtime considers the {@link java.io.Externalizable} interface for serializing types that implement Externalizable.
     *
     * @param enableExternalizable indicator to set
     */
    public void setEnableExternalizable(boolean enableExternalizable) {
        this.enableExternalizable = enableExternalizable;
    }

    /**
     * Returns currently-registered serde provider factories.
     * Each entry is the fully-qualified class name of the serde provider factory.
     *
     * @return serde provider factory class names
     */
    public List<String> getSerdeProviderFactories() {
        return serdeProviderFactories;
    }

    /**
     * Add a serde provider factory. Provide the fully-qualified class name of the serde provider factory.
     *
     * @param className serde provider factory class name
     */
    public void addSerdeProviderFactory(String className) {
        serdeProviderFactories.add(className);
    }

    /**
     * Sets the currently-registered serde provider factories.
     * Each entry is the fully-qualified class name of the serde provider factory.
     *
     * @param serdeProviderFactories class names
     */
    public void setSerdeProviderFactories(List<String> serdeProviderFactories) {
        this.serdeProviderFactories = serdeProviderFactories;
    }

    /**
     * Returns indicator whether the runtime, for types for which no other serde is available,
     * falls back to using JVM serialization. Fallback does not check whether the type actually implements Serializable.
     *
     * @return indicator
     */
    public boolean isEnableSerializationFallback() {
        return enableSerializationFallback;
    }

    /**
     * Sets indicator whether the runtime, for types for which no other serde is available,
     * falls back to using JVM serialization. Fallback does not check whether the type actually implements Serializable.
     *
     * @param enableSerializationFallback indicator
     */
    public void setEnableSerializationFallback(boolean enableSerializationFallback) {
        this.enableSerializationFallback = enableSerializationFallback;
    }
}

