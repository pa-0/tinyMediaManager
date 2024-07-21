ARG OS=max06net/base-gui:0.6.2-bookworm-slim

FROM $OS

ENV APP=tinyMediaManager
ENV ALLOW_DIRECT_VNC=true
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV LC_TIME=C.UTF-8

ARG PKG="apt-get install --no-install-recommends -y --ignore-missing"

RUN apt-get -q update && \
    LC_ALL=C DEBIAN_FRONTEND=noninteractive apt-get -y -q upgrade && \
    LC_ALL=C DEBIAN_FRONTEND=noninteractive ${PKG} \
        ca-certificates \
        fontconfig \
        fonts-dejavu \
        fonts-wqy-zenhei \
        libcurl3-gnutls \
        libmms0 \
        libxtst6 \
        wget \
        yad \
    && rm -rf /var/lib/apt/lists/*

# $TARGETPLATFORM will be "linux/amd64", "linux/arm64", etc
# based on the --platform argument
ARG TARGETPLATFORM
ARG PREFIX=https://mediaarea.net/download/binary
ARG WGET="wget --progress=dot:mega"

RUN TMPD=$(mktemp -d) && \
    echo ${TARGETPLATFORM} && \
    if [ "${TARGETPLATFORM}" = "linux/amd64" ]; then \
        LIBZEN=${PREFIX}/libzen0/0.4.41/libzen0v5_0.4.41-1_amd64.Debian_11.deb; \
        LIBMEDIAINFO=${PREFIX}/libmediainfo0/24.05/libmediainfo0v5_24.05-1_amd64.Debian_11.deb; \
    elif [ "${TARGETPLATFORM}" = "linux/arm64" ]; then \
        LIBZEN=${PREFIX}/libzen0/0.4.41/libzen0v5_0.4.41-1_arm64.Raspbian_11.deb; \
        LIBMEDIAINFO=${PREFIX}/libmediainfo0/24.05/libmediainfo0v5_24.05-1_arm64.Raspbian_11.deb; \
    else \
        echo "Unsupported platform: ${TARGETPLATFORM}" && exit 1; \
    fi && \
    ${WGET} -O ${TMPD}/libzen.deb ${LIBZEN} && \
    ${WGET} -O ${TMPD}/libmediainfo.deb ${LIBMEDIAINFO} && \
    dpkg -i ${TMPD}/lib*.deb && \
    rm -rf ${TMPD}


RUN mkdir -p /data/data

LABEL "org.opencontainers.image.version"="${human.version}"
LABEL "org.opencontainers.image.title"="${project.artifactId}"
LABEL "org.opencontainers.image.description"="${project.description}"
LABEL "org.opencontainers.image.url"="${project.url}"
LABEL "org.opencontainers.image.licenses"="Apache License, Version 2.0"
LABEL "org.opencontainers.image.revision"="${buildNumber}"
LABEL "org.opencontainers.image.documentation"="https://www.tinymediamanager.org/docs/docker"
LABEL "org.opencontainers.image.source"="${project.scm.url}"

# Install tmm & requirements
ADD tinyMediaManager /app

WORKDIR /app
CMD ["/app/tinyMediaManager", "-Dtmm.contentfolder=/data"]
