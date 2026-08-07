package com.pisanadam.realcars.entity;

/** Şasi çeşitleri — yalnızca craft girdisi olarak kullanılır. */
public enum ChassisType {
	CHASSIS_COMPACT("chassis_compact"),
	CHASSIS_SPORT("chassis_sport"),
	CHASSIS_OFFROAD("chassis_offroad"),
	CHASSIS_CLASSIC("chassis_classic"),
	CHASSIS_VAN("chassis_van");

	private final String name;

	ChassisType(final String name) {
		this.name = name;
	}

	public String itemName() {
		return this.name;
	}
}
