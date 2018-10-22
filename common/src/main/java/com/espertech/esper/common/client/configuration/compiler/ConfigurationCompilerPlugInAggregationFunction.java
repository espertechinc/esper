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

/**
 * Configuration information for plugging in a custom aggregation function.
 */
public class ConfigurationCompilerPlugInAggregationFunction implements Serializable {
    private String name;
    private String forgeClassName;

    private static final long serialVersionUID = 4096734947283212246L;

    /**
     * Ctor.
     */
    public ConfigurationCompilerPlugInAggregationFunction() {
    }

    /**
     * Ctor.
     *
     * @param name           of the aggregation function
     * @param forgeClassName the name of the aggregation function factory class
     */
    public ConfigurationCompilerPlugInAggregationFunction(String name, String forgeClassName) {
        this.name = name;
        this.forgeClassName = forgeClassName;
    }

    /**
     * Returns the aggregation function name.
     *
     * @return aggregation function name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the aggregation function name.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the class name of the aggregation function factory class.
     *
     * @return class name
     */
    public String getForgeClassName() {
        return forgeClassName;
    }

    /**
     * Sets the class name of the aggregation function factory class.
     *
     * @param forgeClassName class name to set
     */
    public void setForgeClassName(String forgeClassName) {
        this.forgeClassName = forgeClassName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigurationCompilerPlugInAggregationFunction that = (ConfigurationCompilerPlugInAggregationFunction) o;

        if (forgeClassName != null ? !forgeClassName.equals(that.forgeClassName) : that.forgeClassName != null)
            return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (forgeClassName != null ? forgeClassName.hashCode() : 0);
        return result;
    }
}
