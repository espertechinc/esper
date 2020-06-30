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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiler.NEWLINE;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge.optionalEvaluator;

public class FilterSpecPlanPathForge {
    private final FilterSpecPlanPathTripletForge[] triplets;
    private ExprNode pathNegate;

    public FilterSpecPlanPathForge(FilterSpecPlanPathTripletForge[] triplets, ExprNode pathNegate) {
        this.triplets = triplets;
        this.pathNegate = pathNegate;
    }

    public FilterSpecPlanPathTripletForge[] getTriplets() {
        return triplets;
    }

    public ExprNode getPathNegate() {
        return pathNegate;
    }

    public void setPathNegate(ExprNode pathNegate) {
        this.pathNegate = pathNegate;
    }

    public boolean equalsFilter(FilterSpecPlanPathForge other) {
        if (triplets.length != other.getTriplets().length) {
            return false;
        }

        for (int i = 0; i < triplets.length; i++) {
            FilterSpecPlanPathTripletForge mytriplet = triplets[i];
            FilterSpecPlanPathTripletForge othertriplet = other.triplets[i];
            if (!mytriplet.equalsFilter(othertriplet)) {
                return false;
            }
        }

        if (!ExprNodeUtilityCompare.deepEqualsNullChecked(pathNegate, other.pathNegate, true)) {
            return false;
        }
        return true;
    }

    public CodegenMethod codegen(CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FilterSpecPlanPath.EPTYPE, FilterSpecParamForge.class, classScope);
        method.getBlock().declareVar(FilterSpecPlanPathTriplet.EPTYPEARRAY, "triplets", newArrayByLength(FilterSpecPlanPathTriplet.EPTYPE, constant(triplets.length)));
        for (int i = 0; i < triplets.length; i++) {
            CodegenMethod triplet = triplets[i].codegen(method, symbols, classScope);
            method.getBlock().assignArrayElement("triplets", constant(i), localMethod(triplet));
        }
        method.getBlock()
            .declareVarNewInstance(FilterSpecPlanPath.EPTYPE, "path")
            .exprDotMethod(ref("path"), "setTriplets", ref("triplets"))
            .exprDotMethod(ref("path"), "setPathNegate", optionalEvaluator(pathNegate, method, classScope))
            .methodReturn(ref("path"));
        return method;
    }

    protected void appendFilterPlanPath(int indexPath, StringBuilder buf) {
        buf.append("  -path #").append(indexPath).append(" there are ").append(triplets.length).append(" triplets").append(NEWLINE);
        if (pathNegate != null) {
            buf.append("    -path-negate-expression: ").append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(pathNegate)).append(NEWLINE);
        }
        int indextriplet = 0;
        for (FilterSpecPlanPathTripletForge forge : triplets) {
            forge.appendFilterPlanTriplet(indextriplet, buf);
            indextriplet++;
        }
    }
}
