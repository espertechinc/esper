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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.context.activator.ViewableActivator;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirementSubquery;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.StatementContext;

public class SubSelectFactory implements StatementReadyCallback {
    private int subqueryNumber;
    private ViewableActivator activator;
    private SubSelectStrategyFactory strategyFactory;
    private boolean hasAggregation;
    private boolean hasPrior;
    private boolean hasPrevious;

    public int getSubqueryNumber() {
        return subqueryNumber;
    }

    public void setSubqueryNumber(int subqueryNumber) {
        this.subqueryNumber = subqueryNumber;
    }

    public ViewableActivator getActivator() {
        return activator;
    }

    public void setActivator(ViewableActivator activator) {
        this.activator = activator;
    }

    public SubSelectStrategyFactory getStrategyFactory() {
        return strategyFactory;
    }

    public void setStrategyFactory(SubSelectStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public void setHasAggregation(boolean hasAggregation) {
        this.hasAggregation = hasAggregation;
    }

    public void setHasPrior(boolean hasPrior) {
        this.hasPrior = hasPrior;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        strategyFactory.ready(statementContext, activator.getEventType());
    }

    public void ready(SubSelectStrategyFactoryContext subselectFactoryContext, boolean recovery) {
        strategyFactory.ready(subselectFactoryContext, activator.getEventType());
    }

    public AIRegistryRequirementSubquery getRegistryRequirements() {
        return new AIRegistryRequirementSubquery(hasAggregation, hasPrior, hasPrevious, strategyFactory.getLookupStrategyDesc());
    }
}
