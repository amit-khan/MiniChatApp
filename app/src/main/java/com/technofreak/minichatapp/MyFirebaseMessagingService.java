package com.technofreak.minichatapp;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if(remoteMessage.getNotification()!=null){
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
//            String title = remoteMessage.getData().get("title");
//            String body = remoteMessage.getData().get("body");

            NotificationHelper.displayNotification(getApplicationContext(),title,body);
        }
    }
}
