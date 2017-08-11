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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import apps.shark.pingme.R;
import apps.shark.pingme.model.Friend;
import apps.shark.pingme.model.Message;
import apps.shark.pingme.model.User;
import apps.shark.pingme.utils.Constants;
import apps.shark.pingme.utils.Methods;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class FriendsActivity extends AppCompatActivity {

    private String TAG = "Friends Activity";

    private ListView mListView;

    private FirebaseListAdapter mFriendListAdapter;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mCurrentUsersFriends;

    private FirebaseAuth mFirebaseAuth;
    private String mCurrentUserEmail;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friends_activity);
        initializeScreen();

        showFriendList();
    }

    private void showFriendList() {
        mFriendListAdapter = new FirebaseListAdapter<User>(this, User.class, R.layout.added_friend_item, mCurrentUsersFriends) {
            @Override
            protected void populateView(final View view, final User user, final int position) {
                Log.e("TAG", Methods.EncodeString(user.getEmail()));

                Log.e("TAG", "in onDataChange");
                ((TextView)view.findViewById(R.id.messageTextView)).setText(user.getUsername());
                ((TextView)view.findViewById(R.id.nameTextView)).setText(Methods.DecodeString(user.getEmail()));

                Methods.checkFriendsConnection(Methods.EncodeString(user.getEmail()),
                        (ImageView) view.findViewById(R.id.ivStatus), getApplicationContext());
                Log.e(TAG, "checkConnection");

                if(user.getProfilePicLocation() != null && user.getProfilePicLocation().length() > 0) {
                    StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReference().child(user.getProfilePicLocation());

                    Glide.with(view.getContext())
                            .using(new FirebaseImageLoader())
                            .load(storageRef)
                            .bitmapTransform(new CropCircleTransformation(view.getContext()))
                            .into((ImageView)view.findViewById(R.id.photoImageView));
                }
            }
        };

        mListView.setAdapter(mFriendListAdapter);
    }

    private void initializeScreen() {
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentUserEmail = Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail());

        mCurrentUsersFriends = mFirebaseDatabase.getReference().child(Constants.FRIENDS_LOCATION)
                .child(Methods.EncodeString(mCurrentUserEmail)).orderByChild("email").getRef();

        mListView = (ListView) findViewById(R.id.friendsListView);
        Toolbar mToolBar = (Toolbar) findViewById(R.id.toolbar);

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

