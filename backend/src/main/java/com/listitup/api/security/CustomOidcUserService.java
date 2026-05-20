package com.listitup.api.security;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String username = oidcUser.getFullName();
        if (username == null && email != null) {
            username = email.split("@")[0];
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User dbUser;
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            dbUser = new User();
            dbUser.setEmail(email);
            dbUser.setUsername(username);
            dbUser.setAuthProvider("GOOGLE");
            dbUser.setHasCompletedSetup(false);

            // Promote specific user to Admin automatically
            if (email.equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                dbUser.setRole("ADMIN");
                dbUser.setHasCompletedSetup(true);
            }
            dbUser = userRepository.save(dbUser);
        } else {
            dbUser = userOptional.get();
            if (Boolean.TRUE.equals(dbUser.getIsBlocked())) {
                throw new OAuth2AuthenticationException("account_blocked");
            }

            // Ensure specific user is Admin even if they existed as Standard
            if (email.equalsIgnoreCase("alvaropueblaruisanchez@gmail.com") && !"ADMIN".equals(dbUser.getRole())) {
                dbUser.setRole("ADMIN");
                dbUser = userRepository.save(dbUser);
            }
        }

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + dbUser.getRole()));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
