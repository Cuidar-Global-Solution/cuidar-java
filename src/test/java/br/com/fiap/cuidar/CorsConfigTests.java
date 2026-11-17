package br.com.fiap.cuidar;

import br.com.fiap.cuidar.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTests {

    private MockHttpServletResponse request(String origins, String origin,
            String method, String requestedMethod, String requestedHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/pessoas-acompanhadas");
        request.addHeader("Origin", origin);
        if (requestedMethod != null) {
            request.addHeader("Access-Control-Request-Method", requestedMethod);
        }
        if (requestedHeader != null) {
            request.addHeader("Access-Control-Request-Headers", requestedHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsConfig().corsFilter(origins).doFilter(request, response,
                (req, res) -> ((MockHttpServletResponse) res).setStatus(204));
        return response;
    }

    @Test
    void allowsExactOriginsAndRequiredPreflightMethods() throws Exception {
        for (String method : new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"}) {
            var response = request(" https://front.example, , https://admin.example ",
                    "https://admin.example", "OPTIONS", method, "Content-Type");
            assertEquals(200, response.getStatus());
            assertEquals("https://admin.example", response.getHeader("Access-Control-Allow-Origin"));
            assertEquals("Content-Type", response.getHeader("Access-Control-Allow-Headers"));
            assertEquals("GET,POST,PUT,DELETE,OPTIONS", response.getHeader("Access-Control-Allow-Methods"));
            assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void exposesLocationWithoutCredentials() throws Exception {
        var response = request("https://front.example", "https://front.example", "POST", null, null);
        assertEquals(204, response.getStatus());
        assertEquals("Location", response.getHeader("Access-Control-Expose-Headers"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    void rejectsEmptyOrDifferentOrigins() throws Exception {
        for (String origins : new String[]{"", " , ", "https://front.example.evil", "http://front.example"}) {
            var response = request(origins, "https://front.example", "OPTIONS", "GET", null);
            assertEquals(403, response.getStatus());
            assertNull(response.getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void rejectsOtherMethodsAndHeaders() throws Exception {
        assertEquals(403, request("https://front.example", "https://front.example",
                "OPTIONS", "PATCH", null).getStatus());
        assertEquals(403, request("https://front.example", "https://front.example",
                "OPTIONS", "POST", "Authorization").getStatus());
    }

    @Test
    void rejectsWildcardConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new CorsConfig().corsFilter("*"));
    }
}
