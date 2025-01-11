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

/**
 * A filter that provides basic CSRF protection by logging request details.
 * This filter is applied to all requests with URLs ending in ".xhtml".
 * 
 * @author Davide Scaccia
 */
@WebFilter(filterName = "CSRFFilter", urlPatterns = {"*.xhtml"})
public class CSRFFilter implements Filter {
    
    /**
     * Filters requests to log details and check for CSRF vulnerabilities.
     *
     * @param request  the ServletRequest object contains the client's request
     * @param response the ServletResponse object contains the filter's response
     * @param chain    the FilterChain for invoking the next filter or the resource
     * @throws IOException      if an I/O error occurs during the processing
     * @throws ServletException if a servlet error occurs during the processing
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        boolean isVerificationPage = requestURI.contains("verify.xhtml");
        
        //Log request details before processing
        System.out.println("\n=== Request Details ===");
            System.out.println("Request URI: " + requestURI);
            System.out.println("Method: " + httpRequest.getMethod());
            System.out.println("Is Ajax: " + isAjaxRequest(httpRequest));
            System.out.println("Is Verification: " + isVerificationPage);
            System.out.println("Session ID: " + httpRequest.getSession(true).getId());
        
        //Process the request
        chain.doFilter(request, response);
        
        System.out.println("=== Request Completed ===\n");
    }

    /**
     * Determines if the request is an Ajax request.
     *
     * @param request the HttpServletRequest object
     * @return true if the request is an Ajax request, false otherwise
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
               request.getParameter("javax.faces.partial.ajax") != null;
    }
}