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
package com.espertech.esper.epl.core.engineimport;

import com.espertech.esper.client.ConfigurationPlugInSingleRowFunction;

import java.io.Serializable;

/**
 * Provides information about a single-row function.
 */
public class EngineImportSingleRowDesc implements Serializable {
    private static final long serialVersionUID = -6024893655764123446L;
    private final String className;
    private final String methodName;
    private final ConfigurationPlugInSingleRowFunction.ValueCache valueCache;
    private final ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable;
    private final boolean rethrowExceptions;
    private final String optionalEventTypeName;

    public EngineImportSingleRowDesc(String className, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions, String optionalEventTypeName) {
        this.className = className;
        this.methodName = methodName;
        this.valueCache = valueCache;
        this.filterOptimizable = filterOptimizable;
        this.rethrowExceptions = rethrowExceptions;
        this.optionalEventTypeName = optionalEventTypeName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public ConfigurationPlugInSingleRowFunction.ValueCache getValueCache() {
        return valueCache;
    }

    public ConfigurationPlugInSingleRowFunction.FilterOptimizable getFilterOptimizable() {
        return filterOptimizable;
    }

    public boolean isRethrowExceptions() {
        return rethrowExceptions;
    }

    public String getOptionalEventTypeName() {
        return optionalEventTypeName;
    }
}
