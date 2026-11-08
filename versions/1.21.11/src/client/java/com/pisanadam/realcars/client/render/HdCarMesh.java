package com.pisanadam.realcars.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pisanadam.realcars.RealCars;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Gerçek bir araç modelinden dönüştürülmüş yüksek poligonlu gövde.
 *
 * <p>Dosyalar {@code tools/import_hd_models.py} tarafından açık lisanslı GLB
 * modellerden üretilir ({@code assets/realcars/hd/<id>.rcm}, gzip'li, küçük
 * uçlu ikili). Her dosyada parçalar (boyanan gövde, boyasız gövde, cam, far,
 * stop, dört tekerlek) ve her parçanın iki detay seviyesi vardır: yakın
 * görünüm için yüksek, uzak görünüm için düşük poligon.
 *
 * <p>Koordinatlar blok cinsindendir; burun -Z'ye bakar, Y yukarıdır, zemin
 * y=0'dadır. Tekerlek köşeleri kendi göbeklerine göredir ki dönüş ve
 * direksiyon göbek etrafında uygulanabilsin.
 *
 * <p>Minecraft'ın varlık çizimi dörtgenlerle çalışır; dönüştürücü komşu
 * üçgenleri dörtgene birleştirir, eşsiz kalan üçgen son köşesi tekrarlanmış
 * bir dörtgen olarak gelir.
 */
public final class HdCarMesh {
	private static final int MAGIC = 'R' | 'C' << 8 | 'M' << 16 | '1' << 24;
	private static final int VERSION = 1;

	public enum Kind {
		PAINT, PLAIN, GLASS, LIGHT_FRONT, LIGHT_REAR, WHEEL
	}

	/** Bir parçanın tek detay seviyesi. */
	public static final class Lod {
		final int vertexCount;
		final float[] pos;
		final float[] nrm;
		final float[] uv;
		final int[] quads;
		/** Her çizimde yeniden kullanılan dönüştürülmüş köşe tamponları. */
		private final float[] tp;
		private final float[] tn;

		Lod(final int vertexCount, final float[] pos, final float[] nrm, final float[] uv, final int[] quads) {
			this.vertexCount = vertexCount;
			this.pos = pos;
			this.nrm = nrm;
			this.uv = uv;
			this.quads = quads;
			this.tp = new float[vertexCount * 3];
			this.tn = new float[vertexCount * 3];
		}

		public int quadCount() {
			return this.quads.length / 4;
		}

		/**
		 * Köşeleri bir kez dönüştürüp dörtgenleri yazar. Aynı köşe ortalama
		 * dört dörtgende kullanıldığı için matris çarpımını köşe başına bir
		 * kez yapmak çizimi belirgin şekilde hızlandırır.
		 *
		 * <p>Çizim tek iş parçacığında (render thread) yapıldığı için geçici
		 * tamponlar nesneye ait olabilir.
		 */
		public void emit(final PoseStack.Pose pose, final VertexConsumer out, final int color,
						 final int overlay, final int light) {
			final Matrix4f m = pose.pose();
			final Matrix3f n = pose.normal();
			final float m00 = m.m00(), m01 = m.m01(), m02 = m.m02();
			final float m10 = m.m10(), m11 = m.m11(), m12 = m.m12();
			final float m20 = m.m20(), m21 = m.m21(), m22 = m.m22();
			final float m30 = m.m30(), m31 = m.m31(), m32 = m.m32();
			final float n00 = n.m00(), n01 = n.m01(), n02 = n.m02();
			final float n10 = n.m10(), n11 = n.m11(), n12 = n.m12();
			final float n20 = n.m20(), n21 = n.m21(), n22 = n.m22();
			final float[] p = this.pos;
			final float[] q = this.nrm;
			final float[] tp = this.tp;
			final float[] tn = this.tn;
			for (int i = 0, k = 0; i < this.vertexCount; i++, k += 3) {
				final float x = p[k], y = p[k + 1], z = p[k + 2];
				tp[k] = m00 * x + m10 * y + m20 * z + m30;
				tp[k + 1] = m01 * x + m11 * y + m21 * z + m31;
				tp[k + 2] = m02 * x + m12 * y + m22 * z + m32;
				final float a = q[k], b = q[k + 1], c = q[k + 2];
				float nx = n00 * a + n10 * b + n20 * c;
				float ny = n01 * a + n11 * b + n21 * c;
				float nz = n02 * a + n12 * b + n22 * c;
				final float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (len > 1.0E-6F) {
					nx /= len;
					ny /= len;
					nz /= len;
				}
				tn[k] = nx;
				tn[k + 1] = ny;
				tn[k + 2] = nz;
			}
			final float[] uv = this.uv;
			final int[] idx = this.quads;
			for (int i = 0; i < idx.length; i++) {
				final int v = idx[i];
				final int k = v * 3;
				out.addVertex(tp[k], tp[k + 1], tp[k + 2], color, uv[v * 2], uv[v * 2 + 1], overlay, light,
					tn[k], tn[k + 1], tn[k + 2]);
			}
		}
	}

