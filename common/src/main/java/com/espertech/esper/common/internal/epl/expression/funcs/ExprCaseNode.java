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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.CoercionException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeBoolean;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeOrNull;

/**
 * Represents the case-when-then-else control flow function is an expression tree.
 */
public class ExprCaseNode extends ExprNodeBase {
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
                if (!isTypeBoolean(pair.getFirst().getForge().getEvaluationType())) {
                    throw new ExprValidationException("Case node 'when' expressions must return a boolean value");
                }
            }
        }

        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        if (isCase2) {
            // validate we can compare result types
            List<EPType> comparedTypes = new LinkedList<>();
            EPType epType = analysis.getOptionalCompareExprNode().getForge().getEvaluationType();
            comparedTypes.add(epType);
            for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
                EPType pairType = pair.getFirst().getForge().getEvaluationType();
                comparedTypes.add(pairType);
            }

            // Determine common denominator type
            try {
                EPType coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new EPType[comparedTypes.size()]));

                // Determine if we need to coerce numbers when one type doesn't match any other type
                if (coercionType != EPTypeNull.INSTANCE) {
                    EPTypeClass coercionClass = (EPTypeClass) coercionType;
                    if (JavaClassHelper.isNumeric(coercionClass)) {
                        for (EPType comparedType : comparedTypes) {
                            if (!(comparedType.equals(coercionType))) {
                                mustCoerce = true;
                                break;
                            }
                        }
                        if (mustCoerce) {
                            coercer = SimpleNumberCoercerFactory.getCoercer(null, coercionClass);
                        }
                    }
                }
            } catch (CoercionException ex) {
                throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
            }
        }

        // Determine type of each result (then-node and else node) child node expression
        List<EPType> childTypes = new LinkedList<>();
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
            EPType type = pair.getSecond().getForge().getEvaluationType();
            childTypes.add(type);
        }
        if (analysis.getOptionalElseExprNode() != null) {
            if (analysis.getOptionalElseExprNode().getForge() instanceof ExprTypableReturnForge) {
                ExprTypableReturnForge typableReturn = (ExprTypableReturnForge) analysis.getOptionalElseExprNode().getForge();
                LinkedHashMap<String, Object> rowProps = typableReturn.getRowProperties();
                if (rowProps != null) {
                    childMapTypes.add(rowProps);
                } else {
                    EPType type = analysis.getOptionalElseExprNode().getForge().getEvaluationType();
                    childTypes.add(type);
                }
            } else {
                EPType type = analysis.getOptionalElseExprNode().getForge().getEvaluationType();
                childTypes.add(type);
            }
        }

        if (!childMapTypes.isEmpty() && !childTypes.isEmpty()) {
            String message = "Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value";
            String check;
            int count = -1;
            for (UniformPair<ExprNode> pair : analysis.getWhenThenNodeList()) {
                count++;
                EPType type = pair.getSecond().getForge().getEvaluationType();
                if (!isTypeOrNull(type, Map.class)) {
                    check = ", check when-condition number " + count;
                    throw new ExprValidationException(message + check);
                }
            }
            if (analysis.getOptionalElseExprNode() != null) {
                EPType type = analysis.getOptionalElseExprNode().getForge().getEvaluationType();
                if (!isTypeOrNull(type, Map.class)) {
                    check = ", check the else-condition";
                    throw new ExprValidationException(message + check);
                }
            }
            throw new ExprValidationException(message);
        }

        LinkedHashMap<String, Object> mapResultType = null;
        EPTypeClass resultType;
        boolean isNumericResult = false;
        if (childMapTypes.isEmpty()) {
            // Determine common denominator type
            try {
                EPType coercionType = JavaClassHelper.getCommonCoercionType(childTypes.toArray(new EPType[childTypes.size()]));
                if (coercionType == EPTypeNull.INSTANCE) {
                    throw new ExprValidationException("Null-type return value is not allowed");
                }
                resultType = (EPTypeClass) coercionType;
                if (JavaClassHelper.isNumeric(resultType)) {
                    isNumericResult = true;
                }
            } catch (CoercionException ex) {
                throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
            }
        } else {
            resultType = EPTypePremade.MAP.getEPType();
            mapResultType = childMapTypes.get(0);
            for (int i = 1; i < childMapTypes.size(); i++) {
                Map<String, Object> other = childMapTypes.get(i);
                ExprValidationException messageEquals = MapEventType.isDeepEqualsProperties("Case-when number " + i, mapResultType, other, false);
                if (messageEquals != null) {
                    throw new ExprValidationException("Incompatible case-when return types by new-operator in case-when number " + i + ": " + messageEquals.getMessage(), messageEquals);
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        CaseAnalysis analysis;
        try {
            analysis = analyzeCase();
        } catch (ExprValidationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        writer.append("case");
        if (isCase2) {
            writer.append(' ');
            analysis.getOptionalCompareExprNode().toEPL(writer, getPrecedence(), flags);
        }
        for (UniformPair<ExprNode> p : analysis.getWhenThenNodeList()) {
            writer.append(" when ");
            p.getFirst().toEPL(writer, getPrecedence(), flags);
            writer.append(" then ");
            p.getSecond().toEPL(writer, getPrecedence(), flags);
        }
        if (analysis.getOptionalElseExprNode() != null) {
            writer.append(" else ");
            analysis.getOptionalElseExprNode().toEPL(writer, getPrecedence(), flags);
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


