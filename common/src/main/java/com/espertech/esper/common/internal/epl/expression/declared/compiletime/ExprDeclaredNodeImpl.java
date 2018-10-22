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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDeclaredOrLambdaNode;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentVisitorWParent;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Expression instance as declared elsewhere.
 */
public class ExprDeclaredNodeImpl extends ExprNodeBase implements ExprDeclaredNode, ExprDeclaredOrLambdaNode, ExprFilterOptimizableNode, ExprNodeInnerNodeProvider, ExprConstantNode {
    private final ExpressionDeclItem prototypeWVisibility;
    private List<ExprNode> chainParameters;
    private transient ExprForge forge;
    private ExprNode expressionBodyCopy;

    public ExprDeclaredNodeImpl(ExpressionDeclItem prototype, List<ExprNode> chainParameters, ContextCompileTimeDescriptor contextDescriptor, ExprNode expressionBodyCopy) {
        this.prototypeWVisibility = prototype;
        this.chainParameters = chainParameters;
        this.expressionBodyCopy = expressionBodyCopy;

        // replace context-properties where they are currently identifiers
        if (contextDescriptor == null) {
            return;
        }
        ExprNodeIdentVisitorWParent visitorWParent = new ExprNodeIdentVisitorWParent();
        expressionBodyCopy.accept(visitorWParent);
        for (Pair<ExprNode, ExprIdentNode> pair : visitorWParent.getIdentNodes()) {
            String streamOrProp = pair.getSecond().getStreamOrPropertyName();
            if (streamOrProp != null && contextDescriptor.getContextPropertyRegistry().isContextPropertyPrefix(streamOrProp)) {
                ExprContextPropertyNodeImpl context = new ExprContextPropertyNodeImpl(pair.getSecond().getUnresolvedPropertyName());
                if (pair.getFirst() == null) {
                    this.expressionBodyCopy = context;
                } else {
                    ExprNodeUtilityModify.replaceChildNode(pair.getFirst(), pair.getSecond(), context);
                }
            }
        }
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode getBody() {
        return expressionBodyCopy;
    }

    public List<ExprNode> getAdditionalNodes() {
        return chainParameters;
    }

    public boolean validated() {
        return forge != null;
    }

    public Class getConstantType() {
        checkValidated(forge);
        return forge.getEvaluationType();
    }

    public Object getConstantValue() {
        return forge.getExprEvaluator().evaluate(null, true, null);
    }


    public LinkedHashMap<String, Integer> getOuterStreamNames(Map<String, Integer> outerStreamNames) throws ExprValidationException {
        checkParameterCount();
        ExpressionDeclItem prototype = prototypeWVisibility;

        // determine stream ids for each parameter
        LinkedHashMap<String, Integer> streamParameters = new LinkedHashMap<String, Integer>();
        for (int param = 0; param < chainParameters.size(); param++) {
            if (!(chainParameters.get(param) instanceof ExprIdentNode)) {
                throw new ExprValidationException("Sub-selects in an expression declaration require passing only stream names as parameters");
            }
            String parameterName = ((ExprIdentNode) chainParameters.get(param)).getUnresolvedPropertyName();
            Integer streamIdFound = outerStreamNames.get(parameterName);
            if (streamIdFound == null) {
                throw new ExprValidationException("Failed validation of expression declaration '" + prototype.getName() + "': Invalid parameter to expression declaration, parameter " + param + " is not the name of a stream in the query");
            }
            String prototypeName = prototype.getParametersNames()[param];
            streamParameters.put(prototypeName, streamIdFound);
        }
        return streamParameters;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        ExpressionDeclItem prototype = prototypeWVisibility;
        if (prototype.isAlias()) {
            try {
                expressionBodyCopy = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.ALIASEXPRBODY, expressionBodyCopy, validationContext);
            } catch (ExprValidationException ex) {
                String message = "Error validating expression alias '" + prototype.getName() + "': " + ex.getMessage();
                throw new ExprValidationException(message, ex);
            }

            forge = expressionBodyCopy.getForge();
            return null;
        }

        if (forge != null) {
            return null; // already evaluated
        }

        if (this.getChildNodes().length > 0) {
            throw new IllegalStateException("Execution node has its own child nodes");
        }

        // validate chain
        List<ExprNode> validated = new ArrayList<ExprNode>();
        for (ExprNode expr : chainParameters) {
            validated.add(ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.DECLAREDEXPRPARAM, expr, validationContext));
        }
        chainParameters = validated;

