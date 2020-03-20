package Utils.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FilesOperations {
    // get a string to write, and a file to write to
    public static boolean writeTextToFile(String textToWrite, String pathOfFile) throws IOException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(pathOfFile);
            StreamRegistry.streams.add(fw);
            fw.write(textToWrite);
            return true;
        }
        catch(Exception e){
            return false;
        }
        finally {
            fw.close();// close its a must when we write to a file
            StreamRegistry.streams.remove(fw);
        }
    }
    public static boolean appendTextToFile( String textToWrite, String pathOfFile) throws IOException
    {
        FileWriter fw = null;
        try
        {
            // true falg for appending to the file
            fw = new FileWriter(pathOfFile, true);
            StreamRegistry.streams.add(fw);
            fw.write(textToWrite);
            fw.close();
            StreamRegistry.streams.remove(fw);
            return true;
        }
        catch(Exception e){
            return false;
        }
        finally {
            fw.close();
            StreamRegistry.streams.remove(fw);
        }
    }
    public static String readStringFromFileUntilEnter(String pathOfAFile) throws IOException
    {
        BufferedReader in = null;
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        // pass file path without file type extension
        try
        {
            fileInputStream = new FileInputStream(pathOfAFile);
            StreamRegistry.streams.add(fileInputStream);
            inputStreamReader = new InputStreamReader(fileInputStream);
            StreamRegistry.streams.add(inputStreamReader);
            in = new BufferedReader(inputStreamReader);
            StreamRegistry.streams.add(in);

            String result = in.readLine();
            return result;
        }
        catch (IOException e) {
            throw new IOException("Error, could not read from " + pathOfAFile);
        }
        finally {
            fileInputStream.close();
            inputStreamReader.close();
            in.close();
            StreamRegistry.streams.remove(in);
            StreamRegistry.streams.remove(inputStreamReader);
            StreamRegistry.streams.remove(fileInputStream);
        }
    }

//    public static String readAllContentFromFile(String pathOfAFile) throws IOException {
//
//        BufferedReader in = null;
//        String result = null;
//        if(Files.exists(Paths.get(pathOfAFile))){
//            // pass file path without file type extension
//            try
//            {
//                in = new BufferedReader
//                        (new InputStreamReader(
//                                new FileInputStream(pathOfAFile)));
//                StreamRegistry.streams.add(in);
//
//
//                result = "";
//                String temp = "";
//
//                while ((temp = in.readLine()) != null){
//                    if (temp.isEmpty())
//                    {
//                        // means a line of enter
//                        temp = "\n";
//                    }
//                    result += temp + System.lineSeparator();
//                }
//            }
//            catch (IOException e) {
//                throw e;
//            }
//            finally {
//                in.close();
//                StreamRegistry.streams.remove(in);
//            }
//        }
//
//        return result;
//    }

    public static ArrayList<String> readAllContentFromFileByStrings(String pathOfAFile) throws IOException {

        BufferedReader in = null;
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        ArrayList<String> result = new ArrayList<>();
        if(Files.exists(Paths.get(pathOfAFile))){
            // pass file path without file type extension
            try
            {
                fileInputStream = new FileInputStream(pathOfAFile);
                StreamRegistry.streams.add(fileInputStream);
                inputStreamReader = new InputStreamReader(fileInputStream);
                StreamRegistry.streams.add(inputStreamReader);
                in = new BufferedReader(inputStreamReader);
                StreamRegistry.streams.add(in);
                String temp = "";
                while ((temp = in.readLine()) != null){
                    result.add(temp);
                }
            }
            catch (IOException e) {
                throw new IOException("Error, Could not read from " + pathOfAFile);
            }
            finally {
                fileInputStream.close();
                inputStreamReader.close();
                in.close();
                StreamRegistry.streams.remove(in);
                StreamRegistry.streams.remove(inputStreamReader);
                StreamRegistry.streams.remove(fileInputStream);
            }
        }

        return result;
    }

    public static boolean isXmlFile(String filePath)
    {
        int index = filePath.lastIndexOf('.');
        String fileExtension = filePath.substring(index +1);
        return (fileExtension.equals("xml"));
    }

    //This method checks if a file is empty, or if a folder doesn't contain any regular files
    //for example if a folder contains only empty folders- then it will be considered empty and the method will return true
    public static boolean isDirOrFileEmpty(final File file) throws IOException {
        boolean res = false;
        if (file.isDirectory()) {
            Stream<Path> fileWalk = Files.walk(Paths.get(file.getPath()));
            List<String> allRegularFilesInFolderPath = fileWalk.filter(Files::isRegularFile)//Getting all text files in folder
                    .map(x -> x.toString())
                    .collect(Collectors.toList());//Getting list of the files paths
            return(!(allRegularFilesInFolderPath.size() > 0));
        }
        else if (file.isFile()){
            res = FileUtils.readFileToString(file, StandardCharsets.UTF_8) == null ? true : false;
        }

        return res;
    }

    public static String readAllContentFromZipFile(String path) throws IOException {
        // the path name should include the extension ".zip"
        String result = null;
        InputStream stream = null;
        ZipFile zip = null;
        if(Files.exists(Paths.get(path))){


            zip = new ZipFile(path);
            StreamRegistry.streams.add(zip);
            String temp = "";
            result = "";

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                stream = zip.getInputStream(entry);
                StreamRegistry.streams.add(stream);
                result = IOUtils.toString( stream, StandardCharsets.UTF_8);
            }
        }
        stream.close();
        StreamRegistry.streams.remove(stream);
        zip.close();
        StreamRegistry.streams.remove(zip);
        return result;
    }


    // recursive function to delete folder and all its content (files & folders)
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static String getFileNameWithoutExtention(String fileName){

        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }

        return fileName;
    }
    public static ArrayList<String> readAllContentFromZipFileByStrings(String path) throws IOException {
        // the path name should include the extension ".zip"
        ArrayList<String> result = new ArrayList<>();

        if(Files.exists(Paths.get(path))){
            ZipFile zip = new ZipFile(path);
            StreamRegistry.streams.add(zip);
            String temp = "";

            for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    InputStreamReader inputStreamReader = new InputStreamReader(zip.getInputStream(entry));
                    StreamRegistry.streams.add(inputStreamReader);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    StreamRegistry.streams.add(reader);
                    try {
                        while ((temp = reader.readLine()) != null){
                            result.add(temp);
                        }

                    } catch (IOException ex) {
                        // do something, probably not a text file
                        throw ex;
                        //ex.printStackTrace();
                    }
                    finally {
                        inputStreamReader.close();
                        reader.close();
                        StreamRegistry.streams.remove(reader);
                        StreamRegistry.streams.remove(inputStreamReader);
                    }
                }
            }
            zip.close();
            StreamRegistry.streams.remove(zip);
        }

        return result;
    }

    // this function is called with exsiting files only
    static public String readFileContent(File f) throws IOException
    {
        return (FileUtils.readFileToString(f, StandardCharsets.UTF_8));
    }
}
