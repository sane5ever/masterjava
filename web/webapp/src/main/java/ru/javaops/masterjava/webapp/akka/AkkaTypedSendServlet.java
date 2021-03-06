package ru.javaops.masterjava.webapp.akka;

import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.service.mail.MailRemoteService;
import ru.javaops.masterjava.service.mail.util.MailUtils.MailObject;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.javaops.masterjava.util.Exceptions.wrap;
import static ru.javaops.masterjava.webapp.WebUtil.*;
import static ru.javaops.masterjava.webapp.akka.AkkaWebappListener.akkaActivator;

@Slf4j
@WebServlet(value = "/sendAkkaTyped", loadOnStartup = 1, asyncSupported = true)
@MultipartConfig
public class AkkaTypedSendServlet extends HttpServlet {
    private MailRemoteService mailSerivice;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mailSerivice = akkaActivator.getTypedRef(MailRemoteService.class,
                "akka.tcp://MailService@127.0.0.1:2553/user/mail-remote-service");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(UTF_8.name());
        doAsync(response, () -> process(request));
    }

    private void process(HttpServletRequest request) throws IOException, ServletException {
        var mailObject = createMailObject(request);
        final var context = request.startAsync();
        context.start(wrap(() -> {
            doAndWriteResponse((HttpServletResponse) context.getResponse(), () -> sendAkka(mailObject));
            context.complete();
        }));
    }

    private String sendAkka(MailObject mailObject) throws Exception {
        var future = mailSerivice.sendBulk(mailObject);
        log.info("Receive future, waiting result...");
        var groupResult = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        return groupResult.toString();
    }
}
