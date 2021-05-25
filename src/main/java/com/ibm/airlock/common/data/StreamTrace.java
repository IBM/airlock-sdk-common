package com.ibm.airlock.common.data;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;


/**
 * @author eitan.schreiber on 09/08/2017.
 */

public class StreamTrace {

    private static final int MAX_ENTRIES_COUNT = 200;
    private int nextIndex = 0;
    //noinspection unchecked
    private List<String> entriesArr = Arrays.asList(new String[MAX_ENTRIES_COUNT]);
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd:HH.mm.ss");

    public StreamTrace(@Nullable JSONArray array) {
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                entriesArr.add(array.getString(i));
            }
        }
    }

    public void getSize() {
        entriesArr.size();
    }

    public void clearTrace() {
        //noinspection unchecked
        entriesArr = Arrays.asList(new String[MAX_ENTRIES_COUNT]);
        nextIndex = 0;
    }

    public synchronized void write(String traceInfo) {
        entriesArr.set(nextIndex, traceInfo + ";;" + formatter.format(new Date()));
        if (++nextIndex >= MAX_ENTRIES_COUNT) {
            nextIndex = 0;
        }
    }

    public JSONArray toJSONArray() {
        return new JSONArray(entriesArr);
    }

    public void write(String[] traceInfo) {
        for (String trace : traceInfo) {
            write(trace);
        }
    }

    public String[] getTraceArr() {
        return entriesArr.toArray(new String[entriesArr.size()]);
    }
}
