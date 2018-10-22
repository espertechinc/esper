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
import com.espertech.esper.common.internal.type.RelationalOpEnum;

import java.util.LinkedHashMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Represents a subselect in an expression tree.
 */
public class ExprSubselectAllSomeAnyNode extends ExprSubselectNode {
    private final boolean isNot;
    private final boolean isAll;
    private final RelationalOpEnum relationalOp;
    private SubselectForgeNR evalStrategy;

    /**
     * Ctor.
     *
     * @param statementSpec    is the lookup statement spec from the parser, unvalidated
     * @param not              when NOT
     * @param all              when ALL, false for ANY
     * @param relationalOpEnum operator
     */
    public ExprSubselectAllSomeAnyNode(StatementSpecRaw statementSpec, boolean not, boolean all, RelationalOpEnum relationalOpEnum) {
        super(statementSpec);
        isNot = not;
        isAll = all;
        this.relationalOp = relationalOpEnum;
    }

    /**
     * Returns true for not.
     *
     * @return not indicator
     */
    public boolean isNot() {
        return isNot;
    }

    /**
     * Returns true for all.
     *
     * @return all indicator
     */
    public boolean isAll() {
        return isAll;
    }

    /**
     * Returns relational op.
     *
     * @return op
     */
    public RelationalOpEnum getRelationalOp() {
        return relationalOp;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public void validateSubquery(ExprValidationContext validationContext) throws ExprValidationException {
        evalStrategy = SubselectNRForgeFactory.createStrategyAnyAllIn(this, isNot, isAll, !isAll, relationalOp, validationContext.getClasspathImportService());
    }

    public LinkedHashMap<String, Object> typableGetRowProperties() {
        return null;
    }


    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    protected CodegenExpression evalMatchesPlainCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return evalStrategy.evaluateMatchesCodegen(parent, symbols, classScope);
    }

    protected CodegenExpression evalMatchesTypableMultiCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    protected CodegenExpression evalMatchesGetEventBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    protected CodegenExpression evalMatchesGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        return null;
    }

    protected CodegenExpression evalMatchesTypableSingleCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    protected CodegenExpression evalMatchesGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        return constantNull();
    }

    @Override
    public boolean isAllowMultiColumnSelect() {
        return false;
    }
}
