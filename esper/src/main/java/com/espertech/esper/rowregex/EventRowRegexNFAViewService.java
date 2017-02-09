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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.StopCallback;

/**
 * Service interface for match recognize.
 */
public interface EventRowRegexNFAViewService extends StopCallback {
    public void init(EventBean[] newEvents);

    public RegexExprPreviousEvalStrategy getPreviousEvaluationStrategy();

    public void accept(EventRowRegexNFAViewServiceVisitor visitor);
}
