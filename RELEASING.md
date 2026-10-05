# Releasing to Maven Central

Releases are built from immutable `vMAJOR.MINOR.PATCH` tags that point to commits contained in `main`.
Publishing a stable GitHub Release starts `.github/workflows/release.yml`, which verifies the tag, builds and signs
the artifacts, and publishes them to Maven Central.

## One-time repository setup

1. Verify the `io.github.stasbykov` namespace in the Maven Central Portal.
2. Create a Maven Central Portal user token.
3. Create a password-protected GPG signing key and publish its public key to a public key server.
4. Create the `maven-central` environment in the GitHub repository.
5. Restrict that environment to tags matching `v*` and, if desired, require approval before deployment.
6. Add these environment secrets:

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | Maven Central Portal token username |
   | `MAVEN_CENTRAL_PASSWORD` | Maven Central Portal token password |
   | `MAVEN_GPG_PRIVATE_KEY` | ASCII-armored GPG private key |
   | `MAVEN_GPG_PASSPHRASE` | GPG key passphrase |

Never commit Maven Central credentials, a private signing key, or a generated `settings.xml` file.

## Repository protection

Protect `main` with required pull requests and the `Build` status check. Disable force pushes and branch deletion.
Add a tag ruleset for `v*` that restricts tag creation to maintainers and prevents tag updates and deletion.

## Release process

1. Merge the release changes into `main` and wait for CI to pass.
2. Confirm that the Maven version in `pom.xml` is the version being released.
3. Create a signed tag on the release commit. For example:

   ```shell
   git switch main
   git pull --ff-only
   git tag -s v2.0.0 -m "Release 2.0.0"
   git push origin v2.0.0
   ```

4. Create a GitHub Release for the existing tag and publish it.
5. Approve the `maven-central` environment deployment if approval is enabled.
6. Wait for the `Publish to Maven Central` workflow to complete.
7. Verify the published version on Maven Central.

The tag name and the Maven version in `pom.xml` must match: `v2.0.0` publishes Maven version `2.0.0`.
The workflow rejects prereleases, non-semantic tags, mismatched Maven versions, and tags whose commits are not
contained in `main`.

## Failed releases

If publishing has not reached Maven Central, fix the infrastructure problem and rerun the workflow. If the artifact
has already been published, never move the tag or reuse the version: fix the problem and release a new patch version.
Published Maven Central versions are immutable.

For a local release-artifact check, run:

```shell
./mvnw --batch-mode --no-transfer-progress -Prelease -DskipPublishing=true clean deploy
```

With the pinned Central Publishing plugin version, this verifies the build, tests, sources, Javadocs, and signatures
without uploading artifacts. It does not create a local Central bundle.
