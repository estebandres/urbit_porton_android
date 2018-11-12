package com.urbit_iot.porton.umodusers;

/**
 * Used with the filter spinner in the umod users list.
 */
public enum UModUsersFilterType {
    /**
     * Do not filter users.
     */
    ALL_UMOD_USERS,

    /**
     * Filters only the users that are not admins.
     */
    ADMINS,

    /**
     * Filters only the users that are admins.
     */
    NOT_ADMINS,

    /**
     * Filters every user that is not pending of approval.
     */
    PENDING_USERS,
}
