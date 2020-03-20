package Servlets;

import Exceptions.BranchAlreadyInUseException;
import Exceptions.CommitIsNullException;
import Utils.Files.JsonOperations;
import ajaxResponses.*;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitObjects.Branch;
import magitObjects.Commit;
import magitObjects.Repository;
import notifications.ClientsManager;
import notifications.Message;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


@WebServlet(name = "RepositoryServlet", urlPatterns = "/loadRepo")
public class RepositoryServlet extends HttpServlet {


    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // we want to return an object, any object and not only text, so we use json
        response.setContentType("application/json;charset=UTF-8");
        try {
            String userName = request.getParameter(Constants.USERNAME);
            String repoName = request.getParameter(Constants.REPO_NAME);
            String typeOfRequest = request.getParameter(Constants.REQUEST_TYPE);
            switch (typeOfRequest)
            {
                case(Constants.GET_BRANCHES_INFO_REQUEST):
                    // get all repositories of user
                    handleGetRepoBranches(userName, repoName, response);
                    break;
                case(Constants.DELETE_BRANCH_REQUEST):
                    String branchToDelete = request.getParameter(Constants.BRANCH_NAME);
                    handleDeleteBranch(userName, repoName, branchToDelete, response);
                    break;
                case(Constants.GET_WC_INFO_REQUEST):
                    handleCheckWorkingCopyOpenChanges(userName, repoName, response);
                    break;
                case(Constants.GET_COMMITS_INFO_REQUEST):
                    handleGetHeadCommitsInfo(userName, repoName, response);
                    break;
                case(Constants.CHECKOUT_TO_BRANCH_REQUEST):
                    String branchToCheckoutTo = request.getParameter(Constants.BRANCH_NAME);
                    handleCheckoutToBranch(userName, repoName, branchToCheckoutTo, response);
                    break;
                case(Constants.HANDLE_RTB_REQUEST):
                    branchToCheckoutTo = request.getParameter(Constants.BRANCH_NAME);
                    handleAddRTBAndCheckout(userName, repoName, branchToCheckoutTo, response);
                    break;
                case(Constants.OPEN_CHANGES_ON_CHECKOUT_REQUEST):
                    branchToCheckoutTo = request.getParameter(Constants.BRANCH_NAME);
                    handleOpenedChangesOnCheckout(userName, repoName, branchToCheckoutTo, response);
                    break;
                case(Constants.ADD_NEW_BRANCH):
                    String newBranchName = request.getParameter(Constants.BRANCH_NAME);
                    handleAddNewBranch(userName, repoName, newBranchName , response);
                    break;
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void handleAddNewBranch(String userName,String repoName,String newBranchName ,HttpServletResponse response) throws IOException
    {
        GenericOperationResponse res = null;
        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        try
        {
            magitEngine.addNewBranch(newBranchName);
            res = new GenericOperationResponse(true, "");
        }
        catch (BranchAlreadyInUseException e)
        {
            res = new GenericOperationResponse(false, e.getMessage());
        }
        catch (IOException e)
        {
            res = new GenericOperationResponse(false, "Add branch failed, " + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(res, response.getWriter());
        }
    }
    private void handleOpenedChangesOnCheckout(String userName, String repoName, String branchToCheckoutTo, HttpServletResponse response) {

        //Creating an instance of magitEngine with the current username and repository
        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        CheckoutToBranchAjaxResponse result = null;
        PrintWriter out = null;

        try {
            out = response.getWriter();
            magitEngine.checkOutToBranch(branchToCheckoutTo);
            result = new CheckoutToBranchAjaxResponse(true, "You have checked out to branch " + branchToCheckoutTo);

        } catch (Exception e) {
            result = new CheckoutToBranchAjaxResponse(false, "Error, unable to checkout " + branchToCheckoutTo + ".\n" + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(result, out);
        }
    }

    private void handleAddRTBAndCheckout(String userName, String repoName, String branchToCheckoutTo, HttpServletResponse response) {

        //Creating an instance of magitEngine with the current username and repository
        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        CheckoutToBranchAjaxResponse result = null;
        PrintWriter out = null;

        try
        {
            out = response.getWriter();
            String newBranchName = magitEngine.addRTBForRBAndCheckoutToIt(branchToCheckoutTo);
            result = new CheckoutToBranchAjaxResponse(true, "Remote tracking branch added!\n, Branch " + newBranchName + " was created and you checked out to it.");
        }
        catch (Exception e)
        {
            result = new CheckoutToBranchAjaxResponse(false, "Error, " + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(result, out);
        }
    }

    private void handleCheckoutToBranch(String userName, String repoName, String branchToCheckoutTo, HttpServletResponse response) {

        //Creating an instance of magitEngine with the current username and repository
        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        CheckoutToBranchAjaxResponse result = null;
        PrintWriter out = null;

        try{
            out = response.getWriter();
            // it user wants to checkout to a RB, he can't
            // but we offer him to create new RTB and checkout to it instead
            if (magitEngine.getCurrentRepo().getBranchByName(branchToCheckoutTo).getIsRemote())
            {
                //We need to send this result back to the html page, because we need to user to send confirmation
                //if he wants to create an RTB and checkout to it
                result = new CheckoutToBranchAjaxResponse(true, "RTB");
            }
            else
            {
                try{
                    if (! magitEngine.isThereOpenChangesInRepository())
                    {
                        // no opened changes on this commit, we can checkout
                        try
                        {
                            magitEngine.checkOutToBranch(branchToCheckoutTo);
                            result = new CheckoutToBranchAjaxResponse(true, "You have checked out to branch " + branchToCheckoutTo);
                        }
                        catch (IOException e)
                        {
                            result = new CheckoutToBranchAjaxResponse(false, "Error, Unable to checkout " + branchToCheckoutTo + ".\n" + e.getMessage());
                        }
                    }
                    else
                    {
                        //If there are open changes then we need to inform the user an get confirmation from him
                        //if he wants to cancel the checkout or checkout anywat
                        result = new CheckoutToBranchAjaxResponse(true, "OpenChanges");
                    }
                }
                catch (CommitIsNullException e)
                {
                    result = new CheckoutToBranchAjaxResponse(false, "Error, this branch doesn't point to any commit.\nCan't checkout.");
                }
                catch (Exception e)
                {
                    result = new CheckoutToBranchAjaxResponse(false, "Error, could not check for open changes");
                }
            }
        }
        catch (Exception e){
            result = new CheckoutToBranchAjaxResponse(false, "Error, " + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(result, out);
        }

    }


    private void handleDeleteBranch(String userName, String repoName, String branchToDelete, HttpServletResponse response) {

        //Creating an instance of magitEngine with the current username and repository
        MagitEngine magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
        DeleteBranchAjaxResponse result = null;
        PrintWriter out = null;

        try
        {
            out = response.getWriter();

            if(magitEngine.getCurrentRepo().getBranchByName(branchToDelete).getIsRemote()){
                result = new DeleteBranchAjaxResponse(false, "Error, " + branchToDelete + " is a remote branch.\nYou can't delete remote branches!");
            }
            else
            {
                // this branch exist, and is not the head or remote branch - > should delete it
                magitEngine.deleteBranch(branchToDelete); // deleting local branch

                // in case this branch is RTB, delete its RB and also delete the RR branch, and notify the remote repo user
                if (magitEngine.isBranchRTB(branchToDelete))
                {
                    // delete the RB
                    String remoteRepoName = magitEngine.getCurrentRepo().getRemoteRepoName();
                    String remoteRepoUserName = magitEngine.getCurrentRepo().getRemoteRepoUsername();
                    String remoteBranchName = remoteRepoName + File.separator + branchToDelete;
                    magitEngine.deleteBranch(remoteBranchName);
                    // delete the branch from the RTB file
                    magitEngine.deleteRTBFromFile(branchToDelete);

                    // delete the branch in the RR
                    MagitEngine remoteRepoEngine = ServletUtils.createMagitEngine(remoteRepoUserName, remoteRepoName, getServletContext());
                    remoteRepoEngine.deleteBranch(branchToDelete);

                    // notify the remote repo user
                    Message deleteBranchMessage = new Message(userName, remoteRepoName, userName + " deleted your branch, " + branchToDelete, new Date());
                    ClientsManager manager = ServletUtils.getClientsManager(getServletContext());
                    manager.sendMessageToUser(remoteRepoUserName, deleteBranchMessage);
                }

                // delete branch succeeded
                result = new DeleteBranchAjaxResponse(true, "The branch " + branchToDelete + " was deleted.");

            }
        }
        catch (Exception e)
        {
            result = new DeleteBranchAjaxResponse(true, "Error, could not delete the branch " + branchToDelete + ".\n" + e.getMessage());
        }
        finally {
            JsonOperations.printToOut(result, out);
        }

    }

    private void handleGetRepoBranches(String userName, String repoName, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        ArrayList<BranchesAjaxResponse> result = getBranchesOfRepo(userName, repoName);
        JsonOperations.printToOut(result, out);
    }

    private ArrayList<BranchesAjaxResponse> getBranchesOfRepo(String username, String repoName) throws IOException
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        ArrayList<Branch> listOfBranches = CM.getAllRepoBranches(username, repoName);
        MagitEngine magitEngine = ServletUtils.createMagitEngine(username, repoName, getServletContext());
        ArrayList<BranchesAjaxResponse> result = new ArrayList<>();

        for (Branch b: listOfBranches)
        {
            String branchName = b.getName();
            String branchSha1 = "";
            String commitMessage = "";
            if (b.getCommit() != null && b.getCommit().getSha1() != null && !b.getCommit().getSha1().equals("null"))
            {
                branchSha1  = b.getCommit().getSha1();
                commitMessage = b.getCommit().getmMessage();
            }
            boolean isHead = CM.isHeadBranch(username, repoName, b.getName());
            boolean isRTB = magitEngine.isBranchRTB(b.getName());
            BranchesAjaxResponse curr  = new BranchesAjaxResponse(branchName, branchSha1, commitMessage, isHead, b.getIsRemote(), isRTB);
            result.add(curr);
        }
        return result;
    }

    private void handleCheckWorkingCopyOpenChanges(String userName, String repoName, HttpServletResponse response) throws Exception
    {
        PrintWriter out = response.getWriter();
        boolean result = CheckIfThereIsOpenChangesInUserRepo(userName, repoName);
        JsonOperations.printToOut(result, out);
    }

    private boolean CheckIfThereIsOpenChangesInUserRepo(String userName, String repoName) throws Exception
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        return CM.isThereOpenChangesInRepo(userName, repoName);
    }

    private void handleGetHeadCommitsInfo(String userName, String repoName, HttpServletResponse response) throws Exception
    {
        PrintWriter out = response.getWriter();
        ArrayList<HeadCommitsAjaxResponse> result = getHeadCommitsInfo(userName, repoName);
        JsonOperations.printToOut(result, out);
    }

    private ArrayList<HeadCommitsAjaxResponse> getHeadCommitsInfo(String username, String repoName) throws IOException
    {
        ArrayList<HeadCommitsAjaxResponse> result = new ArrayList<>();
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        ArrayList<Commit> allOfHeadCommitsInHistory =  CM.getHeadCommits(username, repoName);
        Collections.sort(allOfHeadCommitsInHistory, Commit.commitDateComparator);
        for (Commit commit : allOfHeadCommitsInHistory)
        {
            ArrayList<String> branchesPointedByThisCommit = CM.getBranchesPointedByCommit(username, repoName, commit);
            HeadCommitsAjaxResponse headCommit = new HeadCommitsAjaxResponse(commit.getmMessage(),
                    commit.getSha1(), commit.getmCreatedDate(), commit.getmCreatedBy(), branchesPointedByThisCommit);
            result.add(headCommit);
        }
        return result;
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