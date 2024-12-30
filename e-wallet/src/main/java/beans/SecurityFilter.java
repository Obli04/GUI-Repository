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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(filterName = "SecurityFilter", urlPatterns = {
    "/dashboard.xhtml", "/deposit.xhtml", "/withdraw.xhtml", 
    "/budget.xhtml", "/transactions.xhtml", "/transfer.xhtml", "/piggybank.xhtml"
})
public class SecurityFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String loginURL = httpRequest.getContextPath() + "/login.xhtml";
        String requestURI = httpRequest.getRequestURI();
        
        boolean isLoggedIn = (session != null && session.getAttribute("userBean") != null);
        boolean isLoginPage = requestURI.equals(loginURL);
        boolean isResourceRequest = requestURI.contains("javax.faces.resource");
        boolean isVerificationPage = requestURI.contains("verify.xhtml");
        boolean isPasswordRecoveryPage = requestURI.contains("passwordRecovery.xhtml");
        boolean is2FApage = requestURI.contains("2fa.xhtml");
        
        logger.debug("Request URI: {}", requestURI);
        logger.debug("Is Logged In: {}", isLoggedIn);
        
        if (isLoggedIn || isLoginPage || isResourceRequest || isVerificationPage || isPasswordRecoveryPage || is2FApage) {
            logger.debug("Access granted to: {}", requestURI);
            chain.doFilter(request, response);
        } else {
            logger.warn("Access denied to: {}. Redirecting to login page.", requestURI);
            httpResponse.sendRedirect(loginURL);
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
        logger.info("SecurityFilter initialized.");
    }
    
    @Override
    public void destroy() {
        // Cleanup code if needed
        logger.info("SecurityFilter destroyed.");
    }
}