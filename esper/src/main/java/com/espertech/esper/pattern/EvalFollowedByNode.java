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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a followed-by operator in the evaluation tree representing any event expressions.
 */
public class EvalFollowedByNode extends EvalNodeBase {
    protected final EvalFollowedByFactoryNode factoryNode;
    private final EvalNode[] childNodes;

    public EvalFollowedByNode(PatternAgentInstanceContext context, EvalFollowedByFactoryNode factoryNode, EvalNode[] childNodes) {
        super(context);
        this.factoryNode = factoryNode;
        this.childNodes = childNodes;
    }

    public EvalNode[] getChildNodes() {
        return childNodes;
    }

    public EvalFollowedByFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public EvalStateNode newState(Evaluator parentNode,
                                  EvalStateNodeNumber stateNodeNumber, long stateNodeId) {
        switch (factoryNode.opType) {
            case NOMAX_PLAIN:
                return new EvalFollowedByStateNode(parentNode, this);
            default:
                return new EvalFollowedByWithMaxStateNodeManaged(parentNode, this);
        }
    }

    public boolean isTrackWithPool() {
        return factoryNode.getOpType() == EvalFollowedByNodeOpType.NOMAX_POOL ||
                factoryNode.getOpType() == EvalFollowedByNodeOpType.MAX_POOL;
    }

    public boolean isTrackWithMax() {
        return factoryNode.getOpType() == EvalFollowedByNodeOpType.MAX_PLAIN ||
                factoryNode.getOpType() == EvalFollowedByNodeOpType.MAX_POOL;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalFollowedByNode.class);
}
