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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.spec.MatchRecognizeInterval;
import com.espertech.esper.epl.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * View for match recognize support.
 */
public class EventRowRegexNFAView extends ViewSupport implements StopCallback, EventRowRegexNFAViewService, EventRowRegexNFAViewScheduleCallback {
    private static final Logger log = LoggerFactory.getLogger(EventRowRegexNFAView.class);
    private static final boolean IS_DEBUG = false;
    private static final Iterator<EventBean> NULL_ITERATOR = new SingleEventIterator(null);

    private final EventRowRegexNFAViewFactory factory;

    private final AgentInstanceContext agentInstanceContext;

    // for interval-handling
    protected final EventRowRegexNFAViewScheduler scheduler;

    private final RegexPartitionStateRandomAccessGetter prevGetter;
    private final ObjectArrayBackedEventBean compositeEventBean;

    // state
    protected RegexPartitionStateRepo regexPartitionStateRepo;
    private LinkedHashSet<EventBean> windowMatchedEventset; // this is NOT per partition - some optimizations are done for batch-processing (minus is out-of-sequence in partition) 

    public EventRowRegexNFAView(EventRowRegexNFAViewFactory factory,
                                AgentInstanceContext agentInstanceContext,
                                EventRowRegexNFAViewScheduler scheduler) {
        this.factory = factory;
        this.compositeEventBean = new ObjectArrayEventBean(new Object[factory.variableStreams.size()], factory.compositeEventType);
        this.scheduler = scheduler;
        this.agentInstanceContext = agentInstanceContext;

        this.windowMatchedEventset = new LinkedHashSet<EventBean>();

        // handle "previous" function nodes (performance-optimized for direct index access)
        if (!factory.callbacksPerIndex.isEmpty()) {
            // Build an array of indexes
            int[] randomAccessIndexesRequested = new int[factory.callbacksPerIndex.size()];
            int count = 0;
            for (Map.Entry<Integer, List<ExprPreviousMatchRecognizeNode>> entry : factory.callbacksPerIndex.entrySet()) {
                randomAccessIndexesRequested[count] = entry.getKey();
                count++;
            }
            prevGetter = new RegexPartitionStateRandomAccessGetter(randomAccessIndexesRequested, factory.isUnbound);
        } else {
            prevGetter = null;
        }

        // create state repository
        RegexHandlerFactory repoFactory = agentInstanceContext.getStatementContext().getRegexPartitionStateRepoFactory();
        RegexPartitionTerminationStateComparator terminationStateCompare = new RegexPartitionTerminationStateComparator(factory.multimatchStreamNumToVariable, factory.variableStreams);
        if (this.factory.matchRecognizeSpec.getPartitionByExpressions().isEmpty()) {
            regexPartitionStateRepo = repoFactory.makeSingle(prevGetter, agentInstanceContext, this, factory.matchRecognizeSpec.getInterval() != null, terminationStateCompare);
        } else {
            RegexPartitionStateRepoGroupMeta stateRepoGroupMeta = new RegexPartitionStateRepoGroupMeta(factory.matchRecognizeSpec.getInterval() != null,
                    ExprNodeUtilityCore.toArray(factory.matchRecognizeSpec.getPartitionByExpressions()),
                    factory.partitionByEvals, agentInstanceContext);
            regexPartitionStateRepo = repoFactory.makePartitioned(prevGetter, stateRepoGroupMeta, agentInstanceContext, this, factory.matchRecognizeSpec.getInterval() != null, terminationStateCompare);
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.removeSchedule();
        }
        if (factory.isTrackMaxStates) {
            int size = regexPartitionStateRepo.getStateCount();
            MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
            poolSvc.getEngineSvc().decreaseCount(agentInstanceContext, size);
            poolSvc.getStmtHandler().decreaseCount(size);
        }
        regexPartitionStateRepo.destroy();
    }

    public void init(EventBean[] newEvents) {
        updateInternal(newEvents, null, false);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        updateInternal(newData, oldData, true);
    }

