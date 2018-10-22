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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public abstract class JoinSetComposerPrototypeBase implements JoinSetComposerPrototype {

    protected boolean isOuterJoins;
    protected EventType[] streamTypes;
    protected ExprEvaluator postJoinFilterEvaluator;

    public void setOuterJoins(boolean outerJoins) {
        isOuterJoins = outerJoins;
    }

    public void setStreamTypes(EventType[] streamTypes) {
        this.streamTypes = streamTypes;
    }

    public void setPostJoinFilterEvaluator(ExprEvaluator postJoinFilterEvaluator) {
        this.postJoinFilterEvaluator = postJoinFilterEvaluator;
    }
}
