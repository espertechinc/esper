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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * This class represents the state of a match-until node in the evaluation state tree.
 */
public class EvalMatchUntilStateNode extends EvalStateNode implements Evaluator {
    protected final EvalMatchUntilNode evalMatchUntilNode;
    protected MatchedEventMap beginState;
    protected final ArrayList<EventBean>[] matchedEventArrays;

    protected EvalStateNode stateMatcher;
    protected EvalStateNode stateUntil;
    protected int numMatches;
    protected Integer lowerbounds;
    protected Integer upperbounds;

    /**
     * Constructor.
     *
     * @param parentNode         is the parent evaluator to call to indicate truth value
     * @param evalMatchUntilNode is the factory node associated to the state
     */
    public EvalMatchUntilStateNode(Evaluator parentNode,
                                   EvalMatchUntilNode evalMatchUntilNode) {
        super(parentNode);

        this.matchedEventArrays = (ArrayList<EventBean>[]) new ArrayList[evalMatchUntilNode.getFactoryNode().getTagsArrayed().length];
        this.evalMatchUntilNode = evalMatchUntilNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        boolean quit = PatternConsumptionUtil.containsEvent(matchEvent, beginState);
        if (!quit) {
            for (ArrayList<EventBean> list : matchedEventArrays) {
                if (list == null) {
                    continue;
                }
                for (EventBean event : list) {
                    if (matchEvent.contains(event)) {
                        quit = true;
                        break;
                    }
                }
                if (quit) {
                    break;
                }
            }
        }
        if (quit) {
            quit();
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            if (stateMatcher != null) {
                stateMatcher.removeMatch(matchEvent);
            }
            if (stateUntil != null) {
                stateUntil.removeMatch(matchEvent);
            }
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalMatchUntilNode;
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternMatchUntilStart(evalMatchUntilNode, beginState);
        }
        this.beginState = beginState;

        EvalNode childMatcher = evalMatchUntilNode.getChildNodeSub();
        stateMatcher = childMatcher.newState(this, null, 0L);

        if (evalMatchUntilNode.getChildNodeUntil() != null) {
            EvalNode childUntil = evalMatchUntilNode.getChildNodeUntil();
            stateUntil = childUntil.newState(this, null, 0L);
        }

        // start until first, it controls the expression
        // if the same event fires both match and until, the match should not count
        if (stateUntil != null) {
            stateUntil.start(beginState);
        }

        EvalMatchUntilStateBounds bounds = EvalMatchUntilStateBounds.initBounds(evalMatchUntilNode.getFactoryNode(), beginState, evalMatchUntilNode.getContext());
        this.lowerbounds = bounds.getLowerbounds();
        this.upperbounds = bounds.getUpperbounds();

        if (stateMatcher != null) {
            stateMatcher.start(beginState);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternMatchUntilStart();
        }
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternMatchUntilEvaluateTrue(evalMatchUntilNode, matchEvent, fromNode == stateUntil);
        }
        boolean isMatcher = false;
        if (fromNode == stateMatcher) {
            // Add the additional tagged events to the list for later posting
            isMatcher = true;
            numMatches++;
            int[] tags = evalMatchUntilNode.getFactoryNode().getTagsArrayed();
            for (int i = 0; i < tags.length; i++) {
                Object theEvent = matchEvent.getMatchingEventAsObject(tags[i]);
                if (theEvent != null) {
                    if (matchedEventArrays[i] == null) {
                        matchedEventArrays[i] = new ArrayList<EventBean>();
                    }
                    if (theEvent instanceof EventBean) {
                        matchedEventArrays[i].add((EventBean) theEvent);
                    } else {
                        EventBean[] arrayEvents = (EventBean[]) theEvent;
                        matchedEventArrays[i].addAll(Arrays.asList(arrayEvents));
                    }

                }
            }
        }

        if (isQuitted) {
            if (isMatcher) {
                stateMatcher = null;
            } else {
                stateUntil = null;
            }
        }

        // handle matcher evaluating true
        if (isMatcher) {
            if ((isTightlyBound()) && (numMatches == lowerbounds)) {
                quitInternal();
                MatchedEventMap consolidated = consolidate(matchEvent, matchedEventArrays, evalMatchUntilNode.getFactoryNode().getTagsArrayed());
                this.getParentEvaluator().evaluateTrue(consolidated, this, true, optionalTriggeringEvent);
            } else {
                // restart or keep started if not bounded, or not upper bounds, or upper bounds not reached
                boolean restart = (!isBounded()) ||
                        (upperbounds == null) ||
                        (upperbounds > numMatches);
                if (stateMatcher == null) {
                    if (restart) {
                        EvalNode childMatcher = evalMatchUntilNode.getChildNodeSub();
                        stateMatcher = childMatcher.newState(this, null, 0L);
                        stateMatcher.start(beginState);
                    }
                } else {
                    if (!restart) {
                        stateMatcher.quit();
                        stateMatcher = null;
                    }
                }
            }
        } else {
            // handle until-node
            quitInternal();

            // consolidate multiple matched events into a single event
            MatchedEventMap consolidated = consolidate(matchEvent, matchedEventArrays, evalMatchUntilNode.getFactoryNode().getTagsArrayed());

            if ((lowerbounds != null) && (numMatches < lowerbounds)) {
                this.getParentEvaluator().evaluateFalse(this, true);
            } else {
                this.getParentEvaluator().evaluateTrue(consolidated, this, true, optionalTriggeringEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternMatchUntilEvaluateTrue(stateMatcher == null && stateUntil == null);
        }
    }

    public static MatchedEventMap consolidate(MatchedEventMap beginState, ArrayList<EventBean>[] matchedEventList, int[] tagsArrayed) {
        if (tagsArrayed == null) {
            return beginState;
        }

        for (int i = 0; i < tagsArrayed.length; i++) {
            if (matchedEventList[i] == null) {
                continue;
            }
            EventBean[] eventsForTag = matchedEventList[i].toArray(new EventBean[matchedEventList[i].size()]);
            beginState.add(tagsArrayed[i], eventsForTag);
        }

        return beginState;
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternMatchUntilEvalFalse(evalMatchUntilNode, fromNode == stateUntil);
        }
        boolean isMatcher = false;
        if (fromNode == stateMatcher) {
            isMatcher = true;
        }

        if (isMatcher) {
            stateMatcher.quit();
            stateMatcher = null;
        } else {
            stateUntil.quit();
            stateUntil = null;
        }
        this.getParentEvaluator().evaluateFalse(this, true);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternMatchUntilEvalFalse();
        }
    }

    public final void quit() {
        if (stateMatcher == null && stateUntil == null) {
            return;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternMatchUntilQuit(evalMatchUntilNode);
        }
        quitInternal();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternMatchUntilQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitMatchUntil(evalMatchUntilNode.getFactoryNode(), this, matchedEventArrays, beginState);
        if (stateMatcher != null) {
            stateMatcher.accept(visitor);
        }
        if (stateUntil != null) {
            stateUntil.accept(visitor);
        }
    }

    public final String toString() {
        return "EvalMatchUntilStateNode";
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    private boolean isTightlyBound() {
        return lowerbounds != null && upperbounds != null && upperbounds.equals(lowerbounds);
    }

    private boolean isBounded() {
        return lowerbounds != null || upperbounds != null;
    }

    private void quitInternal() {
        if (stateMatcher != null) {
            stateMatcher.quit();
            stateMatcher = null;
        }
        if (stateUntil != null) {
            stateUntil.quit();
            stateUntil = null;
        }
    }
}