    private void updateInternal(EventBean[] newData, EventBean[] oldData, boolean postOutput) {
        if (factory.isIterateOnly) {
            if (oldData != null) {
                regexPartitionStateRepo.removeOld(oldData, false, new boolean[oldData.length]);
            }
            if (newData != null) {
                for (EventBean newEvent : newData) {
                    RegexPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
                    if ((partitionState != null) && (partitionState.getRandomAccess() != null)) {
                        partitionState.getRandomAccess().newEventPrepare(newEvent);
                    }
                }
            }
            return;
        }

        if (oldData != null) {
            boolean isOutOfSequenceRemove = false;

            EventBean first = null;
            if (!windowMatchedEventset.isEmpty()) {
                first = windowMatchedEventset.iterator().next();
            }

            // remove old data, if found in set
            boolean[] found = new boolean[oldData.length];
            int count = 0;

            // detect out-of-sequence removes
            for (EventBean oldEvent : oldData) {
                boolean removed = windowMatchedEventset.remove(oldEvent);
                if (removed) {
                    if ((oldEvent != first) && (first != null)) {
                        isOutOfSequenceRemove = true;
                    }
                    found[count++] = true;
                    if (!windowMatchedEventset.isEmpty()) {
                        first = windowMatchedEventset.iterator().next();
                    }
                }
            }

            // reset, rebuilding state
            if (isOutOfSequenceRemove) {
                if (factory.isTrackMaxStates) {
                    int size = regexPartitionStateRepo.getStateCount();
                    MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
                    poolSvc.getEngineSvc().decreaseCount(agentInstanceContext, size);
                    poolSvc.getStmtHandler().decreaseCount(size);
                }

                regexPartitionStateRepo = regexPartitionStateRepo.copyForIterate(true);
                Iterator<EventBean> parentEvents = this.getParent().iterator();
                EventRowRegexIteratorResult iteratorResult = processIterator(true, parentEvents, regexPartitionStateRepo);
                regexPartitionStateRepo.setEventSequenceNum(iteratorResult.getEventSequenceNum());
            } else {
                // remove old events from repository - and let the repository know there are no interesting events left
                int numRemoved = regexPartitionStateRepo.removeOld(oldData, windowMatchedEventset.isEmpty(), found);

                if (factory.isTrackMaxStates) {
                    MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
                    poolSvc.getEngineSvc().decreaseCount(agentInstanceContext, numRemoved);
                    poolSvc.getStmtHandler().decreaseCount(numRemoved);
                }
            }
        }

        if (newData == null) {
            return;
        }

        List<RegexNFAStateEntry> endStates = new ArrayList<RegexNFAStateEntry>();
        List<RegexNFAStateEntry> terminationStatesAll = null;

        for (EventBean newEvent : newData) {
            List<RegexNFAStateEntry> nextStates = new ArrayList<RegexNFAStateEntry>(2);
            int eventSequenceNumber = regexPartitionStateRepo.incrementAndGetEventSequenceNum();

            // get state holder for this event
            RegexPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
            Iterator<RegexNFAStateEntry> currentStatesIterator = partitionState.getCurrentStatesIterator();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegEx(newEvent, partitionState);
            }

            if (partitionState.getRandomAccess() != null) {
                partitionState.getRandomAccess().newEventPrepare(newEvent);
            }

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || IS_DEBUG) {
                log.info("Evaluating event " + newEvent.getUnderlying() + "\n" +
                        "current : " + EventRowRegexNFAViewUtil.printStates(partitionState.getCurrentStatesForPrint(), factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable));
            }

