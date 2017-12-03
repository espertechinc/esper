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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.enummethod.dot.ExprDeclaredOrLambdaNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentVisitorWParent;
import com.espertech.esper.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.util.SerializableObjectCopier;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expression instance as declared elsewhere.
 */
public class ExprDeclaredNodeImpl extends ExprNodeBase implements ExprDeclaredNode, ExprDeclaredOrLambdaNode, ExprFilterOptimizableNode, ExprNodeInnerNodeProvider, ExprConstantNode {
    private static final long serialVersionUID = 9140100131374697808L;

    private final ExpressionDeclItem prototype;
    private List<ExprNode> chainParameters;
    private transient ExprForge forge;
    private ExprNode expressionBodyCopy;

    public ExprDeclaredNodeImpl(ExpressionDeclItem prototype, List<ExprNode> chainParameters, ContextDescriptor contextDescriptor) {
        this.prototype = prototype;
        this.chainParameters = chainParameters;

        // copy expression - we do it at this time and not later
        try {
            expressionBodyCopy = (ExprNode) SerializableObjectCopier.copy(prototype.getInner());
        } catch (Exception e) {
            throw new RuntimeException("Internal error providing expression tree: " + e.getMessage(), e);
        }

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
                    expressionBodyCopy = context;
                } else {
                    ExprNodeUtilityCore.replaceChildNode(pair.getFirst(), pair.getSecond(), context);
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

    public Object getConstantValue(ExprEvaluatorContext context) {
        return forge.getExprEvaluator().evaluate(null, true, context);
    }

    public boolean isConstantValue() {
        return expressionBodyCopy.isConstantResult();
    }

    public LinkedHashMap<String, Integer> getOuterStreamNames(Map<String, Integer> outerStreamNames) throws ExprValidationException {
        checkParameterCount();

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
            String prototypeName = prototype.getParametersNames().get(param);
            streamParameters.put(prototypeName, streamIdFound);
        }
        return streamParameters;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (prototype.isAlias()) {
            try {
                expressionBodyCopy = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.ALIASEXPRBODY, expressionBodyCopy, validationContext);
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
            validated.add(ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.DECLAREDEXPRPARAM, expr, validationContext));
        }
        chainParameters = validated;

        // validate parameter count
        checkParameterCount();

        // create context for expression body
        EventType[] eventTypes = new EventType[prototype.getParametersNames().size()];
        String[] streamNames = new String[prototype.getParametersNames().size()];
        boolean[] isIStreamOnly = new boolean[prototype.getParametersNames().size()];
        int[] streamsIdsPerStream = new int[prototype.getParametersNames().size()];
        boolean allStreamIdsMatch = true;

        for (int i = 0; i < prototype.getParametersNames().size(); i++) {
            ExprNode parameter = chainParameters.get(i);
            streamNames[i] = prototype.getParametersNames().get(i);

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
        StreamTypeServiceImpl copyTypes = new StreamTypeServiceImpl(eventTypes, streamNames, isIStreamOnly, streamTypeService.getEngineURIQualifier(), streamTypeService.isOnDemandStreams(), streamTypeService.isOptionalStreams());
        copyTypes.setRequireStreamNames(true);

        // validate expression body in this context
        try {
            ExprValidationContext expressionBodyContext = new ExprValidationContext(copyTypes, validationContext);
            expressionBodyCopy = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.DECLAREDEXPRBODY, expressionBodyCopy, expressionBodyContext);
        } catch (ExprValidationException ex) {
            String message = "Error validating expression declaration '" + prototype.getName() + "': " + ex.getMessage();
            throw new ExprValidationException(message, ex);
        }

        // analyze child node
        ExprNodeSummaryVisitor summaryVisitor = new ExprNodeSummaryVisitor();
        expressionBodyCopy.accept(summaryVisitor);
        boolean isCache = !(summaryVisitor.isHasAggregation() || summaryVisitor.isHasPreviousPrior());
        isCache &= validationContext.getExprEvaluatorContext().getExpressionResultCacheService().isDeclaredExprCacheEnabled();

        // determine a suitable evaluation
        boolean audit = AuditEnum.EXPRDEF.getAudit(validationContext.getAnnotations()) != null;
        String engineURI = validationContext.getStreamTypeService().getEngineURIQualifier();
        String statementName = validationContext.getStatementName();
        if (expressionBodyCopy.isConstantResult()) {
            // pre-evaluated
            forge = new ExprDeclaredForgeConstant(this, expressionBodyCopy.getForge().getEvaluationType(), prototype, expressionBodyCopy.getForge().getExprEvaluator().evaluate(null, true, null), audit, engineURI, statementName);
        } else if (prototype.getParametersNames().isEmpty() ||
                (allStreamIdsMatch && prototype.getParametersNames().size() == streamTypeService.getEventTypes().length)) {
            forge = new ExprDeclaredForgeNoRewrite(this, expressionBodyCopy.getForge(), isCache, audit, engineURI, statementName);
        } else {
            forge = new ExprDeclaredForgeRewrite(this, expressionBodyCopy.getForge(), isCache, streamsIdsPerStream, audit, engineURI, statementName);
        }
        return null;
    }

    public boolean getFilterLookupEligible() {
        return true;
    }

    public ExprFilterSpecLookupable getFilterLookupable() {
        if (!(forge instanceof ExprDeclaredForgeBase)) {
            return null;
        }
        ExprDeclaredForgeBase declaredForge = (ExprDeclaredForgeBase) forge;
        ExprEvaluator evaluator = declaredForge.getInnerForge().getExprEvaluator();
        return new ExprFilterSpecLookupable(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(this), new DeclaredNodeEventPropertyGetter(evaluator), forge.getEvaluationType(), true);
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprDeclaredNodeImpl)) {
            return false;
        }

        ExprDeclaredNodeImpl otherExprCaseNode = (ExprDeclaredNodeImpl) node;
        return ExprNodeUtilityCore.deepEquals(expressionBodyCopy, otherExprCaseNode, false);
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
        return prototype;
    }

    public List<ExprNode> getChainParameters() {
        return chainParameters;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    private void checkParameterCount() throws ExprValidationException {
        if (chainParameters.size() != prototype.getParametersNames().size()) {
            throw new ExprValidationException("Parameter count mismatches for declared expression '" + prototype.getName() + "', expected " +
                    prototype.getParametersNames().size() + " parameters but received " + chainParameters.size() + " parameters");
        }
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
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

    private final static class DeclaredNodeEventPropertyGetter implements EventPropertyGetter {
        private final ExprEvaluator exprEvaluator;

        private DeclaredNodeEventPropertyGetter(ExprEvaluator exprEvaluator) {
            this.exprEvaluator = exprEvaluator;
        }

        public Object get(EventBean eventBean) throws PropertyAccessException {
            EventBean[] events = new EventBean[1];
            events[0] = eventBean;
            return exprEvaluator.evaluate(events, true, null);
        }

        public boolean isExistsProperty(EventBean eventBean) {
            return false;
        }

        public Object getFragment(EventBean eventBean) throws PropertyAccessException {
            return null;
        }
    }
}