package com.pisanadam.realcars.entity;

/**
 * Sürücünün tuş durumu. Bu Minecraft sürümünde oyuncu girdisi için ortak bir
 * kayıt yoktur; araç yalnızca sürücünün istemcisinde sürüldüğü için girdi
 * oradan her tick aktarılır.
 */
public record DriverInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
	public static final DriverInput EMPTY = new DriverInput(false, false, false, false, false);
}
