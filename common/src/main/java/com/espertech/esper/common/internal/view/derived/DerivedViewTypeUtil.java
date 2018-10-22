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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.util.LinkedHashMap;
import java.util.Map;

public class DerivedViewTypeUtil {
    public static EventType newType(String name, LinkedHashMap<String, Object> schemaMap, ViewForgeEnv env, int streamNum) {
        String outputEventTypeName = env.getStatementCompileTimeServices().getEventTypeNameGeneratorStatement().getViewDerived(name, streamNum);
        EventTypeMetadata metadata = new EventTypeMetadata(outputEventTypeName, env.getModuleName(), EventTypeTypeClass.VIEWDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        Map<String, Object> propertyTypes = EventTypeUtility.getPropertyTypesNonPrimitive(schemaMap);
        EventType resultEventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, propertyTypes, null, null, null, null, env.getBeanEventTypeFactoryProtected(), env.getEventTypeCompileTimeResolver());
        env.getEventTypeModuleCompileTimeRegistry().newType(resultEventType);
        return resultEventType;
    }
}
