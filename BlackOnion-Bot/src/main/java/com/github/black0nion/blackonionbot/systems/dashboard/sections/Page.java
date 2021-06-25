package com.github.black0nion.blackonionbot.systems.dashboard.sections;

/**
 * @author _SIM_
 */
public enum Page {
    OVERVIEW("Overview", "overview"), JOINLEAVE("Join/Leave", "joinleave"), SETUP("Setup", "setup"), ACTIONS("Actions", "actions"), MISC("Miscellaneous", "misc");

    private final String id;
    private final String name;

    private Page(final String name, final String id) {
	this.name = name;
	this.id = id;
    }

    /**
     * @return the id
     */
    public String getId() {
	return this.id;
    }

    /**
     * @return the pretty name
     */
    public String getName() {
	return this.name;
    }
}