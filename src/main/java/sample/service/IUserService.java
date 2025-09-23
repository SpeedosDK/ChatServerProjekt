package sample.service;
import sample.domain.User;

public interface IUserService {
    boolean register(User user);
    User login(String username, String password);
}
