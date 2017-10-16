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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.orderby.OrderByProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.REF_AGENTINSTANCECONTEXT;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.REF_ISNEWDATA;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

public class OrderByProcessorCompiler {

    public static void makeOrderByProcessors(OrderByProcessorFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> providerExplicitMembers, CodegenCtor providerCtor, String providerClassName, String memberOrderByFactory) {
        providerExplicitMembers.add(new CodegenTypedParam(OrderByProcessorFactory.class, memberOrderByFactory));
        if (forge == null) {
            providerCtor.getBlock().assignRef(memberOrderByFactory, constantNull());
            return;
        }

        makeFactory(forge, classScope, innerClasses, providerClassName);
        makeService(forge, classScope, innerClasses, providerClassName);

        providerCtor.getBlock().assignRef(memberOrderByFactory, newInstanceInnerClass(CLASSNAME_ORDERBYPROCESSORFACTORY, ref("this")));
    }

    private static void makeFactory(OrderByProcessorFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName) {
        CodegenMethodNode instantiateMethod = CodegenMethodNode.makeParentNode(OrderByProcessor.class, OrderByProcessorCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AgentInstanceContext.class, REF_AGENTINSTANCECONTEXT.getRef());
        forge.instantiateCodegen(instantiateMethod, classScope);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(OrderByProcessorCompiler.class, classScope, ctorParams);

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(instantiateMethod, "instantiate", methods);
        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_ORDERBYPROCESSORFACTORY, OrderByProcessorFactory.class, ctor, Collections.emptyList(), Collections.emptyMap(), methods);
        innerClasses.add(innerClass);
    }

    private static void makeService(OrderByProcessorFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName) {
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();

        CodegenMethodNode sortPlainMethod = CodegenMethodNode.makeParentNode(EventBean[].class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(SORTPLAIN_PARAMS);
        forge.sortPlainCodegen(sortPlainMethod, classScope, namedMethods);

        CodegenMethodNode sortWGroupKeysMethod = CodegenMethodNode.makeParentNode(EventBean[].class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(SORTWGROUPKEYS_PARAMS);
        forge.sortWGroupKeysCodegen(sortWGroupKeysMethod, classScope, namedMethods);

        CodegenMethodNode sortRollupMethod = CodegenMethodNode.makeParentNode(EventBean[].class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(SORTROLLUP_PARAMS);
        forge.sortRollupCodegen(sortRollupMethod, classScope, namedMethods);

        CodegenMethodNode getSortKeyMethod = CodegenMethodNode.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, REF_EPS.getRef()).addParam(boolean.class, REF_ISNEWDATA.getRef()).addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        forge.getSortKeyCodegen(getSortKeyMethod, classScope, namedMethods);

        CodegenMethodNode getSortKeyRollupMethod = CodegenMethodNode.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, REF_EPS.getRef()).addParam(boolean.class, REF_ISNEWDATA.getRef()).addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef()).addParam(AggregationGroupByRollupLevel.class, REF_ORDERROLLUPLEVEL.getRef());
        forge.getSortKeyRollupCodegen(getSortKeyRollupMethod, classScope, namedMethods);

        CodegenMethodNode sortWOrderKeysMethod = CodegenMethodNode.makeParentNode(EventBean[].class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, REF_OUTGOINGEVENTS.getRef()).addParam(Object[].class, REF_ORDERKEYS.getRef()).addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        forge.sortWOrderKeysCodegen(sortWOrderKeysMethod, classScope, namedMethods);

        CodegenMethodNode sortTwoKeysMethod = CodegenMethodNode.makeParentNode(EventBean[].class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(SORTTWOKEYS_PARAMS);
        forge.sortTwoKeysCodegen(sortTwoKeysMethod, classScope, namedMethods);

        List<CodegenTypedParam> members = new ArrayList<>();
        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(OrderByProcessorCompiler.class, classScope, ctorParams);
        forge.ctorCodegen(ctor, members, classScope);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(sortPlainMethod, "sortPlain", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(sortWGroupKeysMethod, "sortWGroupKeys", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(sortRollupMethod, "sortRollup", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getSortKeyMethod, "getSortKey", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getSortKeyRollupMethod, "getSortKeyRollup", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(sortWOrderKeysMethod, "sortWOrderKeys", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(sortTwoKeysMethod, "sortTwoKeys", innerMethods);
        for (Map.Entry<String, CodegenMethodNode> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(OrderByProcessorCodegenNames.CLASSNAME_ORDERBYPROCESSOR, OrderByProcessor.class, ctor, members, Collections.emptyMap(), innerMethods);
        innerClasses.add(innerClass);
    }
}
