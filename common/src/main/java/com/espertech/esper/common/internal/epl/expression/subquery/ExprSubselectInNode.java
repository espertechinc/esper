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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Collection;
import java.util.LinkedHashMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Represents a subselect in an expression tree.
 */
public class ExprSubselectInNode extends ExprSubselectNode {
    private final boolean isNotIn;
    private SubselectForgeNR evalStrategy;

    public ExprSubselectInNode(StatementSpecRaw statementSpec, boolean isNotIn) {
        super(statementSpec);
        this.isNotIn = isNotIn;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    /**
     * Returns true for not-in, or false for in.
     *
     * @return true for not-in
     */
    public boolean isNotIn() {
        return isNotIn;
    }

    public void validateSubquery(ExprValidationContext validationContext) throws ExprValidationException {
        evalStrategy = SubselectNRForgeFactory.createStrategyAnyAllIn(this, isNotIn, false, false, null, validationContext.getClasspathImportService());
    }

    public LinkedHashMap<String, Object> typableGetRowProperties() {
        return null;
    }

    protected CodegenExpression evalMatchesPlainCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return evalStrategy.evaluateMatchesCodegen(parent, symbols, classScope);
    }

    public boolean isAllowMultiColumnSelect() {
        return false;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        return null;
    }

    protected CodegenExpression evalMatchesGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    protected CodegenExpression evalMatchesGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    protected CodegenExpression evalMatchesGetEventBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    protected CodegenExpression evalMatchesTypableSingleCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    protected CodegenExpression evalMatchesTypableMultiCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }
}
