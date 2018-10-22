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
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.rowrecog.expr.*;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.*;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.*;

/**
 * Helper for match recognize.
 */
public class RowRecogHelper {
    public static RowRecogNFAViewService recursiveFindRegexService(Viewable top) {
        if (top == null) {
            return null;
        }
        if (top instanceof RowRecogNFAViewService) {
            return (RowRecogNFAViewService) top;
        }
        return recursiveFindRegexService(top.getChild());
    }

    protected static final Comparator<RowRecogNFAStateEntry> END_STATE_COMPARATOR = new Comparator<RowRecogNFAStateEntry>() {
        public int compare(RowRecogNFAStateEntry o1, RowRecogNFAStateEntry o2) {
            if (o1.getMatchEndEventSeqNo() > o2.getMatchEndEventSeqNo()) {
                return -1;
            }
            if (o1.getMatchEndEventSeqNo() < o2.getMatchEndEventSeqNo()) {
                return 1;
            }
            return 0;
        }
    };

    /**
     * Inspect variables recursively.
     *
     * @param parent            parent regex expression node
     * @param isMultiple        if the variable in the stack is multiple of single
     * @param variablesSingle   single variables list
     * @param variablesMultiple group variables list
     */
    public static void recursiveInspectVariables(RowRecogExprNode parent, boolean isMultiple, Set<String> variablesSingle, Set<String> variablesMultiple) {
        if (parent instanceof RowRecogExprNodeNested) {
            RowRecogExprNodeNested nested = (RowRecogExprNodeNested) parent;
            for (RowRecogExprNode child : parent.getChildNodes()) {
                recursiveInspectVariables(child, nested.getType().isMultipleMatches() || isMultiple, variablesSingle, variablesMultiple);
            }
        } else if (parent instanceof RowRecogExprNodeAlteration) {
            for (RowRecogExprNode childAlteration : parent.getChildNodes()) {
                LinkedHashSet<String> singles = new LinkedHashSet<String>();
                LinkedHashSet<String> multiples = new LinkedHashSet<String>();

                recursiveInspectVariables(childAlteration, isMultiple, singles, multiples);

                variablesMultiple.addAll(multiples);
                variablesSingle.addAll(singles);
            }
            variablesSingle.removeAll(variablesMultiple);
        } else if (parent instanceof RowRecogExprNodeAtom) {
            RowRecogExprNodeAtom atom = (RowRecogExprNodeAtom) parent;
            String name = atom.getTag();
            if (variablesMultiple.contains(name)) {
                return;
            }
            if (variablesSingle.contains(name)) {
                variablesSingle.remove(name);
                variablesMultiple.add(name);
                return;
            }
            if (atom.getType().isMultipleMatches()) {
                variablesMultiple.add(name);
                return;
            }
            if (isMultiple) {
                variablesMultiple.add(name);
            } else {
                variablesSingle.add(name);
            }
        } else {
            for (RowRecogExprNode child : parent.getChildNodes()) {
                recursiveInspectVariables(child, isMultiple, variablesSingle, variablesMultiple);
            }
        }
    }

    /**
     * Build a list of start states from the parent node.
     *
     * @param parent                      to build start state for
     * @param variableDefinitions         each variable and its expressions
     * @param variableStreams             variable name and its stream number
     * @param exprRequiresMultimatchState indicator whether multi-match state required
     * @return strand of regex state nodes
     */
    protected static RowRecogNFAStrandResult buildStartStates(RowRecogExprNode parent,
                                                              Map<String, ExprNode> variableDefinitions,
                                                              Map<String, Pair<Integer, Boolean>> variableStreams,
                                                              boolean[] exprRequiresMultimatchState
    ) {
        Stack<Integer> nodeNumStack = new Stack<Integer>();

        RowRecogNFAStrand strand = recursiveBuildStatesInternal(parent,
                variableDefinitions,
                variableStreams,
                nodeNumStack,
                exprRequiresMultimatchState);

        // add end state
        RowRecogNFAStateEndForge end = new RowRecogNFAStateEndForge();
        end.setNodeNumFlat(-1);
        for (RowRecogNFAStateForgeBase endStates : strand.getEndStates()) {
            endStates.addState(end);
        }

        // assign node num as a counter
        int nodeNumberFlat = 0;
        for (RowRecogNFAStateForgeBase theBase : strand.getAllStates()) {
            theBase.setNodeNumFlat(nodeNumberFlat++);
        }

        return new RowRecogNFAStrandResult(new ArrayList<>(strand.getStartStates()), strand.getAllStates());
    }

