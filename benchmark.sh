#!/bin/bash
sbt -Dsbt.supershell=false clean 2>&1
sbt -Dsbt.supershell=false 'charts -jvm /usr/lib/jvm/openjdk-12/bin/java -p overlap=1 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=16 -p partToUpdate=0.1 -p geometry=plane .*' 2>&1 | tee plane_overlap_1.txt
sbt -Dsbt.supershell=false 'charts -jvm /usr/lib/jvm/openjdk-12/bin/java -p overlap=10 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=16 -p partToUpdate=0.1 -p geometry=plane .*' 2>&1 | tee plane_overlap_10.txt
sbt -Dsbt.supershell=false 'charts -jvm /usr/lib/jvm/openjdk-12/bin/java -p overlap=1 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=4,8,16 -p partToUpdate=0.1 -p geometry=spherical RTree2D.*' 2>&1 | tee spherical_overlap_1.txt
sbt -Dsbt.supershell=false 'charts -jvm /usr/lib/jvm/openjdk-12/bin/java -p overlap=10 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=4,8,16 -p partToUpdate=0.1 -p geometry=spherical RTree2D.*' 2>&1 | tee spherical_overlap_10.txt
