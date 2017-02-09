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
package com.espertech.esper.example.trivia;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class TriviaHelper {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static long parseGetMSec(String datestr) {
        return parse(datestr).getTime();
    }

    private static Date parse(String datestr) {
        Date date;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            date = sdf.parse(datestr);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing date '" + datestr + "' as format '" + DATE_FORMAT + "' : " + ex.getMessage(), ex);
        }
        return date;
    }

    public static String print(Object date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (date instanceof Long) {
            return sdf.format(new Date((Long) date));
        }
        if (date instanceof Date) {
            return sdf.format((Date) date);
        }
        if (date instanceof Calendar) {
            return sdf.format(((Calendar) date).getTime());
        }
        throw new IllegalArgumentException("Date format for type '" + date.getClass() + "' not possible");
    }

    public static void assertScore(EPServiceProvider engine, String scoreDesc) {

        Map<String, Integer> expected = parseScoreInput(scoreDesc);
        Map<String, Integer> received = getScores(engine);
        compare(expected, received);
    }

    private static void compare(Map<String, Integer> expected, Map<String, Integer> received) {

        if (expected.size() != received.size()) {
            throw new RuntimeException("Failed assertion: Mismatch count" + print(expected, received));
        }

        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            Integer other = received.get(entry.getKey());

            if (other == null) {
                throw new RuntimeException("Failed assertion: Not found for user '" + entry.getKey() + "' " + print(expected, received));
            }
            if (!other.equals(entry.getValue())) {
                throw new RuntimeException("Failed assertion: Difference found for user '" + entry.getKey() + "' " + print(expected, received));
            }
        }
    }

    private static String print(Map<String, Integer> expected, Map<String, Integer> received) {
        StringWriter writer = new StringWriter();
        writer.append("expected " + expected);
        writer.append(" received " + received);
        return writer.toString();
    }

    public static Map<String, Integer> getScores(EPServiceProvider engine) {
        EPStatement stmt = engine.getEPAdministrator().getStatement("Score window");
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (Iterator<EventBean> it = stmt.iterator(); it.hasNext(); ) {
            EventBean next = it.next();
            String playerId = (String) next.get("playerId");
            Integer score = (Integer) next.get("score");
            result.put(playerId, score);
        }
        return result;
    }

    private static Map<String, Integer> parseScoreInput(String scoreDesc) {
        String[] split = scoreDesc.split(",");
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < split.length; i++) {
            String[] item = split[i].split("=");
            String user = item[0];
            Integer score = Integer.parseInt(item[1]);
            result.put(user, score);
        }
        return result;
    }
}
