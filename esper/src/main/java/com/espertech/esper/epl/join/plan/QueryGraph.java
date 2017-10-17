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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.hint.ExcludePlanFilterOperatorType;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.type.RelationalOpEnum;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Model of relationships between streams based on properties in both streams that are
 * specified as equal in a filter expression.
 */
public class QueryGraph {
    public final static int SELF_STREAM = Integer.MIN_VALUE;

    private final int numStreams;
    private final ExcludePlanHint optionalHint;
    private final boolean nToZeroAnalysis; // for subqueries and on-action
    private final Map<QueryGraphKey, QueryGraphValue> streamJoinMap;

    /**
     * Ctor.
     *
     * @param numStreams      - number of streams
     * @param optionalHint    hint if any
     * @param nToZeroAnalysis indicator for star-eval
     */
    public QueryGraph(int numStreams, ExcludePlanHint optionalHint, boolean nToZeroAnalysis) {
        this.numStreams = numStreams;
        this.optionalHint = optionalHint;
        this.nToZeroAnalysis = nToZeroAnalysis;
        streamJoinMap = new HashMap<QueryGraphKey, QueryGraphValue>();
    }

    /**
     * Returns the number of streams.
     *
     * @return number of streams
     */
    public int getNumStreams() {
        return numStreams;
    }

    /**
     * Add properties for 2 streams that are equal.
     *
     * @param streamLeft    - left hand stream
     * @param propertyLeft  - left hand stream property
     * @param streamRight   - right hand stream
     * @param propertyRight - right hand stream property
     * @param nodeLeft      left expr
     * @param nodeRight     right expr
     * @return true if added and did not exist, false if already known
     */
    public boolean addStrictEquals(int streamLeft, String propertyLeft, ExprIdentNode nodeLeft, int streamRight, String propertyRight, ExprIdentNode nodeRight) {
        check(streamLeft, streamRight);
        if (propertyLeft == null || propertyRight == null) {
            throw new IllegalArgumentException("Null property names supplied");
        }

        if (streamLeft == streamRight) {
            throw new IllegalArgumentException("Streams supplied are the same");
        }

        boolean addedLeft = internalAddEquals(streamLeft, propertyLeft, nodeLeft, streamRight, nodeRight);
        boolean addedRight = internalAddEquals(streamRight, propertyRight, nodeRight, streamLeft, nodeLeft);
        return addedLeft || addedRight;
    }

    public boolean isNavigableAtAll(int streamFrom, int streamTo) {
        QueryGraphKey key = new QueryGraphKey(streamFrom, streamTo);
        QueryGraphValue value = streamJoinMap.get(key);
        return value != null && !value.isEmptyNotNavigable();
    }

    /**
     * Returns set of streams that the given stream is navigable to.
     *
     * @param streamFrom - from stream number
     * @return set of streams related to this stream, or empty set if none
     */
    public Set<Integer> getNavigableStreams(int streamFrom) {
        Set<Integer> result = new HashSet<Integer>();
        for (int i = 0; i < numStreams; i++) {
            if (isNavigableAtAll(streamFrom, i)) {
                result.add(i);
            }
        }
        return result;
    }

    public QueryGraphValue getGraphValue(int streamLookup, int streamIndexed) {
        QueryGraphKey key = new QueryGraphKey(streamLookup, streamIndexed);
        QueryGraphValue value = streamJoinMap.get(key);
        if (value != null) {
            return value;
        }
        return new QueryGraphValue();
    }

    /**
     * Fill in equivalent key properties (navigation entries) on all streams.
     * For example, if  a=b and b=c  then addRelOpInternal a=c. The method adds new equalivalent key properties
     * until no additional entries to be added are found, ie. several passes can be made.
     *
     * @param queryGraph     - navigablity info between streamss
     * @param typesPerStream type info
     */
    public static void fillEquivalentNav(EventType[] typesPerStream, QueryGraph queryGraph) {
        boolean addedEquivalency;

        // Repeat until no more entries were added
        do {
            addedEquivalency = false;

            // For each stream-to-stream combination
            for (int lookupStream = 0; lookupStream < queryGraph.numStreams; lookupStream++) {
                for (int indexedStream = 0; indexedStream < queryGraph.numStreams; indexedStream++) {
                    if (lookupStream == indexedStream) {
                        continue;
                    }

                    boolean added = fillEquivalentNav(typesPerStream, queryGraph, lookupStream, indexedStream);
                    if (added) {
                        addedEquivalency = true;
                    }
                }
            }
        }
        while (addedEquivalency);
    }

