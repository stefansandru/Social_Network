package com.example.social_network.domain;

public class Entity<ID> {
    private ID id;

    public Entity() {
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}