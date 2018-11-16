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
package com.espertech.esper.example.stockticker;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPEventService;
import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for monitoring a EPRuntime instance.
 */
public class StockTickerEPLUtil {
    private static final Logger log = LoggerFactory.getLogger(StockTickerEPLUtil.class);

    public static EPCompiled compileEPL(Configuration configuration) {
        // Allocate a new partition for each new PriceLimit event.
        // Terminate any old partition for PriceLimit events with the same symbol and user id.
        String eplContextPerLimit = "create context PriceLimitContext " +
            "initiated by PriceLimit as pl " +
            "terminated by PriceLimit(userId=pl.userId and symbol=pl.symbol);\n";

        // Use first stock tick event to compute an upper and lower limit based on the current price
        String eplComputeUpper = "context PriceLimitContext " +
            "insert into ObservedLimit " +
            "select context.pl.userId as userId, context.pl.symbol as symbol, price as reference, " +
            " price * (1 - (context.pl.limitPct/100d)) as lowerLimit, " +
            " price * (1 + (context.pl.limitPct/100d)) as upperLimit from StockTick(symbol = context.pl.symbol)#firstevent;\n";

        // React when a stock tick comes up that have a price that is less or greater than the observed limit
        String eplAlert = "@name('alert') context PriceLimitContext " +
            "select context.pl as priceLimit, * from pattern [" +
            " observed=ObservedLimit(userId = context.pl.userId and symbol = context.pl.symbol)" +
            " -> every tick=StockTick(symbol = context.pl.symbol and (price < observed.lowerLimit or price > observed.upperLimit))];\n";

        // Compile EPL
        log.info("Compiling EPL");
        EPCompiled compiled;
        try {
            compiled = EPCompilerProvider.getCompiler().compile(eplContextPerLimit + eplComputeUpper + eplAlert, new CompilerArguments(configuration));
        } catch (EPCompileException ex) {
            throw new RuntimeException(ex);
        }

        return compiled;
    }

    public static Configuration getConfiguration() {
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(PriceLimit.class);
        configuration.getCommon().addEventType(StockTick.class);
        return configuration;
    }

    public static boolean awaitCompletion(EPEventService epRuntime,
                                          int numEventsExpected,
                                          int numSecAwait,
                                          int numSecThreadSleep,
                                          int numSecThreadReport) {
        log.info(".awaitCompletion Waiting for completion, expecting " + numEventsExpected +
            " events within " + numSecAwait + " sec");

        int secondsWaitTotal = numSecAwait;
        long lastNumEventsProcessed = 0;
        int secondsUntilReport = 0;

        long startTimeMSec = System.currentTimeMillis();
        long endTimeMSec = 0;

        while (secondsWaitTotal > 0) {
            try {
                Thread.sleep(numSecThreadSleep * 1000);
            } catch (InterruptedException ex) {
            }

            secondsWaitTotal -= numSecThreadSleep;
            secondsUntilReport += numSecThreadSleep;
            long currNumEventsProcessed = epRuntime.getNumEventsEvaluated();

            if (secondsUntilReport > numSecThreadReport) {
                long numPerSec = (currNumEventsProcessed - lastNumEventsProcessed) / numSecThreadReport;
                log.info(".awaitCompletion received=" + epRuntime.getNumEventsEvaluated() +
                    "  processed=" + currNumEventsProcessed +
                    "  perSec=" + numPerSec);
                lastNumEventsProcessed = currNumEventsProcessed;
                secondsUntilReport = 0;
            }

            // Completed loop if the total event count has been reached
            if (epRuntime.getNumEventsEvaluated() == numEventsExpected) {
                endTimeMSec = System.currentTimeMillis();
                break;
            }
        }

        if (endTimeMSec == 0) {
            log.info(".awaitCompletion Not completed within " + numSecAwait + " seconds");
            return false;
        }

        long totalUnitsProcessed = epRuntime.getNumEventsEvaluated();
        long deltaTimeSec = (endTimeMSec - startTimeMSec) / 1000;

        long numPerSec = 0;
        if (deltaTimeSec > 0) {
            numPerSec = totalUnitsProcessed / deltaTimeSec;
        } else {
            numPerSec = -1;
        }

        log.info(".awaitCompletion Completed, sec=" + deltaTimeSec + "  avgPerSec=" + numPerSec);

        long numReceived = epRuntime.getNumEventsEvaluated();
        long numReceivedPerSec = 0;
        if (deltaTimeSec > 0) {
            numReceivedPerSec = numReceived / deltaTimeSec;
        } else {
            numReceivedPerSec = -1;
        }

        log.info(".awaitCompletion Runtime reports, numReceived=" + numReceived +
            "  numProcessed=" + epRuntime.getNumEventsEvaluated() +
            "  perSec=" + numReceivedPerSec
        );

        return true;
    }

    public static void deploy(EPRuntime runtime, EPCompiled compiled) {
        try {
            runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("stockticker"));
        } catch (EPDeployException ex) {
            throw new RuntimeException(ex);
        }
    }
}
