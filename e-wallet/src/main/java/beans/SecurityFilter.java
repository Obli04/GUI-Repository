package beans;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Security filter for protecting sensitive pages by ensuring that the user is authenticated.
 * This filter checks if a user is logged in before allowing access to certain pages.
 * If the user is not logged in, they are redirected to the login page.
 * 
 * @author Davide Scaccia - xscaccd00
 */
@WebFilter(filterName = "SecurityFilter", urlPatterns = {
    "/dashboard.xhtml", "/deposit.xhtml", "/withdraw.xhtml", 
    "/budget.xhtml", "/account.xhtml", "/transactions.xhtml", "/transfer.xhtml", "/piggybank.xhtml", "/request.xhtml", "/send.xhtml", "/friends.xhtml"
})
public class SecurityFilter implements Filter {

    /**
     * Filters requests to ensure the user is authenticated.
     * 
     * @param request  the ServletRequest object
     * @param response the ServletResponse object
     * @param chain    the FilterChain object
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request; //Casting the request to an HttpServletRequest
        HttpServletResponse httpResponse = (HttpServletResponse) response; //Casting the response to an HttpServletResponse
        HttpSession session = httpRequest.getSession(false); //Getting the session from the request
        
        String loginURL = httpRequest.getContextPath() + "/login.xhtml"; //Getting the login page URL
        String indexURL = httpRequest.getContextPath() + "/index.xhtml"; //Getting the index page URL
        String requestURI = httpRequest.getRequestURI(); //Getting the request URI
        
        boolean isLoggedIn = (session != null && session.getAttribute("userBean") != null);
        boolean isLoginPage = requestURI.equals(loginURL);
        boolean isIndexPage = requestURI.equals(indexURL);
        boolean isResourceRequest = requestURI.contains("javax.faces.resource");
        boolean isVerificationPage = requestURI.contains("verify.xhtml");
        boolean isPasswordRecoveryPage = requestURI.contains("passwordRecovery.xhtml");
        
        //If the user is logged in or in a page that doesn't require auth: allow the request
        if (isLoggedIn || isLoginPage || isIndexPage || isResourceRequest || isVerificationPage || isPasswordRecoveryPage ) chain.doFilter(request, response);
        else httpResponse.sendRedirect(loginURL); //If the user is not logged in and is accesing a page that requires auth, redirect to the login page
    }
}