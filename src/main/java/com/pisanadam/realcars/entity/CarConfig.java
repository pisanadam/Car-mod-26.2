package com.pisanadam.realcars.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Bir aracın kalıcı yapılandırması: boya rengi, takılı parçalar ve depodaki
 * yakıt. Hem entity üzerinde tutulur hem de aracı toplayınca item'a bir veri
 * bileşeni (data component) olarak yazılır — böylece modifiye ve yakıt aracı
 * yerden alıp tekrar koyunca kaybolmaz.
 */
public record CarConfig(
	int color,
	WheelType wheel,
	SpoilerType spoiler,
	EngineType engine,
	TransmissionType transmission,
	float fuel
) {
	public static final Codec<CarConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("color").forGetter(CarConfig::color),
		WheelType.CODEC.fieldOf("wheel").forGetter(CarConfig::wheel),
		SpoilerType.CODEC.fieldOf("spoiler").forGetter(CarConfig::spoiler),
		EngineType.CODEC.fieldOf("engine").forGetter(CarConfig::engine),
		TransmissionType.CODEC.fieldOf("transmission").forGetter(CarConfig::transmission),
		Codec.FLOAT.fieldOf("fuel").forGetter(CarConfig::fuel)
	).apply(instance, CarConfig::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CarConfig> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, CarConfig::color,
		WheelType.STREAM_CODEC, CarConfig::wheel,
		SpoilerType.STREAM_CODEC, CarConfig::spoiler,
		EngineType.STREAM_CODEC, CarConfig::engine,
		TransmissionType.STREAM_CODEC, CarConfig::transmission,
		ByteBufCodecs.FLOAT, CarConfig::fuel,
		CarConfig::new
	);

	/** Fabrika çıkışı yapılandırma: deposu dolu, standart parçalarla. */
	public static CarConfig factory(final CarModel model) {
		return new CarConfig(model.defaultColor(), model.defaultWheel(), SpoilerType.SPOILER_NONE,
			model.defaultEngine(), model.defaultTransmission(), model.fuelCapacity());
	}

	public CarConfig withColor(final int newColor) {
		return new CarConfig(newColor, this.wheel, this.spoiler, this.engine, this.transmission, this.fuel);
	}

	public CarConfig withWheel(final WheelType newWheel) {
		return new CarConfig(this.color, newWheel, this.spoiler, this.engine, this.transmission, this.fuel);
	}

	public CarConfig withSpoiler(final SpoilerType newSpoiler) {
		return new CarConfig(this.color, this.wheel, newSpoiler, this.engine, this.transmission, this.fuel);
	}

	public CarConfig withEngine(final EngineType newEngine) {
		return new CarConfig(this.color, this.wheel, this.spoiler, newEngine, this.transmission, this.fuel);
	}

	public CarConfig withTransmission(final TransmissionType newTransmission) {
		return new CarConfig(this.color, this.wheel, this.spoiler, this.engine, newTransmission, this.fuel);
	}

	public CarConfig withFuel(final float newFuel) {
		return new CarConfig(this.color, this.wheel, this.spoiler, this.engine, this.transmission, newFuel);
	}
}
