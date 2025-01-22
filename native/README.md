# Native libs/binaries

## libmediainfo

* Windows - Windows 7 - 11, 64bit, https://mediaarea.net/en/MediaInfo/Download/Windows
* macOS - macOS 10.10 - 12, https://mediaarea.net/en/MediaInfo/Download/Mac_OS

## libtinyfiledialogs

* Windows - Windows x64 - https://sourceforge.net/projects/tinyfiledialogs/files/
* Linux - self compiled - https://sourceforge.net/projects/tinyfiledialogs/files/

Linux x64:

```
docker run --rm -v "$PWD":/usr/src/myapp -w /usr/src/myapp gcc:9-buster bash -c 'gcc  -ansi -std=gnu89 -pedantic -Wstrict-prototypes -Wall -fPIC -c tinyfiledialogs.c && gcc tinyfiledialogs.o -shared -o libtinyfiledialogs.so'
```

Linux arm64:

```
docker run --rm -v "$PWD":/work dockcross/linux-arm64 bash -c '$CC  -ansi -std=gnu89 -pedantic -Wstrict-prototypes -Wall -fPIC -c tinyfiledialogs.c && $CC tinyfiledialogs.o -shared -o libtinyfiledialogs.so'
```

## FFmpeg

* Windows 
  * 64 bit, https://www.gyan.dev/ffmpeg/builds/
* Linux 
  * amd64/arm64 https://ffmpeg.martin-riedl.de/
* macOS
  * amd64/arm64 https://ffmpeg.martin-riedl.de/

