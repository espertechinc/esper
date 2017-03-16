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
package com.espertech.esper.example.benchmark.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A container for thread local Stats instances.
 * Upon dump, data is gathered and merged from all registered thread local instances.
 * The stat name is used as a key for a dump.
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class StatsHolder {

    public static final int[] DEFAULT_MS = new int[]{1, 5, 10, 50, 100, 250, 500, 1000}; //ms
    public static final int[] DEFAULT_NS = new int[]{5, 10, 15, 20, 25, 50, 100, 500, 1000, 2500, 5000}; //micro secs

    static {
        for (int i = 0; i < DEFAULT_NS.length; i++)
            DEFAULT_NS[i] *= 1000; //turn to ns
    }

    private final static List<Stats> STATS = Collections.synchronizedList(new ArrayList<Stats>());

    private static ThreadLocal<Stats> statsEngine = new ThreadLocal<Stats>() {
        protected synchronized Stats initialValue() {
            Stats s = new Stats("engine", "ns", DEFAULT_NS);
            STATS.add(s);
            return s;
        }
    };

    private static ThreadLocal<Stats> statsServer = new ThreadLocal<Stats>() {
        protected synchronized Stats initialValue() {
            Stats s = new Stats("server", "ns", DEFAULT_NS);
            STATS.add(s);
            return s;
        }
    };

    private static ThreadLocal<Stats> statsEndToEnd = new ThreadLocal<Stats>() {
        protected synchronized Stats initialValue() {
            Stats s = new Stats("endToEnd", "ms", DEFAULT_MS);
            STATS.add(s);
            return s;
        }
    };

    public static Stats getEngine() {
        return statsEngine.get();
    }

    public static Stats getServer() {
        return statsServer.get();
    }

    public static Stats getEndToEnd() {
        return statsEndToEnd.get();
    }

    public static void remove(Stats stats) {
        STATS.remove(stats);
    }

    public static void dump(String name) {
        Stats sum = null;
        for (Stats s : STATS) {
            if (name.equals(s.name)) {
                if (sum == null)
                    sum = Stats.createAndMergeFrom(s);
                else
                    sum.merge(s);
            }
        }
        if (sum != null)
            sum.dump();
    }

    public static void reset() {
        for (Stats s : STATS) {
            s.reset();
        }
    }
}
