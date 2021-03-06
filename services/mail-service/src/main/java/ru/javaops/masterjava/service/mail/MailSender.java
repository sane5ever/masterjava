package ru.javaops.masterjava.service.mail;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import ru.javaops.masterjava.ExceptionType;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.service.mail.persist.MailCase;
import ru.javaops.masterjava.service.mail.persist.MailCaseDao;
import ru.javaops.masterjava.web.WebStateException;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class MailSender {
    private static final MailCaseDao MAIL_CASE_DAO = DBIProvider.getDao(MailCaseDao.class);

    static MailResult sendTo(Addressee to, String subject, String body, List<Attachment> attachments) throws WebStateException {
        val state = sendToGroup(Set.of(to), Set.of(), subject, body, attachments);
        return new MailResult(to.getEmail(), state);
    }

    static String sendToGroup(Set<Addressee> to, Set<Addressee> cc, String subject, String body, List<Attachment> attachments) throws WebStateException {
        log.info("Send mail to '" + to + "' cc '" + cc + "' subject '" + subject + '\''
                + (log.isDebugEnabled() ? "\nbody=" + body : ""));
        var state = MailResult.OK;
        try {
            val email = prepareToSend(to, cc, subject, body, attachments);
            email.send();
        } catch (EmailException | UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            state = e.getMessage();
        }
        saveMailHistory(MailCase.of(to, cc, subject, state));
        log.info("Sent with state: " + state);
        return state;
    }

    private static Email prepareToSend(Set<Addressee> to, Set<Addressee> cc, String subject, String body, List<Attachment> attachments) throws EmailException, UnsupportedEncodingException {
        final var email = MailConfig.createHtmlEmail();
        email.setSubject(subject);
        email.setHtmlMsg(body);
        for (var addressee : to) {
            email.addTo(addressee.getEmail(), addressee.getName());
        }
        for (var addressee : cc) {
            email.addCc(addressee.getEmail(), addressee.getName());
        }
        for (var attachment : attachments) {
            email.attach(attachment.getDataHandler().getDataSource(), encodeWord(attachment.getName()), null);
        }
        // https://yandex.ru/blog/company/66296
        email.setHeaders(Map.of("List-Unsubscribe", "<mailto:sane4ever@ya.ru?subject=Unsubscribe&body=Unsubscribe>"));
        return email;
    }

    private static String encodeWord(String word) throws UnsupportedEncodingException {
        return word == null ? null
                : MimeUtility.encodeWord(word, UTF_8.name(), null);
    }

    private static void saveMailHistory(MailCase mailCase) throws WebStateException {
        try {
            MAIL_CASE_DAO.insert(mailCase);
        } catch (Exception e) {
            log.error("Mail history saving exception", e);
            throw new WebStateException(e, ExceptionType.DATA_BASE);
        }
    }
}
