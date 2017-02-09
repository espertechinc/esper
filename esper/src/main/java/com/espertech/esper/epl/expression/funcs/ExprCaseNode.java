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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Represents the case-when-then-else control flow function is an expression tree.
 */
public class ExprCaseNode extends ExprNodeBase implements ExprEvaluator, ExprEvaluatorTypableReturn {
    private static final long serialVersionUID = 792538321520346459L;

    private final boolean isCase2;
    private Class resultType;
    private transient LinkedHashMap<String, Object> mapResultType;
    private boolean isNumericResult;
    private boolean mustCoerce;

    private transient SimpleNumberCoercer coercer;
    private transient List<UniformPair<ExprEvaluator>> whenThenNodeList;
    private transient ExprEvaluator optionalCompareExprNode;
    private transient ExprEvaluator optionalElseExprNode;

    /**
     * Ctor.
     *
     * @param isCase2 is an indicator of which Case statement we are working on.
     *                <p> True indicates a 'Case2' statement with syntax "case a when a1 then b1 else b2".
     *                <p> False indicates a 'Case1' statement with syntax "case when a=a1 then b1 else b2".
     */
    public ExprCaseNode(boolean isCase2) {
        this.isCase2 = isCase2;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    /**
     * Returns true if this is a switch-type case.
     *
     * @return true for switch-type case, or false for when-then type
     */
    public boolean isCase2() {
        return isCase2;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        CaseAnalysis analysis = analyzeCase();

        whenThenNodeList = new ArrayList<UniformPair<ExprEvaluator>>();
        for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
            if (!isCase2) {
                if (pair.getFirst().getExprEvaluator().getType() != Boolean.class) {
                    throw new ExprValidationException("Case node 'when' expressions must return a boolean value");
                }
            }
            whenThenNodeList.add(new UniformPair<ExprEvaluator>(pair.getFirst().getExprEvaluator(), pair.getSecond().getExprEvaluator()));
        }
        if (analysis.getOptionalCompareExprNode() != null) {
            optionalCompareExprNode = analysis.getOptionalCompareExprNode().getExprEvaluator();
        }
        if (analysis.getOptionalElseExprNode() != null) {
            optionalElseExprNode = analysis.getOptionalElseExprNode().getExprEvaluator();
        }

        if (isCase2) {
            validateCaseTwo();
        }

        // Determine type of each result (then-node and else node) child node expression
        List<Class> childTypes = new LinkedList<Class>();
        List<LinkedHashMap<String, Object>> childMapTypes = new LinkedList<LinkedHashMap<String, Object>>();
        for (UniformPair<ExprEvaluator> pair : whenThenNodeList) {
            if (pair.getSecond() instanceof ExprEvaluatorTypableReturn) {
                ExprEvaluatorTypableReturn typableReturn = (ExprEvaluatorTypableReturn) pair.getSecond();
                LinkedHashMap<String, Object> rowProps = typableReturn.getRowProperties();
                if (rowProps != null) {
                    childMapTypes.add(rowProps);
                    continue;
                }
            }
            childTypes.add(pair.getSecond().getType());

        }
        if (optionalElseExprNode != null) {
            if (optionalElseExprNode instanceof ExprEvaluatorTypableReturn) {
                ExprEvaluatorTypableReturn typableReturn = (ExprEvaluatorTypableReturn) optionalElseExprNode;
                LinkedHashMap<String, Object> rowProps = typableReturn.getRowProperties();
                if (rowProps != null) {
                    childMapTypes.add(rowProps);
                } else {
                    childTypes.add(optionalElseExprNode.getType());
                }
            } else {
                childTypes.add(optionalElseExprNode.getType());
            }
        }

        if (!childMapTypes.isEmpty() && !childTypes.isEmpty()) {
            String message = "Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value";
            String check;
            int count = -1;
            for (UniformPair<ExprEvaluator> pair : whenThenNodeList) {
                count++;
                if (pair.getSecond().getType() != Map.class && pair.getSecond().getType() != null) {
                    check = ", check when-condition number " + count;
                    throw new ExprValidationException(message + check);
                }
            }
            if (optionalElseExprNode != null) {
                if (optionalElseExprNode.getType() != Map.class && optionalElseExprNode.getType() != null) {
                    check = ", check the else-condition";
                    throw new ExprValidationException(message + check);
                }
            }
            throw new ExprValidationException(message);
        }

        if (childMapTypes.isEmpty()) {
            // Determine common denominator type
            try {
                resultType = JavaClassHelper.getCommonCoercionType(childTypes.toArray(new Class[childTypes.size()]));
                if (JavaClassHelper.isNumeric(resultType)) {
                    isNumericResult = true;
                }
            } catch (CoercionException ex) {
                throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
            }
        } else {
            mapResultType = childMapTypes.get(0);
            for (int i = 1; i < childMapTypes.size(); i++) {
                Map<String, Object> other = childMapTypes.get(i);
                String messageEquals = MapEventType.isDeepEqualsProperties("Case-when number " + i, mapResultType, other);
                if (messageEquals != null) {
                    throw new ExprValidationException("Incompatible case-when return types by new-operator in case-when number " + i + ": " + messageEquals);
                }
            }
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return resultType;
    }

    public LinkedHashMap<String, Object> getRowProperties() throws ExprValidationException {
        return mapResultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCase(this);
            Object result;
            if (!isCase2) {
                result = evaluateCaseSyntax1(eventsPerStream, isNewData, exprEvaluatorContext);
            } else {
                result = evaluateCaseSyntax2(eventsPerStream, isNewData, exprEvaluatorContext);
            }
            InstrumentationHelper.get().aExprCase(result);
            return result;
        }

        if (!isCase2) {
            return evaluateCaseSyntax1(eventsPerStream, isNewData, exprEvaluatorContext);
        } else {
            return evaluateCaseSyntax2(eventsPerStream, isNewData, exprEvaluatorContext);
        }
    }

