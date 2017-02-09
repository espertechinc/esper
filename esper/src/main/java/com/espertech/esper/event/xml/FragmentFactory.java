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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventBean;
import org.w3c.dom.Node;

/**
 * Factory for event fragments for use with DOM getters.
 */
public interface FragmentFactory {
    /**
     * Returns a fragment for the node.
     *
     * @param result node to fragment
     * @return fragment
     */
    public EventBean getEvent(Node result);
}
