/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.ExtensionServicesContext;
import com.espertech.esper.epl.agg.service.AggregationServiceMatchRecognize;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.spec.MatchRecognizeDefineItem;
import com.espertech.esper.epl.spec.MatchRecognizeMeasureItem;
import com.espertech.esper.epl.spec.MatchRecognizeSkipEnum;
import com.espertech.esper.epl.spec.MatchRecognizeSpec;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.ScheduleSlot;
import com.espertech.esper.util.ExecutionPathDebugLog;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * View for match recognize support.
 */
public class EventRowRegexNFAView extends ViewSupport implements StopCallback, EventRowRegexNFAViewService
{
    private static final Log log = LogFactory.getLog(EventRowRegexNFAView.class);
    private static final boolean IS_DEBUG = false;
    private static final Iterator<EventBean> NULL_ITERATOR = new SingleEventIterator(null);

    private final EventRowRegexNFAViewFactory factory;
    private final MatchRecognizeSpec matchRecognizeSpec;
    private final boolean isUnbound;
    private final boolean isIterateOnly;
    private final boolean isCollectMultimatches;

    private final EventType rowEventType;
    private final AgentInstanceContext agentInstanceContext;
    private final AggregationServiceMatchRecognize aggregationService;

    // for interval-handling
    private final ScheduleSlot scheduleSlot;
    private final EPStatementHandleCallback handle;
    private final TreeMap<Long, Object> schedule;
    private final boolean isOrTerminated;

    private final ExprEvaluator[] columnEvaluators;
    private final String[] columnNames;

    private final RegexNFAState[] startStates;
    protected final RegexNFAState[] allStates;

    private final String[] multimatchVariablesArray;
    private final int[] multimatchStreamNumToVariable;
    private final int[] multimatchVariableToStreamNum;
    private final LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams;
    private final Map<Integer, String> streamsVariables;
    protected final int numEventsEventsPerStreamDefine;
    private final boolean isDefineAsksMultimatches;
    private final ObjectArrayBackedEventBean defineMultimatchEventBean;

    private final RegexPartitionStateRandomAccessGetter prevGetter;
    private final ObjectArrayBackedEventBean compositeEventBean;

    // state
    private RegexPartitionStateRepo regexPartitionStateRepo;
    private LinkedHashSet<EventBean> windowMatchedEventset; // this is NOT per partition - some optimizations are done for batch-processing (minus is out-of-sequence in partition) 
    private int eventSequenceNumber;

    /**
     * Ctor.
     * @param compositeEventType final event type
     * @param rowEventType event type for input rows
     * @param matchRecognizeSpec specification
     * @param variableStreams variables and their assigned stream number
     * @param streamsVariables stream number and the assigned variable
     * @param variablesSingle single variables
     * @param callbacksPerIndex  for handling the 'prev' function
     * @param aggregationService handles aggregations
     * @param isUnbound true if unbound stream
     * @param isIterateOnly true for iterate-only
     * @param isCollectMultimatches if asking for multimatches
     */
    public EventRowRegexNFAView(EventRowRegexNFAViewFactory factory,
                                ObjectArrayEventType compositeEventType,
                                EventType rowEventType,
                                MatchRecognizeSpec matchRecognizeSpec,
                                LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams,
                                Map<Integer, String> streamsVariables,
                                Set<String> variablesSingle,
                                AgentInstanceContext agentInstanceContext,
                                TreeMap<Integer, List<ExprPreviousMatchRecognizeNode>> callbacksPerIndex,
                                AggregationServiceMatchRecognize aggregationService,
                                boolean isDefineAsksMultimatches,
                                ObjectArrayBackedEventBean defineMultimatchEventBean,
                                boolean[] isExprRequiresMultimatchState,
                                boolean isUnbound,
                                boolean isIterateOnly,
                                boolean isCollectMultimatches,
                                RowRegexExprNode expandedPatternNode)
    {
        this.factory = factory;
        this.matchRecognizeSpec = matchRecognizeSpec;
        this.compositeEventBean = new ObjectArrayEventBean(new Object[variableStreams.size()], compositeEventType);
        this.rowEventType = rowEventType;
        this.variableStreams = variableStreams;

        // determine names of multimatching variables
        if (variablesSingle.size() == variableStreams.size()) {
            multimatchVariablesArray = new String[0];
            multimatchStreamNumToVariable = new int[0];
            multimatchVariableToStreamNum = new int[0];
        }
        else {
            multimatchVariablesArray = new String[variableStreams.size() - variablesSingle.size()];
            multimatchVariableToStreamNum = new int[multimatchVariablesArray.length];
            multimatchStreamNumToVariable = new int[variableStreams.size()];
            Arrays.fill(multimatchStreamNumToVariable, -1);
            int count = 0;
            for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
                if (entry.getValue().getSecond()) {
                    int index = count;
                    multimatchVariablesArray[index] = entry.getKey();
                    multimatchVariableToStreamNum[index] = entry.getValue().getFirst();
                    multimatchStreamNumToVariable[entry.getValue().getFirst()] = index;
                    count++;
                }
            }
        }

