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
package com.espertech.esper.common.internal.epl.agg.access.plugin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationForgeFactoryAccessBase;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprPlugInMultiFunctionAggNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationForgeFactoryAccessPlugin extends AggregationForgeFactoryAccessBase {

    private final ExprPlugInMultiFunctionAggNode parent;
    private final AggregationMultiFunctionHandler handler;
    private EPType returnType;

    public AggregationForgeFactoryAccessPlugin(ExprPlugInMultiFunctionAggNode parent, AggregationMultiFunctionHandler handler) {
        this.parent = parent;
        this.handler = handler;
    }

    public AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return handler.getAggregationStateUniqueKey();
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        AggregationMultiFunctionStateMode stateMode = handler.getStateMode();
        if (stateMode instanceof AggregationMultiFunctionStateModeManaged) {
            AggregationMultiFunctionStateModeManaged managed = (AggregationMultiFunctionStateModeManaged) stateMode;
            return new AggregationStateFactoryForgePlugin(this, managed);
        } else {
            throw new IllegalStateException("Unrecognized state mode " + stateMode);
        }
    }

    public AggregationAccessorForge getAccessorForge() {
        AggregationMultiFunctionAccessorMode accessorMode = handler.getAccessorMode();
        if (accessorMode instanceof AggregationMultiFunctionAccessorModeManaged) {
            AggregationMultiFunctionAccessorModeManaged managed = (AggregationMultiFunctionAccessorModeManaged) accessorMode;
            return new AggregationAccessorForgePlugin(this, managed);
        } else {
            throw new IllegalStateException("Unrecognized accessor mode " + accessorMode);
        }
    }

    public AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        AggregationMultiFunctionAgentMode agentMode = handler.getAgentMode();
        if (agentMode instanceof AggregationMultiFunctionAgentModeManaged) {
            AggregationMultiFunctionAgentModeManaged managed = (AggregationMultiFunctionAgentModeManaged) agentMode;
            return new AggregationAgentForgePlugin(this, managed, parent.getOptionalFilter() == null ? null : parent.getOptionalFilter().getForge());
        } else {
            throw new IllegalStateException("Unrecognized accessor mode " + agentMode);
        }
    }

    public Class getResultType() {
        obtainReturnType();
        return EPTypeHelper.getNormalizedClass(returnType);
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        AggregationPortableValidationPluginMultiFunc portable = new AggregationPortableValidationPluginMultiFunc();
        portable.setHandler(handler);
        portable.setAggregationFunctionName(parent.getAggregationFunctionName());
        return portable;
    }

    public EventType getEventTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeMultiValued(returnType);
    }

    public EventType getEventTypeSingle() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeSingleValued(returnType);
    }

    public Class getComponentTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getClassMultiValued(returnType);
    }

    private void obtainReturnType() {
        if (returnType == null) {
            returnType = handler.getReturnType();
        }
    }
}
