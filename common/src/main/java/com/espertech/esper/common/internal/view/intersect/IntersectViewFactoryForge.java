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
package com.espertech.esper.common.internal.view.intersect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil.makeViewFactories;

/**
 * Factory for union-views.
 */
public class IntersectViewFactoryForge extends ViewFactoryForgeBase implements DataWindowViewForge, DataWindowViewForgeUniqueCandidate {
    protected final List<ViewFactoryForge> intersected;
    protected int batchViewIndex = -1;
    protected boolean hasAsymetric;

    public IntersectViewFactoryForge(List<ViewFactoryForge> intersected) {
        this.intersected = intersected;
        if (intersected.isEmpty()) {
            throw new IllegalStateException("Empty intersected forges");
        }

        int batchCount = 0;
        for (int i = 0; i < intersected.size(); i++) {
            ViewFactoryForge forge = intersected.get(i);
            hasAsymetric |= forge instanceof AsymetricDataWindowViewForge;
            if (forge instanceof DataWindowBatchingViewForge) {
                batchCount++;
                batchViewIndex = i;
            }
        }
        if (batchCount > 1) {
            throw new ViewProcessingException("Cannot combined multiple batch data windows into an intersection");
        }
    }

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    protected Class typeOfFactory() {
        return IntersectViewFactory.class;
    }

    protected String factoryMethod() {
        return "intersect";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(factory, "setBatchViewIndex", constant(batchViewIndex))
                .exprDotMethod(factory, "setHasAsymetric", constant(hasAsymetric))
                .exprDotMethod(factory, "setIntersecteds", localMethod(makeViewFactories(intersected, this.getClass(), method, classScope, symbols)));
    }

    public String getViewName() {
        return getViewNameUnionIntersect(true, intersected);
    }

    @Override
    public void accept(ViewForgeVisitor visitor) {
        visitor.visit(this);
        for (ViewFactoryForge forge : intersected) {
            forge.accept(visitor);
        }
    }

    public static String getViewNameUnionIntersect(boolean intersect, Collection<ViewFactoryForge> forges) {
        StringBuilder buf = new StringBuilder();
        buf.append(intersect ? "Intersection" : "Union");

        if (forges == null) {
            return buf.toString();
        }
        buf.append(" of ");
        String delimiter = "";
        for (ViewFactoryForge forge : forges) {
            buf.append(delimiter);
            buf.append(forge.getViewName());
            delimiter = ",";
        }

        return buf.toString();
    }

    public Set<String> getUniquenessCandidatePropertyNames() {
        for (ViewFactoryForge forge : intersected) {
            if (forge instanceof DataWindowViewForgeUniqueCandidate) {
                DataWindowViewForgeUniqueCandidate unique = (DataWindowViewForgeUniqueCandidate) forge;
                Set<String> props = unique.getUniquenessCandidatePropertyNames();
                if (props != null) {
                    return props;
                }
            }
        }
        return null;
    }

    @Override
    public List<ViewFactoryForge> getInnerForges() {
        return intersected;
    }
}
