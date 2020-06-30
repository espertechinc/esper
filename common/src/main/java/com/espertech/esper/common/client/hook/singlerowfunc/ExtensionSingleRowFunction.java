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
 * Annotation for use in EPL statements with inline classes for providing a plug-in single-row function.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionSingleRowFunction {
    /**
     * The function name
     * @return function name
     */
    String name();

    /**
     * The method name of the provider
     * @return method name
     */
    String methodName();

    /**
     * Value cache setting.
     * @return setting
     */
    ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache() default ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED;

    /**
     * Filter optimization setting
     * @return setting
     */
    ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable() default ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED;

    /**
     * Rethrow-exception setting
     * @return setting
     */
    boolean rethrowExceptions() default false;

    /**
     * Event type name of return value or empty string when not returning an event type name.
     * @return event type name
     */
    String eventTypeName() default "";
}
