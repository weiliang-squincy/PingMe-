package apps.shark.pingme.ui;

/**
 * Created by Harsha on 7/18/2017.
 */

import android.app.ProgressDialog;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.graphics.Color;
        import android.media.AudioManager;
        import android.media.MediaPlayer;
        import android.media.MediaRecorder;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
        import android.support.annotation.NonNull;
        import android.support.v4.content.ContextCompat;
        import android.support.v4.content.res.ResourcesCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.support.v7.widget.Toolbar;
        import android.text.Editable;
        import android.text.TextWatcher;
        import android.util.Log;
        import android.view.Gravity;
        import android.view.MotionEvent;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.ListView;
        import android.widget.TextView;

        import java.text.SimpleDateFormat;
        import java.util.Date;
        import com.bumptech.glide.Glide;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import com.firebase.ui.database.FirebaseListAdapter;
        import com.firebase.ui.storage.images.FirebaseImageLoader;
        import com.google.android.gms.tasks.OnCompleteListener;
        import com.google.android.gms.tasks.OnFailureListener;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;
        import com.google.firebase.storage.StorageReference;
        import com.google.firebase.storage.UploadTask;

        import org.w3c.dom.Text;

        import java.io.File;
        import java.io.IOException;
        import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
        import java.util.UUID;

        import apps.shark.pingme.R;
        import apps.shark.pingme.model.Message;
        import apps.shark.pingme.model.User;
        import apps.shark.pingme.utils.Constants;
import apps.shark.pingme.utils.Methods;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static apps.shark.pingme.R.id.timeTextView;

public class ChatMessagesActivity extends AppCompatActivity {

    private String messageId;
    private TextView mMessageField;
    private ImageButton mSendButton;
    private String chatName;
    private ListView mMessageList;
    private Toolbar mToolBar;
    private String currentUserEmail;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessageDatabaseReference;
    private DatabaseReference mUsersDatabaseReference;
    private FirebaseListAdapter<Message> mMessageListAdapter;
    private FirebaseAuth mFirebaseAuth;

    private ImageButton mphotoPickerButton;
    private static final int GALLERY_INTENT=2;
    private StorageReference mStorage;
    private ProgressDialog mProgress;

    private ImageButton mrecordVoiceButton;
    private TextView mRecordLable;

    private MediaRecorder mRecorder;
    private String mFileName = null;

    private static final String LOG_TAG = "Record_log";
    private static final String TAG = "Chat Message Activity";
    private ValueEventListener mValueEventListener;

