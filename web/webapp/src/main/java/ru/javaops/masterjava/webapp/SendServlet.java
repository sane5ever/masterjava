package ru.javaops.masterjava.webapp;

import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.service.mail.GroupResult;
import ru.javaops.masterjava.service.mail.MailWSClient;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@WebServlet("/send")
public class SendServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String result;
        try {
            log.info("Start sending");
            req.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String users = req.getParameter("users");
            String subject = req.getParameter("subject");
            String body = req.getParameter("body");
            GroupResult groupResult = MailWSClient.sendBulk(MailWSClient.split(users), subject, body);
            result = groupResult.toString();
            log.info("Processing finished with result: {}", result);
        } catch (Exception e) {
            log.error("Processing failed", e);
            result = e.toString();
        }
        resp.getWriter().write(result);
    }
}