        // validate parameter count
        checkParameterCount();

        // create context for expression body
        EventType[] eventTypes = new EventType[prototype.getParametersNames().length];
        String[] streamNames = new String[prototype.getParametersNames().length];
        boolean[] isIStreamOnly = new boolean[prototype.getParametersNames().length];
        int[] streamsIdsPerStream = new int[prototype.getParametersNames().length];
        boolean allStreamIdsMatch = true;

        for (int i = 0; i < prototype.getParametersNames().length; i++) {
            ExprNode parameter = chainParameters.get(i);
            streamNames[i] = prototype.getParametersNames()[i];

            if (parameter instanceof ExprStreamUnderlyingNode) {
                ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) parameter;
                eventTypes[i] = validationContext.getStreamTypeService().getEventTypes()[und.getStreamId()];
                isIStreamOnly[i] = validationContext.getStreamTypeService().getIStreamOnly()[und.getStreamId()];
                streamsIdsPerStream[i] = und.getStreamId();
            } else if (parameter instanceof ExprWildcard) {
                if (validationContext.getStreamTypeService().getEventTypes().length != 1) {
                    throw new ExprValidationException("Expression '" + prototype.getName() + "' only allows a wildcard parameter if there is a single stream available, please use a stream or tag name instead");
                }
                eventTypes[i] = validationContext.getStreamTypeService().getEventTypes()[0];
                isIStreamOnly[i] = validationContext.getStreamTypeService().getIStreamOnly()[0];
                streamsIdsPerStream[i] = 0;
            } else {
                throw new ExprValidationException("Expression '" + prototype.getName() + "' requires a stream name as a parameter");
            }

