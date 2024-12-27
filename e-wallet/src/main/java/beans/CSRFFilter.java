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

@WebFilter(filterName = "CSRFFilter", urlPatterns = {"*.xhtml"})
public class CSRFFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        boolean isVerificationPage = requestURI.contains("verify.xhtml");
        
        // Log request details before processing
        System.out.println("\n=== Request Details ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("Is Ajax: " + isAjaxRequest(httpRequest));
        System.out.println("Is Verification: " + isVerificationPage);
        System.out.println("Session ID: " + httpRequest.getSession(true).getId());
        
        // Process the request
        chain.doFilter(request, response);
        
        System.out.println("=== Request Completed ===\n");
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
               request.getParameter("javax.faces.partial.ajax") != null;
    }
}