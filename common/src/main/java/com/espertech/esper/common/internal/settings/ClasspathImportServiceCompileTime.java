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

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.AdvancedIndexFactoryProvider;

import java.lang.reflect.Method;
import java.math.MathContext;

public interface ClasspathImportServiceCompileTime extends ClasspathImportService {
    String EXT_SINGLEROW_FUNCTION_TRANSPOSE = "transpose";

    AdvancedIndexFactoryProvider resolveAdvancedIndexProvider(String indexTypeName) throws ClasspathImportException;
    Method resolveMethodOverloadChecked(Class clazz, String methodName) throws ClasspathImportException;
    Method resolveMethodOverloadChecked(String className, String methodName) throws ClasspathImportException;
    Class resolveAnnotation(String className) throws ClasspathImportException;
    Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) throws ClasspathImportException, ClasspathImportUndefinedException;
    Method resolveNonStaticMethodOverloadChecked(Class clazz, String methodName) throws ClasspathImportException;
    Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType) throws ClasspathImportException;
    Class resolveEnumMethod(String name) throws ClasspathImportException;
    AggregationFunctionForge resolveAggregationFunction(String functionName) throws ClasspathImportUndefinedException, ClasspathImportException;
    ConfigurationCompilerPlugInAggregationMultiFunction resolveAggregationMultiFunction(String name);
    ExprNode resolveAggExtendedBuiltin(String name, boolean isDistinct);
    Class resolveDateTimeMethod(String name) throws ClasspathImportException;
    ExprNode resolveSingleRowExtendedBuiltin(String name);

    boolean isSortUsingCollator();
    MathContext getDefaultMathContext();

    void addImport(String importName) throws ClasspathImportException;
}
