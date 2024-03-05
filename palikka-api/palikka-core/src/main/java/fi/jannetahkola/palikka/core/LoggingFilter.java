package fi.jannetahkola.palikka.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.UnaryOperator;

// Source https://vkuzel.com/log-requests-and-responses-including-body-in-spring-boot
@Slf4j
public class LoggingFilter extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        ContentCachingRequestWrapper requestWrapper = requestWrapper(servletRequest);
        ContentCachingResponseWrapper responseWrapper = responseWrapper(servletResponse);

        filterChain.doFilter(requestWrapper, responseWrapper);

        logRequest(requestWrapper);
        logResponse(responseWrapper);
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder builder = new StringBuilder();
        builder.append(headersToString(Collections.list(request.getHeaderNames()), request::getHeader));
        builder.append(new String(request.getContentAsByteArray()));
        log.info("Request: {}", builder);
    }

    private void logResponse(ContentCachingResponseWrapper response) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(headersToString(response.getHeaderNames(), response::getHeader));
        builder.append(new String(response.getContentAsByteArray()));
        log.info("Response: {}", builder);
        response.copyBodyToResponse();
    }

    private String headersToString(Collection<String> headerNames, UnaryOperator<String> headerValueResolver) {
        StringBuilder builder = new StringBuilder();
        for (String headerName : headerNames) {
            String header = headerValueResolver.apply(headerName);
            builder.append("%s=%s".formatted(headerName, header)).append("\n");
        }
        return builder.toString();
    }

    private ContentCachingRequestWrapper requestWrapper(ServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper requestWrapper) {
            return requestWrapper;
        }
        return new ContentCachingRequestWrapper((HttpServletRequest) request);
    }

    private ContentCachingResponseWrapper responseWrapper(ServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper responseWrapper) {
            return responseWrapper;
        }
        return new ContentCachingResponseWrapper((HttpServletResponse) response);
    }
}
