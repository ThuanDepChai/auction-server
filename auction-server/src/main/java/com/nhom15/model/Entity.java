package com.nhom15.model;

public abstract class Entity {

  protected int id;
  protected long createdAt;
  protected long updatedAt;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }
}
