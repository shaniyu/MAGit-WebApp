package Utils;

import Utils.Files.FilesOperations;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class SHA1 {
    private static final int blockSize = 4096;
    public static final int sha1Size = 40;

    // gets an exsiting text file and return a 40 bytes SHA1 String
    public static String  createSha1(File inputTextFile) throws Exception  {
        String content = "";
        if(inputTextFile.getPath().contains(".zip")){
            content = FilesOperations.readAllContentFromZipFile(inputTextFile.getPath());
        }
        else
            {
                //read file content to a string with FileUtils
                // we use FileUtils and not FilesOperations to recognize diff when enter is added to end of file
            content = FileUtils.readFileToString(inputTextFile, StandardCharsets.UTF_8);
        }
        // library from Aviad
        return (DigestUtils.sha1Hex(content));
    }
}
