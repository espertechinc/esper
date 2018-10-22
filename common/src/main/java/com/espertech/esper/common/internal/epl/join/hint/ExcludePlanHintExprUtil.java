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
package com.espertech.esper.common.internal.epl.join.hint;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryDisallow;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.core.EventTypeNameUtil;

import java.util.LinkedHashMap;

public class ExcludePlanHintExprUtil {

    protected final static ObjectArrayEventType OAEXPRESSIONTYPE;

    static {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("from_streamnum", Integer.class);
        properties.put("to_streamnum", Integer.class);
        properties.put("from_streamname", String.class);
        properties.put("to_streamname", String.class);
        properties.put("opname", String.class);
        properties.put("exprs", String[].class);
        String eventTypeName = EventTypeNameUtil.getAnonymousTypeNameExcludePlanHint();
        EventTypeMetadata eventTypeMetadata = new EventTypeMetadata(eventTypeName, null, EventTypeTypeClass.EXCLUDEPLANHINTDERIVED,
                EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        OAEXPRESSIONTYPE = BaseNestableEventUtil.makeOATypeCompileTime(eventTypeMetadata, properties, null, null, null, null, new BeanEventTypeFactoryDisallow(EventBeanTypedEventFactoryCompileTime.INSTANCE), null);
    }

    public static EventBean toEvent(int fromStreamnum,
                                    int toStreamnum,
                                    String fromStreamname,
                                    String toStreamname,
                                    String opname,
                                    ExprNode[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            texts[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expressions[i]);
        }
        Object[] event = new Object[]{fromStreamnum, toStreamnum, fromStreamname, toStreamname, opname, texts};
        return new ObjectArrayEventBean(event, OAEXPRESSIONTYPE);
    }

    public static ExprForge toExpression(String hint, StatementRawInfo rawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        ExprNode expr = services.getCompilerServices().compileExpression(hint, services);
        ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.HINT, expr, OAEXPRESSIONTYPE, false, rawInfo, services);
        return validated.getForge();
    }
}
