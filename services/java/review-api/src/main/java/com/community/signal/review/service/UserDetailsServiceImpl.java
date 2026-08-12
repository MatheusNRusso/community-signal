package com.community.signal.review.service;

import com.community.signal.review.repository.ReviewUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ReviewUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loading_user_by_username username={}", username);

        return userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("user_not_found username={}", username);
                return new UsernameNotFoundException("user.not.found");
            });
    }
}
