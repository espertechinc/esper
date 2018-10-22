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
package com.espertech.esper.common.internal.compile.stage1.specmapper;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionDeclarationContext;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprPlugInMultiFunctionAggNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprPlugInAggNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.settings.ClasspathImportUndefinedException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.LazyAllocatedMap;
import com.espertech.esper.common.internal.util.ValidationException;

import java.util.Locale;

public class ASTAggregationHelper {
    public static ExprNode tryResolveAsAggregation(ClasspathImportServiceCompileTime classpathImportService,
                                                   boolean distinct,
                                                   String functionName,
                                                   LazyAllocatedMap<ConfigurationCompilerPlugInAggregationMultiFunction, AggregationMultiFunctionForge> plugInAggregations) {
        try {
            AggregationFunctionForge aggregationFactory = classpathImportService.resolveAggregationFunction(functionName);
            return new ExprPlugInAggNode(distinct, aggregationFactory, functionName);
        } catch (ClasspathImportUndefinedException e) {
            // Not an aggregation function
        } catch (ClasspathImportException e) {
            throw new ValidationException("Error resolving aggregation: " + e.getMessage(), e);
        }

        // try plug-in aggregation multi-function
        ConfigurationCompilerPlugInAggregationMultiFunction config = classpathImportService.resolveAggregationMultiFunction(functionName);
        if (config != null) {
            AggregationMultiFunctionForge factory = plugInAggregations.getMap().get(config);
            if (factory == null) {
                factory = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, config.getMultiFunctionForgeClassName(), classpathImportService.getClassForNameProvider());
                plugInAggregations.getMap().put(config, factory);
            }
            factory.addAggregationFunction(new AggregationMultiFunctionDeclarationContext(functionName.toLowerCase(Locale.ENGLISH), distinct, config));
            return new ExprPlugInMultiFunctionAggNode(distinct, config, factory, functionName);
        }

        // try built-in expanded set of aggregation functions
        return classpathImportService.resolveAggExtendedBuiltin(functionName, distinct);
    }
}
