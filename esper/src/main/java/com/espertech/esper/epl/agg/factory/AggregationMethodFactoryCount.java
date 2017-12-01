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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.*;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationValidationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.methodagg.ExprCountNode;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class AggregationMethodFactoryCount implements AggregationMethodFactory {
    protected final ExprCountNode parent;
    protected final boolean ignoreNulls;
    protected final Class countedValueType;

    public AggregationMethodFactoryCount(ExprCountNode parent, boolean ignoreNulls, Class countedValueType) {
        this.parent = parent;
        this.ignoreNulls = ignoreNulls;
        this.countedValueType = countedValueType;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return Long.class;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationAccessorForge getAccessorForge() {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationMethod make() {
        AggregationMethod method = makeCountAggregator(ignoreNulls, parent.isHasFilter());
        if (!parent.isDistinct()) {
            return method;
        }
        return AggregationMethodFactoryUtil.makeDistinctAggregator(method, parent.isHasFilter());
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryCount that = (AggregationMethodFactoryCount) intoTableAgg;
        AggregationValidationUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
        if (parent.isDistinct()) {
            AggregationValidationUtil.validateAggregationInputType(countedValueType, that.countedValueType);
        }
        if (ignoreNulls != that.ignoreNulls) {
            throw new ExprValidationException("The aggregation declares" +
                    (ignoreNulls ? "" : " no") +
                    " ignore nulls and provided is" +
                    (that.ignoreNulls ? "" : " no") +
                    " ignore nulls");
        }
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return null;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return getMethodAggregationEvaluatorCountByForge(parent.getPositionalParams(), join, typesPerStream);
    }

    private static ExprForge[] getMethodAggregationEvaluatorCountByForge(ExprNode[] childNodes, boolean join, EventType[] typesPerStream)
            throws ExprValidationException {
        if (childNodes[0] instanceof ExprWildcard && childNodes.length == 2) {
            return ExprMethodAggUtil.getDefaultForges(new ExprNode[]{childNodes[1]}, join, typesPerStream);
        }
        if (childNodes[0] instanceof ExprWildcard && childNodes.length == 1) {
            return ExprNodeUtilityCore.EMPTY_FORGE_ARRAY;
        }
        return ExprMethodAggUtil.getDefaultForges(childNodes, join, typesPerStream);
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        AggregatorCount.rowMemberCodegen(parent.isDistinct(), column, ctor, membersColumnized);
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        AggregatorCount.applyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, forges, symbols, classScope);
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        AggregatorCount.applyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, forges, symbols, classScope);
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        AggregatorCount.clearCodegen(parent.isDistinct(), column, method);
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        AggregatorCount.getValueCodegen(column, method);
    }

    private AggregationMethod makeCountAggregator(boolean isIgnoreNull, boolean hasFilter) {
        if (!hasFilter) {
            if (isIgnoreNull) {
                return new AggregatorCountNonNull();
            }
            return new AggregatorCount();
        } else {
            if (isIgnoreNull) {
                return new AggregatorCountNonNullFilter();
            }
            return new AggregatorCountFilter();
        }
    }
}