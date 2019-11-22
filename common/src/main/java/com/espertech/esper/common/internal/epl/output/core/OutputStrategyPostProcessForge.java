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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.output.view.OutputStrategyPostProcessFactory;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.output.core.OutputProcessViewCodegenNames.MEMBER_CHILD;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;

public class OutputStrategyPostProcessForge {
    private final boolean isRouted;
    private final SelectClauseStreamSelectorEnum insertIntoStreamSelector;
    private final SelectClauseStreamSelectorEnum selectStreamSelector;
    private final boolean routeToFront;
    private final TableMetaData table;
    private final boolean audit;

    public OutputStrategyPostProcessForge(boolean isRouted, SelectClauseStreamSelectorEnum insertIntoStreamSelector, SelectClauseStreamSelectorEnum selectStreamSelector, boolean routeToFront, TableMetaData table, boolean audit) {
        this.isRouted = isRouted;
        this.insertIntoStreamSelector = insertIntoStreamSelector;
        this.selectStreamSelector = selectStreamSelector;
        this.routeToFront = routeToFront;
        this.table = table;
        this.audit = audit;
    }

    public boolean hasTable() {
        return table != null;
    }

    /**
     * Code for post-process, "result" can be null, "force-update" can be passed in
     *
     * @param classScope class scope
     * @param parent     parent
     * @return method
     */
    public CodegenMethod postProcessCodegenMayNullMayForce(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(void.class, OutputStrategyPostProcessForge.class, classScope).addParam(boolean.class, "forceUpdate").addParam(UniformPair.class, "result");

        CodegenBlock ifChild = method.getBlock().ifCondition(notEqualsNull(MEMBER_CHILD));

        // handle non-null
        CodegenBlock ifResultNotNull = ifChild.ifRefNotNull("result");
        if (isRouted) {
            if (insertIntoStreamSelector.isSelectsIStream()) {
                ifResultNotNull.localMethod(routeCodegen(classScope, parent), cast(EventBean[].class, exprDotMethod(ref("result"), "getFirst")));
            }
            if (insertIntoStreamSelector.isSelectsRStream()) {
                ifResultNotNull.localMethod(routeCodegen(classScope, parent), cast(EventBean[].class, exprDotMethod(ref("result"), "getSecond")));
            }
        }
        if (selectStreamSelector == SelectClauseStreamSelectorEnum.RSTREAM_ONLY) {
            ifResultNotNull.ifCondition(notEqualsNull(exprDotMethod(ref("result"), "getSecond")))
                    .exprDotMethod(MEMBER_CHILD, "newResult", newInstance(UniformPair.class, exprDotMethod(ref("result"), "getSecond"), constantNull()))
                    .ifElseIf(ref("forceUpdate"))
                    .exprDotMethod(MEMBER_CHILD, "newResult", publicConstValue(UniformPair.class, "EMPTY_PAIR"));
        } else if (selectStreamSelector == SelectClauseStreamSelectorEnum.RSTREAM_ISTREAM_BOTH) {
            ifResultNotNull.ifCondition(or(notEqualsNull(exprDotMethod(ref("result"), "getFirst")), notEqualsNull(exprDotMethod(ref("result"), "getSecond"))))
                    .exprDotMethod(MEMBER_CHILD, "newResult", ref("result"))
                    .ifElseIf(ref("forceUpdate"))
                    .exprDotMethod(MEMBER_CHILD, "newResult", publicConstValue(UniformPair.class, "EMPTY_PAIR"));
        } else {
            ifResultNotNull.ifCondition(notEqualsNull(exprDotMethod(ref("result"), "getFirst")))
                    .exprDotMethod(MEMBER_CHILD, "newResult", newInstance(UniformPair.class, exprDotMethod(ref("result"), "getFirst"), constantNull()))
                    .ifElseIf(ref("forceUpdate"))
                    .exprDotMethod(MEMBER_CHILD, "newResult", publicConstValue(UniformPair.class, "EMPTY_PAIR"));
        }

        // handle null-result (force-update)
        CodegenBlock ifResultNull = ifResultNotNull.ifElse();
        ifResultNull.ifCondition(ref("forceUpdate"))
                .exprDotMethod(MEMBER_CHILD, "newResult", publicConstValue(UniformPair.class, "EMPTY_PAIR"));

        return method;
    }

    private CodegenMethod routeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(void.class, OutputStrategyPostProcessForge.class, classScope).addParam(EventBean[].class, "events");
        CodegenBlock forEach = method.getBlock()
                .ifRefNull("events").blockReturnNoValue()
                .forEach(EventBean.class, "routed", ref("events"));

        if (audit) {
            forEach.expression(exprDotMethodChain(MEMBER_AGENTINSTANCECONTEXT).add("getAuditProvider").add("insert", ref("routed"), MEMBER_AGENTINSTANCECONTEXT));
        }
        forEach.expression(exprDotMethodChain(MEMBER_AGENTINSTANCECONTEXT).add("getInternalEventRouter").add("route", ref("routed"), MEMBER_AGENTINSTANCECONTEXT, constant(routeToFront)));

        return method;
    }

    public CodegenExpression make(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression resolveTable = table == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method));
        return newInstance(OutputStrategyPostProcessFactory.class, constant(isRouted),
            insertIntoStreamSelector == null ? constantNull() : enumValue(SelectClauseStreamSelectorEnum.class, insertIntoStreamSelector.name()),
                enumValue(SelectClauseStreamSelectorEnum.class, selectStreamSelector.name()),
                constant(routeToFront), resolveTable);
    }
}
