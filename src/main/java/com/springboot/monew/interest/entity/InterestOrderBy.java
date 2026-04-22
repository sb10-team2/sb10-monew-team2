package com.springboot.monew.interest.entity;

public enum InterestOrderBy {
  name {
    @Override
    public String getCursor(Interest interest) {
      return interest.getName();
    }
  },
  subscriberCount {
    @Override
    public String getCursor(Interest interest) {
      return String.valueOf(interest.getSubscriberCount());
    }
  };

  public abstract String getCursor(Interest interest);
}
