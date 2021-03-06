package ru.javaops.masterjava.service.mail.persist;

import com.bertoncelj.jdbi.entitymapper.EntityMapperFactory;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import ru.javaops.masterjava.persist.dao.AbstractDao;

import java.util.Date;
import java.util.List;

@RegisterMapperFactory(EntityMapperFactory.class)
public abstract class MailCaseDao implements AbstractDao {
    @SqlUpdate("TRUNCATE mail_history")
    @Override
    public abstract void clean();

    @SqlQuery("SELECT * FROM mail_history WHERE dateTime >= :after ORDER BY datetime DESC")
    public abstract List<MailCase> getAfter(@Bind("after") Date date);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO mail_history (list_to, list_cc, subject, state, datetime) VALUES (:listTo, :listCc, :subject, :state, :dateTime)")
    public abstract int insert(@BindBean MailCase mails);
}
