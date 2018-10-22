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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAState;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEndEval;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEntry;
import com.espertech.esper.common.internal.epl.rowrecog.state.*;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * View for match recognize support.
 */
public class RowRecogNFAView extends ViewSupport implements AgentInstanceStopCallback, RowRecogNFAViewService, RowRecogNFAViewScheduleCallback {
    private static final Logger log = LoggerFactory.getLogger(RowRecogNFAView.class);
    private static final boolean IS_DEBUG = false;

    private final RowRecogNFAViewFactory factory;
    protected final AgentInstanceContext agentInstanceContext;
    protected final RowRecogNFAViewScheduler scheduler; // for interval-handling

    private final RowRecogPreviousStrategyImpl rowRecogPreviousStrategy;
    private final ObjectArrayBackedEventBean compositeEventBean;

    // state
    protected RowRecogPartitionStateRepo regexPartitionStateRepo;
    private LinkedHashSet<EventBean> windowMatchedEventset; // this is NOT per partition - some optimizations are done for batch-processing (minus is out-of-sequence in partition)

    private final ObjectArrayBackedEventBean defineMultimatchEventBean;

    public RowRecogNFAView(RowRecogNFAViewFactory factory,
                           AgentInstanceContext agentInstanceContext,
                           RowRecogNFAViewScheduler scheduler) {
        this.factory = factory;
        RowRecogDesc desc = factory.getDesc();
        this.scheduler = scheduler;
        this.agentInstanceContext = agentInstanceContext;

        EventType compositeEventType = desc.getCompositeEventType();
        this.compositeEventBean = new ObjectArrayEventBean(new Object[compositeEventType.getPropertyNames().length], compositeEventType);

        EventType multimatchEventType = desc.getMultimatchEventType();
        defineMultimatchEventBean = multimatchEventType == null ? null : agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedObjectArray(new Object[multimatchEventType.getPropertyNames().length], multimatchEventType);

        this.windowMatchedEventset = new LinkedHashSet<>();

        // handle "previous" function nodes (performance-optimized for direct index access)
        if (desc.getPreviousRandomAccessIndexes() != null) {
            // Build an array of indexes
            rowRecogPreviousStrategy = new RowRecogPreviousStrategyImpl(desc.getPreviousRandomAccessIndexes(), factory.getDesc().isUnbound());
        } else {
            rowRecogPreviousStrategy = null;
        }

        // create state repository
        RowRecogStateRepoFactory repoFactory = agentInstanceContext.getRowRecogStateRepoFactory();
        RowRecogPartitionTerminationStateComparator terminationStateCompare = new RowRecogPartitionTerminationStateComparator(desc.getMultimatchStreamNumToVariable(), desc.getVariableStreams());
        if (desc.getPartitionEvalMayNull() == null) {
            regexPartitionStateRepo = repoFactory.makeSingle(rowRecogPreviousStrategy, agentInstanceContext, this, desc.isHasInterval(), terminationStateCompare);
        } else {
            RowRecogPartitionStateRepoGroupMeta stateRepoGroupMeta = new RowRecogPartitionStateRepoGroupMeta(desc.isHasInterval(),
                    desc.getPartitionEvalMayNull(), agentInstanceContext);
            regexPartitionStateRepo = repoFactory.makePartitioned(rowRecogPreviousStrategy, stateRepoGroupMeta, agentInstanceContext, this, desc.isHasInterval(), terminationStateCompare);
        }
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        updateInternal(newData, oldData, true);
    }

    public void stop(AgentInstanceStopServices services) {
        if (scheduler != null) {
            scheduler.removeSchedule();
        }
        if (factory.isTrackMaxStates()) {
            int size = regexPartitionStateRepo.getStateCount();
            RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
            poolSvc.getRuntimeSvc().decreaseCount(agentInstanceContext, size);
            poolSvc.getStmtHandler().decreaseCount(size);
        }
        regexPartitionStateRepo.destroy();
    }