    /*
     * Looks at the key and index (aka. left and right) properties of the 2 streams and checks
     * for each property if any equivalent index properties exist for other streams.
     */
    private static boolean fillEquivalentNav(EventType[] typesPerStream, QueryGraph queryGraph, int lookupStream, int indexedStream) {
        boolean addedEquivalency = false;

        QueryGraphValue value = queryGraph.getGraphValue(lookupStream, indexedStream);
        if (value.isEmptyNotNavigable()) {
            return false;
        }

        QueryGraphValuePairHashKeyIndex hashKeys = value.getHashKeyProps();
        String[] strictKeyProps = hashKeys.getStrictKeys();
        String[] indexProps = hashKeys.getIndexed();

        if (strictKeyProps.length == 0) {
            return false;
        }
        if (strictKeyProps.length != indexProps.length) {
            throw new IllegalStateException("Unexpected key and index property number mismatch");
        }

        for (int i = 0; i < strictKeyProps.length; i++) {
            if (strictKeyProps[i] == null) {
                continue;   // not a strict key
            }

            boolean added = fillEquivalentNav(typesPerStream, queryGraph, lookupStream, strictKeyProps[i], indexedStream, indexProps[i]);
            if (added) {
                addedEquivalency = true;
            }
        }

        return addedEquivalency;
    }

    /*
     * Looks at the key and index (aka. left and right) properties of the 2 streams and checks
     * for each property if any equivalent index properties exist for other streams.
     *
     * Example:  s0.p0 = s1.p1  and  s1.p1 = s2.p2  ==> therefore s0.p0 = s2.p2
     * ==> look stream s0, property p0; indexed stream s1, property p1
     * Is there any other lookup stream that has stream 1 and property p1 as index property? ==> this is stream s2, p2
     * Add navigation entry between stream s0 and property p0 to stream s2, property p2
     */
    private static boolean fillEquivalentNav(EventType[] typesPerStream, QueryGraph queryGraph, int lookupStream, String keyProp, int indexedStream, String indexProp) {
        boolean addedEquivalency = false;

        for (int otherStream = 0; otherStream < queryGraph.numStreams; otherStream++) {
            if ((otherStream == lookupStream) || (otherStream == indexedStream)) {
                continue;
            }

            QueryGraphValue value = queryGraph.getGraphValue(otherStream, indexedStream);
            QueryGraphValuePairHashKeyIndex hashKeys = value.getHashKeyProps();

            String[] otherStrictKeyProps = hashKeys.getStrictKeys();
            String[] otherIndexProps = hashKeys.getIndexed();
            int otherPropertyNum = -1;

            if (otherIndexProps == null) {
                continue;
            }

            for (int i = 0; i < otherIndexProps.length; i++) {
                if (otherIndexProps[i].equals(indexProp)) {
                    otherPropertyNum = i;
                    break;
                }
            }

            if (otherPropertyNum != -1) {
                if (otherStrictKeyProps[otherPropertyNum] != null) {
                    ExprIdentNode identNodeLookup = new ExprIdentNodeImpl(typesPerStream[lookupStream], keyProp, lookupStream);
                    ExprIdentNode identNodeOther = new ExprIdentNodeImpl(typesPerStream[otherStream], otherStrictKeyProps[otherPropertyNum], otherStream);
                    boolean added = queryGraph.addStrictEquals(lookupStream, keyProp, identNodeLookup, otherStream, otherStrictKeyProps[otherPropertyNum], identNodeOther);
                    if (added) {
                        addedEquivalency = true;
                    }
                }
            }
        }

        return addedEquivalency;
    }

    public String toString() {
        StringWriter buf = new StringWriter();
        PrintWriter writer = new PrintWriter(buf);

        int count = 0;
        for (Map.Entry<QueryGraphKey, QueryGraphValue> entry : streamJoinMap.entrySet()) {
            count++;
            writer.println("Entry " + count + ": key=" + entry.getKey());
            writer.println("  value=" + entry.getValue());
        }

        return buf.toString();
    }

