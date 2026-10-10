package dev.agenda.legacy.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public LegacyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .map(LegacyUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
