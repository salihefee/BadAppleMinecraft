reuploaded cause i noticed it didnt have the pom.xml

the code is scuffed asf
# BadAppleMinecraft

A Minecraft plugin that plays any video you want, on a screen made out of blocks. It requires ffmpeg to be installed on
the system, and added to PATH.

## Commands

### `/extract <videoPath> <height>`

This command uses ffmpeg to extract frames from the video specified. The width is automatically calculated to keep the
aspect ratio.

### `/badapple <video>`

Iterates through the PNG files generated by /extractr, processes them and renders them on the screen. Include the
extension in the video argument.

### `/cancel`

Stops playback. Stops all if there are multiple videos playing.
