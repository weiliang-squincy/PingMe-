package apps.shark.pingme.model;

/**
 * Created by Harsha on 7/18/2017.
 */

import java.util.HashMap;
import java.util.List;

public class User {

    private String username;
    private String email;
    private String profilePicLocation;

    public User(){

    }

    public User(String email) {
        this.email = email;
    }

    public User(String name, String email){
        this.username = name;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicLocation() {
        return profilePicLocation;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return String.format("Username = %s, email = %s", getUsername(), getEmail());
    }
}
