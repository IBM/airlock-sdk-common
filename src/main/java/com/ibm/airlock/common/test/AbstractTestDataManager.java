package com.ibm.airlock.common.test;

import java.io.IOException;

/**
 * Created by iditb on 20/11/17.
 */

public interface AbstractTestDataManager {

    /*
     * This method return a string representation of a file located under the data folder
     * (expected hierarchy should be followed)
     */
    public String getFileContent(String filePathUnderDataFolder) throws IOException;

    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException;
}
