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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.pattern.core.*;

/**
 * This class represents a guard in the evaluation tree representing an event expressions.
 */
public class EvalGuardFactoryNode extends EvalFactoryNodeBase {
    protected GuardFactory guardFactory;
    protected EvalFactoryNode childNode;

    public void setGuardFactory(GuardFactory guardFactory) {
        this.guardFactory = guardFactory;
    }

    public void setChildNode(EvalFactoryNode childNode) {
        this.childNode = childNode;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(childNode, agentInstanceContext, parentNode);
        return new EvalGuardNode(agentInstanceContext, this, child);
    }

    public final String toString() {
        return "EvalGuardNode guardFactory=" + guardFactory;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return true;
    }

    public GuardFactory getGuardFactory() {
        return guardFactory;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
        childNode.accept(visitor);
    }
}