    public void addRangeStrict(int streamNumStart, ExprIdentNode propertyStartExpr,
                               int streamNumEnd, ExprIdentNode propertyEndExpr,
                               int streamNumValue, ExprIdentNode propertyValueExpr,
                               QueryGraphRangeEnum rangeOp) {
        check(streamNumStart, streamNumValue);
        check(streamNumEnd, streamNumValue);

        // add as a range if the endpoints are from the same stream
        if (streamNumStart == streamNumEnd && streamNumStart != streamNumValue) {
            internalAddRange(streamNumStart, streamNumValue, rangeOp, propertyStartExpr, propertyEndExpr, propertyValueExpr);

            internalAddRelOp(streamNumValue, streamNumStart, propertyValueExpr, QueryGraphRangeEnum.GREATER_OR_EQUAL, propertyEndExpr, false);
            internalAddRelOp(streamNumValue, streamNumStart, propertyValueExpr, QueryGraphRangeEnum.LESS_OR_EQUAL, propertyStartExpr, false);
        } else {
            // endpoints from a different stream, add individually
            if (streamNumValue != streamNumStart) {
                // read propertyValue >= propertyStart
                internalAddRelOp(streamNumStart, streamNumValue, propertyStartExpr, QueryGraphRangeEnum.GREATER_OR_EQUAL, propertyValueExpr, true);
                // read propertyStart <= propertyValue
                internalAddRelOp(streamNumValue, streamNumStart, propertyValueExpr, QueryGraphRangeEnum.LESS_OR_EQUAL, propertyStartExpr, true);
            }

            if (streamNumValue != streamNumEnd) {
                // read propertyValue <= propertyEnd
                internalAddRelOp(streamNumEnd, streamNumValue, propertyEndExpr, QueryGraphRangeEnum.LESS_OR_EQUAL, propertyValueExpr, true);
                // read propertyEnd >= propertyValue
                internalAddRelOp(streamNumValue, streamNumEnd, propertyValueExpr, QueryGraphRangeEnum.GREATER_OR_EQUAL, propertyEndExpr, true);
            }
        }
    }

    public void addRelationalOpStrict(int streamIdLeft, ExprIdentNode propertyLeftExpr,
                                      int streamIdRight, ExprIdentNode propertyRightExpr,
                                      RelationalOpEnum relationalOpEnum) {
        check(streamIdLeft, streamIdRight);
        internalAddRelOp(streamIdLeft, streamIdRight, propertyLeftExpr, QueryGraphRangeEnum.mapFrom(relationalOpEnum.reversed()), propertyRightExpr, false);
        internalAddRelOp(streamIdRight, streamIdLeft, propertyRightExpr, QueryGraphRangeEnum.mapFrom(relationalOpEnum), propertyLeftExpr, false);
    }

    public void addUnkeyedExpression(int indexedStream, ExprIdentNode indexedProp, ExprNode exprNodeNoIdent) {
        if (indexedStream < 0 || indexedStream >= numStreams) {
            throw new IllegalArgumentException("Invalid indexed stream " + indexedStream);
        }

        if (numStreams > 1) {
            for (int i = 0; i < numStreams; i++) {
                if (i != indexedStream) {
                    internalAddEqualsUnkeyed(i, indexedStream, indexedProp, exprNodeNoIdent);
                }
            }
        } else {
            internalAddEqualsUnkeyed(SELF_STREAM, indexedStream, indexedProp, exprNodeNoIdent);
        }
    }

    public void addKeyedExpression(int indexedStream, ExprIdentNode indexedProp, int keyExprStream, ExprNode exprNodeNoIdent) {
        check(indexedStream, keyExprStream);
        internalAddEqualsNoProp(keyExprStream, indexedStream, indexedProp, exprNodeNoIdent);
    }

    private void check(int indexedStream, int keyStream) {
        if (indexedStream < 0 || indexedStream >= numStreams) {
            throw new IllegalArgumentException("Invalid indexed stream " + indexedStream);
        }
        if (keyStream >= numStreams) {
            throw new IllegalArgumentException("Invalid key stream " + keyStream);
        }
        if (numStreams > 1) {
            if (keyStream < 0) {
                throw new IllegalArgumentException("Invalid key stream " + keyStream);
            }
        } else {
            if (keyStream != SELF_STREAM) {
                throw new IllegalArgumentException("Invalid key stream " + keyStream);
            }
        }
        if (keyStream == indexedStream) {
            throw new IllegalArgumentException("Invalid key stream equals indexed stream " + keyStream);
        }
    }

