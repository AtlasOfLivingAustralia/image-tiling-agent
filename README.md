### image-tiling-agent   [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/image-tiling-agent.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/image-tiling-agent)

Command line Image loading utility for tiling images into an instance of the [image-service](https://github.com/AtlasOfLivingAustralia/image-service). 

This can be used to parallelise the tiling of images across multiple machines. Tiling is CPU intensive, so this utility can spread the load across multiple machines when a large number of images (100,000) need to be tiled.

This application builds to an executable jar file.
