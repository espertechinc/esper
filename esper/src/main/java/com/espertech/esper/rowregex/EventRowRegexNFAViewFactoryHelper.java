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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.MatchRecognizeDefineItem;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EventRowRegexNFAViewFactoryHelper {
    public static ObjectArrayBackedEventBean getDefineMultimatchBean(StatementContext statementContext,
                                                                     LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams,
                                                                     EventType parentViewType) {
        Map<String, Object> multievent = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            if (entry.getValue().getSecond()) {
                multievent.put(entry.getKey(), new EventType[]{parentViewType});
            }
        }
        EventType multimatch = statementContext.getEventAdapterService().createAnonymousObjectArrayType(
                "esper_matchrecog_internal", multievent);
        return (ObjectArrayBackedEventBean) statementContext.getEventAdapterService().adapterForTypedObjectArray(new Object[multievent.size()], multimatch);
    }

    public static StreamTypeService buildDefineStreamTypeServiceDefine(StatementContext statementContext,
                                                                       LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams,
                                                                       MatchRecognizeDefineItem defineItem,
                                                                       Map<String, Set<String>> visibilityByIdentifier,
                                                                       EventType parentViewType)
            throws ExprValidationException {
        if (!variableStreams.containsKey(defineItem.getIdentifier())) {
            throw new ExprValidationException("Variable '" + defineItem.getIdentifier() + "' does not occur in pattern");
        }

        String[] streamNamesDefine = new String[variableStreams.size() + 1];
        EventType[] typesDefine = new EventType[variableStreams.size() + 1];
        boolean[] isIStreamOnly = new boolean[variableStreams.size() + 1];
        Arrays.fill(isIStreamOnly, true);

        int streamNumDefine = variableStreams.get(defineItem.getIdentifier()).getFirst();
        streamNamesDefine[streamNumDefine] = defineItem.getIdentifier();
        typesDefine[streamNumDefine] = parentViewType;

        // add visible single-value
        Set<String> visibles = visibilityByIdentifier.get(defineItem.getIdentifier());
        boolean hasVisibleMultimatch = false;
        if (visibles != null) {
            for (String visible : visibles) {
                Pair<Integer, Boolean> def = variableStreams.get(visible);
                if (!def.getSecond()) {
                    streamNamesDefine[def.getFirst()] = visible;
                    typesDefine[def.getFirst()] = parentViewType;
                } else {
                    hasVisibleMultimatch = true;
                }
            }
        }

        // compile multi-matching event type (in last position), if any are used
        if (hasVisibleMultimatch) {
            Map<String, Object> multievent = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
                String identifier = entry.getKey();
                if (entry.getValue().getSecond()) {
                    if (visibles.contains(identifier)) {
                        multievent.put(identifier, new EventType[]{parentViewType});
                    } else {
                        multievent.put("esper_matchrecog_internal", null);
                    }
                }
            }
            EventType multimatch = statementContext.getEventAdapterService().createAnonymousObjectArrayType(
                    "esper_matchrecog_internal", multievent);
            typesDefine[typesDefine.length - 1] = multimatch;
            streamNamesDefine[streamNamesDefine.length - 1] = multimatch.getName();
        }

        return new StreamTypeServiceImpl(typesDefine, streamNamesDefine, isIStreamOnly, statementContext.getEngineURI(), false, true);
    }
}
