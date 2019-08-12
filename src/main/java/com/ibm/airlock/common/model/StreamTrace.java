package com.ibm.airlock.common.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import org.json.JSONArray;


/**
 * @author eitan.schreiber on 09/08/2017.
 */

public class StreamTrace {

    private static final int MAX_ENTERIES_COUNT = 200;
    private int nextIndex = 0;
    private List<String> enteriesArr = Arrays.asList(new String[MAX_ENTERIES_COUNT]);
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd:HH.mm.ss");

    public StreamTrace(@Nullable JSONArray array) {
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                enteriesArr.add(array.getString(i));
            }
        }
    }

    public void getSize() {
        enteriesArr.size();
    }

    public void clearTrace() {
        enteriesArr = Arrays.asList(new String[MAX_ENTERIES_COUNT]);
        nextIndex = 0;
    }

    public void write(String traceInfo) {
        enteriesArr.set(nextIndex, traceInfo + ";;" + formatter.format(new Date()));
        if (++nextIndex >= MAX_ENTERIES_COUNT) {
            nextIndex = 0;
        }
    }

    public JSONArray toJSONArray() {
        return new JSONArray(enteriesArr);
    }

    public void write(String[] traceInfo) {
        for (String trace : traceInfo) {
            write(trace);
        }
    }

    public String[] getTraceArr() {
        return enteriesArr.toArray(new String[0]);
    }
}
