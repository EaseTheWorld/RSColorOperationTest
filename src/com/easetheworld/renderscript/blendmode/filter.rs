#pragma version(1)
#pragma rs java_package_name(com.easetheworld.renderscript.blendmode)

#include "rgb_hsv.rsh"
#include "option.rsh"

static rs_allocation inputAllocation;
rs_allocation drawingAllocation;
rs_allocation blendingAllocation;
rs_allocation blurAllocation;

static float3 processNormal(float3 in, float3 layer, uint32_t x, uint32_t y) {
    return layer;
}

static float3 processEraser(float3 in, float3 layer, uint32_t x, uint32_t y) {
    return in;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationlightenonlymode.c
static float3 processLightenOnly(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = fmax(in, layer);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationscreenmode.c
static float3 processScreen(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = 1.f - (1.f - in) * (1.f - layer);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationdodgemode.c
static float3 processDodge(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in / (1.f - layer);
    return fmin(pixel, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationadditionmode.c
static float3 processAddition(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in + layer;
    return fmin(pixel, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationdarkenonlymode.c
static float3 processDarkenOnly(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = fmin(in, layer);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationmultiplymode.c
static float3 processMultiply(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in * layer;
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationburnmode.c
static float3 processBurn(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = 1.f - (1.f - in) / layer;
    return clamp(pixel, 0.f, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationoverlaymode.c
static float3 processOverlay(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in * (in + (2.f * layer) * (1.f - in));
    return fmin(pixel, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationsoftlightmode.c
static float3 processSoftLight(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 multiply = in * layer;
    float3 screen = 1.f - (1.f - in) * (1.f - layer);
    float3 pixel = (1.f - in) * multiply + in * screen;
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationhardlightmode.c
static float processHardLightComp(float in, float layer) {
	float comp;
    if (layer > 0.5f) {
        comp = (1.f - in) * (1.f - (layer - 0.5f) * 2.f);
        comp = fmin(1.f - comp, 1.f);
    } else {
        comp = in * (layer * 2.f);
        comp = fmin(comp, 1.f);
    }
    return comp;
}
static float3 processHardLight(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel;
    pixel.r = processHardLightComp(in.r, layer.r);
    pixel.g = processHardLightComp(in.g, layer.g);
    pixel.b = processHardLightComp(in.b, layer.b);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationdifferencemode.c
static float3 processDifference(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = fabs(in - layer);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationsubtractmode.c
static float3 processSubtract(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = fmax(0.f, in - layer);
    return pixel;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationgrainextractmode.c
static float3 processGrainExtract(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in - layer + 0.5f;
    return clamp(pixel, 0.f, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationgrainmergemode.c
static float3 processGrainMerge(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in + layer - 0.5f;
    return clamp(pixel, 0.f, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationdividemode.c
static float3 processDivide(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 pixel = in / layer;
    // It seems fmin/fmax handles Inf/-Inf quite well.
    return fmin(pixel, 1.f);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationhuemode.c
static float3 processHue(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 inHsv = rgb_to_hsv(in);
    float3 layerHsv = rgb_to_hsv(layer);
    if (layerHsv.g > 0.f) {
    	inHsv.r = layerHsv.r; // replace h
    }
    return hsv_to_rgb(inHsv);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationsaturationmode.c
static float3 processSaturation(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 inHsv = rgb_to_hsv(in);
    float3 layerHsv = rgb_to_hsv(layer);
    inHsv.g = layerHsv.g; // replace s
    return hsv_to_rgb(inHsv);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationvaluemode.c
static float3 processValue(float3 in, float3 layer, uint32_t x, uint32_t y) {
    float3 inHsv = rgb_to_hsv(in);
    float3 layerHsv = rgb_to_hsv(layer);
    inHsv.b = layerHsv.b; // replace v
    return hsv_to_rgb(inHsv);
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationcolormode.c
static float3 processColor(float3 in, float3 lay, uint32_t x, uint32_t y) {
    // dstHSL = {layH, layS, srcL} (replace only h and s)
    float srcMax = fmax(fmax(in.r, in.g), in.b);
    float srcMin = fmin(fmin(in.r, in.g), in.b);
    float srcL = (srcMax + srcMin) / 2.f;

    float layMax;
    float layMin;
    float layMid;
        
    // find min, mid, max
    if (lay.r > lay.g) {
        if (lay.g > lay.b) {
        	layMax = lay.r;
        	layMid = lay.g;
        	layMin = lay.b;
        } else {
        	layMin = lay.g;
        	if (lay.r > lay.b) {
        		layMax = lay.r;
        		layMid = lay.b;
        	} else {
        		layMax = lay.b;
        		layMid = lay.r;
        	}
        }
    } else {
        if (lay.r > lay.b) {
        	layMax = lay.g;
        	layMid = lay.r;
        	layMin = lay.b;
        } else {
        	layMin = lay.r;
        	if (lay.g > lay.b) {
        		layMax = lay.g;
        		layMid = lay.b;
        	} else {
        		layMax = lay.b;
        		layMid = lay.g;
        	}
        }
    }
	if (layMax == layMin) {
		return (float3) {srcL, srcL, srcL};
	}
        
    float layDelta = layMax - layMin;
    float layS = layDelta / (1.f - fabs(layMax + layMin - 1.f));

    // dstDelta = dstMax - dstMin
    float dstDelta = (1.f - fabs(2.f * srcL - 1.f)) * layS;
    float dstMin = srcL - dstDelta / 2.f;
    float dstMax = dstDelta + dstMin;
    float dstMid = dstDelta * (layMid - layMin) / layDelta + dstMin;

    // rgb order of dstRGB is same as layRGB.
	float3 dst;
    if (lay.r > lay.g) {
        if (lay.g > lay.b) {
        	dst.r = dstMax;
        	dst.g = dstMid;
        	dst.b = dstMin;
        } else {
        	dst.g = dstMin;
        	if (lay.r > lay.b) {
        		dst.r = dstMax;
        		dst.b = dstMid;
        	} else {
        		dst.b = dstMax;
        		dst.r = dstMid;
        	}
        }
    } else {
        if (lay.r > lay.b) {
        	dst.g = dstMax;
        	dst.r = dstMid;
        	dst.b = dstMin;
        } else {
        	dst.r = dstMin;
        	if (lay.g > lay.b) {
        		dst.g = dstMax;
        		dst.b = dstMid;
        	} else {
        		dst.b = dstMax;
        		dst.g = dstMid;
        	}
        }
    }
    
    return dst;
}

static float getApproxSCurve(float x, float b) {
	// b == 0.00 : 1/2
	// b == 0.25 : sqrt(x/2)
	// b == 0.50 : x
	// b == 0.75 : 2x^2
	// b == 1.00 : 4x^3
	// interpolate
	if (b == 0.f) {
		return 0.5f;
	} else if (b < 0.25f) {
		b = b * 4.f;
		return 0.5f * (1.f - b) + sqrt(x * 0.5f) * b;
	} else if (b < 0.5f) {
		b = (b - 0.25f) * 4.f;
		return sqrt(x * 0.5f) * (1.f - b) + x * b;
	} else if (b < 0.75f) {
		b = (b - 0.5f) * 4.f;
		return x * (1.f + (2.f * x - 1.f) * b);
	} else {
		b = (b - 0.75f) * 4.f;
		return 2.f * x * x * (1.f + (2.f * x - 1.f) * b);
	}
}

static float3 processToneCurve(float3 in, float3 layer, uint32_t x, uint32_t y) {
	float3 dst;
	dst.r = getApproxSCurve(in.r, layer.r);
	dst.g = getApproxSCurve(in.g, layer.g);
	dst.b = getApproxSCurve(in.b, layer.b);
	return dst;
}

// https://git.gnome.org/browse/gimp/tree/app/operations/gimpoperationbrightnesscontrast.c
static float3 processBC(float3 in, float3 layer, uint32_t x, uint32_t y) {
	float b = layer.r;
	float c = layer.g;
	
	// this could be done by each rgb component.
	float3 dst;
	b = 2.f * b - 1.f;
	if (b < 0.f)
		dst = in * (1.f + b);
	else
		dst = in + ((1.f - in) * b);

	c = 4.f * c * c;//tanpi((c + 1.f) / 4.f); // I think tangent is too steep.
	dst = (dst - 0.5f) * c + 0.5f;
	dst = clamp(dst, 0.f, 1.f);
	return dst;
}

const static uint32_t mosaicSize = 20;
static float3 processMosaic(float3 in, float3 layer, uint32_t x, uint32_t y) {
	int xm = mosaicSize * (x / mosaicSize);
	int ym = mosaicSize * (y / mosaicSize);
    const uchar4* mosaic = rsGetElementAt(inputAllocation, xm, ym);
    float4 mosaicRgba = rsUnpackColor8888(*mosaic);
    return mosaicRgba.rgb;
}

const static float noiseIntensity = 0.15f;
static float3 processNoise(float3 in, float3 layer, uint32_t x, uint32_t y) {
	float3 dst;
	dst.r = in.r + rsRand(-noiseIntensity, noiseIntensity);
	dst.g = in.g + rsRand(-noiseIntensity, noiseIntensity);
	dst.b = in.b + rsRand(-noiseIntensity, noiseIntensity);
	dst = clamp(dst, 0.f, 1.f);
    return dst;
}

static float3 processBlurPrecomputed(float3 in, float3 layer, uint32_t x, uint32_t y) {
	const uchar4* pixel = rsGetElementAt(blurAllocation, x, y);
	float4 rgba = rsUnpackColor8888(*pixel);
	return rgba.rgb;
}

const static int blurRadius = 3;
const static float blurSize = (2 * blurRadius + 1) * (2 * blurRadius + 1);

static float3 processBlur(float3 in, float3 layer, uint32_t x, uint32_t y) {
	float3 dstRgb = {0.f, 0.f, 0.f};
	float size = 0.f;
	for (int nx = x - blurRadius; nx <= x + blurRadius; nx++) {
       	for (int ny = y - blurRadius; ny <= y + blurRadius; ny++) {
       		const uchar4* neigh = rsGetElementAt(inputAllocation, nx, ny);
       		float4 neighRgba = rsUnpackColor8888(*neigh);
			dstRgb += neighRgba.rgb;
			size += 1.f;
       	}
	}
	dstRgb /= size;
    return dstRgb;
}

// original sepia algorithm is
// r' = 0.393r + 0.769g + 0.189b
// g' = 0.349r + 0.686g + 0.168b
// b' = 0.272r + 0.534g + 0.131b
// I generalized(and approximated) this to
// r' = 0.189*c
// g' = (0.189+0.131)/2*c
// b' = 0.131*c
// c = 2r + 4g + b;
// if input rgb=(1,0.5,0) this is similar to sepia.
static float3 processSepia(float3 in, float3 layer, uint32_t x, uint32_t y) {
	float3 dst = (0.131f + (0.189f - 0.131f) * layer) * (2.f * in.r + 4.f * in.g + in.b);
	dst = clamp(dst, 0.f, 1.f);
    return dst;
}

const static processFunc PROCESS_FUNC_TABLE[] = {
	&processNormal,
	&processEraser,

	&processLightenOnly,
	&processScreen,
	&processDodge,
	&processAddition,

	&processDarkenOnly,
	&processMultiply,
	&processBurn,

	&processOverlay,
	&processSoftLight,
	&processHardLight,

	&processDifference,
	&processSubtract,
	&processGrainExtract,
	&processGrainMerge,
	&processDivide,

	&processHue,
	&processSaturation,
	&processColor,
	&processValue,

	&processToneCurve,
	&processSepia,
	&processBC,

	&processMosaic,
	&processNoise,
	&processBlurPrecomputed,
};

// out = alphaMix(blending, filter(src, drawing))
void root(const uchar4 *v_in, uchar4 *v_out, const void* usrData, uint32_t x, uint32_t y) {
    // assume srcRgba.a = 1, blendingRgba.a = 1, 0<=drawingRgba.a<=1
    const uchar4* drawing = rsGetElementAt(drawingAllocation, x, y);
    float4 drawingRgba = rsUnpackColor8888(*drawing);
    const uchar4* blending = rsGetElementAt(blendingAllocation, x, y);
    if (drawingRgba.a == 0.f) {
    	*v_out = *blending;
    } else {
    	FilterOption* fo = (FilterOption*)usrData;
    	float4 srcRgba = rsUnpackColor8888(*v_in);
    	float4 dstRgba = rsUnpackColor8888(*blending);
    	// drawingRgb is alpha-premultiplied. so divide by alpha.
    	float3 filterRgb = fo->func(srcRgba.rgb, drawingRgba.rgb / drawingRgba.a, x, y);
    	dstRgba.rgb = mix(dstRgba.rgb, filterRgb, drawingRgba.a);
    	*v_out = rsPackColorTo8888(dstRgba.rgb);
    }
}

void __attribute__((overloadable)) filter(rs_script script, int mode, rs_allocation in, rs_allocation out) {
	inputAllocation = in;
	FilterOption fo;
	fo.func = PROCESS_FUNC_TABLE[mode];
	rsForEach(script, in, out, &fo, sizeof(FilterOption), NULL);
}

void __attribute__((overloadable)) filter(rs_script script, int mode, rs_allocation in, rs_allocation out, int left, int top, int right, int bottom) {
	inputAllocation = in;
	FilterOption fo;
	fo.func = PROCESS_FUNC_TABLE[mode];

	rs_script_call_t sc;
	sc.xStart = left;
	sc.xEnd = right;
	sc.yStart = top;
	sc.yEnd = bottom;
	sc.zStart = 0;
	sc.zEnd = 0;
	rsForEach(script, in, out, &fo, sizeof(FilterOption), &sc);
}