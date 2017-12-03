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
package com.espertech.esper.pattern;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.guard.Guard;
import com.espertech.esper.pattern.observer.EventObserver;

import java.util.Collection;

/**
 * Interface for visiting each element in the evaluation node tree for an event expression (see Visitor pattern).
 */
public interface EvalStateNodeVisitor {
    public void visitGuard(EvalGuardFactoryNode factoryNode, EvalStateNode stateNode, Guard guard);

    public void visitFollowedBy(EvalFollowedByFactoryNode factoryNode, EvalStateNode stateNode, Object... stateFlat);

    public void visitFilter(EvalFilterFactoryNode factoryNode, EvalStateNode stateNode, EPStatementHandleCallback handle, MatchedEventMap beginState);

    public void visitMatchUntil(EvalMatchUntilFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep);

    public void visitObserver(EvalObserverFactoryNode factoryNode, EvalStateNode stateNode, EventObserver eventObserver);

    public void visitNot(EvalNotFactoryNode factoryNode, EvalStateNode stateNode);

    public void visitOr(EvalOrFactoryNode factoryNode, EvalStateNode stateNode);

    public void visitRoot(EvalStateNode stateNode);

    public void visitAnd(EvalAndFactoryNode factoryNode, EvalStateNode stateNode, Object... stateDeep);

    public void visitEvery(EvalEveryFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Object... stateFlat);

    public void visitEveryDistinct(EvalEveryDistinctFactoryNode factoryNode, EvalStateNode stateNode, MatchedEventMap beginState, Collection keySetCollection);

    public void visitAudit();
}
