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
package com.espertech.esper.common.client.hook.singlerowfunc;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for use in EPL statements to add a debug.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionSingleRowFunction {
    String name();
    String methodName();
    ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache() default ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED;
    ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable() default ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED;
    boolean rethrowExceptions() default false;
    String eventTypeName() default "";
}
