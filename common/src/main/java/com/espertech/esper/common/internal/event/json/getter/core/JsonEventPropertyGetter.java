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
package com.espertech.esper.common.internal.event.json.getter.core;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

public interface JsonEventPropertyGetter extends EventPropertyGetterSPI {
    Object getJsonProp(Object object) throws PropertyAccessException;

    boolean getJsonExists(Object object) throws PropertyAccessException;

    Object getJsonFragment(Object object) throws PropertyAccessException;
}
