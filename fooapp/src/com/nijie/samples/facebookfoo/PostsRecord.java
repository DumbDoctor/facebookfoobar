package com.nijie.samples.facebookfoo;

/**
 * Created by Ni Jie on 2/14/2015.
 */
public final class PostsRecord {

    private String id=null;
    private String name=null;
    private String message=null;
    private String updated_time=null;
    private boolean published=true;

    public PostsRecord(String id, String name, String message, String updated_time, boolean published){
        this.id = id;
        this.name = name;
        this.message = message;
        this.updated_time = updated_time;
        this.published = published;
    }

    public String getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getMessage() {
        return this.message;
    }

    public String getUpdated_time(){
        return this.updated_time;
    }

    public boolean isPublished(){
        return this.published;
    }

}
