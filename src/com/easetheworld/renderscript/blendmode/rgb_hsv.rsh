// https://git.gnome.org/browse/gimp/tree/libgimpcolor/gimpcolorspace.c

// r, g, b : 0~1
// h : 0~6, s,v : 0~1
static float3 rgb_to_hsv(float3 rgb) {
	float max = fmax(fmax(rgb.r, rgb.g), rgb.b);
	float min = fmin(fmin(rgb.r, rgb.g), rgb.b);

	float v = max;
	float s;
	float h;
	float delta = max - min;
	if (delta > 0.f) {
		s = delta / v;
		if (rgb.r == max) {
			h = (rgb.g - rgb.b) / delta;
			if (h < 0.f) {
				h += 6.f;
			}
		} else if (rgb.g == max) {
			h = (rgb.b - rgb.r) / delta + 2.f;
		} else {
			h = (rgb.r - rgb.g) / delta + 4.f;
		}
	} else {
		s = 0.f;
		h = 0.f;
	}
	
	return (float3){h, s, v};
}

// s, g, b : 0~1
// h : 0~6, s,v : 0~1
static float3 hsv_to_rgb(float3 hsv) {
	float h = hsv.r;
	float s = hsv.g;
	float v = hsv.b;

	float3 rgb;
	
	if (s == 0.f) {
		rgb = (float3){v, v, v};
	} else {
		if (h == 6.f) {
			h = 0.f;
		}

		int i = (int)h;
		float f = h - (int)h;
		float w = v * (1.f - s);
		float q = v * (1.f - s * f);
		float t = v * (1.f - s * (1.f - f));
		
		switch(i) {
		case 0:
			rgb = (float3){v, t, w};
		break;
		case 1:
			rgb = (float3){q, v, w};
		break;
		case 2:
			rgb = (float3){w, v, t};
		break;
		case 3:
			rgb = (float3){w, q, v};
		break;
		case 4:
			rgb = (float3){t, w, v};
		break;
		case 5:
			rgb = (float3){v, w, q};
		break;
		}
	}
	return rgb;
}