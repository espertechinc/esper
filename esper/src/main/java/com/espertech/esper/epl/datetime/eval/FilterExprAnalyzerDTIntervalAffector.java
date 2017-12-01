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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.type.RelationalOpEnum;

import java.util.List;

public class FilterExprAnalyzerDTIntervalAffector implements FilterExprAnalyzerAffector {
    private final DatetimeMethodEnum currentMethod;
    private final EventType[] typesPerStream;
    private final int targetStreamNum;
    private final String targetStartProp;
    private final String targetEndProp;
    private final Integer parameterStreamNum;
    private final String parameterStartProp;
    private final String parameterEndProp;

    public FilterExprAnalyzerDTIntervalAffector(DatetimeMethodEnum currentMethod, EventType[] typesPerStream, int targetStreamNum, String targetStartProp, String targetEndProp, Integer parameterStreamNum, String parameterStartProp, String parameterEndProp) {
        this.currentMethod = currentMethod;
        this.typesPerStream = typesPerStream;
        this.targetStreamNum = targetStreamNum;
        this.targetStartProp = targetStartProp;
        this.targetEndProp = targetEndProp;
        this.parameterStreamNum = parameterStreamNum;
        this.parameterStartProp = parameterStartProp;
        this.parameterEndProp = parameterEndProp;
    }

    public ExprNode[] getIndexExpressions() {
        return null;
    }

    public List<Pair<ExprNode, int[]>> getKeyExpressions() {
        return null;
    }

    public AdvancedIndexConfigContextPartition getOptionalIndexSpec() {
        return null;
    }

    public String getOptionalIndexName() {
        return null;
    }

