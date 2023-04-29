package com.filtershekanha.tlsfragmenter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {

    public LoggingConfiguration() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{HH:mm:ss.SSS} [%-5level] - %msg%n");
        encoder.setContext(loggerContext);
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setName("console");
        consoleAppender.setEncoder(encoder);
        consoleAppender.setContext(loggerContext);
        consoleAppender.start();

        loggerContext.getLogger("ROOT").detachAndStopAllAppenders();
        loggerContext.getLogger("ROOT").addAppender(consoleAppender);
    }
}
