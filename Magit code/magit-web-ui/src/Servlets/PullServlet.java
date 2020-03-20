package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.GenericOperationResponse;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitObjects.Repository;
import notifications.ClientsManager;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "PullServlet", urlPatterns = "/pull")
public class PullServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        GenericOperationResponse res = null;

        try {
            String userName = request.getParameter(Constants.USERNAME);
            String repoNameToPullTo = request.getParameter(Constants.REPO_NAME);

            MagitEngine myEngine = ServletUtils.createMagitEngine(userName, repoNameToPullTo, getServletContext());


            if (! myEngine.checkHeadIsRTB())
            {
                // if head branch is not a RTB is it impossible to pull
                res = new GenericOperationResponse(false,"The head branch is not a RTB, can't pull");
            }
            else if (myEngine.isThereOpenChangesInRepository())
            {
                // if repository has open changes it is impossible to pull
                res = new GenericOperationResponse(false,"There are open changes on WC, can't pull");
            }
            else {
                // else- can pull
                myEngine.pull();
                res = new GenericOperationResponse(true,null);
            }
        }
        catch (Exception e)
        {
            res = new GenericOperationResponse(false, e.getMessage());
        }
        finally {
            JsonOperations.printToOut(res, response.getWriter());
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
