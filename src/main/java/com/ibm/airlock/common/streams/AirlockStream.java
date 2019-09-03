package com.ibm.airlock.common.streams;

import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.model.StreamTrace;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author eitan.schreiber
 */

@SuppressWarnings("WeakerAccess")
public class AirlockStream {

    private static final String STREAM_CACHE_IN_MEMORY_CACHE = "cache";
    private static final String RESULTS_IN_MEMORY_CACHE = "result";

    public static final int KILOBYTE = 1024;
    private static final int DEF_CACHE_SIZE = 1024;
    private static final int DEF_MAX_CACHE_SIZE = 5120;
    private static final int DEF_EVENTS_SIZE = 256;
    private static final int DEF_MAX_EVENTS_SIZE = 5120;
    private static final int DEFAULT_MAX_EVENTS_TO_PROCESS = 100;
    private String filter;
    private String processor;
    private StreamTrace trace;
    private String error;
    private int maxQueuedEvents;
    private boolean enabled;
    private boolean processingSuspended;
    //This boolean is a result of calculation and not only the "enabled" flag
    private boolean processingEnabled;
    private boolean isProcessing;
    private JSONArray internalUserGroups;
    private String minAppVersion;
    private long rolloutPercentage;
    private String stage;
    private String name;
    private PersistenceHandler persistenceHandler;
    private String appVersion;
    private int maxCacheSize;
    private int maxEventsSize;
    private String lastProcessedTime;
    private String id;

    //Objects to be persisted when processing
    private JSONArray events;
    private JSONArray pendingEvents;
    private final Object pendingEventsLock = new Object();

    private final InMemoryCache<String, String> runtimeData = new InMemoryCache<>();


    public AirlockStream(JSONObject obj, PersistenceHandler persistenceHandler, String appVersion) {
        filter = obj.optString("filter", "false");
        processor = obj.optString("processor");
        maxQueuedEvents = obj.optInt("maxQueuedEvents", -1);
        if (maxQueuedEvents > DEFAULT_MAX_EVENTS_TO_PROCESS) {
            maxQueuedEvents = -1;
        }
        processingSuspended = false;
        this.persistenceHandler = persistenceHandler;
        this.appVersion = appVersion;
        name = obj.optString("name");
        // noinspection DynamicRegexReplaceableByCompiledPattern
        name = name.replaceAll("[. ]", "_");
        trace = new StreamTrace(obj.optJSONArray("trace"));
        enabled = obj.optBoolean("enabled", false);
        internalUserGroups = obj.optJSONArray("internalUserGroups");
        minAppVersion = obj.optString("minAppVersion");
        rolloutPercentage = obj.optLong("rolloutPercentage");
        stage = obj.optString("stage");
        id = obj.optString("uniqueId");
        maxCacheSize = obj.optInt("cacheSizeKB", DEF_CACHE_SIZE);
        if (maxCacheSize < 1 || maxCacheSize > DEF_MAX_CACHE_SIZE) {
            maxCacheSize = DEF_CACHE_SIZE;
        }
        maxEventsSize = obj.optInt("queueSizeKB", DEF_EVENTS_SIZE);
        if (maxEventsSize < 1 || maxEventsSize > DEF_MAX_EVENTS_SIZE) {
            maxEventsSize = DEF_EVENTS_SIZE;
        }
        events = new JSONArray();
        pendingEvents = new JSONArray();
        isProcessing = false;
        lastProcessedTime = "";
        setProcessingEnablement();

        runtimeData.put(STREAM_CACHE_IN_MEMORY_CACHE, "");
        runtimeData.put(RESULTS_IN_MEMORY_CACHE, "");
    }

    public AirlockStream(String name, boolean processingEnabled,@Nullable String result) {
        this.name = name;
        this.processingEnabled = processingEnabled;
        if (result != null){
            runtimeData.put(RESULTS_IN_MEMORY_CACHE, result);
        }
    }

    public void clearTrace() {
        trace.clearTrace();
    }


    public StreamTrace getTrace() {
        return trace;
    }

