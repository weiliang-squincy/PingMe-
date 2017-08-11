package apps.shark.pingme.ui;

/**
 * Created by Harsha on 7/18/2017.
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import apps.shark.pingme.R;
import apps.shark.pingme.model.Friend;
import apps.shark.pingme.model.Message;
import apps.shark.pingme.model.User;
import apps.shark.pingme.utils.Constants;
import apps.shark.pingme.utils.Methods;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class FriendsListActivity extends AppCompatActivity {

    private String TAG = "Friends List Activity";

    private ListView mListView;
    private Toolbar mToolBar;

    private FirebaseListAdapter mFriendListAdapter;
    private ValueEventListener mValueEventListener;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mCurrentUsersFriends;
    private FirebaseAuth mFirebaseAuth;

    private final List<String> mUsersFriends = new ArrayList<>();
    private String mCurrentUserEmail;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friends_activity);
        initializeScreen();

        showUserList();
    }

    private void showUserList(){
        Query query = mUserDatabaseReference.orderByChild(Methods.EncodeString(mCurrentUserEmail)).equalTo(null);

        mFriendListAdapter = new FirebaseListAdapter<User>(this, User.class, R.layout.friend_item, query) {
            @Override
            protected void populateView(final View view, final User user, final int position) {
                final String email = user.getEmail();

                //Check if this user is already your friend
                final DatabaseReference friendRef =
                        mFirebaseDatabase.getReference(Constants.FRIENDS_LOCATION
                                + "/" + mCurrentUserEmail + "/" + Methods.EncodeString(email));

                friendRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue() != null){
                            view.findViewById(R.id.addFriend).setVisibility(View.GONE);
                            view.findViewById(R.id.removeFriend).setVisibility(View.VISIBLE);
                        } else {
                            view.findViewById(R.id.removeFriend).setVisibility(View.GONE);
                            view.findViewById(R.id.addFriend).setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                if(user.getProfilePicLocation() != null && user.getProfilePicLocation().length() > 0){
                    StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReference().child(user.getProfilePicLocation());
                    Glide.with(view.getContext())
                            .using(new FirebaseImageLoader())
                            .load(storageRef)
                            .bitmapTransform(new CropCircleTransformation(view.getContext()))
                            .into((ImageView)view.findViewById(R.id.photoImageView));
                }

                ((TextView)view.findViewById(R.id.messageTextView)).setText(user.getUsername());
                ((TextView)view.findViewById(R.id.nameTextView)).setText(email);
                (view.findViewById(R.id.addFriend)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.w(TAG, "Adding friend");
                        Log.w(TAG, "Clicking row: " + position);
                        Log.w(TAG, "Clicking user: " + email);
                        //Add this user to your friends list, by email
                        addNewFriend(user);
                    }
                });
                (view.findViewById(R.id.removeFriend)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.w(TAG, "Removing friend");
                        Log.w(TAG, "Clicking row: " + position);
                        Log.w(TAG, "Clicking user: " + email);
                        //Add this user to your friends list, by email
                        removeFriend(user);
                    }
                });
            }
        };

        mListView.setAdapter(mFriendListAdapter);

        mValueEventListener = mUserDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(user == null) {
                    finish();
                    return;
                }

                mFriendListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeFriend(User friend){
        //Get current user logged in by email
        String userLoggedIn = mFirebaseAuth.getCurrentUser().getEmail();
        DatabaseReference friendsRef = mFirebaseDatabase.getReference(Constants.FRIENDS_LOCATION
                + "/" + Methods.EncodeString(userLoggedIn));
        friendsRef.child(Methods.EncodeString(friend.getEmail())).removeValue();
    }

    private void addNewFriend(User newFriend){
        //Get current user logged in by email
        final String userLoggedIn = mFirebaseAuth.getCurrentUser().getEmail();

        final DatabaseReference friendsRef = mFirebaseDatabase.getReference(Constants.FRIENDS_LOCATION
                + "/" + Methods.EncodeString(userLoggedIn));
        //Add friends to current users friends list
        friendsRef.child(Methods.EncodeString(newFriend.getEmail())).setValue(newFriend);
    }

    private void initializeScreen() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentUserEmail = Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail().toString());
        //Eventually this list will filter out users that are already your friend
        mUserDatabaseReference = mFirebaseDatabase.getReference().child(Constants.USERS_LOCATION);

        mCurrentUsersFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS_LOCATION
                + "/" + Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail()));

        mListView = (ListView) findViewById(R.id.friendsListView);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);

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
}

