package com.urbit_iot.porton.umods;

/**
 * Used with the filter spinner in the tasks list.
 */
public enum UModsFilterType {
    /**
     * Do not filter tasks.
     */
    ALL_UMODS,

    /**
     * Filters only the active (not completed yet) tasks.
     */
    NOTIF_EN_UMODS,

    /**
     * Filters only the completed tasks.
     */
    NOTIF_DIS_UMODS,
}
