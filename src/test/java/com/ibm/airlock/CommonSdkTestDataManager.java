package com.ibm.airlock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author Denis Voloshin
 */

public class CommonSdkTestDataManager implements AbstractTestDataManager {

    @Override
    public String getFileContent(String filePathUnderDataFolder) throws IOException {
        InputStream st =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(filePathUnderDataFolder);
        return convertStreamToString(st);
    }

    @Override
    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException {
        File directory = new File(Thread.currentThread().getContextClassLoader().getResource(dirPathUnderDataFolder).getFile());
        return directory.list();
    }

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}
