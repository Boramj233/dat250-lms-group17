package no.hvl.dat250.lms.user;

import java.nio.charset.StandardCharsets;

import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** POST /api/admin/users/import: CSV class list -> local profiles, with a report per row. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserImportApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    private User admin;

    @BeforeEach
    void createAdmin() {
        admin = new User("admin@example.com", "System", "Administrator");
        admin.setAccess(Access.ADMIN);
        users.save(admin);
    }

    private static MockMultipartHttpServletRequestBuilder upload(User actor, String csv) {
        return upload(actor, csv.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartHttpServletRequestBuilder upload(User actor, byte[] csv) {
        var builder = multipart("/api/admin/users/import")
                .file(new MockMultipartFile("file", "users.csv", "text/csv", csv));
        if (actor != null) {
            builder.header("Authorization", TestTokens.bearer(actor));
        }
        return builder;
    }

    @Test
    void createsProfilesAndReportsEveryRow() throws Exception {
        users.save(new User("existing@example.com", "Old", "Name"));
        String csv = """
                email,firstName,lastName,access
                kari@example.com,Kari,Hansen,COURSE_MANAGER
                ola@example.com,Ola,Nordmann,
                existing@example.com,New,Name,ADMIN
                not-an-email,Per,Pettersen,STANDARD
                ola@example.com,Ola,Igjen,STANDARD
                lise@example.com,,Lie,BOSS
                """;
        mvc.perform(upload(admin, csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.failed").value(3))
                .andExpect(jsonPath("$.rows", hasSize(6)))
                .andExpect(jsonPath("$.rows[0].line").value(2))
                .andExpect(jsonPath("$.rows[0].status").value("CREATED"))
                .andExpect(jsonPath("$.rows[0].userId").isNumber())
                .andExpect(jsonPath("$.rows[2].status").value("ALREADY_EXISTS"))
                .andExpect(jsonPath("$.rows[3].status").value("INVALID"))
                .andExpect(jsonPath("$.rows[3].errors[0]", startsWith("email:")))
                .andExpect(jsonPath("$.rows[4].status").value("DUPLICATE_IN_FILE"))
                .andExpect(jsonPath("$.rows[5].status").value("INVALID"))
                .andExpect(jsonPath("$.rows[5].errors", hasSize(2)))
                .andExpect(jsonPath("$.rows[5].errors", hasItem(startsWith("access:"))))
                .andExpect(jsonPath("$.rows[5].errors", hasItem(startsWith("firstName:"))));

        assertEquals(Access.COURSE_MANAGER, users.findByEmail("kari@example.com").orElseThrow().getAccess());
        assertEquals(Access.STANDARD, users.findByEmail("ola@example.com").orElseThrow().getAccess());
        assertEquals("Nordmann", users.findByEmail("ola@example.com").orElseThrow().getLastName());
        // Existing profiles are never changed by an import.
        User existing = users.findByEmail("existing@example.com").orElseThrow();
        assertEquals(Access.STANDARD, existing.getAccess());
        assertEquals("Old", existing.getFirstName());
        assertTrue(users.findByEmail("not-an-email").isEmpty());
        assertTrue(users.findByEmail("lise@example.com").isEmpty());
    }

    @Test
    void importingTheSameFileAgainCreatesNothingNew() throws Exception {
        String csv = "email,firstName,lastName\nkari@example.com,Kari,Hansen\n";
        mvc.perform(upload(admin, csv)).andExpect(jsonPath("$.created").value(1));
        mvc.perform(upload(admin, csv))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void readsExcelStyleFilesWithSemicolonsBomQuotesAndAnyColumnOrder() throws Exception {
        // Excel with Norwegian settings: UTF-8 BOM, semicolons, CRLF; columns reordered.
        String csv = "﻿Access;LastName;FirstName;Email\r\n"
                + "course_manager;\"Hansen; Berg\";Kåre;Kare.Hansen@Example.com\r\n"
                + "\r\n";
        mvc.perform(upload(admin, csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.rows[0].email").value("kare.hansen@example.com"));
        // Stored in lower case, as Keycloak reports emails at login.
        User kare = users.findByEmail("kare.hansen@example.com").orElseThrow();
        assertEquals("Kåre", kare.getFirstName());
        assertEquals("Hansen; Berg", kare.getLastName());
        assertEquals(Access.COURSE_MANAGER, kare.getAccess());
    }

    @Test
    void fileWithoutRequiredColumnsIsRejectedWith400() throws Exception {
        mvc.perform(upload(admin, "mail,name\nkari@example.com,Kari\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("email")));
    }

    @Test
    void fileThatIsNotUtf8IsRejectedWith400() throws Exception {
        byte[] latin1 = "email,firstName,lastName\nk@example.com,Kåre,Hansen\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        mvc.perform(upload(admin, latin1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("UTF-8")));
    }

    @Test
    void unclosedQuoteIsRejectedWith400() throws Exception {
        mvc.perform(upload(admin, "email,firstName,lastName\n\"kari@example.com,Kari,Hansen\n"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Line 2")));
    }

    @Test
    void onlyAdminsMayImport() throws Exception {
        User teacher = new User("teacher@example.com", "Demo", "Teacher");
        teacher.setAccess(Access.COURSE_MANAGER);
        users.save(teacher);
        String csv = "email,firstName,lastName\nkari@example.com,Kari,Hansen\n";

        mvc.perform(upload(teacher, csv)).andExpect(status().isForbidden());
        mvc.perform(upload(null, csv)).andExpect(status().isUnauthorized());
        assertTrue(users.findByEmail("kari@example.com").isEmpty());
    }
}
