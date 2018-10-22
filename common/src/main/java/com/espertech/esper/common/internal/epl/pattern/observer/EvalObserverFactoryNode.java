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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNodeVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;

public class EvalObserverFactoryNode extends EvalFactoryNodeBase {

    private ObserverFactory observerFactory;

    public void setObserverFactory(ObserverFactory observerFactory) {
        this.observerFactory = observerFactory;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return false;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return new EvalObserverNode(agentInstanceContext, this);
    }

    public ObserverFactory getObserverFactory() {
        return observerFactory;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return observerFactory.isNonRestarting();
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
    }
}
