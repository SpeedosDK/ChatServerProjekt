package sample.client;

import sample.domain.User;
import sample.persistence.UserRepo;
import sample.service.IUserService;
import sample.service.UserService;

public class AddUser {
    private final IUserService userService;

    public AddUser(IUserService userService) {
        this.userService = userService;
    }

    public void registerUser(User user) {
        userService.register(user);
    }

    public static void main(String[] args) {
        IUserService userService = new UserService(new UserRepo());
        AddUser addUser = new AddUser(userService);

        String username = "kaj";
        String password = "123";
        User user = new User(username, password, null, null);

        addUser.registerUser(user);
    }
}
