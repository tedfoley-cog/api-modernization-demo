package com.acme.autofinance.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/loans")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiAllowsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/loans").with(httpBasic("apiuser", "devpassword")))
                .andExpect(status().isOk());
    }

    @Test
    void apiRejectsBadCredentials() throws Exception {
        mockMvc.perform(get("/api/loans").with(httpBasic("apiuser", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/reports/portfolio").with(httpBasic("apiuser", "devpassword")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/portfolio").with(httpBasic("apiadmin", "adminpassword")))
                .andExpect(status().isOk());
    }

    @Test
    void h2ConsoleRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/h2-console").with(httpBasic("apiuser", "devpassword")))
                .andExpect(status().isForbidden());
    }

    @Test
    void openApiDocsRemainPublic() throws Exception {
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
    }
}
