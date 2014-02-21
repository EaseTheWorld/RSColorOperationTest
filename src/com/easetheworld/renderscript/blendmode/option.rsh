typedef float3 (*processFunc)(float3 in, float3 layer, uint32_t x, uint32_t y);

typedef struct {
    processFunc func;
    rs_allocation layer;
} FilterOption;
