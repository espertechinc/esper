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
package com.espertech.esper.common.client.util;

import java.util.List;

/**
 * Interface for exceptions that have a line items
 */
public interface ExceptionWithLineItems {
    /**
     * Returns the line items
     * @return items
     */
    List<? extends ExceptionLineItem> getItems();
}
