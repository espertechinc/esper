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
package com.espertech.esper.common.internal.view.union;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.intersect.IntersectViewFactoryForge;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil.makeViewFactories;

/**
 * Factory for union-views.
 */
public class UnionViewFactoryForge extends ViewFactoryForgeBase implements DataWindowViewForge {
    private final List<ViewFactoryForge> unioned;
    private boolean hasAsymetric;

    public UnionViewFactoryForge(List<ViewFactoryForge> unioned) {
        this.unioned = unioned;
        if (unioned.isEmpty()) {
            throw new IllegalStateException("Empty unioned views");
        }
        for (ViewFactoryForge forge : unioned) {
            hasAsymetric |= forge instanceof AsymetricDataWindowViewForge;
        }
    }

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return UnionViewFactory.class;
    }

    protected String factoryMethod() {
        return "union";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(factory, "setHasAsymetric", constant(hasAsymetric))
                .exprDotMethod(factory, "setUnioned", localMethod(makeViewFactories(unioned, this.getClass(), method, classScope, symbols)));
    }

    @Override
    public void accept(ViewForgeVisitor visitor) {
        visitor.visit(this);
        for (ViewFactoryForge forge : unioned) {
            forge.accept(visitor);
        }
    }

    public List<ViewFactoryForge> getInnerForges() {
        return unioned;
    }

    public String getViewName() {
        return IntersectViewFactoryForge.getViewNameUnionIntersect(false, unioned);
    }
}
