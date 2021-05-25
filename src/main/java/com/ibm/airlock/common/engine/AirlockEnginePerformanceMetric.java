package com.ibm.airlock.common.engine;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;


/**
 * The class holds as a map and exposes performance results of the airlock SDK performance metrics
 * per thread. The class is singleton which holds data per specific thread.
 * <p>
 * The usage:
 * AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().startMeasuring();
 * AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().reportValue(String,value);
 * AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().getReport();
 * AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric().stopMeasuring();
 *
 * @author Denis Voloshin
 */

public class AirlockEnginePerformanceMetric {


    public static final String EVALS_COUNTER = "total_js_evals_counter";
    public static final String FEATURES_NUMBER = "total_features_number";
    public static final String MERGE_CALCULATION_RESULTS = "merge_calculation_results_time";
    public static final String RHINO_INIT = "rhino_init_time";
    public static final String CONTEXT_INIT = "rhino_airlock_context_time";
    public static final String MERGE_SHARED_CONTEXT = "rhino_merge_shared_context_time";
    public static final String JS_UTILS_LOADING = "js_utils_loading_time";
    public static final String TRANSLATION_LOADING = "translations_loading_time";
    public static final String BRANCH_MERGING = "branch_merging_time";
    public static final String CALCULATION_TOTAL = "calculation_total_time";
    public static final String CALCULATION_FEATURES = "calculation_features_time";
    private final Hashtable<Long, Hashtable<String, Long>> metricPerThread = new Hashtable<>();
    private boolean isMeasuringStarted = false;

    /**
     * Returns singleton instance to AirlockEnginePerformanceMetric object
     *
     * @return AirlockContextManager
     */
    public static AirlockEnginePerformanceMetric getAirlockEnginePerformanceMetric() {
        return AirlockEnginePerformanceMetricLazyHolder.INSTANCE;
    }

    /**
     * The method should be called before a measuring data need to be collected for specific thread.
     */
    public void startMeasuring() {
        //metricPerThread.put(Thread.currentThread().getId(), new Hashtable<String, Long>());
        isMeasuringStarted = true;
    }

    /**
     * Stores performance result for specific metric from either predefined list of metrics or any string.
     * if the there is not active measuring session, the report is ignored
     *
     * @param metricName a name of measured metric
     * @param startPoint a moment an measured event have been started.
     */
    public void report(String metricName, long startPoint) {
        if(!isMeasuringStarted){
            return;
        }
//        if (metricPerThread.containsKey(Thread.currentThread().getId())) {
//            metricPerThread.get(Thread.currentThread().getId()).put(metricName, System.currentTimeMillis() - startPoint);
//        }
        getThreadHash(Thread.currentThread().getId()).put(metricName, System.currentTimeMillis() - startPoint);
    }

    private Hashtable<String, Long> getThreadHash(Long threadId){
        if(!metricPerThread.containsKey(Thread.currentThread().getId())){
            Hashtable<String, Long> threadReport =  new Hashtable<String, Long>();
            metricPerThread.put(Thread.currentThread().getId(), threadReport);
            return threadReport;
        }else{
            return metricPerThread.get(Thread.currentThread().getId());
        }
    }

    /**
     * Stores performance result for specific metric from either predefined list of metrics or any string.
     * if the there is not active measuring session, the report is ignored
     *
     * @param metricName a name of measured metric
     * @param value      a measured value
     */

    public void reportValue(String metricName, long value) {
//        if (metricPerThread.containsKey(Thread.currentThread().getId())) {
//            metricPerThread.get(Thread.currentThread().getId()).put(metricName, value);
//        }

        if(!isMeasuringStarted){
            return;
        }
        getThreadHash(Thread.currentThread().getId()).put(metricName, value);

    }

    /**
     * @return a table of measured results for the active measuring session. if the there is not active measuring session, the empty table will be returned.
     */
    public Hashtable<String, Long> getReport() {
        if (!isMeasuringStarted) {
            return new Hashtable<>();
        }
        Hashtable<String, Long> reports = new Hashtable<>();

        Set<Long> reportsKeyPerThread = metricPerThread.keySet();
        TreeSet<Long> mutableSet = new TreeSet<>();
        mutableSet.addAll(reportsKeyPerThread);

        Iterator<Long> iterator = mutableSet.iterator();
        while (iterator.hasNext()) {
            Long key = iterator.next();
            reports.put(metricPerThread.get(key).toString(), key);
        }
        return reports;
    }

    /**
     * should be call when the measuring session has to be finished.
     */
    public void stopMeasuring() {
        isMeasuringStarted  = false;
//        if (metricPerThread.containsKey(Thread.currentThread().getId())) {
//            metricPerThread.remove(Thread.currentThread().getId());
//        }
        metricPerThread.clear();

    }

    private static final class AirlockEnginePerformanceMetricLazyHolder {
        private static final AirlockEnginePerformanceMetric INSTANCE = new AirlockEnginePerformanceMetric();
    }
}

