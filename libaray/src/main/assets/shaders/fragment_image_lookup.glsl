varying highp vec2 vTextureCoord;

uniform sampler2D sTexture;
uniform sampler2D currentLutTexture;
uniform sampler2D prevLutTexture;
uniform sampler2D nextLutTexture;

uniform lowp float uIntensity;
uniform lowp float uScrollX;

lowp vec4 getColor(sampler2D lutTexture, highp vec2 texPos1, highp vec2 texPos2, highp float blueColor){
    lowp vec4 newColor1 = texture2D(lutTexture, texPos1);
    lowp vec4 newColor2 = texture2D(lutTexture, texPos2);
    return mix(newColor1, newColor2, fract(blueColor));
}

void main(){
    highp vec4 textureColor = texture2D(sTexture, vTextureCoord);
    highp float blueColor = textureColor.b * 63.0;

    highp vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);

    highp vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    highp vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);

    highp vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);

    lowp vec4 primaryColor = getColor(currentLutTexture, texPos1, texPos2, blueColor);
    if (uScrollX == 0.0){
        gl_FragColor = mix(textureColor, vec4(primaryColor.rgb, textureColor.w), uIntensity);
    }
    if (uScrollX > 0.0) {
        lowp vec4 prevColor = getColor(prevLutTexture, texPos1, texPos2, blueColor);
        if (vTextureCoord.x > uScrollX){
            gl_FragColor = mix(textureColor, vec4(primaryColor.rgb, textureColor.w), uIntensity);
        } else {
            gl_FragColor = mix(textureColor, vec4(prevColor.rgb, textureColor.w), uIntensity);
        }
    }
    if (uScrollX < 0.0) {
        lowp vec4 nextColor = getColor(nextLutTexture, texPos1, texPos2, blueColor);
        if (vTextureCoord.x > (1.0+uScrollX)){
            gl_FragColor = mix(textureColor, vec4(nextColor.rgb, textureColor.w), uIntensity);
        } else {
            gl_FragColor = mix(textureColor, vec4(primaryColor.rgb, textureColor.w), uIntensity);

        }
    }

}