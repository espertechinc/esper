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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiler.NEWLINE;

public class FilterSpecPlanForge {
    public final static FilterSpecPlanForge EMPTY = new FilterSpecPlanForge(new FilterSpecPlanPathForge[0], null, null, null);

    private final FilterSpecPlanPathForge[] paths;
    private ExprNode filterConfirm;
    private ExprNode filterNegate;
    private MatchedEventConvertorForge convertorForge;

    public FilterSpecPlanForge(FilterSpecPlanPathForge[] paths, ExprNode filterConfirm, ExprNode filterNegate, MatchedEventConvertorForge convertorForge) {
        this.paths = paths;
        this.filterConfirm = filterConfirm;
        this.filterNegate = filterNegate;
        this.convertorForge = convertorForge;
    }

    public FilterSpecPlanPathForge[] getPaths() {
        return paths;
    }

    public ExprNode getFilterConfirm() {
        return filterConfirm;
    }

    public void setFilterConfirm(ExprNode filterConfirm) {
        this.filterConfirm = filterConfirm;
    }

    public ExprNode getFilterNegate() {
        return filterNegate;
    }

    public boolean equalsFilter(FilterSpecPlanForge other) {
        if (paths.length != other.getPaths().length) {
            return false;
        }
        for (int i = 0; i < this.paths.length; i++) {
            FilterSpecPlanPathForge myPath = paths[i];
            FilterSpecPlanPathForge otherPath = other.paths[i];
            if (!myPath.equalsFilter(otherPath)) {
                return false;
            }
        }
        if (!ExprNodeUtilityCompare.deepEqualsNullChecked(filterConfirm, other.filterConfirm, true)) {
            return false;
        }
        return ExprNodeUtilityCompare.deepEqualsNullChecked(filterNegate, other.filterNegate, true);
    }

    public CodegenMethod codegenWithEventType(CodegenMethodScope parent, CodegenClassScope classScope) {
        SAIFFInitializeSymbolWEventType symbolsWithType = new SAIFFInitializeSymbolWEventType();
        CodegenMethod method = parent.makeChildWithScope(FilterSpecPlan.EPTYPE, FilterSpecParamForge.class, symbolsWithType, classScope).addParam(EventType.EPTYPE, SAIFFInitializeSymbolWEventType.REF_EVENTTYPE.getRef()).addParam(EPStatementInitServices.EPTYPE, SAIFFInitializeSymbolWEventType.REF_STMTINITSVC.getRef());
        if (paths.length == 0) {
            method.getBlock().methodReturn(publicConstValue(FilterSpecPlan.class, "EMPTY_PLAN"));
            return method;
        }

        method.getBlock().declareVar(FilterSpecPlanPath.EPTYPEARRAY, "paths", newArrayByLength(FilterSpecPlanPath.EPTYPE, constant(paths.length)));
        for (int i = 0; i < paths.length; i++) {
            method.getBlock().assignArrayElement("paths", constant(i), localMethod(paths[i].codegen(method, symbolsWithType, classScope)));
        }
        method.getBlock()
                .declareVarNewInstance(FilterSpecPlan.EPTYPE, "plan")
                .exprDotMethod(ref("plan"), "setPaths", ref("paths"))
                .exprDotMethod(ref("plan"), "setFilterConfirm", optionalEvaluator(filterConfirm, method, classScope))
                .exprDotMethod(ref("plan"), "setFilterNegate", optionalEvaluator(filterNegate, method, classScope))
                .exprDotMethod(ref("plan"), "setConvertor", convertorForge == null ? constantNull() : convertorForge.makeAnonymous(method, classScope))
                .exprDotMethod(ref("plan"), "initialize")
                .methodReturn(ref("plan"));
        return method;
    }

    protected static CodegenExpression optionalEvaluator(ExprNode node, CodegenMethod method, CodegenClassScope classScope) {
        return node == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(node.getForge(), method, FilterSpecPlanForge.class, classScope);
    }

    public static FilterSpecPlanPathForge makePathFromTriplets(Collection<FilterSpecPlanPathTripletForge> tripletsColl, ExprNode control) {
        FilterSpecPlanPathTripletForge[] triplets = tripletsColl.toArray(new FilterSpecPlanPathTripletForge[0]);
        return new FilterSpecPlanPathForge(triplets, control);
    }

    public static FilterSpecPlanForge makePlanFromTriplets(Collection<FilterSpecPlanPathTripletForge> triplets, ExprNode topLevelNegation, FilterSpecCompilerArgs args) {
        FilterSpecPlanPathForge path = makePathFromTriplets(triplets, null);
        MatchedEventConvertorForge convertor = new MatchedEventConvertorForge(args.taggedEventTypes, args.arrayEventTypes, args.allTagNamesOrdered, null, true);
        return new FilterSpecPlanForge(new FilterSpecPlanPathForge[]{path}, null, topLevelNegation, convertor);
    }

    public MatchedEventConvertorForge getConvertorForge() {
        return convertorForge;
    }

    public void appendPlan(StringBuilder buf) {
        if (filterNegate != null) {
            logFilterPlanExpr(buf, "filter-negate-expression", filterNegate);
        }
        if (filterConfirm != null) {
            logFilterPlanExpr(buf, "filter-confirm-expression", filterConfirm);
        }
        for (int i = 0; i < paths.length; i++) {
            paths[i].appendFilterPlanPath(i, buf);
        }
    }

    private static void logFilterPlanExpr(StringBuilder buf, String name, ExprNode exprNode) {
        buf.append("  -").append(name).append(": ").append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode)).append(NEWLINE);
    }
}
