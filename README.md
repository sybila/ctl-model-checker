[![Release](https://jitpack.io/v/daemontus/Distributed-CTL-Model-Checker.svg)](https://jitpack.io/#daemontus/Distributed-CTL-Model-Checker)
[![Build Status](https://travis-ci.org/daemontus/Distributed-CTL-Model-Checker.svg?branch=master)](https://travis-ci.org/daemontus/Distributed-CTL-Model-Checker)
[![codecov.io](https://codecov.io/github/daemontus/Distributed-CTL-Model-Checker/coverage.svg?branch=master)](https://codecov.io/github/daemontus/Distributed-CTL-Model-Checker?branch=master)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=flat)](https://github.com/daemontus/Distributed-CTL-Model-Checker/blob/master/LICENSE.txt)
[![Kotlin](https://img.shields.io/badge/kotlin-1.0.0--rc--1036-blue.svg)](http://kotlinlang.org)

A CTL model checker library for analysis of parametrized models in distributed environment.

###How to get it
This repo is jitpack-compatibile, so all you have to do is look up the latest version on jitpack and then integrate it into your favorite build system: [Model Checker on Jitpack](https://jitpack.io/#daemontus/Distributed-CTL-Model-Checker)

###How to use
In order to verify your models, you need to provide implementations for KripkeFragment 
(using your Node, NodeSet and Colors classes) and JobQueue. You can also use SingleThreadJobQueue and just
provide your own Communicator (or use the default one, although it is not optimized for performence) and PartitionFunction.

If you plan on using just a single core, you can just provide empty implementation of communicator. No messages will be sent.

There are also sample implementations for all above mentioned interfaces. You are free to use them directly, 
or as an inspiration, although they might not provide optimal performence.

When you have your model implemented, you just need to provide a CTL formula. 
Model checker uses format defined in this [CTL Parser](https://github.com/sybila/CTL-Parser). 
