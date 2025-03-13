# Repository workflow

*Dan Villarreal, University of Pittsburgh*

<https://github.com/djvill/labbcat-server> is a fork of <https://github.com/nzilbb/labbcat-server> (i.e., _upstream_).
This fork is for developing the [LaBB-CAT] user interface[^ui], with two specific purposes:
(1) Tailoring LaBB-CAT instances that I manage (e.g., [APLS]) toward those instances' specific needs.
(2) Suggesting user interface modifications to the main trunk of LaBB-CAT development via pull requests to upstream.

These purposes require a particular workflow for branches, development, deployment, and upstream suggestions.

[^ui]:
  Some finer points:
  
  - By _user interface_, I also mean the framework for developing & deploying the Angular-based LaBB-CAT UI (e.g., [`deploy-user-interface.sh`](deploy-user-interface.sh)).
  - Some of LaBB-CAT's UI is implemented via the 'classic' [legacy code] based on JavaServer Pages. This UI has increasingly been migrated to the Angular framework, on a page-by-page basis, but (as of March 2025) this migration is still ongoing. As a result, some pages' UI can only be modified via their JSP implementation (stored on the server in `<corpus-root-directory>/mvc/`).
  - Down the line, I may try to develop/tailor other functionalities included in both this repo and [`nzilbb/ag`]: the data schema, API, formatter modules, annotator modules, etc.


## Purpose (1) vs. purpose (2)

While these two purposes are in a push-pull relationship, the majority of changes will serve purpose (1).
Main-trunk LaBB-CAT needs to be conservative to not break existing users' code and knowhow.
APLS, free from this burden, needs to be aggressive to maximize accessibility for novice users.
That said, numerous changes to the main-trunk UI over the years have come from my suggestions to Robert Fromont (the main-trunk maintainer), so there will certainly be changes that can serve purpose (2).

### Workflow

- Branches are `main` and `apls`.
- Remotes are `origin` (<https://github.com/djvill/labbcat-server>) and `upstream` (<https://github.com/nzilbb/labbcat-server>).
- Development happens in `apls`.
- Periodic batches of commits are pushed from `apls` to `origin/apls` for backup purposes.
- For testing purposes, `apls` gets deployed to the APLS-Dev corpus (see next section).
- Commits are small, atomic, and targeted (like in upstream).
  - Commit messages start with app/library name (e.g., [`transcripts`]), `Development`, `Deployment`, or `Meta`.
- Optional: feature branches
  - For projects that are more complicated (like making multiple changes to [`lib-layer-checkboxes`]).
  - Branch off of tip of `apls`.
  - While in progress, development happens in the feature branch with deployment to APLS-Dev.
  - Once complete, `git switch apls ; git merge <feature-branch>`.
  - No development in `apls` while feature branch is in-progress, to avoid ambiguity with APLS-Dev.
  - Feature branch doesn't get pushed to `origin`.
- To sync with upstream:
  ```
  git switch main
  git pull upstream main
  git switch apls
  git merge main
  ```
- For a purpose-(2) change:
  1. Ensure `apls` and `main` are synced with upstream
  1. `git cherry-pick` the relevant commit(s) from `apls` to `main`.
  1. `git push origin main`
  1. On <https://github.com/nzilbb/labbcat-server>, create a PR (with `origin/main` as source) for the suggested change.
  N.B. Possible revision in the future: To accommodate simultaneous PRs, PR source should be a feature branch off of `origin/main` rather than `origin/main` itself.


## Purpose (1) complications

Complicating type-(1) modifications are a few factors:
- I need to be conservative with the _actually-deployed_ APLS, both to avoid breaking them and causing downtime and to avoid having to make too many updates to [APLS documentation].
  - Once APLS has a bigger user base, I'll need to be more conservative still.
- Whenever Robert is actively making changes to upstream, I need to avoid getting out too far in front of them, not only to avoid potential merge conflicts if we work on the same thing, but also to have my changes be in sync with LaBB-CAT deployments.
- These considerations also apply if I create a new LaBB-CAT corpus that's in the "APLS dialect" (like the radio corpus).

### Workflow

TBD




[labb-cat]: https://nzilbb.github.io/labbcat-doc
[apls]: https://apls.pitt.edu
[legacy code]: https://sourceforge.net/projects/labbcat/
[`nzilbb/ag`]: https://github.com/nzilbb/ag
[`lib-layer-checkboxes`]: user-interface/src/main/angular/projects/labbcat-view/src/app
[`lib-layer-checkboxes`]: user-interface/src/main/angular/projects/labbcat-common/src/lib/layer-checkboxes
[apls documentation]: https://djvill.github.io/APLS
