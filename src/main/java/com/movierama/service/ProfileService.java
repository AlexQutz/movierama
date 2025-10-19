package com.movierama.service;

import com.movierama.entity.User;
import com.movierama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileService implements UserDetailsService {

    private final UserRepository profileRepository;


    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return profileRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Profile with username " + username + " does not exist"));
    }

}
