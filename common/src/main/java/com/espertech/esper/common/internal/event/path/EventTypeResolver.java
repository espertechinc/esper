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
package com.espertech.esper.common.internal.event.path;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.serde.runtime.event.EventSerdeFactory;

public interface EventTypeResolver {
    String RESOLVE_METHOD = "resolve";
    String RESOLVE_PRIVATE_BEAN_METHOD = "resolvePrivateBean";
    String GETEVENTSERDEFACTORY = "getEventSerdeFactory";

    EventType resolve(EventTypeMetadata metadata);

    BeanEventType resolvePrivateBean(Class clazz, boolean publicFields);

    EventSerdeFactory getEventSerdeFactory();
}
