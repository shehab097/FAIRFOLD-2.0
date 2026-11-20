package com.fairfold.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Local/development mail implementation: prints the message (and any links)
 * to the console instead of sending real email.
 */
@Service
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("""
                [MAIL] to={}
                [MAIL] subject={}
                [MAIL] body:
                {}
                """, to, subject, body);
    }
}
