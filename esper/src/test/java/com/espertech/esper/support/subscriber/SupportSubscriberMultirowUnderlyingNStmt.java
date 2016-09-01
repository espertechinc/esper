/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.subscriber;

import com.espertech.esper.support.bean.SupportBean;

public class SupportSubscriberMultirowUnderlyingNStmt extends SupportSubscriberMultirowUnderlyingBase
{
    public SupportSubscriberMultirowUnderlyingNStmt() {
        super(false);
    }

    public void update(SupportBean[] newEvents, SupportBean[] oldEvents)
    {
        addIndication(newEvents, oldEvents);
    }
}
