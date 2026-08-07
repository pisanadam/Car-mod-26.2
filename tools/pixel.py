"""Küçük piksel-sanat yardımcı kütüphanesi (numpy RGBA tabanlı)."""

import numpy as np
from PIL import Image

TRANSPARENT = (0, 0, 0, 0)


class Canvas:
    def __init__(self, w, h, fill=TRANSPARENT):
        self.w, self.h = w, h
        self.px = np.zeros((h, w, 4), dtype=np.uint8)
        self.px[:, :] = _rgba(fill)

    # -- temel çizim ------------------------------------------------------
    def rect(self, x, y, w, h, color):
        c = _rgba(color)
        x0, y0 = max(0, x), max(0, y)
        x1, y1 = min(self.w, x + w), min(self.h, y + h)
        if x1 > x0 and y1 > y0:
            self.px[y0:y1, x0:x1] = c

    def px_set(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y, x] = _rgba(color)

    def hline(self, x, y, w, color):
        self.rect(x, y, w, 1, color)

    def vline(self, x, y, h, color):
        self.rect(x, y, 1, h, color)

    def line(self, x0, y0, x1, y1, color):
        """Bresenham."""
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.px_set(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def outline_rect(self, x, y, w, h, color):
        self.hline(x, y, w, color)
        self.hline(x, y + h - 1, w, color)
        self.vline(x, y, h, color)
        self.vline(x + w - 1, y, h, color)

    def disc(self, cx, cy, r, color, inner=0):
        c = _rgba(color)
        yy, xx = np.mgrid[0:self.h, 0:self.w]
        d = (xx - cx) ** 2 + (yy - cy) ** 2
        mask = (d <= r * r) & (d >= inner * inner)
        self.px[mask] = c

    def ring(self, cx, cy, r, thickness, color):
        self.disc(cx, cy, r, color, inner=max(0, r - thickness))

    # -- doku efektleri ---------------------------------------------------
    def noise(self, x, y, w, h, amount, seed=0):
        """Bölgeye hafif parlaklık gürültüsü ekler (alfa korunur)."""
        rng = np.random.default_rng(seed)
        x1, y1 = min(self.w, x + w), min(self.h, y + h)
        region = self.px[y:y1, x:x1, :3].astype(np.int16)
        n = rng.integers(-amount, amount + 1, size=region.shape[:2])[:, :, None]
        self.px[y:y1, x:x1, :3] = np.clip(region + n, 0, 255).astype(np.uint8)

    def vshade(self, x, y, w, h, top_delta, bottom_delta):
        """Dikey parlaklık geçişi (üstten alta)."""
        x1, y1 = min(self.w, x + w), min(self.h, y + h)
        hh = y1 - y
        if hh <= 0:
            return
        t = np.linspace(top_delta, bottom_delta, hh)[:, None, None]
        region = self.px[y:y1, x:x1, :3].astype(np.float32) + t
        self.px[y:y1, x:x1, :3] = np.clip(region, 0, 255).astype(np.uint8)

    def scanlines(self, x, y, w, h, delta, step=2):
        for yy in range(y, min(self.h, y + h), step):
            row = self.px[yy, x:min(self.w, x + w), :3].astype(np.int16) + delta
            self.px[yy, x:min(self.w, x + w), :3] = np.clip(row, 0, 255).astype(np.uint8)

    def blit(self, other, x, y):
        """Alfa harmanlamalı yapıştırma."""
        h, w = other.px.shape[:2]
        x1, y1 = min(self.w, x + w), min(self.h, y + h)
        sw, sh = x1 - x, y1 - y
        if sw <= 0 or sh <= 0:
            return
        src = other.px[:sh, :sw].astype(np.float32)
        dst = self.px[y:y1, x:x1].astype(np.float32)
        a = src[:, :, 3:4] / 255.0
        out_rgb = src[:, :, :3] * a + dst[:, :, :3] * (1 - a)
        out_a = np.clip(src[:, :, 3:4] + dst[:, :, 3:4] * (1 - a), 0, 255)
        self.px[y:y1, x:x1, :3] = out_rgb.astype(np.uint8)
        self.px[y:y1, x:x1, 3:4] = out_a.astype(np.uint8)

    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(self.px, "RGBA").save(path)


def _rgba(c):
    if len(c) == 3:
        return (c[0], c[1], c[2], 255)
    return c


def shade(color, delta):
    """Rengi delta kadar aydınlat/karart."""
    return tuple(int(max(0, min(255, color[i] + delta))) for i in range(3))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
