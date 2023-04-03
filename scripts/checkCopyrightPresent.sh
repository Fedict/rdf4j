#!/bin/bash
filesWithOutCopyright=()
filesWithOutSPDX=()
for i in $(find ../ -name pom.xml); do
  # only look in non test files but make sure src/main exists.
  # and not in package-info
  dir="${i/pom.xml/}src/"
  if [ -d "$dir" ]; then
    for c in $(find ${dir} -name *.java -not -name package-info.java -exec grep -L Copyright {} \;); do
      # skip a file that is generated by.
      f=$(grep -L 'Generated By:' $c)
      if [ ! -z $f ]; then
        filesWithOutCopyright+=($f)
      fi
    done
    for c in $(find ${dir} -name *.java -not -name package-info.java -exec grep -L 'SPDX-License-Identifier: BSD-3-Clause' {} \;); do
        filesWithOutSPDX+=($c)
    done
  fi
done

for f in ${filesWithOutCopyright[@]}; do
  echo "Missing copyright: $f"
done
for f in ${filesWithOutSPDX[@]}; do
  echo "Missing SPDX line in: $f"
done
if [ ${#filesWithOutCopyright[@]} -ne 0 ]; then
  exit 1
fi
if [ ${#filesWithOutSPDX[@]} -ne 0 ]; then
  exit 2
fi
