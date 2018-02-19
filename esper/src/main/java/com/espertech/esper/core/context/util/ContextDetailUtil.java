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
package com.espertech.esper.core.context.util;

import com.espertech.esper.epl.spec.*;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.pattern.EvalFilterFactoryNode;
import com.espertech.esper.pattern.EvalNodeAnalysisResult;
import com.espertech.esper.pattern.EvalNodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContextDetailUtil {
    public static List<FilterSpecCompiled> getFilterSpecs(ContextDetail detail) {
        if (detail instanceof ContextDetailInitiatedTerminated) {
            ContextDetailInitiatedTerminated initTerm = (ContextDetailInitiatedTerminated) detail;
            List<FilterSpecCompiled> startFS = ContextDetailUtil.getFilterSpecIfAny(initTerm.getStart());
            List<FilterSpecCompiled> endFS = ContextDetailUtil.getFilterSpecIfAny(initTerm.getEnd());
            if (startFS == null && endFS == null) {
                return Collections.emptyList();
            }
            List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(2);
            if (startFS != null) {
                filters.addAll(startFS);
            }
            if (endFS != null) {
                filters.addAll(endFS);
            }
            return filters;
        } else if (detail instanceof ContextDetailCategory) {
            List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(1);
            filters.add(((ContextDetailCategory) detail).getFilterSpecCompiled());
            return filters;
        } else if (detail instanceof ContextDetailHash) {
            ContextDetailHash hash = (ContextDetailHash) detail;
            List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(hash.getItems().size());
            for (ContextDetailHashItem item : hash.getItems()) {
                filters.add(item.getFilterSpecCompiled());
            }
            return filters;
        } else if (detail instanceof ContextDetailNested) {
            ContextDetailNested nested = (ContextDetailNested) detail;
            List<FilterSpecCompiled> filterSpecs = new ArrayList<FilterSpecCompiled>(2);
            for (CreateContextDesc desc : nested.getContexts()) {
                filterSpecs.addAll(getFilterSpecs(desc.getContextDetail()));
            }
            return filterSpecs;
        } else if (detail instanceof ContextDetailPartitioned) {
            ContextDetailPartitioned partitioned = (ContextDetailPartitioned) detail;
            List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>(partitioned.getItems().size());
            for (ContextDetailPartitionItem item : partitioned.getItems()) {
                filters.add(item.getFilterSpecCompiled());
            }
            if (partitioned.getOptionalInit() != null) {
                for (ContextDetailConditionFilter filter : partitioned.getOptionalInit()) {
                    filters.add(filter.getFilterSpecCompiled());
                }
            }
            if (partitioned.getOptionalTermination() != null) {
                List<FilterSpecCompiled> specs = ContextDetailUtil.getFilterSpecIfAny(partitioned.getOptionalTermination());
                if (specs != null) {
                    filters.addAll(specs);
                }
            }
            return filters;
        } else {
            throw new IllegalStateException("Unrecognized context detail " + detail);
        }
    }

    public static List<FilterSpecCompiled> getFilterSpecIfAny(ContextDetailCondition detail) {
        if (detail instanceof ContextDetailConditionFilter) {
            ContextDetailConditionFilter filter = (ContextDetailConditionFilter) detail;
            return Collections.singletonList(filter.getFilterSpecCompiled());
        }
        if (detail instanceof ContextDetailConditionPattern) {
            ContextDetailConditionPattern pattern = (ContextDetailConditionPattern) detail;
            EvalNodeAnalysisResult result = EvalNodeUtil.recursiveAnalyzeChildNodes(pattern.getPatternCompiled().getEvalFactoryNode());
            if (result.getFilterNodes().isEmpty()) {
                return Collections.emptyList();
            }

            List<FilterSpecCompiled> filters = new ArrayList<FilterSpecCompiled>();
            List<EvalFilterFactoryNode> filterNodes = result.getFilterNodes();
            for (EvalFilterFactoryNode filterNode : filterNodes) {
                filters.add(filterNode.getFilterSpec());
            }
            return filters.isEmpty() ? Collections.emptyList() : filters;
        }
        return Collections.emptyList();
    }
}
