package ru.javaops.masterjava.upload;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.dao.UserGroupDao;
import ru.javaops.masterjava.persist.model.City;
import ru.javaops.masterjava.persist.model.Group;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserGroup;
import ru.javaops.masterjava.persist.model.type.UserFlag;
import ru.javaops.masterjava.upload.PayloadProcessor.FailedEmails;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ru.javaops.masterjava.upload.PayloadProcessor.jaxbParser;

@Slf4j
public class UserProcessor {
    private static final int NUMBER_THREADS = 4;

    private static UserDao userDao = DBIProvider.getDao(UserDao.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);

    /*
     * return failed users chunks
     */
    public List<FailedEmails> process(final StaxStreamProcessor processor, Map<String, Group> groups, Map<String, City> cities, int chunkSize) throws XMLStreamException, JAXBException {
        log.info("Start processing with chunkSize=" + chunkSize);

        var chunkFutures = new LinkedHashMap<String, Future<List<String>>>(); // ordered map (emailRange -> chunk future)
        int id = userDao.getSeqAndSkip(chunkSize);
        List<User> userChunk = new ArrayList<>(chunkSize);
        List<UserGroup> userGroupChunk = new ArrayList<>(chunkSize);

        val unmarshaller = jaxbParser.createUnmarshaller();
        var failed = new ArrayList<FailedEmails>();

        while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
            // unmarshaller doesn't get refs
            val cityRef = processor.getAttribute("city");
            val groupRefs = processor.getAttribute("groupRefs");
            var xmlUser = unmarshaller.unmarshal(processor.getReader(), ru.javaops.masterjava.xml.schema.User.class);
            String email = xmlUser.getEmail();
            if (cities.get(cityRef) == null) {
                failed.add(new FailedEmails(email, "City '" + cityRef + "' is not present in DB"));
            } else {
                List<String> groupNames = groupRefs == null ? List.of() : Splitter.on(' ').splitToList(groupRefs);
                if (!groups.keySet().containsAll(groupNames)) {
                    failed.add(new FailedEmails(email, "One of group from '" + groupRefs + "' is not present in DB"));
                } else {
                    val user = new User(id++, xmlUser.getValue(), xmlUser.getEmail(), UserFlag.valueOf(xmlUser.getFlag().value()), cityRef);
                    userChunk.add(user);
                    var userGroups = StreamEx.of(groupNames).map(name -> new UserGroup(user.getId(), groups.get(name).getId())).toList();
                    userGroupChunk.addAll(userGroups);
                    if (userChunk.size() == chunkSize) {
                        addChunkFutures(chunkFutures, userChunk, userGroupChunk);
                        userChunk = new ArrayList<>(chunkSize);
                        userGroupChunk = new ArrayList<>(chunkSize);
                        id = userDao.getSeqAndSkip(chunkSize);
                    }
                }
            }
        }

        if (!userChunk.isEmpty()) {
            addChunkFutures(chunkFutures, userChunk, userGroupChunk);
        }

        var allAlreadyPresents = new ArrayList<String>();
        chunkFutures.forEach((emailRange, task) -> {
            try {
                List<String> alreadyPresentsInChunk = task.get();
                log.info("{} successfully executed with already presents: {}", emailRange, alreadyPresentsInChunk);
                allAlreadyPresents.addAll(alreadyPresentsInChunk);
            } catch (InterruptedException | ExecutionException e) {
                log.error(emailRange + " failed", e);
                failed.add(new FailedEmails(emailRange, e.toString()));
            }
        });
        if (!allAlreadyPresents.isEmpty()) {
            failed.add(new FailedEmails(allAlreadyPresents.toString(), "already presents"));
        }
        return failed;
    }

    private void addChunkFutures(Map<String, Future<List<String>>> chunkFutures, List<User> userChunk, List<UserGroup> userGroupChunk) {
        var emailRange = String.format("[%s-%s]", userChunk.get(0).getEmail(), userChunk.get(userChunk.size() - 1).getEmail());
        var submittedTask = executorService.submit(() -> {
            // https://www.programcreek.com/java-api-examples/index.php?api=org.skife.jdbi.v2.TransactionCallback
            List<String> alreadyPresentEmails = DBIProvider.getDBI().inTransaction((handle, status) -> {
                var tempUserDao = handle.attach(UserDao.class);
                var tempUserGroupDao = handle.attach(UserGroupDao.class);
                var alreadyPresentUsers = tempUserDao.insertAndGetConflictEmails(userChunk);
                var alreadyPresentIds = StreamEx.of(alreadyPresentUsers).map(User::getId).toSet();
                tempUserGroupDao.insertBatch(
                        StreamEx.of(userGroupChunk).filter(userGroup -> !alreadyPresentIds.contains(userGroup.getUserId())).toList());
                return StreamEx.of(alreadyPresentUsers).map(User::getEmail).toList();
            });

            // get GC clear chunk after insert
            userChunk.clear();
            userGroupChunk.clear();
            return alreadyPresentEmails;
        });
        chunkFutures.put(emailRange, submittedTask);
        log.info("Submit chunk: " + emailRange);
    }
}
