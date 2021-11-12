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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTableFactory;

import java.util.List;

public class PollResultIndexingStrategyComposite implements PollResultIndexingStrategy {
    public final static EPTypeClass EPTYPE = new EPTypeClass(PollResultIndexingStrategyComposite.class);

    private int streamNum;
    private String[] optionalKeyedProps;
    private EPTypeClass[] optKeyCoercedTypes;
    private EventPropertyValueGetter hashGetter;
    private String[] rangeProps;
    private EPTypeClass[] optRangeCoercedTypes;
    private EventPropertyValueGetter[] rangeGetters;
    private PropertyCompositeEventTableFactory factory;

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, ExprEvaluatorContext exprEvaluatorContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }
        EventTable[] tables = factory.makeEventTables(exprEvaluatorContext, null);
        for (EventTable table : tables) {
            table.add(pollResult.toArray(new EventBean[pollResult.size()]), exprEvaluatorContext);
        }
        return tables;
    }

    public void init() {
        factory = new PropertyCompositeEventTableFactory(streamNum, optionalKeyedProps, optKeyCoercedTypes, hashGetter, null,
                rangeProps, optRangeCoercedTypes, rangeGetters);
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setOptionalKeyedProps(String[] optionalKeyedProps) {
        this.optionalKeyedProps = optionalKeyedProps;
    }

    public void setOptKeyCoercedTypes(EPTypeClass[] optKeyCoercedTypes) {
        this.optKeyCoercedTypes = optKeyCoercedTypes;
    }

    public void setHashGetter(EventPropertyValueGetter hashGetter) {
        this.hashGetter = hashGetter;
    }

    public void setRangeProps(String[] rangeProps) {
        this.rangeProps = rangeProps;
    }

    public void setOptRangeCoercedTypes(EPTypeClass[] optRangeCoercedTypes) {
        this.optRangeCoercedTypes = optRangeCoercedTypes;
    }

    public void setRangeGetters(EventPropertyValueGetter[] rangeGetters) {
        this.rangeGetters = rangeGetters;
    }

    public void setFactory(PropertyCompositeEventTableFactory factory) {
        this.factory = factory;
    }
}
