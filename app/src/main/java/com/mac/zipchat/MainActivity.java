package com.mac.zipchat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mac.zipchat.Chat.Chat_Activity;
import com.mac.zipchat.FCM_experiment.Model.NotificationData;
import com.mac.zipchat.FCM_experiment.Model.PushNotification;
import com.mac.zipchat.FCM_experiment.SendNotification;
import com.mac.zipchat.Login.PhoneNumber_Activity;
import com.mac.zipchat.Touch.OnSwipeTouchListener;
import com.mac.zipchat.help_request.HelpRequestActivity;
import com.mac.zipchat.map_location.Map_Fragment;
import com.mac.zipchat.map_location.PrefConfig;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
    private CardView trash_near_me_btn;
    private TextView pinCode_txt;
    private RelativeLayout menu_layout;
    private ImageView close_drawer;
    private int drawerToggle = 0;
    private boolean mapToggle = false;
    private View open_drawer;
    private FrameLayout fragment;
    private CircleImageView show_map_btn, mark_trash_toggle;
    private TextView show_map_text;
    private ImageView logout_btn;
    private FirebaseAuth auth;
    private LottieAnimationView loader;
    private FirebaseUser user;
    private String newUser;
    private StorageReference storageReference;
    private DatabaseReference reference;
    private int marker_toggle = 0;
    private TextView user_name;
    private String showMapIntent;
    private View drawer_helper;
    private boolean userChoice = false;
    private CircleImageView iv_profile;
    private boolean isUserAvailable = false;
    private final int REQ = 1;
    private Bitmap bitmap;
    private boolean notified = false;
    private boolean emulator = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        showMapIntent = getIntent().getStringExtra("showMap");

        PrefConfig.SetPref(MainActivity.this, "markerPref", "marker", "0");
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(getString(R.string.bar_clor)));
        }

//        hideStatusBar();
        checkNetwork();

        newUser = getIntent().getStringExtra("newUser");
        if (newUser != null) {
            if (newUser.equals("1")) {
                ShowDialog();
                PrefConfig.SetPref(MainActivity.this, "newUser", "new", "1");
            } else {
                if (PrefConfig.GetPref(MainActivity.this, "newUser", "new").equals("1")) {
                    ShowDialog();


                }
            }

        } else {

            if (PrefConfig.GetPref(MainActivity.this, "newUser", "new").equals("1")) {
                ShowDialog();

            }

        }


        if (user != null) {
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!notified) {
                        CheckHelpRequest();

                    }
                    handler.postDelayed(this, 1000);

                }
            };
            handler.post(runnable);
        }

        trash_near_me_btn = findViewById(R.id.send_help_request_btn);
        pinCode_txt = findViewById(R.id.pinCode_txt);
        menu_layout = findViewById(R.id.menu_layout);
        loader = findViewById(R.id.loader);
        close_drawer = findViewById(R.id.close_drawer);
        open_drawer = findViewById(R.id.open_drawer);
        fragment = findViewById(R.id.fragment);
        show_map_btn = findViewById(R.id.show_map_btn);
        logout_btn = findViewById(R.id.log_out_btn);
        show_map_text = findViewById(R.id.show_map_text);
        iv_profile = findViewById(R.id.iv_profile);

        if (user.getPhotoUrl() != null) {
            Picasso.get().load(user.getPhotoUrl()).placeholder(R.drawable.profile_icon).into(iv_profile);
        }
        FirebaseMessaging.getInstance().subscribeToTopic(user.getUid());
        reference = FirebaseDatabase.getInstance().getReference();
        drawer_helper = findViewById(R.id.drawer_helper);
        user_name = findViewById(R.id.user_name);
        storageReference = FirebaseStorage.getInstance().getReference().child("profileImage");
        mark_trash_toggle = findViewById(R.id.mark_trash_toggle);
        if (showMapIntent != null) {
            fragment.setVisibility(View.VISIBLE);
            mapToggle = true;
            show_map_text.setText("Hide Map");


        }


        drawer_helper.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
            }

            public void onSwipeRight() {

            }

            public void onSwipeLeft() {

//                OpenDrawer();

            }

            public void onSwipeBottom() {

            }

        });


        menu_layout.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
            }

            public void onSwipeRight() {
//                DrawerClose();
            }

            public void onSwipeLeft() {
            }

            public void onSwipeBottom() {

            }

        });

