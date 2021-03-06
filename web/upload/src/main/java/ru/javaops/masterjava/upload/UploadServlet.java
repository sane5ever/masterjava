package ru.javaops.masterjava.upload;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.javaops.masterjava.common.web.ThymeleafListener.engine;

@Slf4j
@WebServlet(urlPatterns = "/", loadOnStartup = 1)
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10) // 10 MB in memory limit
public class UploadServlet extends HttpServlet {
    private static final int CHUNK_SIZE = 2000;

    private final PayloadProcessor processor = new PayloadProcessor();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        out(request, response, "", CHUNK_SIZE);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String message;
        int chunkSize = CHUNK_SIZE;
        try {
            chunkSize = Integer.parseInt(request.getParameter("chunkSize"));
            if (chunkSize < 1) {
                message = "Chunk size must be > 1";
            } else {
                // http://docs.oracle.com/javaee/6/tutorial/doc/glraq.html
                var filePart = request.getPart("fileToUpload");
                try (var in = filePart.getInputStream()) {
                    var failed = processor.process(in, chunkSize);
                    log.info("Failed users: " + failed);
                    final var webContext = new WebContext(request, response, request.getServletContext(),
                            request.getLocale(), ImmutableMap.of("users", failed));
                    engine.process("result", webContext, response.getWriter());
                    return;
                }
            }
        } catch (XMLStreamException | JAXBException e) {
            log.info(e.getMessage(), e);
            message = e.toString();
        }
        out(request, response, message, chunkSize);
    }

    private void out(HttpServletRequest request, HttpServletResponse response, String message, int chunkSize)
            throws IOException {
        response.setCharacterEncoding(UTF_8.name());
        final var webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(),
                ImmutableMap.of("message", message, "chunkSize", chunkSize));
        engine.process("upload", webContext, response.getWriter());
    }
}
