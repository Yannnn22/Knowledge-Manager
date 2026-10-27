package zjc.knowledgemanager.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private static JwtUtil jwtUtil;

    @Autowired
    public AuthUtil(JwtUtil jwtUtil) {
        AuthUtil.jwtUtil = jwtUtil;
    }

    public static Long getUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return 1L;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return 1L;
        }
    }

    public static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}