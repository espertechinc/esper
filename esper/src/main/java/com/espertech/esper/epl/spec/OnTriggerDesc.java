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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

/**
 * Specification for on-trigger statements.
 */
public abstract class OnTriggerDesc implements Serializable {
    private OnTriggerType onTriggerType;
    private static final long serialVersionUID = -70427959082206739L;

    /**
     * Ctor.
     *
     * @param onTriggerType the type of on-trigger
     */
    public OnTriggerDesc(OnTriggerType onTriggerType) {
        this.onTriggerType = onTriggerType;
    }

    /**
     * Returns the type of the on-trigger statement.
     *
     * @return trigger type
     */
    public OnTriggerType getOnTriggerType() {
        return onTriggerType;
    }
}
