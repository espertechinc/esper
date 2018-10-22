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

import com.espertech.esper.common.internal.epl.pattern.and.EvalAndFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.everydistinct.EvalEveryDistinctFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.followedby.EvalFollowedByFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.guard.EvalGuardFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.matchuntil.EvalMatchUntilFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.not.EvalNotFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.or.EvalOrFactoryNode;

public interface EvalFactoryNodeVisitor {

    void visit(EvalRootFactoryNode root);

    void visit(EvalOrFactoryNode or);

    void visit(EvalNotFactoryNode not);

    void visit(EvalEveryDistinctFactoryNode everyDistinct);

    void visit(EvalMatchUntilFactoryNode matchUntil);

    void visit(EvalFollowedByFactoryNode followedBy);

    void visit(EvalAndFactoryNode and);

    void visit(EvalObserverFactoryNode observer);

    void visit(EvalEveryFactoryNode every);

    void visit(EvalGuardFactoryNode guard);

    void visit(EvalFilterFactoryNode filter);
}
