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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The actual event.
 * The time property (ms) is the send time from the client sender, and can be used for end to end latency providing client(s)
 * and server OS clocks are in sync.
 * The inTime property is the unmarshal (local) time (ns).
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class MarketData {

    public final static int SIZE = Symbols.SIZE + Double.SIZE + Integer.SIZE + Long.SIZE;

    static {
        System.out.println("MarketData event = " + SIZE + " bit = " + SIZE / 8 + " bytes");
        System.out.println("  100 Mbit/s <==> " + (int) (100 * 1024 * 1024 / SIZE / 1000) + "k evt/s");
        System.out.println("    1 Gbit/s <==> " + (int) (1024 * 1024 * 1024 / SIZE / 1000) + "k evt/s");
    }

    private String ticker;
    private double price;
    private int volume;

    private long time; //ms
    private final long inTime;

    public MarketData(String ticker, double price, int volume) {
        this();
        this.ticker = ticker;
        this.price = price;
        this.volume = volume;
    }

    private MarketData() {
        this.inTime = System.nanoTime();
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public long getTime() {
        return time;
    }

    private void setTime(long time) {
        this.time = time;
    }

    public long getInTime() {
        return inTime;
    }

    public void toByteBuffer(ByteBuffer b) {
        //symbol
        CharBuffer cb = b.asCharBuffer();
        cb.put(ticker); //we know ticker is a fixed length string
        b.position(b.position() + cb.position() * 2);
        //price, volume
        b.putDouble(price);
        b.putInt(volume);
        // current time ms for end to end latency
        b.putLong(System.currentTimeMillis());
    }

    public static MarketData fromByteBuffer(ByteBuffer byteBuffer) {
        MarketData md = new MarketData();
        //symbol
        char[] ticker = new char[Symbols.LENGTH];
        CharBuffer cb = byteBuffer.asCharBuffer();
        cb.get(ticker);
        md.setTicker(String.valueOf(ticker));
        //price, volume
        byteBuffer.position(byteBuffer.position() + cb.position() * 2);
        md.setPrice(byteBuffer.getDouble());
        md.setVolume(byteBuffer.getInt());
        // time
        md.setTime(byteBuffer.getLong());
        return md;
    }

    public String toString() {
        return ticker + " : " + time + " : " + price + " : " + volume;
    }

    public Object clone() throws CloneNotSupportedException {
        return new MarketData(ticker, price, volume);
    }
}


