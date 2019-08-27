package com.ibm.airlock.common.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String fileToString(String fileName, String encoding, boolean unifyNewLine) throws IOException {
        File file = new File(fileName);
        FileInputStream fin = new FileInputStream(file);
        byte[] fileContent = new byte[(int)file.length()];
        fin.read(fileContent);
        fin.close();
        String result = new String(fileContent, encoding);
        //noinspection DynamicRegexReplaceableByCompiledPattern
        return unifyNewLine ? result.replace("\r\n", "\n") : result;
    }

    public static byte[] fileToBytes(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int)length];
        int offset = 0;

        int num;
        while(offset < bytes.length && (num = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += num;
        }

        if (offset < bytes.length) {
            is.close();
            throw new IOException("The end of the stream has been reached: " + file.getName());
        } else {
            is.close();
            return bytes;
        }
    }

    public static ArrayList<File> splitFile(String fileName, int size) throws IOException {
        ArrayList<File> smallFiles = new ArrayList();
        File file = new File(fileName);
        if ((long)size >= file.length()) {
            smallFiles.add(file);
            return smallFiles;
        } else {
            FileInputStream fin = new FileInputStream(file);
            int sum = 0;

            for(int index = 0; (long)sum < file.length(); ++index) {
                byte[] fileContent = new byte[size];
                fin.read(fileContent, 0, size);
                sum += size;
                File addMe = new File(fileName.substring(0, fileName.lastIndexOf('.')) + index + '.' + fileName.substring(fileName.lastIndexOf('.') + 1));
                addMe.createNewFile();
                FileOutputStream fos = new FileOutputStream(addMe);
                fos.write(fileContent);
                fos.close();
                smallFiles.add(addMe);
            }

            fin.close();
            return smallFiles;
        }
    }

    public static void stringToFile(String writeMe, String filePath) throws IOException {
        File file = new File(filePath);
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(writeMe);
        fileWriter.flush();
        fileWriter.close();
    }

    public static void appendStringToEndOfFile(String appendMe, String filePath) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
        out.println(appendMe);
        out.close();
    }

    public static void replaceStringInFile(String filePath, String encoding, String replaceMe, String toReplace) throws IOException {
        String fileString = fileToString(filePath, encoding, false);
        fileString = fileString.replaceAll(replaceMe, toReplace);
        stringToFile(fileString, filePath);
    }

    public static boolean isStringInFile(String filePath, String encoding, String findMe) throws IOException {
        String fileString = fileToString(filePath, encoding, false);
        return fileString.contains(findMe);
    }

    public static void mergeFiles(String folderPath, String outputFileName) throws IOException {
        File outputFile = new File(folderPath + File.separator + outputFileName);
        outputFile.createNewFile();
        FileWriter writer = new FileWriter(outputFile, true);
        ArrayList<File> files = FolderUtils.allFilesFromFolder(folderPath);

        for (File current : files) {
            FileInputStream fin = new FileInputStream(current);
            byte[] fileContent = new byte[(int) current.length()];
            fin.read(fileContent);
            fin.close();
            writer.write(new String(fileContent));
        }

        writer.close();
    }

    public static void copy(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileInputStream fis = new FileInputStream(sourceFile); FileOutputStream fos = new FileOutputStream(destFile); FileChannel source = fis.getChannel(); FileChannel destination = fos.getChannel()) {
            long count = 0L;
            long size = source.size();

            while (count < size) {
                count += destination.transferFrom(source, count, size - count);
            }
        }

    }

    public static void zip(String folderPath) throws IOException {
        File folder = new File(folderPath);
        String zipFilePath = folderPath + File.separator + folder.getName() + ".zip";
        ArrayList<File> allFiles = FolderUtils.allFilesFromFolder(folderPath);
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        Iterator var8 = allFiles.iterator();

        while(var8.hasNext()) {
            File file = (File)var8.next();
            ZipEntry ze = new ZipEntry(file.getAbsolutePath());
            zos.putNextEntry(ze);
            FileInputStream in = new FileInputStream(file);

            int len;
            while((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            in.close();
            zos.closeEntry();
        }

        zos.close();
    }

    public static String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
