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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EPException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.PermutationEnumeration;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SerializableObjectCopier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RegexPatternExpandUtil {

    private static final RowRegexExprNodeCopierAtom ATOM_HANDLER = new RowRegexExprNodeCopierAtom();
    private static final RowRegexExprNodeCopierNested NESTED_HANDLER = new RowRegexExprNodeCopierNested();

    public static RowRegexExprNode expand(RowRegexExprNode pattern) throws ExprValidationException {
        RowRegexExprNodeVisitorRepeat visitor = new RowRegexExprNodeVisitorRepeat();
        pattern.accept(visitor);
        RowRegexExprNode newParentNode = pattern;

        // expand permutes
        List<RowRegexExprNodeVisitorRepeat.RowRegexPermuteDesc> permutes = visitor.getPermutes();
        Collections.sort(permutes, new Comparator<RowRegexExprNodeVisitorRepeat.RowRegexPermuteDesc>() {
            public int compare(RowRegexExprNodeVisitorRepeat.RowRegexPermuteDesc o1, RowRegexExprNodeVisitorRepeat.RowRegexPermuteDesc o2) {
                if (o1.getLevel() > o2.getLevel()) {
                    return -1;
                }
                return o1.getLevel() == o2.getLevel() ? 0 : 1;
            }
        });
        for (RowRegexExprNodeVisitorRepeat.RowRegexPermuteDesc permute : permutes) {
            RowRegexExprNodeAlteration alteration = expandPermute(permute.getPermute());
            RowRegexExprNode optionalNewParent = replace(permute.getOptionalParent(), permute.getPermute(), Collections.<RowRegexExprNode>singletonList(alteration));
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        // expand atoms
        List<Pair<RowRegexExprNodeAtom, RowRegexExprNode>> atomPairs = visitor.getAtoms();
        for (Pair<RowRegexExprNodeAtom, RowRegexExprNode> pair : atomPairs) {
            RowRegexExprNodeAtom atom = pair.getFirst();
            List<RowRegexExprNode> expandedRepeat = expandRepeat(atom, atom.getOptionalRepeat(), atom.getType(), ATOM_HANDLER);
            RowRegexExprNode optionalNewParent = replace(pair.getSecond(), pair.getFirst(), expandedRepeat);
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        // expand nested
        List<RowRegexExprNodeVisitorRepeat.RowRegexNestedDesc> nestedPairs = visitor.getNesteds();
        Collections.sort(nestedPairs, new Comparator<RowRegexExprNodeVisitorRepeat.RowRegexNestedDesc>() {
            public int compare(RowRegexExprNodeVisitorRepeat.RowRegexNestedDesc o1, RowRegexExprNodeVisitorRepeat.RowRegexNestedDesc o2) {
                if (o1.getLevel() > o2.getLevel()) {
                    return -1;
                }
                return o1.getLevel() == o2.getLevel() ? 0 : 1;
            }
        });
        for (RowRegexExprNodeVisitorRepeat.RowRegexNestedDesc pair : nestedPairs) {
            RowRegexExprNodeNested nested = pair.getNested();
            List<RowRegexExprNode> expandedRepeat = expandRepeat(nested, nested.getOptionalRepeat(), nested.getType(), NESTED_HANDLER);
            RowRegexExprNode optionalNewParent = replace(pair.getOptionalParent(), pair.getNested(), expandedRepeat);
            if (optionalNewParent != null) {
                newParentNode = optionalNewParent;
            }
        }

        return newParentNode;
    }

    private static RowRegexExprNodeAlteration expandPermute(RowRegexExprNodePermute permute) {
        PermutationEnumeration e = new PermutationEnumeration(permute.getChildNodes().size());
        RowRegexExprNodeAlteration parent = new RowRegexExprNodeAlteration();
        while (e.hasMoreElements()) {
            int[] indexes = e.nextElement();
            RowRegexExprNodeConcatenation concat = new RowRegexExprNodeConcatenation();
            parent.addChildNode(concat);
            for (int i = 0; i < indexes.length; i++) {
                RowRegexExprNode toCopy = permute.getChildNodes().get(indexes[i]);
                RowRegexExprNode copy = checkedCopy(toCopy);
                concat.addChildNode(copy);
            }
        }
        return parent;
    }

    private static RowRegexExprNode replace(RowRegexExprNode optionalParent, RowRegexExprNode originalNode, List<RowRegexExprNode> expandedRepeat) {
        if (optionalParent == null) {
            RowRegexExprNodeConcatenation newParentNode = new RowRegexExprNodeConcatenation();
            newParentNode.getChildNodes().addAll(expandedRepeat);
            return newParentNode;
        }

        // for nested nodes, use a concatenation instead
        if (optionalParent instanceof RowRegexExprNodeNested ||
                optionalParent instanceof RowRegexExprNodeAlteration) {
            RowRegexExprNodeConcatenation concatenation = new RowRegexExprNodeConcatenation();
            concatenation.getChildNodes().addAll(expandedRepeat);
            optionalParent.replaceChildNode(originalNode, Collections.<RowRegexExprNode>singletonList(concatenation));
        } else {
            // concatenations are simply changed
            optionalParent.replaceChildNode(originalNode, expandedRepeat);
        }

        return null;
    }

    private static List<RowRegexExprNode> expandRepeat(RowRegexExprNode node,
                                                       RowRegexExprRepeatDesc repeat,
                                                       RegexNFATypeEnum type,
                                                       RowRegexExprNodeCopier copier) throws ExprValidationException {
        // handle single-bounds (no ranges)
        List<RowRegexExprNode> repeated = new ArrayList<RowRegexExprNode>();
        if (repeat.getSingle() != null) {
            validateExpression(repeat.getSingle());
            int numRepeated = (Integer) repeat.getSingle().getForge().getExprEvaluator().evaluate(null, true, null);
            validateRange(numRepeated, 1, Integer.MAX_VALUE);
            for (int i = 0; i < numRepeated; i++) {
                RowRegexExprNode copy = copier.copy(node, type);
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
                RowRegexExprNode copy = copier.copy(node, type);
                repeated.add(copy);
            }
            for (int i = lower; i < upper; i++) {
                // make type optional
                RegexNFATypeEnum newType = type;
                if (type == RegexNFATypeEnum.SINGLE) {
                    newType = RegexNFATypeEnum.ONE_OPTIONAL;
                } else if (type == RegexNFATypeEnum.ONE_TO_MANY) {
                    newType = RegexNFATypeEnum.ZERO_TO_MANY;
                } else if (type == RegexNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                    newType = RegexNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
                }
                RowRegexExprNode copy = copier.copy(node, newType);
                repeated.add(copy);
            }
            return repeated;
        }

        // handle lower-bounds only
        if (upper == null) {
            validateRange(lower, 1, Integer.MAX_VALUE);
            for (int i = 0; i < lower; i++) {
                RowRegexExprNode copy = copier.copy(node, type);
                repeated.add(copy);
            }
            // make type optional
            RegexNFATypeEnum newType = type;
            if (type == RegexNFATypeEnum.SINGLE) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RegexNFATypeEnum.ONE_OPTIONAL) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RegexNFATypeEnum.ONE_OPTIONAL_RELUCTANT) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            } else if (type == RegexNFATypeEnum.ONE_TO_MANY) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RegexNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            }
            RowRegexExprNode copy = copier.copy(node, newType);
            repeated.add(copy);
            return repeated;
        }

        // handle upper-bounds only
        validateRange(upper, 1, Integer.MAX_VALUE);
        for (int i = 0; i < upper; i++) {
            // make type optional
            RegexNFATypeEnum newType = type;
            if (type == RegexNFATypeEnum.SINGLE) {
                newType = RegexNFATypeEnum.ONE_OPTIONAL;
            } else if (type == RegexNFATypeEnum.ONE_TO_MANY) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY;
            } else if (type == RegexNFATypeEnum.ONE_TO_MANY_RELUCTANT) {
                newType = RegexNFATypeEnum.ZERO_TO_MANY_RELUCTANT;
            }
            RowRegexExprNode copy = copier.copy(node, newType);
            repeated.add(copy);
        }
        return repeated;
    }

    private static RowRegexExprNode checkedCopy(RowRegexExprNode inner) {
        try {
            return (RowRegexExprNode) SerializableObjectCopier.copy(inner);
        } catch (Exception e) {
            throw new EPException("Failed to repeat nested match-recognize: " + e.getMessage(), e);
        }
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
        ExprNodeUtilityRich.validatePlainExpression(ExprNodeOrigin.MATCHRECOGPATTERN, repeat);
        if (!repeat.isConstantResult()) {
            throw new ExprValidationException(getPatternQuantifierExpressionText(repeat) + " must return a constant value");
        }
        if (JavaClassHelper.getBoxedType(repeat.getForge().getEvaluationType()) != Integer.class) {
            throw new ExprValidationException(getPatternQuantifierExpressionText(repeat) + " must return an integer-type value");
        }
    }

    private static interface RowRegexExprNodeCopier {
        public RowRegexExprNode copy(RowRegexExprNode nodeToCopy, RegexNFATypeEnum newType);
    }

    private static class RowRegexExprNodeCopierAtom implements RowRegexExprNodeCopier {
        public RowRegexExprNode copy(RowRegexExprNode nodeToCopy, RegexNFATypeEnum newType) {
            RowRegexExprNodeAtom atom = (RowRegexExprNodeAtom) nodeToCopy;
            return new RowRegexExprNodeAtom(atom.getTag(), newType, null);
        }
    }

    private static class RowRegexExprNodeCopierNested implements RowRegexExprNodeCopier {
        public RowRegexExprNode copy(RowRegexExprNode nodeToCopy, RegexNFATypeEnum newType) {
            RowRegexExprNodeNested nested = (RowRegexExprNodeNested) nodeToCopy;
            RowRegexExprNodeNested nestedCopy = new RowRegexExprNodeNested(newType, null);
            for (RowRegexExprNode inner : nested.getChildNodes()) {
                RowRegexExprNode innerCopy = checkedCopy(inner);
                nestedCopy.addChildNode(innerCopy);
            }
            return nestedCopy;
        }
    }

    private static String getPatternQuantifierExpressionText(ExprNode exprNode) {
        return "pattern quantifier '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprNode) + "'";
    }
}
