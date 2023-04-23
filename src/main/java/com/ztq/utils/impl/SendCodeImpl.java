package com.ztq.utils.impl;

import com.ztq.utils.SendCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Component
public class SendCodeImpl implements SendCode {

    @Autowired
    JavaMailSender javaMailSender;
    @Value("${sendCode.from}")
    private String from;

    @Value("${sendCode.subject}")
    private String subject;


    @Override
    public void sendCode(String to, String text) {

        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg);
        try {
            helper.setFrom(from);
            helper.setTo(to);
            //设置true属性表示解析正文的html格式
            helper.setText(text, true);
            helper.setSubject(subject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        javaMailSender.send(msg);

    }
}
