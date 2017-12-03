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
package com.espertech.esper.pattern;


import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class represents the state of an "and" operator in the evaluation state tree.
 */
public class EvalAndStateNode extends EvalStateNode implements Evaluator {
    protected final EvalAndNode evalAndNode;
    protected final EvalStateNode[] activeChildNodes;
    protected Object[] eventsPerChild;

    /**
     * Constructor.
     *
     * @param parentNode  is the parent evaluator to call to indicate truth value
     * @param evalAndNode is the factory node associated to the state
     */
    public EvalAndStateNode(Evaluator parentNode,
                            EvalAndNode evalAndNode) {
        super(parentNode);

        this.evalAndNode = evalAndNode;
        this.activeChildNodes = new EvalStateNode[evalAndNode.getChildNodes().length];
        this.eventsPerChild = new Object[evalAndNode.getChildNodes().length];
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        boolean quit = false;
        if (eventsPerChild != null) {
            for (Object entry : eventsPerChild) {
                if (entry instanceof MatchedEventMap) {
                    quit = PatternConsumptionUtil.containsEvent(matchEvent, (MatchedEventMap) entry);
                } else if (entry != null) {
                    List<MatchedEventMap> list = (List<MatchedEventMap>) entry;
                    for (MatchedEventMap map : list) {
                        quit = PatternConsumptionUtil.containsEvent(matchEvent, map);
                        if (quit) {
                            break;
                        }
                    }
                }
                if (quit) {
                    break;
                }
            }
        }
        if (!quit && activeChildNodes != null) {
            for (EvalStateNode child : activeChildNodes) {
                if (child != null) {
                    child.removeMatch(matchEvent);
                }
            }
        }
        if (quit) {
            quit();
            this.getParentEvaluator().evaluateFalse(this, true);
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalAndNode;
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternAndStart(evalAndNode, beginState);
        }
        // In an "and" expression we need to create a state for all child listeners
        int count = 0;
        for (EvalNode node : evalAndNode.getChildNodes()) {
            EvalStateNode childState = node.newState(this, null, 0L);
            activeChildNodes[count++] = childState;
        }

        // Start all child nodes
        for (EvalStateNode child : activeChildNodes) {
            if (child != null) {
                child.start(beginState);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternAndStart();
        }
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternAndEvaluateTrue(evalAndNode, matchEvent);
        }

        Integer indexFrom = null;
        for (int i = 0; i < activeChildNodes.length; i++) {
            if (activeChildNodes[i] == fromNode) {
                indexFrom = i;
            }
        }

        // If one of the children quits, remove the child
        if (isQuitted && indexFrom != null) {
            activeChildNodes[indexFrom] = null;
        }

        if (eventsPerChild == null || indexFrom == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aPatternAndEvaluateTrue(true);
            }
            return;
        }

        // If all nodes have events received, the AND expression turns true
        boolean allHaveEventsExcludingFromChild = true;
        for (int i = 0; i < eventsPerChild.length; i++) {
            if (indexFrom != i && eventsPerChild[i] == null) {
                allHaveEventsExcludingFromChild = false;
                break;
            }
        }

