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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.faf.StmtClassForgableQueryMethodProvider;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableRSPFactoryProvider;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodSelectForge implements FAFQueryMethodForge {
    private final FAFQueryMethodSelectDesc desc;
    private final String classNameResultSetProcessor;
    private final StatementRawInfo statementRawInfo;

    public FAFQueryMethodSelectForge(FAFQueryMethodSelectDesc desc, String classNameResultSetProcessor, StatementRawInfo statementRawInfo) {
        this.desc = desc;
        this.classNameResultSetProcessor = classNameResultSetProcessor;
        this.statementRawInfo = statementRawInfo;
    }

    public List<StmtClassForgable> makeForgables(String queryMethodProviderClassName, String classPostfix, CodegenPackageScope packageScope) {
        List<StmtClassForgable> forgables = new ArrayList<>();

        // generate RSP
        forgables.add(new StmtClassForgableRSPFactoryProvider(classNameResultSetProcessor, desc.getResultSetProcessor(), packageScope, statementRawInfo));

        // generate faf-select
        forgables.add(new StmtClassForgableQueryMethodProvider(queryMethodProviderClassName, packageScope, this));

        return forgables;
    }

    public void makeMethod(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionRef select = ref("select");
        method.getBlock()
                .declareVar(FAFQueryMethodSelect.class, select.getRef(), newInstance(FAFQueryMethodSelect.class))
                .exprDotMethod(select, "setAnnotations", localMethod(AnnotationUtil.makeAnnotations(Annotation[].class, desc.getAnnotations(), method, classScope)))
                .exprDotMethod(select, "setProcessors", FireAndForgetProcessorForge.makeArray(desc.getProcessors(), method, symbols, classScope))
                .declareVar(classNameResultSetProcessor, "rsp", CodegenExpressionBuilder.newInstance(classNameResultSetProcessor, symbols.getAddInitSvc(method)))
                .exprDotMethod(select, "setResultSetProcessorFactoryProvider", ref("rsp"))
                .exprDotMethod(select, "setQueryGraph", desc.getQueryGraph().make(method, symbols, classScope))
                .exprDotMethod(select, "setWhereClause", desc.getWhereClause() == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(desc.getWhereClause().getForge(), method, this.getClass(), classScope))
                .exprDotMethod(select, "setJoinSetComposerPrototype", desc.getJoins() == null ? constantNull() : desc.getJoins().make(method, symbols, classScope))
                .exprDotMethod(select, "setConsumerFilters", ExprNodeUtilityCodegen.codegenEvaluators(desc.getConsumerFilters(), method, this.getClass(), classScope))
                .exprDotMethod(select, "setContextName", constant(desc.getContextName()))
                .exprDotMethod(select, "setTableAccesses", ExprTableEvalStrategyUtil.codegenInitMap(desc.getTableAccessForges(), this.getClass(), method, symbols, classScope))
                .exprDotMethod(select, "setHasTableAccess", constant(desc.isHasTableAccess()))
                .exprDotMethod(select, "setDistinct", constant(desc.isDistinct()))
                .methodReturn(select);
    }
}
