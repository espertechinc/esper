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

import com.espertech.esper.example.benchmark.Symbols;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * The main Esper Server thread listens on the given port.
 * It bootstrap an ESP/CEP engine (defaults to Esper) and registers EPL statement(s) into it based
 * on the given -mode argument.
 * Statements are read from an statements.properties file in the classpath
 * If statements contains '$' the '$' is replaced by a symbol string, so as to register one statement per symbol.
 * <p/>
 * Based on -queue, the server implements a direct handoff to the ESP/CEP engine, or uses a Syncrhonous queue
 * (somewhat an indirect direct handoff), or uses a FIFO queue where each events is put/take one by one from the queue.
 * Usually with few clients sending a lot of events, use the direct handoff, else consider using queues. Consumer thread
 * can be configured using -thread (it will range up to #processor x #thread).
 * When queues is full, overload policy triggers execution on the caller side.
 * <p/>
 * To simulate an ESP/CEP listener work, use -sleep.
 * <p/>
 * Use -stat to control how often percentile stats are displayed. At each display stats are reset.
 * <p/>
 * If you use -rate nxM (n threads, M event/s), the server will simulate the load for a standalone simulation without
 * any remote client(s).
 * <p/>
 * By default the benchmark registers a subscriber to the statement(s). Use -Desper.benchmark.ul to use
 * an UpdateListener instead. Note that the subscriber contains suitable update(..) methods for the default
 * proposed statement in the statements.properties files but might not be suitable if you change statements due
 * to the strong binding with statement results.
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class Server extends Thread {

    private int port;
    private int threadCore;
    private int queueMax;
    private int sleepListenerMillis;
    private int statSec;
    private int simulationRate;
    private int simulationThread;
    private String mode;

    public static final int DEFAULT_PORT = 6789;
    public static final int DEFAULT_THREADCORE = Runtime.getRuntime().availableProcessors();
    public static final int DEFAULT_QUEUEMAX = -1;
    public static final int DEFAULT_SLEEP = 0;
    public static final int DEFAULT_SIMULATION_RATE = -1; //-1: no simulation
    public static final int DEFAULT_SIMULATION_THREAD = -1; //-1: no simulation
    public static final int DEFAULT_STAT = 5;
    public static final String DEFAULT_MODE = "NOOP";
    public static final Properties MODES = new Properties();

    private ThreadPoolExecutor executor; //can be null

    private CEPProvider.ICEPProvider cepProvider;

    public Server(String mode, int port, int threads, int queueMax, int sleep, final int statSec, int simulationThread, final int simulationRate) {
        super("EsperServer-main");
        this.mode = mode;
        this.port = port;
        this.threadCore = threads;
        this.queueMax = queueMax;
        this.sleepListenerMillis = sleep;
        this.statSec = statSec;
        this.simulationThread = simulationThread;
        this.simulationRate = simulationRate;

        // turn on stat dump
        Timer t = new Timer("EsperServer-stats", true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                StatsHolder.dump("engine");
                StatsHolder.dump("server");
                StatsHolder.dump("endToEnd");
                StatsHolder.reset();
                if (simulationRate <= 0) {
                    ClientConnection.dumpStats(statSec);
                } else {
                    SimulateClientConnection.dumpStats(statSec);
                }
            }
        }, 0L, statSec * 1000);
    }

    public void setCEPProvider(CEPProvider.ICEPProvider cepProvider) {
        this.cepProvider = cepProvider;
    }

    public synchronized void start() {
        // register ESP/CEP engine
        cepProvider = CEPProvider.getCEPProvider();
        cepProvider.init(sleepListenerMillis);

        // register statements
        String suffix = Server.MODES.getProperty("_SUFFIX");
        if ("NOOP".equals(mode)) {
            // no action
        } else {
            String stmtString = Server.MODES.getProperty(mode) + " " + suffix;
            System.out.println("Using " + mode + " : " + stmtString);

            if (Server.MODES.getProperty(mode).indexOf('$') < 0) {
                cepProvider.registerStatement(stmtString, mode);
                System.out.println("\nStatements registered # 1 only");
            } else {
                // create a stmt for each symbol
                for (int i = 0; i < Symbols.SYMBOLS.length; i++) {
                    if (i % 100 == 0) System.out.print(".");
                    String ticker = Symbols.SYMBOLS[i];
                    cepProvider.registerStatement(stmtString.replaceAll("\\$", ticker), mode + "-" + ticker);
                }
                System.out.println("\nStatements registered # " + Symbols.SYMBOLS.length);
            }
        }

        // start thread pool if any
        if (queueMax < 0) {
            executor = null;
            System.out.println("Using direct handoff, cpu#" + Runtime.getRuntime().availableProcessors());
        } else {
            // executor
            System.out.println("Using ThreadPoolExecutor, cpu#" + Runtime.getRuntime().availableProcessors() + ", threadCore#" + threadCore + " queue#" + queueMax);
            BlockingQueue<Runnable> queue;
            if (queueMax == 0) {
                queue = new SynchronousQueue<Runnable>(true); //enforce fairness
            } else {
                queue = new LinkedBlockingQueue<Runnable>(queueMax);
            }

            executor = new ThreadPoolExecutor(
                    threadCore,
                    Runtime.getRuntime().availableProcessors() * threadCore,
                    10, TimeUnit.SECONDS,
                    queue,
                    new ThreadFactory() {
                        long count = 0;

                        public Thread newThread(Runnable r) {
                            System.out.println("Create EsperServer thread " + (count + 1));
                            return new Thread(r, "EsperServer-" + count++);
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy() {
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            super.rejectedExecution(r, e);
                        }
                    }
            );
            executor.prestartAllCoreThreads();
        }

        super.start();
    }

    public void run() {
        if (simulationRate <= 0) {
            runServer();
        } else {
            runSimulation();
        }
    }

    public void runServer() {
        try {
            System.out.println((new StringBuilder("Server accepting connections on port ")).append(port).append(".").toString());
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            do {
                SocketChannel socketChannel = serverSocketChannel.accept();
                System.out.println("Client connected to server.");
                (new ClientConnection(socketChannel, executor, cepProvider, statSec)).start();
            } while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runSimulation() {
        System.out.println("Server in sumulation mode with event/s "
                + simulationThread + " x " + simulationRate
                + " = " + simulationThread * simulationRate
        );
        SimulateClientConnection[] sims = new SimulateClientConnection[simulationThread];
        for (int i = 0; i < sims.length; i++) {
            sims[i] = new SimulateClientConnection(simulationRate, executor, cepProvider, statSec);
            sims[i].start();
        }

        try {
            for (SimulateClientConnection sim : sims) {
                sim.join();
            }
        } catch (InterruptedException e) {
        }
    }

    public static void main(String[] argv) throws IOException {
        // load modes
        MODES.load(Server.class.getClassLoader().getResourceAsStream("statements.properties"));
        MODES.put("NOOP", "");

        int port = DEFAULT_PORT;
        int threadCore = DEFAULT_THREADCORE;
        int queueMax = DEFAULT_QUEUEMAX;
        int sleep = DEFAULT_SLEEP;
        int simulationRate = DEFAULT_SIMULATION_RATE;
        int simulationThread = DEFAULT_SIMULATION_THREAD;
        String mode = DEFAULT_MODE;
        int stats = DEFAULT_STAT;
        for (int i = 0; i < argv.length; i++)
            if ("-port".equals(argv[i])) {
                i++;
                port = Integer.parseInt(argv[i]);
            } else if ("-thread".equals(argv[i])) {
                i++;
                threadCore = Integer.parseInt(argv[i]);
            } else if ("-queue".equals(argv[i])) {
                i++;
                queueMax = Integer.parseInt(argv[i]);
            } else if ("-sleep".equals(argv[i])) {
                i++;
                sleep = Integer.parseInt(argv[i]);
            } else if ("-stat".equals(argv[i])) {
                i++;
                stats = Integer.parseInt(argv[i]);
            } else if ("-mode".equals(argv[i])) {
                i++;
                mode = argv[i];
                if (MODES.getProperty(mode) == null) {
                    System.err.println("Unknown mode");
                    printUsage();
                }
            } else if ("-rate".equals(argv[i])) {
                i++;
                int xIndex = argv[i].indexOf('x');
                simulationThread = Integer.parseInt(argv[i].substring(0, xIndex));
                simulationRate = Integer.parseInt(argv[i].substring(xIndex + 1));
            } else {
                printUsage();
            }

        Server bs = new Server(mode, port, threadCore, queueMax, sleep, stats, simulationThread, simulationRate);
        bs.start();
        try {
            bs.join();
        } catch (InterruptedException e) {
        }
    }

    private static void printUsage() {
        System.err.println("usage: com.espertech.esper.example.benchmark.server.Server <-port #> <-thread #> <-queue #> <-sleep #> <-stat #> <-rate #x#> <-mode xyz>");
        System.err.println("defaults:");
        System.err.println("  -port:    " + DEFAULT_PORT);
        System.err.println("  -thread:  " + DEFAULT_THREADCORE);
        System.err.println("  -queue:   " + DEFAULT_QUEUEMAX + "(-1: no executor, 0: SynchronousQueue, n: LinkedBlockingQueue");
        System.err.println("  -sleep:   " + DEFAULT_SLEEP + "(no sleep)");
        System.err.println("  -stat:   " + DEFAULT_STAT + "(s)");
        System.err.println("  -rate:    " + DEFAULT_SIMULATION_RATE + "(no standalone simulation, else <n>x<evt/s> such as 2x1000)");
        System.err.println("  -mode:    " + "(default " + DEFAULT_MODE + ", choose from " + MODES.keySet().toString() + ")");
        System.err.println("Modes are read from statements.properties in the classpath");
        System.exit(1);
    }

}
