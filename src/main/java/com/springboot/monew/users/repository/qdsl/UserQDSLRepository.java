package com.springboot.monew.users.repository.qdsl;

import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.util.List;

public interface UserQDSLRepository {

  List<User> findUsersDeletedBefore(Instant cutoff);
}