            if (streamsIdsPerStream[i] != i) {
                allStreamIdsMatch = false;
            }
        }

        StreamTypeService streamTypeService = validationContext.getStreamTypeService();
        StreamTypeServiceImpl copyTypes = new StreamTypeServiceImpl(eventTypes, streamNames, isIStreamOnly, streamTypeService.isOnDemandStreams(), streamTypeService.isOptionalStreams());
        copyTypes.setRequireStreamNames(true);

        // validate expression body in this context
        try {
            ExprValidationContext expressionBodyContext = new ExprValidationContext(copyTypes, validationContext);
            expressionBodyCopy = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.DECLAREDEXPRBODY, expressionBodyCopy, expressionBodyContext);
        } catch (ExprValidationException ex) {
            String message = "Error validating expression declaration '" + prototype.getName() + "': " + ex.getMessage();
            throw new ExprValidationException(message, ex);
        }

        // analyze child node
        ExprNodeSummaryVisitor summaryVisitor = new ExprNodeSummaryVisitor();
        expressionBodyCopy.accept(summaryVisitor);
        boolean isCache = !(summaryVisitor.isHasAggregation() || summaryVisitor.isHasPreviousPrior());
        isCache &= validationContext.getStatementCompileTimeService().getConfiguration().getCompiler().getExecution().isEnabledDeclaredExprValueCache();

        // determine a suitable evaluation
        boolean audit = AuditEnum.EXPRDEF.getAudit(validationContext.getAnnotations()) != null;
        String statementName = validationContext.getStatementName();
        if (expressionBodyCopy.getForge().getForgeConstantType().isConstant()) {
            // pre-evaluated
            forge = new ExprDeclaredForgeConstant(this, expressionBodyCopy.getForge().getEvaluationType(), prototype, expressionBodyCopy.getForge().getExprEvaluator().evaluate(null, true, null), audit, statementName);
        } else if (prototype.getParametersNames().length == 0 ||
                (allStreamIdsMatch && prototype.getParametersNames().length == streamTypeService.getEventTypes().length)) {
            forge = new ExprDeclaredForgeNoRewrite(this, expressionBodyCopy.getForge(), isCache, audit, statementName);
        } else {
            forge = new ExprDeclaredForgeRewrite(this, expressionBodyCopy.getForge(), isCache, streamsIdsPerStream, audit, statementName);
        }
        return null;
    }

    public boolean getFilterLookupEligible() {
        return true;
    }

    public ExprFilterSpecLookupableForge getFilterLookupable() {
        if (!(forge instanceof ExprDeclaredForgeBase)) {
            return null;
        }
        ExprDeclaredForgeBase declaredForge = (ExprDeclaredForgeBase) forge;
        ExprForge forge = declaredForge.getInnerForge();
        return new ExprFilterSpecLookupableForge(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(this), new DeclaredNodeEventPropertyGetterForge(forge), forge.getEvaluationType(), true);
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprDeclaredNodeImpl)) {
            return false;
        }

        ExprDeclaredNodeImpl otherExprCaseNode = (ExprDeclaredNodeImpl) node;
        return ExprNodeUtilityCompare.deepEquals(expressionBodyCopy, otherExprCaseNode, false);
    }

    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        if (this.getChildNodes().length == 0) {
            expressionBodyCopy.accept(visitor);
        }
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        if (this.getChildNodes().length == 0) {
            expressionBodyCopy.accept(visitor);
        }
    }

    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        if (visitor.isVisit(this) && this.getChildNodes().length == 0) {
            expressionBodyCopy.accept(visitor);
        }
    }

    public ExprNode getExpressionBodyCopy() {
        return expressionBodyCopy;
    }

    public ExpressionDeclItem getPrototype() {
        return prototypeWVisibility;
    }

    public ExpressionDeclItem getPrototypeWVisibility() {
        return prototypeWVisibility;
    }

    public List<ExprNode> getChainParameters() {
        return chainParameters;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    private void checkParameterCount() throws ExprValidationException {
        ExpressionDeclItem prototype = prototypeWVisibility;
        if (chainParameters.size() != prototype.getParametersNames().length) {
            throw new ExprValidationException("Parameter count mismatches for declared expression '" + prototype.getName() + "', expected " +
                    prototype.getParametersNames().length + " parameters but received " + chainParameters.size() + " parameters");
        }
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExpressionDeclItem prototype = prototypeWVisibility;
        writer.append(prototype.getName());

        if (prototype.isAlias()) {
            return;
        }

        writer.append("(");
        String delimiter = "";
        for (ExprNode parameter : chainParameters) {
            writer.append(delimiter);
            parameter.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
        writer.append(")");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    private final static class DeclaredNodeEventPropertyGetterForge implements EventPropertyValueGetterForge {
        private final ExprForge exprForge;

        public DeclaredNodeEventPropertyGetterForge(ExprForge exprForge) {
            this.exprForge = exprForge;
        }

        public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
            CodegenMethod method = parent.makeChild(exprForge.getEvaluationType(), this.getClass(), codegenClassScope).addParam(EventBean.class, "bean");
            CodegenMethod exprMethod = CodegenLegoMethodExpression.codegenExpression(exprForge, method, codegenClassScope);

            method.getBlock()
                    .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(1)))
                    .assignArrayElement(ref("events"), constant(0), ref("bean"))
                    .methodReturn(localMethod(exprMethod, ref("events"), constantTrue(), constantNull()));

            return localMethod(method, beanExpression);
        }
    }
}