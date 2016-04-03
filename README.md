[![Release](https://jitpack.io/v/sybila/ctl-model-checker.svg)](https://jitpack.io/#sybila/ctl-model-checker)
[![Build Status](https://travis-ci.org/sybila/ctl-model-checker.svg?branch=master)](https://travis-ci.org/sybila/ctl-model-checker)
[![codecov.io](https://codecov.io/github/sybila/ctl-model-checker/coverage.svg?branch=master)](https://codecov.io/github/sybila/ctl-model-checker?branch=master)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=flat)](https://github.com/sybila/ctl-model-checker/blob/master/LICENSE.txt)
[![Kotlin](https://img.shields.io/badge/kotlin-1.0.1-blue.svg)](http://kotlinlang.org)

A CTL model checker library for analysis of parametrized models in distributed environment. This library is part of the BioDivine package.

###How to use
This repo is jitpack-compatibile, so all you have to do is look up the latest version on jitpack and then integrate it into your favorite build system: [CTL Model Checker on Jitpack](https://jitpack.io/#sybila/ctl-model-checker)

###API
In order to verify your models, you need to provide implementations for KripkeFragment 
(using your Node, NodeSet and Colors classes) and JobQueue. You can also use default implementations:
SingleThreadJobQueue, MapNodes, SharedMemoryCommunicator. However, note that these are not very well optimised.
(Their primary purpose is testing)

If you plan on using just a single core, you can just provide empty implementation of communicator. No messages will be sent.

When you have your model implemented, you just need to provide a CTL formula. 
Model checker uses format defined in this [CTL Parser](https://github.com/sybila/ctl-parser).
