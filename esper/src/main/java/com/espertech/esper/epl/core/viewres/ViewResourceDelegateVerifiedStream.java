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
package com.espertech.esper.epl.core.viewres;

import com.espertech.esper.epl.expression.prev.ExprPreviousMatchRecognizeNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;

import java.util.*;

/**
 * Coordinates between view factories and requested resource (by expressions) the
 * availability of view resources to expressions.
 */
public class ViewResourceDelegateVerifiedStream {
    private final List<ExprPreviousNode> previousRequests;
    private final SortedMap<Integer, List<ExprPriorNode>> priorRequests;
    private final Set<ExprPreviousMatchRecognizeNode> matchRecognizePreviousRequests;

    public ViewResourceDelegateVerifiedStream(List<ExprPreviousNode> previousRequests, SortedMap<Integer, List<ExprPriorNode>> priorRequests, Set<ExprPreviousMatchRecognizeNode> matchRecognizePreviousRequests) {
        this.previousRequests = previousRequests;
        this.priorRequests = priorRequests;
        this.matchRecognizePreviousRequests = matchRecognizePreviousRequests;
    }

    public List<ExprPreviousNode> getPreviousRequests() {
        return previousRequests;
    }

    public SortedMap<Integer, List<ExprPriorNode>> getPriorRequests() {
        return priorRequests;
    }

    public Set<ExprPreviousMatchRecognizeNode> getMatchRecognizePreviousRequests() {
        return matchRecognizePreviousRequests;
    }

    public List<ExprPriorNode> getPriorRequestsAsList() {
        if (priorRequests.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExprPriorNode> nodes = new ArrayList<ExprPriorNode>();
        for (List<ExprPriorNode> priorNodes : priorRequests.values()) {
            nodes.addAll(priorNodes);
        }
        return nodes;
    }
}
