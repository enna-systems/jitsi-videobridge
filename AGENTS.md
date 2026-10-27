# enna Jitsi Videobridge fork

Read `CLAUDE.md` for upstream architecture and conventions. This fork's integration branch is `dev`.
Changes go through feature branches and pull requests to `dev`; keep upstream `master` untouched.
Documentation and commit messages are in English.

## Dev image

- `dev` starts at upstream `bf271b11f795d6546f8d78dee441c67d9ba2bf6b`, matching JVB `2.3.318-gbf271b11f`.
- `enna/Dockerfile` pins the existing `stable-11248` image and replaces only its media-transform JAR.
  Updating upstream requires checking the base image, JAR path and runtime dependency compatibility together.
- Run `bash enna/build.sh` with Java 17. It runs the full Maven `verify` lifecycle, including the VP9 regression
  tests. Upstream numeric formatting tests require English Java locale; the script sets it explicitly.
- `.github/workflows/enna-dev-image.yml` verifies pull requests and feature branches. Only pushes within this
  repository publish a commit-tagged image to the existing dev registry. No workflow deploys a cluster.
- GitOps owns the deployment and pins a verified image digest. Never update the cluster from this repository.
- `DOCKER_PASSWORD_SCW_DEV` is an Actions secret, configured by a repository administrator. Never read, print,
  copy into source, or request the value in chat. No production credential is needed here.