	public record Part(Kind kind, boolean front, boolean left, float pivotX, float pivotY, float pivotZ,
					   List<Lod> lods) {
		public Lod lod(final int level) {
			return this.lods.get(Math.min(level, this.lods.size() - 1));
		}
	}

	private final List<Part> parts;
	private final float[] profile;
	private final float length;
	private final float width;
	private final float height;

	private HdCarMesh(final List<Part> parts, final float[] profile, final float width, final float height,
					  final float length) {
		this.parts = parts;
		this.profile = profile;
		this.width = width;
		this.height = height;
		this.length = length;
	}

	public List<Part> parts() {
		return this.parts;
	}

	public float width() {
		return this.width;
	}

	public float height() {
		return this.height;
	}

	public float length() {
		return this.length;
	}

	/** En yüksek detay seviyesindeki toplam dörtgen sayısı. */
	public int quadCount() {
		int total = 0;
		for (final Part part : this.parts) {
			total += part.lod(0).quadCount();
		}
		return total;
	}

	/**
	 * Gövdenin verilen boyuna konum aralığındaki en yüksek noktası (blok).
	 * Rüzgarlık, bagaj kapağının gerçek yüksekliğine oturtulurken kullanılır.
	 */
	public float maxHeight(final float fromZ, final float toZ) {
		final int bins = this.profile.length;
		final float half = this.length / 2.0F;
		int a = (int) Math.floor((fromZ + half) / this.length * bins);
		int b = (int) Math.floor((toZ + half) / this.length * bins);
		a = Math.max(0, Math.min(bins - 1, a));
		b = Math.max(0, Math.min(bins - 1, b));
		float max = 0.0F;
		for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
			max = Math.max(max, this.profile[i]);
		}
		return max;
	}

	public static Identifier meshId(final String carId) {
		return RealCars.id("hd/" + carId + ".rcm");
	}

	public static Identifier textureId(final String carId) {
		return RealCars.id("textures/entity/car/hd/" + carId + ".png");
	}

	/**
	 * Kaynak paketlerinden modeli okur. Dosya yoksa ya da bozuksa null döner;
	 * o zaman araç eski parametrik gövdesiyle çizilir.
	 */
	public static HdCarMesh load(final ResourceManager resources, final String carId) {
		final Optional<Resource> resource = resources.getResource(meshId(carId));
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream raw = resource.get().open(); InputStream in = new GZIPInputStream(raw, 1 << 16)) {
			return parse(ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN));
		} catch (final IOException | RuntimeException e) {
			RealCars.LOGGER.warn("Yüksek poligonlu model okunamadı ({}), parametrik gövdeye dönülüyor", carId, e);
			return null;
		}
	}

	static HdCarMesh parse(final ByteBuffer buf) {
		if (buf.getInt() != MAGIC) {
			throw new IllegalArgumentException("RCM dosyası değil");
		}
		final int version = Short.toUnsignedInt(buf.getShort());
		if (version != VERSION) {
			throw new IllegalArgumentException("desteklenmeyen RCM sürümü " + version);
		}
		final float width = buf.getFloat();
		final float height = buf.getFloat();
		final float length = buf.getFloat();
		final float[] profile = new float[Byte.toUnsignedInt(buf.get())];
		for (int i = 0; i < profile.length; i++) {
			profile[i] = buf.getFloat();
		}
		final int partCount = Short.toUnsignedInt(buf.getShort());
		final List<Part> parts = new ArrayList<>(partCount);
		final Kind[] kinds = Kind.values();
		for (int p = 0; p < partCount; p++) {
			final Kind kind = kinds[Byte.toUnsignedInt(buf.get())];
			final int flags = Byte.toUnsignedInt(buf.get());
			final float px = buf.getFloat();
			final float py = buf.getFloat();
			final float pz = buf.getFloat();
			final int lodCount = Byte.toUnsignedInt(buf.get());
			final List<Lod> lods = new ArrayList<>(lodCount);
			for (int l = 0; l < lodCount; l++) {
				lods.add(readLod(buf));
			}
			parts.add(new Part(kind, (flags & 1) != 0, (flags & 2) != 0, px, py, pz,
				Collections.unmodifiableList(lods)));
		}
		return new HdCarMesh(Collections.unmodifiableList(parts), profile, width, height, length);
	}

	private static Lod readLod(final ByteBuffer buf) {
		final int n = buf.getInt();
		final float[] pos = new float[n * 3];
		buf.asFloatBuffer().get(pos);
		buf.position(buf.position() + n * 12);
		final float[] nrm = new float[n * 3];
		for (int i = 0; i < nrm.length; i++) {
			nrm[i] = buf.get() / 127.0F;
		}
		final float[] uv = new float[n * 2];
		for (int i = 0; i < uv.length; i++) {
			uv[i] = Short.toUnsignedInt(buf.getShort()) / 65535.0F;
		}
		final int q = buf.getInt();
		final int[] quads = new int[q * 4];
		buf.asIntBuffer().get(quads);
		buf.position(buf.position() + q * 16);
		for (final int index : quads) {
			if (index < 0 || index >= n) {
				throw new IllegalArgumentException("dörtgen indisi aralık dışı: " + index);
			}
		}
		return new Lod(n, pos, nrm, uv, quads);
	}
}
