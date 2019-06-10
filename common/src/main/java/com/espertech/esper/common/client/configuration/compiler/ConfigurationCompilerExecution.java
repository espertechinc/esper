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
package com.espertech.esper.common.client.configuration.compiler;

import java.io.Serializable;

/**
 * Holds execution-related settings.
 */
public class ConfigurationCompilerExecution implements Serializable {
    private static final long serialVersionUID = 7211697789154513169L;

    private int filterServiceMaxFilterWidth = 16;
    private boolean enabledDeclaredExprValueCache = true;

    /**
     * Returns the maximum width for breaking up "or" expression in filters to
     * subexpressions for reverse indexing.
     *
     * @return max filter width
     */
    public int getFilterServiceMaxFilterWidth() {
        return filterServiceMaxFilterWidth;
    }

    /**
     * Sets the maximum width for breaking up "or" expression in filters to
     * subexpressions for reverse indexing.
     *
     * @param filterServiceMaxFilterWidth max filter width
     */
    public void setFilterServiceMaxFilterWidth(int filterServiceMaxFilterWidth) {
        this.filterServiceMaxFilterWidth = filterServiceMaxFilterWidth;
    }

    /**
     * Returns indicator whether declared-expression-value-cache is enabled (true by default)
     *
     * @return indicator
     */
    public boolean isEnabledDeclaredExprValueCache() {
        return enabledDeclaredExprValueCache;
    }

    /**
     * Sets indicator whether declared-expression-value-cache is enabled (true by default)
     *
     * @param enabledDeclaredExprValueCache indicator
     */
    public void setEnabledDeclaredExprValueCache(boolean enabledDeclaredExprValueCache) {
        this.enabledDeclaredExprValueCache = enabledDeclaredExprValueCache;
    }
}
