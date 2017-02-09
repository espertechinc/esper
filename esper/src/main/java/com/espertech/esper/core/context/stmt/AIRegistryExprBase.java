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
package com.espertech.esper.core.context.stmt;

import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;

import java.util.HashMap;
import java.util.Map;

public abstract class AIRegistryExprBase implements AIRegistryExpr {

    private final Map<ExprSubselectNode, AIRegistrySubselect> subselects;
    private final Map<ExprSubselectNode, AIRegistryAggregation> subselectAggregations;
    private final Map<ExprPriorNode, AIRegistryPrior> priors;
    private final Map<ExprPreviousNode, AIRegistryPrevious> previous;
    private final AIRegistryMatchRecognizePrevious matchRecognizePrevious;
    private final Map<ExprTableAccessNode, AIRegistryTableAccess> tableAccess;

    public AIRegistryExprBase() {
        subselects = new HashMap<ExprSubselectNode, AIRegistrySubselect>();
        subselectAggregations = new HashMap<ExprSubselectNode, AIRegistryAggregation>();
        priors = new HashMap<ExprPriorNode, AIRegistryPrior>();
        previous = new HashMap<ExprPreviousNode, AIRegistryPrevious>();
        matchRecognizePrevious = allocateAIRegistryMatchRecognizePrevious();
        tableAccess = new HashMap<ExprTableAccessNode, AIRegistryTableAccess>();
    }

    public abstract AIRegistrySubselect allocateAIRegistrySubselect();

    public abstract AIRegistryPrevious allocateAIRegistryPrevious();

    public abstract AIRegistryPrior allocateAIRegistryPrior();

    public abstract AIRegistryAggregation allocateAIRegistrySubselectAggregation();

    public abstract AIRegistryMatchRecognizePrevious allocateAIRegistryMatchRecognizePrevious();

    public abstract AIRegistryTableAccess allocateAIRegistryTableAccess();

    public AIRegistrySubselect getSubselectService(ExprSubselectNode exprSubselectNode) {
        return subselects.get(exprSubselectNode);
    }

    public AIRegistryAggregation getSubselectAggregationService(ExprSubselectNode exprSubselectNode) {
        return subselectAggregations.get(exprSubselectNode);
    }

    public AIRegistryPrior getPriorServices(ExprPriorNode key) {
        return priors.get(key);
    }

    public AIRegistryPrevious getPreviousServices(ExprPreviousNode key) {
        return previous.get(key);
    }

    public AIRegistryMatchRecognizePrevious getMatchRecognizePrevious() {
        return matchRecognizePrevious;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public AIRegistryTableAccess getTableAccessServices(ExprTableAccessNode key) {
        return tableAccess.get(key);
    }

    public AIRegistrySubselect allocateSubselect(ExprSubselectNode subselectNode) {
        AIRegistrySubselect subselect = allocateAIRegistrySubselect();
        subselects.put(subselectNode, subselect);
        return subselect;
    }

    public AIRegistryAggregation allocateSubselectAggregation(ExprSubselectNode subselectNode) {
        AIRegistryAggregation subselectAggregation = allocateAIRegistrySubselectAggregation();
        subselectAggregations.put(subselectNode, subselectAggregation);
        return subselectAggregation;
    }

    public AIRegistryPrior allocatePrior(ExprPriorNode key) {
        AIRegistryPrior service = allocateAIRegistryPrior();
        priors.put(key, service);
        return service;
    }

    public AIRegistryPrior getOrAllocatePrior(ExprPriorNode key) {
        AIRegistryPrior existing = priors.get(key);
        if (existing != null) {
            return existing;
        }
        return allocatePrior(key);
    }

    public AIRegistryPrevious getOrAllocatePrevious(ExprPreviousNode key) {
        AIRegistryPrevious existing = previous.get(key);
        if (existing != null) {
            return existing;
        }
        return allocatePrevious(key);
    }

    public AIRegistrySubselect getOrAllocateSubquery(ExprSubselectNode key) {
        AIRegistrySubselect existing = subselects.get(key);
        if (existing != null) {
            return existing;
        }
        return allocateSubselect(key);
    }

    public AIRegistryAggregation getOrAllocateSubselectAggregation(ExprSubselectNode subselectNode) {
        AIRegistryAggregation existing = subselectAggregations.get(subselectNode);
        if (existing != null) {
            return existing;
        }
        return allocateSubselectAggregation(subselectNode);
    }

    public AIRegistryPrevious allocatePrevious(ExprPreviousNode previousNode) {
        AIRegistryPrevious service = allocateAIRegistryPrevious();
        previous.put(previousNode, service);
        return service;
    }

    public AIRegistryTableAccess allocateTableAccess(ExprTableAccessNode tableNode) {
        AIRegistryTableAccess service = allocateAIRegistryTableAccess();
        tableAccess.put(tableNode, service);
        return service;
    }

    public AIRegistryMatchRecognizePrevious allocateMatchRecognizePrevious() {
        return matchRecognizePrevious;
    }

    public int getSubselectAgentInstanceCount() {
        int total = 0;
        for (Map.Entry<ExprSubselectNode, AIRegistrySubselect> entry : subselects.entrySet()) {
            total += entry.getValue().getAgentInstanceCount();
        }
        return total;
    }

    public int getPreviousAgentInstanceCount() {
        int total = 0;
        for (Map.Entry<ExprPreviousNode, AIRegistryPrevious> entry : previous.entrySet()) {
            total += entry.getValue().getAgentInstanceCount();
        }
        return total;
    }

    public int getPriorAgentInstanceCount() {
        int total = 0;
        for (Map.Entry<ExprPriorNode, AIRegistryPrior> entry : priors.entrySet()) {
            total += entry.getValue().getAgentInstanceCount();
        }
        return total;
    }

    public void deassignService(int agentInstanceId) {
        for (Map.Entry<ExprSubselectNode, AIRegistrySubselect> entry : subselects.entrySet()) {
            entry.getValue().deassignService(agentInstanceId);
        }
        for (Map.Entry<ExprSubselectNode, AIRegistryAggregation> entry : subselectAggregations.entrySet()) {
            entry.getValue().deassignService(agentInstanceId);
        }
        for (Map.Entry<ExprPriorNode, AIRegistryPrior> entry : priors.entrySet()) {
            entry.getValue().deassignService(agentInstanceId);
        }
        for (Map.Entry<ExprPreviousNode, AIRegistryPrevious> entry : previous.entrySet()) {
            entry.getValue().deassignService(agentInstanceId);
        }
        for (Map.Entry<ExprTableAccessNode, AIRegistryTableAccess> entry : tableAccess.entrySet()) {
            entry.getValue().deassignService(agentInstanceId);
        }
        matchRecognizePrevious.deassignService(agentInstanceId);
    }
}
