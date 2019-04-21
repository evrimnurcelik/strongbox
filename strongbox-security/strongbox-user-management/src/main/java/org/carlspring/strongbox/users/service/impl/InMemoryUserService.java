package org.carlspring.strongbox.users.service.impl;

import org.carlspring.strongbox.data.CacheName;
import org.carlspring.strongbox.users.domain.User;
import org.carlspring.strongbox.users.domain.Users;
import org.carlspring.strongbox.users.dto.UserAccessModelDto;
import org.carlspring.strongbox.users.dto.UserDto;
import org.carlspring.strongbox.users.dto.UserReadContract;
import org.carlspring.strongbox.users.dto.UsersDto;
import org.carlspring.strongbox.users.security.AuthoritiesProvider;
import org.carlspring.strongbox.users.security.SecurityTokenProvider;
import org.carlspring.strongbox.users.service.UserService;

import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jose4j.lang.JoseException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Component
@InMemoryUserService.InMemoryUserServiceQualifier
public class InMemoryUserService implements UserService
{

    protected Map<String, UserDto> userMap = new ConcurrentHashMap<>();

    private final ReadWriteLock usersLock = new ReentrantReadWriteLock();

    @Inject
    private SecurityTokenProvider tokenProvider;

    @Inject
    private AuthoritiesProvider authoritiesProvider;

    @Override
    public Users findAll()
    {
        final Lock readLock = usersLock.readLock();
        readLock.lock();

        try
        {
            Set<UserDto> userSet = new HashSet<>(userMap.values());
            
            return new Users(new UsersDto(userSet));
        }
        finally
        {
            readLock.unlock();
        }
    }
    
    @Override
    public User findByUserName(final String username)
    {
        if (username == null)
        {
            return null;
        }
        
        final Lock readLock = usersLock.readLock();
        readLock.lock();

        try
        {
            Optional<UserDto> optionalUserDto = Optional.ofNullable(userMap.get(username));

            if (optionalUserDto.isPresent())
            {
                UserDto userDto = optionalUserDto.get();

                Set<String> authorities = userDto.getRoles()
                                                 .stream()
                                                 .map(this::getGrantedAuthorities)
                                                 .map(this::getAuthoritiesAsString)
                                                 .flatMap(Collection::stream)
                                                 .collect(Collectors.toCollection(HashSet::new));

                if (authorities.size() > 0)
                {
                    userDto.setAuthorities(authorities);
                    optionalUserDto = Optional.of(userDto);
                }
            }

            return optionalUserDto.map(User::new).orElse(null);
        }
        finally
        {
            readLock.unlock();
        }
    }

    @Override
    public String generateSecurityToken(final String username)
            throws JoseException
    {
        final User user = findByUserName(username);

        if (StringUtils.isEmpty(user.getSecurityTokenKey()))
        {
            return null;
        }

        final Map<String, String> claimMap = new HashMap<>();
        claimMap.put(User.SECURITY_TOKEN_KEY, user.getSecurityTokenKey());

        return tokenProvider.getToken(username, claimMap, null);
    }

    @Override
    public String generateAuthenticationToken(final String username,
                                              final Integer expireMinutes)
            throws JoseException
    {
        return tokenProvider.getToken(username, Collections.emptyMap(), expireMinutes);
    }

    @Override
    public void revokeEveryone(final String roleToRevoke)
    {
        modifyInLock(users -> {
            users.values().forEach(user -> user.removeRole(roleToRevoke));
        });
    }

    @Override
    @CacheEvict(cacheNames = CacheName.User.AUTHENTICATIONS, key = "#p0.username")
    public void save(final UserReadContract user)
    {
        modifyInLock(users -> {
            UserDto u = Optional.ofNullable(users.get(user.getUsername())).orElseGet(() -> new UserDto());

            if (!StringUtils.isBlank(user.getPassword()))
            {
                u.setPassword(user.getPassword());
            }
            u.setUsername(user.getUsername());
            u.setEnabled(user.isEnabled());
            u.setRoles(user.getRoles());
            u.setSecurityTokenKey(user.getSecurityTokenKey());
            u.setUserAccessModel(user.getUserAccessModel());
            
            users.putIfAbsent(user.getUsername(), u);
        });
    }

    @Override
    public void delete(final String username)
    {
        modifyInLock(users -> {
            users.remove(username);
        });
    }

    @Override
    public void updateAccessModel(final String username,
                                  final UserAccessModelDto accessModel)
    {
        modifyInLock(users -> {
            Optional.ofNullable(users.get(username))
                    .ifPresent(u -> u.setUserAccessModel(accessModel));
        });
    }

    @Override
    public void updatePassword(final UserDto userToUpdate)
    {
        if (StringUtils.isBlank(userToUpdate.getPassword()))
        {
            return;
        }

        modifyInLock(users -> {
            Optional.ofNullable(users.get(userToUpdate.getUsername()))
                    .ifPresent(user -> user.setPassword(userToUpdate.getPassword()));
        });
    }

    @Override
    public void updateSecurityToken(final UserDto userToUpdate)
    {
        modifyInLock(users -> {
            Optional.ofNullable(users.get(userToUpdate.getUsername()))
                    .ifPresent(user -> updateSecurityToken(user, userToUpdate.getSecurityTokenKey()));
        });
    }

    @Override
    public void updateAccountDetailsByUsername(UserDto userToUpdate)
    {
        modifyInLock(users -> {
            Optional.ofNullable(users.get(userToUpdate.getUsername()))
                    .ifPresent(user -> {
                        if (!StringUtils.isBlank(userToUpdate.getPassword()))
                        {
                            user.setPassword(userToUpdate.getPassword());
                        }
                        
                        updateSecurityToken(user, userToUpdate.getSecurityTokenKey());
                    });
        });
    }

    private void updateSecurityToken(final UserDto user,
                                     final String securityToken)
    {
        if (StringUtils.isNotBlank(securityToken))
        {
            user.setSecurityTokenKey(securityToken);
        }
    }

    protected void modifyInLock(final Consumer<Map<String, UserDto>> operation)
    {
        final Lock writeLock = usersLock.writeLock();
        writeLock.lock();

        try
        {
            operation.accept(userMap);

        }
        finally
        {
            writeLock.unlock();
        }
    }

    private Set<GrantedAuthority> getGrantedAuthorities(String role)
    {
        return authoritiesProvider.getAuthoritiesByRoleName(role);
    }

    private Set<String> getAuthoritiesAsString(Set<GrantedAuthority> authorities)
    {
        return authorities.stream()
                          .map(GrantedAuthority::getAuthority)
                          .collect(Collectors.toCollection(HashSet::new));
    }

    @Documented
    @Retention(RUNTIME)
    @Qualifier
    public @interface InMemoryUserServiceQualifier
    {
        String value() default "inMemoryUserServiceQualifier";
    }

}
