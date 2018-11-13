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
package com.espertech.esper.common.internal.view.unique;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluators;

/**
 * Factory for {@link UniqueByPropertyView} instances.
 */
public class UniqueByPropertyViewForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeUniqueCandidate {
    protected List<ExprNode> viewParameters;
    protected ExprNode[] criteriaExpressions;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        criteriaExpressions = ViewForgeSupport.validate(getViewName(), parentEventType, viewParameters, false, viewForgeEnv, streamNumber);

        if (criteriaExpressions.length == 0) {
            String errorMessage = getViewName() + " view requires a one or more expressions providing unique values as parameters";
            throw new ViewParameterException(errorMessage);
        }

        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return UniqueByPropertyViewFactory.class;
    }

    protected String factoryMethod() {
        return "unique";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(factory, "setCriteriaEvals", codegenEvaluators(criteriaExpressions, method, this.getClass(), classScope))
                .exprDotMethod(factory, "setCriteriaTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions)));
    }

    public Set<String> getUniquenessCandidatePropertyNames() {
        return ExprNodeUtilityQuery.getPropertyNamesIfAllProps(criteriaExpressions);
    }

    public String getViewName() {
        return ViewEnum.UNIQUE_BY_PROPERTY.getName();
    }

    public ExprNode[] getCriteriaExpressions() {
        return criteriaExpressions;
    }
}