    //Audio Runtime Permissions
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) ChatMessagesActivity.super.finish();
        if (!permissionToWriteAccepted) ChatMessagesActivity.super.finish();

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages_activity);

        Intent intent = this.getIntent();
        //MessageID is the location of the messages for this specific chat
        messageId = intent.getStringExtra(Constants.MESSAGE_ID);
        chatName = intent.getStringExtra(Constants.CHAT_NAME);

        if(messageId == null){
            finish(); // replace this.. nav user back to home
            return;
        }

        //Check Permissions at runtime
        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

        initializeScreen();
        mToolBar.setTitle(chatName);
        showMessages();
        addListeners();
        openImageSelector();
        openVoiceRecorder();
    }

    //Add listener for on completion of image selection
    public void openImageSelector() {
        mphotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mProgress = new ProgressDialog(this);
        mphotoPickerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_INTENT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data){

        mStorage = FirebaseStorage.getInstance().getReference(); //make global
        super.onActivityResult(requestCode, requestCode, data);

        if(requestCode == GALLERY_INTENT && resultCode == RESULT_OK){

            mProgress.setMessage("Sending the image...");
            mProgress.show();

            Uri uri = data.getData();
            //Keep all images for a specific chat grouped together
            final String imageLocation = "Photos" + "/" + messageId;
            final String imageLocationId = imageLocation + "/" + uri.getLastPathSegment();
            final String uniqueId = UUID.randomUUID().toString();
            final StorageReference filepath = mStorage.child(imageLocation).child(uniqueId + "/image_message");
            final String downloadURl = filepath.getPath();
            filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //create a new message containing this image
                    addImageToMessages(downloadURl);
                    mProgress.dismiss();
                }
            });
        }

    }

    public void openVoiceRecorder(){
        //Implement voice selection
        mrecordVoiceButton = (ImageButton) findViewById(R.id.recordVoiceButton);

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/recorded_audio.3gp";

        mrecordVoiceButton.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent){

                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){

                    startRecording();
                    mRecordLable.setText("Recording started...");
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP){

                    stopRecording();
                    mRecordLable.setText("Recording stopped...");

                }
                return false;
            }
        });

        //on complete: sendVoice()
    }

    private void startRecording() {

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        uploadAudio();
    }

    private void uploadAudio() {

        mStorage = FirebaseStorage.getInstance().getReference();

        mProgress.setMessage("Sending the Audio...");
        mProgress.show();

        Uri uri = Uri.fromFile(new File(mFileName));
        //Keep all voice for a specific chat grouped together
        final String voiceLocation = "Voice" + "/" + messageId;
        final String voiceLocationId = voiceLocation + "/" + uri.getLastPathSegment();
        final String uniqueId = UUID.randomUUID().toString();
        final StorageReference filepath = mStorage.child(voiceLocation).child(uniqueId + "/audio_message.3gp");
        final String downloadURl = filepath.getPath();

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                addVoiceToMessages(downloadURl);
                mProgress.dismiss();
                mRecordLable.setText("Tap and Hold the Phone Button to Record");

            }
        });
    }

    public void addListeners(){
        mMessageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    //If voice message add them to Firebase.Storage
    public void addVoiceToMessages(String voiceLocation){
        final DatabaseReference pushRef = mMessageDatabaseReference.push();
        final String pushKey = pushRef.getKey();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        Date date = new Date();
        String timestamp = timeFormat.format(date);
        String datestamp = dateFormat.format(date);
        //Create message object with text/voice etc
        Message message =
                new Message(encodeEmail(mFirebaseAuth.getCurrentUser().getEmail()),
                        "Message: Voice Sent", "VOICE", voiceLocation, timestamp, datestamp);
        //Create HashMap for Pushing
        HashMap<String, Object> messageItemMap = new HashMap<String, Object>();
        HashMap<String,Object> messageObj = (HashMap<String, Object>) new ObjectMapper()
                .convertValue(message, Map.class);
        messageItemMap.put("/" + pushKey, messageObj);
        mMessageDatabaseReference.updateChildren(messageItemMap)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mMessageField.setText("");
                    }
                });
    }

    //Send image messages from here
    public void addImageToMessages(String imageLocation){
        final DatabaseReference pushRef = mMessageDatabaseReference.push();
        final String pushKey = pushRef.getKey();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        String timestamp = timeFormat.format(date);
        String datestamp = dateFormat.format(date);
        //Create message object with text/voice etc
        Message message =
                new Message(encodeEmail(mFirebaseAuth.getCurrentUser().getEmail()),
                        "", "IMAGE", imageLocation, timestamp, datestamp);
        //Create HashMap for Pushing
        HashMap<String, Object> messageItemMap = new HashMap<String, Object>();
        HashMap<String,Object> messageObj = (HashMap<String, Object>) new ObjectMapper()
                .convertValue(message, Map.class);
        messageItemMap.put("/" + pushKey, messageObj);
        mMessageDatabaseReference.updateChildren(messageItemMap)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mMessageField.setText("");
                    }
                });
    }

    public void sendMessage(View view){
        //final DatabaseReference messageRef = mFirebaseDatabase.getReference(Constants.MESSAGE_LOCATION);
        final DatabaseReference pushRef = mMessageDatabaseReference.push();
        final String pushKey = pushRef.getKey();

        String messageString = mMessageField.getText().toString();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        String timestamp = timeFormat.format(date);
        String datestamp = dateFormat.format(date);
        //Create message object with text/voice etc
        Message message = new Message(encodeEmail(mFirebaseAuth.getCurrentUser().getEmail()), messageString, timestamp, datestamp);
        //Create HashMap for Pushing
        HashMap<String, Object> messageItemMap = new HashMap<String, Object>();
        HashMap<String,Object> messageObj = (HashMap<String, Object>) new ObjectMapper()
                .convertValue(message, Map.class);
        messageItemMap.put("/" + pushKey, messageObj);
        mMessageDatabaseReference.updateChildren(messageItemMap)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mMessageField.setText("");
                    }
                });
    }



    private void showMessages() {
        mMessageListAdapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message_item, mMessageDatabaseReference) {
            @Override
            protected void populateView(final View view, final Message message, final int position) {
                LinearLayout messageLine = (LinearLayout) view.findViewById(R.id.messageLine);
                TextView messageText = (TextView) view.findViewById(R.id.messageTextView);
                TextView senderText = (TextView) view.findViewById(R.id.senderTextView);
                TextView timeTextView = (TextView) view.findViewById(R.id.timeTextView);
                TextView dateTextView = (TextView) view.findViewById(R.id.dateTextView);
                final ImageView leftImage = (ImageView) view.findViewById(R.id.leftMessagePic);
                final ImageView rightImage = (ImageView) view.findViewById(R.id.rightMessagePic);
                LinearLayout individMessageLayout = (LinearLayout) view.findViewById(R.id.individMessageLayout);

                //display timestamp correctly
                String time = message.getTimestamp();
                Log.e("debug", String.format("timestamp = %s", time));
                if(time != null && !time.equals("")) {
                    time = changeTime12HrsFormat(time);
                    timeTextView.setText(time);
                } else {
                    timeTextView.setVisibility(View.GONE);
                }

                //display timestamp correctly
                String date = message.getDatestamp();
                if(date != null && !date.equals("")) {
                    dateTextView.setText(date);
                } else {
                    dateTextView.setVisibility(View.GONE);
                }

                //set message and sender text
                messageText.setText(message.getMessage());
                senderText.setText(Methods.DecodeString(message.getSender()));
                //If you sent this message, right align
                String mSender = message.getSender();

                if(mSender.equals(currentUserEmail)) {
                    individMessageLayout.setGravity(Gravity.RIGHT);
                    messageLine.setGravity(Gravity.RIGHT);
                    leftImage.setVisibility(View.GONE);
                    rightImage.setVisibility(View.VISIBLE);
                    timeTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                    dateTextView.setGravity(Gravity.RIGHT);

                    //profile image back to here
                    mUsersDatabaseReference.child(mSender).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User userInfo = dataSnapshot.getValue(User.class);
                            userInfo.setEmail(Methods.DecodeString(userInfo.getEmail()));

                            try
                            {
                                if(userInfo != null && userInfo.getProfilePicLocation() != null){
                                    StorageReference storageRef = FirebaseStorage.getInstance()
                                            .getReference().child(userInfo.getProfilePicLocation());
                                    Glide.with(view.getContext())
                                            .using(new FirebaseImageLoader())
                                            .load(storageRef)
                                            .bitmapTransform(new CropCircleTransformation(view.getContext()))
                                            .into(rightImage);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    individMessageLayout.setBackgroundResource(R.drawable.roundedmessagescolored);
                } else if(mSender.equals("System")) {
                    individMessageLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
                    leftImage.setVisibility(View.GONE);
                    rightImage.setVisibility(View.GONE);
                    timeTextView.setVisibility(View.GONE);
                    dateTextView.setVisibility(View.GONE);

                    individMessageLayout.setBackgroundResource(R.drawable.roundedmessagescoloredsystem);
                } else {
                    messageLine.setGravity(Gravity.LEFT);
                    leftImage.setVisibility(View.VISIBLE);
                    rightImage.setVisibility(View.GONE);
                    individMessageLayout.setBackgroundResource(R.drawable.roundedmessages);
                    //messgaeText.setBackgroundColor(ResourcesCompat.getColor(getResources(),
                    //       R.color.colorPrimary, null));

                    //profile image back to here
                    mUsersDatabaseReference.child(mSender).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User userInfo = dataSnapshot.getValue(User.class);
                            userInfo.setEmail(Methods.DecodeString(userInfo.getEmail()));
                            if(userInfo != null && userInfo.getProfilePicLocation() != null){
                                try{
                                    StorageReference storageRef = FirebaseStorage.getInstance()
                                            .getReference().child(userInfo.getProfilePicLocation());
                                    Glide.with(view.getContext())
                                            .using(new FirebaseImageLoader())
                                            .load(storageRef)
                                            .bitmapTransform(new CropCircleTransformation(view.getContext()))
                                            .into(leftImage);
                                }catch(Exception e){
                                    Log.e("Err", e.toString());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                //If this is multimedia display it
                final ImageView imageView = (ImageView) view.findViewById(R.id.imageMessage);
                final ImageButton activateVoiceMsg = (ImageButton) view.findViewById(R.id.voiceMessageButton);
                if(message.getMultimedia()) {
                    //messageText.setVisibility(View.GONE);
                    if(message.getContentType().equals("IMAGE")) {
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReference().child(message.getContentLocation());
                        imageView.setVisibility(View.VISIBLE);
                        activateVoiceMsg.setVisibility(View.GONE);
                        activateVoiceMsg.setImageDrawable(null);
                        //storageRef.getDownloadUrl().addOnCompleteListener(new O)
                        Glide.with(view.getContext())
                                .using(new FirebaseImageLoader())
                                .load(storageRef)
                                .into(imageView);
                    }

                    if(message.getContentType().equals("VOICE")) {
                        //show play button
                        activateVoiceMsg.setVisibility(View.VISIBLE);
                        //hide imageview
                        imageView.setVisibility(View.GONE);
                        imageView.setImageDrawable(null);
                        //line below will reduce padding further on play audio image if necessary
                        //individMessageLayout.setPadding(10,0,0,10);
                        activateVoiceMsg.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(message.getContentLocation());
                                storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        playSound(uri);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle any errors
                                    }
                                });

                            }
                        });
                    }
                } else {
                    activateVoiceMsg.setVisibility(View.GONE);
                    activateVoiceMsg.setImageDrawable(null);
                    imageView.setVisibility(View.GONE);
                    imageView.setImageDrawable(null);
                }
            }
        };
        mMessageList.setAdapter(mMessageListAdapter);
    }

    private void playSound(Uri uri){
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(uri.toString());
        }catch(Exception e){

        }
        mediaPlayer.prepareAsync();
        //You can show progress dialog here untill it prepared to play
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //Now dismis progress dialog, Media palyer will start playing
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // dissmiss progress bar here. It will come here when MediaPlayer
                //  is not able to play file. You can show error message to user
                return false;
            }
        });
    }

    private void initializeScreen() {
        mMessageList = (ListView) findViewById(R.id.messageListView);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mMessageField = (TextView)findViewById(R.id.messageToSend);
        mSendButton = (ImageButton)findViewById(R.id.sendButton);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        currentUserEmail = encodeEmail(mFirebaseAuth.getCurrentUser().getEmail());
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child(Constants.USERS_LOCATION);
        mMessageDatabaseReference = mFirebaseDatabase.getReference().child(Constants.MESSAGE_LOCATION
                + "/" + messageId);

        mToolBar.setTitle(chatName);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String changeTime12HrsFormat(String time) {
        Log.e("debug", "time is not null and not blank");
        String ampm = "A.M.";
        String hours = time.substring(0, 2);
        String minutes = time.substring(3, 5);
        int numHours = Integer.parseInt(hours);
        if(numHours == 12){ //if numhours is 12 then its pm
            ampm = "P.M.";
        }
        if (numHours > 12) {
            numHours -= 12;
            ampm = "P.M.";
        }
        if(numHours == 0){
            numHours = 12;
        }
        if(numHours >= 10) {
            hours = Integer.toString(numHours);
        } else {
            hours = String.format("0%s", Integer.toString(numHours));
        }

        time = String.format("%s:%s %s", hours, minutes, ampm);
        Log.e("debug", String.format("time = %s", time));
        return time;
    }

    //TODO: Used in multiple places, should probably move to its own class
    public String encodeEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

}