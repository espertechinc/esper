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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstFactory;
import com.espertech.esper.view.ViewParameterException;

import java.util.Locale;

/**
 * Parameters for batch views that provides common data flow parameter parsing.
 */
public class TimeBatchViewFactoryParams {

    /**
     * Keyword for force update, i.e. update if no data.
     */
    protected static final String FORCE_UPDATE_KEYWORD = "force_update";

    /**
     * Keyword for starting eager, i.e. start early.
     */
    protected static final String START_EAGER_KEYWORD = "start_eager";

    /**
     * Event type
     */
    protected EventType eventType;

    /**
     * Number of msec before batch fires (either interval or number of events).
     */
    protected ExprTimePeriodEvalDeltaConstFactory timeDeltaComputationFactory;

    /**
     * Indicate whether to output only if there is data, or to keep outputting empty batches.
     */
    protected boolean isForceUpdate;

    /**
     * Indicate whether to output only if there is data, or to keep outputting empty batches.
     */
    protected boolean isStartEager;

    /**
     * Convert keywords into isForceUpdate and isStartEager members
     *
     * @param keywords     flow control keyword string expression
     * @param errorMessage error message
     * @throws ViewParameterException if parsing failed
     */
    protected void processKeywords(Object keywords, String errorMessage) throws ViewParameterException {

        if (!(keywords instanceof String)) {
            throw new ViewParameterException(errorMessage);
        }

        String[] keyword = ((String) keywords).split(",");
        for (int i = 0; i < keyword.length; i++) {
            String keywordText = keyword[i].toLowerCase(Locale.ENGLISH).trim();
            if (keywordText.length() == 0) {
                continue;
            }
            if (keywordText.equals(FORCE_UPDATE_KEYWORD)) {
                isForceUpdate = true;
            } else if (keywordText.equals(START_EAGER_KEYWORD)) {
                isForceUpdate = true;
                isStartEager = true;
            } else {
                String keywordRange = FORCE_UPDATE_KEYWORD + "," + START_EAGER_KEYWORD;
                throw new ViewParameterException("Time-length-combination view encountered an invalid keyword '" + keywordText + "', valid control keywords are: " + keywordRange);
            }
        }
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public boolean isStartEager() {
        return isStartEager;
    }
}
