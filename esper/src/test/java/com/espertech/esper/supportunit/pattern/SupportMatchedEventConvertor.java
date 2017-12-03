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
package com.espertech.esper.supportunit.pattern;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.pattern.MatchedEventConvertor;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapMeta;

public class SupportMatchedEventConvertor implements MatchedEventConvertor {
    public EventBean[] convert(MatchedEventMap events) {
        return new EventBean[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        return new MatchedEventMapMeta(new String[0], false);
    }
}
