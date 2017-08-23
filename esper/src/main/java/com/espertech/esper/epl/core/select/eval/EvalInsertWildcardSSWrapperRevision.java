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
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.DecoratingEventBean;
import com.espertech.esper.event.vaevent.ValueAddEventProcessor;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class EvalInsertWildcardSSWrapperRevision extends EvalBaseMap implements SelectExprProcessor {

    private final ValueAddEventProcessor vaeProcessor;

    public EvalInsertWildcardSSWrapperRevision(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, ValueAddEventProcessor vaeProcessor) {
        super(selectExprForgeContext, resultEventType);
        this.vaeProcessor = vaeProcessor;
    }

    protected void initSelectExprProcessorSpecific(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
    }

    // In case of a wildcard and single stream that is itself a
    // wrapper bean, we also need to add the map properties
    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        return selectExprInsertWildcardSSWrapRevision(eventsPerStream, evaluators.length, props, vaeProcessor);
    }

    protected CodegenExpression processSpecificCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenExpression props, CodegenMethodNode methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember member = codegenClassScope.makeAddMember(ValueAddEventProcessor.class, vaeProcessor);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        return staticMethod(EvalInsertWildcardSSWrapperRevision.class, "selectExprInsertWildcardSSWrapRevision", refEPS, constant(evaluators.length), props, CodegenExpressionBuilder.member(member.getMemberId()));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param eventsPerStream events
     * @param numEvaluators num evals
     * @param props props
     * @param vaeProcessor processor
     * @return bean
     */
    public static EventBean selectExprInsertWildcardSSWrapRevision(EventBean[] eventsPerStream, int numEvaluators, Map<String, Object> props, ValueAddEventProcessor vaeProcessor) {
        DecoratingEventBean wrapper = (DecoratingEventBean) eventsPerStream[0];
        if (wrapper != null) {
            Map<String, Object> map = wrapper.getDecoratingProperties();
            if ((numEvaluators == 0) && (!map.isEmpty())) {
                // no action
            } else {
                props.putAll(map);
            }
        }

        EventBean theEvent = eventsPerStream[0];
        return vaeProcessor.getValueAddEventBean(theEvent);
    }
}
