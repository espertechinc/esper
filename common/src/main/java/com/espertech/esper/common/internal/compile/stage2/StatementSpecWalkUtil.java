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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextNested;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;

import java.util.*;

public class StatementSpecWalkUtil {

    public static boolean isPotentialSelfJoin(StatementSpecCompiled spec) {
        // Include create-context as nested contexts that have pattern-initiated sub-contexts may change filters during execution
        if (spec.getRaw().getCreateContextDesc() != null && spec.getRaw().getCreateContextDesc().getContextDetail() instanceof ContextNested) {
            return true;
        }

        // if order-by is specified, ans since multiple output rows may produce, ensure dispatch
        if (spec.getRaw().getOrderByList().size() > 0) {
            return true;
        }

        for (StreamSpecCompiled streamSpec : spec.getStreamSpecs()) {
            if (streamSpec instanceof PatternStreamSpecCompiled) {
                return true;
            }
        }

        // not a self join
        if ((spec.getStreamSpecs().length <= 1) && (spec.getSubselectNodes().size() == 0)) {
            return false;
        }

        // join - determine types joined
        List<EventType> filteredTypes = new ArrayList<EventType>();

        // consider subqueryes
        Set<EventType> optSubselectTypes = populateSubqueryTypes(spec.getSubselectNodes());

        boolean hasFilterStream = false;
        for (StreamSpecCompiled streamSpec : spec.getStreamSpecs()) {
            if (streamSpec instanceof FilterStreamSpecCompiled) {
                EventType type = ((FilterStreamSpecCompiled) streamSpec).getFilterSpecCompiled().getFilterForEventType();
                filteredTypes.add(type);
                hasFilterStream = true;
            }
        }

        if ((filteredTypes.size() == 1) && (optSubselectTypes.isEmpty())) {
            return false;
        }

        // pattern-only streams are not self-joins
        if (!hasFilterStream) {
            return false;
        }

        // is type overlap in filters
        for (int i = 0; i < filteredTypes.size(); i++) {
            for (int j = i + 1; j < filteredTypes.size(); j++) {
                EventType typeOne = filteredTypes.get(i);
                EventType typeTwo = filteredTypes.get(j);
                if (typeOne == typeTwo) {
                    return true;
                }

                if (typeOne.getSuperTypes() != null) {
                    for (EventType typeOneSuper : typeOne.getSuperTypes()) {
                        if (typeOneSuper == typeTwo) {
                            return true;
                        }
                    }
                }
                if (typeTwo.getSuperTypes() != null) {
                    for (EventType typeTwoSuper : typeTwo.getSuperTypes()) {
                        if (typeOne == typeTwoSuper) {
                            return true;
                        }
                    }
                }
            }
        }

        // analyze subselect types
        if (!optSubselectTypes.isEmpty()) {
            for (EventType typeOne : filteredTypes) {
                if (optSubselectTypes.contains(typeOne)) {
                    return true;
                }

                if (typeOne.getSuperTypes() != null) {
                    for (EventType typeOneSuper : typeOne.getSuperTypes()) {
                        if (optSubselectTypes.contains(typeOneSuper)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static Set<EventType> populateSubqueryTypes(List<ExprSubselectNode> subSelectExpressions) {
        Set<EventType> set = null;
        for (ExprSubselectNode subselect : subSelectExpressions) {
            for (StreamSpecCompiled streamSpec : subselect.getStatementSpecCompiled().getStreamSpecs()) {
                if (streamSpec instanceof FilterStreamSpecCompiled) {
                    EventType type = ((FilterStreamSpecCompiled) streamSpec).getFilterSpecCompiled().getFilterForEventType();
                    if (set == null) {
                        set = new HashSet<>();
                    }
                    set.add(type);
                } else if (streamSpec instanceof PatternStreamSpecCompiled) {
                    EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(((PatternStreamSpecCompiled) streamSpec).getRoot());
                    List<EvalFilterForgeNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
                    for (EvalFilterForgeNode filterNode : filterNodes) {
                        if (set == null) {
                            set = new HashSet<>();
                        }
                        set.add(filterNode.getFilterSpecCompiled().getFilterForEventType());
                    }
                }
            }
        }
        if (set == null) {
            return Collections.emptySet();
        }
        return set;
    }
}
