package sample.service;

import sample.domain.User;

public interface IUserRepository {
    boolean registerUser(User user);
    User findByUsername(String username);
}
