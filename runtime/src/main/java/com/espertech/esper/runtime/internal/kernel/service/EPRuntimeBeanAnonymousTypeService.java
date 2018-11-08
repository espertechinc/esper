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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryRuntime;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;

import java.util.Collections;

public class EPRuntimeBeanAnonymousTypeService {
    private final BeanEventTypeStemService stemSvc = new BeanEventTypeStemService(Collections.emptyMap(), null, PropertyResolutionStyle.CASE_SENSITIVE, AccessorStyle.JAVABEAN);
    private final BeanEventTypeFactoryPrivate factoryPrivate = new BeanEventTypeFactoryPrivate(new EventBeanTypedEventFactoryRuntime(null), EventTypeFactoryImpl.INSTANCE, stemSvc);

    public BeanEventType makeBeanEventTypeAnonymous(Class beanType) {
        EventTypeMetadata metadata = new EventTypeMetadata(beanType.getName(), null, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        BeanEventTypeStem stem = stemSvc.getCreateStem(beanType, null);
        return new BeanEventType(stem, metadata, factoryPrivate, null, null, null, null);
    }
}
