package com.technofreak.minichatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatroomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    FirebaseAuth mAuth;
    DatabaseReference myref;
    private String userID;
    private List<User> userlist;
    private UserAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        userlist = new ArrayList<>();
        recyclerView = findViewById(R.id.recycler);
        myAdapter = new UserAdapter(this,userlist);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(myAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        setTitle(user.getDisplayName() + " \uD83D\uDCF2  Mini Chat App");
        userID = user.getUid();
        myref = FirebaseDatabase.getInstance().getReference("Users");
        updateUI();

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful()){
                    String tok = task.getResult().getToken();
                    saveToken(tok);
                } else{
                    Toast.makeText(getApplicationContext(), "token not generated", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void saveToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userID);
        HashMap<String, Object> t = new HashMap<>();
        t.put("token", token);
        ref.updateChildren(t);
    }


    private void updateUI() {

        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userlist.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()){
                    User a = userSnapshot.getValue(User.class);
                    if (!a.id.equals(userID)){
                        userlist.add(a);
                    }
                    myAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    public void logout(View view) {
        mAuth.signOut();
        finish();
        startActivity(new Intent(this,MainActivity.class));

    }
}
