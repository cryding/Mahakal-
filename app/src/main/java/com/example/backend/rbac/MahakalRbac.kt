package com.example.backend.rbac

/**
 * The three strict roles in the MAHAKAL platform hierarchy.
 * ADMIN -> AGENT -> USER
 */
enum class AccountRole {
    ADMIN,
    AGENT,
    USER
}

/**
 * Account operational status.
 * ACTIVE: Login allowed, sessions valid.
 * SUSPENDED: Login denied, all sessions immediately invalidated.
 * DISABLED: Permanent deactivation.
 */
enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    DISABLED
}

/**
 * Centralized permissions defined in the MAHAKAL specification.
 */
enum class Permission {
    // Admin permissions
    ADMIN_MANAGE_AGENTS,
    ADMIN_MANAGE_USERS,
    ADMIN_MANAGE_AGENT_COINS,
    ADMIN_VIEW_TRANSACTIONS,
    ADMIN_VIEW_REPORTS,
    ADMIN_VIEW_AUDIT,
    ADMIN_MANAGE_SYSTEM,
    ADMIN_MANAGE_GAMES,

    // Agent permissions
    AGENT_CREATE_USER,
    AGENT_MANAGE_OWN_USERS,
    AGENT_MANAGE_OWN_USER_COINS,
    AGENT_VIEW_OWN_TRANSACTIONS,
    AGENT_VIEW_OWN_USERS,

    // User permissions
    USER_VIEW_PROFILE,
    USER_VIEW_BALANCE,
    USER_VIEW_TRANSACTIONS,
    USER_USE_GAME,
    USER_CHANGE_PASSWORD
}

/**
 * Security context established after successful server-side token authentication.
 * All downstream controllers and services MUST derive identity solely from this context.
 */
data class SecurityContext(
    val accountId: String,
    val loginId: String,
    val role: AccountRole,
    val status: AccountStatus,
    val permissions: Set<Permission>,
    val parentId: String? = null
)

/**
 * Centralized RBAC and Hierarchical Ownership Guard.
 */
object MahakalRbac {

    private val ROLE_PERMISSIONS: Map<AccountRole, Set<Permission>> = mapOf(
        AccountRole.ADMIN to setOf(
            Permission.ADMIN_MANAGE_AGENTS,
            Permission.ADMIN_MANAGE_USERS,
            Permission.ADMIN_MANAGE_AGENT_COINS,
            Permission.ADMIN_VIEW_TRANSACTIONS,
            Permission.ADMIN_VIEW_REPORTS,
            Permission.ADMIN_VIEW_AUDIT,
            Permission.ADMIN_MANAGE_SYSTEM,
            Permission.ADMIN_MANAGE_GAMES
        ),
        AccountRole.AGENT to setOf(
            Permission.AGENT_CREATE_USER,
            Permission.AGENT_MANAGE_OWN_USERS,
            Permission.AGENT_MANAGE_OWN_USER_COINS,
            Permission.AGENT_VIEW_OWN_TRANSACTIONS,
            Permission.AGENT_VIEW_OWN_USERS
        ),
        AccountRole.USER to setOf(
            Permission.USER_VIEW_PROFILE,
            Permission.USER_VIEW_BALANCE,
            Permission.USER_VIEW_TRANSACTIONS,
            Permission.USER_USE_GAME,
            Permission.USER_CHANGE_PASSWORD
        )
    )

    fun getPermissionsForRole(role: AccountRole): Set<Permission> {
        return ROLE_PERMISSIONS[role] ?: emptySet()
    }

    /**
     * Reusable guard requiring authenticated security context.
     */
    fun requireAuthentication(context: SecurityContext?): SecurityContext {
        if (context == null) {
            throw SecurityException("UNAUTHENTICATED: Authentication token is missing or invalid.")
        }
        if (context.status != AccountStatus.ACTIVE) {
            throw SecurityException("FORBIDDEN: Account status is ${context.status}. Access denied.")
        }
        return context
    }

    /**
     * Reusable guard requiring a specific role.
     */
    fun requireRole(context: SecurityContext, requiredRole: AccountRole) {
        requireAuthentication(context)
        if (context.role != requiredRole) {
            throw SecurityException("FORBIDDEN: Role ${context.role} cannot perform actions reserved for $requiredRole.")
        }
    }

    /**
     * Reusable guard requiring a specific permission.
     */
    fun requirePermission(context: SecurityContext, permission: Permission) {
        requireAuthentication(context)
        if (!context.permissions.contains(permission)) {
            throw SecurityException("FORBIDDEN: Account lacking required permission: $permission")
        }
    }

    /**
     * Hierarchical Resource Ownership Check:
     * - Admin can manage subordinated Agents.
     * - Agent can ONLY manage Users where user.agentId == authenticatedAgent.id.
     * - User can ONLY access their own resource (resourceOwnerId == authenticatedUser.id).
     */
    fun validateResourceOwnership(
        context: SecurityContext,
        resourceOwnerId: String,
        resourceAgentId: String? = null
    ): Boolean {
        requireAuthentication(context)
        return when (context.role) {
            AccountRole.ADMIN -> true
            AccountRole.AGENT -> {
                // Agent can access the resource only if the resource is owned by the agent,
                // or the resource belongs to a User assigned to this Agent.
                context.accountId == resourceOwnerId || context.accountId == resourceAgentId
            }
            AccountRole.USER -> {
                context.accountId == resourceOwnerId
            }
        }
    }
}
