package org.hoyo.celestia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MaintenanceFilter extends OncePerRequestFilter {

    private final AtomicBoolean maintenance = new AtomicBoolean(false);

    public void engage() {
        maintenance.set(true);
    }

    public void release() {
        maintenance.set(false);
    }

    public boolean isActive() {
        return maintenance.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (maintenance.get()) {
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write("{\"maintenance\":true}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
