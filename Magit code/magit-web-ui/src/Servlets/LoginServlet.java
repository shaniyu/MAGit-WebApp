package Servlets;

import magitEngine.Constants;
import magitObjects.Repository;
import notifications.ClientsManager;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Set;


@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final String USER_NAME = Constants.USERNAME;
    private static final String CLIENTS_MANAGER = "clienstManager";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try(PrintWriter out = response.getWriter())
        {
            String userName = request.getParameter(USER_NAME);

            if (illegalUserName(userName))
            {
                writeIllegalUserName(out);
            }
            else {
                synchronized (this){
                    userName = userName.trim(); // normalize the string parameter
                    if (isNewUser(userName))
                    {
                        // register a new user
                        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
                        CM.addUser(userName);
                    }

                    response.sendRedirect("UserPage.html?username=" + userName);
                }
            }
        }
    }


    private boolean illegalUserName(String userName) {
        return (userName == null || userName.isEmpty());
    }

    // updates first page
    private void writeIllegalUserName(PrintWriter out) {

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Login page</title>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1  style=\"font-family: Comic Sans MS;color: #2b2301; text-align: center;\">Welcome to&nbsp;<span style=\"font-family: Comic Sans MS;color: #ff621f;\">magit!</span></h1>");
        out.println("<form action=\"login\" method=\"get\">");
        out.println("<p style=\"font-family: Comic Sans MS;text-align: center;\">Username: <input name=\"username\" type=\"text\" /></p>");
        out.println("<h2 style=\"text-align: center;\">");
        out.println("<input style=\"font-family: Comic Sans MS;text-align: center;\" type=\"submit\" value=\"Login\" /></h2>");
        out.println("<h3 style=\"font-family: Comic Sans MS;color: #fa6171; text-align: center;\">Name can't be empty!</h3>");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }

    private boolean isNewUser(String newUserName)
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        return ! CM.isUserExist(newUserName);
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
