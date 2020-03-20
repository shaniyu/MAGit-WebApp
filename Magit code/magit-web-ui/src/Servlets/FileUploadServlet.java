package Servlets;

//taken from: http://www.servletworld.com/servlet-tutorials/servlet3/multipartconfig-file-upload-example.html
// and http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html

import Exceptions.XmlNotValidException;
import Utils.Files.JsonOperations;
import ajaxResponses.LoadRepoFromXMLAjaxResponse;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitEngine.XmlValidation;
import magitObjects.Repository;
import notifications.ClientsManager;
import org.apache.commons.io.FileUtils;
import servletUtils.ServletUtils;
import xmlObjects.MagitRepository;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

@WebServlet(name ="FileUploadServlet",urlPatterns = "/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("fileupload/form.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //response.setContentType("text/html");
        response.setContentType("application/json;charset=UTF-8");
        Collection<Part> parts = request.getParts();
        Part part = parts.iterator().next(); // get first part ( we only sent one part)
        String username = request.getParameter("username");

        StringBuilder fileContent = new StringBuilder();
        LoadRepoFromXMLAjaxResponse result = null;
            //to write the content of the file to a string
            if (part.getSubmittedFileName() == null)
            {
                // no file was chosen
                result = new LoadRepoFromXMLAjaxResponse(false, "No file chosen");
            }
            else
                {
                fileContent.append(readFromInputStream(part.getInputStream()));
                InputStream xmlInputStream = part.getInputStream();

                try {
                    //Creating magit repository objects from the xml
                    JAXBContext jaxbContext = JAXBContext.newInstance(MagitRepository.class);
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    MagitRepository magitRepository = (MagitRepository) jaxbUnmarshaller.unmarshal(xmlInputStream);
                    XmlValidation xmlValidator = new XmlValidation(magitRepository);

                    if (xmlValidator.isMagitRepositoryValid()) {
                        if (isThereRepoWithThisName(magitRepository.getName(), username)) {
                            // can't create another repository with the same name
                            result = new LoadRepoFromXMLAjaxResponse(false, "Repository with same name already exists");

                        }
                        else {
                            String newRepoPath = Constants.MAGIT_PATH + File.separator + username + File.separator + magitRepository.getName();
                            File repoFolder = new File(newRepoPath);
                            if (repoFolder.exists()) {
                                result = new LoadRepoFromXMLAjaxResponse(false, "The directory " + newRepoPath + " already exist.");
                            } else {
                                // create the repository directory and spread all the xml data to it
                                MagitEngine engine = new MagitEngine();
                                Repository newRepo = new Repository();
                                engine.setCurrentRepo(newRepo);
                                engine.setUserName(username);
                                newRepo.spreadRepositoryFromXml(magitRepository, username);
                                // add new repo succeed
                                // add this repository to the user
                                ServletUtils.getClientsManager(getServletContext()).addRepositoryToUser(username, newRepo);
                                result = new LoadRepoFromXMLAjaxResponse(true, null);
                            }
                        }
                    }
                    // if the xml is not valid, exception is thrown
                }
                catch (XmlNotValidException e)
                {
                    result = new LoadRepoFromXMLAjaxResponse(false, "Xml is not valid, "+ e.getMessage());
                }
                catch (Exception e) {
                    result = new LoadRepoFromXMLAjaxResponse(false, "Xml could not be loaded, "+ e.getMessage());
                }
                finally {
                    ArrayList<LoadRepoFromXMLAjaxResponse> arr = new ArrayList();
                    arr.add(result);
                    JsonOperations.printToOut(arr, response.getWriter());
                }
            }
    }

    private boolean isThereRepoWithThisName(String repoName, String username)
    {
        Hashtable<String, Repository> userRepos = ServletUtils.getClientsManager(getServletContext()).getAllReposOfUser(username);
        return userRepos.get(repoName) != null; // there is a repo in this name to the user
    }
    private String readFromInputStream(InputStream inputStream) {
        return new Scanner(inputStream).useDelimiter("\\Z").next();
    }
}