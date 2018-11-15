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
package com.espertech.esper.common.internal.view.rank;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluator;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluators;

/**
 * Factory for rank window views.
 */
public class RankWindowViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeWithPrevious {
    private List<ExprNode> viewParameters;

    /**
     * The unique-by expressions.
     */
    protected ExprNode[] uniqueCriteriaExpressions;

    /**
     * The sort-by expressions.
     */
    protected ExprNode[] sortCriteriaExpressions;

    /**
     * The flags defining the ascending or descending sort order.
     */
    protected boolean[] isDescendingValues;

    /**
     * The sort window size.
     */
    protected ExprForge sizeForge;

    protected boolean useCollatorSort;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
        this.useCollatorSort = viewForgeEnv.getConfiguration().getCompiler().getLanguage().isSortUsingCollator();
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        eventType = parentEventType;
        String message = getViewName() + " view requires a list of expressions providing unique keys, a numeric size parameter and a list of expressions providing sort keys";
        if (viewParameters.size() < 3) {
            throw new ViewParameterException(message);
        }

        // validate
        ExprNode[] validated = ViewForgeSupport.validate(getViewName(), parentEventType, viewParameters, true, viewForgeEnv, streamNumber);

        // find size-parameter index
        int indexNumericSize = -1;
        for (int i = 0; i < validated.length; i++) {
            if (validated[i] instanceof ExprConstantNode || validated[i] instanceof ExprContextPropertyNode) {
                indexNumericSize = i;
                break;
            }
        }
        if (indexNumericSize == -1) {
            throw new ViewParameterException("Failed to find constant value for the numeric size parameter");
        }
        if (indexNumericSize == 0) {
            throw new ViewParameterException("Failed to find unique value expressions that are expected to occur before the numeric size parameter");
        }
        if (indexNumericSize == validated.length - 1) {
            throw new ViewParameterException("Failed to find sort key expressions after the numeric size parameter");
        }

        // validate non-constant for unique-keys and sort-keys
        for (int i = 0; i < indexNumericSize; i++) {
            ViewForgeSupport.assertReturnsNonConstant(getViewName(), validated[i], i);
        }
        for (int i = indexNumericSize + 1; i < validated.length; i++) {
            ViewForgeSupport.assertReturnsNonConstant(getViewName(), validated[i], i);
        }

        // get sort size
        ViewForgeSupport.validateNoProperties(getViewName(), validated[indexNumericSize], indexNumericSize);
        sizeForge = ViewForgeSupport.validateSizeParam(getViewName(), validated[indexNumericSize], indexNumericSize);

        // compile unique expressions
        uniqueCriteriaExpressions = new ExprNode[indexNumericSize];
        System.arraycopy(validated, 0, uniqueCriteriaExpressions, 0, indexNumericSize);

        // compile sort expressions
        sortCriteriaExpressions = new ExprNode[validated.length - indexNumericSize - 1];
        isDescendingValues = new boolean[sortCriteriaExpressions.length];

        int count = 0;
        for (int i = indexNumericSize + 1; i < validated.length; i++) {
            if (validated[i] instanceof ExprOrderedExpr) {
                isDescendingValues[count] = ((ExprOrderedExpr) validated[i]).isDescending();
                sortCriteriaExpressions[count] = validated[i].getChildNodes()[0];
            } else {
                sortCriteriaExpressions[count] = validated[i];
            }
            count++;
        }
    }

    protected Class typeOfFactory() {
        return RankWindowViewFactory.class;
    }

    protected String factoryMethod() {
        return "rank";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(factory, "setSize", codegenEvaluator(sizeForge, method, this.getClass(), classScope))
                .exprDotMethod(factory, "setSortCriteriaEvaluators", codegenEvaluators(sortCriteriaExpressions, method, this.getClass(), classScope))
                .exprDotMethod(factory, "setSortCriteriaTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(sortCriteriaExpressions)))
                .exprDotMethod(factory, "setIsDescendingValues", constant(isDescendingValues))
                .exprDotMethod(factory, "setUseCollatorSort", constant(useCollatorSort))
                .exprDotMethod(factory, "setUniqueEvaluators", codegenEvaluators(uniqueCriteriaExpressions, method, this.getClass(), classScope))
                .exprDotMethod(factory, "setUniqueTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(uniqueCriteriaExpressions)));
    }

    public String getViewName() {
        return ViewEnum.RANK_WINDOW.getName();
    }
}