    private static RowRecogNFAStrand recursiveBuildStatesInternal(RowRecogExprNode node,
                                                                  Map<String, ExprNode> variableDefinitions,
                                                                  Map<String, Pair<Integer, Boolean>> variableStreams,
                                                                  Stack<Integer> nodeNumStack,
                                                                  boolean[] exprRequiresMultimatchState
    ) {
        if (node instanceof RowRecogExprNodeAlteration) {
            int nodeNum = 0;

            List<RowRecogNFAStateForgeBase> cumulativeStartStates = new ArrayList<>();
            List<RowRecogNFAStateForgeBase> cumulativeStates = new ArrayList<>();
            List<RowRecogNFAStateForgeBase> cumulativeEndStates = new ArrayList<>();

            boolean isPassthrough = false;
            for (RowRecogExprNode child : node.getChildNodes()) {
                nodeNumStack.push(nodeNum);
                RowRecogNFAStrand strand = recursiveBuildStatesInternal(child,
                        variableDefinitions,
                        variableStreams,
                        nodeNumStack,
                        exprRequiresMultimatchState);
                nodeNumStack.pop();

                cumulativeStartStates.addAll(strand.getStartStates());
                cumulativeStates.addAll(strand.getAllStates());
                cumulativeEndStates.addAll(strand.getEndStates());
                if (strand.isPassthrough()) {
                    isPassthrough = true;
                }

                nodeNum++;
            }

            return new RowRecogNFAStrand(cumulativeStartStates, cumulativeEndStates, cumulativeStates, isPassthrough);
        } else if (node instanceof RowRecogExprNodeConcatenation) {
            int nodeNum = 0;

            boolean isPassthrough = true;
            List<RowRecogNFAStateForgeBase> cumulativeStates = new ArrayList<>();
            RowRecogNFAStrand[] strands = new RowRecogNFAStrand[node.getChildNodes().size()];

            for (RowRecogExprNode child : node.getChildNodes()) {
                nodeNumStack.push(nodeNum);
                strands[nodeNum] = recursiveBuildStatesInternal(child,
                        variableDefinitions,
                        variableStreams,
                        nodeNumStack,
                        exprRequiresMultimatchState);
                nodeNumStack.pop();

                cumulativeStates.addAll(strands[nodeNum].getAllStates());
                if (!strands[nodeNum].isPassthrough()) {
                    isPassthrough = false;
                }

                nodeNum++;
            }

            // determine start states: all states until the first non-passthrough start state
            List<RowRecogNFAStateForgeBase> startStates = new ArrayList<>();
            for (int i = 0; i < strands.length; i++) {
                startStates.addAll(strands[i].getStartStates());
                if (!strands[i].isPassthrough()) {
                    break;
                }
            }

            // determine end states: all states from the back until the last non-passthrough end state
            List<RowRecogNFAStateForgeBase> endStates = new ArrayList<>();
            for (int i = strands.length - 1; i >= 0; i--) {
                endStates.addAll(strands[i].getEndStates());
                if (!strands[i].isPassthrough()) {
                    break;
                }
            }

            // hook up the end state of each strand with the start states of each next strand
            for (int i = strands.length - 1; i >= 1; i--) {
                RowRecogNFAStrand current = strands[i];
                for (int j = i - 1; j >= 0; j--) {
                    RowRecogNFAStrand prior = strands[j];

                    for (RowRecogNFAStateForgeBase endState : prior.getEndStates()) {
                        for (RowRecogNFAStateForgeBase startState : current.getStartStates()) {
                            endState.addState(startState);
                        }
                    }

                    if (!prior.isPassthrough()) {
                        break;
                    }
                }
            }

            return new RowRecogNFAStrand(startStates, endStates, cumulativeStates, isPassthrough);
        } else if (node instanceof RowRecogExprNodeNested) {
            RowRecogExprNodeNested nested = (RowRecogExprNodeNested) node;
            nodeNumStack.push(0);
            RowRecogNFAStrand strand = recursiveBuildStatesInternal(node.getChildNodes().get(0),
                    variableDefinitions,
                    variableStreams,
                    nodeNumStack,
                    exprRequiresMultimatchState);
            nodeNumStack.pop();

            boolean isPassthrough = strand.isPassthrough() || nested.getType().isOptional();

            // if this is a repeating node then pipe back each end state to each begin state
            if (nested.getType().isMultipleMatches()) {
                for (RowRecogNFAStateForgeBase endstate : strand.getEndStates()) {
                    for (RowRecogNFAStateForgeBase startstate : strand.getStartStates()) {
                        if (!endstate.getNextStates().contains(startstate)) {
                            endstate.getNextStates().add(startstate);
                        }
                    }
                }
            }
            return new RowRecogNFAStrand(strand.getStartStates(), strand.getEndStates(), strand.getAllStates(), isPassthrough);
        } else {
            RowRecogExprNodeAtom atom = (RowRecogExprNodeAtom) node;

            // assign stream number for single-variables for most direct expression eval; multiple-variable gets -1
            int streamNum = variableStreams.get(atom.getTag()).getFirst();
            boolean multiple = variableStreams.get(atom.getTag()).getSecond();
            ExprNode expression = variableDefinitions.get(atom.getTag());
            boolean exprRequiresMultimatch = exprRequiresMultimatchState[streamNum];

            RowRecogNFAStateForgeBase nextState;
            if ((atom.getType() == RowRecogNFATypeEnum.ZERO_TO_MANY) || (atom.getType() == RowRecogNFATypeEnum.ZERO_TO_MANY_RELUCTANT)) {
                nextState = new RowRecogNFAStateZeroToManyForge(toString(nodeNumStack), atom.getTag(), streamNum, multiple, atom.getType().isGreedy(), exprRequiresMultimatch, expression);
            } else if ((atom.getType() == RowRecogNFATypeEnum.ONE_TO_MANY) || (atom.getType() == RowRecogNFATypeEnum.ONE_TO_MANY_RELUCTANT)) {
                nextState = new RowRecogNFAStateOneToManyForge(toString(nodeNumStack), atom.getTag(), streamNum, multiple, atom.getType().isGreedy(), exprRequiresMultimatch, expression);
            } else if ((atom.getType() == RowRecogNFATypeEnum.ONE_OPTIONAL) || (atom.getType() == RowRecogNFATypeEnum.ONE_OPTIONAL_RELUCTANT)) {
                nextState = new RowRecogNFAStateOneOptionalForge(toString(nodeNumStack), atom.getTag(), streamNum, multiple, atom.getType().isGreedy(), exprRequiresMultimatch, expression);
            } else if (expression == null) {
                nextState = new RowRecogNFAStateAnyOneForge(toString(nodeNumStack), atom.getTag(), streamNum, multiple);
            } else {
                nextState = new RowRecogNFAStateFilterForge(toString(nodeNumStack), atom.getTag(), streamNum, multiple, exprRequiresMultimatch, expression);
            }

            return new RowRecogNFAStrand(Collections.singletonList(nextState), Collections.singletonList(nextState),
                    Collections.singletonList(nextState), atom.getType().isOptional());
        }
    }