    public void addRangeExpr(int indexedStream, ExprIdentNode indexedProp, ExprNode startNode, Integer optionalStartStreamNum, ExprNode endNode, Integer optionalEndStreamNum, QueryGraphRangeEnum rangeOp) {
        if (optionalStartStreamNum == null && optionalEndStreamNum == null) {
            if (numStreams > 1) {
                for (int i = 0; i < numStreams; i++) {
                    if (i == indexedStream) {
                        continue;
                    }
                    internalAddRange(i, indexedStream, rangeOp, startNode, endNode, indexedProp);
                }
            } else {
                internalAddRange(SELF_STREAM, indexedStream, rangeOp, startNode, endNode, indexedProp);
            }
            return;
        }

        optionalStartStreamNum = optionalStartStreamNum != null ? optionalStartStreamNum : -1;
        optionalEndStreamNum = optionalEndStreamNum != null ? optionalEndStreamNum : -1;

        // add for a specific stream only
        if (optionalStartStreamNum.equals(optionalEndStreamNum) || optionalEndStreamNum.equals(-1)) {
            internalAddRange(optionalStartStreamNum, indexedStream, rangeOp, startNode, endNode, indexedProp);
        }
        if (optionalStartStreamNum.equals(-1)) {
            internalAddRange(optionalEndStreamNum, indexedStream, rangeOp, startNode, endNode, indexedProp);
        }
    }

    public void addRelationalOp(int indexedStream, ExprIdentNode indexedProp, Integer keyStreamNum, ExprNode exprNodeNoIdent, RelationalOpEnum relationalOpEnum) {
        if (keyStreamNum == null) {
            if (numStreams > 1) {
                for (int i = 0; i < numStreams; i++) {
                    if (i == indexedStream) {
                        continue;
                    }
                    internalAddRelOp(i, indexedStream, exprNodeNoIdent, QueryGraphRangeEnum.mapFrom(relationalOpEnum), indexedProp, false);
                }
            } else {
                internalAddRelOp(SELF_STREAM, indexedStream, exprNodeNoIdent, QueryGraphRangeEnum.mapFrom(relationalOpEnum), indexedProp, false);
            }
            return;
        }

        // add for a specific stream only
        internalAddRelOp(keyStreamNum, indexedStream, exprNodeNoIdent, QueryGraphRangeEnum.mapFrom(relationalOpEnum), indexedProp, false);
    }

    public void addInSetSingleIndex(int testStreamNum, ExprNode testPropExpr, int setStreamNum, ExprNode[] setPropExpr) {
        check(testStreamNum, setStreamNum);
        internalAddInKeywordSingleIndex(setStreamNum, testStreamNum, testPropExpr, setPropExpr);
    }

    public void addInSetSingleIndexUnkeyed(int testStreamNum, ExprNode testPropExpr, ExprNode[] setPropExpr) {
        if (numStreams > 1) {
            for (int i = 0; i < numStreams; i++) {
                if (i != testStreamNum) {
                    internalAddInKeywordSingleIndex(i, testStreamNum, testPropExpr, setPropExpr);
                }
            }
        } else {
            internalAddInKeywordSingleIndex(SELF_STREAM, testStreamNum, testPropExpr, setPropExpr);
        }
    }

    public void addInSetMultiIndex(int testStreamNum, ExprNode testPropExpr, int setStreamNum, ExprNode[] setPropExpr) {
        check(testStreamNum, setStreamNum);
        internalAddInKeywordMultiIndex(testStreamNum, setStreamNum, testPropExpr, setPropExpr);
    }

    public void addInSetMultiIndexUnkeyed(ExprNode testPropExpr, int setStreamNum, ExprNode[] setPropExpr) {
        for (int i = 0; i < numStreams; i++) {
            if (i != setStreamNum) {
                internalAddInKeywordMultiIndex(i, setStreamNum, testPropExpr, setPropExpr);
            }
        }
    }

    public void addCustomIndex(String operationName, ExprNode[] indexExpressions, List<Pair<ExprNode, int[]>> streamKeys, int streamValue) {
        int expressionPosition = 0;
        for (Pair<ExprNode, int[]> pair : streamKeys) {
            if (pair.getSecond().length == 0) {
                if (numStreams > 1) {
                    for (int i = 0; i < numStreams; i++) {
                        if (i != streamValue) {
                            QueryGraphValue value = getCreateValue(i, streamValue);
                            value.addCustom(indexExpressions, operationName, expressionPosition, pair.getFirst());
                        }
                    }
                } else {
                    QueryGraphValue value = getCreateValue(SELF_STREAM, streamValue);
                    value.addCustom(indexExpressions, operationName, expressionPosition, pair.getFirst());
                }
            } else {
                for (int providingStream : pair.getSecond()) {
                    QueryGraphValue value = getCreateValue(providingStream, streamValue);
                    value.addCustom(indexExpressions, operationName, expressionPosition, pair.getFirst());
                }
            }
            expressionPosition++;
        }
    }