//        SetAnimation();


        ClickOnCloseDrawer();
        ClickOnOpenDrawer();
        ClickOnShowMApBtn();
        ClickOnTrashNearMeBtn();
        CLickOnLogoutBtn();
        ClickOnUserNameBtn();
        ClickOnMarkTrashBtn();
        ClickOnIvProfileBtn();

        ShowPPDialog();


        if (PrefConfig.GetPref(MainActivity.this, "userPref", "username").equals("error")) {
            SetUserdetails();

        } else {
            user_name.setText(PrefConfig.GetPref(MainActivity.this, "userPref", "username"));
            FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + PrefConfig.GetPref(MainActivity.this, "userPref", "userpin"));
//            Toast.makeText(this, "subs"+PrefConfig.GetPref(MainActivity.this,"userPref","userpin"), Toast.LENGTH_SHORT).show();
        }


//        openMapFragment();


        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawerClose();

            }
        }, 1000);


    }

    private void ShowPPDialog() {

        final String[] ppValue = new String[1];

        if (user != null) {

            reference.child("User").child(user.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        ppValue[0] = snapshot.child("pp").getValue(String.class);

                    }

                    if (ppValue[0]==null) {
                        ShowPrivacyDialog();
                    }else if(ppValue[0].equals("0")){
                        ShowPrivacyDialog();
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        }
    }

    private void ShowPrivacyDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.privacy_policy_dialog);
        dialog.show();

        CardView accept_pp_btn = (CardView) dialog.findViewById(R.id.pp_accept_btn);

        accept_pp_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                HashMap hp = new HashMap();
                hp.put("pp", "1");
                reference.child("User").child(user.getPhoneNumber()).updateChildren(hp);
            }
        });

    }

    private void ClickOnIvProfileBtn() {

        iv_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();

            }
        });
    }


    private void ClickOnMarkTrashBtn() {
        mark_trash_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (marker_toggle == 0) {
                    mark_trash_toggle.setBackground(getResources().getDrawable(R.drawable.green_circulatr_back));
                    marker_toggle = 1;
                    PrefConfig.SetPref(MainActivity.this, "markerPref", "marker", "1");
                    Toast.makeText(MainActivity.this, "Marker on", Toast.LENGTH_SHORT).show();
                } else {
                    mark_trash_toggle.setBackground(getResources().getDrawable(R.drawable.white_circulatr_back));
                    marker_toggle = 0;
                    Toast.makeText(MainActivity.this, "Marker off", Toast.LENGTH_SHORT).show();
                    PrefConfig.SetPref(MainActivity.this, "markerPref", "marker", "0");


                    Map_Fragment fragment = (Map_Fragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    fragment.operateMapFromOutSide();

                }

            }
        });
    }

    private void ClickOnUserNameBtn() {
        user_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userChoice = true;

                ShowDialog();

            }
        });
    }

    private void SetUserdetails() {

        if (user != null) {

            reference.child("User").child(user.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {


                    String name = snapshot.child("name").getValue(String.class);
                    String pinCode = snapshot.child("pinCode").getValue(String.class);
                    String uid = snapshot.child("uid").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);


                    if (name.equals("")) {
                        user_name.setText(user.getPhoneNumber());
                    } else {
                        user_name.setText(name);
                        FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + pinCode);

//                        Toast.makeText(MainActivity.this, "subs data"+pinCode, Toast.LENGTH_SHORT).show();
                        PrefConfig.SetPref(MainActivity.this, "userPref", "username", name);
                        PrefConfig.SetPref(MainActivity.this, "userPref", "userpin", pinCode);
                        PrefConfig.SetPref(MainActivity.this, "userPref", "useruid", uid);
                        PrefConfig.SetPref(MainActivity.this, "userPref", "usergender", gender);
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


        } else {

        }


    }

    private void SetCode() {

        String getPin = PrefConfig.GetPref(MainActivity.this, "pinCode", "code");

        if (getPin.equals("error")) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SetCode();

                }
            }, 2000);
        } else {
            pinCode_txt.setText("Your Realtime PinCode: " + getPin);

        }


    }

    private void ShowDialog() {


        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (userChoice) {
            dialog.setCancelable(true);
        } else {
            dialog.setCancelable(false);

        }
        dialog.setContentView(R.layout.necessary_field_dialog_layout);

        EditText et_name = (EditText) dialog.findViewById(R.id.et_name);
        EditText et_pinCode = (EditText) dialog.findViewById(R.id.et_pinCode);
        Spinner genderSpinner = (Spinner) dialog.findViewById(R.id.sp_gender);
        TextView phoneNumber = (TextView) dialog.findViewById(R.id.show_phoneNumber);
        TextView pp_link = (TextView) dialog.findViewById(R.id.pp_lnk);
        pp_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bookmyematch.000webhostapp.com/zip_chat%20pp.txt"));
                startActivity(browserIntent);
            }
        });
        phoneNumber.setText(user.getPhoneNumber());
        if (userChoice) {
            genderSpinner.setEnabled(false);

        }
        final String[] genderTypeSelected = new String[1];
        String[] gender_items;
        if (!userChoice) {
            gender_items = new String[]{"You are?", "Straight Male", "Straight Female", "Gay", "Lesbian", "Bisexual", "Transgender", "Queer"};

        } else {
            gender_items = new String[]{PrefConfig.GetPref(MainActivity.this, "userPref", "usergender")};
        }

        genderSpinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, com.airbnb.lottie.R.layout.support_simple_spinner_dropdown_item, gender_items));

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                genderTypeSelected[0] = genderSpinner.getSelectedItem().toString();
//                Toast.makeText(MainActivity.this, genderTypeSelected[0], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        if (!PrefConfig.GetPref(MainActivity.this, "userPref", "username").equals("error")) {
            et_name.setText(PrefConfig.GetPref(MainActivity.this, "userPref", "username"));
            et_pinCode.setText(PrefConfig.GetPref(MainActivity.this, "userPref", "userpin"));

        }

        CardView proceedBtn = (CardView) dialog.findViewById(R.id.proceed_btn);
        LottieAnimationView animationView = (LottieAnimationView) dialog.findViewById(R.id.animationView);
        proceedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                reference.child("User").child(user.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {


                            if (et_name.getText().toString().isEmpty()) {
                                et_name.setError("Required Field");
                                et_name.requestFocus();

                            } else if (et_pinCode.getText().toString().isEmpty()) {
                                et_pinCode.setError("Required Field");
                                et_pinCode.requestFocus();

                            } else if (et_pinCode.getText().length() < 6) {
                                et_pinCode.setError("PinCode must be of 6 digits");
                                et_pinCode.requestFocus();

                            } else if (genderTypeSelected[0].equals("You are?")) {

                                Toast.makeText(MainActivity.this, "Please specify who you are", Toast.LENGTH_SHORT).show();


                            } else {

                                animationView.setVisibility(View.VISIBLE);


                                HashMap hp = new HashMap();
                                hp.put("name", et_name.getText().toString().trim());
                                hp.put("pinCode", et_pinCode.getText().toString().trim());
                                hp.put("gender", genderTypeSelected[0]);


                                reference.child("User").child(user.getPhoneNumber()).updateChildren(hp).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
//                                        Toast.makeText(MainActivity.this, "first", Toast.LENGTH_SHORT).show();
                                        PrefConfig.SetPref(MainActivity.this, "newUser", "new", "0");
                                        String pin = PrefConfig.GetPref(MainActivity.this, "userPref", "userpin");
                                        if (pin.equals("error")) {


                                            HashMap hp = new HashMap();
                                            hp.put("pin", et_pinCode.getText().toString());
                                            hp.put("phone", user.getPhoneNumber());


                                            reference.child("PinCodes").child(et_pinCode.getText().toString()).child(user.getPhoneNumber()).setValue(hp).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
//                                                    Toast.makeText(MainActivity.this, "second", Toast.LENGTH_SHORT).show();


                                                    FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + et_pinCode.getText().toString().trim());
//                                                    Toast.makeText(MainActivity.this, "subs_et", Toast.LENGTH_SHORT).show();


                                                    SetUserdetails();

//                                                    Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                                                    animationView.setVisibility(View.GONE);
                                                    dialog.dismiss();

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });

                                        } else {
                                            FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/" + PrefConfig.GetPref(MainActivity.this, "userPref", "userpin"));
//                                            Toast.makeText(MainActivity.this, "unsubscribe"+PrefConfig.GetPref(MainActivity.this, "userPref", "userpin"), Toast.LENGTH_SHORT).show();


                                            HashMap hp = new HashMap();
                                            hp.put("pin", et_pinCode.getText().toString());
                                            hp.put("phone", user.getPhoneNumber());

                                            reference.child("PinCodes").child(pin).child(user.getPhoneNumber()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
//                                                    Toast.makeText(MainActivity.this, "third", Toast.LENGTH_SHORT).show();
                                                    reference.child("PinCodes").child(et_pinCode.getText().toString()).child(user.getPhoneNumber()).setValue(hp).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

//                                                            Toast.makeText(MainActivity.this, "fourth", Toast.LENGTH_SHORT).show();


                                                            SetUserdetails();
                                                            animationView.setVisibility(View.GONE);
                                                            dialog.dismiss();

//                                                            Toast.makeText(MainActivity.this, "done deleting", Toast.LENGTH_SHORT).show();

                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {

                                                        }
                                                    });


                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });


                                        }


                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });

                            }

                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {


                    }
                });


            }
        });

        dialog.show();

    }


    private void CLickOnLogoutBtn() {
        logout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.logout_warning_dialog);
                dialog.show();

                CardView yes_btn = (CardView) dialog.findViewById(R.id.yes_back_btn);
                CardView no_back_btn = (CardView) dialog.findViewById(R.id.no_back_btn);

                yes_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(PrefConfig.GetPref(MainActivity.this, "userPref", "userpin"));
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(user.getUid());
                        String topic = PrefConfig.GetPref(MainActivity.this, "tempTopic", "topic");
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("cancel" + topic);


                        auth.signOut();
                        SharedPreferences sharedPreferences = getSharedPreferences("userPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.commit();

                        SharedPreferences sharedPreferences1 = getSharedPreferences("tempTopic", MODE_PRIVATE);
                        SharedPreferences.Editor editor1 = sharedPreferences1.edit();
                        editor1.clear();
                        editor1.commit();

                        SharedPreferences sharedPreferences2 = getSharedPreferences("userActive", MODE_PRIVATE);
                        SharedPreferences.Editor editor2 = sharedPreferences2.edit();
                        editor2.clear();
                        editor2.commit();

                        SharedPreferences sharedPreferences3 = getSharedPreferences("rPref", MODE_PRIVATE);
                        SharedPreferences.Editor editor3 = sharedPreferences3.edit();
                        editor3.clear();
                        editor3.commit();

                        SharedPreferences sharedPreferences4 = getSharedPreferences("userHelp", MODE_PRIVATE);
                        SharedPreferences.Editor editor4 = sharedPreferences4.edit();
                        editor4.clear();
                        editor4.commit();

                        Intent intent = new Intent(MainActivity.this, PhoneNumber_Activity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    }
                });

                no_back_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });


            }
        });
    }

    private void ClickOnTrashNearMeBtn() {
        trash_near_me_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, TrashNearMeActivity.class);
