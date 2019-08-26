[![Build Status](https://travis-ci.org/matsim-eth/python-matsim.svg?branch=master)](https://travis-ci.org/matsim-eth/python-matsim)

Python API for MATSim
=====================

This is a Python API to interact with the MATSim Software.
It is mostly meant for teaching MATSim to non-programmers without loosing too much
of the ability to configure, modify and analyse MATSim runs.

Installation
------------

### Pip

To install the package using `pip`, simply run the following:

```
# The installation procedure requires numpy to be available,
# so you have to install it first if not already available
pip install numpy
pip install pythonmatsim
```

### Conda

Coming soon!

Roadmap
---------

This is a wish-list of what should be there by Fall 2019:

* [x] Fast (enough) event handling
* [ ] Fast (enough) data manipulation
* [x] Good enough IDE support for scenario and config
* [x] Deploy to PyPi
* [ ] Deploy to conda-forge

Changelog
---------

- **Next version - unreleased**
    - fix additional problems with distribution

- **0.1.1 - 2019-08-26**
    - fix distribution of stub files (type hints)

- **0.1.0 - 2019-08-26**
    - static type hints for all java classes 
    - protocol-buffer based event communication
