package Fleet.check.security;

import Fleet.check.entity.User;
import Fleet.check.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findById(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        String roleName = user.getRole() != null ? "ROLE_" + user.getRole().getName().toUpperCase() : "ROLE_DRIVER";
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPinHash() != null ? user.getPinHash() : "")
                .authorities(List.of(new SimpleGrantedAuthority(roleName)))
                .build();
    }
}
