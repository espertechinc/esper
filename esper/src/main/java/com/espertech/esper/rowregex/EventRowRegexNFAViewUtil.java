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
import com.espertech.esper.collection.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class EventRowRegexNFAViewUtil {
    protected static EventBean[] getMultimatchArray(int[] multimatchStreamNumToVariable, RegexNFAStateEntry state, int stream) {
        if (state.getOptionalMultiMatches() == null) {
            return null;
        }
        int index = multimatchStreamNumToVariable[stream];
        MultimatchState multiMatches = state.getOptionalMultiMatches()[index];
        if (multiMatches == null) {
            return null;
        }
        return multiMatches.getShrinkEventArray();
    }

    protected static String printStates(List<RegexNFAStateEntry> states, Map<Integer, String> streamsVariables, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams, int[] multimatchStreamNumToVariable) {
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (RegexNFAStateEntry state : states) {
            buf.append(delimiter);
            buf.append(state.getState().getNodeNumNested());

            buf.append("{");
            EventBean[] eventsPerStream = state.getEventsPerStream();
            if (eventsPerStream == null) {
                buf.append("null");
            } else {
                String eventDelimiter = "";
                for (Map.Entry<Integer, String> streamVariable : streamsVariables.entrySet()) {
                    buf.append(eventDelimiter);
                    buf.append(streamVariable.getValue());
                    buf.append('=');
                    boolean single = !variableStreams.get(streamVariable.getValue()).getSecond();
                    if (single) {
                        if (eventsPerStream[streamVariable.getKey()] == null) {
                            buf.append("null");
                        } else {
                            buf.append(eventsPerStream[streamVariable.getKey()].getUnderlying());
                        }
                    } else {
                        int streamNum = state.getState().getStreamNum();
                        int index = multimatchStreamNumToVariable[streamNum];
                        if (state.getOptionalMultiMatches() == null) {
                            buf.append("null-mm");
                        } else if (state.getOptionalMultiMatches()[index] == null) {
                            buf.append("no-entry");
                        } else {
                            buf.append("{");
                            String arrayEventDelimiter = "";
                            EventBean[] multiMatch = state.getOptionalMultiMatches()[index].getBuffer();
                            int count = state.getOptionalMultiMatches()[index].getCount();
                            for (int i = 0; i < count; i++) {
                                buf.append(arrayEventDelimiter);
                                buf.append(multiMatch[i].getUnderlying());
                                arrayEventDelimiter = ", ";
                            }
                            buf.append("}");
                        }
                    }
                    eventDelimiter = ", ";
                }
            }
            buf.append("}");

            delimiter = ", ";
        }
        return buf.toString();
    }

    protected static String print(RegexNFAState[] states) {
        StringWriter writer = new StringWriter();
        PrintWriter buf = new PrintWriter(writer);
        Stack<RegexNFAState> currentStack = new Stack<RegexNFAState>();
        print(Arrays.asList(states), buf, 0, currentStack);
        return writer.toString();
    }

    protected static void print(List<RegexNFAState> states, PrintWriter writer, int indent, Stack<RegexNFAState> currentStack) {

        for (RegexNFAState state : states) {
            indent(writer, indent);
            if (currentStack.contains(state)) {
                writer.println("(self)");
            } else {
                writer.println(printState(state));

                currentStack.push(state);
                print(state.getNextStates(), writer, indent + 4, currentStack);
                currentStack.pop();
            }
        }
    }

    private static String printState(RegexNFAState state) {
        if (state instanceof RegexNFAStateEnd) {
            return "#" + state.getNodeNumNested();
        } else {
            return "#" + state.getNodeNumNested() + " " + state.getVariableName() + " s" + state.getStreamNum() + " defined as " + state;
        }
    }

    private static void indent(PrintWriter writer, int indent) {
        for (int i = 0; i < indent; i++) {
            writer.append(' ');
        }
    }
}
