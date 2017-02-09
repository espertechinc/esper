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

public interface AIRegistryExpr {

    public AIRegistrySubselect getSubselectService(ExprSubselectNode exprSubselectNode);

    public AIRegistryAggregation getSubselectAggregationService(ExprSubselectNode exprSubselectNode);

    public AIRegistryPrior getPriorServices(ExprPriorNode key);

    public AIRegistryPrevious getPreviousServices(ExprPreviousNode key);

    public AIRegistryMatchRecognizePrevious getMatchRecognizePrevious();

    public AIRegistryTableAccess getTableAccessServices(ExprTableAccessNode key);

    public AIRegistrySubselect allocateSubselect(ExprSubselectNode subselectNode);

    public AIRegistryAggregation allocateSubselectAggregation(ExprSubselectNode subselectNode);

    public AIRegistryPrior allocatePrior(ExprPriorNode key);

    public AIRegistryPrevious allocatePrevious(ExprPreviousNode previousNode);

    public AIRegistryMatchRecognizePrevious allocateMatchRecognizePrevious();

    public AIRegistryTableAccess allocateTableAccess(ExprTableAccessNode tableNode);

    public AIRegistryPrior getOrAllocatePrior(ExprPriorNode key);

    public AIRegistryPrevious getOrAllocatePrevious(ExprPreviousNode key);

    public AIRegistrySubselect getOrAllocateSubquery(ExprSubselectNode key);

    public AIRegistryAggregation getOrAllocateSubselectAggregation(ExprSubselectNode exprSubselectNode);

    public int getSubselectAgentInstanceCount();

    public int getPreviousAgentInstanceCount();

    public int getPriorAgentInstanceCount();

    public void deassignService(int agentInstanceId);
}
