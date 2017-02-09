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

import java.io.Serializable;

/**
 * Configuration information for plugging in a custom aggregation function.
 */
public class ConfigurationPlugInAggregationFunction implements Serializable {
    private String name;
    private String factoryClassName;

    private static final long serialVersionUID = 4096734947283212246L;

    /**
     * Ctor.
     */
    public ConfigurationPlugInAggregationFunction() {
    }

    /**
     * Ctor.
     *
     * @param name             of the aggregation function
     * @param factoryClassName the name of the aggregation function factory class
     */
    public ConfigurationPlugInAggregationFunction(String name, String factoryClassName) {
        this.name = name;
        this.factoryClassName = factoryClassName;
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
    public String getFactoryClassName() {
        return factoryClassName;
    }

    /**
     * Sets the class name of the aggregation function factory class.
     *
     * @param factoryClassName class name to set
     */
    public void setFactoryClassName(String factoryClassName) {
        this.factoryClassName = factoryClassName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigurationPlugInAggregationFunction that = (ConfigurationPlugInAggregationFunction) o;

        if (factoryClassName != null ? !factoryClassName.equals(that.factoryClassName) : that.factoryClassName != null)
            return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (factoryClassName != null ? factoryClassName.hashCode() : 0);
        return result;
    }
}
