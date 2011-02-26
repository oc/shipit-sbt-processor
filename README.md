WTF?
====

Will git tag your app, push, bump the version, push and reload.... And create peace on earth.

Reqs:
-----

In your project you need a repository. Configure to match your maven / ivy2 repo. oc pity the fools who don't have a repo.

<code>
val publishTo = "repo" at "http://server/repo/releases"
Credentials(Path.userHome + ".credentials")
</code>

<code>
*shipit is shipit shipit-processor 0.0.1
</code>

<code>
shipit
</code>