            List<RegexNFAStateEntry> terminationStates = step(false, currentStatesIterator, newEvent, nextStates, endStates, !factory.isUnbound, eventSequenceNumber, partitionState.getOptionalKeys());

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || IS_DEBUG) {
                log.info("Evaluated event " + newEvent.getUnderlying() + "\n" +
                        "next : " + EventRowRegexNFAViewUtil.printStates(nextStates, factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable) + "\n" +
                        "end : " + EventRowRegexNFAViewUtil.printStates(endStates, factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable));
            }

            // add termination states, for use with interval and "or terminated"
            if (terminationStates != null) {
                if (terminationStatesAll == null) {
                    terminationStatesAll = terminationStates;
                } else {
                    terminationStatesAll.addAll(terminationStates);
                }
            }

            partitionState.setCurrentStates(nextStates);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegEx(partitionState, endStates, terminationStates);
            }
        }

        if (endStates.isEmpty() && (!factory.isOrTerminated || terminationStatesAll == null)) {
            return;
        }

        // perform inter-ranking and elimination of duplicate matches
        if (!factory.matchRecognizeSpec.isAllMatches()) {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        // handle interval for the set of matches
        if (factory.matchRecognizeSpec.getInterval() != null) {
            Iterator<RegexNFAStateEntry> it = endStates.iterator();
            for (; it.hasNext(); ) {
                RegexNFAStateEntry endState = it.next();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qRegIntervalState(endState, factory.variableStreams, factory.multimatchStreamNumToVariable, agentInstanceContext.getStatementContext().getSchedulingService().getTime());
                }
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aRegIntervalState(false);
                    }
                    continue;
                }

                // determine whether to schedule
                boolean scheduleDelivery;
                if (!factory.isOrTerminated) {
                    scheduleDelivery = true;
                } else {
                    // determine whether there can be more matches
                    if (endState.getState().getNextStates().size() == 1 &&
                            endState.getState().getNextStates().get(0) instanceof RegexNFAStateEnd) {
                        scheduleDelivery = false;
                    } else {
                        scheduleDelivery = true;
                    }
                }

                // only schedule if not an end-state or not or-terminated
                if (scheduleDelivery) {
                    long matchBeginTime = endState.getMatchBeginEventTime();
                    long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                    long deltaFromStart = current - matchBeginTime;
                    long deltaUntil = computeScheduleForwardDelta(current, deltaFromStart);

                    if (regexPartitionStateRepo.getScheduleState().containsKey(matchBeginTime)) {
                        scheduleCallback(deltaUntil, endState);
                        if (InstrumentationHelper.ENABLED) {
                            InstrumentationHelper.get().aRegIntervalState(true);
                        }
                        it.remove();
                    } else {
                        if (deltaFromStart < deltaUntil) {
                            scheduleCallback(deltaUntil, endState);
                            if (InstrumentationHelper.ENABLED) {
                                InstrumentationHelper.get().aRegIntervalState(true);
                            }
                            it.remove();
                        } else {
                            if (InstrumentationHelper.ENABLED) {
                                InstrumentationHelper.get().aRegIntervalState(false);
                            }
                        }
                    }
                } else {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aRegIntervalState(false);
                    }
                }
            }

            // handle termination states - those that terminated the pattern and remove the callback
            if (factory.isOrTerminated && terminationStatesAll != null) {
                for (RegexNFAStateEntry terminationState : terminationStatesAll) {
                    RegexPartitionState partitionState = regexPartitionStateRepo.getState(terminationState.getPartitionKey());
                    if (partitionState == null) {
                        log.warn("Null partition state encountered, skipping row");
                        continue;
                    }

                    removeScheduleAddEndState(terminationState, endStates);
                }

                // rank
                if (!factory.matchRecognizeSpec.isAllMatches()) {
                    endStates = rankEndStatesMultiPartition(endStates);
                }
            }

            if (endStates.isEmpty()) {
                return;
            }
        } else if (factory.matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW) {
            // handle skip for incremental mode
            Iterator<RegexNFAStateEntry> endStateIter = endStates.iterator();
            for (; endStateIter.hasNext(); ) {
                RegexNFAStateEntry endState = endStateIter.next();
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RegexNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (; stateIter.hasNext(); ) {
                    RegexNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchEndEventSeqNo()) {
                        stateIter.remove();
                    }
                }
            }
        } else if (factory.matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW) {
            Iterator<RegexNFAStateEntry> endStateIter = endStates.iterator();
            for (; endStateIter.hasNext(); ) {
                RegexNFAStateEntry endState = endStateIter.next();
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RegexNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (; stateIter.hasNext(); ) {
                    RegexNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchBeginEventSeqNo()) {
                        stateIter.remove();
                    }
                }
            }
        }

        EventBean[] outBeans = new EventBean[endStates.size()];
        int count = 0;
        for (RegexNFAStateEntry endState : endStates) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegMeasure(endState, factory.variableStreams, factory.multimatchStreamNumToVariable);
            }
            outBeans[count] = generateOutputRow(endState);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegMeasure(outBeans[count]);
            }
            count++;

            // check partition state - if empty delete (no states and no random access)
            if (endState.getPartitionKey() != null) {
                RegexPartitionState state = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (state.isEmptyCurrentState() && state.getRandomAccess() == null) {
                    regexPartitionStateRepo.removeState(endState.getPartitionKey());
                }
            }
        }

        if (postOutput) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegOut(outBeans);
            }
            updateChildren(outBeans, null);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegOut();
            }
        }
    }

    private long computeScheduleForwardDelta(long current, long deltaFromStart) {
        MatchRecognizeInterval interval = factory.matchRecognizeSpec.getInterval();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegIntervalValue(interval.getTimePeriodExpr());
        }
        if (interval.getTimeDeltaComputation() == null) {
            ExprTimePeriodEvalDeltaConst timeDeltaComputation = interval.getTimePeriodExpr().constEvaluator(new ExprEvaluatorContextStatement(agentInstanceContext.getStatementContext(), false));
            interval.setTimeDeltaComputation(timeDeltaComputation);
        }
        long result = interval.getTimeDeltaComputation().deltaAdd(current);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegIntervalValue(result);
        }
        return result - deltaFromStart;
    }

    private RegexNFAStateEntry rankEndStates(List<RegexNFAStateEntry> endStates) {

        // sort by end-event descending (newest first)
        Collections.sort(endStates, EventRowRegexHelper.END_STATE_COMPARATOR);

        // find the earliest begin-event
        RegexNFAStateEntry found = null;
        int min = Integer.MAX_VALUE;
        boolean multipleMinimums = false;
        for (RegexNFAStateEntry state : endStates) {
            if (state.getMatchBeginEventSeqNo() < min) {
                found = state;
                min = state.getMatchBeginEventSeqNo();
            } else if (state.getMatchBeginEventSeqNo() == min) {
                multipleMinimums = true;
            }
        }

        if (!multipleMinimums) {
            Collections.singletonList(found);
        }

        // compare greedy counts
        int[] best = null;
        found = null;
        for (RegexNFAStateEntry state : endStates) {
            if (state.getMatchBeginEventSeqNo() != min) {
                continue;
            }
            if (best == null) {
                best = state.getGreedycountPerState();
                found = state;
            } else {
                int[] current = state.getGreedycountPerState();
                if (compare(current, best)) {
                    best = current;
                    found = state;
                }
            }
        }

        return found;
    }

    private boolean compare(int[] current, int[] best) {
        for (RegexNFAState state : factory.allStates) {
            if (state.isGreedy() == null) {
                continue;
            }
            if (state.isGreedy()) {
                if (current[state.getNodeNumFlat()] > best[state.getNodeNumFlat()]) {
                    return true;
                }
            } else {
                if (current[state.getNodeNumFlat()] < best[state.getNodeNumFlat()]) {
                    return true;
                }
            }
        }

        return false;
    }

    private EventRowRegexIteratorResult processIterator(boolean isOutOfSeqDelete,
                                                        Iterator<EventBean> events,
                                                        RegexPartitionStateRepo regexPartitionStateRepo) {
        List<RegexNFAStateEntry> endStates = new ArrayList<RegexNFAStateEntry>();
        Iterator<RegexNFAStateEntry> currentStates;
        int eventSequenceNumber = 0;

        EventBean theEvent;
        for (; events.hasNext(); ) {
            List<RegexNFAStateEntry> nextStates = new ArrayList<RegexNFAStateEntry>(2);
            theEvent = events.next();
            eventSequenceNumber++;

            RegexPartitionState partitionState = regexPartitionStateRepo.getState(theEvent, false);
            currentStates = partitionState.getCurrentStatesIterator();

            if (partitionState.getRandomAccess() != null) {
                partitionState.getRandomAccess().existingEventPrepare(theEvent);
            }

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || IS_DEBUG) {
                log.info("Evaluating event " + theEvent.getUnderlying() + "\n" +
                        "current : " + EventRowRegexNFAViewUtil.printStates(partitionState.getCurrentStatesForPrint(), factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable));
            }

            step(!isOutOfSeqDelete, currentStates, theEvent, nextStates, endStates, false, eventSequenceNumber, partitionState.getOptionalKeys());

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || IS_DEBUG) {
                log.info("Evaluating event " + theEvent.getUnderlying() + "\n" +
                        "next : " + EventRowRegexNFAViewUtil.printStates(nextStates, factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable) + "\n" +
                        "end : " + EventRowRegexNFAViewUtil.printStates(endStates, factory.streamVariables, factory.variableStreams, factory.multimatchStreamNumToVariable));
            }

            partitionState.setCurrentStates(nextStates);
        }

        return new EventRowRegexIteratorResult(endStates, eventSequenceNumber);
    }

    public EventType getEventType() {
        return factory.rowEventType;
    }

    public Iterator<EventBean> iterator() {
        if (factory.isUnbound) {
            return NULL_ITERATOR;
        }

        Iterator<EventBean> it = parent.iterator();

        RegexPartitionStateRepo regexPartitionStateRepoNew = regexPartitionStateRepo.copyForIterate(false);

        EventRowRegexIteratorResult iteratorResult = processIterator(false, it, regexPartitionStateRepoNew);
        List<RegexNFAStateEntry> endStates = iteratorResult.getEndStates();
        if (endStates.isEmpty()) {
            return NULL_ITERATOR;
        } else {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        List<EventBean> output = new ArrayList<EventBean>();
        for (RegexNFAStateEntry endState : endStates) {
            output.add(generateOutputRow(endState));
        }
        return output.iterator();
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
        regexPartitionStateRepo.accept(visitor);
    }

    private List<RegexNFAStateEntry> rankEndStatesMultiPartition(List<RegexNFAStateEntry> endStates) {
        if (endStates.isEmpty()) {
            return endStates;
        }
        if (endStates.size() == 1) {
            return endStates;
        }

        // unpartitioned case -
        if (factory.matchRecognizeSpec.getPartitionByExpressions().isEmpty()) {
            return rankEndStatesWithinPartitionByStart(endStates);
        }

        // partitioned case - structure end states by partition
        Map<Object, Object> perPartition = new LinkedHashMap<Object, Object>();
        for (RegexNFAStateEntry endState : endStates) {
            Object value = perPartition.get(endState.getPartitionKey());
            if (value == null) {
                perPartition.put(endState.getPartitionKey(), endState);
            } else if (value instanceof List) {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) value;
                entries.add(endState);
            } else {
                List<RegexNFAStateEntry> entries = new ArrayList<RegexNFAStateEntry>();
                entries.add((RegexNFAStateEntry) value);
                entries.add(endState);
                perPartition.put(endState.getPartitionKey(), entries);
            }
        }

        List<RegexNFAStateEntry> finalEndStates = new ArrayList<RegexNFAStateEntry>();
        for (Map.Entry<Object, Object> entry : perPartition.entrySet()) {
            if (entry.getValue() instanceof RegexNFAStateEntry) {
                finalEndStates.add((RegexNFAStateEntry) entry.getValue());
            } else {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) entry.getValue();
                finalEndStates.addAll(rankEndStatesWithinPartitionByStart(entries));
            }
        }
        return finalEndStates;
    }

    private List<RegexNFAStateEntry> rankEndStatesWithinPartitionByStart(List<RegexNFAStateEntry> endStates) {
        if (endStates.isEmpty()) {
            return endStates;
        }
        if (endStates.size() == 1) {
            return endStates;
        }

        TreeMap<Integer, Object> endStatesPerBeginEvent = new TreeMap<Integer, Object>();
        for (RegexNFAStateEntry entry : endStates) {
            Integer beginNum = entry.getMatchBeginEventSeqNo();
            Object value = endStatesPerBeginEvent.get(beginNum);
            if (value == null) {
                endStatesPerBeginEvent.put(beginNum, entry);
            } else if (value instanceof List) {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) value;
                entries.add(entry);
            } else {
                List<RegexNFAStateEntry> entries = new ArrayList<RegexNFAStateEntry>();
                entries.add((RegexNFAStateEntry) value);
                entries.add(entry);
                endStatesPerBeginEvent.put(beginNum, entries);
            }
        }

        if (endStatesPerBeginEvent.size() == 1) {
            List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) endStatesPerBeginEvent.values().iterator().next();
            if (factory.matchRecognizeSpec.isAllMatches()) {
                return endStatesUnranked;
            }
            RegexNFAStateEntry chosen = rankEndStates(endStatesUnranked);
            return Collections.singletonList(chosen);
        }

        List<RegexNFAStateEntry> endStatesRanked = new ArrayList<RegexNFAStateEntry>();
        Set<Integer> keyset = endStatesPerBeginEvent.keySet();
        Integer[] keys = keyset.toArray(new Integer[keyset.size()]);
        for (Integer key : keys) {
            Object value = endStatesPerBeginEvent.remove(key);
            if (value == null) {
                continue;
            }

            RegexNFAStateEntry entryTaken;
            if (value instanceof List) {
                List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) value;
                if (endStatesUnranked.isEmpty()) {
                    continue;
                }
                entryTaken = rankEndStates(endStatesUnranked);

                if (factory.matchRecognizeSpec.isAllMatches()) {
                    endStatesRanked.addAll(endStatesUnranked);  // we take all matches and don't rank except to determine skip-past
                } else {
                    endStatesRanked.add(entryTaken);
                }
            } else {
                entryTaken = (RegexNFAStateEntry) value;
                endStatesRanked.add(entryTaken);
            }
            // could be null as removals take place

            if (entryTaken != null) {
                if (factory.matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW) {
                    int skipPastRow = entryTaken.getMatchEndEventSeqNo();
                    removeSkippedEndStates(endStatesPerBeginEvent, skipPastRow);
                } else if (factory.matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW) {
                    int skipPastRow = entryTaken.getMatchBeginEventSeqNo();
                    removeSkippedEndStates(endStatesPerBeginEvent, skipPastRow);
                }
            }
        }

        return endStatesRanked;
    }

    private void removeSkippedEndStates(TreeMap<Integer, Object> endStatesPerEndEvent, int skipPastRow) {
        for (Map.Entry<Integer, Object> entry : endStatesPerEndEvent.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof List) {
                List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) value;
                Iterator<RegexNFAStateEntry> it = endStatesUnranked.iterator();
                for (; it.hasNext(); ) {
                    RegexNFAStateEntry endState = it.next();
                    if (endState.getMatchBeginEventSeqNo() <= skipPastRow) {
                        it.remove();
                    }
                }
            } else {
                RegexNFAStateEntry endState = (RegexNFAStateEntry) value;
                if (endState.getMatchBeginEventSeqNo() <= skipPastRow) {
                    endStatesPerEndEvent.put(entry.getKey(), null);
                }
            }
        }
    }

    private List<RegexNFAStateEntry> step(boolean skipTrackMaxState,
                                          Iterator<RegexNFAStateEntry> currentStatesIterator,
                                          EventBean theEvent,
                                          List<RegexNFAStateEntry> nextStates,
                                          List<RegexNFAStateEntry> endStates,
                                          boolean isRetainEventSet,
                                          int currentEventSequenceNumber,
                                          Object partitionKey) {
        List<RegexNFAStateEntry> terminationStates = null;  // always null or a list of entries (no singleton list)

        // handle current state matching
        for (; currentStatesIterator.hasNext(); ) {
            RegexNFAStateEntry currentState = currentStatesIterator.next();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegExState(currentState, factory.variableStreams, factory.multimatchStreamNumToVariable);
            }

            if (factory.isTrackMaxStates && !skipTrackMaxState) {
                MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
                poolSvc.getEngineSvc().decreaseCount(agentInstanceContext);
                poolSvc.getStmtHandler().decreaseCount();
            }

            EventBean[] eventsPerStream = currentState.getEventsPerStream();
            int currentStateStreamNum = currentState.getState().getStreamNum();
            eventsPerStream[currentStateStreamNum] = theEvent;
            if (factory.isDefineAsksMultimatches) {
                eventsPerStream[factory.numEventsEventsPerStreamDefine - 1] = getMultimatchState(currentState);
            }

            if (currentState.getState().matches(eventsPerStream, agentInstanceContext)) {
                if (isRetainEventSet) {
                    this.windowMatchedEventset.add(theEvent);
                }
                List<RegexNFAState> nextStatesFromHere = currentState.getState().getNextStates();

                // save state for each next state
                boolean copy = nextStatesFromHere.size() > 1;
                for (RegexNFAState next : nextStatesFromHere) {
                    EventBean[] eventsForState = eventsPerStream;
                    MultimatchState[] multimatches = currentState.getOptionalMultiMatches();
                    int[] greedyCounts = currentState.getGreedycountPerState();

                    if (copy) {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;

                        if (factory.isCollectMultimatches) {
                            multimatches = deepCopy(multimatches);
                        }
                    }

                    if (factory.isCollectMultimatches && (currentState.getState().isMultiple())) {
                        multimatches = addTag(currentState.getState().getStreamNum(), theEvent, multimatches);
                        eventsForState[currentStateStreamNum] = null; // remove event from evaluation list
                    }

                    if ((currentState.getState().isGreedy() != null) && (currentState.getState().isGreedy())) {
                        greedyCounts[currentState.getState().getNodeNumFlat()]++;
                    }

                    RegexNFAStateEntry entry = new RegexNFAStateEntry(currentState.getMatchBeginEventSeqNo(), currentState.getMatchBeginEventTime(), currentState.getState(), eventsForState, greedyCounts, multimatches, partitionKey);
                    if (next instanceof RegexNFAStateEnd) {
                        entry.setMatchEndEventSeqNo(currentEventSequenceNumber);
                        endStates.add(entry);
                    } else {
                        if (factory.isTrackMaxStates && !skipTrackMaxState) {
                            MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
                            boolean allow = poolSvc.getEngineSvc().tryIncreaseCount(agentInstanceContext);
                            if (allow) {
                                poolSvc.getStmtHandler().increaseCount();
                                entry.setState(next);
                                nextStates.add(entry);
                            }
                        } else {
                            entry.setState(next);
                            nextStates.add(entry);
                        }
                    }
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aRegExState(nextStates, factory.variableStreams, factory.multimatchStreamNumToVariable);
                }
            } else {
                // when not-matches
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aRegExState(Collections.<RegexNFAStateEntry>emptyList(), factory.variableStreams, factory.multimatchStreamNumToVariable);
                }

                // determine interval and or-terminated
                if (factory.isOrTerminated) {
                    eventsPerStream[currentStateStreamNum] = null;  // deassign
                    List<RegexNFAState> nextStatesFromHere = currentState.getState().getNextStates();

                    // save state for each next state
                    RegexNFAState theEndState = null;
                    for (RegexNFAState next : nextStatesFromHere) {
                        if (next instanceof RegexNFAStateEnd) {
                            theEndState = next;
                        }
                    }
                    if (theEndState != null) {
                        RegexNFAStateEntry entry = new RegexNFAStateEntry(currentState.getMatchBeginEventSeqNo(), currentState.getMatchBeginEventTime(), theEndState, eventsPerStream, currentState.getGreedycountPerState(), currentState.getOptionalMultiMatches(), partitionKey);
                        if (terminationStates == null) {
                            terminationStates = new ArrayList<RegexNFAStateEntry>();
                        }
                        terminationStates.add(entry);
                    }
                }
            }
        }

        // handle start states for the event
        for (RegexNFAState startState : factory.startStates) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegExStateStart(startState, factory.variableStreams, factory.multimatchStreamNumToVariable);
            }

            EventBean[] eventsPerStream = new EventBean[factory.numEventsEventsPerStreamDefine];
            int currentStateStreamNum = startState.getStreamNum();
            eventsPerStream[currentStateStreamNum] = theEvent;

            if (startState.matches(eventsPerStream, agentInstanceContext)) {
                if (isRetainEventSet) {
                    this.windowMatchedEventset.add(theEvent);
                }
                List<RegexNFAState> nextStatesFromHere = startState.getNextStates();

                // save state for each next state
                boolean copy = nextStatesFromHere.size() > 1;
                for (RegexNFAState next : nextStatesFromHere) {

                    if (factory.isTrackMaxStates && !skipTrackMaxState) {
                        MatchRecognizeStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc();
                        boolean allow = poolSvc.getEngineSvc().tryIncreaseCount(agentInstanceContext);
                        if (!allow) {
                            continue;
                        }
                        poolSvc.getStmtHandler().increaseCount();
                    }

                    EventBean[] eventsForState = eventsPerStream;
                    MultimatchState[] multimatches = factory.isCollectMultimatches ? new MultimatchState[factory.multimatchVariablesArray.length] : null;
                    int[] greedyCounts = new int[factory.allStates.length];

                    if (copy) {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;
                    }

                    if (factory.isCollectMultimatches && (startState.isMultiple())) {
                        multimatches = addTag(startState.getStreamNum(), theEvent, multimatches);
                        eventsForState[currentStateStreamNum] = null; // remove event from evaluation list
                    }

                    if ((startState.isGreedy() != null) && (startState.isGreedy())) {
                        greedyCounts[startState.getNodeNumFlat()]++;
                    }

                    long time = 0;
                    if (factory.matchRecognizeSpec.getInterval() != null) {
                        time = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                    }

                    RegexNFAStateEntry entry = new RegexNFAStateEntry(currentEventSequenceNumber, time, startState, eventsForState, greedyCounts, multimatches, partitionKey);
                    if (next instanceof RegexNFAStateEnd) {
                        entry.setMatchEndEventSeqNo(currentEventSequenceNumber);
                        endStates.add(entry);
                    } else {
                        entry.setState(next);
                        nextStates.add(entry);
                    }
                }
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegExStateStart(nextStates, factory.variableStreams, factory.multimatchStreamNumToVariable);
            }
        }

        return terminationStates;   // only for immediate use, not for scheduled use as no copy of state
    }

    private ObjectArrayBackedEventBean getMultimatchState(RegexNFAStateEntry currentState) {
        if (currentState.getOptionalMultiMatches() == null || !currentState.getState().isExprRequiresMultimatchState()) {
            return null;
        }
        Object[] props = factory.defineMultimatchEventBean.getProperties();
        MultimatchState[] states = currentState.getOptionalMultiMatches();
        for (int i = 0; i < props.length; i++) {
            MultimatchState state = states[i];
            if (state == null) {
                props[i] = null;
            } else {
                props[i] = state.getShrinkEventArray();
            }
        }
        return factory.defineMultimatchEventBean;
    }

    private MultimatchState[] deepCopy(MultimatchState[] multimatchStates) {
        if (multimatchStates == null) {
            return null;
        }

        MultimatchState[] copy = new MultimatchState[multimatchStates.length];
        for (int i = 0; i < copy.length; i++) {
            if (multimatchStates[i] != null) {
                copy[i] = new MultimatchState(multimatchStates[i]);
            }
        }

        return copy;
    }

    private MultimatchState[] addTag(int streamNum, EventBean theEvent, MultimatchState[] multimatches) {
        if (multimatches == null) {
            multimatches = new MultimatchState[factory.multimatchVariablesArray.length];
        }

        int index = factory.multimatchStreamNumToVariable[streamNum];
        MultimatchState state = multimatches[index];
        if (state == null) {
            multimatches[index] = new MultimatchState(theEvent);
            return multimatches;
        }

        multimatches[index].add(theEvent);
        return multimatches;
    }

    private EventBean generateOutputRow(RegexNFAStateEntry entry) {
        Object[] rowDataRaw = compositeEventBean.getProperties();

        // we first generate a raw row of <String, Object> for each variable name.
        for (Map.Entry<String, Pair<Integer, Boolean>> variableDef : factory.variableStreams.entrySet()) {
            if (!variableDef.getValue().getSecond()) {
                int index = variableDef.getValue().getFirst();
                rowDataRaw[index] = entry.getEventsPerStream()[index];
            }
        }
        if (factory.aggregationService != null) {
            factory.aggregationService.clearResults();
        }
        if (entry.getOptionalMultiMatches() != null) {
            MultimatchState[] multimatchState = entry.getOptionalMultiMatches();
            for (int i = 0; i < multimatchState.length; i++) {
                if (multimatchState[i] == null) {
                    rowDataRaw[factory.multimatchVariableToStreamNum[i]] = null;
                    continue;
                }
                EventBean[] multimatchEvents = multimatchState[i].getShrinkEventArray();
                rowDataRaw[factory.multimatchVariableToStreamNum[i]] = multimatchEvents;

                if (factory.aggregationService != null) {
                    EventBean[] eventsPerStream = entry.getEventsPerStream();
                    int streamNum = factory.multimatchVariableToStreamNum[i];

                    for (EventBean multimatchEvent : multimatchEvents) {
                        eventsPerStream[streamNum] = multimatchEvent;
                        factory.aggregationService.applyEnter(eventsPerStream, streamNum, agentInstanceContext);
                    }
                }
            }
        } else {
            for (int index : factory.multimatchVariableToStreamNum) {
                rowDataRaw[index] = null;
            }
        }

        Map<String, Object> row = new HashMap<String, Object>();
        int columnNum = 0;
        EventBean[] eventsPerStream = new EventBean[1];
        for (ExprEvaluator expression : factory.columnEvaluators) {
            eventsPerStream[0] = compositeEventBean;
            Object result = expression.evaluate(eventsPerStream, true, agentInstanceContext);
            row.put(factory.columnNames[columnNum], result);
            columnNum++;
        }

        return agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(row, factory.rowEventType);
    }

    private void scheduleCallback(long timeDelta, RegexNFAStateEntry endState) {
        long matchBeginTime = endState.getMatchBeginEventTime();
        if (regexPartitionStateRepo.getScheduleState().isEmpty()) {
            regexPartitionStateRepo.getScheduleState().putOrAdd(matchBeginTime, endState);
            scheduler.addSchedule(timeDelta);
        } else {
            boolean newEntry = regexPartitionStateRepo.getScheduleState().putOrAdd(matchBeginTime, endState);
            if (newEntry) {
                long currentFirstKey = regexPartitionStateRepo.getScheduleState().firstKey();
                if (currentFirstKey > matchBeginTime) {
                    scheduler.changeSchedule(timeDelta);
                }
            }
        }
    }

    private void removeScheduleAddEndState(RegexNFAStateEntry terminationState, List<RegexNFAStateEntry> foundEndStates) {
        long matchBeginTime = terminationState.getMatchBeginEventTime();
        boolean removedOne = regexPartitionStateRepo.getScheduleState().findRemoveAddToList(matchBeginTime, terminationState, foundEndStates);
        if (removedOne && regexPartitionStateRepo.getScheduleState().isEmpty()) {
            scheduler.removeSchedule();
        }
    }

    public void triggered() {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long intervalMSec = computeScheduleBackwardDelta(currentTime);
        if (regexPartitionStateRepo.getScheduleState().isEmpty()) {
            return;
        }

        List<RegexNFAStateEntry> indicatables = new ArrayList<RegexNFAStateEntry>();
        while (true) {
            long firstKey = regexPartitionStateRepo.getScheduleState().firstKey();
            long cutOffTime = currentTime - intervalMSec;
            if (firstKey > cutOffTime) {
                break;
            }

            regexPartitionStateRepo.getScheduleState().removeAddRemoved(firstKey, indicatables);

            if (regexPartitionStateRepo.getScheduleState().isEmpty()) {
                break;
            }
        }

        // schedule next
        if (!regexPartitionStateRepo.getScheduleState().isEmpty()) {
            long msecAfterCurrentTime = regexPartitionStateRepo.getScheduleState().firstKey() + intervalMSec - agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            scheduler.addSchedule(msecAfterCurrentTime);
        }

        if (!factory.matchRecognizeSpec.isAllMatches()) {
            indicatables = rankEndStatesMultiPartition(indicatables);
        }

        EventBean[] outBeans = new EventBean[indicatables.size()];
        int count = 0;
        for (RegexNFAStateEntry endState : indicatables) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qRegMeasure(endState, factory.variableStreams, factory.multimatchStreamNumToVariable);
            }
            outBeans[count] = generateOutputRow(endState);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aRegMeasure(outBeans[count]);
            }
            count++;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegOut(outBeans);
        }
        updateChildren(outBeans, null);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegOut();
        }
    }

    private long computeScheduleBackwardDelta(long currentTime) {
        MatchRecognizeInterval interval = factory.matchRecognizeSpec.getInterval();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qRegIntervalValue(interval.getTimePeriodExpr());
        }
        if (interval.getTimeDeltaComputation() == null) {
            ExprTimePeriodEvalDeltaConst timeDeltaComputation = interval.getTimePeriodExpr().constEvaluator(new ExprEvaluatorContextStatement(agentInstanceContext.getStatementContext(), false));
            interval.setTimeDeltaComputation(timeDeltaComputation);
        }
        long result = interval.getTimeDeltaComputation().deltaSubtract(currentTime);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aRegIntervalValue(result);
        }
        return result;
    }

    public RegexExprPreviousEvalStrategy getPreviousEvaluationStrategy() {
        return prevGetter;
    }

    public EventRowRegexNFAViewFactory getFactory() {
        return factory;
    }
}
