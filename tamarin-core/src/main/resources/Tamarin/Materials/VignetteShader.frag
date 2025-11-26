//#define FRAGMENT_SHADER
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform float m_VignetteAmount; // Intensity of the vignette (0.0 = no vignette, 1.0 = full vignette)
uniform float m_EdgeWidth;      // Width of the soft edge for the vignette
uniform vec4 m_VignetteColor;   // The color of the vignette (RGBA)
uniform vec2 g_Resolution;      // Screen resolution in pixels

void main() {
    if(m_VignetteAmount == 0.0){
         gl_FragColor = vec4(0.0,0.0,0.0,0.0);
    } else if(m_VignetteAmount == 1.0){
         gl_FragColor = m_VignetteColor;
    } else {
        // Get the fragment's screen position in normalized coordinates (0 to 1)
        vec2 uv = gl_FragCoord.xy / g_Resolution;

        // Compute the radial distance from the center
        vec2 center = vec2(0.5, 0.5); // Normalized screen center
        float distance = length(uv - center);

        // Compute the vignette effect
        float vignetteRadius = 0.5 - m_VignetteAmount * 0.5; // Radius of the clear area
        float edgeStart = vignetteRadius + m_EdgeWidth; // Where fading starts

        // Smooth transition between clear center and vignette edge
        float vignetteFactor = smoothstep(vignetteRadius, edgeStart, distance);

        // Apply the vignette effect to the color, modulating its alpha
        vec4 vignetteEffect = m_VignetteColor;
        vignetteEffect.a *= vignetteFactor; // Scale alpha by vignette strength
        vignetteEffect.b *= vignetteFactor;
        gl_FragColor = vignetteEffect;
    }
}