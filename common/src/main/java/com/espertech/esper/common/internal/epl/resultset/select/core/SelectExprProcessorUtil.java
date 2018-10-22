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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProvider;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.REF_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.REF_ISSYNTHESIZE;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class SelectExprProcessorUtil {
    public static CodegenExpressionNewAnonymousClass makeAnonymous(SelectExprProcessorForge insertHelper, CodegenMethod method, CodegenExpressionRef initSvc, CodegenClassScope classScope) {
        CodegenExpressionField resultType = classScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(insertHelper.getResultEventType(), initSvc));
        CodegenExpressionField eventBeanFactory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, true);
        SelectExprProcessorCodegenSymbol selectEnv = new SelectExprProcessorCodegenSymbol();
        CodegenSymbolProvider symbolProvider = new CodegenSymbolProvider() {
            public void provide(Map<String, Class> symbols) {
                exprSymbol.provide(symbols);
                selectEnv.provide(symbols);
            }
        };

        CodegenExpressionNewAnonymousClass anonymousSelect = newAnonymousClass(method.getBlock(), SelectExprProcessor.class);
        CodegenMethod processMethod = CodegenMethod.makeParentNode(EventBean.class, SelectExprProcessorUtil.class, symbolProvider, classScope)
                .addParam(EventBean[].class, ExprForgeCodegenNames.NAME_EPS)
                .addParam(boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA)
                .addParam(boolean.class, SelectExprProcessorCodegenSymbol.NAME_ISSYNTHESIZE)
                .addParam(ExprEvaluatorContext.class, ExprForgeCodegenNames.NAME_EXPREVALCONTEXT);
        anonymousSelect.addMethod("process", processMethod);
        processMethod.getBlock().apply(instblock(classScope, "qSelectClause", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_EXPREVALCONTEXT));

        CodegenMethod performMethod = insertHelper.processCodegen(resultType, eventBeanFactory, processMethod, selectEnv, exprSymbol, classScope);
        exprSymbol.derivedSymbolsCodegen(processMethod, processMethod.getBlock(), classScope);
        processMethod.getBlock()
                .declareVar(EventBean.class, "result", localMethod(performMethod))
                .apply(instblock(classScope, "aSelectClause", REF_ISNEWDATA, ref("result"), constantNull()))
                .methodReturn(ref("result"));

        return anonymousSelect;
    }
}
