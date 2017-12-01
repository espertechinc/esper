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

import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the case-when-then-else control flow function is an expression tree.
 */
public class ExprCaseNode extends ExprNodeBase {
    private static final long serialVersionUID = 792538321520346459L;

    private final boolean isCase2;

    private transient ExprCaseNodeForge forge;

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
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
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

        for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
            if (!isCase2) {
                Class returnType = pair.getFirst().getForge().getEvaluationType();
                if (returnType != boolean.class && returnType != Boolean.class) {
                    throw new ExprValidationException("Case node 'when' expressions must return a boolean value");
                }
            }
        }

        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        if (isCase2) {
            // validate we can compare result types
            List<Class> comparedTypes = new LinkedList<>();
            comparedTypes.add(analysis.getOptionalCompareExprNode().getForge().getEvaluationType());
            for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
                comparedTypes.add(pair.getFirst().getForge().getEvaluationType());
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

        // Determine type of each result (then-node and else node) child node expression
        List<Class> childTypes = new LinkedList<>();
        List<LinkedHashMap<String, Object>> childMapTypes = new LinkedList<>();
        for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
            if (pair.getSecond().getForge() instanceof ExprTypableReturnForge) {
                ExprTypableReturnForge typableReturn = (ExprTypableReturnForge) pair.getSecond().getForge();
                LinkedHashMap<String, Object> rowProps = typableReturn.getRowProperties();
                if (rowProps != null) {
                    childMapTypes.add(rowProps);
                    continue;
                }
            }
            childTypes.add(pair.getSecond().getForge().getEvaluationType());

        }
        if (analysis.getOptionalElseExprNode() != null) {
            if (analysis.getOptionalElseExprNode().getForge() instanceof ExprTypableReturnForge) {
                ExprTypableReturnForge typableReturn = (ExprTypableReturnForge) analysis.getOptionalElseExprNode().getForge();
                LinkedHashMap<String, Object> rowProps = typableReturn.getRowProperties();
                if (rowProps != null) {
                    childMapTypes.add(rowProps);
                } else {
                    childTypes.add(analysis.getOptionalElseExprNode().getForge().getEvaluationType());
                }
            } else {
                childTypes.add(analysis.getOptionalElseExprNode().getForge().getEvaluationType());
            }
        }

        if (!childMapTypes.isEmpty() && !childTypes.isEmpty()) {
            String message = "Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value";
            String check;
            int count = -1;
            for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
                count++;
                if (pair.getSecond().getForge().getEvaluationType() != Map.class && pair.getSecond().getForge().getEvaluationType() != null) {
                    check = ", check when-condition number " + count;
                    throw new ExprValidationException(message + check);
                }
            }
            if (analysis.getOptionalElseExprNode() != null) {
                if (analysis.getOptionalElseExprNode().getForge().getEvaluationType() != Map.class && analysis.getOptionalElseExprNode().getForge().getEvaluationType() != null) {
                    check = ", check the else-condition";
                    throw new ExprValidationException(message + check);
                }
            }
            throw new ExprValidationException(message);
        }

        LinkedHashMap<String, Object> mapResultType = null;
        Class resultType = null;
        boolean isNumericResult = false;
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
            resultType = Map.class;
            mapResultType = childMapTypes.get(0);
            for (int i = 1; i < childMapTypes.size(); i++) {
                Map<String, Object> other = childMapTypes.get(i);
                String messageEquals = MapEventType.isDeepEqualsProperties("Case-when number " + i, mapResultType, other);
                if (messageEquals != null) {
                    throw new ExprValidationException("Incompatible case-when return types by new-operator in case-when number " + i + ": " + messageEquals);
                }
            }
        }

        forge = new ExprCaseNodeForge(this, resultType, mapResultType, isNumericResult, mustCoerce, coercer, analysis.whenThenNodeList, analysis.optionalCompareExprNode, analysis.optionalElseExprNode);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
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

        List<UniformPair<ExprNode>> whenThenNodeList = new LinkedList<>();
        int numWhenThen = children.length >> 1;
        for (int i = 0; i < numWhenThen; i++) {
            ExprNode whenExpr = children[i << 1];
            ExprNode thenExpr = children[(i << 1) + 1];
            whenThenNodeList.add(new UniformPair<>(whenExpr, thenExpr));
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

        List<UniformPair<ExprNode>> whenThenNodeList = new LinkedList<>();
        int numWhenThen = (children.length - 1) / 2;
        for (int i = 0; i < numWhenThen; i++) {
            whenThenNodeList.add(new UniformPair<>(children[i * 2 + 1], children[i * 2 + 2]));
        }
        ExprNode optionalElseExprNode = null;
        if (numWhenThen * 2 + 1 < children.length) {
            optionalElseExprNode = children[children.length - 1];
        }
        return new CaseAnalysis(whenThenNodeList, optionalCompareExprNode, optionalElseExprNode);
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


