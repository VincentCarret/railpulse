
package altn72.railpulse.service;

import altn72.railpulse.modele.User;
import altn72.railpulse.modele.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class
AuthService implements UserDetailsService, UserDetailsPasswordService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable"
                ));

        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        String password = user.getPassword();
        // Existing SQL accounts may contain plain text or BCrypt without its Spring prefix.
        // DaoAuthenticationProvider upgrades them after a successful password check.
        if (password.startsWith("$2")) {
            password = "{bcrypt}" + password;
        } else if (!password.startsWith("{")) {
            password = "{noop}" + password;
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(password)
                .roles(user.getProfil().name())
                .build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails userDetails, String newPassword) {
        User user = getUserByLogin(userDetails.getUsername());
        user.setPassword(newPassword);
        return toUserDetails(userRepository.save(user));
    }

    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable"
                ));
    }
}
