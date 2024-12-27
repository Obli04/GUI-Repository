package beans;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter(filterName = "SecurityFilter", urlPatterns = {"/dashboard.xhtml"})
public class SecurityFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String loginURL = httpRequest.getContextPath() + "/login.xhtml";
        String requestURI = httpRequest.getRequestURI();
        
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        boolean isLoginPage = requestURI.equals(loginURL);
        boolean isResourceRequest = requestURI.contains("javax.faces.resource");
        boolean isVerificationPage = requestURI.contains("verify.xhtml");
        
        if (isLoggedIn || isLoginPage || isResourceRequest || isVerificationPage) {
            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect(loginURL);
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }
    
    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}