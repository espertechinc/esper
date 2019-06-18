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
package com.espertech.esper.common.client.json.util;

import com.espertech.esper.common.client.EventSender;

/**
 * Event sender for JSON documents and pre-parsed event objects.
 * <p>
 *     Allows parsing a JSON document returning the event object.
 * </p>
 */
public interface EventSenderJson extends EventSender {
    /**
     *
     * @param json to parse
     * @return event object
     * @throws com.espertech.esper.common.client.EPException when parsing the document failed
     */
    Object parse(String json);
}
