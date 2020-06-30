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
package com.espertech.esper.common.internal.epl.expression.prev;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndex;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndex;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetter;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

/**
 * Represents the 'prev' previous event function in an expression node tree.
 */
public class ExprPreviousNode extends ExprNodeBase implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForgeInstrumentable {
    private final ExprPreviousNodePreviousType previousType;

    private EPTypeClass resultType;
    private int streamNumber;
    private Integer constantIndexNumber;
    private boolean isConstantIndex;
    private EventType enumerationMethodType;
    private CodegenFieldName previousStrategyFieldName;

    public ExprPreviousNode(ExprPreviousNodePreviousType previousType) {
        this.previousType = previousType;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public Integer getConstantIndexNumber() {
        return constantIndexNumber;
    }

    public boolean isConstantIndex() {
        return isConstantIndex;
    }

    public EPTypeClass getResultType() {
        return resultType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public EPTypeClass getEvaluationType() {
        return getResultType();
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if ((this.getChildNodes().length > 2) || (this.getChildNodes().length == 0)) {
            throw new ExprValidationException("Previous node must have 1 or 2 parameters");
        }

        // add constant of 1 for previous index
        if (this.getChildNodes().length == 1) {
            if (previousType == ExprPreviousNodePreviousType.PREV) {
                this.addChildNodeToFront(new ExprConstantNodeImpl(1));
            } else {
                this.addChildNodeToFront(new ExprConstantNodeImpl(0));
            }
        }

        // the row recognition patterns allows "prev(prop, index)", we switch index the first position
        if (ExprNodeUtilityQuery.isConstant(this.getChildNodes()[1])) {
            ExprNode first = this.getChildNodes()[0];
            ExprNode second = this.getChildNodes()[1];
            this.setChildNodes(second, first);
        }

        // Determine if the index is a constant value or an expression to evaluate
        ExprNode index = this.getChildNodes()[0];
        if (index.getForge().getForgeConstantType().isCompileTimeConstant()) {
            ExprNode constantNode = index;
            Object value = constantNode.getForge().getExprEvaluator().evaluate(null, false, null);
            if (!(value instanceof Number)) {
                throw new ExprValidationException("Previous function requires an integer index parameter or expression");
            }

            Number valueNumber = (Number) value;
            if (JavaClassHelper.isFloatingPointNumber(valueNumber)) {
                throw new ExprValidationException("Previous function requires an integer index parameter or expression");
            }

            constantIndexNumber = valueNumber.intValue();
            isConstantIndex = true;
        }

        // Determine stream number
        ExprNode value = this.getChildNodes()[1];
        if (value instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) this.getChildNodes()[1];
            streamNumber = identNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType((EPTypeClass) value.getForge().getEvaluationType());
        } else if (value instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode streamNode = (ExprStreamUnderlyingNode) value;
            streamNumber = streamNode.getStreamId();
            resultType = JavaClassHelper.getBoxedType((EPTypeClass) value.getForge().getEvaluationType());
            enumerationMethodType = validationContext.getStreamTypeService().getEventTypes()[streamNode.getStreamId()];
        } else {
            throw new ExprValidationException("Previous function requires an event property as parameter");
        }

        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            resultType = EPTypePremade.LONGBOXED.getEPType();
        }
        if (previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            resultType = JavaClassHelper.getArrayType(resultType);
        }

        if (validationContext.getViewResourceDelegate() == null) {
            throw new ExprValidationException("Previous function cannot be used in this context");
        }
        validationContext.getViewResourceDelegate().addPreviousRequest(this);
        previousStrategyFieldName = validationContext.getMemberNames().previousStrategy(streamNumber);
        return null;
    }