    private void internalAddRange(int streamKey, int streamValue, QueryGraphRangeEnum rangeOp, ExprNode propertyStartExpr, ExprNode propertyEndExpr, ExprIdentNode propertyValueExpr) {
        if (nToZeroAnalysis && streamValue != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(streamKey, streamValue, ExcludePlanFilterOperatorType.RELOP)) {
            return;
        }
        QueryGraphValue valueLeft = getCreateValue(streamKey, streamValue);
        valueLeft.addRange(rangeOp, propertyStartExpr, propertyEndExpr, propertyValueExpr);
    }

    private void internalAddRelOp(int streamKey, int streamValue, ExprNode keyExpr, QueryGraphRangeEnum rangeEnum, ExprIdentNode valueExpr, boolean isBetweenOrIn) {
        if (nToZeroAnalysis && streamValue != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(streamKey, streamValue, ExcludePlanFilterOperatorType.RELOP)) {
            return;
        }
        QueryGraphValue value = getCreateValue(streamKey, streamValue);
        value.addRelOp(keyExpr, rangeEnum, valueExpr, isBetweenOrIn);
    }

    private boolean internalAddEquals(int streamLookup, String propertyLookup, ExprIdentNode propertyLookupNode, int streamIndexed, ExprIdentNode propertyIndexedNode) {
        if (nToZeroAnalysis && streamIndexed != 0) {
            return false;
        }
        if (optionalHint != null && optionalHint.filter(streamLookup, propertyIndexedNode.getStreamId(), ExcludePlanFilterOperatorType.EQUALS, propertyLookupNode, propertyIndexedNode)) {
            return false;
        }
        QueryGraphValue value = getCreateValue(streamLookup, streamIndexed);
        return value.addStrictCompare(propertyLookup, propertyLookupNode, propertyIndexedNode);
    }

    private void internalAddEqualsNoProp(int keyExprStream, int indexedStream, ExprIdentNode indexedProp, ExprNode exprNodeNoIdent) {
        if (nToZeroAnalysis && indexedStream != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(keyExprStream, indexedStream, ExcludePlanFilterOperatorType.EQUALS)) {
            return;
        }
        QueryGraphValue value = getCreateValue(keyExprStream, indexedStream);
        value.addKeyedExpr(indexedProp, exprNodeNoIdent);
    }

    private void internalAddEqualsUnkeyed(int streamKey, int streamValue, ExprIdentNode indexedProp, ExprNode exprNodeNoIdent) {
        if (nToZeroAnalysis && streamValue != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(streamKey, streamValue, ExcludePlanFilterOperatorType.EQUALS)) {
            return;
        }
        QueryGraphValue value = getCreateValue(streamKey, streamValue);
        value.addUnkeyedExpr(indexedProp, exprNodeNoIdent);
    }

    private void internalAddInKeywordSingleIndex(int streamKey, int streamValue, ExprNode testPropExpr, ExprNode[] setPropExpr) {
        if (nToZeroAnalysis && streamValue != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(streamKey, streamValue, ExcludePlanFilterOperatorType.INKW)) {
            return;
        }
        QueryGraphValue valueSingleIdx = getCreateValue(streamKey, streamValue);
        valueSingleIdx.addInKeywordSingleIdx(testPropExpr, setPropExpr);
    }

    private void internalAddInKeywordMultiIndex(int streamKey, int streamValue, ExprNode testPropExpr, ExprNode[] setPropExpr) {
        if (nToZeroAnalysis && streamValue != 0) {
            return;
        }
        if (optionalHint != null && optionalHint.filter(streamKey, streamValue, ExcludePlanFilterOperatorType.INKW)) {
            return;
        }
        QueryGraphValue value = getCreateValue(streamKey, streamValue);
        value.addInKeywordMultiIdx(testPropExpr, setPropExpr);
    }

    private QueryGraphValue getCreateValue(int streamKey, int streamValue) {
        check(streamValue, streamKey);
        QueryGraphKey key = new QueryGraphKey(streamKey, streamValue);
        QueryGraphValue value = streamJoinMap.get(key);
        if (value == null) {
            value = new QueryGraphValue();
            streamJoinMap.put(key, value);
        }
        return value;
    }
}