    private void updateInternal(EventBean[] newData, EventBean[] oldData, boolean postOutput) {
        RowRecogDesc desc = factory.getDesc();
        if (desc.isIterateOnly()) {
            if (oldData != null) {
                regexPartitionStateRepo.removeOld(oldData, false, new boolean[oldData.length]);
            }
            if (newData != null) {
                for (EventBean newEvent : newData) {
                    RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
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
                if (factory.isTrackMaxStates()) {
                    int size = regexPartitionStateRepo.getStateCount();
                    RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
                    poolSvc.getRuntimeSvc().decreaseCount(agentInstanceContext, size);
                    poolSvc.getStmtHandler().decreaseCount(size);
                }

                regexPartitionStateRepo = regexPartitionStateRepo.copyForIterate(true);
                Iterator<EventBean> parentEvents = this.getParent().iterator();
                RowRecogIteratorResult iteratorResult = processIterator(true, parentEvents, regexPartitionStateRepo);
                regexPartitionStateRepo.setEventSequenceNum(iteratorResult.getEventSequenceNum());
            } else {
                // remove old events from repository - and let the repository know there are no interesting events left
                int numRemoved = regexPartitionStateRepo.removeOld(oldData, windowMatchedEventset.isEmpty(), found);

                if (factory.isTrackMaxStates()) {
                    RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
                    poolSvc.getRuntimeSvc().decreaseCount(agentInstanceContext, numRemoved);
                    poolSvc.getStmtHandler().decreaseCount(numRemoved);
                }
            }
        }

        if (newData == null) {
            return;
        }

        List<RowRecogNFAStateEntry> endStates = new ArrayList<>();
        List<RowRecogNFAStateEntry> terminationStatesAll = null;

        for (EventBean newEvent : newData) {
            List<RowRecogNFAStateEntry> nextStates = new ArrayList<>(2);
            int eventSequenceNumber = regexPartitionStateRepo.incrementAndGetEventSequenceNum();

            // get state holder for this event
            RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
            Iterator<RowRecogNFAStateEntry> currentStatesIterator = partitionState.getCurrentStatesIterator();
            agentInstanceContext.getInstrumentationProvider().qRegEx(newEvent, partitionState);

            if (partitionState.getRandomAccess() != null) {
                partitionState.getRandomAccess().newEventPrepare(newEvent);
            }

            List<RowRecogNFAStateEntry> terminationStates = step(false, currentStatesIterator, newEvent, nextStates, endStates, !desc.isUnbound(), eventSequenceNumber, partitionState.getOptionalKeys());

            // add termination states, for use with interval and "or terminated"
            if (terminationStates != null) {
                if (terminationStatesAll == null) {
                    terminationStatesAll = terminationStates;
                } else {
                    terminationStatesAll.addAll(terminationStates);
                }
            }

            partitionState.setCurrentStates(nextStates);
            agentInstanceContext.getInstrumentationProvider().aRegEx(partitionState, endStates, terminationStates);
        }

        if (endStates.isEmpty() && (!desc.isOrTerminated() || terminationStatesAll == null)) {
            return;
        }

        // perform inter-ranking and elimination of duplicate matches
        if (!desc.isAllMatches()) {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        // handle interval for the set of matches
        if (desc.isHasInterval()) {
            Iterator<RowRecogNFAStateEntry> it = endStates.iterator();
            for (; it.hasNext(); ) {
                RowRecogNFAStateEntry endState = it.next();
                agentInstanceContext.getInstrumentationProvider().qRegIntervalState(endState, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable(), agentInstanceContext.getStatementContext().getSchedulingService().getTime());

                RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    agentInstanceContext.getInstrumentationProvider().aRegIntervalState(false);
                    continue;
                }

                // determine whether to schedule
                boolean scheduleDelivery;
                if (!desc.isOrTerminated()) {
                    scheduleDelivery = true;
                } else {
                    // determine whether there can be more matches
                    if (endState.getState().getNextStates().length == 1 &&
                            endState.getState().getNextStates()[0] instanceof RowRecogNFAStateEndEval) {
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
                        agentInstanceContext.getInstrumentationProvider().aRegIntervalState(true);
                        it.remove();
                    } else {
                        if (deltaFromStart < deltaUntil) {
                            scheduleCallback(deltaUntil, endState);
                            agentInstanceContext.getInstrumentationProvider().aRegIntervalState(true);
                            it.remove();
                        } else {
                            agentInstanceContext.getInstrumentationProvider().aRegIntervalState(false);
                        }
                    }
                } else {
                    agentInstanceContext.getInstrumentationProvider().aRegIntervalState(false);
                }
            }

            // handle termination states - those that terminated the pattern and remove the callback
            if (desc.isOrTerminated() && terminationStatesAll != null) {
                for (RowRecogNFAStateEntry terminationState : terminationStatesAll) {
                    RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(terminationState.getPartitionKey());
                    if (partitionState == null) {
                        log.warn("Null partition state encountered, skipping row");
                        continue;
                    }

                    removeScheduleAddEndState(terminationState, endStates);
                }

                // rank
                if (!desc.isAllMatches()) {
                    endStates = rankEndStatesMultiPartition(endStates);
                }
            }

            if (endStates.isEmpty()) {
                return;
            }
        } else if (desc.getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW) {
            // handle skip for incremental mode
            Iterator<RowRecogNFAStateEntry> endStateIter = endStates.iterator();
            for (; endStateIter.hasNext(); ) {
                RowRecogNFAStateEntry endState = endStateIter.next();
                RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RowRecogNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (; stateIter.hasNext(); ) {
                    RowRecogNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchEndEventSeqNo()) {
                        stateIter.remove();
                    }
                }
            }
        } else if (desc.getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW) {
            Iterator<RowRecogNFAStateEntry> endStateIter = endStates.iterator();
            for (; endStateIter.hasNext(); ) {
                RowRecogNFAStateEntry endState = endStateIter.next();
                RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null) {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RowRecogNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (; stateIter.hasNext(); ) {
                    RowRecogNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchBeginEventSeqNo()) {
                        stateIter.remove();
                    }
                }
            }
        }

