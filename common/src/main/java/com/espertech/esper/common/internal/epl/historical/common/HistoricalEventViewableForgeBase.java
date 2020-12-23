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
package com.espertech.esper.common.internal.epl.historical.common;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.SortedSet;
import java.util.TreeSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen.codegenEvaluatorReturnObjectOrArray;

public abstract class HistoricalEventViewableForgeBase implements HistoricalEventViewableForge {
    protected final int streamNum;
    protected final EventType eventType;
    protected ExprForge[] inputParamEvaluators;
    protected MultiKeyClassRef multiKeyClassRef;
    protected final TreeSet<Integer> subordinateStreams = new TreeSet<>();
    protected int scheduleCallbackId = -1;

    public abstract EPTypeClass typeOfImplementation();

    public abstract void codegenSetter(CodegenExpressionRef ref, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public HistoricalEventViewableForgeBase(int streamNum, EventType eventType) {
        this.streamNum = streamNum;
        this.eventType = eventType;
    }

    public SortedSet<Integer> getRequiredStreams() {
        return subordinateStreams;
    }

    public final CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOfImplementation(), this.getClass(), classScope);
        CodegenExpressionRef ref = ref("hist");

        CodegenExpression evaluator = codegenEvaluatorReturnObjectOrArray(inputParamEvaluators, method, this.getClass(), classScope);
        CodegenExpression transform = getHistoricalLookupValueToMultiKey(method, classScope);

        method.getBlock().declareVarNewInstance(typeOfImplementation(), ref.getRef())
            .exprDotMethod(ref, "setStreamNumber", constant(streamNum))
            .exprDotMethod(ref, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref, "setHasRequiredStreams", constant(!subordinateStreams.isEmpty()))
            .exprDotMethod(ref, "setScheduleCallbackId", constant(scheduleCallbackId))
            .exprDotMethod(ref, "setEvaluator", evaluator)
            .exprDotMethod(ref, "setLookupValueToMultiKey", transform);
        codegenSetter(ref, method, symbols, classScope);
        method.getBlock()
            .expression(exprDotMethodChain(symbols.getAddInitSvc(method)).add("addReadyCallback", ref))
            .methodReturn(ref);
        return localMethod(method);
    }

    private CodegenExpression getHistoricalLookupValueToMultiKey(CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass transformer = newAnonymousClass(method.getBlock(), HistoricalEventViewableLookupValueToMultiKey.EPTYPE);
        CodegenMethod transform = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.OBJECT.getEPType(), "lv");
        transformer.addMethod("transform", transform);

        if (inputParamEvaluators.length == 0) {
            transform.getBlock().methodReturn(constantNull());
        } else if (inputParamEvaluators.length == 1) {
            EPType paramType = inputParamEvaluators[0].getEvaluationType();
            if (paramType == null || paramType == EPTypeNull.INSTANCE || !((EPTypeClass) paramType).getType().isArray()) {
                transform.getBlock().methodReturn(ref("lv"));
            } else {
                EPTypeClass paramClass = (EPTypeClass) paramType;
                EPTypeClass componentType = JavaClassHelper.getArrayComponentType(paramClass);
                EPTypeClass mktype = MultiKeyPlanner.getMKClassForComponentType(componentType);
                transform.getBlock().methodReturn(newInstance(mktype, cast(paramClass, ref("lv"))));
            }
        } else {
            transform.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", cast(EPTypePremade.OBJECTARRAY.getEPType(), ref("lv")));
            CodegenExpression[] expressions = new CodegenExpression[multiKeyClassRef.getMKTypes().length];
            for (int i = 0; i < expressions.length; i++) {
                EPType type = multiKeyClassRef.getMKTypes()[i];
                expressions[i] = type == EPTypeNull.INSTANCE ? constantNull() : cast((EPTypeClass) type, arrayAtIndex(ref("values"), constant(i)));
            }
            transform.getBlock().methodReturn(newInstance(multiKeyClassRef.getClassNameMK(), expressions));
        }
        return transformer;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setScheduleCallbackId(int id) {
        this.scheduleCallbackId = id;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }
}
