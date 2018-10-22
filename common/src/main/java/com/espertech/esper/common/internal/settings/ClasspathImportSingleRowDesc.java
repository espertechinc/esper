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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;

import java.io.Serializable;

/**
 * Provides information about a single-row function.
 */
public class ClasspathImportSingleRowDesc implements Serializable {
    private static final long serialVersionUID = -6024893655764123446L;
    private final String className;
    private final String methodName;
    private final ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache;
    private final ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable;
    private final boolean rethrowExceptions;
    private final String optionalEventTypeName;

    public ClasspathImportSingleRowDesc(String className, String methodName, ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions, String optionalEventTypeName) {
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

    public ConfigurationCompilerPlugInSingleRowFunction.ValueCache getValueCache() {
        return valueCache;
    }

    public ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable getFilterOptimizable() {
        return filterOptimizable;
    }

    public boolean isRethrowExceptions() {
        return rethrowExceptions;
    }

    public String getOptionalEventTypeName() {
        return optionalEventTypeName;
    }
}
