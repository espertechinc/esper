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
package com.espertech.esper.example.benchmark;

import java.util.Random;

/**
 * Holds the list of symbols. Defaults to 1000
 * Use -Desper.benchmark.symbol=1000 to configure the number of symbols to use (hence the number of EPL statements)
 * <p/>
 * Each symbol is prefixed with "S" and suffixed with "A" to have all symbols have the same length
 * (f.e. S1AA S2AA ... S99A for 100 symbols)
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class Symbols {

    private static final Random RAND = new Random();
    public static final int SIZE;
    public static final int LENGTH;

    static {
        int symbolcount = Integer.parseInt(System.getProperty("esper.benchmark.symbol", "1000"));
        LENGTH = ("" + symbolcount).length();
        String[] symbols = new String[symbolcount];
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = "S" + i;
            while (symbols[i].length() < LENGTH) {
                symbols[i] += "A";
            }
        }

        SYMBOLS = symbols;
        SIZE = LENGTH * Character.SIZE;
    }

    public static final String[] SYMBOLS;

    public static double nextPrice(double theBase) {
        int percentVar = RAND.nextInt(9) + 1;
        int trend = RAND.nextInt(3);
        double result = theBase;
        switch (trend) {
            case 0:
                result *= 1.0D - (double) percentVar * 0.01D;
                break;
            case 2:
                result *= 1.0D + (double) percentVar * 0.01D;
                break;
        }
        return result;
    }

    public static int nextVolume(int max) {
        return RAND.nextInt(max - 1) + 1;
    }
}
