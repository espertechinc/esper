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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PermutationEnumeration;
import com.espertech.esper.common.internal.compile.stage1.specmapper.ExpressionCopier;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.rowrecog.expr.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RowRecogPatternExpandUtil {

    private static final RowRegexExprNodeCopierAtom ATOM_HANDLER = new RowRegexExprNodeCopierAtom();
    private static final RowRegexExprNodeCopierNested NESTED_HANDLER = new RowRegexExprNodeCopierNested();

    public static RowRecogExprNode expand(RowRecogExprNode pattern, ExpressionCopier expressionCopier) throws ExprValidationException {
        RowRecogExprNodeVisitorRepeat visitor = new RowRecogExprNodeVisitorRepeat();
        pattern.accept(visitor);
        RowRecogExprNode newParentNode = pattern;

        // expand permutes
        List<RowRecogExprNodeVisitorRepeat.RowRegexPermuteDesc> permutes = visitor.getPermutes();
        Collections.sort(permutes, new Comparator<RowRecogExprNodeVisitorRepeat.RowRegexPermuteDesc>() {
            public int compare(RowRecogExprNodeVisitorRepeat.RowRegexPermuteDesc o1, RowRecogExprNodeVisitorRepeat.RowRegexPermuteDesc o2) {
                if (o1.getLevel() > o2.getLevel()) {
                    return -1;
                }
                return o1.getLevel() == o2.getLevel() ? 0 : 1;
            }
        });
        for (RowRecogExprNodeVisitorRepeat.RowRegexPermuteDesc permute : permutes) {
            RowRecogExprNodeAlteration alteration = expandPermute(permute.getPermute(), expressionCopier);
            RowRecogExprNode optionalNewParent = replace(permute.getOptionalParent(), permute.getPermute(), Collections.<RowRecogExprNode>singletonList(alteration));
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        // expand atoms
        List<Pair<RowRecogExprNodeAtom, RowRecogExprNode>> atomPairs = visitor.getAtoms();
        for (Pair<RowRecogExprNodeAtom, RowRecogExprNode> pair : atomPairs) {
            RowRecogExprNodeAtom atom = pair.getFirst();
            List<RowRecogExprNode> expandedRepeat = expandRepeat(atom, atom.getOptionalRepeat(), atom.getType(), ATOM_HANDLER, expressionCopier);
            RowRecogExprNode optionalNewParent = replace(pair.getSecond(), pair.getFirst(), expandedRepeat);
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        // expand nested
        List<RowRecogExprNodeVisitorRepeat.RowRegexNestedDesc> nestedPairs = visitor.getNesteds();
        Collections.sort(nestedPairs, new Comparator<RowRecogExprNodeVisitorRepeat.RowRegexNestedDesc>() {
            public int compare(RowRecogExprNodeVisitorRepeat.RowRegexNestedDesc o1, RowRecogExprNodeVisitorRepeat.RowRegexNestedDesc o2) {
                if (o1.getLevel() > o2.getLevel()) {
                    return -1;
                }
                return o1.getLevel() == o2.getLevel() ? 0 : 1;
            }
        });
        for (RowRecogExprNodeVisitorRepeat.RowRegexNestedDesc pair : nestedPairs) {
            RowRecogExprNodeNested nested = pair.getNested();
            List<RowRecogExprNode> expandedRepeat = expandRepeat(nested, nested.getOptionalRepeat(), nested.getType(), NESTED_HANDLER, expressionCopier);
            RowRecogExprNode optionalNewParent = replace(pair.getOptionalParent(), pair.getNested(), expandedRepeat);
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        return newParentNode;
    }

    private static RowRecogExprNodeAlteration expandPermute(RowRecogExprNodePermute permute, ExpressionCopier expressionCopier) {
        PermutationEnumeration e = new PermutationEnumeration(permute.getChildNodes().size());
        RowRecogExprNodeAlteration parent = new RowRecogExprNodeAlteration();
        while (e.hasMoreElements()) {
            int[] indexes = e.nextElement();
            RowRecogExprNodeConcatenation concat = new RowRecogExprNodeConcatenation();
            parent.addChildNode(concat);
            for (int i = 0; i < indexes.length; i++) {
                RowRecogExprNode toCopy = permute.getChildNodes().get(indexes[i]);
                RowRecogExprNode copy = toCopy.checkedCopy(expressionCopier);
                concat.addChildNode(copy);
            }
        }
        return parent;
    }

    private static RowRecogExprNode replace(RowRecogExprNode optionalParent, RowRecogExprNode originalNode, List<RowRecogExprNode> expandedRepeat) {
        if (optionalParent == null) {
            RowRecogExprNodeConcatenation newParentNode = new RowRecogExprNodeConcatenation();
            newParentNode.getChildNodes().addAll(expandedRepeat);
            return newParentNode;
        }

        // for nested nodes, use a concatenation instead
        if (optionalParent instanceof RowRecogExprNodeNested ||
                optionalParent instanceof RowRecogExprNodeAlteration) {
            RowRecogExprNodeConcatenation concatenation = new RowRecogExprNodeConcatenation();
            concatenation.getChildNodes().addAll(expandedRepeat);
            optionalParent.replaceChildNode(originalNode, Collections.<RowRecogExprNode>singletonList(concatenation));
        } else {
            // concatenations are simply changed
            optionalParent.replaceChildNode(originalNode, expandedRepeat);
        }

        return null;
    }

    private static List<RowRecogExprNode> expandRepeat(RowRecogExprNode node,
                                                       RowRecogExprRepeatDesc repeat,
                                                       RowRecogNFATypeEnum type,
                                                       RowRegexExprNodeCopier copier,
                                                       ExpressionCopier expressionCopier) throws ExprValidationException {
        // handle single-bounds (no ranges)
        List<RowRecogExprNode> repeated = new ArrayList<RowRecogExprNode>();
        if (repeat.getSingle() != null) {
            validateExpression(repeat.getSingle());
            int numRepeated = (Integer) repeat.getSingle().getForge().getExprEvaluator().evaluate(null, true, null);
            validateRange(numRepeated, 1, Integer.MAX_VALUE);
            for (int i = 0; i < numRepeated; i++) {
                RowRecogExprNode copy = copier.copy(node, type, expressionCopier);
                repeated.add(copy);
            }
            return repeated;
        }

        // evaluate bounds
        Integer lower = null;
        Integer upper = null;
        if (repeat.getLower() != null) {
            validateExpression(repeat.getLower());
            lower = (Integer) repeat.getLower().getForge().getExprEvaluator().evaluate(null, true, null);
        }
        if (repeat.getUpper() != null) {
            validateExpression(repeat.getUpper());
            upper = (Integer) repeat.getUpper().getForge().getExprEvaluator().evaluate(null, true, null);
        }

        // handle range
        if (lower != null && upper != null) {
            validateRange(lower, 1, Integer.MAX_VALUE);
            validateRange(upper, 1, Integer.MAX_VALUE);
            validateRange(lower, 1, upper);
            for (int i = 0; i < lower; i++) {
                RowRecogExprNode copy = copier.copy(node, type, expressionCopier);
                repeated.add(copy);
            }
            for (int i = lower; i < upper; i++) {
                // makeInline type optional
                RowRecogNFATypeEnum newType = type;
                if (type == RowRecogNFATypeEnum.SINGLE) {
                    newType = RowRecogNFATypeEnum.ONE_OPTIONAL;
                } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY) {
                    newType = RowRecogNFATypeEnum.ZERO_TO_MANY;
                } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                    newType = RowRecogNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
                }
                RowRecogExprNode copy = copier.copy(node, newType, expressionCopier);
                repeated.add(copy);
            }
            return repeated;
        }

        // handle lower-bounds only
        if (upper == null) {
            validateRange(lower, 1, Integer.MAX_VALUE);
            for (int i = 0; i < lower; i++) {
                RowRecogExprNode copy = copier.copy(node, type, expressionCopier);
                repeated.add(copy);
            }
            // makeInline type optional
            RowRecogNFATypeEnum newType = type;
            if (type == RowRecogNFATypeEnum.SINGLE) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RowRecogNFATypeEnum.ONE_OPTIONAL) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RowRecogNFATypeEnum.ONE_OPTIONAL_RELUCTANT) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            }
            RowRecogExprNode copy = copier.copy(node, newType, expressionCopier);
            repeated.add(copy);
            return repeated;
        }

        // handle upper-bounds only
        validateRange(upper, 1, Integer.MAX_VALUE);
        for (int i = 0; i < upper; i++) {
            // makeInline type optional
            RowRecogNFATypeEnum newType = type;
            if (type == RowRecogNFATypeEnum.SINGLE) {
                newType = RowRecogNFATypeEnum.ONE_OPTIONAL;
            } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RowRecogNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                newType = RowRecogNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            }
            RowRecogExprNode copy = copier.copy(node, newType, expressionCopier);
            repeated.add(copy);
        }
        return repeated;
    }

    private static void validateRange(int value, int min, int maxValue) throws ExprValidationException {
        if (value < min || value > maxValue) {
            String message = "Invalid pattern quantifier value " + value + ", expecting a minimum of " + min;
            if (maxValue != Integer.MAX_VALUE) {
                message += " and maximum of " + maxValue;
            }
            throw new ExprValidationException(message);
        }
    }

    private static void validateExpression(ExprNode repeat) throws ExprValidationException {
        ExprNodeUtilityValidate.validatePlainExpression(ExprNodeOrigin.MATCHRECOGPATTERN, repeat);

        if (!(repeat instanceof ExprConstantNode)) {
            throw new ExprValidationException(getPatternQuantifierExpressionText(repeat) + " must return a constant value");
        }
        if (JavaClassHelper.getBoxedType(repeat.getForge().getEvaluationType()) != Integer.class) {
            throw new ExprValidationException(getPatternQuantifierExpressionText(repeat) + " must return an integer-type value");
        }
    }

    private interface RowRegexExprNodeCopier {
        RowRecogExprNode copy(RowRecogExprNode nodeToCopy, RowRecogNFATypeEnum newType, ExpressionCopier expressionCopier);
    }

    private static class RowRegexExprNodeCopierAtom implements RowRegexExprNodeCopier {
        public RowRecogExprNode copy(RowRecogExprNode nodeToCopy, RowRecogNFATypeEnum newType, ExpressionCopier expressionCopier) {
            RowRecogExprNodeAtom atom = (RowRecogExprNodeAtom) nodeToCopy;
            return new RowRecogExprNodeAtom(atom.getTag(), newType, null);
        }
    }

    private static class RowRegexExprNodeCopierNested implements RowRegexExprNodeCopier {
        public RowRecogExprNode copy(RowRecogExprNode nodeToCopy, RowRecogNFATypeEnum newType, ExpressionCopier expressionCopier) {
            RowRecogExprNodeNested nested = (RowRecogExprNodeNested) nodeToCopy;
            RowRecogExprNodeNested nestedCopy = new RowRecogExprNodeNested(newType, null);
            for (RowRecogExprNode inner : nested.getChildNodes()) {
                RowRecogExprNode innerCopy = inner.checkedCopy(expressionCopier);
                nestedCopy.addChildNode(innerCopy);
            }
            return nestedCopy;
        }
    }

    private static String getPatternQuantifierExpressionText(ExprNode exprNode) {
        return "Pattern quantifier '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode) + "'";
    }
}
