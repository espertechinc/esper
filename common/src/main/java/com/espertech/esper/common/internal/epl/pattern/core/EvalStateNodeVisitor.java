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

/**
 * Interface for visiting each element in the evaluation node tree for an event expression (see Visitor pattern).
 */
public interface EvalStateNodeVisitor {
    void visitGuard(EvalGuardFactoryNode factoryNode, EvalStateNode stateNode, Guard guard);

    void visitFollowedBy(EvalFollowedByFactoryNode factoryNode, EvalStateNode stateNode, Object... stateFlat);

    void visitFilter(EvalFilterFactoryNode factoryNode, EvalStateNode stateNode, EPStatementHandleCallbackFilter handle, MatchedEventMap beginState);

    void visitMatchUntil(EvalMatchUntilFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep);

    void visitObserver(EvalObserverFactoryNode factoryNode, EvalStateNode stateNode, EventObserver eventObserver);

    void visitNot(EvalNotFactoryNode factoryNode, EvalStateNode stateNode);

    void visitOr(EvalOrFactoryNode factoryNode, EvalStateNode stateNode);

    void visitRoot(EvalStateNode stateNode);

    void visitAnd(EvalAndFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep);

    void visitEvery(EvalEveryFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Object... stateFlat);

    void visitEveryDistinct(EvalEveryDistinctFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Collection keySetCollection);

    void visitAudit();
}
