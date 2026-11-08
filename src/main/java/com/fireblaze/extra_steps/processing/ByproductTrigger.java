package com.fireblaze.extra_steps.processing;

public enum ByproductTrigger {

    ON_COMPLETE("on_complete"),
    PER_INTERACTION("per_interaction");

    private final String id;

    ByproductTrigger(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ByproductTrigger fromString(String id) {

        for (ByproductTrigger trigger : values()) {
            if (trigger.id.equalsIgnoreCase(id)) {
                return trigger;
            }
        }

        return ON_COMPLETE;
    }
}