package ru.javaops.masterjava.webapp;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.service.mail.GroupResult;
import ru.javaops.masterjava.service.mail.MailWSClient;
import ru.javaops.masterjava.service.mail.util.MailUtils;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@MultipartConfig
@WebServlet("/sendSoap")
@Slf4j
public class SoapSendServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String result;
        try {
            log.info("Start sending");
            req.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
            resp.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
            String users = req.getParameter("users");
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            Part filePart = req.getPart("attach");
            GroupResult groupResult = MailWSClient.sendBulk(
                    MailUtils.split(users), subject, body,
                    filePart == null ? null : ImmutableList.of(
                            MailUtils.getAttachment(filePart.getSubmittedFileName(), filePart.getInputStream())
                    )
            );
            result = groupResult.toString();
            log.info("Processing finished with result: {}", result);
        } catch (Exception e) {
            log.error("Processing failed", e);
            result = e.toString();
        }
        resp.getWriter().write(result);
    }
}