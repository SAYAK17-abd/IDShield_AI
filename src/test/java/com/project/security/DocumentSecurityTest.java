package com.project.security;

import com.project.document.entity.Document;
import com.project.document.repository.DocumentRepository;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import com.project.verification.repository.VerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSecurityTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentSecurity documentSecurity;

    private User ownerUser;
    private User otherUser;
    private User investigatorUser;
    private User adminUser;
    private Document document;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().id(101L).email("owner@example.com").role(Role.ROLE_USER).build();
        otherUser = User.builder().id(102L).email("other@example.com").role(Role.ROLE_USER).build();
        investigatorUser = User.builder().id(201L).email("investigator@example.com").role(Role.ROLE_INVESTIGATOR).build();
        adminUser = User.builder().id(301L).email("admin@example.com").role(Role.ROLE_ADMIN).build();

        document = Document.builder().id(501L).owner(ownerUser).build();
    }

    @Test
    void canAccessDocument_Owner_ShouldReturnTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "owner@example.com", "password", ownerUser.getAuthorities()
        );
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(documentRepository.findById(501L)).thenReturn(Optional.of(document));

        assertTrue(documentSecurity.canAccessDocument(501L, auth));
    }

    @Test
    void canAccessDocument_OtherUser_ShouldReturnFalse_IdorDefense() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "other@example.com", "password", otherUser.getAuthorities()
        );
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(documentRepository.findById(501L)).thenReturn(Optional.of(document));

        assertFalse(documentSecurity.canAccessDocument(501L, auth));
    }

    @Test
    void canAccessDocument_Investigator_ShouldReturnTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "investigator@example.com", "password", investigatorUser.getAuthorities()
        );
        when(userRepository.findByEmail("investigator@example.com")).thenReturn(Optional.of(investigatorUser));

        assertTrue(documentSecurity.canAccessDocument(501L, auth));
    }

    @Test
    void canAccessDocument_Admin_ShouldReturnTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "password", adminUser.getAuthorities()
        );
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        assertTrue(documentSecurity.canAccessDocument(501L, auth));
    }
}
