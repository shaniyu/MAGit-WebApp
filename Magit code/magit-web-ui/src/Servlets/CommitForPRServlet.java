package Servlets;

import DataStructures.CommitChanges;
import Utils.Files.JsonOperations;
import ajaxResponses.CommitChangesAjaxResponse;
import ajaxResponses.CommitForPRAjaxResponse;
import ajaxResponses.FileAjaxResponse;
import magitEngine.Constants;
import magitObjects.Commit;
import magitObjects.Repository;
import org.apache.commons.io.FileUtils;
import servletUtils.ServletUtils;
import magitEngine.MagitEngine;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebServlet(name = "CommitForPRServlet", urlPatterns = "/commitForPR")
public class CommitForPRServlet extends HttpServlet {

    private static final int FIRST_PARENT = 1;
    private static final int SECOND_PARENT = 2;

    protected synchronized void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        CommitForPRAjaxResponse commitForPRAjaxResponse = null;
        String username = request.getParameter(Constants.USERNAME);
        String commitSha1 = request.getParameter(Constants.COMMIT_SHA1);
        String repoName = request.getParameter(Constants.REPO_NAME);

        Repository currRepo = ServletUtils.getClientsManager(getServletContext()).getRepoOfUserByRepoName(repoName, username);
        MagitEngine magitEngine = ServletUtils.createMagitEngine(username, repoName, getServletContext());
        Commit commit = currRepo.getCommitBySHA1(commitSha1);
        PrintWriter out = null;

        try{
            calcualteChangesBetweenCommitAndPrecedingCommits(magitEngine, commit);
            CommitChangesAjaxResponse commitChangesFirstParent = getCommitChangesResponseFromCommit(commit, FIRST_PARENT, username, magitEngine);
            CommitChangesAjaxResponse commitChangesSecondParent = getCommitChangesResponseFromCommit(commit, SECOND_PARENT, username, magitEngine);
            commitForPRAjaxResponse = new CommitForPRAjaxResponse(commitSha1, commit.getmMessage(), commit.getFirstParentSha1(),
                    commit.getSecondParentSha1(), commitChangesFirstParent, commitChangesSecondParent);
            out = response.getWriter();
        }
        catch (Exception e){
            commitForPRAjaxResponse = null;
        }
        finally {
            JsonOperations.printToOut(commitForPRAjaxResponse, out);
        }
    }

    private CommitChangesAjaxResponse getCommitChangesResponseFromCommit(Commit commit, int whichParent,
                                                                         String username, MagitEngine magitEngine) throws Exception{

        CommitChangesAjaxResponse res = null;

        CommitChanges commitChanges = whichParent == FIRST_PARENT ?
                commit.getCommitChangesToFirstPrecedingCommit() : commit.getCommitChangesToSecondPrecedingCommit();

        if(commitChanges != null){
            res = new CommitChangesAjaxResponse();
            String repoName = magitEngine.getCurrentRepo().getRepositoryName();
            //We need to spread the files in some other temp folder in order to read the files contents
            String tempFolderPath = magitEngine.getCurrentRepo().getMagitPath() + File.separator + "temp";
            File tempFolder = new File (tempFolderPath);
            magitEngine.spreadCommitToCheckInTemp(tempFolder, commit.getSha1());

            for(String fileName : commitChanges.getChangedFiles()){
                String fileFullPath = Constants.MAGIT_PATH + File.separator + username + File.separator + repoName + File.separator + fileName;
                File fileToRead = new File(tempFolderPath + File.separator + fileName);
                String fileContent = FileUtils.readFileToString(fileToRead, StandardCharsets.UTF_8);
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(fileName, fileFullPath, fileContent);
                res.addToChangedFiles(fileAjaxResponse);
            }

            for(String fileName : commitChanges.getNewFiles()){
                String fileFullPath = Constants.MAGIT_PATH + File.separator + username + File.separator + repoName + File.separator + fileName;
                File fileToRead = new File(tempFolderPath + File.separator + fileName);
                String fileContent = FileUtils.readFileToString(fileToRead, StandardCharsets.UTF_8);
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(fileName, fileFullPath, fileContent);
                res.addToNewFiles(fileAjaxResponse);
            }

            for(String fileName : commitChanges.getDeletedFiles()){
                String fileFullPath = Constants.MAGIT_PATH + File.separator + username + File.separator + repoName + File.separator + fileName;
                //No need for fileContent for deleted files
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(fileName, fileFullPath, null);
                res.addToDeletedFiles(fileAjaxResponse);
            }
        }

        return res;
    }

    // This function compares a commit to its parents and creates the diffs lists objects
    private void calcualteChangesBetweenCommitAndPrecedingCommits(MagitEngine magitEngine, Commit commit) throws Exception{
        String pathToSpreadTo = magitEngine.getRepositoryLocation() + File.separator + ".magit" + File.separator + "temp";
        // spread those commits to temp folder under magit folder
        if(commit.getFirstParentSha1() != null && !commit.getFirstParentSha1().equals("null")){
            if(commit.getCommitChangesToFirstPrecedingCommit() == null){
                commit.setCommitChangesToFirstPrecedingCommit(new CommitChanges());
                //Calculate changes only if CommitChangesToFirstPrecedingCommit is null.
                //If it wasn't null then it means we already calculated the changes at some point,
                //and these changes won't change because a commit can't changes
                magitEngine.calculateChangesBetween2Commits(
                        commit, commit.getFirstParentSha1(), commit.getCommitChangesToFirstPrecedingCommit(), pathToSpreadTo, "temp");
            }
        }
        if(commit.getSecondParentSha1() != null && !commit.getSecondParentSha1().equals("null")){
            if(commit.getCommitChangesToSecondPrecedingCommit() == null){
                commit.setCommitChangesToSecondPrecedingCommit(new CommitChanges());
                //Calculate changes only if CommitChangesToSecondPrecedingCommit is null.
                //If it wasn't null then it means we already calculated the changes at some point,
                //and these changes won't change because a commit can't changes
                magitEngine.calculateChangesBetween2Commits(
                        commit, commit.getSecondParentSha1(), commit.getCommitChangesToSecondPrecedingCommit(), pathToSpreadTo, "temp");
            }
        }
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
