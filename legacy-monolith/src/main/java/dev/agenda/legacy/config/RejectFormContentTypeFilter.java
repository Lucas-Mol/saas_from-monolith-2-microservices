package dev.agenda.legacy.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

class RejectFormContentTypeFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(), HttpMethod.TRACE.name());

    private static final Set<MediaType> FORM_TYPES = Set.of(MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.MULTIPART_FORM_DATA, MediaType.TEXT_PLAIN);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (!SAFE_METHODS.contains(request.getMethod()) && contentType != null && isFormType(contentType)) {
            response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isFormType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return FORM_TYPES.stream().anyMatch(type -> type.includes(mediaType));
        } catch (InvalidMediaTypeException ex) {
            return true;
        }
    }
}
