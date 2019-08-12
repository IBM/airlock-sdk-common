package com.ibm.airlock.common.util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class PropertiesFileUtils {
    public PropertiesFileUtils() {
    }

    public static void propertiesToFile(ArrayList<String[]> properties, String filePath) throws IOException {
        String res = "";

        for(int i = 0; i < properties.size(); ++i) {
            String[] current = (String[])properties.get(i);
            res = res + current[0] + "=" + current[1] + "\n";
        }

        FileUtils.stringToFile(res, filePath);
    }

    public static void propertiesToFile(HashMap<String, String> properties, String filePath) throws IOException {
        String res = "";
        Set<String> keys = properties.keySet();

        String key;
        for(Iterator var5 = keys.iterator(); var5.hasNext(); res = res + key + "=" + (String)properties.get(key) + "\n") {
            key = (String)var5.next();
        }

        FileUtils.stringToFile(res, filePath);
    }

    public static HashMap<String, String> pfileToHashMap(String filePath) throws FileNotFoundException, IOException {
        HashMap<String, String> map = new HashMap();
        BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
        String line = null;

        while((line = br.readLine()) != null) {
            String[] keyVal = line.split("=");
            map.put(keyVal[0], keyVal[1]);
        }

        br.close();
        return map;
    }

    public static String pfileToPostRequestParametersString(String filePath) throws IOException {
        String res = "";
        BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));

        for(String line = null; (line = br.readLine()) != null; res = res + line + "&") {
        }

        br.close();
        return StringUtils.removeLastChar(res);
    }
}
