#pragma version(1)
#pragma rs java_package_name(com.easetheworld.renderscript.blendmode)

float gSize;

void root(const uchar4 *v_in, uchar4 *v_out, const void* usrData, uint32_t x, uint32_t y) {
    float4 srcRgba = rsUnpackColor8888(*v_in);
    float4 dstRgba = {0.0, 0.5f, 1.0f, x / gSize};
    rsDebug("src=", srcRgba);
    rsDebug("dst=", dstRgba);
    // *v_out = rsPackColorTo8888(dstRgba.r, dstRgba.g, dstRgba.b, dstRgba.a);
    *v_out = rsPackColorTo8888(dstRgba.r*dstRgba.a, dstRgba.g*dstRgba.a, dstRgba.b*dstRgba.a, dstRgba.a);
    rsDebug("dst int2=", *v_out);
}

void dump(uchar4 *aout, uint32_t x, uint32_t y) {
	if (y > 0) return;
    float4 srcRgba = rsUnpackColor8888(*aout);
    rsDebug("dump xy=", x, y);
    rsDebug("dump color=", srcRgba);
}

void solveThirdDegreeEquation(float a, float b, float c, float d) {
	float p = ((3.f * a * c) - (b * b)) / (3.f * a * a);
	float q = ((2.f * b * b * b) - (9.f * a * b * c) + (27.f * a * a * d)) / (27.f * a * a * a);
	float h = (q * q / 4.f) + (p * p * p / 27.f);
	rsDebug("p", p);
	rsDebug("q", q);
	rsDebug("h", h);
}