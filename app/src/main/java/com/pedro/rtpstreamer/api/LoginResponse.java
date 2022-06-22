package com.pedro.rtpstreamer.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("_id")
    @Expose
    private String id;

    @SerializedName("username")
    @Expose
    private String userName;

    @SerializedName("public_key")
    @Expose
    private String publicKey;

    @SerializedName("thruyou_public_key")
    @Expose
    private String truYouPublicKey;

    /**
     *
     * @return
     * The id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param level
     * The Level
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The username
     */
    public String getUsername() {
        return userName;
    }

    /**
     *
     * @param username
     * The Username
     */
    public void setUsername(String username) {
        this.userName = username;
    }

    /**
     *
     * @return
     * The public key
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     *
     * @param publicKey
     * The public key
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     *
     * @return
     * TruYou's public key
     */
    public String getTruYouPublicKey() { return truYouPublicKey; }

    /**
     *
     * @param truYouPublicKey
     * The Username
     */
    public void setTruYouPublicKey(String truYouPublicKey) { this.truYouPublicKey = truYouPublicKey; }
}
