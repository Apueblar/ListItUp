package com.listitup.api.security;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oAuth2User.getAttribute("email");
        String username = oAuth2User.getAttribute("name");
        if (username == null) {
            username = oAuth2User.getAttribute("login"); // GitHub uses 'login' sometimes
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User dbUser;
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            dbUser = new User();
            dbUser.setEmail(email);
            dbUser.setUsername(username != null ? username : email.split("@")[0]);
            dbUser.setAuthProvider(registrationId.toUpperCase());
            
            // Promote specific user to Admin automatically
            if (email.equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                dbUser.setRole("ADMIN");
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

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + dbUser.getRole()));

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), userNameAttributeName);
    }
}
