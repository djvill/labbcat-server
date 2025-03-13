# Repository workflow

*Dan Villarreal, University of Pittsburgh*

<https://github.com/djvill/labbcat-server> is a fork of <https://github.com/nzilbb/labbcat-server> (i.e., _upstream_).
This fork is for developing the [LaBB-CAT] user interface[^ui], with two specific purposes:
(1) Tailoring LaBB-CAT instances that I manage (e.g., [APLS]) toward those instances' specific needs.
(2) Suggesting user interface modifications to the main trunk of LaBB-CAT development via pull requests to upstream.

[^ui]:
  Some finer points:
  
  - By _user interface_, I also mean the framework for developing & deploying the Angular-based LaBB-CAT UI (e.g., [`deploy-user-interface.sh`](deploy-user-interface.sh)).
  - Some of LaBB-CAT's UI is implemented via the 'classic' [legacy code] based on JavaServer Pages. This UI has increasingly been migrated to the Angular framework, on a page-by-page basis, but (as of March 2025) this migration is still ongoing. As a result, some pages' UI can only be modified via their JSP implementation (stored on the server in `<corpus-root-directory>/mvc/`).
  - Down the line, I may try to develop/tailor other functionalities included in both this repo and [`nzilbb/ag`]: the data schema, API, formatter modules, annotator modules, etc.

While these two purposes are in a push-pull relationship, the majority of changes will serve purpose (1).
Main-trunk LaBB-CAT needs to be conservative to not break existing users' code and knowhow.
APLS, free from this burden, needs to be aggressive to maximize accessibility for novice users.
That said, numerous changes to the main-trunk UI over the years have come from my suggestions to Robert Fromont (the main-trunk maintainer), so there will certainly be changes that can serve purpose (2).

Complicating type-(1) modifications are a few factors:
- I need to be conservative with the _actually-deployed_ APLS, both to avoid breaking them and causing downtime and to avoid having to make too many updates to [APLS documentation].
  - Once APLS has a bigger user base, I'll need to be more conservative still.
- Whenever Robert is actively making changes to upstream, I need to avoid getting out too far in front of them, not only to avoid potential merge conflicts if we work on the same thing, but also to have my changes be in sync with LaBB-CAT deployments.
- These considerations also apply if I create a new LaBB-CAT corpus that's in the "APLS dialect" (like the radio corpus).

Considering all of this, I've settled on a particular workflow for branches, development, deployment, and upstream suggestions.

## Workflow


### Remotes

|            | URL                                        |
|------------|--------------------------------------------|
| `origin`   | <https://github.com/djvill/labbcat-server> |
| `upstream` | <https://github.com/nzilbb/labbcat-server> |


### (Local) branches

|                       | Purpose      | Speed   | Start-point  | Cherry-picks from | Merges          | Pushed |
|-----------------------|--------------|---------|--------------|-------------------|-----------------|--------|
| `main`                | Creating PRs | Slowest | [`2075533`]  | `apls-dev`        | N/A             | Yes    |
| `new-corpus`          | Production   | Slow    | [`4e13ef8`]  | N/A               | `apls-dev`      | Yes    |
| `apls`                | Production   | Medium  | [`4e13ef8`]  | `apls-dev`        | `apls-dev`      | Yes    |
| `<addl-labbcat>`[^al] | Production   | Medium  | `new-corpus` | `apls-dev`        | `apls-dev`      | No     |
| `apls-dev`            | Development  | Fast    | [`4e13ef8`]  | N/A               | `<feat-branch>` | Yes    |
| `<feat-branch>`[^fb]  | Development  | Fast    | `apls-dev`   | N/A               | N/A             | No     |

