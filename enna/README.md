# enna dev JVB image

This fork retains the current dev JVB revision, `bf271b11f795d6546f8d78dee441c67d9ba2bf6b`
(`2.3.318-gbf271b11f`). It fixes cumulative VP9 temporal-layer bitrate accounting in `Vp9Parser`.

Flexible VP9's scalability structure describes spatial layers but not all temporal layers. When a new temporal
layer appeared, the parser copied T0's dependencies without including T0 itself. The allocator therefore saw only
the incremental bitrate of T1/T2 and could select a stream exceeding the receiver's actual bandwidth budget.
The fix adds temporal dependencies and conditional spatial dependencies, including out-of-order layer discovery.
K-SVC still excludes lower spatial delta layers. VP8/H.264 and the network bandwidth estimator are unchanged.

Five regression tests cover cumulative temporal rates, scalability-structure refresh with retained measurements,
K-SVC, full SVC and non-flexible VP9. Before the fix, four fail and the non-flexible control passes.

## Build

Requirements: Java 17, Maven >= 3.6.3, Git and Docker.

```sh
bash enna/build.sh
docker build --platform linux/amd64 --build-arg ENNA_REVISION="$(git rev-parse HEAD)" \
  -t enna-jvb:vp9-temporal-bitrate enna
```

The script builds this checkout directly and runs the full upstream Maven `verify` lifecycle. Maven arguments can
be appended, e.g. `-Dmaven.repo.local=/path/to/cache`. Generated image inputs are ignored under `enna/target/`.
The Dockerfile pins `ghcr.io/jitsi/jvb:stable-11248` by digest and replaces only its media-transform JAR. Runtime
JVB, RTP, other dependencies and startup remain from that image. Build provenance is stored in the image at
`/usr/share/jitsi-videobridge/enna-build-info.properties` and in OCI labels.

## CI and dev rollout

`dev` is the integration branch; feature branches use `TKO/…`. The enna workflow runs verification for pull
requests, feature pushes and dev pushes. Image publication runs only for pushes to this repository, after
successful verification. External pull requests never receive registry credentials.

A repository administrator must configure Actions secret `DOCKER_PASSWORD_SCW_DEV` for the existing dev registry,
or grant this repository access to an equivalent organization secret. No production secret is used.

Images are published as:

```
rg.fr-par.scw.cloud/dev-enna-nsp/enna-jvb:stable-11248-vp9-<full-git-commit>
```

CI retains test reports and build provenance and reports the pushed digest. GitOps separately pins that verified
digest in its dev call overlay. There is no deployment step in this fork. If the registry secret is added after
a failed publication attempt, rerun the failed job; verification artifacts remain available for seven days.

After the GitOps merge, compare VP9 under a controlled bandwidth bottleneck, then recovery and mixed-device calls.
Keep client codec settings and `JVB_CC_TRUST_BWE=1` constant. Record rendered/decoded frames, resolution, FPS,
packet loss, NACK/PLI, freezes, estimated bandwidth and allocated layer bitrate. Unit tests establish the accounting
fix; practical call quality must be confirmed on dev.

Rollback in GitOps to the previous upstream image:
`ghcr.io/jitsi/jvb:stable-11248@sha256:55cf534aa12931c524835269475c1ea63e5e884a43540f9b318378fd080a335c`.
