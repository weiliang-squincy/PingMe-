package apps.shark.pingme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import apps.shark.pingme.R;
import apps.shark.pingme.model.Chat;
import apps.shark.pingme.model.Message;
import apps.shark.pingme.model.User;
import apps.shark.pingme.utils.Constants;
import apps.shark.pingme.utils.Methods;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class ChatListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    //public static final String ANONYMOUS = "anonymous";
    public static final int RC_SIGN_IN = 1;

    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mChatDatabaseReference;

    private ListView mChatListView;
    private FirebaseListAdapter mChatAdapter;
    public String mUsername;
    private ValueEventListener mValueEventListener;
    private DatabaseReference mUserDatabaseReference;
    private FloatingActionButton fab;
    private static ArrayList<User> friendList = new ArrayList<>();
    private boolean connectionCheck = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.add_conversation);
        fab.setOnClickListener(this);

        //Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        Log.v("tag", "in ChatListActivity");

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //User is signed in
                    //Nav to ChatListActivity
                    createUser(user);
                    onSignedInInitialize(user);
                } else {
                    //User is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(
                                            AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    private void checkFriends() {
        //Check if this user has any friend
        if(mFirebaseAuth.getCurrentUser() != null) {
            DatabaseReference mCurrentUsersFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS_LOCATION
                    + "/" + Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail()));
            mCurrentUsersFriends.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    friendList.clear();

                    for (Object o : dataSnapshot.getChildren()) {
                        friendList.add(new User(o.toString()));
                    }

                    if (!friendList.isEmpty()) {
                        fab.show();
                    } else {
                        fab.hide();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    private void onSignedInInitialize(FirebaseUser user) {
        mUsername = user.getDisplayName();
        mChatDatabaseReference = mFirebaseDatabase.getReference()
                .child(Constants.USERS_LOCATION
                        + "/" + Methods.EncodeString(user.getEmail()) + "/"
                        + Constants.CHAT_LOCATION );
        mUserDatabaseReference = mFirebaseDatabase.getReference()
                .child(Constants.USERS_LOCATION);

        //Initialize screen variables
        mChatListView = (ListView) findViewById(R.id.chatListView);

        mChatAdapter = new FirebaseListAdapter<Chat>(this, Chat.class, R.layout.chat_item, mChatDatabaseReference) {
            @Override
            protected void populateView(final View view, Chat chat, final int position) {

                ((TextView) view.findViewById(R.id.messageTextView)).setText(chat.getChatName());

                final DatabaseReference messageRef =
                        mFirebaseDatabase.getReference(Constants.MESSAGE_LOCATION
                                + "/" + chat.getUid());

                final TextView latestMessage = view.findViewById(R.id.nameTextView);
                final ImageView senderPic = view.findViewById(R.id.photoImageView);
                final TextView sendTime = view.findViewById(R.id.timeTextView);

                messageRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                        Message newMsg = dataSnapshot.getValue(Message.class);
                        //latestMessage.setText(newMsg.getSender() + ": " + newMsg.getMessage());
                        latestMessage.setText(newMsg.getMessage());
                        if(!newMsg.getTimestamp().equals("") && !newMsg.getDatestamp().equals("")) {
                            if(sendTime.getVisibility() == View.GONE) sendTime.setVisibility(View.VISIBLE);
                            sendTime.setText(String.format("%s, %s", newMsg.getTimestamp(), newMsg.getDatestamp()));
                        } else {
                            sendTime.setVisibility(View.GONE);
                        }

                        mUserDatabaseReference.child(newMsg.getSender())
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        User msgSender = dataSnapshot.getValue(User.class);
                                        if(msgSender != null && msgSender.getProfilePicLocation() != null) {
                                            try {
                                                StorageReference storageRef = FirebaseStorage.getInstance()
                                                        .getReference().child(msgSender.getProfilePicLocation());
                                                Glide.with(view.getContext())
                                                        .using(new FirebaseImageLoader())
                                                        .load(storageRef)
                                                        .bitmapTransform(new CropCircleTransformation(view.getContext()))
                                                        .into(senderPic);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Log.e(TAG, e.toString());
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

            }
        };

        //Set adapter
        mChatListView.setAdapter(mChatAdapter);

        //Add on click listener to line items
        mChatListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String messageLocation = mChatAdapter.getRef(position).toString();

                if(messageLocation != null){
                    Intent intent = new Intent(view.getContext(), ChatMessagesActivity.class);
                    String messageKey = mChatAdapter.getRef(position).getKey();
                    intent.putExtra(Constants.MESSAGE_ID, messageKey);
                    Chat chatItem = (Chat)mChatAdapter.getItem(position);
                    intent.putExtra(Constants.CHAT_NAME, chatItem.getChatName());
                    startActivity(intent);
                }
            }
        });

        mValueEventListener = mChatDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Chat chat = dataSnapshot.getValue(Chat.class);
                //Check if any chats exists
                if (chat == null) {
                    return;
                }
                mChatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        checkFriends();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (resultCode == RESULT_OK) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        switch(id) {
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                connectionCheck = false;
                break;
            case R.id.listFriends:
                intent = new Intent(this, FriendsListActivity.class);
                break;
            case R.id.profilePage:
                intent = new Intent(this, ProfileActivity.class);
                break;
            case R.id.addedFriends:
                intent = new Intent(this, FriendsActivity.class);
                break;
        }

        if(intent != null) {
            startActivity(intent);
        }

        return false;
    }

    private void createUser(FirebaseUser user) {
        final DatabaseReference usersRef = mFirebaseDatabase.getReference(Constants.USERS_LOCATION);
        final String encodedEmail = Methods.EncodeString(user.getEmail());
        final DatabaseReference userRef = usersRef.child(encodedEmail);
        final String username = user.getDisplayName();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    User newUser = new User(username, Methods.EncodeString(encodedEmail));
                    userRef.setValue(newUser);
                    userRef.child(newUser.getEmail()).setValue(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getDetails());
            }
        });

        if(!connectionCheck) {
            Methods.checkConnection(Methods.EncodeString(user.getEmail()));
            connectionCheck = true;
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId())
        {
            case R.id.add_conversation:
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
        }
    }
}