[^al]: One branch per actually-deployed LaBB-CAT instance, named after its root directory on the server
[^fb]: See [below](#optional-feature-branches).

[`2075533`]: https://github.com/djvill/labbcat-server/tree/2075533
[`4e13ef8`]: https://github.com/djvill/labbcat-server/tree/4e13ef8


### Commits

- Small, atomic, and targeted (like in upstream).
- Commit messages start with one of the following:
  - app/library name (e.g., [`transcripts`])
  - `Development`
  - `Deployment`
  - `Meta` (i.e., documentation)


### Development

- Development happens in `apls-dev` (which branched off of `main` .
- For testing purposes, `apls-dev` gets deployed to the APLS-Dev corpus.
- Always test changes in APLS-Dev (with [`deploy-view.sh`]) before committing.
- Periodically rebuild the whole app properly with [`deploy-user-interface.sh`]
- Periodic batches of commits are pushed from `apls-dev` to `origin/apls` for backup purposes.
- Periodically [sync with upstream](#syncing-with-upstream).


#### Optional: feature branches

- For projects that are more complicated (like making multiple changes to [`lib-layer-checkboxes`]).
- Branch off of tip of `apls-dev`.
- While in progress, development happens in the feature branch with deployment to APLS-Dev.
- Once complete, `git switch apls-dev ; git merge <feat-branch>`.
- No development in `apls-dev` while feature branch is in-progress, to avoid ambiguity with APLS-Dev.
- Feature branch doesn't get pushed to `origin`.


### Syncing with upstream

Always start from `main`, then merge with `apls-dev`:

```
git switch main
git pull upstream main
git push main
git switch apls-dev
git merge main
```


### Deployment

Two scenarios for deploying to in-production corpora: patches and package updates.
These are analogous to when Robert sends me a tweaked, undocumented LaBB-CAT release vs. when he releases a documented version publicly.

- Use patches when necessary to resolve some small _and_ pressing issue in production corpora 
  - While [APLS documentation] is still under construction, favor releasing patches over package updates if it makes documentation easier to write.
  - Patches are _not_ for `new-corpus`
- All other times, wait for a package release
  - TBD how long I'll wait between these


#### Patches 

1. [Sync `apls-dev` with upstream](#syncing-with-upstream)
1. Patch:
   1. `git switch apls`
   1. `git cherry-pick <commits>`
   1. [`deploy-user-interface.sh`]
1. `git push origin apls`
1. Repeat the "Patch" step for all other production corpora


#### Package releases

1. [Sync `apls-dev` with upstream](#syncing-with-upstream)
   - N.B. Possible revision in the future: _Don't_ sync `apls-dev` with upstream _until_ the next (public) LaBB-CAT release.
1. Update:
   1. `git switch apls`
   1. `git merge apls-dev`
   1. Resolve any merge conflicts
      - If there have been any patches since the last package release, there might be some weirdness, but it'll all work out
   1. [`deploy-user-interface.sh`]
   1. Increment [APLS version]
1. `git push origin apls`
1. Repeat the "Update" step for all other production corpora
1. Repeat the "Update" step for `new-corpus`
1. `git push origin new-corpus`


### Suggesting main-trunk changes
  
1. Ensure `apls-dev` and `main` are synced with upstream
1. `git cherry-pick` the relevant commit(s) from `apls-dev` to `main`.
1. `git push origin main`
1. On <https://github.com/nzilbb/labbcat-server>, create a PR (with `origin/main` as source) for the suggested change.
- N.B. Possible revision in the future: To accommodate multiple simultaneous PRs, PR source should be a feature branch off of `origin/main` rather than `origin/main` itself.



[labb-cat]: https://nzilbb.github.io/labbcat-doc
[apls]: https://apls.pitt.edu
[legacy code]: https://sourceforge.net/projects/labbcat/
[`nzilbb/ag`]: https://github.com/nzilbb/ag
[`transcripts`]: user-interface/src/main/angular/projects/labbcat-view/src/app/transcripts
[`lib-layer-checkboxes`]: user-interface/src/main/angular/projects/labbcat-common/src/lib/layer-checkboxes
[apls documentation]: https://djvill.github.io/APLS
[`deploy-view.sh`]: user-interface/src/main/angular
[apls version]: https://github.com/djvill/APLS/tree/main/_versions