        this.streamsVariables = streamsVariables;
        this.aggregationService = aggregationService;
        this.isDefineAsksMultimatches = isDefineAsksMultimatches;
        this.defineMultimatchEventBean = defineMultimatchEventBean;
        this.numEventsEventsPerStreamDefine = isDefineAsksMultimatches ? variableStreams.size() + 1 : variableStreams.size();
        this.isUnbound = isUnbound;
        this.isIterateOnly = isIterateOnly;
        this.agentInstanceContext = agentInstanceContext;
        this.isCollectMultimatches = isCollectMultimatches;

        if (matchRecognizeSpec.getInterval() != null)
        {
            scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
            ScheduleHandleCallback callback = new ScheduleHandleCallback() {
                public void scheduledTrigger(ExtensionServicesContext extensionServicesContext)
                {
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegExScheduledEval();}
                    EventRowRegexNFAView.this.triggered();
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExScheduledEval();}
                }
            };
            handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
            schedule = new TreeMap<Long, Object>();

            agentInstanceContext.addTerminationCallback(this);
            isOrTerminated = matchRecognizeSpec.getInterval().isOrTerminated();
        }
        else
        {
            scheduleSlot = null;
            handle = null;
            schedule = null;
            isOrTerminated = false;
        }

        this.windowMatchedEventset = new LinkedHashSet<EventBean>();

        // handle "previous" function nodes (performance-optimized for direct index access)
        if (!callbacksPerIndex.isEmpty())
        {
            // Build an array of indexes
            int[] randomAccessIndexesRequested = new int[callbacksPerIndex.size()];
            int count = 0;
            for (Map.Entry<Integer, List<ExprPreviousMatchRecognizeNode>> entry : callbacksPerIndex.entrySet())
            {
                randomAccessIndexesRequested[count] = entry.getKey();
                count++;
            }
            prevGetter = new RegexPartitionStateRandomAccessGetter(randomAccessIndexesRequested, isUnbound);
        }
        else
        {
            prevGetter = null;
        }

        Map<String, ExprNode> variableDefinitions = new LinkedHashMap<String, ExprNode>();
        for (MatchRecognizeDefineItem defineItem : matchRecognizeSpec.getDefines())
        {
            variableDefinitions.put(defineItem.getIdentifier(), defineItem.getExpression());
        }

        // build states
        RegexNFAStrandResult strand = EventRowRegexHelper.recursiveBuildStartStates(expandedPatternNode, variableDefinitions, variableStreams, isExprRequiresMultimatchState);
        startStates = strand.getStartStates().toArray(new RegexNFAState[strand.getStartStates().size()]);
        allStates = strand.getAllStates().toArray(new RegexNFAState[strand.getAllStates().size()]);

        if (log.isDebugEnabled() || IS_DEBUG)
        {
            log.info("NFA tree:\n" + EventRowRegexNFAViewUtil.print(startStates));
        }

        // create evaluators
        columnNames = new String[matchRecognizeSpec.getMeasures().size()];
        columnEvaluators = new ExprEvaluator[matchRecognizeSpec.getMeasures().size()];
        int count = 0;
        for (MatchRecognizeMeasureItem measureItem : matchRecognizeSpec.getMeasures())
        {
            columnNames[count] = measureItem.getName();
            columnEvaluators[count] = measureItem.getExpr().getExprEvaluator();
            count++;
        }

        // create state repository
        RegexHandlerFactory repoFactory = agentInstanceContext.getStatementContext().getRegexPartitionStateRepoFactory();
        if (this.matchRecognizeSpec.getPartitionByExpressions().isEmpty())
        {
            regexPartitionStateRepo = repoFactory.makeSingle(prevGetter, agentInstanceContext, this);
        }
        else
        {
            RegexPartitionStateRepoGroupMeta stateRepoGroupMeta = new RegexPartitionStateRepoGroupMeta(matchRecognizeSpec.getInterval() != null,
                ExprNodeUtility.toArray(matchRecognizeSpec.getPartitionByExpressions()),
                ExprNodeUtility.getEvaluators(matchRecognizeSpec.getPartitionByExpressions()), agentInstanceContext);
            regexPartitionStateRepo = repoFactory.makePartitioned(prevGetter, stateRepoGroupMeta, agentInstanceContext, this);
        }
    }

    public void stop() {
        if (handle != null) {
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public void init(EventBean[] newEvents) {
        updateInternal(newEvents, null, false);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        updateInternal(newData, oldData, true);
    }

    private void updateInternal(EventBean[] newData, EventBean[] oldData, boolean postOutput)
    {
        if (isIterateOnly)
        {
            if (oldData != null)
            {
                regexPartitionStateRepo.removeOld(oldData, false, new boolean[oldData.length]);
            }
            if (newData != null)
            {
                for (EventBean newEvent : newData)
                {
                    RegexPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
                    if ((partitionState != null) && (partitionState.getRandomAccess() != null))
                    {
                        partitionState.getRandomAccess().newEventPrepare(newEvent);
                    }
                }
            }            
            return;
        }

        if (oldData != null)
        {
            boolean isOutOfSequenceRemove = false;

            EventBean first = null;
            if (!windowMatchedEventset.isEmpty())
            {
                first = windowMatchedEventset.iterator().next();
            }

            // remove old data, if found in set
            boolean[] found = new boolean[oldData.length];
            int count = 0;

            // detect out-of-sequence removes
            for (EventBean oldEvent : oldData)
            {
                boolean removed = windowMatchedEventset.remove(oldEvent);
                if (removed)
                {
                    if ((oldEvent != first) && (first != null))
                    {
                        isOutOfSequenceRemove = true;
                    }
                    found[count++] = true;
                    if (!windowMatchedEventset.isEmpty())
                    {
                        first = windowMatchedEventset.iterator().next();
                    }
                }
            }

            // remove old events from repository - and let the repository know there are no interesting events left
            regexPartitionStateRepo.removeOld(oldData, windowMatchedEventset.isEmpty(), found);

            // reset, rebuilding state
            if (isOutOfSequenceRemove)
            {
                regexPartitionStateRepo = regexPartitionStateRepo.copyForIterate();
                windowMatchedEventset = new LinkedHashSet<EventBean>();
                Iterator<EventBean> parentEvents = this.getParent().iterator();
                EventRowRegexIteratorResult iteratorResult = processIterator(startStates, parentEvents, regexPartitionStateRepo);
                eventSequenceNumber = iteratorResult.getEventSequenceNum();
            }
        }

        if (newData == null)
        {
            return;
        }
        
        List<RegexNFAStateEntry> endStates = new ArrayList<RegexNFAStateEntry>();
        List<RegexNFAStateEntry> terminationStatesAll = null;

        for (EventBean newEvent : newData)
        {
            List<RegexNFAStateEntry> nextStates = new ArrayList<RegexNFAStateEntry>(2);
            eventSequenceNumber++;

            // get state holder for this event
            RegexPartitionState partitionState = regexPartitionStateRepo.getState(newEvent, true);
            Iterator<RegexNFAStateEntry> currentStatesIterator = partitionState.getCurrentStatesIterator();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegEx(newEvent, partitionState);}

            if (partitionState.getRandomAccess() != null)
            {
                partitionState.getRandomAccess().newEventPrepare(newEvent);
            }

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || (IS_DEBUG))
            {
                log.info("Evaluating event " + newEvent.getUnderlying() + "\n" +
                    "current : " + EventRowRegexNFAViewUtil.printStates(partitionState.getCurrentStatesForPrint(), streamsVariables, variableStreams, multimatchStreamNumToVariable));
            }

            List<RegexNFAStateEntry> terminationStates = step(startStates, currentStatesIterator, newEvent, nextStates, endStates, !isUnbound, eventSequenceNumber, partitionState.getOptionalKeys());

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || (IS_DEBUG))
            {
                log.info("Evaluated event " + newEvent.getUnderlying() + "\n" +
                    "next : " + EventRowRegexNFAViewUtil.printStates(nextStates, streamsVariables, variableStreams, multimatchStreamNumToVariable) + "\n" +
                    "end : " + EventRowRegexNFAViewUtil.printStates(endStates, streamsVariables, variableStreams, multimatchStreamNumToVariable));
            }

            // add termination states, for use with interval and "or terminated"
            if (terminationStates != null) {
                if (terminationStatesAll == null) {
                    terminationStatesAll = terminationStates;
                }
                else {
                    terminationStatesAll.addAll(terminationStates);
                }
            }

            partitionState.setCurrentStates(nextStates);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegEx(partitionState, endStates, terminationStates);}
        }

        if (endStates.isEmpty() && (!isOrTerminated || terminationStatesAll == null))
        {
            return;
        }

        // perform inter-ranking and elimination of duplicate matches
        if (!matchRecognizeSpec.isAllMatches())
        {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        // handle interval for the set of matches
        if (matchRecognizeSpec.getInterval() != null)
        {
            Iterator<RegexNFAStateEntry> it = endStates.iterator();
            for (;it.hasNext();)
            {
                RegexNFAStateEntry endState = it.next();
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegIntervalState(endState, variableStreams, multimatchStreamNumToVariable, agentInstanceContext.getStatementContext().getSchedulingService().getTime());}
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null)
                {
                    log.warn("Null partition state encountered, skipping row");
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegIntervalState(false);}
                    continue;
                }

                // determine whether to schedule
                boolean scheduleDelivery;
                if (!isOrTerminated) {
                    scheduleDelivery = true;
                }
                else {
                    // determine whether there can be more matches
                    if (endState.getState().getNextStates().size() == 1 &&
                        endState.getState().getNextStates().get(0) instanceof RegexNFAStateEnd) {
                        scheduleDelivery = false;
                    }
                    else {
                        scheduleDelivery = true;
                    }
                }

                // only schedule if not an end-state or not or-terminated
                if (scheduleDelivery) {
                    long matchBeginTime = endState.getMatchBeginEventTime();
                    long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                    long deltaFromStart = current - matchBeginTime;
                    long deltaUntil = matchRecognizeSpec.getInterval().getScheduleForwardDelta(current, agentInstanceContext) - deltaFromStart;

                    if (schedule.containsKey(matchBeginTime))
                    {
                        scheduleCallback(deltaUntil, endState);
                        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegIntervalState(true);}
                        it.remove();
                    }
                    else
                    {
                        if (deltaFromStart < deltaUntil)
                        {
                            scheduleCallback(deltaUntil, endState);
                            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegIntervalState(true);}
                            it.remove();
                        }
                        else {
                            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegIntervalState(false);}
                        }
                    }
                }
                else {
                    if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegIntervalState(false);}
                }
            }

            // handle termination states - those that terminated the pattern and remove the callback
            if (isOrTerminated && terminationStatesAll != null) {
                for (RegexNFAStateEntry terminationState : terminationStatesAll)
                {
                    RegexPartitionState partitionState = regexPartitionStateRepo.getState(terminationState.getPartitionKey());
                    if (partitionState == null) {
                        log.warn("Null partition state encountered, skipping row");
                        continue;
                    }

                    removeScheduleAddEndState(terminationState, endStates);
                }

                // rank
                if (!matchRecognizeSpec.isAllMatches()) {
                    endStates = rankEndStatesMultiPartition(endStates);
                }
            }

            if (endStates.isEmpty())
            {
                return;
            }
        }
        // handle skip for incremental mode
        else if (matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW)
        {
            Iterator<RegexNFAStateEntry> endStateIter = endStates.iterator();
            for (;endStateIter.hasNext();)
            {
                RegexNFAStateEntry endState = endStateIter.next();
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null)
                {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RegexNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (;stateIter.hasNext();)
                {
                    RegexNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchEndEventSeqNo())
                    {
                        stateIter.remove();
                    }
                }
            }
        }
        else if (matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW)
        {
            Iterator<RegexNFAStateEntry> endStateIter = endStates.iterator();
            for (;endStateIter.hasNext();)
            {
                RegexNFAStateEntry endState = endStateIter.next();
                RegexPartitionState partitionState = regexPartitionStateRepo.getState(endState.getPartitionKey());
                if (partitionState == null)
                {
                    log.warn("Null partition state encountered, skipping row");
                    continue;
                }

                Iterator<RegexNFAStateEntry> stateIter = partitionState.getCurrentStatesIterator();
                for (;stateIter.hasNext();)
                {
                    RegexNFAStateEntry currentState = stateIter.next();
                    if (currentState.getMatchBeginEventSeqNo() <= endState.getMatchBeginEventSeqNo())
                    {
                        stateIter.remove();
                    }
                }
            }
        }

        EventBean[] outBeans = new EventBean[endStates.size()];
        int count = 0;
        for (RegexNFAStateEntry endState : endStates)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegMeasure(endState, variableStreams, multimatchStreamNumToVariable);}
            outBeans[count] = generateOutputRow(endState);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegMeasure(outBeans[count]);}
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
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegOut(outBeans);}
            updateChildren(outBeans, null);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegOut();}
        }
    }

    private RegexNFAStateEntry rankEndStates(List<RegexNFAStateEntry> endStates) {

        // sort by end-event descending (newest first)
        Collections.sort(endStates, EventRowRegexHelper.END_STATE_COMPARATOR);

        // find the earliest begin-event
        RegexNFAStateEntry found = null;
        int min = Integer.MAX_VALUE;
        boolean multipleMinimums = false;
        for (RegexNFAStateEntry state : endStates)
        {
            if (state.getMatchBeginEventSeqNo() < min)
            {
                found = state;
                min = state.getMatchBeginEventSeqNo();
            }
            else if (state.getMatchBeginEventSeqNo() == min)
            {
                multipleMinimums = true;
            }
        }

        if (!multipleMinimums)
        {
            Collections.singletonList(found);
        }

        // compare greedy counts
        int[] best = null;
        found = null;
        for (RegexNFAStateEntry state : endStates)
        {
            if (state.getMatchBeginEventSeqNo() != min)
            {
                continue;
            }
            if (best == null)
            {
                best = state.getGreedycountPerState();
                found = state;
            }
            else
            {
                int[] current = state.getGreedycountPerState();
                if (compare(current, best))
                {
                    best = current;
                    found = state;
                }
            }
        }

        return found;
    }

    private boolean compare(int[] current, int[] best)
    {
        for (RegexNFAState state : allStates)
        {
            if (state.isGreedy() == null)
            {
                continue;
            }
            if (state.isGreedy())
            {
                if (current[state.getNodeNumFlat()] > best[state.getNodeNumFlat()])
                {
                    return true;
                }
            } else
            {
                if (current[state.getNodeNumFlat()] < best[state.getNodeNumFlat()])
                {
                    return true;
                }
            }
        }

        return false;
    }

 private EventRowRegexIteratorResult processIterator(RegexNFAState[] startStates,
                                                     Iterator<EventBean> events,
                                                     RegexPartitionStateRepo regexPartitionStateRepo)
    {
        List<RegexNFAStateEntry> endStates = new ArrayList<RegexNFAStateEntry>();
        Iterator<RegexNFAStateEntry> currentStates;
        int eventSequenceNumber = 0;

        EventBean theEvent;
        for (;events.hasNext();)
        {
            List<RegexNFAStateEntry> nextStates = new ArrayList<RegexNFAStateEntry>(2);
            theEvent = events.next();
            eventSequenceNumber++;

            RegexPartitionState partitionState = regexPartitionStateRepo.getState(theEvent, false);
            currentStates = partitionState.getCurrentStatesIterator();

            if (partitionState.getRandomAccess() != null)
            {
                partitionState.getRandomAccess().existingEventPrepare(theEvent);
            }

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || (IS_DEBUG))
            {
                log.info("Evaluating event " + theEvent.getUnderlying() + "\n" +
                    "current : " + EventRowRegexNFAViewUtil.printStates(partitionState.getCurrentStatesForPrint(), streamsVariables, variableStreams, multimatchStreamNumToVariable));
            }

            step(startStates, currentStates, theEvent, nextStates, endStates, false, eventSequenceNumber, partitionState.getOptionalKeys());

            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled()) || (IS_DEBUG))
            {
                log.info("Evaluating event " + theEvent.getUnderlying() + "\n" +
                    "next : " + EventRowRegexNFAViewUtil.printStates(nextStates, streamsVariables, variableStreams, multimatchStreamNumToVariable) + "\n" +
                    "end : " + EventRowRegexNFAViewUtil.printStates(endStates, streamsVariables, variableStreams, multimatchStreamNumToVariable));
            }

            partitionState.setCurrentStates(nextStates);
        }

        return new EventRowRegexIteratorResult(endStates, eventSequenceNumber);
    }

    public EventType getEventType() {
        return rowEventType;
    }

    public Iterator<EventBean> iterator() {
        if (isUnbound)
        {
            return NULL_ITERATOR;
        }

        Iterator<EventBean> it = parent.iterator();

        RegexPartitionStateRepo regexPartitionStateRepoNew = regexPartitionStateRepo.copyForIterate();

        EventRowRegexIteratorResult iteratorResult = processIterator(startStates, it, regexPartitionStateRepoNew);
        List<RegexNFAStateEntry> endStates = iteratorResult.getEndStates();
        if (endStates.isEmpty())
        {
            return NULL_ITERATOR;
        }
        else
        {
            endStates = rankEndStatesMultiPartition(endStates);
        }

        List<EventBean> output = new ArrayList<EventBean>();
        for (RegexNFAStateEntry endState : endStates)
        {
            output.add(generateOutputRow(endState));
        }
        return output.iterator();
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
        regexPartitionStateRepo.accept(visitor);
    }

    private List<RegexNFAStateEntry> rankEndStatesMultiPartition(List<RegexNFAStateEntry> endStates) {
        if (endStates.isEmpty())
        {
            return endStates;
        }
        if (endStates.size() == 1)
        {
            return endStates;
        }

        // unpartitioned case -
        if (matchRecognizeSpec.getPartitionByExpressions().isEmpty())
        {
            return rankEndStatesWithinPartitionByStart(endStates);
        }

        // partitioned case - structure end states by partition
        Map<Object, Object> perPartition = new LinkedHashMap<Object, Object>();
        for (RegexNFAStateEntry endState : endStates)
        {
            Object value = perPartition.get(endState.getPartitionKey());
            if (value == null)
            {
                perPartition.put(endState.getPartitionKey(), endState);
            }
            else if (value instanceof List)
            {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) value;
                entries.add(endState);
            }
            else
            {
                List<RegexNFAStateEntry> entries = new ArrayList<RegexNFAStateEntry>();
                entries.add((RegexNFAStateEntry) value);
                entries.add(endState);
                perPartition.put(endState.getPartitionKey(), entries);
            }
        }

        List<RegexNFAStateEntry> finalEndStates = new ArrayList<RegexNFAStateEntry>();
        for (Map.Entry<Object, Object> entry : perPartition.entrySet())
        {
            if (entry.getValue() instanceof RegexNFAStateEntry)
            {
                finalEndStates.add((RegexNFAStateEntry) entry.getValue());
            }
            else
            {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) entry.getValue();
                finalEndStates.addAll(rankEndStatesWithinPartitionByStart(entries));
            }            
        }
        return finalEndStates;
    }

    private List<RegexNFAStateEntry> rankEndStatesWithinPartitionByStart(List<RegexNFAStateEntry> endStates) {
        if (endStates.isEmpty())
        {
            return endStates;
        }
        if (endStates.size() == 1)
        {
            return endStates;
        }

        TreeMap<Integer, Object> endStatesPerBeginEvent = new TreeMap<Integer, Object>();
        for (RegexNFAStateEntry entry : endStates)
        {
            Integer beginNum = entry.getMatchBeginEventSeqNo();
            Object value = endStatesPerBeginEvent.get(beginNum);
            if (value == null)
            {
                endStatesPerBeginEvent.put(beginNum, entry);
            }
            else if (value instanceof List)
            {
                List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) value;
                entries.add(entry);
            }
            else
            {
                List<RegexNFAStateEntry> entries = new ArrayList<RegexNFAStateEntry>();
                entries.add((RegexNFAStateEntry) value);
                entries.add(entry);
                endStatesPerBeginEvent.put(beginNum, entries);
            }
        }

        if (endStatesPerBeginEvent.size() == 1)
        {
            List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) endStatesPerBeginEvent.values().iterator().next();
            if (matchRecognizeSpec.isAllMatches())
            {
                return endStatesUnranked;
            }
            RegexNFAStateEntry chosen = rankEndStates(endStatesUnranked);
            return Collections.singletonList(chosen);
        }

        List<RegexNFAStateEntry> endStatesRanked = new ArrayList<RegexNFAStateEntry>();
        Set<Integer> keyset = endStatesPerBeginEvent.keySet();
        Integer[] keys = keyset.toArray(new Integer[keyset.size()]);
        for (Integer key : keys)
        {
            Object value = endStatesPerBeginEvent.remove(key);
            if (value == null)
            {
                continue;
            }

            RegexNFAStateEntry entryTaken;
            if (value instanceof List)
            {
                List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) value;
                if (endStatesUnranked.isEmpty())
                {
                    continue;
                }
                entryTaken = rankEndStates(endStatesUnranked);

                if (matchRecognizeSpec.isAllMatches())
                {
                    endStatesRanked.addAll(endStatesUnranked);  // we take all matches and don't rank except to determine skip-past
                }
                else
                {
                    endStatesRanked.add(entryTaken);
                }
            }
            else
            {
                entryTaken = (RegexNFAStateEntry) value;
                endStatesRanked.add(entryTaken);
            }
            // could be null as removals take place

            if (entryTaken != null)
            {
                if (matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.PAST_LAST_ROW)
                {
                    int skipPastRow = entryTaken.getMatchEndEventSeqNo();
                    removeSkippedEndStates(endStatesPerBeginEvent, skipPastRow);
                }
                else if (matchRecognizeSpec.getSkip().getSkip() == MatchRecognizeSkipEnum.TO_NEXT_ROW)
                {
                    int skipPastRow = entryTaken.getMatchBeginEventSeqNo();
                    removeSkippedEndStates(endStatesPerBeginEvent, skipPastRow);
                }
            }
        }

        return endStatesRanked;
    }

    private void removeSkippedEndStates(TreeMap<Integer, Object> endStatesPerEndEvent, int skipPastRow)
    {
        for (Map.Entry<Integer, Object> entry : endStatesPerEndEvent.entrySet())
        {
            Object value = entry.getValue();

            if (value instanceof List)
            {
                List<RegexNFAStateEntry> endStatesUnranked = (List<RegexNFAStateEntry>) value;
                Iterator<RegexNFAStateEntry> it = endStatesUnranked.iterator();
                for (;it.hasNext();)
                {
                    RegexNFAStateEntry endState = it.next();
                    if (endState.getMatchBeginEventSeqNo() <= skipPastRow)
                    {
                        it.remove();
                    }
                }
            }
            else
            {
                RegexNFAStateEntry endState = (RegexNFAStateEntry) value;
                if (endState.getMatchBeginEventSeqNo() <= skipPastRow)
                {
                    endStatesPerEndEvent.put(entry.getKey(), null);
                }
            }
        }
    }

    private List<RegexNFAStateEntry> step(RegexNFAState[] startStates,
                                          Iterator<RegexNFAStateEntry> currentStatesIterator,
                                          EventBean theEvent,
                                          List<RegexNFAStateEntry> nextStates,
                                          List<RegexNFAStateEntry> endStates,
                                          boolean isRetainEventSet,
                                          int currentEventSequenceNumber,
                                          Object partitionKey)
    {
        List<RegexNFAStateEntry> terminationStates = null;  // always null or a list of entries (no singleton list)

        // handle start states for the event
        for (RegexNFAState startState : startStates)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegExStateStart(variableStreams, multimatchStreamNumToVariable);}

            EventBean[] eventsPerStream = new EventBean[numEventsEventsPerStreamDefine];
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
                    EventBean[] eventsForState = eventsPerStream;
                    MultimatchState[] multimatches = isCollectMultimatches ? new MultimatchState[multimatchVariablesArray.length] : null;
                    int[] greedyCounts = new int[allStates.length];

                    if (copy) {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;
                    }

                    if ((isCollectMultimatches) && (startState.isMultiple())) {
                        multimatches = addTag(startState.getStreamNum(), theEvent, multimatches);
                    }

                    if ((startState.isGreedy() != null) && (startState.isGreedy())) {
                        greedyCounts[startState.getNodeNumFlat()]++;
                    }

                    long time = 0;
                    if (matchRecognizeSpec.getInterval() != null) {
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

            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExStateStart(nextStates, variableStreams, multimatchStreamNumToVariable);}
        }

        // handle current state matching
        for (;currentStatesIterator.hasNext();)
        {
            RegexNFAStateEntry currentState  = currentStatesIterator.next();
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegExState(currentState, variableStreams, multimatchStreamNumToVariable);}

            EventBean[] eventsPerStream = currentState.getEventsPerStream();
            int currentStateStreamNum = currentState.getState().getStreamNum();
            eventsPerStream[currentStateStreamNum] = theEvent;
            if (isDefineAsksMultimatches) {
                eventsPerStream[numEventsEventsPerStreamDefine-1] = getMultimatchState(currentState);
            }

            if (currentState.getState().matches(eventsPerStream, agentInstanceContext))
            {
                if (isRetainEventSet)
                {
                    this.windowMatchedEventset.add(theEvent);
                }
                List<RegexNFAState> nextStatesFromHere = currentState.getState().getNextStates();

                // save state for each next state
                boolean copy = nextStatesFromHere.size() > 1;
                for (RegexNFAState next : nextStatesFromHere)
                {
                    EventBean[] eventsForState = eventsPerStream;
                    MultimatchState[] multimatches = currentState.getOptionalMultiMatches();
                    int[] greedyCounts = currentState.getGreedycountPerState();

                    if (copy)
                    {
                        eventsForState = new EventBean[eventsForState.length];
                        System.arraycopy(eventsPerStream, 0, eventsForState, 0, eventsForState.length);

                        int[] greedyCountsCopy = new int[greedyCounts.length];
                        System.arraycopy(greedyCounts, 0, greedyCountsCopy, 0, greedyCounts.length);
                        greedyCounts = greedyCountsCopy;

                        if (isCollectMultimatches) {
                            multimatches = deepCopy(multimatches);
                        }
                    }

                    if ((isCollectMultimatches) && (currentState.getState().isMultiple()))
                    {
                        multimatches = addTag(currentState.getState().getStreamNum(), theEvent, multimatches);
                    }

                    if ((currentState.getState().isGreedy() != null) && (currentState.getState().isGreedy()))
                    {
                        greedyCounts[currentState.getState().getNodeNumFlat()]++;
                    }

                    RegexNFAStateEntry entry = new RegexNFAStateEntry(currentState.getMatchBeginEventSeqNo(), currentState.getMatchBeginEventTime(), currentState.getState(), eventsForState, greedyCounts, multimatches, partitionKey);
                    if (next instanceof RegexNFAStateEnd)
                    {
                        entry.setMatchEndEventSeqNo(currentEventSequenceNumber);
                        endStates.add(entry);
                    }
                    else
                    {
                        entry.setState(next);
                        nextStates.add(entry);
                    }
                }
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExState(nextStates, variableStreams, multimatchStreamNumToVariable);}
            }
            // when not-matches
            else {
                if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExState(Collections.<RegexNFAStateEntry>emptyList(), variableStreams, multimatchStreamNumToVariable);}

                // determine interval and or-terminated
                if (isOrTerminated) {
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

        return terminationStates;   // only for immediate use, not for scheduled use as no copy of state
    }

    private ObjectArrayBackedEventBean getMultimatchState(RegexNFAStateEntry currentState) {
        if (currentState.getOptionalMultiMatches() == null || !currentState.getState().isExprRequiresMultimatchState()) {
            return null;
        }
        Object[] props = defineMultimatchEventBean.getProperties();
        MultimatchState[] states = currentState.getOptionalMultiMatches();
        for (int i = 0; i < props.length; i++) {
            MultimatchState state = states[i];
            if (state == null) {
                props[i] = null;
            }
            else {
                props[i] = state.getShrinkEventArray();
            }
        }
        return defineMultimatchEventBean;
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

    private MultimatchState[] addTag(int streamNum, EventBean theEvent, MultimatchState[] multimatches)
    {
        if (multimatches == null) {
            multimatches = new MultimatchState[multimatchVariablesArray.length];
        }

        int index = multimatchStreamNumToVariable[streamNum];
        MultimatchState state = multimatches[index];
        if (state == null) {
            multimatches[index] = new MultimatchState(theEvent);
            return multimatches;
        }

        multimatches[index].add(theEvent);
        return multimatches;
    }

    private EventBean generateOutputRow(RegexNFAStateEntry entry)
    {
        Object[] rowDataRaw = compositeEventBean.getProperties();

        // we first generate a raw row of <String, Object> for each variable name.
        for (Map.Entry<String, Pair<Integer, Boolean>> variableDef : variableStreams.entrySet())
        {
            if (!variableDef.getValue().getSecond()) {
                int index = variableDef.getValue().getFirst();
                rowDataRaw[index] = entry.getEventsPerStream()[index];
            }
        }
        if (aggregationService != null)
        {
            aggregationService.clearResults();
        }
        if (entry.getOptionalMultiMatches() != null)
        {
            MultimatchState[] multimatchState = entry.getOptionalMultiMatches();
            for (int i = 0; i < multimatchState.length; i++)
            {
                if (multimatchState[i] == null) {
                    rowDataRaw[multimatchVariableToStreamNum[i]] = null;
                    continue;
                }
                EventBean[] multimatchEvents = multimatchState[i].getShrinkEventArray();
                rowDataRaw[multimatchVariableToStreamNum[i]] = multimatchEvents;

                if (aggregationService != null) {
                    EventBean[] eventsPerStream = entry.getEventsPerStream();
                    int streamNum = multimatchVariableToStreamNum[i];

                    for (EventBean multimatchEvent : multimatchEvents) {
                        eventsPerStream[streamNum] = multimatchEvent;
                        aggregationService.applyEnter(eventsPerStream, streamNum, agentInstanceContext);
                    }
                }
            }
        }
        else {
            for (int index : multimatchVariableToStreamNum) {
                rowDataRaw[index] = null;
            }
        }

        Map<String, Object> row = new HashMap<String, Object>();
        int columnNum = 0;
        EventBean[] eventsPerStream = new EventBean[1];
        for (ExprEvaluator expression : columnEvaluators)
        {
            eventsPerStream[0] = compositeEventBean;
            Object result = expression.evaluate(eventsPerStream, true, agentInstanceContext);
            row.put(columnNames[columnNum], result);
            columnNum++;
        }

        return agentInstanceContext.getStatementContext().getEventAdapterService().adapterForTypedMap(row, rowEventType);
    }

    private void scheduleCallback(long msecAfterCurrentTime, RegexNFAStateEntry endState)
    {
        long matchBeginTime = endState.getMatchBeginEventTime();
        if (schedule.isEmpty())
        {
            schedule.put(matchBeginTime, endState);
            agentInstanceContext.getStatementContext().getSchedulingService().add(msecAfterCurrentTime, handle, scheduleSlot);
        }
        else
        {
            Object value = schedule.get(matchBeginTime);
            if (value == null)
            {
                long currentFirstKey = schedule.firstKey();
                if (currentFirstKey > matchBeginTime)
                {
                    agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                    agentInstanceContext.getStatementContext().getSchedulingService().add(msecAfterCurrentTime, handle, scheduleSlot);
                }

                schedule.put(matchBeginTime, endState);
            }
            else if (value instanceof RegexNFAStateEntry)
            {
                RegexNFAStateEntry valueEntry = (RegexNFAStateEntry) value;
                List<RegexNFAStateEntry> list = new ArrayList<RegexNFAStateEntry>();
                list.add(valueEntry);
                list.add(endState);
                schedule.put(matchBeginTime, list);
            }
            else
            {
                List<RegexNFAStateEntry> list = (List<RegexNFAStateEntry>) value;
                list.add(endState);
            }
        }
    }

    private void removeScheduleAddEndState(RegexNFAStateEntry terminationState, List<RegexNFAStateEntry> foundEndStates) {
        long matchBeginTime = terminationState.getMatchBeginEventTime();
        Object value = schedule.get(matchBeginTime);
        if (value == null) {
            return;
        }
        if (value instanceof RegexNFAStateEntry) {
            RegexNFAStateEntry single = (RegexNFAStateEntry) value;
            if (compareTerminationStateToEndState(terminationState, single)) {
                schedule.remove(matchBeginTime);
                if (schedule.isEmpty()) {
                    // we do not reschedule and accept a wasted schedule check
                    agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                }
                foundEndStates.add(single);
            }
        }
        else {
            List<RegexNFAStateEntry> entries = (List<RegexNFAStateEntry>) value;
            Iterator<RegexNFAStateEntry> it = entries.iterator();
            for (;it.hasNext();) {
                RegexNFAStateEntry endState = it.next();
                if (compareTerminationStateToEndState(terminationState, endState)) {
                    it.remove();
                    foundEndStates.add(endState);
                }
            }
            if (entries.isEmpty()) {
                schedule.remove(matchBeginTime);
                if (schedule.isEmpty()) {
                    // we do not reschedule and accept a wasted schedule check
                    agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                }
            }
        }
    }

    // End-state may have less events then the termination state
    private boolean compareTerminationStateToEndState(RegexNFAStateEntry terminationState, RegexNFAStateEntry endState) {
        if (terminationState.getMatchBeginEventSeqNo() != endState.getMatchBeginEventSeqNo()) {
            return false;
        }
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            int stream = entry.getValue().getFirst();
            boolean multi = entry.getValue().getSecond();
            if (multi) {
                EventBean[] termStreamEvents = getMultimatchArray(terminationState, stream);
                EventBean[] endStreamEvents = getMultimatchArray(endState, stream);
                if (endStreamEvents != null) {
                    if (termStreamEvents == null) {
                        return false;
                    }
                    for (int i = 0; i < endStreamEvents.length; i++) {
                        if (termStreamEvents.length > i && endStreamEvents[i] != termStreamEvents[i]) {
                            return false;
                        }
                    }
                }
            }
            else {
                EventBean termStreamEvent = terminationState.getEventsPerStream()[stream];
                EventBean endStreamEvent = endState.getEventsPerStream()[stream];
                if (endStreamEvent != null && endStreamEvent != termStreamEvent) {
                    return false;
                }
            }
        }
        return true;
    }

    private EventBean[] getMultimatchArray(RegexNFAStateEntry state, int stream) {
        if (state.getOptionalMultiMatches() == null) {
            return null;
        }
        int index = multimatchStreamNumToVariable[stream];
        MultimatchState multiMatches = state.getOptionalMultiMatches()[index];
        if (multiMatches == null) {
            return null;
        }
        return multiMatches.getShrinkEventArray();
    }

    private void triggered()
    {
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long intervalMSec = this.matchRecognizeSpec.getInterval().getScheduleBackwardDelta(currentTime, agentInstanceContext);
        if (schedule.isEmpty()) {
            return;
        }

        List<RegexNFAStateEntry> indicatables = new ArrayList<RegexNFAStateEntry>();
        while (true)
        {
            long firstKey = schedule.firstKey();
            long cutOffTime = currentTime - intervalMSec;
            if (firstKey > cutOffTime)
            {
                break;
            }

            Object value = schedule.remove(firstKey);

            if (value instanceof RegexNFAStateEntry)
            {
                indicatables.add((RegexNFAStateEntry) value);
            }
            else
            {
                List<RegexNFAStateEntry> list = (List<RegexNFAStateEntry>) value;
                indicatables.addAll(list);
            }

            if (schedule.isEmpty())
            {
                break;
            }
        }

        // schedule next
        if (!schedule.isEmpty())
        {
            long msecAfterCurrentTime = schedule.firstKey() + intervalMSec - agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            agentInstanceContext.getStatementContext().getSchedulingService().add(msecAfterCurrentTime, handle, scheduleSlot);
        }

        if (!matchRecognizeSpec.isAllMatches())
        {
            indicatables = rankEndStatesMultiPartition(indicatables);
        }

        EventBean[] outBeans = new EventBean[indicatables.size()];
        int count = 0;
        for (RegexNFAStateEntry endState : indicatables)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegMeasure(endState, variableStreams, multimatchStreamNumToVariable);}
            outBeans[count] = generateOutputRow(endState);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegMeasure(outBeans[count]);}
            count++;
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegOut(outBeans);}
        updateChildren(outBeans, null);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegOut();}
    }

    public RegexExprPreviousEvalStrategy getPreviousEvaluationStrategy() {
        return prevGetter;
    }

    public EventRowRegexNFAViewFactory getFactory() {
        return factory;
    }
}
