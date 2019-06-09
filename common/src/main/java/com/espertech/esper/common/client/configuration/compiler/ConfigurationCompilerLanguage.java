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
 * Language settings in the runtime are for string comparisons.
 */
public class ConfigurationCompilerLanguage implements Serializable {
    private static final long serialVersionUID = -6019225386478861987L;
    private boolean sortUsingCollator;

    /**
     * Ctor.
     */
    public ConfigurationCompilerLanguage() {
        sortUsingCollator = false;
    }

    /**
     * Returns true to indicate to perform locale-independent string comparisons using Collator.
     * <p>
     * By default this setting is false, i.e. string comparisons use the compare method.
     *
     * @return indicator whether to use Collator for string comparisons
     */
    public boolean isSortUsingCollator() {
        return sortUsingCollator;
    }

    /**
     * Set to true to indicate to perform locale-independent string comparisons using Collator.
     * <p>
     * Set to false to perform string comparisons via the compare method (the default).
     *
     * @param sortUsingCollator indicator whether to use Collator for string comparisons
     */
    public void setSortUsingCollator(boolean sortUsingCollator) {
        this.sortUsingCollator = sortUsingCollator;
    }
}