    public ExprPreviousNodePreviousType getPreviousType() {
        return previousType;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (previousType == ExprPreviousNodePreviousType.PREV ||
                previousType == ExprPreviousNodePreviousType.PREVTAIL ||
                previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            return constantNull();
        }
        if (previousType != ExprPreviousNodePreviousType.PREVWINDOW) {
            throw new IllegalStateException("Unrecognized previous type " + previousType);
        }

        CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), codegenClassScope);
        method.getBlock().declareVar(PreviousGetterStrategy.EPTYPE, "strategy", exprDotMethod(getterField(codegenClassScope), "getStrategy", exprSymbol.getAddExprEvalCtx(method)));

        method.getBlock().ifCondition(not(exprSymbol.getAddIsNewData(method))).blockReturn(constantNull());
        CodegenBlock randomAccess = method.getBlock().ifCondition(instanceOf(ref("strategy"), RandomAccessByIndexGetter.EPTYPE));
        {
            randomAccess
                    .declareVar(RandomAccessByIndexGetter.EPTYPE, "getter", cast(RandomAccessByIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(RandomAccessByIndex.EPTYPE, "randomAccess", exprDotMethod(ref("getter"), "getAccessor"))
                    .blockReturn(exprDotMethod(ref("randomAccess"), "getWindowCollectionReadOnly"));
        }
        CodegenBlock relativeAccess = randomAccess.ifElse();
        {
            relativeAccess
                    .declareVar(RelativeAccessByEventNIndexGetter.EPTYPE, "getter", cast(RelativeAccessByEventNIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(EventBean.EPTYPE, "evalEvent", arrayAtIndex(exprSymbol.getAddEPS(method), constant(streamNumber)))
                    .declareVar(RelativeAccessByEventNIndex.EPTYPE, "relativeAccess", exprDotMethod(ref("getter"), "getAccessor", ref("evalEvent")))
                    .ifRefNullReturnNull("relativeAccess")
                    .blockReturn(exprDotMethod(ref("relativeAccess"), "getWindowToEventCollReadOnly"));
        }
        return localMethod(method);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (previousType == ExprPreviousNodePreviousType.PREVWINDOW || previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
        method.getBlock()
                .ifCondition(not(exprSymbol.getAddIsNewData(method))).blockReturn(constantNull())
                .methodReturn(localMethod(getSubstituteCodegen(method, exprSymbol, codegenClassScope), exprSymbol.getAddEPS(method), exprSymbol.getAddExprEvalCtx(method)));
        return localMethod(method);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            return constantNull();
        }

        if (previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), codegenClassScope);

            method.getBlock()
                    .declareVar(PreviousGetterStrategy.EPTYPE, "strategy", exprDotMethod(getterField(codegenClassScope), "getStrategy", exprSymbol.getAddExprEvalCtx(method)))
                    .apply(new PreviousBlockGetSizeAndIterator(method, exprSymbol, streamNumber, ref("strategy")));

            CodegenExpressionRef eps = exprSymbol.getAddEPS(method);
            CodegenMethod innerEval = CodegenLegoMethodExpression.codegenExpression(this.getChildNodes()[1].getForge(), method, codegenClassScope);

            method.getBlock().declareVar(EventBean.EPTYPE, "originalEvent", arrayAtIndex(eps, constant(streamNumber)))
                    .declareVar(EPTypePremade.COLLECTION.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType(), ref("size")))
                    .forLoopIntSimple("i", ref("size"))
                    .assignArrayElement(eps, constant(streamNumber), cast(EventBean.EPTYPE, exprDotMethod(ref("events"), "next")))
                    .exprDotMethod(ref("result"), "add", localMethod(innerEval, eps, constantTrue(), exprSymbol.getAddExprEvalCtx(method)))
                    .blockEnd()
                    .assignArrayElement(eps, constant(streamNumber), ref("originalEvent"))
                    .methodReturn(ref("result"));
            return localMethod(method);
        }

        CodegenMethod method = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), this.getClass(), codegenClassScope);
        method.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "result", evaluateCodegenPrevAndTail(method, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(Collections.class, "singletonList", ref("result")));
        return localMethod(method);
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (previousType == ExprPreviousNodePreviousType.PREV || previousType == ExprPreviousNodePreviousType.PREVTAIL) {
            return null;
        }
        return enumerationMethodType;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (previousType == ExprPreviousNodePreviousType.PREV || previousType == ExprPreviousNodePreviousType.PREVTAIL) {
            return enumerationMethodType;
        }
        return null;
    }

    public EPTypeClass getComponentTypeCollection() throws ExprValidationException {
        if (resultType.getType().isArray()) {
            return JavaClassHelper.getArrayComponentType(resultType);
        }
        return resultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public CodegenExpression evaluateCodegenUninstrumented(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (previousType == ExprPreviousNodePreviousType.PREV || previousType == ExprPreviousNodePreviousType.PREVTAIL) {
            return evaluateCodegenPrevAndTail(codegenMethodScope, exprSymbol, codegenClassScope);
        }
        if (previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            return evaluateCodegenPrevWindow(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT) {
            return evaluateCodegenPrevCount(requiredType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
        throw new IllegalStateException("Unrecognized previous type " + previousType);
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprPrev", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).qparam(exprSymbol.getAddIsNewData(codegenMethodScope)).build();
    }

    private CodegenExpression evaluateCodegenPrevCount(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(resultType, this.getClass(), codegenClassScope);

        method.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "size", constant(0))
                .declareVar(PreviousGetterStrategy.EPTYPE, "strategy", exprDotMethod(getterField(codegenClassScope), "getStrategy", exprSymbol.getAddExprEvalCtx(method)));

        CodegenBlock randomAccess = method.getBlock().ifCondition(instanceOf(ref("strategy"), RandomAccessByIndexGetter.EPTYPE));
        {
            randomAccess
                    .ifCondition(not(exprSymbol.getAddIsNewData(method))).blockReturn(constantNull())
                    .declareVar(RandomAccessByIndexGetter.EPTYPE, "getter", cast(RandomAccessByIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(RandomAccessByIndex.EPTYPE, "randomAccess", exprDotMethod(ref("getter"), "getAccessor"))
                    .assignRef("size", exprDotMethod(ref("randomAccess"), "getWindowCount"));
        }
        CodegenBlock relativeAccess = randomAccess.ifElse();
        {
            relativeAccess
                    .declareVar(RelativeAccessByEventNIndexGetter.EPTYPE, "getter", cast(RelativeAccessByEventNIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(EventBean.EPTYPE, "evalEvent", arrayAtIndex(exprSymbol.getAddEPS(method), constant(streamNumber)))
                    .declareVar(RelativeAccessByEventNIndex.EPTYPE, "relativeAccess", exprDotMethod(ref("getter"), "getAccessor", ref("evalEvent")))
                    .ifRefNullReturnNull("relativeAccess")
                    .assignRef("size", exprDotMethod(ref("relativeAccess"), "getWindowToEventCount"));
        }

        method.getBlock().methodReturn(ref("size"));
        return localMethod(method);
    }

    private CodegenExpression evaluateCodegenPrevWindow(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(resultType, this.getClass(), codegenClassScope);

        method.getBlock()
                .declareVar(PreviousGetterStrategy.EPTYPE, "strategy", exprDotMethod(getterField(codegenClassScope), "getStrategy", exprSymbol.getAddExprEvalCtx(method)))
                .apply(new PreviousBlockGetSizeAndIterator(method, exprSymbol, streamNumber, ref("strategy")));

        CodegenExpressionRef eps = exprSymbol.getAddEPS(method);
        CodegenMethod innerEval = CodegenLegoMethodExpression.codegenExpression(this.getChildNodes()[1].getForge(), method, codegenClassScope);

        EPTypeClass componentType = JavaClassHelper.getArrayComponentType(resultType);
        method.getBlock().declareVar(EventBean.EPTYPE, "originalEvent", arrayAtIndex(eps, constant(streamNumber)))
                .declareVar(resultType, "result", newArrayByLength(componentType, ref("size")))
                .forLoopIntSimple("i", ref("size"))
                .assignArrayElement(eps, constant(streamNumber), cast(EventBean.EPTYPE, exprDotMethod(ref("events"), "next")))
                .assignArrayElement("result", ref("i"), localMethod(innerEval, eps, constantTrue(), exprSymbol.getAddExprEvalCtx(method)))
                .blockEnd()
                .assignArrayElement(eps, constant(streamNumber), ref("originalEvent"))
                .methodReturn(ref("result"));
        return localMethod(method);
    }

    private CodegenExpression evaluateCodegenPrevAndTail(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(resultType, this.getClass(), codegenClassScope);

        CodegenExpressionRef eps = exprSymbol.getAddEPS(method);
        CodegenMethod innerEval = CodegenLegoMethodExpression.codegenExpression(this.getChildNodes()[1].getForge(), method, codegenClassScope);

        method.getBlock()
                .ifCondition(not(exprSymbol.getAddIsNewData(method))).blockReturn(constantNull())
                .declareVar(EventBean.EPTYPE, "substituteEvent", localMethod(getSubstituteCodegen(method, exprSymbol, codegenClassScope), eps, exprSymbol.getAddExprEvalCtx(method)))
                .ifRefNullReturnNull("substituteEvent")
                .declareVar(EventBean.EPTYPE, "originalEvent", arrayAtIndex(eps, constant(streamNumber)))
                .assignArrayElement(eps, constant(streamNumber), ref("substituteEvent"))
                .declareVar(resultType, "evalResult", localMethod(innerEval, eps, exprSymbol.getAddIsNewData(method), exprSymbol.getAddExprEvalCtx(method)))
                .assignArrayElement(eps, constant(streamNumber), ref("originalEvent"))
                .methodReturn(ref("evalResult"));

        return localMethod(method);
    }

    private CodegenMethod getSubstituteCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChildWithScope(EventBean.EPTYPE, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, codegenClassScope)
                .addParam(EventBean.EPTYPEARRAY, REF_EPS.getRef()).addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef());

        method.getBlock().declareVar(PreviousGetterStrategy.EPTYPE, "strategy", exprDotMethod(getterField(codegenClassScope), "getStrategy", exprSymbol.getAddExprEvalCtx(method)));
        if (isConstantIndex) {
            method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index", constant(constantIndexNumber));
        } else {
            ExprForge index = this.getChildNodes()[0].getForge();
            CodegenMethod indexMethod = CodegenLegoMethodExpression.codegenExpression(index, method, codegenClassScope);
            CodegenExpression indexCall = localMethod(indexMethod, REF_EPS, constantTrue(), REF_EXPREVALCONTEXT);
            method.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "indexResult", indexCall)
                    .ifRefNullReturnNull("indexResult")
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index", exprDotMethod(cast(EPTypePremade.NUMBER.getEPType(), ref("indexResult")), "intValue"));
        }

        CodegenBlock randomAccess = method.getBlock().ifCondition(instanceOf(ref("strategy"), RandomAccessByIndexGetter.EPTYPE));
        {
            randomAccess
                    .declareVar(RandomAccessByIndexGetter.EPTYPE, "getter", cast(RandomAccessByIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(RandomAccessByIndex.EPTYPE, "randomAccess", exprDotMethod(ref("getter"), "getAccessor"));
            if (previousType == ExprPreviousNodePreviousType.PREV) {
                randomAccess.blockReturn(exprDotMethod(ref("randomAccess"), "getNewData", ref("index")));
            } else if (previousType == ExprPreviousNodePreviousType.PREVTAIL) {
                randomAccess.blockReturn(exprDotMethod(ref("randomAccess"), "getNewDataTail", ref("index")));
            } else {
                throw new IllegalStateException("Previous type not recognized: " + previousType);
            }
        }
        CodegenBlock relativeAccess = randomAccess.ifElse();
        {
            relativeAccess
                    .declareVar(RelativeAccessByEventNIndexGetter.EPTYPE, "getter", cast(RelativeAccessByEventNIndexGetter.EPTYPE, ref("strategy")))
                    .declareVar(EventBean.EPTYPE, "evalEvent", arrayAtIndex(exprSymbol.getAddEPS(method), constant(streamNumber)))
                    .declareVar(RelativeAccessByEventNIndex.EPTYPE, "relativeAccess", exprDotMethod(ref("getter"), "getAccessor", ref("evalEvent")))
                    .ifRefNullReturnNull("relativeAccess");
            if (previousType == ExprPreviousNodePreviousType.PREV) {
                relativeAccess.blockReturn(exprDotMethod(ref("relativeAccess"), "getRelativeToEvent", ref("evalEvent"), ref("index")));
            } else if (previousType == ExprPreviousNodePreviousType.PREVTAIL) {
                relativeAccess.blockReturn(exprDotMethod(ref("relativeAccess"), "getRelativeToEnd", ref("index")));
            } else {
                throw new IllegalStateException("Previous type not recognized: " + previousType);
            }
        }
        return method;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.append(previousType.toString().toLowerCase(Locale.ENGLISH));
        writer.append("(");
        if (previousType == ExprPreviousNodePreviousType.PREVCOUNT || previousType == ExprPreviousNodePreviousType.PREVWINDOW) {
            this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
        } else {
            this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
            if (this.getChildNodes().length > 1) {
                writer.append(",");
                this.getChildNodes()[1].toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
            }
        }
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (node == null || getClass() != node.getClass()) {
            return false;
        }

        ExprPreviousNode that = (ExprPreviousNode) node;

        if (previousType != that.previousType) {
            return false;
        }

        return true;
    }

    private CodegenExpression getterField(CodegenClassScope classScope) {
        return classScope.getPackageScope().addOrGetFieldWellKnown(previousStrategyFieldName, PreviousGetterStrategy.EPTYPE);
    }

    private static class PreviousBlockGetSizeAndIterator implements Consumer<CodegenBlock> {
        private final CodegenMethod method;
        private final ExprForgeCodegenSymbol exprSymbol;
        private final int streamNumber;
        private final CodegenExpression getter;

        public PreviousBlockGetSizeAndIterator(CodegenMethod method, ExprForgeCodegenSymbol exprSymbol, int streamNumber, CodegenExpression getter) {
            this.method = method;
            this.exprSymbol = exprSymbol;
            this.streamNumber = streamNumber;
            this.getter = getter;
        }

        public void accept(CodegenBlock block) {
            block
                    .ifCondition(not(exprSymbol.getAddIsNewData(method))).blockReturn(constantNull())
                    .declareVar(EPTypeClassParameterized.from(Iterator.class, EventBean.class), "events", constantNull())
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "size", constant(0));

            CodegenBlock randomAccess = method.getBlock().ifCondition(instanceOf(getter, RandomAccessByIndexGetter.EPTYPE));
            {
                randomAccess
                        .declareVar(RandomAccessByIndexGetter.EPTYPE, "getter", cast(RandomAccessByIndexGetter.EPTYPE, getter))
                        .declareVar(RandomAccessByIndex.EPTYPE, "randomAccess", exprDotMethod(ref("getter"), "getAccessor"))
                        .assignRef("events", exprDotMethod(ref("randomAccess"), "getWindowIterator"))
                        .assignRef("size", exprDotMethod(ref("randomAccess"), "getWindowCount"));
            }
            CodegenBlock relativeAccess = randomAccess.ifElse();
            {
                relativeAccess
                        .declareVar(RelativeAccessByEventNIndexGetter.EPTYPE, "getter", cast(RelativeAccessByEventNIndexGetter.EPTYPE, getter))
                        .declareVar(EventBean.EPTYPE, "evalEvent", arrayAtIndex(exprSymbol.getAddEPS(method), constant(streamNumber)))
                        .declareVar(RelativeAccessByEventNIndex.EPTYPE, "relativeAccess", exprDotMethod(ref("getter"), "getAccessor", ref("evalEvent")))
                        .ifRefNullReturnNull("relativeAccess")
                        .assignRef("events", exprDotMethod(ref("relativeAccess"), "getWindowToEvent"))
                        .assignRef("size", exprDotMethod(ref("relativeAccess"), "getWindowToEventCount"));
            }

            method.getBlock().ifCondition(relational(ref("size"), LE, constant(0)))
                    .blockReturn(constantNull());
        }
    }
}