    public void loadStreamRuntimeDataFormDisk() {
        JSONObject persistedStreamData = persistenceHandler.readStream(name);
        String events = persistedStreamData.optString("events");
        JSONArray eventsArray = new JSONArray();
        if (!events.isEmpty()) {
            try {
                eventsArray = new JSONArray(events);
            } catch (JSONException e) {
                //
            }
            setEvents(eventsArray);
        }

        String pendingEvents = persistedStreamData.optString("pendingEvents");
        if (!pendingEvents.isEmpty()) {
            JSONArray pendingEventsArray = new JSONArray();
            try {
                pendingEventsArray = new JSONArray(pendingEvents);
            } catch (JSONException e) {
                //
            }
            setPendingEvents(pendingEventsArray);
        }

        setProcessingSuspended(persistedStreamData.optBoolean("processingSuspended", false));
        setCache(persistedStreamData.optString("cache", "{}"));
        setResult(persistedStreamData.optString("result", "{}"));
        setLastProcessedTime(persistedStreamData.optString("lastProcessedTime", ""));
    }

    @SuppressWarnings("unused")
    public String[] getTraceRecords() {
        return trace.getTraceArr();
    }

    @SuppressWarnings("unused")
    public String getLastProcessedTime() {
        return lastProcessedTime;
    }

    public void setLastProcessedTime(String lastProcessedTime) {
        this.lastProcessedTime = lastProcessedTime;
    }

    public void putTraceRecord(String traceInfo) {
        trace.write(traceInfo);
    }

    public void pushPendingEvents(JSONArray events) {
        synchronized (pendingEventsLock) {
            for (int i = 0; i < events.length(); i++) {
                try {
                    pendingEvents.put(events.get(i));
                } catch (JSONException e) {
                    // Nothing to do
                }
            }
        }
    }

