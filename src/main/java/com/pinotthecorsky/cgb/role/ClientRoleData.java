package com.pinotthecorsky.cgb.role;

import com.pinotthecorsky.cgb.compat.EmiCompat;
import com.pinotthecorsky.cgb.compat.JeiCompat;
import com.pinotthecorsky.cgb.compat.ReiCompat;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientRoleData {
    private static final Set<String> ROLES = new LinkedHashSet<>();

    public static void setRoles(Collection<String> roles) {
        ROLES.clear();
        ROLES.addAll(roles);
        JeiCompat.onRolesChanged();
        EmiCompat.onRolesChanged();
        ReiCompat.onRolesChanged();
        JeiCompat.onBadgesChanged();
    }

    public static boolean hasRole(String role) {
        if (role == null || role.isEmpty()) {
            return true;
        }
        return ROLES.contains(role);
    }

    public static Set<String> getRoles() {
        return Collections.unmodifiableSet(ROLES);
    }

    private ClientRoleData() {
    }
}
