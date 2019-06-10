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
 * Holds pattern settings.
 */
public class ConfigurationRuntimePatterns implements Serializable {
    private static final long serialVersionUID = -7596853289989573800L;

    private Long maxSubexpressions;
    private boolean maxSubexpressionPreventStart = true;

    /**
     * Returns the maximum number of subexpressions
     *
     * @return subexpression count
     */
    public Long getMaxSubexpressions() {
        return maxSubexpressions;
    }

    /**
     * Sets the maximum number of subexpressions
     *
     * @param maxSubexpressions subexpression count
     */
    public void setMaxSubexpressions(Long maxSubexpressions) {
        this.maxSubexpressions = maxSubexpressions;
    }

    /**
     * Returns true, the default, to indicate that if there is a maximum defined
     * it is being enforced and new subexpressions are not allowed.
     *
     * @return indicate whether enforced or not
     */
    public boolean isMaxSubexpressionPreventStart() {
        return maxSubexpressionPreventStart;
    }

    /**
     * Set to true, the default, to indicate that if there is a maximum defined
     * it is being enforced and new subexpressions are not allowed.
     *
     * @param maxSubexpressionPreventStart indicate whether enforced or not
     */
    public void setMaxSubexpressionPreventStart(boolean maxSubexpressionPreventStart) {
        this.maxSubexpressionPreventStart = maxSubexpressionPreventStart;
    }
}
