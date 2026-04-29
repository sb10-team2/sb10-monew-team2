package com.springboot.monew.user.repository.qdsl;

import com.springboot.monew.user.entity.User;
import java.time.Instant;
import java.util.List;

public interface UserQDSLRepository {

  List<User> findUsersDeletedBefore(Instant cutoff);
}
