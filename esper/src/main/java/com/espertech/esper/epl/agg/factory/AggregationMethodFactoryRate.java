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
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprRateAggNode;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.schedule.TimeProvider;

public class AggregationMethodFactoryRate implements AggregationMethodFactory {
    protected final ExprRateAggNode parent;
    protected final boolean isEver;
    protected final long intervalTime;
    protected final TimeProvider timeProvider;
    protected final TimeAbacus timeAbacus;

    public AggregationMethodFactoryRate(ExprRateAggNode parent, boolean isEver, long intervalTime, TimeProvider timeProvider, TimeAbacus timeAbacus) {
        this.parent = parent;
        this.isEver = isEver;
        this.intervalTime = intervalTime;
        this.timeProvider = timeProvider;
        this.timeAbacus = timeAbacus;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return Double.class;
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
        if (isEver) {
            if (parent.getPositionalParams().length == 0) {
                return new AggregatorRateEver(intervalTime, timeAbacus.getOneSecond(), timeProvider);
            } else {
                return new AggregatorRateEverFilter(intervalTime, timeAbacus.getOneSecond(), timeProvider);
            }
        } else {
            if (parent.getOptionalFilter() != null) {
                return new AggregatorRateFilter(timeAbacus.getOneSecond());
            }
            return new AggregatorRate(timeAbacus.getOneSecond());
        }
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryRate that = (AggregationMethodFactoryRate) intoTableAgg;
        if (intervalTime != that.intervalTime) {
            throw new ExprValidationException("The size is " +
                    intervalTime +
                    " and provided is " +
                    that.intervalTime);
        }
        AggregationValidationUtil.validateAggregationUnbound(!isEver, !that.isEver);
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return null;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        if (isEver) {
            AggregatorRateEver.rowMemberCodegen(column, ctor, membersColumnized);
        } else {
            AggregatorRate.rowMemberCodegen(column, ctor, membersColumnized);
        }
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (isEver) {
            AggregatorRateEver.applyEnterCodegen(this, column, method, symbols, forges, classScope);
        } else {
            AggregatorRate.applyEnterCodegen(this, column, method, symbols, forges, classScope);
        }
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (isEver) {
            // no leave
        } else {
            AggregatorRate.applyLeaveCodegen(this, column, method, symbols, forges, classScope);
        }
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (isEver) {
            AggregatorRateEver.clearCodegen(column, method);
        } else {
            AggregatorRate.clearCodegen(column, method);
        }
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (isEver) {
            AggregatorRateEver.getValueCodegen(this, column, method);
        } else {
            AggregatorRate.getValueCodegen(this, column, method);
        }
    }

    public ExprRateAggNode getParent() {
        return parent;
    }

    public boolean isEver() {
        return isEver;
    }

    public long getIntervalTime() {
        return intervalTime;
    }

    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }
}