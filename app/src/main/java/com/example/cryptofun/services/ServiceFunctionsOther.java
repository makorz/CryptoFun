package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.cryptofun.R;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ServiceFunctionsOther {

    public static void writeToFile(String data, Context context, String fileName) {

        String nameOFLogFile;
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("dd");

        if (fileName.equals("result")) {
            nameOFLogFile = "result_" + df2.format(new Date(stamp.getTime())) + ".txt";
        } else if (fileName.equals("strategy")){
            nameOFLogFile = "StrategyTest.txt";
        } else {
            nameOFLogFile = "OrdersLog.txt";
        }


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(nameOFLogFile, Context.MODE_APPEND));
            outputStreamWriter.write(df.format(new Date(stamp.getTime())) + " " + data + "\n");
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e);
        }
    }

    public static void deleteFile(Context context, String fileName) {

        @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("dd");

        String nameOFLogFile;
        Timestamp stamp = new Timestamp(System.currentTimeMillis());

        if (fileName.equals("result")) {
            nameOFLogFile = "result_" + df2.format(new Date(stamp.getTime())) + ".txt";
        } else if (fileName.equals("strategy")){
            nameOFLogFile = "StrategyTest.txt";
        } else {
            nameOFLogFile = "OrdersLog.txt";
        }

        boolean deleted = context.deleteFile(nameOFLogFile);

        if (deleted) {
            Log.e("File delete result", "File delete success.");
        } else {
            Log.e("File delete result", "File delete failed.");
        }
    }



    public static void createNotificationWithText(String notification, String tag, Context context) {

        String CHANNEL_ID = "cryptoFun";

        NotificationChannel chan = new NotificationChannel(
                CHANNEL_ID,
                tag,
                NotificationManager.IMPORTANCE_HIGH);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        // Create a notification to indicate that the service is running.
        // You can customize the notification to display the information you want.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Order was made")
                .setSmallIcon(R.drawable.crypto_fun_logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Create a BigTextStyle object and set it to the builder
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(notification);
        builder.setStyle(bigTextStyle);

        Random random = new Random();
        int notificationId = random.nextInt(100);
        manager.notify(notificationId, builder.build());
    }

    public static Notification createNotificationSimple(String notification, String tag, Context context) {

        String CHANNEL_ID = "cryptoFun";

        NotificationChannel chan = new NotificationChannel(
                CHANNEL_ID,
                tag,
                NotificationManager.IMPORTANCE_LOW);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        // Create a notification to indicate that the service is running.
        // You can customize the notification to display the information you want.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("CryptoFun")
                .setContentText(notification)
                .setSmallIcon(R.drawable.crypto_fun_logo)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        return builder.build();
    }

    public static void sendEmail(String topic, String body) {
        final String username = "moquiteush@wp.pl"; // replace with your email
        final String password = "DUdP^i&T#jz7!T4"; // replace with your password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.wp.pl"); // use your email provider's SMTP host
        props.put("mail.smtp.port", "465"); // use your email provider's SMTP port

        Log.e("MAIL", "start");
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("moquiteush@wp.pl")); // replace with your email
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("mateusz.korzeniak@gmail.com")); // replace with recipient email
            message.setSubject(topic);
            message.setText(body);

            Transport.send(message);
            Log.e("MAIL", "email send");
            System.out.println("Email sent successfully.");

        } catch (MessagingException e) {
            Log.e("MAIL", "File write failed: " + e);
            throw new RuntimeException(e);
        }
    }

}
