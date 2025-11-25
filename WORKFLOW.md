# Repository workflow

*Dan Villarreal, University of Pittsburgh*

<https://github.com/djvill/labbcat-server> is a fork of <https://github.com/nzilbb/labbcat-server> (i.e., _upstream_).
This fork is for developing the [LaBB-CAT] user interface[^ui], with two specific purposes:
1. Tailoring LaBB-CAT instances that I manage (e.g., [APLS]) toward those instances' specific needs.
2. Suggesting user interface modifications to the main trunk of LaBB-CAT development via pull requests to upstream.

[^ui]: 
    Some finer points:
    
    - By _user interface_, I also mean the framework for developing & deploying the Angular-based LaBB-CAT UI (e.g., [`deploy-user-interface.sh`](deploy-user-interface.sh)).
    - Some of LaBB-CAT's UI is implemented via the 'classic' [legacy code] based on JavaServer Pages. This UI has increasingly been migrated to the Angular framework, on a page-by-page basis, but (as of September 2025) this migration is still ongoing. As a result, some pages' UI can only be modified via their JSP implementation (stored on the server in `<corpus-root-directory>/mvc/`).
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

> [!NOTE]
> This workflow has been heavily revised relative to the previous version in [`9d4b6e7`].
> That workflow prioritized developing directly in `apls-dev` and cherry-picking from feature branches.
> But that proved to be a mess when I wanted to contribute to the upstream remote.
> The current workflow instead prioritizes a hard separation between APLS-specific development and main-trunk development.

[`9d4b6e7`]: https://github.com/djvill/labbcat-server/tree/9d4b6e7

### Remotes

|            | URL                                        |
|------------|--------------------------------------------|
| `origin`   | <https://github.com/djvill/labbcat-server> |
| `upstream` | <https://github.com/nzilbb/labbcat-server> |


### Branches

|                            | Purpose                   | Speed   | Start-point  | Merges                                      |
|----------------------------|---------------------------|---------|--------------|---------------------------------------------|
| `upstream`                 | Tracking `upstream/main`  | N/A[^u] | N/A          | `upstream/main`                             |
| `new-corpus`               | Production                | Slow    | `upstream`   | `<feat-branch>`                             |
| `apls`                     | Production                | Medium  | `upstream`   | `apls-dev`                                  |
| `apls-dev`                 | Development               | Fastish | `upstream`   | `exclusive-apls`, `<feat-branch>`           |
| `<addl-labbcat>`[^al]      | Production                | Fastish | `new-corpus` | `exclusive-<addl-labbcat>`, `<feat-branch>` |
| `exclusive-apls`           | Development               | Fast    | `upstream`   | `<limited-feat-branch>`, `apls-dev`         |
| `exclusive-<addl-labbcat>` | Development               | Fast    | `upstream`   | `<limited-feat-branch>`                     |
| `<feat-branch>`            | Development, contribution | Fast    | `apls-dev`   | N/A                                         |
| `<limited-feat-branch>`    | Development               | Fast    | `apls-dev`   | N/A                                         |

[^u]:  Depends on how quickly Robert modifies `upstream/main`
[^al]: One branch per actually-deployed LaBB-CAT instance, named after its root directory on the server


### Commits

- Small, atomic, and targeted (like in upstream remote).
- Commit messages start with one of the following:
  - app/library name (e.g., [`transcripts`], [`layer-checkboxes`])
  - `Development`
  - `Deployment`
  - `Meta` (i.e., documentation)


### Development

- Development happens in `apls-dev`, though changes get **committed** to either:
  - A **general** feature branch (for features to be suggested to `upstream/main`), or
  - An **exclusive** production branch like `exclusive-apls` (for features specific to a single corpus, e.g. deployment paths, APLS-specific wording)
  - A **limited** feature branch (in between the previous two; features not to be suggested to `upstream/main` but that may be useful for multiple corpora)
- Limited feature branches get merged to whichever exclusive production branches are desired
- General feature branches and `exclusive-apls` get merged to `apls-dev`
  - Feature doesn't need to be "complete" before merging