        EventBean[] outBeans = new EventBean[endStates.size()];
        int count = 0;
        for (RowRecogNFAStateEntry endState : endStates) {
            agentInstanceContext.getInstrumentationProvider().qRegMeasure(endState, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());

            outBeans[count] = generateOutputRow(endState);

            agentInstanceContext.getInstrumentationProvider().aRegMeasure(outBeans[count]);
            count++;

            // check partition state - if empty delete (no states and no random access)
            if (endState.getPartitionKey() != null) {
                RowRecogPartitionState state = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (state.isEmptyCurrentState() && state.getRandomAccess() == null) {
                    regexPartitionStateRepo.removeState(endState.getPartitionKey());
                }
            }
        }

        if (postOutput) {
            agentInstanceContext.getInstrumentationProvider().qRegOut(outBeans);
            child.update(outBeans, null);
            agentInstanceContext.getInstrumentationProvider().aRegOut();
        }
    }

    private long computeScheduleForwardDelta(long current, long deltaFromStart) {
        agentInstanceContext.getInstrumentationProvider().qRegIntervalValue();
        long result = factory.getDesc().getIntervalCompute().deltaAdd(current, null, true, null);
        agentInstanceContext.getInstrumentationProvider().aRegIntervalValue(result);
        return result - deltaFromStart;
    }

    private RowRecogNFAStateEntry rankEndStates(List<RowRecogNFAStateEntry> endStates) {
        // sort by end-event descending (newest first)
        Collections.sort(endStates, RowRecogHelper.END_STATE_COMPARATOR);

        // find the earliest begin-event
        RowRecogNFAStateEntry found = null;
        int min = Integer.MAX_VALUE;
        boolean multipleMinimums = false;
        for (RowRecogNFAStateEntry state : endStates) {
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
        for (RowRecogNFAStateEntry state : endStates) {
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
        for (RowRecogNFAState state : factory.getAllStates()) {
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

    private RowRecogIteratorResult processIterator(boolean isOutOfSeqDelete,
                                                   Iterator<EventBean> events,
                                                   RowRecogPartitionStateRepo regexPartitionStateRepo) {
        List<RowRecogNFAStateEntry> endStates = new ArrayList<RowRecogNFAStateEntry>();
        Iterator<RowRecogNFAStateEntry> currentStates;
        int eventSequenceNumber = 0;

        EventBean theEvent;
        for (; events.hasNext(); ) {
            List<RowRecogNFAStateEntry> nextStates = new ArrayList<RowRecogNFAStateEntry>(2);
            theEvent = events.next();
            eventSequenceNumber++;

            RowRecogPartitionState partitionState = regexPartitionStateRepo.getState(theEvent, false);
            currentStates = partitionState.getCurrentStatesIterator();

            if (partitionState.getRandomAccess() != null) {
                partitionState.getRandomAccess().existingEventPrepare(theEvent);
            }

            step(!isOutOfSeqDelete, currentStates, theEvent, nextStates, endStates, false, eventSequenceNumber, partitionState.getOptionalKeys());

            partitionState.setCurrentStates(nextStates);
        }

        return new RowRecogIteratorResult(endStates, eventSequenceNumber);
    }

    public EventType getEventType() {
        return factory.getDesc().getRowEventType();
    }

    public Iterator<EventBean> iterator() {
        if (factory.getDesc().isUnbound()) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        }

        Iterator<EventBean> it = parent.iterator();

        RowRecogPartitionStateRepo regexPartitionStateRepoNew = regexPartitionStateRepo.copyForIterate(false);

        RowRecogIteratorResult iteratorResult = processIterator(false, it, regexPartitionStateRepoNew);
        List<RowRecogNFAStateEntry> endStates = iteratorResult.getEndStates();
        if (endStates.isEmpty()) {
            return CollectionUtil.NULL_EVENT_ITERATOR;
        } else {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        List<EventBean> output = new ArrayList<EventBean>();
        for (RowRecogNFAStateEntry endState : endStates) {
            output.add(generateOutputRow(endState));
        }
        return output.iterator();
    }

    public void accept(RowRecogNFAViewServiceVisitor visitor) {
        regexPartitionStateRepo.accept(visitor);
    }

    private List<RowRecogNFAStateEntry> rankEndStatesMultiPartition(List<RowRecogNFAStateEntry> endStates) {
        if (endStates.isEmpty()) {
            return endStates;
        }
        if (endStates.size() == 1) {
            return endStates;
        }

        // unpartitioned case -
        if (factory.getDesc().getPartitionEvalMayNull() == null) {
            return rankEndStatesWithinPartitionByStart(endStates);
        }

        // partitioned case - structure end states by partition
        Map<Object, Object> perPartition = new LinkedHashMap<Object, Object>();
        for (RowRecogNFAStateEntry endState : endStates) {
            Object value = perPartition.get(endState.getPartitionKey());
            if (value == null) {
                perPartition.put(endState.getPartitionKey(), endState);
            } else if (value instanceof List) {
                List<RowRecogNFAStateEntry> entries = (List<RowRecogNFAStateEntry>) value;
                entries.add(endState);
            } else {
                List<RowRecogNFAStateEntry> entries = new ArrayList<RowRecogNFAStateEntry>();
                entries.add((RowRecogNFAStateEntry) value);
                entries.add(endState);
                perPartition.put(endState.getPartitionKey(), entries);
            }
        }

        List<RowRecogNFAStateEntry> finalEndStates = new ArrayList<RowRecogNFAStateEntry>();
        for (Map.Entry<Object, Object> entry : perPartition.entrySet()) {
            if (entry.getValue() instanceof RowRecogNFAStateEntry) {
                finalEndStates.add((RowRecogNFAStateEntry) entry.getValue());
            } else {
                List<RowRecogNFAStateEntry> entries = (List<RowRecogNFAStateEntry>) entry.getValue();
                finalEndStates.addAll(rankEndStatesWithinPartitionByStart(entries));
            }
        }
        return finalEndStates;
    }

    private List<RowRecogNFAStateEntry> rankEndStatesWithinPartitionByStart(List<RowRecogNFAStateEntry> endStates) {
        if (endStates.isEmpty()) {
            return endStates;
        }
        if (endStates.size() == 1) {
            return endStates;
        }

        RowRecogDesc rowRecogDesc = factory.getDesc();
        TreeMap<Integer, Object> endStatesPerBeginEvent = new TreeMap<Integer, Object>();
        for (RowRecogNFAStateEntry entry : endStates) {
            Integer beginNum = entry.getMatchBeginEventSeqNo();
            Object value = endStatesPerBeginEvent.get(beginNum);
            if (value == null) {
                endStatesPerBeginEvent.put(beginNum, entry);
            } else if (value instanceof List) {
                List<RowRecogNFAStateEntry> entries = (List<RowRecogNFAStateEntry>) value;
                entries.add(entry);
            } else {
                List<RowRecogNFAStateEntry> entries = new ArrayList<RowRecogNFAStateEntry>();
                entries.add((RowRecogNFAStateEntry) value);
                entries.add(entry);
                endStatesPerBeginEvent.put(beginNum, entries);
            }
        }

        if (endStatesPerBeginEvent.size() == 1) {
            List<RowRecogNFAStateEntry> endStatesUnranked = (List<RowRecogNFAStateEntry>) endStatesPerBeginEvent.values().iterator().next();
            if (rowRecogDesc.isAllMatches()) {
                return endStatesUnranked;
            }
            RowRecogNFAStateEntry chosen = rankEndStates(endStatesUnranked);
            return Collections.singletonList(chosen);
        }

        List<RowRecogNFAStateEntry> endStatesRanked = new ArrayList<RowRecogNFAStateEntry>();
        Set<Integer> keyset = endStatesPerBeginEvent.keySet();
        Integer[] keys = keyset.toArray(new Integer[keyset.size()]);
        for (Integer key : keys) {
            Object value = endStatesPerBeginEvent.remove(key);
            if (value == null) {
                continue;
            }

            RowRecogNFAStateEntry entryTaken;
            if (value instanceof List) {
                List<RowRecogNFAStateEntry> endStatesUnranked = (List<RowRecogNFAStateEntry>) value;
                if (endStatesUnranked.isEmpty()) {
                    continue;
                }
                entryTaken = rankEndStates(endStatesUnranked);

                if (rowRecogDesc.isAllMatches()) {
                    endStatesRanked.addAll(endStatesUnranked);  // we take all matches and don't rank except to determine skip-past
                } else {
                    endStatesRanked.add(entryTaken);
                }
            } else {
                entryTaken = (RowRecogNFAStateEntry) value;
                endStatesRanked.add(entryTaken);
            }
            // could be null as removals take place

            if (entryTaken != null) {
                if (rowRecogDesc.getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW) {
                    int skipPastRow = entryTaken.getMatchEndEventSeqNo();
                    removeSkippedEndStates(endStatesPerBeginEvent, skipPastRow);
                } else if (rowRecogDesc.getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW) {
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
                List<RowRecogNFAStateEntry> endStatesUnranked = (List<RowRecogNFAStateEntry>) value;
                Iterator<RowRecogNFAStateEntry> it = endStatesUnranked.iterator();
                for (; it.hasNext(); ) {
                    RowRecogNFAStateEntry endState = it.next();
                    if (endState.getMatchBeginEventSeqNo() <= skipPastRow) {
                        it.remove();
                    }
                }
            } else {
                RowRecogNFAStateEntry endState = (RowRecogNFAStateEntry) value;
                if (endState.getMatchBeginEventSeqNo() <= skipPastRow) {
                    endStatesPerEndEvent.put(entry.getKey(), null);
                }
            }
        }
    }

    private List<RowRecogNFAStateEntry> step(boolean skipTrackMaxState,
                                             Iterator<RowRecogNFAStateEntry> currentStatesIterator,
                                             EventBean theEvent,
                                             List<RowRecogNFAStateEntry> nextStates,
                                             List<RowRecogNFAStateEntry> endStates,
                                             boolean isRetainEventSet,
                                             int currentEventSequenceNumber,
                                             Object partitionKey) {
        RowRecogDesc rowRecogDesc = factory.getDesc();
        List<RowRecogNFAStateEntry> terminationStates = null;  // always null or a list of entries (no singleton list)

        // handle current state matching
        for (; currentStatesIterator.hasNext(); ) {
            RowRecogNFAStateEntry currentState = currentStatesIterator.next();
            agentInstanceContext.getInstrumentationProvider().qRegExState(currentState, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());

            if (factory.isTrackMaxStates() && !skipTrackMaxState) {
                RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
                poolSvc.getRuntimeSvc().decreaseCount(agentInstanceContext);
                poolSvc.getStmtHandler().decreaseCount();
            }

            EventBean[] eventsPerStream = currentState.getEventsPerStream();
            int currentStateStreamNum = currentState.getState().getStreamNum();
            eventsPerStream[currentStateStreamNum] = theEvent;
            if (rowRecogDesc.isDefineAsksMultimatches()) {
                eventsPerStream[rowRecogDesc.getNumEventsEventsPerStreamDefine() - 1] = getMultimatchState(currentState);
            }

            if (currentState.getState().matches(eventsPerStream, agentInstanceContext)) {
                if (isRetainEventSet) {
                    this.windowMatchedEventset.add(theEvent);
                }
                RowRecogNFAState[] nextStatesFromHere = currentState.getState().getNextStates();

                // save state for each next state
                boolean copy = nextStatesFromHere.length > 1;
                for (RowRecogNFAState next : nextStatesFromHere) {
                    EventBean[] eventsForState = eventsPerStream;
                    RowRecogMultimatchState[] multimatches = currentState.getOptionalMultiMatches();
                    int[] greedyCounts = currentState.getGreedycountPerState();

                    if (copy) {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;

                        if (rowRecogDesc.isCollectMultimatches()) {
                            multimatches = deepCopy(multimatches);
                        }
                    }

                    if (rowRecogDesc.isCollectMultimatches() && (currentState.getState().isMultiple())) {
                        multimatches = addTag(currentState.getState().getStreamNum(), theEvent, multimatches);
                        eventsForState[currentStateStreamNum] = null; // remove event from evaluation list
                    }

                    if ((currentState.getState().isGreedy() != null) && (currentState.getState().isGreedy())) {
                        greedyCounts[currentState.getState().getNodeNumFlat()]++;
                    }

                    RowRecogNFAStateEntry entry = new RowRecogNFAStateEntry(currentState.getMatchBeginEventSeqNo(), currentState.getMatchBeginEventTime(), currentState.getState(), eventsForState, greedyCounts, multimatches, partitionKey);
                    if (next instanceof RowRecogNFAStateEndEval) {
                        entry.setMatchEndEventSeqNo(currentEventSequenceNumber);
                        endStates.add(entry);
                    } else {
                        if (factory.isTrackMaxStates() && !skipTrackMaxState) {
                            RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
                            boolean allow = poolSvc.getRuntimeSvc().tryIncreaseCount(agentInstanceContext);
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
                agentInstanceContext.getInstrumentationProvider().aRegExState(nextStates, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());
            } else {
                // when not-matches
                agentInstanceContext.getInstrumentationProvider().aRegExState(Collections.<RowRecogNFAStateEntry>emptyList(), factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());

                // determine interval and or-terminated
                if (rowRecogDesc.isOrTerminated()) {
                    eventsPerStream[currentStateStreamNum] = null;  // deassign
                    RowRecogNFAState[] nextStatesFromHere = currentState.getState().getNextStates();

                    // save state for each next state
                    RowRecogNFAState theEndState = null;
                    for (RowRecogNFAState next : nextStatesFromHere) {
                        if (next instanceof RowRecogNFAStateEndEval) {
                            theEndState = next;
                        }
                    }
                    if (theEndState != null) {
                        RowRecogNFAStateEntry entry = new RowRecogNFAStateEntry(currentState.getMatchBeginEventSeqNo(), currentState.getMatchBeginEventTime(), theEndState, eventsPerStream, currentState.getGreedycountPerState(), currentState.getOptionalMultiMatches(), partitionKey);
                        if (terminationStates == null) {
                            terminationStates = new ArrayList<RowRecogNFAStateEntry>();
                        }
                        terminationStates.add(entry);
                    }
                }
            }
        }

        // handle start states for the event
        for (RowRecogNFAState startState : factory.getStartStates()) {
            agentInstanceContext.getInstrumentationProvider().qRegExStateStart(startState, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());

            EventBean[] eventsPerStream = new EventBean[rowRecogDesc.getNumEventsEventsPerStreamDefine()];
            int currentStateStreamNum = startState.getStreamNum();
            eventsPerStream[currentStateStreamNum] = theEvent;

            if (startState.matches(eventsPerStream, agentInstanceContext)) {
                if (isRetainEventSet) {
                    this.windowMatchedEventset.add(theEvent);
                }
                RowRecogNFAState[] nextStatesFromHere = startState.getNextStates();

                // save state for each next state
                boolean copy = nextStatesFromHere.length > 1;
                for (RowRecogNFAState next : nextStatesFromHere) {

                    if (factory.isTrackMaxStates() && !skipTrackMaxState) {
                        RowRecogStatePoolStmtSvc poolSvc = agentInstanceContext.getStatementContext().getRowRecogStatePoolStmtSvc();
                        boolean allow = poolSvc.getRuntimeSvc().tryIncreaseCount(agentInstanceContext);
                        if (!allow) {
                            continue;
                        }
                        poolSvc.getStmtHandler().increaseCount();
                    }

                    EventBean[] eventsForState = eventsPerStream;
                    RowRecogMultimatchState[] multimatches = rowRecogDesc.isCollectMultimatches() ? new RowRecogMultimatchState[rowRecogDesc.getMultimatchVariablesArray().length] : null;
                    int[] greedyCounts = new int[factory.getAllStates().length];

                    if (copy) {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;
                    }

                    if (rowRecogDesc.isCollectMultimatches() && (startState.isMultiple())) {
                        multimatches = addTag(startState.getStreamNum(), theEvent, multimatches);
                        eventsForState[currentStateStreamNum] = null; // remove event from evaluation list
                    }

                    if ((startState.isGreedy() != null) && (startState.isGreedy())) {
                        greedyCounts[startState.getNodeNumFlat()]++;
                    }

                    long time = 0;
                    if (rowRecogDesc.isHasInterval()) {
                        time = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                    }

                    RowRecogNFAStateEntry entry = new RowRecogNFAStateEntry(currentEventSequenceNumber, time, startState, eventsForState, greedyCounts, multimatches, partitionKey);
                    if (next instanceof RowRecogNFAStateEndEval) {
                        entry.setMatchEndEventSeqNo(currentEventSequenceNumber);
                        endStates.add(entry);
                    } else {
                        entry.setState(next);
                        nextStates.add(entry);
                    }
                }
            }
            agentInstanceContext.getInstrumentationProvider().aRegExStateStart(nextStates, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());
        }

        return terminationStates;   // only for immediate use, not for scheduled use as no copy of state
    }

    private ObjectArrayBackedEventBean getMultimatchState(RowRecogNFAStateEntry currentState) {
        if (currentState.getOptionalMultiMatches() == null || !currentState.getState().isExprRequiresMultimatchState()) {
            return null;
        }
        Object[] props = defineMultimatchEventBean.getProperties();
        RowRecogMultimatchState[] states = currentState.getOptionalMultiMatches();
        for (int i = 0; i < props.length; i++) {
            RowRecogMultimatchState state = states[i];
            if (state == null) {
                props[i] = null;
            } else {
                props[i] = state.getShrinkEventArray();
            }
        }
        return defineMultimatchEventBean;
    }

    private RowRecogMultimatchState[] deepCopy(RowRecogMultimatchState[] multimatchStates) {
        if (multimatchStates == null) {
            return null;
        }

        RowRecogMultimatchState[] copy = new RowRecogMultimatchState[multimatchStates.length];
        for (int i = 0; i < copy.length; i++) {
            if (multimatchStates[i] != null) {
                copy[i] = new RowRecogMultimatchState(multimatchStates[i]);
            }
        }

        return copy;
    }

    private RowRecogMultimatchState[] addTag(int streamNum, EventBean theEvent, RowRecogMultimatchState[] multimatches) {
        if (multimatches == null) {
            multimatches = new RowRecogMultimatchState[factory.getDesc().getMultimatchVariablesArray().length];
        }

        int index = factory.getDesc().getMultimatchStreamNumToVariable()[streamNum];
        RowRecogMultimatchState state = multimatches[index];
        if (state == null) {
            multimatches[index] = new RowRecogMultimatchState(theEvent);
            return multimatches;
        }

        multimatches[index].add(theEvent);
        return multimatches;
    }

    private EventBean generateOutputRow(RowRecogNFAStateEntry entry) {
        AggregationServiceFactory[] aggregationServiceFactories = factory.getDesc().getAggregationServiceFactories();
        if (aggregationServiceFactories != null) {
            // we must synchronize here when aggregations are used
            // since expression futures are set
            AggregationService[] aggregationServices = new AggregationService[aggregationServiceFactories.length];
            for (int i = 0; i < aggregationServices.length; i++) {
                if (aggregationServiceFactories[i] != null) {
                    aggregationServices[i] = aggregationServiceFactories[i].makeService(agentInstanceContext, agentInstanceContext.getClasspathImportServiceRuntime(), false, null, null);
                    factory.getDesc().getAggregationResultFutureAssignables()[i].assign(aggregationServices[i]);
                }
            }

            synchronized (factory) {
                return generateOutputRowUnderLockIfRequired(entry, aggregationServices);
            }
        } else {
            return generateOutputRowUnderLockIfRequired(entry, null);
        }
    }

    private EventBean generateOutputRowUnderLockIfRequired(RowRecogNFAStateEntry entry, AggregationService[] aggregationServices) {
        Object[] rowDataRaw = compositeEventBean.getProperties();

        // we first generate a raw row of <String, Object> for each variable name.
        for (Map.Entry<String, Pair<Integer, Boolean>> variableDef : factory.getDesc().getVariableStreams().entrySet()) {
            if (!variableDef.getValue().getSecond()) {
                int index = variableDef.getValue().getFirst();
                rowDataRaw[index] = entry.getEventsPerStream()[index];
            }
        }

        int[] multimatchVariableToStreamNum = factory.getDesc().getMultimatchVariableToStreamNum();
        if (entry.getOptionalMultiMatches() != null) {
            RowRecogMultimatchState[] multimatchState = entry.getOptionalMultiMatches();
            for (int i = 0; i < multimatchState.length; i++) {
                int streamNum = multimatchVariableToStreamNum[i];
                if (multimatchState[i] == null) {
                    rowDataRaw[streamNum] = null;
                    continue;
                }
                EventBean[] multimatchEvents = multimatchState[i].getShrinkEventArray();
                rowDataRaw[streamNum] = multimatchEvents;

                if (aggregationServices != null && aggregationServices[streamNum] != null) {
                    EventBean[] eventsPerStream = entry.getEventsPerStream();

                    for (EventBean multimatchEvent : multimatchEvents) {
                        eventsPerStream[streamNum] = multimatchEvent;
                        aggregationServices[streamNum].applyEnter(eventsPerStream, null, agentInstanceContext);
                    }
                }
            }
        } else {
            for (int index : multimatchVariableToStreamNum) {
                rowDataRaw[index] = null;
            }
        }

        Map<String, Object> row = new HashMap<>();
        int columnNum = 0;
        EventBean[] eventsPerStream = new EventBean[1];
        String[] columnNames = factory.getDesc().getColumnNames();
        for (ExprEvaluator expression : factory.getDesc().getColumnEvaluators()) {
            eventsPerStream[0] = compositeEventBean;
            Object result = expression.evaluate(eventsPerStream, true, agentInstanceContext);
            row.put(columnNames[columnNum], result);
            columnNum++;
        }

        return agentInstanceContext.getStatementContext().getEventBeanTypedEventFactory().adapterForTypedMap(row, factory.getDesc().getRowEventType());
    }

    private void scheduleCallback(long timeDelta, RowRecogNFAStateEntry endState) {
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

    private void removeScheduleAddEndState(RowRecogNFAStateEntry terminationState, List<RowRecogNFAStateEntry> foundEndStates) {
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

        List<RowRecogNFAStateEntry> indicatables = new ArrayList<RowRecogNFAStateEntry>();
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

        if (!factory.getDesc().isAllMatches()) {
            indicatables = rankEndStatesMultiPartition(indicatables);
        }

        EventBean[] outBeans = new EventBean[indicatables.size()];
        int count = 0;
        for (RowRecogNFAStateEntry endState : indicatables) {
            agentInstanceContext.getInstrumentationProvider().qRegMeasure(endState, factory.getDesc().getVariableStreams(), factory.getDesc().getMultimatchStreamNumToVariable());
            outBeans[count] = generateOutputRow(endState);
            agentInstanceContext.getInstrumentationProvider().aRegMeasure(outBeans[count]);
            count++;
        }

        agentInstanceContext.getInstrumentationProvider().qRegOut(outBeans);
        child.update(outBeans, null);
        agentInstanceContext.getInstrumentationProvider().aRegOut();
    }

    private long computeScheduleBackwardDelta(long currentTime) {
        agentInstanceContext.getInstrumentationProvider().qRegIntervalValue();
        long result = factory.getDesc().getIntervalCompute().deltaSubtract(currentTime, null, true, null);
        agentInstanceContext.getInstrumentationProvider().aRegIntervalValue(result);
        return result;
    }

    public RowRecogPreviousStrategy getPreviousEvaluationStrategy() {
        return rowRecogPreviousStrategy;
    }

    public RowRecogNFAViewFactory getFactory() {
        return factory;
    }
}
