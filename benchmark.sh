#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/openjdk-11 clean 2>&1
sbt -batch -java-home /usr/lib/jvm/openjdk-11 'charts -p overlap=1 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=16 -p partToUpdate=0.1 -p geometry=plane .*' 2>&1 | tee plane_overlap_1.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 'charts -p overlap=10 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=16 -p partToUpdate=0.1 -p geometry=plane .*' 2>&1 | tee plane_overlap_10.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 'charts -p overlap=1 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=4,8,16 -p partToUpdate=0.1 -p geometry=spherical RTree2D.*' 2>&1 | tee spherical_overlap_1.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 'charts -p overlap=10 -p rectSize=10 -p nearestMax=10 -p nodeCapacity=4,8,16 -p partToUpdate=0.1 -p geometry=spherical RTree2D.*' 2>&1 | tee spherical_overlap_10.txt
