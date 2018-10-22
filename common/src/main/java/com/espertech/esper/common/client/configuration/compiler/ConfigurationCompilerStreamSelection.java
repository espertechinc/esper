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

import com.espertech.esper.common.client.soda.StreamSelector;

import java.io.Serializable;

/**
 * Holds default settings for stream selection in the select-clause.
 */
public class ConfigurationCompilerStreamSelection implements Serializable {
    private StreamSelector defaultStreamSelector;
    private static final long serialVersionUID = -7943748323859161674L;

    /**
     * Ctor - sets up defaults.
     */
    protected ConfigurationCompilerStreamSelection() {
        defaultStreamSelector = StreamSelector.ISTREAM_ONLY;
    }

    /**
     * Returns the default stream selector.
     * <p>
     * Statements that select data from streams and that do not use one of the explicit stream
     * selection keywords (istream/rstream/irstream), by default,
     * generate selection results for the insert stream only, and not for the remove stream.
     * <p>
     * This setting can be used to change the default behavior: Use the RSTREAM_ISTREAM_BOTH
     * value to have your statements generate both insert and remove stream results
     * without the use of the "irstream" keyword in the select clause.
     *
     * @return default stream selector, which is ISTREAM_ONLY unless changed
     */
    public StreamSelector getDefaultStreamSelector() {
        return defaultStreamSelector;
    }

    /**
     * Sets the default stream selector.
     * <p>
     * Statements that select data from streams and that do not use one of the explicit stream
     * selection keywords (istream/rstream/irstream), by default,
     * generate selection results for the insert stream only, and not for the remove stream.
     * <p>
     * This setting can be used to change the default behavior: Use the RSTREAM_ISTREAM_BOTH
     * value to have your statements generate both insert and remove stream results
     * without the use of the "irstream" keyword in the select clause.
     *
     * @param defaultStreamSelector default stream selector
     */
    public void setDefaultStreamSelector(StreamSelector defaultStreamSelector) {
        this.defaultStreamSelector = defaultStreamSelector;
    }
}
