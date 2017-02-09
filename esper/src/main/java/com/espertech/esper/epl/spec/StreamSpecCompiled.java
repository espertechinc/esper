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

/**
 * Validated stream specifications generally have expression nodes that are valid and event types exist.
 */
public interface StreamSpecCompiled extends StreamSpec {
    public final static StreamSpecCompiled[] EMPTY_STREAM_ARRAY = new StreamSpecCompiled[0];
}
