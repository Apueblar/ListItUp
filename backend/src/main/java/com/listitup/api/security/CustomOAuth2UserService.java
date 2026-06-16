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

        final String githubLogin = username; // save the login handle before any fallback

        if (email == null) {
            // GitHub email is private — try to find an existing account by GitHub login first
            // to avoid creating duplicate ghost rows on every login.
            Optional<User> byLogin = userRepository.findFirstByUsernameAndAuthProvider(githubLogin, "GITHUB");
            if (byLogin.isPresent()) {
                User existing = byLogin.get();
                if (Boolean.TRUE.equals(existing.getIsBlocked())) {
                    throw new OAuth2AuthenticationException("account_blocked");
                }
                Set<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + existing.getRole()));
                String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
                java.util.Map<String, Object> attrs = new java.util.HashMap<>(oauth2User.getAttributes());
                attrs.put("email", existing.getEmail());
                return new DefaultOAuth2User(authorities, attrs, userNameAttributeName);
            }
            // No existing account — generate a stable placeholder email
            email = githubLogin + "@github.local";
        }

        User dbUser;
        Optional<User> userOptional = userRepository.findFirstByEmail(email);

        if (userOptional.isEmpty()) {
            dbUser = new User();
            dbUser.setEmail(email);
            dbUser.setAuthProvider("GITHUB");
            dbUser.setHasCompletedSetup(false);

            // Resolve a unique username — GitHub login may clash with an existing account from another provider.
            // We'll suggest a de-conflicted name; the user can always change it on the setup screen.
            String resolvedUsername = username;
            if (userRepository.findFirstByUsername(resolvedUsername).isPresent()) {
                resolvedUsername = username + "_gh";
                int counter = 2;
                while (userRepository.findFirstByUsername(resolvedUsername).isPresent()) {
                    resolvedUsername = username + "_gh" + counter;
                    counter++;
                }
            }
            dbUser.setUsername(resolvedUsername);

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
