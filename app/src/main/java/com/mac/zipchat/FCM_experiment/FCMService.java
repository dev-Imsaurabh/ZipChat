package com.mac.zipchat.FCM_experiment;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mac.zipchat.MainActivity;
import com.mac.zipchat.R;
import com.mac.zipchat.map_location.PrefConfig;

import java.util.Random;

public class FCMService extends FirebaseMessagingService {
    private final String CHANNEL_ID = "trash_id";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);


        String getHelpPref = PrefConfig.GetPref(this, "helpPref", "helpmessage");
        String[] splitMessageByComma = getHelpPref.split(",");
        String uid = null;
        try {
            String name = splitMessageByComma[0];
            String displayMessage = splitMessageByComma[1];
            String phoneNumber = splitMessageByComma[2];
            uid = splitMessageByComma[3];
            String timeStamp = splitMessageByComma[4];
            String latlang = splitMessageByComma[5];
        } catch (Exception e) {
            e.printStackTrace();
        }

        String fullMessage = message.getData().get("message");




        if (fullMessage.contains("finding")) {

            String activeTime =PrefConfig.GetPref(this,"userActive","active");
            String[] preference = fullMessage.split(",");
            String gender =PrefConfig.GetPref(this,"userPref","usergender");

            if(preference[6].equals(gender)){

                if(!activeTime.equals("error")){

                    double checkDifference = System.currentTimeMillis()-Double.parseDouble(activeTime);

                    if(checkDifference>180000){
                        PrefConfig.SetPref(this,"userHelp","user","true");

                    }



                    if(PrefConfig.GetPref(this,"userHelp","user").equals("true")||PrefConfig.GetPref(this,"userHelp","user").equals("error")){
                        SetMessagePref(fullMessage,message);

                    }


                }else{
                    SetMessagePref(fullMessage,message);



                }


            }


        } else if (fullMessage.equals("cancel" + uid)) {


            SharedPreferences preferences = getSharedPreferences("helpPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/" + "cancel" + uid);
            SharedPreferences preferences1 = getSharedPreferences("tempTopic",MODE_PRIVATE);
            SharedPreferences.Editor editor1 = preferences1.edit();
            editor1.clear();
            editor1.commit();
            PrefConfig.SetPref(this,"userHelp","user","true");

        } else if (fullMessage.contains("chat")) {
            PrefConfig.SetPref(this, "rPref", "rece", fullMessage);
        }else if(fullMessage.contains("accepted")){
            PrefConfig.SetPref(this,"acceptStatus","accept","1");
//            String[] split = fullMessage.split(",");


        }else if(fullMessage.contains("time")){

            PrefConfig.SetPref(this,"onlineStatus","online",String.valueOf(System.currentTimeMillis()));

        }else if (fullMessage.contains("ignore")){
            PrefConfig.SetPref(this,"reqReject","reject","1");

        }


    }

    private void SetMessagePref(String fullMessage, RemoteMessage message) {

        String[] splitMessageByComma1 = fullMessage.split(",");
        String name1 = splitMessageByComma1[0];
        String displayMessage1 = splitMessageByComma1[1];
        String phoneNumber1 = splitMessageByComma1[2];
        String uid1 = splitMessageByComma1[3];
        String timeStamp1 = splitMessageByComma1[4];
        String latlang1 = splitMessageByComma1[5];

        PrefConfig.SetPref(this, "helpPref", "helpmessage", fullMessage);

        if (!PrefConfig.GetPref(this, "tempTopic", "topic").equals("error")) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/" + "cancel" + PrefConfig.GetPref(this, "tempTopic", "topic"));
            PrefConfig.SetPref(this, "tempTopic", "topic", uid1);
            FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + "cancel" + uid1);
        } else {
//                    ShowHelpDialog();

            FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + "cancel" + uid1);
            PrefConfig.SetPref(this, "tempTopic", "topic", uid1);

//                ShowHelpDialog();

        }


//            FirebaseMessaging.getInstance().subscribeToTopic("/topics/"+"cancel"+uid);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        Double checkDifference = System.currentTimeMillis() - Double.parseDouble(timeStamp1);

        if (checkDifference < 180000) {

            if (!user.getUid().equals(uid1)) {
                ShowNotification(displayMessage1, message);
//                    ShowHelpDialog();

            }

        }
    }

    private void ShowNotification(String displayMessage, RemoteMessage message) {
//        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.help_me_please);
//        mp.start();

        Intent intent = new Intent(this, MainActivity.class);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = new Random().nextInt();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification;
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.getData().get("title"))
                .setContentText(displayMessage)
                .setSmallIcon(R.drawable.noti_icon_5)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(notificationId, notification);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(NotificationManager notificationManager) {

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "trashName", NotificationManager.IMPORTANCE_HIGH);

        notificationChannel.setDescription("MyDesc");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.WHITE);

        notificationManager.createNotificationChannel(notificationChannel);

    }


    private void ShowHelpDialog() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getBaseContext().startActivity(intent);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,0 /* request code */, intent,PendingIntent.FLAG_UPDATE_CURRENT);


    }


}
