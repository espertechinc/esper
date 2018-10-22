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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

/**
 * Factory for aggregation methods.
 */
public interface AggregationForgeFactory {
    boolean isAccessAggregation();

    void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope);

    AggregatorMethod getAggregator();

    Class getResultType();

    AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize);

    AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize);

    AggregationAccessorForge getAccessorForge();

    ExprAggregateNodeBase getAggregationExpression();

    AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName);

    ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException;

    AggregationPortableValidation getAggregationPortableValidation();
}