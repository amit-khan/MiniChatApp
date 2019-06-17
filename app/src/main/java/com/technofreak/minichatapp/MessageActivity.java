package com.technofreak.minichatapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "minichatapp";
    public static final String CHANNEL_NAME = "minichatapp";
    public static final String CHANNEL_DESC = "minichatapp";

    private RecyclerView recyclerView;
    private EditText text_send;
    private TextView writing;
    private FirebaseUser fuser;
    private User friend;
    List<Chat> chatList;
    MessageAdapter mAdapter;
    DatabaseReference myref;

    ValueEventListener seenListener;
    MediaPlayer mp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        text_send = findViewById(R.id.text_send);
        writing = findViewById(R.id.writing);
        text_send.addTextChangedListener(searchTextWatcher);
        recyclerView = findViewById(R.id.recycler);

        friend = (User) getIntent().getSerializableExtra("user_to");
        setTitle("To: "+friend.username);
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        chatList = new ArrayList<>();
        readMessage(fuser.getUid(),friend.id);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);

        seenMessage(friend.id);
        writingListener();

        mp = MediaPlayer.create(getApplicationContext(),R.raw.send_tone);

        //Check OS version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

    }

    private void writingListener() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Active").child(friend.id);
        ref.child("writingto").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.getValue(String.class);
                if (s != null) {
                    if (s.equals(fuser.getUid())){
                        writing.setVisibility(View.VISIBLE);
                    } else{
                        writing.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private Timer timer;

    private TextWatcher searchTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable arg0) {
            // user typed: start the timer
            final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Active").child(fuser.getUid());
            ref.child("writingto").setValue(friend.id);
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Active").child(fuser.getUid());
                    ref.child("writingto").setValue("none");
                    // do your actual work here
                }
            }, 2500); // ms delay before the timer executes the „run“ method from TimerTask
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // nothing to do here
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // user is typing: reset already started timer (if existing)
            if (timer != null) {
                timer.cancel();
            }
        }
    };


    public void send(View view) {
        String message = text_send.getText().toString();
        if (TextUtils.isEmpty(message)){
            Toast.makeText(getApplicationContext(),"Can't send empty message",Toast.LENGTH_SHORT).show();
        } else {
            //play sound
            mp.start();
            //get current Timestamp
            String date = new SimpleDateFormat("HH:mm▪dd/MM/yy", Locale.getDefault()).format(new Date());
            Chat chat = new Chat(fuser.getUid(),friend.id,message,date,false);
            DatabaseReference myref1 = FirebaseDatabase.getInstance().getReference();
            myref1.child("Chats").push().setValue(chat);

            sendNotification(friend.token, chat);
            text_send.setText("");

        }
    }


    //Firebase cloud messaging push notification

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void sendNotification(final String regToken, final Chat chat) {
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json=new JSONObject();
                    JSONObject dataJson=new JSONObject();
                    dataJson.put("body",chat.getMessage());
                    dataJson.put("title",fuser.getDisplayName());
                    json.put("notification",dataJson);
                    json.put("to",regToken);
                    RequestBody body = RequestBody.create(JSON, json.toString());
                    Request request = new Request.Builder()
                            .header("Authorization","key=AIzaSyBysFhm8bqo3kYiEYf6-TZykvKmt9N4deI")
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    String finalResponse = response.body().string();
                }catch (Exception e){
                    //Log.d(TAG,e+"");
                }
                return null;
            }
        }.execute();
    }


    private void seenMessage(final String friend_id){
        myref = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(friend_id)){
                        HashMap<String, Object> seen = new HashMap<>();
                        seen.put("isseen",true);
                        snapshot.getRef().updateChildren(seen);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void readMessage(final String myid, final String friend_id){
        myref = FirebaseDatabase.getInstance().getReference("Chats");
        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getSender().equals(myid) && chat.getReceiver().equals(friend_id)
                    || chat.getSender().equals(friend_id) && chat.getReceiver().equals(myid)){
                        chatList.add(chat);
                    }
                }
                mAdapter = new MessageAdapter(getApplicationContext(),chatList,friend.imageURL);
                recyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        myref.removeEventListener(seenListener);
    }


}
