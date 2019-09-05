package com.ibm.airlock.common.util;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;


public class JsonUtils {
   public static void mergeJson(JSONObject to, JSONObject from) throws JSONException {
        Iterator<String> keys = from.keys();
        while (keys.hasNext()) {

            String key = keys.next();
            Object obj = from.get(key);

            Object objTo = null;
            try {
                objTo = to.get(key);
            } catch (Exception e) {
                //do nothing
            }

            if (objTo == null) {
                to.put(key, obj);
                continue;
            }

            if (obj instanceof JSONObject && objTo instanceof JSONObject) {
                mergeJson((JSONObject) objTo, (JSONObject) obj);
            } else // replace leaf item
            {
                to.put(key, obj); // arrays are not concatenated but replaced
            }
        }
    }
}
