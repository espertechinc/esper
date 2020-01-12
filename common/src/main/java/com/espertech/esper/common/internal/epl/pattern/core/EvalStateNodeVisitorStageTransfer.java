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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.context.util.AgentInstanceTransferServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.epl.pattern.and.EvalAndFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.Guard;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.not.EvalNotFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EventObserver;
import com.espertech.esper.common.internal.epl.pattern.or.EvalOrFactoryNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.Collection;

public class EvalStateNodeVisitorStageTransfer implements EvalStateNodeVisitor {
    private final AgentInstanceTransferServices services;

    public EvalStateNodeVisitorStageTransfer(AgentInstanceTransferServices services) {
        this.services = services;
    }

    public void visitGuard(EvalGuardFactoryNode factoryNode, EvalStateNode stateNode, Guard guard) {
        stateNode.transfer(services);
    }

    public void visitFilter(EvalFilterFactoryNode factoryNode, EvalStateNode stateNode, EPStatementHandleCallbackFilter handle, MatchedEventMap beginState) {
        stateNode.transfer(services);
    }

    public void visitObserver(EvalObserverFactoryNode factoryNode, EvalStateNode stateNode, EventObserver eventObserver) {
        stateNode.transfer(services);
    }

    public void visitFollowedBy(EvalFollowedByFactoryNode factoryNode, EvalStateNode stateNode, Object... stateFlat) {
        // no action
    }

    public void visitMatchUntil(EvalMatchUntilFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep) {
        // no action
    }

    public void visitNot(EvalNotFactoryNode factoryNode, EvalStateNode stateNode) {
        // no action
    }

    public void visitOr(EvalOrFactoryNode factoryNode, EvalStateNode stateNode) {
        // no action
    }

    public void visitRoot(EvalStateNode stateNode) {
        // no action
    }

    public void visitAnd(EvalAndFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep) {
        // no action
    }

    public void visitEvery(EvalEveryFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Object... stateFlat) {
        // no action
    }

    public void visitEveryDistinct(EvalEveryDistinctFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Collection keySetCollection) {
        // no action
    }

}
