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
package com.espertech.esper.common.client.configuration.runtime;

import java.io.Serializable;

/**
 * Holds match-recognize settings.
 */
public class ConfigurationRuntimeMatchRecognize implements Serializable {
    private static final long serialVersionUID = 6812580847949366921L;

    private Long maxStates;
    private boolean maxStatesPreventStart = true;

    /**
     * Returns the maximum number of states
     *
     * @return state count
     */
    public Long getMaxStates() {
        return maxStates;
    }

    /**
     * Sets the maximum number of states
     *
     * @param maxStates state count
     */
    public void setMaxStates(Long maxStates) {
        this.maxStates = maxStates;
    }

    /**
     * Returns true, the default, to indicate that if there is a maximum defined
     * it is being enforced and new states are not allowed.
     *
     * @return indicate whether enforced or not
     */
    public boolean isMaxStatesPreventStart() {
        return maxStatesPreventStart;
    }

    /**
     * Set to true, the default, to indicate that if there is a maximum defined
     * it is being enforced and new states are not allowed.
     *
     * @param maxStatesPreventStart indicate whether enforced or not
     */
    public void setMaxStatesPreventStart(boolean maxStatesPreventStart) {
        this.maxStatesPreventStart = maxStatesPreventStart;
    }
}
