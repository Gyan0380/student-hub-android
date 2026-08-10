package com.studenthub.app.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps to a document under Users/{uid} in Firestore — same collection and
 * field names the Web App writes on registration (see app.js renderRegister).
 */
public class UserModel {

    private String uid;
    private String fullName;
    private String username;
    private String dob;
    private String classLevel;
    private String schoolName;
    private String profilePhoto;
    private String bio;
    private String role;
    private List<String> classAccess;
    private boolean isBanned;

    public UserModel() {
        // Required empty constructor for Firestore deserialization
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getClassLevel() { return classLevel; }
    public void setClassLevel(String classLevel) { this.classLevel = classLevel; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getClassAccess() { return classAccess; }
    public void setClassAccess(List<String> classAccess) { this.classAccess = classAccess; }

    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }

    @Exclude
    public boolean isAdminOrOwner() {
        return "Admin".equals(role) || "Owner".equals(role);
    }

    @Exclude
    public List<String> classAccessOrDefault() {
        if (classAccess != null && !classAccess.isEmpty()) return classAccess;
        List<String> fallback = new ArrayList<>();
        fallback.add(classLevel != null ? classLevel : "Class 9");
        return fallback;
    }
}
