package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.FileAjaxResponse;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import org.apache.commons.io.FileUtils;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebServlet(name = "GetCommitFiles", urlPatterns = "/getCommitFiles")
public class GetCommitFiles extends HttpServlet {

    private static final String TEMP_FOLDER = "tempCommitFiles";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // we want to return an object, any object and not only text, so we use json
        response.setContentType("application/json;charset=UTF-8");

        String userName = request.getParameter(Constants.USERNAME);
        String repoName = request.getParameter(Constants.REPO_NAME);
        String commitSha1 = request.getParameter(Constants.COMMIT_SHA1);
        List<FileAjaxResponse> fileAjaxResponseList = new ArrayList<>(); //List of FileAjaxResponses to return to javascript code

        try {
            //Creating magitEngine object in order to execute the logic
            MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
            //If this is an empty repository and there is no commit yet, then commit sha1 of the request will be "class='btn'".
            boolean isEmptyRepo = commitSha1.contains("class");
            getFilesOfCommit(commitSha1, fileAjaxResponseList, magitEngine, isEmptyRepo);

        } catch (Exception e) {

            fileAjaxResponseList = null;
        }
        finally {
            JsonOperations.printToOut(fileAjaxResponseList ,response.getWriter());
        }
    }

    private void getFilesOfCommit(String commitSha1,
                                  List<FileAjaxResponse> fileAjaxResponseList,
                                  MagitEngine magitEngine, boolean isEmptyRepo) throws IOException, Exception {

        List<String> allFilePathsInWC ; //Will contain all of the commit's file paths

        //If the commit is the head branch commit then all the files are already in the WC and we can take them from there
        if (isEmptyRepo || isCommitHead(commitSha1, magitEngine)) {

            //Getting all of the file paths in the working copy EXCLUDING file in .magit folder
            Stream<Path> fileWalk = Files.walk(Paths.get(magitEngine.getCurrentRepo().getRepositoryLocation()));
            allFilePathsInWC = fileWalk.filter(Files::isRegularFile) //Getting all text files in repository folder
                    .filter(x -> !(x.toAbsolutePath().toString().contains(".magit"))) //Excluding any file paths that contain ".magit"
                    .map(x -> x.toString()).collect(Collectors.toList()); //Getting list of the files paths

        } else { //If the commit isn't the head branch commit then its files aren't in the working copy
            //We need to spread the files in some other temp folder in order to iterate over them
            String tempFolderPath = magitEngine.getCurrentRepo().getMagitPath() + File.separator + TEMP_FOLDER;
            File tempFolder = new File (tempFolderPath);
            magitEngine.spreadCommitToCheckInTemp(tempFolder, commitSha1);

            //Getting all file paths of the commit from the temp folder is which the commit files were spreaded
            Stream<Path> fileWalk = Files.walk(Paths.get(tempFolderPath));
            allFilePathsInWC = fileWalk.filter(Files::isRegularFile) //Getting all text files in repository folder
                    .map(x -> x.toString()).collect(Collectors.toList()); //Getting list of the files paths
        }

        for (String filePath : allFilePathsInWC) {
            File currFile = new File(filePath);
            String fileContent = FileUtils.readFileToString(currFile, StandardCharsets.UTF_8);
            String fileName = getFileNameFromPath(filePath, magitEngine);
            FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(fileName, filePath, fileContent);
            fileAjaxResponseList.add(fileAjaxResponse);
        }
    }

    private String getFileNameFromPath(String filePath, MagitEngine magitEngine) {

        String fileName = null;

        if (filePath.contains(TEMP_FOLDER)){

            int lastIndexOfTempFolderName = filePath.lastIndexOf(TEMP_FOLDER);
            int tempFolderNameLength = TEMP_FOLDER.length();
            fileName = filePath.substring(lastIndexOfTempFolderName + tempFolderNameLength + 1);
        }
        else{
            int lastIndexOfRepoName = filePath.lastIndexOf(magitEngine.getRepositoryName());
            int repoNameLength = magitEngine.getRepositoryName().length();
            fileName = filePath.substring(lastIndexOfRepoName + repoNameLength + 1);
        }

        return fileName;
    }

    private boolean isCommitHead(String commitSha1, MagitEngine magitEngine) {
        return magitEngine.getCurrentRepo().getHeadBranch().getCommit().getSha1().equals(commitSha1);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
