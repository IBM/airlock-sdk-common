package com.ibm.airlock.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

class FolderUtils {

    public static void validateFolderStructure(String folderPath, HashMap<String, ArrayList<String>> expectedStructure) throws Exception {
        HashMap<String, ArrayList<String>> folderStructure = buildFolderStructure(folderPath);
        Set<String> expectedItems = expectedStructure.keySet();
        Iterator var5 = expectedItems.iterator();

        String itemName;
        ArrayList folderValue;
        ArrayList expectedValue;
        do {
            if (!var5.hasNext()) {
                return;
            }

            itemName = (String)var5.next();
            if (!folderStructure.containsKey(itemName)) {
                throw new Exception("An expected item in the given structure wasn't found. Item name: " + itemName);
            }

            folderValue = folderStructure.get(itemName);
            expectedValue = expectedStructure.get(itemName);
            if (!expectedValue.containsAll(folderValue)) {
                throw new Exception("Unexpected items were found in folder: " + itemName);
            }
        } while(folderValue.containsAll(expectedValue));

        throw new Exception("Expected items are missing in folder: " + itemName);
    }

    private static HashMap<String, ArrayList<String>> buildFolderStructure(String folderPath) {
        return mapContent(new File(folderPath), new HashMap());
    }

    private static HashMap<String, ArrayList<String>> mapContent(File item, HashMap<String, ArrayList<String>> map) {
        if (item.isFile()) {
            return map;
        } else {
            File[] content = item.listFiles();
            ArrayList<String> contentNames = new ArrayList();

            for(int i = 0; i < content.length; ++i) {
                if (content[i].isDirectory()) {
                    map = mapContent(content[i], map);
                }

                contentNames.add(content[i].getName());
            }

            map.put(item.getName(), contentNames);
            return map;
        }
    }

    private static ArrayList<File> allFilesFromDirectory(File dir, ArrayList<File> list) {
        dir.setReadOnly();
        File[] files = dir.listFiles();

        for(int i = 0; i < files.length; ++i) {
            if (files[i].isFile() && !list.contains(files[i])) {
                list.add(files[i]);
            }

            if (files[i].isDirectory()) {
                allFilesFromDirectory(files[i], list);
            }
        }

        dir.setWritable(true);
        return list;
    }

    public static ArrayList<File> allFilesFromFolder(String folderPath) {
        return allFilesFromDirectory(new File(folderPath), new ArrayList());
    }

    public static HashMap<String, File> folderContentArrayToHashMap(File[] content) {
        HashMap<String, File> map = new HashMap();

        for(int i = 0; i < content.length; ++i) {
            map.put(content[i].getName(), content[i]);
        }

        return map;
    }

    public static HashMap<String, File> folderContentArrayToHashMap(ArrayList<File> content) {
        HashMap<String, File> map = new HashMap();

        for(int i = 0; i < content.size(); ++i) {
            File file = content.get(i);
            map.put(file.getName(), file);
        }

        return map;
    }

    public static boolean isEmptyFolder(String folderPath) throws Exception {
        File file = new File(folderPath);
        if (file.isDirectory()) {
            String[] content = file.list();
            if (content != null) {
                return content.length <= 0;
            } else {
                throw new Exception("A NULL value was returned from File list method. The given path is not a folder");
            }
        } else {
            throw new Exception("The given path is not a folder");
        }
    }

    public static void validateDataFolder(String dataPath) throws Exception {
        File dataFolder = new File(dataPath);
        if (!dataFolder.exists()) {
            throw new Exception("The given folder path does not exist");
        } else if (!dataFolder.isDirectory()) {
            throw new Exception("The given folder path is not a folder");
        } else if (!dataFolder.canRead()) {
            throw new Exception("The given folder path cannot be read");
        }
    }
}
