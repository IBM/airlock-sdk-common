package com.ibm.airlock;

import java.io.IOException;

/**
 * @author Denis Voloshin
 */

public interface AbstractTestDataManager {

    /*
     * This method return a string representation of a file located under the model folder
     * (expected hierarchy should be followed)
     */
    public String getFileContent(String filePathUnderDataFolder) throws IOException;

    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException;
}