//                intent.putExtra("pinCode", pinCode_txt.getText().toString());
//                intent.putExtra("latitude", PrefConfig.GetPref(MainActivity.this, "latitudePref", "latitude"));
//                intent.putExtra("longitude", PrefConfig.GetPref(MainActivity.this, "longitudePref", "longitude"));
//                startActivity(intent);

                if (!PrefConfig.GetPref(MainActivity.this, "requestPref", "request").equals("error")) {
                    double checkDifference = System.currentTimeMillis() - Double.parseDouble(PrefConfig.GetPref(MainActivity.this, "requestPref", "request"));
                    if (checkDifference > 180000) {
                        ShowReqDialog();

                    } else {
                        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                        mp.start();
                        final Dialog dialog = new Dialog(MainActivity.this);

                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.setCancelable(true);
                        dialog.setContentView(R.layout.request_warning_dialog);
                        CardView cancel_dialog_btn = (CardView) dialog.findViewById(R.id.cancel_dialog_btn);
                        TextView warning_text = (TextView) dialog.findViewById(R.id.warning_txt);

                        warning_text.setText("You are eligible to send new request after\n" + String.valueOf((int) (180 - checkDifference / 1000)) + " seconds.");

                        cancel_dialog_btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });


                        dialog.show();


                    }
                } else {
                    ShowReqDialog();
                }


            }
        });
    }

    private void ClickOnShowMApBtn() {
        show_map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mapToggle) {
                    fragment.setVisibility(View.VISIBLE);
                    mapToggle = true;
                    show_map_text.setText("Hide Map");

                } else {
                    fragment.setVisibility(View.GONE);
                    mapToggle = false;
                    show_map_text.setText("Show Map");


                }
            }
        });
    }

    private void ClickOnOpenDrawer() {
        open_drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//              OpenDrawer();


            }
        });

    }

    private void OpenDrawer() {
        TranslateAnimation animate = new TranslateAnimation(menu_layout.getWidth() - 50, 0, 0, 0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        menu_layout.startAnimation(animate);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                close_drawer.setRotation(360);

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                    fab_open.setVisibility(View.VISIBLE);
//                    fab_open.animate().alpha(1.0f).setDuration(200);
                drawerToggle = 0;
                menu_layout.setVisibility(View.VISIBLE);
                open_drawer.setVisibility(View.GONE);
                drawer_helper.setVisibility(View.GONE);


            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void ClickOnCloseDrawer() {
        close_drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DrawerClose();

            }
        });
    }


    private void DrawerClose() {
        if (drawerToggle == 0) {

            TranslateAnimation animate = new TranslateAnimation(0, menu_layout.getWidth() - 50, 0, 0);
            animate.setDuration(500);
            animate.setFillAfter(true);
            menu_layout.startAnimation(animate);
            animate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    close_drawer.setRotation(180);


                }

                @Override
                public void onAnimationEnd(Animation animation) {
//                    fab_open.setVisibility(View.VISIBLE);
//                    fab_open.animate().alpha(1.0f).setDuration(200);
                    drawerToggle = 1;
                    menu_layout.setVisibility(View.GONE);
                    open_drawer.setVisibility(View.VISIBLE);
                    drawer_helper.setVisibility(View.VISIBLE);

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

        }
    }


    public void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }


    private void openMapFragment() {
        Map_Fragment fragment = new Map_Fragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, fragment);
        transaction.commit();
        SetCode();