    public void apply(QueryGraph queryGraph) {

        if (targetStreamNum == parameterStreamNum) {
            return;
        }

        ExprIdentNode targetStartExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, targetStreamNum, targetStartProp);
        ExprIdentNode targetEndExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, targetStreamNum, targetEndProp);
        ExprIdentNode parameterStartExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, parameterStreamNum, parameterStartProp);
        ExprIdentNode parameterEndExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, parameterStreamNum, parameterEndProp);

        if (targetStartExpr.getForge().getEvaluationType() != parameterStartExpr.getForge().getEvaluationType()) {
            return;
        }

        if (currentMethod == DatetimeMethodEnum.BEFORE) {
            // a.end < b.start
            queryGraph.addRelationalOpStrict(targetStreamNum, targetEndExpr,
                    parameterStreamNum, parameterStartExpr,
                    RelationalOpEnum.LT);
        } else if (currentMethod == DatetimeMethodEnum.AFTER) {
            // a.start > b.end
            queryGraph.addRelationalOpStrict(targetStreamNum, targetStartExpr,
                    parameterStreamNum, parameterEndExpr,
                    RelationalOpEnum.GT);
        } else if (currentMethod == DatetimeMethodEnum.COINCIDES) {
            // a.startTimestamp = b.startTimestamp and a.endTimestamp = b.endTimestamp
            queryGraph.addStrictEquals(targetStreamNum, targetStartProp, targetStartExpr,
                    parameterStreamNum, parameterStartProp, parameterStartExpr);

            boolean noDuration = parameterEndProp.equals(parameterStartProp) && targetEndProp.equals(targetStartProp);
            if (!noDuration) {
                ExprIdentNode leftEndExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, targetStreamNum, targetEndProp);
                ExprIdentNode rightEndExpr = ExprNodeUtilityCore.getExprIdentNode(typesPerStream, parameterStreamNum, parameterEndProp);
                queryGraph.addStrictEquals(targetStreamNum, targetEndProp, leftEndExpr,
                        parameterStreamNum, parameterEndProp, rightEndExpr);
            }
        } else if (currentMethod == DatetimeMethodEnum.DURING || currentMethod == DatetimeMethodEnum.INCLUDES) {
            // DURING:   b.startTimestamp < a.startTimestamp <= a.endTimestamp < b.endTimestamp
            // INCLUDES: a.startTimestamp < b.startTimestamp <= b.endTimestamp < a.endTimestamp
            RelationalOpEnum relop = currentMethod == DatetimeMethodEnum.DURING ? RelationalOpEnum.LT : RelationalOpEnum.GT;
            queryGraph.addRelationalOpStrict(parameterStreamNum, parameterStartExpr,
                    targetStreamNum, targetStartExpr,
                    relop);

            queryGraph.addRelationalOpStrict(targetStreamNum, targetEndExpr,
                    parameterStreamNum, parameterEndExpr,
                    relop);
        } else if (currentMethod == DatetimeMethodEnum.FINISHES || currentMethod == DatetimeMethodEnum.FINISHEDBY) {
            // FINISHES:   b.startTimestamp < a.startTimestamp and a.endTimestamp = b.endTimestamp
            // FINISHEDBY: a.startTimestamp < b.startTimestamp and a.endTimestamp = b.endTimestamp
            RelationalOpEnum relop = currentMethod == DatetimeMethodEnum.FINISHES ? RelationalOpEnum.LT : RelationalOpEnum.GT;
            queryGraph.addRelationalOpStrict(parameterStreamNum, parameterStartExpr,
                    targetStreamNum, targetStartExpr,
                    relop);

            queryGraph.addStrictEquals(targetStreamNum, targetEndProp, targetEndExpr,
                    parameterStreamNum, parameterEndProp, parameterEndExpr);
        } else if (currentMethod == DatetimeMethodEnum.MEETS) {
            // a.endTimestamp = b.startTimestamp
            queryGraph.addStrictEquals(targetStreamNum, targetEndProp, targetEndExpr,
                    parameterStreamNum, parameterStartProp, parameterStartExpr);
        } else if (currentMethod == DatetimeMethodEnum.METBY) {
            // a.startTimestamp = b.endTimestamp
            queryGraph.addStrictEquals(targetStreamNum, targetStartProp, targetStartExpr,
                    parameterStreamNum, parameterEndProp, parameterEndExpr);
        } else if (currentMethod == DatetimeMethodEnum.OVERLAPS || currentMethod == DatetimeMethodEnum.OVERLAPPEDBY) {
            // OVERLAPS:     a.startTimestamp < b.startTimestamp < a.endTimestamp < b.endTimestamp
            // OVERLAPPEDBY: b.startTimestamp < a.startTimestamp < b.endTimestamp < a.endTimestamp
            RelationalOpEnum relop = currentMethod == DatetimeMethodEnum.OVERLAPS ? RelationalOpEnum.LT : RelationalOpEnum.GT;
            queryGraph.addRelationalOpStrict(targetStreamNum, targetStartExpr,
                    parameterStreamNum, parameterStartExpr,
                    relop);

            queryGraph.addRelationalOpStrict(targetStreamNum, targetEndExpr,
                    parameterStreamNum, parameterEndExpr,
                    relop);

            if (currentMethod == DatetimeMethodEnum.OVERLAPS) {
                queryGraph.addRelationalOpStrict(parameterStreamNum, parameterStartExpr,
                        targetStreamNum, targetEndExpr,
                        RelationalOpEnum.LT);
            } else {
                queryGraph.addRelationalOpStrict(targetStreamNum, targetStartExpr,
                        parameterStreamNum, parameterEndExpr,
                        RelationalOpEnum.LT);
            }
        } else if (currentMethod == DatetimeMethodEnum.STARTS || currentMethod == DatetimeMethodEnum.STARTEDBY) {
            // STARTS:       a.startTimestamp = b.startTimestamp and a.endTimestamp < b.endTimestamp
            // STARTEDBY:    a.startTimestamp = b.startTimestamp and b.endTimestamp < a.endTimestamp
            queryGraph.addStrictEquals(targetStreamNum, targetStartProp, targetStartExpr,
                    parameterStreamNum, parameterStartProp, parameterStartExpr);

            RelationalOpEnum relop = currentMethod == DatetimeMethodEnum.STARTS ? RelationalOpEnum.LT : RelationalOpEnum.GT;
            queryGraph.addRelationalOpStrict(targetStreamNum, targetEndExpr,
                    parameterStreamNum, parameterEndExpr,
                    relop);
        }
    }
}

