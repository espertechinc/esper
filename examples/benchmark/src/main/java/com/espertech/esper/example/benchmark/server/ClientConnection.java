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

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The ClientConnection handles unmarshalling from the connected client socket and delegates the event to
 * the underlying ESP/CEP engine by using/or not the executor policy.
 * Each ClientConnection manages a throughput statistic (evt/10s) over a 10s batched window
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 * @See Server
 */
public class ClientConnection extends Thread {

    final static Map<Integer, ClientConnection> CLIENTCONNECTIONS = Collections.synchronizedMap(new HashMap<Integer, ClientConnection>());

    public static void dumpStats(int statSec) {
        long totalCount = 0;
        int cnx = 0;
        ClientConnection any = null;
        for (ClientConnection m : CLIENTCONNECTIONS.values()) {
            cnx++;
            totalCount += m.countForStatSecLast;
            any = m;
        }
        if (any != null) {
            System.out.printf("Throughput %.0f (active %d pending %d cnx %d)\n",
                    (float) totalCount / statSec,
                    any.executor == null ? 0 : any.executor.getCorePoolSize(),
                    any.executor == null ? 0 : any.executor.getQueue().size(),
                    cnx
            );
        }
    }

    private SocketChannel socketChannel;
    private CEPProvider.ICEPProvider cepProvider;
    private ThreadPoolExecutor executor; //this guy is shared
    private final int statSec;
    private long countForStatSec = 0;
    private long countForStatSecLast = 0;
    private long lastThroughputTick = System.currentTimeMillis();
    private int myID;
    private static int id = 0;

    public ClientConnection(SocketChannel socketChannel, ThreadPoolExecutor executor, CEPProvider.ICEPProvider cepProvider, int statSec) {
        super("EsperServer-cnx-" + id++);
        this.socketChannel = socketChannel;
        this.executor = executor;
        this.cepProvider = cepProvider;
        this.statSec = statSec;
        myID = id - 1;

        CLIENTCONNECTIONS.put(myID, this);
    }

    public void run() {
        try {
            ByteBuffer packet = ByteBuffer.allocateDirect(MarketData.SIZE / 8);
            do {
                if (socketChannel.read(packet) < 0) {
                    System.err.println("Error receiving data from client (got null). Did client disconnect?");
                    break;
                }
                if (packet.hasRemaining()) {
                    // no action
                } else {
                    packet.flip();
                    final MarketData theEvent = MarketData.fromByteBuffer(packet);
                    if (executor == null) {
                        long ns = System.nanoTime();
                        cepProvider.sendEvent(theEvent);
                        long nsDone = System.nanoTime();
                        long msDone = System.currentTimeMillis();
                        StatsHolder.getEngine().update(nsDone - ns);
                        StatsHolder.getEndToEnd().update(msDone - theEvent.getTime());
                    } else {
                        executor.execute(new Runnable() {
                            public void run() {
                                long ns = System.nanoTime();
                                cepProvider.sendEvent(theEvent);
                                long nsDone = System.nanoTime();
                                long msDone = System.currentTimeMillis();
                                StatsHolder.getEngine().update(nsDone - ns);
                                StatsHolder.getServer().update(nsDone - theEvent.getInTime());
                                StatsHolder.getEndToEnd().update(msDone - theEvent.getTime());
                            }
                        });
                    }
                    //stats
                    countForStatSec++;
                    if (System.currentTimeMillis() - lastThroughputTick > statSec * 1E3) {
                        countForStatSecLast = countForStatSec;
                        countForStatSec = 0;
                        lastThroughputTick = System.currentTimeMillis();
                    }
                    packet.clear();
                }
            } while (true);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Error receiving data from client. Did client disconnect?");
        } finally {
            CLIENTCONNECTIONS.remove(myID);
            StatsHolder.remove(StatsHolder.getEngine());
            StatsHolder.remove(StatsHolder.getServer());
            StatsHolder.remove(StatsHolder.getEndToEnd());
        }
    }
}
