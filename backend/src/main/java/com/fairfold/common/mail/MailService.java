package com.fairfold.common.mail;

public interface MailService {

    void send(String to, String subject, String body);
}
