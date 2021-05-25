package com.ibm.airlock.common.streams;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Created by eitan.schreiber on 12/07/2017.
 * <p>
 * Note : all methods that iterate over streams should be synchronized otherwise it can cause ConcurrentModificationException
 */

public class StreamsManager {

    private static final String FREE_SIZE_TITLE = "\"freeSize\"";
    private static final String TRACE_VAR_NAME = "enteriesArr";
    private ContextFactory contextFactory;
    private final PersistenceHandler ph;
    @Nullable
    private CopyOnWriteArrayList<AirlockStream> streams;
    private final String appVersion;
    private int testMode = 1;

    private static String TAG = StreamsManager.class.getName();

    public StreamsManager(PersistenceHandler ph, String appVersion) {
        this.ph = ph;
        this.appVersion = appVersion;
    }

    public void suspendStreamingProcess(boolean isSuspended) {
        ph.write(Constants.SP_STREAMS_PROCESS_SUSPENDED, isSuspended);
    }

    public boolean isEnabled() {
        return streams != null && streams.size() > 0;
    }

    public JSONArray calculateAndSaveStreams(@Nullable JSONArray events, boolean processImmediately) throws JSONException {
        return calculateAndSaveStreams(events, processImmediately, null,null,null);
    }



    /**
     * @param events             the events list
     * @param processImmediately indicates whether the processing is immediate or not
     * @param streamsFilter      array of streams which should be processed
     */

