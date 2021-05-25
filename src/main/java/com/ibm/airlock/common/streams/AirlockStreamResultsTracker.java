package com.ibm.airlock.common.streams;

import java.util.Map;

public interface AirlockStreamResultsTracker {
    void trackResults(Map<String,Object> map);
}
