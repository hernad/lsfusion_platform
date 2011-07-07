package platform.server.mail;


import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.server.logics.ServerResourceBundle;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.util.*;

public class EmailSender {
    final static Logger logger = Logger.getLogger(EmailSender.class);

    MimeMessage message;
    Multipart mp = new MimeMultipart();
    Properties mailProps = new Properties();
    String userName;
    String password;
    Map<String,Message.RecipientType> emails = new HashMap<String, Message.RecipientType>();

    public static class AttachmentProperties {
        public String fileName;
        public String attachmentName;
        public EmailActionProperty.Format format;

        public AttachmentProperties(String fileName, String attachmentName, EmailActionProperty.Format format) {
            this.fileName = fileName;
            this.attachmentName = attachmentName;
            this.format = format;
        }
    }

    public EmailSender(String smtpHost, String fromAddress, Map<String,Message.RecipientType> targets) {
        //mailProps.setProperty("mail.debug", "true");
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.setProperty("mail.from", fromAddress);
        emails = targets;
    }

    public EmailSender(String smtpHost, String smtpPort, String fromAddress, String userName, String password, Map<String,Message.RecipientType> targets) {
        this(smtpHost, fromAddress, targets);

        if (!smtpPort.isEmpty()) {
            mailProps.put("mail.smtp.port", smtpPort);
            mailProps.setProperty("mail.smtp.starttls.enable", "true");
            if (smtpPort.equals("465")) {
                mailProps.put("mail.smtp.socketFactory.port", smtpPort);
                mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
        }

        if (!userName.isEmpty() && !password.isEmpty()) {
            mailProps.setProperty("mail.smtp.auth", "true");
            this.userName = userName;
            this.password = password;
        }
    }

    private Session getSession() {
        if (mailProps.containsKey("mail.smtp.auth") && mailProps.getProperty("mail.smtp.auth").equals("true")) {
            return Session.getInstance(mailProps, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(userName, password));
                }
            });
        } else {
            return Session.getInstance(mailProps, null);
        }
    }

    private void setMessageHeading() throws MessagingException {
        message = new MimeMessage(getSession());
        message.setFrom();
        message.setSentDate(new java.util.Date());
        setRecipients(emails);
    }


    public void setRecipients(Map<String,Message.RecipientType> targets) throws MessagingException {
        for (Map.Entry<String, Message.RecipientType> target : targets.entrySet()) {
            message.addRecipients(target.getValue(), target.getKey());
        }
    }

    public void setText(String text) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(text, "text/html; charset=utf-8")));
        textPart.setDisposition(Part.INLINE);
        mp.addBodyPart(textPart);
    }

    private String getMimeType(EmailActionProperty.Format format) {
        switch (format) {
            case PDF:
                return "application/pdf";
            case DOCX:
                return "application/msword";
            case RTF:
                return "text/rtf";
            default:
                return "text/html";
        }
    }

    public void attachFile(AttachmentProperties props) throws MessagingException, IOException {
        FileDataSource fds = new FileDataSource(props.fileName);
        ByteArrayDataSource dataSource = new ByteArrayDataSource(fds.getInputStream(), getMimeType(props.format));
        attachFile(dataSource, props.attachmentName);
    }

    public void attachFile(byte[] buf, String attachmentName) throws MessagingException {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(buf, getMimeType(EmailActionProperty.Format.PDF));
        attachFile(dataSource, attachmentName);
    }

    private void attachFile(DataSource source, String attachmentName) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();
        filePart.setDataHandler(new DataHandler(source));
        filePart.setFileName(attachmentName);
        mp.addBodyPart(filePart);
    }

    private String createInlinePart(List<String> inlineForms) throws IOException {
        String result = "";
        for (String path : inlineForms) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
            while (in.ready()) {
                result += in.readLine();
            }
        }
        if (result.equals("")) {
            result = ServerResourceBundle.getString("mail.you.have.received.reports");
        }
        return result;
    }

    public void sendMail(String subject, List<String> inlineForms, List<AttachmentProperties> attachmentForms, Map<ByteArray, String> files) throws MessagingException, IOException {
        assert inlineForms != null && attachmentForms != null && files != null;

        setMessageHeading();
        message.setSubject(subject, "utf-8");

        setText(createInlinePart(inlineForms));

        for (AttachmentProperties formProps : attachmentForms) {
            attachFile(formProps);
        }
        for (Map.Entry<ByteArray, String> entry : files.entrySet()) {
            attachFile(entry.getKey().array, entry.getValue());
        }

        message.setContent(mp);
        sendMail(message, subject);
    }

    private void sendMail(final MimeMessage message, final String subject) {
        new Thread() {
            public void run() {

                String messageInfo = subject.trim();
                try {
                    Address[] addressesTo = message.getRecipients(MimeMessage.RecipientType.TO);
                    if (addressesTo == null || addressesTo.length == 0) {
                        logger.error(ServerResourceBundle.getString("mail.failed.to.send.mail")+" " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
                        throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail") + " " + messageInfo + " : "+ServerResourceBundle.getString("mail.recipient.not.specified"));
                    }
                    messageInfo += " "+ServerResourceBundle.getString("mail.recipients")+" : " + BaseUtils.toString(addressesTo, ",");
                } catch (MessagingException me) {
                    messageInfo += " "+ServerResourceBundle.getString("mail.failed.to.get.list.of.recipients")+" " + me.toString();
                }

                boolean send = false;
                int count = 0;
                while (!send) {
                    send = true;
                    count++;
                    try {
                        Transport.send(message);
                    } catch (MessagingException e) {
                        if (count < 40) {
                            logger.info(ServerResourceBundle.getString("mail.unsuccessful.attempt.to.send.mail") + " " + messageInfo);
                            send = false;
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException e1) {
                            }
                        } else {
                            logger.error(ServerResourceBundle.getString("mail.failed.to.send.mail") + " " + messageInfo);
                            throw new RuntimeException(ServerResourceBundle.getString("mail.error.send.mail")+" " + messageInfo, e);
                        }
                    }
                }
                logger.info(ServerResourceBundle.getString("mail.successful.mail.sending") + messageInfo);
            }
        }.start();
    }
}
