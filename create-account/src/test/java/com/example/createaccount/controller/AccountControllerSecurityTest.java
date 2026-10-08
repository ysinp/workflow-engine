package com.example.createaccount.controller;

import com.example.createaccount.config.CorsConfig;
import com.example.createaccount.config.JwtService;
import com.example.createaccount.config.SecurityConfig;
import com.example.createaccount.dto.AccountResponse;
import com.example.createaccount.dto.CreateAccountRequest;
import com.example.createaccount.dto.SignInResponse;
import com.example.createaccount.exception.InvalidCredentialsException;
import com.example.createaccount.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real SecurityFilterChain + JwtAuthFilter + CORS config + GlobalExceptionHandler
 * over MockMvc, with only the service layer mocked.
 */
@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtService.class})
class AccountControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AccountService accountService;

    private static final AccountResponse SAMPLE = new AccountResponse(
            UUID.fromString("b3f1c2e4-0000-4000-8000-000000000001"), "Jane Doe", "jane@example.com",
            Instant.parse("2026-09-26T12:00:00Z"), "US", "Software Engineer", "50K_TO_100K", "****4567",
            "CUSTOMER", new java.math.BigDecimal("5000.00"));

    private static final String SIGNUP_JSON = """
            {
              "fullName": "Jane Doe", "email": "jane@example.com", "password": "correct-horse-battery",
              "dateOfBirth": "1990-05-17", "nationality": "US", "phone": "+1-555-0100",
              "addressLine1": "1 Main St", "addressLine2": null, "city": "Springfield", "state": "IL",
              "postalCode": "62701", "country": "US", "idType": "PASSPORT", "idNumber": "A1234567",
              "occupation": "Software Engineer", "annualIncome": "50K_TO_100K", "sourceOfFunds": "EMPLOYMENT"
            }
            """;

    // ------------------------------------------------------------ protected endpoints

    @Test
    void protectedEndpoint_withoutToken_returnsJson401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void protectedEndpoint_withInvalidToken_behavesLikeNoToken() throws Exception {
        mockMvc.perform(get("/api/accounts/" + SAMPLE.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withReviewerToken_returns200() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of(SAMPLE));
        String token = jwtService.generateToken("jane@example.com", "REVIEWER");

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));
    }

    @Test
    void protectedEndpoint_withCustomerToken_returns403() throws Exception {
        String token = jwtService.generateToken("jane@example.com", "CUSTOMER");

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ public endpoints

    @Test
    void signup_isPublic_returns201WithMinimalAccountView() throws Exception {
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(SAMPLE);

        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SAMPLE.id().toString()))
                .andExpect(jsonPath("$.nationality").value("US"))
                .andExpect(jsonPath("$.occupation").value("Software Engineer"))
                .andExpect(jsonPath("$.annualIncome").value("50K_TO_100K"))
                .andExpect(jsonPath("$.idNumberMasked").value("****4567"))
                .andExpect(jsonPath("$.idNumber").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void signup_withShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane\",\"email\":\"jane@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane\",\"email\":\"not-an-email\",\"password\":\"long-enough-pw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signup_withMalformedDateOfBirth_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane\",\"email\":\"jane@example.com\","
                                + "\"password\":\"long-enough-pw\",\"dateOfBirth\":\"17/05/1990\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void signin_success_returnsTokenAndAccount() throws Exception {
        when(accountService.signIn(any())).thenReturn(new SignInResponse("jwt-token", SAMPLE));

        mockMvc.perform(post("/api/accounts/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@example.com\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.account.email").value("jane@example.com"))
                .andExpect(jsonPath("$.account.idNumberMasked").value("****4567"));
    }

    @Test
    void signin_failure_returnsGeneric401() throws Exception {
        when(accountService.signIn(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/accounts/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void exists_isPublic() throws Exception {
        when(accountService.emailExists("jane@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/accounts/exists").param("email", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    // ------------------------------------------------------------ CORS through the security chain

    @Test
    void corsPreflight_forProtectedEndpointWithAuthorizationHeader_isAllowedWithoutToken() throws Exception {
        mockMvc.perform(options("/api/accounts")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"));
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/accounts")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
