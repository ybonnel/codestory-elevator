package com.mycompany.model;


import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Transient;
import com.google.gson.annotations.Expose;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

@Entity("beers")
public class Beer {

    @Id
    private ObjectId internalId;

    @Transient
    private String id;

    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Beer prepareForJson() {
        setId(internalId.toString());
        internalId = null;
        return this;
    }

    public Beer prepareForDb() {
        if (id != null) {
            internalId = new ObjectId(id);
        }
        return this;
    }
}
