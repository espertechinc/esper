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
package com.espertech.esper.example.benchmark.client;

/**
 * A client that sends MarketData information over a TCP socket to a remote server.
 * MarketData packets are sent using NIO
 * <p>
 * Run with no args to see available options
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 * @see MarketClient
 */
public class Client {

    public static final int MINIMUM_RATE = 1000;
    public static final int DEFAULT_PORT = 6789;
    public static final int DEFAULT_RATE = 4000;
    public static final String DEFAULT_HOST = "localhost";

    String host;
    int port;
    int rate;

    public Client(String host, int port, int rate) {
        this.host = host;
        this.port = port;
        this.rate = rate;
    }

    public static void main(String[] argv) {
        int rate = Math.max(DEFAULT_RATE, MINIMUM_RATE);
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;
        for (int i = 0; i < argv.length; i++)
            if ("-rate".equals(argv[i])) {
                i++;
                rate = Integer.parseInt(argv[i]);
                if (rate < MINIMUM_RATE) {
                    System.err.println("[WARNING] Minimum rate is " + MINIMUM_RATE);
                    rate = MINIMUM_RATE;
                }
            } else if ("-port".equals(argv[i])) {
                i++;
                port = Integer.parseInt(argv[i]);
            } else if ("-host".equals(argv[i])) {
                i++;
                host = argv[i];
            } else {
                printUsage();
            }

        Client client = new Client(host, port, rate);
        MarketClient ms = new MarketClient(client);
        ms.start();
        try {
            ms.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.err.println("com.espertech.esper.example.benchmark.client.Client <-host hostname> <-port #> <-rate #>");
        System.err.println("defaults:");
        System.err.println("    Rate: " + DEFAULT_RATE + " msg/s");
        System.err.println("    Host: " + DEFAULT_HOST);
        System.err.println("    Port: " + DEFAULT_PORT);
        System.exit(1);
    }
}
