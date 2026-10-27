# Keep the existing dev JVB and runtime dependencies; replace only the corrected media-transform module.
FROM ghcr.io/jitsi/jvb:stable-11248@sha256:55cf534aa12931c524835269475c1ea63e5e884a43540f9b318378fd080a335c

ARG ENNA_REVISION
LABEL org.opencontainers.image.source="https://github.com/enna-systems/jitsi-videobridge" \
      org.opencontainers.image.revision="${ENNA_REVISION}" \
      care.enna.jvb.upstream-revision="bf271b11f795d6546f8d78dee441c67d9ba2bf6b" \
      care.enna.jvb.fix="vp9-temporal-bitrate"

RUN test -f /usr/share/jitsi-videobridge/lib/jitsi-media-transform-2.3-318-gbf271b11f.jar
COPY target/jitsi-media-transform.jar /usr/share/jitsi-videobridge/lib/jitsi-media-transform-2.3-318-gbf271b11f.jar
COPY target/build-info.properties /usr/share/jitsi-videobridge/enna-build-info.properties
