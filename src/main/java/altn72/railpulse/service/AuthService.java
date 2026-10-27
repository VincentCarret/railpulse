
package altn72.railpulse.service;

import altn72.railpulse.modele.User;
import altn72.railpulse.modele.UserRepository;
import altn72.railpulse.modele.FormatMotDePasse;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService, UserDetailsPasswordService {

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
        if (!FormatMotDePasse.estBcrypt(user.getPassword())) {
            throw new UsernameNotFoundException("Ce compte nécessite une migration ou une réinitialisation du mot de passe.");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(FormatMotDePasse.normaliser(user.getPassword()))
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
