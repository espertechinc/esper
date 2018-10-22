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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.MappedEventBean;

import java.util.Arrays;

public abstract class ExprNodeAdapterMSBase extends ExprNodeAdapterBase {

    protected final EventBean[] prototypeArray;

    public ExprNodeAdapterMSBase(FilterSpecParamExprNode factory, ExprEvaluatorContext evaluatorContext, EventBean[] prototypeArray) {
        super(factory, evaluatorContext);
        this.prototypeArray = prototypeArray;
    }

    public EventBean[] getPrototypeArray() {
        return prototypeArray;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ExprNodeAdapterMSBase that = (ExprNodeAdapterMSBase) o;

        // Array-of-events comparison must consider array-tag holders
        for (int i = 0; i < prototypeArray.length; i++) {
            EventBean mine = prototypeArray[i];
            EventBean other = that.prototypeArray[i];
            if (mine == null) {
                if (other != null) {
                    return false;
                }
                continue;
            }
            if (mine.equals(other)) {
                continue;
            }
            if (mine.getEventType().getMetadata().getTypeClass() != EventTypeTypeClass.PATTERNDERIVED) {
                return false;
            }
            // these events holds array-matches
            MappedEventBean mineMapped = (MappedEventBean) mine;
            MappedEventBean otherMapped = (MappedEventBean) other;
            String propName = mineMapped.getEventType().getPropertyNames()[0];
            EventBean[] mineEvents = (EventBean[]) mineMapped.getProperties().get(propName);
            EventBean[] otherEvents = (EventBean[]) otherMapped.getProperties().get(propName);
            if (!Arrays.equals(mineEvents, otherEvents)) {
                return false;
            }
        }

        return true;
    }

    public int hashCode() {
        return super.hashCode();
    }
}
