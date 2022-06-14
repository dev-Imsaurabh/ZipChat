package com.mac.zipchat.Chat;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mac.zipchat.FCM_experiment.Model.NotificationData;
import com.mac.zipchat.FCM_experiment.Model.PushNotification;
import com.mac.zipchat.FCM_experiment.SendNotification;
import com.mac.zipchat.MainActivity;
import com.mac.zipchat.R;
import com.mac.zipchat.map_location.PrefConfig;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class Chat_Activity extends AppCompatActivity {
    private String latlang;
    private String uid, name, action,number;
    private Handler handler, handler1, handler2, handler3, handler4, handler5,activeHandler;
    private String Topic;
    private ProgressDialog progressDialog;
    private ConstraintLayout root;
    private FirebaseAuth auth;
    private boolean recheck = true;
    private FirebaseUser user;
    private boolean responseTime = true;
    private RecyclerView chatRecycler;
    private EditText messageBoET;
    private FrameLayout sendBtn;
    private ArrayList<MessageModel> list;
    private DatabaseReference sendReference,receiveReference;
    private CircleImageView iv_profile;
    private TextView user_name,checkStatus;
    private boolean afterRes=false;


    private TextView Time_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        FindViewByID();
        GetIntent();
        CheckInRealTime();
        CheckResponse();
        CheckIgnore();
        SetUser();
        SetActiveTime();

//        Check();



    }

    private void SetActiveTime() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                PrefConfig.SetPref(Chat_Activity.this,"userActive","active",String.valueOf(System.currentTimeMillis()));

                activeHandler.postDelayed(this,60000);

            }
        };

        activeHandler.post(runnable);

    }

    private void Check() {

        Handler checkHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if(PrefConfig.GetPref(Chat_Activity.this,"tempTopic","topic").equals(uid)){

                    Toast.makeText(Chat_Activity.this, PrefConfig.GetPref(Chat_Activity.this,"tempTopic","topic"), Toast.LENGTH_SHORT).show();

                }

                checkHandler.postDelayed(this,4000);


            }
        };
        checkHandler.post(runnable);

    }


    private void SetUser() {
        user_name=findViewById(R.id.user_name);
        iv_profile=findViewById(R.id.iv_profile);
        checkStatus =findViewById(R.id.statusCheck);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Images");
        reference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Picasso.get().load(snapshot.child("image").getValue(String.class)).placeholder(R.drawable.profile_icon).into(iv_profile);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference().child("User");

        reference2.child(number).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child("name").getValue(String.class);
                    user_name.setText(name);



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });





        Time_txt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkStatus.animate().alpha(1.0f).setDuration(300);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        checkStatus.animate().alpha(0.0f).setDuration(300);


                    }
                },500);
            }

            @Override
            public void afterTextChanged(Editable editable) {


                    }
        });




    }


    private void FindViewByID() {
        root = findViewById(R.id.root);
        Time_txt = findViewById(R.id.Time_txt);
        progressDialog = new ProgressDialog(this);
        handler1 = new Handler();
        handler2 = new Handler();
        handler3 = new Handler();
        handler4 = new Handler();
        handler5 = new Handler();
        activeHandler=new Handler();
        chatRecycler=findViewById(R.id.chatRecycler);

        messageBoET=findViewById(R.id.messageBoxET);
        sendBtn=findViewById(R.id.sendBtn);
        sendBtn.setVisibility(View.GONE);
        receiveReference = FirebaseDatabase.getInstance().getReference().child("Chats");
        sendReference = FirebaseDatabase.getInstance().getReference().child("Chats");

        EditTextListner();


        list=new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        layoutManager.setSmoothScrollbarEnabled(true);

        chatRecycler.setLayoutManager(layoutManager);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageModel msg = new MessageModel(messageBoET.getText().toString(),CustomAdapter.MESSAGE_TYPE_OUT);
                list.add(msg);

                CustomAdapter adapter = new CustomAdapter(Chat_Activity.this, list);

                chatRecycler.setAdapter(adapter);
                SendToFirebase(messageBoET.getText().toString());
                messageBoET.setText("");

            }
        });

    }

    private void EditTextListner() {

        messageBoET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {


            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                sendBtn.setVisibility(View.VISIBLE);

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if(!messageBoET.getText().toString().trim().equals("")){
                    sendBtn.setVisibility(View.VISIBLE);
                }else {
                    sendBtn.setVisibility(View.GONE);
                }



            }
        });
    }

    private void SendToFirebase(String senderMessage) {
        HashMap hashMap = new HashMap();
        hashMap.put("message",senderMessage);

        receiveReference.child(user.getUid()).updateChildren(hashMap);


    }


    private void GetIntent() {
        latlang = getIntent().getStringExtra("latlang");
        uid = getIntent().getStringExtra("uid");
        name = getIntent().getStringExtra("name");
        action = getIntent().getStringExtra("action");
        number=getIntent().getStringExtra("number");
        HashMap hashMap = new HashMap();
        hashMap.put("message","");
        DatabaseReference sendReference= FirebaseDatabase.getInstance().getReference().child("Chats");
        sendReference.child(uid).setValue(hashMap);
        Topic = "/topics/" + uid;
//        String[] splitLatlang = latlang.split("-");
//
//        String latitude = splitLatlang[0];
//        String longitude = splitLatlang[1];


        if (action.equals("chat")) {
            PushNotification pushNotification = new PushNotification(new NotificationData("chat", "accepted"), Topic);
            SendNotification.Send(pushNotification, Chat_Activity.this);
//            Toast.makeText(this, "sent " + uid, Toast.LENGTH_SHORT).show();
            ReceiveMessages();
            SendTime2();

//            CheckOnlineStatus();
        } else {
//            Toast.makeText(this, "accepted", Toast.LENGTH_SHORT).show();
            progressDialog.setMessage("Waiting For " + name + " to join the room..\nWaiting maximum time 20 seconds.");
            progressDialog.setCancelable(false);
            progressDialog.show();


            handler = new Handler();

            final Runnable runnable = new Runnable() {
                public void run() {
                    String status = PrefConfig.GetPref(Chat_Activity.this, "acceptStatus", "accept");
                    if (!status.equals("0")) {
                        handler.removeCallbacksAndMessages(null);
                        progressDialog.dismiss();
                        PrefConfig.SetPref(Chat_Activity.this, "acceptStatus", "accept", "0");
                        Snackbar.make(root, name + " joined the room", Snackbar.LENGTH_SHORT).show();

                        ReceiveMessages();
                        responseTime = false;
                        SendTime1();


                    } else {
                        handler.postDelayed(this, 1000);

                    }


                }
            };
            handler.post(runnable);


        }


    }

    private void ReceiveMessages() {


        receiveReference.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String receMsg = snapshot.child("message").getValue(String.class);

                MessageModel msg = new MessageModel(receMsg,CustomAdapter.MESSAGE_TYPE_IN);
                try {
                    if(!receMsg.equals("")){
                        list.add(msg);
                    }

                    CustomAdapter adapter = new CustomAdapter(Chat_Activity.this, list);

                    chatRecycler.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void CheckInRealTime() {


        final Runnable runnable = new Runnable() {
            public void run() {

                String time = PrefConfig.GetPref(Chat_Activity.this, "onlineStatus", "online");
                if (!time.equals("0")) {
                    Time_txt.setText(time);
                    double checkDifference = System.currentTimeMillis() - Double.parseDouble(time);
                    if (checkDifference > 20000) {
                        recheck = false;
                        Time_txt.setText("person left");

                        handler3.removeCallbacksAndMessages(null);

                        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                        mp.start();

                        ShowChatLeftDiaog();


                    }


                }

                if (recheck) {
                    handler3.postDelayed(this, 1000);

                }


            }
        };
        handler3.post(runnable);
    }


    private void CheckResponse() {

        if (action.equals("accept")) {

            TimeOut();

        }
    }


    private void CheckIgnore() {

        final Runnable runnable = new Runnable() {
            public void run() {
                String rejectValue = PrefConfig.GetPref(Chat_Activity.this, "reqReject", "reject");
                if (!rejectValue.equals("0")) {
                    handler5.removeCallbacksAndMessages(null);
                    ShowChatLeftDiaog();
                } else {
                    handler5.postDelayed(this, 1000);

                }


            }
        };
        handler5.post(runnable);


    }


    private void TimeOut() {

        final Runnable runnable = new Runnable() {
            public void run() {

                String time = PrefConfig.GetPref(Chat_Activity.this, "userActive", "active");

                if (System.currentTimeMillis() - Double.parseDouble(time) > 20000) {
                    responseTime = false;
                    progressDialog.dismiss();
                    ShowTimeOutDialog();
                    handler4.removeCallbacksAndMessages(null);


                }

                if (responseTime) {
                    handler4.postDelayed(this, 2000);

                }


            }
        };
        handler4.post(runnable);

    }

    private void ShowTimeOutDialog() {
        Dialog dialog1 = new Dialog(Chat_Activity.this);
        dialog1.setCancelable(false);
        dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog1.setContentView(R.layout.time_out_dialog);
        dialog1.show();

        CardView close_btn = (CardView) dialog1.findViewById(R.id.close_timeout_btn);
        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog1.dismiss();
                PrefConfig.SetPref(Chat_Activity.this, "userHelp", "user", "false");
                finish();
            }
        });


    }

    private void SendTime1() {

        final Runnable runnable = new Runnable() {
            public void run() {
                String Topic = "/topics/" + uid;


                PushNotification pushNotification = new PushNotification(new NotificationData("accepted", "time"), Topic);
                SendNotification.Send(pushNotification, Chat_Activity.this);
                PrefConfig.SetPref(Chat_Activity.this, "userActive", "active", String.valueOf(System.currentTimeMillis()));


                handler1.postDelayed(this, 5000);

            }
        };
        handler1.post(runnable);


    }

    private void SendTime2() {


        final Runnable runnable = new Runnable() {
            public void run() {

                String Topic = "/topics/" + uid;


                PushNotification pushNotification = new PushNotification(new NotificationData("chat", "time"), Topic);
                SendNotification.Send(pushNotification, Chat_Activity.this);
                PrefConfig.SetPref(Chat_Activity.this, "userActive", "active", String.valueOf(System.currentTimeMillis()));

                handler2.postDelayed(this, 5000);


            }
        };
        handler2.post(runnable);


    }


    private void ShowChatLeftDiaog() {

        Dialog dialog1 = new Dialog(Chat_Activity.this);
        dialog1.setCancelable(false);
        dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog1.setContentView(R.layout.chat_left_dialog);

        handler2.removeCallbacksAndMessages(null);
        handler1.removeCallbacksAndMessages(null);


        CardView gobackBtn = (CardView) dialog1.findViewById(R.id.go_back_btn);
        gobackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog1.dismiss();
                PrefConfig.SetPref(Chat_Activity.this, "userHelp", "user", "true");

                finish();
            }
        });
        try {
            dialog1.show();
        } catch (WindowManager.BadTokenException e) {
            //use a log message
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
//        handler1.removeCallbacksAndMessages(null);
//        handler2.removeCallbacksAndMessages(null);
//        handler3.removeCallbacksAndMessages(null);

    }

    @Override
    protected void onResume() {
        super.onResume();

//        if(action.equals("chat")){
//            SendTime2();
//        }else {
//            SendTime1();
//        }
//
//        Handler handler5 = new Handler();
//        handler5.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                CheckInRealTime();
//
//            }
//        },5000);

    }

    @Override
    public void onBackPressed() {


        showDialog();


    }


    private void showDialog() {
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
        mp.start();
        final Dialog dialog = new Dialog(Chat_Activity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.close_warning_dialog);

        CardView yes_back_btn = (CardView) dialog.findViewById(R.id.yes_back_btn);
        CardView no_ba_btn = (CardView) dialog.findViewById(R.id.no_back_btn);

        yes_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                PushNotification pushNotification = new PushNotification(new NotificationData("ignored","ignore"),Topic);
                SendNotification.Send(pushNotification,Chat_Activity.this);
                PrefConfig.SetPref(Chat_Activity.this, "userHelp", "user", "true");
                finish();

            }
        });

        no_ba_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activeHandler.removeCallbacksAndMessages(null);
        handler1.removeCallbacksAndMessages(null);
        handler2.removeCallbacksAndMessages(null);
        handler3.removeCallbacksAndMessages(null);
        handler4.removeCallbacksAndMessages(null);
    }
}