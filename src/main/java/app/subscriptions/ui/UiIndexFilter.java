package app.subscriptions.ui;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * onno 2.2.0 maps {@code /ui} to a static handler that looks for {@code "."} and returns 404.
 * Serve the SPA shell here and inject the UI base path.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UiIndexFilter extends OncePerRequestFilter {

    private final byte[] indexHtml;

    public UiIndexFilter() throws IOException {
        ClassPathResource source = new ClassPathResource("static/ui/index.html");
        String html = new String(source.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        html = html.replace("\"__ONNO_BASE_PATH__\"", "\"/ui\"");
        this.indexHtml = html.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String path = request.getRequestURI();
            if ("/ui".equals(path) || "/ui/".equals(path)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getOutputStream().write(indexHtml);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
