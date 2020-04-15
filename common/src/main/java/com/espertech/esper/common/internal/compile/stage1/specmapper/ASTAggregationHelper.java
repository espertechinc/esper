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
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedClasspathExtension;
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
                                                   LazyAllocatedMap<HashableMultiKey, AggregationMultiFunctionForge> plugInAggregations,
                                                   ClassProvidedClasspathExtension classProvidedClasspathExtension) {
        try {
            AggregationFunctionForge aggregationFactory = classpathImportService.resolveAggregationFunction(functionName, classProvidedClasspathExtension);
            return new ExprPlugInAggNode(distinct, aggregationFactory, functionName);
        } catch (ClasspathImportUndefinedException e) {
            // Not an aggregation function
        } catch (ClasspathImportException e) {
            throw new ValidationException("Error resolving aggregation: " + e.getMessage(), e);
        }

        // try plug-in aggregation multi-function
        Pair<ConfigurationCompilerPlugInAggregationMultiFunction, Class> configPair = classpathImportService.resolveAggregationMultiFunction(functionName, classProvidedClasspathExtension);
        if (configPair != null) {
            HashableMultiKey multiKey = new HashableMultiKey(configPair.getFirst().getFunctionNames());
            AggregationMultiFunctionForge factory = plugInAggregations.getMap().get(multiKey);
            if (factory == null) {
                if (configPair.getSecond() != null) {
                    factory = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, configPair.getSecond());
                } else {
                    factory = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, configPair.getFirst().getMultiFunctionForgeClassName(), classpathImportService.getClassForNameProvider());
                }
                plugInAggregations.getMap().put(multiKey, factory);
            }
            factory.addAggregationFunction(new AggregationMultiFunctionDeclarationContext(functionName.toLowerCase(Locale.ENGLISH), distinct, configPair.getFirst()));
            return new ExprPlugInMultiFunctionAggNode(distinct, configPair.getFirst(), factory, functionName);
        }

        // try built-in expanded set of aggregation functions
        return classpathImportService.resolveAggExtendedBuiltin(functionName, distinct);
    }
}
