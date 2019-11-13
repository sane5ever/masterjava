package ru.javaops.masterjava.service.mail.listeners;

import lombok.extern.slf4j.Slf4j;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@Slf4j
@WebListener
public class JmsMailListener implements ServletContextListener {
    private Thread listenerThread = null;
    private QueueConnection connection;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            var queueReceiver = initReceiver();
            log.info("Listen to JMS messages...");
            listenerThread = new Thread(() -> {
                try {
                    listenerCircle(queueReceiver);
                } catch (Exception e) {
                    log.error("Receiving messaged failed: " + e.getMessage(), e);
                }
            });
            listenerThread.start();
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }

    private QueueReceiver initReceiver() throws JMSException, NamingException {
        var initContext = new InitialContext();
        var connectionFactory = (QueueConnectionFactory) initContext.lookup("java:comp/env/jms/ConnectionFactory");
        connection = connectionFactory.createQueueConnection();
        var queueSession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        var queue = (Queue) initContext.lookup("java:comp/env/jms/queue/MailQueue");
        var queueReceiver = queueSession.createReceiver(queue);
        connection.start();
        return queueReceiver;
    }

    private void listenerCircle(QueueReceiver receiver) throws JMSException {
        while (!Thread.interrupted()) {
            var message = receiver.receive();
            // TODO implement mail sending
            if (message instanceof TextMessage) {
                var textMsg = (TextMessage) message;
                var text = textMsg.getText();
                log.info("Received TextMessage with text '{}'", text);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        closeConnection();
        interruptListenerThread();
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                log.warn("Couldn't close JMSConnection: " + e.getMessage(), e);
            }
        }
    }

    private void interruptListenerThread() {
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
