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
package com.espertech.esper.common.internal.view.util;

import com.espertech.esper.common.internal.view.core.ViewParameterException;

import java.util.Locale;

public class TimeBatchFlags {
    /**
     * Keyword for force update, i.e. update if no data.
     */
    private static final String FORCE_UPDATE_KEYWORD = "force_update";

    /**
     * Keyword for starting eager, i.e. start early.
     */
    private static final String START_EAGER_KEYWORD = "start_eager";

    private final boolean isForceUpdate;
    private final boolean isStartEager;

    public TimeBatchFlags(boolean isForceUpdate, boolean isStartEager) {
        this.isForceUpdate = isForceUpdate;
        this.isStartEager = isStartEager;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public boolean isStartEager() {
        return isStartEager;
    }

    public static TimeBatchFlags processKeywords(Object keywords, String errorMessage) throws ViewParameterException {

        boolean isForceUpdate = false;
        boolean isStartEager = false;
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
                throw new ViewParameterException("Time-batch encountered an invalid keyword '" + keywordText + "', valid control keywords are: " + keywordRange);
            }
        }
        return new TimeBatchFlags(isForceUpdate, isStartEager);
    }
}
