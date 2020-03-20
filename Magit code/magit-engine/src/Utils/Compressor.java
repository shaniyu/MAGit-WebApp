package Utils;

import Utils.Files.StreamRegistry;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


// Example of inputs to run the methods:
// For zipFile:
// fileToZipPath: can be: "C:\\Users\\Yuval Vahaba\\Documents\\yuval.txt"
// zipFilePath: can be:  "C:\\Users\\Yuval Vahaba\\Documents\\yuval.zip"
// entryFileName: can be: "cf23df2207d99a74fbe169e3eba035e633b65d94.txt" ( this is a sha1)
// For unzipFile:
// zipFilePath: can be:  "C:\\Users\\Yuval Vahaba\\Documents\\yuval.zip"
// destDirectory: can be: "C:\\Users\\Yuval Vahaba\\Documents"

public class Compressor {

    private static final int bufferSize = 1024;
    public static void zipFile(String fileToZipPath, String zipFilePath, String entryFileName)
            throws  IOException
    {
        ZipOutputStream out = null;
        // entryFileName for example: "mytext.txt"
        // get all data from the input file
        try
        {
            InputStream fileToZipStream = new FileInputStream(fileToZipPath);
            StreamRegistry.streams.add(fileToZipStream);
            File zipFile = new File(zipFilePath);
            checkFileIsValid(zipFile, zipFilePath);
            // create output zip file
            out = new ZipOutputStream(new FileOutputStream(zipFile));
            StreamRegistry.streams.add(out);
            // create an entry to the zip file
            ZipEntry entry = new ZipEntry(entryFileName);
            out.putNextEntry(entry);
            readFromTextFileAndWriteToZipFile(fileToZipStream, out);
        }
        catch (FileNotFoundException e)
        {
            throw new FileNotFoundException("The file " + fileToZipPath + " doesn't exist.");
        }
    }
    // for zipping
    private static void readFromTextFileAndWriteToZipFile(InputStream fileToZipStream, ZipOutputStream out) throws IOException
    {
        byte buffer[] = new byte[bufferSize];
        int length;
        while ((length = fileToZipStream.read(buffer)) >= 0)
        {
            out.write(buffer, 0, length);
        }
        out.closeEntry();
        out.close();
        StreamRegistry.streams.remove(out);
        fileToZipStream.close();
        StreamRegistry.streams.remove(fileToZipStream);

    }

    public static void createZipFileFromContent(String content, String zipFilePath, String entryFileName) throws IOException
    {
        ZipOutputStream out = null;
        byte[] buffer = content.getBytes();
        File zipFile = new File(zipFilePath);
        checkFileIsValid(zipFile, zipFilePath);
        // create output zip file
        out = new ZipOutputStream(new FileOutputStream(zipFile));
        StreamRegistry.streams.add(out);
        // create an entry to the zip file
        ZipEntry entry = new ZipEntry(entryFileName);
        out.putNextEntry(entry);
        out.write(buffer, 0, content.length());
        out.closeEntry();
        out.close();
        StreamRegistry.streams.remove(out);
    }

    private static void checkFileIsValid(File zipFile, String zippedFilePath) throws IOException {
        // check that zip file with this name doesn't exist already, and that input path is not a directory
        if ( zipFile.exists())
        {
            throw new IOException("The file " + zippedFilePath+ " already exist.");
        }
        if (zipFile.isDirectory())
        {
            throw new IOException("The file " + zippedFilePath + " is a directory.");
        }
    }

    // for unzipping
    public static void unzipFile(String zipFilePath, String destDirectory) throws IOException {
        File zippedFile = new File(destDirectory);
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        StreamRegistry.streams.add(zipInputStream);
        ZipEntry entry = zipInputStream.getNextEntry();
        String zippedFilePath = destDirectory + File.separator + entry.getName();
        if ( ! entry.isDirectory())
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(zippedFilePath));
            StreamRegistry.streams.add(bos);
            byte[] bytesIn = new byte[bufferSize];
            int read = 0;
            while ((read = zipInputStream.read(bytesIn))!= -1)
            {
                bos.write(bytesIn, 0, read);
            }
            zipInputStream.close();
            StreamRegistry.streams.remove(zipInputStream);
            bos.close();
            StreamRegistry.streams.remove(bos);
        }
    }
}