    public Boolean isMultirow() {
        return mapResultType == null ? null : false;
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> map = (Map<String, Object>) evaluate(eventsPerStream, isNewData, context);
        Object[] row = new Object[map.size()];
        int index = -1;
        for (Map.Entry<String, Object> entry : mapResultType.entrySet()) {
            index++;
            row[index] = map.get(entry.getKey());
        }
        return row;
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;    // always single-row
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprCaseNode)) {
            return false;
        }

        ExprCaseNode otherExprCaseNode = (ExprCaseNode) node;
        return this.isCase2 == otherExprCaseNode.isCase2;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        CaseAnalysis analysis;
        try {
            analysis = analyzeCase();
        } catch (ExprValidationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        writer.append("case");
        if (isCase2) {
            writer.append(' ');
            analysis.getOptionalCompareExprNode().toEPL(writer, getPrecedence());
        }
        for (UniformPair<ExprNode> p : analysis.getWhenThenNodeList()) {
            writer.append(" when ");
            p.getFirst().toEPL(writer, getPrecedence());
            writer.append(" then ");
            p.getSecond().toEPL(writer, getPrecedence());
        }
        if (analysis.getOptionalElseExprNode() != null) {
            writer.append(" else ");
            analysis.getOptionalElseExprNode().toEPL(writer, getPrecedence());
        }
        writer.append(" end");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.CASE;
    }

    private CaseAnalysis analyzeCaseOne() throws ExprValidationException {
        // Case 1 expression example:
        //      case when a=b then x [when c=d then y...] [else y]
        //
        ExprNode[] children = this.getChildNodes();
        if (children.length < 2) {
            throw new ExprValidationException("Case node must have at least 2 parameters");
        }

        List<UniformPair<ExprNode>> whenThenNodeList = new LinkedList<UniformPair<ExprNode>>();
        int numWhenThen = children.length >> 1;
        for (int i = 0; i < numWhenThen; i++) {
            ExprNode whenExpr = children[i << 1];
            ExprNode thenExpr = children[(i << 1) + 1];
            whenThenNodeList.add(new UniformPair<ExprNode>(whenExpr, thenExpr));
        }
        ExprNode optionalElseExprNode = null;
        if (children.length % 2 != 0) {
            optionalElseExprNode = children[children.length - 1];
        }
        return new CaseAnalysis(whenThenNodeList, null, optionalElseExprNode);
    }

    private CaseAnalysis analyzeCaseTwo() throws ExprValidationException {
        // Case 2 expression example:
        //      case p when p1 then x [when p2 then y...] [else z]
        //
        ExprNode[] children = this.getChildNodes();
        if (children.length < 3) {
            throw new ExprValidationException("Case node must have at least 3 parameters");
        }

        ExprNode optionalCompareExprNode = children[0];

        List<UniformPair<ExprNode>> whenThenNodeList = new LinkedList<UniformPair<ExprNode>>();
        int numWhenThen = (children.length - 1) / 2;
        for (int i = 0; i < numWhenThen; i++) {
            whenThenNodeList.add(new UniformPair<ExprNode>(children[i * 2 + 1], children[i * 2 + 2]));
        }
        ExprNode optionalElseExprNode = null;
        if (numWhenThen * 2 + 1 < children.length) {
            optionalElseExprNode = children[children.length - 1];
        }
        return new CaseAnalysis(whenThenNodeList, optionalCompareExprNode, optionalElseExprNode);
    }

    private void validateCaseTwo() throws ExprValidationException {
        // validate we can compare result types
        List<Class> comparedTypes = new LinkedList<Class>();
        comparedTypes.add(optionalCompareExprNode.getType());
        for (UniformPair<ExprEvaluator> pair : whenThenNodeList) {
            comparedTypes.add(pair.getFirst().getType());
        }

        // Determine common denominator type
        try {
            Class coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new Class[comparedTypes.size()]));

            // Determine if we need to coerce numbers when one type doesn't match any other type
            if (JavaClassHelper.isNumeric(coercionType)) {
                mustCoerce = false;
                for (Class comparedType : comparedTypes) {
                    if (comparedType != coercionType) {
                        mustCoerce = true;
                    }
                }
                if (mustCoerce) {
                    coercer = SimpleNumberCoercerFactory.getCoercer(null, coercionType);
                }
            }
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }
    }

    private Object evaluateCaseSyntax1(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Case 1 expression example:
        //      case when a=b then x [when c=d then y...] [else y]

        Object caseResult = null;
        boolean matched = false;
        for (UniformPair<ExprEvaluator> p : whenThenNodeList) {
            Boolean whenResult = (Boolean) p.getFirst().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            // If the 'when'-expression returns true
            if ((whenResult != null) && whenResult) {
                caseResult = p.getSecond().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                matched = true;
                break;
            }
        }

        if ((!matched) && (optionalElseExprNode != null)) {
            caseResult = optionalElseExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        if (caseResult == null) {
            return null;
        }

        if ((caseResult.getClass() != resultType) && isNumericResult) {
            return JavaClassHelper.coerceBoxed((Number) caseResult, resultType);
        }
        return caseResult;
    }

    private Object evaluateCaseSyntax2(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Case 2 expression example:
        //      case p when p1 then x [when p2 then y...] [else z]

        Object checkResult = optionalCompareExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        Object caseResult = null;
        boolean matched = false;
        for (UniformPair<ExprEvaluator> p : whenThenNodeList) {
            Object whenResult = p.getFirst().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (compare(checkResult, whenResult)) {
                caseResult = p.getSecond().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                matched = true;
                break;
            }
        }

        if ((!matched) && (optionalElseExprNode != null)) {
            caseResult = optionalElseExprNode.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        if (caseResult == null) {
            return null;
        }

        if ((caseResult.getClass() != resultType) && isNumericResult) {
            return JavaClassHelper.coerceBoxed((Number) caseResult, resultType);
        }
        return caseResult;
    }

    private boolean compare(Object leftResult, Object rightResult) {
        if (leftResult == null) {
            return rightResult == null;
        }
        if (rightResult == null) {
            return false;
        }

        if (!mustCoerce) {
            return leftResult.equals(rightResult);
        } else {
            Number left = coercer.coerceBoxed((Number) leftResult);
            Number right = coercer.coerceBoxed((Number) rightResult);
            return left.equals(right);
        }
    }

    private CaseAnalysis analyzeCase() throws ExprValidationException {
        if (isCase2) {
            return analyzeCaseTwo();
        } else {
            return analyzeCaseOne();
        }
    }

    public static class CaseAnalysis {
        private List<UniformPair<ExprNode>> whenThenNodeList;
        private ExprNode optionalCompareExprNode;
        private ExprNode optionalElseExprNode;

        public CaseAnalysis(List<UniformPair<ExprNode>> whenThenNodeList, ExprNode optionalCompareExprNode, ExprNode optionalElseExprNode) {
            this.whenThenNodeList = whenThenNodeList;
            this.optionalCompareExprNode = optionalCompareExprNode;
            this.optionalElseExprNode = optionalElseExprNode;
        }

        public List<UniformPair<ExprNode>> getWhenThenNodeList() {
            return whenThenNodeList;
        }

        public ExprNode getOptionalCompareExprNode() {
            return optionalCompareExprNode;
        }

        public ExprNode getOptionalElseExprNode() {
            return optionalElseExprNode;
        }
    }
}


