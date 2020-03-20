package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.GenericOperationResponse;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "CommitServlet", urlPatterns = "/createCommit")
public class CommitServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // we want to return an object, any object and not only text, so we use json
        response.setContentType("application/json;charset=UTF-8");

        String userName = request.getParameter(Constants.USERNAME);
        String repoName = request.getParameter(Constants.REPO_NAME);
        String commitMessage = request.getParameter(Constants.COMMIT_MESSAGE);

        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        handleCreateCommit(magitEngine, commitMessage, response);
    }

    private void handleCreateCommit(MagitEngine magitEngine, String commitMessage, HttpServletResponse response) {

        GenericOperationResponse genericOperationResponse = null;
        PrintWriter out = null;
        try {
            out = response.getWriter();

            if (magitEngine.isThereOpenChangesInRepository()) {
                //We check if the commitMessage is empty in the client side so if we got to the server it means the commit message is not empty
                try {
                    String newCommitSha1 = magitEngine.createCommit(commitMessage, null);
                    genericOperationResponse = new GenericOperationResponse(true, newCommitSha1);

                } catch (Exception e) {
                    genericOperationResponse = new GenericOperationResponse(false, "Error, could not create a commit\n" + e.getMessage());
                }
            }
            else {
                genericOperationResponse = new GenericOperationResponse(false, "There are no changes to commit.");
            }
        }
        catch (Exception e)
        {
            genericOperationResponse = new GenericOperationResponse(false, "Error, could not check for open changes before commit\n" + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(genericOperationResponse, out);
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