    public synchronized JSONArray calculateAndSaveStreams(@Nullable JSONArray events, boolean processImmediately, @Nullable String[] streamsFilter,@Nullable JSONArray fieldsToSendToAnalytics,@Nullable AirlockStreamResultsTracker airlockStreamResultsTracker ) {

        JSONArray errors = null;
        if (streams == null || streams.size() == 0) {
            return null;
        }

        if (contextFactory == null) {
            contextFactory = ContextFactory.getGlobal();
        }

        String utilsAsString = ph.read(Constants.SP_FEATURE_UTILS_STREAMS, "");
        Context context = contextFactory.enterContext();
        Scriptable scope;
        context.setOptimizationLevel(-1);
        scope = context.initStandardObjects();
        try {
            if (!utilsAsString.isEmpty()) {
                context.evaluateString(scope, utilsAsString, "<cmd>", 1, null);
            }
        } catch (Throwable th) {
            errors = new JSONArray();
            JSONObject error = new JSONObject();
            error.put("name", "Utils");
            error.put("error", th.getMessage());
            errors.put(error);
        }
        String JS_TRACE_USING_UTILS = TRACE_VAR_NAME + " = Array.apply(undefined,Array(MAX_ENTERIES_COUNT))" + "" + ";nextIndex = " + "";

        //#### test hack #####
        //If testMode property exist on streams file - it is dev hack for testing (since this property is not created by server side)
        //If testMode on odd number - it is stress test (with sleep on each run)
        boolean isStressTest = false;
        if (this.testMode > 1 && this.testMode % 2 == 1) {
            isStressTest = true;
        }
        if (isStressTest) {
            String waitFunc = "function wait(ms){var start = new Date().getTime();var end = start;while(end < start + ms){end = new Date().getTime();}}";
            context.evaluateString(scope, waitFunc, "<cmd>", 1, null);
        }
        //#### end test hack ########

        for (int counter = 0; counter < this.testMode; counter++) {
            if (!isEnabled()) {
                return null;
            }

            //run filters on event and add event to queue if needed
            //If thread was changed need to init the context again...
            for (int i = 0; i < streams.size(); i++) {
                AirlockStream stream = streams.get(i);
                if (stream == null || !stream.isProcessingEnabled() || !stream.isEnabled()) {
                    continue;
                }

                //filter out streams which shouldn't be processed
                if (streamsFilter != null) {
                    boolean streamFilteredOut = true;
                    for (String streamName : streamsFilter) {
                        if (streamName.equals(stream.getName())) {
                            streamFilteredOut = false;
                        }
                    }
                    if (streamFilteredOut) {
                        continue;
                    }
                }

                if (events != null) {
                    //Events could be null if the call is made to trigger immediate processing...
                    stream.pushPendingEvents(events);
                }
                if (stream.isProcessing()) {
                    if (processImmediately) {
                        int countWaits = 0;
                        //if process immediately wait max 30 seconds...
                        while (countWaits < 30 && stream.isProcessing()) {
                            countWaits++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                //Do nothing
                            }
                        }
                    } else {
                        //it is not process immediately mode - continue to next stream.
                        continue;
                    }
                }

                stream.setProcessing(true);
                JSONObject pendingEvent = stream.popPendingEvent();

                while (pendingEvent != null) {
                    String eventAsString = "event=" + pendingEvent;
                    Object filterResult = null;
                    try {
                        context.evaluateString(scope, eventAsString, "<cmd>", 1, null);
                        filterResult = context.evaluateString(scope, stream.getFilter(), "<cmd>", 1, null);
                    } catch (Throwable th) {
                        //If single stream processing fails do not stop processing of other streams...
//                            StreamTrace.getInstance().write("javascript processing failed for stream -  " + stream.getName() + ", and filter -" + stream.getFilter() + ": " + th.getMessage());
                        stream.setProcessing(false);
                    }
                    if (filterResult != null && filterResult.equals(true)) {
                        stream.addEvent(pendingEvent);
                    }
                    pendingEvent = stream.popPendingEvent();
                }

                //skip processing if in the debug mode the processing is suspended
                if (stream.isProcessingSuspended()) {
                    stream.setProcessing(false);
                    stream.persist(ph);
                    setTraceRecords(stream, context, scope);
                } else {
                    if ((stream.getEvents().length() > 0) && (processImmediately || stream.shouldProcess())) {
                        String eventsAsString = "events=" + stream.getEvents().toString();
                        try {
                            context.evaluateString(scope, eventsAsString, "<cmd>", 1, null);
                            int freeSize = stream.getMaxCacheSize();
                            String cacheString = stream.getCache();
                            if (cacheString == null || cacheString.isEmpty() || cacheString.equals("{}")) {
                                cacheString = "{" + FREE_SIZE_TITLE + ":" + freeSize + "}";
                            } else {
                                int indexA = cacheString.indexOf(FREE_SIZE_TITLE);
                                int indexB = cacheString.indexOf(",", indexA);
                                if (indexB < 0) {
                                    //no found - means it is the last item on json....
                                    indexB = cacheString.indexOf("}", indexA);
                                }
                                String sizeString = cacheString.substring(indexA + FREE_SIZE_TITLE.length() + 1, indexB);
                                cacheString = cacheString.replaceFirst(sizeString, String.valueOf(freeSize - getCacheSize(cacheString)));
                            }

                            String trace = "";
                            if (!utilsAsString.isEmpty()) {
                                trace = JS_TRACE_USING_UTILS + stream.getTrace().toJSONArray().length();
                            }
                            String cacheAsString = "result={};cache=" + cacheString + ";" + trace;
                            context.evaluateString(scope, cacheAsString, "<cmd>", 1, null);
                            String processorCommand = stream.getProcessor();
                            if (isStressTest) {
                                //On stress test sleep for 0.1 seconds for each processing
                                processorCommand = "wait(100);" + processorCommand;
                            }
                            context.evaluateString(scope, processorCommand, "<cmd>", 1, null);
                            Object cacheResult = context.evaluateString(scope, "JSON.stringify(cache)", "<cmd>", 1, null);
                            Object ctxResult = context.evaluateString(scope, "JSON.stringify(result)", "<cmd>", 1, null);
                            String newResultMap = Context.toString(ctxResult);
                            //track stream results in airlytics if needed
                            if(fieldsToSendToAnalytics != null){
                                try {
                                    Map<String, Object> streamResultsChanges = stream.getStreamResultsChanges(newResultMap, fieldsToSendToAnalytics);
                                    if (airlockStreamResultsTracker != null && streamResultsChanges != null && !streamResultsChanges.isEmpty()) {
                                        airlockStreamResultsTracker.trackResults(streamResultsChanges);
                                    }
                                }catch (Throwable e){
                                    //error in callback should not fail the update of the result
                                    Logger.log.e(TAG, e.getMessage(), e);
                                }
                            }
                            stream.updateResults(newResultMap, Context.toString(cacheResult));
                            setTraceRecords(stream, context, scope);
                        } catch (Throwable th) {
                            //If single stream processing fails do not stop processing of other streams...
                            stream.setError("Javascript processing failed for stream [" + stream.getName() + "], Error:" + th.getMessage());
                            stream.putTraceRecord("Javascript processing failed for stream [" + stream.getName() + "], Error:" + th.getMessage());
                        } finally {
                            stream.setProcessing(false);
                        }
                    } else {
                        stream.setProcessing(false);
                    }
                    if (stream.getError() != null && !stream.getError().isEmpty()) {
                        if (errors == null) {
                            errors = new JSONArray();
                        }
                        JSONObject error = new JSONObject();
                        error.put("name", stream.getName());
                        error.put("error", stream.getError());
                        errors.put(error);
                    }
                    stream.setLastProcessedTime(System.currentTimeMillis() + "");
                    stream.persist(ph);
                }
            }
        }
        return errors;
    }

    private void setTraceRecords(AirlockStream stream, Context context, Scriptable scope) throws JSONException {
        Object traceResult = null;
        try {
            traceResult = context.evaluateString(scope, TRACE_VAR_NAME, "<cmd>", 1, null);
        } catch (Throwable th) {
            stream.putTraceRecord("Javascript processing failed for stream [" + stream.getName() + "], Error:" + th.getMessage());
        }
        if (traceResult != null) {
            String traceResultStr = Context.toString(traceResult);
            String[] traceResultArray = traceResultStr.split(",");
            for (int i = 0; i < traceResultArray.length; i++) {
                if (!traceResultArray[i].isEmpty()) {
                    stream.putTraceRecord(traceResultArray[i]);
                }
            }
        }
    }

    @TestOnly
    public void clearStreams() {
        if (streams != null) {
            streams = new CopyOnWriteArrayList<>();
        }
        String cachedStreams = ph.read(Constants.SP_STREAM_NAMES, "");
        if (cachedStreams == null || cachedStreams.isEmpty()) {
            return;
        }
        String[] steamNames = cachedStreams.split(",");
        for (String singleStreamName : steamNames) {
            ph.writeStream(singleStreamName, null);
        }
        ph.write(Constants.SP_STREAM_NAMES, "");
    }

    private int getCacheSize(String streamCache) {
        //this is an approximate value - should be close enough to be accurate : each character consume 2 bytes...
        return (streamCache.length() * 2) / AirlockStream.KILLOBYTE;//this will return size in KB -
    }

    private JSONArray getAirlockStreams() throws JSONException {
        JSONObject jsonObject = ph.readJSON(Constants.SP_FEATURE_USAGE_STREAMS);
        JSONArray streamsArray = jsonObject.optJSONArray("streams");
        //This is a hidden property to be used for test only - can be added manually to file os S3
        //>0 to process every call number of times,odd number for stress test (sleep on js processing)
        this.testMode = jsonObject.optInt("testMode", 1);
        if (streamsArray == null) {
            streamsArray = new JSONArray();
        }
        return streamsArray;
    }

    @CheckForNull
    public synchronized AirlockStream getStreamByName(String streamName) {
        if (streamName == null) {
            return null;
        }
        streamName = streamName.replaceAll(" ", "_");
        if (this.streams != null) {
            for (AirlockStream stream : this.streams) {
                if (stream.getName().equals(streamName)) {
                    return stream;
                }
            }
        }
        return null;
    }

    //This method should not be synchronized since it is being called by updateStreams which is synchronized
    private void initStreams() {
        if (ph.readJSON(Constants.SP_FEATURE_USAGE_STREAMS).length() == 0) {
            return;
        }

        StringBuilder cachedStreamNames = new StringBuilder();
        try {
            JSONArray streamsArray = getAirlockStreams();
            streams = new CopyOnWriteArrayList<>();
            if (streamsArray != null) {
                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject obj = streamsArray.getJSONObject(i);
                    AirlockStream stream = new AirlockStream(obj, ph, this.appVersion);
                    String streamName = stream.getName();
                    streams.add(stream);
                    if (i > 0) {
                        cachedStreamNames.append(",");
                    }
                    cachedStreamNames.append(streamName);
                }
            }
        } catch (JSONException e) {
//            StreamTrace.getInstance().write("Initializing streams got an exception: " + e.getMessage());
        }
        loadPersistentStreams();
        ph.write(Constants.SP_STREAM_NAMES, cachedStreamNames.toString());
    }

    public synchronized void updateStreams() {

        if (streams == null) {
            initStreams();
            return;
        }
        StringBuilder cachedStreamNames = new StringBuilder();
        HashSet<String> newStreamNames = new HashSet<>();
        try {
            JSONArray streamsArray = getAirlockStreams();
            if (streamsArray != null) {
                for (int i = 0; i < streamsArray.length(); i++) {
                    JSONObject obj = streamsArray.getJSONObject(i);
                    AirlockStream stream = new AirlockStream(obj, ph, this.appVersion);
                    String streamName = stream.getName();
                    newStreamNames.add(streamName);
                    boolean streamExists = false;
                    for (AirlockStream existingStream : this.streams) {
                        if (existingStream.getName().equals(streamName)) {
                            streamExists = true;
                            existingStream.update(stream);
                        }
                    }
                    if (!streamExists) {
                        streams.add(stream);
                    }
                    if (i > 0) {
                        cachedStreamNames.append(",");
                    }
                    cachedStreamNames.append(streamName);
                }
                for (int i = 0; i < this.streams.size(); i++) {
                    if (!newStreamNames.contains(this.streams.get(i).getName())) {
                        this.streams.remove(i);
                    }
                }
            }
            ph.write(Constants.SP_STREAM_NAMES, cachedStreamNames.toString());
        } catch (JSONException e) {
//            StreamTrace.getInstance().write("Updating streams got an exception: " + e.getMessage());
        }
    }

    public synchronized void updateStreamsEnablement() {
        try {
            if (!isEnabled()) {
                return;
            }
            if (this.streams != null) {
                for (AirlockStream stream : this.streams) {
                    stream.setProcessingEnablement();
                }
            }
        } catch (Throwable e) {
            //do nothing - this is to avoid crash if streams were updated by elsewhere on app
        }
    }

    //This method should not be synchronized since it is being called by updateStreams which is synchronized
    private void loadPersistentStreams() {
        //loads persisted data and deleted orphan data
        String cachedStreams = ph.read(Constants.SP_STREAM_NAMES, "");
        if (cachedStreams == null || cachedStreams.isEmpty()) {
            return;
        }
        String[] steamNames = cachedStreams.split(",");
        HashSet<String> cachedStreamSet = new HashSet<>();
        for (String streamName : steamNames) {
            cachedStreamSet.add(streamName);
        }

        if (this.streams == null) {
            return;
        }

        for (AirlockStream streamItem : this.streams) {
            String name = streamItem.getName();
            if (cachedStreamSet.contains(name)) {
                //load data
                streamItem.loadStreamRuntimeDataFormDisk();
                //delete from cached - it is not orphan
                cachedStreamSet.remove(name);
            }
        }
        if (cachedStreamSet.size() == 0) {
            //No orphan streams
            return;
        }
        if (cachedStreamSet.size() > 0) {
            for (String name : cachedStreamSet) {
                ph.writeStream(name, null);
            }
        }
    }

    public synchronized void persist() {
        if (streams != null) {
            for (AirlockStream stream : streams) {
                stream.persist(ph);
            }
        }
    }

    //This method should not be synchronized since it is being called only by tests
    @TestOnly
    public void clearAllStreams() {
        if (streams != null) {
            if (isEnabled()) {
                for (AirlockStream stream : streams) {
                    stream.clearProcessingData();
                }
            }
        }
    }

    public synchronized String getStreamsResults() {
        JSONObject streamResults = new JSONObject();
        if (streams != null && streams.size() > 0) {
            for (AirlockStream stream : streams) {
                if (stream != null && stream.isProcessingEnabled()) {
                    try {
                        String result = stream.getResult();
                        if (result != null && !result.isEmpty() && !result.equals("{}")) {
                            streamResults.put(stream.getName(), new JSONObject(result));
                        }
                    } catch (JSONException e) {
                        //Do nothing
                    }
                }
            }
        }
        return streamResults.toString();
    }

    @Nullable
    public List<AirlockStream> getStreams() {
        return streams == null ? Collections.<AirlockStream>emptyList() : streams;
    }

    public List<String> getStreamNames() {
        List<String> streamNames = new ArrayList<>();
        if (streams != null && streams.size() > 0) {
            for (AirlockStream stream : streams) {
                if (stream != null && stream.isProcessingEnabled()) {
                    streamNames.add(stream.getName());
                }
            }
        }
        return streamNames;
    }
}
