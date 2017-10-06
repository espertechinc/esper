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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements an aggregation service for match recognize.
 */
public class AggregationServiceMatchRecognizeFactoryImpl implements AggregationServiceMatchRecognizeFactory {
    private ExprEvaluator[][] evaluatorsEachStream;
    private AggregationMethodFactory[][] factoryEachStream;

    /**
     * Ctor.
     *
     * @param countStreams         number of streams/variables
     * @param aggregatorsPerStream aggregation methods per stream
     * @param evaluatorsPerStream  evaluation functions per stream
     */
    public AggregationServiceMatchRecognizeFactoryImpl(int countStreams, LinkedHashMap<Integer, AggregationMethodFactory[]> aggregatorsPerStream, Map<Integer, ExprEvaluator[]> evaluatorsPerStream) {
        evaluatorsEachStream = new ExprEvaluator[countStreams][];
        factoryEachStream = new AggregationMethodFactory[countStreams][];

        for (Map.Entry<Integer, AggregationMethodFactory[]> agg : aggregatorsPerStream.entrySet()) {
            factoryEachStream[agg.getKey()] = agg.getValue();
        }

        for (Map.Entry<Integer, ExprEvaluator[]> eval : evaluatorsPerStream.entrySet()) {
            evaluatorsEachStream[eval.getKey()] = eval.getValue();
        }
    }

    public AggregationServiceMatchRecognize makeService(AgentInstanceContext agentInstanceContext) {

        AggregationMethod[][] aggregatorsEachStream = new AggregationMethod[factoryEachStream.length][];

        int count = 0;
        for (int stream = 0; stream < factoryEachStream.length; stream++) {
            AggregationMethodFactory[] thatStream = factoryEachStream[stream];
            if (thatStream != null) {
                aggregatorsEachStream[stream] = new AggregationMethod[thatStream.length];
                for (int aggId = 0; aggId < thatStream.length; aggId++) {
                    aggregatorsEachStream[stream][aggId] = factoryEachStream[stream][aggId].make();
                    count++;
                }
            }
        }

        AggregationMethod[] aggregatorsAll = new AggregationMethod[count];
        count = 0;
        for (int stream = 0; stream < factoryEachStream.length; stream++) {
            if (factoryEachStream[stream] != null) {
                for (int aggId = 0; aggId < factoryEachStream[stream].length; aggId++) {
                    aggregatorsAll[count] = aggregatorsEachStream[stream][aggId];
                    count++;
                }
            }
        }

        return new AggregationServiceMatchRecognizeImpl(evaluatorsEachStream, aggregatorsEachStream, aggregatorsAll);
    }
}