- For testing purposes, `apls-dev` gets deployed to the APLS-Dev corpus.
- Always test changes in APLS-Dev (with [`deploy-view.sh`]) before committing.
- Periodically rebuild the whole app properly with [`deploy-user-interface.sh`]
- Periodic batches of commits are pushed from `apls-dev` to `origin/apls-dev` for backup purposes.
- Periodically [sync with `upstream/main`](#syncing-with-upstreammain).

So the process is like:

1. If a feat branch: `git switch -c <feat-branch> upstream` if it doesn't exist, `git switch <feat-branch>` if it does. Or `git switch exclusive-apls`
1. Modify code
1. `cd user-interface/src/main/angular/`
1. If a feat branch: `git restore -s apls-dev angular.json deploy-view.sh`
1. Run [`deploy-view.sh`]
1. Assess and optionally commit
1. Once done with development, `git restore angular.json ; rm deploy-view.sh`

When ready to merge to `apls-dev`:

1. `git switch apls-dev`
1. `git merge <feat-branch>`
   - Possible future tweak: Use GitHub pull requests instead of merging locally, to keep track of what's been merged when (especially given the multiple production branches)
1. [`deploy-user-interface.sh`](deploy-user-interface.sh)


### Syncing with `upstream/main`

Always start from `upstream`, then merge with `apls-dev`:

```
git switch upstream
git pull upstream main
git push upstream
git switch apls-dev
git merge upstream
```

Possible future change: Use `git rebase` rather than `git merge`

### Deployment

Two scenarios for deploying to in-production corpora: patches and package updates.
These are analogous to when Robert sends me a tweaked, undocumented LaBB-CAT release vs. when he releases a documented version publicly.

- Use patches when necessary to resolve some small _and_ pressing issue in production corpora 
  - While [APLS documentation] is still under construction, favor releasing patches over package updates if it makes documentation easier to write.
  - Patches are _not_ for `new-corpus`
- All other times, wait for a package release
  - TBD how long I'll wait between these


#### Patches 

1. [Sync `apls-dev` with `upstream/main`](#syncing-with-upstreammain)
1. [Development workflow](#development)
1. Patch:
   1. `git switch apls`
   1. `git merge apls-dev`
   1. `sed -i 's/apls-dev/labbcat/' deploy-user-interface.sh`[^stash-1]
   1. Run [`deploy-user-interface.sh`]
1. `git push origin apls`
1. Repeat the "Patch" step for all other production corpora

[^stash-1]: These changes **not** committed to `apls`, so the `apls` commit history doesn't diverge from `apls-dev`.


#### Package releases

1. [Sync `apls-dev` with `upstream/main`](#syncing-with-upstreammain)
1. Update:
   1. `git switch apls`
   1. `git merge apls-dev`
   1. `sed -i 's/apls-dev/labbcat/' deploy-user-interface.sh`[^stash-1]
   1. Run [`deploy-user-interface.sh`]
   1. Increment [APLS version]
1. `git push origin apls`
1. Repeat the "Update" step for all other production corpora
1. Repeat the "Update" step for `new-corpus`
1. `git push origin new-corpus`


### Suggesting main-trunk changes
  
1. Ensure `upstream` and `<feat-branch>` are synced with upstream
1. `git switch <feat-branch>`
1. `cd user-interface/src/main/angular/`
1. `git restore -s apls-dev angular.json deploy-view.sh`[^stash-2]
1. Run [`deploy-view.sh`]
1. `git push -u origin <feat-branch>`
1. At <https://github.com/nzilbb/labbcat-server>, create a PR (with `origin/<feat-branch>` as source) for the suggested change.

[^stash-2]: These changes are **not** committed to `<feat-branch>`, so the `<feat-branch>` commit history doesn't include changes that shouldn't go in the pull request.


## Useful Git idioms

| Idiom | Purpose |
|-------|---------|
| `git diff --name-status --diff-filter=U \| while read a b ; do notepad++ $b ; done`[^id-1] | When there's a merge conflict, open up each file in Notepad++ for conflict resolution. |
| `git log --oneline --no-merges branch1 ^branch2` | View commits on `branch1` that aren't on `branch2` (ignoring merge commits) |
| `git branch -a --contains SHA` | View all branches that contain commit `SHA` |
| `git branch --(no-)merged` | View all branches that are(n't) merged with the current branch |
| `git push origin branch1 branch2` | Push multiple branches to `origin` |

[^id-1]: Only works in root directory.


[labb-cat]: https://nzilbb.github.io/labbcat-doc
[apls]: https://apls.pitt.edu
[legacy code]: https://sourceforge.net/projects/labbcat/
[`nzilbb/ag`]: https://github.com/nzilbb/ag
[`transcripts`]: user-interface/src/main/angular/projects/labbcat-view/src/app/transcripts
[`layer-checkboxes`]: user-interface/src/main/angular/projects/labbcat-common/src/lib/layer-checkboxes
[apls documentation]: https://djvill.github.io/APLS
[`deploy-view.sh`]: user-interface/src/main/angular/deploy-view.sh
[`deploy-user-interface.sh`]: deploy-user-interface.sh
[apls version]: https://github.com/djvill/APLS/tree/main/_versions