    @CheckForNull
    public JSONObject popPendingEvent() {
        JSONObject event = null;
        synchronized (pendingEventsLock) {
            if (pendingEvents.length() > 0) {
                event = (JSONObject) pendingEvents.remove(0);
            }
        }
        return event;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @SuppressWarnings("unused")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JSONArray getInternalUserGroups() {
        return internalUserGroups;
    }

    @CheckForNull
    public String getMinAppVersion() {
        return minAppVersion;
    }

    public long getRolloutPercentage() {
        return rolloutPercentage;
    }

    @SuppressWarnings("unused")
    public void setRolloutPercentage(long rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    private String getStage() {
        return stage;
    }

    public String getFilter() {
        return filter;
    }

    public String getProcessor() {
        return processor;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void addEvent(JSONObject event) {
        this.events.put(event);
    }

    @TestOnly
    @CheckForNull
    public String getResultForTest() {
        return runtimeData.get(RESULTS_IN_MEMORY_CACHE);
    }

    @TestOnly
    @CheckForNull
    public String getCacheForTest() {
        return runtimeData.get(STREAM_CACHE_IN_MEMORY_CACHE);
    }


    @CheckForNull
    public String getCache() {
        if (runtimeData.get(STREAM_CACHE_IN_MEMORY_CACHE) == null) {
            loadStreamRuntimeDataFormDisk();
        }
        return runtimeData.get(STREAM_CACHE_IN_MEMORY_CACHE);
    }

    public void setCache(String cache) {
        runtimeData.put(STREAM_CACHE_IN_MEMORY_CACHE, cache);
    }

    @CheckForNull
    public String getResult() {
        if (runtimeData.get(RESULTS_IN_MEMORY_CACHE) == null) {
            loadStreamRuntimeDataFormDisk();
        }
        return runtimeData.get(RESULTS_IN_MEMORY_CACHE);
    }

    private void setResult(String result) {
        runtimeData.put(RESULTS_IN_MEMORY_CACHE, result);
    }

    public JSONArray getEvents() {
        return events;
    }

    public void setEvents(JSONArray events) {
        this.events = events;
    }

    public void persist(PersistenceHandler ph) {

        if (getCache() != null && getCache().length() * 2 / KILOBYTE > maxCacheSize) {
            //If cache reached the max size - disable stream
            setProcessingEnabled("Stream reached maxCacheSize");
        }
        if (events != null) {
            int eventsInKilo = events.toString().length() * 2 / KILOBYTE;
            if (eventsInKilo > maxEventsSize) {
                if (eventsInKilo > DEF_MAX_EVENTS_SIZE) {
                    setProcessingEnabled("Stream reached maxEventsSize");
                } else {
                    maxEventsSize = DEF_MAX_EVENTS_SIZE;
                }
            }
        }

        ph.writeStream(name, toJSON().toString());
    }

    public boolean isProcessingSuspended() {
        return processingSuspended;
    }

    public void setProcessingSuspended(boolean processingSuspended) {
        this.processingSuspended = processingSuspended;
    }

    public void updateResults(@Nullable String resultMap, String cacheMap) {
        setResult(resultMap != null ? resultMap : "");

        if (cacheMap.length() * 2 / KILOBYTE > maxCacheSize) {
            //If cache reached the max size - need to disable the stream.
            setProcessingEnabled("Stream reached maxCacheSize");
        } else {
            setCache(cacheMap);
        }

        events = new JSONArray();
    }

    public void setPendingEvents(JSONArray events) {
        this.pendingEvents = events;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public synchronized void setProcessing(boolean processing) {
        isProcessing = processing;
    }

    /**
     * identifies of stream processing should be running
     * It depends on number ov events or on max size of events
     */
    public boolean shouldProcess() {
        int eventsToProcess = DEFAULT_MAX_EVENTS_TO_PROCESS;
        if (maxQueuedEvents > 0) {
            //If trigger is -1 it means we should our default...
            eventsToProcess = maxQueuedEvents;
        }
        return (events.length() >= eventsToProcess) || (events != null && events.toString().length() * 2 / KILOBYTE > maxEventsSize);
    }

    /**
     * Method that sets boolean if stream is enabled depending the following:
     * 1. enablement flag
     * 2. minApp version
     * 3. percentage
     * 4. stage and userGroups
     */
    public void setProcessingEnablement() {
        boolean isProcessingEnabled = true;
        String disableReason = "";
        if (enabled) {
            //check min app version
            // the minAppVersion of the feature should be <= to the current productVersion
            // minAppVersion is now mandatory, so fail if missing
            String minAppVersion = getMinAppVersion();
            AirlockVersionComparator comparator = new AirlockVersionComparator();
            if (minAppVersion == null || comparator.compare(minAppVersion, appVersion) > 0) {
                isProcessingEnabled = false;
                disableReason = "app version is too low";
            }
        } else {
            // Stream is disabled - clear the result field
            isProcessingEnabled = false;
            disableReason = "Stream is disables (enable = false)";
        }

        //check percentage
        JSONObject streamsRandomNumber = persistenceHandler.getStreamsRandomMap();
        if (isProcessingEnabled && streamsRandomNumber != null && streamsRandomNumber.length() > 0) {
            double threshold = getRolloutPercentage();
            if (threshold <= 0) {
                isProcessingEnabled = false;
            } else if (threshold < 100.0) {
                int userFeatureRand = streamsRandomNumber.optInt(getName());
                if (userFeatureRand == 0) {
                    isProcessingEnabled = false;
                } else {
                    isProcessingEnabled = userFeatureRand <= threshold * 10000;
                }
                if (!isProcessingEnabled) {
                    disableReason = "Stream did not reach rollout percentage";
                }
            }
        }
        if (!isAssociatedWithUserGroup()) {
            //Stage and user groups
            disableReason = "App do not have the required user group definitions";
            setProcessingEnabled(disableReason);
        } else {
            if (!isProcessingEnabled) {
                setProcessingEnabled(disableReason);
            } else {
                setProcessingEnabled(null);
            }
        }
    }

    public boolean isAssociatedWithUserGroup() {
        boolean isAssociatedWithUserGroup = true;
        if (getStage().equals("DEVELOPMENT")) {
            JSONArray supportedUserGroups = getInternalUserGroups();
            //if any user group is defined return false.
            if (supportedUserGroups.length() == 0) {
                return false;
            }
            List<String> deviceUserGroups = persistenceHandler.getDeviceUserGroups();
            isAssociatedWithUserGroup = false;
            for (String userGroup : deviceUserGroups) {
                for (int i = 0; i < supportedUserGroups.length(); i++) {
                    try {
                        if (userGroup.equals(supportedUserGroups.get(i))) {
                            isAssociatedWithUserGroup = true;
                            break;
                        }
                    } catch (JSONException e) {
                        //Do nothing - group was not found
                    }
                }
                if (isAssociatedWithUserGroup) {
                    break;
                }
            }
        }
        return isAssociatedWithUserGroup;
    }

    public boolean isProcessingEnabled() {
        return processingEnabled;
    }

    public synchronized void setProcessingEnabled(@Nullable String disableReason) {
        if (disableReason == null) {
            this.processingEnabled = true;
        } else {
            this.processingEnabled = false;
            clearProcessingData();
            trace.write(this.name + " disabled: " + disableReason);
        }
    }

    /**
     * the method is used by debug screen only
     */
    @SuppressWarnings("unused")
    public void clearEvents() {
        this.events = new JSONArray();
    }

    public void clearProcessingData() {
        setResult("");
        setCache("");
        this.events = new JSONArray();
        this.pendingEvents = new JSONArray();
        persistenceHandler.writeStream(name, "{}");
    }

    public synchronized void update(AirlockStream stream) {

        boolean streamWasUpdated = false;
        if (!filter.equals(stream.getFilter())) {
            filter = stream.getFilter();
            streamWasUpdated = true;
        }
        if (!processor.equals(stream.getProcessor())) {
            processor = stream.getProcessor();
            streamWasUpdated = true;
        }
        if (maxQueuedEvents != stream.maxQueuedEvents) {
            maxQueuedEvents = stream.maxQueuedEvents;
            streamWasUpdated = true;
        }
        if (enabled != stream.enabled) {
            enabled = stream.enabled;
            streamWasUpdated = true;
        }
        try {
            if (!areJSONArraysEqual(internalUserGroups, stream.getInternalUserGroups())) {
                internalUserGroups = stream.getInternalUserGroups();
                streamWasUpdated = true;
            }
        } catch (JSONException e) {
            internalUserGroups = stream.getInternalUserGroups();
            streamWasUpdated = true;
        }

        if (!minAppVersion.equals(stream.minAppVersion)) {
            minAppVersion = stream.minAppVersion;
            streamWasUpdated = true;
        }
        if (rolloutPercentage != stream.rolloutPercentage) {
            rolloutPercentage = stream.rolloutPercentage;
            streamWasUpdated = true;
        }
        if (!stage.equals(stream.stage)) {
            stage = stream.stage;
            streamWasUpdated = true;
        }
        if (!id.equals(stream.id)) {
            id = stream.id;
            streamWasUpdated = true;
        }
        if (maxCacheSize != stream.maxCacheSize) {
            maxCacheSize = stream.maxCacheSize;
            streamWasUpdated = true;
        }
        if (maxEventsSize != stream.maxEventsSize) {
            maxEventsSize = stream.maxEventsSize;
            streamWasUpdated = true;
        }
        if (streamWasUpdated || processingEnabled != stream.processingEnabled) {
            setProcessingEnablement();
        }
    }

    public JSONObject toJSON() {
        JSONObject objectAsJSON = new JSONObject();
        try {
            objectAsJSON.put("cache", getCache());
            objectAsJSON.put("result", getResult());
            objectAsJSON.put("events", this.events.toString());
            objectAsJSON.put("trace", this.trace.toJSONArray());
            StringBuilder pendingEvents = new StringBuilder("[");
            int arrayLength = this.pendingEvents.length();
            for (int i = 0; i < arrayLength; i++) {
                JSONObject eventObject = (JSONObject) this.pendingEvents.get(i);
                if (i > 0) {
                    pendingEvents.append(',');
                }
                pendingEvents.append(eventObject.toString());
            }
            pendingEvents.append(']');
            objectAsJSON.put("processingSuspended", this.processingSuspended);
            objectAsJSON.put("pendingEvents", pendingEvents.toString());
            objectAsJSON.put("lastProcessedTime", this.lastProcessedTime);
        } catch (JSONException e) {
            //Do nothing
        }
        return objectAsJSON;
    }

    public boolean areJSONArraysEqual(JSONArray arrayA, JSONArray arrayB) throws JSONException {

        boolean matches = true;
        for (int i = 0; i < arrayA.length(); i++) {
            boolean matchesInternal = false;

            String groupItem = (String) arrayA.get(i);
            for (int j = 0; j < arrayB.length(); j++) {
                if (groupItem.equals(arrayA.get(i))) {
                    matchesInternal = true;
                    break;
                }
            }
            if (!matchesInternal) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public AirlockStream clone() {
        return new AirlockStream(name, processingEnabled, getResult());
    }
}