//        Toast.makeText(this, "done", Toast.LENGTH_SHORT).show();


    }


    private void SetAnimation() {


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                menu_layout.animate().alpha(1.0f).setDuration(500);

            }
        }, 1000);
    }


    private void requestPermissions() {
        // below line is use to request
        // permission in the current activity.
        Dexter.withActivity(this)
                // below line is use to request the number of
                // permissions which are required in our app.
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        // below is the list of permissions
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                // after adding permissions we are
                // calling an with listener method.
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // this method is called when all permissions are granted
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            // do you work now
                            openMapFragment();


//                            Toast.makeText(MainActivity.this, "All the permissions are granted..", Toast.LENGTH_SHORT).show();

                        }
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permanently,
                            // we will show user a dialog message.
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // this method is called when user grants some
                        // permission and denies some of them.
                        permissionToken.continuePermissionRequest();
                    }
                }).withErrorListener(new PermissionRequestErrorListener() {
            // this method is use to handle error
            // in runtime permissions
            @Override
            public void onError(DexterError error) {
                // we are displaying a toast message for error message.
//                Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
            }
        })
                // below line is use to run the permissions
                // on same thread and to check the permissions
                .onSameThread().check();
    }

    // below is the shoe setting dialog
    // method which is use to display a
    // dialogue message.
    private void showSettingsDialog() {
        // we are displaying an alert dialog for permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // below line is the title
        // for our alert dialog.
        builder.setTitle("Need Permissions");

        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // this method is called on click on positive
                // button and on clicking shit button we
                // are redirecting our user from our app to the
                // settings page of our app.

//if you added fragment via layout xml

                dialog.cancel();
                // below is the intent from which we
                // are redirecting our user.
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 101);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // this method is called when
                // user click on negative button.
                dialog.cancel();
            }
        });
        // below line is used
        // to display our dialog
        builder.show();
    }


    @Override
    public void onBackPressed() {

        if (fragment.getVisibility() == View.VISIBLE) {
            fragment.setVisibility(View.GONE);
            mapToggle = false;
            show_map_text.setText("Show Map");

        } else {
            super.onBackPressed();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PrefConfig.SetPref(MainActivity.this, "markerPref", "marker", "0");

        mark_trash_toggle.setBackground(getResources().getDrawable(R.drawable.white_circulatr_back));
        marker_toggle = 0;

    }

    @Override
    protected void onResume() {
        super.onResume();
        notified = false;
        PrefConfig.SetPref(MainActivity.this, "markerPref", "marker", "0");

    }

    private void CheckHelpRequest() {
        String requestTracker = PrefConfig.GetPref(this, "helpPref", "helpmessage");
        if (!requestTracker.equals("error")) {
            String[] splitMessageByComma = requestTracker.split(",");


            String name = splitMessageByComma[0];
            String displayMessage = splitMessageByComma[1];
            String phoneNumber = splitMessageByComma[2];
            String uid = splitMessageByComma[3];
            String timeStamp = splitMessageByComma[4];
            String latlang = splitMessageByComma[5];

            VerifyTimeStamp(name, displayMessage, phoneNumber, uid, timeStamp, latlang);


        }

    }

    private void VerifyTimeStamp(String name, String displayMessage, String phoneNumber, String uid, String timeStamp, String latlang) {
        Double checkDifference = System.currentTimeMillis() - Double.parseDouble(timeStamp);
        if (checkDifference < 180000) {
            if (!uid.equals(user.getUid())) {
                ShowHelpDialog(name, displayMessage, phoneNumber, uid, timeStamp, latlang);

            }


        } else {
            SharedPreferences preferences = getSharedPreferences("helpPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            FirebaseMessaging.getInstance().unsubscribeFromTopic("cancel" + uid);
        }
    }

    private void ShowHelpDialog(String name, String displayMessage, String phoneNumber, String uid, String timeStamp, String latlang) {

        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.bell_sound);
        mp.start();
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.receive_help_dialog);

        FrameLayout help_btn = (FrameLayout) dialog.findViewById(R.id.help_btn);
        FrameLayout ignore_request = (FrameLayout) dialog.findViewById(R.id.ignore_request);
        TextView displayMsg = (TextView) dialog.findViewById(R.id.et_displayMsg);
        TextView user_name = (TextView) dialog.findViewById(R.id.user_name);
        user_name.setText(name);
        displayMsg.setText(displayMessage);
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notified = true;


        ignore_request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notified = false;
                dialog.dismiss();
                SharedPreferences preferences = getSharedPreferences("helpPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
                FirebaseMessaging.getInstance().unsubscribeFromTopic("cancel" + uid);

            }
        });


        help_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                if (PrefConfig.GetPref(MainActivity.this, "tempTopic", "topic").equals(uid)) {


                    CreateChatRoom(dialog, latlang, uid, name, phoneNumber);

                } else {
                    MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                    mp.start();

                    Dialog dialog1 = new Dialog(MainActivity.this);
                    dialog1.setCancelable(true);
                    dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog1.setContentView(R.layout.session_expired_warning_dialog);


                    CardView gobackBtn = (CardView) dialog1.findViewById(R.id.go_back_btn);
                    gobackBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            dialog1.dismiss();
                        }
                    });

                    dialog1.show();


                }

            }
        });


    }

    private void CreateChatRoom(Dialog dialog, String latlang, String uid, String name, String phoneNumber) {
        if (PrefConfig.GetPref(MainActivity.this, "tempTopic", "topic").equals(uid)) {
            String latitude = "";
//                String latitude = "29.017652732110413";
            String longitude = "";
//                String longitude = "77.7619469538331";
            String topic = "/topics/" + uid;
//                String topic ="/topics/"+"250001";
            String phone = user.getPhoneNumber();

            String displayMessage = "Hey I am " + PrefConfig.GetPref(MainActivity.this, "userPref", "username") + " lets chat.";

            String message = latitude + "," + longitude + "," + user_name.getText().toString() + "," + displayMessage + "," + phone + "," + user.getUid();

            PushNotification pushNotification = new PushNotification(new NotificationData("Received Help", message), topic);
            SendNotification.Send(pushNotification, MainActivity.this);
            dialog.dismiss();

            SharedPreferences preferences = getSharedPreferences("helpPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            FirebaseMessaging.getInstance().unsubscribeFromTopic("cancel" + uid);

            PrefConfig.SetPref(MainActivity.this, "userHelp", "user", "true");

            Toast.makeText(MainActivity.this, "Connecting with " + name, Toast.LENGTH_SHORT).show();

            PrefConfig.SetPref(MainActivity.this, "userActive", "active", String.valueOf(System.currentTimeMillis()));
            PrefConfig.SetPref(MainActivity.this, "userHelp", "user", "false");
            PrefConfig.SetPref(MainActivity.this, "acceptStatus", "accept", "0");
            PrefConfig.SetPref(MainActivity.this, "onlineStatus", "online", "0");
            PrefConfig.SetPref(this, "reqReject", "reject", "0");


            Intent intent = new Intent(MainActivity.this, Chat_Activity.class);
            intent.putExtra("latlang", latlang);
            intent.putExtra("uid", uid);
            intent.putExtra("name", name);
            intent.putExtra("number", phoneNumber);
            intent.putExtra("action", "accept");
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.slide_up);


        } else {
            dialog.dismiss();
            MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.error_sound);
            mp.start();
            Dialog dialog1 = new Dialog(MainActivity.this);
            dialog1.setCancelable(false);
            dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog1.setContentView(R.layout.session_expired_warning_dialog);


            CardView gobackBtn = (CardView) dialog1.findViewById(R.id.go_back_btn);
            gobackBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PrefConfig.SetPref(MainActivity.this, "userHelp", "user", "true");

                    dialog1.dismiss();
                }
            });

            dialog1.show();

        }


    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Location seems to be disabled, You have enable it to use the app.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
