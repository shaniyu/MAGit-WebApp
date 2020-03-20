package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.CommitChangesAjaxResponse;
import ajaxResponses.EditWCAjaxResponse;
import ajaxResponses.FileAjaxResponse;
import magitEngine.Constants;
import org.apache.commons.io.FileUtils;
import servletUtils.ServletUtils;
import sun.security.util.AuthResources_ja;
import magitEngine.MagitEngine;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@WebServlet(name = "EditWCServlet", urlPatterns = "/editWC")
public class EditWCServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // we want to return an object, any object and not only text, so we use json
        response.setContentType("application/json;charset=UTF-8");

        String fileFullPath = request.getParameter(Constants.FILE_PATH);
        String fileContent = request.getParameter(Constants.FILE_CONTENT);
        String typeOfRequest = request.getParameter(Constants.REQUEST_TYPE);
        String username = request.getParameter(Constants.USERNAME);
        String repositoryName = request.getParameter(Constants.REPO_NAME);
        
        try
        {
            PrintWriter out = response.getWriter();
            switch (typeOfRequest) {
                case (Constants.GET_OPEN_CHANGES):
                    handleGetOpenChanges(username, repositoryName, out);
                    break;
                case (Constants.DELETE_FILE):
                    // get all repositories of user
                    handleDeleteFile(fileFullPath, out);
                    break;
                case (Constants.EDIT_FILE):
                    handleEditFile(fileFullPath, fileContent, out);
                    break;
                case(Constants.ADD_NEW_FILE):
                    handleAddNewFile(username, repositoryName, fileFullPath, fileContent, out);
                    break;
            }
        }
        catch(Exception e){
            
            System.out.println(e.getMessage());
        }
    }

    private void handleGetOpenChanges(String username, String repositoryName, PrintWriter out){

        CommitChangesAjaxResponse openChangesRes = new CommitChangesAjaxResponse();
        MagitEngine magitEngine = ServletUtils.createMagitEngine(username, repositoryName, getServletContext());

        try{
            magitEngine.updateAllOpenChangesOfWC();

            for(String fileName : magitEngine.getChangedFiles()){
                String relativeFilePath = fileName.substring(fileName.lastIndexOf(repositoryName) + repositoryName.length() + 1);
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(relativeFilePath, fileName, null);
                openChangesRes.addToChangedFiles(fileAjaxResponse);
            }

            for(String fileName : magitEngine.getNewFiles()){
                String relativeFilePath = fileName.substring(fileName.lastIndexOf(repositoryName) + repositoryName.length() + 1);
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(relativeFilePath, fileName, null);
                openChangesRes.addToNewFiles(fileAjaxResponse);
            }

            for(String fileName : magitEngine.getDeletedFiles()){
                FileAjaxResponse fileAjaxResponse = new FileAjaxResponse(fileName, null, null);
                openChangesRes.addToDeletedFiles(fileAjaxResponse);
            }
        }
        catch (Exception e){
            openChangesRes = null;
        }
        finally {
            JsonOperations.printToOut(openChangesRes, out);
        }
    }

    //The fileFullPath is the path of the new file relative to the repository location
    private void handleAddNewFile(String username, String repositoryName, String fileFullPath, String fileContent, PrintWriter out){

        EditWCAjaxResponse editWCAjaxResponse = null;
        String repositoryPath = Constants.MAGIT_PATH + File.separator + username + File.separator + repositoryName;
        //Need to add the repository location to the fileFullPath which is relative to the repository location
        File newFileToAdd = new File(repositoryPath + File.separator + fileFullPath);

        //Checking if there is already a file/ folder with the same name
        if(newFileToAdd.exists()){
            editWCAjaxResponse = new EditWCAjaxResponse(false, "Couldn't create file.\na file or folder with the same name already exists");
            JsonOperations.printToOut(editWCAjaxResponse, out);
        }
        else{
            // if folder of the new file doesn't exist, create it
            if (! newFileToAdd.getParentFile().exists())
            {
                newFileToAdd.getParentFile().mkdirs();
            }
            try{
                newFileToAdd.createNewFile();
                FileUtils.writeStringToFile(newFileToAdd, fileContent, StandardCharsets.UTF_8);
                editWCAjaxResponse = new EditWCAjaxResponse(true, "");
                JsonOperations.printToOut(editWCAjaxResponse, out);
            }
            catch (IOException e){
                editWCAjaxResponse = new EditWCAjaxResponse(false, "Couldn't create new file");
                JsonOperations.printToOut(editWCAjaxResponse, out);
            }
        }
    }

    private void handleDeleteFile(String fileFullPath, PrintWriter out) {

        File fileToDelete = new File(fileFullPath);
        boolean isFileDeleted = fileToDelete.delete();
        EditWCAjaxResponse editWCAjaxResponse = new EditWCAjaxResponse(isFileDeleted, isFileDeleted ? "" : "Couldn't delete file");
        JsonOperations.printToOut(editWCAjaxResponse, out);
    }

    private void handleEditFile(String fileFullPath, String fileContent, PrintWriter out){

        EditWCAjaxResponse editWCAjaxResponse = null;
        try{
            File fileToEdit = new File(fileFullPath);
            FileUtils.writeStringToFile(fileToEdit, fileContent, StandardCharsets.UTF_8);
            editWCAjaxResponse = new EditWCAjaxResponse(true, "");
        }
        catch (IOException e){
            editWCAjaxResponse = new EditWCAjaxResponse(false, "Error, couldn't edit the file");
        }
        finally {
            JsonOperations.printToOut(editWCAjaxResponse, out);
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


