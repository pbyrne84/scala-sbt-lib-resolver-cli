set -e
sbt test
sbt assembly
mkdir -p binaries

#see if we need to do a new image etc. - does not work as cmp finds difference on repeated runs
echo "Comparing binaries"
set +e
is_not_same=$(cmp target/scala-2.13/scala-sbt-lib-resolver-cli.jar  binaries/scala-sbt-lib-resolver-cli.jar)
set -e
echo "Finished comparing binaries is_not_same = $is_not_same"

if [ "$is_not_same" -eq "0" ]
then
  echo "No change in fat jar build, exiting."
  exit 0
fi

echo "Adding updated fat jar to git"
cp target/scala-2.13/scala-sbt-lib-resolver-cli.jar binaries
git add -f binaries/scala-sbt-lib-resolver-cli.jar

