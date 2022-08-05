package com.moesif.servlet.example;

import com.moesif.servlet.wrappers.LoggingHttpServletRequestWrapper;
import com.moesif.servlet.wrappers.LoggingHttpServletResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class LogFilter implements Filter {

    private static final Logger logger = Logger.getLogger(LogFilter.class.toString());

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("LogFilter initialized");
        String appId = filterConfig.getInitParameter("application-id");
    }

    @Override
    public void destroy() {
        logger.info("LogFilter destroyed");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        logger.info("doFilter");

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            logger.info("Non-HTTP request");
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        LoggingHttpServletRequestWrapper requestWrapper = new LoggingHttpServletRequestWrapper(httpRequest);
        LoggingHttpServletResponseWrapper responseWrapper = new LoggingHttpServletResponseWrapper(httpResponse);

        // pass to next step in the chain.
        filterChain.doFilter(requestWrapper, responseWrapper);
    }
}
