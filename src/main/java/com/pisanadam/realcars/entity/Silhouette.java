package com.pisanadam.realcars.entity;

/**
 * Gövde tipi. Aracın yan profili {@link CarModel.Body} içindeki noktalarla
 * verilir; siluet ise o profilin <em>nasıl</em> yumuşatılacağını söyler.
 *
 * <p>Minecraft yalnızca eksen hizalı kutu üretebildiği için her eğri, uygun
 * açıyla döndürülmüş ince panellerin (bkz. {@code CarMeshFactory}) zinciriyle
 * kurulur. {@code segments} kaç panele bölüneceğini, {@code bow} ise zincirin
 * düz kirişten ne kadar dışa kavis yapacağını (kirişin uzunluğuna oran)
 * belirler. Yani {@code bow = 0} düz bir yüzey, {@code bow = 0.15} belirgin bir
 * kubbe demektir.
 */
public enum Silhouette {
	/** Arka cam uzun ve yatık, bagaj çıkıntısı kısa: Mustang, GT-R, 911. */
	FASTBACK(2, 0.05F, 2, 0.06F, 0.04F, false),
	/** Belirgin üç kutu — kaput / kabin / bagaj: M3, Corolla. */
	NOTCHBACK(1, 0.02F, 1, 0.02F, 0.03F, false),
	/** Dik arka kapak, bagaj çıkıntısı yok: Golf GTI. */
	HATCHBACK(2, 0.04F, 1, 0.02F, 0.03F, false),
	/** Neredeyse dik camlar, düz tavan, düz kaput: Wrangler, Defender. */
	BOXY(1, 0.0F, 1, 0.0F, 0.01F, false),
	/** Kabin + ayrı kasa: Hilux, C10. */
	PICKUP(1, 0.01F, 1, 0.0F, 0.02F, true),
	/** Kaputtan tavana, tavandan motor kapağına kesintisiz kavis: Beetle. */
	ROUNDED(3, 0.14F, 3, 0.16F, 0.10F, false),
	/** Ön aksın üstünde dik ön cam, burunsuz: Transporter. */
	FORWARD_CONTROL(2, 0.10F, 1, 0.0F, 0.03F, false);

	private final int noseSegments;
	private final float noseBow;
	private final int tailSegments;
	private final float tailBow;
	private final float roofCrown;
	private final boolean bed;

	Silhouette(final int noseSegments, final float noseBow, final int tailSegments, final float tailBow,
			   final float roofCrown, final boolean bed) {
		this.noseSegments = noseSegments;
		this.noseBow = noseBow;
		this.tailSegments = tailSegments;
		this.tailBow = tailBow;
		this.roofCrown = roofCrown;
		this.bed = bed;
	}

	/** Kaput kaç panele bölünür. */
	public int noseSegments() {
		return this.noseSegments;
	}

	/** Kaputun kavis miktarı (kiriş uzunluğuna oran). */
	public float noseBow() {
		return this.noseBow;
	}

	/** Arka cam + bagaj kapağı kaç panele bölünür. */
	public int tailSegments() {
		return this.tailSegments;
	}

	public float tailBow() {
		return this.tailBow;
	}

	/** Tavanın ortadaki hafif kubbesi. */
	public float roofCrown() {
		return this.roofCrown;
	}

	/** Kabinin arkasında ayrı bir kasa var mı? */
	public boolean hasBed() {
		return this.bed;
	}
}
