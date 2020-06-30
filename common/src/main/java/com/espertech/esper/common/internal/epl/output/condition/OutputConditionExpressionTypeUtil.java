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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;

import java.util.LinkedHashMap;

public class OutputConditionExpressionTypeUtil {
    public final static LinkedHashMap<String, Object> TYPEINFO;

    static {
        TYPEINFO = new LinkedHashMap<>();
        TYPEINFO.put("count_insert", EPTypePremade.INTEGERBOXED.getEPType());
        TYPEINFO.put("count_remove", EPTypePremade.INTEGERBOXED.getEPType());
        TYPEINFO.put("count_insert_total", EPTypePremade.INTEGERBOXED.getEPType());
        TYPEINFO.put("count_remove_total", EPTypePremade.INTEGERBOXED.getEPType());
        TYPEINFO.put("last_output_timestamp", EPTypePremade.LONGBOXED.getEPType());
    }

    public static Object[] getOAPrototype() {
        return new Object[TYPEINFO.size()];
    }

    public static void populate(Object[] builtinProperties, int totalNewEventsCount, int totalOldEventsCount,
                                int totalNewEventsSum, int totalOldEventsSum, Long lastOutputTimestamp) {
        builtinProperties[0] = totalNewEventsCount;
        builtinProperties[1] = totalOldEventsCount;
        builtinProperties[2] = totalNewEventsSum;
        builtinProperties[3] = totalOldEventsSum;
        builtinProperties[4] = lastOutputTimestamp;
    }

    public static EventType getBuiltInEventType(String moduleName, BeanEventTypeFactory beanEventTypeFactory) {
        EventTypeMetadata metadata = new EventTypeMetadata("anonymous", moduleName, EventTypeTypeClass.STREAM, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        return BaseNestableEventUtil.makeOATypeCompileTime(metadata, OutputConditionExpressionTypeUtil.TYPEINFO, null, null, null, null, beanEventTypeFactory, null);
    }
}
