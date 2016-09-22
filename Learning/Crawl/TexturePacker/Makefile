JAR=../../../bin/runnable-texturepacker.jar
INPUT=tmp
OUTPUT=../android/assets/texture
PACKNAME=claudius
SPRITE_SHEET_ORIGINAL=sprites_map_claudius.png

pack:
	java -cp ${JAR} com.badlogic.gdx.tools.texturepacker.TexturePacker ${INPUT} ${OUTPUT} ${PACKNAME}
	rm -rf tmp

unpack:
	rm -rf tmp; mkdir -p tmp; cd tmp; convert -crop 32x64 ../$(SPRITE_SHEET_ORIGINAL)  claudius_%d.png
