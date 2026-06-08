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
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String username = oauth2User.getAttribute("login");
        if (username == null) {
            username = oauth2User.getAttribute("name");
        }
        if (username == null && email != null) {
            username = email.split("@")[0];
        }

        if (email == null) {
            // Sometimes GitHub emails are private, ideally we'd fetch them from the /emails endpoint,
            // but for simplicity we rely on the user having a public email or use the login.
            // If email is truly null, we can try to mock one or fail.
            email = username + "@github.local"; 
        }

        User dbUser;
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            dbUser = new User();
            dbUser.setEmail(email);
            dbUser.setUsername(username);
            dbUser.setAuthProvider("GITHUB");
            dbUser.setHasCompletedSetup(false);

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

            if (email.equalsIgnoreCase("alvaropueblaruisanchez@gmail.com") && !"ADMIN".equals(dbUser.getRole())) {
                dbUser.setRole("ADMIN");
                dbUser = userRepository.save(dbUser);
            }
        }

        Set<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + dbUser.getRole()));

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        
        // IMPORTANT: Controllers look up the user by calling oauthUser.getAttribute("email").
        // If the email was private on GitHub, we generated a fallback one, but we MUST
        // inject it into the attributes map returned by the UserDetailsService, otherwise
        // it will still be null in the controllers.
        java.util.Map<String, Object> attributes = new java.util.HashMap<>(oauth2User.getAttributes());
        if (attributes.get("email") == null) {
            attributes.put("email", email);
        }
        
        return new DefaultOAuth2User(authorities, attributes, userNameAttributeName);
    }
}
