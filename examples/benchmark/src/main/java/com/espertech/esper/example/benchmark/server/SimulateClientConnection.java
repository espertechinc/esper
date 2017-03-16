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

import com.espertech.esper.example.benchmark.MarketData;
import com.espertech.esper.example.benchmark.Symbols;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A thread started by the Server when running in simulation mode.
 * It acts as ClientConnection
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class SimulateClientConnection extends Thread {

    final static Map<Integer, SimulateClientConnection> CLIENT_CONNECTIONS = Collections.synchronizedMap(new HashMap<Integer, SimulateClientConnection>());

    public static void dumpStats(int statSec) {
        long totalCount = 0;
        int cnx = 0;
        SimulateClientConnection any = null;
        for (SimulateClientConnection m : CLIENT_CONNECTIONS.values()) {
            cnx++;
            totalCount += m.countLast10sLast;
            any = m;
        }
        if (any != null) {
            System.out.printf("Throughput %.0f (active %d pending %d)\n",
                    (float) totalCount / statSec,
                    any.executor == null ? 0 : any.executor.getCorePoolSize(),
                    any.executor == null ? 0 : any.executor.getQueue().size()
            );
        }
    }

    private int simulationRate;
    private CEPProvider.ICEPProvider cepProvider;
    private ThreadPoolExecutor executor;
    private final int statSec;
    private long countLast10sLast = 0;
    private long countLast10s = 0;
    private long lastThroughputTick = System.currentTimeMillis();
    private int myID;
    private static int id = 0;

    public SimulateClientConnection(int simulationRate, ThreadPoolExecutor executor, CEPProvider.ICEPProvider cepProvider, int statSec) {
        super("EsperServer-cnx-" + id++);
        this.simulationRate = simulationRate;
        this.executor = executor;
        this.cepProvider = cepProvider;
        this.statSec = statSec;
        myID = id - 1;

        // simulationRate event / s
        // 10ms ~ simulationRate / 1E2
        CLIENT_CONNECTIONS.put(myID, this);

    }

    public void run() {
        System.out.println("Event per    s = " + simulationRate);
        int eventPer10Millis = (int) (simulationRate / 100);
        System.out.println("Event per 10ms = " + Math.max(eventPer10Millis, 1));
        final MarketData[] market = new MarketData[Symbols.SYMBOLS.length];
        for (int i = 0; i < market.length; i++) {
            market[i] = new MarketData(Symbols.SYMBOLS[i], Symbols.nextPrice(10), Symbols.nextVolume(10));
        }

        try {
            int tickerIndex = 0;
            do {
                long ms = System.currentTimeMillis();
                for (int i = 0; i < eventPer10Millis; i++) {
                    tickerIndex = tickerIndex % Symbols.SYMBOLS.length;
                    final MarketData theEvent = market[tickerIndex++];
                    //note the cloning here, although we don't change volume or price
                    final MarketData simulatedEvent = (MarketData) theEvent.clone();
                    if (executor == null) {
                        long ns = System.nanoTime();
                        cepProvider.sendEvent(simulatedEvent);
                        StatsHolder.getEngine().update(System.nanoTime() - ns);
                    } else {
                        executor.execute(new Runnable() {
                            public void run() {
                                long ns = System.nanoTime();
                                cepProvider.sendEvent(simulatedEvent);
                                long nsDone = System.nanoTime();
                                long msDone = System.currentTimeMillis();
                                StatsHolder.getEngine().update(nsDone - ns);
                                StatsHolder.getServer().update(nsDone - simulatedEvent.getInTime());
                                StatsHolder.getEndToEnd().update(msDone - simulatedEvent.getTime());
                            }
                        });
                    }
                    //stats
                    countLast10s++;
                }
                if (System.currentTimeMillis() - lastThroughputTick > statSec * 1E3) {
                    //System.out.println("Avg["+myID+"] " + countLast10s/10 + " active " + executor.getPoolSize() + " pending " + executor.getQueue().size());
                    countLast10sLast = countLast10s;
                    countLast10s = 0;
                    lastThroughputTick = System.currentTimeMillis();
                }
                // going to fast compared to target rate
                if (System.currentTimeMillis() - ms < 10) {
                    Thread.sleep(Math.max(1, 10 - (System.currentTimeMillis() - ms)));
                }
            } while (true);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Error receiving data from market. Did market disconnect?");
        } finally {
            CLIENT_CONNECTIONS.remove(myID);
        }
    }
}
