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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.plugin.PlugInAggregationMultiFunctionDeclarationContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionValidationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupportAggMFFactory implements PlugInAggregationMultiFunctionFactory {

    private static Set<SupportAggMFFactory> factories = new HashSet<SupportAggMFFactory>();
    private static List<PlugInAggregationMultiFunctionDeclarationContext> functionDeclContexts = new ArrayList<PlugInAggregationMultiFunctionDeclarationContext>();
    private static List<PlugInAggregationMultiFunctionValidationContext> functionHandlerValidationContexts = new ArrayList<PlugInAggregationMultiFunctionValidationContext>();

    public static void reset() {
        factories.clear();
        functionDeclContexts.clear();
        functionHandlerValidationContexts.clear();
    }

    public static Set<SupportAggMFFactory> getFactories() {
        return factories;
    }

    public static List<PlugInAggregationMultiFunctionDeclarationContext> getFunctionDeclContexts() {
        return functionDeclContexts;
    }

    public static List<PlugInAggregationMultiFunctionValidationContext> getFunctionHandlerValidationContexts() {
        return functionHandlerValidationContexts;
    }

    public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext) {
        factories.add(this);
        functionDeclContexts.add(declarationContext);
    }

    public SupportAggMFFactory() {
        factories.add(this);
    }

    public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
        functionHandlerValidationContexts.add(validationContext);
        return new SupportAggMFHandler(validationContext);
    }
}
