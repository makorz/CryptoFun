package app.makorz.cryptofun.services;

import android.util.Log;

import com.example.cryptofun.BuildConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void sendEmail(String username, String password, String topic, String body) {
        executorService.execute(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", BuildConfig.SMTP_HOST); // use your email provider's SMTP host
            props.put("mail.smtp.port", Integer.toString(BuildConfig.SMTP_PORT)); // use your email provider's SMTP port
            props.put("mail.smtp.socketFactory.port", Integer.toString(BuildConfig.SMTP_PORT));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

            Log.e("MAIL", "start");

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                            return new javax.mail.PasswordAuthentication(username, password);
                        }
                    });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(BuildConfig.SMTP_USERNAME)); // replace with your email
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("moquiteush@gmail.com")); // replace with recipient email
                message.setSubject(topic);
                message.setText(body);

                Transport.send(message);
                Log.e("MAIL", "email sent");
                System.out.println("Email sent successfully.");

            } catch (MessagingException e) {
                Log.e("MAIL", "File write failed: " + e);
                throw new RuntimeException(e);
            }
        });
    }
}


