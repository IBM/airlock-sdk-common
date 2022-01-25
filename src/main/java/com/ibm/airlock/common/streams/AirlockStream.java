package com.ibm.airlock.common.streams;

import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.StreamTrace;
import com.ibm.airlock.common.util.AirlockVersionComparator;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * @author eitan.schreiber on 24/07/2017.
 */

public class AirlockStream {


    private static final String STREAM_CACHE_IN_MEMORY_CACHE = "cache";
    private static final String RESULTS_IN_MEMORY_CACHE = "result";


    public final static int KILLOBYTE = 1024;
    private final static int DEF_CACHE_SIZE = 1024;
    private final static int DEF_MAX_CACHE_SIZE = 5120;
    private final static int DEF_EVENTS_SIZE = 256;
    private final static int DEF_MAX_EVENTS_SIZE = 5120;
    private final int DEFAULT_MAX_EVENTS_TO_PROCESS = 100;
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
    private String origName;
    private PersistenceHandler ph;
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


    public AirlockStream(JSONObject obj, PersistenceHandler ph, String appVersion) {
        filter = obj.optString("filter", "false");
        processor = obj.optString("processor");
        maxQueuedEvents = obj.optInt("maxQueuedEvents", -1);
        if (maxQueuedEvents > DEFAULT_MAX_EVENTS_TO_PROCESS) {
            maxQueuedEvents = -1;
        }
        this.processingSuspended = false;
        this.ph = ph;
        this.appVersion = appVersion;
        origName = obj.optString("name");
        name = origName.replaceAll("\\.", "_").replaceAll(" ", "_");
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

    private AirlockStream(String name, boolean processingEnabled,@Nullable String result) {
        this.name = name;
        this.processingEnabled = processingEnabled;
        runtimeData.put(RESULTS_IN_MEMORY_CACHE, result);
    }

    public void clearTrace() {
        trace.clearTrace();
    }


    public StreamTrace getTrace() {
        return trace;
    }

    public void loadStreamRuntimeDataFormDisk() {
        JSONObject persistedStreamData = ph.readStream(name);
        String events = persistedStreamData.optString("events");
        JSONArray eventsArray = null;
        if (!events.isEmpty()) {
            try {
                eventsArray = new JSONArray(events);
            } catch (JSONException e) {
                //
            }
            this.setEvents(eventsArray);
        }

        String pendingEvents = persistedStreamData.optString("pendingEvents");
        if (!pendingEvents.isEmpty()) {
            JSONArray pendingEventsArray = null;
            try {
                pendingEventsArray = new JSONArray(pendingEvents);
                this.setPendingEvents(pendingEventsArray);
            } catch (JSONException e) {
                //
            }
        }

        this.setProcessingSuspended(persistedStreamData.optBoolean("processingSuspended", false));
        this.setCache(persistedStreamData.optString("cache", "{}"));
        this.setResult(persistedStreamData.optString("result", "{}"));
        this.setLastProcessedTime(persistedStreamData.optString("lastProcessedTime", ""));
    }

    public String[] getTraceRecords() {
        return trace.getTraceArr();
    }

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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private JSONArray getInternalUserGroups() {
        return internalUserGroups;
    }

    public String getMinAppVersion() {
        return minAppVersion;
    }

    public long getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(long rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public String getStage() {
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

    public String getId() {
        return id;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public void addEvent(JSONObject event) {
        this.events.put(event);
    }

    @CheckForNull
    @TestOnly
    public String getResultForTest() {
        return runtimeData.get(RESULTS_IN_MEMORY_CACHE);
    }

    @CheckForNull
    @TestOnly
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

    private void setCache(String cache) {
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

    public void setEvents(@Nullable JSONArray events) {
        this.events = events;
    }

    public void persist(PersistenceHandler ph) {

        if (getCache() != null && getCache().length() * 2 / KILLOBYTE > maxCacheSize) {
            //If cache reached the max size - disable stream
            setProcessingEnabled("Stream reached maxCacheSize");
        }
        if (events != null) {
            int eventsInKillo = events.toString().length() * 2 / KILLOBYTE;
            if (eventsInKillo > maxEventsSize) {
                if (eventsInKillo > DEF_MAX_EVENTS_SIZE) {
                    setProcessingEnabled("Stream reached maxEventsSize");
                } else {
                    //trace.write("Stream " + this.name + " reached its queueSizeKB(" + maxEventsSize + ") the maxEventsSize was changed to "+ DEF_MAX_EVENTS_SIZE);
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

    private void getFlattenJsonArray(@Nullable JSONArray array,String prefix,Map<String,Object> mapResults,List<String> fieldsToReport){
        if(array == null){
            return;
        }
        for (Object item:array){
            getFlattenObject(item,prefix,mapResults,fieldsToReport);
        }
    }

    private void getFlattenObject(@Nullable Object obj,String prefix,Map<String,Object> mapResults,List<String> fieldsToReport){
        if(obj == null){
            return;
        }
        if(obj instanceof JSONObject){
            getFlattenJsonObject((JSONObject)obj,prefix,mapResults,fieldsToReport);
        }else if (obj instanceof JSONArray){
            getFlattenJsonArray((JSONArray)obj,prefix,mapResults,fieldsToReport);
        }
        if(fieldsToReport.contains(prefix)) {
            mapResults.put(name+ "." + prefix, obj);
        }
    }

    private void getFlattenJsonObject(@Nullable JSONObject obj,String prefix,Map<String,Object> mapResults,List<String> fieldsToReport){
        if(obj == null){
            return;
        }
        for (String curKey : obj.keySet()){
            Object opt = obj.opt(curKey);
            getFlattenObject(opt,prefix +((prefix.isEmpty())?"":".")+curKey,mapResults,fieldsToReport);
        }
    }

    private List<String> getContextFieldsToCurrentStream(JSONArray fieldsToReport){
        List<String> retList = new LinkedList<>();
        String curContextField = "context.streams."+name;
        int length = fieldsToReport.length();
        for(int i=0;i<length;++i){
            String curField = fieldsToReport.optString(i);
            if(curField != null && curField.indexOf(curContextField) == 0){
                int lengthOfPrefix = curContextField.length();
                if(curField.length() > lengthOfPrefix + 1){
                    retList.add(curField.substring(lengthOfPrefix+1));
                }
            }
        }
        return retList;
    }


    @CheckForNull
    public Map<String,Object> getStreamResultsChanges(@Nullable String newResultMap,@Nullable JSONArray fieldsToReport){
        if(newResultMap == null || fieldsToReport == null || fieldsToReport.length() == 0){
            return null;
        }
        List<String> contextFieldsToCurrentStream = getContextFieldsToCurrentStream(fieldsToReport);
        if(contextFieldsToCurrentStream.isEmpty()){
            return null;
        }

        String currentResult = getResult();
        if(currentResult == null || currentResult.isEmpty()){
            if(!newResultMap.isEmpty()){
                //no old results for the stream report all required fields
                JSONObject newResultMapJsonObj = new JSONObject(newResultMap);
                Map<String,Object> mapResults = new HashMap<>();
                getFlattenJsonObject(newResultMapJsonObj,"",mapResults,contextFieldsToCurrentStream);
                replaceNormalizedNameWithOrig(mapResults);
                return mapResults;
            }
        }else{
            //there are old results for the stream need to compare values of required fields
            if (!currentResult.equals(newResultMap)){
                JSONObject curObjResults = new JSONObject(currentResult);
                JSONObject newResultMapJsonObj = (!newResultMap.isEmpty())?new JSONObject(newResultMap):new JSONObject();
                Map<String,Object> mapResultsNew = new HashMap<>();
                Map<String,Object> mapResultsOld = new HashMap<>();
                getFlattenJsonObject(newResultMapJsonObj,"",mapResultsNew,contextFieldsToCurrentStream);
                getFlattenJsonObject(curObjResults,"",mapResultsOld,contextFieldsToCurrentStream);
                //remove all equals values in old and new results
                List<String> resultsToRemove = new ArrayList<>();
                for(String curKey:mapResultsNew.keySet()){
                    if(mapResultsOld.containsKey(curKey)){
                        Object newVal = mapResultsNew.get(curKey);
                        Object oldVal = mapResultsOld.get(curKey);
                        if(Objects.equals(newVal,oldVal)){
                            resultsToRemove.add(curKey);
                        }
                    }
                }
                for (String key : resultsToRemove){
                    mapResultsNew.remove(key);
                }
                replaceNormalizedNameWithOrig(mapResultsNew);
                return mapResultsNew;
            }
        }
        return null;
    }

    private void replaceNormalizedNameWithOrig(Map<String, Object> valuesChangedMap){
        if (this.name.equals(origName)){
            return;
        }
        List<String> keysToRemove = new ArrayList();
        Set<String> keys = new HashSet<>(valuesChangedMap.keySet());
        for (String key: keys){
            valuesChangedMap.put(key.replace(this.name, this.origName), valuesChangedMap.get(key));
            valuesChangedMap.remove(key);
        }
    }

    public void updateResults(@Nullable String resultMap, String cacheMap) {
        setResult(resultMap != null ? resultMap : "");

        if (cacheMap.length() * 2 / KILLOBYTE > maxCacheSize) {
            //If cache reached the max size - need to disable the stream.
            setProcessingEnabled("Stream reached maxCacheSize");
        } else {
            setCache(cacheMap);
        }

        this.events = new JSONArray();
    }

    private void setPendingEvents(@Nullable JSONArray events) {
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
        return (events.length() >= eventsToProcess) || (events != null && events.toString().length() * 2 / KILLOBYTE > maxEventsSize);
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
            //check minimum application version
            // the minAppVersion of the feature should be <= to the current productVersion
            // minAppVersion is now mandatory, so fail if missing
            String minAppVersion = getMinAppVersion();
            AirlockVersionComparator comparator = new AirlockVersionComparator();
            if (comparator.compare(minAppVersion, appVersion) > 0) {
                isProcessingEnabled = false;
                disableReason = "app version is too low";
            }
        } else {
            // Stream is disabled - clear the result field
            isProcessingEnabled = false;
            disableReason = "Stream is disables (enable = false)";
        }

        //check percentage
        JSONObject streamsRandomNumber = ph.getStreamsRandomMap();
        if (isProcessingEnabled && streamsRandomNumber != null && streamsRandomNumber.length() > 0) {
            double threshold = getRolloutPercentage();
            if (threshold <= 0) {
                isProcessingEnabled = false;
            } else if (threshold < 100.0) {
                isProcessingEnabled = streamsRandomNumber.optInt(getName()) <= threshold * 10000;
                if (!isProcessingEnabled) {
                    disableReason = "Stream did not reach roll out percentage";
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
            List<String> deviceUserGroups = ph.getDeviceUserGroups();
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

    private synchronized void setProcessingEnabled(@Nullable String disableReason) {
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
    public void clearEvents() {
        this.events = new JSONArray();
    }

    public void clearProcessingData() {
        setResult("");
        setCache("");
        this.events = new JSONArray();
        this.pendingEvents = new JSONArray();
        ph.writeStream(name, "{}");
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
            if (!compareJSONArrays(internalUserGroups, stream.getInternalUserGroups())) {
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
                    pendingEvents.append(",");
                }
                pendingEvents.append(eventObject.toString());
            }
            pendingEvents.append("]");
            objectAsJSON.put("processingSuspended", this.processingSuspended);
            objectAsJSON.put("pendingEvents", pendingEvents.toString());
            objectAsJSON.put("lastProcessedTime", this.lastProcessedTime);
        } catch (JSONException e) {
            //Do nothing
        }
        return objectAsJSON;
    }

    private boolean compareJSONArrays(JSONArray arrayA, JSONArray arrayB) throws JSONException {

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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public AirlockStream clone() {
        return new AirlockStream(name, processingEnabled, getResult());
    }
}