        // if we don't have events from all child nodes, add event and done
        if (!allHaveEventsExcludingFromChild) {
            addMatchEvent(eventsPerChild, indexFrom, matchEvent);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aPatternAndEvaluateTrue(false);
            }
            return;
        }

        // if all other nodes have quit other then the from-node, don't retain matching event
        boolean allOtherNodesQuit = true;
        boolean hasActive = false;
        for (int i = 0; i < eventsPerChild.length; i++) {
            if (activeChildNodes[i] != null) {
                hasActive = true;
                if (i != indexFrom) {
                    allOtherNodesQuit = false;
                }
            }
        }

        // if not all other nodes have quit, add event to received list
        if (!allOtherNodesQuit) {
            addMatchEvent(eventsPerChild, indexFrom, matchEvent);
        }

        // For each combination in eventsPerChild for all other state nodes generate an event to the parent
        List<MatchedEventMap> result = generateMatchEvents(matchEvent, eventsPerChild, indexFrom);

        // Check if this is quitting
        boolean quitted = true;
        if (hasActive) {
            for (EvalStateNode stateNode : activeChildNodes) {
                if (stateNode != null && !(stateNode.isNotOperator())) {
                    quitted = false;
                }
            }
        }

        // So we are quitting if all non-not child nodes have quit, since the not-node wait for evaluate false
        if (quitted) {
            quitInternal();
        }

        // Send results to parent
        for (MatchedEventMap theEvent : result) {
            this.getParentEvaluator().evaluateTrue(theEvent, this, quitted, optionalTriggeringEvent);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternAndEvaluateTrue(eventsPerChild == null);
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternAndEvaluateFalse(evalAndNode);
        }
        Integer indexFrom = null;
        for (int i = 0; i < activeChildNodes.length; i++) {
            if (activeChildNodes[i] == fromNode) {
                activeChildNodes[i] = null;
                indexFrom = i;
            }
        }

        if (indexFrom != null) {
            eventsPerChild[indexFrom] = null;
        }

        // The and node cannot turn true anymore, might as well quit all child nodes
        quitInternal();
        this.getParentEvaluator().evaluateFalse(this, restartable ? true : false);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternAndEvaluateFalse();
        }
    }

    /**
     * Generate a list of matching event combinations constisting of the events per child that are passed in.
     *
     * @param matchEvent     can be populated with prior events that must be passed on
     * @param eventsPerChild is the list of events for each child node to the "And" node.
     * @param indexFrom      from-index
     * @return list of events populated with all possible combinations
     */
    public static List<MatchedEventMap> generateMatchEvents(MatchedEventMap matchEvent,
                                                            Object[] eventsPerChild,
                                                            int indexFrom) {
        // Place event list for each child state node into an array, excluding the node where the event came from
        ArrayList<List<MatchedEventMap>> listArray = new ArrayList<List<MatchedEventMap>>();
        int index = 0;
        for (int i = 0; i < eventsPerChild.length; i++) {
            Object eventsChild = eventsPerChild[i];
            if (indexFrom != i && eventsChild != null) {
                if (eventsChild instanceof MatchedEventMap) {
                    listArray.add(index++, Collections.singletonList((MatchedEventMap) eventsChild));
                } else {
                    listArray.add(index++, (List<MatchedEventMap>) eventsChild);
                }
            }
        }

        // Recusively generate MatchedEventMap instances for all accumulated events
        List<MatchedEventMap> results = new ArrayList<MatchedEventMap>();
        generateMatchEvents(listArray, 0, results, matchEvent);

        return results;
    }

    /**
     * For each combination of MatchedEventMap instance in all collections, add an entry to the list.
     * Recursive method.
     *
     * @param eventList  is an array of lists containing MatchedEventMap instances to combine
     * @param index      is the current index into the array
     * @param result     is the resulting list of MatchedEventMap
     * @param matchEvent is the start MatchedEventMap to generate from
     */
    protected static void generateMatchEvents(ArrayList<List<MatchedEventMap>> eventList,
                                              int index,
                                              List<MatchedEventMap> result,
                                              MatchedEventMap matchEvent) {
        List<MatchedEventMap> events = eventList.get(index);

        for (MatchedEventMap theEvent : events) {
            MatchedEventMap current = matchEvent.shallowCopy();
            current.merge(theEvent);

            // If this is the very last list in the array of lists, add accumulated MatchedEventMap events to result
            if ((index + 1) == eventList.size()) {
                result.add(current);
            } else {
                // make a copy of the event collection and hand to next list of events
                generateMatchEvents(eventList, index + 1, result, current);
            }
        }
    }

    public final void quit() {
        if (eventsPerChild == null) {
            return;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternAndQuit(evalAndNode);
        }
        quitInternal();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternAndQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitAnd(evalAndNode.getFactoryNode(), this, eventsPerChild);
        for (EvalStateNode node : activeChildNodes) {
            if (node != null) {
                node.accept(visitor);
            }
        }
    }

    public final String toString() {
        return "EvalAndStateNode";
    }

    public static void addMatchEvent(Object[] eventsPerChild, int indexFrom, MatchedEventMap matchEvent) {
        Object matchEventHolder = eventsPerChild[indexFrom];
        if (matchEventHolder == null) {
            eventsPerChild[indexFrom] = matchEvent;
        } else if (matchEventHolder instanceof MatchedEventMap) {
            List<MatchedEventMap> list = new ArrayList<MatchedEventMap>(4);
            list.add((MatchedEventMap) matchEventHolder);
            list.add(matchEvent);
            eventsPerChild[indexFrom] = list;
        } else {
            List<MatchedEventMap> list = (List<MatchedEventMap>) matchEventHolder;
            list.add(matchEvent);
        }
    }

    private void quitInternal() {
        for (EvalStateNode child : activeChildNodes) {
            if (child != null) {
                child.quit();
            }
        }
        Arrays.fill(activeChildNodes, null);
        eventsPerChild = null;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalAndStateNode.class);
}
