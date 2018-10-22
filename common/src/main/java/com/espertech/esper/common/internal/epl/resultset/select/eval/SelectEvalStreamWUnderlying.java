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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprStreamDesc;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.event.core.*;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalStreamWUnderlying extends SelectEvalStreamBaseMap implements SelectExprProcessorForge {

    private final WrapperEventType wrapperEventType;
    private final List<SelectExprStreamDesc> unnamedStreams;
    private final boolean singleStreamWrapper;
    private final boolean underlyingIsFragmentEvent;
    private final int underlyingStreamNumber;
    private final EventPropertyGetterSPI underlyingPropertyEventGetter;
    private final ExprForge underlyingExprForge;
    private final TableMetaData tableMetadata;
    private final EventType[] eventTypes;

    public SelectEvalStreamWUnderlying(SelectExprForgeContext selectExprForgeContext,
                                       EventType resultEventType,
                                       List<SelectClauseStreamCompiledSpec> namedStreams,
                                       boolean usingWildcard,
                                       List<SelectExprStreamDesc> unnamedStreams,
                                       boolean singleStreamWrapper,
                                       boolean underlyingIsFragmentEvent,
                                       int underlyingStreamNumber,
                                       EventPropertyGetterSPI underlyingPropertyEventGetter,
                                       ExprForge underlyingExprForge,
                                       TableMetaData tableMetadata, /* TableMetadata */
                                       EventType[] eventTypes) {
        super(selectExprForgeContext, resultEventType, namedStreams, usingWildcard);
        this.wrapperEventType = (WrapperEventType) resultEventType;
        this.unnamedStreams = unnamedStreams;
        this.singleStreamWrapper = singleStreamWrapper;
        this.underlyingIsFragmentEvent = underlyingIsFragmentEvent;
        this.underlyingStreamNumber = underlyingStreamNumber;
        this.underlyingPropertyEventGetter = underlyingPropertyEventGetter;
        this.underlyingExprForge = underlyingExprForge;
        this.tableMetadata = tableMetadata;
        this.eventTypes = eventTypes;
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, SelectEvalStreamWUnderlying.class, codegenClassScope).addParam(Map.class, "props");
        CodegenExpressionField wrapperUndType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(wrapperEventType.getUnderlyingEventType(), EPStatementInitServices.REF));

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenBlock block = methodNode.getBlock();
        if (singleStreamWrapper) {
            block.declareVar(DecoratingEventBean.class, "wrapper", cast(DecoratingEventBean.class, arrayAtIndex(refEPS, constant(0))))
                    .ifRefNotNull("wrapper")
                    .exprDotMethod(props, "putAll", exprDotMethod(ref("wrapper"), "getDecoratingProperties"))
                    .blockEnd();
        }

        if (underlyingIsFragmentEvent) {
            CodegenExpression fragment = ((EventTypeSPI) eventTypes[underlyingStreamNumber]).getGetterSPI(unnamedStreams.get(0).getStreamSelected().getStreamName()).eventBeanFragmentCodegen(ref("eventBean"), methodNode, codegenClassScope);
            block.declareVar(EventBean.class, "eventBean", arrayAtIndex(refEPS, constant(underlyingStreamNumber)))
                    .declareVar(EventBean.class, "theEvent", cast(EventBean.class, fragment));
        } else if (underlyingPropertyEventGetter != null) {
            block.declareVar(EventBean.class, "theEvent", constantNull())
                    .declareVar(Object.class, "value", underlyingPropertyEventGetter.eventBeanGetCodegen(arrayAtIndex(refEPS, constant(underlyingStreamNumber)), methodNode, codegenClassScope))
                    .ifRefNotNull("value")
                    .assignRef("theEvent", exprDotMethod(eventBeanFactory, "adapterForTypedBean", ref("value"), wrapperUndType))
                    .blockEnd();
        } else if (underlyingExprForge != null) {
            block.declareVar(EventBean.class, "theEvent", constantNull())
                    .declareVar(Object.class, "value", underlyingExprForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope))
                    .ifRefNotNull("value")
                    .assignRef("theEvent", exprDotMethod(eventBeanFactory, "adapterForTypedBean", ref("value"), wrapperUndType))
                    .blockEnd();
        } else {
            block.declareVar(EventBean.class, "theEvent", arrayAtIndex(refEPS, constant(underlyingStreamNumber)));
            if (tableMetadata != null) {
                CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(tableMetadata, codegenClassScope, this.getClass());
                block.ifRefNotNull("theEvent")
                        .assignRef("theEvent", exprDotMethod(eventToPublic, "convert", ref("theEvent"), refEPS, refIsNewData, refExprEvalCtx))
                        .blockEnd();
            }
        }
        block.methodReturn(exprDotMethod(eventBeanFactory, "adapterForTypedWrapper", ref("theEvent"), ref("props"), resultEventType));
        return localMethod(methodNode, props);
    }
}
