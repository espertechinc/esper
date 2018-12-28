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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationForgeFactoryAccessBase;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationForgeFactoryAccessSorted extends AggregationForgeFactoryAccessBase {
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;
    private final AggregationAccessorForge accessor;
    private final Class accessorResultType;
    private final EventType containedEventType;

    private final AggregationMultiFunctionStateKey optionalStateKey;
    private final SortedAggregationStateDesc optionalSortedStateDesc;
    private final AggregationAgentForge optionalAgent;

    public AggregationForgeFactoryAccessSorted(ExprAggMultiFunctionSortedMinMaxByNode parent, AggregationAccessorForge accessor, Class accessorResultType, EventType containedEventType, AggregationMultiFunctionStateKey optionalStateKey, SortedAggregationStateDesc optionalSortedStateDesc, AggregationAgentForge optionalAgent) {
        this.parent = parent;
        this.accessor = accessor;
        this.accessorResultType = accessorResultType;
        this.containedEventType = containedEventType;
        this.optionalStateKey = optionalStateKey;
        this.optionalSortedStateDesc = optionalSortedStateDesc;
        this.optionalAgent = optionalAgent;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        throw new UnsupportedOperationException("Not supported");
    }

    public Class getResultType() {
        return accessorResultType;
    }

    public AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return optionalStateKey;
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        if (isMatchRecognize || optionalSortedStateDesc == null) {
            return null;
        }
        if (optionalSortedStateDesc.isEver()) {
            return new AggregationStateMinMaxByEverForge(this);
        }
        return new AggregationStateSortedForge(this);
    }

    public AggregationAccessorForge getAccessorForge() {
        return accessor;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationSorted(parent.getAggregationFunctionName(), containedEventType, optionalSortedStateDesc == null ? null : optionalSortedStateDesc.getCriteriaTypes());
    }

    public AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        return optionalAgent;
    }

    public EventType getContainedEventType() {
        return containedEventType;
    }

    public ExprAggMultiFunctionSortedMinMaxByNode getParent() {
        return parent;
    }

    public SortedAggregationStateDesc getOptionalSortedStateDesc() {
        return optionalSortedStateDesc;
    }
}
