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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeBase;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;
import com.espertech.esper.common.internal.view.core.ViewParameterException;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.util.List;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenEvaluator;

/**
 * Factory for {@link RegressionLinestView} instances.
 */
public class RegressionLinestViewForge extends ViewFactoryForgeBase {
    private List<ExprNode> viewParameters;

    private ExprNode expressionX;
    private ExprNode expressionY;
    protected StatViewAdditionalPropsForge additionalProps;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        ExprNode[] validated = ViewForgeSupport.validate(getViewName(), parentEventType, viewParameters, true, viewForgeEnv, streamNumber);

        if (validated.length < 2) {
            throw new ViewParameterException(getViewParamMessage());
        }
        if ((!JavaClassHelper.isNumeric(validated[0].getForge().getEvaluationType())) || (!JavaClassHelper.isNumeric(validated[1].getForge().getEvaluationType()))) {
            throw new ViewParameterException(getViewParamMessage());
        }

        expressionX = validated[0];
        expressionY = validated[1];

        additionalProps = StatViewAdditionalPropsForge.make(validated, 2, parentEventType, streamNumber, viewForgeEnv);
        eventType = RegressionLinestView.createEventType(additionalProps, viewForgeEnv, streamNumber);
    }

    @Override
    public List<StmtClassForgeableFactory> initAdditionalForgeables(ViewForgeEnv viewForgeEnv) {
        return SerdeEventTypeUtility.plan(eventType, viewForgeEnv.getStatementRawInfo(), viewForgeEnv.getSerdeEventTypeRegistry(), viewForgeEnv.getSerdeResolver());
    }

    public Class typeOfFactory() {
        return RegressionLinestViewFactory.class;
    }

    public String factoryMethod() {
        return "regression";
    }

    public void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (additionalProps != null) {
            method.getBlock().exprDotMethod(factory, "setAdditionalProps", additionalProps.codegen(method, classScope));
        }
        method.getBlock()
                .exprDotMethod(factory, "setExpressionXEval", codegenEvaluator(expressionX.getForge(), method, this.getClass(), classScope))
                .exprDotMethod(factory, "setExpressionYEval", codegenEvaluator(expressionY.getForge(), method, this.getClass(), classScope));
    }

    public String getViewName() {
        return "Regression";
    }

    private String getViewParamMessage() {
        return getViewName() + " view requires two expressions providing x and y values as properties";
    }
}