//                        finish();
                    }
                });
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    public void onClick(final DialogInterface dialog, final int id) {
//                        dialog.cancel();
//                    }
//                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        } else {
            requestPermissions();
        }
    }


    public boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {

                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {

                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                        return true;
                    }
                }
            }
        }


        return false;

    }

    private void checkNetwork() {

        if (!isNetworkAvailable() == true) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setTitle("Internet Connection Alert")
                    .setMessage("Please Check Your Internet Connection")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).show();
        } else if (isNetworkAvailable() == true) {

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        statusCheck();

    }


    private void ShowReqDialog() {

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.send_help_dialog);

        CardView send_request_btn = (CardView) dialog.findViewById(R.id.send_request);
        TextView user_name = (TextView) dialog.findViewById(R.id.user_name);
        TextView num_gen_info = (TextView) dialog.findViewById(R.id.num_gen_info);
        num_gen_info.setText(user.getPhoneNumber()+" , "+PrefConfig.GetPref(MainActivity.this,"userPref","usergender"));
        user_name.setText(PrefConfig.GetPref(MainActivity.this, "userPref", "username"));
        Spinner genderSpinner = dialog.findViewById(R.id.sp_gender);
        String[] gender_items = new String[]{"Match Preferences", "Straight Male", "Straight Female", "Gay", "Lesbian", "Bisexual", "Transgender", "Queer"};
        final String genderTypeSelected[] = new String[1];
        genderSpinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, com.airbnb.lottie.R.layout.support_simple_spinner_dropdown_item, gender_items));
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {


                genderTypeSelected[0] = genderSpinner.getSelectedItem().toString();
//                Toast.makeText(MainActivity.this, genderTypeSelected[0], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        send_request_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String topic;
                String latitude;
                String longitude;
                if (!genderTypeSelected[0].equals("Match Preferences")) {

                    if (emulator) {
                        latitude = "29.017652732110413";
                        longitude = "77.7619469538331";
                        topic = "/topics/" + "250001";


                    } else {
                        topic = "/topics/" + PrefConfig.GetPref(MainActivity.this, "pinCode", "code");
                        latitude = PrefConfig.GetPref(MainActivity.this, "latitudePref", "latitude");
                        longitude = PrefConfig.GetPref(MainActivity.this, "longitudePref", "longitude");


                    }

                    String name = PrefConfig.GetPref(MainActivity.this, "userPref", "username");

                    String phoneNumber = user.getPhoneNumber();
                    String timeStamp = String.valueOf(System.currentTimeMillis());
                    String gender = genderTypeSelected[0];
//                    Toast.makeText(MainActivity.this, gender, Toast.LENGTH_SHORT).show();
                    String uid = user.getUid();
                    String displayMessage = "Hey I am " + name + " finding someone to have chat with me";

                    String message = name + "," + displayMessage + "," + phoneNumber + "," + uid + "," + timeStamp + "," + latitude + "-" + longitude + "," + gender;

                    PushNotification pushNotification = new PushNotification(new NotificationData("Chat Request", message), topic);
                    SendNotification.Send(pushNotification, MainActivity.this);
                    String rPref = PrefConfig.GetPref(MainActivity.this, "rPref", "rece");
//                Toast.makeText(TrashNearMeActivity.this, rPref, Toast.LENGTH_SHORT).show();
                    SharedPreferences preferences = getSharedPreferences("rPref", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.clear();
                    editor.commit();

                    Toast.makeText(MainActivity.this, "Chat Request sent", Toast.LENGTH_SHORT).show();
                    PrefConfig.SetPref(MainActivity.this, "userHelp", "user", "false");
                    PrefConfig.SetPref(MainActivity.this, "userActive", "active", String.valueOf(System.currentTimeMillis()));
                    PrefConfig.SetPref(MainActivity.this, "reqReject", "reject", "0");
//                    HashMap hashMap = new HashMap();
//                    hashMap.put("message","");
//                    DatabaseReference sendReference= FirebaseDatabase.getInstance().getReference().child("Chats");
//                    sendReference.child(user.getUid()).setValue(hashMap);


                    Intent intent = new Intent(MainActivity.this, HelpRequestActivity.class);
                    startActivity(intent);

                    dialog.dismiss();

                } else {
                    Toast.makeText(MainActivity.this, "Please choose match preferences", Toast.LENGTH_SHORT).show();

                }


            }
        });


        dialog.show();
    }


    private void openGallery() {
        Intent pickImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);


        startActivityForResult(pickImage, REQ);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ && resultCode == RESULT_OK) {


            Uri uri = data.getData();
            try {

                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                iv_profile.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();

            }

            if (bitmap != null) {
                uploadImage();

            }
        }


    }


    private void uploadImage() {
        loader.setVisibility(View.VISIBLE);
        try {
            if (user.getPhotoUrl() != null) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(user.getPhotoUrl().toString());
                storageReference.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] finalimage = baos.toByteArray();
        final StorageReference filePath;
        filePath = storageReference.child("UserImages").child(uid + "jpg");
        final UploadTask uploadTask = filePath.putBytes(finalimage);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {


                        updateUser(uri);

                    }
                });
            }
        });
    }


    private void updateUser(Uri uri) {

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        auth.getCurrentUser().updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                HashMap hashMap = new HashMap();
                hashMap.put("image", uri.toString());

                reference.child("Images").child(user.getUid()).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        loader.setVisibility(View.GONE);
                        Snackbar.make(loader, "Profile image updated", Snackbar.LENGTH_SHORT).show();

                    }
                });


            }
        });


    }


}
