package apps.shark.pingme.ui;

/**
 * Created by Harsha on 7/18/2017.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.util.UUID;

import apps.shark.pingme.R;
import apps.shark.pingme.model.User;
import apps.shark.pingme.utils.Constants;
import apps.shark.pingme.utils.Methods;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class ProfileActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private ImageButton mphotoPickerButton;
    private static final int GALLERY_INTENT=2;
    private ProgressDialog mProgress;
    private StorageReference mStorage;
    private FirebaseAuth mFirebaseAuth;
    private String currentUserEmail;
    private ImageView profileImage;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mCurrentUserDatabaseReference;
    private Context mView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        mView = ProfileActivity.this;
        initializeScreen();
        openImageSelector();
        initializeUserInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data){

        mStorage = FirebaseStorage.getInstance().getReference(); //make global
        super.onActivityResult(requestCode, requestCode, data);

        if(requestCode == GALLERY_INTENT && resultCode == RESULT_OK){

            mProgress.setMessage("Uploading...");
            mProgress.show();

            Uri uri = data.getData();
            //Keep all images for a specific chat grouped together
            final String imageLocation = "Photos/profile_picture/" + currentUserEmail;
            final String imageLocationId = imageLocation + "/" + uri.getLastPathSegment();
            final String uniqueId = UUID.randomUUID().toString();
            final StorageReference filepath = mStorage.child(imageLocation).child(uniqueId + "/profile_pic");
            final String downloadURl = filepath.getPath();
            filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //create a new message containing this image
                    addImageToProfile(downloadURl);
                    mProgress.dismiss();
                }
            });
        }

    }

    public void addImageToProfile(final String imageLocation){
        final ImageView imageView = (ImageView) findViewById(R.id.profilePicture);
        mCurrentUserDatabaseReference
                .child("profilePicLocation").setValue(imageLocation).addOnCompleteListener(
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReference().child(imageLocation);
                        Glide.with(mView)
                                .using(new FirebaseImageLoader())
                                .load(storageRef)
                                .bitmapTransform(new CropCircleTransformation(mView))
                                .into(imageView);
                    }
                }
        );

    }

    public void openImageSelector(){

        mphotoPickerButton = (ImageButton) findViewById(R.id.imageButton);
        mProgress = new ProgressDialog(this);
        mphotoPickerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_INTENT);
                //mView = view;
            }
        });
    }

    private void initializeUserInfo(){
        final ImageView imageView = (ImageView) findViewById(R.id.profilePicture);
        final TextView tvUsername = (TextView) findViewById(R.id.tvUsername);
        final TextView tvEmail = (TextView) findViewById(R.id.tvEmail);

        mCurrentUserDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                try {

                    if(user.getProfilePicLocation() != null) {
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReference().child(user.getProfilePicLocation());

                        Glide.with(mView)
                                .using(new FirebaseImageLoader())
                                .load(storageRef)
                                .bitmapTransform(new CropCircleTransformation(mView))
                                .into(imageView);
                    }

                    if(!(user.getUsername() == null)) {
                        tvUsername.setText(String.format("username: %s", user.getUsername()));
                    }
                    tvEmail.setText(String.format("e-mail: %s", user.getEmail()));

                } catch (Exception e) {
                    Log.e("Err", e.toString());
                    e.printStackTrace();
                }
             }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initializeScreen(){
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mCurrentUserDatabaseReference = mFirebaseDatabase
                .getReference().child(Constants.USERS_LOCATION
                        + "/" + Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail()));
        currentUserEmail = Methods.EncodeString(mFirebaseAuth.getCurrentUser().getEmail());

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
