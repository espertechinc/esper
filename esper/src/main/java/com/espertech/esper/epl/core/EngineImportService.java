/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.MathContext;
import java.util.TimeZone;

/**
 * Service for engine-level resolution of static methods and aggregation methods.
 */
public interface EngineImportService
{
    public final static String EXT_SINGLEROW_FUNCTION_TRANSPOSE = "transpose";

    /**
     * Returns the method invocation caches for the from-clause for a class.
     * @param className the class name providing the method
     * @return cache configs
     */
    public ConfigurationMethodRef getConfigurationMethodRef(String className);

    /**
     * Add an import, such as "com.mypackage.*" or "com.mypackage.MyClass".
     * @param importName is the import to add
     * @throws EngineImportException if the information or format is invalid
     */
    public void addImport(String importName) throws EngineImportException;

    /**
     * Add an aggregation function.
     * @param functionName is the name of the function to make known.
     * @param aggregationDesc is the descriptor for the aggregation function
     * @throws EngineImportException throw if format or information is invalid
     */
    public void addAggregation(String functionName, ConfigurationPlugInAggregationFunction aggregationDesc) throws EngineImportException;

    /**
     * Add an single-row function.
     * @param functionName is the name of the function to make known.
     * @param singleRowFuncClass is the class that provides the single row function
     * @param methodName is the name of the public static method provided by the class that provides the single row function
     * @param valueCache setting to control value cache behavior which may cache a result value when constant parameters are passed
     * @throws EngineImportException throw if format or information is invalid
     */
    public void addSingleRow(String functionName, String singleRowFuncClass, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions) throws EngineImportException;

    /**
     * Used at statement compile-time to try and resolve a given function name into an
     * aggregation method. Matches function name case-neutral.
     * @param functionName is the function name
     * @return aggregation provider
     * @throws EngineImportUndefinedException if the function is not a configured aggregation function
     * @throws EngineImportException if the aggregation providing class could not be loaded or doesn't match
     */
    public AggregationFunctionFactory resolveAggregationFactory(String functionName) throws EngineImportUndefinedException, EngineImportException;

    public ConfigurationPlugInAggregationMultiFunction resolveAggregationMultiFunction(String name);

        /**
        * Used at statement compile-time to try and resolve a given function name into an
        * single-row function. Matches function name case-neutral.
        * @param functionName is the function name
        * @return class name and method name pair
        * @throws EngineImportUndefinedException if the function is not a configured single-row function
        * @throws EngineImportException if the function providing class could not be loaded or doesn't match
        */
    public Pair<Class, EngineImportSingleRowDesc> resolveSingleRow(String functionName) throws EngineImportUndefinedException, EngineImportException;

    /**
     * Resolves a given class, method and list of parameter types to a static method.
     * @param className is the class name to use
     * @param methodName is the method name
     * @param paramTypes is parameter types match expression sub-nodes
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method
     */
    public Method resolveMethod(String className, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws EngineImportException;

    /**
     * Resolves a constructor matching list of parameter types.
     * @param clazz is the class to use
     * @param paramTypes is parameter types match expression sub-nodes
     * @return method this resolves to
     * @throws EngineImportException if the ctor cannot be resolved
     */
    public Constructor resolveCtor(Class clazz, Class[] paramTypes) throws EngineImportException;

    /**
     * Resolves a given class name, either fully qualified and simple and imported to a class.
     * @param className is the class name to use
     * @return class this resolves to
     * @throws EngineImportException if there was an error resolving the class
     */
    public Class resolveClass(String className) throws EngineImportException;

    /**
     * Resolves a given class name, either fully qualified and simple and imported to a annotation.
     * @param className is the class name to use
     * @return annotation class this resolves to
     * @throws EngineImportException if there was an error resolving the class
     */
    public Class resolveAnnotation(String className) throws EngineImportException;

    /**
     * Resolves a given class and method name to a static method, expecting the method to exist
     * exactly once and not be overloaded, with any parameters.
     * @param className is the class name to use
     * @param methodName is the method name
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method, or
     * if the method is overloaded
     */
    public Method resolveMethod(String className, String methodName) throws EngineImportException;

    /**
     * Resolves a given class and method name to a non-static method, expecting the method to exist
     * exactly once and not be overloaded, with any parameters.
     * @param clazz is the class
     * @param methodName is the method name
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static method, or
     * if the method is overloaded
     */
    public Method resolveNonStaticMethod(Class clazz, String methodName) throws EngineImportException;

    /**
     * Resolves a given method name and list of parameter types to an instance or static method exposed by the given class.
     * @param clazz is the class to look for a fitting method
     * @param methodName is the method name
     * @param paramTypes is parameter types match expression sub-nodes
     * @param allowEventBeanType whether EventBean footprint is allowed
     * @return method this resolves to
     * @throws EngineImportException if the method cannot be resolved to a visible static or instance method
     */
    public Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws EngineImportException;

    /**
     * Resolve an extended (non-SQL std) builtin aggregation.
     * @param name of func
     * @param isDistinct indicator
     * @return aggregation func node
     */
    public ExprNode resolveAggExtendedBuiltin(String name, boolean isDistinct);

    /**
     * Resolve an extended (non-SQL std) single-row function.
     * @param name of func
     * @return node or null
     */
    public ExprNode resolveSingleRowExtendedBuiltin(String name);

    public boolean isDuckType();

    public boolean isUdfCache();

    public boolean isSortUsingCollator();

    void addAggregationMultiFunction(ConfigurationPlugInAggregationMultiFunction desc) throws EngineImportException;

    public MathContext getDefaultMathContext();

    public TimeZone getTimeZone();

    public ConfigurationEngineDefaults.ThreadingProfile getThreadingProfile();
}
