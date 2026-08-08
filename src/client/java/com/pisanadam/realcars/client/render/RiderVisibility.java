package com.pisanadam.realcars.client.render;

/**
 * Çizim durumuna "bu varlık bir arabanın içinde" bilgisini iliştiren arayüz.
 *
 * <p>Çizim durumu (render state) varlığın kendisine erişmez — zaten amacı
 * çizimi varlıktan koparmaktır. Bu yüzden bilgi, durum çıkarılırken bir kere
 * yazılır ve çizim sırasında oradan okunur.
 * {@code EntityRenderStateMixin} bu arayüzü {@code EntityRenderState}'e
 * ekler, yani her çizim durumu bu arayüze güvenle dönüştürülebilir.
 *
 * <p>Arayüz bilerek mixin paketinin dışındadır: mixin paketindeki sınıflar
 * dönüştürülmek üzere ayrıldığı için doğrudan referans verilemez.
 */
public interface RiderVisibility {
	boolean realcars$isInCar();

	void realcars$setInCar(boolean inCar);
}
