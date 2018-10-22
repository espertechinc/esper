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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionDeclarationContext;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionHandler;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionValidationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupportAggMFMultiRTForge implements AggregationMultiFunctionForge {
    private static Set<SupportAggMFMultiRTForge> forges = new HashSet<>();
    private static List<AggregationMultiFunctionDeclarationContext> functionDeclContexts = new ArrayList<>();
    private static List<AggregationMultiFunctionValidationContext> functionHandlerValidationContexts = new ArrayList<>();

    public SupportAggMFMultiRTForge() {
        forges.add(this);
    }

    public void addAggregationFunction(AggregationMultiFunctionDeclarationContext declarationContext) {
        forges.add(this);
        functionDeclContexts.add(declarationContext);
    }

    public AggregationMultiFunctionHandler validateGetHandler(AggregationMultiFunctionValidationContext validationContext) {
        functionHandlerValidationContexts.add(validationContext);
        return new SupportAggMFMultiRTHandler(validationContext);
    }

    public static void reset() {
        forges.clear();
        functionDeclContexts.clear();
        functionHandlerValidationContexts.clear();
    }

    public static Set<SupportAggMFMultiRTForge> getForges() {
        return forges;
    }

    public static List<AggregationMultiFunctionDeclarationContext> getFunctionDeclContexts() {
        return functionDeclContexts;
    }

    public static List<AggregationMultiFunctionValidationContext> getFunctionHandlerValidationContexts() {
        return functionHandlerValidationContexts;
    }

}