    private static String toString(Stack<Integer> nodeNumStack) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (Integer atom : nodeNumStack) {
            builder.append(delimiter);
            builder.append(Integer.toString(atom));
            delimiter = ".";
        }
        return builder.toString();
    }

    public static Map<String, Set<String>> determineVisibility(RowRecogExprNode pattern) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        ArrayDeque<RowRecogExprNode> path = new ArrayDeque<RowRecogExprNode>();
        recursiveFindPatternAtoms(pattern, path, map);
        return map;
    }

    private static void recursiveFindPatternAtoms(RowRecogExprNode parent, ArrayDeque<RowRecogExprNode> path, Map<String, Set<String>> map) {
        path.add(parent);
        for (RowRecogExprNode child : parent.getChildNodes()) {
            if (child instanceof RowRecogExprNodeAtom) {
                handleAtom((RowRecogExprNodeAtom) child, path, map);
            } else {
                recursiveFindPatternAtoms(child, path, map);
            }
        }
        path.removeLast();
    }

    private static void handleAtom(RowRecogExprNodeAtom atom, ArrayDeque<RowRecogExprNode> path, Map<String, Set<String>> map) {

        RowRecogExprNode[] patharr = path.toArray(new RowRecogExprNode[path.size()]);
        Set<String> identifiers = null;

        for (int i = 0; i < patharr.length; i++) {
            RowRecogExprNode parent = patharr[i];
            if (!(parent instanceof RowRecogExprNodeConcatenation)) {
                continue;
            }

            RowRecogExprNodeConcatenation concat = (RowRecogExprNodeConcatenation) parent;
            int indexWithinConcat;
            if (i == patharr.length - 1) {
                indexWithinConcat = parent.getChildNodes().indexOf(atom);
            } else {
                indexWithinConcat = parent.getChildNodes().indexOf(patharr[i + 1]);
            }

            if (identifiers == null && indexWithinConcat > 0) {
                identifiers = new HashSet<String>();
            }

            for (int j = 0; j < indexWithinConcat; j++) {
                RowRecogExprNode concatChildNode = concat.getChildNodes().get(j);
                recursiveCollectAtomsWExclude(concatChildNode, identifiers, atom.getTag());
            }
        }

        if (identifiers == null) {
            return;
        }

        Set<String> existingVisibility = map.get(atom.getTag());
        if (existingVisibility == null) {
            map.put(atom.getTag(), identifiers);
        } else {
            existingVisibility.addAll(identifiers);
        }
    }

    private static void recursiveCollectAtomsWExclude(RowRecogExprNode node, Set<String> identifiers, String excludedTag) {
        if (node instanceof RowRecogExprNodeAtom) {
            RowRecogExprNodeAtom atom = (RowRecogExprNodeAtom) node;
            if (!excludedTag.equals(atom.getTag())) {
                identifiers.add(atom.getTag());
            }
        }
        for (RowRecogExprNode child : node.getChildNodes()) {
            recursiveCollectAtomsWExclude(child, identifiers, excludedTag);
        }
    }
}
